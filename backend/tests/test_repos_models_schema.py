"""SQLModel table schema validation — column types, constraints, indexes.

Audit gap: test_repos_models.py had only 1 trivial assertion (column name exists).
These tests verify column types, index definitions, unique constraints, and
foreign key relationships — catching schema drift at import time.
"""

from __future__ import annotations


from src.repos.models import (
    CartItem,
    EvidenceLink,
    Feedback,
    Product,
    ProductChunk,
    ProductImageEmbedding,
)
from src.repos.vector import EMBEDDING_DIMENSIONS, VL_EMBEDDING_DIMENSIONS, EmbeddingType


class TestProductTableSchema:
    def test_product_metadata_column_is_json(self):
        table = Product.__table__
        col = table.c["metadata"]
        assert col.type.__class__.__name__ in ("JSON", "JSONB")

    def test_product_image_urls_column_is_json(self):
        col = Product.__table__.c["image_urls"]
        assert col.type.__class__.__name__ in ("JSON", "JSONB")

    def test_product_has_composite_index_on_category_price(self):
        table = Product.__table__
        index_names = [idx.name for idx in table.indexes]
        assert "idx_products_category_price" in index_names

    def test_product_has_brand_index(self):
        table = Product.__table__
        index_names = [idx.name for idx in table.indexes]
        assert "idx_products_brand" in index_names


class TestProductChunkTableSchema:
    def test_embedding_column_type_dimension(self):
        col = ProductChunk.__table__.c["embedding"]
        assert isinstance(col.type, EmbeddingType)
        assert col.type.dimensions == EMBEDDING_DIMENSIONS

    def test_embedding_dimensions_is_1024(self):
        assert EMBEDDING_DIMENSIONS == 1024

    def test_product_id_has_foreign_key_to_products(self):
        col = ProductChunk.__table__.c["product_id"]
        fk_targets = {str(fk.target_fullname) for fk in col.foreign_keys}
        assert "products.id" in fk_targets


class TestProductImageEmbeddingSchema:
    def test_vl_embedding_dimensions(self):
        col = ProductImageEmbedding.__table__.c["embedding"]
        assert isinstance(col.type, EmbeddingType)
        assert col.type.dimensions == VL_EMBEDDING_DIMENSIONS

    def test_unique_constraint_on_product_image_model(self):
        table = ProductImageEmbedding.__table__
        unique_constraints = [uc for uc in table.constraints if uc.__class__.__name__ == "UniqueConstraint"]
        assert len(unique_constraints) >= 1
        uc_columns = [frozenset(uc.columns.keys()) for uc in unique_constraints]
        expected = frozenset({"product_id", "image_path", "embedding_model", "embedding_dimensions"})
        assert expected in uc_columns


class TestCartItemTableSchema:
    def test_unique_constraint_session_product(self):
        table = CartItem.__table__
        unique_constraints = [uc for uc in table.constraints if uc.__class__.__name__ == "UniqueConstraint"]
        assert len(unique_constraints) >= 1
        uc_columns = [frozenset(uc.columns.keys()) for uc in unique_constraints]
        assert frozenset({"session_id", "product_id"}) in uc_columns

    def test_product_id_foreign_key(self):
        col = CartItem.__table__.c["product_id"]
        fk_targets = {str(fk.target_fullname) for fk in col.foreign_keys}
        assert "products.id" in fk_targets


class TestFeedbackTableSchema:
    def test_feedback_has_session_id_column(self):
        assert "session_id" in Feedback.__table__.c

    def test_feedback_has_action_column(self):
        assert "action" in Feedback.__table__.c

    def test_feedback_has_created_at_column(self):
        assert "created_at" in Feedback.__table__.c


class TestEvidenceLinkSchema:
    def test_product_id_foreign_key(self):
        col = EvidenceLink.__table__.c["product_id"]
        fk_targets = {str(fk.target_fullname) for fk in col.foreign_keys}
        assert "products.id" in fk_targets

    def test_chunk_id_foreign_key(self):
        col = EvidenceLink.__table__.c["chunk_id"]
        fk_targets = {str(fk.target_fullname) for fk in col.foreign_keys}
        assert "product_chunks.id" in fk_targets

    def test_has_product_id_index(self):
        table = EvidenceLink.__table__
        index_names = [idx.name for idx in table.indexes]
        assert "idx_evidence_links_product_id" in index_names
