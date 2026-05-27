"""Feedback use cases."""

from __future__ import annotations

from src.repos.feedbacks import add_feedback, extract_feedback_from_session
from src.types.schemas import FeedbackRequest, FeedbackResponse


async def submit_feedback_request(body: FeedbackRequest) -> FeedbackResponse:
    action = body.feedback_type or body.action or "feedback"
    await record_feedback(
        body.session_id,
        action=action,
        product_id=body.product_id,
        reason=body.reason,
        deck_id=body.deck_id,
    )
    return FeedbackResponse(session_id=body.session_id, feedback_type=body.feedback_type, action=body.action)


async def record_feedback(
    session_id: str,
    action: str,
    product_id: str | None = None,
    reason: str | None = None,
    deck_id: str | None = None,
) -> None:
    await add_feedback(session_id, action=action, product_id=product_id, reason=reason, deck_id=deck_id)


async def get_feedback_context(session_id: str, deck_id: str | None = None) -> dict[str, list[str]]:
    return await extract_feedback_from_session(session_id, deck_id=deck_id)
