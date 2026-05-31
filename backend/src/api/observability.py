"""Admin observability API for request logs and audit trails."""

from __future__ import annotations

from pathlib import Path
from typing import Any

from fastapi import APIRouter
from fastapi.responses import HTMLResponse

from src.services.observability import (
    get_session_debug_bundle,
    get_turn_debug_bundle,
    list_audit,
    list_fallbacks,
    list_requests,
)

observability_router = APIRouter(tags=["observability"], prefix="/admin/observability")

_DASHBOARD_HTML_PATH = Path(__file__).resolve().parents[2] / "static" / "observability_dashboard.html"


@observability_router.get("/dashboard", response_class=HTMLResponse)
async def dashboard() -> HTMLResponse:
    """Serve the observability web dashboard."""
    if not _DASHBOARD_HTML_PATH.exists():
        return HTMLResponse(
            f"<h1>Dashboard not found</h1><p>Expected at: {_DASHBOARD_HTML_PATH}</p>",
            status_code=404,
        )
    return HTMLResponse(_DASHBOARD_HTML_PATH.read_text(encoding="utf-8"))


@observability_router.get("/requests")
async def read_requests(
    trace_id: str | None = None,
    session_id: str | None = None,
    turn_id: str | None = None,
    limit: int = 50,
) -> list[dict[str, Any]]:
    return await list_requests(trace_id=trace_id, session_id=session_id, turn_id=turn_id, limit=limit)


@observability_router.get("/audit")
async def read_audit(
    trace_id: str | None = None,
    session_id: str | None = None,
    turn_id: str | None = None,
    action: str | None = None,
    limit: int = 50,
) -> list[dict[str, Any]]:
    return await list_audit(
        trace_id=trace_id,
        session_id=session_id,
        turn_id=turn_id,
        action=action,
        limit=limit,
    )


@observability_router.get("/turns/{turn_id}")
async def read_turn(turn_id: str, limit: int = 100) -> dict[str, Any]:
    return await get_turn_debug_bundle(turn_id, limit=limit)


@observability_router.get("/sessions/{session_id}")
async def read_session(session_id: str, limit: int = 100) -> dict[str, Any]:
    return await get_session_debug_bundle(session_id, limit=limit)


@observability_router.get("/fallbacks")
async def read_fallbacks(limit: int = 50) -> list[dict[str, Any]]:
    return await list_fallbacks(limit=limit)
