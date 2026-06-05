"""HTTP request and response contracts for the BuyPilot backend.

API modules import these models instead of defining request shapes inline.
"""

from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel, Field

from src.types.sse_events import CriteriaPayload, EvidencePayload, ProductPayload


class MessageLite(BaseModel):
    role: Literal["user", "assistant", "system"]
    content: str


class ChatStreamRequest(BaseModel):
    message: str = Field(..., min_length=1, max_length=2000)
    session_id: str | None = None
    history: list[MessageLite] = Field(default_factory=list)
    image_url: str | None = None
    criteria_patch: dict[str, Any] | None = None
    skip_stages: list[str] = Field(default_factory=list)
    client_turn_id: str | None = None
    client_trace_id: str | None = None
    converge: bool = False


class CancelRequest(BaseModel):
    session_id: str
    turn_id: str


class CancelResponse(BaseModel):
    session_id: str
    turn_id: str
    canceled: bool = True


class ImageUploadResponse(BaseModel):
    image_url: str
    width: int | None = None
    height: int | None = None
    mime_type: str = "image/jpeg"
    ocr_text: str | None = None
    analysis: dict[str, Any] = Field(default_factory=dict)


class FeedbackRequest(BaseModel):
    session_id: str
    deck_id: str | None = None
    feedback_type: str | None = None
    action: str | None = None
    product_id: str | None = None
    reason: str | None = None


class FeedbackResponse(BaseModel):
    status: str = "received"
    session_id: str
    feedback_type: str | None = None
    action: str | None = None


class CartItemPayload(BaseModel):
    product_id: str
    name: str
    price: float | None = None
    quantity: int = 1
    added_at: str | None = None
    product: ProductPayload | None = None


class CartResponse(BaseModel):
    items: list[CartItemPayload] = Field(default_factory=list)
    total_items: int = 0
    total_price: float = 0.0


class CartMutationRequest(BaseModel):
    quantity: int = Field(default=1, ge=0)


class IntentResult(BaseModel):
    intent: Literal[
        "recommend",
        "clarify",
        "continue",
        "feedback",
        "compare",
        "add_to_cart",
        "remove_from_cart",
        "update_cart_quantity",
        "view_cart",
        "chitchat",
    ]
    confidence: float = 1.0
    category: str | None = None
    extracted_constraints: dict[str, Any] = Field(default_factory=dict)
    soft_preferences: list[str] = Field(default_factory=list)
    target_product_id: str | None = None
    compare_product_ids: list[str] = Field(default_factory=list)


class RecommendationResult(BaseModel):
    text_chunks: list[str] = Field(default_factory=list)
    products: list[ProductPayload] = Field(default_factory=list)
    evidence_by_product: dict[str, list[EvidencePayload]] = Field(default_factory=dict)


DecisionStatus = Literal["selected", "no_match", "no_suitable_winner", "needs_more_signal"]
DecisionConfidence = Literal["high", "medium", "low"]
DecisionNextStep = Literal["adjust_criteria", "replace_deck", "continue_current_deck", "accept_recommendation"]


class DecisionResult(BaseModel):
    winner_product_id: str
    summary: str
    why: list[str] = Field(default_factory=list)
    not_for: list[str] = Field(default_factory=list)
    decision_status: DecisionStatus | None = None
    confidence: DecisionConfidence | None = None
    next_step: DecisionNextStep | None = None


class SessionState(BaseModel):
    session_id: str
    last_criteria: CriteriaPayload | None = None
    last_product_ids: list[str] = Field(default_factory=list)


class FaqItem(BaseModel):
    question: str
    answer: str


class ReviewItem(BaseModel):
    nickname: str
    rating: int
    content: str


class ProductDetailResponse(BaseModel):
    product: ProductPayload
    marketing_description: str | None = None
    highlights: list[str] = Field(default_factory=list)
    faqs: list[FaqItem] = Field(default_factory=list)
    reviews: list[ReviewItem] = Field(default_factory=list)
