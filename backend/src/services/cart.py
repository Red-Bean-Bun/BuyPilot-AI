"""Cart use cases.

API and runtime code should call this module instead of reaching into the
cart repository directly.
"""

from __future__ import annotations

from src.repos.cart_items import add_to_cart, get_cart, remove_from_cart, update_cart_quantity
from src.types.schemas import CartItemPayload, CartResponse


async def add_product_to_cart(session_id: str, product_id: str, quantity: int = 1) -> CartItemPayload:
    return await add_to_cart(session_id, product_id, quantity=quantity)


async def get_session_cart(session_id: str) -> CartResponse:
    return await get_cart(session_id)


async def remove_product_from_cart(session_id: str, product_id: str) -> CartItemPayload | None:
    return await remove_from_cart(session_id, product_id)


async def update_product_quantity(session_id: str, product_id: str, quantity: int) -> CartItemPayload | None:
    return await update_cart_quantity(session_id, product_id, quantity)
