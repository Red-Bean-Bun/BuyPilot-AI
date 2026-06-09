"""Conversation state use cases."""

from __future__ import annotations

from typing import Any

from src.repos.conversations import (
    get_last_criteria,
    get_last_deck_id,
    get_last_product_ids,
    list_recent_turns,
    save_turn,
)
from src.services.feedback import get_feedback_context
from src.types.sse_events import CriteriaPayload


async def save_recommendation_turn(
    session_id: str,
    criteria: CriteriaPayload | None,
    product_ids: list[str],
    message_id: str | None = None,
    deck_id: str | None = None,
    user_message: str = "",
    ai_response: str | None = None,
    product_cards: list[dict[str, Any]] | None = None,
    decision: dict[str, Any] | None = None,
    turn_text: str | None = None,
) -> str | None:
    return await save_turn(
        session_id,
        criteria,
        product_ids,
        message_id=message_id,
        deck_id=deck_id,
        user_message=user_message,
        ai_response=ai_response,
        product_cards=product_cards,
        decision=decision,
        turn_text=turn_text,
    )


async def get_previous_criteria(session_id: str) -> CriteriaPayload | None:
    return await get_last_criteria(session_id)


async def get_previous_product_ids(session_id: str) -> list[str]:
    return await get_last_product_ids(session_id)


async def get_previous_deck_id(session_id: str) -> str | None:
    return await get_last_deck_id(session_id)


async def get_conversation_summary(session_id: str, max_turns: int = 4) -> str:
    """Build a compact summary of recent conversation turns for LLM context.

    Returns an empty string when there is no history (first turn), so the
    prompt template variable renders as a no-op for single-turn use.

    Enhanced format includes:
    - User message (truncated to 100 chars)
    - Criteria summary (purchase standards)
    - Recommended product count and IDs
    - Feedback context (avoided/liked products, trait preferences)
    """
    turns = await list_recent_turns(session_id, max_turns)
    if not turns:
        return ""

    # Fetch feedback context for the session to enrich summary
    feedback = await get_feedback_context(session_id)

    lines: list[str] = []
    for i, turn in enumerate(turns, 1):
        user = str(turn["user_message"])[:100]
        summary = str(turn.get("summary", ""))
        product_ids = list(turn.get("product_ids", []))  # type: ignore[arg-type]
        ids_str = "、".join(str(pid) for pid in product_ids[:5])

        line = f"第{i}轮: 用户'{user}'"
        if summary:
            line += f"\n  → 标准: {summary}"
        if product_ids:
            line += f"\n  → 推荐: {len(product_ids)}个商品 ({ids_str})"
        lines.append(line)

    # Append feedback summary if any feedback exists
    feedback_lines: list[str] = []
    if feedback.get("avoid_products"):
        feedback_lines.append(f"不喜欢的商品: {', '.join(feedback['avoid_products'][:3])}")
    if feedback.get("liked_products"):
        feedback_lines.append(f"喜欢的商品: {', '.join(feedback['liked_products'][:3])}")
    if feedback.get("avoid_traits"):
        feedback_lines.append(f"避免的特征: {', '.join(feedback['avoid_traits'][:3])}")
    if feedback.get("prefer_traits"):
        feedback_lines.append(f"偏好的特征: {', '.join(feedback['prefer_traits'][:3])}")

    if feedback_lines:
        lines.append("用户反馈:")
        lines.extend(f"  • {fl}" for fl in feedback_lines)

    return "\n".join(lines)
