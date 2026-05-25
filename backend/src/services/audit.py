"""Service layer for request logging and business audit events."""

from __future__ import annotations

from typing import Any

from src.repos.audit import (
    insert_api_request_log,
    insert_audit_event,
    list_api_request_logs,
    list_audit_events,
)
from src.services.request_context import get_request_context


async def record_api_request(
    *,
    method: str,
    path: str,
    status_code: int,
    duration_ms: float,
    client_ip: str | None = None,
    user_agent: str | None = None,
    error_code: str | None = None,
    error_type: str | None = None,
) -> str | None:
    context = get_request_context()
    request_id = context.request_id if context else ""
    return await insert_api_request_log(
        request_id=request_id,
        trace_id=context.trace_id if context else None,
        session_id=context.session_id if context else None,
        turn_id=context.turn_id if context else None,
        method=method,
        path=path,
        status_code=status_code,
        duration_ms=duration_ms,
        client_ip=client_ip,
        user_agent=user_agent,
        error_code=error_code,
        error_type=error_type,
    )


async def record_audit_event(
    action: str,
    *,
    session_id: str | None = None,
    turn_id: str | None = None,
    trace_id: str | None = None,
    actor_type: str = "anonymous",
    resource_type: str | None = None,
    resource_id: str | None = None,
    side_effect: bool = True,
    before_json: dict[str, Any] | None = None,
    after_json: dict[str, Any] | None = None,
    metadata: dict[str, Any] | None = None,
) -> str | None:
    context = get_request_context()
    return await insert_audit_event(
        action=action,
        request_id=context.request_id if context else None,
        trace_id=trace_id or (context.trace_id if context else None),
        session_id=session_id or (context.session_id if context else None),
        turn_id=turn_id or (context.turn_id if context else None),
        actor_type=actor_type,
        resource_type=resource_type,
        resource_id=resource_id,
        side_effect=side_effect,
        before_json=before_json,
        after_json=after_json,
        metadata=metadata,
    )


def api_request_log_payload(row) -> dict[str, Any]:
    return {
        "id": row.id,
        "request_id": row.request_id,
        "trace_id": row.trace_id,
        "session_id": row.session_id,
        "turn_id": row.turn_id,
        "method": row.method,
        "path": row.path,
        "status_code": row.status_code,
        "duration_ms": row.duration_ms,
        "client_ip": row.client_ip,
        "user_agent": row.user_agent,
        "error_code": row.error_code,
        "error_type": row.error_type,
        "created_at": row.created_at.isoformat(),
    }


def audit_event_payload(row) -> dict[str, Any]:
    return {
        "id": row.id,
        "request_id": row.request_id,
        "trace_id": row.trace_id,
        "session_id": row.session_id,
        "turn_id": row.turn_id,
        "actor_type": row.actor_type,
        "action": row.action,
        "resource_type": row.resource_type,
        "resource_id": row.resource_id,
        "side_effect": row.side_effect,
        "before_json": row.before_json,
        "after_json": row.after_json,
        "metadata": row.audit_metadata,
        "created_at": row.created_at.isoformat(),
    }


async def list_request_log_payloads(**filters: Any) -> list[dict[str, Any]]:
    return [api_request_log_payload(row) for row in await list_api_request_logs(**filters)]


async def list_audit_event_payloads(**filters: Any) -> list[dict[str, Any]]:
    return [audit_event_payload(row) for row in await list_audit_events(**filters)]
