"""Feedback repository and same-session feedback extraction helpers."""

from __future__ import annotations

from dataclasses import dataclass

_FEEDBACKS: list["FeedbackRecord"] = []


@dataclass(frozen=True)
class FeedbackRecord:
    session_id: str
    action: str
    product_id: str | None = None
    reason: str | None = None


def add_feedback(session_id: str, action: str, product_id: str | None = None, reason: str | None = None) -> None:
    _FEEDBACKS.append(FeedbackRecord(session_id=session_id, action=action, product_id=product_id, reason=reason))


def get_session_feedbacks(session_id: str) -> list[FeedbackRecord]:
    return [item for item in _FEEDBACKS if item.session_id == session_id]


def extract_feedback_from_session(session_id: str) -> dict[str, list[str]]:
    avoid_products: list[str] = []
    avoid_traits: list[str] = []
    prefer_traits: list[str] = []
    for item in get_session_feedbacks(session_id):
        if item.action in {"not_interested", "dislike"} and item.product_id:
            avoid_products.append(item.product_id)
        if item.reason:
            if any(token in item.reason for token in ("不要", "不含", "避免")):
                avoid_traits.append(item.reason)
            else:
                prefer_traits.append(item.reason)
    return {
        "avoid_products": avoid_products,
        "avoid_traits": avoid_traits,
        "prefer_traits": prefer_traits,
    }

