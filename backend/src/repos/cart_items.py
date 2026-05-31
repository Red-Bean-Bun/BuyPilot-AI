"""Cart repository backed by SQLModel."""

from __future__ import annotations

from uuid import uuid4

from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.dialects.sqlite import insert as sqlite_insert
from sqlmodel import select
from sqlmodel.ext.asyncio.session import AsyncSession

from src.repos.database import create_db_and_tables, get_async_engine
from src.repos.models import CartItem, utc_now
from src.repos.products import get_product
from src.types.schemas import CartItemPayload, CartResponse


async def add_to_cart(session_id: str, product_id: str, quantity: int = 1) -> CartItemPayload:
    if quantity <= 0:
        quantity = 1
    product = get_product(product_id)
    if product is None:
        raise ValueError(f"Product not found: {product_id}")

    await create_db_and_tables()
    engine = get_async_engine()
    async with AsyncSession(engine, expire_on_commit=False) as session:
        if engine.dialect.name in {"postgresql", "sqlite"}:
            await _upsert_cart_item(session, engine.dialect.name, session_id, product_id, quantity)
            row = await _get_cart_item_row(session, session_id, product_id)
            if row is None:
                raise RuntimeError("Cart upsert succeeded but row lookup failed.")
            return _payload_from_row(row)

        row = (
            await session.exec(
                select(CartItem)
                .where(CartItem.session_id == session_id)
                .where(CartItem.product_id == product_id)
                .limit(1)
            )
        ).first()
        if row:
            row.quantity += quantity
        else:
            row = CartItem(session_id=session_id, product_id=product_id, quantity=quantity)
            session.add(row)
        await session.commit()
        await session.refresh(row)
        return _payload_from_row(row)


async def get_cart(session_id: str) -> CartResponse:
    await create_db_and_tables()
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        rows = (
            await session.exec(select(CartItem).where(CartItem.session_id == session_id).order_by(CartItem.added_at))
        ).all()
    return _cart_response([_payload_from_row(row) for row in rows])


async def remove_from_cart(session_id: str, product_id: str) -> CartItemPayload | None:
    product = get_product(product_id)
    if product is None:
        raise ValueError(f"Product not found: {product_id}")

    await create_db_and_tables()
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        row = (
            await session.exec(
                select(CartItem)
                .where(CartItem.session_id == session_id)
                .where(CartItem.product_id == product_id)
                .limit(1)
            )
        ).first()
        if row is None:
            return None
        payload = _payload_from_row(row)
        await session.delete(row)
        await session.commit()
        return payload


async def update_cart_quantity(session_id: str, product_id: str, quantity: int) -> CartItemPayload | None:
    if quantity <= 0:
        return await remove_from_cart(session_id, product_id)
    product = get_product(product_id)
    if product is None:
        raise ValueError(f"Product not found: {product_id}")

    await create_db_and_tables()
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        row = (
            await session.exec(
                select(CartItem)
                .where(CartItem.session_id == session_id)
                .where(CartItem.product_id == product_id)
                .limit(1)
            )
        ).first()
        if row is None:
            return None
        row.quantity = quantity
        await session.commit()
        await session.refresh(row)
        return _payload_from_row(row)


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


async def _upsert_cart_item(
    session: AsyncSession,
    dialect_name: str,
    session_id: str,
    product_id: str,
    quantity: int,
) -> None:
    table = CartItem.__table__
    insert_fn = pg_insert if dialect_name == "postgresql" else sqlite_insert
    stmt = (
        insert_fn(table)
        .values(
            id=str(uuid4()),
            session_id=session_id,
            product_id=product_id,
            quantity=quantity,
            added_at=utc_now(),
        )
        .on_conflict_do_update(
            index_elements=["session_id", "product_id"],
            set_={"quantity": table.c.quantity + quantity},
        )
    )
    await session.exec(stmt)
    await session.commit()


async def _get_cart_item_row(session: AsyncSession, session_id: str, product_id: str) -> CartItem | None:
    return (
        await session.exec(
            select(CartItem).where(CartItem.session_id == session_id).where(CartItem.product_id == product_id).limit(1)
        )
    ).first()
