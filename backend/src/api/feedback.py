"""Root feedback endpoint matching the backend PRD."""

from __future__ import annotations

from fastapi import APIRouter

from src.services.async_io import run_sync_io
from src.services.audit import record_audit_event
from src.services.feedback import submit_feedback_request
from src.services.request_context import update_request_context
from src.types.schemas import FeedbackRequest, FeedbackResponse

feedback_router = APIRouter(tags=["feedback"])


@feedback_router.post("/feedback")
async def submit_feedback(body: FeedbackRequest) -> FeedbackResponse:
    update_request_context(session_id=body.session_id)
    response = await run_sync_io(submit_feedback_request, body)
    await run_sync_io(
        record_audit_event,
        "feedback.created",
        session_id=body.session_id,
        resource_type="feedback",
        resource_id=body.product_id,
        metadata={
            "feedback_type": body.feedback_type,
            "action": body.action,
            "reason": body.reason,
        },
    )
    return response


@feedback_router.post("/chat/feedback", include_in_schema=False)
async def submit_feedback_legacy(body: FeedbackRequest) -> FeedbackResponse:
    return await submit_feedback(body)
