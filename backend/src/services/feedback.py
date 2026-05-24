"""Feedback use cases."""

from __future__ import annotations

import logging

from sqlalchemy.exc import SQLAlchemyError

from src.config.settings import get_settings
from src.repos.feedbacks import FeedbackRecord, add_feedback, extract_feedback_context, extract_feedback_from_session
from src.services.fallbacks import record_fallback
from src.types.schemas import FeedbackRequest, FeedbackResponse

logger = logging.getLogger(__name__)

_FEEDBACKS: list[FeedbackRecord] = []


def submit_feedback_request(body: FeedbackRequest) -> FeedbackResponse:
    action = body.feedback_type or body.action or "feedback"
    record_feedback(body.session_id, action=action, product_id=body.product_id, reason=body.reason)
    return FeedbackResponse(session_id=body.session_id, feedback_type=body.feedback_type, action=body.action)


def record_feedback(
    session_id: str,
    action: str,
    product_id: str | None = None,
    reason: str | None = None,
) -> None:
    record = FeedbackRecord(session_id=session_id, action=action, product_id=product_id, reason=reason)
    try:
        add_feedback(session_id, action=action, product_id=product_id, reason=reason)
        if _memory_state_fallback_enabled():
            _FEEDBACKS.append(record)
    except SQLAlchemyError:
        logger.exception("add_feedback DB write failed")
        if not _memory_state_fallback_enabled():
            raise
        _FEEDBACKS.append(record)
        record_fallback("feedback", "explicit_dev_memory_fallback", operation="add")


def get_feedback_context(session_id: str) -> dict[str, list[str]]:
    try:
        return extract_feedback_from_session(session_id)
    except SQLAlchemyError:
        logger.exception("get_session_feedbacks DB read failed")
        if not _memory_state_fallback_enabled():
            raise
        record_fallback("feedback", "explicit_dev_memory_fallback", operation="get")
        return extract_feedback_context([item for item in _FEEDBACKS if item.session_id == session_id])


def _memory_state_fallback_enabled() -> bool:
    settings = get_settings()
    return settings.allow_memory_state_fallback and not settings.strict_runtime
