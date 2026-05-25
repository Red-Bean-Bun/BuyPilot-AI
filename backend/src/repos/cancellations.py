"""Persistence for active chat turns and cross-process cancellation requests."""

from __future__ import annotations

from sqlmodel import select
from sqlmodel.ext.asyncio.session import AsyncSession

from src.repos.database import create_db_and_tables, get_async_engine
from src.repos.models import ActiveChatTurn, ChatTurnCancellation


async def register_active_turn(session_id: str, turn_id: str, trace_id: str | None = None) -> None:
    await create_db_and_tables()
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        await _delete_active_turns(session, session_id, turn_id)
        await _delete_cancellation_requests(session, session_id, turn_id)
        session.add(ActiveChatTurn(session_id=session_id, turn_id=turn_id, trace_id=trace_id))
        await session.commit()


async def clear_active_turn(session_id: str, turn_id: str) -> None:
    await create_db_and_tables()
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        await _delete_active_turns(session, session_id, turn_id)
        await _delete_cancellation_requests(session, session_id, turn_id)
        await session.commit()


async def request_turn_cancellation(session_id: str, turn_id: str) -> bool:
    await create_db_and_tables()
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        active = (
            await session.exec(
                select(ActiveChatTurn)
                .where(ActiveChatTurn.session_id == session_id)
                .where(ActiveChatTurn.turn_id == turn_id)
                .limit(1)
            )
        ).first()
        if active is None:
            return False
        existing = (
            await session.exec(
                select(ChatTurnCancellation)
                .where(ChatTurnCancellation.session_id == session_id)
                .where(ChatTurnCancellation.turn_id == turn_id)
                .limit(1)
            )
        ).first()
        if existing is None:
            session.add(ChatTurnCancellation(session_id=session_id, turn_id=turn_id))
            await session.commit()
    return True


async def cancellation_requested(session_id: str, turn_id: str) -> bool:
    await create_db_and_tables()
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        row = (
            await session.exec(
                select(ChatTurnCancellation)
                .where(ChatTurnCancellation.session_id == session_id)
                .where(ChatTurnCancellation.turn_id == turn_id)
                .limit(1)
            )
        ).first()
    return row is not None


async def _delete_active_turns(session: AsyncSession, session_id: str, turn_id: str) -> None:
    rows = (
        await session.exec(
            select(ActiveChatTurn)
            .where(ActiveChatTurn.session_id == session_id)
            .where(ActiveChatTurn.turn_id == turn_id)
        )
    ).all()
    for row in rows:
        await session.delete(row)


async def _delete_cancellation_requests(session: AsyncSession, session_id: str, turn_id: str) -> None:
    rows = (
        await session.exec(
            select(ChatTurnCancellation)
            .where(ChatTurnCancellation.session_id == session_id)
            .where(ChatTurnCancellation.turn_id == turn_id)
        )
    ).all()
    for row in rows:
        await session.delete(row)
