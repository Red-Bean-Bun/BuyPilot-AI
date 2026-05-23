"""Conversation state use cases."""

from __future__ import annotations

import logging

from pydantic import ValidationError
from sqlalchemy.exc import SQLAlchemyError

from src.config.settings import get_settings
from src.repos.conversations import get_last_criteria, get_last_product_ids, save_turn
from src.services.fallbacks import record_fallback
from src.types.sse_events import CriteriaPayload

logger = logging.getLogger(__name__)

_LAST_CRITERIA: dict[str, CriteriaPayload] = {}
_LAST_PRODUCT_IDS: dict[str, list[str]] = {}


def save_recommendation_turn(
    session_id: str,
    criteria: CriteriaPayload | None,
    product_ids: list[str],
    message_id: str | None = None,
    user_message: str = "",
    ai_response: str | None = None,
) -> str | None:
    try:
        conversation_id = save_turn(
            session_id,
            criteria,
            product_ids,
            message_id=message_id,
            user_message=user_message,
            ai_response=ai_response,
        )
        _cache_turn(session_id, criteria, product_ids)
        return conversation_id
    except SQLAlchemyError:
        logger.exception("save_turn DB write failed")
        if get_settings().strict_runtime:
            raise
        _cache_turn(session_id, criteria, product_ids)
        record_fallback("conversation_state", "memory_fallback", operation="save")
        return None


def get_previous_criteria(session_id: str) -> CriteriaPayload | None:
    try:
        criteria = get_last_criteria(session_id)
        if criteria is not None:
            _LAST_CRITERIA[session_id] = criteria.model_copy(deep=True)
            return criteria
    except (SQLAlchemyError, ValidationError):
        logger.exception("get_last_criteria DB read failed")
        if get_settings().strict_runtime:
            raise
        record_fallback("conversation_state", "memory_fallback", operation="get_criteria")
    cached = _LAST_CRITERIA.get(session_id)
    return cached.model_copy(deep=True) if cached else None


def get_previous_product_ids(session_id: str) -> list[str]:
    try:
        product_ids = get_last_product_ids(session_id)
        if product_ids:
            _LAST_PRODUCT_IDS[session_id] = list(product_ids)
            return product_ids
    except SQLAlchemyError:
        logger.exception("get_last_product_ids DB read failed")
        if get_settings().strict_runtime:
            raise
        record_fallback("conversation_state", "memory_fallback", operation="get_product_ids")
    return list(_LAST_PRODUCT_IDS.get(session_id, []))


def _cache_turn(session_id: str, criteria: CriteriaPayload | None, product_ids: list[str]) -> None:
    if criteria is not None:
        _LAST_CRITERIA[session_id] = criteria.model_copy(deep=True)
    _LAST_PRODUCT_IDS[session_id] = list(product_ids)
