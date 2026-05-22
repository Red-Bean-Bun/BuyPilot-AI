"""Deterministic evaluation metrics that require no LLM calls."""

from __future__ import annotations

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
    checks = 0
    passed = 0
    for key, expected_value in expected_constraints.items():
        checks += 1
        actual_value = extracted_constraints.get(key)
        if key == "must_have_features" or key == "forbidden_features":
            actual_set = set(actual_value) if isinstance(actual_value, list) else set()
            expected_set = set(expected_value) if isinstance(expected_value, list) else set()
            if expected_set and expected_set.issubset(actual_set):
                passed += 1
            elif not expected_set:
                passed += 1
        elif key == "max_price":
            if actual_value is not None and isinstance(actual_value, (int, float)):
                passed += 1 if actual_value <= expected_value else 0
            else:
                passed += 0
        elif key == "min_price":
            if actual_value is not None and isinstance(actual_value, (int, float)):
                passed += 1 if actual_value >= expected_value else 0
            else:
                passed += 0
        else:
            if actual_value == expected_value:
                passed += 1
    return passed / checks if checks > 0 else 1.0


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
    checks: list[bool] = []
    if constraints.get("max_price"):
        checks.append(
            product_metadata.get("price", float("inf")) <= constraints["max_price"]
        )
    if constraints.get("category"):
        checks.append(
            product_metadata.get("category") == constraints["category"]
        )
    if constraints.get("forbidden_features"):
        product_text = str(product_metadata).lower()
        for feat in constraints["forbidden_features"]:
            checks.append(feat.lower() not in product_text)
    if constraints.get("must_have_features"):
        product_text = str(product_metadata).lower()
        for feat in constraints["must_have_features"]:
            checks.append(feat.lower() in product_text)
    if not checks:
        return 1.0
    return sum(checks) / len(checks)


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
