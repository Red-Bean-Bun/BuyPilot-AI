"""Document/chunk read repository for retrieval and evidence lookup."""

from __future__ import annotations

import logging
from dataclasses import dataclass
from typing import Any

from sqlalchemy import text
from sqlalchemy.exc import SQLAlchemyError
from sqlmodel import col, select
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


@dataclass(frozen=True)
class VectorSearchFilters:
    category: str | None = None
    budget_max: float | None = None
    product_type_aliases: tuple[str, ...] = ()
    brand_avoid: tuple[str, ...] = ()
    avoid_product_ids: tuple[str, ...] = ()


@dataclass(frozen=True)
class ImageSimilarityHit:
    product_id: str
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


async def list_vector_chunks_by_similarity(
    query_embedding: list[float],
    limit: int,
    filters: VectorSearchFilters | None = None,
) -> list[VectorChunkHit]:
    await create_db_and_tables()
    engine = get_async_engine()
    if not is_postgres_engine(engine) or not query_embedding:
        return []

    filters = filters or VectorSearchFilters()
    sql_filter, params = _pgvector_sql_filters(filters)
    params.update({"query_embedding": vector_to_pg_literal(query_embedding), "limit": limit})
    try:
        async with engine.connect() as conn:
            rows = (
                (
                    await conn.execute(
                        text(
                            f"""
                    SELECT
                        pc.id,
                        pc.product_id,
                        pc.chunk_text,
                        pc.chunk_index,
                        pc.embedding,
                        pc."metadata" AS chunk_metadata,
                        pc.embedding <=> CAST(:query_embedding AS vector) AS distance
                    FROM product_chunks pc
                    JOIN products p ON p.id = pc.product_id
                    WHERE pc.embedding IS NOT NULL
                      AND COALESCE(pc."metadata"->>'retrieval_role', '') <> 'risk'
                      {sql_filter}
                    ORDER BY pc.embedding <=> CAST(:query_embedding AS vector)
                    LIMIT :limit
                    """
                        ),
                        params,
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


async def list_products_by_image_similarity(
    query_embedding: list[float],
    limit: int,
    filters: VectorSearchFilters | None = None,
) -> list[ImageSimilarityHit]:
    """pgvector cosine similarity on product_image_embeddings with SQL hard filters."""
    await create_db_and_tables()
    engine = get_async_engine()
    if not is_postgres_engine(engine) or not query_embedding:
        return []

    filters = filters or VectorSearchFilters()
    sql_filter, params = _image_sql_filters(filters)
    params.update({"query_embedding": vector_to_pg_literal(query_embedding), "limit": limit})
    try:
        async with engine.connect() as conn:
            rows = (
                (
                    await conn.execute(
                        text(
                            f"""
                    SELECT
                        pie.product_id,
                        pie.embedding <=> CAST(:query_embedding AS vector) AS distance
                    FROM product_image_embeddings pie
                    JOIN products p ON p.id = pie.product_id
                    WHERE pie.embedding IS NOT NULL
                      {sql_filter}
                    ORDER BY pie.embedding <=> CAST(:query_embedding AS vector)
                    LIMIT :limit
                    """
                        ),
                        params,
                    )
                )
                .mappings()
                .all()
            )
    except SQLAlchemyError:
        logger.exception("image similarity search failed")
        raise

    return [
        ImageSimilarityHit(
            product_id=str(row["product_id"]),
            distance=float(row["distance"]),
        )
        for row in rows
    ]


def _image_sql_filters(filters: VectorSearchFilters) -> tuple[str, dict[str, Any]]:
    clauses: list[str] = []
    params: dict[str, Any] = {}

    if filters.category:
        clauses.append("AND p.category = :img_category")
        params["img_category"] = filters.category
    if filters.budget_max is not None:
        clauses.append("AND (p.price IS NULL OR p.price <= :img_budget_max)")
        params["img_budget_max"] = filters.budget_max
    if filters.product_type_aliases:
        placeholders = _named_placeholders("img_product_type", filters.product_type_aliases, params, lowercase=True)
        clauses.append(f"AND lower(COALESCE(p.sub_category, '')) IN ({placeholders})")
    if filters.brand_avoid:
        placeholders = _named_placeholders("img_brand_avoid", filters.brand_avoid, params, lowercase=True)
        clauses.append(f"AND lower(COALESCE(p.brand, '')) NOT IN ({placeholders})")
    if filters.avoid_product_ids:
        placeholders = _named_placeholders("img_avoid_product", filters.avoid_product_ids, params)
        clauses.append(f"AND pie.product_id NOT IN ({placeholders})")

    return ("\n                      " + "\n                      ".join(clauses)) if clauses else "", params


def _pgvector_sql_filters(filters: VectorSearchFilters) -> tuple[str, dict[str, Any]]:
    clauses: list[str] = []
    params: dict[str, Any] = {}

    if filters.category:
        clauses.append("AND p.category = :category")
        params["category"] = filters.category
    if filters.budget_max is not None:
        clauses.append("AND (p.price IS NULL OR p.price <= :budget_max)")
        params["budget_max"] = filters.budget_max
    if filters.product_type_aliases:
        placeholders = _named_placeholders("product_type", filters.product_type_aliases, params, lowercase=True)
        clauses.append(f"AND lower(COALESCE(p.sub_category, '')) IN ({placeholders})")
    if filters.brand_avoid:
        placeholders = _named_placeholders("brand_avoid", filters.brand_avoid, params, lowercase=True)
        clauses.append(f"AND lower(COALESCE(p.brand, '')) NOT IN ({placeholders})")
    if filters.avoid_product_ids:
        placeholders = _named_placeholders("avoid_product", filters.avoid_product_ids, params)
        clauses.append(f"AND pc.product_id NOT IN ({placeholders})")

    return ("\n                      " + "\n                      ".join(clauses)) if clauses else "", params


def _named_placeholders(prefix: str, values: tuple[str, ...], params: dict[str, Any], lowercase: bool = False) -> str:
    names: list[str] = []
    for index, value in enumerate(dict.fromkeys(item.strip() for item in values if item and item.strip())):
        name = f"{prefix}_{index}"
        params[name] = value.casefold() if lowercase else value
        names.append(f":{name}")
    return ", ".join(names) if names else "NULL"


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


async def risk_chunks_for_products(product_ids: list[str]) -> dict[str, list[ChunkDocument]]:
    """Fetch risk-role chunks for given products (negative reviews, warnings)."""
    if not product_ids:
        return {}
    await create_db_and_tables()
    try:
        async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
            rows: list[ProductChunk] = list(
                (
                    await session.exec(
                        select(ProductChunk)
                        .where(col(ProductChunk.product_id).in_(product_ids))
                        .order_by(col(ProductChunk.product_id), col(ProductChunk.chunk_index))
                    )
                ).all()
            )
    except SQLAlchemyError:
        logger.exception("risk_chunks_for_products failed")
        return {}

    result: dict[str, list[ChunkDocument]] = {}
    for row in rows:
        meta = row.chunk_metadata or {}
        if meta.get("retrieval_role") != "risk":
            continue
        doc = ChunkDocument(
            id=row.id,
            product_id=row.product_id,
            chunk_text=row.chunk_text,
            chunk_index=row.chunk_index,
            embedding=row.embedding,
            metadata=meta,
        )
        result.setdefault(row.product_id, []).append(doc)
    return result


async def _evidence_chunks(product_id: str) -> list[ChunkDocument]:
    await create_db_and_tables()
    try:
        async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
            rows: list[ProductChunk] = list(
                (
                    await session.exec(
                        select(ProductChunk)
                        .where(ProductChunk.product_id == product_id)
                        .order_by(col(ProductChunk.chunk_index))
                    )
                ).all()
            )
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

    selected: list[ProductChunk] = [groups[kind] for kind in _EVIDENCE_KIND_PRIORITY if kind in groups]
    if not selected:
        selected = list(candidates[:1])

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
