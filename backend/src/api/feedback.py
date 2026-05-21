"""Root feedback endpoint matching the backend PRD."""

from __future__ import annotations

from fastapi import APIRouter

from src.repos.feedbacks import add_feedback
from src.types.schemas import FeedbackRequest, FeedbackResponse

feedback_router = APIRouter(tags=["feedback"])


@feedback_router.post("/feedback")
async def submit_feedback(body: FeedbackRequest) -> FeedbackResponse:
    action = body.feedback_type or body.action or "feedback"
    add_feedback(body.session_id, action=action, product_id=body.product_id, reason=body.reason)
    return FeedbackResponse(session_id=body.session_id, feedback_type=body.feedback_type, action=body.action)

