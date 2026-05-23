"""Cart repository backed by SQLModel with an in-memory fallback."""

from __future__ import annotations

from datetime import datetime, timezone

from sqlalchemy.exc import SQLAlchemyError
from sqlmodel import Session, select

from src.repos.database import create_db_and_tables, get_engine
from src.repos.models import CartItem
from src.repos.products import get_product
from src.types.schemas import CartItemPayload, CartResponse

_CARTS: dict[str, dict[str, CartItemPayload]] = {}


def add_to_cart(session_id: str, product_id: str, quantity: int = 1) -> CartItemPayload:
    if quantity <= 0:
        quantity = 1

    try:
        create_db_and_tables()
        with Session(get_engine()) as session:
            row = session.exec(
                select(CartItem)
                .where(CartItem.session_id == session_id)
                .where(CartItem.product_id == product_id)
                .limit(1)
            ).first()
            if row:
                row.quantity += quantity
            else:
                row = CartItem(session_id=session_id, product_id=product_id, quantity=quantity)
                session.add(row)
            session.commit()
            session.refresh(row)
            item = _payload_from_row(row)
            _cache_item(session_id, item)
            return item
    except SQLAlchemyError:
        return _add_to_memory_cart(session_id, product_id, quantity)


def get_cart(session_id: str) -> CartResponse:
    try:
        create_db_and_tables()
        with Session(get_engine()) as session:
            rows = session.exec(
                select(CartItem)
                .where(CartItem.session_id == session_id)
                .order_by(CartItem.added_at)
            ).all()
        items = [_payload_from_row(row) for row in rows]
        for item in items:
            _cache_item(session_id, item)
        return _cart_response(items)
    except SQLAlchemyError:
        return _cart_response(list(_CARTS.get(session_id, {}).values()))


def _add_to_memory_cart(session_id: str, product_id: str, quantity: int) -> CartItemPayload:
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


def _payload_from_row(row: CartItem) -> CartItemPayload:
    product = get_product(row.product_id)
    return CartItemPayload(
        product_id=row.product_id,
        name=product.name if product else row.product_id,
        price=product.price if product else None,
        quantity=row.quantity,
        added_at=row.added_at.isoformat() if row.added_at else None,
        product=product,
    )


def _cache_item(session_id: str, item: CartItemPayload) -> None:
    _CARTS.setdefault(session_id, {})[item.product_id] = item


def _cart_response(items: list[CartItemPayload]) -> CartResponse:
    total_items = sum(item.quantity for item in items)
    total_price = sum((item.price or 0.0) * item.quantity for item in items)
    return CartResponse(items=items, total_items=total_items, total_price=total_price)
