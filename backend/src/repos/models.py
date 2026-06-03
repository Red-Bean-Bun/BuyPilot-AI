"""SQLModel table definitions for the BuyPilot backend."""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Any
from uuid import uuid4

from sqlalchemy import Column, Index, JSON, UniqueConstraint
from sqlmodel import Field, SQLModel

from src.repos.vector import EMBEDDING_DIMENSIONS, VL_EMBEDDING_DIMENSIONS, EmbeddingType


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


class Product(SQLModel, table=True):
    __tablename__ = "products"
    __table_args__ = (
        Index("idx_products_category_price", "category", "price"),
        Index("idx_products_category_sub_category", "category", "sub_category"),
        Index("idx_products_brand", "brand"),
    )

    id: str = Field(primary_key=True)
    name: str
    category: str
    sub_category: str | None = None
    price: float | None = None
    brand: str | None = None
    image_urls: list[str] = Field(default_factory=list, sa_column=Column(JSON))
    product_url: str | None = None
    product_metadata: dict[str, Any] = Field(default_factory=dict, sa_column=Column("metadata", JSON))
    marketing_description: str | None = None
    official_faq: list[dict[str, str]] = Field(default_factory=list, sa_column=Column("official_faq", JSON))
    user_reviews: list[dict[str, Any]] = Field(default_factory=list, sa_column=Column("user_reviews", JSON))


class ProductChunk(SQLModel, table=True):
    __tablename__ = "product_chunks"

    id: str = Field(default_factory=lambda: str(uuid4()), primary_key=True)
    product_id: str = Field(foreign_key="products.id", index=True)
    chunk_text: str
    chunk_index: int
    embedding: list[float] = Field(default_factory=list, sa_column=Column(EmbeddingType(EMBEDDING_DIMENSIONS)))
    chunk_metadata: dict[str, Any] = Field(default_factory=dict, sa_column=Column("metadata", JSON))


class ProductImageEmbedding(SQLModel, table=True):
    """Image embeddings for visual similarity search (qwen3-vl-embedding)."""

    __tablename__ = "product_image_embeddings"
    __table_args__ = (
        UniqueConstraint("product_id", "image_path", "embedding_model", "embedding_dimensions"),
    )

    id: str = Field(default_factory=lambda: str(uuid4()), primary_key=True)
    product_id: str = Field(foreign_key="products.id", index=True)
    image_path: str
    embedding: list[float] = Field(default_factory=list, sa_column=Column(EmbeddingType(VL_EMBEDDING_DIMENSIONS)))
    embedding_model: str
    embedding_dimensions: int
    image_hash: str
    indexed_at: str


class Conversation(SQLModel, table=True):
    __tablename__ = "conversations"

    id: str = Field(default_factory=lambda: str(uuid4()), primary_key=True)
    session_id: str = Field(index=True)
    message_id: str
    deck_id: str | None = Field(default=None, index=True)
    user_message: str
    criteria_json: dict[str, Any] | None = Field(default=None, sa_column=Column(JSON))
    ai_response: str | None = None
    product_ids: list[str] = Field(default_factory=list, sa_column=Column(JSON))
    created_at: datetime = Field(default_factory=utc_now)


class Feedback(SQLModel, table=True):
    __tablename__ = "feedbacks"

    id: str = Field(default_factory=lambda: str(uuid4()), primary_key=True)
    session_id: str = Field(index=True)
    deck_id: str | None = Field(default=None, index=True)
    product_id: str | None = None
    action: str
    reason: str | None = None
    created_at: datetime = Field(default_factory=utc_now)


class CartItem(SQLModel, table=True):
    __tablename__ = "cart_items"
    __table_args__ = (UniqueConstraint("session_id", "product_id", name="uq_cart_items_session_product"),)

    id: str = Field(default_factory=lambda: str(uuid4()), primary_key=True)
    session_id: str = Field(index=True)
    product_id: str = Field(foreign_key="products.id")
    quantity: int = 1
    added_at: datetime = Field(default_factory=utc_now)


class ActiveChatTurn(SQLModel, table=True):
    __tablename__ = "active_chat_turns"

    id: str = Field(default_factory=lambda: str(uuid4()), primary_key=True)
    session_id: str = Field(index=True)
    turn_id: str = Field(index=True)
    trace_id: str | None = Field(default=None, index=True)
    started_at: datetime = Field(default_factory=utc_now)


class ChatTurnCancellation(SQLModel, table=True):
    __tablename__ = "chat_turn_cancellations"

    id: str = Field(default_factory=lambda: str(uuid4()), primary_key=True)
    session_id: str = Field(index=True)
    turn_id: str = Field(index=True)
    requested_at: datetime = Field(default_factory=utc_now)


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
    __table_args__ = (
        Index("idx_retrieval_traces_conversation_id", "conversation_id"),
        Index("idx_retrieval_traces_created_at", "created_at"),
    )

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
    __table_args__ = (
        Index("idx_evidence_links_conversation_id", "conversation_id"),
        Index("idx_evidence_links_product_id", "product_id"),
    )

    id: str = Field(default_factory=lambda: str(uuid4()), primary_key=True)
    conversation_id: str | None = Field(default=None, foreign_key="conversations.id")
    product_id: str = Field(foreign_key="products.id")
    chunk_id: str | None = Field(default=None, foreign_key="product_chunks.id")
    source_id_raw: str | None = None
    snippet: str | None = None
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


class ApiRequestLog(SQLModel, table=True):
    __tablename__ = "api_request_logs"

    id: str = Field(default_factory=lambda: str(uuid4()), primary_key=True)
    request_id: str = Field(index=True)
    trace_id: str | None = Field(default=None, index=True)
    session_id: str | None = Field(default=None, index=True)
    turn_id: str | None = Field(default=None, index=True)
    method: str
    path: str
    status_code: int
    duration_ms: float
    client_ip: str | None = None
    user_agent: str | None = None
    request_content_type: str | None = None
    request_body_json: dict[str, Any] | list[Any] | None = Field(default=None, sa_column=Column(JSON))
    request_body_text: str | None = None
    request_body_truncated: bool = False
    response_content_type: str | None = None
    response_body_json: dict[str, Any] | list[Any] | None = Field(default=None, sa_column=Column(JSON))
    response_body_text: str | None = None
    response_body_truncated: bool = False
    error_code: str | None = None
    error_type: str | None = None
    created_at: datetime = Field(default_factory=utc_now)


class AuditEvent(SQLModel, table=True):
    __tablename__ = "audit_events"

    id: str = Field(default_factory=lambda: str(uuid4()), primary_key=True)
    request_id: str | None = Field(default=None, index=True)
    trace_id: str | None = Field(default=None, index=True)
    session_id: str | None = Field(default=None, index=True)
    turn_id: str | None = Field(default=None, index=True)
    actor_type: str = "anonymous"
    action: str = Field(index=True)
    resource_type: str | None = None
    resource_id: str | None = None
    side_effect: bool = True
    before_json: dict[str, Any] | None = Field(default=None, sa_column=Column(JSON))
    after_json: dict[str, Any] | None = Field(default=None, sa_column=Column(JSON))
    audit_metadata: dict[str, Any] = Field(default_factory=dict, sa_column=Column("metadata", JSON))
    created_at: datetime = Field(default_factory=utc_now)


class SystemMetadata(SQLModel, table=True):
    __tablename__ = "system_metadata"

    key: str = Field(primary_key=True)
    value_json: dict[str, Any] = Field(default_factory=dict, sa_column=Column("value", JSON))
    updated_at: datetime = Field(default_factory=utc_now)


class ObservabilityLLMCall(SQLModel, table=True):
    """LLM 调用明细记录，用于幻觉排查。"""

    __tablename__ = "observability_llm_calls"
    __table_args__ = (
        Index("ix_obs_llm_turn_id", "turn_id"),
        Index("ix_obs_llm_session_id", "session_id"),
        Index("ix_obs_llm_created_at", "created_at"),
    )

    id: str = Field(default_factory=lambda: str(uuid4()), primary_key=True)
    turn_id: str | None = Field(default=None, index=True)
    session_id: str | None = Field(default=None, index=True)
    task: str  # analyze_intent / generate_criteria / generate_recommendation / generate_decision / analyze_image
    profile: str
    model: str
    provider: str  # Doubao / Qwen
    status: str  # success / failed / fallback
    duration_ms: float
    prompt_hash: str
    prompt_preview: str | None = None
    prompt_json: str | None = None  # full payload 模式
    response_preview: str | None = None
    response_json: str | None = None  # full payload 模式
    parsed_json: dict[str, Any] | None = Field(default=None, sa_column=Column(JSON))
    validation_error: str | None = None
    token_usage: dict[str, Any] | None = Field(default=None, sa_column=Column(JSON))
    fallback_from: str | None = None
    error_type: str | None = None
    error_message: str | None = None
    error_raw: str | None = None  # full payload 模式
    created_at: datetime = Field(default_factory=utc_now)


class ObservabilitySSEEvent(SQLModel, table=True):
    """SSE 事件记录，用于前后端一致性验证。"""

    __tablename__ = "observability_sse_events"
    __table_args__ = (
        Index("ix_obs_sse_turn_id", "turn_id"),
        Index("ix_obs_sse_session_id", "session_id"),
        Index("ix_obs_sse_created_at", "created_at"),
    )

    id: str = Field(default_factory=lambda: str(uuid4()), primary_key=True)
    turn_id: str | None = Field(default=None, index=True)
    session_id: str | None = Field(default=None, index=True)
    event_type: str  # thinking / text_delta / product_card / criteria_card / final_decision / clarification / cart_action / done / error
    seq: int
    node_id: str | None = None
    deck_id: str | None = None
    criteria_id: str | None = None
    product_ids: list[str] | None = Field(default=None, sa_column=Column(JSON))
    message_id: str | None = None
    delta_preview: str | None = None
    delta_hash: str | None = None
    finish_reason: str | None = None
    created_at: datetime = Field(default_factory=utc_now)
