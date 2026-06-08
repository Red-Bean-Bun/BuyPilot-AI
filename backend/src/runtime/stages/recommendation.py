"""Recommendation stage wrapper."""

from __future__ import annotations

from collections.abc import AsyncGenerator
from dataclasses import dataclass, field
from typing import Mapping

from src.services.llm_client import generate_recommendation, stream_recommendation
from src.services.retriever import retrieve_with_evidence
from src.types.schemas import RecommendationResult
from src.types.sse_events import CriteriaPayload, EvidencePayload, ProductPayload


@dataclass(frozen=True)
class RetrievalResult:
    products: list[ProductPayload]
    evidence_by_product: dict[str, list[EvidencePayload]]
    trace_details: dict = field(default_factory=dict)


async def run_retrieval(
    criteria: CriteriaPayload,
    top_n: int = 5,
    feedback: Mapping[str, list[str]] | None = None,
    image_embedding: list[float] | None = None,
) -> RetrievalResult:
    retrieval = await retrieve_with_evidence(criteria, top_n=top_n, feedback=feedback, image_embedding=image_embedding)
    return RetrievalResult(
        products=retrieval.products,
        evidence_by_product=retrieval.evidence_by_product,
        trace_details=retrieval.trace_details,
    )


async def run_recommendation_text(
    criteria: CriteriaPayload,
    products: list[ProductPayload],
    evidence_by_product: dict[str, list[EvidencePayload]] | None = None,
    conversation_context: str = "",
) -> RecommendationResult:
    result = await generate_recommendation(criteria, products, evidence_by_product, conversation_context)
    return result.model_copy(update={"products": products})


async def run_recommendation_text_stream(
    criteria: CriteriaPayload,
    products: list[ProductPayload],
    evidence_by_product: dict[str, list[EvidencePayload]] | None = None,
    conversation_context: str = "",
) -> AsyncGenerator[str, None]:
    async for delta in stream_recommendation(criteria, products, evidence_by_product, conversation_context):
        yield delta


async def run_recommendation(
    criteria: CriteriaPayload,
    feedback: Mapping[str, list[str]] | None = None,
) -> RecommendationResult:
    retrieval = await run_retrieval(criteria, feedback=feedback)
    result = await run_recommendation_text(criteria, retrieval.products, retrieval.evidence_by_product)
    return result.model_copy(update={"evidence_by_product": retrieval.evidence_by_product})
