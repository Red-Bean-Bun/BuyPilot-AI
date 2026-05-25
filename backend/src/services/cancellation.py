"""Chat turn cancellation use cases."""

from __future__ import annotations

from src.repos.cancellations import (
    cancellation_requested,
    clear_active_turn,
    register_active_turn,
    request_turn_cancellation,
)


async def register_chat_turn(session_id: str, turn_id: str, trace_id: str | None = None) -> None:
    await register_active_turn(session_id, turn_id, trace_id=trace_id)


async def clear_chat_turn(session_id: str, turn_id: str) -> None:
    await clear_active_turn(session_id, turn_id)


async def request_chat_turn_cancellation(session_id: str, turn_id: str) -> bool:
    return await request_turn_cancellation(session_id, turn_id)


async def is_chat_turn_cancellation_requested(session_id: str, turn_id: str) -> bool:
    return await cancellation_requested(session_id, turn_id)
