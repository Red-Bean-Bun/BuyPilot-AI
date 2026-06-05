"""Intent resolution business logic.

This module contains deterministic rules for refining LLM-generated intent
with post-processing: constraint merging, category inference, product lookup,
and validation.

Separated from runtime/pipeline.py to keep business rules testable without
the full SSE pipeline machinery.
"""

from __future__ import annotations

import logging
from typing import Any

from src.config.domain_terms import (
    category_from_text,
    infer_category_from_product_type,
    is_supported_product_type,
    normalize_category,
)
from src.services.message_rules import (
    extract_adjustment_hints,
    extract_brand_prefer_from_message,
    extract_product_lookup_hints,
    extract_product_type_hint,
    has_shopping_signal,
)
from src.types.schemas import IntentResult

logger = logging.getLogger(__name__)

# Constraint fields that hold list values — must be merged (appended) rather
# than overwritten when multiple extraction sources contribute values.
MERGE_LIST_FIELDS = frozenset({
    "ingredient_avoid",
    "ingredient_prefer",
    "brand_avoid",
    "brand_prefer",
    "origin_avoid",
    "dietary",
})

_AMBIGUOUS_PHOTO_SCENE_TERMS = ("拍照", "摄影", "拍摄", "自拍")
_CONCRETE_DIGITAL_PRODUCT_TERMS = (
    "手机",
    "相机",
    "耳机",
    "电脑",
    "笔记本",
    "平板",
    "微单",
    "单反",
    "镜头",
)


def has_context_value(value: object) -> bool:
    """Check if a constraint value is meaningful (not None/empty)."""
    if value is None:
        return False
    if isinstance(value, list | tuple | set | dict):
        return bool(value)
    return True


def is_ambiguous_scene_only_category_inference(message: str, category: str) -> bool:
    """Avoid turning a photo-taking scene into a digital-product category too early.

    Returns True if the category inference is ambiguous and should be skipped.
    """
    if category != "数码电子":
        return False
    if not any(term in message for term in _AMBIGUOUS_PHOTO_SCENE_TERMS):
        return False
    if any(term in message for term in _CONCRETE_DIGITAL_PRODUCT_TERMS):
        return False
    return extract_product_type_hint(message) is None


def _merge_constraints(
    merged: dict[str, Any],
    updates: dict[str, Any],
) -> dict[str, Any]:
    """Merge constraint updates into existing dict, handling list fields correctly."""
    result = dict(merged)
    for key, value in updates.items():
        if key in MERGE_LIST_FIELDS and isinstance(value, list):
            existing = result.get(key, []) or []
            result[key] = list(dict.fromkeys([*existing, *value]))
        else:
            result[key] = value
    return result


def resolve_intent_constraints(
    intent: IntentResult,
    message: str,
) -> IntentResult:
    """Apply deterministic post-processing to LLM-generated intent.

    This function refines the intent by:
    1. Merging adjustment hints into constraints
    2. Extracting brand preferences from natural language
    3. Inferring category for shopping intents without one
    4. Extracting product type hints for LLM gaps
    5. Validating category against product_type

    Returns a new IntentResult with refined constraints.
    """
    updates: dict[str, Any] = {}
    merged = dict(intent.extracted_constraints or {})

    # 1. Merge adjustment hints
    adjustment = extract_adjustment_hints(message)
    if adjustment and intent.intent in {"recommend", "clarify", "feedback"}:
        merged = _merge_constraints(merged, adjustment)

    # 2. Extract brand preference (铁律3)
    brand_prefer = extract_brand_prefer_from_message(message)
    if brand_prefer and intent.intent in {"recommend", "clarify", "feedback"}:
        existing = merged.get("brand_prefer", [])
        if isinstance(existing, list):
            merged["brand_prefer"] = list(dict.fromkeys(existing + brand_prefer))
        else:
            merged["brand_prefer"] = brand_prefer

    # 3. Post-LLM safety net: override chitchat/feedback with shopping signal
    if intent.intent in {"chitchat", "feedback"}:
        if has_shopping_signal(message):
            inferred_category = intent.category or category_from_text(message)
            intent = IntentResult(
                intent="clarify",
                confidence=intent.confidence,
                category=inferred_category,
                extracted_constraints=intent.extracted_constraints or {},
            )
            merged = dict(intent.extracted_constraints or {})
            updates = {}

    # 4. Fallback category inference for shopping intents
    if intent.intent in {"recommend", "clarify"} and not intent.category:
        inferred = category_from_text(message)
        if inferred and not is_ambiguous_scene_only_category_inference(message, inferred):
            updates["category"] = inferred

    # 5. Product-lookup extraction: "有鼠标吗" → product_type="鼠标"
    lookup = extract_product_lookup_hints(message)
    if lookup and intent.intent in {"recommend", "clarify", "feedback"}:
        merged = _merge_constraints(merged, lookup)

    # 6. Deterministic product_type extraction
    product_hint = extract_product_type_hint(message)
    if product_hint and intent.intent in {"recommend", "clarify", "feedback"}:
        if "product_type" not in merged:
            merged["product_type"] = product_hint
        expected_cat = infer_category_from_product_type(product_hint)
        if expected_cat and normalize_category(intent.category) != expected_cat:
            updates["category"] = expected_cat

    # Apply accumulated updates
    if merged != (intent.extracted_constraints or {}):
        updates["extracted_constraints"] = merged
    if updates:
        intent = intent.model_copy(update=updates)

    return intent
