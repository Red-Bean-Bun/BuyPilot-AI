# Agent 2 / Claude Code 提示词：ShoppingStrategyService

你在 `/mnt/disk1/LZJ/project/BuyPilot-AI` 工作。只做后端 service 层，不接 runtime，不碰 Android。

## 背景

阅读：

- `CLAUDE.md`
- `doc/prd/07-场景化选购助手PRD.md`
- `backend/src/types/sse_events.py`
- `backend/src/types/schemas.py`
- `backend/src/runtime/stages/criteria.py`
- `backend/src/services/retriever.py`

目标是实现 P0 场景化策略服务：

- 识别 gift / interest 两类场景
- 输出 `decision_barrier`
- 生成一个可落地 `primary_direction`
- 做轻量可行性检查
- 显式约束不被策略覆盖

## Owned files

你主要修改/新增：

- `backend/src/services/shopping_strategy.py`
- `backend/prompts/shopping_strategy.md`
- `backend/tests/test_shopping_strategy.py`

不要修改：

- `backend/src/runtime/handlers.py`
- `contracts/sse-events.schema.json`
- Android 任何文件

如果 Agent 1 的类型尚未合入，你可以先按它的提示词中定义的类型写 import；不要重复定义同名 payload 到别的文件。

## 稳定接口

请在 `backend/src/services/shopping_strategy.py` 提供这个接口，供 Agent 3 接入：

```python
class ShoppingStrategyPlan(BaseModel):
    route: Literal["scenario_strategy", "scenario_filter"]
    scene_judgement_text: str
    criteria: CriteriaPayload
    shopping_strategy: ShoppingStrategyPayload
    reason_hint: str | None = None


async def build_shopping_strategy_plan(
    body: ChatStreamRequest,
    intent: IntentResult,
    criteria: CriteriaPayload,
    *,
    retrieval_probe: Callable[[CriteriaPayload], Awaitable[RetrievalResult]] | None = None,
) -> ShoppingStrategyPlan | None:
    ...
```

`retrieval_probe` 由 runtime 注入，用于小范围探测候选方向是否有商品。你不需要在 service 里直接依赖 runtime。

## P0 规则

1. 只支持 `gift` / `interest`。
2. 当前处于非推荐意图时返回 `None`。
3. 有明确商品类型/预算/品牌/硬参数时必须保留。策略只能补用户没说的字段。
4. 普通筛选推荐不输出策略：
   - "2000 内蓝牙耳机有哪些" → `None`
5. 场景化推荐输出策略：
   - "男朋友生日，喜欢电子产品" → 数码电子 / 真无线耳机方向
   - "男朋友喜欢足球，生日送什么" → 服饰运动 / 当前库可支撑的运动休闲方向
   - "送妈妈一款护肤品，不知道怎么选" → 美妆护肤 / 温和低风险方向
6. 如果 `retrieval_probe` 返回 0 个商品，方向为 unsupported，最终返回 `None` 或降级到更宽 category hint 再探测。
7. P0 不调用真实 LLM 也可以。可以用确定性规则实现，`backend/prompts/shopping_strategy.md` 作为后续 LLM 化提示词。

## 可行性检查

P0 轻量评分：

```text
feasibility_score_p0 = 0.50 * retrieval_coverage
                     + 0.30 * category_match
                     + 0.20 * constraint_safety
```

最小实现可以只要求：

- count >= 1 才返回 plan
- `primary_direction.available_in_catalog = True`
- `supporting_product_count = count`
- 显式硬约束冲突时返回 `None`

## decision_barrier 建议映射

- 送礼 + 不确定：`fear_wrong_choice`
- 明确预算/更便宜：`price_sensitive`
- 敏感肌/尺码/适配：`fit_uncertainty`
- 候选多/怎么选：`choice_overload`
- 问靠谱不靠谱/依据：`trust_uncertainty`

## 小测试

新增 `backend/tests/test_shopping_strategy.py`，只测 service，不跑 pipeline。

建议覆盖：

- 生日 + 电子产品返回 `scene_type == "gift"` 和 `barrier_type == "fear_wrong_choice"`
- "2000 内蓝牙耳机有哪些" 返回 `None`
- "送男朋友一个 2000 内蓝牙耳机，怎么选" 返回 plan 且 `criteria.constraints.product_type` 保留蓝牙耳机/真无线耳机等显式类型
- `retrieval_probe` 返回空时不返回 unsupported 方向

只跑：

```bash
timeout 180s backend/.venv/bin/python -m pytest backend/tests/test_shopping_strategy.py
```

不要跑全量。

## 交付说明

结束时说明：

- service 的接口是否与上文一致
- P0 确定性规则覆盖哪些 demo
- 小测试命令和结果
- Agent 3 接入时需要注意的行为

