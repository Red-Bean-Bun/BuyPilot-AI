"""Cart repository backed by SQLModel."""

from __future__ import annotations

from sqlmodel import Session, select

from src.repos.database import create_db_and_tables, get_engine
from src.repos.models import CartItem
from src.repos.products import get_product
from src.types.schemas import CartItemPayload, CartResponse


def add_to_cart(session_id: str, product_id: str, quantity: int = 1) -> CartItemPayload:
    if quantity <= 0:
        quantity = 1

    create_db_and_tables()
    with Session(get_engine()) as session:
        row = session.exec(
            select(CartItem).where(CartItem.session_id == session_id).where(CartItem.product_id == product_id).limit(1)
        ).first()
        if row:
            row.quantity += quantity
        else:
            row = CartItem(session_id=session_id, product_id=product_id, quantity=quantity)
            session.add(row)
        session.commit()
        session.refresh(row)
        return _payload_from_row(row)


def get_cart(session_id: str) -> CartResponse:
    create_db_and_tables()
    with Session(get_engine()) as session:
        rows = session.exec(select(CartItem).where(CartItem.session_id == session_id).order_by(CartItem.added_at)).all()
    return _cart_response([_payload_from_row(row) for row in rows])


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


def _cart_response(items: list[CartItemPayload]) -> CartResponse:
    total_items = sum(item.quantity for item in items)
    total_price = sum((item.price or 0.0) * item.quantity for item in items)
    return CartResponse(items=items, total_items=total_items, total_price=total_price)
