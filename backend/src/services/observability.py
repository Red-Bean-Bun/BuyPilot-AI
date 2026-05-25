"""Read-side observability use cases for admin debugging."""

from __future__ import annotations

from typing import Any

from src.repos.traces import list_recent_retrieval_traces
from src.services.audit import list_audit_event_payloads, list_request_log_payloads


async def list_requests(
    trace_id: str | None = None,
    session_id: str | None = None,
    turn_id: str | None = None,
    limit: int = 50,
) -> list[dict[str, Any]]:
    return await list_request_log_payloads(trace_id=trace_id, session_id=session_id, turn_id=turn_id, limit=limit)


async def list_audit(
    trace_id: str | None = None,
    session_id: str | None = None,
    turn_id: str | None = None,
    action: str | None = None,
    limit: int = 50,
) -> list[dict[str, Any]]:
    return await list_audit_event_payloads(
        trace_id=trace_id,
        session_id=session_id,
        turn_id=turn_id,
        action=action,
        limit=limit,
    )


async def get_turn_debug_bundle(turn_id: str, limit: int = 100) -> dict[str, Any]:
    return {
        "turn_id": turn_id,
        "requests": await list_requests(turn_id=turn_id, limit=limit),
        "audit_events": await list_audit(turn_id=turn_id, limit=limit),
    }


async def get_session_debug_bundle(session_id: str, limit: int = 100) -> dict[str, Any]:
    return {
        "session_id": session_id,
        "requests": await list_requests(session_id=session_id, limit=limit),
        "audit_events": await list_audit(session_id=session_id, limit=limit),
    }


async def list_fallbacks(limit: int = 50) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for trace in await list_recent_retrieval_traces(limit=limit):
        fallbacks = trace.filters_applied.get("_fallbacks", []) if trace.filters_applied else []
        if not isinstance(fallbacks, list):
            continue
        for item in fallbacks:
            if not isinstance(item, dict):
                continue
            rows.append(
                {
                    "retrieval_trace_id": trace.id,
                    "criteria_id": trace.criteria_id,
                    "selected_ids": trace.selected_ids,
                    "created_at": trace.created_at.isoformat(),
                    **item,
                }
            )
    return rows[:limit]
