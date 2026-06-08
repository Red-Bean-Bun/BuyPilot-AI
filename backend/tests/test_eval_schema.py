"""Tests for eval schema normalization logic."""

import pytest

from src.repos import eval_samples


def test_eval_ground_truth_normalizes_legacy_constraints():
    normalized = eval_samples.normalize_ground_truth(
        {
            "intent_type": "recommend",
            "constraints": {
                "category": "美妆护肤",
                "max_price": 200,
                "use_case": "日常护肤",
                "must_have_features": ["洁面"],
                "forbidden_features": ["酒精"],
                "brands": ["华为"],
            },
        }
    )

    assert normalized["schema_version"] == eval_samples.GROUND_TRUTH_SCHEMA_VERSION
    assert normalized["constraints"]["budget_max"] == 200
    assert normalized["constraints"]["use_scenario"] == "日常护肤"
    assert normalized["constraints"]["must_match_terms"] == ["洁面"]
    assert normalized["constraints"]["forbidden_terms"] == ["酒精"]
    assert normalized["constraints"]["brand_prefer"] == ["华为"]


def test_eval_ground_truth_rejects_unknown_constraint_key():
    with pytest.raises(ValueError, match="Unsupported eval ground_truth constraints"):
        eval_samples.normalize_ground_truth({"constraints": {"unknown_key": "value"}})
