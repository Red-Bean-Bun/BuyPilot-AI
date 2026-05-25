import asyncio

import pytest

from src.runtime import pipeline as pipeline_module
from src.runtime.cancel_registry import active_turn_count, cancel_turn
from src.runtime.pipeline import chat_stream
from src.runtime.stages.recommendation import RetrievalResult
from src.types.schemas import ChatStreamRequest, DecisionResult, RecommendationResult
from src.types.sse_events import CriteriaPayload, EvidencePayload, ProductPayload


@pytest.fixture(autouse=True)
async def _seed_products_for_pipeline(seeded_products):
    del seeded_products


@pytest.mark.asyncio
async def test_pipeline_event_order_and_deck_id():
    events = [
        event
        async for event in chat_stream(
            "s1",
            ChatStreamRequest(message="推荐适合油皮的洗面奶，200元以内，日常护肤"),
        )
    ]
    tags = [event.event for event in events]
    assert tags.index("thinking") < tags.index("criteria_card")
    assert tags.index("criteria_card") < tags.index("product_card")
    assert tags.index("product_card") < tags.index("text_delta")
    assert tags.index("product_card") < tags.index("final_decision")
    assert tags[-1] == "done"
    product_deck_ids = {event.deck_id for event in events if event.event == "product_card"}
    assert len(product_deck_ids) == 1
    assert [event.seq for event in events] == sorted(event.seq for event in events)


@pytest.mark.asyncio
async def test_pipeline_clarification_short_circuit():
    events = [event async for event in chat_stream("s2", ChatStreamRequest(message="随便看看"))]
    tags = [event.event for event in events]
    assert "clarification" in tags
    assert "product_card" not in tags
    assert tags[-1] == "done"


@pytest.mark.asyncio
async def test_pipeline_emits_heartbeat_during_slow_stage(monkeypatch):
    monkeypatch.setattr(pipeline_module, "HEARTBEAT_INTERVAL_SECONDS", 0.01)

    async def slow_run_criteria(session_id, body, intent):
        await asyncio.sleep(0.035)
        return CriteriaPayload(criteria_id="c_slow", category="美妆护肤", summary="油性肌肤")

    async def fast_retrieval(criteria, feedback=None):
        return RetrievalResult(products=[], evidence_by_product={})

    async def fast_recommendation_text(criteria, products, evidence_by_product=None):
        return RecommendationResult(text_chunks=["已生成推荐。"], products=products)

    async def fast_decision(criteria, products, evidence_by_product=None):
        return DecisionResult(winner_product_id="", summary="暂无匹配商品。")

    monkeypatch.setattr(pipeline_module, "run_criteria", slow_run_criteria)
    monkeypatch.setattr(pipeline_module, "run_retrieval", fast_retrieval)
    monkeypatch.setattr(pipeline_module, "run_recommendation_text", fast_recommendation_text)
    monkeypatch.setattr(pipeline_module, "run_decision", fast_decision)

    events = [
        event
        async for event in pipeline_module.chat_stream(
            "s_heartbeat",
            ChatStreamRequest(message="推荐适合油皮的洗面奶，200元以内，日常护肤"),
        )
    ]

    criteria_thinking = [event for event in events if event.event == "thinking" and event.stage == "criteria"]
    assert len(criteria_thinking) >= 2
    assert [event.seq for event in events] == sorted(event.seq for event in events)


@pytest.mark.asyncio
async def test_pipeline_emits_product_card_before_slow_recommendation_text(monkeypatch):
    monkeypatch.setattr(pipeline_module, "HEARTBEAT_INTERVAL_SECONDS", 0.01)
    product = ProductPayload(
        product_id="p_test_001",
        name="测试洁面乳",
        category="美妆护肤",
        price=99,
        skin_type_match=["油性"],
    )

    async def fast_retrieval(criteria, feedback=None):
        return RetrievalResult(
            products=[product],
            evidence_by_product={
                product.product_id: [
                    EvidencePayload(source_type="product_chunk", source_id="test_chunk", snippet="测试证据")
                ]
            },
        )

    async def slow_recommendation_text(criteria, products, evidence_by_product=None):
        await asyncio.sleep(0.035)
        return RecommendationResult(text_chunks=["这款更适合油皮日常使用。"], products=products)

    async def fast_decision(criteria, products, evidence_by_product=None):
        return DecisionResult(winner_product_id=product.product_id, summary="优先选测试洁面乳。")

    monkeypatch.setattr(pipeline_module, "run_retrieval", fast_retrieval)
    monkeypatch.setattr(pipeline_module, "run_recommendation_text", slow_recommendation_text)
    monkeypatch.setattr(pipeline_module, "run_decision", fast_decision)

    events = [
        event
        async for event in pipeline_module.chat_stream(
            "s_fast_cards",
            ChatStreamRequest(message="推荐适合油皮的洗面奶，200元以内，日常护肤"),
        )
    ]

    tags = [event.event for event in events]
    assert tags.index("product_card") < tags.index("text_delta")
    assert tags.index("text_delta") < tags.index("final_decision")
    assert any(event.event == "thinking" and event.stage == "recommending" for event in events)


@pytest.mark.asyncio
async def test_pipeline_honors_client_turn_cancellation():
    events = []
    async for event in chat_stream(
        "s_cancel",
        ChatStreamRequest(
            message="推荐适合油皮的洗面奶，200元以内，日常护肤",
            client_turn_id="turn_cancel_test",
        ),
    ):
        events.append(event)
        if event.event == "thinking":
            assert cancel_turn("s_cancel", "turn_cancel_test") is True

    assert [event.event for event in events] == ["thinking", "done"]
    assert active_turn_count() == 0


@pytest.mark.asyncio
async def test_pipeline_error_message_is_sanitized(monkeypatch):
    async def exploding_intent(session_id, body):
        raise RuntimeError("database password leaked")

    monkeypatch.setattr(pipeline_module, "run_intent", exploding_intent)

    events = [
        event
        async for event in chat_stream(
            "s_error",
            ChatStreamRequest(message="推荐适合油皮的洗面奶，200元以内，日常护肤"),
        )
    ]

    errors = [event for event in events if event.event == "error"]
    assert len(errors) == 1
    assert "database password leaked" not in errors[0].message
    assert "trace_id=turn_" in errors[0].message
    assert events[-1].event == "done"
