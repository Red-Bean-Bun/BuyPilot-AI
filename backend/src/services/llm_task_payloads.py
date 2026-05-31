"""Prompt and response helpers for task-oriented LLM calls."""

from __future__ import annotations

import json
import re
from typing import Any

from pydantic import ValidationError

from src.config.domain_terms import (
    infer_category_from_product_type,
    normalize_category,
    normalize_product_type,
)
from src.config.tuning import DEFAULT_CRITERIA_ID
from src.services.criteria_sanitizer import sanitize_criteria_product_type, sanitize_product_type_constraint
from src.services.prompts import get_prompt_store
from src.types.sse_events import Constraints, CriteriaPayload, EvidencePayload, ProductPayload, ReasonAtomPayload

# ── Schema overrides loaded from prompts/schema_overrides.md ────────────
# Each task maps to a ``## section`` heading in that file.

_SCHEMA_TASK_NAMES = {
    "analyze_intent": "analyze_intent",
    "generate_criteria": "generate_criteria",
    "generate_recommendation": "generate_recommendation",
    "generate_recommendation_stream": "generate_recommendation_stream",
    "generate_decision": "generate_decision",
    "analyze_image": "analyze_image",
}

_SCHEMA_FILE = "schema_overrides"


def _schema_override(task: str) -> str:
    """Load the runtime schema override for *task* from schema_overrides.md."""
    section = _SCHEMA_TASK_NAMES.get(task, task)
    loaded = get_prompt_store().load_section(_SCHEMA_FILE, section)
    if loaded:
        return loaded
    # Fallback: should not happen in production, but keeps tests green
    return f"(schema override for {task} not found)"


CONFIDENCE_LABELS = {
    "high": 0.9,
    "medium": 0.7,
    "low": 0.5,
    "高": 0.9,
    "中": 0.7,
    "低": 0.5,
}


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
                {
                    "user_message": message,
                    "history": history or [],
                    "conversation_context": conversation_context,
                },
                _schema_override("analyze_intent"),
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
                {
                    "user_message": message,
                    "intent_result": intent_dump,
                    "history": [],
                    "feedback_constraints": feedback or {},
                    "existing": existing_dump,
                    "conversation_context": conversation_context,
                },
                _schema_override("generate_criteria"),
            ),
        },
        {"role": "user", "content": json.dumps(payload, ensure_ascii=False)},
    ]


def recommendation_messages(
    criteria: CriteriaPayload,
    products: list[ProductPayload],
    evidence_by_product: dict[str, list[EvidencePayload]] | None = None,
    reason_atoms_by_product: dict[str, list[ReasonAtomPayload]] | None = None,
) -> list[dict[str, Any]]:
    payload = {
        "criteria": criteria.model_dump(),
        "products": [product.model_dump() for product in products],
        "reason_atoms_by_product": {
            product_id: [atom.model_dump() for atom in atoms]
            for product_id, atoms in (reason_atoms_by_product or {}).items()
        },
    }
    return [
        {
            "role": "system",
            "content": _prompt_content(
                "recommendation",
                {
                    "criteria": criteria.model_dump(),
                    "ranked_products": [product.model_dump() for product in products],
                    "evidence_chunks": _format_evidence_context(evidence_by_product or {}),
                    "reason_atoms_by_product": {
                        product_id: [atom.model_dump() for atom in atoms]
                        for product_id, atoms in (reason_atoms_by_product or {}).items()
                    },
                },
                _schema_override("generate_recommendation"),
            ),
        },
        {"role": "user", "content": json.dumps(payload, ensure_ascii=False)},
    ]


def recommendation_stream_messages(
    criteria: CriteriaPayload,
    products: list[ProductPayload],
    evidence_by_product: dict[str, list[EvidencePayload]] | None = None,
    reason_atoms_by_product: dict[str, list[ReasonAtomPayload]] | None = None,
) -> list[dict[str, Any]]:
    payload = {
        "criteria": criteria.model_dump(),
        "products": [product.model_dump() for product in products],
        "reason_atoms_by_product": {
            product_id: [atom.model_dump() for atom in atoms]
            for product_id, atoms in (reason_atoms_by_product or {}).items()
        },
    }
    return [
        {
            "role": "system",
            "content": _prompt_content(
                "recommendation_stream",
                {
                    "criteria": criteria.model_dump(),
                    "ranked_products": [product.model_dump() for product in products],
                    "evidence_chunks": _format_evidence_context(evidence_by_product or {}),
                    "reason_atoms_by_product": {
                        product_id: [atom.model_dump() for atom in atoms]
                        for product_id, atoms in (reason_atoms_by_product or {}).items()
                    },
                },
                _schema_override("generate_recommendation_stream"),
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
                {"image_url": image_url},
                _schema_override("analyze_image"),
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


def decision_messages(
    criteria: CriteriaPayload,
    products: list[ProductPayload],
    evidence_by_product: dict[str, list[EvidencePayload]] | None = None,
    locked_winner_product_id: str | None = None,
    score_breakdown: dict[str, Any] | None = None,
) -> list[dict[str, Any]]:
    payload = {
        "criteria": criteria.model_dump(),
        "products": [product.model_dump() for product in products],
        "valid_winner_ids": [product.product_id for product in products],
    }
    prompt_vars = {
        "criteria": criteria.model_dump(),
        "recommendations": [product.model_dump() for product in products],
        "feedback_history": [],
        "evidence_context": _format_evidence_context(evidence_by_product or {}),
    }
    if locked_winner_product_id is not None:
        payload["locked_winner_product_id"] = locked_winner_product_id
        prompt_vars["locked_winner_product_id"] = locked_winner_product_id
    if score_breakdown:
        payload["score_breakdown"] = score_breakdown
        prompt_vars["score_breakdown"] = score_breakdown
    return [
        {
            "role": "system",
            "content": _prompt_content(
                "decision",
                prompt_vars,
                _schema_override("generate_decision"),
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
    normalized["intent"] = _normalize_intent(
        normalized.get("intent") if normalized.get("intent") is not None else normalized.get("intent_type"),
        normalized.get("is_shopping_related"),
    )
    normalized["confidence"] = _normalize_confidence(normalized.get("confidence"))
    normalized["category"] = normalize_category(normalized.get("category"))

    constraints = normalized.get("extracted_constraints")
    if constraints is None and isinstance(normalized.get("constraints"), dict):
        constraints = normalized["constraints"]
    normalized["extracted_constraints"] = _normalize_intent_constraints(
        constraints if isinstance(constraints, dict) else {}
    )
    if normalized["category"]:
        sanitize_product_type_constraint(
            normalized["extracted_constraints"],
            normalized["category"],
            allow_unknown=True,
        )
    else:
        normalized["category"] = infer_category_from_product_type(
            normalized["extracted_constraints"].get("product_type")
        )

    preferences = normalized.get("soft_preferences")
    if preferences is None and isinstance(normalized.get("user_intent_summary"), str):
        preferences = normalized["user_intent_summary"]
    normalized["soft_preferences"] = _normalize_string_list(preferences)

    target_product_id = normalized.get("target_product_id")
    if target_product_id is None:
        target_product_id = normalized.get("product_id") or normalized.get("target_product")
    normalized["target_product_id"] = _normalize_nullable_string(target_product_id)
    return normalized


def _normalize_intent(value: Any, is_shopping_related: Any) -> str:
    if isinstance(value, str):
        key = value.strip().lower()
        normalized = {
            "recommend": "recommend",
            "recommendation": "recommend",
            "recommend_product": "recommend",
            "shopping": "recommend",
            "filter": "recommend",
            "compare": "recommend",
            "clarify": "clarify",
            "unclear": "clarify",
            "question": "clarify",
            "continue": "continue",
            "confirm": "continue",
            "confirmed": "continue",
            "proceed": "continue",
            "add_to_cart": "add_to_cart",
            "add_cart": "add_to_cart",
            "cart_add": "add_to_cart",
            "remove_from_cart": "remove_from_cart",
            "remove_cart": "remove_from_cart",
            "cart_remove": "remove_from_cart",
            "delete_from_cart": "remove_from_cart",
            "delete_cart": "remove_from_cart",
            "update_cart_quantity": "update_cart_quantity",
            "update_quantity": "update_cart_quantity",
            "cart_update": "update_cart_quantity",
            "set_cart_quantity": "update_cart_quantity",
            "view_cart": "view_cart",
            "cart_view": "view_cart",
            "feedback": "feedback",
            "dislike": "feedback",
            "not_interested": "feedback",
            "chitchat": "chitchat",
            "chat": "chitchat",
            "non_shopping": "chitchat",
            "推荐": "recommend",
            "筛选": "recommend",
            "过滤": "recommend",
            "对比": "recommend",
            "澄清": "clarify",
            "追问": "clarify",
            "继续": "continue",
            "确认": "continue",
            "没问题": "continue",
            "可以": "continue",
            "开始推荐": "continue",
            "收敛": "continue",
            "加购": "add_to_cart",
            "加入购物车": "add_to_cart",
            "移出购物车": "remove_from_cart",
            "从购物车删除": "remove_from_cart",
            "删除购物车": "remove_from_cart",
            "改数量": "update_cart_quantity",
            "修改数量": "update_cart_quantity",
            "查看购物车": "view_cart",
            "反馈": "feedback",
            "不喜欢": "feedback",
            "闲聊": "chitchat",
        }.get(key)
        if normalized:
            return normalized
    return "recommend" if _normalize_bool(is_shopping_related, default=True) else "chitchat"


def _normalize_confidence(value: Any) -> float:
    if value is None:
        return 1.0
    if isinstance(value, bool):
        return 1.0
    if isinstance(value, int | float):
        confidence = float(value)
    elif isinstance(value, str):
        confidence = _confidence_from_string(value)
    else:
        confidence = 1.0
    if confidence > 1 and confidence <= 100:
        confidence = confidence / 100
    return max(0.0, min(1.0, confidence))


def _confidence_from_string(value: str) -> float:
    key = value.strip().lower()
    if key.endswith("%"):
        return _parse_float(key[:-1].strip(), default=100.0) / 100
    if key in CONFIDENCE_LABELS:
        return CONFIDENCE_LABELS[key]
    return _parse_float(key, default=1.0)


def _parse_float(value: str, *, default: float) -> float:
    try:
        return float(value)
    except ValueError:
        return default


def _normalize_nullable_string(value: Any) -> str | None:
    if value is None:
        return None
    if isinstance(value, int | float):
        return str(value)
    if not isinstance(value, str):
        return None
    stripped = value.strip()
    if not stripped or stripped.lower() in {"null", "none", "nil", "n/a", "unknown", "未识别", "无"}:
        return None
    return stripped


def _normalize_string_list(value: Any) -> list[str]:
    if value is None:
        return []
    if isinstance(value, str):
        stripped = value.strip()
        return [stripped] if stripped else []
    if isinstance(value, list):
        return [item.strip() for item in value if isinstance(item, str) and item.strip()]
    return []


def _normalize_bool(value: Any, *, default: bool) -> bool:
    if isinstance(value, bool):
        return value
    if isinstance(value, str):
        key = value.strip().lower()
        if key in {"true", "yes", "y", "1", "是", "购物", "相关"}:
            return True
        if key in {"false", "no", "n", "0", "否", "非购物", "不相关"}:
            return False
    return default


def criteria_from_live_payload(
    payload: dict[str, Any] | None, existing: CriteriaPayload | None
) -> CriteriaPayload | None:
    if not payload:
        return None
    base = existing.model_copy(deep=True) if existing else CriteriaPayload()
    raw_constraints_obj = payload.get("constraints")
    raw_constraints: dict[str, Any] = raw_constraints_obj if isinstance(raw_constraints_obj, dict) else {}
    if "product_type" in raw_constraints:
        raw_constraints = {
            **raw_constraints,
            "product_type": normalize_product_type(_normalize_nullable_string(raw_constraints.get("product_type"))),
        }
    # ── Defensive sanitization: strip unknown fields & coerce types ──
    raw_constraints = _sanitize_constraints(raw_constraints)
    category = (
        normalize_category(payload.get("category"))
        or infer_category_from_product_type(raw_constraints.get("product_type"))
        or base.category
    )
    if not category:
        return None
    sanitize_product_type_constraint(raw_constraints, category)
    # Category switch: old constraints are semantically irrelevant to the new
    # category (e.g. dietary=["坚果","零食"] has no meaning for 数码电子).
    # Start from clean constraints to prevent cross-category field leakage.
    category_switched = existing is not None and category != existing.category
    base_constraints: dict[str, Any] = {} if category_switched else base.constraints.model_dump()
    try:
        constraints = Constraints.model_validate(
            {
                **base_constraints,
                **raw_constraints,
            }
        )
        criteria = CriteriaPayload.model_validate(
            {
                "criteria_id": payload.get("criteria_id")
                or (base.criteria_id if not category_switched else DEFAULT_CRITERIA_ID),
                "category": category,
                "summary": payload.get("summary") or ("" if category_switched else base.summary),
                "chips": payload.get("chips") if isinstance(payload.get("chips"), list) else [],
                "constraints": constraints.model_dump(),
            }
        )
    except ValidationError:
        return None
    criteria = sanitize_criteria_product_type(criteria, chips_for_constraints=_chips_for_constraints)
    if not criteria.chips:
        criteria.chips = _chips_for_constraints(criteria.category, criteria.constraints)
    if not criteria.summary:
        criteria.summary = "，".join(criteria.chips) if criteria.chips else f"{criteria.category}导购"
    return criteria


def _sanitize_constraints(raw: dict[str, Any]) -> dict[str, Any]:
    """Filter unknown fields and coerce common LLM type errors before Pydantic validation.

    The LLM occasionally returns field values with the wrong type (e.g. string
    budget, empty-string enums) or invents fields outside the Constraints schema.
    Without this sanitization these would raise ``ValidationError`` and cause the
    entire criteria generation to fail.
    """
    from src.types.sse_events import Constraints

    allowed = set(Constraints.model_fields)
    numeric = {"budget_min", "budget_max"}
    list_fields = {"brand_avoid", "origin_avoid", "ingredient_avoid", "ingredient_prefer", "dietary"}
    cleaned: dict[str, Any] = {}
    for key, value in raw.items():
        if key not in allowed:
            continue
        # Drop Pydantic-empty values that aren't valid for the field type
        if value is None or value == "" or value == []:
            continue
        if key in numeric and isinstance(value, str):
            try:
                cleaned[key] = float(value)
            except (ValueError, TypeError):
                continue  # drop unparseable budget values
        elif key in list_fields and isinstance(value, str):
            cleaned[key] = [value]
        elif key in list_fields and isinstance(value, list):
            coerced = [str(v) for v in value if v]
            if coerced:  # don't let a degenerate list override a valid base value
                cleaned[key] = coerced
        else:
            cleaned[key] = value
    return cleaned


def _normalize_intent_constraints(constraints: dict[str, Any]) -> dict[str, Any]:
    normalized = dict(constraints)
    if "product_type" in normalized:
        normalized["product_type"] = normalize_product_type(_normalize_nullable_string(normalized.get("product_type")))
    return normalized


def _format_evidence_context(evidence_by_product: dict[str, list[EvidencePayload]]) -> str:
    if not evidence_by_product:
        return "无商品证据片段。"
    lines: list[str] = []
    for product_id, pieces in evidence_by_product.items():
        for piece in pieces[:3]:
            snippet = piece.snippet[:150]
            source_type = piece.source_type or "证据"
            lines.append(f"- [{product_id}] ({source_type}) {snippet}")
    return "\n".join(lines) if lines else "无商品证据片段。"


def _history_prompt(message: str, history: list[dict[str, Any]] | None, image_url: str | None) -> str:
    return json.dumps(
        {
            "message": message,
            "history": history or [],
            "image_url": image_url,
        },
        ensure_ascii=False,
    )


def _chips_for_constraints(category: str, constraints: Constraints) -> list[str]:
    chips = [category]
    if constraints.skin_type:
        chips.append(f"{constraints.skin_type}肌肤")
    if constraints.budget_max is not None:
        chips.append(f"{constraints.budget_max:g}元内")
    if constraints.use_scenario:
        chips.append(constraints.use_scenario)
    for item in constraints.ingredient_avoid:
        chips.append(f"不要{item}")
    for item in constraints.brand_avoid:
        chips.append(f"不要{item}")
    for item in constraints.origin_avoid:
        chips.append(f"不要{item}")
    if constraints.product_type:
        chips.append(constraints.product_type)
    return chips


def _prompt_content(name: str, variables: dict[str, Any], schema_override: str) -> str:
    rendered = get_prompt_store().render(name, variables)
    return f"{rendered}\n\n## Runtime Schema Override\n{schema_override}"
