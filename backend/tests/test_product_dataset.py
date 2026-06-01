import pytest
from sqlmodel import select
from sqlmodel.ext.asyncio.session import AsyncSession

from src.repos.models import Product, ProductChunk, SystemMetadata
from src.repos.products import PRODUCT_ASSET_URL_PREFIX, get_raw_product, list_products, list_raw_products
from src.services.product_ingest import chunk_embedding_stats, seed_products, seed_products_if_needed


def test_dataset_is_runtime_product_source():
    raw_products = list_raw_products()
    products = list_products()
    food_product = next(product for product in products if product.product_id == "p_food_001")

    assert len(raw_products) == 100
    assert len(products) == 100
    assert products[0].product_id == raw_products[0]["product_id"]
    assert get_raw_product(products[0].product_id) is not None
    assert get_raw_product("p_food_001")["category"] == "食品生活"
    assert food_product.category == "食品生活"


def test_product_image_urls_are_frontend_loadable():
    product = list_products()[0]

    assert product.image_url
    assert product.image_url.startswith(f"{PRODUCT_ASSET_URL_PREFIX}/")
    assert "/images/" in product.image_url


@pytest.mark.asyncio
async def test_seed_products_writes_dataset_to_database(monkeypatch, tmp_path):
    database_url = f"sqlite:///{tmp_path / 'seed.db'}"
    monkeypatch.setenv("DATABASE_URL", database_url)

    from src.config import settings as settings_module

    settings_module._settings = None

    result = await seed_products()

    engine = settings_module.get_settings()
    assert result["products"] == 100
    assert result["chunks"] >= 100

    from src.repos.database import get_async_engine

    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        products = (await session.exec(select(Product))).all()
        chunks = (await session.exec(select(ProductChunk))).all()
        metadata = await session.get(SystemMetadata, "dataset_index")
        product_count = len(products)
        chunk_count = len(chunks)

    assert engine.database_url == database_url
    assert product_count == 100
    assert chunk_count == result["chunks"]
    assert chunks[0].embedding
    assert metadata is not None
    assert metadata.value_json["dataset_version"] == "ecommerce_agent_dataset:v1"
    assert metadata.value_json["chunking_version"] == "semantic_v1"
    assert metadata.value_json["embedding_model"] == "text-embedding-v3"
    assert products[0].product_metadata["knowledge_package"]["basic"]["product_id"] == products[0].id
    assert products[0].product_metadata["source_hash"]
    assert products[0].product_metadata["chunking_version"] == "semantic_v1"
    food_product = next(product for product in products if product.id == "p_food_001")
    assert food_product.category == "食品生活"
    assert food_product.product_metadata["source_category"] == "食品生活"
    assert food_product.product_metadata["knowledge_package"]["basic"]["source_category"] == "食品生活"
    chunk_types = {chunk.chunk_metadata.get("chunk_type") for chunk in chunks}
    assert {"profile", "marketing", "faq", "positive_review", "negative_review", "warning", "compare"} <= chunk_types
    assert all("retrieval_role" in chunk.chunk_metadata for chunk in chunks)
    negative_chunks = [chunk for chunk in chunks if chunk.chunk_metadata.get("chunk_type") == "negative_review"]
    assert negative_chunks
    assert all(chunk.chunk_metadata["retrieval_role"] == "risk" for chunk in negative_chunks)

    stats = await chunk_embedding_stats()
    assert stats["chunks"] == result["chunks"]
    assert stats["embedded_chunks"] == result["chunks"]
    assert stats["embedding_dimensions"] == 1024


@pytest.mark.asyncio
async def test_seed_products_if_needed_skips_non_empty_database(monkeypatch, tmp_path):
    database_url = f"sqlite:///{tmp_path / 'seed_if_needed.db'}"
    monkeypatch.setenv("DATABASE_URL", database_url)

    from src.config import settings as settings_module

    settings_module._settings = None

    first = await seed_products_if_needed()
    second = await seed_products_if_needed()

    assert first["seeded"] is True
    assert first["products"] == 100
    assert second["seeded"] is False
    assert second["products"] == 100
    assert second["chunks"] == first["chunks"]
