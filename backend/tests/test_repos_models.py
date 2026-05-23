from src.repos import models


def test_product_metadata_column_name():
    assert "metadata" in models.Product.__table__.columns
