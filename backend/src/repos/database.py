"""Database engine helpers.

The P0 runtime can use in-memory repositories, but these helpers keep the SQL
entrypoint ready for PostgreSQL/pgvector wiring.
"""

from __future__ import annotations

import threading

from sqlalchemy import inspect, text
from sqlalchemy.engine import Engine, make_url
from sqlmodel import Session, SQLModel, create_engine

from src.config.settings import get_settings

_CREATE_TABLES_LOCK = threading.Lock()
_CREATED_DATABASE_URLS: set[str] = set()
_ENGINE_CACHE: tuple[str, Engine] | None = None


def get_engine() -> Engine:
    global _ENGINE_CACHE
    database_url = get_settings().database_url
    cached = _ENGINE_CACHE
    if cached is None or cached[0] != database_url:
        connect_args = {"check_same_thread": False} if database_url.startswith("sqlite") else {}
        cached = (database_url, create_engine(database_url, connect_args=connect_args))
        _ENGINE_CACHE = cached
    return cached[1]


def create_db_and_tables() -> None:
    database_url = get_settings().database_url
    if database_url in _CREATED_DATABASE_URLS:
        return
    with _CREATE_TABLES_LOCK:
        if database_url in _CREATED_DATABASE_URLS:
            return
        engine = get_engine()
        if is_postgres_engine(engine):
            ensure_pgvector_extension(engine)
        SQLModel.metadata.create_all(engine)
        if is_postgres_engine(engine):
            ensure_pgvector_indexes(engine)
        _CREATED_DATABASE_URLS.add(database_url)


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


def ensure_eval_schema() -> None:
    """Create eval tables and add missing nullable columns from older dev DBs."""

    engine = get_engine()
    SQLModel.metadata.create_all(engine)
    for table_name in ("eval_runs", "eval_samples"):
        _add_missing_columns(engine, table_name)


def _add_missing_columns(engine: Engine, table_name: str) -> None:
    table = SQLModel.metadata.tables[table_name]
    inspector = inspect(engine)
    existing = {column["name"] for column in inspector.get_columns(table_name)}
    preparer = engine.dialect.identifier_preparer
    missing = [column for column in table.columns if column.name not in existing and not column.primary_key]
    if not missing:
        return

    with engine.begin() as conn:
        for column in missing:
            column_type = column.type.compile(dialect=engine.dialect)
            conn.execute(
                text(f"ALTER TABLE {preparer.quote(table_name)} ADD COLUMN {preparer.quote(column.name)} {column_type}")
            )
