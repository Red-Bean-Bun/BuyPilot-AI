"""Recommendation stage wrapper."""

from __future__ import annotations

from src.services.llm_client import generate_recommendation
from src.services.retriever import retrieve
from src.types.schemas import RecommendationResult
from src.types.sse_events import CriteriaPayload


async def run_recommendation(criteria: CriteriaPayload) -> RecommendationResult:
    products = await retrieve(criteria, top_n=5)
    return await generate_recommendation(criteria, products)

