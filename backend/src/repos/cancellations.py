"""Persistence for active chat turns and cross-process cancellation requests."""

from __future__ import annotations

from sqlmodel import Session, select

from src.repos.database import create_db_and_tables, get_engine
from src.repos.models import ActiveChatTurn, ChatTurnCancellation


def register_active_turn(session_id: str, turn_id: str, trace_id: str | None = None) -> None:
    create_db_and_tables()
    with Session(get_engine()) as session:
        _delete_active_turns(session, session_id, turn_id)
        _delete_cancellation_requests(session, session_id, turn_id)
        session.add(ActiveChatTurn(session_id=session_id, turn_id=turn_id, trace_id=trace_id))
        session.commit()


def clear_active_turn(session_id: str, turn_id: str) -> None:
    create_db_and_tables()
    with Session(get_engine()) as session:
        _delete_active_turns(session, session_id, turn_id)
        _delete_cancellation_requests(session, session_id, turn_id)
        session.commit()


def request_turn_cancellation(session_id: str, turn_id: str) -> bool:
    create_db_and_tables()
    with Session(get_engine()) as session:
        active = session.exec(
            select(ActiveChatTurn)
            .where(ActiveChatTurn.session_id == session_id)
            .where(ActiveChatTurn.turn_id == turn_id)
            .limit(1)
        ).first()
        if active is None:
            return False
        existing = session.exec(
            select(ChatTurnCancellation)
            .where(ChatTurnCancellation.session_id == session_id)
            .where(ChatTurnCancellation.turn_id == turn_id)
            .limit(1)
        ).first()
        if existing is None:
            session.add(ChatTurnCancellation(session_id=session_id, turn_id=turn_id))
            session.commit()
    return True


def cancellation_requested(session_id: str, turn_id: str) -> bool:
    create_db_and_tables()
    with Session(get_engine()) as session:
        row = session.exec(
            select(ChatTurnCancellation)
            .where(ChatTurnCancellation.session_id == session_id)
            .where(ChatTurnCancellation.turn_id == turn_id)
            .limit(1)
        ).first()
    return row is not None


def _delete_active_turns(session: Session, session_id: str, turn_id: str) -> None:
    rows = session.exec(
        select(ActiveChatTurn).where(ActiveChatTurn.session_id == session_id).where(ActiveChatTurn.turn_id == turn_id)
    ).all()
    for row in rows:
        session.delete(row)


def _delete_cancellation_requests(session: Session, session_id: str, turn_id: str) -> None:
    rows = session.exec(
        select(ChatTurnCancellation)
        .where(ChatTurnCancellation.session_id == session_id)
        .where(ChatTurnCancellation.turn_id == turn_id)
    ).all()
    for row in rows:
        session.delete(row)
