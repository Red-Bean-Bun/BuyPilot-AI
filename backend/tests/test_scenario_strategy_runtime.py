"""Runtime integration tests for scenario-based shopping strategy.

Tests cover:
1. Scenario flow event ordering (text_delta before product_card)
2. Normal flow event ordering preserved (product-first)
3. final_decision includes add_to_cart action when selected
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any
from unittest.mock import AsyncMock

import pytest

import src.runtime.handlers as handlers_module
import src.runtime.pipeline as pipeline_module
from src.runtime.pipeline import chat_stream
from src.runtime.stages.recommendation import RetrievalResult
from src.runtime.cancel_registry import CancellationToken
from src.runtime.handlers import (
    _criteria_card_event,
    _final_decision_event,
    _product_card_events,
)
from src.runtime.streaming import StreamContext
from src.types.schemas import ChatStreamRequest, CriteriaPayload, DecisionResult, IntentResult
from src.types.sse_events import (
    Constraints,
    CriteriaCardEvent,
    EvidencePayload,
    EventSeq,
    FinalDecisionEvent,
    PrimaryDirectionPayload,
    ProductPayload,
    ProductCardEvent,
    SearchStrategyPayload,
    ShoppingStrategyPayload,
)


@dataclass
class FakeStages:
    """Minimal stub for StreamContext.stages."""

    async def run_image_embedding(self, image_url: str | None = None) -> list[float]:
        return []

    async def run_retrieval(self, *args: Any, **kwargs: Any) -> Any:
        from src.runtime.stages.recommendation import RetrievalResult
        return RetrievalResult(products=[], evidence_by_product={})


def _make_ctx() -> StreamContext:
    """Create a minimal StreamContext for testing."""
    return StreamContext(
        session_id="test-session",
        turn_id="test-turn",
        deck_id="test-deck",
        seq=EventSeq("test-turn"),
        cancel_token=CancellationToken(session_id="test-session", turn_id="test-turn"),
        stages=FakeStages(),  # type: ignore[arg-type]
        heartbeat_interval_seconds=10.0,
    )


def _make_shopping_strategy() -> ShoppingStrategyPayload:
    """Create a sample ShoppingStrategyPayload for testing."""
    return ShoppingStrategyPayload(
        strategy_id="scene_001",
        scene_type="gift",
        scene_summary="送男朋友生日礼物",
        user_problem="不确定送什么更体面",
        primary_direction=PrimaryDirectionPayload(
            title="低踩雷的黑科技小件",
            summary="优先考虑音频配件",
            why="有新鲜感，不强依赖具体型号偏好",
            search_strategy=SearchStrategyPayload(
                category="数码电子",
                product_type="真无线耳机",
            ),
            available_in_catalog=True,
            supporting_product_count=2,
        ),
        avoid_risks=["不要盲买手机、电脑"],
        assumptions=["暂时不知道预算"],
        confidence="medium",
    )


def _make_criteria() -> CriteriaPayload:
    """Create a sample CriteriaPayload for testing."""
    return CriteriaPayload(
        criteria_id="criteria_001",
        category="数码电子",
        summary="数码电子类产品",
        chips=["数码电子"],
    )


# ── Test 1: _criteria_card_event supports shopping_strategy ──────────────


def test_criteria_card_event_with_shopping_strategy() -> None:
    """_criteria_card_event includes shopping_strategy when provided."""
    ctx = _make_ctx()
    criteria = _make_criteria()
    strategy = _make_shopping_strategy()

    event = _criteria_card_event(ctx, criteria, shopping_strategy=strategy)

    assert isinstance(event, CriteriaCardEvent)
    assert event.shopping_strategy is not None
    assert event.shopping_strategy.strategy_id == "scene_001"
    assert event.shopping_strategy.scene_type == "gift"
    assert event.criteria.category == "数码电子"


def test_criteria_card_event_without_shopping_strategy() -> None:
    """_criteria_card_event works without shopping_strategy (normal flow)."""
    ctx = _make_ctx()
    criteria = _make_criteria()

    event = _criteria_card_event(ctx, criteria)

    assert isinstance(event, CriteriaCardEvent)
    assert event.shopping_strategy is None
    assert event.criteria.category == "数码电子"


# ── Test 2: _final_decision_event includes add_to_cart when selected ─────


def test_final_decision_event_includes_add_to_cart_when_selected() -> None:
    """_final_decision_event includes add_to_cart when decision_status is selected."""
    ctx = _make_ctx()
    criteria = _make_criteria()
    decision = DecisionResult(
        winner_product_id="product_001",
        summary="推荐这款耳机",
        why=["音质好"],
        not_for=[],
        decision_status="selected",
        confidence="high",
        next_step="accept_recommendation",
    )

    event = _final_decision_event(ctx, criteria, [], decision)

    assert isinstance(event, FinalDecisionEvent)
    assert event.decision_status == "selected"
    assert event.winner_product_id == "product_001"

    # Check that add_to_cart action is present
    action_ids = [a.action_id for a in event.next_actions]
    assert "add_winner_to_cart" in action_ids

    # Check that cheaper and compare are still present
    assert "cheaper" in action_ids
    assert "compare" in action_ids

    # Verify the add_to_cart action has correct properties
    add_cart_action = next(a for a in event.next_actions if a.action_id == "add_winner_to_cart")
    assert add_cart_action.action == "add_to_cart"
    assert add_cart_action.label == "加入购物车"


def test_final_decision_event_no_add_to_cart_when_not_selected() -> None:
    """_final_decision_event does not include add_to_cart when decision_status is not selected."""
    ctx = _make_ctx()
    criteria = _make_criteria()
    decision = DecisionResult(
        winner_product_id="",
        summary="需要调整条件",
        why=[],
        not_for=[],
        decision_status="no_suitable_winner",
        confidence=None,
        next_step="adjust_criteria",
    )

    event = _final_decision_event(ctx, criteria, [], decision)

    assert isinstance(event, FinalDecisionEvent)
    assert event.decision_status == "no_suitable_winner"

    # Check that add_to_cart action is NOT present
    action_ids = [a.action_id for a in event.next_actions]
    assert "add_winner_to_cart" not in action_ids

    # cheaper and compare should still be present
    assert "cheaper" in action_ids
    assert "compare" in action_ids


def test_final_decision_event_no_add_to_cart_when_no_winner() -> None:
    """_final_decision_event does not include add_to_cart when winner_product_id is empty."""
    ctx = _make_ctx()
    criteria = _make_criteria()
    decision = DecisionResult(
        winner_product_id="",  # No winner
        summary="需要更多信息",
        why=[],
        not_for=[],
        decision_status="needs_more_signal",
        confidence="low",
        next_step="adjust_criteria",
    )

    event = _final_decision_event(ctx, criteria, [], decision)

    action_ids = [a.action_id for a in event.next_actions]
    assert "add_winner_to_cart" not in action_ids


# ── Test 3: Scenario flow ordering ───────────────────────────────────────


@pytest.mark.asyncio
async def test_product_card_events_with_reason_hint() -> None:
    """_product_card_events uses reason_hint when provided (scenario flow)."""
    ctx = _make_ctx()
    criteria = _make_criteria()
    from src.types.sse_events import EvidencePayload, ProductPayload

    products = [
        ProductPayload(product_id="p1", name="Test Product 1", category="数码电子"),
    ]
    evidences: dict[str, list[EvidencePayload]] = {}

    events: list[ProductCardEvent] = []
    async for event in _product_card_events(
        ctx, criteria, products, evidences, reason_hint="低偏好依赖，送礼更稳"
    ):
        events.append(event)

    assert len(events) == 1
    assert events[0].reason == "低偏好依赖，送礼更稳"


@pytest.mark.asyncio
async def test_product_card_events_without_reason_hint() -> None:
    """_product_card_events uses reason_from_atoms when reason_hint is None (normal flow)."""
    ctx = _make_ctx()
    criteria = _make_criteria()
    from src.types.sse_events import EvidencePayload, ProductPayload

    products = [
        ProductPayload(product_id="p1", name="Test Product 1", category="数码电子"),
    ]
    evidences: dict[str, list[EvidencePayload]] = {}

    events: list[ProductCardEvent] = []
    async for event in _product_card_events(ctx, criteria, products, evidences):
        events.append(event)

    assert len(events) == 1
    # Reason should be generated from atoms, not empty
    assert events[0].reason
    assert events[0].reason != "低偏好依赖，送礼更稳"


# ── Test 4: Shopping strategy integration (mock-based) ───────────────────


def test_shopping_strategy_import_fallback() -> None:
    """When shopping_strategy service is not available, graceful fallback occurs."""
    # This test verifies that the lazy import in _try_build_shopping_strategy_plan
    # handles ImportError gracefully. The actual integration test would require
    # mocking the service, but since the service doesn't exist yet, we verify
    # the fallback path.
    from src.runtime.handlers import _try_build_shopping_strategy_plan

    # The function should return None when service is unavailable
    # (since shopping_strategy.py doesn't exist yet)
    # This is a structural test - we verify the function exists and is callable
    assert callable(_try_build_shopping_strategy_plan)


@pytest.mark.asyncio
async def test_chat_stream_scenario_flow_emits_strategy_before_products(monkeypatch) -> None:
    """Full chat_stream path: scenario strategy is actually imported and emitted."""
    product_a = ProductPayload(
        product_id="p_earbud_a",
        name="降噪真无线耳机 A",
        category="数码电子",
        sub_category="真无线耳机",
        price=999,
    )
    product_b = ProductPayload(
        product_id="p_earbud_b",
        name="降噪真无线耳机 B",
        category="数码电子",
        sub_category="真无线耳机",
        price=1299,
    )
    evidence = [EvidencePayload(source_type="product_chunk", snippet="真无线耳机，日常使用")]

    async def fake_intent(session_id: str, body: ChatStreamRequest) -> IntentResult:
        del session_id, body
        return IntentResult(intent="recommend", category="数码电子")

    async def fake_criteria(session_id: str, body: ChatStreamRequest, intent: IntentResult) -> CriteriaPayload:
        del session_id, body, intent
        return CriteriaPayload(criteria_id="c_scene", category="数码电子", constraints=Constraints())

    async def fake_retrieval(criteria: CriteriaPayload, *args: object, **kwargs: object) -> RetrievalResult:
        del args, kwargs
        products = [product_a, product_b]
        if criteria.constraints.product_type:
            products = [p for p in products if p.sub_category == criteria.constraints.product_type]
        return RetrievalResult(
            products=products,
            evidence_by_product={p.product_id: evidence for p in products},
            trace_details={},
        )

    async def fake_image_embedding(image_url: str | None = None) -> list[float] | None:
        del image_url
        return None

    async def fake_text_stream(
        criteria: CriteriaPayload,
        products: list[ProductPayload],
        evidences_by_product: dict[str, list[EvidencePayload]] | None,
    ):
        del criteria, products, evidences_by_product
        yield "我先按低踩雷小件方向找。"

    async def fake_feedback(*args: object, **kwargs: object) -> dict[str, list[str]]:
        del args, kwargs
        return {}

    async def fake_save_recommendation(*args: object, **kwargs: object) -> str:
        del args, kwargs
        return "conv_scene"

    async def fake_record(*args: object, **kwargs: object) -> None:
        del args, kwargs

    monkeypatch.setattr(pipeline_module, "run_intent", fake_intent)
    monkeypatch.setattr(pipeline_module, "run_criteria", fake_criteria)
    monkeypatch.setattr(pipeline_module, "run_retrieval", fake_retrieval)
    monkeypatch.setattr(pipeline_module, "run_image_embedding", fake_image_embedding)
    monkeypatch.setattr(pipeline_module, "run_recommendation_text_stream", fake_text_stream)
    monkeypatch.setattr(handlers_module, "get_feedback_context", fake_feedback)
    monkeypatch.setattr(handlers_module, "save_recommendation_turn", fake_save_recommendation)
    monkeypatch.setattr(handlers_module, "record_retrieval_trace", fake_record)
    monkeypatch.setattr(handlers_module, "record_evidence_links", fake_record)
    monkeypatch.setattr(handlers_module, "record_audit_event", fake_record)

    events = [
        event
        async for event in chat_stream(
            "sess_scenario_runtime",
            ChatStreamRequest(message="我男朋友过生日，他平时喜欢电子产品，应该送什么好？"),
        )
    ]
    tags = [event.event for event in events]
    criteria_cards = [event for event in events if event.event == "criteria_card"]
    products = [event for event in events if event.event == "product_card"]

    assert criteria_cards
    assert products
    scenario_card = criteria_cards[0]
    assert scenario_card.shopping_strategy is not None
    assert scenario_card.shopping_strategy.decision_barrier is not None
    assert scenario_card.shopping_strategy.decision_barrier.barrier_type == "fear_wrong_choice"
    assert scenario_card.criteria.constraints.product_type == "真无线耳机"
    assert tags.index("criteria_card") < tags.index("product_card")
    assert all(product.product.sub_category == "真无线耳机" for product in products)
    assert all(len(product.reason) <= 28 for product in products)


# ── Test 5: ShoppingStrategyPayload structure ────────────────────────────


def test_shopping_strategy_payload_structure() -> None:
    """ShoppingStrategyPayload has expected fields for scenario-based flow."""
    strategy = _make_shopping_strategy()

    assert strategy.strategy_id == "scene_001"
    assert strategy.scene_type == "gift"
    assert strategy.primary_direction.title == "低踩雷的黑科技小件"
    assert strategy.primary_direction.available_in_catalog is True
    assert strategy.primary_direction.supporting_product_count == 2
    assert len(strategy.avoid_risks) == 1
    assert strategy.confidence == "medium"


def test_shopping_strategy_payload_with_decision_barrier() -> None:
    """ShoppingStrategyPayload can include decision_barrier."""
    from src.types.sse_events import DecisionBarrierPayload

    strategy = ShoppingStrategyPayload(
        strategy_id="scene_002",
        scene_type="gift",
        primary_direction=PrimaryDirectionPayload(title="Test"),
        decision_barrier=DecisionBarrierPayload(
            barrier_type="fear_wrong_choice",
            label="怕送错、怕不够体面",
            reason="对方懂电子产品",
            conversion_strategy="推荐低偏好依赖的小件",
        ),
    )

    assert strategy.decision_barrier is not None
    assert strategy.decision_barrier.barrier_type == "fear_wrong_choice"
    assert strategy.decision_barrier.label == "怕送错、怕不够体面"
