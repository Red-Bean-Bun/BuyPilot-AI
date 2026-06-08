# TODO — Cloudflare 部署与性能优化

> 记录时间：2026-05-29
> 状态：已完成（投机检索已交接给队友）


## 1. 性能诊断与优化

### 容器内实测（优化前）
```
0.04s ─── thinking (understanding)
0.86s ─── 意图识别完成 (qwen-turbo, 0.82s) ✅
0.86s ─── 开始生成购买标准
3.96s ─── criteria 完成 (qwen-plus, 3.10s) ⚠️
4.44s ─── 检索完成 (embedding+rerank, 0.48s) ✅
4.44s ─── 开始生成最终决策
8.47s ─── decision 完成 (qwen-plus, 4.03s) ⚠️
8.49s ─── done
总计: 8.5s
```

### 根因
- **decision (qwen-plus)**：4.03s，纯文本生成，不需要 qwen-plus 的结构化输出能力
- **criteria (qwen-plus)**：3.10s，结构化 JSON 但 prompt 约束已足够严格（148 行 + 4 品类示例）
- **Doubao API key 未配置**：所有 Doubao fallback 都无效，但不影响当前性能（qwen-turbo 已够快）

### 改动
`backend/src/config/settings.py` TASK_MODEL_MAP：
```python
# 改前
"generate_criteria": {"primary": "qwen_plus", ...},
"generate_recommendation": {"primary": "qwen_plus", ...},
"generate_decision": {"primary": "qwen_plus", ...},

# 改后
"generate_criteria": {"primary": "qwen_turbo", ...},
"generate_recommendation": {"primary": "qwen_turbo", ...},
"generate_decision": {"primary": "qwen_turbo", ...},
```

### 优化结果
| 版本 | 端到端 | understanding | criteria | retrieval | decision |
|------|--------|--------------|----------|-----------|----------|
| 原始 (qwen-plus) | **8.5s** | 0.82s | 3.49s | 0.48s | 4.03s |
| 全部 → qwen-turbo | **3.96s** | 0.81s | 1.18s | 0.57s | 1.14s |

**提速 53%，结构化 JSON 输出质量验证通过（chips/constraints/summary 完整正确）。**

## 2. 已交接（投机检索）

criteria 和 retrieval 目前是串行的（handlers.py:269→321），理论上可以并行省 ~0.5s。已交接给队友，不在本次范围内。

## 5. 遗留注意项

- `.env` 里缺少 `DOUBAO_BASE_URL` / `DOUBAO_API_KEY` / `DOUBAO_INTENT_MODEL`，所有 Doubao 调用走 fallback 失败。如果需要双轨策略生效，需要补充这些变量。 也可测一测 Doubao 模型的速度
