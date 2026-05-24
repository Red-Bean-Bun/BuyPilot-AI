"""Chat turn cancellation use cases."""

from __future__ import annotations

from sqlalchemy.exc import SQLAlchemyError

from src.repos.cancellations import (
    cancellation_requested,
    clear_active_turn,
    register_active_turn,
    request_turn_cancellation,
)


def register_chat_turn(session_id: str, turn_id: str, trace_id: str | None = None) -> None:
    register_active_turn(session_id, turn_id, trace_id=trace_id)


def clear_chat_turn(session_id: str, turn_id: str) -> None:
    clear_active_turn(session_id, turn_id)


def request_chat_turn_cancellation(session_id: str, turn_id: str) -> bool:
    return request_turn_cancellation(session_id, turn_id)


def is_chat_turn_cancellation_requested(session_id: str, turn_id: str) -> bool:
    try:
        return cancellation_requested(session_id, turn_id)
    except SQLAlchemyError:
        return False
