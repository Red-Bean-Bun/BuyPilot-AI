"""Async database engine helpers."""

from __future__ import annotations

import threading

from sqlalchemy import inspect, text
from sqlalchemy.engine import Connection
from sqlalchemy.ext.asyncio import AsyncEngine, create_async_engine
from sqlmodel import SQLModel
from sqlmodel.ext.asyncio.session import AsyncSession

from src.config.settings import get_settings
from src.repos import models as _models  # noqa: F401  # register SQLModel tables

_CREATE_TABLES_LOCK = threading.Lock()
_CREATED_DATABASE_URLS: set[str] = set()
_ASYNC_ENGINE_CACHE: tuple[str, AsyncEngine] | None = None


def get_async_engine() -> AsyncEngine:
    global _ASYNC_ENGINE_CACHE
    database_url = get_settings().database_url
    cached = _ASYNC_ENGINE_CACHE
    if cached is None or cached[0] != database_url:
        cached = (database_url, create_async_engine(database_url))
        _ASYNC_ENGINE_CACHE = cached
    return cached[1]


async def dispose_async_engine() -> None:
    global _ASYNC_ENGINE_CACHE
    cached = _ASYNC_ENGINE_CACHE
    _ASYNC_ENGINE_CACHE = None
    if cached is not None:
        await cached[1].dispose()


async def create_db_and_tables() -> None:
    database_url = get_settings().database_url
    if database_url in _CREATED_DATABASE_URLS:
        return
    with _CREATE_TABLES_LOCK:
        if database_url in _CREATED_DATABASE_URLS:
            return
    engine = get_async_engine()
    await ensure_pgvector_extension(engine)
    async with engine.begin() as conn:
        await conn.run_sync(SQLModel.metadata.create_all)
        await conn.run_sync(_ensure_model_columns_sync)
        await ensure_runtime_indexes(conn)
    await ensure_pgvector_indexes(engine)
    with _CREATE_TABLES_LOCK:
        _CREATED_DATABASE_URLS.add(database_url)



async def ensure_pgvector_extension(engine: AsyncEngine) -> None:
    async with engine.begin() as conn:
        await conn.execute(text("CREATE EXTENSION IF NOT EXISTS vector"))


async def drop_stale_pgvector_tables(engine: AsyncEngine | None = None) -> bool:
    """Drop derived chunk/evidence tables when an old JSON embedding column exists."""

    engine = engine or get_async_engine()

    async with engine.begin() as conn:
        row = (
            (
                await conn.execute(
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
            )
            .mappings()
            .first()
        )
        if row is not None and row["udt_name"] != "vector":
            await conn.execute(text("DROP TABLE IF EXISTS evidence_links"))
            await conn.execute(text("DROP TABLE IF EXISTS product_chunks"))
            return True
    return False


async def ensure_pgvector_indexes(engine: AsyncEngine) -> None:
    async with engine.begin() as conn:
        await conn.execute(
            text(
                "CREATE INDEX IF NOT EXISTS idx_product_chunks_embedding_hnsw "
                "ON product_chunks USING hnsw (embedding vector_cosine_ops)"
            )
        )
        await conn.execute(
            text(
                "CREATE INDEX IF NOT EXISTS idx_product_image_embeddings_hnsw "
                "ON product_image_embeddings USING hnsw (embedding vector_cosine_ops)"
            )
        )


async def ensure_runtime_indexes(conn) -> None:
    """Create non-destructive indexes used by retrieval, cart, trace, and evidence queries."""

    await _merge_duplicate_cart_items(conn)

    statements = (
        "CREATE INDEX IF NOT EXISTS idx_products_category_price ON products (category, price)",
        "CREATE INDEX IF NOT EXISTS idx_products_category_sub_category ON products (category, sub_category)",
        "CREATE INDEX IF NOT EXISTS idx_products_brand ON products (brand)",
        "CREATE UNIQUE INDEX IF NOT EXISTS uq_cart_items_session_product ON cart_items (session_id, product_id)",
        "CREATE INDEX IF NOT EXISTS idx_retrieval_traces_conversation_id ON retrieval_traces (conversation_id)",
        "CREATE INDEX IF NOT EXISTS idx_retrieval_traces_created_at ON retrieval_traces (created_at)",
        "CREATE INDEX IF NOT EXISTS idx_evidence_links_conversation_id ON evidence_links (conversation_id)",
        "CREATE INDEX IF NOT EXISTS idx_evidence_links_product_id ON evidence_links (product_id)",
        "CREATE INDEX IF NOT EXISTS ix_obs_llm_turn_id ON observability_llm_calls (turn_id)",
        "CREATE INDEX IF NOT EXISTS ix_obs_llm_session_id ON observability_llm_calls (session_id)",
        "CREATE INDEX IF NOT EXISTS ix_obs_llm_created_at ON observability_llm_calls (created_at)",
        "CREATE INDEX IF NOT EXISTS ix_obs_sse_turn_id ON observability_sse_events (turn_id)",
        "CREATE INDEX IF NOT EXISTS ix_obs_sse_session_id ON observability_sse_events (session_id)",
        "CREATE INDEX IF NOT EXISTS ix_obs_sse_created_at ON observability_sse_events (created_at)",
    )
    for statement in statements:
        await conn.execute(text(statement))


async def _merge_duplicate_cart_items(conn) -> None:
    await conn.execute(
        text(
            """
            WITH ranked AS (
                SELECT
                    id,
                    SUM(quantity) OVER (PARTITION BY session_id, product_id) AS total_quantity,
                    ROW_NUMBER() OVER (
                        PARTITION BY session_id, product_id
                        ORDER BY added_at ASC, id ASC
                    ) AS row_num
                FROM cart_items
            )
            UPDATE cart_items
            SET quantity = (
                SELECT total_quantity
                FROM ranked
                WHERE ranked.id = cart_items.id
            )
            WHERE id IN (SELECT id FROM ranked WHERE row_num = 1)
            """
        )
    )
    await conn.execute(
        text(
            """
            DELETE FROM cart_items
            WHERE id IN (
                SELECT id
                FROM (
                    SELECT
                        id,
                        ROW_NUMBER() OVER (
                            PARTITION BY session_id, product_id
                            ORDER BY added_at ASC, id ASC
                        ) AS row_num
                    FROM cart_items
                ) ranked
                WHERE row_num > 1
            )
            """
        )
    )


async def get_session():
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        yield session


async def migrate_eval_tables() -> None:
    """Drop and recreate eval_runs and eval_samples tables."""

    engine = get_async_engine()
    async with engine.begin() as conn:
        await conn.execute(text("DROP TABLE IF EXISTS eval_runs"))
        await conn.execute(text("DROP TABLE IF EXISTS eval_samples"))
        await conn.run_sync(SQLModel.metadata.create_all)


async def migrate_eval_runs_table() -> None:
    """Drop and recreate only the eval_runs table (preserves eval_samples)."""

    engine = get_async_engine()
    async with engine.begin() as conn:
        await conn.execute(text("DROP TABLE IF EXISTS eval_runs"))
        await conn.run_sync(SQLModel.metadata.create_all)


async def ensure_eval_schema() -> None:
    """Create eval tables, add current columns, and remove legacy sample columns."""

    engine = get_async_engine()
    async with engine.begin() as conn:
        await conn.run_sync(SQLModel.metadata.create_all)
        for table_name in ("eval_runs", "eval_samples"):
            await conn.run_sync(_add_missing_columns_sync, table_name)
        await conn.run_sync(_drop_legacy_eval_sample_columns_sync)


def _ensure_model_columns_sync(conn: Connection) -> None:
    inspector = inspect(conn)
    existing_tables = set(inspector.get_table_names())
    for table_name in SQLModel.metadata.tables:
        if table_name in existing_tables:
            _add_missing_columns_sync(conn, table_name)


def _add_missing_columns_sync(conn: Connection, table_name: str) -> None:
    table = SQLModel.metadata.tables[table_name]
    inspector = inspect(conn)
    existing = {column["name"] for column in inspector.get_columns(table_name)}
    preparer = conn.dialect.identifier_preparer
    missing = [column for column in table.columns if column.name not in existing and not column.primary_key]
    if not missing:
        return

    for column in missing:
        column_type = column.type.compile(dialect=conn.dialect)
        conn.execute(
            text(f"ALTER TABLE {preparer.quote(table_name)} ADD COLUMN {preparer.quote(column.name)} {column_type}")
        )


def _drop_legacy_eval_sample_columns_sync(conn: Connection) -> None:
    legacy_columns = {"must_have", "preferred", "forbidden"}
    inspector = inspect(conn)
    if "eval_samples" not in set(inspector.get_table_names()):
        return
    existing = {column["name"] for column in inspector.get_columns("eval_samples")}
    to_drop = sorted(legacy_columns & existing)
    if not to_drop:
        return

    preparer = conn.dialect.identifier_preparer
    for column_name in to_drop:
        conn.execute(text(f"ALTER TABLE {preparer.quote('eval_samples')} DROP COLUMN {preparer.quote(column_name)}"))
