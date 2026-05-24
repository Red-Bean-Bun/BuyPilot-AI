# product_chunks

## 服务对象

`product_chunks` 是 RAG 检索文档表。每个商品被拆成 profile、marketing、FAQ、评论、warning、compare 等语义 chunk，用于向量召回、证据展示和风险提示。

## 为什么这样设计

- 商品主表保存“商品事实”，chunk 表保存“可检索证据”，职责分离。
- `chunk_type` 和 `retrieval_role` 放在 `metadata` 中，便于检索时排除风险 chunk、证据展示时区分卖点/FAQ/风险。
- `id` 使用 `{product_id}:{chunk_index}`，让重复入库时天然幂等，便于删除重建单个商品的 chunks。

## 字段说明

| 字段 | 类型 | 含义和作用 | 设计原因 |
| --- | --- | --- | --- |
| `id` | `VARCHAR`, PK | chunk 唯一 ID，例如 `p_beauty_001:0`。 | 由商品 ID 和序号组成，稳定、可回溯。 |
| `product_id` | `VARCHAR`, not null, FK | 所属商品 ID。 | 召回 chunk 后能回到商品卡片。 |
| `chunk_text` | `VARCHAR`, not null | 实际参与 embedding 和证据展示的文本。 | RAG 的最小文本单元。 |
| `chunk_index` | `INTEGER`, not null | 该商品内的 chunk 顺序。 | 保留原始拆分顺序，便于调试和重建。 |
| `embedding` | `JSON`, nullable | SQLite 开发库中的向量数组。生产 PostgreSQL 中设计为 `vector(1024)`。 | 支撑语义召回；SQLite 使用 JSON 是本地开发兼容方案。 |
| `metadata` | `JSON`, nullable | chunk 属性，包括 `chunk_type`、`retrieval_role`、`evidence_kind`、品类、价格等。 | 让检索和证据逻辑按 chunk 语义处理，而不是只看文本。 |

## `metadata` 结构

| key | 含义 |
| --- | --- |
| `source` | 数据来源，当前为 `ecommerce_agent_dataset`。 |
| `category` / `sub_category` / `brand` / `price` | 冗余商品基础字段，方便检索阶段使用。 |
| `chunk_type` | chunk 类型：`profile`、`marketing`、`faq`、`positive_review`、`negative_review`、`warning`、`compare`。 |
| `retrieval_role` | 检索角色：`primary`、`evidence`、`risk`。向量检索会排除 `risk`。 |
| `evidence_kind` | 证据用途：`profile`、`why_buy`、`faq`、`risk`、`compare`。 |
| `section_index` | 在 FAQ、评论或营销描述中的原始序号。 |
| `question` | FAQ chunk 的问题文本。 |
| `rating` / `nickname` | 评论 chunk 的评分和昵称。 |

## 关系和索引

- 外键：`product_id -> products.id`。
- 索引：`ix_product_chunks_product_id`。
- PostgreSQL 目标环境会额外创建 `idx_product_chunks_embedding_hnsw`，用于 `embedding vector_cosine_ops`。

## Review 关注点

- 当前 dev.db 里 embedding 样例为 16 维 deterministic fallback；生产目标是 1024 维模型 embedding。
- `risk` chunk 不参与普通向量召回，是为了避免“过敏、差评、风险词”被当成正向卖点召回；但它们仍可作为风险证据使用。
