"""Hard-rule slot checks for required shopping context."""

from __future__ import annotations

from src.config.domain_terms import KNOWN_CATEGORIES, normalize_product_type
from src.types.schemas import IntentResult

# Constraints that indicate the user has already narrowed their search enough.
_NARROWING_CONSTRAINT_KEYS = frozenset(
    {"budget_max", "budget_min", "skin_type", "dietary", "use_scenario", "sport_type"}
)


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
    if category != "数码电子" or constraints.get("budget_max") is not None:
        return False
    normalized = normalize_product_type(str(product_type)) if product_type is not None else None
    return normalized == "智能手机"


def build_clarification_question(missing_slots: list[str]) -> tuple[str, list[str]]:
    if "category" in missing_slots:
        return "你想买哪一类商品？", ["美妆护肤", "数码电子", "服饰运动", "食品生活"]
    if "budget" in missing_slots:
        return "这类商品价格跨度比较大，你的预算或价位范围大概是多少？", [
            "1000-2000元",
            "2000-4000元",
            "4000元以上",
        ]
    if "product_type" in missing_slots:
        return "你想买具体哪一类商品？", [
            "洁面",
            "防晒",
            "面霜",
            "精华",
            "手机",
            "耳机",
            "跑鞋",
            "T恤",
            "咖啡",
            "茶饮",
            "零食",
        ]
    return "请补充一下你的购买需求。", []
