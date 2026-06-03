"""Read-side observability use cases for admin debugging."""

from __future__ import annotations

from typing import Any

from src.repos.observability_llm import list_llm_calls_by_turn, list_sse_events_by_turn
from src.repos.traces import list_evidence_links, list_recent_retrieval_traces, list_retrieval_traces
from src.services.audit import get_request_log_payload, list_audit_event_payloads, list_request_log_payloads


async def list_requests(
    request_id: str | None = None,
    trace_id: str | None = None,
    session_id: str | None = None,
    turn_id: str | None = None,
    path: str | None = None,
    method: str | None = None,
    status_code: int | None = None,
    limit: int = 50,
) -> list[dict[str, Any]]:
    return await list_request_log_payloads(
        request_id=request_id,
        trace_id=trace_id,
        session_id=session_id,
        turn_id=turn_id,
        path=path,
        method=method,
        status_code=status_code,
        limit=limit,
    )


async def list_audit(
    request_id: str | None = None,
    trace_id: str | None = None,
    session_id: str | None = None,
    turn_id: str | None = None,
    action: str | None = None,
    limit: int = 50,
) -> list[dict[str, Any]]:
    return await list_audit_event_payloads(
        request_id=request_id,
        trace_id=trace_id,
        session_id=session_id,
        turn_id=turn_id,
        action=action,
        limit=limit,
    )


async def get_turn_debug_bundle(turn_id: str, limit: int = 100) -> dict[str, Any]:
    audit_events = await list_audit(turn_id=turn_id, limit=limit)
    sse_events = await list_sse_events_by_turn(turn_id)
    context_diagnostics = [e for e in audit_events if e.get("action") == "chat.context_diagnostic"]
    criteria_ids = _criteria_ids_from_sse(sse_events)
    retrieval_traces = await list_retrieval_trace_payloads(criteria_ids=criteria_ids, limit=limit)
    return {
        "turn_id": turn_id,
        "requests": await list_requests(turn_id=turn_id, limit=limit),
        "audit_events": audit_events,
        "llm_calls": await list_llm_calls_by_turn(turn_id),
        "sse_events": sse_events,
        "retrieval_traces": retrieval_traces,
        "evidence_links": await list_evidence_link_payloads(
            conversation_ids=_conversation_ids_from_traces(retrieval_traces),
            limit=limit * 2,
        ),
        "context_diagnostics": context_diagnostics,
    }


async def get_session_debug_bundle(session_id: str, limit: int = 100) -> dict[str, Any]:
    requests = await list_requests(session_id=session_id, limit=limit)
    audit_events = await list_audit(session_id=session_id, limit=limit)
    context_diagnostics = [e for e in audit_events if e.get("action") == "chat.context_diagnostic"]

    # Aggregate llm_calls and sse_events across all turns in this session
    all_llm_calls: list[dict[str, Any]] = []
    all_sse_events: list[dict[str, Any]] = []
    seen_turns: set[str] = set()
    for req in requests:
        turn_id = req.get("turn_id")
        if turn_id and turn_id not in seen_turns:
            seen_turns.add(turn_id)
            all_llm_calls.extend(await list_llm_calls_by_turn(turn_id))
            all_sse_events.extend(await list_sse_events_by_turn(turn_id))
    retrieval_traces = await list_retrieval_trace_payloads(session_id=session_id, limit=limit)

    return {
        "session_id": session_id,
        "requests": requests,
        "audit_events": audit_events,
        "llm_calls": all_llm_calls,
        "sse_events": all_sse_events,
        "retrieval_traces": retrieval_traces,
        "evidence_links": await list_evidence_link_payloads(session_id=session_id, limit=limit * 2),
        "context_diagnostics": context_diagnostics,
    }


async def get_request_debug_bundle(request_id: str, limit: int = 100) -> dict[str, Any]:
    request = await get_request_log_payload(request_id)
    if request is None:
        return {"error": "not_found", "request_id": request_id}
    audit_events = await list_audit(request_id=request_id, limit=limit)
    turn_id = request.get("turn_id")
    session_id = request.get("session_id")
    llm_calls = await list_llm_calls_by_turn(turn_id) if turn_id else []
    sse_events = await list_sse_events_by_turn(turn_id) if turn_id else []
    criteria_ids = _criteria_ids_from_sse(sse_events)
    retrieval_traces = await list_retrieval_trace_payloads(
        criteria_ids=criteria_ids or None,
        session_id=session_id if not criteria_ids else None,
        limit=limit,
    )
    return {
        "request_id": request_id,
        "requests": [request],
        "audit_events": audit_events,
        "llm_calls": llm_calls,
        "sse_events": sse_events,
        "retrieval_traces": retrieval_traces,
        "evidence_links": await list_evidence_link_payloads(
            conversation_ids=_conversation_ids_from_traces(retrieval_traces),
            session_id=session_id if not retrieval_traces else None,
            limit=limit * 2,
        ),
        "context_diagnostics": [e for e in audit_events if e.get("action") == "chat.context_diagnostic"],
    }


async def list_retrieval_trace_payloads(
    *,
    criteria_ids: list[str] | None = None,
    conversation_ids: list[str] | None = None,
    session_id: str | None = None,
    limit: int = 50,
) -> list[dict[str, Any]]:
    if criteria_ids == [] and conversation_ids is None and session_id is None:
        return []
    if conversation_ids == [] and criteria_ids is None and session_id is None:
        return []
    rows = await list_retrieval_traces(
        criteria_ids=criteria_ids,
        conversation_ids=conversation_ids,
        session_id=session_id,
        limit=limit,
    )
    return [_retrieval_trace_payload(row) for row in rows]


async def list_evidence_link_payloads(
    *,
    conversation_ids: list[str] | None = None,
    session_id: str | None = None,
    product_id: str | None = None,
    limit: int = 200,
) -> list[dict[str, Any]]:
    if conversation_ids == [] and session_id is None and product_id is None:
        return []
    rows = await list_evidence_links(
        conversation_ids=conversation_ids,
        session_id=session_id,
        product_id=product_id,
        limit=limit,
    )
    return [_evidence_link_payload(row) for row in rows]


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


def _retrieval_trace_payload(row) -> dict[str, Any]:
    return {
        "id": row.id,
        "conversation_id": row.conversation_id,
        "criteria_id": row.criteria_id,
        "filters_applied": row.filters_applied,
        "vector_top_k": row.vector_top_k,
        "rerank_top_n": row.rerank_top_n,
        "selected_ids": row.selected_ids,
        "hit_count": row.hit_count,
        "vector_count": row.vector_count,
        "created_at": row.created_at.isoformat(),
    }


def _evidence_link_payload(row) -> dict[str, Any]:
    return {
        "id": row.id,
        "conversation_id": row.conversation_id,
        "product_id": row.product_id,
        "chunk_id": row.chunk_id,
        "source_id_raw": row.source_id_raw,
        "snippet": row.snippet,
        "evidence_type": row.evidence_type,
        "relevance_score": row.relevance_score,
        "cited_in": row.cited_in,
    }


def _criteria_ids_from_sse(sse_events: list[dict[str, Any]]) -> list[str]:
    ids: list[str] = []
    for event in sse_events:
        criteria_id = event.get("criteria_id")
        if criteria_id and criteria_id not in ids:
            ids.append(criteria_id)
    return ids


def _conversation_ids_from_traces(traces: list[dict[str, Any]]) -> list[str]:
    ids: list[str] = []
    for trace in traces:
        conversation_id = trace.get("conversation_id")
        if conversation_id and conversation_id not in ids:
            ids.append(conversation_id)
    return ids
