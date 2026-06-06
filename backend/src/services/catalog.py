"""Catalog read service boundary."""

from __future__ import annotations

import logging

from src.repos.products import get_product as repo_get_product
from src.repos.products import get_product_detail_rows
from src.types.schemas import FaqItem, ProductDetailResponse, ReviewItem
from src.types.sse_events import ProductPayload

logger = logging.getLogger(__name__)


def get_product(product_id: str) -> ProductPayload | None:
    return repo_get_product(product_id)


async def get_product_detail(product_id: str) -> ProductDetailResponse | None:
    """Compose product detail from raw JSON cache + DB detail fields."""
    product = repo_get_product(product_id)
    if product is None:
        return None

    detail = await get_product_detail_rows(product_id)

    raw_faqs = (detail or {}).get("official_faq") or []
    raw_reviews = (detail or {}).get("user_reviews") or []
    faqs = [f for f in (_safe_faq(faq) for faq in raw_faqs) if f is not None]
    reviews = [r for r in (_safe_review(rev) for rev in raw_reviews) if r is not None]

    return ProductDetailResponse(
        product=product,
        marketing_description=(detail or {}).get("marketing_description"),
        highlights=(detail or {}).get("highlights") or [],
        faqs=faqs,
        reviews=reviews,
    )


def _safe_faq(d: dict) -> FaqItem | None:
    try:
        return FaqItem(**d)
    except Exception:
        logger.warning("Malformed FAQ entry skipped", exc_info=True)
        return None


def _safe_review(d: dict) -> ReviewItem | None:
    try:
        return ReviewItem(**d)
    except Exception:
        logger.warning("Malformed review entry skipped", exc_info=True)
        return None
