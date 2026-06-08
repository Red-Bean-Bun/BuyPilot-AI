from sqlalchemy.dialects import postgresql
from sqlalchemy.schema import CreateTable

from src.repos.models import ProductChunk
from src.repos.vector import coerce_vector, vector_to_pg_literal


def test_product_chunk_embedding_compiles_to_pgvector_for_postgres():
    ddl = str(CreateTable(ProductChunk.__table__).compile(dialect=postgresql.dialect()))

    assert "VECTOR(1024)" in ddl


def test_vector_literal_roundtrip():
    literal = vector_to_pg_literal([0.125, 1.0, -2.5])

    assert literal == "[0.125,1,-2.5]"
    assert coerce_vector(literal) == [0.125, 1.0, -2.5]
