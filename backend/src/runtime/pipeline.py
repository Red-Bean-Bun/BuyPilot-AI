"""Real chat pipeline owner."""

from __future__ import annotations

import logging
import uuid
import weakref
from collections.abc import AsyncGenerator, Awaitable, Callable
from dataclasses import dataclass
from typing import Any

from src.runtime.cancel_registry import StreamCancelled, register_turn, unregister_turn
from src.runtime.handlers import INTENT_HANDLERS, handle_clarification
from src.runtime.message_rules import (
    COMMERCIAL_CLAIM_REPLY,
    extract_adjustment_hints,
    is_commercial_claim_question,
    is_replace_deck_phrase,
    maybe_intercept_budget_patch,
    message_with_image_context,
)
from src.runtime.stages.criteria import _constraint_chips, criteria_quick_actions, run_criteria
from src.runtime.stages.decision import run_decision
from src.runtime.stages.intent import run_intent
from src.runtime.stages.multimodal import run_multimodal
from src.runtime.stages.recommendation import run_recommendation_text, run_recommendation_text_stream, run_retrieval
from src.runtime.stages.slot_checker import check_required_slots
from src.config.domain_terms import KNOWN_CATEGORIES, is_supported_product_type
from src.runtime.streaming import RunRetrieval, StageResult, StreamContext, cancel_background_tasks, run_with_heartbeat
from src.services.audit import record_audit_event
from src.services.cancellation import clear_chat_turn, register_chat_turn
from src.services.conversation_state import get_previous_criteria, save_recommendation_turn
from src.services.fallbacks import reset_fallback_events
from src.services.request_context import update_request_context
from src.types.schemas import ChatStreamRequest, DecisionResult, IntentResult, RecommendationResult
from src.types.sse_events import (
    Constraints,
    CriteriaPayload,
    CriteriaCardEvent,
    ErrorEvent,
    EventSeq,
    EvidencePayload,
    ProductPayload,
    SSEEventBase,
    TextDeltaEvent,
    now_ms,
)

HEARTBEAT_INTERVAL_SECONDS = 0.8
PUBLIC_PIPELINE_ERROR_MESSAGE = "本轮导购处理失败，请稍后重试。"

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
        [CriteriaPayload, list[ProductPayload], dict[str, list[EvidencePayload]] | None], Awaitable[DecisionResult]
    ]


@dataclass(frozen=True)
class _ResolvedIntent:
    body: ChatStreamRequest
    intent: IntentResult


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

    if is_commercial_claim_question(pipeline_body.message):
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
        async for event in _emit_unsupported_product_type(ctx, resolved.intent):
            yield event
        return

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
        yield ctx.thinking("analyzing_image", "正在分析图片...")
        image_analysis: dict[str, Any] | None = None
        ctx.ensure_active()
        async for image_item in run_with_heartbeat(
            ctx,
            ctx.stages.run_multimodal(body.image_url),
            "analyzing_image",
            "正在分析图片...",
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

    # "换一组" / replace-deck: force recommend intent without LLM call
    if synthetic_intent is None and is_replace_deck_phrase(pipeline_body.message):
        prev_category = ""
        try:
            from src.services.conversation_state import get_previous_criteria

            prev = await get_previous_criteria(ctx.session_id)
            if prev and prev.category:
                prev_category = prev.category
        except Exception:
            pass
        synthetic_intent = IntentResult(intent="recommend", category=prev_category or None)

    if synthetic_intent is not None:
        intent = synthetic_intent
    else:
        yield ctx.thinking("understanding", "正在理解您的需求...")
        intent = None
        ctx.ensure_active()
        async for intent_item in run_with_heartbeat(
            ctx,
            ctx.stages.run_intent(ctx.session_id, pipeline_body),
            "understanding",
            "正在理解您的需求...",
            timing_key="intent",
        ):
            if isinstance(intent_item, StageResult):
                intent = intent_item.value
            else:
                yield intent_item
        if intent is None:
            raise RuntimeError("intent stage completed without a result.")

    # Merge natural-language adjustment hints into extracted constraints
    adjustment = extract_adjustment_hints(pipeline_body.message)
    if adjustment and intent.intent in {"recommend", "clarify", "feedback"}:
        merged = dict(intent.extracted_constraints or {})
        for key, value in adjustment.items():
            if key in merged and isinstance(merged[key], list) and isinstance(value, list):
                merged[key] = list(dict.fromkeys([*merged[key], *value]))
            else:
                merged[key] = value
        intent = intent.model_copy(update={"extracted_constraints": merged})

    yield StageResult(_ResolvedIntent(body=pipeline_body, intent=intent))


def _missing_slots(pipeline_body: ChatStreamRequest, intent: IntentResult) -> list[str]:
    if pipeline_body.criteria_patch or pipeline_body.image_url:
        return []
    return check_required_slots(pipeline_body.message, intent)


async def _emit_clarification(
    ctx: StreamContext, pipeline_body: ChatStreamRequest, intent: IntentResult, missing_slots: list[str]
) -> AsyncGenerator[SSEEventBase, None]:
    partial = _intent_to_partial_criteria(intent, pipeline_body.message)
    if _should_emit_partial_criteria(missing_slots, partial):
        yield _partial_criteria_card_event(ctx, partial)
    async for event in handle_clarification(ctx, missing_slots):
        yield event
    await save_recommendation_turn(ctx.session_id, partial, [], user_message=pipeline_body.message)


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
    for key, value in previous.constraints.model_dump().items():
        if key not in constraints and _has_context_value(value):
            constraints[key] = value
    category = intent.category or previous.category or None
    return _ResolvedIntent(
        body=resolved.body,
        intent=intent.model_copy(update={"category": category, "extracted_constraints": constraints}),
    )


def _should_merge_previous_context(intent: IntentResult) -> bool:
    if intent.intent in {"clarify", "continue"}:
        return True
    if intent.intent != "recommend" or intent.category:
        return False
    return any(_has_context_value(value) for value in (intent.extracted_constraints or {}).values())


def _unsupported_product_type(intent: IntentResult) -> bool:
    if intent.intent not in {"recommend", "clarify"}:
        return False
    if intent.category and intent.category not in KNOWN_CATEGORIES:
        return True
    product_type = (intent.extracted_constraints or {}).get("product_type")
    return bool(product_type and not is_supported_product_type(str(product_type)))


def _should_emit_partial_criteria(missing_slots: list[str], criteria: CriteriaPayload) -> bool:
    return "category" not in missing_slots and bool(criteria.category)


def _partial_criteria_card_event(ctx: StreamContext, criteria: CriteriaPayload) -> CriteriaCardEvent:
    return CriteriaCardEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"criteria_{criteria.criteria_id}",
        created_at_ms=now_ms(),
        criteria=criteria,
        quick_actions=criteria_quick_actions(),
    )


async def _emit_unsupported_product_type(
    ctx: StreamContext, intent: IntentResult
) -> AsyncGenerator[SSEEventBase, None]:
    product_type = (intent.extracted_constraints or {}).get("product_type") or intent.category or "这类商品"
    yield TextDeltaEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"unsupported_{ctx.turn_id}",
        created_at_ms=now_ms(),
        message_id=f"unsupported_{ctx.turn_id}",
        delta=f"当前商品库暂不覆盖{product_type}，我不能基于现有数据给出可靠推荐。",
        done=True,
    )
    yield ctx.done()


def _has_context_value(value: object) -> bool:
    if value is None:
        return False
    if isinstance(value, list | tuple | set | dict):
        return bool(value)
    return True


def _current_stages() -> PipelineStages:
    """Resolve stage implementations via module attributes so tests can monkeypatch them."""
    return PipelineStages(
        run_multimodal=run_multimodal,
        run_intent=run_intent,
        run_criteria=run_criteria,
        run_retrieval=run_retrieval,
        run_recommendation_text=run_recommendation_text,
        run_recommendation_text_stream=run_recommendation_text_stream,
        run_decision=run_decision,
    )


async def _emit_commercial_claim_reply(ctx: StreamContext) -> AsyncGenerator[SSEEventBase, None]:
    yield ctx.thinking("understanding", "正在处理...")
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
    """Construct a partial CriteriaPayload from intent for multi-turn clarification continuity.

    The resulting payload captures the category and any constraints already extracted
    from the user's message. It reuses the existing conversation state machinery —
    get_conversation_summary and get_previous_criteria both pick it up automatically.
    """
    allowed = set(Constraints.model_fields)
    constraint_kwargs: dict[str, Any] = {}
    for key, value in (intent.extracted_constraints or {}).items():
        if key in allowed and value is not None:
            constraint_kwargs[key] = value
    constraints = Constraints(**constraint_kwargs)
    category = intent.category or ""
    chips = [category] if category else []
    chips.extend(_constraint_chips(constraints))
    return CriteriaPayload(
        criteria_id=f"pending_{uuid.uuid4().hex[:8]}",
        category=category,
        summary="，".join(chips) if chips else "",
        chips=chips,
        constraints=constraints,
        field_sources={
            **({"category": "user"} if category else {}),
            **{f"constraints.{key}": "user" for key in constraint_kwargs},
        },
    )
