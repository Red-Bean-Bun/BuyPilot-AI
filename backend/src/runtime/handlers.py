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

from src.config.tuning import (
    CHEAPER_BUDGET_DEFAULT_MAX,
    CHEAPER_BUDGET_MIN_MAX,
    CHEAPER_BUDGET_RATIO,
    INTER_CARD_DELAY_MS,
)
from src.runtime.cart_rules import message_refers_to_previous_product, quantity_from_intent, referenced_product_id
from src.runtime.message_rules import is_replace_deck_phrase
from src.runtime.stages.criteria import criteria_from_intent, criteria_quick_actions
from src.runtime.stages.recommendation import RetrievalResult
from src.runtime.stages.slot_checker import build_clarification_question
from src.runtime.streaming import (
    StageResult,
    StreamContext,
    run_with_heartbeat,
    start_stage_task,
)
from src.services.audit import record_audit_event
from src.services.cart import add_product_to_cart, remove_product_from_cart, update_product_quantity
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
from src.services.recommendation_reasons import build_reason_atoms, reason_from_atoms
from src.services.retriever import filter_products
from src.services.trace_recorder import record_evidence_links, record_retrieval_trace
from src.types.schemas import ChatStreamRequest, DecisionResult, IntentResult
from src.types.sse_events import (
    AlternativePayload,
    CartActionEvent,
    ClarificationEvent,
    CriteriaCardEvent,
    CriteriaPayload,
    EvidencePayload,
    FinalDecisionEvent,
    ProductCardEvent,
    ProductPayload,
    QuickActionPayload,
    SSEEventBase,
    TextDeltaEvent,
    now_ms,
)

IntentHandler = Callable[[StreamContext, ChatStreamRequest, IntentResult], AsyncGenerator[SSEEventBase, None]]
T = TypeVar("T")
CLARIFICATION_ANALYSIS_TEXT = (
    "我先看了一下你的需求，已经能判断大方向，但还缺一个会影响推荐标准的关键信息。补齐后再生成购买标准会更稳。"
)
RECOMMENDATION_STREAM_FALLBACK_TEXT = "我先把匹配商品列出来，方便你快速比较。"
RECOMMENDATION_STREAM_MIN_CHARS = 1
RECOMMENDATION_STREAM_MAX_CHARS = 8
RECOMMENDATION_STREAM_BOUNDARIES = set("，。！？；、,.!?;:\n")
INTRO_TEXT_NO_CONSTRAINTS = "正在分析你的需求，马上为你查找匹配商品..."
FOLLOWUP_TEXT_DEFAULT = (
    "你先看看这几款候选。如果想调整筛选范围，可以直接说「再温和一点」「不要酒精」「预算再低一点」，也可以点筛选卡修改。"
)
FOLLOWUP_TEXT_NO_MATCH = "当前条件下暂时没有匹配的商品。你可以放宽预算、换一个品类，或者去掉一些排除条件试试。"

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


async def handle_view_cart(
    ctx: StreamContext, body: ChatStreamRequest, intent: IntentResult
) -> AsyncGenerator[SSEEventBase, None]:
    del body, intent
    yield ctx.thinking("generating", "正在查看购物车...")
    yield CartActionEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"cart_{ctx.turn_id}",
        created_at_ms=now_ms(),
        action="view",
        product_id="",
        quantity=0,
        status="success",
    )
    yield ctx.done()


async def handle_add_to_cart(
    ctx: StreamContext, body: ChatStreamRequest, intent: IntentResult
) -> AsyncGenerator[SSEEventBase, None]:
    product_id = await referenced_product_id(ctx.session_id, intent, body.message)
    if product_id is None:
        async for event in _clarify_cart_target(ctx, "你想把哪个商品加入购物车？"):
            yield event
        yield ctx.done()
        return
    quantity = quantity_from_intent(intent, body.message, default=1)
    try:
        await add_product_to_cart(ctx.session_id, product_id, quantity=quantity)
    except ValueError:
        yield _cart_action_event(ctx, "add", product_id, 0, "failed")
        yield ctx.done()
        return
    await record_audit_event(
        "cart.item_added",
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        resource_type="cart_item",
        resource_id=product_id,
        metadata={"quantity": quantity, "source": "chat_intent"},
    )
    ctx.ensure_active()
    yield _cart_action_event(ctx, "add", product_id, quantity, "success")
    yield ctx.done()


async def handle_remove_from_cart(
    ctx: StreamContext, body: ChatStreamRequest, intent: IntentResult
) -> AsyncGenerator[SSEEventBase, None]:
    product_id = await referenced_product_id(ctx.session_id, intent, body.message)
    if product_id is None:
        async for event in _clarify_cart_target(ctx, "你想从购物车移出哪个商品？"):
            yield event
        yield ctx.done()
        return
    try:
        item = await remove_product_from_cart(ctx.session_id, product_id)
    except ValueError:
        item = None
    if item is None:
        yield _cart_action_event(ctx, "remove", product_id, 0, "failed")
        yield ctx.done()
        return
    await record_audit_event(
        "cart.item_removed",
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        resource_type="cart_item",
        resource_id=product_id,
        metadata={"source": "chat_intent"},
    )
    yield _cart_action_event(ctx, "remove", product_id, 0, "success")
    yield ctx.done()


async def handle_update_cart_quantity(
    ctx: StreamContext, body: ChatStreamRequest, intent: IntentResult
) -> AsyncGenerator[SSEEventBase, None]:
    product_id = await referenced_product_id(ctx.session_id, intent, body.message)
    if product_id is None:
        async for event in _clarify_cart_target(ctx, "你想修改哪个商品的数量？"):
            yield event
        yield ctx.done()
        return
    quantity = quantity_from_intent(intent, body.message, default=1)
    try:
        item = await update_product_quantity(ctx.session_id, product_id, quantity)
    except ValueError:
        item = None
    if item is None:
        yield _cart_action_event(ctx, "update_quantity", product_id, quantity, "failed")
        yield ctx.done()
        return
    await record_audit_event(
        "cart.quantity_updated",
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        resource_type="cart_item",
        resource_id=product_id,
        metadata={"quantity": quantity, "source": "chat_intent"},
    )
    yield _cart_action_event(ctx, "update_quantity", product_id, quantity, "success")
    yield ctx.done()


async def handle_chitchat(
    ctx: StreamContext, body: ChatStreamRequest, intent: IntentResult
) -> AsyncGenerator[SSEEventBase, None]:
    del body, intent
    yield ctx.thinking("understanding", "正在处理...")
    yield TextDeltaEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"ai_text_{ctx.turn_id}",
        created_at_ms=now_ms(),
        message_id=f"msg_{ctx.turn_id}",
        delta="我是电商导购助手，可以帮你推荐商品、添加购物车。请问你想买什么？",
        done=True,
    )
    yield ctx.done()


async def handle_clarification(ctx: StreamContext, missing_slots: list[str]) -> AsyncGenerator[SSEEventBase, None]:
    question, options = build_clarification_question(missing_slots)
    yield ctx.thinking("clarifying", "需要补充一个关键信息。")
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
    skin_type = constraints.get("skin_type")
    if skin_type:
        parts.append(f"{skin_type}肌肤适用")
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
    max_products: int = 5,
) -> RetrievalResult:
    """Re-screen speculative retrieval results against full criteria hard filters.

    Does NOT re-run embedding or Rerank — just applies the hard-filter checks (O(n)).
    """
    kept = filter_products(
        result.products, criteria, feedback, max_products=max_products
    )
    kept_ids = {p.product_id for p in kept}
    return RetrievalResult(
        products=kept,
        evidence_by_product={
            pid: ev
            for pid, ev in result.evidence_by_product.items()
            if pid in kept_ids
        },
        trace_details={**result.trace_details, "speculative_post_filtered": True},
    )


async def handle_recommendation(
    ctx: StreamContext, body: ChatStreamRequest, intent: IntentResult
) -> AsyncGenerator[SSEEventBase, None]:
    if intent.intent == "feedback":
        await _record_feedback_intent(ctx, intent, body.message)

    # ── Fire DB reads before intro text: overlap with typewriter animation ──
    feedback_task = asyncio.create_task(get_feedback_context(ctx.session_id))
    ctx.background_tasks.append(feedback_task)
    if _is_replace_deck_request(body):
        product_ids_task = asyncio.create_task(get_previous_product_ids(ctx.session_id))
        ctx.background_tasks.append(product_ids_task)
    else:
        product_ids_task = None

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
    retrieval_task = start_stage_task(
        ctx,
        ctx.stages.run_retrieval(spec_criteria, top_n=8, feedback=feedback),
        timing_key="retrieve",
        background=True,
    )

    # ── Criteria generation (in parallel with speculative retrieval) ──
    yield ctx.thinking("criteria", "正在生成购买标准...")
    criteria_capture: CapturedStage[CriteriaPayload] = CapturedStage()
    ctx.ensure_active()
    async for event in _capture_stage_result(
        criteria_capture,
        run_with_heartbeat(
            ctx,
            ctx.stages.run_criteria(ctx.session_id, body, intent),
            "criteria",
            "正在生成购买标准...",
            timing_key="criteria",
        ),
    ):
        yield event
    criteria = criteria_capture.require("criteria")

    # ── Await speculative retrieval + post-filter, then delegate ──
    if not retrieval_task.task.done():
        yield ctx.thinking("searching", "正在检索匹配商品...")
    try:
        precomputed = await retrieval_task.task
    except Exception:
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
        ctx, body, criteria, precomputed_retrieval=precomputed, precomputed_feedback=feedback,
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
) -> AsyncGenerator[SSEEventBase, None]:
    ctx.ensure_active()

    if precomputed_feedback is not None:
        feedback = precomputed_feedback
    else:
        feedback = await get_feedback_context(ctx.session_id)
        if _is_replace_deck_request(body):
            previous_product_ids = await get_previous_product_ids(ctx.session_id)
            feedback = _feedback_with_avoided_products(feedback, previous_product_ids)

    if precomputed_retrieval is not None:
        retrieval = precomputed_retrieval
    else:
        yield ctx.thinking("searching", "正在检索匹配商品...")
        retrieval_capture: CapturedStage[RetrievalResult] = CapturedStage()
        ctx.ensure_active()
        async for event in _capture_stage_result(
            retrieval_capture,
            run_with_heartbeat(
                ctx,
                ctx.stages.run_retrieval(criteria, feedback=feedback),
                "searching",
                "正在检索匹配商品...",
                timing_key="retrieve",
            ),
        ):
            yield event
        retrieval = retrieval_capture.require("retrieval")

    products = retrieval.products
    evidences_by_product = dict(retrieval.evidence_by_product)

    # Branch 0: no matching products
    if not products:
        yield _criteria_card_event(ctx, criteria)
        async for event in stream_text(
            ctx,
            FOLLOWUP_TEXT_NO_MATCH,
            message_id=f"no_match_{ctx.turn_id}",
            node_id=f"no_match_{ctx.turn_id}",
        ):
            yield event
        await save_recommendation_turn(ctx.session_id, criteria, [], user_message=body.message)
        yield ctx.done("awaiting_criteria_adjustment")
        return

    # Emit product cards with pacing delay (built into _product_card_events)
    async for event in _product_card_events(ctx, criteria, products, evidences_by_product):
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

        yield _criteria_card_event(ctx, criteria)
        yield ctx.thinking("decision", "正在生成适配建议...")
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
        yield _final_decision_event(ctx, criteria, products, merged)
        await _persist_recommendation(ctx, body, criteria, retrieval)
        yield ctx.done("completed")
        return

    # Branch 2+: LLM explanation → criteria_card → followup guidance → done
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
    yield _criteria_card_event(ctx, criteria)

    # Step 2.5: first-round lightweight decision. This gives the turn a
    # complete loop while explicitly marking the decision as low-confidence.
    yield _final_decision_event(
        ctx,
        criteria,
        products,
        _lightweight_initial_decision(criteria, products, evidences_by_product, feedback),
        deck_id=ctx.deck_id,
    )

    # Step 3: followup guidance text ("你可以自然语言调整...")
    async for event in stream_text(
        ctx,
        FOLLOWUP_TEXT_DEFAULT,
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
                summary="当前候选都不太符合你的偏好。你可以换一组商品，或者调整筛选条件后重新推荐。",
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

    yield ctx.thinking("decision", "正在结合你的反馈生成最终建议...")
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
            "正在结合你的反馈生成最终建议...",
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

    yield _final_decision_event(ctx, criteria, products, merged, deck_id=deck_id)
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


def _lightweight_initial_decision(
    criteria: CriteriaPayload,
    products: list[ProductPayload],
    evidences_by_product: dict[str, list[EvidencePayload]],
    feedback: dict[str, list[str]],
) -> DecisionResult:
    scored = score_candidates(
        products,
        criteria,
        feedback=feedback,
        evidence_by_product=evidences_by_product,
    )
    winner_score = scored[0]
    winner = next(
        (product for product in products if product.product_id == winner_score.product_id),
        products[0],
    )
    evidence = evidences_by_product.get(winner.product_id, [])
    reason_atoms = build_reason_atoms(criteria, winner, evidence)
    why = [atom.text for atom in reason_atoms[:3] if atom.text]
    if not why:
        why = ["当前候选中综合匹配度最高。"]
    return DecisionResult(
        winner_product_id=winner.product_id,
        summary=f"当前先把{winner.name}作为首选候选；继续反馈后我会再收敛。",
        why=why,
        not_for=[],
        decision_status="needs_more_signal",
        confidence="low",
        next_step="continue_current_deck",
    )


def _criteria_card_event(ctx: StreamContext, criteria: CriteriaPayload) -> CriteriaCardEvent:
    return CriteriaCardEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"criteria_{criteria.criteria_id}",
        created_at_ms=now_ms(),
        criteria=criteria,
        quick_actions=criteria_quick_actions(category=criteria.category or None),
    )


async def _product_card_events(
    ctx: StreamContext,
    criteria: CriteriaPayload,
    products: list[ProductPayload],
    evidences_by_product: dict[str, list[EvidencePayload]],
) -> AsyncGenerator[ProductCardEvent, None]:
    for rank, product in enumerate(products, start=1):
        ctx.ensure_active()
        evidence = evidences_by_product.get(product.product_id)
        if evidence is None:
            evidence = await get_evidence(product)
        evidences_by_product[product.product_id] = evidence
        reason_atoms = build_reason_atoms(criteria, product, evidence)
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
            reason=reason_from_atoms(product, reason_atoms),
            reason_atoms=reason_atoms,
            risk_notes=[],
            evidence=evidence,
            actions=[
                QuickActionPayload(action_id="show_evidence", label="看证据", action="open_evidence"),
                QuickActionPayload(action_id="add_to_cart", label="加入购物车", action="add_to_cart"),
                QuickActionPayload(
                    action_id="dislike_product",
                    label="不喜欢这个",
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
                kind, value = await asyncio.wait_for(queue.get(), timeout=0.05)
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
            parts.append(f"{float(budget):g}元以内")
        except (ValueError, TypeError):
            pass
    product_type = constraints.get("product_type")
    if product_type:
        parts.append(str(product_type))
    scenario = constraints.get("use_scenario")
    if scenario:
        parts.append(str(scenario))
    skin = constraints.get("skin_type")
    if skin:
        parts.append(f"{skin}肌肤")
    if parts:
        intro = f"我先按{'、'.join(parts)}这几个条件找一组候选。"
        # When only category is present, add a guidance hint
        if len(parts) == 1 and intent.category:
            intro += "看到商品后也可以继续调整筛选范围。"
        return intro
    return INTRO_TEXT_NO_CONSTRAINTS


def _final_decision_event(
    ctx: StreamContext,
    criteria: CriteriaPayload,
    products: list[ProductPayload],
    decision: DecisionResult,
    deck_id: str | None = None,
) -> FinalDecisionEvent:
    alternatives = [
        AlternativePayload(product_id=p.product_id, name=p.name)
        for p in products
        if p.product_id != decision.winner_product_id
    ][:2]
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
        next_actions=[
            QuickActionPayload(
                action_id="cheaper",
                label="再便宜一点",
                action="criteria_patch",
                criteria_patch={"constraints": {"budget_max": _cheaper_budget_max(criteria)}},
            ),
            QuickActionPayload(action_id="compare", label="加入对比", action="compare"),
        ],
        decision_status=decision.decision_status,
        confidence=decision.confidence,
        next_step=decision.next_step,
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


INTENT_HANDLERS: dict[str, IntentHandler] = {
    "recommend": handle_recommendation,
    "clarify": handle_recommendation,
    "continue": handle_continue,
    "feedback": handle_recommendation,
    "view_cart": handle_view_cart,
    "add_to_cart": handle_add_to_cart,
    "remove_from_cart": handle_remove_from_cart,
    "update_cart_quantity": handle_update_cart_quantity,
    "chitchat": handle_chitchat,
}


async def _clarify_cart_target(ctx: StreamContext, question: str) -> AsyncGenerator[SSEEventBase, None]:
    yield ctx.thinking("clarifying", "需要确认购物车商品。")
    yield ClarificationEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"clarification_{ctx.turn_id}",
        created_at_ms=now_ms(),
        question=question,
        required_slots=["target_product"],
        suggested_options=[],
    )


def _cart_action_event(ctx: StreamContext, action: str, product_id: str, quantity: int, status: str) -> CartActionEvent:
    return CartActionEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"cart_{ctx.turn_id}",
        created_at_ms=now_ms(),
        action=action,
        product_id=product_id,
        quantity=quantity,
        status=status,
    )


def _cheaper_budget_max(criteria: CriteriaPayload) -> float:
    current = criteria.constraints.budget_max
    if current is None:
        return CHEAPER_BUDGET_DEFAULT_MAX
    return max(CHEAPER_BUDGET_MIN_MAX, round(current * CHEAPER_BUDGET_RATIO, 2))
