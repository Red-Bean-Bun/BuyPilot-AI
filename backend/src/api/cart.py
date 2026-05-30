"""Cart query endpoint matching the backend PRD."""

from __future__ import annotations

from fastapi import APIRouter, HTTPException, status

from src.services.cart import get_session_cart, remove_product_from_cart, update_product_quantity
from src.types.schemas import CartItemPayload, CartMutationRequest, CartResponse

cart_router = APIRouter(tags=["cart"])


@cart_router.get("/cart/{session_id}")
async def read_cart(session_id: str) -> CartResponse:
    return await get_session_cart(session_id)


@cart_router.patch("/cart/{session_id}/items/{product_id}")
async def patch_cart_item(session_id: str, product_id: str, body: CartMutationRequest) -> CartItemPayload:
    try:
        item = await update_product_quantity(session_id, product_id, body.quantity)
    except ValueError:
        # Product not found in catalog or cart item doesn't exist
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Product or cart item not found.")
    if item is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Cart item not found.")
    return item


@cart_router.delete("/cart/{session_id}/items/{product_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_cart_item(session_id: str, product_id: str) -> None:
    item = await remove_product_from_cart(session_id, product_id)
    if item is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Cart item not found.")
