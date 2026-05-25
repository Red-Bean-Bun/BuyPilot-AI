"""Cart query endpoint matching the backend PRD."""

from __future__ import annotations

from fastapi import APIRouter

from src.services.cart import get_session_cart
from src.types.schemas import CartResponse

cart_router = APIRouter(tags=["cart"])


@cart_router.get("/cart/{session_id}")
async def read_cart(session_id: str) -> CartResponse:
    return await get_session_cart(session_id)
