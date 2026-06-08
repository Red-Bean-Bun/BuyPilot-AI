"""Final decision stage wrapper."""

from __future__ import annotations

from src.services.llm_client import generate_decision
from src.types.schemas import DecisionResult
from src.types.sse_events import CriteriaPayload, EvidencePayload, ProductPayload


async def run_decision(
    criteria: CriteriaPayload,
    products: list[ProductPayload],
    evidence_by_product: dict[str, list[EvidencePayload]] | None = None,
    *,
    locked_winner_product_id: str | None = None,
    score_breakdown: dict | None = None,
    conversation_context: str = "",
) -> DecisionResult:
    return await generate_decision(
        criteria,
        products,
        evidence_by_product,
        locked_winner_product_id=locked_winner_product_id,
        score_breakdown=score_breakdown,
        conversation_context=conversation_context,
    )
