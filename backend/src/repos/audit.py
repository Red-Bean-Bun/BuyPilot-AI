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
        return None


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
        return None


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
