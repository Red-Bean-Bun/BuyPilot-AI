# system_metadata

## 服务对象

`system_metadata` 保存数据库级轻量版本信息。当前主要记录 `dataset_index`，用于说明 `products/product_chunks` 是由哪一版 raw 数据、chunking 策略和 embedding 模型生成的。

## 为什么这样设计

- 商品事实来自 raw 数据 seed，chunk embedding 又依赖模型和 chunking 代码。没有版本记录时，无法判断 DB 索引是否和当前 raw 数据一致。
- 只需要少量全局键值，不值得引入复杂 migration 系统；JSON value 足以承载版本诊断。
- 该表不参与在线推荐路径，只服务启动诊断、审计和答辩解释。

## 字段说明

| 字段 | 类型 | 含义和作用 | 设计原因 |
| --- | --- | --- | --- |
| `key` | `VARCHAR`, PK | 元数据键。当前主要为 `dataset_index`。 | 支持后续增加 `schema_version`、`runtime_config` 等键。 |
| `value` | `JSON`, not null | 元数据内容。 | 不同 key 的结构可能不同，JSON 更适合。 |
| `updated_at` | `DATETIME`, not null | 最近更新时间。 | 判断索引何时生成。 |

## `dataset_index` 结构

| key | 含义 |
| --- | --- |
| `dataset_version` | 数据集版本，当前为 `ecommerce_agent_dataset:v1`。 |
| `dataset_hash` | 所有 raw 商品 source hash 的聚合 SHA-256。 |
| `chunking_version` | 当前 chunking 策略版本，当前为 `semantic_v1`。 |
| `embedding_model` | 当前索引使用的 embedding 模型，例如 `text-embedding-v3`。 |
| `embedding_dimensions` | 目标 embedding 维度，当前为 `1024`。 |
| `product_count` | 本次 seed 的商品数量。 |
| `chunk_count` | 本次 seed 的 chunk 数量。 |
| `indexed_at` | 本次 seed/reindex 时间。 |

## 关系和索引

- 主键：`system_metadata.key`。
- 不建立外键；这是数据库全局诊断表，不绑定某个业务对象。

## Review 关注点

- `system_metadata` 不是完整迁移系统，只记录当前索引版本事实。
- 如果未来引入 Alembic，可把 schema migration 版本也写入此表或改用 Alembic 自带版本表。
