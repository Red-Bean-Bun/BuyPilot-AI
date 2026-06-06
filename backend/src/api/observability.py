"""Admin observability API for request logs and audit trails."""

from __future__ import annotations

from pathlib import Path
from typing import Any

from fastapi import APIRouter, Depends
from fastapi.responses import HTMLResponse

from src.api.admin_auth import require_admin_key
from src.services.observability import (
    get_request_debug_bundle,
    get_session_debug_bundle,
    get_turn_debug_bundle,
    list_audit,
    list_evidence_link_payloads,
    list_fallbacks,
    list_requests,
    list_retrieval_trace_payloads,
)
from src.services.retrieval_cache import get_retrieval_cache

observability_router = APIRouter(
    tags=["observability"],
    prefix="/admin/observability",
    dependencies=[Depends(require_admin_key)],
)

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
    request_id: str | None = None,
    trace_id: str | None = None,
    session_id: str | None = None,
    turn_id: str | None = None,
    path: str | None = None,
    method: str | None = None,
    status_code: int | None = None,
    limit: int = 50,
) -> list[dict[str, Any]]:
    return await list_requests(
        request_id=request_id,
        trace_id=trace_id,
        session_id=session_id,
        turn_id=turn_id,
        path=path,
        method=method,
        status_code=status_code,
        limit=limit,
    )


@observability_router.get("/audit")
async def read_audit(
    request_id: str | None = None,
    trace_id: str | None = None,
    session_id: str | None = None,
    turn_id: str | None = None,
    action: str | None = None,
    limit: int = 50,
) -> list[dict[str, Any]]:
    return await list_audit(
        request_id=request_id,
        trace_id=trace_id,
        session_id=session_id,
        turn_id=turn_id,
        action=action,
        limit=limit,
    )


@observability_router.get("/requests/{request_id}")
async def read_request(request_id: str, limit: int = 100) -> dict[str, Any]:
    return await get_request_debug_bundle(request_id, limit=limit)


@observability_router.get("/turns/{turn_id}")
async def read_turn(turn_id: str, limit: int = 100) -> dict[str, Any]:
    return await get_turn_debug_bundle(turn_id, limit=limit)


@observability_router.get("/sessions/{session_id}")
async def read_session(session_id: str, limit: int = 100) -> dict[str, Any]:
    return await get_session_debug_bundle(session_id, limit=limit)


@observability_router.get("/fallbacks")
async def read_fallbacks(limit: int = 50) -> list[dict[str, Any]]:
    return await list_fallbacks(limit=limit)


@observability_router.get("/retrieval-traces")
async def read_retrieval_traces(
    session_id: str | None = None,
    criteria_id: str | None = None,
    conversation_id: str | None = None,
    limit: int = 50,
) -> list[dict[str, Any]]:
    return await list_retrieval_trace_payloads(
        session_id=session_id,
        criteria_ids=[criteria_id] if criteria_id else None,
        conversation_ids=[conversation_id] if conversation_id else None,
        limit=limit,
    )


@observability_router.get("/evidence-links")
async def read_evidence_links(
    session_id: str | None = None,
    conversation_id: str | None = None,
    product_id: str | None = None,
    limit: int = 200,
) -> list[dict[str, Any]]:
    return await list_evidence_link_payloads(
        session_id=session_id,
        conversation_ids=[conversation_id] if conversation_id else None,
        product_id=product_id,
        limit=limit,
    )


@observability_router.get("/cache")
async def read_cache_stats() -> dict[str, Any]:
    """Return retrieval cache statistics including hot keys."""
    return get_retrieval_cache().stats()
