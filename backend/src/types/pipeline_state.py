"""Shared state shape passed between runtime pipeline stages."""

from __future__ import annotations

from typing import Any, TypedDict

from src.types.schemas import IntentResult
from src.types.sse_events import CriteriaPayload, ProductPayload, Constraints


class PipelineState(TypedDict, total=False):
    session_id: str
    turn_id: str
    user_message: str
    image_url: str | None
    history: list[dict[str, Any]]
    intent: IntentResult | None
    criteria: CriteriaPayload | None
    needs_clarification: bool
    clarification_questions: list[str] | None
    missing_slots: list[str]
    filters: Constraints | None
    soft_preferences: dict[str, Any] | None
    query_embedding: list[float] | None
    retrieval_results: list[ProductPayload] | None
    reranked_results: list[ProductPayload] | None
    recommendation_text: str | None
    decision: dict[str, Any] | None
