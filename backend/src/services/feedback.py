"""Feedback use cases."""

from __future__ import annotations

from src.repos.feedbacks import add_feedback, extract_feedback_from_session
from src.types.schemas import FeedbackRequest, FeedbackResponse


async def submit_feedback_request(body: FeedbackRequest) -> FeedbackResponse:
    action = body.feedback_type or body.action or "feedback"
    await record_feedback(body.session_id, action=action, product_id=body.product_id, reason=body.reason)
    return FeedbackResponse(session_id=body.session_id, feedback_type=body.feedback_type, action=body.action)


async def record_feedback(
    session_id: str,
    action: str,
    product_id: str | None = None,
    reason: str | None = None,
) -> None:
    await add_feedback(session_id, action=action, product_id=product_id, reason=reason)


async def get_feedback_context(session_id: str) -> dict[str, list[str]]:
    return await extract_feedback_from_session(session_id)
