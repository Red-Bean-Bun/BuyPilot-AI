"""Hard-rule slot checks for required shopping context."""

from __future__ import annotations

from src.types.schemas import IntentResult


def check_required_slots(message: str, intent: IntentResult) -> list[str]:
    if intent.intent not in {"recommend", "clarify"}:
        return []
    if not intent.category:
        return ["category"]
    return []


def build_clarification_question(missing_slots: list[str]) -> tuple[str, list[str]]:
    if "category" in missing_slots:
        return "你想买哪一类商品？", ["美妆护肤", "数码电子", "服饰运动", "食品饮料"]
    return "请补充一下你的购买需求。", []
