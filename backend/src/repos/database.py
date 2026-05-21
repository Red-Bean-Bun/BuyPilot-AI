"""Database engine helpers.

The P0 runtime can use in-memory repositories, but these helpers keep the SQL
entrypoint ready for PostgreSQL/pgvector wiring.
"""

from __future__ import annotations

from sqlmodel import Session, SQLModel, create_engine

from src.config.settings import get_settings


def get_engine():
    return create_engine(get_settings().database_url)


def create_db_and_tables() -> None:
    SQLModel.metadata.create_all(get_engine())


def get_session():
    with Session(get_engine()) as session:
        yield session

