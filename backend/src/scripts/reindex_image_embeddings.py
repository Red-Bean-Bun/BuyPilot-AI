"""Build or rebuild product image embeddings for visual similarity search.

Run from backend/:
    .venv/bin/python -m src.scripts.reindex_image_embeddings

Requires:
    - BAILIAN_API_KEY configured
    - PostgreSQL + pgvector (not SQLite)
    - Product data already seeded (products table populated)
"""

from __future__ import annotations

import asyncio
import base64
import hashlib
import json
import mimetypes
import os
import sys
from datetime import datetime, timezone
from pathlib import Path

from sqlalchemy import delete, select
from sqlmodel.ext.asyncio.session import AsyncSession

from src.repos.database import create_db_and_tables, get_async_engine
from src.repos.models import ProductImageEmbedding
from src.repos.products import dataset_dir, list_raw_products
from src.repos.vector import VL_EMBEDDING_DIMENSIONS
from src.services.embedding import EmbeddingUnavailable, embed_image


BATCH_SIZE = 5
EMBEDDING_MODEL = "qwen3-vl-embedding"


def _check_prerequisites() -> None:
    api_key = os.getenv("BAILIAN_API_KEY")
    if not api_key or api_key == "test-key":
        print(
            json.dumps(
                {"check": "reindex_image_embeddings", "ok": False, "error": "BAILIAN_API_KEY not configured or is mock value."},
                ensure_ascii=False,
            )
        )
        sys.exit(1)


def _image_to_data_url(image_path: Path) -> str:
    if not image_path.exists():
        raise FileNotFoundError(f"Image not found: {image_path}")
    mime_type = mimetypes.guess_type(image_path.name)[0] or "image/jpeg"
    encoded = base64.b64encode(image_path.read_bytes()).decode("ascii")
    return f"data:{mime_type};base64,{encoded}"


def _image_hash(image_path: Path) -> str:
    return hashlib.sha256(image_path.read_bytes()).hexdigest()


async def _load_existing_hashes() -> dict[str, str]:
    """Return {product_id: image_hash} for existing embeddings with matching model/dimensions."""
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        rows = (
            await session.exec(
                select(ProductImageEmbedding.product_id, ProductImageEmbedding.image_hash).where(
                    ProductImageEmbedding.embedding_model == EMBEDDING_MODEL,
                    ProductImageEmbedding.embedding_dimensions == VL_EMBEDDING_DIMENSIONS,
                )
            )
        ).all()
    return {pid: h for pid, h in rows}


async def reindex_image_embeddings() -> dict[str, Any]:
    _check_prerequisites()
    await create_db_and_tables()

    raw_products = list_raw_products()
    root = dataset_dir()
    existing = await _load_existing_hashes()

    embedded = 0
    skipped = 0
    errors = 0
    total = len(raw_products)

    for i in range(0, total, BATCH_SIZE):
        batch = raw_products[i : i + BATCH_SIZE]
        for raw in batch:
            product_id = raw.get("product_id", "")
            image_rel = raw.get("image_path", "")
            if not product_id or not image_rel:
                skipped += 1
                continue

            image_path = root / image_rel
            if not image_path.exists():
                print(json.dumps({"warning": f"Image not found: {image_path}"}, ensure_ascii=False), flush=True)
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
                print(json.dumps({"error": f"embed_image failed for {product_id}: {exc}"}, ensure_ascii=False), flush=True)
                errors += 1
                continue

            if len(vector) != VL_EMBEDDING_DIMENSIONS:
                print(
                    json.dumps(
                        {"error": f"Dimension mismatch for {product_id}: got {len(vector)}, expected {VL_EMBEDDING_DIMENSIONS}"},
                        ensure_ascii=False,
                    ),
                    flush=True,
                )
                errors += 1
                continue

            async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
                # Delete old rows for this product+model+dimensions
                await session.exec(
                    delete(ProductImageEmbedding).where(
                        ProductImageEmbedding.product_id == product_id,
                        ProductImageEmbedding.embedding_model == EMBEDDING_MODEL,
                        ProductImageEmbedding.embedding_dimensions == VL_EMBEDDING_DIMENSIONS,
                    )
                )
                row = ProductImageEmbedding(
                    product_id=product_id,
                    image_path=str(image_rel),
                    embedding=vector,
                    embedding_model=EMBEDDING_MODEL,
                    embedding_dimensions=VL_EMBEDDING_DIMENSIONS,
                    image_hash=current_hash,
                    indexed_at=datetime.now(timezone.utc).isoformat(),
                )
                session.add(row)
                await session.commit()
            embedded += 1

        done = min(i + BATCH_SIZE, total)
        print(
            json.dumps(
                {"check": "reindex_image_embeddings", "phase": "embedding", "done": done, "total": total, "embedded": embedded, "skipped": skipped},
                ensure_ascii=False,
            ),
            flush=True,
        )

    return {
        "total": total,
        "embedded": embedded,
        "skipped": skipped,
        "errors": errors,
        "embedding_model": EMBEDDING_MODEL,
        "embedding_dimensions": VL_EMBEDDING_DIMENSIONS,
    }


def main() -> None:
    stats = asyncio.run(reindex_image_embeddings())
    ok = stats["errors"] == 0
    print(json.dumps({"check": "reindex_image_embeddings", "ok": ok, **stats}, ensure_ascii=False))
    if not ok:
        sys.exit(1)


if __name__ == "__main__":
    main()
