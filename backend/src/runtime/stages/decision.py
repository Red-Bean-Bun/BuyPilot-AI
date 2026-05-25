"""Final decision stage wrapper."""

from __future__ import annotations

from src.services.llm_client import generate_decision
from src.types.schemas import DecisionResult
from src.types.sse_events import CriteriaPayload, EvidencePayload, ProductPayload


async def run_decision(
    criteria: CriteriaPayload,
    products: list[ProductPayload],
    evidence_by_product: dict[str, list[EvidencePayload]] | None = None,
) -> DecisionResult:
    return await generate_decision(criteria, products, evidence_by_product)
