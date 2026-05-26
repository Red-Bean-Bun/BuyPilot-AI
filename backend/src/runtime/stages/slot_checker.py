"""Hard-rule slot checks for required shopping context."""

from __future__ import annotations

from src.types.schemas import IntentResult

_KNOWN_CATEGORIES = frozenset({"美妆护肤", "数码电子", "服饰运动", "食品饮料"})

# Constraints that indicate the user has already narrowed their search enough.
_NARROWING_CONSTRAINT_KEYS = frozenset(
    {"budget_max", "budget_min", "skin_type", "dietary", "use_scenario", "sport_type"}
)


def check_required_slots(message: str, intent: IntentResult) -> list[str]:
    if intent.intent not in {"recommend", "clarify"}:
        return []
    missing: list[str] = []
    if not intent.category:
        missing.append("category")
        return missing
    if intent.category in _KNOWN_CATEGORIES:
        product_type = (intent.extracted_constraints or {}).get("product_type")
        if not product_type and not _has_narrowing_constraints(intent.extracted_constraints or {}):
            missing.append("product_type")
    return missing


def _has_narrowing_constraints(constraints: dict) -> bool:
    """True when the user has already provided enough detail to narrow the search."""
    return bool(_NARROWING_CONSTRAINT_KEYS & set(constraints))


def build_clarification_question(missing_slots: list[str]) -> tuple[str, list[str]]:
    if "category" in missing_slots:
        return "你想买哪一类商品？", ["美妆护肤", "数码电子", "服饰运动", "食品饮料"]
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
