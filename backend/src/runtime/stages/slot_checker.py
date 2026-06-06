"""Hard-rule slot checks for required shopping context."""

from __future__ import annotations

from src.config import user_messages as msg
from src.config.domain_terms import KNOWN_CATEGORIES, normalize_product_type
from src.types.schemas import IntentResult

# Constraints that indicate the user has already narrowed their search enough.
_NARROWING_CONSTRAINT_KEYS = frozenset(
    {"budget_max", "budget_min", "skin_type", "dietary", "use_scenario", "sport_type"}
)
_PHONE_PREFERENCE_OPTIONS = ["拍照", "续航", "性价比", "游戏"]
_DIGITAL_PREFERENCE_OPTIONS = ["音质", "续航", "轻便", "性价比"]
_SKIN_TYPE_OPTIONS = ["油性", "干性", "混合", "敏感"]
_SPORT_TYPE_OPTIONS = ["跑步", "健身", "户外", "日常通勤"]

_PHONE_TYPES = frozenset({"智能手机"})
_DIGITAL_GENERIC_TYPES = frozenset({"耳机", "平板电脑"})
_SKINCARE_TYPES = frozenset({"面霜", "精华"})
_SPORT_TYPES = frozenset({"跑步鞋", "运动服"})
_PHONE_TERMS = ("手机", "智能手机")
_DIGITAL_GENERIC_TERMS = ("耳机", "平板")
_SKINCARE_GENERIC_TERMS = ("护肤", "面霜", "精华")
_SPORT_GENERIC_TERMS = ("跑鞋", "运动服", "运动")


# Categories whose sub-types have high price variance, making budget a required
# slot before retrieval.  Map: category -> set of normalised product_types that
# trigger the budget clarification.  Add new entries here instead of branching.
_BUDGET_REQUIRED: dict[str, frozenset[str]] = {
    "数码电子": frozenset({"智能手机"}),
}


def check_required_slots(message: str, intent: IntentResult) -> list[str]:
    """Return missing required slots. Only clarifies when truly missing key entry info.

    Product-first: if the user has given a category or any narrowing constraint,
    don't block — go straight to broad candidates and let criteria_card guide narrowing.
    """
    if intent.intent not in {"recommend", "clarify"}:
        return []
    missing: list[str] = []
    if not intent.category:
        missing.append("category")
        return missing
    if intent.category in KNOWN_CATEGORIES:
        product_type = (intent.extracted_constraints or {}).get("product_type")
        # Existing P0 acceptance treats smartphone budget as the blocking slot.
        # Keep that guard before softer preference clarifications.
        if _needs_budget(intent.category, product_type, intent.extracted_constraints or {}):
            missing.append("budget")
            return missing
        preference_slot = _missing_preference_slot(
            message, intent.category, product_type, intent.extracted_constraints or {}
        )
        if preference_slot:
            missing.append(preference_slot)
        # Product-first: no longer clarify for missing product_type —
        # broad candidates + criteria_card will do the narrowing
    return missing


def _has_narrowing_constraints(constraints: dict) -> bool:
    """True when the user has already provided enough detail to narrow the search."""
    for key in _NARROWING_CONSTRAINT_KEYS:
        value = constraints.get(key)
        if value not in (None, "", []):
            return True
    return False


def _needs_budget(category: str, product_type: object, constraints: dict) -> bool:
    required_types = _BUDGET_REQUIRED.get(category)
    if required_types is None or constraints.get("budget_max") is not None:
        return False
    normalized = normalize_product_type(str(product_type)) if product_type is not None else None
    return normalized in required_types


def _missing_preference_slot(message: str, category: str, product_type: object, constraints: dict) -> str | None:
    normalized = normalize_product_type(str(product_type)) if product_type is not None else None
    if category == "数码电子":
        if _matches_product_request(message, normalized, _PHONE_TYPES, _PHONE_TERMS):
            # Smartphone requests are covered by the budget guard above and
            # otherwise remain product-first to preserve existing acceptance.
            return None
        # Product-first: generic digital queries (耳机, 平板) go straight to
        # recommend — broad candidates + criteria_card handle narrowing.
        if _matches_product_request(message, normalized, _DIGITAL_GENERIC_TYPES, _DIGITAL_GENERIC_TERMS):
            return None
    # Product-first: generic skincare (面霜, 精华) and sport (跑鞋, 运动服)
    # queries also go straight to recommend without preference clarification.
    if category == "美妆护肤" and _matches_product_request(message, normalized, _SKINCARE_TYPES, _SKINCARE_GENERIC_TERMS):
        return None
    if category == "服饰运动" and _matches_product_request(message, normalized, _SPORT_TYPES, _SPORT_GENERIC_TERMS):
        # Running shoes / sportswear are accepted product-first in existing
        # regression tests; do not block them with a soft clarification.
        return None
    return None


def _matches_product_request(
    message: str,
    normalized_product_type: str | None,
    product_types: frozenset[str],
    message_terms: tuple[str, ...],
) -> bool:
    return bool(
        (normalized_product_type and normalized_product_type in product_types)
        or _has_any_term(message, message_terms)
    )


def _has_preference_signal(message: str, constraints: dict, options: list[str]) -> bool:
    value = constraints.get("preference_dimension") or constraints.get("use_scenario")
    if isinstance(value, str) and any(option in value for option in options):
        return True
    return _has_any_term(message, tuple(options))


def _has_any_term(message: str, terms: tuple[str, ...] | list[str]) -> bool:
    return any(term in message for term in terms)


def build_clarification_question(missing_slots: list[str]) -> tuple[str, list[str]]:
    if "category" in missing_slots:
        return msg.CLARIFY_CATEGORY_QUESTION, list(msg.CATEGORY_DISPLAY_ORDER)
    if "budget" in missing_slots:
        return msg.CLARIFY_BUDGET_QUESTION, list(msg.CLARIFY_BUDGET_OPTIONS)
    if "preference_dimension_digital" in missing_slots:
        return "请问您更看重哪个方面？", list(_DIGITAL_PREFERENCE_OPTIONS)
    if "preference_dimension" in missing_slots:
        return "请问您更看重哪个方面？", list(_PHONE_PREFERENCE_OPTIONS)
    if "skin_type" in missing_slots:
        return "请问您的肤质更接近哪一种？", list(_SKIN_TYPE_OPTIONS)
    if "sport_type" in missing_slots:
        return "主要用于哪类运动或穿着场景？", list(_SPORT_TYPE_OPTIONS)
    if "product_type" in missing_slots:
        return msg.CLARIFY_PRODUCT_TYPE_QUESTION, list(msg.CLARIFY_PRODUCT_TYPE_OPTIONS)
    return msg.CLARIFY_FALLBACK_QUESTION, []
