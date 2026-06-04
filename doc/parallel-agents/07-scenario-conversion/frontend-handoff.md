# 前端队友交接：场景化选购转化契约

更新时间：2026-06-05

后端 P0 已接通并通过重点回归。全量 `backend/tests` 当前结果为 `399 passed, 1 failed`，唯一失败是 Android Markdown/Markwon 静态验收项，见本文最后一节。

## 后端现状

- 不新增 SSE event type。
- 不新增 quick action type。
- `criteria_card` 顶层新增可选字段 `shopping_strategy`。
- 只有 `criteria_card.shopping_strategy != null` 的 turn 才是场景化 turn。
- 普通推荐仍保持现有 product-first 行为，前端不要全局改成 criteria-first。
- P0 不下发 `strategy_feedback`，不下发 `replace_target`。

## 数据模型

Android 需要在 `android/core/model/src/main/java/com/buypilot/core/model/AgentPayload.kt` 给 `CriteriaCardPayload` 增加：

```kotlin
@SerialName("shopping_strategy")
val shoppingStrategy: ShoppingStrategyPayload? = null
```

建议新增这些 `@Serializable` model，字段按后端 snake_case 映射：

```kotlin
@Serializable
data class ShoppingStrategyPayload(
    @SerialName("strategy_id") val strategyId: String = "",
    @SerialName("scene_type") val sceneType: String = "",
    @SerialName("scene_summary") val sceneSummary: String = "",
    @SerialName("user_problem") val userProblem: String = "",
    @SerialName("decision_barrier") val decisionBarrier: DecisionBarrierPayload? = null,
    @SerialName("primary_direction") val primaryDirection: PrimaryDirectionPayload = PrimaryDirectionPayload(),
    @SerialName("avoid_risks") val avoidRisks: List<String> = emptyList(),
    val assumptions: List<String> = emptyList(),
    val confidence: String = "medium",
)

@Serializable
data class DecisionBarrierPayload(
    @SerialName("barrier_type") val barrierType: String = "",
    val label: String = "",
    val reason: String = "",
    @SerialName("conversion_strategy") val conversionStrategy: String = "",
)

@Serializable
data class PrimaryDirectionPayload(
    val title: String = "",
    val summary: String = "",
    val why: String = "",
    @SerialName("search_strategy") val searchStrategy: SearchStrategyPayload = SearchStrategyPayload(),
    @SerialName("available_in_catalog") val availableInCatalog: Boolean = false,
    @SerialName("supporting_product_count") val supportingProductCount: Int = 0,
)

@Serializable
data class SearchStrategyPayload(
    val category: String? = null,
    @SerialName("product_type") val productType: String? = null,
    @SerialName("use_scenario") val useScenario: String? = null,
)
```

示例 payload：

```json
{
  "event": "criteria_card",
  "criteria": {},
  "shopping_strategy": {
    "strategy_id": "scene_001",
    "scene_type": "gift",
    "scene_summary": "送男朋友礼物",
    "user_problem": "用户不确定这个场景下送什么更体面、更有心意、更不容易踩雷",
    "decision_barrier": {
      "barrier_type": "fear_wrong_choice",
      "label": "怕送错、怕不够体面",
      "reason": "礼物场景下核心设备容易踩型号偏好，送礼风险高",
      "conversion_strategy": "先推荐低偏好依赖、礼物感更强的小件，并保留查看依据和换方向入口"
    },
    "primary_direction": {
      "title": "低踩雷的黑科技小件",
      "summary": "优先考虑音频配件或轻量数码配件",
      "why": "有新鲜感，不强依赖具体型号偏好，也更适合生日礼物",
      "search_strategy": {
        "category": "数码电子",
        "product_type": "真无线耳机",
        "use_scenario": "日常使用"
      },
      "available_in_catalog": true,
      "supporting_product_count": 2
    },
    "avoid_risks": ["不要盲买手机、电脑这类强型号偏好的大件"],
    "assumptions": ["暂时不知道预算", "暂时不知道对方常用品牌"],
    "confidence": "high"
  }
}
```

## 渲染要求

有 `shoppingStrategy` 时，仍渲染为 `CriteriaCard` 的一部分，不新增 `AgentEventType.ScenarioAdvice`。

建议 UI 层级：

- 主标题：`选购思路`
- 购买顾虑：`decisionBarrier.label`
- 主方向：`primaryDirection.title`
- 方向说明：`primaryDirection.summary`
- 为什么：`primaryDirection.why`
- 避坑点：`avoidRisks`
- 假设：`assumptions`
- 筛选条件：继续渲染原 `criteria`，但视觉层级降为辅助信息

无 `shoppingStrategy` 时，保持现有筛选卡渲染。

商品卡不用特殊解析新字段。后端已经把场景化商品 `reason` 改成短转化文案，例如 `低偏好依赖，送礼更稳`，前端照常展示 `product_card.reason`。

## 事件顺序

场景化 turn 的可靠判定是：同一 turn 中存在 `criteria_card.shopping_strategy != null`。

场景化 turn 需要保证：

```text
text_delta / thinking 若干
criteria_card(shopping_strategy != null)
product_card*
text_delta 推荐说明
done
```

注意：

- 前面可能已有普通 intro `text_delta` 或 `thinking`，不要假设第一个 `text_delta` 一定是场景判断。
- 只要求同一 turn 内 `criteria_card(shopping_strategy)` 早于 `product_card`。
- 普通推荐仍可能是 `product_card* -> criteria_card -> text_delta -> done`，不要改全局 reducer 假设。

## Quick Action

P0 只使用现有 action：

- `open_evidence`
- `criteria_patch`
- `feedback`
- `compare`
- `add_to_cart`

`final_decision.decision_status == "selected"` 且有 `winner_product_id` 时，后端会在 `final_decision.next_actions` 追加：

```json
{
  "action_id": "add_winner_to_cart",
  "label": "加入购物车",
  "action": "add_to_cart"
}
```

这个 action 可能不带 `product_id`。前端点击时应使用同一 `final_decision.winner_product_id` 作为目标商品。当前 `BuyPilotChatScreen.kt` 的决策面板里已有类似兜底逻辑：当 `action == "add_to_cart"` 且 `productId` 为空时，用 `winnerProductId` 回填。

点击后复用现有加购逻辑，不显示新的用户气泡。

## 前端必测

1. `criteria_card` 带 `shopping_strategy` 时能反序列化，不崩溃。
2. `shoppingStrategy == null` 时，普通筛选卡完全保持原样。
3. 场景化 turn 中，选购思路区出现在商品卡之前。
4. 普通推荐 turn 不受影响，仍支持 product-first。
5. 商品卡短 reason 正常展示，不被本地重写。
6. `final_decision.next_actions.add_winner_to_cart` 点击后加购 winner。
7. 不新增 `AgentEventType.ScenarioAdvice`。
8. 不展示 `换个思路`。

## 当前前端风险

后端全量测试唯一失败项是：

```text
backend/tests/test_issue_acceptance.py::test_issue8_android_markdown_rendering_uses_markwon
```

失败原因：静态检查期望 `android/feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt` 引入并使用 `io.noties.markwon.Markwon`，当前文件没有该 import。

这个不是场景化后端契约问题，但会影响整体验收。前端同学需要补 Markdown/Markwon 渲染入口，避免加粗、列表、编号在 Android 里显示成原始 Markdown 符号。

## P0 不做

- 不新增 `AgentEventType.ScenarioAdvice`
- 不展示 `换个思路`
- 不处理 `strategy_feedback`
- 不处理 `replace_target`
- 不接真实短视频、直播、优惠券、达人数据
