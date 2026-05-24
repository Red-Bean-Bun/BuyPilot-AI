"""Admin observability API for request logs and audit trails."""

from __future__ import annotations

from typing import Any

from fastapi import APIRouter

from src.services.async_io import run_sync_io
from src.services.observability import (
    get_session_debug_bundle,
    get_turn_debug_bundle,
    list_audit,
    list_fallbacks,
    list_requests,
)

observability_router = APIRouter(tags=["observability"], prefix="/admin/observability")


@observability_router.get("/requests")
async def read_requests(
    trace_id: str | None = None,
    session_id: str | None = None,
    turn_id: str | None = None,
    limit: int = 50,
) -> list[dict[str, Any]]:
    return await run_sync_io(list_requests, trace_id=trace_id, session_id=session_id, turn_id=turn_id, limit=limit)


@observability_router.get("/audit")
async def read_audit(
    trace_id: str | None = None,
    session_id: str | None = None,
    turn_id: str | None = None,
    action: str | None = None,
    limit: int = 50,
) -> list[dict[str, Any]]:
    return await run_sync_io(
        list_audit,
        trace_id=trace_id,
        session_id=session_id,
        turn_id=turn_id,
        action=action,
        limit=limit,
    )


@observability_router.get("/turns/{turn_id}")
async def read_turn(turn_id: str, limit: int = 100) -> dict[str, Any]:
    return await run_sync_io(get_turn_debug_bundle, turn_id, limit=limit)


@observability_router.get("/sessions/{session_id}")
async def read_session(session_id: str, limit: int = 100) -> dict[str, Any]:
    return await run_sync_io(get_session_debug_bundle, session_id, limit=limit)


@observability_router.get("/fallbacks")
async def read_fallbacks(limit: int = 50) -> list[dict[str, Any]]:
    return await run_sync_io(list_fallbacks, limit=limit)
