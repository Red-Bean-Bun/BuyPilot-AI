"""Cart use cases.

API and runtime code should call this module instead of reaching into the
cart repository directly.
"""

from __future__ import annotations

import logging
from datetime import datetime, timezone

from sqlalchemy.exc import SQLAlchemyError

from src.config.settings import get_settings
from src.repos.cart_items import add_to_cart, get_cart
from src.repos.products import get_product
from src.services.fallbacks import record_fallback
from src.types.schemas import CartItemPayload, CartResponse

logger = logging.getLogger(__name__)

_MEMORY_CARTS: dict[str, dict[str, CartItemPayload]] = {}


async def add_product_to_cart(session_id: str, product_id: str, quantity: int = 1) -> CartItemPayload:
    try:
        item = await add_to_cart(session_id, product_id, quantity=quantity)
        if _memory_state_fallback_enabled():
            _cache_item(session_id, item)
        return item
    except SQLAlchemyError:
        logger.exception("add_to_cart DB write failed")
        if not _memory_state_fallback_enabled():
            raise
        record_fallback("cart", "explicit_dev_memory_fallback", operation="add")
        return _add_to_memory_cart(session_id, product_id, quantity)


async def get_session_cart(session_id: str) -> CartResponse:
    try:
        cart = await get_cart(session_id)
        if _memory_state_fallback_enabled():
            for item in cart.items:
                _cache_item(session_id, item)
        return cart
    except SQLAlchemyError:
        logger.exception("get_cart DB read failed")
        if not _memory_state_fallback_enabled():
            raise
        record_fallback("cart", "explicit_dev_memory_fallback", operation="get")
        return _cart_response(list(_MEMORY_CARTS.get(session_id, {}).values()))


def _memory_state_fallback_enabled() -> bool:
    settings = get_settings()
    return settings.allow_memory_state_fallback and not settings.strict_runtime


def _add_to_memory_cart(session_id: str, product_id: str, quantity: int) -> CartItemPayload:
    if quantity <= 0:
        quantity = 1
    product = get_product(product_id)
    name = product.name if product else product_id
    price = product.price if product else None
    cart = _MEMORY_CARTS.setdefault(session_id, {})
    existing = cart.get(product_id)
    if existing:
        existing.quantity += quantity
        return existing
    item = CartItemPayload(
        product_id=product_id,
        name=name,
        price=price,
        quantity=quantity,
        added_at=datetime.now(timezone.utc).isoformat(),
        product=product,
    )
    cart[product_id] = item
    return item


def _cache_item(session_id: str, item: CartItemPayload) -> None:
    _MEMORY_CARTS.setdefault(session_id, {})[item.product_id] = item


def _cart_response(items: list[CartItemPayload]) -> CartResponse:
    total_items = sum(item.quantity for item in items)
    total_price = sum((item.price or 0.0) * item.quantity for item in items)
    return CartResponse(items=items, total_items=total_items, total_price=total_price)
