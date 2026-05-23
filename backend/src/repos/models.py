"""SQLModel table definitions for the BuyPilot backend."""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Any
from uuid import uuid4

from sqlalchemy import Column, JSON
from sqlmodel import Field, SQLModel

from src.repos.vector import EMBEDDING_DIMENSIONS, EmbeddingType


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


class Product(SQLModel, table=True):
    __tablename__ = "products"

    id: str = Field(primary_key=True)
    name: str
    category: str
    sub_category: str | None = None
    price: float | None = None
    brand: str | None = None
    image_urls: list[str] = Field(default_factory=list, sa_column=Column(JSON))
    product_url: str | None = None
    product_metadata: dict[str, Any] = Field(default_factory=dict, sa_column=Column("metadata", JSON))


class ProductChunk(SQLModel, table=True):
    __tablename__ = "product_chunks"

    id: str = Field(default_factory=lambda: str(uuid4()), primary_key=True)
    product_id: str = Field(foreign_key="products.id", index=True)
    chunk_text: str
    chunk_index: int
    embedding: list[float] = Field(default_factory=list, sa_column=Column(EmbeddingType(EMBEDDING_DIMENSIONS)))
    chunk_metadata: dict[str, Any] = Field(default_factory=dict, sa_column=Column("metadata", JSON))


class Conversation(SQLModel, table=True):
    __tablename__ = "conversations"

    id: str = Field(default_factory=lambda: str(uuid4()), primary_key=True)
    session_id: str = Field(index=True)
    message_id: str
    user_message: str
    criteria_json: dict[str, Any] | None = Field(default=None, sa_column=Column(JSON))
    ai_response: str | None = None
    product_ids: list[str] = Field(default_factory=list, sa_column=Column(JSON))
    created_at: datetime = Field(default_factory=utc_now)


class Feedback(SQLModel, table=True):
    __tablename__ = "feedbacks"

    id: str = Field(default_factory=lambda: str(uuid4()), primary_key=True)
    session_id: str = Field(index=True)
    product_id: str | None = None
    action: str
    reason: str | None = None
    created_at: datetime = Field(default_factory=utc_now)


class CartItem(SQLModel, table=True):
    __tablename__ = "cart_items"

    id: str = Field(default_factory=lambda: str(uuid4()), primary_key=True)
    session_id: str = Field(index=True)
    product_id: str = Field(foreign_key="products.id")
    quantity: int = 1
    added_at: datetime = Field(default_factory=utc_now)


class EvalRun(SQLModel, table=True):
    __tablename__ = "eval_runs"

    id: str = Field(default_factory=lambda: str(uuid4()), primary_key=True)
    run_name: str
    strategy_tag: str | None = None
    prompt_version: str | None = None
    git_commit: str | None = None
    metrics: dict[str, Any] = Field(default_factory=dict, sa_column=Column(JSON))
    samples_detail: list[dict[str, Any]] = Field(default_factory=list, sa_column=Column(JSON))
    sample_count: int | None = None
    created_at: datetime = Field(default_factory=utc_now)


class RetrievalTrace(SQLModel, table=True):
    __tablename__ = "retrieval_traces"

    id: str = Field(default_factory=lambda: str(uuid4()), primary_key=True)
    conversation_id: str | None = Field(default=None, foreign_key="conversations.id")
    criteria_id: str | None = None
    filters_applied: dict[str, Any] = Field(default_factory=dict, sa_column=Column(JSON))
    vector_top_k: list[dict[str, Any]] = Field(default_factory=list, sa_column=Column(JSON))
    rerank_top_n: list[dict[str, Any]] = Field(default_factory=list, sa_column=Column(JSON))
    selected_ids: list[str] = Field(default_factory=list, sa_column=Column(JSON))
    hit_count: int = 0
    vector_count: int = 0
    created_at: datetime = Field(default_factory=utc_now)


class EvidenceLink(SQLModel, table=True):
    __tablename__ = "evidence_links"

    id: str = Field(default_factory=lambda: str(uuid4()), primary_key=True)
    conversation_id: str | None = Field(default=None, foreign_key="conversations.id")
    product_id: str = Field(foreign_key="products.id")
    chunk_id: str | None = Field(default=None, foreign_key="product_chunks.id")
    evidence_type: str | None = None
    relevance_score: float | None = None
    cited_in: str | None = None


class EvalSample(SQLModel, table=True):
    __tablename__ = "eval_samples"

    id: str = Field(default_factory=lambda: str(uuid4()), primary_key=True)
    question: str
    image_path: str | None = None
    context: dict[str, Any] | None = Field(default=None, sa_column=Column(JSON))
    scenario_type: str | None = None
    difficulty: str | None = None
    ground_truth: dict[str, Any] = Field(default_factory=dict, sa_column=Column(JSON))
    tags: list[str] = Field(default_factory=list, sa_column=Column(JSON))
    created_at: datetime = Field(default_factory=utc_now)
