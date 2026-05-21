"""Retrieval trace and evidence link persistence."""

from __future__ import annotations

from sqlalchemy.exc import SQLAlchemyError
from sqlmodel import Session

from src.repos.database import create_db_and_tables, get_engine
from src.repos.models import EvidenceLink, RetrievalTrace
from src.types.sse_events import CriteriaPayload, EvidencePayload, ProductPayload


def write_retrieval_trace(
    criteria: CriteriaPayload,
    products: list[ProductPayload],
    evidences_by_product: dict[str, list[EvidencePayload]],
    conversation_id: str | None = None,
) -> str | None:
    create_db_and_tables()
    trace = RetrievalTrace(
        conversation_id=conversation_id,
        criteria_id=criteria.criteria_id or None,
        filters_applied=criteria.constraints.model_dump(),
        vector_top_k=[
            {
                "product_id": product.product_id,
                "rank": index + 1,
                "evidence_ids": [evidence.source_id for evidence in evidences_by_product.get(product.product_id, [])],
            }
            for index, product in enumerate(products)
        ],
        rerank_top_n=[
            {
                "product_id": product.product_id,
                "rank": index + 1,
            }
            for index, product in enumerate(products)
        ],
        selected_ids=[product.product_id for product in products],
        hit_count=len(products),
        vector_count=len(products),
    )
    try:
        with Session(get_engine()) as session:
            session.add(trace)
            session.commit()
            session.refresh(trace)
            return trace.id
    except SQLAlchemyError:
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
                    session.add(
                            EvidenceLink(
                                conversation_id=conversation_id,
                                product_id=product.product_id,
                                chunk_id=evidence.source_id if evidence.source_id and ":" in evidence.source_id else None,
                                evidence_type=evidence.source_type,
                            relevance_score=None,
                            cited_in=cited_in,
                        )
                    )
                    count += 1
            session.commit()
    except SQLAlchemyError:
        return 0
    return count
