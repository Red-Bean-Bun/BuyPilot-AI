import asyncio

import pytest

from src.runtime import handlers as handlers_module
from src.runtime import pipeline as pipeline_module
from src.runtime import streaming as streaming_module
from src.runtime.cancel_registry import active_turn_count, cancel_turn
from src.runtime.handlers import _no_match_followup_text
from src.runtime.pipeline import _should_merge_previous_context, chat_stream
from src.runtime.stages.recommendation import RetrievalResult
from src.services.fallbacks import get_fallback_events
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

    async def default_retrieval(criteria, top_n=5, feedback=None, **kwargs):
        del criteria, top_n, feedback, kwargs
        return RetrievalResult(products=[product], evidence_by_product={product.product_id: evidence})

    async def default_decision(criteria, products, evidence_by_product=None):
        del criteria, evidence_by_product
        return DecisionResult(winner_product_id=products[0].product_id, summary="优先选测试洁面乳。")

    async def default_stream(criteria, products, evidence_by_product=None):
        del criteria, products, evidence_by_product
        yield "这款更适合油皮日常使用。"

    async def default_image_embedding(image_url):
        del image_url
        return None

    monkeypatch.setattr(pipeline_module, "register_chat_turn", noop)
    monkeypatch.setattr(pipeline_module, "clear_chat_turn", noop)
    monkeypatch.setattr(pipeline_module, "record_audit_event", noop)
    monkeypatch.setattr(pipeline_module, "get_previous_criteria", previous_criteria)
    monkeypatch.setattr(pipeline_module, "maybe_intercept_budget_patch", no_budget_intercept)
    monkeypatch.setattr(pipeline_module, "run_intent", default_intent)
    monkeypatch.setattr(pipeline_module, "run_image_embedding", default_image_embedding)
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
    monkeypatch.setattr(
        handlers_module, "get_product", lambda product_id: product if product_id == product.product_id else None
    )
    monkeypatch.setattr(streaming_module, "is_chat_turn_cancellation_requested", not_cancelled)


@pytest.mark.asyncio
async def test_pipeline_product_first_default_flow():
    """Product-first: default request emits product_card + criteria_card, done is awaiting_product_feedback or completed."""
    events = [
        event
        async for event in chat_stream(
            "s1",
            ChatStreamRequest(message="推荐适合油皮的洗面奶，200元以内，日常护肤"),
        )
    ]
    tags = [event.event for event in events]
    assert "thinking" in tags
    assert "product_card" in tags
    assert "criteria_card" in tags
    assert events[-1].event == "done"
    assert events[-1].finish_reason in ("awaiting_product_feedback", "completed")
    assert [event.seq for event in events] == sorted(event.seq for event in events)


def test_no_match_text_for_category_only_asks_for_product_type():
    criteria = CriteriaPayload(category="数码电子", constraints=Constraints())

    text = _no_match_followup_text(criteria)

    assert "手机、耳机、平板、笔记本电脑" in text
    assert "放宽预算" not in text
    assert "排除条件" not in text


def test_no_match_text_with_budget_mentions_budget():
    criteria = CriteriaPayload(category="数码电子", constraints=Constraints(budget_max=500))

    text = _no_match_followup_text(criteria)

    assert "放宽预算" in text


@pytest.mark.asyncio
async def test_pipeline_product_deck_has_consistent_deck_id():
    """Product-first: product_card before criteria_card, deck_id consistent."""
    events = [
        event
        async for event in chat_stream(
            "s_deck",
            ChatStreamRequest(message="推荐适合油皮的洗面奶，200元以内，日常护肤"),
        )
    ]
    tags = [event.event for event in events]
    assert tags.index("product_card") < tags.index("criteria_card")
    assert events[-1].finish_reason in ("awaiting_product_feedback", "completed")
    product_deck_ids = {event.deck_id for event in events if event.event == "product_card"}
    assert len(product_deck_ids) == 1
    if events[-1].deck_id is not None:
        assert events[-1].deck_id in product_deck_ids


@pytest.mark.asyncio
async def test_pipeline_multi_product_first_turn_waits_for_convergence(monkeypatch):
    product_a = ProductPayload(product_id="p_initial_a", name="候选A", category="美妆护肤", price=99)
    product_b = ProductPayload(product_id="p_initial_b", name="候选B", category="美妆护肤", price=109)
    evidence = [EvidencePayload(source_type="product_chunk", source_id="initial_chunk", snippet="初步证据")]

    async def two_product_retrieval(criteria, top_n=5, feedback=None, **kwargs):
        del criteria, top_n, feedback
        return RetrievalResult(
            products=[product_a, product_b],
            evidence_by_product={product_a.product_id: evidence, product_b.product_id: evidence},
        )

    monkeypatch.setattr(pipeline_module, "run_retrieval", two_product_retrieval)

    events = [event async for event in chat_stream("s_initial_decision", ChatStreamRequest(message="推荐洗面奶"))]
    tags = [event.event for event in events]
    decisions = [event for event in events if event.event == "final_decision"]

    assert "product_card" in tags
    assert "criteria_card" in tags
    assert tags.index("product_card") < tags.index("criteria_card")
    assert decisions == []
    assert events[-1].finish_reason == "awaiting_product_feedback"


@pytest.mark.asyncio
async def test_pipeline_records_fallback_when_recommendation_stream_fails(monkeypatch):
    product_a = ProductPayload(product_id="p_stream_a", name="候选A", category="美妆护肤", price=99)
    product_b = ProductPayload(product_id="p_stream_b", name="候选B", category="美妆护肤", price=109)
    evidence = [EvidencePayload(source_type="product_chunk", source_id="stream_chunk", snippet="流式证据")]

    async def two_product_retrieval(criteria, top_n=5, feedback=None, **kwargs):
        del criteria, top_n, feedback
        return RetrievalResult(
            products=[product_a, product_b],
            evidence_by_product={product_a.product_id: evidence, product_b.product_id: evidence},
        )

    async def failing_stream(criteria, products, evidence_by_product=None):
        del criteria, products, evidence_by_product
        raise RuntimeError("stream failed")
        yield ""

    monkeypatch.setattr(pipeline_module, "run_retrieval", two_product_retrieval)
    monkeypatch.setattr(pipeline_module, "run_recommendation_text_stream", failing_stream)

    events = [event async for event in chat_stream("s_stream_fallback", ChatStreamRequest(message="推荐洗面奶"))]
    fallbacks = get_fallback_events()

    assert "error" not in [event.event for event in events]
    assert any(
        item["component"] == "llm.generate_recommendation_stream"
        and item["reason"] == "stream_text_failed"
        and item["error_type"] == "RuntimeError"
        for item in fallbacks
    )


@pytest.mark.asyncio
async def test_pipeline_continue_after_deck_emits_final_decision():
    _ = [
        event
        async for event in chat_stream(
            "s_decision",
            ChatStreamRequest(message="推荐适合油皮的洗面奶，200元以内，日常护肤"),
        )
    ]
    events = [event async for event in chat_stream("s_decision", ChatStreamRequest(message="继续"))]
    tags = [event.event for event in events]
    assert "criteria_card" not in tags
    assert "product_card" not in tags
    assert "final_decision" in tags
    assert events[-1].finish_reason == "completed"


@pytest.mark.asyncio
async def test_pipeline_locks_scored_winner_before_decision_explanation(monkeypatch):
    product_a = ProductPayload(product_id="p_lock_a", name="算法首选", category="美妆护肤", price=88)
    product_b = ProductPayload(product_id="p_lock_b", name="LLM偏好", category="美妆护肤", price=89)
    evidence = [EvidencePayload(source_type="product_chunk", source_id="lock_chunk", snippet="锁定证据")]
    captured: dict[str, object] = {}

    async def two_product_retrieval(criteria, top_n=5, feedback=None, **kwargs):
        del criteria, top_n, feedback
        return RetrievalResult(
            products=[product_a, product_b],
            evidence_by_product={product_a.product_id: evidence, product_b.product_id: evidence},
        )

    async def locked_decision(
        criteria,
        products,
        evidence_by_product=None,
        *,
        locked_winner_product_id=None,
        score_breakdown=None,
    ):
        del criteria, products, evidence_by_product
        captured["locked_winner_product_id"] = locked_winner_product_id
        captured["score_breakdown"] = score_breakdown
        return DecisionResult(winner_product_id="p_lock_b", summary="解释锁定商品。")

    monkeypatch.setattr(pipeline_module, "run_retrieval", two_product_retrieval)
    monkeypatch.setattr(pipeline_module, "run_decision", locked_decision)
    monkeypatch.setattr(
        handlers_module,
        "get_product",
        lambda product_id: {product_a.product_id: product_a, product_b.product_id: product_b}.get(product_id),
    )

    _ = [event async for event in chat_stream("s_locked_decision", ChatStreamRequest(message="推荐适合油皮的洗面奶"))]
    events = [event async for event in chat_stream("s_locked_decision", ChatStreamRequest(message="继续"))]
    decision = next(event for event in events if event.event == "final_decision")

    assert captured["locked_winner_product_id"] == product_a.product_id
    assert captured["score_breakdown"]
    assert decision.winner_product_id == product_a.product_id


@pytest.mark.asyncio
async def test_pipeline_replace_deck_excludes_previous_products(monkeypatch):
    product_a = ProductPayload(product_id="p_replace_a", name="上一组A", category="美妆护肤", price=88)
    product_b = ProductPayload(product_id="p_replace_b", name="上一组B", category="美妆护肤", price=89)
    product_c = ProductPayload(product_id="p_replace_c", name="新一组C", category="美妆护肤", price=90)
    evidence = [EvidencePayload(source_type="product_chunk", source_id="replace_chunk", snippet="换组证据")]
    retrieval_feedbacks: list[dict | None] = []

    async def replacement_retrieval(criteria, top_n=5, feedback=None, **kwargs):
        del criteria, top_n
        retrieval_feedbacks.append(feedback)
        avoided = set((feedback or {}).get("avoid_products", []))
        products = [product_c] if {product_a.product_id, product_b.product_id} <= avoided else [product_a, product_b]
        return RetrievalResult(
            products=products,
            evidence_by_product={product.product_id: evidence for product in products},
        )

    monkeypatch.setattr(pipeline_module, "run_retrieval", replacement_retrieval)

    first = [event async for event in chat_stream("s_replace", ChatStreamRequest(message="推荐适合油皮的洗面奶"))]
    second = [event async for event in chat_stream("s_replace", ChatStreamRequest(message="换一组"))]

    assert [event.product.product_id for event in first if event.event == "product_card"] == [
        product_a.product_id,
        product_b.product_id,
    ]
    assert [event.product.product_id for event in second if event.event == "product_card"] == [product_c.product_id]
    assert retrieval_feedbacks[-1]
    assert {product_a.product_id, product_b.product_id} <= set(retrieval_feedbacks[-1]["avoid_products"])


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


# ── _should_merge_previous_context unit tests ──────────────────────────


def test_should_merge_when_intent_is_clarify():
    """clarify intent always merges previous context."""
    assert _should_merge_previous_context(IntentResult(intent="clarify")) is True


def test_should_merge_when_intent_is_continue():
    """continue intent always merges previous context."""
    assert _should_merge_previous_context(IntentResult(intent="continue")) is True


def test_should_not_merge_when_recommend_has_category():
    """recommend with a category means the LLM already understood context — no merge needed."""
    assert _should_merge_previous_context(IntentResult(intent="recommend", category="食品生活")) is False


def test_should_merge_when_recommend_no_category_no_constraints():
    """recommend without category and without constraints — should merge (the fix for context-loss)."""
    assert (
        _should_merge_previous_context(IntentResult(intent="recommend", category=None, extracted_constraints={}))
        is True
    )


def test_should_merge_when_recommend_no_category_with_constraints():
    """recommend without category but with constraints — should merge (original behaviour, regression)."""
    assert (
        _should_merge_previous_context(
            IntentResult(intent="recommend", category=None, extracted_constraints={"product_type": "咖啡"})
        )
        is True
    )


def test_should_not_merge_when_chitchat():
    """chitchat intent should never merge previous shopping context."""
    assert _should_merge_previous_context(IntentResult(intent="chitchat")) is False


def test_should_not_merge_when_feedback():
    """feedback intent should not merge — it goes through its own handler path."""
    assert _should_merge_previous_context(IntentResult(intent="feedback")) is False


# ── Multi-turn context integration test ───────────────────────────────


@pytest.mark.asyncio
async def test_multi_turn_short_followup_merges_previous_category(monkeypatch):
    """Turn 2 with a short ambiguous message inherits category from Turn 1 criteria."""
    # Turn 1: use default mock (returns valid category), saves criteria to state
    _ = [event async for event in chat_stream("s_multi", ChatStreamRequest(message="有什么零食"))]

    # Override intent for Turn 2: simulate LLM returning no category
    async def return_no_category(session_id, body):
        del session_id
        return IntentResult(intent="recommend", category=None, extracted_constraints={})

    monkeypatch.setattr(pipeline_module, "run_intent", return_no_category)

    # Turn 2: short follow-up that would trigger clarification without the fix
    events = [event async for event in chat_stream("s_multi", ChatStreamRequest(message="咖啡"))]

    clarification_events = [e for e in events if e.event == "clarification"]
    assert len(clarification_events) == 0
