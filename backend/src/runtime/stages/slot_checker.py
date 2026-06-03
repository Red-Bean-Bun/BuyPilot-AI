"""Hard-rule slot checks for required shopping context."""

from __future__ import annotations

from src.config import user_messages as msg
from src.config.domain_terms import KNOWN_CATEGORIES, normalize_product_type
from src.types.schemas import IntentResult

# Constraints that indicate the user has already narrowed their search enough.
_NARROWING_CONSTRAINT_KEYS = frozenset(
    {"budget_max", "budget_min", "skin_type", "dietary", "use_scenario", "sport_type"}
)


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
        # Only clarify budget for high-variance digital categories with no price hint
        if _needs_budget(intent.category, product_type, intent.extracted_constraints or {}):
            missing.append("budget")
        # Product-first: no longer clarify for missing product_type —
        # broad candidates + criteria_card will do the narrowing
    return missing


def _has_narrowing_constraints(constraints: dict) -> bool:
    """True when the user has already provided enough detail to narrow the search."""
    return bool(_NARROWING_CONSTRAINT_KEYS & set(constraints))


def _needs_budget(category: str, product_type: object, constraints: dict) -> bool:
    required_types = _BUDGET_REQUIRED.get(category)
    if required_types is None or constraints.get("budget_max") is not None:
        return False
    normalized = normalize_product_type(str(product_type)) if product_type is not None else None
    return normalized in required_types


def build_clarification_question(missing_slots: list[str]) -> tuple[str, list[str]]:
    if "category" in missing_slots:
        return msg.CLARIFY_CATEGORY_QUESTION, list(msg.CATEGORY_DISPLAY_ORDER)
    if "budget" in missing_slots:
        return msg.CLARIFY_BUDGET_QUESTION, list(msg.CLARIFY_BUDGET_OPTIONS)
    if "product_type" in missing_slots:
        return msg.CLARIFY_PRODUCT_TYPE_QUESTION, list(msg.CLARIFY_PRODUCT_TYPE_OPTIONS)
    return msg.CLARIFY_FALLBACK_QUESTION, []
