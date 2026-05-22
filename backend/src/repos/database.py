"""Database engine helpers.

The P0 runtime can use in-memory repositories, but these helpers keep the SQL
entrypoint ready for PostgreSQL/pgvector wiring.
"""

from __future__ import annotations

from sqlalchemy import text
from sqlmodel import Session, SQLModel, create_engine

from src.config.settings import get_settings


def get_engine():
    return create_engine(get_settings().database_url)


def create_db_and_tables() -> None:
    SQLModel.metadata.create_all(get_engine())


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


