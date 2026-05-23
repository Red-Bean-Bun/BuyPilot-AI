"""Root feedback endpoint matching the backend PRD."""

from __future__ import annotations

from fastapi import APIRouter

from src.services.async_io import run_sync_io
from src.services.feedback import submit_feedback_request
from src.types.schemas import FeedbackRequest, FeedbackResponse

feedback_router = APIRouter(tags=["feedback"])


@feedback_router.post("/feedback")
async def submit_feedback(body: FeedbackRequest) -> FeedbackResponse:
    return await run_sync_io(submit_feedback_request, body)


@feedback_router.post("/chat/feedback", include_in_schema=False)
async def submit_feedback_legacy(body: FeedbackRequest) -> FeedbackResponse:
    return await submit_feedback(body)
