"""Document/chunk read repository for retrieval and evidence lookup."""

from __future__ import annotations

import logging
from dataclasses import dataclass
from typing import Any

from sqlalchemy import text
from sqlalchemy.exc import SQLAlchemyError
from sqlmodel import select
from sqlmodel.ext.asyncio.session import AsyncSession

from src.repos.database import create_db_and_tables, get_async_engine, is_postgres_engine
from src.repos.models import ProductChunk
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


async def list_embedded_chunks() -> list[ChunkDocument]:
    await create_db_and_tables()
    try:
        async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
            rows = (await session.exec(select(ProductChunk))).all()
    except SQLAlchemyError:
        logger.exception("list_embedded_chunks failed")
        raise
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


async def list_vector_chunks_by_similarity(query_embedding: list[float], limit: int) -> list[VectorChunkHit]:
    await create_db_and_tables()
    engine = get_async_engine()
    if not is_postgres_engine(engine) or not query_embedding:
        return []

    try:
        async with engine.connect() as conn:
            rows = (
                (
                    await conn.execute(
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
                )
                .mappings()
                .all()
            )
    except SQLAlchemyError:
        logger.exception("pgvector similarity search failed")
        raise

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


async def evidence_for_product(product: ProductPayload) -> list[EvidencePayload]:
    chunks = await _evidence_chunks(product.product_id)
    if chunks:
        return [evidence_for_chunk(chunk) for chunk in chunks]
    parts = [
        product.name,
        product.category,
        product.sub_category or "",
        f"{product.price:g}元" if product.price is not None else "",
    ]
    snippet = " | ".join(part for part in parts if part)
    return [
        EvidencePayload(
            source_type="product_fallback",
            snippet=snippet,
            source_id=f"fallback_{product.product_id}",
        )
    ]


_EVIDENCE_KIND_PRIORITY = ("why_buy", "faq", "risk", "compare")


async def _evidence_chunks(product_id: str) -> list[ChunkDocument]:
    await create_db_and_tables()
    try:
        async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
            rows = (
                await session.exec(
                    select(ProductChunk).where(ProductChunk.product_id == product_id).order_by(ProductChunk.chunk_index)
                )
            ).all()
    except SQLAlchemyError:
        logger.exception("evidence chunks lookup failed")
        raise

    if not rows:
        return []

    preferred = [row for row in rows if (row.chunk_metadata or {}).get("retrieval_role") != "risk"]
    candidates = preferred or rows

    groups: dict[str, Any] = {}
    for row in candidates:
        kind = (row.chunk_metadata or {}).get("evidence_kind") or "other"
        if kind not in groups:
            groups[kind] = row

    selected = [groups[kind] for kind in _EVIDENCE_KIND_PRIORITY if kind in groups]
    if not selected:
        selected = candidates[:1]

    return [
        ChunkDocument(
            id=row.id,
            product_id=row.product_id,
            chunk_text=row.chunk_text,
            chunk_index=row.chunk_index,
            embedding=row.embedding,
            metadata=row.chunk_metadata,
        )
        for row in selected
    ]
