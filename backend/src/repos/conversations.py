"""Conversation state repository.

The runtime only needs the latest criteria and product ids for a session.
Those are persisted so multi-turn demos survive process restarts.
"""

from __future__ import annotations

import uuid

from pydantic import ValidationError
from sqlalchemy.exc import SQLAlchemyError
from sqlmodel import Session, select

from src.repos.database import create_db_and_tables, get_engine
from src.repos.models import Conversation
from src.types.sse_events import CriteriaPayload

_LAST_CRITERIA: dict[str, CriteriaPayload] = {}
_LAST_PRODUCT_IDS: dict[str, list[str]] = {}


def save_turn(
    session_id: str,
    criteria: CriteriaPayload | None,
    product_ids: list[str],
    message_id: str | None = None,
    user_message: str = "",
    ai_response: str | None = None,
) -> str | None:
    if criteria is not None:
        _LAST_CRITERIA[session_id] = criteria.model_copy(deep=True)
    _LAST_PRODUCT_IDS[session_id] = list(product_ids)

    try:
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
    except SQLAlchemyError:
        return None


def get_last_criteria(session_id: str) -> CriteriaPayload | None:
    try:
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
            return CriteriaPayload.model_validate(row.criteria_json)
    except (SQLAlchemyError, ValidationError):
        pass

    criteria = _LAST_CRITERIA.get(session_id)
    return criteria.model_copy(deep=True) if criteria else None


def get_last_product_ids(session_id: str) -> list[str]:
    try:
        create_db_and_tables()
        with Session(get_engine()) as session:
            row = session.exec(
                select(Conversation)
                .where(Conversation.session_id == session_id)
                .order_by(Conversation.created_at.desc())
                .limit(1)
            ).first()
        if row:
            return list(row.product_ids)
    except SQLAlchemyError:
        pass

    return list(_LAST_PRODUCT_IDS.get(session_id, []))
