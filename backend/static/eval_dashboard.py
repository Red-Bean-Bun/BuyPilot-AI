"""BuyPilot-AI Eval Dashboard — Streamlit-based evaluation results viewer.

Usage:
    cd backend
    streamlit run static/eval_dashboard.py

Requires: pip install streamlit plotly requests
"""

from __future__ import annotations

import os
import sys
from pathlib import Path

import plotly.graph_objects as go
import requests
import streamlit as st

API_BASE = os.getenv("EVAL_API_BASE", "http://localhost:8000")

# 从项目根目录 .env 读取 admin key，复用 FastAPI 的配置加载逻辑
_ENV_PATH = Path(__file__).resolve().parents[3] / ".env"
if _ENV_PATH.exists():
    for _raw in _ENV_PATH.read_text(encoding="utf-8").splitlines():
        _line = _raw.strip()
        if _line and not _line.startswith("#") and "=" in _line:
            _key, _val = _line.split("=", 1)
            os.environ.setdefault(_key.strip(), _val.strip())
_ADMIN_KEY = os.getenv("ADMIN_API_KEY", "")
_AUTH = {"Authorization": f"Bearer {_ADMIN_KEY}"}

st.set_page_config(page_title="BuyPilot Eval", layout="wide")
st.title("BuyPilot-AI 评测看板")

# ── helpers ──────────────────────────────────────────────────────────────────

RADAR_METRICS = [
    "faithfulness",
    "recall_at_10",
    "constraint_satisfaction",
    "context_precision",
    "answer_correctness",
]


def _ensure_samples_seeded():
    """Seed eval samples if the table is empty."""
    try:
        resp = requests.get(f"{API_BASE}/admin/eval/samples", timeout=5, headers=_AUTH)
        if resp.status_code == 200 and len(resp.json()) == 0:
            with st.spinner("Seeding eval samples..."):
                requests.post(f"{API_BASE}/admin/eval/samples/seed", timeout=10, headers=_AUTH)
    except requests.ConnectionError:
        st.warning(f"Cannot connect to API at {API_BASE}. Start the FastAPI server first.")


def _fetch_runs():
    try:
        resp = requests.get(f"{API_BASE}/admin/eval/runs", timeout=10, headers=_AUTH)
        if resp.status_code == 200:
            return resp.json()
    except requests.ConnectionError:
        pass
    return []


def _fetch_run_detail(run_id: str):
    try:
        resp = requests.get(f"{API_BASE}/admin/eval/runs/{run_id}", timeout=10, headers=_AUTH)
        if resp.status_code == 200:
            return resp.json()
    except requests.ConnectionError:
        pass
    return None


def _metric_mean(run: dict, key: str) -> float | None:
    m = run.get("metrics", {}).get(key, {})
    if isinstance(m, dict):
        return m.get("mean")
    return None


# ── sidebar ──────────────────────────────────────────────────────────────────

_ensure_samples_seeded()
runs = _fetch_runs()

st.sidebar.header("Filters")
strategy_tags = sorted({r.get("strategy_tag", "?") for r in runs})
selected_tags = st.sidebar.multiselect(
    "Strategy", strategy_tags, default=strategy_tags
)

filtered_runs = [r for r in runs if r.get("strategy_tag") in selected_tags]
selected_run_ids = st.sidebar.multiselect(
    "Compare Runs (max 5)",
    [r["id"] for r in filtered_runs],
    default=[r["id"] for r in filtered_runs[: min(5, len(filtered_runs))]],
)

# ── tabs ─────────────────────────────────────────────────────────────────────

tab1, tab2, tab3, tab4 = st.tabs(["总览", "版本对比", "样本详情", "错误分析"])

# ── tab 1: Overview ──────────────────────────────────────────────────────────

with tab1:
    if not runs:
        st.info("暂无评测数据。先启动 FastAPI 服务，然后运行 `python -m src.scripts.eval` 执行一次评测。")
    else:
        latest = runs[0]
        st.subheader(f"最新评测: {latest['run_name']} ({latest['strategy_tag']})")
        st.caption(f"时间: {latest['created_at']} | 样本数: {latest['sample_count']} | git: {latest.get('git_commit', '-')}")

        col1, col2, col3, col4, col5 = st.columns(5)
        with col1:
            overall = latest.get("overall_score", 0)
            st.metric("综合分", f"{overall:.2f}")

        with col2:
            f_mean = _metric_mean(latest, "faithfulness")
            st.metric("Faithfulness", f"{f_mean:.2%}" if f_mean else "-")
        with col3:
            r_mean = _metric_mean(latest, "recall_at_10")
            st.metric("Recall@10", f"{r_mean:.2%}" if r_mean else "-")
        with col4:
            cs_mean = _metric_mean(latest, "constraint_satisfaction")
            st.metric("约束满足率", f"{cs_mean:.2%}" if cs_mean else "-")
        with col5:
            h_mean = _metric_mean(latest, "hallucination_rate")
            st.metric("幻觉率", f"{h_mean:.2%}" if h_mean else "-")

        st.subheader("P0 指标雷达图")
        radar_vals = []
        for m in RADAR_METRICS:
            v = _metric_mean(latest, m)
            radar_vals.append(v if v is not None else 0)
        fig = go.Figure(
            data=go.Scatterpolar(r=radar_vals, theta=RADAR_METRICS, fill="toself", name=latest["strategy_tag"])
        )
        fig.update_layout(polar=dict(radialaxis=dict(range=[0, 1])))
        st.plotly_chart(fig, use_container_width=True)

        if len(filtered_runs) > 1:
            st.subheader("指标趋势")
            metric_choice = st.selectbox("选择指标", RADAR_METRICS + ["hallucination_rate", "overall_score"], key="trend_metric")
            trend_runs = sorted(filtered_runs, key=lambda r: r["created_at"])
            x_labels = [r["strategy_tag"] for r in trend_runs]
            y_vals = [_metric_mean(r, metric_choice) or 0 for r in trend_runs]
            fig2 = go.Figure(data=go.Bar(x=x_labels, y=y_vals, text=[f"{v:.2%}" for v in y_vals], textposition="auto"))
            fig2.update_layout(yaxis_range=[0, 1], title=f"{metric_choice} 变化")
            st.plotly_chart(fig2, use_container_width=True)

# ── tab 2: Version Comparison ────────────────────────────────────────────────

with tab2:
    compare_ids = selected_run_ids[:5]
    if len(compare_ids) < 2:
        st.info("在侧边栏选择至少 2 个评测运行以进行比较。")
    else:
        compare_runs = [_fetch_run_detail(rid) for rid in compare_ids]
        compare_runs = [r for r in compare_runs if r]

        st.subheader("指标对比表")
        all_metric_keys = set()
        for r in compare_runs:
            m = r.get("metrics", {})
            all_metric_keys.update(k for k in m if isinstance(m[k], dict) and "mean" in m[k])

        table_data = []
        for key in sorted(all_metric_keys):
            row = {"指标": key}
            for r in compare_runs:
                v = _metric_mean(r, key)
                row[r["strategy_tag"]] = f"{v:.2%}" if v is not None else "-"
            table_data.append(row)
        st.dataframe(table_data, use_container_width=True)

        st.subheader("并排柱状图对比")
        core_metrics = ["faithfulness", "recall_at_10", "constraint_satisfaction", "context_precision"]
        fig3 = go.Figure()
        for i, metric in enumerate(core_metrics):
            fig3.add_trace(
                go.Bar(
                    name=metric,
                    x=[r["strategy_tag"] for r in compare_runs],
                    y=[_metric_mean(r, metric) or 0 for r in compare_runs],
                    text=[f"{_metric_mean(r, metric) or 0:.2%}" for r in compare_runs],
                    textposition="auto",
                )
            )
        fig3.update_layout(barmode="group", yaxis_range=[0, 1], title="核心指标对比")
        st.plotly_chart(fig3, use_container_width=True)

# ── tab 3: Sample Details ────────────────────────────────────────────────────

with tab3:
    if not selected_run_ids:
        st.info("在侧边栏选择一个评测运行查看样本详情。")
    else:
        detail = _fetch_run_detail(selected_run_ids[0])
        if detail and detail.get("samples_detail"):
            st.subheader(f"样本详情 — {detail['run_name']}")
            for sample in detail["samples_detail"]:
                with st.expander(f"{sample['sample_id']}: {sample['question'][:60]}... ({sample['scenario_type']})"):
                    col_a, col_b = st.columns(2)
                    with col_a:
                        st.write("**指标得分**")
                        st.json(sample["metrics"])
                    with col_b:
                        st.write("**详情**")
                        st.json(sample["details"])
        else:
            st.info("该评测运行没有样本详情数据。")

# ── tab 4: Error Analysis ────────────────────────────────────────────────────

with tab4:
    st.subheader("低分样本分析")
    threshold = st.slider("Faithfulness 阈值", 0.0, 1.0, 0.6)
    if selected_run_ids:
        detail = _fetch_run_detail(selected_run_ids[0])
        if detail and detail.get("samples_detail"):
            low_samples = [
                s
                for s in detail["samples_detail"]
                if s["metrics"].get("faithfulness", 1.0) < threshold
            ]
            if not low_samples:
                st.success(f"所有样本 faithfulness >= {threshold}")
            for sample in low_samples:
                with st.expander(
                    f"[faithfulness={sample['metrics'].get('faithfulness', 0):.2%}] {sample['question'][:60]}..."
                ):
                    st.write("**可能问题**: 该样本的答案可信度较低，请检查检索结果和生成质量。")
                    st.json(sample["metrics"])
                    if sample["details"].get("error"):
                        st.error(f"Pipeline error: {sample['details']['error']}")
        else:
            st.info("暂无数据。")
