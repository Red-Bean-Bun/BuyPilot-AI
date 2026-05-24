"""Task-oriented LLM client.

Runtime stages call these task methods only. They must not choose raw model
names or call provider SDKs directly.
"""

from __future__ import annotations

import json
import logging
from typing import Any

from pydantic import ValidationError

from src.config.domain_terms import INTENT_TERMS, contains_any
from src.config.settings import get_settings
from src.services.fallbacks import record_fallback
from src.services.image_upload import image_url_to_provider_url
from src.services.llm_fallbacks import (
    budget_from_text as _budget_from_text,
    category_from_text as _category_from_text,
    chips_for_constraints as _chips_for_constraints,
    ingredient_avoid_from_text as _ingredient_avoid_from_text,
    scenario_from_text as _scenario_from_text,
    skin_type_from_text as _skin_type_from_text,
)
from src.services.llm_gateway import (
    LiveLLMUnavailable,
    _call_chat_task,
)
from src.services.llm_task_payloads import (
    criteria_from_live_payload as _criteria_from_live_payload,
    criteria_messages,
    decision_messages,
    image_messages,
    intent_messages,
    normalize_intent_payload as _normalize_intent_payload,
    parse_json_object as _parse_json_object,
    recommendation_messages,
)
from src.types.schemas import DecisionResult, IntentResult, RecommendationResult
from src.types.sse_events import Constraints, CriteriaPayload, ProductPayload

logger = logging.getLogger(__name__)
_LOG_PAYLOAD_PREVIEW_CHARS = 2000

__all__ = [
    "LiveLLMUnavailable",
    "analyze_image",
    "analyze_intent",
    "generate_criteria",
    "generate_decision",
    "generate_recommendation",
]


async def analyze_intent(
    message: str,
    history: list[dict[str, Any]] | None = None,
    image_url: str | None = None,
    conversation_context: str = "",
) -> IntentResult:
    live = await _call_chat_task(
        "analyze_intent",
        intent_messages(message, history, image_url, conversation_context),
        json_object=True,
    )
    if live:
        parsed = _parse_json_object(live)
        if parsed:
            try:
                return IntentResult.model_validate(_normalize_intent_payload(parsed))
            except ValidationError as exc:
                record_fallback(
                    "llm.analyze_intent",
                    "invalid_json_schema",
                    error_fields=_validation_error_fields(exc),
                )
                logger.warning(
                    "Live intent payload failed schema validation; using deterministic fallback. "
                    "validation_errors=%s payload_preview=%s",
                    _json_preview(exc.errors()),
                    _json_preview(parsed),
                )
                _raise_if_strict("Live intent payload failed schema validation.")
        else:
            _raise_if_strict("Live intent response was not a JSON object.")
    _raise_if_strict("Strict runtime requires a valid live intent response.")
    if contains_any(message, INTENT_TERMS["add_to_cart"]):
        return IntentResult(intent="add_to_cart")
    if contains_any(message, INTENT_TERMS["view_cart"]):
        return IntentResult(intent="view_cart")
    if contains_any(message, INTENT_TERMS["feedback"]):
        return IntentResult(intent="feedback", extracted_constraints={"feedback_text": message})

    constraints: dict[str, Any] = {}
    category = _category_from_text(message)
    if category is None and contains_any(message, INTENT_TERMS["shopping"]):
        category = "美妆护肤"
    budget = _budget_from_text(message)
    skin_type = _skin_type_from_text(message)
    ingredient_avoid = _ingredient_avoid_from_text(message)
    if budget is not None:
        constraints["budget_max"] = budget
    if skin_type:
        constraints["skin_type"] = skin_type
    if ingredient_avoid:
        constraints["ingredient_avoid"] = ingredient_avoid
    if image_url:
        constraints["image_url"] = image_url
    return IntentResult(
        intent="recommend",
        category=category,
        extracted_constraints=constraints,
        soft_preferences=[message],
    )


async def generate_criteria(
    message: str,
    intent: IntentResult,
    feedback: dict[str, list[str]] | None = None,
    existing: CriteriaPayload | None = None,
    conversation_context: str = "",
) -> CriteriaPayload:
    live = await _call_chat_task(
        "generate_criteria",
        criteria_messages(
            message,
            intent.model_dump(),
            feedback,
            existing.model_dump() if existing else None,
            conversation_context,
        ),
        json_object=True,
    )
    if live:
        parsed = _parse_json_object(live)
        criteria = _criteria_from_live_payload(parsed, existing)
        if criteria:
            return criteria
        _raise_if_strict("Live criteria payload failed schema validation.")
    _raise_if_strict("Strict runtime requires a valid live criteria response.")
    base = existing.model_copy(deep=True) if existing else CriteriaPayload()
    category = intent.category or base.category or _category_from_text(message) or "美妆护肤"
    constraints = base.constraints.model_copy(deep=True) if base.constraints else Constraints()
    updates = intent.extracted_constraints
    constraints = constraints.model_copy(update={k: v for k, v in updates.items() if hasattr(constraints, k)})
    if constraints.use_scenario is None:
        constraints.use_scenario = _scenario_from_text(message)
    if feedback:
        constraints.ingredient_avoid = list(
            dict.fromkeys(constraints.ingredient_avoid + feedback.get("avoid_traits", []))
        )

    chips = _chips_for_constraints(category, constraints)
    summary = "，".join(chips) if chips else f"{category}导购"
    return CriteriaPayload(
        criteria_id=base.criteria_id or "c_auto_001",
        category=category,
        summary=summary,
        chips=chips,
        constraints=constraints,
    )


async def generate_recommendation(criteria: CriteriaPayload, products: list[ProductPayload]) -> RecommendationResult:
    live = await _call_chat_task(
        "generate_recommendation",
        recommendation_messages(criteria, products),
        json_object=True,
    )
    if live:
        parsed = _parse_json_object(live)
        chunks = parsed.get("text_chunks") if parsed else None
        if isinstance(chunks, list) and all(isinstance(chunk, str) for chunk in chunks):
            return RecommendationResult(text_chunks=chunks[:4], products=products)
        _raise_if_strict("Live recommendation payload failed schema validation.")
    _raise_if_strict("Strict runtime requires a valid live recommendation response.")
    if not products:
        return RecommendationResult(text_chunks=["没有找到完全匹配的商品，我会放宽软偏好再试。"])
    category = criteria.category or "商品"
    return RecommendationResult(
        text_chunks=[
            f"我先按{criteria.summary or category}来筛选。",
            "下面给你几个更匹配的选择。",
        ],
        products=products,
    )


async def analyze_image(image_url: str) -> dict[str, Any]:
    provider_image_url = image_url_to_provider_url(image_url)
    live = await _call_chat_task(
        "analyze_image",
        image_messages(image_url, provider_image_url),
        json_object=True,
    )
    if live:
        parsed = _parse_json_object(live)
        if parsed:
            parsed["image_url"] = image_url
            return parsed
        _raise_if_strict("Live image analysis payload was not valid JSON.")
    _raise_if_strict("Strict runtime requires a valid live image analysis response.")
    return {"image_url": image_url, "category_hint": "美妆护肤", "description": "图片已接收，P0 使用文本约束继续导购。"}


async def generate_decision(criteria: CriteriaPayload, products: list[ProductPayload]) -> DecisionResult:
    if not products:
        return DecisionResult(winner_product_id="", summary="暂时没有找到合适商品。")
    valid_ids = [p.product_id for p in products]
    live = await _call_chat_task(
        "generate_decision",
        decision_messages(criteria, products),
        json_object=True,
    )
    if live:
        parsed = _parse_json_object(live)
        if parsed:
            winner_id = parsed.get("winner_product_id", "")
            if winner_id in valid_ids:
                return DecisionResult(
                    winner_product_id=winner_id,
                    summary=parsed.get("summary", f"优先选{winner_id}。"),
                    why=parsed.get("why", ["综合匹配度最高"])
                    if isinstance(parsed.get("why"), list)
                    else ["综合匹配度最高"],
                    not_for=parsed.get("not_for", []) if isinstance(parsed.get("not_for"), list) else [],
                )
        _raise_if_strict("Live decision payload failed schema validation.")
    _raise_if_strict("Strict runtime requires a valid live decision response.")
    winner = products[0]
    why = []
    if winner.price is not None:
        why.append(f"{winner.price:g}元")
    if criteria.constraints.skin_type and criteria.constraints.skin_type in winner.skin_type_match:
        why.append(f"{criteria.constraints.skin_type}适用")
    if winner.use_scenario:
        why.append(winner.use_scenario)
    if not why:
        why.append("综合匹配度最高")
    return DecisionResult(
        winner_product_id=winner.product_id,
        summary=f"优先选{winner.name}。",
        why=why,
        not_for=[],
    )


def _raise_if_strict(message: str) -> None:
    if get_settings().strict_runtime:
        raise RuntimeError(message)


def _json_preview(value: Any) -> str:
    try:
        text = json.dumps(value, ensure_ascii=False, default=str, separators=(",", ":"))
    except TypeError:
        text = str(value)
    if len(text) <= _LOG_PAYLOAD_PREVIEW_CHARS:
        return text
    return f"{text[:_LOG_PAYLOAD_PREVIEW_CHARS]}...<truncated>"


def _validation_error_fields(exc: ValidationError) -> list[str]:
    fields: list[str] = []
    for error in exc.errors()[:8]:
        loc = error.get("loc", ())
        if isinstance(loc, tuple):
            fields.append(".".join(str(part) for part in loc))
        else:
            fields.append(str(loc))
    return fields
