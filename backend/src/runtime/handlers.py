"""Intent handlers for the chat stream."""

from __future__ import annotations

import asyncio
import inspect
import logging
import time
from collections.abc import AsyncGenerator, Callable
from contextlib import suppress
from dataclasses import dataclass
from typing import Generic, TypeVar

from src.config import user_messages as msg
from src.config.tuning import (
    CHEAPER_BUDGET_DEFAULT_MAX,
    CHEAPER_BUDGET_MIN_MAX,
    CHEAPER_BUDGET_RATIO,
    INTER_CARD_DELAY_MS,
    SSE_DELTA_POLL_TIMEOUT_SECONDS,
)
from src.runtime.cart_handlers import (
    handle_add_to_cart,
    handle_remove_from_cart,
    handle_update_cart_quantity,
    handle_view_cart,
)
from src.runtime.compare_handlers import handle_compare
from src.runtime.cart_rules import message_refers_to_previous_product, referenced_product_id
from src.runtime.message_rules import is_replace_deck_phrase
from src.runtime.stages.criteria import criteria_from_intent, criteria_quick_actions
from src.runtime.stages.recommendation import RetrievalResult
from src.config.domain_terms import normalize_product_type, product_type_aliases
from src.services.retrieval_features import keyword_boost_score
from src.runtime.stages.slot_checker import build_clarification_question
from src.runtime.streaming import (
    StageResult,
    StreamContext,
    TimedTask,
    run_with_heartbeat,
    start_stage_task,
)
from src.services.audit import record_audit_event
from src.services.catalog import get_product
from src.services.conversation_state import (
    get_previous_criteria,
    get_previous_deck_id,
    get_previous_product_ids,
    save_recommendation_turn,
)
from src.services.decision_scoring import decision_confidence, score_candidates
from src.services.evidence import get_evidence
from src.services.fallbacks import get_fallback_events, record_fallback
from src.services.feedback import get_feedback_context, record_feedback
from src.services.recommendation_reasons import build_reason_atoms, fetch_risk_notes_for_products, reason_from_atoms
from src.services.retriever import filter_products
from src.services.trace_recorder import record_evidence_links, record_retrieval_trace
from src.types.schemas import ChatStreamRequest, DecisionResult, IntentResult
from src.types.sse_events import (
    AlternativePayload,
    ClarificationEvent,
    CriteriaCardEvent,
    CriteriaPayload,
    EvidencePayload,
    FinalDecisionEvent,
    ProductCardEvent,
    ProductPayload,
    QuickActionPayload,
    SSEEventBase,
    ShoppingStrategyPayload,
    TextDeltaEvent,
    now_ms,
)

IntentHandler = Callable[[StreamContext, ChatStreamRequest, IntentResult], AsyncGenerator[SSEEventBase, None]]
T = TypeVar("T")
CLARIFICATION_ANALYSIS_TEXT = msg.CLARIFICATION_ANALYSIS
RECOMMENDATION_STREAM_FALLBACK_TEXT = msg.RECOMMENDATION_STREAM_FALLBACK
RECOMMENDATION_STREAM_MIN_CHARS = 1
RECOMMENDATION_STREAM_MAX_CHARS = 8
RECOMMENDATION_STREAM_BOUNDARIES = set("，。！？；、,.!?;:\n")
INTRO_TEXT_NO_CONSTRAINTS = msg.INTRO_NO_CONSTRAINTS
FOLLOWUP_TEXT_DEFAULT = msg.FOLLOWUP_DEFAULT
FOLLOWUP_TEXT_SUBSEQUENT = msg.FOLLOWUP_SUBSEQUENT
FOLLOWUP_TEXT_BUDGET_RELAXED = msg.FOLLOWUP_BUDGET_RELAXED

STREAM_TEXT_DEFAULT_DELAY_MS = 25
TYPEWRITER_MIN_CHARS = 2
TYPEWRITER_MAX_CHARS = 6
TYPEWRITER_PUNCTUATION = set("，。！？；、,.!?;:，。！？")

logger = logging.getLogger(__name__)


@dataclass
class CapturedStage(Generic[T]):
    value: T | None = None

    def require(self, stage: str) -> T:
        if self.value is None:
            raise RuntimeError(f"{stage} stage completed without a result.")
        return self.value


async def handle_chitchat(
    ctx: StreamContext, body: ChatStreamRequest, intent: IntentResult
) -> AsyncGenerator[SSEEventBase, None]:
    del body, intent
    yield ctx.thinking("understanding", msg.THINKING_PROCESSING)
    # Guide user toward shopping: emit a helpful text + category clarification
    # instead of a dead-end greeting.
    yield TextDeltaEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"ai_text_{ctx.turn_id}",
        created_at_ms=now_ms(),
        message_id=f"msg_{ctx.turn_id}",
        delta=msg.CHITCHAT_HINT,
        done=True,
    )
    question, options = build_clarification_question(["category"])
    yield ClarificationEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"clarification_{ctx.turn_id}",
        created_at_ms=now_ms(),
        question=question,
        required_slots=["category"],
        suggested_options=options,
    )
    yield ctx.done()


async def handle_clarification(ctx: StreamContext, missing_slots: list[str]) -> AsyncGenerator[SSEEventBase, None]:
    question, options = build_clarification_question(missing_slots)
    yield ctx.thinking("clarifying", msg.THINKING_NEEDS_INFO)
    async for event in stream_text(
        ctx,
        CLARIFICATION_ANALYSIS_TEXT,
        message_id=f"clarification_analysis_{ctx.turn_id}",
        node_id=f"clarification_analysis_{ctx.turn_id}",
    ):
        yield event
    yield ClarificationEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"clarification_{ctx.turn_id}",
        created_at_ms=now_ms(),
        question=question,
        required_slots=missing_slots,
        suggested_options=options,
    )
    yield ctx.done()


# ── Speculative retrieval helpers ──────────────────────────────────────────


def _speculative_summary(intent: IntentResult) -> str:
    """Build a natural-language summary from intent for speculative retrieval embedding.

    Produces neutral descriptions like "用户需要美妆护肤类产品，油性肌肤适用，预算200元以内"
    that match the linguistic style of LLM-generated criteria summaries.
    """
    constraints = intent.extracted_constraints or {}
    parts: list[str] = []
    if intent.category:
        parts.append(f"{intent.category}类产品")

    # Category-specific attributes
    if skin_type := constraints.get("skin_type"):
        parts.append(f"{skin_type}肌肤适用")
    if storage := constraints.get("storage"):
        parts.append(f"{storage}存储")
    if screen_size := constraints.get("screen_size"):
        parts.append(f"{screen_size}屏幕")
    if sport_type := constraints.get("sport_type"):
        parts.append(f"{sport_type}运动")
    if dietary := constraints.get("dietary"):
        if isinstance(dietary, list):
            parts.append("、".join(dietary))
        else:
            parts.append(str(dietary))

    budget = constraints.get("budget_max")
    if budget is not None:
        parts.append(f"预算{budget:g}元以内")
    scenario = constraints.get("use_scenario")
    if scenario:
        parts.append(f"{scenario}场景")
    product_type = constraints.get("product_type")
    if product_type:
        parts.append(str(product_type))
    return "用户需要" + "，".join(parts) if parts else ""


def _post_filter_retrieval(
    result: RetrievalResult,
    criteria: CriteriaPayload,
    feedback: dict,
    max_products: int = 12,
) -> RetrievalResult:
    """Re-screen speculative retrieval results against full criteria hard filters.

    Does NOT re-run embedding or Rerank — just applies the hard-filter checks (O(n)).
    Then elevates brand_prefer products to the front of the list.
    """
    kept = filter_products(result.products, criteria, feedback, max_products=max_products)
    # Guarantee brand_prefer products are present and first. The speculative
    # retrieval used spec_criteria (built from intent) which may lack
    # brand_prefer if _merge_followup_context was skipped. This is the final
    # safety net that both injects missing brand products and reorders.
    kept = _ensure_brand_products(kept, criteria, feedback)
    kept_ids = {p.product_id for p in kept}
    return RetrievalResult(
        products=kept,
        evidence_by_product={pid: ev for pid, ev in result.evidence_by_product.items() if pid in kept_ids},
        trace_details={**result.trace_details, "speculative_post_filtered": True},
    )


def _ensure_brand_products(
    products: list[ProductPayload],
    criteria: CriteriaPayload,
    feedback: dict,
) -> list[ProductPayload]:
    """Inject missing brand_prefer products and move them to the front."""
    from src.services.retriever import inject_brand_preference_products

    return inject_brand_preference_products(products, criteria, feedback)


async def _try_build_shopping_strategy_plan(
    ctx: StreamContext,
    body: ChatStreamRequest,
    intent: IntentResult,
    criteria: CriteriaPayload,
    feedback: dict,
    image_embedding: list[float] | None,
) -> tuple[CriteriaPayload, ShoppingStrategyPayload | None, str | None, str | None] | None:
    """Try to build shopping strategy plan for scenario-based recommendations.

    Returns (updated_criteria, shopping_strategy, scene_judgement_text, reason_hint) if successful.
    Returns None if normal filter_recommend flow should be used.
    """
    try:
        from src.services.shopping_strategy import (
            build_shopping_strategy_plan,
            build_scenario_reason_hint,
        )
    except ImportError:
        logger.debug("shopping_strategy service not available, using normal flow")
        return None

    async def retrieval_probe(probe_criteria: CriteriaPayload) -> RetrievalResult:
        return await ctx.stages.run_retrieval(
            probe_criteria, top_n=3, feedback=feedback, image_embedding=image_embedding
        )

    try:
        plan = await build_shopping_strategy_plan(
            body,
            intent,
            criteria,
            retrieval_probe=retrieval_probe,
        )
    except Exception:
        logger.warning("Shopping strategy plan failed, falling back to normal flow", exc_info=True)
        return None

    if plan is None:
        return None

    reason_hint = build_scenario_reason_hint(plan.shopping_strategy)
    return (plan.criteria, plan.shopping_strategy, plan.scene_judgement_text, reason_hint)


async def handle_recommendation(
    ctx: StreamContext, body: ChatStreamRequest, intent: IntentResult
) -> AsyncGenerator[SSEEventBase, None]:
    if intent.intent == "feedback":
        await _record_feedback_intent(ctx, intent, body.message)

    # ── Fire DB reads before intro text: overlap with typewriter animation ──
    feedback_task = asyncio.create_task(get_feedback_context(ctx.session_id))
    ctx.background_tasks.append(TimedTask(task=feedback_task, started_at=time.perf_counter()))
    if _is_replace_deck_request(body):
        product_ids_task = asyncio.create_task(get_previous_product_ids(ctx.session_id))
        ctx.background_tasks.append(TimedTask(task=product_ids_task, started_at=time.perf_counter()))
    else:
        product_ids_task = None

    # ── Fire image embedding in parallel (for visual similarity retrieval) ──
    image_embedding_task: asyncio.Task | None = None
    if body.image_url:
        image_embedding_task = asyncio.create_task(ctx.stages.run_image_embedding(body.image_url))
        ctx.background_tasks.append(TimedTask(task=image_embedding_task, started_at=time.perf_counter()))

    # ── Build speculative criteria (instant, no I/O) ──
    spec_criteria = criteria_from_intent(intent, summary=_speculative_summary(intent))

    # ── Stream intro text while DB queries complete ──
    intro_text = _build_intro_text(intent, body.message)
    async for event in stream_text(
        ctx,
        intro_text,
        message_id=f"intro_{ctx.turn_id}",
        node_id=f"intro_{ctx.turn_id}",
    ):
        yield event

    # ── Collect DB results (already done by now) ──
    feedback = await feedback_task
    if product_ids_task is not None:
        previous_product_ids = await product_ids_task
        feedback = _feedback_with_avoided_products(feedback, previous_product_ids)

    # ── Speculative retrieval: launch in background while criteria runs ──
    image_embedding = await image_embedding_task if image_embedding_task else None
    retrieval_task = start_stage_task(
        ctx,
        ctx.stages.run_retrieval(spec_criteria, top_n=12, feedback=feedback, image_embedding=image_embedding),
        timing_key="retrieve",
        background=True,
    )

    # ── Criteria generation (in parallel with speculative retrieval) ──
    yield ctx.thinking("criteria", msg.THINKING_GENERATING_CRITERIA)
    criteria_capture: CapturedStage[CriteriaPayload] = CapturedStage()
    ctx.ensure_active()
    async for event in _capture_stage_result(
        criteria_capture,
        run_with_heartbeat(
            ctx,
            ctx.stages.run_criteria(ctx.session_id, body, intent),
            "criteria",
            msg.THINKING_GENERATING_CRITERIA,
            timing_key="criteria",
        ),
    ):
        yield event
    criteria = criteria_capture.require("criteria")

    # ── Await speculative retrieval + post-filter, then delegate ──
    if not retrieval_task.task.done():
        yield ctx.thinking("searching", msg.THINKING_SEARCHING)
    try:
        precomputed = await retrieval_task.task
    except Exception:
        precomputed = None

    # ── Try shopping strategy for scenario-based recommendations ──
    strategy_result = await _try_build_shopping_strategy_plan(
        ctx, body, intent, criteria, feedback, image_embedding
    )
    shopping_strategy: ShoppingStrategyPayload | None = None
    scene_judgement_text: str | None = None
    reason_hint: str | None = None
    if strategy_result is not None:
        criteria, shopping_strategy, scene_judgement_text, reason_hint = strategy_result
        # Discard speculative retrieval — strategy may have changed criteria
        precomputed = None

    if precomputed is not None:
        # Always re-screen with full criteria. The speculative retrieval used
        # a CriteriaPayload built from intent (which may lack brand_avoid,
        # product_type, or a tighter budget that the LLM criteria adds).
        # Post-filter is O(n) on at most 8 products — negligible cost.
        precomputed = _post_filter_retrieval(precomputed, criteria, feedback)
        precomputed = RetrievalResult(
            products=precomputed.products,
            evidence_by_product=precomputed.evidence_by_product,
            trace_details={**precomputed.trace_details, "speculative": True},
        )
        if not precomputed.products:
            precomputed = None  # trigger serial fallback in continue_recommendation_from_criteria

    async for event in continue_recommendation_from_criteria(
        ctx,
        body,
        criteria,
        precomputed_retrieval=precomputed,
        precomputed_feedback=feedback,
        image_embedding=image_embedding,
        shopping_strategy=shopping_strategy,
        scene_judgement_text=scene_judgement_text,
        reason_hint=reason_hint,
    ):
        yield event


async def handle_continue(
    ctx: StreamContext, body: ChatStreamRequest, intent: IntentResult
) -> AsyncGenerator[SSEEventBase, None]:
    del intent
    criteria = await get_previous_criteria(ctx.session_id)
    if criteria is None:
        async for event in handle_clarification(ctx, ["category"]):
            yield event
        return

    product_ids = await get_previous_product_ids(ctx.session_id)
    if product_ids:
        feedback = await get_feedback_context(ctx.session_id)
        has_actionable_feedback = bool(
            body.converge
            or feedback.get("avoid_products")
            or feedback.get("avoid_traits")
            or feedback.get("prefer_traits")
            or feedback.get("liked_products")
        )
        if has_actionable_feedback:
            async for event in continue_decision_from_current_deck(ctx, body, criteria, product_ids):
                yield event
            return

    async for event in continue_recommendation_from_criteria(ctx, body, criteria):
        yield event


async def continue_recommendation_from_criteria(
    ctx: StreamContext,
    body: ChatStreamRequest,
    criteria: CriteriaPayload,
    *,
    precomputed_retrieval: RetrievalResult | None = None,
    precomputed_feedback: dict | None = None,
    image_embedding: list[float] | None = None,
    shopping_strategy: ShoppingStrategyPayload | None = None,
    scene_judgement_text: str | None = None,
    reason_hint: str | None = None,
) -> AsyncGenerator[SSEEventBase, None]:
    ctx.ensure_active()

    # For scenario-based flow: emit scene judgement text and criteria_card early
    is_scenario_flow = shopping_strategy is not None
    if is_scenario_flow:
        if scene_judgement_text:
            async for event in stream_text(
                ctx,
                scene_judgement_text,
                message_id=f"scene_{ctx.turn_id}",
                node_id=f"scene_{ctx.turn_id}",
            ):
                yield event
        yield _criteria_card_event(ctx, criteria, shopping_strategy=shopping_strategy)

    if precomputed_feedback is not None:
        feedback = precomputed_feedback
    else:
        feedback = await get_feedback_context(ctx.session_id)
        if _is_replace_deck_request(body):
            previous_product_ids = await get_previous_product_ids(ctx.session_id)
            feedback = _feedback_with_avoided_products(feedback, previous_product_ids)

    retrieval_capture: CapturedStage[RetrievalResult] | None = None
    if precomputed_retrieval is not None:
        retrieval = precomputed_retrieval
    else:
        yield ctx.thinking("searching", msg.THINKING_SEARCHING)
        retrieval_capture = CapturedStage()
        ctx.ensure_active()
        async for event in _capture_stage_result(
            retrieval_capture,
            run_with_heartbeat(
                ctx,
                ctx.stages.run_retrieval(criteria, feedback=feedback, image_embedding=image_embedding),
                "searching",
                msg.THINKING_SEARCHING,
                timing_key="retrieve",
            ),
        ):
            yield event
        retrieval = retrieval_capture.require("retrieval")

    products = retrieval.products
    evidences_by_product = dict(retrieval.evidence_by_product)
    budget_relaxed = _budget_was_relaxed(retrieval.trace_details)
    risk_notes_extra = [msg.RISK_OVER_BUDGET] if budget_relaxed else []

    # Branch 0: no matching products
    if not products:
        # Retry without product_type filter — recommend related same-category items
        product_type = criteria.constraints.product_type
        if product_type and retrieval_capture is not None:
            relaxed = criteria.model_copy(update={
                "constraints": criteria.constraints.model_copy(update={"product_type": None}),
            })
            ctx.ensure_active()
            async for event in _capture_stage_result(
                retrieval_capture,
                run_with_heartbeat(
                    ctx,
                    ctx.stages.run_retrieval(relaxed, feedback=feedback, image_embedding=image_embedding),
                    "searching",
                    msg.THINKING_SEARCHING,
                    timing_key="retrieve",
                ),
            ):
                yield event
            fallback_retrieval = retrieval_capture.require("retrieval")
            if fallback_retrieval.products:
                products = fallback_retrieval.products
                evidences_by_product = dict(fallback_retrieval.evidence_by_product)
                retrieval = RetrievalResult(
                    products=products,
                    evidence_by_product=evidences_by_product,
                    trace_details=fallback_retrieval.trace_details,
                )
                # Check if any returned products match the requested type via hierarchy
                _expanded = {a.lower() for a in product_type_aliases(product_type)}
                _has_match = any(
                    normalize_product_type(p.sub_category).lower() in _expanded
                    for p in products
                )
                if _has_match:
                    fallback_text = f"以下是为你筛选的{product_type}相关商品"
                else:
                    fallback_text = f"当前商品库没有{product_type}，以下是同品类的相关推荐"
                if not is_scenario_flow:
                    yield _criteria_card_event(ctx, criteria)
                async for event in stream_text(ctx, fallback_text):
                    yield event
                # Fall through to product card emission below
            else:
                if not is_scenario_flow:
                    yield _criteria_card_event(ctx, criteria)
                async for event in stream_text(
                    ctx,
                    _no_match_followup_text(criteria),
                    message_id=f"no_match_{ctx.turn_id}",
                    node_id=f"no_match_{ctx.turn_id}",
                ):
                    yield event
                await save_recommendation_turn(ctx.session_id, criteria, [], user_message=body.message)
                yield ctx.done("awaiting_criteria_adjustment")
                return
        else:
            if not is_scenario_flow:
                yield _criteria_card_event(ctx, criteria)
            async for event in stream_text(
                ctx,
                _no_match_followup_text(criteria),
                message_id=f"no_match_{ctx.turn_id}",
                node_id=f"no_match_{ctx.turn_id}",
            ):
                yield event
            await save_recommendation_turn(ctx.session_id, criteria, [], user_message=body.message)
            yield ctx.done("awaiting_criteria_adjustment")
            return

    # Boost products matching explicit user intent keywords (e.g. "坚果" → nut products first)
    _keyword_hints = list(criteria.chips) if criteria.chips else []
    if criteria.constraints.product_type:
        _keyword_hints.append(criteria.constraints.product_type)
    if _keyword_hints:
        products = sorted(
            products,
            key=lambda p: (keyword_boost_score(p, _keyword_hints), -(p.price or float('inf'))),
            reverse=True,
        )
        retrieval = RetrievalResult(
            products=products,
            evidence_by_product=evidences_by_product,
            trace_details=retrieval.trace_details,
        )

    # Emit product cards with pacing delay (built into _product_card_events)
    async for event in _product_card_events(
        ctx, criteria, products, evidences_by_product,
        risk_notes_extra=risk_notes_extra,
        reason_hint=reason_hint,
        shopping_strategy=shopping_strategy,
    ):
        yield event

    # Branch 1: single product — score, then LLM explanation, then final_decision
    if len(products) == 1:
        # Run scoring for consistent decision_status/confidence (PRD 06)
        scored = score_candidates(
            products,
            criteria,
            feedback=feedback,
            evidence_by_product=evidences_by_product,
        )
        status, confidence = decision_confidence(scored, user_signal_count=0)

        if not is_scenario_flow:
            yield _criteria_card_event(ctx, criteria)
        yield ctx.thinking("decision", msg.THINKING_DECISION)
        winner = scored[0]
        decision = await _run_decision_with_context(
            ctx,
            criteria,
            products,
            evidences_by_product,
            locked_winner_product_id=winner.product_id,
            score_breakdown=winner.score_breakdown,
        )
        merged = DecisionResult(
            winner_product_id=winner.product_id,
            summary=decision.summary,
            why=decision.why,
            not_for=decision.not_for,
            decision_status=status,
            confidence=confidence,
            next_step="accept_recommendation" if status == "selected" else "adjust_criteria",
        )
        yield _final_decision_event(ctx, criteria, products, merged, score_breakdown=winner.score_breakdown)
        await _persist_recommendation(ctx, body, criteria, retrieval)
        yield ctx.done("completed")
        return

    # Branch 2+: LLM explanation → criteria_card → followup guidance → done.
    # PRD 05/06: multi-candidate decks must wait for user feedback or an
    # explicit convergence turn before emitting final_decision.
    # Step 1: LLM recommendation explanation ("为什么先给这些")
    try:
        async for event in _stream_recommendation_text_events(
            ctx,
            ctx.stages.run_recommendation_text_stream(criteria, products, evidences_by_product),
        ):
            yield event
    except Exception as exc:
        record_fallback(
            "llm.generate_recommendation_stream",
            "stream_text_failed",
            error_type=type(exc).__name__,
        )
        logger.warning("Recommendation stream text failed; continuing with product cards.", exc_info=True)

    # Step 2: criteria_card as post-hoc filter adjustment card
    if not is_scenario_flow:
        yield _criteria_card_event(ctx, criteria)

    # Step 3: followup guidance text ("你可以自然语言调整...")
    if budget_relaxed:
        followup_text = FOLLOWUP_TEXT_BUDGET_RELAXED
    elif feedback:
        followup_text = FOLLOWUP_TEXT_SUBSEQUENT
    else:
        followup_text = FOLLOWUP_TEXT_DEFAULT
    async for event in stream_text(
        ctx,
        followup_text,
        message_id=f"followup_{ctx.turn_id}",
        node_id=f"followup_{ctx.turn_id}",
    ):
        yield event

    await _persist_recommendation(ctx, body, criteria, retrieval)
    yield ctx.done("awaiting_product_feedback", deck_id=ctx.deck_id)


async def continue_decision_from_current_deck(
    ctx: StreamContext,
    body: ChatStreamRequest,
    criteria: CriteriaPayload,
    product_ids: list[str],
) -> AsyncGenerator[SSEEventBase, None]:
    deck_id = await get_previous_deck_id(ctx.session_id)
    feedback = await get_feedback_context(ctx.session_id, deck_id=deck_id)
    avoided = set(feedback.get("avoid_products") or [])
    products = [product for product_id in product_ids if (product := get_product(product_id)) is not None]
    if not products:
        async for event in handle_clarification(ctx, ["product_type"]):
            yield event
        return

    # Filter out disliked products
    filtered_products = [product for product in products if product.product_id not in avoided]

    # Decision state B: all candidates excluded by user
    if not filtered_products:
        yield _final_decision_event(
            ctx,
            criteria,
            products,
            DecisionResult(
                winner_product_id="",
                summary=msg.NO_SUITABLE_WINNER,
                why=[],
                not_for=[p.product_id for p in products],
                decision_status="no_suitable_winner",
                confidence=None,
                next_step="replace_deck",
            ),
            deck_id=deck_id,
        )
        yield _criteria_card_event(ctx, criteria)
        await save_recommendation_turn(
            ctx.session_id,
            criteria,
            [product.product_id for product in products],
            deck_id=deck_id,
            user_message=body.message,
        )
        yield ctx.done("awaiting_criteria_adjustment")
        return

    products = filtered_products
    evidences_list = await asyncio.gather(*(get_evidence(p) for p in products))
    evidences_by_product = {p.product_id: ev for p, ev in zip(products, evidences_list)}

    # Run scoring algorithm (PRD 06) before LLM
    scored = score_candidates(
        products,
        criteria,
        feedback=feedback,
        evidence_by_product=evidences_by_product,
    )
    winner = scored[0]
    user_signal_count = sum(1 for pid in feedback.get("avoid_products", []) if pid) + sum(
        1 for pid in feedback.get("prefer_traits", []) if pid
    )
    status, confidence = decision_confidence(scored, user_signal_count=user_signal_count)

    # Determine next_step from decision status
    next_step: str | None = None
    if status == "selected":
        next_step = "accept_recommendation"
    elif status == "needs_more_signal":
        next_step = "continue_current_deck"

    yield ctx.thinking("decision", msg.THINKING_DECISION_WITH_FEEDBACK)
    decision_capture: CapturedStage[DecisionResult] = CapturedStage()
    async for event in _capture_stage_result(
        decision_capture,
        run_with_heartbeat(
            ctx,
            _run_decision_with_context(
                ctx,
                criteria,
                products,
                evidences_by_product,
                locked_winner_product_id=winner.product_id,
                score_breakdown=winner.score_breakdown,
            ),
            "decision",
            msg.THINKING_DECISION_WITH_FEEDBACK,
            timing_key="decision",
        ),
    ):
        yield event
    llm_decision = decision_capture.require("decision")

    # Lock winner to scoring result; LLM is only for explanation
    merged = DecisionResult(
        winner_product_id=winner.product_id,
        summary=llm_decision.summary,
        why=llm_decision.why,
        not_for=llm_decision.not_for,
        decision_status=status,
        confidence=confidence,
        next_step=next_step,
    )

    yield _final_decision_event(
        ctx, criteria, products, merged, deck_id=deck_id, score_breakdown=winner.score_breakdown
    )
    await save_recommendation_turn(
        ctx.session_id,
        criteria,
        [product.product_id for product in products],
        deck_id=deck_id,
        user_message=body.message,
        ai_response=merged.summary,
    )
    yield ctx.done()


async def _record_feedback_intent(ctx: StreamContext, intent: IntentResult, message: str) -> None:
    reason = intent.extracted_constraints.get("feedback_text", "feedback")
    product_id = None
    if intent.target_product_id or message_refers_to_previous_product(message):
        product_id = await referenced_product_id(ctx.session_id, intent, message)
    deck_id = await get_previous_deck_id(ctx.session_id)
    await record_feedback(
        ctx.session_id,
        action="not_interested" if product_id else "feedback",
        product_id=product_id,
        reason=reason,
        deck_id=deck_id,
    )
    await record_audit_event(
        "feedback.created",
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        resource_type="feedback",
        resource_id=product_id,
        metadata={"source": "chat_intent", "reason": reason},
    )


async def _capture_stage_result(
    captured: CapturedStage[T],
    items: AsyncGenerator[SSEEventBase | StageResult[T], None],
) -> AsyncGenerator[SSEEventBase, None]:
    async for item in items:
        if isinstance(item, StageResult):
            captured.value = item.value
        else:
            yield item


async def _run_decision_with_context(
    ctx: StreamContext,
    criteria: CriteriaPayload,
    products: list[ProductPayload],
    evidences_by_product: dict[str, list[EvidencePayload]],
    *,
    locked_winner_product_id: str | None = None,
    score_breakdown: dict | None = None,
) -> DecisionResult:
    kwargs: dict[str, object] = {}
    if _callable_accepts(ctx.stages.run_decision, "locked_winner_product_id"):
        kwargs["locked_winner_product_id"] = locked_winner_product_id
    if _callable_accepts(ctx.stages.run_decision, "score_breakdown"):
        kwargs["score_breakdown"] = score_breakdown
    return await ctx.stages.run_decision(criteria, products, evidences_by_product, **kwargs)


def _callable_accepts(func: Callable[..., object], parameter_name: str) -> bool:
    try:
        parameters = inspect.signature(func).parameters.values()
    except (TypeError, ValueError):
        return False
    return any(
        parameter.name == parameter_name or parameter.kind == inspect.Parameter.VAR_KEYWORD for parameter in parameters
    )


def _is_replace_deck_request(body: ChatStreamRequest) -> bool:
    if body.criteria_patch and body.criteria_patch.get("replace_deck") is True:
        return True
    return is_replace_deck_phrase(body.message)


def _feedback_with_avoided_products(feedback: dict[str, list[str]], product_ids: list[str]) -> dict[str, list[str]]:
    if not product_ids:
        return feedback
    merged = {key: list(value) for key, value in feedback.items()}
    merged["avoid_products"] = list(dict.fromkeys([*merged.get("avoid_products", []), *product_ids]))
    return merged


def _criteria_card_event(
    ctx: StreamContext,
    criteria: CriteriaPayload,
    shopping_strategy: ShoppingStrategyPayload | None = None,
) -> CriteriaCardEvent:
    return CriteriaCardEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"criteria_{criteria.criteria_id}",
        created_at_ms=now_ms(),
        criteria=criteria,
        shopping_strategy=shopping_strategy,
        quick_actions=criteria_quick_actions(category=criteria.category or None),
    )


async def _product_card_events(
    ctx: StreamContext,
    criteria: CriteriaPayload,
    products: list[ProductPayload],
    evidences_by_product: dict[str, list[EvidencePayload]],
    *,
    risk_notes_extra: list[str] | None = None,
    reason_hint: str | None = None,
    shopping_strategy: ShoppingStrategyPayload | None = None,
) -> AsyncGenerator[ProductCardEvent, None]:
    product_ids = [p.product_id for p in products]
    risk_notes_map = await fetch_risk_notes_for_products(product_ids)

    for rank, product in enumerate(products, start=1):
        ctx.ensure_active()
        evidence = evidences_by_product.get(product.product_id)
        if evidence is None:
            evidence = await get_evidence(product)
        evidences_by_product[product.product_id] = evidence
        reason_atoms = build_reason_atoms(criteria, product, evidence)
        per_product_risk = risk_notes_map.get(product.product_id, [])
        all_risk_notes = list(risk_notes_extra or []) + per_product_risk

        # Use reason_hint for scenario-based flow, otherwise use atoms
        if reason_hint:
            reason_text = reason_hint
        else:
            reason_text = reason_from_atoms(product, reason_atoms)

        yield ProductCardEvent(
            session_id=ctx.session_id,
            turn_id=ctx.turn_id,
            seq=ctx.seq.next(),
            event_id=ctx.seq.event_id(),
            node_id=f"product_{product.product_id}",
            deck_id=ctx.deck_id,
            created_at_ms=now_ms(),
            rank=rank,
            product=product,
            reason=reason_text,
            reason_atoms=reason_atoms,
            risk_notes=all_risk_notes,
            evidence=evidence,
            actions=[
                QuickActionPayload(action_id="show_evidence", label=msg.QA_SHOW_EVIDENCE, action="open_evidence"),
                QuickActionPayload(action_id="add_to_cart", label=msg.QA_ADD_TO_CART, action="add_to_cart"),
                QuickActionPayload(
                    action_id="dislike_product",
                    label=msg.QA_DISLIKE,
                    action="feedback",
                    feedback_type="not_interested",
                ),
            ],
        )
        if rank < len(products):
            ctx.ensure_active()
            await asyncio.sleep(INTER_CARD_DELAY_MS / 1000)


async def _stream_recommendation_text_events(
    ctx: StreamContext,
    deltas: AsyncGenerator[str, None],
) -> AsyncGenerator[TextDeltaEvent | SSEEventBase, None]:
    message_id = f"msg_{ctx.turn_id}"
    node_id = f"ai_text_{ctx.turn_id}"
    queue: asyncio.Queue[tuple[str, str | BaseException | None]] = asyncio.Queue()

    async def pump() -> None:
        try:
            async for delta in deltas:
                await queue.put(("delta", delta))
            await queue.put(("done", None))
        except BaseException as exc:
            await queue.put(("error", exc))

    started_at = time.perf_counter()
    pump_task = asyncio.create_task(pump())
    buffer = ""
    emitted = False
    try:
        while True:
            ctx.ensure_active()
            try:
                kind, value = await asyncio.wait_for(queue.get(), timeout=SSE_DELTA_POLL_TIMEOUT_SECONDS)
            except asyncio.TimeoutError:
                # Fast 50ms timeout for low-latency delta passthrough
                if buffer:
                    # Flush accumulated buffer on timeout
                    chunk, buffer = buffer, ""
                    if chunk:
                        emitted = True
                        yield _text_delta_event(ctx, message_id=message_id, node_id=node_id, delta=chunk, done=False)
                continue

            if kind == "delta":
                buffer += str(value or "")
                while True:
                    chunk, buffer = _pop_recommendation_stream_chunk(buffer)
                    if chunk is None:
                        break
                    emitted = True
                    yield _text_delta_event(ctx, message_id=message_id, node_id=node_id, delta=chunk, done=False)
                continue

            if kind == "error":
                error = value if isinstance(value, BaseException) else RuntimeError("Recommendation stream failed.")
                raise error

            break

        final_delta = buffer
        if not emitted and not final_delta:
            final_delta = RECOMMENDATION_STREAM_FALLBACK_TEXT
        yield _text_delta_event(ctx, message_id=message_id, node_id=node_id, delta=final_delta, done=True)
        ctx.stage_timings_ms.setdefault("recommendation", round((time.perf_counter() - started_at) * 1000, 2))
    finally:
        if not pump_task.done():
            pump_task.cancel()
            with suppress(asyncio.CancelledError):
                await pump_task


def _pop_recommendation_stream_chunk(buffer: str, *, final: bool = False) -> tuple[str | None, str]:
    if not buffer:
        return None, buffer
    if final:
        return buffer, ""

    scan_limit = min(len(buffer), RECOMMENDATION_STREAM_MAX_CHARS)
    for index, char in enumerate(buffer[:scan_limit], start=1):
        if index >= RECOMMENDATION_STREAM_MIN_CHARS and char in RECOMMENDATION_STREAM_BOUNDARIES:
            return buffer[:index], buffer[index:]
    if len(buffer) >= RECOMMENDATION_STREAM_MAX_CHARS:
        return buffer[:RECOMMENDATION_STREAM_MAX_CHARS], buffer[RECOMMENDATION_STREAM_MAX_CHARS:]
    return None, buffer


def _text_delta_event(
    ctx: StreamContext,
    *,
    message_id: str,
    node_id: str,
    delta: str,
    done: bool,
) -> TextDeltaEvent:
    ctx.ensure_active()
    return TextDeltaEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=node_id,
        created_at_ms=now_ms(),
        message_id=message_id,
        delta=delta,
        done=done,
    )


def _split_for_typewriter(text: str) -> list[str]:
    """Split text into typewriter-friendly chunks at punctuation boundaries."""
    if not text:
        return []
    chunks: list[str] = []
    buf = ""
    for char in text:
        buf += char
        if char in TYPEWRITER_PUNCTUATION or len(buf) >= TYPEWRITER_MAX_CHARS:
            chunks.append(buf)
            buf = ""
    if buf:
        chunks.append(buf)
    # Merge very short trailing chunks
    if len(chunks) >= 2 and len(chunks[-1]) < TYPEWRITER_MIN_CHARS:
        chunks[-2] += chunks[-1]
        chunks.pop()
    return chunks


async def stream_text(
    ctx: StreamContext,
    text: str,
    message_id: str | None = None,
    node_id: str | None = None,
    delay_ms: int = STREAM_TEXT_DEFAULT_DELAY_MS,
) -> AsyncGenerator[TextDeltaEvent, None]:
    """Yield text as progressively arriving TextDeltaEvents with real delays."""
    mid = message_id or f"msg_{ctx.turn_id}"
    nid = node_id or f"ai_text_{ctx.turn_id}"
    chunks = _split_for_typewriter(text)
    for i, chunk in enumerate(chunks):
        ctx.ensure_active()
        yield TextDeltaEvent(
            session_id=ctx.session_id,
            turn_id=ctx.turn_id,
            seq=ctx.seq.next(),
            event_id=ctx.seq.event_id(),
            node_id=nid,
            created_at_ms=now_ms(),
            message_id=mid,
            delta=chunk,
            done=False,
        )
        if i < len(chunks) - 1:
            await asyncio.sleep(delay_ms / 1000)
    yield TextDeltaEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=nid,
        created_at_ms=now_ms(),
        message_id=mid,
        delta="",
        done=True,
    )


def _build_intro_text(intent: IntentResult, _message: str) -> str:
    """Construct the product-first intro text from intent data."""
    parts: list[str] = []
    if intent.category:
        parts.append(intent.category)
    constraints = intent.extracted_constraints or {}
    budget = constraints.get("budget_max")
    if budget is not None:
        try:
            parts.append(f"{float(budget):g}{msg.INTRO_BUDGET_SUFFIX}")
        except (ValueError, TypeError):
            pass
    product_type = constraints.get("product_type")
    if product_type:
        parts.append(str(product_type))
    scenario = constraints.get("use_scenario")
    if scenario:
        parts.append(str(scenario))

    # Category-specific attributes
    if skin := constraints.get("skin_type"):
        parts.append(f"{skin}{msg.INTRO_SKIN_SUFFIX}")
    if storage := constraints.get("storage"):
        parts.append(f"{storage}{msg.INTRO_STORAGE_SUFFIX}")
    if screen := constraints.get("screen_size"):
        parts.append(f"{screen}{msg.INTRO_SCREEN_SUFFIX}")
    if sport := constraints.get("sport_type"):
        parts.append(f"{sport}{msg.INTRO_SPORT_SUFFIX}")

    if parts:
        intro = f"{msg.INTRO_CONDITIONS_PREFIX}{'、'.join(parts)}{msg.INTRO_CONDITIONS_INFIX}"
        # When only category is present, add a guidance hint
        if len(parts) == 1 and intent.category:
            intro += msg.INTRO_SINGLE_CATEGORY_SUFFIX
        return intro
    return INTRO_TEXT_NO_CONSTRAINTS


def _final_decision_event(
    ctx: StreamContext,
    criteria: CriteriaPayload,
    products: list[ProductPayload],
    decision: DecisionResult,
    deck_id: str | None = None,
    score_breakdown: dict | None = None,
) -> FinalDecisionEvent:
    alternatives = [
        AlternativePayload(product_id=p.product_id, name=p.name)
        for p in products
        if p.product_id != decision.winner_product_id
    ][:2]

    # Build next_actions: always include cheaper and compare
    next_actions: list[QuickActionPayload] = [
        QuickActionPayload(
            action_id="cheaper",
            label=msg.QA_CHEAPER,
            action="criteria_patch",
            criteria_patch={"constraints": {"budget_max": _cheaper_budget_max(criteria)}},
        ),
        QuickActionPayload(action_id="compare", label=msg.QA_COMPARE, action="compare"),
    ]

    # Add add_to_cart action when decision_status is selected and winner exists
    if decision.decision_status == "selected" and decision.winner_product_id:
        next_actions.append(
            QuickActionPayload(
                action_id="add_winner_to_cart",
                label=msg.QA_ADD_TO_CART,
                action="add_to_cart",
            )
        )

    return FinalDecisionEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"decision_{ctx.turn_id}",
        deck_id=deck_id,
        created_at_ms=now_ms(),
        winner_product_id=decision.winner_product_id,
        summary=decision.summary,
        why=decision.why,
        not_for=decision.not_for,
        alternatives=alternatives,
        next_actions=next_actions,
        decision_status=decision.decision_status,
        confidence=decision.confidence,
        next_step=decision.next_step,
        score_breakdown=score_breakdown,
    )


async def _persist_recommendation(
    ctx: StreamContext,
    body: ChatStreamRequest,
    criteria: CriteriaPayload,
    retrieval: RetrievalResult,
) -> None:
    products = retrieval.products
    evidences_by_product = retrieval.evidence_by_product
    conversation_id = await save_recommendation_turn(
        ctx.session_id,
        criteria,
        [product.product_id for product in products],
        deck_id=ctx.deck_id,
        user_message=body.message,
    )
    await asyncio.gather(
        record_retrieval_trace(
            criteria,
            products,
            evidences_by_product,
            conversation_id=conversation_id,
            stage_timings_ms=ctx.stage_timings_ms,
            fallback_events=get_fallback_events(),
            trace_details=retrieval.trace_details,
        ),
        record_evidence_links(products, evidences_by_product, conversation_id=conversation_id),
        record_audit_event(
            "chat.recommendation_persisted",
            session_id=ctx.session_id,
            turn_id=ctx.turn_id,
            resource_type="conversation",
            resource_id=conversation_id,
            metadata={
                "criteria_id": criteria.criteria_id,
                "selected_ids": [product.product_id for product in products],
                "fallbacks": get_fallback_events(),
            },
        ),
    )


def _budget_was_relaxed(trace_details: dict) -> bool:
    """Check whether budget_max was relaxed during retrieval."""
    relaxation_steps = (trace_details.get("filters_applied") or {}).get("relaxation_steps") or []
    for step in relaxation_steps:
        if "budget_max" in (step.get("relaxed_fields") or []):
            return True
    return False


def _no_match_followup_text(criteria: CriteriaPayload) -> str:
    constraints = criteria.constraints
    has_budget = constraints.budget_min is not None or constraints.budget_max is not None
    has_exclusions = bool(
        constraints.brand_avoid or constraints.origin_avoid or constraints.ingredient_avoid or constraints.dietary
    )
    has_product_type = bool(constraints.product_type)
    if criteria.category and not has_budget and not has_exclusions and not has_product_type:
        examples = msg.PRODUCT_TYPE_HINTS_BY_CATEGORY.get(criteria.category, "具体商品类型")
        return msg.FOLLOWUP_NO_MATCH_NEED_PRODUCT_TYPE_TEMPLATE.format(category=criteria.category, examples=examples)

    suggestions: list[str] = []
    if has_budget:
        suggestions.append("放宽预算")
    if has_product_type:
        suggestions.append("换一个具体商品类型")
    else:
        suggestions.append("补充具体商品类型")
    if has_exclusions:
        suggestions.append("去掉部分排除条件")
    if criteria.category:
        suggestions.append("换一个品类")
    return msg.FOLLOWUP_NO_MATCH_ADJUST_TEMPLATE.format(suggestions="、".join(dict.fromkeys(suggestions)))


INTENT_HANDLERS: dict[str, IntentHandler] = {
    "recommend": handle_recommendation,
    "clarify": handle_recommendation,
    "continue": handle_continue,
    "feedback": handle_recommendation,
    "compare": handle_compare,
    "view_cart": handle_view_cart,
    "add_to_cart": handle_add_to_cart,
    "remove_from_cart": handle_remove_from_cart,
    "update_cart_quantity": handle_update_cart_quantity,
    "chitchat": handle_chitchat,
}


def _cheaper_budget_max(criteria: CriteriaPayload) -> float:
    current = criteria.constraints.budget_max
    if current is None:
        return CHEAPER_BUDGET_DEFAULT_MAX
    return max(CHEAPER_BUDGET_MIN_MAX, round(current * CHEAPER_BUDGET_RATIO, 2))
