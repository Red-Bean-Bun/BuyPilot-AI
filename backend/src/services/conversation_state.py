"""Conversation state use cases."""

from __future__ import annotations

import logging

from pydantic import ValidationError
from sqlalchemy.exc import SQLAlchemyError

from src.config.settings import get_settings
from src.repos.conversations import get_last_criteria, get_last_product_ids, list_recent_turns, save_turn
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


def get_conversation_summary(session_id: str, max_turns: int = 2) -> str:
    """Build a compact summary of recent conversation turns for LLM context.

    Returns an empty string when there is no history (first turn), so the
    prompt template variable renders as a no-op for single-turn use.
    """
    try:
        turns = list_recent_turns(session_id, max_turns)
    except SQLAlchemyError:
        logger.exception("list_recent_turns DB read failed")
        record_fallback("conversation_state", "memory_fallback", operation="list_turns")
        return ""
    if not turns:
        return ""
    lines: list[str] = []
    for i, turn in enumerate(turns, 1):
        user = str(turn["user_message"])[:80]
        summary = str(turn.get("summary", ""))
        product_ids = list(turn.get("product_ids", []))  # type: ignore[arg-type]
        ids_str = "、".join(str(pid) for pid in product_ids[:3])
        lines.append(
            f"第{i}轮: 用户'{user}', "
            f"购买标准'{summary}', "
            f"推荐了{len(product_ids)}个商品({ids_str})."
        )
    return "\n".join(lines)


def _cache_turn(session_id: str, criteria: CriteriaPayload | None, product_ids: list[str]) -> None:
    if criteria is not None:
        _LAST_CRITERIA[session_id] = criteria.model_copy(deep=True)
    _LAST_PRODUCT_IDS[session_id] = list(product_ids)
