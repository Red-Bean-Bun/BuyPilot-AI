# Agent 3 / Claude Code 提示词：Runtime 集成与转化动作

你在 `/mnt/disk1/LZJ/project/BuyPilot-AI` 工作。只做后端 runtime 集成与转化动作，不碰 Android，不改 schema/type 定义。

## 背景

阅读：

- `CLAUDE.md`
- `doc/prd/07-场景化选购助手PRD.md`
- `backend/src/runtime/handlers.py`
- `backend/src/runtime/pipeline.py`
- `backend/src/runtime/stages/recommendation.py`
- `backend/src/services/recommendation_reasons.py`
- `backend/src/config/user_messages.py`

目标：

- 场景化 turn 使用 `ShoppingStrategyService`
- 场景化顺序：`thinking -> text_delta -> criteria_card(shopping_strategy) -> thinking -> product_card* -> text_delta -> done`
- 普通推荐顺序保持 product-first，不改现有普通推荐测试语义
- `final_decision.decision_status == selected` 时下发 `add_to_cart`
- 商品 reason 在场景化链路中服务 `decision_barrier`

## Owned files

你主要修改：

- `backend/src/runtime/handlers.py`
- `backend/src/services/recommendation_reasons.py`（仅当必须）
- 可新增 runtime 小测试，例如 `backend/tests/test_scenario_strategy_runtime.py`

不要修改：

- `contracts/sse-events.schema.json`
- `backend/src/types/sse_events.py`
- `backend/src/services/shopping_strategy.py`
- Android 任何文件

## 依赖接口

假设 Agent 2 提供：

```python
from src.services.shopping_strategy import build_shopping_strategy_plan, ShoppingStrategyPlan
```

接口：

```python
async def build_shopping_strategy_plan(
    body: ChatStreamRequest,
    intent: IntentResult,
    criteria: CriteriaPayload,
    *,
    retrieval_probe: Callable[[CriteriaPayload], Awaitable[RetrievalResult]] | None = None,
) -> ShoppingStrategyPlan | None:
    ...
```

`ShoppingStrategyPlan` 字段：

- `route`
- `scene_judgement_text`
- `criteria`
- `shopping_strategy`
- `reason_hint`

如果 Agent 2 尚未合入，可以先写集成代码，但不要在 runtime 里重复实现 service。

## 具体任务

### 1. criteria_card 支持 shopping_strategy

修改 `_criteria_card_event`，增加可选参数：

```python
def _criteria_card_event(
    ctx: StreamContext,
    criteria: CriteriaPayload,
    shopping_strategy: ShoppingStrategyPayload | None = None,
) -> CriteriaCardEvent:
    ...
```

普通链路继续不传该参数。

### 2. handle_recommendation 接入策略

在 criteria 生成后、正式推荐输出前调用 `build_shopping_strategy_plan`。

传入 `retrieval_probe`，内部用 `ctx.stages.run_retrieval(candidate_criteria, top_n=3, feedback=feedback, image_embedding=image_embedding)`。

如果返回 `None`：保持现有普通推荐链路。

如果返回 plan：

- 使用 `plan.criteria`
- 先 stream `plan.scene_judgement_text`
- 再 yield `_criteria_card_event(ctx, plan.criteria, shopping_strategy=plan.shopping_strategy)`
- 再检索和输出商品卡
- 商品卡不得早于 `criteria_card(shopping_strategy)`
- 最后输出一段 recommendation text
- done 用现有 `awaiting_product_feedback` 即可

不要破坏普通推荐 product-first 顺序。

### 3. 场景化商品 reason

场景化 `_product_card_events` 可以增加可选参数：

```python
shopping_strategy: ShoppingStrategyPayload | None = None
```

如果有 `shopping_strategy.decision_barrier`，优先输出短 reason，例如：

- `fear_wrong_choice` → `低偏好依赖，送礼更稳`
- `price_sensitive` → `预算更可控`
- `fit_uncertainty` → `风险点清楚，适合核对`
- `choice_overload` → `礼物感和实用性更平衡`

仍保留 `reason_atoms` 和 evidence，不编造商品事实。

### 4. final_decision 增加 add_to_cart

在 `_final_decision_event` 中：

- 当 `decision.decision_status == "selected"` 且 `winner_product_id` 非空时，`next_actions` 包含：

```python
QuickActionPayload(
    action_id="add_winner_to_cart",
    label=msg.QA_ADD_TO_CART,
    action="add_to_cart",
)
```

- 保留现有 cheaper / compare action
- 不新增 quick action type

### 5. 可观测字段

如果不大改 trace，可以先在 retrieval trace details 或 audit metadata 中记录最小信息：

- `shopping_strategy.strategy_id`
- `decision_barrier.barrier_type`
- `route`

如果这会扩大改动面，可以先不做，留 TODO 注释即可。

## 小测试

新增或更新小测试即可，不跑全量。

建议覆盖：

1. mock `build_shopping_strategy_plan` 返回 plan，断言场景化 event 顺序：
   - `text_delta` 在 `product_card` 前
   - `criteria_card.shopping_strategy` 非空
   - `product_card` 在该 `criteria_card` 后
2. 普通推荐 mock service 返回 `None`，断言普通 product-first 顺序不变。
3. `_final_decision_event` 在 selected 时包含 `add_to_cart` action。

只跑：

```bash
timeout 180s backend/.venv/bin/python -m pytest backend/tests/test_scenario_strategy_runtime.py backend/tests/test_pipeline.py::test_pipeline_product_deck_has_consistent_deck_id
```

如果目标测试名不同，只跑你新增的小测试和一个普通顺序保护测试。不要跑全量。

## 交付说明

结束时说明：

- 场景化链路接入点
- 普通推荐顺序如何保护
- final_decision 加购 action 是否完成
- 小测试命令和结果
- 是否有需要主控统一验收的风险点

