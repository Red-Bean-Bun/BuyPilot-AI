"""Seed database tables from the official ecommerce dataset."""

from __future__ import annotations

import asyncio
from dataclasses import dataclass

from sqlalchemy import delete
from sqlmodel import Session, select

from src.repos.database import create_db_and_tables, get_engine
from src.repos.models import Product, ProductChunk
from src.repos.products import build_product_text, list_raw_products
from src.services.chunking import chunk_product_text
from src.services.embedding import embed_texts

EXPECTED_EMBEDDING_DIMENSIONS = 1024


@dataclass(frozen=True)
class ChunkSeedRow:
    product_id: str
    chunk_text: str
    chunk_index: int


def seed_products(expected_embedding_dimensions: int | None = None) -> dict[str, int]:
    create_db_and_tables()
    products = list_raw_products()
    chunk_rows = _build_chunk_rows(products)
    embeddings = _embed_chunks(chunk_rows, expected_dimensions=expected_embedding_dimensions)

    with Session(get_engine()) as session:
        for raw in products:
            product_id = str(raw["product_id"])
            session.execute(delete(ProductChunk).where(ProductChunk.product_id == product_id))
            session.merge(
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
                    chunk_metadata={"source": "ecommerce_agent_dataset"},
                )
            )
        session.commit()

    return {"products": len(products), "chunks": len(chunk_rows)}


def reindex_chunk_embeddings() -> dict[str, int]:
    """Rebuild product chunks and embeddings from the raw dataset."""
    result = seed_products(expected_embedding_dimensions=EXPECTED_EMBEDDING_DIMENSIONS)
    stats = chunk_embedding_stats()
    return {**result, **stats}


def chunk_embedding_stats() -> dict[str, int]:
    create_db_and_tables()
    with Session(get_engine()) as session:
        chunks = session.exec(select(ProductChunk)).all()
    dimensions = {len(chunk.embedding) for chunk in chunks if chunk.embedding}
    return {
        "embedded_chunks": sum(1 for chunk in chunks if chunk.embedding),
        "embedding_dimensions": dimensions.pop() if len(dimensions) == 1 else 0,
    }


def _build_chunk_rows(products: list[dict]) -> list[ChunkSeedRow]:
    rows: list[ChunkSeedRow] = []
    for raw in products:
        product_id = str(raw["product_id"])
        for chunk_text, chunk_index in chunk_product_text(build_product_text(raw)):
            rows.append(ChunkSeedRow(product_id=product_id, chunk_text=chunk_text, chunk_index=chunk_index))
    return rows


def _embed_chunks(
    rows: list[ChunkSeedRow],
    batch_size: int = 10,
    expected_dimensions: int | None = None,
) -> list[list[float]]:
    async def run_batches() -> list[list[float]]:
        vectors: list[list[float]] = []
        for start in range(0, len(rows), batch_size):
            batch = rows[start:start + batch_size]
            batch_vectors = await embed_texts([row.chunk_text for row in batch])
            if expected_dimensions is not None:
                for vector in batch_vectors:
                    if len(vector) != expected_dimensions:
                        raise RuntimeError(
                            f"Embedding dimension mismatch: expected {expected_dimensions}, got {len(vector)}"
                        )
            vectors.extend(batch_vectors)
        return vectors

    return asyncio.run(run_batches())


if __name__ == "__main__":
    print(seed_products())
