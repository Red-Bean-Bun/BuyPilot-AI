"""Per-request correlation context for logs and audit records."""

from __future__ import annotations

from contextvars import ContextVar
from dataclasses import dataclass


@dataclass(frozen=True)
class RequestContext:
    request_id: str
    trace_id: str | None = None
    session_id: str | None = None
    turn_id: str | None = None


_REQUEST_CONTEXT: ContextVar[RequestContext | None] = ContextVar("request_context", default=None)


def set_request_context(context: RequestContext) -> None:
    _REQUEST_CONTEXT.set(context)


def get_request_context() -> RequestContext | None:
    return _REQUEST_CONTEXT.get()


def update_request_context(
    *,
    request_id: str | None = None,
    trace_id: str | None = None,
    session_id: str | None = None,
    turn_id: str | None = None,
) -> RequestContext:
    current = get_request_context()
    context = RequestContext(
        request_id=request_id or (current.request_id if current else ""),
        trace_id=trace_id if trace_id is not None else (current.trace_id if current else None),
        session_id=session_id if session_id is not None else (current.session_id if current else None),
        turn_id=turn_id if turn_id is not None else (current.turn_id if current else None),
    )
    set_request_context(context)
    return context


def clear_request_context() -> None:
    _REQUEST_CONTEXT.set(None)
