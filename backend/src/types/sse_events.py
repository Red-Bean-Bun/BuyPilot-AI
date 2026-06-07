from __future__ import annotations

import json
import time
from pathlib import Path
from typing import Any, Literal, Union

from pydantic import BaseModel, Field

SCHEMA_VERSION = "2026-05-20"
CriteriaFieldSource = Literal["user", "inferred", "history"]
DoneFinishReason = Literal[
    "awaiting_criteria_confirmation",
    "awaiting_criteria_adjustment",
    "awaiting_product_feedback",
    "completed",
    "cancelled",
    "error",
]

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
    # 排除与限定
    brand_avoid: list[str] = Field(default_factory=list)
    brand_prefer: list[str] = Field(default_factory=list)
    origin_avoid: list[str] = Field(default_factory=list)
    product_type: str | None = None
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
    field_sources: dict[str, CriteriaFieldSource] = Field(default_factory=dict)


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
    sku_options: list[dict[str, Any]] | None = None


class EvidencePayload(BaseModel):
    source_type: str
    snippet: str
    source_id: str | None = None


class ReasonAtomPayload(BaseModel):
    dimension: str
    value: str
    text: str
    evidence_id: str | None = None


class AlternativePayload(BaseModel):
    product_id: str
    name: str


class CompareAxisValuePayload(BaseModel):
    """Single product's score on one comparison axis."""

    product_id: str
    score: float | None = None
    label: str | None = None
    detail: str | None = None
    evidence_ids: list[str] = Field(default_factory=list)


class CompareAxisPayload(BaseModel):
    """One comparison dimension with per-product values."""

    name: str
    values: list[CompareAxisValuePayload] = Field(default_factory=list)


class CompareRiskNotePayload(BaseModel):
    product_id: str
    note: str


class QuickActionPayload(BaseModel):
    action_id: str
    label: str
    action: str
    feedback_type: str | None = None
    criteria_patch: dict[str, Any] | None = None


class DecisionBarrierPayload(BaseModel):
    barrier_type: Literal[
        "fear_wrong_choice",
        "value_uncertainty",
        "fit_uncertainty",
        "trust_uncertainty",
        "price_sensitive",
        "choice_overload",
    ]
    label: str
    reason: str = ""
    conversion_strategy: str = ""


class SearchStrategyPayload(BaseModel):
    category: str | None = None
    product_type: str | None = None
    use_scenario: str | None = None


class PrimaryDirectionPayload(BaseModel):
    title: str
    summary: str = ""
    why: str = ""
    search_strategy: SearchStrategyPayload = Field(default_factory=SearchStrategyPayload)
    available_in_catalog: bool = False
    supporting_product_count: int = 0


class ShoppingStrategyPayload(BaseModel):
    strategy_id: str
    scene_type: Literal["gift", "interest", "travel", "usage", "risk_sensitive", "goal_oriented"]
    scene_summary: str = ""
    user_problem: str = ""
    decision_barrier: DecisionBarrierPayload | None = None
    primary_direction: PrimaryDirectionPayload
    avoid_risks: list[str] = Field(default_factory=list)
    assumptions: list[str] = Field(default_factory=list)
    confidence: Literal["low", "medium", "high"] = "medium"


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
    shopping_strategy: ShoppingStrategyPayload | None = None
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
    reason_atoms: list[ReasonAtomPayload] = Field(default_factory=list)
    risk_notes: list[str] = Field(default_factory=list)
    evidence: list[EvidencePayload] = Field(default_factory=list)
    actions: list[QuickActionPayload] = Field(default_factory=list)


class CartItemEventPayload(BaseModel):
    product_id: str
    name: str
    price: float | None = None
    quantity: int = 1
    added_at: str | None = None
    product: ProductPayload | None = None


class CartSummaryPayload(BaseModel):
    items: list[CartItemEventPayload] = Field(default_factory=list)
    total_items: int = 0
    total_price: float = 0.0


class CartActionEvent(SSEEventBase):
    event: Literal["cart_action"] = "cart_action"
    display_mode: DisplayMode = "inline_card"
    action: str  # add/remove/update_quantity/view
    product_id: str
    quantity: int = 1
    status: str = "success"  # success/failed
    cart: CartSummaryPayload | None = None


class FinalDecisionEvent(SSEEventBase):
    event: Literal["final_decision"] = "final_decision"
    display_mode: DisplayMode = "summary_card"
    winner_product_id: str
    summary: str
    why: list[str] = Field(default_factory=list)
    not_for: list[str] = Field(default_factory=list)
    alternatives: list[AlternativePayload] = Field(default_factory=list)
    next_actions: list[QuickActionPayload] = Field(default_factory=list)
    decision_status: Literal["selected", "no_match", "no_suitable_winner", "needs_more_signal"] | None = None
    confidence: Literal["high", "medium", "low"] | None = None
    next_step: Literal["adjust_criteria", "replace_deck", "continue_current_deck", "accept_recommendation"] | None = (
        None
    )
    score_breakdown: dict[str, Any] | None = None


class DoneEvent(SSEEventBase):
    event: Literal["done"] = "done"
    display_mode: DisplayMode = "none"
    finish_reason: DoneFinishReason = "completed"


class ErrorEvent(SSEEventBase):
    event: Literal["error"] = "error"
    display_mode: DisplayMode = "inline_card"
    code: str
    message: str
    retryable: bool = True


class CompareCardEvent(SSEEventBase):
    event: Literal["compare_card"] = "compare_card"
    display_mode: DisplayMode = "summary_card"
    compare_id: str
    source_deck_id: str | None = None
    mode: Literal["exploratory", "decision"]
    focus: str | None = None
    products: list[ProductPayload] = Field(default_factory=list)
    axes: list[CompareAxisPayload] = Field(default_factory=list)
    winner_product_id: str | None = None
    winner_reason: str | None = None
    tradeoffs: list[str] = Field(default_factory=list)
    risk_notes: list[CompareRiskNotePayload] = Field(default_factory=list)
    confidence: Literal["high", "medium", "low"] | None = None


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
    CompareCardEvent,
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
    "compare_card": CompareCardEvent,
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


# ═══════════════════════════════════════════════════════════════════════
# Import-time SSE protocol guard (Rust-style compile-time check)
#
# This runs at module import. If the JSON Schema and Python models
# drift apart, the module fails to import → uvicorn refuses to start.
# This is the Python equivalent of Rust's borrow checker: you cannot
# run the program with an inconsistent protocol definition.
#
# Cross-language check (Schema ↔ Kotlin) lives in:
#   scripts/check_sse_protocol.py  (run via `make protocol-check`)
# ═══════════════════════════════════════════════════════════════════════

def _load_schema_event_types() -> frozenset[str]:
    """Extract event type names from contracts/sse-events.schema.json."""
    schema_path = Path(__file__).resolve().parents[3] / "contracts" / "sse-events.schema.json"
    if not schema_path.exists():
        return frozenset()  # graceful degradation for isolated test envs
    schema = json.loads(schema_path.read_text(encoding="utf-8"))
    types: list[str] = []
    for name, defn in schema.get("$defs", {}).items():
        for part in defn.get("allOf", []):
            event_const = part.get("properties", {}).get("event", {}).get("const")
            if event_const:
                types.append(event_const)
    return frozenset(types)


def _verify_protocol_consistency() -> None:
    """Schema ↔ Python drift check. Raises ImportError on mismatch."""
    schema_types = _load_schema_event_types()
    if not schema_types:
        return  # schema file not found (isolated test env), skip

    python_types = frozenset(EVENT_TAG_MAP.keys())

    in_schema_not_python = schema_types - python_types
    in_python_not_schema = python_types - schema_types

    errors: list[str] = []
    if in_schema_not_python:
        errors.append(
            f"JSON Schema defines event types missing from Python EVENT_TAG_MAP: "
            f"{sorted(in_schema_not_python)}. "
            f"Add the corresponding Pydantic class + EVENT_TAG_MAP entry in sse_events.py."
        )
    if in_python_not_schema:
        errors.append(
            f"Python EVENT_TAG_MAP defines event types not in JSON Schema: "
            f"{sorted(in_python_not_schema)}. "
            f"Update contracts/sse-events.schema.json FIRST (铁律1: Schema is source of truth)."
        )
    if errors:
        raise ImportError(
            "SSE protocol drift detected — server cannot start.\n"
            + "\n".join(errors)
            + "\n\nFix: update the out-of-sync source, then restart."
        )


_verify_protocol_consistency()
