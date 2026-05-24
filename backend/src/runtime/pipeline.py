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
from src.runtime.streaming import StageResult, StreamContext, cancel_background_tasks, run_with_heartbeat
from src.services.fallbacks import reset_fallback_events
from src.types.schemas import ChatStreamRequest
from src.types.sse_events import ErrorEvent, EventSeq, SSEEventBase, now_ms

HEARTBEAT_INTERVAL_SECONDS = 0.8
PUBLIC_PIPELINE_ERROR_MESSAGE = "本轮导购处理失败，请稍后重试。"

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class PipelineStages:
    run_multimodal: Callable[[str | None], Awaitable[dict[str, Any] | None]]
    run_intent: Callable[[str, ChatStreamRequest], Awaitable[Any]]
    run_criteria: Callable[..., Awaitable[Any]]
    run_retrieval: Callable[..., Awaitable[Any]]
    run_recommendation_text: Callable[..., Awaitable[Any]]
    run_decision: Callable[..., Awaitable[Any]]


async def chat_stream(session_id: str, body: ChatStreamRequest) -> AsyncGenerator[SSEEventBase, None]:
    reset_fallback_events()
    turn_id = body.client_turn_id or f"turn_{uuid.uuid4().hex[:8]}"
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
        async for event in _run_chat_turn(ctx, body):
            yield event
    except StreamCancelled:
        cancel_background_tasks(ctx.background_tasks)
        yield ctx.done()
    except Exception:
        cancel_background_tasks(ctx.background_tasks)
        logger.exception("chat_stream failed: session_id=%s turn_id=%s", session_id, turn_id)
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
        unregister_turn(session_id, turn_id)


async def _run_chat_turn(ctx: StreamContext, body: ChatStreamRequest) -> AsyncGenerator[SSEEventBase, None]:
    pipeline_body = body
    if body.image_url:
        yield ctx.thinking("analyzing_image", "正在分析图片...")
        image_analysis = None
        ctx.ensure_active()
        async for item in run_with_heartbeat(
            ctx,
            ctx.stages.run_multimodal(body.image_url),
            "analyzing_image",
            "正在分析图片...",
            timing_key="image_analysis",
        ):
            if isinstance(item, StageResult):
                image_analysis = item.value
            else:
                yield item
        pipeline_body = body.model_copy(update={"message": _message_with_image_context(body.message, image_analysis)})

    yield ctx.thinking("understanding", "正在理解您的需求...")
    intent = None
    ctx.ensure_active()
    async for item in run_with_heartbeat(
        ctx,
        ctx.stages.run_intent(ctx.session_id, pipeline_body),
        "understanding",
        "正在理解您的需求...",
        timing_key="intent",
    ):
        if isinstance(item, StageResult):
            intent = item.value
        else:
            yield item

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
