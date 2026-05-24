import asyncio
import uuid
from typing import AsyncGenerator

from src.config.tuning import CHEAPER_BUDGET_FALLBACK_MAX, DEFAULT_CART_PRODUCT_ID
from src.types.sse_events import (
    AlternativePayload,
    CartActionEvent,
    Constraints,
    CriteriaCardEvent,
    CriteriaPayload,
    DoneEvent,
    EventSeq,
    EvidencePayload,
    FinalDecisionEvent,
    ProductCardEvent,
    ProductPayload,
    QuickActionPayload,
    SSEEventBase,
    TextDeltaEvent,
    ThinkingEvent,
    now_ms,
)

SSE_CHUNK_DELAY = 0.1

MOCK_PRODUCTS = [
    ProductPayload(
        product_id=DEFAULT_CART_PRODUCT_ID,
        name="珊珂洗颜专科绵润泡沫洁面乳",
        price=52.0,
        currency="CNY",
        image_url="https://example.com/p_beauty_011.jpg",
        category="美妆护肤",
        sub_category="洁面",
        brand="珊珂",
        skin_type_match=["油性", "混合性"],
        ingredient_tags=["氨基酸表活", "绵润泡沫"],
        ingredient_avoid=[],
        use_scenario="日常洁面",
    ),
    ProductPayload(
        product_id="p_beauty_007",
        name="薇诺娜舒敏保湿特护霜",
        price=268.0,
        currency="CNY",
        image_url="https://example.com/p_beauty_007.jpg",
        category="美妆护肤",
        sub_category="面霜",
        brand="薇诺娜",
        skin_type_match=["敏感", "干性"],
        ingredient_tags=["马齿苋提取物", "透明质酸"],
        ingredient_avoid=["不含酒精", "不含香精"],
        use_scenario="日常修护",
    ),
    ProductPayload(
        product_id="p_beauty_006",
        name="巴黎欧莱雅新多重防护隔离露",
        price=170.0,
        currency="CNY",
        image_url="https://example.com/p_beauty_006.jpg",
        category="美妆护肤",
        sub_category="防晒",
        brand="欧莱雅",
        skin_type_match=["油性", "混合性", "通用"],
        ingredient_tags=["多重防护", "水感轻薄"],
        ingredient_avoid=[],
        use_scenario="户外防晒",
    ),
]


async def mock_pipeline(session_id: str, user_input: str) -> AsyncGenerator[SSEEventBase, None]:
    turn_id = f"turn_{uuid.uuid4().hex[:8]}"
    deck_id = f"deck_{turn_id}"
    seq = EventSeq(turn_id)

    await asyncio.sleep(SSE_CHUNK_DELAY)
    yield ThinkingEvent(
        session_id=session_id,
        turn_id=turn_id,
        seq=seq.next(),
        event_id=seq.event_id(),
        node_id=f"thinking_{turn_id}",
        created_at_ms=now_ms(),
        stage="understanding",
        message="正在理解肤质、预算与使用场景",
    )

    await asyncio.sleep(SSE_CHUNK_DELAY)
    yield CriteriaCardEvent(
        session_id=session_id,
        turn_id=turn_id,
        seq=seq.next(),
        event_id=seq.event_id(),
        node_id="criteria_c_demo_001",
        created_at_ms=now_ms(),
        editable=True,
        criteria=CriteriaPayload(
            criteria_id="c_demo_001",
            category="美妆护肤",
            summary="油性肌肤日常洁面，200元以内",
            chips=["油性肌肤", "200元内", "日常护肤", "洁面类"],
            constraints=Constraints(
                skin_type="油性",
                budget_max=200,
                ingredient_avoid=[],
                use_scenario="日常护肤",
            ),
        ),
        quick_actions=[
            QuickActionPayload(
                action_id="budget_low",
                label="预算压低",
                action="criteria_patch",
                criteria_patch={"constraints": {"budget_max": CHEAPER_BUDGET_FALLBACK_MAX}},
            ),
            QuickActionPayload(
                action_id="sensitive_skin",
                label="敏感肌适用",
                action="criteria_patch",
                criteria_patch={"constraints": {"skin_type": "敏感"}},
            ),
            QuickActionPayload(
                action_id="no_alcohol",
                label="不要含酒精",
                action="criteria_patch",
                criteria_patch={"constraints": {"ingredient_avoid": ["酒精"]}},
            ),
        ],
    )

    await asyncio.sleep(SSE_CHUNK_DELAY)
    yield TextDeltaEvent(
        session_id=session_id,
        turn_id=turn_id,
        seq=seq.next(),
        event_id=seq.event_id(),
        node_id=f"ai_text_{turn_id}",
        created_at_ms=now_ms(),
        message_id=f"msg_{turn_id}",
        delta="我先按油性肌肤、200元以内、日常洁面来筛选。",
        done=False,
    )

    await asyncio.sleep(SSE_CHUNK_DELAY)
    yield TextDeltaEvent(
        session_id=session_id,
        turn_id=turn_id,
        seq=seq.next(),
        event_id=seq.event_id(),
        node_id=f"ai_text_{turn_id}",
        created_at_ms=now_ms(),
        message_id=f"msg_{turn_id}",
        delta="下面给你几个适合油皮的洁面推荐。",
        done=True,
    )

    await asyncio.sleep(SSE_CHUNK_DELAY)
    yield ThinkingEvent(
        session_id=session_id,
        turn_id=turn_id,
        seq=seq.next(),
        event_id=seq.event_id(),
        node_id=f"thinking_{turn_id}",
        created_at_ms=now_ms(),
        stage="searching",
        message="找到3个匹配商品...",
    )

    for i, prod in enumerate(MOCK_PRODUCTS, start=1):
        await asyncio.sleep(SSE_CHUNK_DELAY)
        yield ProductCardEvent(
            session_id=session_id,
            turn_id=turn_id,
            seq=seq.next(),
            event_id=seq.event_id(),
            node_id=f"product_{prod.product_id}",
            deck_id=deck_id,
            created_at_ms=now_ms(),
            rank=i,
            product=prod,
            reason=f"{prod.skin_type_match[0]}适用，{prod.ingredient_tags[0]}效果好。",
            risk_notes=[],
            evidence=[
                EvidencePayload(
                    source_type="product_chunk",
                    snippet=f"{prod.skin_type_match} 适用，{prod.use_scenario}。",
                    source_id=f"chunk_{prod.product_id}",
                )
            ],
            actions=[
                QuickActionPayload(
                    action_id="show_evidence",
                    label="看证据",
                    action="open_evidence",
                ),
                QuickActionPayload(
                    action_id="add_to_cart",
                    label="加入购物车",
                    action="add_to_cart",
                ),
                QuickActionPayload(
                    action_id="dislike_product",
                    label="不喜欢这个",
                    action="feedback",
                    feedback_type="not_interested",
                ),
            ],
        )

    await asyncio.sleep(SSE_CHUNK_DELAY)
    yield CartActionEvent(
        session_id=session_id,
        turn_id=turn_id,
        seq=seq.next(),
        event_id=seq.event_id(),
        node_id=f"cart_{turn_id}",
        created_at_ms=now_ms(),
        action="add",
        product_id=DEFAULT_CART_PRODUCT_ID,
        quantity=1,
        status="success",
    )

    await asyncio.sleep(SSE_CHUNK_DELAY)
    yield FinalDecisionEvent(
        session_id=session_id,
        turn_id=turn_id,
        seq=seq.next(),
        event_id=seq.event_id(),
        node_id=f"decision_{turn_id}",
        created_at_ms=now_ms(),
        winner_product_id=MOCK_PRODUCTS[0].product_id,
        summary="如果你更看重控油效果和性价比，优先选珊珂洗颜专科洁面乳。",
        why=["油性适用", "52元性价比高", "控油效果好", "日常洁面友好"],
        not_for=["若你希望避免含香精产品，建议考虑薇诺娜"],
        alternatives=[
            AlternativePayload(product_id="p_beauty_007", name="薇诺娜舒敏保湿特护霜"),
            AlternativePayload(product_id="p_beauty_006", name="巴黎欧莱雅新多重防护隔离露"),
        ],
        next_actions=[
            QuickActionPayload(
                action_id="cheaper",
                label="再便宜一点",
                action="criteria_patch",
                criteria_patch={"constraints": {"budget_max": CHEAPER_BUDGET_FALLBACK_MAX}},
            ),
            QuickActionPayload(
                action_id="no_alcohol",
                label="不要含酒精",
                action="criteria_patch",
                criteria_patch={"constraints": {"ingredient_avoid": ["酒精"]}},
            ),
            QuickActionPayload(
                action_id="compare",
                label="加入对比",
                action="compare",
            ),
        ],
    )

    await asyncio.sleep(SSE_CHUNK_DELAY)
    yield DoneEvent(
        session_id=session_id,
        turn_id=turn_id,
        seq=seq.next(),
        event_id=seq.event_id(),
        node_id=f"done_{turn_id}",
        created_at_ms=now_ms(),
    )
