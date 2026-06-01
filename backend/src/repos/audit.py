"""Persistence for API request logs and business audit events."""

from __future__ import annotations

import logging
from typing import Any

from sqlalchemy.exc import SQLAlchemyError
from sqlmodel import select
from sqlmodel.ext.asyncio.session import AsyncSession

from src.repos.database import create_db_and_tables, get_async_engine
from src.repos.models import ApiRequestLog, AuditEvent

logger = logging.getLogger(__name__)


async def insert_api_request_log(
    *,
    request_id: str,
    method: str,
    path: str,
    status_code: int,
    duration_ms: float,
    trace_id: str | None = None,
    session_id: str | None = None,
    turn_id: str | None = None,
    client_ip: str | None = None,
    user_agent: str | None = None,
    error_code: str | None = None,
    error_type: str | None = None,
) -> str | None:
    await create_db_and_tables()
    row = ApiRequestLog(
        request_id=request_id,
        trace_id=trace_id,
        session_id=session_id,
        turn_id=turn_id,
        method=method,
        path=path,
        status_code=status_code,
        duration_ms=duration_ms,
        client_ip=client_ip,
        user_agent=user_agent,
        error_code=error_code,
        error_type=error_type,
    )
    try:
        async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
            session.add(row)
            await session.commit()
            await session.refresh(row)
            return row.id
    except SQLAlchemyError:
        logger.exception("insert_api_request_log failed")
        raise


async def insert_audit_event(
    *,
    action: str,
    request_id: str | None = None,
    trace_id: str | None = None,
    session_id: str | None = None,
    turn_id: str | None = None,
    actor_type: str = "anonymous",
    resource_type: str | None = None,
    resource_id: str | None = None,
    side_effect: bool = True,
    before_json: dict[str, Any] | None = None,
    after_json: dict[str, Any] | None = None,
    metadata: dict[str, Any] | None = None,
) -> str | None:
    await create_db_and_tables()
    row = AuditEvent(
        request_id=request_id,
        trace_id=trace_id,
        session_id=session_id,
        turn_id=turn_id,
        actor_type=actor_type,
        action=action,
        resource_type=resource_type,
        resource_id=resource_id,
        side_effect=side_effect,
        before_json=before_json,
        after_json=after_json,
        audit_metadata=metadata or {},
    )
    try:
        async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
            session.add(row)
            await session.commit()
            await session.refresh(row)
            return row.id
    except SQLAlchemyError:
        logger.exception("insert_audit_event failed")
        raise


async def list_api_request_logs(
    *,
    trace_id: str | None = None,
    session_id: str | None = None,
    turn_id: str | None = None,
    limit: int = 50,
) -> list[ApiRequestLog]:
    await create_db_and_tables()
    statement = select(ApiRequestLog)
    if trace_id:
        statement = statement.where(ApiRequestLog.trace_id == trace_id)
    if session_id:
        statement = statement.where(ApiRequestLog.session_id == session_id)
    if turn_id:
        statement = statement.where(ApiRequestLog.turn_id == turn_id)
    statement = statement.order_by(ApiRequestLog.created_at.desc()).limit(limit)
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        return list((await session.exec(statement)).all())


async def list_api_request_logs_by_request_ids(request_ids: list[str], limit: int = 50) -> list[ApiRequestLog]:
    await create_db_and_tables()
    unique_request_ids = [request_id for request_id in dict.fromkeys(request_ids) if request_id]
    if not unique_request_ids:
        return []

    statement = (
        select(ApiRequestLog)
        .where(ApiRequestLog.request_id.in_(unique_request_ids))
        .order_by(ApiRequestLog.created_at.desc())
        .limit(limit)
    )
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        return list((await session.exec(statement)).all())


async def map_turn_ids_by_request_ids(request_ids: list[str]) -> dict[str, str]:
    await create_db_and_tables()
    unique_request_ids = [request_id for request_id in dict.fromkeys(request_ids) if request_id]
    if not unique_request_ids:
        return {}

    statement = (
        select(AuditEvent)
        .where(AuditEvent.request_id.in_(unique_request_ids))
        .where(AuditEvent.turn_id.is_not(None))
        .order_by(AuditEvent.created_at.asc())
    )
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        rows = list((await session.exec(statement)).all())

    turn_ids: dict[str, str] = {}
    for row in rows:
        if row.request_id and row.turn_id and row.request_id not in turn_ids:
            turn_ids[row.request_id] = row.turn_id
    return turn_ids


async def list_request_ids_by_turn_id(turn_id: str) -> list[str]:
    await create_db_and_tables()
    if not turn_id:
        return []

    statement = (
        select(AuditEvent)
        .where(AuditEvent.turn_id == turn_id)
        .where(AuditEvent.request_id.is_not(None))
        .order_by(AuditEvent.created_at.desc())
    )
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        rows = list((await session.exec(statement)).all())

    request_ids: list[str] = []
    seen: set[str] = set()
    for row in rows:
        if row.request_id and row.request_id not in seen:
            seen.add(row.request_id)
            request_ids.append(row.request_id)
    return request_ids


async def list_audit_events(
    *,
    trace_id: str | None = None,
    session_id: str | None = None,
    turn_id: str | None = None,
    action: str | None = None,
    limit: int = 50,
) -> list[AuditEvent]:
    await create_db_and_tables()
    statement = select(AuditEvent)
    if trace_id:
        statement = statement.where(AuditEvent.trace_id == trace_id)
    if session_id:
        statement = statement.where(AuditEvent.session_id == session_id)
    if turn_id:
        statement = statement.where(AuditEvent.turn_id == turn_id)
    if action:
        statement = statement.where(AuditEvent.action == action)
    statement = statement.order_by(AuditEvent.created_at.desc()).limit(limit)
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        return list((await session.exec(statement)).all())
