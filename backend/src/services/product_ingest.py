"""Seed database tables from the official ecommerce dataset."""

from __future__ import annotations

import asyncio
import hashlib
import json
from dataclasses import dataclass
from datetime import timezone
from typing import Callable

from sqlalchemy import delete, func, text
from sqlmodel import select
from sqlmodel.ext.asyncio.session import AsyncSession

from src.config.domain_terms import normalize_category
from src.config.settings import get_settings
from src.repos.database import create_db_and_tables, get_async_engine, is_postgres_engine
from src.repos.models import Product, ProductChunk, SystemMetadata, utc_now
from src.repos.products import list_raw_products
from src.services.chunking import build_product_chunks, build_product_knowledge_package
from src.services.embedding import embed_texts

EXPECTED_EMBEDDING_DIMENSIONS = 1024
DATASET_VERSION = "ecommerce_agent_dataset:v1"
CHUNKING_VERSION = "semantic_v1"


ProgressCallback = Callable[[int, int], None]


@dataclass(frozen=True)
class ChunkSeedRow:
    product_id: str
    chunk_text: str
    chunk_index: int
    metadata: dict


async def seed_products(
    expected_embedding_dimensions: int | None = None,
    progress: ProgressCallback | None = None,
) -> dict[str, int]:
    await create_db_and_tables()
    products = list_raw_products()
    indexed_at = utc_now().astimezone(timezone.utc).isoformat()
    embedding_model = _embedding_model_name()
    chunk_rows = _build_chunk_rows(products)
    embeddings = await _embed_chunks(chunk_rows, expected_dimensions=expected_embedding_dimensions, progress=progress)

    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        for raw in products:
            product_id = str(raw["product_id"])
            source_category = str(raw.get("category") or "")
            source_hash = _source_hash(raw)
            knowledge_package = build_product_knowledge_package(raw)
            await session.exec(delete(ProductChunk).where(ProductChunk.product_id == product_id))
            await session.merge(
                Product(
                    id=product_id,
                    name=str(raw["title"]),
                    category=normalize_category(source_category) or source_category,
                    sub_category=raw.get("sub_category"),
                    price=float(raw["base_price"]) if raw.get("base_price") is not None else None,
                    brand=raw.get("brand"),
                    image_urls=[str(raw.get("image_path") or "")],
                    product_metadata={
                        "dataset_version": DATASET_VERSION,
                        "source_hash": source_hash,
                        "source_category": source_category,
                        "source_file": raw.get("_source_file"),
                        "skus": raw.get("skus") or [],
                        "rag_knowledge": raw.get("rag_knowledge") or {},
                        "knowledge_package": knowledge_package,
                        "chunking_version": CHUNKING_VERSION,
                        "embedding_model": embedding_model,
                        "indexed_at": indexed_at,
                    },
                )
            )
        for row, embedding in zip(chunk_rows, embeddings, strict=True):
            session.add(
                ProductChunk(
                    id=f"{row.product_id}:{row.chunk_index}",
                    product_id=row.product_id,
                    chunk_text=row.chunk_text,
                    chunk_index=row.chunk_index,
                    embedding=embedding,
                    chunk_metadata={
                        **row.metadata,
                        "dataset_version": DATASET_VERSION,
                        "chunking_version": CHUNKING_VERSION,
                        "embedding_model": embedding_model,
                        "embedded_at": indexed_at,
                    },
                )
            )
        await session.merge(
            SystemMetadata(
                key="dataset_index",
                value_json={
                    "dataset_version": DATASET_VERSION,
                    "dataset_hash": _dataset_hash(products),
                    "chunking_version": CHUNKING_VERSION,
                    "embedding_model": embedding_model,
                    "embedding_dimensions": expected_embedding_dimensions or EXPECTED_EMBEDDING_DIMENSIONS,
                    "product_count": len(products),
                    "chunk_count": len(chunk_rows),
                    "indexed_at": indexed_at,
                },
                updated_at=utc_now(),
            )
        )
        await session.commit()

    return {"products": len(products), "chunks": len(chunk_rows)}


async def reindex_chunk_embeddings(progress: ProgressCallback | None = None) -> dict[str, int]:
    """Rebuild product chunks and embeddings from the raw dataset."""
    result = await seed_products(expected_embedding_dimensions=EXPECTED_EMBEDDING_DIMENSIONS, progress=progress)
    stats = await chunk_embedding_stats()
    return {**result, **stats}


async def seed_products_if_needed(expected_embedding_dimensions: int | None = None) -> dict[str, int | bool]:
    """Seed product/chunk tables only when the current database is empty or stale."""
    await create_db_and_tables()
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        product_count = (await session.exec(select(func.count(Product.id)))).one()

    stats = await chunk_embedding_stats()
    chunk_count = stats["chunks"]
    current_dimensions = stats["embedding_dimensions"]
    dimension_ok = expected_embedding_dimensions is None or current_dimensions == expected_embedding_dimensions
    if product_count > 0 and chunk_count > 0 and dimension_ok:
        return {
            "seeded": False,
            "products": product_count,
            "chunks": chunk_count,
            "embedded_chunks": stats["embedded_chunks"],
            "embedding_dimensions": current_dimensions,
        }

    result = await seed_products(expected_embedding_dimensions=expected_embedding_dimensions)
    stats = await chunk_embedding_stats()
    return {"seeded": True, **result, **stats}


async def chunk_embedding_stats() -> dict[str, int]:
    await create_db_and_tables()
    engine = get_async_engine()
    if is_postgres_engine(engine):
        query = text(
            """
            SELECT
              COUNT(*) AS chunks,
              COUNT(embedding) AS embedded_chunks,
              MIN(vector_dims(embedding)) FILTER (WHERE embedding IS NOT NULL) AS min_dimension,
              MAX(vector_dims(embedding)) FILTER (WHERE embedding IS NOT NULL) AS max_dimension
            FROM product_chunks
            """
        )
    else:
        query = text(
            """
            SELECT
              COUNT(*) AS chunks,
              SUM(CASE
                    WHEN embedding IS NOT NULL AND json_array_length(embedding) > 0 THEN 1
                    ELSE 0
                  END) AS embedded_chunks,
              MIN(CASE
                    WHEN embedding IS NOT NULL AND json_array_length(embedding) > 0
                    THEN json_array_length(embedding)
                  END) AS min_dimension,
              MAX(CASE
                    WHEN embedding IS NOT NULL AND json_array_length(embedding) > 0
                    THEN json_array_length(embedding)
                  END) AS max_dimension
            FROM product_chunks
            """
        )

    async with engine.connect() as conn:
        row = (await conn.execute(query)).mappings().one()
    min_dimension = int(row["min_dimension"] or 0)
    max_dimension = int(row["max_dimension"] or 0)
    return {
        "chunks": int(row["chunks"] or 0),
        "embedded_chunks": int(row["embedded_chunks"] or 0),
        "embedding_dimensions": min_dimension if min_dimension == max_dimension else 0,
    }


def _build_chunk_rows(products: list[dict]) -> list[ChunkSeedRow]:
    rows: list[ChunkSeedRow] = []
    for raw in products:
        product_id = str(raw["product_id"])
        source_hash = _source_hash(raw)
        for chunk in build_product_chunks(raw):
            rows.append(
                ChunkSeedRow(
                    product_id=product_id,
                    chunk_text=chunk.chunk_text,
                    chunk_index=chunk.chunk_index,
                    metadata={**chunk.metadata, "source_hash": source_hash},
                )
            )
    return rows


async def _embed_chunks(
    rows: list[ChunkSeedRow],
    batch_size: int = 10,
    expected_dimensions: int | None = None,
    progress: ProgressCallback | None = None,
) -> list[list[float]]:
    vectors: list[list[float]] = []
    for start in range(0, len(rows), batch_size):
        batch = rows[start : start + batch_size]
        batch_vectors = await embed_texts([row.chunk_text for row in batch])
        if expected_dimensions is not None:
            for vector in batch_vectors:
                if len(vector) != expected_dimensions:
                    raise RuntimeError(
                        f"Embedding dimension mismatch: expected {expected_dimensions}, got {len(vector)}"
                    )
        vectors.extend(batch_vectors)
        if progress is not None:
            progress(len(vectors), len(rows))
    return vectors


def _source_hash(raw: dict) -> str:
    payload = {key: value for key, value in raw.items() if key != "_source_file"}
    encoded = json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def _dataset_hash(products: list[dict]) -> str:
    rows = sorted(
        ({"product_id": str(raw.get("product_id") or ""), "source_hash": _source_hash(raw)} for raw in products),
        key=lambda row: (row["product_id"], row["source_hash"]),
    )
    encoded = json.dumps(rows, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def _embedding_model_name() -> str:
    settings = get_settings()
    profile_name = (settings.task_model_map.get("embedding") or {}).get("primary")
    if not profile_name:
        return "unknown"
    raw = settings.llm_profiles.get("profiles", {}).get(profile_name) or {}
    return str(raw.get("model") or profile_name)


if __name__ == "__main__":
    print(asyncio.run(seed_products()))
