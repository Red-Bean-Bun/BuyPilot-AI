"""Audit domain terms against the official ecommerce dataset.

The script is intentionally read-only for production rules. It scans raw JSON
and generates a report with coverage gaps and candidate updates for human review.

Run from backend/:
    .venv/bin/python -m src.scripts.audit_domain_terms --output ../doc/domain_terms_audit.md
"""

from __future__ import annotations

import argparse
import json
import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

from src.config.domain_terms import (
    SCENARIO_VALUES,
    WARNING_MARKERS,
    infer_category_from_product_type,
    is_supported_product_type,
    normalize_category,
    normalize_product_type,
)

PROJECT_ROOT = Path(__file__).resolve().parents[3]
DEFAULT_DATASET_DIR = PROJECT_ROOT / "data" / "raw" / "ecommerce_agent_dataset"

WARNING_CANDIDATE_TERMS = (
    "泛红",
    "刺痛",
    "过敏",
    "闷痘",
    "紧绷",
    "干燥",
    "拔干",
    "痒",
    "红肿",
    "卡顿",
    "死机",
    "发热",
    "耗电",
    "断连",
    "延迟",
    "磨脚",
    "夹脚",
    "起球",
    "掉色",
    "偏码",
    "太甜",
    "太咸",
    "漏液",
    "破损",
    "结块",
    "发霉",
)

SCENARIO_CANDIDATE_TERMS = (
    "日常",
    "通勤",
    "户外",
    "运动",
    "跑步",
    "训练",
    "旅行",
    "露营",
    "送礼",
    "办公",
    "早餐",
    "健身",
    "熬夜",
    "加班",
    "宿舍",
    "学生",
    "儿童",
    "孩子",
    "老人",
    "下午茶",
    "深夜",
    "宅家",
    "应急",
    "敏感肌",
    "油皮",
)


def build_audit(dataset_dir: Path = DEFAULT_DATASET_DIR) -> dict[str, Any]:
    products = _load_products(dataset_dir)
    product_count = len(products)
    brands = sorted({str(raw.get("brand") or "").strip() for raw in products if raw.get("brand")})

    category_rows = _category_mappings(products)
    sub_category_rows = _sub_category_coverage(products)
    sku_property_rows = _sku_property_keys(products)
    warning_rows = _term_candidates(products, WARNING_CANDIDATE_TERMS, low_rating_only=True)
    scenario_rows = _term_candidates(products, SCENARIO_CANDIDATE_TERMS, low_rating_only=False)

    uncovered = [row for row in sub_category_rows if not row["supported"]]
    warning_updates = [row["term"] for row in warning_rows if not row["current_marker"] and row["count"] >= 2]
    scenario_updates = [
        {"term": row["term"], "suggested_value": row["term"]}
        for row in scenario_rows
        if not row["current_mapping"] and row["count"] >= 2
    ]

    return {
        "dataset_dir": _display_path(dataset_dir),
        "product_count": product_count,
        "brand_count": len(brands),
        "brands": brands,
        "category_mappings": category_rows,
        "sub_category_coverage": sub_category_rows,
        "uncovered_sub_categories": uncovered,
        "sku_property_keys": sku_property_rows,
        "warning_candidates": warning_rows,
        "scenario_candidates": scenario_rows,
        "suggested_updates": {
            "product_type_aliases": [_product_type_suggestion(row) for row in uncovered],
            "warning_markers": warning_updates,
            "scenario_values": scenario_updates,
        },
    }


def render_markdown(report: dict[str, Any]) -> str:
    lines = [
        "# Domain Terms Audit",
        "",
        "该报告由 `python -m src.scripts.audit_domain_terms` 生成，只提供审计和建议，不自动修改生产规则。",
        "",
        "## Summary",
        "",
        f"- Dataset: `{report['dataset_dir']}`",
        f"- Products: {report['product_count']}",
        f"- Brands: {report['brand_count']}",
        f"- Sub-category coverage gaps: {len(report['uncovered_sub_categories'])}",
        "",
        "## Category Mapping",
        "",
        _markdown_table(
            ["raw_category", "canonical_category", "count", "sub_categories"],
            [
                [
                    row["raw_category"],
                    row["canonical_category"],
                    row["count"],
                    ", ".join(row["sub_categories"]),
                ]
                for row in report["category_mappings"]
            ],
        ),
        "",
        "## Sub-category Coverage",
        "",
        _markdown_table(
            ["sub_category", "normalized", "supported", "inferred_category", "count", "raw_categories"],
            [
                [
                    row["sub_category"],
                    row["normalized_product_type"],
                    "yes" if row["supported"] else "no",
                    row["inferred_category"] or "",
                    row["count"],
                    ", ".join(row["raw_categories"]),
                ]
                for row in report["sub_category_coverage"]
            ],
        ),
        "",
        "## Brand Terms",
        "",
        ", ".join(report["brands"]) if report["brands"] else "无",
        "",
        "## SKU Property Keys",
        "",
        _markdown_table(
            ["key", "count", "sample_values"],
            [[row["key"], row["count"], ", ".join(row["sample_values"])] for row in report["sku_property_keys"]],
        ),
        "",
        "## Warning Candidates",
        "",
        _markdown_table(
            ["term", "count", "current_marker", "categories", "sample_products"],
            [
                [
                    row["term"],
                    row["count"],
                    "yes" if row["current_marker"] else "no",
                    ", ".join(row["categories"]),
                    ", ".join(row["sample_product_ids"]),
                ]
                for row in report["warning_candidates"][:30]
            ],
        ),
        "",
        "## Scenario Candidates",
        "",
        _markdown_table(
            ["term", "count", "current_mapping", "categories", "sample_products"],
            [
                [
                    row["term"],
                    row["count"],
                    row["current_mapping"] or "",
                    ", ".join(row["categories"]),
                    ", ".join(row["sample_product_ids"]),
                ]
                for row in report["scenario_candidates"][:30]
            ],
        ),
        "",
        "## Suggested Updates",
        "",
        "### PRODUCT_TYPE_ALIASES gaps",
        "",
        "无" if not report["suggested_updates"]["product_type_aliases"] else "",
    ]
    for item in report["suggested_updates"]["product_type_aliases"]:
        lines.append(f"- `{item['canonical']}`: aliases={item['aliases']}, category={item['category']}")

    lines.extend(
        [
            "",
            "### WARNING_MARKERS candidates",
            "",
            ", ".join(report["suggested_updates"]["warning_markers"]) or "无",
            "",
            "### SCENARIO_VALUES candidates",
            "",
        ]
    )
    scenario_values = report["suggested_updates"]["scenario_values"]
    if not scenario_values:
        lines.append("无")
    else:
        for item in scenario_values:
            lines.append(f"- `{item['term']}` -> `{item['suggested_value']}`")
    lines.append("")
    return "\n".join(lines)


def _load_products(dataset_dir: Path) -> list[dict[str, Any]]:
    if not dataset_dir.exists():
        raise FileNotFoundError(f"Dataset directory not found: {dataset_dir}")
    products: list[dict[str, Any]] = []
    for path in sorted(dataset_dir.glob("*/data/*.json")):
        with path.open(encoding="utf-8") as f:
            raw = json.load(f)
        raw["_source_file"] = str(path.relative_to(dataset_dir))
        products.append(raw)
    return products


def _display_path(path: Path) -> str:
    resolved = path.resolve()
    try:
        return str(resolved.relative_to(PROJECT_ROOT.resolve()))
    except ValueError:
        return str(resolved)


def _category_mappings(products: list[dict[str, Any]]) -> list[dict[str, Any]]:
    grouped: dict[tuple[str, str], dict[str, Any]] = {}
    for raw in products:
        raw_category = str(raw.get("category") or "")
        canonical = normalize_category(raw_category) or raw_category
        key = (raw_category, canonical)
        row = grouped.setdefault(
            key,
            {
                "raw_category": raw_category,
                "canonical_category": canonical,
                "count": 0,
                "sub_categories": set(),
            },
        )
        row["count"] += 1
        if raw.get("sub_category"):
            row["sub_categories"].add(str(raw["sub_category"]))
    return [
        {**row, "sub_categories": sorted(row["sub_categories"])}
        for row in sorted(grouped.values(), key=lambda item: (item["canonical_category"], item["raw_category"]))
    ]


def _sub_category_coverage(products: list[dict[str, Any]]) -> list[dict[str, Any]]:
    grouped: dict[str, dict[str, Any]] = {}
    for raw in products:
        sub_category = str(raw.get("sub_category") or "").strip()
        if not sub_category:
            continue
        row = grouped.setdefault(
            sub_category,
            {
                "sub_category": sub_category,
                "count": 0,
                "raw_categories": set(),
                "product_ids": [],
            },
        )
        row["count"] += 1
        row["raw_categories"].add(str(raw.get("category") or ""))
        row["product_ids"].append(str(raw.get("product_id") or ""))

    rows: list[dict[str, Any]] = []
    for row in grouped.values():
        normalized = normalize_product_type(row["sub_category"])
        rows.append(
            {
                "sub_category": row["sub_category"],
                "normalized_product_type": normalized,
                "supported": is_supported_product_type(row["sub_category"]),
                "inferred_category": infer_category_from_product_type(row["sub_category"]),
                "count": row["count"],
                "raw_categories": sorted(row["raw_categories"]),
                "sample_product_ids": sorted(row["product_ids"])[:5],
            }
        )
    return sorted(rows, key=lambda item: (not item["supported"], item["sub_category"]))


def _sku_property_keys(products: list[dict[str, Any]]) -> list[dict[str, Any]]:
    counts: Counter[str] = Counter()
    values: dict[str, Counter[str]] = defaultdict(Counter)
    for raw in products:
        for sku in raw.get("skus") or []:
            properties = sku.get("properties") if isinstance(sku, dict) else None
            if not isinstance(properties, dict):
                continue
            for key, value in properties.items():
                key_str = str(key)
                counts[key_str] += 1
                values[key_str][str(value)] += 1
    return [
        {
            "key": key,
            "count": count,
            "sample_values": [value for value, _ in values[key].most_common(5)],
        }
        for key, count in counts.most_common()
    ]


def _term_candidates(
    products: list[dict[str, Any]],
    terms: tuple[str, ...],
    *,
    low_rating_only: bool,
) -> list[dict[str, Any]]:
    counts: Counter[str] = Counter()
    product_ids: dict[str, set[str]] = defaultdict(set)
    categories: dict[str, set[str]] = defaultdict(set)
    for raw in products:
        texts = _low_rating_review_texts(raw) if low_rating_only else [_rag_text(raw)]
        category = normalize_category(raw.get("category")) or str(raw.get("category") or "")
        product_id = str(raw.get("product_id") or "")
        for text in texts:
            for term in terms:
                occurrences = text.count(term)
                if occurrences <= 0:
                    continue
                counts[term] += occurrences
                product_ids[term].add(product_id)
                categories[term].add(category)

    marker_set = set(WARNING_MARKERS)
    return [
        {
            "term": term,
            "count": count,
            "current_marker": term in marker_set,
            "current_mapping": SCENARIO_VALUES.get(term),
            "categories": sorted(categories[term]),
            "sample_product_ids": sorted(product_ids[term])[:5],
        }
        for term, count in counts.most_common()
    ]


def _low_rating_review_texts(raw: dict[str, Any]) -> list[str]:
    result: list[str] = []
    for review in (raw.get("rag_knowledge") or {}).get("user_reviews") or []:
        rating = review.get("rating")
        if isinstance(rating, int | float) and rating <= 3:
            result.append(str(review.get("content") or ""))
    return result


def _rag_text(raw: dict[str, Any]) -> str:
    rag = raw.get("rag_knowledge") or {}
    parts: list[str] = [str(rag.get("marketing_description") or "")]
    for faq in rag.get("official_faq") or []:
        parts.append(str(faq.get("question") or ""))
        parts.append(str(faq.get("answer") or ""))
    for review in rag.get("user_reviews") or []:
        parts.append(str(review.get("content") or ""))
    return " ".join(parts)


def _product_type_suggestion(row: dict[str, Any]) -> dict[str, Any]:
    raw_categories = row.get("raw_categories") or []
    category = normalize_category(raw_categories[0]) if raw_categories else None
    return {
        "canonical": row["normalized_product_type"] or row["sub_category"],
        "aliases": [row["sub_category"]],
        "category": category or "",
        "sample_product_ids": row.get("sample_product_ids", []),
    }


def _markdown_table(headers: list[str], rows: list[list[Any]]) -> str:
    if not rows:
        return "无"
    header = "| " + " | ".join(headers) + " |"
    divider = "| " + " | ".join("---" for _ in headers) + " |"
    body = ["| " + " | ".join(_cell(value) for value in row) + " |" for row in rows]
    return "\n".join([header, divider, *body])


def _cell(value: Any) -> str:
    return str(value).replace("|", "\\|")


def main() -> None:
    parser = argparse.ArgumentParser(description="Audit domain terms against the raw product dataset.")
    parser.add_argument("--dataset-dir", type=Path, default=DEFAULT_DATASET_DIR)
    parser.add_argument("--output", type=Path, help="Write report to this path. Defaults to stdout.")
    parser.add_argument("--format", choices=("markdown", "json"), default="markdown")
    parser.add_argument(
        "--fail-on-missing-sub-category",
        action="store_true",
        help="Exit non-zero when raw sub_category values are not covered by PRODUCT_TYPE_ALIASES.",
    )
    args = parser.parse_args()

    report = build_audit(args.dataset_dir)
    content = (
        json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True)
        if args.format == "json"
        else render_markdown(report)
    )
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(content, encoding="utf-8")
    else:
        print(content)

    if args.fail_on_missing_sub_category and report["uncovered_sub_categories"]:
        sys.exit(1)


if __name__ == "__main__":
    main()
