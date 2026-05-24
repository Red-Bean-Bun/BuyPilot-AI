"""Prompt and response helpers for task-oriented LLM calls."""

from __future__ import annotations

import json
import re
from typing import Any

from pydantic import ValidationError

from src.services.llm_fallbacks import chips_for_constraints
from src.services.prompts import get_prompt_store
from src.types.sse_events import Constraints, CriteriaPayload, ProductPayload

INTENT_SYSTEM_FALLBACK = (
    "你是电商导购意图识别器。只输出 JSON，字段为 intent、confidence、category、"
    "extracted_constraints、soft_preferences、target_product_id。intent 只能是 "
    "recommend/clarify/feedback/add_to_cart/view_cart/chitchat。"
)

CRITERIA_SYSTEM_FALLBACK = (
    "你是电商导购购买标准生成器。只输出 JSON，字段为 criteria_id、category、summary、"
    "chips、constraints。constraints 必须只使用允许字段：budget_min,budget_max,"
    "use_scenario,skin_type,ingredient_avoid,ingredient_prefer,storage,screen_size,"
    "sport_type,season,dietary。不要输出商品。"
)

RECOMMENDATION_SYSTEM_FALLBACK = (
    "你是电商导购推荐解释生成器。只输出 JSON，字段为 text_chunks。只能解释传入商品，不得编造商品、价格、优惠或库存。"
)

DECISION_SYSTEM_FALLBACK = (
    "你是电商导购决策器。只输出 JSON，字段为 winner_product_id、summary、why、not_for。"
    "winner_product_id 必须是传入商品之一，不得编造。"
    "why 是选择该商品的理由列表（每条一句话）。"
    "not_for 是不适合人群或场景列表。"
)

IMAGE_SYSTEM_FALLBACK = "你是商品图片理解器。只输出 JSON，字段为 category_hint、description、visible_traits。"


def intent_messages(
    message: str,
    history: list[dict[str, Any]] | None,
    image_url: str | None,
    conversation_context: str = "",
) -> list[dict[str, Any]]:
    return [
        {
            "role": "system",
            "content": _prompt_content(
                "intent_analysis",
                INTENT_SYSTEM_FALLBACK,
                {
                    "user_message": message,
                    "history": history or [],
                    "conversation_context": conversation_context,
                },
                INTENT_SYSTEM_FALLBACK,
            ),
        },
        {"role": "user", "content": _history_prompt(message, history, image_url)},
    ]


def criteria_messages(
    message: str,
    intent_dump: dict[str, Any],
    feedback: dict[str, list[str]] | None,
    existing_dump: dict[str, Any] | None,
    conversation_context: str = "",
) -> list[dict[str, Any]]:
    payload = {
        "message": message,
        "intent": intent_dump,
        "feedback": feedback or {},
        "existing": existing_dump,
    }
    return [
        {
            "role": "system",
            "content": _prompt_content(
                "criteria_generation",
                CRITERIA_SYSTEM_FALLBACK,
                {
                    "user_message": message,
                    "intent_result": intent_dump,
                    "history": [],
                    "feedback_constraints": feedback or {},
                    "conversation_context": conversation_context,
                },
                CRITERIA_SYSTEM_FALLBACK,
            ),
        },
        {"role": "user", "content": json.dumps(payload, ensure_ascii=False)},
    ]


def recommendation_messages(criteria: CriteriaPayload, products: list[ProductPayload]) -> list[dict[str, Any]]:
    payload = {
        "criteria": criteria.model_dump(),
        "products": [product.model_dump() for product in products],
    }
    return [
        {
            "role": "system",
            "content": _prompt_content(
                "recommendation",
                RECOMMENDATION_SYSTEM_FALLBACK,
                {
                    "criteria": criteria.model_dump(),
                    "ranked_products": [product.model_dump() for product in products],
                    "evidence_chunks": [],
                },
                RECOMMENDATION_SYSTEM_FALLBACK,
            ),
        },
        {"role": "user", "content": json.dumps(payload, ensure_ascii=False)},
    ]


def image_messages(image_url: str, provider_image_url: str) -> list[dict[str, Any]]:
    return [
        {
            "role": "system",
            "content": _prompt_content(
                "image_analysis",
                IMAGE_SYSTEM_FALLBACK,
                {"image_url": image_url},
                IMAGE_SYSTEM_FALLBACK,
            ),
        },
        {
            "role": "user",
            "content": [
                {"type": "text", "text": "请识别这张商品图片，输出适合导购检索的简短结构化信息。"},
                {"type": "image_url", "image_url": {"url": provider_image_url}},
            ],
        },
    ]


def decision_messages(criteria: CriteriaPayload, products: list[ProductPayload]) -> list[dict[str, Any]]:
    payload = {
        "criteria": criteria.model_dump(),
        "products": [product.model_dump() for product in products],
        "valid_winner_ids": [product.product_id for product in products],
    }
    return [
        {
            "role": "system",
            "content": _prompt_content(
                "decision",
                DECISION_SYSTEM_FALLBACK,
                {
                    "criteria": criteria.model_dump(),
                    "recommendations": [product.model_dump() for product in products],
                    "feedback_history": [],
                },
                DECISION_SYSTEM_FALLBACK,
            ),
        },
        {"role": "user", "content": json.dumps(payload, ensure_ascii=False)},
    ]


def parse_json_object(text: str) -> dict[str, Any] | None:
    try:
        parsed = json.loads(text)
        return parsed if isinstance(parsed, dict) else None
    except json.JSONDecodeError:
        match = re.search(r"\{.*\}", text, flags=re.S)
        if not match:
            return None
        try:
            parsed = json.loads(match.group(0))
        except json.JSONDecodeError:
            return None
        return parsed if isinstance(parsed, dict) else None


def normalize_intent_payload(payload: dict[str, Any]) -> dict[str, Any]:
    normalized = dict(payload)
    if "intent" not in normalized and isinstance(normalized.get("intent_type"), str):
        intent_type = normalized["intent_type"]
        normalized["intent"] = {
            "filter": "recommend",
            "compare": "recommend",
            "unclear": "clarify",
        }.get(intent_type, intent_type)
    if normalized.get("intent") not in {"recommend", "clarify", "feedback", "add_to_cart", "view_cart", "chitchat"}:
        normalized["intent"] = "recommend" if normalized.get("is_shopping_related", True) else "chitchat"
    if "soft_preferences" not in normalized and isinstance(normalized.get("user_intent_summary"), str):
        normalized["soft_preferences"] = [normalized["user_intent_summary"]]
    return normalized


def criteria_from_live_payload(
    payload: dict[str, Any] | None, existing: CriteriaPayload | None
) -> CriteriaPayload | None:
    if not payload:
        return None
    base = existing.model_copy(deep=True) if existing else CriteriaPayload()
    raw_constraints_obj = payload.get("constraints")
    raw_constraints: dict[str, Any] = raw_constraints_obj if isinstance(raw_constraints_obj, dict) else {}
    try:
        constraints = Constraints.model_validate(
            {
                **base.constraints.model_dump(),
                **raw_constraints,
            }
        )
        criteria = CriteriaPayload.model_validate(
            {
                "criteria_id": payload.get("criteria_id") or base.criteria_id or "c_auto_001",
                "category": payload.get("category") or base.category or "美妆护肤",
                "summary": payload.get("summary") or base.summary,
                "chips": payload.get("chips") if isinstance(payload.get("chips"), list) else [],
                "constraints": constraints.model_dump(),
            }
        )
    except ValidationError:
        return None
    if not criteria.chips:
        criteria.chips = chips_for_constraints(criteria.category, criteria.constraints)
    if not criteria.summary:
        criteria.summary = "，".join(criteria.chips) if criteria.chips else f"{criteria.category}导购"
    return criteria


def _history_prompt(message: str, history: list[dict[str, Any]] | None, image_url: str | None) -> str:
    return json.dumps(
        {
            "message": message,
            "history": history or [],
            "image_url": image_url,
        },
        ensure_ascii=False,
    )


def _prompt_content(name: str, fallback: str, variables: dict[str, Any], schema_override: str) -> str:
    rendered = get_prompt_store().render(name, fallback, variables)
    if rendered == fallback:
        return fallback
    return f"{rendered}\n\n## Runtime Schema Override\n{schema_override}"
