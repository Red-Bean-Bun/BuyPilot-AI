from __future__ import annotations

import json
import time
from typing import Any, Literal, Union

from pydantic import BaseModel, Field

SCHEMA_VERSION = "2026-05-20"

DisplayMode = Literal[
    "inline_thinking",
    "inline_card",
    "inline_text",
    "summary_card",
    "swipe_deck_item",
    "none",
]


class SSEEventBase(BaseModel):
    schema_version: str = SCHEMA_VERSION
    event: str
    session_id: str
    turn_id: str
    seq: int
    event_id: str
    node_id: str
    deck_id: str | None = None
    display_mode: DisplayMode | None = None
    created_at_ms: int | None = None


class EventSeq:
    """同一 turn_id 内的 seq 递增计数器，用于生成 event_id 和 seq"""

    def __init__(self, turn_id: str) -> None:
        self.turn_id = turn_id
        self._seq = 0

    def next(self) -> int:
        self._seq += 1
        return self._seq

    def event_id(self) -> str:
        return f"{self.turn_id}:{self._seq}"


def now_ms() -> int:
    return int(time.time() * 1000)


class Constraints(BaseModel):
    """封闭 DSL —— 所有允许的约束维度显式枚举，禁止 dict[str, Any]"""
    # 通用字段（所有品类共享）
    budget_min: float | None = None
    budget_max: float | None = None
    use_scenario: str | None = None
    # 美妆护肤专属
    skin_type: str | None = None
    ingredient_avoid: list[str] = Field(default_factory=list)
    ingredient_prefer: list[str] = Field(default_factory=list)
    # 数码电子专属
    storage: str | None = None
    screen_size: str | None = None
    # 服饰运动专属
    sport_type: str | None = None
    season: str | None = None
    # 食品生活专属
    dietary: list[str] = Field(default_factory=list)


class CriteriaPayload(BaseModel):
    criteria_id: str = ""
    category: str = ""
    summary: str = ""
    chips: list[str] = Field(default_factory=list)
    constraints: Constraints = Field(default_factory=Constraints)


class ProductPayload(BaseModel):
    product_id: str
    name: str
    price: float | None = None
    currency: str | None = None
    image_url: str | None = None
    category: str = ""
    sub_category: str | None = None
    brand: str | None = None
    skin_type_match: list[str] = Field(default_factory=list)
    ingredient_tags: list[str] = Field(default_factory=list)
    ingredient_avoid: list[str] = Field(default_factory=list)
    use_scenario: str | None = None


class EvidencePayload(BaseModel):
    source_type: str
    snippet: str
    source_id: str | None = None


class AlternativePayload(BaseModel):
    product_id: str
    name: str


class QuickActionPayload(BaseModel):
    action_id: str
    label: str
    action: str
    feedback_type: str | None = None
    criteria_patch: dict[str, Any] | None = None


class ThinkingEvent(SSEEventBase):
    event: Literal["thinking"] = "thinking"
    display_mode: DisplayMode = "inline_thinking"
    stage: str
    message: str


class ClarificationEvent(SSEEventBase):
    event: Literal["clarification"] = "clarification"
    display_mode: DisplayMode = "inline_card"
    question: str
    required_slots: list[str] = Field(default_factory=list)
    suggested_options: list[str] = Field(default_factory=list)


class CriteriaCardEvent(SSEEventBase):
    event: Literal["criteria_card"] = "criteria_card"
    display_mode: DisplayMode = "summary_card"
    editable: bool = True
    criteria: CriteriaPayload = Field(default_factory=CriteriaPayload)
    quick_actions: list[QuickActionPayload] = Field(default_factory=list)


class TextDeltaEvent(SSEEventBase):
    event: Literal["text_delta"] = "text_delta"
    display_mode: DisplayMode = "inline_text"
    message_id: str
    delta: str
    done: bool = False


class ProductCardEvent(SSEEventBase):
    event: Literal["product_card"] = "product_card"
    display_mode: DisplayMode = "swipe_deck_item"
    deck_id: str  # required for product_card, not optional
    rank: int
    product: ProductPayload
    reason: str
    risk_notes: list[str] = Field(default_factory=list)
    evidence: list[EvidencePayload] = Field(default_factory=list)
    actions: list[QuickActionPayload] = Field(default_factory=list)


class CartActionEvent(SSEEventBase):
    event: Literal["cart_action"] = "cart_action"
    display_mode: DisplayMode = "inline_card"
    action: str  # add/remove/view
    product_id: str
    quantity: int = 1
    status: str = "success"  # success/failed


class FinalDecisionEvent(SSEEventBase):
    event: Literal["final_decision"] = "final_decision"
    display_mode: DisplayMode = "summary_card"
    winner_product_id: str
    summary: str
    why: list[str] = Field(default_factory=list)
    not_for: list[str] = Field(default_factory=list)
    alternatives: list[AlternativePayload] = Field(default_factory=list)
    next_actions: list[QuickActionPayload] = Field(default_factory=list)


class DoneEvent(SSEEventBase):
    event: Literal["done"] = "done"
    display_mode: DisplayMode = "none"


class ErrorEvent(SSEEventBase):
    event: Literal["error"] = "error"
    display_mode: DisplayMode = "inline_card"
    code: str
    message: str
    retryable: bool = True


SSEEvent = Union[
    ThinkingEvent,
    ClarificationEvent,
    CriteriaCardEvent,
    TextDeltaEvent,
    ProductCardEvent,
    CartActionEvent,
    FinalDecisionEvent,
    DoneEvent,
    ErrorEvent,
]

EVENT_TAG_MAP: dict[str, type[SSEEvent]] = {
    "thinking": ThinkingEvent,
    "clarification": ClarificationEvent,
    "criteria_card": CriteriaCardEvent,
    "text_delta": TextDeltaEvent,
    "product_card": ProductCardEvent,
    "cart_action": CartActionEvent,
    "final_decision": FinalDecisionEvent,
    "done": DoneEvent,
    "error": ErrorEvent,
}


def parse_sse_event(data: str) -> SSEEvent | None:
    obj = json.loads(data)
    tag = obj.get("event")
    model_cls = EVENT_TAG_MAP.get(tag)
    if model_cls is None:
        return None
    return model_cls.model_validate(obj)


def format_sse(event: SSEEventBase) -> str:
    tag = event.event
    data = event.model_dump_json()
    return f"event: {tag}\ndata: {data}\n\n"