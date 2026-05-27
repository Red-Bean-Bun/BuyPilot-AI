"""Conversation state use cases."""

from __future__ import annotations

from src.repos.conversations import get_last_criteria, get_last_deck_id, get_last_product_ids, list_recent_turns, save_turn
from src.types.sse_events import CriteriaPayload


async def save_recommendation_turn(
    session_id: str,
    criteria: CriteriaPayload | None,
    product_ids: list[str],
    message_id: str | None = None,
    deck_id: str | None = None,
    user_message: str = "",
    ai_response: str | None = None,
) -> str | None:
    return await save_turn(
        session_id,
        criteria,
        product_ids,
        message_id=message_id,
        deck_id=deck_id,
        user_message=user_message,
        ai_response=ai_response,
    )


async def get_previous_criteria(session_id: str) -> CriteriaPayload | None:
    return await get_last_criteria(session_id)


async def get_previous_product_ids(session_id: str) -> list[str]:
    return await get_last_product_ids(session_id)


async def get_previous_deck_id(session_id: str) -> str | None:
    return await get_last_deck_id(session_id)


async def get_conversation_summary(session_id: str, max_turns: int = 2) -> str:
    """Build a compact summary of recent conversation turns for LLM context.

    Returns an empty string when there is no history (first turn), so the
    prompt template variable renders as a no-op for single-turn use.
    """
    turns = await list_recent_turns(session_id, max_turns)
    if not turns:
        return ""
    lines: list[str] = []
    for i, turn in enumerate(turns, 1):
        user = str(turn["user_message"])[:80]
        summary = str(turn.get("summary", ""))
        product_ids = list(turn.get("product_ids", []))  # type: ignore[arg-type]
        ids_str = "、".join(str(pid) for pid in product_ids[:3])
        lines.append(f"第{i}轮: 用户'{user}', 购买标准'{summary}', 推荐了{len(product_ids)}个商品({ids_str}).")
    return "\n".join(lines)
