# BuyPilot-AI 商品详情页增强方案

> 📌 **面向**：Android 开发者
> 📅 **日期**：2026-06-03
> 🔗 **后端接口**：`GET /products/{product_id}`（后端同步开发中）
> ⏰ **截止**：2026-06-10

---

## 1. 背景

当前商品详情页（`ProductHeroDetailScreen`）没有独立 API，所有数据来自 SSE `product_card` 事件中的 `ProductPayload`。详情页只展示商品图 + 名称/价格/品牌 + 标签 + AI 推荐理由 + 风险提示。

原始数据中还有丰富的内容没有送到前端：

| 数据 | 当前状态 |
|------|---------|
| 营销描述（`marketing_description`，约 300 字） | ❌ 未送前端 |
| 官方 FAQ（3 条 question + answer） | ❌ 未送前端 |
| 用户评价（5 条，含昵称/评分/内容） | ❌ 未送前端 |
| SKU 多规格选项（已在 payload 中） | ⚠️ 有数据但未渲染 |

**目标**：新增 REST 接口 + Android 投机预取，让详情页秒开且内容丰富，接近商业电商体验。

---

## 2. 后端接口契约

### 2.1 端点

```
GET /products/{product_id}
```

- **Content-Type**：`application/json`
- **缓存**：`Cache-Control: public, max-age=3600`
- **错误码**：`404`（商品不存在）/ `500`（服务异常）

### 2.2 响应结构（ProductDetailResponse）

```json
{
  "product": { ... },                    // ProductPayload，与 SSE product_card 中结构一致
  "marketing_description": "...",         // string | null，品牌营销描述原文
  "highlights": ["...", "..."],           // string[]，3-5 条预计算卖点（每条 ≤ 20 字）
  "faqs": [{ "question": "...", "answer": "..." }],   // FaqItem[]
  "reviews": [{ "nickname": "...", "rating": 5, "content": "..." }]  // ReviewItem[]
}
```

**product 字段**：与现有 SSE `product_card` 中的 `product` 完全一致，包含 `product_id`、`name`、`price`、`currency`、`image_url`、`category`、`sub_category`、`brand`、`skin_type_match`、`ingredient_tags`、`ingredient_avoid`、`use_scenario`、`sku_options`。自包含响应，详情页只需依赖 REST 数据，无需合并 SSE 状态。

**高亮字段（highlights）**：后端预计算的静态卖点，3-5 条，每条不超过 20 字。例如：`["核心成分二裂酵母发酵产物溶胞物", "夜间修护，促进肌肤代谢", "透明质酸锁水保湿"]`

### 2.3 完整响应示例（以 p_beauty_001 为例）

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

### 2.4 字段速查表

| 字段 | 类型 | 可空 | 说明 |
|------|------|------|------|
| `product` | `ProductPayload` | 否 | 与 SSE product_card.product 结构一致 |
| `marketing_description` | `string` | 是 | 品牌营销描述原文，约 200-400 字 |
| `highlights` | `string[]` | 否 | 3-5 条卖点，每条 ≤ 20 字，空数组兜底 |
| `faqs` | `FaqItem[]` | 否 | 官方 FAQ，空数组兜底 |
| `reviews` | `ReviewItem[]` | 否 | 用户评价，空数组兜底 |

**FaqItem**：`{ question: string, answer: string }`

**ReviewItem**：`{ nickname: string, rating: int (1-5), content: string }`

---

## 3. Android 实现方案

### 3.1 数据流总览

```
SSE product_card 事件到达（ChatViewModel 处理）
  │
  ├─ 提取 product_id
  │
  └─ ChatRepository.prefetchProductDetail(productId)
       │
       ├─ 缓存命中 → 跳过
       │
       └─ 缓存未命中 → GET /products/{product_id}
            │
            ├─ 成功 → 存入 LruCache
            │
            └─ 失败 → 静默处理（不阻断主流程）

用户点击商品卡片进入详情页
  │
  ├─ 缓存命中 → 零延迟渲染详情页
  │
  └─ 缓存未命中 → 直接发 REST 请求（降级场景）
       │
       └─ 失败 → 回退到 SSE ProductPayload 渲染（已有行为）
```

### 3.2 新增类型

**文件**：`core/model/src/main/java/com/buypilot/core/model/responses/ProductDetailResponse.kt`

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
data class FaqItem(
    val question: String = "",
    val answer: String = "",
)

@Serializable
data class ReviewItem(
    val nickname: String = "",
    val rating: Int = 0,
    val content: String = "",
)
```

### 3.3 网络层

**文件**：`core/network/src/main/java/com/buypilot/core/network/ProductDetailApi.kt`

```kotlin
interface ProductDetailApi {
    suspend fun getProductDetail(productId: String): ProductDetailResponse
}

class OkHttpProductDetailApi(
    private val restClient: RestClient,
) : ProductDetailApi {
    override suspend fun getProductDetail(productId: String): ProductDetailResponse =
        restClient.getJson(
            path = "/products/$productId",
            responseDeserializer = ProductDetailResponse.serializer(),
        )
}
```

**DI 注册**：在 `NetworkModule.kt` 中增加 `ProductDetailApi` 的 `@Provides`。

### 3.4 预取 + 缓存

**修改文件**：`core/data/src/main/java/com/buypilot/core/data/ChatRepository.kt`

```kotlin
class ChatRepository @Inject constructor(
    // ... existing deps ...
    private val productDetailApi: ProductDetailApi,
) {
    // 内存缓存，最多 20 个商品详情
    private val productDetailCache = LruCache<String, ProductDetailResponse>(20)
    private val prefetchScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** SSE product_card 到达时调用，fire-and-forget */
    fun prefetchProductDetail(productId: String) {
        if (productDetailCache.get(productId) != null) return
        prefetchScope.launch {
            try {
                val detail = productDetailApi.getProductDetail(productId)
                productDetailCache.put(productId, detail)
            } catch (_: Exception) {
                // 静默失败，详情页降级到 SSE 数据
            }
        }
    }

    /** 详情页获取数据，优先缓存 */
    fun getCachedProductDetail(productId: String): ProductDetailResponse? =
        productDetailCache.get(productId)

    suspend fun fetchProductDetail(productId: String): ProductDetailResponse {
        getCachedProductDetail(productId)?.let { return it }
        return productDetailApi.getProductDetail(productId)
    }
}
```

### 3.5 预取触发点

**修改文件**：`feature/chat/src/main/java/com/buypilot/feature/chat/ChatViewModel.kt`

在 SSE 事件处理的 `ProductCard` 分支中增加预取：

```kotlin
AgentEventType.ProductCard -> {
    val productId = (payload as? ProductCardPayload)?.product?.productId
    if (!productId.isNullOrBlank()) {
        chatRepository.prefetchProductDetail(productId)
    }
    // ... existing handling ...
}
```

### 3.6 详情页数据读取

**修改文件**：`feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt`

在 `ProductHeroDetailScreen` 中从缓存读取详情数据：

```kotlin
// 从 ChatRepository 获取详情数据（同步读缓存，无需 loading）
val detail = remember(activeProductId) {
    chatRepository.getCachedProductDetail(activeProductId)
}
// detail 可能为 null（预取失败场景），UI 需做空安全处理
```

---

## 4. 详情页 UI 模块规范

### 4.1 模块排列顺序（从上到下）

在 `CinematicProductDetailPanel` 的 `LazyColumn` 中，**已有模块**与**新增模块**交错排列：

| # | 模块 | 状态 | 数据源 |
|---|------|------|--------|
| 1 | 商品大图 + 渐变遮罩 | ✅ 已有 | `product.image_url` |
| 2 | 价格 + 名称 + 品牌 | ✅ 已有 | `product.*` |
| 3 | 标签行（肤质/成分/场景） | ✅ 已有 | `product.skin_type_match` 等 |
| **4** | **🆕 SKU 规格选择器** | **新增** | `product.sku_options` |
| 5 | AI 推荐理由 | ✅ 已有 | `payload.reason` |
| **6** | **🆕 商品亮点** | **新增** | `detail.highlights` |
| 7 | 风险提示 | ✅ 已有 | `payload.risk_notes` |
| 8 | 属性行（适用对象/成分/场景） | ✅ 已有 | `product.*` |
| **9** | **🆕 商品描述（可折叠）** | **新增** | `detail.marketingDescription` |
| **10** | **🆕 官方 FAQ** | **新增** | `detail.faqs` |
| **11** | **🆕 用户评价** | **新增** | `detail.reviews` |
| 12 | 加购按钮 | ✅ 已有 | cart action |

> ⚠️ 所有新增模块在数据为空时整块隐藏，不显示空态占位。

### 4.2 SKU 规格选择器

**位置**：标签行下方，AI 推荐理由上方

**布局**：`LazyRow`，`horizontalArrangement = spacedBy(10.dp)`

**每个 SKU 标签**：

```
Surface(圆角 12dp) {
    Row(spacedBy(6.dp), padding = 12.dp) {
        Text("30ml 经典装", TextPrimary, Label)
        Spacer(4.dp)
        Text("¥720", Primary, Label, SemiBold)
    }
}
```

**选中态**：
- 边框：`Primary` 色，1.5dp
- 背景：`Primary.copy(alpha = 0.08f)`
- 文字价格：`Primary` 色

**未选中态**：
- 边框：`Border` 色，1dp
- 背景：`SurfaceCard`
- 文字价格：`TextSecondary` 色

**交互**：
- 默认选中第一个 SKU
- 切换时顶部价格 `animateContentSize(tween(200ms))` 更新
- 价格取自选中 SKU 的 `price` 字段（非 `product.price` 基础价）

**数据**：`product.sku_options`（`List<JsonObject>?`），每个对象含 `sku_id`、`properties`（`Map<String, String>`）、`price`

### 4.3 商品亮点

**位置**：AI 推荐理由下方，分隔线前

**布局**：

```
Column(spacedBy = 10.dp) {
    ImmersiveSectionTitle("商品亮点")
    highlights.forEach { highlight ->
        Row(spacedBy = 10.dp) {
            Text("✅", fontSize = 16.sp)
            Text(highlight, TextSecondary, Body, lineHeight = 21.sp)
        }
    }
}
```

**无数据时**：整块隐藏

### 4.4 商品描述（可折叠）

**位置**：分隔线后，`ProductAttributeRowsDark` 下方

**布局**：

```
Column {
    ImmersiveSectionTitle("商品描述")
    Column(Modifier.animateContentSize(tween(260ms))) {
        Text(
            text = marketingDescription,
            style = TextStyle(TextSecondary, Body, lineHeight = 23.sp),
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "收起" else "展开全文", Primary, Label)
        }
    }
}
```

**折叠判断**：只在文本实际超过 3 行时显示"展开"按钮（可通过 `onTextLayout` 的 `hasVisualOverflow` 检测）

**无数据时**：整块隐藏

### 4.5 官方 FAQ（手风琴）

**位置**：商品描述下方

**布局**：

```
Column {
    ImmersiveSectionTitle("官方问答")
    Column(spacedBy = 8.dp) {
        faqs.forEachIndexed { index, faq ->
            Surface(圆角 16dp, SurfaceCard 背景, Border 边框) {
                Column(padding = 16.dp) {
                    Row(verticalAlignment = CenterVertically) {
                        Text(faq.question, TextPrimary, Label, SemiBold, Modifier.weight(1f))
                        Icon(
                            if (expandedIndex == index) ExpandLess else ExpandMore,
                            TextMuted
                        )
                    }
                    if (expandedIndex == index) {
                        Spacer(Modifier.height(8.dp))
                        Text(faq.answer, TextSecondary, Body, lineHeight = 23.sp)
                    }
                }
            }
        }
    }
}
```

**互斥模式**：`var expandedIndex by remember { mutableStateOf(-1) }`，同时只展开一条

**动画**：`Modifier.animateContentSize(tween(200ms))`

**无数据时**：整块隐藏

### 4.6 用户评价

**位置**：FAQ 下方

**布局**：

```
Column {
    ImmersiveSectionTitle("用户评价 (${reviews.size})")

    // 评分分布条
    RatingDistributionBar(reviews)

    Spacer(Modifier.height(12.dp))

    // 评价列表
    Column(spacedBy = 12.dp) {
        reviews.forEach { review ->
            Column(spacedBy = 4.dp) {
                Row(verticalAlignment = CenterVertically) {
                    Text(review.nickname, TextMuted, Label)
                    Spacer(Modifier.width(8.dp))
                    RatingStars(review.rating)  // 1-5 星
                }
                Text(
                    review.content,
                    TextSecondary, Body,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
```

**评分分布条** `RatingDistributionBar`：
- 5 行水平条形图，从 5 星到 1 星
- 每行：`Text("5星", TextMuted, 12.sp)` + 比例条 `Box(width = fraction * maxWidth, height = 6.dp, Primary 色)` + `Text(count, TextMuted, 12.sp)`
- `fraction = count / total`

**评分星星** `RatingStars`：
- 用 `Icons.Filled.Star`（黄色 `Color(0xFFFFC107)`）和 `Icons.Filled.StarBorder`（灰色 `TextMuted`）
- 大小 14.dp

**无数据时**：整块隐藏

---

## 5. 设计系统对齐

所有新增模块必须遵循项目 `DESIGN.md` 设计系统：

| 属性 | 值 |
|------|-----|
| 颜色 | `BuyPilotColors` 色板：`Primary` / `TextPrimary` / `TextSecondary` / `TextMuted` / `SurfaceCard` / `Border` |
| 字号 | `BuyPilotType.Body` (15sp) / `BuyPilotType.Label` (13sp) / `BuyPilotType.Title` (18sp) |
| 圆角 | 卡片 16dp，标签 12dp |
| 间距 | 模块间 18dp，内容间 10-12dp |
| 动画 | `tween(200-260ms)`，`PremiumRevealEase` 或 `MenuEaseOut` |
| 分割线 | 复用 `CinematicDetailDivider` |
| 段落标题 | 复用 `ImmersiveSectionTitle` |

---

## 6. 降级策略

详情页数据获取存在三种情况：

| 场景 | 数据来源 | 详情页表现 |
|------|---------|-----------|
| 预取成功（正常路径） | `LruCache` 缓存 | 全部 5 个新模块 + 已有模块，零延迟 |
| 预取失败 + 进入时重试成功 | REST 实时请求 | 全部模块，有短暂加载 |
| 预取失败 + 重试也失败 | SSE `ProductPayload` | 只展示已有模块（图片/名称/价格/标签/AI 理由/风险），**不崩溃** |

**关键原则**：新增模块在 `detail == null` 时全部隐藏，已有模块正常显示。用户不会看到 loading spinner 或错误提示。

---

## 7. 与现有系统的兼容性

| 方面 | 影响 |
|------|------|
| SSE 协议 | **不变**。不新增事件类型，不修改现有事件字段 |
| `contracts/sse-events.schema.json` | **不需要改** |
| `AgentPayload.kt` / `SseEventParser.kt` | **不需要改** |
| `ProductPayload` (Kotlin) | **不需要改**（`sku_options` 已在其中） |
| 现有聊天流 | **不变**。预取是旁路操作 |
| 现有详情页（SSE 数据渲染） | **不变**。降级路径复用现有逻辑 |

---

## 8. Android 关键文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `core/model/.../responses/ProductDetailResponse.kt` | 🆕 新增 | 响应类型 + FaqItem + ReviewItem |
| `core/network/.../ProductDetailApi.kt` | 🆕 新增 | REST 客户端接口 + OkHttp 实现 |
| `core/network/.../di/NetworkModule.kt` | ✏️ 修改 | 注册 ProductDetailApi 的 @Provides |
| `core/data/.../ChatRepository.kt` | ✏️ 修改 | 注入 ProductDetailApi + LruCache + prefetch/fetch 方法 |
| `feature/chat/.../ChatViewModel.kt` | ✏️ 修改 | SSE ProductCard 分支触发预取 |
| `feature/chat/.../BuyPilotChatScreen.kt` | ✏️ 修改 | 5 个新 Composable + 详情页读取 detail 数据 |

---

## 9. 验收标准

### 功能验收

- [ ] 发送推荐请求 → 收到 `product_card` SSE → Logcat 确认预取请求发出
- [ ] 点击商品卡片 → 详情页零延迟渲染（无 loading 状态）
- [ ] SKU 规格选择器显示 + 切换后价格更新
- [ ] 商品亮点显示 3-5 条
- [ ] 商品描述默认折叠 3 行，点击"展开全文"展开
- [ ] 官方 FAQ 手风琴展开/折叠，互斥模式
- [ ] 用户评价显示评分分布条 + 评价列表
- [ ] 网络断开时进入详情页 → 降级显示 SSE 已有数据，不崩溃

### Demo 路径验收

- [ ] 输入 "推荐适合油皮的洗面奶，200元以内"
- [ ] 收到多个 `product_card` → 预取完成
- [ ] 点击任一商品 → 详情页秒开，内容丰富
- [ ] 返回聊天 → 再次点击 → 仍从缓存秒开

---

## 10. 风险

| 风险 | 缓解 |
|------|------|
| REST 预取失败 | 静默失败 + 降级到 SSE 数据 |
| 预取网络开销 | 可忽略（单商品 < 2KB，3 商品 < 6KB） |
| 工期不足 | 模块化实现，可按优先级裁剪：先 SKU + 亮点 + 描述（P0），后 FAQ + 评价（P1） |
| `sku_options` 为 null | UI 做空安全处理，SKU 选择器整块隐藏 |
