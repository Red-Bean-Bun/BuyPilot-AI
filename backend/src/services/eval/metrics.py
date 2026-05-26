"""Deterministic evaluation metrics that require no LLM calls."""

from __future__ import annotations

from collections.abc import Callable
from typing import Any


def compute_intent_accuracy(actual_intent: str, expected_intent: str) -> float:
    """Binary accuracy of intent classification."""
    return 1.0 if actual_intent == expected_intent else 0.0


def compute_constraint_extraction_accuracy(
    extracted_constraints: dict[str, Any],
    expected_constraints: dict[str, Any],
) -> float:
    """Fraction of expected constraint fields correctly identified by the LLM.

    Compare field-by-field on the enumerated Constraints schema. Missing fields
    and value mismatches both count against accuracy.
    """
    if not expected_constraints:
        return 1.0
    passed = sum(
        1
        for key, expected_value in expected_constraints.items()
        if _constraint_matches(key, extracted_constraints.get(key), expected_value)
    )
    return passed / len(expected_constraints)


ConstraintComparator = Callable[[Any, Any], bool]


def _constraint_matches(key: str, actual_value: Any, expected_value: Any) -> bool:
    comparator = CONSTRAINT_COMPARATORS.get(key, _equals)
    return comparator(actual_value, expected_value)


def _expected_subset(actual_value: Any, expected_value: Any) -> bool:
    actual_set = set(actual_value) if isinstance(actual_value, list) else set()
    expected_set = set(expected_value) if isinstance(expected_value, list) else set()
    return not expected_set or expected_set.issubset(actual_set)


def _max_price_match(actual_value: Any, expected_value: Any) -> bool:
    return isinstance(actual_value, int | float) and actual_value <= expected_value


def _min_price_match(actual_value: Any, expected_value: Any) -> bool:
    return isinstance(actual_value, int | float) and actual_value >= expected_value


def _equals(actual_value: Any, expected_value: Any) -> bool:
    return actual_value == expected_value


CONSTRAINT_COMPARATORS: dict[str, ConstraintComparator] = {
    "must_have_features": _expected_subset,
    "forbidden_features": _expected_subset,
    "max_price": _max_price_match,
    "min_price": _min_price_match,
}


def compute_criteria_coverage(
    generated_chips: list[str],
    expected_chips: list[str],
) -> float:
    """Fraction of expected criteria chips present in the generated output."""
    if not expected_chips:
        return 1.0
    generated_set = set(c.lower() for c in generated_chips)
    hits = sum(1 for e in expected_chips if e.lower() in generated_set)
    return hits / len(expected_chips)


def compute_recall_at_k(
    retrieved_product_ids: list[str],
    relevant_product_ids: list[str],
    k: int = 10,
) -> float:
    """Recall@K: fraction of relevant products that appear in top-K retrieved."""
    if not relevant_product_ids:
        return 1.0
    retrieved_k = set(retrieved_product_ids[:k])
    relevant = set(relevant_product_ids)
    return len(retrieved_k & relevant) / len(relevant)


def compute_evidence_coverage(
    products_with_evidence: int,
    total_products: int,
) -> float:
    """Fraction of recommended products that have at least one evidence link."""
    if total_products == 0:
        return 0.0
    return products_with_evidence / total_products


def compute_constraint_satisfaction(
    product_metadata: dict[str, Any],
    constraints: dict[str, Any],
) -> float:
    """Per-product: fraction of hard constraints satisfied."""
    checks = [result for rule in SATISFACTION_RULES for result in rule(product_metadata, constraints)]
    if not checks:
        return 1.0
    return sum(checks) / len(checks)


SatisfactionRule = Callable[[dict[str, Any], dict[str, Any]], list[bool]]


def _price_satisfaction(product_metadata: dict[str, Any], constraints: dict[str, Any]) -> list[bool]:
    if not constraints.get("max_price"):
        return []
    return [product_metadata.get("price", float("inf")) <= constraints["max_price"]]


def _category_satisfaction(product_metadata: dict[str, Any], constraints: dict[str, Any]) -> list[bool]:
    if not constraints.get("category"):
        return []
    return [product_metadata.get("category") == constraints["category"]]


def _forbidden_feature_satisfaction(product_metadata: dict[str, Any], constraints: dict[str, Any]) -> list[bool]:
    features = constraints.get("forbidden_features")
    if not features:
        return []
    product_text = str(product_metadata).lower()
    return [feat.lower() not in product_text for feat in features]


def _must_have_feature_satisfaction(product_metadata: dict[str, Any], constraints: dict[str, Any]) -> list[bool]:
    features = constraints.get("must_have_features")
    if not features:
        return []
    product_text = str(product_metadata).lower()
    return [feat.lower() in product_text for feat in features]


SATISFACTION_RULES: tuple[SatisfactionRule, ...] = (
    _price_satisfaction,
    _category_satisfaction,
    _forbidden_feature_satisfaction,
    _must_have_feature_satisfaction,
)


def compute_feedback_change_rate(
    before_product_ids: list[str],
    after_product_ids: list[str],
) -> float:
    """Jaccard distance between pre-feedback and post-feedback recommendation lists."""
    if not before_product_ids and not after_product_ids:
        return 0.0
    before_set = set(before_product_ids)
    after_set = set(after_product_ids)
    intersection = len(before_set & after_set)
    union = len(before_set | after_set)
    if union == 0:
        return 0.0
    return 1.0 - (intersection / union)
