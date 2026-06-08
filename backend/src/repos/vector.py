"""PostgreSQL pgvector type helpers."""

from __future__ import annotations

import json
from typing import Any

from sqlalchemy.types import UserDefinedType


EMBEDDING_DIMENSIONS = 1024
VL_EMBEDDING_DIMENSIONS = 1024  # qwen3-vl-embedding supports 256-2560; 1024 matches text embedding


class PgVector(UserDefinedType):
    """Minimal SQLAlchemy type for pgvector without adding a runtime package."""

    cache_ok = True

    def __init__(self, dimensions: int = EMBEDDING_DIMENSIONS) -> None:
        self.dimensions = dimensions

    def get_col_spec(self, **kw: Any) -> str:
        return f"VECTOR({self.dimensions})"


class EmbeddingType(UserDefinedType):
    """PostgreSQL pgvector type that handles Python list ↔ pgvector literal conversion."""

    cache_ok = True

    def __init__(self, dimensions: int = EMBEDDING_DIMENSIONS) -> None:
        self.dimensions = dimensions

    def get_col_spec(self, **kw: Any) -> str:
        return f"VECTOR({self.dimensions})"

    def bind_processor(self, dialect):
        def process(value):
            return vector_to_pg_literal(value)
        return process

    def result_processor(self, dialect, coltype):
        def process(value):
            return coerce_vector(value)
        return process


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
