"""Feedback repository and same-session feedback extraction helpers."""

from __future__ import annotations

from dataclasses import dataclass

from sqlmodel import select
from sqlmodel.ext.asyncio.session import AsyncSession

from src.config.domain_terms import extract_feedback_avoid_terms, is_negative_feedback
from src.repos.database import create_db_and_tables, get_async_engine
from src.repos.models import Feedback


@dataclass(frozen=True)
class FeedbackRecord:
    session_id: str
    action: str
    deck_id: str | None = None
    product_id: str | None = None
    reason: str | None = None


async def add_feedback(
    session_id: str,
    action: str,
    product_id: str | None = None,
    reason: str | None = None,
    deck_id: str | None = None,
) -> None:
    await create_db_and_tables()
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        session.add(
            Feedback(
                session_id=session_id,
                deck_id=deck_id,
                action=action,
                product_id=product_id,
                reason=reason,
            )
        )
        await session.commit()


async def get_session_feedbacks(session_id: str, deck_id: str | None = None) -> list[FeedbackRecord]:
    await create_db_and_tables()
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        stmt = select(Feedback).where(Feedback.session_id == session_id)
        if deck_id is not None:
            stmt = stmt.where(Feedback.deck_id == deck_id)
        rows = (await session.exec(stmt.order_by(Feedback.created_at))).all()
    return [
        FeedbackRecord(
            session_id=row.session_id,
            action=row.action,
            deck_id=row.deck_id,
            product_id=row.product_id,
            reason=row.reason,
        )
        for row in rows
    ]


async def extract_feedback_from_session(session_id: str, deck_id: str | None = None) -> dict[str, list[str]]:
    return extract_feedback_context(await get_session_feedbacks(session_id, deck_id=deck_id))


def extract_feedback_context(records: list[FeedbackRecord]) -> dict[str, list[str]]:
    avoid_products: list[str] = []
    avoid_traits: list[str] = []
    prefer_traits: list[str] = []
    for item in records:
        if is_negative_feedback(item.action) and item.product_id:
            avoid_products.append(item.product_id)
        if item.reason:
            if is_negative_feedback(item.action, item.reason):
                avoid_traits.extend(extract_feedback_avoid_terms(item.reason))
            else:
                prefer_traits.append(item.reason)
    return {
        "avoid_products": list(dict.fromkeys(avoid_products)),
        "avoid_traits": list(dict.fromkeys(avoid_traits)),
        "prefer_traits": prefer_traits,
    }
