"""Evidence binding service."""

from __future__ import annotations

from src.repos.documents import evidence_for_product
from src.services.retriever import cached_evidence_for_product
from src.types.sse_events import EvidencePayload, ProductPayload


async def get_evidence(product: ProductPayload) -> list[EvidencePayload]:
    cached = cached_evidence_for_product(product.product_id)
    if cached is not None:
        return [cached]
    return [evidence_for_product(product)]
