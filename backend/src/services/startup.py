"""Application startup use cases."""

from __future__ import annotations

from src.repos.database import create_db_and_tables
from src.services.product_ingest import EXPECTED_EMBEDDING_DIMENSIONS, seed_products_if_needed


async def initialize_database(auto_seed: bool = False, strict_embeddings: bool = False) -> dict[str, int | bool] | None:
    await create_db_and_tables()
    if not auto_seed:
        return None
    expected_dimensions = EXPECTED_EMBEDDING_DIMENSIONS if strict_embeddings else None
    return await seed_products_if_needed(expected_embedding_dimensions=expected_dimensions)
