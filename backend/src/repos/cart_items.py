"""In-memory cart repository for the P1 cart_action path."""

from __future__ import annotations

from datetime import datetime, timezone

from src.repos.products import get_product
from src.types.schemas import CartItemPayload, CartResponse

_CARTS: dict[str, dict[str, CartItemPayload]] = {}


def add_to_cart(session_id: str, product_id: str, quantity: int = 1) -> CartItemPayload:
    product = get_product(product_id)
    name = product.name if product else product_id
    price = product.price if product else None
    cart = _CARTS.setdefault(session_id, {})
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


def get_cart(session_id: str) -> CartResponse:
    items = list(_CARTS.get(session_id, {}).values())
    total_items = sum(item.quantity for item in items)
    total_price = sum((item.price or 0.0) * item.quantity for item in items)
    return CartResponse(items=items, total_items=total_items, total_price=total_price)

