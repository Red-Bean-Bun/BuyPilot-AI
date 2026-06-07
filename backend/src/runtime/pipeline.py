"""Real chat pipeline owner."""

from __future__ import annotations

import logging
import uuid
import weakref
from collections.abc import AsyncGenerator, Awaitable, Callable
from dataclasses import dataclass
from typing import Any

from src.runtime.cancel_registry import StreamCancelled, register_turn, unregister_turn
from src.runtime.handlers import INTENT_HANDLERS, handle_clarification, _criteria_card_event
from src.services.intent_resolution import (
    has_context_value,
    resolve_intent_constraints,
)
from src.services.message_rules import (
    COMMERCIAL_CLAIM_REPLY,
    is_compare_phrase,
    is_commercial_claim_question,
    is_replace_deck_phrase,
    maybe_checkout_intent,
    maybe_intercept_budget_patch,
    maybe_shopping_intent,
    message_with_image_context,
    resolve_compare_ids_mixed,
    resolve_compare_targets,
)
from src.runtime.stages.criteria import criteria_from_intent, run_criteria
from src.runtime.stages.decision import run_decision
from src.runtime.stages.intent import run_intent
from src.runtime.stages.multimodal import run_image_embedding, run_multimodal
from src.runtime.stages.recommendation import run_recommendation_text, run_recommendation_text_stream, run_retrieval
from src.runtime.stages.slot_checker import check_required_slots
from src.config import user_messages as msg
from src.config.domain_terms import (
    KNOWN_CATEGORIES,
    is_known_brand_or_synonym,
    is_supported_product_type,
    normalize_category,
)
from src.runtime.streaming import RunRetrieval, StageResult, StreamContext, cancel_background_tasks, run_with_heartbeat
from src.services.audit import record_audit_event
from src.services.cancellation import clear_chat_turn, register_chat_turn
from src.services.cart import get_session_cart
from src.services.conversation_state import get_previous_criteria, get_previous_product_ids, save_recommendation_turn
from src.services.fallbacks import reset_fallback_events
from src.services.feedback import get_feedback_context
from src.services.request_context import update_request_context
from src.types.schemas import ChatStreamRequest, DecisionResult, IntentResult, RecommendationResult
from src.types.sse_events import (
    CriteriaPayload,
    ErrorEvent,
    EventSeq,
    EvidencePayload,
    ProductPayload,
    SSEEventBase,
    TextDeltaEvent,
    now_ms,
)

HEARTBEAT_INTERVAL_SECONDS = 0.8
PUBLIC_PIPELINE_ERROR_MESSAGE = msg.PIPELINE_ERROR

logger = logging.getLogger(__name__)


class _TurnGuard:
    """Ensures cancel_token cleanup even if the generator is abandoned without iteration."""

    def __init__(self, session_id: str, turn_id: str) -> None:
        self.session_id = session_id
        self.turn_id = turn_id
        self._finalizer = weakref.finalize(self, unregister_turn, session_id, turn_id)

    def detach(self) -> None:
        self._finalizer.detach()


@dataclass(frozen=True)
class PipelineStages:
    run_multimodal: Callable[[str | None], Awaitable[dict[str, Any] | None]]
    run_image_embedding: Callable[[str | None], Awaitable[list[float] | None]]
    run_intent: Callable[[str, ChatStreamRequest], Awaitable[IntentResult]]
    run_criteria: Callable[[str, ChatStreamRequest, IntentResult], Awaitable[CriteriaPayload]]
    run_retrieval: RunRetrieval
    run_recommendation_text: Callable[
        [CriteriaPayload, list[ProductPayload], dict[str, list[EvidencePayload]] | None],
        Awaitable[RecommendationResult],
    ]
    run_recommendation_text_stream: Callable[
        [CriteriaPayload, list[ProductPayload], dict[str, list[EvidencePayload]] | None],
        AsyncGenerator[str, None],
    ]
    run_decision: Callable[
        ...,
        Awaitable[DecisionResult],
    ]


@dataclass(frozen=True)
class _ResolvedIntent:
    body: ChatStreamRequest
    intent: IntentResult
    skip_slot_check: bool = False


async def chat_stream(session_id: str, body: ChatStreamRequest) -> AsyncGenerator[SSEEventBase, None]:
    reset_fallback_events()
    turn_id = body.client_turn_id or f"turn_{uuid.uuid4().hex[:8]}"
    update_request_context(trace_id=body.client_trace_id, session_id=session_id, turn_id=turn_id)
    cancel_token = register_turn(session_id, turn_id)
    guard = _TurnGuard(session_id, turn_id)
    ctx = StreamContext(
        session_id=session_id,
        turn_id=turn_id,
        deck_id=f"deck_{turn_id}",
        seq=EventSeq(turn_id),
        cancel_token=cancel_token,
        stages=_current_stages(),
        heartbeat_interval_seconds=HEARTBEAT_INTERVAL_SECONDS,
    )

    try:
        await register_chat_turn(session_id, turn_id, trace_id=body.client_trace_id)
        await record_audit_event(
            "chat.turn_started",
            session_id=session_id,
            turn_id=turn_id,
            trace_id=body.client_trace_id,
            resource_type="chat_turn",
            resource_id=turn_id,
            metadata={"has_image": bool(body.image_url), "message_chars": len(body.message)},
        )
        async for event in _run_chat_turn(ctx, body):
            yield event
        await record_audit_event(
            "chat.turn_completed",
            session_id=session_id,
            turn_id=turn_id,
            trace_id=body.client_trace_id,
            resource_type="chat_turn",
            resource_id=turn_id,
            metadata={"stage_timings_ms": ctx.stage_timings_ms},
        )
    except StreamCancelled:
        cancel_background_tasks(ctx.background_tasks)
        await record_audit_event(
            "chat.turn_cancelled",
            session_id=session_id,
            turn_id=turn_id,
            trace_id=body.client_trace_id,
            resource_type="chat_turn",
            resource_id=turn_id,
            metadata={"stage_timings_ms": ctx.stage_timings_ms},
        )
        yield ctx.done("cancelled")
    except Exception as exc:
        cancel_background_tasks(ctx.background_tasks)
        logger.exception("chat_stream failed: session_id=%s turn_id=%s", session_id, turn_id)
        await record_audit_event(
            "chat.turn_failed",
            session_id=session_id,
            turn_id=turn_id,
            trace_id=body.client_trace_id,
            resource_type="chat_turn",
            resource_id=turn_id,
            metadata={"error_type": type(exc).__name__, "stage_timings_ms": ctx.stage_timings_ms},
        )
        yield ErrorEvent(
            session_id=session_id,
            turn_id=turn_id,
            seq=ctx.seq.next(),
            event_id=ctx.seq.event_id(),
            node_id=f"error_{turn_id}",
            created_at_ms=now_ms(),
            code="PIPELINE_ERROR",
            message=f"{PUBLIC_PIPELINE_ERROR_MESSAGE} trace_id={turn_id}",
            retryable=True,
        )
        yield ctx.done("error")
    finally:
        guard.detach()
        await clear_chat_turn(session_id, turn_id)
        unregister_turn(session_id, turn_id)


async def _run_chat_turn(ctx: StreamContext, body: ChatStreamRequest) -> AsyncGenerator[SSEEventBase, None]:
    pipeline_body = None
    async for item in _prepare_pipeline_body(ctx, body):
        if isinstance(item, StageResult):
            pipeline_body = item.value
        else:
            yield item
    if pipeline_body is None:
        raise RuntimeError("pipeline body stage completed without a result.")

    if is_commercial_claim_question(pipeline_body.message) and maybe_checkout_intent(pipeline_body.message) is None:
        async for event in _emit_commercial_claim_reply(ctx):
            yield event
        return

    resolved = None
    async for item in _resolve_intent(ctx, pipeline_body):
        if isinstance(item, StageResult):
            resolved = item.value
        else:
            yield item
    if resolved is None:
        raise RuntimeError("intent stage completed without a result.")
    resolved = await _merge_followup_context(ctx.session_id, resolved)

    if _unsupported_product_type(resolved.intent):
        _pt = (resolved.intent.extracted_constraints or {}).get("product_type")
        # When the "unsupported" product_type is actually a brand, skip the
        # misleading "not covered" text — the brand matcher will pick it up
        # and retrieval will find matching products.
        if _pt and is_known_brand_or_synonym(str(_pt)):
            constraints = dict(resolved.intent.extracted_constraints or {})
            constraints["product_type"] = None
            resolved = _ResolvedIntent(
                body=resolved.body,
                intent=resolved.intent.model_copy(update={"extracted_constraints": constraints}),
            )
        else:
            async for event in _emit_unsupported_product_type(ctx, resolved.intent):
                yield event
            return

    if not resolved.skip_slot_check:
        missing_slots = _missing_slots(resolved.body, resolved.intent)
        if missing_slots:
            async for event in _emit_clarification(ctx, resolved.body, resolved.intent, missing_slots):
                yield event
            return

    async for event in _dispatch_intent_handler(ctx, resolved.body, resolved.intent):
        yield event


async def _prepare_pipeline_body(
    ctx: StreamContext, body: ChatStreamRequest
) -> AsyncGenerator[SSEEventBase | StageResult[ChatStreamRequest], None]:
    if body.image_url:
        yield ctx.thinking("analyzing_image", msg.THINKING_ANALYZING_IMAGE)
        image_analysis: dict[str, Any] | None = None
        ctx.ensure_active()
        async for image_item in run_with_heartbeat(
            ctx,
            ctx.stages.run_multimodal(body.image_url),
            "analyzing_image",
            msg.THINKING_ANALYZING_IMAGE,
            timing_key="image_analysis",
        ):
            if isinstance(image_item, StageResult):
                image_analysis = image_item.value
            else:
                yield image_item
        body = body.model_copy(update={"message": message_with_image_context(body.message, image_analysis)})
    yield StageResult(body)


async def _resolve_intent(
    ctx: StreamContext, pipeline_body: ChatStreamRequest
) -> AsyncGenerator[SSEEventBase | StageResult[_ResolvedIntent], None]:
    # Deterministic pre-checks before LLM intent (铁律3)
    pipeline_body, synthetic_intent = await maybe_intercept_budget_patch(ctx.session_id, pipeline_body)

    # Converge signal from frontend: skip LLM intent, force continue handler
    if synthetic_intent is None and pipeline_body.converge:
        prev = await get_previous_criteria(ctx.session_id)
        synthetic_intent = IntentResult(
            intent="continue",
            confidence=1.0,
            category=prev.category or None if prev else None,
        )

    # Deterministic shopping-intent pre-check (铁律3):
    # short-circuit LLM intent for obvious shopping / non-shopping phrases.
    # Only fires when no higher-priority rule has claimed the turn, and
    # only for text-only messages (image uploads carry VL analysis context).
    if synthetic_intent is None and not pipeline_body.image_url and not pipeline_body.criteria_patch:
        determined = maybe_checkout_intent(pipeline_body.message) or maybe_shopping_intent(pipeline_body.message)
        if determined is not None and determined.intent == "checkout_confirm":
            cart = await get_session_cart(ctx.session_id)
            if cart.total_items == 0:
                determined = maybe_shopping_intent(pipeline_body.message)
        if determined is not None:
            synthetic_intent = determined

    # "换一组" / replace-deck: force recommend intent without LLM call
    if synthetic_intent is None and is_replace_deck_phrase(pipeline_body.message):
        prev = await get_previous_criteria(ctx.session_id)
        if prev is not None:
            constraints = {
                key: value for key, value in prev.constraints.model_dump().items() if has_context_value(value)
            }
            synthetic_intent = IntentResult(
                intent="recommend",
                category=prev.category or None,
                extracted_constraints=constraints,
            )
        else:
            synthetic_intent = IntentResult(intent="recommend")

    # Compare: deterministic pre-check for "对比第一个和第二个" style requests
    # or explicit compare_product_ids from the client
    if synthetic_intent is None:
        client_ids = pipeline_body.compare_product_ids or []
        if len(client_ids) >= 2:
            prev = await get_previous_criteria(ctx.session_id)
            synthetic_intent = IntentResult(
                intent="compare",
                category=prev.category or None if prev else None,
                compare_product_ids=client_ids[:4],
            )
        elif is_compare_phrase(pipeline_body.message):
            previous_ids = await get_previous_product_ids(ctx.session_id)
            if previous_ids:
                resolved = resolve_compare_targets(pipeline_body.message, previous_ids)
                if len(resolved) >= 2:
                    prev = await get_previous_criteria(ctx.session_id)
                    synthetic_intent = IntentResult(
                        intent="compare",
                        category=prev.category or None if prev else None,
                        compare_product_ids=resolved,
                    )

    if synthetic_intent is not None:
        intent = synthetic_intent
    else:
        yield ctx.thinking("understanding", msg.THINKING_UNDERSTANDING)
        intent = None
        ctx.ensure_active()
        async for intent_item in run_with_heartbeat(
            ctx,
            ctx.stages.run_intent(ctx.session_id, pipeline_body),
            "understanding",
            msg.THINKING_UNDERSTANDING,
            timing_key="intent",
        ):
            if isinstance(intent_item, StageResult):
                intent = intent_item.value
            else:
                yield intent_item
        if intent is None:
            raise RuntimeError("intent stage completed without a result.")

    # Apply deterministic post-processing to refine intent constraints
    intent = resolve_intent_constraints(intent, pipeline_body.message)

    # Guard: reclassify unresolvable add_to_cart → recommend
    # When no previous products exist, the user said "加到购物车" in a vacuum.
    # Reclassify to recommend and skip slot_checker to avoid a useless
    # "which category?" clarification — just show broad candidates.
    skip_slot_check = False
    if intent.intent == "add_to_cart" and not intent.target_product_id:
        previous_ids = await get_previous_product_ids(ctx.session_id)
        if not previous_ids:
            logger.info("Reclassified add_to_cart → recommend (no product reference)")
            intent = intent.model_copy(update={"intent": "recommend"})
            skip_slot_check = True

    # Guard: skip slot check for scenario-based requests (gift/interest/travel).
    # These requests intentionally lack category — shopping_strategy will determine
    # the direction. Without this guard, slot_checker blocks with "which category?"
    # before handle_recommendation can invoke shopping_strategy.
    if intent.intent in {"recommend", "clarify"} and not skip_slot_check:
        from src.services.shopping_strategy import is_likely_shopping_strategy_request
        if is_likely_shopping_strategy_request(pipeline_body, intent):
            logger.info("Skipping slot check for scenario-based request")
            skip_slot_check = True

    # Guard: resolve compare product references to actual IDs
    # Priority: LLM integer indices > string IDs > regex fallback on message
    if intent.intent == "compare":
        previous_ids = await get_previous_product_ids(ctx.session_id)
        client_ids = pipeline_body.compare_product_ids or []
        llm_ids = intent.compare_product_ids or []

        # Merge client and LLM IDs (client takes priority)
        combined_ids = client_ids if client_ids else llm_ids

        if len(previous_ids) >= 2 and combined_ids:
            # Use mixed resolver: handles int indices, string IDs, and regex fallback
            resolved = resolve_compare_ids_mixed(combined_ids, pipeline_body.message, previous_ids)
            if len(resolved) >= 2:
                intent = intent.model_copy(update={"compare_product_ids": resolved})
            else:
                logger.info("Reclassified compare → recommend (no ≥2 resolvable IDs from %s)", combined_ids)
                intent = intent.model_copy(update={"intent": "recommend"})
        elif len(previous_ids) < 2:
            logger.info("Reclassified compare → recommend (need ≥2 previous products, have %d)", len(previous_ids))
            intent = intent.model_copy(update={"intent": "recommend"})
        else:
            logger.info("Reclassified compare → recommend (no compare_product_ids provided)")
            intent = intent.model_copy(update={"intent": "recommend"})

    yield StageResult(_ResolvedIntent(body=pipeline_body, intent=intent, skip_slot_check=skip_slot_check))


def _missing_slots(pipeline_body: ChatStreamRequest, intent: IntentResult) -> list[str]:
    if intent.intent == "compare":
        return []
    if pipeline_body.criteria_patch or pipeline_body.image_url:
        return []
    return check_required_slots(pipeline_body.message, intent)


async def _emit_clarification(
    ctx: StreamContext, pipeline_body: ChatStreamRequest, intent: IntentResult, missing_slots: list[str]
) -> AsyncGenerator[SSEEventBase, None]:
    partial = _intent_to_partial_criteria(intent, pipeline_body.message)
    if _should_emit_partial_criteria(missing_slots, partial):
        yield _criteria_card_event(ctx, partial)

    # Pre-retrieve products so subsequent compare/intent turns have candidates
    # to reference, even when clarification is needed (e.g. budget for phones).
    product_ids: list[str] = []
    try:
        feedback = await get_feedback_context(ctx.session_id)
        retrieval = await run_retrieval(partial, top_n=5, feedback=feedback)
        product_ids = [p.product_id for p in (retrieval.products or [])]
    except Exception:
        logger.warning("Pre-retrieval in clarification failed", exc_info=True)

    async for event in handle_clarification(ctx, missing_slots):
        yield event
    await save_recommendation_turn(ctx.session_id, partial, product_ids, user_message=pipeline_body.message)


async def _dispatch_intent_handler(
    ctx: StreamContext, pipeline_body: ChatStreamRequest, intent: IntentResult
) -> AsyncGenerator[SSEEventBase, None]:
    handler = INTENT_HANDLERS.get(intent.intent)
    if handler is None:
        logger.warning("Unhandled intent '%s' — falling back to clarification.", intent.intent)
        async for event in handle_clarification(ctx, ["category"]):
            yield event
        return
    async for event in handler(ctx, pipeline_body, intent):
        yield event


async def _merge_followup_context(session_id: str, resolved: _ResolvedIntent) -> _ResolvedIntent:
    intent = resolved.intent
    if not _should_merge_previous_context(intent):
        return resolved
    previous = await get_previous_criteria(session_id)
    if previous is None:
        return resolved
    constraints = dict(intent.extracted_constraints or {})

    # Detect topic switch: when the user explicitly changes product_type, don't
    # carry brand_prefer from the previous turn. Otherwise "有苹果手机吗" leaks
    # brand_prefer into "有电脑吗" or "Mac", forcing the wrong products.
    topic_switched = _is_topic_switch(intent, previous)

    for key, value in previous.constraints.model_dump().items():
        if key not in constraints and has_context_value(value):
            if topic_switched and key in ("brand_prefer", "brand_avoid", "product_type"):
                continue
            constraints[key] = value
    category = intent.category or previous.category or None
    return _ResolvedIntent(
        body=resolved.body,
        intent=intent.model_copy(update={"category": category, "extracted_constraints": constraints}),
    )


def _is_topic_switch(intent: IntentResult, previous: CriteriaPayload) -> bool:
    """True when the user switches away from the previous turn's topic.

    Detected when:
    - The current turn has a different product_type (e.g. 手机→电脑)
    - The current turn explicitly names a brand but no product_type,
      suggesting a new search direction (e.g. "Mac" after phone discussion)
    """
    current_pt = (intent.extracted_constraints or {}).get("product_type")
    previous_pt = previous.constraints.product_type
    if current_pt and previous_pt:
        from src.config.domain_terms import normalize_product_type

        return normalize_product_type(current_pt) != normalize_product_type(previous_pt)
    # Current turn names a brand but no product_type — treat as fresh search
    current_bp = (intent.extracted_constraints or {}).get("brand_prefer")
    if current_bp and not current_pt and previous_pt:
        return True
    return False


def _should_merge_previous_context(intent: IntentResult) -> bool:
    if intent.intent in {"clarify", "continue", "compare"}:
        return True
    if intent.intent != "recommend":
        return False
    if intent.category:
        return False
    return True


def _unsupported_product_type(intent: IntentResult) -> bool:
    if intent.intent not in {"recommend", "clarify"}:
        return False
    if intent.category and intent.category not in KNOWN_CATEGORIES:
        return True
    product_type = (intent.extracted_constraints or {}).get("product_type")
    if normalize_category(product_type) in KNOWN_CATEGORIES:
        return False
    return bool(product_type and not is_supported_product_type(str(product_type)))


def _should_emit_partial_criteria(missing_slots: list[str], criteria: CriteriaPayload) -> bool:
    return "category" not in missing_slots and bool(criteria.category)


async def _emit_unsupported_product_type(
    ctx: StreamContext, intent: IntentResult
) -> AsyncGenerator[SSEEventBase, None]:
    product_type = (
        (intent.extracted_constraints or {}).get("product_type")
        or intent.category
        or msg.UNSUPPORTED_PRODUCT_TYPE_FALLBACK
    )
    yield TextDeltaEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"unsupported_{ctx.turn_id}",
        created_at_ms=now_ms(),
        message_id=f"unsupported_{ctx.turn_id}",
        delta=msg.UNSUPPORTED_PRODUCT_TYPE_TEMPLATE.format(product_type=product_type),
        done=True,
    )
    yield ctx.done()


def _current_stages() -> PipelineStages:
    """Resolve stage implementations via module attributes so tests can monkeypatch them."""
    return PipelineStages(
        run_multimodal=run_multimodal,
        run_image_embedding=run_image_embedding,
        run_intent=run_intent,
        run_criteria=run_criteria,
        run_retrieval=run_retrieval,
        run_recommendation_text=run_recommendation_text,
        run_recommendation_text_stream=run_recommendation_text_stream,
        run_decision=run_decision,
    )


async def _emit_commercial_claim_reply(ctx: StreamContext) -> AsyncGenerator[SSEEventBase, None]:
    yield ctx.thinking("understanding", msg.THINKING_PROCESSING)
    yield TextDeltaEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"ai_text_{ctx.turn_id}",
        created_at_ms=now_ms(),
        message_id=f"msg_{ctx.turn_id}",
        delta=COMMERCIAL_CLAIM_REPLY,
        done=True,
    )
    yield ctx.done()


def _intent_to_partial_criteria(intent: IntentResult, message: str) -> CriteriaPayload:
    """Construct a partial CriteriaPayload from intent for multi-turn clarification continuity."""
    return criteria_from_intent(intent)
