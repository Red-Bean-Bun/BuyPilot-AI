"""Database engine helpers.

The P0 runtime can use in-memory repositories, but these helpers keep the SQL
entrypoint ready for PostgreSQL/pgvector wiring.
"""

from __future__ import annotations

from sqlalchemy import text
from sqlalchemy.engine import Engine, make_url
from sqlmodel import Session, SQLModel, create_engine

from src.config.settings import get_settings


def get_engine():
    database_url = get_settings().database_url
    cached = getattr(get_engine, "_cache", None)
    if cached is None or cached[0] != database_url:
        connect_args = {"check_same_thread": False} if database_url.startswith("sqlite") else {}
        cached = (database_url, create_engine(database_url, connect_args=connect_args))
        setattr(get_engine, "_cache", cached)
    return cached[1]


def create_db_and_tables() -> None:
    engine = get_engine()
    if is_postgres_engine(engine):
        ensure_pgvector_extension(engine)
    SQLModel.metadata.create_all(engine)
    if is_postgres_engine(engine):
        ensure_pgvector_indexes(engine)


def is_postgres_database_url(database_url: str | None = None) -> bool:
    url = make_url(database_url or get_settings().database_url)
    return url.get_backend_name() == "postgresql"


def is_postgres_engine(engine: Engine) -> bool:
    return engine.dialect.name == "postgresql"


def ensure_pgvector_extension(engine: Engine) -> None:
    with engine.begin() as conn:
        conn.execute(text("CREATE EXTENSION IF NOT EXISTS vector"))


def drop_stale_pgvector_tables(engine: Engine | None = None) -> bool:
    """Drop derived chunk/evidence tables when an old JSON embedding column exists."""

    engine = engine or get_engine()
    if not is_postgres_engine(engine):
        return False

    with engine.begin() as conn:
        row = (
            conn.execute(
                text(
                    """
                SELECT udt_name
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name = 'product_chunks'
                  AND column_name = 'embedding'
                """
                )
            )
            .mappings()
            .first()
        )
        if row is not None and row["udt_name"] != "vector":
            conn.execute(text("DROP TABLE IF EXISTS evidence_links"))
            conn.execute(text("DROP TABLE IF EXISTS product_chunks"))
            return True
    return False


def ensure_pgvector_indexes(engine: Engine) -> None:
    with engine.begin() as conn:
        conn.execute(
            text(
                "CREATE INDEX IF NOT EXISTS idx_product_chunks_embedding_hnsw "
                "ON product_chunks USING hnsw (embedding vector_cosine_ops)"
            )
        )


def get_session():
    with Session(get_engine()) as session:
        yield session


def migrate_eval_tables() -> None:
    """Drop and recreate eval_runs and eval_samples tables.

    Warning: This drops ALL existing eval data. Only call during schema changes
    when tables are empty or data is expendable.
    """
    engine = get_engine()
    with engine.begin() as conn:
        conn.execute(text("DROP TABLE IF EXISTS eval_runs"))
        conn.execute(text("DROP TABLE IF EXISTS eval_samples"))
    SQLModel.metadata.create_all(engine)


def migrate_eval_runs_table() -> None:
    """Drop and recreate only the eval_runs table (preserves eval_samples)."""
    engine = get_engine()
    with engine.begin() as conn:
        conn.execute(text("DROP TABLE IF EXISTS eval_runs"))
    SQLModel.metadata.create_all(engine)
