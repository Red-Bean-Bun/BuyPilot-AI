"""Real chat pipeline owner."""

from __future__ import annotations

import logging
import uuid
from collections.abc import AsyncGenerator, Awaitable, Callable
from dataclasses import dataclass
from typing import Any

from src.runtime.cancel_registry import StreamCancelled, register_turn, unregister_turn
from src.runtime.handlers import INTENT_HANDLERS, handle_clarification, handle_recommendation
from src.runtime.stages.criteria import run_criteria
from src.runtime.stages.decision import run_decision
from src.runtime.stages.intent import run_intent
from src.runtime.stages.multimodal import run_multimodal
from src.runtime.stages.recommendation import run_recommendation_text, run_retrieval
from src.runtime.stages.slot_checker import check_required_slots
from src.runtime.streaming import RunRetrieval, StageResult, StreamContext, cancel_background_tasks, run_with_heartbeat
from src.services.audit import record_audit_event
from src.services.async_io import run_sync_io
from src.services.cancellation import clear_chat_turn, register_chat_turn
from src.services.fallbacks import reset_fallback_events
from src.services.request_context import update_request_context
from src.types.schemas import ChatStreamRequest, DecisionResult, IntentResult, RecommendationResult
from src.types.sse_events import CriteriaPayload, ErrorEvent, EventSeq, ProductPayload, SSEEventBase, now_ms

HEARTBEAT_INTERVAL_SECONDS = 0.8
PUBLIC_PIPELINE_ERROR_MESSAGE = "本轮导购处理失败，请稍后重试。"

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class PipelineStages:
    run_multimodal: Callable[[str | None], Awaitable[dict[str, Any] | None]]
    run_intent: Callable[[str, ChatStreamRequest], Awaitable[IntentResult]]
    run_criteria: Callable[[str, ChatStreamRequest, IntentResult], Awaitable[CriteriaPayload]]
    run_retrieval: RunRetrieval
    run_recommendation_text: Callable[[CriteriaPayload, list[ProductPayload]], Awaitable[RecommendationResult]]
    run_decision: Callable[[CriteriaPayload, list[ProductPayload]], Awaitable[DecisionResult]]


async def chat_stream(session_id: str, body: ChatStreamRequest) -> AsyncGenerator[SSEEventBase, None]:
    reset_fallback_events()
    turn_id = body.client_turn_id or f"turn_{uuid.uuid4().hex[:8]}"
    update_request_context(trace_id=body.client_trace_id, session_id=session_id, turn_id=turn_id)
    cancel_token = register_turn(session_id, turn_id)
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
        await run_sync_io(register_chat_turn, session_id, turn_id, trace_id=body.client_trace_id)
        await run_sync_io(
            record_audit_event,
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
        await run_sync_io(
            record_audit_event,
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
        await run_sync_io(
            record_audit_event,
            "chat.turn_cancelled",
            session_id=session_id,
            turn_id=turn_id,
            trace_id=body.client_trace_id,
            resource_type="chat_turn",
            resource_id=turn_id,
            metadata={"stage_timings_ms": ctx.stage_timings_ms},
        )
        yield ctx.done()
    except Exception as exc:
        cancel_background_tasks(ctx.background_tasks)
        logger.exception("chat_stream failed: session_id=%s turn_id=%s", session_id, turn_id)
        await run_sync_io(
            record_audit_event,
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
        yield ctx.done()
    finally:
        await run_sync_io(clear_chat_turn, session_id, turn_id)
        unregister_turn(session_id, turn_id)


async def _run_chat_turn(ctx: StreamContext, body: ChatStreamRequest) -> AsyncGenerator[SSEEventBase, None]:
    pipeline_body = body
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
        pipeline_body = body.model_copy(update={"message": _message_with_image_context(body.message, image_analysis)})

    yield ctx.thinking("understanding", "正在理解您的需求...")
    intent: IntentResult | None = None
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

    missing_slots = [] if body.criteria_patch or body.image_url else check_required_slots(pipeline_body.message, intent)
    if missing_slots:
        async for event in handle_clarification(ctx, missing_slots):
            yield event
        return

    handler = INTENT_HANDLERS.get(intent.intent, handle_recommendation)
    async for event in handler(ctx, pipeline_body, intent):
        yield event


def _current_stages() -> PipelineStages:
    return PipelineStages(
        run_multimodal=run_multimodal,
        run_intent=run_intent,
        run_criteria=run_criteria,
        run_retrieval=run_retrieval,
        run_recommendation_text=run_recommendation_text,
        run_decision=run_decision,
    )


def _message_with_image_context(message: str, image_analysis: dict[str, Any] | None) -> str:
    if not image_analysis:
        return message
    parts = []
    category_hint = image_analysis.get("category_hint")
    description = image_analysis.get("description")
    visible_traits = image_analysis.get("visible_traits")
    if category_hint:
        parts.append(f"品类={category_hint}")
    if description:
        parts.append(f"描述={description}")
    if isinstance(visible_traits, list) and visible_traits:
        parts.append("可见特征=" + "，".join(str(item) for item in visible_traits))
    return f"{message}\n图片分析：" + "；".join(parts) if parts else message
