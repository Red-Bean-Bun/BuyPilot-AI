from sqlmodel import SQLModel

from src.repos import models


def test_core_tables_registered():
    table_names = set(SQLModel.metadata.tables)
    assert {
        "products",
        "product_chunks",
        "conversations",
        "feedbacks",
        "cart_items",
        "eval_runs",
        "retrieval_traces",
        "evidence_links",
        "eval_samples",
    }.issubset(table_names)


def test_product_metadata_column_name():
    assert "metadata" in models.Product.__table__.columns

