# products

## 服务对象

`products` 是商品主表，服务推荐检索、硬过滤、商品卡片渲染和购物车引用。它承载官方脱敏电商数据中每个商品的稳定事实，是防止 LLM 编造商品、价格、品类和图片的第一层约束。

## 为什么这样设计

- 用 `id` 直接保存官方 `product_id`，方便从原始 JSON、chunk、前端卡片和购物车之间稳定关联。
- 跨品类差异放在 `metadata` JSON 中，不为每个品类建专属列。美妆有肤质/成分，数码有存储/屏幕，服饰有运动场景，食品有口味/饮食标签，用 JSON 可以避免大量空列。
- `image_urls` 保留原始本地图片路径；运行时再转换为 `/assets/products/...` 的公开 URL，避免数据库绑定部署域名。

## 字段说明

| 字段 | 类型 | 含义和作用 | 设计原因 |
| --- | --- | --- | --- |
| `id` | `VARCHAR`, PK | 商品唯一 ID，例如 `p_beauty_001`。 | 和官方数据、chunk、推荐结果、购物车保持同一个键。 |
| `name` | `VARCHAR`, not null | 商品标题/名称。 | 前端商品卡片展示和检索文本的核心身份信息。 |
| `category` | `VARCHAR`, not null | 一级品类，例如 `美妆护肤`、`数码电子`、`服饰运动`、`食品饮料`。 | 用于硬过滤、澄清和评测分类。 |
| `sub_category` | `VARCHAR`, nullable | 二级品类，例如 `精华`、`防晒`、`智能手机`。 | 支撑更细粒度推荐和解释，但不是所有数据都必须有。 |
| `price` | `FLOAT`, nullable | 基础价格，单位按商品数据理解为人民币。 | 支撑预算硬过滤和卡片价格展示。 |
| `brand` | `VARCHAR`, nullable | 品牌名。 | 支撑品牌偏好、品牌问答和商品卡片展示。 |
| `image_urls` | `JSON`, nullable | 图片路径列表，当前保存原始相对路径。 | 商品可能多图，JSON list 比单列更有扩展性。 |
| `product_url` | `VARCHAR`, nullable | 外部商品详情页 URL。当前 dev.db 多为空。 | 为将来接真实电商详情页预留，不影响当前 demo。 |
| `metadata` | `JSON`, nullable | 商品扩展事实：`source_file`、`skus`、`rag_knowledge`、`knowledge_package`。 | 承载品类属性、SKU、FAQ、评论和导购知识包，避免扩表。 |

## `metadata` 结构

当前入库由 `backend/src/services/product_ingest.py` 生成，主要包含：

| key | 含义 |
| --- | --- |
| `source_file` | 原始商品 JSON 在 `data/raw/ecommerce_agent_dataset` 下的相对路径。 |
| `skus` | 官方数据中的规格列表，用于后续 SKU 选择或价格扩展。 |
| `rag_knowledge` | 原始 `marketing_description`、`official_faq`、`user_reviews`。 |
| `knowledge_package` | 由 `chunking.py` 派生的导购知识包，包含基础信息、属性、别名、证据摘要和风险摘要。 |

## 关系和索引

- 主键：`products.id`。
- 被 `product_chunks.product_id`、`cart_items.product_id`、`evidence_links.product_id` 外键引用。
- 当前没有单独为 `category/price/brand` 建索引，因为 dev 数据只有 100 条；生产规模扩大后可补 `category`、`price`、`brand` 组合索引。

## Review 关注点

- 当前 `metadata` 同时保存原始 `rag_knowledge` 和派生 `knowledge_package`，空间换取可解释性和可回溯性，适合 demo 和评审。
- 如果未来数据量扩大，`metadata.rag_knowledge` 可拆到文档表，`products` 只保留结构化摘要。
