"""Recommendation stage wrapper."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Mapping

from src.services.llm_client import generate_recommendation
from src.services.retriever import cached_evidence_for_product, retrieve
from src.types.schemas import RecommendationResult
from src.types.sse_events import CriteriaPayload, EvidencePayload, ProductPayload


@dataclass(frozen=True)
class RetrievalResult:
    products: list[ProductPayload]
    evidence_by_product: dict[str, list[EvidencePayload]]


async def run_retrieval(
    criteria: CriteriaPayload,
    top_n: int = 5,
    feedback: Mapping[str, list[str]] | None = None,
) -> RetrievalResult:
    products = await retrieve(criteria, top_n=top_n, feedback=feedback)
    return RetrievalResult(
        products=products,
        evidence_by_product=_cached_evidence_for_products(products),
    )


async def run_recommendation_text(criteria: CriteriaPayload, products: list[ProductPayload]) -> RecommendationResult:
    result = await generate_recommendation(criteria, products)
    return result.model_copy(update={"products": products})


async def run_recommendation(
    criteria: CriteriaPayload,
    feedback: Mapping[str, list[str]] | None = None,
) -> RecommendationResult:
    retrieval = await run_retrieval(criteria, feedback=feedback)
    result = await run_recommendation_text(criteria, retrieval.products)
    return result.model_copy(update={"evidence_by_product": retrieval.evidence_by_product})


def _cached_evidence_for_products(products: list[ProductPayload]) -> dict[str, list[EvidencePayload]]:
    return {
        product.product_id: [evidence]
        for product in products
        if (evidence := cached_evidence_for_product(product.product_id)) is not None
    }
