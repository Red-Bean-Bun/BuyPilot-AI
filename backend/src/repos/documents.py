"""Document/chunk read repository for retrieval and evidence lookup."""

from __future__ import annotations

import logging
from dataclasses import dataclass

from sqlalchemy import text
from sqlalchemy.exc import SQLAlchemyError
from sqlmodel import Session, select

from src.config.settings import get_settings
from src.repos.database import get_engine, is_postgres_engine
from src.repos.models import ProductChunk
from src.repos.products import evidence_snippet
from src.repos.vector import coerce_vector, vector_to_pg_literal
from src.types.sse_events import EvidencePayload, ProductPayload

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class ChunkDocument:
    id: str
    product_id: str
    chunk_text: str
    chunk_index: int
    embedding: list[float]
    metadata: dict


@dataclass(frozen=True)
class VectorChunkHit:
    document: ChunkDocument
    distance: float


def list_embedded_chunks() -> list[ChunkDocument]:
    try:
        with Session(get_engine()) as session:
            rows = session.exec(select(ProductChunk)).all()
    except SQLAlchemyError:
        logger.exception("list_embedded_chunks failed")
        if get_settings().strict_runtime:
            raise
        return []
    return [
        ChunkDocument(
            id=row.id,
            product_id=row.product_id,
            chunk_text=row.chunk_text,
            chunk_index=row.chunk_index,
            embedding=row.embedding,
            metadata=row.chunk_metadata,
        )
        for row in rows
        if row.embedding
    ]


def list_vector_chunks_by_similarity(query_embedding: list[float], limit: int) -> list[VectorChunkHit]:
    engine = get_engine()
    if not is_postgres_engine(engine) or not query_embedding:
        if get_settings().strict_runtime:
            raise RuntimeError("Strict runtime requires PostgreSQL/pgvector for similarity search.")
        return []

    try:
        with engine.connect() as conn:
            rows = (
                conn.execute(
                    text(
                        """
                    SELECT
                        id,
                        product_id,
                        chunk_text,
                        chunk_index,
                        embedding,
                        "metadata" AS chunk_metadata,
                        embedding <=> CAST(:query_embedding AS vector) AS distance
                    FROM product_chunks
                    WHERE embedding IS NOT NULL
                      AND COALESCE("metadata"->>'retrieval_role', '') <> 'risk'
                    ORDER BY embedding <=> CAST(:query_embedding AS vector)
                    LIMIT :limit
                    """
                    ),
                    {"query_embedding": vector_to_pg_literal(query_embedding), "limit": limit},
                )
                .mappings()
                .all()
            )
    except SQLAlchemyError:
        logger.exception("pgvector similarity search failed")
        if get_settings().strict_runtime:
            raise
        return []

    hits: list[VectorChunkHit] = []
    for row in rows:
        hits.append(
            VectorChunkHit(
                document=ChunkDocument(
                    id=str(row["id"]),
                    product_id=str(row["product_id"]),
                    chunk_text=str(row["chunk_text"]),
                    chunk_index=int(row["chunk_index"]),
                    embedding=coerce_vector(row["embedding"]),
                    metadata=dict(row["chunk_metadata"] or {}),
                ),
                distance=float(row["distance"]),
            )
        )
    return hits


def evidence_for_chunk(chunk: ChunkDocument, max_chars: int = 180) -> EvidencePayload:
    return EvidencePayload(
        source_type="product_chunk",
        snippet=" ".join(chunk.chunk_text.split())[:max_chars],
        source_id=chunk.id,
    )


def evidence_for_product(product: ProductPayload) -> EvidencePayload:
    dataset_snippet = evidence_snippet(product.product_id)
    if dataset_snippet:
        return EvidencePayload(
            source_type="product_chunk",
            snippet=dataset_snippet,
            source_id=f"chunk_{product.product_id}",
        )

    parts = [
        product.name,
        product.category,
        product.sub_category or "",
        product.use_scenario or "",
        f"{product.price:g}元" if product.price is not None else "",
    ]
    snippet = "，".join(part for part in parts if part)
    return EvidencePayload(
        source_type="product_chunk",
        snippet=snippet,
        source_id=f"chunk_{product.product_id}",
    )
