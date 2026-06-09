"""Session history endpoint for restoring past conversations."""

from __future__ import annotations

import logging

from fastapi import APIRouter
from sqlmodel import select
from sqlmodel.ext.asyncio.session import AsyncSession

from src.repos.conversations import list_all_turns
from src.repos.database import get_async_engine
from src.repos.models import Conversation, Product
from src.services.cart import get_session_cart

logger = logging.getLogger(__name__)

history_router = APIRouter(tags=["history"])


def _row_to_turn_dict(row: Conversation) -> dict:
    """Convert a Conversation row to a serializable turn dict."""
    turn = {
        "user_message": row.user_message,
        "criteria": row.criteria_json,
        "products": row.product_cards_json,
        "turn_text": row.turn_text,
        "decision": row.decision_json,
        "deck_id": row.deck_id,
        "created_at": row.created_at.isoformat() if row.created_at else None,
    }
    return turn


async def _build_legacy_cards(product_ids: list[str]) -> list[dict]:
    """Build simplified product cards for old turns without stored card data."""
    if not product_ids:
        return []
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        rows = (await session.exec(select(Product).where(Product.id.in_(product_ids)))).all()
    product_by_id = {p.id: p for p in rows}
    cards: list[dict] = []
    for pid in product_ids:
        p = product_by_id.get(pid)
        if p is None:
            continue
        cards.append(
            {
                "rank": len(cards) + 1,
                "product": {
                    "product_id": p.id,
                    "name": p.name,
                    "price": p.price,
                    "currency": "CNY",
                    "image_url": f"/assets/products/{p.image_urls[0]}" if p.image_urls else f"/assets/products/{p.category}/{p.id}.jpg",
                    "category": p.category,
                    "sub_category": p.sub_category,
                    "brand": p.brand,
                },
                "reason": "该商品匹配您的筛选条件",
                "risk_notes": [],
                "evidence": [],
            }
        )
    return cards


@history_router.get("/chat/history/{session_id}")
async def get_session_history(session_id: str) -> dict:
    """Return full conversation history for a session."""
    try:
        turns_rows = await list_all_turns(session_id)
    except Exception:
        logger.exception("Failed to load turns for session %s", session_id)
        turns_rows = []
    try:
        cart = await get_session_cart(session_id)
    except Exception:
        logger.exception("Failed to load cart for session %s", session_id)
        cart = None
    enriched_turns: list[dict] = []
    for row in turns_rows:
        turn = _row_to_turn_dict(row)
        if turn["products"] is None and row.product_ids:
            turn["products"] = await _build_legacy_cards(row.product_ids)
        enriched_turns.append(turn)
    return {
        "session_id": session_id,
        "turns": enriched_turns,
        "cart": (
            {"items": cart.items, "total_items": cart.total_items, "total_price": cart.total_price}
            if cart
            else {"items": [], "total_items": 0, "total_price": 0.0}
        ),
    }
