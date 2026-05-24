# evidence_links

## 服务对象

`evidence_links` 保存推荐商品和证据 chunk 的绑定关系，用于证明商品卡片、推荐理由和最终决策来自真实商品数据，而不是 LLM 编造。

## 为什么这样设计

- 证据绑定独立成表，能按 conversation 或 product 下钻，也能计算 evidence coverage。
- `chunk_id` 优先绑定真实 `product_chunks.id`。如果某些兜底证据仍无法映射到真实 chunk，会保留 `source_id_raw` 和 `snippet` 快照，避免证据上下文丢失。
- `cited_in` 标记证据出现在哪类 UI 或回答中，当前主要是 `product_card`。

## 字段说明

| 字段 | 类型 | 含义和作用 | 设计原因 |
| --- | --- | --- | --- |
| `id` | `VARCHAR`, PK | 证据链接 ID。 | 独立标识一次引用。 |
| `conversation_id` | `VARCHAR`, nullable, FK | 对应推荐轮次。 | 能从一次对话下钻到所有证据。允许为空以兼容旧数据或 fallback。 |
| `product_id` | `VARCHAR`, not null, FK | 被证明的商品 ID。 | 证据必须绑定真实商品。 |
| `chunk_id` | `VARCHAR`, nullable, FK | 引用的 chunk ID。 | 当证据来自具体 chunk 时可回溯原文；fallback 证据可为空。 |
| `source_id_raw` | `VARCHAR`, nullable | 原始 evidence source_id。 | 即使 source_id 不是可外键引用的 chunk，也保留原始来源标识。 |
| `snippet` | `VARCHAR`, nullable | 引用证据文本快照。 | 避免后续 chunk 重建或 fallback 证据无法回溯时丢失 review 上下文。 |
| `evidence_type` | `VARCHAR`, nullable | 证据来源类型，当前常见为 `product_chunk`。 | 为未来支持评论、FAQ、图片、外部文档等证据类型预留。 |
| `relevance_score` | `FLOAT`, nullable | 证据相关性分数。当前未系统写入。 | 为后续 rerank/证据排序保留。 |
| `cited_in` | `VARCHAR`, nullable | 证据被引用的位置，例如 `product_card`。 | 区分商品卡片、最终决策、解释文本等不同引用点。 |

## 关系和索引

- 外键：`conversation_id -> conversations.id`。
- 外键：`product_id -> products.id`。
- 外键：`chunk_id -> product_chunks.id`。
- 当前只有主键索引；数据量扩大后建议加 `conversation_id` 和 `product_id` 索引。

## Review 关注点

- 旧记录的 `source_id_raw/snippet` 为空是迁移前历史数据；新推荐写入会带上这两个字段。
- 新证据会优先绑定真实 chunk；如果 `chunk_id` 仍为空，review 时应看 `source_id_raw` 和 `snippet` 判断是否为兜底证据。
