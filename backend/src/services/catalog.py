"""Catalog read service boundary."""

from __future__ import annotations

from src.repos.products import get_product as repo_get_product
from src.types.sse_events import ProductPayload


def get_product(product_id: str) -> ProductPayload | None:
    return repo_get_product(product_id)
