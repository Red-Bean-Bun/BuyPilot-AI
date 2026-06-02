"""Shared retrieval query, document, and scoring helpers."""

from __future__ import annotations

from src.config.domain_terms import normalize_category
from src.types.sse_events import CriteriaPayload, ProductPayload


def criteria_query_text(criteria: CriteriaPayload) -> str:
    constraints = criteria.constraints
    parts = [
        criteria.category,
        criteria.summary,
        constraints.skin_type or "",
        constraints.product_type or "",
        constraints.storage or "",
        constraints.screen_size or "",
        constraints.sport_type or "",
        constraints.season or "",
        constraints.use_scenario or "",
        " ".join(constraints.dietary),
        " ".join(constraints.ingredient_prefer),
        " ".join(constraints.brand_prefer),
        " ".join(f"不要{item}" for item in constraints.ingredient_avoid),
        f"{constraints.budget_max:g}元内" if constraints.budget_max is not None else "",
    ]
    return " ".join(part for part in parts if part)


def product_document_text(product: ProductPayload, extra_text: str = "") -> str:
    parts = [
        product.name,
        product.brand or "",
        product.category,
        product.sub_category or "",
        f"{product.price:g}元" if product.price is not None else "",
        " ".join(product.skin_type_match),
        " ".join(product.ingredient_tags),
        product.use_scenario or "",
        extra_text,
    ]
    return " | ".join(part for part in parts if part)


def _brand_in_summary(brand: str | None, summary: str) -> bool:
    """Check if a product brand name is mentioned in the criteria summary."""
    if not brand or not summary:
        return False
    if len(brand) < 2:
        return False
    return brand in summary


def product_match_score(
    criteria: CriteriaPayload,
    product: ProductPayload,
    *,
    category_weight: float,
    skin_type_weight: float,
    budget_weight: float,
    scenario_weight: float = 0.0,
    brand_weight: float = 0.0,
    brand_prefer_weight: float = 0.0,
) -> float:
    score = 0.0
    if normalize_category(product.category) == normalize_category(criteria.category):
        score += category_weight
    if criteria.constraints.skin_type and criteria.constraints.skin_type in product.skin_type_match:
        score += skin_type_weight
    if (
        criteria.constraints.budget_max is not None
        and product.price is not None
        and product.price <= criteria.constraints.budget_max
    ):
        score += budget_weight
    if (
        scenario_weight
        and criteria.constraints.use_scenario
        and product.use_scenario
        and criteria.constraints.use_scenario in product.use_scenario
    ):
        score += scenario_weight
    if brand_weight and _brand_in_summary(product.brand, criteria.summary):
        score += brand_weight
    if brand_prefer_weight and product.brand and product.brand in criteria.constraints.brand_prefer:
        score += brand_prefer_weight
    return score


def avoid_trait_penalty(criteria: CriteriaPayload, product: ProductPayload) -> bool:
    haystack = " ".join(product.ingredient_tags + product.ingredient_avoid)
    return any(item in haystack for item in criteria.constraints.ingredient_avoid)


def keyword_boost_score(product: ProductPayload, keywords: list[str]) -> float:
    """Additional score boost when a product matches user's explicit keywords.

    Each matching keyword adds a small boost. Multiple matches compound.
    """
    if not keywords:
        return 0.0
    haystack = " ".join(
        part
        for part in (product.name, product.brand or "", product.sub_category or "", product.category)
        if part
    )
    score = 0.0
    for kw in keywords:
        if kw in haystack:
            score += 1.5
    return score
