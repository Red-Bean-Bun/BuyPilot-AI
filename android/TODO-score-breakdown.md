# 前端待办：决策可视化增强（score_breakdown + 已排除提示）

> 对应飞书文档 ⑫ 反馈驱动重排可视化
> 后端已完成（2026-06-04），前端待实现
> 关联后端 PR：`FinalDecisionEvent` 新增 `score_breakdown` 字段

---

## 一、后端变更摘要

`FinalDecisionEvent`（SSE `final_decision` 事件）新增可选字段：

```json
{
  "event": "final_decision",
  "winner_product_id": "p_beauty_018",
  "summary": "...",
  "why": [...],
  "score_breakdown": {
    "retrieval": 0.85,
    "criteria_match": 1.25,
    "user_signal": 1.0,
    "evidence": 0.45,
    "risk_penalty": 0.0,
    "final_score": 0.8125,
    "rank": 1
  }
}
```

| Key | 类型 | 范围 | 含义 |
|-----|------|------|------|
| `retrieval` | float | 0~1 | 检索相关性（向量相似度 + rerank） |
| `criteria_match` | float | 0~2 | 标准匹配度（预算/肤质/场景/成分） |
| `user_signal` | float | -1.5~1.5 | 用户行为信号（滑动/收藏/加购） |
| `evidence` | float | 0~1 | 证据质量（chunk 数量 + 文本丰富度） |
| `risk_penalty` | float | 0~1.5 | 风险惩罚（被排除/被点踩/预算违规） |
| `final_score` | float | -∞~+∞ | 加权最终得分（retrieval×0.35 + criteria×0.25 + signal×0.25 + evidence×0.10 - risk×0.05） |
| `rank` | int | 1~N | 在候选中的排名 |

**字段可能为 `null`**（当 `score_breakdown` 未传时），前端必须做空值兼容。

---

## 二、改动清单

### Step 1：Model 层加字段

**文件**：`android/core/model/src/main/java/com/buypilot/core/model/AgentPayload.kt:126-136`

```kotlin
@Serializable
data class FinalDecisionPayload(
    @SerialName("winner_product_id") val winnerProductId: String? = null,
    val summary: String = "",
    val why: List<String> = emptyList(),
    @SerialName("not_for") val notFor: List<String> = emptyList(),
    val alternatives: List<AlternativePayload> = emptyList(),
    @SerialName("next_actions") val nextActions: List<QuickActionPayload> = emptyList(),
    @SerialName("decision_status") val decisionStatus: String? = null,
    val confidence: String? = null,
    @SerialName("next_step") val nextStep: String? = null,
    // ── 新增 ──
    @SerialName("score_breakdown") val scoreBreakdown: Map<String, Double>? = null,
) : AgentPayload
```

**解析无需改动**：`SseEventParser.kt:69` 使用 `json.decodeFromJsonElement<FinalDecisionPayload>()` 自动反序列化，kotlinx.serialization 会处理 `@SerialName` 映射。只需加字段即可。

### Step 2：ChatUiNode 加便捷访问器

**文件**：`android/feature/chat/src/main/java/com/buypilot/feature/chat/model/ChatUiNode.kt:67-75`

```kotlin
@Immutable
data class FinalDecisionNode(
    override val key: String,
    val payload: FinalDecisionPayload,
    val turnId: String = "",
    val deckId: String? = null,
) : ChatUiNode {
    val alternatives: List<AlternativePayload> = payload.alternatives
    val nextActions: List<QuickActionPayload> = payload.nextActions
    // ── 新增 ──
    val scoreBreakdown: Map<String, Double>? = payload.scoreBreakdown
}
```

### Step 3：DecisionSummaryCard 渲染分数

**文件**：`android/feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:2735-2945`

**插入位置建议**：在 `whyItems` 列表（line ~2896）和 `notForItems` WarningBox（line ~2906）之间，即"推荐理由"之后、"注意"之前。

#### 方案 A：横向条形图（推荐，实现简单）

```
  ┌─ 决策评分 ─────────────────────────────┐
  │  检索相关  ██████████████░░  0.85       │
  │  标准匹配  ████████████████████  1.25   │
  │  用户偏好  ████████████████  1.00       │
  │  证据质量  ████████░░░░░░░░  0.45       │
  │  风险惩罚  ░░░░░░░░░░░░░░░░  0.00       │
  │  ────────────────────────────────────── │
  │  综合得分  █████████████████████  0.81  │
  └─────────────────────────────────────────┘
```

```kotlin
@Composable
private fun ScoreBreakdownCard(breakdown: Map<String, Double>) {
    val displayItems = listOf(
        "retrieval" to "检索相关",
        "criteria_match" to "标准匹配",
        "user_signal" to "用户偏好",
        "evidence" to "证据质量",
        "risk_penalty" to "风险扣分",
    )
    val maxDisplayValue = 2.0  // criteria_match 最大 2.0

    Surface(
        color = BuyPilotColors.SurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BuyPilotColors.Divider),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "决策评分",
                fontSize = BuyPilotType.Label,
                fontWeight = FontWeight.SemiBold,
                color = BuyPilotColors.TextPrimary,
            )
            displayItems.forEach { (key, label) ->
                val value = breakdown[key] ?: 0.0
                ScoreBar(label = label, value = value, max = maxDisplayValue)
            }
            HorizontalDivider(color = BuyPilotColors.Divider)
            val finalScore = breakdown["final_score"] ?: 0.0
            ScoreBar(label = "综合得分", value = finalScore, max = maxDisplayValue, isHighlight = true)
        }
    }
}

@Composable
private fun ScoreBar(label: String, value: Double, max: Double, isHighlight: Boolean = false) {
    val fraction = (value / max).coerceIn(0.0, 1.0).toFloat()
    val barColor = if (isHighlight) BuyPilotColors.Primary else BuyPilotColors.PrimarySoft
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            fontSize = BuyPilotType.Caption,
            color = BuyPilotColors.TextSecondary,
            modifier = Modifier.width(64.dp),
        )
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .weight(1f)
                .height(6.dp),
            color = barColor,
            trackColor = BuyPilotColors.Divider,
        )
        Text(
            text = String.format("%.2f", value),
            fontSize = BuyPilotType.Caption,
            color = BuyPilotColors.TextSecondary,
            modifier = Modifier.width(40.dp).padding(start = 8.dp),
        )
    }
}
```

#### 方案 B：数字徽章行（更轻量，2 行代码）

```
  ┌ 检索 0.85 │ 标准 1.25 │ 偏好 1.00 │ 证据 0.45 │ 风险 0.00 ┐
  │                    综合 0.81                                │
  └─────────────────────────────────────────────────────────────┘
```

```kotlin
@Composable
private fun ScoreBreakdownRow(breakdown: Map<String, Double>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ScoreChip("检索", breakdown["retrieval"])
        ScoreChip("标准", breakdown["criteria_match"])
        ScoreChip("偏好", breakdown["user_signal"])
        ScoreChip("证据", breakdown["evidence"])
        ScoreChip("风险", breakdown["risk_penalty"])
    }
    val finalScore = breakdown["final_score"]
    if (finalScore != null) {
        Text("综合 ${String.format("%.2f", finalScore)}", fontWeight = FontWeight.SemiBold)
    }
}
```

### Step 4：调用处插入

在 `DecisionSummaryCard` 中（`BuyPilotChatScreen.kt`），`whyItems` 之后插入：

```kotlin
// 在 "推荐理由" SectionTitle + DecisionReasonList 之后
// 在 "注意" WarningBox 之前

payload.scoreBreakdown?.let { breakdown ->
    ScoreBreakdownCard(breakdown = breakdown)
    Spacer(modifier = Modifier.height(8.dp))
}
```

---

## 三、配色参考（BuyPilotColors）

| 用途 | 颜色 | 已有引用 |
|------|------|---------|
| 条形图主色 | `BuyPilotColors.Primary` | 已定义 |
| 条形图辅色 | `BuyPilotColors.PrimarySoft` | 已定义 |
| 背景 | `BuyPilotColors.SurfaceElevated` | 已定义 |
| 分割线 | `BuyPilotColors.Divider` | 已定义 |
| 文字主色 | `BuyPilotColors.TextPrimary` | 已定义 |
| 文字辅色 | `BuyPilotColors.TextSecondary` | 已定义 |
| 风险扣分高亮 | `BuyPilotColors.Attention` | 可选 |

---

## 四、空值兼容

`score_breakdown` 可能为 `null`（以下场景不传）：
- `decision_status = "no_match"` 时没有候选，无分数
- `decision_status = "no_suitable_winner"` 时所有候选被排除

前端渲染逻辑：

```kotlin
payload.scoreBreakdown?.let { breakdown ->
    ScoreBreakdownCard(breakdown)
}
// null 时不渲染任何分数区域，卡片其余部分正常展示
```

---

## 五、测试用例

| 场景 | score_breakdown 值 | 预期渲染 |
|------|-------------------|---------|
| 正常选中 | 全部 7 个 key 有值 | 显示所有维度条 + 综合得分 |
| `risk_penalty` 较高 | `risk_penalty: 1.5` | 风险条较长，可用 `Attention` 色高亮 |
| `user_signal` 为负 | `user_signal: -1.2` | 负值条需要 coerceIn 处理或反向显示 |
| `score_breakdown` 为 null | 字段缺失 | 不渲染分数区域 |
| 部分 key 缺失 | 只有 3 个 key | 缺失 key 显示 0.0 |

---

## 六、验证

1. 后端启动 → 发送 "推荐适合油皮的洗面奶，200元以内"
2. 收到 `final_decision` SSE 事件，确认 `score_breakdown` 字段非 null
3. 决策卡片正常渲染分数区域
4. 分数值与后端 `decision_scoring.py` 的权重公式一致

---

## 七、"已排除：xx" 反馈可视化

### 7.1 背景

飞书文档 ⑫ 的另一半需求：用户在 SwipeDeck 里滑掉不喜欢的商品后，决策卡应**显式显示哪些商品被排除**，让用户看到"我的反馈起了作用"。

### 7.2 `not_for` 字段的混合语义（重要）

当前后端 `FinalDecisionEvent.not_for: list[str]` 有两种填法，前端必须区分处理：

| 场景 | `decision_status` | `not_for` 内容 | 示例 |
|------|-------------------|---------------|------|
| 有胜出者 | `"selected"` | LLM 生成的**人话**（不适合人群/注意事项） | `["极敏感肌：含少量香精，严重敏感肌建议选备选"]` |
| 所有候选被排除 | `"no_suitable_winner"` | 被排除的 **product_id** 列表 | `["p_beauty_003", "p_beauty_007"]` |

**当前前端渲染**（`BuyPilotChatScreen.kt:2906`）：

```kotlin
WarningBox(notForItems.joinToString("；"))
```

这会把 product_id 和 LLM 描述文字用同一种方式渲染——当 `not_for` 是 ID 列表时，用户看到的是 `p_beauty_003；p_beauty_007` 这种原始字符串，毫无可读性。

### 7.3 需要做的改动

#### A. 区分 not_for 类型

在 `DecisionSummaryCard` 里（`BuyPilotChatScreen.kt:2758` 附近），区分 `not_for` 是 ID 还是描述文字：

```kotlin
// 判断 not_for 里是否全是 product_id 格式（以 "p_" 开头）
val notForAreIds = payload.notFor.all { it.startsWith("p_") }

val notForDescriptions = if (notForAreIds && productsById.isNotEmpty()) {
    // product_id → 商品名称映射
    payload.notFor.mapNotNull { id ->
        productsById[id]?.product?.name?.let { name -> "$name（已被你排除）" }
    }
} else {
    // LLM 生成的人话，直接用
    payload.notFor
}
val notForItems = notForDescriptions
    .map { it.withoutMarkdownMarkup().withoutInternalDebugTokens().trim() }
    .filter { it.isNotBlank() }
```

#### B. 已排除商品条（SwipeDeck 场景）

在 SwipeDeck 完成滑动 → 最终决策的完整流程中，用户明确滑掉的商品应该在决策卡里**以商品名显示**，而不是 ID。

**插入位置**：`DecisionSummaryCard` 中 `notForItems` WarningBox 之前，新增"被排除的候选"区域：

```kotlin
// ── 已排除候选（从 productsById 中找出非胜出商品）──
val excludedProducts = productsById.values
    .filter { card -> card.product.productId != payload.winnerProductId }
    .filter { card -> payload.notFor.contains(card.product.productId) }

if (excludedProducts.isNotEmpty()) {
    ExcludedProductsRow(excludedProducts)
    Spacer(modifier = Modifier.height(8.dp))
}
```

#### C. ExcludedProductsRow 组件

```
  ┌ 你排除了：兰蔻清滢洁面 · SK-II 神仙水 ──────────┐
  └──────────────────────────────────────────────────┘
```

```kotlin
@Composable
private fun ExcludedProductsRow(excluded: List<ProductCardPayload>) {
    val names = excluded.mapNotNull { it.product.name }.joinToString(" · ")
    if (names.isBlank()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BuyPilotColors.Attention.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "已排除：",
            color = BuyPilotColors.TextSecondary,
            fontSize = BuyPilotType.Caption,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = names,
            color = BuyPilotColors.TextSecondary,
            fontSize = BuyPilotType.Caption,
            modifier = Modifier.weight(1f),
        )
    }
}
```

### 7.4 完整渲染流程

```
DecisionSummaryCard
  ├── DecisionSummaryIntro（summary 文字）
  ├── Surface 卡片体
  │   ├── 胜出商品行（图片 + 名称 + 价格）
  │   ├── 推荐理由列表（why items）
  │   ├── ScoreBreakdownCard（分数可视化，见上文）        ← Step 4
  │   ├── ExcludedProductsRow（"已排除：A · B"）          ← 7.3 C
  │   ├── WarningBox（not_for 人话注意事项）               ← 已有，用 7.3 A 清洗后的值
  │   └── DecisionCartActionBar（加购按钮）
  └── ScrollableQuickActionRow（再便宜/加入对比）
```

### 7.5 数据来源说明

| 数据 | 从哪来 | 何时可用 |
|------|--------|---------|
| 被排除商品 ID | `FinalDecisionPayload.notFor`（当 `decision_status = "no_suitable_winner"`） | SSE 事件到达 |
| 商品 ID → 名称映射 | `productsById: Map<String, ProductCardPayload>` | 同 turn 的 `product_card` 事件已先到达 |
| 用户滑动反馈 | SwipeDeck `onSwipedLeft` 回调（已在 `ProductSwipeComponents.kt` 中实现） | 用户滑动时 |
| 胜出商品 ID | `FinalDecisionPayload.winnerProductId` | SSE 事件到达 |

### 7.6 测试用例

| 场景 | 预期渲染 |
|------|---------|
| 用户滑掉 2 个商品 → 决策选出 1 个 | "已排除：兰蔻清滢洁面 · SK-II 神仙水" |
| `not_for` 是 LLM 人话 | WarningBox 正常显示"极敏感肌慎用" |
| `not_for` 是 product_id 列表 | ExcludedProductsRow 显示商品名，WarningBox 不重复显示 ID |
| `decision_status = "no_suitable_winner"` | 所有候选名显示在"已排除"行，无胜出商品行 |
| `productsById` 为空（极端情况） | 降级为原始字符串渲染，不崩溃 |

---

## 八、改动优先级

| 优先级 | 改动 | 工作量 | 答辩价值 |
|--------|------|--------|---------|
| P0 | Step 1-4（score_breakdown 分数可视化） | ~2h | 展示决策透明度 |
| P0 | 7.3 A（not_for 类型区分） | ~15min | 修复 product_id 渲染 bug |
| P1 | 7.3 B-C（已排除商品条） | ~1h | 展示反馈闭环效果 |

P0 是答辩必须（"评委能看到分数 + 看不到 p_beauty_003 这种乱码"），P1 是加分项。
