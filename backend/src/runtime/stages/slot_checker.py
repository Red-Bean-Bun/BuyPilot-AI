"""Hard-rule slot checks for required shopping context."""

from __future__ import annotations

from src.types.schemas import IntentResult


def check_required_slots(message: str, intent: IntentResult) -> list[str]:
    if intent.intent not in {"recommend", "clarify"}:
        return []
    missing: list[str] = []
    if not intent.category:
        missing.append("category")
    if not _has_scenario(message):
        missing.append("scenario")
    return missing


def build_clarification_question(missing_slots: list[str]) -> tuple[str, list[str]]:
    if "category" in missing_slots:
        return "你想买哪一类商品？", ["美妆护肤", "数码电子", "服饰运动", "食品生活"]
    if "scenario" in missing_slots:
        return "主要使用场景是什么？", ["日常使用", "送礼", "通勤", "户外"]
    return "请补充一下你的购买需求。", []


def _has_scenario(message: str) -> bool:
    return any(
        token in message
        for token in ("日常", "使用", "送", "通勤", "户外", "护肤", "洁面", "防晒", "跑步", "训练", "孩子", "预算")
    )

