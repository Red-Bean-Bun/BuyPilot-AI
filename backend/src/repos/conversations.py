"""Conversation state repository."""

from __future__ import annotations

import uuid

from pydantic import ValidationError
from sqlmodel import Session, select

from src.repos.database import create_db_and_tables, get_engine
from src.repos.models import Conversation
from src.types.sse_events import CriteriaPayload


def save_turn(
    session_id: str,
    criteria: CriteriaPayload | None,
    product_ids: list[str],
    message_id: str | None = None,
    user_message: str = "",
    ai_response: str | None = None,
) -> str | None:
    create_db_and_tables()
    row = Conversation(
        session_id=session_id,
        message_id=message_id or f"msg_{uuid.uuid4().hex[:8]}",
        user_message=user_message,
        criteria_json=criteria.model_dump(mode="json") if criteria is not None else None,
        ai_response=ai_response,
        product_ids=list(product_ids),
    )
    with Session(get_engine()) as session:
        session.add(row)
        session.commit()
        session.refresh(row)
        return row.id


def get_last_criteria(session_id: str) -> CriteriaPayload | None:
    create_db_and_tables()
    with Session(get_engine()) as session:
        row = session.exec(
            select(Conversation)
            .where(Conversation.session_id == session_id)
            .where(Conversation.criteria_json.is_not(None))
            .order_by(Conversation.created_at.desc())
            .limit(1)
        ).first()
    if row and row.criteria_json:
        try:
            return CriteriaPayload.model_validate(row.criteria_json)
        except ValidationError:
            return None
    return None


def list_recent_turns(session_id: str, limit: int = 3) -> list[dict[str, object]]:
    """Return recent conversation turns (oldest first) for LLM context injection."""
    create_db_and_tables()
    with Session(get_engine()) as session:
        rows = session.exec(
            select(Conversation)
            .where(Conversation.session_id == session_id)
            .order_by(Conversation.created_at.desc())
            .limit(limit)
        ).all()
    turns: list[dict[str, object]] = []
    for row in reversed(rows):
        summary = ""
        if row.criteria_json:
            summary = row.criteria_json.get("summary", "")
        turns.append(
            {
                "user_message": row.user_message,
                "summary": summary,
                "product_ids": list(row.product_ids) if row.product_ids else [],
            }
        )
    return turns


def get_last_product_ids(session_id: str) -> list[str]:
    create_db_and_tables()
    with Session(get_engine()) as session:
        row = session.exec(
            select(Conversation)
            .where(Conversation.session_id == session_id)
            .order_by(Conversation.created_at.desc())
            .limit(1)
        ).first()
    return list(row.product_ids) if row else []
