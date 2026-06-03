# BuyPilot-AI 商品详情接口 PRD

**版本**：v1.0
**日期**：2026-06-03
**状态**：待实现
**优先级**：P1（效果与可靠性 20% + 加分项 4.4 ⭐⭐⭐ 端侧体验打磨）

---

## 1. 背景与目标

### 1.1 问题

当前 Android 商品详情页（`ProductHeroDetailScreen`）没有独立 API，所有数据来自 SSE `product_card` 事件中的 `ProductPayload`。详情页只展示：

- 商品大图 + 名称/价格/品牌
- 标签行（肤质/成分/场景）
- AI 推荐理由（`reason` 字段）
- 风险提示（`risk_notes`）

**原始数据中丰富的商品信息完全未送到前端：**

| 数据 | 来源 | 当前状态 |
|------|------|---------|
| `marketing_description` | `rag_knowledge.marketing_description` | ❌ 未送前端 |
| `official_faq` (3条) | `rag_knowledge.official_faq[]` | ❌ 未送前端 |
| `user_reviews` (5条) | `rag_knowledge.user_reviews[]` | ❌ 未送前端 |
| `sku_options` (多规格) | `skus[]` | ⚠️ 已在 payload 但未渲染 |

### 1.2 目标

1. **新增 REST 接口** `GET /products/{product_id}`，返回完整商品详情
2. **Android 投机预取**：收到 SSE `product_card` 事件时立即预取，用户点击时零延迟
3. **详情页内容丰富化**：新增 SKU 选择器、商品亮点、营销描述、FAQ、用户评价 5 个模块
4. **比赛价值**：满足 3.4"商品卡片支持跳转落地页" + 4.4 ⭐⭐⭐"接近商业产品体验"

### 1.3 不做的事

- 不修改 SSE 事件协议（铁律 1：SSE 事件封闭性）
- 不新增 SSE 事件类型
- 不做商品详情页的独立路由页（复用现有 `ProductHeroDetailScreen`）
- 不做商品对比功能（P1 范围外）

---

## 2. 接口契约

### 2.1 端点定义

```
GET /products/{product_id}
```

**用途**：获取单个商品的完整详情，用于详情页渲染。

**认证**：与其他公开 API 一致，受 `ADMIN_API_KEY` 保护（如已配置）。

**缓存**：响应头建议 `Cache-Control: public, max-age=3600`，商品数据在会话期间不变。

### 2.2 请求参数

| 参数 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `product_id` | path | `string` | ✅ | 商品 ID，如 `p_beauty_001` |

### 2.3 成功响应（200 OK）

**Content-Type**: `application/json`

```json
{
  "product": {
    "product_id": "p_beauty_001",
    "name": "雅诗兰黛特润修护肌活精华露淡纹紧致保湿夜间修护抗初老精华30ml",
    "price": 720.0,
    "currency": "CNY",
    "image_url": "/assets/products/1_美妆护肤/images/p_beauty_001_live.jpg",
    "category": "美妆护肤",
    "sub_category": "精华",
    "brand": "雅诗兰黛",
    "skin_type_match": ["干性", "中性"],
    "ingredient_tags": ["二裂酵母", "透明质酸"],
    "ingredient_avoid": [],
    "use_scenario": "夜间护肤",
    "sku_options": [
      {"sku_id": "s_p_beauty_001_1", "properties": {"容量": "30ml 经典装"}, "price": 720.0},
      {"sku_id": "s_p_beauty_001_2", "properties": {"容量": "50ml 加大装"}, "price": 980.0},
      {"sku_id": "s_p_beauty_001_3", "properties": {"容量": "75ml 家用装"}, "price": 1260.0}
    ]
  },
  "marketing_description": "雅诗兰黛特润修护肌活精华露（小棕瓶）是品牌经典抗初老单品，主打夜间肌底修护。核心成分含高浓度二裂酵母发酵产物溶胞物，能深入修护日间紫外线、污染造成的损伤，促进肌肤代谢；搭配透明质酸锁水保湿，猴面包树籽提取物淡纹紧致。适合25+有干纹细纹、熬夜后暗沉的抗初老人群，夜间护肤时使用效果更佳。建议每晚洁面爽肤后，取3-4滴掌心温热，轻按面部至吸收，后续搭配面霜锁养。注意开封后6个月内用完，敏感肌先做耳后测试，避免不适。",
  "highlights": [
    "核心成分二裂酵母发酵产物溶胞物",
    "夜间修护，促进肌肤代谢",
    "透明质酸锁水保湿",
    "适合25+抗初老人群",
    "建议开封后6个月内用完"
  ],
  "faqs": [
    {
      "question": "这款精华的核心成分二裂酵母发酵产物溶胞物有什么作用？",
      "answer": "二裂酵母发酵产物溶胞物是这款精华的核心修护成分，它能模拟皮肤微生态，帮助修护日间紫外线、污染等外界刺激造成的肌底损伤，促进肌肤新陈代谢，增强皮肤屏障功能。同时，它还能协同其他保湿和抗初老成分，提升肌肤对营养的吸收效率，长期使用可改善皮肤稳定性、细腻度，减少干纹细纹的产生。"
    },
    {
      "question": "不同规格的小棕瓶怎么选？30ml、50ml、75ml各适合什么情况？",
      "answer": "不同规格的选择可根据使用需求和频率：30ml经典装适合初次尝试或短期出差携带，小巧便携；50ml加大装性价比更高，单毫升价格比30ml更划算，适合日常长期使用的用户；75ml家用装容量大更实惠，适合对产品效果认可的老用户囤货，或家庭多人共用，能满足较长时间的使用需求。"
    },
    {
      "question": "这款精华适合敏感肌吗？使用前需要注意什么？",
      "answer": "这款精华大部分肤质适用，但敏感肌需谨慎。因含活性修护成分，建议敏感肌使用前取少量精华涂抹于耳后或手臂内侧，观察24小时无红肿、刺痛等不适再使用。此外，开封后建议6个月内用完，避免成分失活影响效果；存放时需远离阳光直射和高温环境，保持成分活性。"
    }
  ],
  "reviews": [
    {"nickname": "李小米", "rating": 1, "content": "用了两次就脸颊泛红刺痛，我是敏感肌平时用其他精华都没事，这款成分可能太刺激了。虽然包装是正品，但真的不适合敏感肌，早知道先做测试，现在只能闲置，浪费720块，太心疼了。"},
    {"nickname": "王梓涵", "rating": 2, "content": "用了快一个月，保湿还行，但淡纹紧致完全没效果。我26岁有轻微干纹，每晚都用，干纹还是在，720块30ml性价比太低，不会再回购了。"},
    {"nickname": "张雅静", "rating": 5, "content": "熬夜党救星！每晚3滴吸收超快不黏腻，第二天皮肤不暗沉。半个月后眼角干纹淡了，皮肤也紧致些，已经回购50ml加大装，太爱这款精华了！"},
    {"nickname": "刘梦琪", "rating": 2, "content": "包装设计有问题，瓶口总残留精华，倒的时候会滴外面浪费。用了三周除了保湿没别的效果，抗初老根本没感觉，价格还贵，有点失望。"},
    {"nickname": "陈宇飞", "rating": 1, "content": "混油皮用了反而更干，还冒闭口。想抗初老结果皮肤状态更差，这款可能不适合混油皮，后悔买了，以后不会再试雅诗兰黛小棕瓶了。"}
  ]
}
```

### 2.4 响应字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `product` | `ProductPayload` | ✅ | 商品基础信息（与 SSE `product_card` 中的 `product` 字段结构一致） |
| `marketing_description` | `string \| null` | ✅ | 品牌营销描述原文，来自 `rag_knowledge.marketing_description` |
| `highlights` | `string[]` | ✅ | 3-5 条预计算卖点，每条不超过 20 字。来自 `Product.metadata["highlights"]` |
| `faqs` | `FaqItem[]` | ✅ | 官方 FAQ 列表，来自 `rag_knowledge.official_faq` |
| `reviews` | `ReviewItem[]` | ✅ | 用户评价列表，来自 `rag_knowledge.user_reviews` |

**FaqItem 结构：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `question` | `string` | 问题 |
| `answer` | `string` | 回答 |

**ReviewItem 结构：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `nickname` | `string` | 用户昵称 |
| `rating` | `int` | 评分 1-5 |
| `content` | `string` | 评价内容 |

### 2.5 错误响应

| 状态码 | 场景 | 响应体 |
|--------|------|--------|
| `404 Not Found` | `product_id` 不存在 | `{"detail": "Product not found: {product_id}"}` |
| `500 Internal Server Error` | 数据库异常 | `{"detail": "Internal server error"}` |

---

## 3. 后端实现规范

### 3.1 数据库 Schema 变更

**Product 表新增 3 列：**

| 列名 | 类型 | 说明 |
|------|------|------|
| `marketing_description` | `TEXT \| NULL` | 营销描述原文 |
| `official_faq` | `JSON` | `[{question: str, answer: str}]` |
| `user_reviews` | `JSON` | `[{nickname: str, rating: int, content: str}]` |

**highlights 存储位置：** `Product.metadata["highlights"]`（JSONB 字段，不新增列）

**影响文件：**
- `backend/src/repos/models.py` — Product 表定义
- `backend/src/services/product_ingest.py` — 入库逻辑

### 3.2 highlights 预计算

**脚本**：`backend/src/scripts/precompute_highlights.py`

**执行方式**：`cd backend && uv run -m src.scripts.precompute_highlights`

**逻辑**：
1. 遍历所有商品
2. 对每个商品，用 LLM 从 `marketing_description` + `official_faq` + `user_reviews` 中提取 3-5 条卖点
3. 写入 `Product.metadata["highlights"]`

**Prompt 模板**：

```
从以下商品信息中提取 3-5 条核心卖点，每条不超过 20 字。
要求：客观事实，不夸大，不编造。优先提取成分、功效、适用人群、使用建议。
格式：JSON 数组 ["卖点1", "卖点2", ...]

商品名：{name}
描述：{marketing_description}
FAQ：{official_faq}
评价：{user_reviews}
```

**约束**：
- 每条卖点 ≤ 20 字
- 数量 3-5 条
- 必须是客观事实，禁止 LLM 编造
- 入库后可人工审核

### 3.3 API 实现

**新增文件**：`backend/src/api/products.py`

**路由注册**：`backend/src/api/app.py` 中增加：

```python
from src.api.products import products_router
app.include_router(products_router, dependencies=_public_api_dependencies())
```

**数据来源**：
- `product`：`repos/products.py` → `get_product(product_id)`（已有）
- `marketing_description`/`faqs`/`reviews`：从 Product 表读取新增列
- `highlights`：从 `Product.metadata["highlights"]` 读取

**新增类型**：`backend/src/types/schemas.py`

```python
class FaqItem(BaseModel):
    question: str
    answer: str

class ReviewItem(BaseModel):
    nickname: str
    rating: int
    content: str

class ProductDetailResponse(BaseModel):
    product: ProductPayload
    marketing_description: str | None = None
    highlights: list[str] = Field(default_factory=list)
    faqs: list[FaqItem] = Field(default_factory=list)
    reviews: list[ReviewItem] = Field(default_factory=list)
```

---

## 4. Android 实现规范

### 4.1 网络层

**新增文件**：`core/network/.../ProductDetailApi.kt`

```kotlin
interface ProductDetailApi {
    suspend fun getProductDetail(productId: String): ProductDetailResponse
}
```

**新增类型**：`core/model/.../responses/ProductDetailResponse.kt`

```kotlin
@Serializable
data class ProductDetailResponse(
    val product: ProductPayload,
    @SerialName("marketing_description") val marketingDescription: String? = null,
    val highlights: List<String> = emptyList(),
    val faqs: List<FaqItem> = emptyList(),
    val reviews: List<ReviewItem> = emptyList(),
)

@Serializable
data class FaqItem(val question: String = "", val answer: String = "")

@Serializable
data class ReviewItem(val nickname: String = "", val rating: Int = 0, val content: String = "")
```

### 4.2 预取 + 缓存策略

**触发时机**：收到 SSE `product_card` 事件时立即触发预取。

**缓存策略**：
- 内存 `LruCache<String, ProductDetailResponse>(20)`
- 缓存 key = `product_id`
- 缓存命中时跳过预取
- 预取失败静默处理，不阻断主流程

**降级策略**：
- REST 请求失败 → 详情页降级显示 SSE 数据（已有的 `ProductPayload`）
- 不显示 loading 状态，不崩溃

**实现位置**：
- `ChatRepository` — 预取 + 缓存逻辑
- `ChatViewModel` — 在 SSE 事件处理中触发预取

### 4.3 详情页 UI 模块

在 `CinematicProductDetailPanel` 中按顺序新增 5 个模块：

#### 4.3.1 SKU 规格选择器

**位置**：标签行下方，AI 推荐理由上方

**UI 规范**：
- 横向滚动 `LazyRow`
- 每个 SKU 显示属性 + 价格（如 "30ml 经典装 ¥720"）
- 选中态：Primary 色边框 + 背景色
- 默认选中第一个 SKU
- 切换 SKU 时更新顶部价格显示

**数据源**：`product.sku_options`

#### 4.3.2 商品亮点（highlights）

**位置**：AI 推荐理由下方，分隔线前

**UI 规范**：
- 标题 "商品亮点"
- 3-5 条亮点，每条 = emoji 图标 + 一行文字
- 图标用 ✅ 或 Material Icons 的 `check_circle`
- 文字样式：`TextSecondary`，`Body` 字号

**数据源**：`highlights` 字段

#### 4.3.3 商品描述（可折叠）

**位置**：分隔线后，"详细信息" 区域

**UI 规范**：
- 标题 "商品描述"
- 默认折叠，显示前 3 行 + "展开" 按钮
- 展开后显示全文 + "收起" 按钮
- 文字样式：`TextSecondary`，`Body` 字号，`lineHeight = 23.sp`

**数据源**：`marketing_description` 字段

#### 4.3.4 官方 FAQ

**位置**：商品描述下方

**UI 规范**：
- 标题 "官方问答"
- 手风琴折叠，每条 FAQ 默认折叠只显示 question
- 点击展开 answer
- 同时只展开一条（互斥模式）
- question 样式：`TextPrimary`，`Label` 字号，`SemiBold`
- answer 样式：`TextSecondary`，`Body` 字号

**数据源**：`faqs` 字段

#### 4.3.5 用户评价

**位置**：FAQ 下方

**UI 规范**：
- 标题 "用户评价"
- 评分分布条（5星/4星/3星/2星/1星 各占比的水平条形图）
- 评价列表：昵称 + 评分星星 + 评价内容
- 星星用 Material Icons 的 `star` / `star_border`
- 评价内容最多显示 3 行，超出省略

**数据源**：`reviews` 字段

### 4.4 状态管理

**`ChatUiState` 新增字段**：

```kotlin
data class ChatUiState(
    // ... existing fields ...
    val productDetails: Map<String, ProductDetailResponse> = emptyMap(),
)
```

**`ChatReducer` 新增 action**：

```kotlin
sealed interface ChatAction {
    // ... existing actions ...
    data class ProductDetailLoaded(val productId: String, val detail: ProductDetailResponse) : ChatAction
    data class ProductDetailFailed(val productId: String) : ChatAction
}
```

---

## 5. 数据流

```
SSE product_card 事件到达
  ↓
ChatViewModel 提取 product_id
  ↓
ChatRepository.prefetchProductDetail(product_id)
  ↓ (async, 静默)
GET /products/{product_id}
  ↓
ProductDetailResponse 存入 LruCache
  ↓
用户点击商品卡片
  ↓
ProductHeroDetailScreen 从 LruCache 读取
  ↓ (缓存命中 → 零延迟)
渲染详情页 5 个模块
```

---

## 6. 验证标准

### 6.1 后端验证

```bash
# 现有测试全部通过
cd backend && uv run pytest -q

# lint 通过
cd backend && uv run ruff check src tests

# 接口正常响应
curl -s http://localhost:8000/products/p_beauty_001 | jq .

# 404 处理
curl -s http://localhost:8000/products/nonexistent
# 期望: {"detail": "Product not found: nonexistent"}

# highlights 预计算验证
cd backend && uv run -m src.scripts.precompute_highlights
# 然后查询 DB 确认 Product.metadata["highlights"] 已写入
```

### 6.2 Android 验证

1. 发送推荐请求 → 收到 `product_card` SSE 事件 → Logcat 确认预取请求发出
2. 点击商品卡片 → 详情页零延迟渲染（无 loading 状态）
3. 详情页 5 个模块全部展示：SKU 选择器 + 亮点 + 描述 + FAQ + 评价
4. SKU 切换 → 价格同步更新
5. 描述折叠/展开
6. FAQ 手风琴展开/折叠
7. 网络断开时进入详情页 → 降级显示 SSE 数据（不崩溃）

### 6.3 端到端 Demo 验证

1. 输入 "推荐适合油皮的洗面奶，200元以内"
2. 收到 3 个 `product_card` → 预取完成
3. 点击第一个商品 → 详情页秒开
4. 详情页内容丰富：营销描述 + FAQ + 评价 + 规格选择 + 亮点
5. 返回聊天 → 再次点击 → 仍从缓存秒开

---

## 7. 与现有系统的兼容性

### 7.1 SSE 协议不变

本方案不涉及 SSE 事件协议的任何变更：
- 不新增事件类型（铁律 1）
- 不修改现有事件字段
- `contracts/sse-events.schema.json` 无需更新

### 7.2 分层架构不变

- API 层：`products.py` 路由 → Service 层
- Service 层：新增 `product_detail` service（或直接复用 `repos/products.py`）
- Repo 层：Product 表新增列，查询逻辑新增
- 依赖方向：API → Service → Repo（无违规）

### 7.3 向后兼容

- 新增 REST 接口，不影响现有 SSE 流
- Product 表新增列为 `NULL` 默认值，不影响现有数据
- Android 详情页降级策略：REST 失败时回退到 SSE 数据

---

## 8. 关键文件清单

### 后端

| 文件 | 操作 | 说明 |
|------|------|------|
| `backend/src/repos/models.py` | 修改 | Product 表加 3 列 |
| `backend/src/services/product_ingest.py` | 修改 | 入库逻辑增加 3 个字段 |
| `backend/src/scripts/precompute_highlights.py` | 新增 | highlights 预计算脚本 |
| `backend/src/api/products.py` | 新增 | REST 接口实现 |
| `backend/src/types/schemas.py` | 修改 | 增加 ProductDetailResponse |
| `backend/src/api/app.py` | 修改 | 注册 products_router |

### Android

| 文件 | 操作 | 说明 |
|------|------|------|
| `core/network/.../ProductDetailApi.kt` | 新增 | REST 客户端 |
| `core/model/.../responses/ProductDetailResponse.kt` | 新增 | 响应类型 |
| `core/data/.../ChatRepository.kt` | 修改 | 预取 + 缓存 |
| `feature/chat/.../ChatViewModel.kt` | 修改 | 触发预取 |
| `feature/chat/.../ChatUiState.kt` | 修改 | 增加 productDetails 状态 |
| `feature/chat/.../ChatReducer.kt` | 修改 | 处理预取结果 |
| `feature/chat/.../BuyPilotChatScreen.kt` | 修改 | 5 个新 UI 模块 |

---

## 9. 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| highlights LLM 生成幻觉 | 详情页展示虚假信息 | 预计算 + 人工审核；Prompt 强调"客观事实，不编造" |
| REST 预取失败 | 详情页无数据 | 静默失败 + 降级显示 SSE 数据 |
| 预取网络开销 | 3 次 REST 调用 | 可忽略（单商品 < 2KB，总 < 6KB） |
| Product 表加列迁移 | 现有数据丢失 | 新列默认 NULL，不影响现有行 |
| 7 天工期不足 | 功能不完整 | 模块化实现，可按优先级裁剪（先 SKU+亮点+描述，后 FAQ+评价） |
