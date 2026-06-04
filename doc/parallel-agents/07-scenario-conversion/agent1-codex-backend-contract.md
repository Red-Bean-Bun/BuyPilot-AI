# Agent 1 / Codex 提示词：后端契约与类型

你在 `/mnt/disk1/LZJ/project/BuyPilot-AI` 工作。只做后端契约与类型，不做 runtime 集成，不碰 Android。

## 背景

阅读：

- `CLAUDE.md`
- `doc/prd/07-场景化选购助手PRD.md`
- `contracts/sse-events.schema.json`
- `backend/src/types/sse_events.py`

目标是为场景化选购助手提供后端可序列化契约：

- `criteria_card.shopping_strategy`
- `shopping_strategy.decision_barrier`
- P0 不新增 event type
- P0 不新增 quick action type

## Owned files

你主要修改：

- `contracts/sse-events.schema.json`
- `backend/src/types/sse_events.py`
- 可新增一个很小的后端契约测试，例如 `backend/tests/test_shopping_strategy_contract.py`

不要修改：

- `backend/src/runtime/handlers.py`
- `backend/src/services/shopping_strategy.py`
- Android 任何文件

## 具体任务

1. 在 `backend/src/types/sse_events.py` 增加 Pydantic payload：
   - `DecisionBarrierPayload`
   - `SearchStrategyPayload`
   - `PrimaryDirectionPayload`
   - `ShoppingStrategyPayload`

2. 给 `CriteriaCardEvent` 增加：

```python
shopping_strategy: ShoppingStrategyPayload | None = None
```

3. 字段建议：

```python
class DecisionBarrierPayload(BaseModel):
    barrier_type: Literal[
        "fear_wrong_choice",
        "value_uncertainty",
        "fit_uncertainty",
        "trust_uncertainty",
        "price_sensitive",
        "choice_overload",
    ]
    label: str
    reason: str = ""
    conversion_strategy: str = ""
```

```python
class SearchStrategyPayload(BaseModel):
    category: str | None = None
    product_type: str | None = None
    use_scenario: str | None = None
```

```python
class PrimaryDirectionPayload(BaseModel):
    title: str
    summary: str = ""
    why: str = ""
    search_strategy: SearchStrategyPayload = Field(default_factory=SearchStrategyPayload)
    available_in_catalog: bool = False
    supporting_product_count: int = 0
```

```python
class ShoppingStrategyPayload(BaseModel):
    strategy_id: str
    scene_type: Literal["gift", "interest", "usage", "risk_sensitive", "goal_oriented"]
    scene_summary: str = ""
    user_problem: str = ""
    decision_barrier: DecisionBarrierPayload | None = None
    primary_direction: PrimaryDirectionPayload
    avoid_risks: list[str] = Field(default_factory=list)
    assumptions: list[str] = Field(default_factory=list)
    confidence: Literal["low", "medium", "high"] = "medium"
```

4. 同步 `contracts/sse-events.schema.json`：
   - `criteria_card.properties.shopping_strategy` 引用新 `$defs.shopping_strategy_payload`
   - 新增 `decision_barrier_payload`
   - 新增 `search_strategy_payload`
   - 新增 `primary_direction_payload`
   - 新增 `shopping_strategy_payload`

5. 保持 schema 兼容：
   - 不新增 event type
   - 不把 `shopping_strategy` 加入 required
   - 不改已有字段含义
   - 不改 `quick_action_payload.action` enum

## 小测试

新增或更新一个小测试即可，覆盖：

- `CriteriaCardEvent(..., shopping_strategy=...)` 可以 `model_dump_json`
- JSON 中包含 `shopping_strategy.decision_barrier.barrier_type`
- `parse_sse_event` 能解析带 `shopping_strategy` 的 `criteria_card`

只跑你的小测试，例如：

```bash
timeout 180s backend/.venv/bin/python -m pytest backend/tests/test_shopping_strategy_contract.py
```

不要跑全量。

## 交付说明

结束时说明：

- 改了哪些契约字段
- 小测试命令和结果
- 是否有需要 Agent 2/3 注意的类型命名

