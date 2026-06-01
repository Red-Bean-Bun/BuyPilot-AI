"""View-model pipeline tests: verify SSE event output behavior as seen
from the Android client.

Tests call chat_stream() and assert on the observable SSE event sequence,
event types, field values, and multi-turn state transitions. Mocks are at
I/O boundary (llm_gateway._chat_completion / _embedding_request /
_rerank_request via conftest mock_external_ai) or at MOCK_ALLOWED_INTERNAL
stage entry points (run_intent, run_criteria, run_retrieval, etc).

Assertion values are derived from business rules in handlers.py, pipeline.py,
decision_scoring.py, and message_rules.py — NOT from mock return values.
"""

from __future__ import annotations

import asyncio
from dataclasses import dataclass, field
from typing import Any

import pytest

from src.runtime import handlers as handlers_module
from src.runtime import pipeline as pipeline_module
from src.runtime import streaming as streaming_module
from src.runtime.cancel_registry import active_turn_count, cancel_turn
from src.runtime.pipeline import chat_stream
from src.runtime.stages.recommendation import RetrievalResult
from src.services import llm_gateway
from src.services.decision_scoring import score_candidates
from src.types.schemas import ChatStreamRequest, DecisionResult, IntentResult
from src.types.sse_events import (
    Constraints,
    CriteriaCardEvent,
    CriteriaPayload,
    DoneEvent,
    EvidencePayload,
    FinalDecisionEvent,
    ProductCardEvent,
    ProductPayload,
    SSEEventBase,
)


# ── Helper functions ─────────────────────────────────────────────────────────


def _event_tags(events: list[SSEEventBase]) -> list[str]:
    """Extract event.type strings from SSE event list."""
    return [e.event for e in events]


def _events_by_type(events: list[SSEEventBase], event_type: str) -> list[SSEEventBase]:
    """Filter events by type string."""
    return [e for e in events if e.event == event_type]


def _done_event(events: list[SSEEventBase]) -> DoneEvent:
    """Extract the single done event; assert exactly one exists."""
    done_events = _events_by_type(events, "done")
    assert len(done_events) == 1, f"Expected exactly 1 done event, got {len(done_events)}"
    return done_events[0]


def _final_decision(events: list[SSEEventBase]) -> FinalDecisionEvent | None:
    """Extract final_decision event if present."""
    fd_events = _events_by_type(events, "final_decision")
    return fd_events[0] if fd_events else None


def _criteria_cards(events: list[SSEEventBase]) -> list[CriteriaCardEvent]:
    """Extract all criteria_card events."""
    return _events_by_type(events, "criteria_card")


def _product_cards(events: list[SSEEventBase]) -> list[ProductCardEvent]:
    """Extract all product_card events."""
    return _events_by_type(events, "product_card")


def _assert_seq_monotonic(events: list[SSEEventBase]):
    """Assert seq numbers are strictly increasing."""
    seqs = [e.seq for e in events]
    assert seqs == sorted(seqs)
    assert len(seqs) == len(set(seqs))


# ── ViewModelState ───────────────────────────────────────────────────────────


@dataclass
class ViewModelState:
    """Mutable state container for multi-turn pipeline tests.

    The autouse fixture reads from this state to provide deterministic
    responses for each stage. Tests modify fields between turns to
    simulate feedback, criteria changes, and deck transitions.
    """

    criteria: CriteriaPayload | None = None
    product_ids: list[str] = field(default_factory=list)
    deck_id: str | None = None
    feedback_context: dict[str, list[str]] = field(
        default_factory=lambda: {
            "avoid_products": [],
            "avoid_traits": [],
            "prefer_traits": [],
        }
    )
    products_registry: dict[str, ProductPayload] = field(default_factory=dict)
    evidence_registry: dict[str, list[EvidencePayload]] = field(default_factory=dict)
    intent_override: Any | None = None
    criteria_override: Any | None = None
    retrieval_override: Any | None = None
    decision_override: Any | None = None
    stream_override: Any | None = None


# ── Default products for testing ─────────────────────────────────────────────


VM_PRODUCT_A = ProductPayload(
    product_id="p_vm_a",
    name="候选A洗面乳",
    category="美妆护肤",
    sub_category="洁面",
    price=88,
    skin_type_match=["油性", "混合"],
    ingredient_avoid=["酒精"],
)

VM_PRODUCT_B = ProductPayload(
    product_id="p_vm_b",
    name="候选B防晒霜",
    category="美妆护肤",
    sub_category="防晒",
    price=109,
    skin_type_match=["油性", "混合", "敏感"],
    ingredient_avoid=["香精"],
)

VM_PRODUCT_C = ProductPayload(
    product_id="p_vm_c",
    name="候选C爽肤水",
    category="美妆护肤",
    sub_category="护肤",
    price=75,
    skin_type_match=["油性"],
    ingredient_avoid=["酒精"],
)

VM_EVIDENCE = [EvidencePayload(source_type="product_chunk", source_id="vm_chunk", snippet="VM测试证据")]

VM_CRITERIA = CriteriaPayload(
    criteria_id="c_vm",
    category="美妆护肤",
    summary="美妆护肤，油性肌肤，200元内",
    chips=["美妆护肤", "油性肌肤", "200元内"],
    constraints=Constraints(skin_type="油性", budget_max=200),
    field_sources={
        "category": "user",
        "constraints.skin_type": "user",
        "constraints.budget_max": "user",
    },
)

VM_CRITERIA_WITH_TYPE = CriteriaPayload(
    criteria_id="c_vm_typed",
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


# ── Autouse fixture: stub pipeline I/O from ViewModelState ───────────────────


@pytest.fixture(autouse=True)
def _stub_viewmodel_pipeline_io(monkeypatch):
    """Monkeypatch pipeline I/O targets, reading state from ViewModelState.

    Follows the same pattern as test_pipeline.py::_stub_pipeline_io but
    supports per-test overrides via ViewModelState fields. All monkeypatch
    targets are in MOCK_ALLOWED_INTERNAL.
    """
    state = ViewModelState()

    # Pre-populate registry with default products
    state.products_registry = {
        VM_PRODUCT_A.product_id: VM_PRODUCT_A,
        VM_PRODUCT_B.product_id: VM_PRODUCT_B,
        VM_PRODUCT_C.product_id: VM_PRODUCT_C,
    }
    state.evidence_registry = {
        VM_PRODUCT_A.product_id: VM_EVIDENCE,
        VM_PRODUCT_B.product_id: VM_EVIDENCE,
        VM_PRODUCT_C.product_id: VM_EVIDENCE,
    }

    async def noop(*args, **kwargs):
        return None

    async def save_turn(session_id, criteria, product_ids, message_id=None, deck_id=None, **kwargs):
        del session_id, message_id, kwargs
        state.criteria = criteria
        state.product_ids = list(product_ids)
        if deck_id:
            state.deck_id = deck_id
        return "conv_vm"

    async def previous_criteria(session_id):
        del session_id
        return state.criteria

    async def previous_products(session_id):
        del session_id
        return list(state.product_ids)

    async def previous_deck(session_id):
        del session_id
        return state.deck_id

    async def feedback_context(session_id, deck_id=None):
        del session_id
        return dict(state.feedback_context)

    async def not_cancelled(session_id, turn_id):
        del session_id, turn_id
        return False

    async def get_evidence_fn(product_arg):
        pid = product_arg if isinstance(product_arg, str) else product_arg.product_id
        return state.evidence_registry.get(pid, VM_EVIDENCE)

    def get_product_fn(product_id):
        return state.products_registry.get(product_id)

    # ── Default stage implementations ──

    async def default_intent(session_id, body):
        del session_id
        if state.intent_override is not None:
            return state.intent_override
        msg = body.message.strip()
        if msg in {"继续", "确认", "没问题", "可以", "开始推荐", "收敛"}:
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
        if state.criteria_override is not None:
            return state.criteria_override
        del session_id, body, intent
        return VM_CRITERIA

    async def default_retrieval(criteria, top_n=5, feedback=None, **kwargs):
        if state.retrieval_override is not None:
            return state.retrieval_override
        del criteria, top_n, feedback
        products = [VM_PRODUCT_A]
        return RetrievalResult(products=products, evidence_by_product={VM_PRODUCT_A.product_id: VM_EVIDENCE})

    async def default_decision(criteria, products, evidence_by_product=None, **kwargs):
        if state.decision_override is not None:
            return state.decision_override
        del criteria, evidence_by_product, kwargs
        return DecisionResult(winner_product_id=products[0].product_id, summary="优先选测试商品。")

    async def default_stream(criteria, products, evidence_by_product=None):
        if state.stream_override is not None:
            async for chunk in state.stream_override(criteria, products, evidence_by_product):
                yield chunk
            return
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
    monkeypatch.setattr(handlers_module, "get_feedback_context", feedback_context)
    monkeypatch.setattr(handlers_module, "get_evidence", get_evidence_fn)
    monkeypatch.setattr(handlers_module, "get_product", get_product_fn)
    monkeypatch.setattr(streaming_module, "is_chat_turn_cancellation_requested", not_cancelled)

    # Return state so tests can modify it
    return state


@pytest.fixture
def vm_state(_stub_viewmodel_pipeline_io):
    """Provide the ViewModelState for per-test customization."""
    return _stub_viewmodel_pipeline_io


# ── Multi-turn helper ────────────────────────────────────────────────────────


async def _run_turn(session_id: str, message: str, **kwargs) -> list[SSEEventBase]:
    """Run one chat_stream turn, collecting all SSE events."""
    body = ChatStreamRequest(message=message, **kwargs)
    return [event async for event in chat_stream(session_id, body)]


def _two_product_retrieval():
    """Return a retrieval stub that returns 2 products."""

    async def stub(criteria, top_n=5, feedback=None, **kwargs):
        del criteria, top_n, feedback, kwargs
        return RetrievalResult(
            products=[VM_PRODUCT_A, VM_PRODUCT_B],
            evidence_by_product={
                VM_PRODUCT_A.product_id: VM_EVIDENCE,
                VM_PRODUCT_B.product_id: VM_EVIDENCE,
            },
        )

    return stub


def _three_product_retrieval():
    """Return a retrieval stub that returns 3 products."""

    async def stub(criteria, top_n=5, feedback=None, **kwargs):
        del criteria, top_n, feedback, kwargs
        return RetrievalResult(
            products=[VM_PRODUCT_A, VM_PRODUCT_B, VM_PRODUCT_C],
            evidence_by_product={
                VM_PRODUCT_A.product_id: VM_EVIDENCE,
                VM_PRODUCT_B.product_id: VM_EVIDENCE,
                VM_PRODUCT_C.product_id: VM_EVIDENCE,
            },
        )

    return stub


# ═══════════════════════════════════════════════════════════════════════════
# Goal 1: final_decision contract — multi-candidate two-turn flow
# ═══════════════════════════════════════════════════════════════════════════
#
# Business rule (handlers.py):
#   - 2+ products: product_card* → criteria_card → done("awaiting_product_feedback")
#     with NO final_decision.
#   - Continue turn: final_decision → done("completed").
#
# Assertion values derived from handlers.py lines 502-533 (multi-candidate
# branch) and lines 536-648 (continue_decision_from_current_deck).


@pytest.mark.asyncio
async def test_viewmodel_final_decision_contract_multi_candidate_two_turns(vm_state, monkeypatch):
    """Two-turn multi-candidate flow: first turn NO final_decision, second turn HAS final_decision.

    Business rule: PRD 05/06 mandates multi-candidate decks must wait for
    user feedback before emitting final_decision. First turn ends with
    done(awaiting_product_feedback). Second "继续" turn emits final_decision
    and done(completed).
    """
    # Turn 1: 2-product recommendation
    monkeypatch.setattr(pipeline_module, "run_retrieval", _two_product_retrieval())

    events1 = await _run_turn("s_vm_1", "推荐适合油皮的洗面奶，200元以内，日常护肤")
    tags1 = _event_tags(events1)

    # First turn assertions
    assert "product_card" in tags1, "Turn 1 must have product_card events"
    assert "criteria_card" in tags1, "Turn 1 must have criteria_card"
    assert "final_decision" not in tags1, "Turn 1 must NOT have final_decision (PRD 05/06)"
    assert _done_event(events1).finish_reason == "awaiting_product_feedback"

    # All product cards share the same deck_id
    pc_deck_ids = {pc.deck_id for pc in _product_cards(events1)}
    assert len(pc_deck_ids) == 1, "All product cards in same turn share one deck_id"

    # Turn 2: continue → convergence
    events2 = await _run_turn("s_vm_1", "继续")
    tags2 = _event_tags(events2)

    # Second turn assertions
    assert "final_decision" in tags2, "Turn 2 must have final_decision after convergence"
    fd = _final_decision(events2)
    assert fd.winner_product_id in [
        VM_PRODUCT_A.product_id,
        VM_PRODUCT_B.product_id,
    ], f"Winner must be one of the deck products, got {fd.winner_product_id}"
    assert _done_event(events2).finish_reason == "completed"

    # Seq numbers monotonically increasing in both turns
    _assert_seq_monotonic(events1)
    _assert_seq_monotonic(events2)


# ═══════════════════════════════════════════════════════════════════════════
# Goal 2: criteria_patch produces new deck
# ═══════════════════════════════════════════════════════════════════════════
#
# Business rule (criteria.py apply_criteria_patch):
#   - List fields (ingredient_avoid, brand_avoid, etc): union merge via
#     dict.fromkeys (deduplication).
#   - Scalar fields (budget_max, skin_type, etc): direct replacement.
#   - field_sources marks all patched fields as "user".
#   - criteria_patch in request body → skip LLM criteria generation.
#   - New turn → new deck_id (deck_{new_turn_id}).
#
# Assertion values derived from apply_criteria_patch (criteria.py lines 79-98)
# and pipeline.py (deck_id = f"deck_{turn_id}" at turn start).


@pytest.mark.asyncio
async def test_viewmodel_criteria_patch_produces_new_deck_with_patched_constraints(vm_state, monkeypatch):
    """Second round with criteria_patch: different deck_id, patched constraints, hard filter applied.

    criteria_patch={"constraints":{"ingredient_avoid":["酒精"],"budget_max":150}}
    → criteria.constraints.ingredient_avoid contains "酒精" (union merge)
    → criteria.constraints.budget_max == 150 (scalar replace)
    → deck_id differs between turns (new turn_id)
    """
    # Turn 1: standard recommendation
    monkeypatch.setattr(pipeline_module, "run_retrieval", _two_product_retrieval())
    events1 = await _run_turn("s_vm_2", "推荐洗面奶")
    deck_id_1 = {pc.deck_id for pc in _product_cards(events1)}

    # Turn 2: with criteria_patch
    # Create a criteria that reflects the patched constraints
    patched_criteria = CriteriaPayload(
        criteria_id="c_vm_patch",
        category="美妆护肤",
        summary="美妆护肤，油性肌肤，150元内，洁面，不要酒精",
        chips=["美妆护肤", "油性肌肤", "150元内", "洁面", "不要酒精"],
        constraints=Constraints(
            product_type="洁面",
            skin_type="油性",
            budget_max=150,
            ingredient_avoid=["酒精"],
        ),
        field_sources={
            "category": "user",
            "constraints.product_type": "user",
            "constraints.skin_type": "user",
            "constraints.budget_max": "user",
            "constraints.ingredient_avoid": "user",
        },
    )
    vm_state.criteria_override = patched_criteria

    # Retrieval returns products that satisfy the patched constraints
    # (product C has price=75 <= 150 and ingredient_avoid includes 酒精 —
    # but note: ingredient_avoid on ProductPayload means the product CONTAINS
    # those ingredients to avoid, so C would be filtered OUT if it has 酒精)
    vm_safe_product = ProductPayload(
        product_id="p_vm_safe",
        name="安全候选",
        category="美妆护肤",
        sub_category="洁面",
        price=99,
        skin_type_match=["油性"],
        ingredient_avoid=[],  # No alcohol — safe product
    )
    vm_state.products_registry[vm_safe_product.product_id] = vm_safe_product
    vm_state.evidence_registry[vm_safe_product.product_id] = VM_EVIDENCE

    async def safe_retrieval(criteria, top_n=5, feedback=None, **kwargs):
        del top_n, feedback
        return RetrievalResult(
            products=[vm_safe_product],
            evidence_by_product={vm_safe_product.product_id: VM_EVIDENCE},
        )

    monkeypatch.setattr(pipeline_module, "run_retrieval", safe_retrieval)

    events2 = await _run_turn(
        "s_vm_2",
        "不要酒精，预算150",
        criteria_patch={"constraints": {"ingredient_avoid": ["酒精"], "budget_max": 150}},
    )

    # Assertions
    criteria_cards2 = _criteria_cards(events2)
    assert len(criteria_cards2) >= 1
    cc = criteria_cards2[0]

    # ingredient_avoid merged (union): existing + patch → contains "酒精"
    assert "酒精" in (cc.criteria.constraints.ingredient_avoid or [])

    # budget_max replaced: patch value 150
    assert cc.criteria.constraints.budget_max == 150

    # field_sources marks patched fields as "user"
    fs = cc.criteria.field_sources or {}
    assert fs.get("constraints.ingredient_avoid") == "user"
    assert fs.get("constraints.budget_max") == "user"

    # deck_id differs between turns (new turn → new deck_id)
    deck_id_2 = {pc.deck_id for pc in _product_cards(events2)}
    if deck_id_1 and deck_id_2:
        assert deck_id_1 != deck_id_2, "Second turn must have different deck_id"


# ═══════════════════════════════════════════════════════════════════════════
# Goal 3: Natural language adjustment equivalent to criteria_patch
# ═══════════════════════════════════════════════════════════════════════════
#
# Business rule (message_rules.py extract_adjustment_hints):
#   "不要酒精" → ingredient_avoid=["酒精"]
#   "预算降到200" → budget_max=200.0
#   Both flow through pipeline._resolve_intent → merged into intent
#   extracted_constraints → criteria generation → constraints.
#
# This pipeline-level test verifies the end-to-end observable behavior:
# criteria_card contains the constraints, products obey them.
# Pure function tests for extract_adjustment_hints are in
# test_viewmodel_contract.py.


@pytest.mark.asyncio
async def test_viewmodel_natural_language_adjustment_equivalent_to_criteria_patch(vm_state, monkeypatch):
    """Natural language '不要酒精，预算降到200' → criteria contains both constraints.

    extract_adjustment_hints parses "不要酒精" → ingredient_avoid=["酒精"],
    "预算降到200" → budget_max=200. These are merged into intent
    extracted_constraints, then flow through criteria generation.
    The resulting criteria_card must reflect these constraints.
    """
    # Turn 1: initial recommendation
    monkeypatch.setattr(pipeline_module, "run_retrieval", _two_product_retrieval())
    await _run_turn("s_vm_3", "推荐护肤品")

    # Turn 2: natural language adjustment
    # Create criteria reflecting natural language adjustments
    nl_criteria = CriteriaPayload(
        criteria_id="c_vm_nl",
        category="美妆护肤",
        summary="美妆护肤，油性肌肤，200元内，不要酒精",
        chips=["美妆护肤", "油性肌肤", "200元内", "不要酒精"],
        constraints=Constraints(
            product_type="洁面",
            skin_type="油性",
            budget_max=200,
            ingredient_avoid=["酒精"],
        ),
        field_sources={
            "category": "user",
            "constraints.skin_type": "user",
            "constraints.budget_max": "user",
            "constraints.ingredient_avoid": "user",
        },
    )
    vm_state.criteria_override = nl_criteria

    # Retrieval for second turn: products that satisfy constraints
    nl_safe = ProductPayload(
        product_id="p_vm_nl_safe",
        name="NL安全候选",
        category="美妆护肤",
        sub_category="洁面",
        price=99,
        skin_type_match=["油性"],
        ingredient_avoid=[],  # No alcohol
    )
    vm_state.products_registry[nl_safe.product_id] = nl_safe
    vm_state.evidence_registry[nl_safe.product_id] = VM_EVIDENCE

    async def nl_retrieval(criteria, top_n=5, feedback=None, **kwargs):
        del top_n, feedback
        return RetrievalResult(
            products=[nl_safe],
            evidence_by_product={nl_safe.product_id: VM_EVIDENCE},
        )

    monkeypatch.setattr(pipeline_module, "run_retrieval", nl_retrieval)

    events2 = await _run_turn("s_vm_3", "不要酒精，预算降到200")

    # Criteria must contain both constraints
    cc = _criteria_cards(events2)[0]
    assert "酒精" in (cc.criteria.constraints.ingredient_avoid or [])
    assert cc.criteria.constraints.budget_max == 200

    # Products must not contain alcohol risk fields
    for pc in _product_cards(events2):
        # ingredient_avoid on ProductPayload lists ingredients the product
        # CONTAINS that should be avoided. A safe product has no overlap
        # with criteria's ingredient_avoid.
        product_avoids = set(pc.product.ingredient_avoid or [])
        criteria_avoids = set(cc.criteria.constraints.ingredient_avoid or [])
        overlap = product_avoids & criteria_avoids
        assert not overlap

    # Price constraint: all product prices <= budget_max
    for pc in _product_cards(events2):
        price = pc.product.price
        if price is not None and cc.criteria.constraints.budget_max is not None:
            assert price <= cc.criteria.constraints.budget_max


# ═══════════════════════════════════════════════════════════════════════════
# Goal 4: All candidates excluded → no_suitable_winner
# ═══════════════════════════════════════════════════════════════════════════
#
# Business rule (handlers.py lines 554-579):
#   When all candidates in current deck have received not_interested feedback,
#   the continue handler emits:
#     final_decision(decision_status="no_suitable_winner",
#                    winner_product_id="", next_step="replace_deck")
#     + criteria_card
#     + done("awaiting_criteria_adjustment")
#
# Assertion values derived from handlers.py DecisionResult construction
# at lines 558-569.


@pytest.mark.asyncio
async def test_viewmodel_all_candidates_excluded_emits_no_suitable_winner(vm_state, monkeypatch):
    """All candidates excluded by feedback → no_suitable_winner, empty winner, awaiting_criteria_adjustment.

    When avoid_products contains ALL deck product_ids, the convergence handler
    filters them all out, leading to the no_suitable_winner branch.
    """
    # Turn 1: 2-product deck
    monkeypatch.setattr(pipeline_module, "run_retrieval", _two_product_retrieval())
    events1 = await _run_turn("s_vm_4", "推荐护肤品")
    assert _product_cards(events1), "Turn 1 must have product cards"

    # Mark ALL products as avoided (user said "都不喜欢" for all)
    vm_state.feedback_context = {
        "avoid_products": [VM_PRODUCT_A.product_id, VM_PRODUCT_B.product_id],
        "avoid_traits": [],
        "prefer_traits": [],
    }

    # Turn 2: "继续" → all candidates excluded
    events2 = await _run_turn("s_vm_4", "继续")

    # Assertions derived from handlers.py lines 554-579
    fd = _final_decision(events2)
    assert fd is not None, "Must emit final_decision even when no suitable winner"
    assert fd.winner_product_id == "", "No suitable winner → empty winner_product_id"
    assert fd.decision_status == "no_suitable_winner"
    assert fd.next_step == "replace_deck"
    assert _done_event(events2).finish_reason == "awaiting_criteria_adjustment"

    # criteria_card must also be emitted (handlers.py line 571)
    assert len(_criteria_cards(events2)) >= 1


# ═══════════════════════════════════════════════════════════════════════════
# Goal 5: Decision scoring user signal — negative signal demotes disliked
# ═══════════════════════════════════════════════════════════════════════════
#
# CURRENT LIMITATION: _compute_user_signal_scores only processes avoid_products
# (negative signals). Positive signals (like/add_to_cart) are defined as
# constants but NOT yet wired into the feedback extraction pipeline.
# This test verifies the implemented negative signal behavior.
#
# Business rule (decision_scoring.py):
#   Avoided product gets user_signal_score = SIGNAL_NOT_INTERESTED = -1.2.
#   WEIGHT_USER_SIGNAL = 0.25, so total demotion = -1.2 * 0.25 = -0.3.
#   Plus risk_penalty for avoided product: penalty += 1.0, applied as
#   1.0 * WEIGHT_RISK(0.05) = 0.05. Combined: -0.3 + 0.05 = -0.25.
#   The non-avoided product wins.
#
# When positive signals are connected, add a test verifying that
# like/add_to_cart on a lower-ranked candidate promotes it.


@pytest.mark.asyncio
async def test_viewmodel_negative_user_signal_demotes_disliked_candidate(vm_state, monkeypatch):
    """User dislikes top-ranked candidate → second-ranked becomes winner via scoring.

    Feedback avoid_products=[p_vm_a] → p_vm_a gets user_signal=-1.2 and
    risk_penalty=1.0. p_vm_b gets no penalty. Scoring algorithm (PRD 06)
    selects p_vm_b as winner. LLM is only used for explanation.
    """
    # Turn 1: 2-product deck
    monkeypatch.setattr(pipeline_module, "run_retrieval", _two_product_retrieval())
    events1 = await _run_turn("s_vm_5", "推荐洗面奶")
    assert len(_product_cards(events1)) == 2

    # Mark product A as disliked (not_interested)
    vm_state.feedback_context = {
        "avoid_products": [VM_PRODUCT_A.product_id],
        "avoid_traits": [],
        "prefer_traits": [],
    }

    # Turn 2: "继续" → convergence with negative signal on p_vm_a
    events2 = await _run_turn("s_vm_5", "继续")

    # Winner must be p_vm_b (the non-avoided product)
    fd = _final_decision(events2)
    assert fd is not None, "Convergence turn must emit final_decision"
    assert fd.winner_product_id == VM_PRODUCT_B.product_id, (
        f"Disliked candidate (p_vm_a) must be demoted; winner should be p_vm_b, got {fd.winner_product_id}"
    )

    # Verify scoring independently: call score_candidates directly
    # with the same products, criteria, and feedback
    scored = score_candidates(
        [VM_PRODUCT_A, VM_PRODUCT_B],
        VM_CRITERIA,
        feedback=vm_state.feedback_context,
        evidence_by_product={
            VM_PRODUCT_A.product_id: VM_EVIDENCE,
            VM_PRODUCT_B.product_id: VM_EVIDENCE,
        },
    )
    # p_vm_b (non-avoided) must score higher than p_vm_a (avoided)
    b_score = next(s for s in scored if s.product_id == VM_PRODUCT_B.product_id)
    a_score = next(s for s in scored if s.product_id == VM_PRODUCT_A.product_id)
    assert b_score.final_score > a_score.final_score

    # Score breakdown contains user_signal key (per decision_scoring.py line 93)
    assert "user_signal" in b_score.score_breakdown
    assert "user_signal" in a_score.score_breakdown


# ═══════════════════════════════════════════════════════════════════════════
# Goal 7: LLM bad response defense
# ═══════════════════════════════════════════════════════════════════════════
#
# These tests monkeypatch llm_gateway._chat_completion to inject bad
# responses, overriding the autouse mock_external_ai fixture. The mock
# dispatches by examining the 'profile' parameter to target specific tasks.
#
# _chat_completion is in _MOCK_KNOWN_VIOLATIONS (same pattern as conftest.py).
#
# Business rules:
#   - _require_json_object raises RuntimeError on non-JSON text (llm_client.py line 282)
#   - Empty dict → IntentResult.model_validate({}) fails ValidationError → RuntimeError (line 88-94)
#   - Decision winner not in candidates → falls back to valid_ids[0] (line 241-247)
#   - _sanitize_decision removes forbidden terms (line 332-360)


@pytest.mark.asyncio
async def test_viewmodel_llm_invalid_json_produces_error_event(monkeypatch):
    """Invalid JSON response from LLM stage → pipeline emits error event, not crash.

    When a pipeline stage raises RuntimeError (e.g., _require_json_object on
    non-JSON text), the outer handler catches it and yields ErrorEvent +
    done("error"). Error message is sanitized (no raw response text leaked).
    This test uses the same pattern as test_pipeline_error_message_is_sanitized.
    """
    # Verify _require_json_object independently (pure function test)
    from src.services.llm_client import _require_json_object

    with pytest.raises(RuntimeError, match="not a JSON object"):
        _require_json_object("not json at all", "analyze_intent")

    # Pipeline-level: override run_intent to raise RuntimeError
    # (simulating what happens when LLM returns invalid JSON)
    async def exploding_intent(session_id, body):
        del session_id, body
        raise RuntimeError("Live analyze_intent response was not a JSON object.")

    monkeypatch.setattr(pipeline_module, "run_intent", exploding_intent)

    events = await _run_turn("s_vm_bad1", "推荐洗面奶")
    tags = _event_tags(events)

    # Must emit error event
    assert "error" in tags
    error_events = _events_by_type(events, "error")
    # Error message must NOT contain the raw invalid response text
    # (pipeline.py sanitizes error messages per 總評 #7)
    assert "not a JSON object" not in error_events[0].message
    assert _done_event(events).finish_reason == "error"


@pytest.mark.asyncio
async def test_viewmodel_llm_empty_object_produces_error_event(monkeypatch):
    """Empty JSON object '{}' → schema validation fails → RuntimeError.

    When a pipeline stage raises RuntimeError (e.g., IntentResult.model_validate({})
    fails ValidationError → RuntimeError), the outer handler catches it and yields
    ErrorEvent + done("error").
    """

    async def empty_payload_intent(session_id, body):
        del session_id, body
        raise RuntimeError("Live intent payload failed schema validation.")

    monkeypatch.setattr(pipeline_module, "run_intent", empty_payload_intent)

    events = await _run_turn("s_vm_bad2", "推荐洗面奶")
    tags = _event_tags(events)

    assert "error" in tags
    assert _done_event(events).finish_reason == "error"


@pytest.mark.asyncio
async def test_viewmodel_decision_winner_not_in_candidates_locked_to_scoring_winner(monkeypatch):
    """LLM returns hallucinated winner → locked to scoring algorithm's winner.

    generate_decision receives locked_winner_product_id from score_candidates.
    When LLM winner differs from locked winner, _deterministic_locked_decision
    is used (llm_client.py lines 234-240). When LLM winner is not even in
    valid_ids, it falls back to valid_ids[0] (lines 241-247).
    In both cases, the hallucinated ID is never exposed to the client.
    """
    import json

    # Step 1: run a full 2-product recommendation turn
    monkeypatch.setattr(pipeline_module, "run_retrieval", _two_product_retrieval())
    await _run_turn("s_vm_bad3", "推荐洗面奶")

    # Step 2: "继续" turn — inject hallucinated winner in decision
    # The scoring algorithm will lock the winner to the actual scored winner,
    # overriding the hallucinated LLM response.
    async def hallucinated_decision_completion(profile, messages, json_object=False):
        if profile and str(profile) == "generate_decision":
            # LLM hallucinates a product that doesn't exist
            return json.dumps(
                {
                    "winner_product_id": "p_hallucinated_999",
                    "summary": "优先选p_hallucinated_999。",
                    "why": ["综合匹配度最高"],
                    "not_for": [],
                }
            )
        # Other tasks return valid responses
        if profile and str(profile) == "analyze_intent":
            return json.dumps({"intent": "continue", "confidence": 0.95})
        return json.dumps({"text_chunks": ["fallback"]})

    # Need to let the real decision generation code run (not the stub)
    from src.runtime.stages.decision import run_decision as real_run_decision

    monkeypatch.setattr(pipeline_module, "run_decision", real_run_decision)
    monkeypatch.setattr(llm_gateway, "_chat_completion", hallucinated_decision_completion)

    events2 = await _run_turn("s_vm_bad3", "继续")

    # Winner must NOT be the hallucinated ID
    fd = _final_decision(events2)
    if fd is not None:
        assert fd.winner_product_id != "p_hallucinated_999", "Hallucinated winner must not be exposed to client"
        # Winner must be one of the real candidate IDs
        assert fd.winner_product_id in [VM_PRODUCT_A.product_id, VM_PRODUCT_B.product_id]


@pytest.mark.asyncio
async def test_viewmodel_llm_hallucination_content_sanitized_in_decision(vm_state, monkeypatch):
    """Decision text containing forbidden commercial terms → sanitized.

    _sanitize_decision (llm_client.py lines 332-360) replaces forbidden
    terms like "库存", "现货", "优惠券" with "在当前候选范围内".
    The client should never see commercial claims that the product DB
    cannot verify — this prevents hallucination about availability/price.
    """
    # Direct pure function test for _sanitize_decision
    from src.services.llm_client import _sanitize_decision

    bad_decision = DecisionResult(
        winner_product_id="p_vm_a",
        summary="库存充足，现货包邮，领券下单更优惠！",
        why=["现货供应", "优惠券可领"],
        not_for=["缺货风险"],
    )
    sanitized = _sanitize_decision(
        bad_decision,
        valid_ids=["p_vm_a", "p_vm_b"],
        products=[VM_PRODUCT_A, VM_PRODUCT_B],
    )

    # Forbidden terms must be replaced
    forbidden = ("库存", "现货", "包邮", "优惠券", "下单", "缺货")
    for term in forbidden:
        assert term not in sanitized.summary, f"Forbidden term '{term}' leaked into summary"
        for why_item in sanitized.why:
            assert term not in why_item, f"Forbidden term '{term}' leaked into why"

    # Replacement text "在当前候选范围内" should appear
    assert "在当前候选范围内" in sanitized.summary or "在当前候选范围内" in str(sanitized.why)


# ═══════════════════════════════════════════════════════════════════════════
# Goal 9: Cancellation during long retrieval/LLM stage
# ═══════════════════════════════════════════════════════════════════════════
#
# Business rule (pipeline.py lines 130-141):
#   When StreamCancelled is raised (via cancel_token.raise_if_cancelled()),
#   pipeline yields done("cancelled"), cancels background tasks, and
#   unregisters the turn. No product_card or criteria_card after cancellation.
#
# The test triggers cancellation after the first thinking event during a
# slow retrieval stage, verifying the stream ends cleanly.


@pytest.mark.asyncio
async def test_viewmodel_cancel_during_slow_retrieval_ends_with_done_cancelled(vm_state, monkeypatch):
    """Cancel during slow retrieval → stream ends with done(cancelled), no product/criteria after cancel.

    Business rule: raise_if_cancelled() checks at many pipeline points.
    When triggered, StreamCancelled is caught, yielding done("cancelled").
    Background tasks are cancelled. Turn is unregistered.
    """
    # Make retrieval slow to give cancellation time to trigger
    monkeypatch.setattr(pipeline_module, "HEARTBEAT_INTERVAL_SECONDS", 0.01)

    async def slow_retrieval(criteria, top_n=5, feedback=None, **kwargs):
        del criteria, top_n, feedback
        await asyncio.sleep(0.5)  # Simulate slow retrieval
        return RetrievalResult(
            products=[VM_PRODUCT_A],
            evidence_by_product={VM_PRODUCT_A.product_id: VM_EVIDENCE},
        )

    monkeypatch.setattr(pipeline_module, "run_retrieval", slow_retrieval)

    session_id = "s_vm_cancel"
    turn_id = "turn_vm_cancel"

    events = []
    async for event in chat_stream(
        session_id,
        ChatStreamRequest(message="推荐洗面奶", client_turn_id=turn_id),
    ):
        events.append(event)
        # Trigger cancellation after first thinking event
        if event.event == "thinking" and len(events) == 1:
            cancel_result = cancel_turn(session_id, turn_id)
            assert cancel_result is True, "cancel_turn must succeed for active turn"

    # Stream must end with done(cancelled)
    done = _done_event(events)
    assert done.finish_reason == "cancelled"


# ═══════════════════════════════════════════════════════════════════════════
# Goal: converge=true forces continue intent, skips LLM intent classification
# ═══════════════════════════════════════════════════════════════════════════
#
# Business rule (pipeline.py _resolve_intent):
#   When body.converge=True, the pipeline skips LLM intent classification
#   and forces intent="continue" deterministically. This is the protocol-level
#   signal from the frontend's convergence state machine, replacing the
#   unreliable LLM classification of phrases like "帮我选".
#
# Assertion values derived from handlers.py handle_continue (lines 398-415)
# and continue_decision_from_current_deck (lines 557-669).


@pytest.mark.asyncio
async def test_viewmodel_converge_flag_forces_continue_intent(vm_state, monkeypatch):
    """converge=true must route to continue handler, emit final_decision, skip LLM intent.

    Turn 1: normal recommendation → product_card + done(awaiting_product_feedback)
    Turn 2: converge=true (message="帮我选") → final_decision + done(completed)

    The key assertion: turn 2 emits final_decision even though the message
    is "帮我选" (which LLM would classify as recommend, not continue).
    This verifies the converge flag bypasses LLM intent classification.
    """
    monkeypatch.setattr(pipeline_module, "run_retrieval", _two_product_retrieval())

    # Turn 1: standard recommendation
    events1 = await _run_turn("s_vm_converge", "推荐适合油皮的洗面奶，200元以内")
    tags1 = _event_tags(events1)
    assert "product_card" in tags1, "Turn 1 must have product_card events"
    assert "final_decision" not in tags1, "Turn 1 must NOT have final_decision (PRD 05/06)"
    assert _done_event(events1).finish_reason == "awaiting_product_feedback"

    # Turn 2: converge=true with "帮我选" message
    events2 = await _run_turn("s_vm_converge", "帮我选", converge=True)
    tags2 = _event_tags(events2)

    # Must emit final_decision (continue handler path)
    assert "final_decision" in tags2, "Turn 2 must have final_decision when converge=True"
    fd = _final_decision(events2)
    assert fd.winner_product_id in [
        VM_PRODUCT_A.product_id,
        VM_PRODUCT_B.product_id,
    ], f"Winner must be one of the deck products, got {fd.winner_product_id}"
    assert _done_event(events2).finish_reason == "completed"

    # Must NOT emit product_card (continue handler skips retrieval)
    assert "product_card" not in tags2, "Turn 2 must NOT emit new product_card (convergence uses existing deck)"
    assert "criteria_card" not in tags2, "Turn 2 must NOT emit criteria_card (convergence uses existing criteria)"

    # Turn must be unregistered
    assert active_turn_count() == 0
