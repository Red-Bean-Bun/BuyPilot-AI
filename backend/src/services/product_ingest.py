"""Seed database tables from the official ecommerce dataset."""

from __future__ import annotations

import asyncio
from dataclasses import dataclass
from typing import Callable

from sqlalchemy import delete, func
from sqlmodel import select
from sqlmodel.ext.asyncio.session import AsyncSession

from src.repos.database import create_db_and_tables, get_async_engine
from src.repos.models import Product, ProductChunk
from src.repos.products import list_raw_products
from src.services.chunking import build_product_chunks, build_product_knowledge_package
from src.services.embedding import embed_texts

EXPECTED_EMBEDDING_DIMENSIONS = 1024


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
    chunk_rows = _build_chunk_rows(products)
    embeddings = await _embed_chunks(chunk_rows, expected_dimensions=expected_embedding_dimensions, progress=progress)

    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        for raw in products:
            product_id = str(raw["product_id"])
            knowledge_package = build_product_knowledge_package(raw)
            await session.exec(delete(ProductChunk).where(ProductChunk.product_id == product_id))
            await session.merge(
                Product(
                    id=product_id,
                    name=str(raw["title"]),
                    category=str(raw.get("category") or ""),
                    sub_category=raw.get("sub_category"),
                    price=float(raw["base_price"]) if raw.get("base_price") is not None else None,
                    brand=raw.get("brand"),
                    image_urls=[str(raw.get("image_path") or "")],
                    product_metadata={
                        "source_file": raw.get("_source_file"),
                        "skus": raw.get("skus") or [],
                        "rag_knowledge": raw.get("rag_knowledge") or {},
                        "knowledge_package": knowledge_package,
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
                    chunk_metadata=row.metadata,
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
        chunk_count = (await session.exec(select(func.count(ProductChunk.id)))).one()
        chunks = (await session.exec(select(ProductChunk.embedding))).all()

    dimensions = {len(embedding) for embedding in chunks if embedding}
    current_dimensions = dimensions.pop() if len(dimensions) == 1 else 0
    dimension_ok = expected_embedding_dimensions is None or current_dimensions == expected_embedding_dimensions
    if product_count > 0 and chunk_count > 0 and dimension_ok:
        return {
            "seeded": False,
            "products": product_count,
            "chunks": chunk_count,
            "embedded_chunks": sum(1 for embedding in chunks if embedding),
            "embedding_dimensions": current_dimensions,
        }

    result = await seed_products(expected_embedding_dimensions=expected_embedding_dimensions)
    stats = await chunk_embedding_stats()
    return {"seeded": True, **result, **stats}


async def chunk_embedding_stats() -> dict[str, int]:
    await create_db_and_tables()
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        chunks = (await session.exec(select(ProductChunk))).all()
    dimensions = {len(chunk.embedding) for chunk in chunks if chunk.embedding}
    return {
        "embedded_chunks": sum(1 for chunk in chunks if chunk.embedding),
        "embedding_dimensions": dimensions.pop() if len(dimensions) == 1 else 0,
    }


def _build_chunk_rows(products: list[dict]) -> list[ChunkSeedRow]:
    rows: list[ChunkSeedRow] = []
    for raw in products:
        product_id = str(raw["product_id"])
        for chunk in build_product_chunks(raw):
            rows.append(
                ChunkSeedRow(
                    product_id=product_id,
                    chunk_text=chunk.chunk_text,
                    chunk_index=chunk.chunk_index,
                    metadata=chunk.metadata,
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


if __name__ == "__main__":
    print(asyncio.run(seed_products()))
