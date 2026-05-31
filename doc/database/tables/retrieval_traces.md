# retrieval_traces

## 服务对象

`retrieval_traces` 保存每次推荐的检索决策链路，是“可解释推荐”和“无幻觉防御”的核心表。它让人工 review 能看到本轮用了哪些约束、候选、rerank 结果、最终商品、阶段耗时和 fallback。

## 为什么这样设计

- RAG 系统出错时，单看最终回答不够，需要知道检索和过滤过程。
- 把 trace 独立于 `conversations` 保存，可以让推荐历史和技术诊断分离。
- `filters_applied`、`vector_top_k`、`rerank_top_n` 用 JSON 保存，适配检索策略快速演进。

## 字段说明

| 字段 | 类型 | 含义和作用 | 设计原因 |
| --- | --- | --- | --- |
| `id` | `VARCHAR`, PK | trace ID。 | 唯一定位一次检索链路。 |
| `conversation_id` | `VARCHAR`, nullable, FK | 对应的推荐轮次 ID。 | 将检索 trace 和用户问题、criteria、商品列表关联。允许为空以兼容旧数据或持久化失败 fallback。 |
| `criteria_id` | `VARCHAR`, nullable | 本轮购买标准 ID。 | 关联 criteria card，便于前端/日志定位。 |
| `filters_applied` | `JSON`, nullable | 应用的约束和诊断信息。通常包含 criteria constraints、`sql_filters_applied`、`pre_filter_count/post_filter_count`，并可能包含 `_stage_timings_ms`、`_fallbacks`。 | 记录“为什么这些商品能进候选”，同时承载运行时观测信息。 |
| `vector_top_k` | `JSON`, nullable | 向量召回阶段的 top chunk/product 明细，包含 `product_id`、`chunk_id`、`vector_score`、`filter_score`、`chunk_type` 等。 | 解释候选从哪个真实 chunk 召回。 |
| `rerank_top_n` | `JSON`, nullable | rerank 后的排序结果，包含最终 rank 及对应 chunk/product 分数。 | 对比召回和最终排序，定位 rerank 问题。 |
| `selected_ids` | `JSON`, nullable | 最终推荐商品 ID 列表。 | 快速查看推荐结果，也服务 observability fallback 展示。 |
| `hit_count` | `INTEGER`, not null | 最终命中/推荐数量。 | 评估检索是否有足够候选。 |
| `vector_count` | `INTEGER`, not null | 向量或候选阶段数量。 | 评估召回规模和过滤损耗。 |
| `created_at` | `DATETIME`, not null | 创建时间。 | 查看最近 trace 和按时间分析。 |

## 关系和索引

- 外键：`conversation_id -> conversations.id`。
- 索引：`idx_retrieval_traces_conversation_id`、`idx_retrieval_traces_created_at`。

## Review 关注点

- `filters_applied.sql_filters_applied` 记录 pgvector SQL 阶段已下推的结构化条件；Python hard filter 仍是二次防线。
- `filters_applied` 同时包含业务约束和 `_stage_timings_ms/_fallbacks`，适合快速调试；如果后续做分析报表，可拆出专门的性能表。
- 旧记录里的 `vector_top_k` 可能仍是商品级简化结构；新记录会写入 chunk 级明细。
