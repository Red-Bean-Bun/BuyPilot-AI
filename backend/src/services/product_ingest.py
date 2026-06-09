"""Seed database tables from the official ecommerce dataset."""

from __future__ import annotations

import asyncio
import base64
import hashlib
import json
import logging
import mimetypes
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Callable

from sqlalchemy import delete, text
from sqlmodel import select
from sqlmodel.ext.asyncio.session import AsyncSession

from src.config.domain_terms import normalize_category
from src.config.settings import get_settings
from src.repos.database import create_db_and_tables, get_async_engine
from src.repos.models import EvidenceLink, Product, ProductChunk, ProductImageEmbedding, SystemMetadata, utc_now
from src.repos.products import dataset_dir, list_raw_products
from src.repos.vector import VL_EMBEDDING_DIMENSIONS
from src.services.chunking import build_product_chunks, build_product_knowledge_package
from src.services.embedding import EmbeddingUnavailable, embed_image, embed_texts

logger = logging.getLogger(__name__)

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
            knowledge = raw.get("rag_knowledge") or {}
            chunk_subq = select(ProductChunk.id).where(ProductChunk.product_id == product_id)
            await session.exec(delete(EvidenceLink).where(EvidenceLink.chunk_id.in_(chunk_subq)))
            await session.exec(delete(ProductChunk).where(ProductChunk.product_id == product_id))
            existing = (await session.exec(select(Product).where(Product.id == product_id))).one_or_none()
            existing_highlights = (
                existing.product_metadata.get("highlights") if existing and existing.product_metadata else None
            )
            product_metadata = {
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
            }
            if existing_highlights is not None:
                product_metadata["highlights"] = existing_highlights
            await session.merge(
                Product(
                    id=product_id,
                    name=str(raw["title"]),
                    category=normalize_category(source_category) or source_category,
                    sub_category=raw.get("sub_category"),
                    price=float(raw["base_price"]) if raw.get("base_price") is not None else None,
                    brand=raw.get("brand"),
                    image_urls=[str(raw.get("image_path") or "")],
                    marketing_description=knowledge.get("marketing_description"),
                    official_faq=knowledge.get("official_faq") or [],
                    user_reviews=knowledge.get("user_reviews") or [],
                    product_metadata=product_metadata,
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
    """Seed product/chunk tables when the dataset has changed since the last seed.

    Uses hash-based idempotency: compares the current dataset hash against the
    hash stored in SystemMetadata.  Falls back to count-based check when no
    stored hash exists (first run after upgrade).
    """
    await create_db_and_tables()
    products = list_raw_products()
    current_hash = _dataset_hash(products)

    # Read stored hash from SystemMetadata
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        stored = (
            await session.exec(select(SystemMetadata).where(SystemMetadata.key == "dataset_index"))
        ).one_or_none()
    stored_hash = (stored.value_json.get("dataset_hash") if stored else None)

    stats = await chunk_embedding_stats()
    chunk_count = stats["chunks"]
    current_dimensions = stats["embedding_dimensions"]
    dimension_ok = expected_embedding_dimensions is None or current_dimensions == expected_embedding_dimensions

    # Hash-based skip: dataset unchanged AND dimensions match
    if stored_hash == current_hash and chunk_count > 0 and dimension_ok:
        return {
            "seeded": False,
            "products": len(products),
            "chunks": chunk_count,
            "embedded_chunks": stats["embedded_chunks"],
            "embedding_dimensions": current_dimensions,
            "reason": "Dataset unchanged",
        }

    # Legacy fallback: no stored hash but DB is populated and dims match
    if stored_hash is None and chunk_count > 0 and dimension_ok:
        # First run after upgrade — seed to write the hash
        pass

    result = await seed_products(expected_embedding_dimensions=expected_embedding_dimensions)
    stats = await chunk_embedding_stats()
    return {"seeded": True, **result, **stats}


async def chunk_embedding_stats() -> dict[str, int]:
    await create_db_and_tables()
    engine = get_async_engine()
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


IMAGE_EMBEDDING_MODEL = "qwen3-vl-embedding"
IMAGE_BATCH_SIZE = 5


def _image_to_data_url(image_path: Path) -> str:
    mime_type = mimetypes.guess_type(image_path.name)[0] or "image/jpeg"
    encoded = base64.b64encode(image_path.read_bytes()).decode("ascii")
    return f"data:{mime_type};base64,{encoded}"


def _image_hash(image_path: Path) -> str:
    return hashlib.sha256(image_path.read_bytes()).hexdigest()


async def _load_existing_image_hashes() -> dict[str, str]:
    """Return {product_id: image_hash} for existing embeddings with matching model/dimensions."""
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        rows = (
            await session.exec(
                select(ProductImageEmbedding.product_id, ProductImageEmbedding.image_hash).where(
                    ProductImageEmbedding.embedding_model == IMAGE_EMBEDDING_MODEL,
                    ProductImageEmbedding.embedding_dimensions == VL_EMBEDDING_DIMENSIONS,
                )
            )
        ).all()
    return {pid: h for pid, h in rows}


async def seed_image_embeddings_if_needed() -> dict[str, int | bool]:
    """Seed image embeddings with content-hash idempotency.

    Compares SHA-256 of each product image against stored hashes.
    Only re-embeds images whose content has changed.
    """
    raw_products = list_raw_products()
    root = dataset_dir()
    existing = await _load_existing_image_hashes()

    embedded = 0
    skipped = 0
    errors = 0
    total = len(raw_products)

    for i in range(0, total, IMAGE_BATCH_SIZE):
        batch = raw_products[i : i + IMAGE_BATCH_SIZE]
        for raw in batch:
            product_id = raw.get("product_id", "")
            image_rel = raw.get("image_path", "")
            if not product_id or not image_rel:
                skipped += 1
                continue

            image_path = root / image_rel
            if not image_path.exists():
                logger.warning("Image not found: %s", image_path)
                skipped += 1
                continue

            current_hash = _image_hash(image_path)
            if existing.get(product_id) == current_hash:
                skipped += 1
                continue

            try:
                data_url = _image_to_data_url(image_path)
                vector = await embed_image(data_url)
            except (EmbeddingUnavailable, Exception) as exc:
                logger.error("embed_image failed for %s: %s", product_id, exc)
                errors += 1
                continue

            if len(vector) != VL_EMBEDDING_DIMENSIONS:
                logger.error(
                    "Dimension mismatch for %s: got %d, expected %d",
                    product_id, len(vector), VL_EMBEDDING_DIMENSIONS,
                )
                errors += 1
                continue

            async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
                await session.exec(
                    delete(ProductImageEmbedding).where(
                        ProductImageEmbedding.product_id == product_id,
                        ProductImageEmbedding.embedding_model == IMAGE_EMBEDDING_MODEL,
                        ProductImageEmbedding.embedding_dimensions == VL_EMBEDDING_DIMENSIONS,
                    )
                )
                session.add(
                    ProductImageEmbedding(
                        product_id=product_id,
                        image_path=str(image_rel),
                        embedding=vector,
                        embedding_model=IMAGE_EMBEDDING_MODEL,
                        embedding_dimensions=VL_EMBEDDING_DIMENSIONS,
                        image_hash=current_hash,
                        indexed_at=datetime.now(timezone.utc).isoformat(),
                    )
                )
                await session.commit()
            embedded += 1

    logger.info(
        "Image seed complete: total=%d embedded=%d skipped=%d errors=%d",
        total, embedded, skipped, errors,
    )
    return {
        "total": total,
        "embedded": embedded,
        "skipped": skipped,
        "errors": errors,
        "embedding_model": IMAGE_EMBEDDING_MODEL,
        "embedding_dimensions": VL_EMBEDDING_DIMENSIONS,
    }


if __name__ == "__main__":
    print(asyncio.run(seed_products()))
