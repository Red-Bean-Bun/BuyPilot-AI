"""Retrieval trace persistence use cases."""

from __future__ import annotations

from typing import Any

from src.repos.traces import write_evidence_links, write_retrieval_trace
from src.types.sse_events import CriteriaPayload, EvidencePayload, ProductPayload


def record_retrieval_trace(
    criteria: CriteriaPayload,
    products: list[ProductPayload],
    evidences_by_product: dict[str, list[EvidencePayload]],
    conversation_id: str | None = None,
    stage_timings_ms: dict[str, float] | None = None,
    fallback_events: list[dict[str, Any]] | None = None,
) -> str | None:
    return write_retrieval_trace(
        criteria,
        products,
        evidences_by_product,
        conversation_id=conversation_id,
        stage_timings_ms=stage_timings_ms,
        fallback_events=fallback_events,
    )


def record_evidence_links(
    products: list[ProductPayload],
    evidences_by_product: dict[str, list[EvidencePayload]],
    conversation_id: str | None = None,
    cited_in: str = "product_card",
) -> int:
    return write_evidence_links(
        products,
        evidences_by_product,
        conversation_id=conversation_id,
        cited_in=cited_in,
    )
