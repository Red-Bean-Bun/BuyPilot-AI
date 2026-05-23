"""Feedback repository and same-session feedback extraction helpers."""

from __future__ import annotations

from dataclasses import dataclass

from sqlalchemy.exc import SQLAlchemyError
from sqlmodel import Session, select

from src.repos.database import create_db_and_tables, get_engine
from src.repos.models import Feedback

_FEEDBACKS: list["FeedbackRecord"] = []
_AVOID_TERMS = ("酒精", "香精", "小零件", "耐克", "Nike", "日系品牌", "日系", "日本品牌")
_NEGATIVE_ACTIONS = {"not_interested", "dislike", "avoid", "negative"}
_NEGATIVE_MARKERS = ("不要", "不含", "避免", "除了", "不喜欢", "别再", "不要再")


@dataclass(frozen=True)
class FeedbackRecord:
    session_id: str
    action: str
    product_id: str | None = None
    reason: str | None = None


def add_feedback(session_id: str, action: str, product_id: str | None = None, reason: str | None = None) -> None:
    record = FeedbackRecord(session_id=session_id, action=action, product_id=product_id, reason=reason)
    _FEEDBACKS.append(record)
    try:
        create_db_and_tables()
        with Session(get_engine()) as session:
            session.add(
                Feedback(
                    session_id=session_id,
                    action=action,
                    product_id=product_id,
                    reason=reason,
                )
            )
            session.commit()
    except SQLAlchemyError:
        return


def get_session_feedbacks(session_id: str) -> list[FeedbackRecord]:
    try:
        create_db_and_tables()
        with Session(get_engine()) as session:
            rows = session.exec(
                select(Feedback)
                .where(Feedback.session_id == session_id)
                .order_by(Feedback.created_at)
            ).all()
        return [
            FeedbackRecord(
                session_id=row.session_id,
                action=row.action,
                product_id=row.product_id,
                reason=row.reason,
            )
            for row in rows
        ]
    except SQLAlchemyError:
        pass

    return [item for item in _FEEDBACKS if item.session_id == session_id]


def extract_feedback_from_session(session_id: str) -> dict[str, list[str]]:
    avoid_products: list[str] = []
    avoid_traits: list[str] = []
    prefer_traits: list[str] = []
    for item in get_session_feedbacks(session_id):
        if item.action in _NEGATIVE_ACTIONS and item.product_id:
            avoid_products.append(item.product_id)
        if item.reason:
            if item.action in _NEGATIVE_ACTIONS or any(token in item.reason for token in _NEGATIVE_MARKERS):
                avoid_traits.extend(_extract_avoid_terms(item.reason))
            else:
                prefer_traits.append(item.reason)
    return {
        "avoid_products": list(dict.fromkeys(avoid_products)),
        "avoid_traits": list(dict.fromkeys(avoid_traits)),
        "prefer_traits": prefer_traits,
    }


def _extract_avoid_terms(reason: str) -> list[str]:
    terms = [term for term in _AVOID_TERMS if term in reason]
    if terms:
        return terms

    cleaned = reason
    for marker in _NEGATIVE_MARKERS:
        cleaned = cleaned.replace(marker, "")
    for filler in ("的", "这个", "这款", "商品", "产品", "品牌", "牌子", "还有什么", "还有吗"):
        cleaned = cleaned.replace(filler, "")
    cleaned = cleaned.strip(" ，。,.！!？?")
    return [cleaned] if cleaned else [reason]
