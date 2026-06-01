from __future__ import annotations

import logging
from typing import Any

from sqlalchemy.exc import SQLAlchemyError
from sqlalchemy.ext.asyncio import AsyncSession
from sqlmodel import col, select

from src.repos.database import create_db_and_tables, get_async_engine
from src.repos.models import ObservabilityLLMCall, ObservabilitySSEEvent

logger = logging.getLogger(__name__)


async def insert_llm_call(
    *,
    turn_id: str | None,
    session_id: str | None,
    task: str,
    profile: str,
    model: str,
    provider: str,
    status: str,
    duration_ms: float,
    prompt_hash: str,
    prompt_preview: str | None = None,
    prompt_json: str | None = None,
    response_preview: str | None = None,
    response_json: str | None = None,
    parsed_json: dict[str, Any] | None = None,
    validation_error: str | None = None,
    token_usage: dict[str, Any] | None = None,
    fallback_from: str | None = None,
    error_type: str | None = None,
    error_message: str | None = None,
    error_raw: str | None = None,
) -> str | None:
    await create_db_and_tables()
    row = ObservabilityLLMCall(
        turn_id=turn_id,
        session_id=session_id,
        task=task,
        profile=profile,
        model=model,
        provider=provider,
        status=status,
        duration_ms=duration_ms,
        prompt_hash=prompt_hash,
        prompt_preview=prompt_preview,
        prompt_json=prompt_json,
        response_preview=response_preview,
        response_json=response_json,
        parsed_json=parsed_json,
        validation_error=validation_error,
        token_usage=token_usage,
        fallback_from=fallback_from,
        error_type=error_type,
        error_message=error_message,
        error_raw=error_raw,
    )
    try:
        async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
            session.add(row)
            await session.commit()
            await session.refresh(row)
            return row.id
    except SQLAlchemyError:
        logger.exception("insert_llm_call failed")
        return None


async def list_llm_calls_by_turn(turn_id: str) -> list[dict[str, Any]]:
    await create_db_and_tables()
    statement = (
        select(ObservabilityLLMCall)
        .where(col(ObservabilityLLMCall.turn_id) == turn_id)
        .order_by(col(ObservabilityLLMCall.created_at).asc())
    )
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        rows = (await session.execute(statement)).scalars().all()
        return [row.model_dump() for row in rows]


async def update_llm_call_parsed_json(
    turn_id: str,
    task: str,
    parsed_json: dict[str, Any] | None,
    validation_error: str | None,
) -> bool:
    await create_db_and_tables()
    try:
        async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
            statement = (
                select(ObservabilityLLMCall)
                .where(col(ObservabilityLLMCall.turn_id) == turn_id)
                .where(col(ObservabilityLLMCall.task) == task)
                .order_by(col(ObservabilityLLMCall.created_at).desc())
                .limit(1)
            )
            row = (await session.execute(statement)).scalars().first()
            if row is None:
                return False
            row.parsed_json = parsed_json
            row.validation_error = validation_error
            session.add(row)
            await session.commit()
            return True
    except SQLAlchemyError:
        logger.warning("update_llm_call_parsed_json failed", exc_info=True)
        return False


async def insert_sse_event(
    *,
    turn_id: str | None,
    session_id: str | None,
    event_type: str,
    seq: int,
    node_id: str | None = None,
    deck_id: str | None = None,
    criteria_id: str | None = None,
    product_ids: list[str] | None = None,
    message_id: str | None = None,
    delta_preview: str | None = None,
    delta_hash: str | None = None,
    finish_reason: str | None = None,
) -> str | None:
    await create_db_and_tables()
    row = ObservabilitySSEEvent(
        turn_id=turn_id,
        session_id=session_id,
        event_type=event_type,
        seq=seq,
        node_id=node_id,
        deck_id=deck_id,
        criteria_id=criteria_id,
        product_ids=product_ids,
        message_id=message_id,
        delta_preview=delta_preview,
        delta_hash=delta_hash,
        finish_reason=finish_reason,
    )
    try:
        async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
            session.add(row)
            await session.commit()
            await session.refresh(row)
            return row.id
    except SQLAlchemyError:
        logger.exception("insert_sse_event failed")
        return None


async def list_sse_events_by_turn(turn_id: str) -> list[dict[str, Any]]:
    await create_db_and_tables()
    statement = (
        select(ObservabilitySSEEvent)
        .where(col(ObservabilitySSEEvent.turn_id) == turn_id)
        .order_by(col(ObservabilitySSEEvent.seq).asc())
    )
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        rows = (await session.execute(statement)).scalars().all()
        return [row.model_dump() for row in rows]
