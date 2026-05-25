"""Per-turn telemetry for allowed LLM provider fallback paths."""

from __future__ import annotations

from contextvars import ContextVar
from typing import Any

from src.config.tuning import FALLBACK_EVENT_LIMIT


_FALLBACK_EVENTS: ContextVar[list[dict[str, Any]] | None] = ContextVar("fallback_events", default=None)


def reset_fallback_events() -> None:
    _FALLBACK_EVENTS.set([])


def record_fallback(component: str, reason: str, **metadata: Any) -> None:
    current = _FALLBACK_EVENTS.get()
    if current is None:
        current = []
        _FALLBACK_EVENTS.set(current)
    if len(current) >= FALLBACK_EVENT_LIMIT:
        return
    event = {"component": component, "reason": reason}
    event.update({key: value for key, value in metadata.items() if value is not None})
    current.append(event)


def get_fallback_events() -> list[dict[str, Any]]:
    current = _FALLBACK_EVENTS.get()
    return [dict(item) for item in current] if current else []
