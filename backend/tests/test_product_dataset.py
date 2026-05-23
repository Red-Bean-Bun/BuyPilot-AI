from sqlmodel import Session, select

from src.repos.ingest import seed_products, seed_products_if_needed
from src.repos.models import Product, ProductChunk
from src.repos.products import get_raw_product, list_products, list_raw_products


def test_dataset_is_runtime_product_source():
    raw_products = list_raw_products()
    products = list_products()

    assert len(raw_products) == 100
    assert len(products) == 100
    assert products[0].product_id == raw_products[0]["product_id"]
    assert get_raw_product(products[0].product_id) is not None


def test_seed_products_writes_dataset_to_database(monkeypatch, tmp_path):
    database_url = f"sqlite:///{tmp_path / 'seed.db'}"
    monkeypatch.setenv("DATABASE_URL", database_url)

    from src.config import settings as settings_module

    settings_module._settings = None

    result = seed_products()

    engine = settings_module.get_settings()
    assert result["products"] == 100
    assert result["chunks"] >= 100

    from src.repos.database import get_engine

    with Session(get_engine()) as session:
        products = session.exec(select(Product)).all()
        chunks = session.exec(select(ProductChunk)).all()
        product_count = len(products)
        chunk_count = len(chunks)

    assert engine.database_url == database_url
    assert product_count == 100
    assert chunk_count == result["chunks"]
    assert chunks[0].embedding
    assert products[0].product_metadata["knowledge_package"]["basic"]["product_id"] == products[0].id
    chunk_types = {chunk.chunk_metadata.get("chunk_type") for chunk in chunks}
    assert {"profile", "marketing", "faq", "positive_review", "negative_review", "warning", "compare"} <= chunk_types
    assert all("retrieval_role" in chunk.chunk_metadata for chunk in chunks)
    negative_chunks = [chunk for chunk in chunks if chunk.chunk_metadata.get("chunk_type") == "negative_review"]
    assert negative_chunks
    assert all(chunk.chunk_metadata["retrieval_role"] == "risk" for chunk in negative_chunks)


def test_seed_products_if_needed_skips_non_empty_database(monkeypatch, tmp_path):
    database_url = f"sqlite:///{tmp_path / 'seed_if_needed.db'}"
    monkeypatch.setenv("DATABASE_URL", database_url)

    from src.config import settings as settings_module

    settings_module._settings = None

    first = seed_products_if_needed()
    second = seed_products_if_needed()

    assert first["seeded"] is True
    assert first["products"] == 100
    assert second["seeded"] is False
    assert second["products"] == 100
    assert second["chunks"] == first["chunks"]
