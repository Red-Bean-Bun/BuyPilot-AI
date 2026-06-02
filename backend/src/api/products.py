"""Product detail endpoint for the Android detail page."""

from __future__ import annotations

from fastapi import APIRouter, HTTPException, status

from src.services.catalog import get_product_detail

products_router = APIRouter(tags=["products"])


@products_router.get("/products/{product_id}")
async def get_product_detail_endpoint(product_id: str):
    result = await get_product_detail(product_id)
    if result is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Product not found: {product_id}",
        )
    return result
