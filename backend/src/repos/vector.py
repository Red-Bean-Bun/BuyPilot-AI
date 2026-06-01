"""PostgreSQL pgvector type helpers with SQLite fallback support."""

from __future__ import annotations

import json
from typing import Any

from sqlalchemy import JSON
from sqlalchemy.types import TypeDecorator, UserDefinedType


EMBEDDING_DIMENSIONS = 1024
VL_EMBEDDING_DIMENSIONS = 1024  # qwen3-vl-embedding supports 256-2560; 1024 matches text embedding


class PgVector(UserDefinedType):
    """Minimal SQLAlchemy type for pgvector without adding a runtime package."""

    cache_ok = True

    def __init__(self, dimensions: int = EMBEDDING_DIMENSIONS) -> None:
        self.dimensions = dimensions

    def get_col_spec(self, **kw: Any) -> str:
        return f"VECTOR({self.dimensions})"


class EmbeddingType(TypeDecorator):
    """Use pgvector on PostgreSQL and JSON everywhere else."""

    impl = JSON
    cache_ok = True

    def __init__(self, dimensions: int = EMBEDDING_DIMENSIONS) -> None:
        super().__init__()
        self.dimensions = dimensions

    def load_dialect_impl(self, dialect):
        if dialect.name == "postgresql":
            return dialect.type_descriptor(PgVector(self.dimensions))
        return dialect.type_descriptor(JSON())

    def process_bind_param(self, value, dialect):
        if dialect.name == "postgresql":
            return vector_to_pg_literal(value)
        return value

    def process_result_value(self, value, dialect):
        return coerce_vector(value)


def vector_to_pg_literal(value: Any) -> str | None:
    if value is None:
        return None
    if isinstance(value, str):
        return value
    return "[" + ",".join(_format_float(float(item)) for item in value) + "]"


def coerce_vector(value: Any) -> list[float]:
    if value is None:
        return []
    if isinstance(value, list | tuple):
        return [float(item) for item in value]
    if isinstance(value, str):
        text = value.strip()
        if not text:
            return []
        if text.startswith("[") and text.endswith("]"):
            text = text[1:-1]
        if not text:
            return []
        return [float(item.strip()) for item in text.split(",") if item.strip()]
    try:
        parsed = json.loads(value)
    except (TypeError, json.JSONDecodeError):
        return []
    return coerce_vector(parsed)


def _format_float(value: float) -> str:
    return format(value, ".8g")
