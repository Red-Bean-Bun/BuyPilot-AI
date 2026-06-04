import json

import jsonschema

from src.types.sse_events import (
    CriteriaCardEvent,
    CriteriaPayload,
    DecisionBarrierPayload,
    PrimaryDirectionPayload,
    SearchStrategyPayload,
    ShoppingStrategyPayload,
    parse_sse_event,
)


def test_criteria_card_shopping_strategy_serializes_parses_and_validates(sse_schema):
    event = CriteriaCardEvent(
        session_id="s1",
        turn_id="turn_strategy",
        seq=1,
        event_id="turn_strategy:1",
        node_id="criteria_scene_001",
        criteria=CriteriaPayload(
            criteria_id="criteria_scene_001",
            category="数码电子",
            summary="送男朋友生日礼物，对方喜欢电子产品",
            chips=["送礼", "电子产品"],
        ),
        shopping_strategy=ShoppingStrategyPayload(
            strategy_id="scene_001",
            scene_type="gift",
            scene_summary="送男朋友生日礼物，对方喜欢电子产品",
            user_problem="用户不确定这个场景下送什么更体面、更不容易踩雷",
            decision_barrier=DecisionBarrierPayload(
                barrier_type="fear_wrong_choice",
                label="怕送错、怕不够体面",
                reason="对方懂电子产品，核心设备容易踩型号和品牌偏好",
                conversion_strategy="先推荐低偏好依赖、礼物感更强的小件",
            ),
            primary_direction=PrimaryDirectionPayload(
                title="低踩雷的黑科技小件",
                summary="优先考虑音频配件、智能穿戴或轻量数码配件",
                why="有新鲜感，不强依赖具体型号偏好，也更适合生日礼物",
                search_strategy=SearchStrategyPayload(
                    category="数码电子",
                    product_type="真无线耳机",
                    use_scenario="日常使用",
                ),
                available_in_catalog=True,
                supporting_product_count=2,
            ),
            avoid_risks=["不要盲买手机、电脑这类强型号偏好的大件"],
            assumptions=["暂时不知道预算"],
            confidence="medium",
        ),
    )

    data_json = event.model_dump_json()
    data = json.loads(data_json)
    assert data["shopping_strategy"]["decision_barrier"]["barrier_type"] == "fear_wrong_choice"
    jsonschema.validate(data, sse_schema)

    parsed = parse_sse_event(data_json)
    assert isinstance(parsed, CriteriaCardEvent)
    assert parsed.shopping_strategy is not None
    assert parsed.shopping_strategy.decision_barrier is not None
    assert parsed.shopping_strategy.decision_barrier.barrier_type == "fear_wrong_choice"
