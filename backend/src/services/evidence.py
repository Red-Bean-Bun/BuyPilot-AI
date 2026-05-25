"""Evidence binding service."""

from __future__ import annotations

from src.repos.documents import evidence_for_product
from src.types.sse_events import EvidencePayload, ProductPayload


async def get_evidence(product: ProductPayload) -> list[EvidencePayload]:
    return await evidence_for_product(product)
