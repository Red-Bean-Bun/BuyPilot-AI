"""Shared criteria sanitization helpers."""

from __future__ import annotations

from collections.abc import Callable, MutableMapping
from typing import Any

from src.config.domain_terms import normalize_product_type_for_category
from src.types.sse_events import Constraints, CriteriaPayload

CriteriaChipBuilder = Callable[[str, Constraints], list[str]]


def sanitize_product_type_constraint(
    constraints: MutableMapping[str, Any],
    category: object,
    *,
    allow_unknown: bool = False,
    clear_invalid: bool = False,
) -> None:
    """Normalize or remove the product_type constraint in-place."""

    if "product_type" not in constraints:
        return
    product_type = normalize_product_type_for_category(
        constraints.get("product_type"),
        category,
        allow_unknown=allow_unknown,
    )
    if product_type:
        constraints["product_type"] = product_type
    elif clear_invalid:
        constraints["product_type"] = None
    else:
        constraints.pop("product_type", None)


def sanitize_criteria_product_type(
    criteria: CriteriaPayload,
    *,
    chips_for_constraints: CriteriaChipBuilder,
) -> CriteriaPayload:
    product_type = normalize_product_type_for_category(criteria.constraints.product_type, criteria.category)
    constraints = (
        criteria.constraints
        if product_type == criteria.constraints.product_type
        else criteria.constraints.model_copy(update={"product_type": product_type})
    )
    chips = chips_for_constraints(criteria.category, constraints)
    return criteria.model_copy(update={"constraints": constraints, "chips": chips, "summary": "，".join(chips)})
