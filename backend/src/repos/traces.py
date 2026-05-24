"""Retrieval trace and evidence link persistence."""

from __future__ import annotations

import logging
from typing import Any

from sqlalchemy.exc import SQLAlchemyError
from sqlmodel import Session, select

from src.config.settings import get_settings
from src.repos.database import create_db_and_tables, get_engine
from src.repos.models import EvidenceLink, ProductChunk, RetrievalTrace
from src.types.sse_events import CriteriaPayload, EvidencePayload, ProductPayload

logger = logging.getLogger(__name__)


def write_retrieval_trace(
    criteria: CriteriaPayload,
    products: list[ProductPayload],
    evidences_by_product: dict[str, list[EvidencePayload]],
    conversation_id: str | None = None,
    stage_timings_ms: dict[str, float] | None = None,
    fallback_events: list[dict[str, Any]] | None = None,
    trace_details: dict[str, Any] | None = None,
) -> str | None:
    create_db_and_tables()
    trace_details = trace_details or {}
    filters_applied = criteria.constraints.model_dump()
    filters_applied.update(_as_dict(trace_details.get("filters_applied")))
    if stage_timings_ms:
        filters_applied["_stage_timings_ms"] = stage_timings_ms
    if fallback_events:
        filters_applied["_fallbacks"] = fallback_events
    vector_top_k = _as_list(trace_details.get("vector_top_k")) or _default_vector_top_k(
        products,
        evidences_by_product,
    )
    rerank_top_n = _as_list(trace_details.get("rerank_top_n")) or _default_rerank_top_n(products)
    selected_ids = _as_str_list(trace_details.get("selected_ids")) or [product.product_id for product in products]
    trace = RetrievalTrace(
        conversation_id=conversation_id,
        criteria_id=criteria.criteria_id or None,
        filters_applied=filters_applied,
        vector_top_k=vector_top_k,
        rerank_top_n=rerank_top_n,
        selected_ids=selected_ids,
        hit_count=int(trace_details.get("hit_count") or len(products)),
        vector_count=int(trace_details.get("vector_count") or len(vector_top_k)),
    )
    try:
        with Session(get_engine()) as session:
            session.add(trace)
            session.commit()
            session.refresh(trace)
            return trace.id
    except SQLAlchemyError:
        logger.exception("write_retrieval_trace failed")
        if get_settings().strict_runtime:
            raise
        return None


def write_evidence_links(
    products: list[ProductPayload],
    evidences_by_product: dict[str, list[EvidencePayload]],
    conversation_id: str | None = None,
    cited_in: str = "product_card",
) -> int:
    create_db_and_tables()
    count = 0
    try:
        with Session(get_engine()) as session:
            for product in products:
                for evidence in evidences_by_product.get(product.product_id, []):
                    source_id_raw = evidence.source_id
                    session.add(
                        EvidenceLink(
                            conversation_id=conversation_id,
                            product_id=product.product_id,
                            chunk_id=_persistable_chunk_id(session, source_id_raw),
                            source_id_raw=source_id_raw,
                            snippet=evidence.snippet,
                            evidence_type=evidence.source_type,
                            relevance_score=None,
                            cited_in=cited_in,
                        )
                    )
                    count += 1
            session.commit()
    except SQLAlchemyError:
        logger.exception("write_evidence_links failed")
        if get_settings().strict_runtime:
            raise
        return 0
    return count


def list_recent_retrieval_traces(limit: int = 50) -> list[RetrievalTrace]:
    create_db_and_tables()
    with Session(get_engine()) as session:
        return list(session.exec(select(RetrievalTrace).order_by(RetrievalTrace.created_at.desc()).limit(limit)).all())


def _persistable_chunk_id(session: Session, source_id: str | None) -> str | None:
    if not source_id:
        return None
    return source_id if session.get(ProductChunk, source_id) is not None else None


def _default_vector_top_k(
    products: list[ProductPayload],
    evidences_by_product: dict[str, list[EvidencePayload]],
) -> list[dict[str, Any]]:
    return [
        {
            "product_id": product.product_id,
            "rank": index + 1,
            "evidence_ids": [evidence.source_id for evidence in evidences_by_product.get(product.product_id, [])],
        }
        for index, product in enumerate(products)
    ]


def _default_rerank_top_n(products: list[ProductPayload]) -> list[dict[str, Any]]:
    return [
        {
            "product_id": product.product_id,
            "rank": index + 1,
        }
        for index, product in enumerate(products)
    ]


def _as_dict(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def _as_list(value: Any) -> list[dict[str, Any]]:
    return value if isinstance(value, list) else []


def _as_str_list(value: Any) -> list[str]:
    if not isinstance(value, list):
        return []
    return [str(item) for item in value if item is not None]
