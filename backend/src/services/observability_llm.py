"""Service layer for LLM call observability.

Provides fire-and-forget background recording so observability writes
never block the main chat flow.
"""

from __future__ import annotations

import asyncio
import hashlib
import json
import logging
from typing import Any, Coroutine

from src.config.settings import get_settings
from src.repos.observability_llm import insert_llm_call, insert_sse_event
from src.services.request_context import get_request_context

logger = logging.getLogger(__name__)
_OBSERVABILITY_TASKS: set[asyncio.Task[None]] = set()


async def safe_observability_task(
    coro: Coroutine[Any, Any, Any],
    session_id: str | None,
    turn_id: str | None,
    label: str,
) -> None:
    """Background task wrapper that catches all exceptions and only logs warnings."""
    if not get_settings().observability_local_enabled:
        coro.close()
        return
    try:
        await coro
    except Exception as exc:
        logger.warning(f"Observability task {label} failed: {exc}", exc_info=True)


async def record_llm_call(
    *,
    task: str,
    profile: str,
    model: str,
    provider: str,
    status: str,
    duration_ms: float,
    messages: list[dict[str, Any]],
    response: str | None,
    parsed_json: dict[str, Any] | None,
    validation_error: str | None,
    token_usage: dict[str, Any] | None,
    fallback_from: str | None,
    error_type: str | None,
    error_message: str | None,
    error_raw: str | None,
) -> None:
    """Main entry point for recording an LLM call observation."""
    context = get_request_context()
    session_id = context.session_id if context else None
    turn_id = context.turn_id if context else None

    settings = get_settings()
    capture_full = settings.observability_capture_full_payload
    preview_chars = settings.observability_preview_chars

    # Prompt text + hash
    prompt_text = json.dumps(messages, ensure_ascii=False)
    prompt_hash = hashlib.sha256(prompt_text.encode("utf-8")).hexdigest()
    prompt_preview = prompt_text if capture_full else prompt_text[:preview_chars]
    prompt_json = prompt_text if capture_full else None

    # Response text
    response_text = response
    response_preview = response_text[:preview_chars] if response_text and not capture_full else response_text
    response_json = response_text if capture_full else None

    await safe_observability_task(
        insert_llm_call(
            session_id=session_id,
            turn_id=turn_id,
            task=task,
            profile=profile,
            model=model,
            provider=provider,
            status=status,
            duration_ms=duration_ms,
            prompt_hash=prompt_hash,
            prompt_preview=prompt_preview,
            prompt_json=prompt_json,
            response_preview=response_preview,
            response_json=response_json,
            parsed_json=parsed_json,
            validation_error=validation_error,
            token_usage=token_usage,
            fallback_from=fallback_from,
            error_type=error_type,
            error_message=error_message,
            error_raw=error_raw,
        ),
        session_id,
        turn_id,
        f"record_llm_call:{task}",
    )


def schedule_llm_call_recording(
    *,
    task: str,
    profile: str,
    model: str,
    provider: str,
    status: str,
    duration_ms: float,
    messages: list[dict[str, Any]],
    response: str | None,
    parsed_json: dict[str, Any] | None,
    validation_error: str | None,
    token_usage: dict[str, Any] | None,
    fallback_from: str | None,
    error_type: str | None,
    error_message: str | None,
    error_raw: str | None,
) -> None:
    """Fire-and-forget wrapper using asyncio.create_task.

    This is what llm_gateway.py should call. The task is created but
    NOT awaited, so observability writes never block the chat flow.
    """
    _schedule_observability_task(
        record_llm_call(
            task=task,
            profile=profile,
            model=model,
            provider=provider,
            status=status,
            duration_ms=duration_ms,
            messages=messages,
            response=response,
            parsed_json=parsed_json,
            validation_error=validation_error,
            token_usage=token_usage,
            fallback_from=fallback_from,
            error_type=error_type,
            error_message=error_message,
            error_raw=error_raw,
        )
    )


async def record_sse_event(
    *,
    event_type: str,
    seq: int,
    node_id: str | None = None,
    deck_id: str | None = None,
    criteria_id: str | None = None,
    product_ids: list[str] | None = None,
    message_id: str | None = None,
    delta_preview: str | None = None,
    delta_hash: str | None = None,
    finish_reason: str | None = None,
) -> None:
    """Record an SSE event observation."""
    context = get_request_context()
    session_id = context.session_id if context else None
    turn_id = context.turn_id if context else None

    await safe_observability_task(
        insert_sse_event(
            session_id=session_id,
            turn_id=turn_id,
            event_type=event_type,
            seq=seq,
            node_id=node_id,
            deck_id=deck_id,
            criteria_id=criteria_id,
            product_ids=product_ids,
            message_id=message_id,
            delta_preview=delta_preview,
            delta_hash=delta_hash,
            finish_reason=finish_reason,
        ),
        session_id,
        turn_id,
        f"record_sse_event:{event_type}",
    )


def schedule_sse_event_recording(
    *,
    event_type: str,
    seq: int,
    node_id: str | None = None,
    deck_id: str | None = None,
    criteria_id: str | None = None,
    product_ids: list[str] | None = None,
    message_id: str | None = None,
    delta_preview: str | None = None,
    delta_hash: str | None = None,
    finish_reason: str | None = None,
) -> None:
    """Fire-and-forget wrapper for SSE event recording."""
    _schedule_observability_task(
        record_sse_event(
            event_type=event_type,
            seq=seq,
            node_id=node_id,
            deck_id=deck_id,
            criteria_id=criteria_id,
            product_ids=product_ids,
            message_id=message_id,
            delta_preview=delta_preview,
            delta_hash=delta_hash,
            finish_reason=finish_reason,
        )
    )


def _schedule_observability_task(coro: Coroutine[Any, Any, None]) -> None:
    task = asyncio.create_task(coro)
    _OBSERVABILITY_TASKS.add(task)
    task.add_done_callback(_OBSERVABILITY_TASKS.discard)


async def drain_observability_tasks(timeout_seconds: float = 1.0) -> None:
    pending = [task for task in _OBSERVABILITY_TASKS if not task.done()]
    if not pending:
        return

    _, still_pending = await asyncio.wait(pending, timeout=timeout_seconds)
    for task in still_pending:
        task.cancel()
    if still_pending:
        await asyncio.gather(*still_pending, return_exceptions=True)
