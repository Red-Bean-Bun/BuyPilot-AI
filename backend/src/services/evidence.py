"""Evidence binding service."""

from __future__ import annotations

from src.repos.documents import evidence_for_product
from src.services.async_io import run_sync_io
from src.types.sse_events import EvidencePayload, ProductPayload


async def get_evidence(product: ProductPayload) -> list[EvidencePayload]:
    return [await run_sync_io(evidence_for_product, product)]
