"""Deterministic fallback rules for LLM tasks."""

from __future__ import annotations

import re

from src.config.domain_terms import (
    category_from_text as _category_from_text,
    extract_avoided_traits,
    first_skin_type,
    scenario_from_text as _scenario_from_text,
)
from src.types.sse_events import Constraints


def category_from_text(text: str) -> str | None:
    return _category_from_text(text)


def budget_from_text(text: str) -> float | None:
    match = re.search(r"预算\s*(?:降到|控制在|不超过|约|大概)?\s*(\d+(?:\.\d+)?)", text)
    if match:
        return float(match.group(1))
    match = re.search(r"(\d+(?:\.\d+)?)\s*元\s*(?:以内|以下|内)?", text)
    if match:
        return float(match.group(1))
    match = re.search(r"(\d+(?:\.\d+)?)\s*(?:以内|以下|内)", text)
    return float(match.group(1)) if match else None


def skin_type_from_text(text: str) -> str | None:
    return first_skin_type(text)


def ingredient_avoid_from_text(text: str) -> list[str]:
    return extract_avoided_traits(text)


def scenario_from_text(text: str) -> str | None:
    return _scenario_from_text(text, category=category_from_text(text))


def chips_for_constraints(category: str, constraints: Constraints) -> list[str]:
    chips = [category]
    if constraints.skin_type:
        chips.append(f"{constraints.skin_type}肌肤")
    if constraints.budget_max is not None:
        chips.append(f"{constraints.budget_max:g}元内")
    if constraints.use_scenario:
        chips.append(constraints.use_scenario)
    for item in constraints.ingredient_avoid:
        chips.append(f"不要{item}")
    return chips
