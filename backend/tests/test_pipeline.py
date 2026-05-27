import asyncio

import pytest

from src.runtime import handlers as handlers_module
from src.runtime import pipeline as pipeline_module
from src.runtime import streaming as streaming_module
from src.runtime.cancel_registry import active_turn_count, cancel_turn
from src.runtime.pipeline import chat_stream
from src.runtime.stages.recommendation import RetrievalResult
from src.types.schemas import ChatStreamRequest, DecisionResult, IntentResult
from src.types.sse_events import Constraints, CriteriaPayload, EvidencePayload, ProductPayload


@pytest.fixture(autouse=True)
def _stub_pipeline_io(monkeypatch):
    product = ProductPayload(
        product_id="p_test_001",
        name="测试洁面乳",
        category="美妆护肤",
        price=99,
        skin_type_match=["油性"],
    )
    evidence = [EvidencePayload(source_type="product_chunk", source_id="test_chunk", snippet="测试证据")]
    state = {"criteria": None, "product_ids": [], "deck_id": None}

    async def noop(*args, **kwargs):
        return None

    async def save_turn(session_id, criteria, product_ids, message_id=None, deck_id=None, **kwargs):
        del session_id, message_id, kwargs
        state["criteria"] = criteria
        state["product_ids"] = list(product_ids)
        if deck_id:
            state["deck_id"] = deck_id
        return "conv_test"

    async def previous_criteria(session_id):
        del session_id
        return state["criteria"]

    async def previous_products(session_id):
        del session_id
        return list(state["product_ids"])

    async def previous_deck(session_id):
        del session_id
        return state["deck_id"]

    async def no_feedback(session_id, deck_id=None):
        del session_id, deck_id
        return {"avoid_products": [], "avoid_traits": [], "prefer_traits": []}

    async def not_cancelled(session_id, turn_id):
        del session_id, turn_id
        return False

    async def fixed_evidence(product_arg):
        del product_arg
        return evidence

    async def default_intent(session_id, body):
        del session_id
        if body.message.strip() in {"继续", "确认", "没问题", "可以", "开始推荐", "收敛"}:
            return IntentResult(intent="continue", confidence=0.95)
        return IntentResult(
            intent="recommend",
            confidence=0.95,
            category="美妆护肤",
            extracted_constraints={"product_type": "洁面", "skin_type": "油性", "budget_max": 200},
        )

    async def no_budget_intercept(session_id, body):
        del session_id
        return body, None

    async def default_criteria(session_id, body, intent):
        del session_id, body, intent
        return CriteriaPayload(
            criteria_id="c_test",
            category="美妆护肤",
            summary="美妆护肤，油性肌肤，200元内，洁面",
            chips=["美妆护肤", "油性肌肤", "200元内", "洁面"],
            constraints=Constraints(product_type="洁面", skin_type="油性", budget_max=200),
            field_sources={
                "category": "user",
                "constraints.product_type": "user",
                "constraints.skin_type": "user",
                "constraints.budget_max": "user",
            },
        )

    async def default_retrieval(criteria, top_n=5, feedback=None):
        del criteria, top_n, feedback
        return RetrievalResult(products=[product], evidence_by_product={product.product_id: evidence})

    async def default_decision(criteria, products, evidence_by_product=None):
        del criteria, evidence_by_product
        return DecisionResult(winner_product_id=products[0].product_id, summary="优先选测试洁面乳。")

    async def default_stream(criteria, products, evidence_by_product=None):
        del criteria, products, evidence_by_product
        yield "这款更适合油皮日常使用。"

    monkeypatch.setattr(pipeline_module, "register_chat_turn", noop)
    monkeypatch.setattr(pipeline_module, "clear_chat_turn", noop)
    monkeypatch.setattr(pipeline_module, "record_audit_event", noop)
    monkeypatch.setattr(pipeline_module, "get_previous_criteria", previous_criteria)
    monkeypatch.setattr(pipeline_module, "maybe_intercept_budget_patch", no_budget_intercept)
    monkeypatch.setattr(pipeline_module, "run_intent", default_intent)
    monkeypatch.setattr(pipeline_module, "run_criteria", default_criteria)
    monkeypatch.setattr(pipeline_module, "run_retrieval", default_retrieval)
    monkeypatch.setattr(pipeline_module, "run_recommendation_text_stream", default_stream)
    monkeypatch.setattr(pipeline_module, "run_decision", default_decision)

    monkeypatch.setattr(handlers_module, "record_audit_event", noop)
    monkeypatch.setattr(handlers_module, "record_retrieval_trace", noop)
    monkeypatch.setattr(handlers_module, "record_evidence_links", noop)
    monkeypatch.setattr(handlers_module, "save_recommendation_turn", save_turn)
    monkeypatch.setattr(handlers_module, "get_previous_criteria", previous_criteria)
    monkeypatch.setattr(handlers_module, "get_previous_product_ids", previous_products)
    monkeypatch.setattr(handlers_module, "get_previous_deck_id", previous_deck)
    monkeypatch.setattr(handlers_module, "get_feedback_context", no_feedback)
    monkeypatch.setattr(handlers_module, "get_evidence", fixed_evidence)
    monkeypatch.setattr(handlers_module, "get_product", lambda product_id: product if product_id == product.product_id else None)
    monkeypatch.setattr(streaming_module, "is_chat_turn_cancellation_requested", not_cancelled)


@pytest.mark.asyncio
async def test_pipeline_waits_for_criteria_confirmation_by_default():
    events = [
        event
        async for event in chat_stream(
            "s1",
            ChatStreamRequest(message="推荐适合油皮的洗面奶，200元以内，日常护肤"),
        )
    ]
    tags = [event.event for event in events]
    assert tags.index("thinking") < tags.index("criteria_card")
    assert "product_card" not in tags
    assert "final_decision" not in tags
    assert events[-1].event == "done"
    assert events[-1].finish_reason == "awaiting_criteria_confirmation"
    assert [event.seq for event in events] == sorted(event.seq for event in events)


@pytest.mark.asyncio
async def test_pipeline_auto_run_emits_product_deck_without_final_decision():
    events = [
        event
        async for event in chat_stream(
            "s_deck",
            ChatStreamRequest(message="推荐适合油皮的洗面奶，200元以内，日常护肤", auto_run=True),
        )
    ]
    tags = [event.event for event in events]
    assert tags.index("criteria_card") < tags.index("product_card")
    assert "final_decision" not in tags
    assert events[-1].finish_reason == "awaiting_product_feedback"
    product_deck_ids = {event.deck_id for event in events if event.event == "product_card"}
    assert len(product_deck_ids) == 1
    assert events[-1].deck_id in product_deck_ids


@pytest.mark.asyncio
async def test_pipeline_continue_after_deck_emits_final_decision():
    _ = [
        event
        async for event in chat_stream(
            "s_decision",
            ChatStreamRequest(message="推荐适合油皮的洗面奶，200元以内，日常护肤", auto_run=True),
        )
    ]
    events = [event async for event in chat_stream("s_decision", ChatStreamRequest(message="继续"))]
    tags = [event.event for event in events]
    assert "criteria_card" not in tags
    assert "product_card" not in tags
    assert "final_decision" in tags
    assert events[-1].finish_reason == "completed"


@pytest.mark.asyncio
async def test_pipeline_emits_heartbeat_during_slow_stage(monkeypatch):
    monkeypatch.setattr(pipeline_module, "HEARTBEAT_INTERVAL_SECONDS", 0.01)

    async def slow_run_criteria(session_id, body, intent):
        del session_id, body, intent
        await asyncio.sleep(0.035)
        return CriteriaPayload(criteria_id="c_slow", category="美妆护肤", summary="油性肌肤")

    monkeypatch.setattr(pipeline_module, "run_criteria", slow_run_criteria)

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
    assert events[-1].finish_reason == "cancelled"
    assert active_turn_count() == 0


@pytest.mark.asyncio
async def test_pipeline_error_message_is_sanitized(monkeypatch):
    async def exploding_intent(session_id, body):
        del session_id, body
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
    assert events[-1].finish_reason == "error"
