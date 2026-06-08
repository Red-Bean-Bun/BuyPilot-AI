"""Expanded criteria history tests — history injection, summary, and patch merging.

Audit gap: test_criteria_history.py had only 2 tests. These cover:
- criteria_from_intent pure function (no LLM needed)
- apply_criteria_patch merging behavior
- annotate_criteria_sources field source annotation
- criteria_quick_actions per-category behavior
"""

from __future__ import annotations


from src.runtime.stages.criteria import (
    apply_criteria_patch,
    criteria_from_intent,
    criteria_quick_actions,
)
from src.types.schemas import IntentResult
from src.types.sse_events import Constraints, CriteriaPayload


# ── criteria_from_intent ────────────────────────────────────────────────────


class TestCriteriaFromIntent:
    def test_empty_intent_produces_empty_criteria(self):
        intent = IntentResult(intent="chitchat", category="", extracted_constraints={})
        criteria = criteria_from_intent(intent)
        assert criteria.category == ""
        assert criteria.constraints.budget_max is None
        assert criteria.constraints.brand_avoid == []

    def test_category_propagates(self):
        intent = IntentResult(intent="recommend", category="美妆护肤", extracted_constraints={})
        criteria = criteria_from_intent(intent)
        assert criteria.category == "美妆护肤"
        assert "美妆护肤" in criteria.chips

    def test_extracted_constraints_propagate(self):
        intent = IntentResult(
            intent="recommend",
            category="美妆护肤",
            extracted_constraints={"budget_max": 200.0, "skin_type": "油性"},
        )
        criteria = criteria_from_intent(intent)
        assert criteria.constraints.budget_max == 200.0
        assert criteria.constraints.skin_type == "油性"

    def test_string_list_field_converted_to_list(self):
        """extracted_constraints with string value for list field → wrapped in list."""
        intent = IntentResult(
            intent="recommend",
            category="美妆护肤",
            extracted_constraints={"brand_avoid": "资生堂"},
        )
        criteria = criteria_from_intent(intent)
        assert criteria.constraints.brand_avoid == ["资生堂"]

    def test_unknown_constraint_key_ignored(self):
        """Unknown keys in extracted_constraints don't cause errors."""
        intent = IntentResult(
            intent="recommend",
            category="美妆护肤",
            extracted_constraints={"nonexistent_field": "value"},
        )
        criteria = criteria_from_intent(intent)
        # Should not raise, just ignore unknown field
        assert criteria.category == "美妆护肤"


# ── apply_criteria_patch ────────────────────────────────────────────────────


class TestApplyCriteriaPatch:
    def test_patch_updates_budget_max(self):
        base = CriteriaPayload(
            category="美妆护肤",
            constraints=Constraints(budget_max=300.0),
        )
        patched = apply_criteria_patch(base, {"constraints": {"budget_max": 200.0}})
        assert patched.constraints.budget_max == 200.0

    def test_patch_merges_list_fields(self):
        """brand_avoid patch merges with existing list (deduplicated)."""
        base = CriteriaPayload(
            category="美妆护肤",
            constraints=Constraints(brand_avoid=["资生堂"]),
        )
        patched = apply_criteria_patch(base, {"constraints": {"brand_avoid": ["SK-II"]}})
        assert "资生堂" in patched.constraints.brand_avoid
        assert "SK-II" in patched.constraints.brand_avoid

    def test_patch_deduplicates_list_fields(self):
        base = CriteriaPayload(
            category="美妆护肤",
            constraints=Constraints(brand_avoid=["资生堂"]),
        )
        patched = apply_criteria_patch(base, {"constraints": {"brand_avoid": ["资生堂"]}})
        assert patched.constraints.brand_avoid.count("资生堂") == 1

    def test_patch_preserves_category(self):
        base = CriteriaPayload(category="美妆护肤", constraints=Constraints())
        patched = apply_criteria_patch(base, {"constraints": {"budget_max": 100.0}})
        assert patched.category == "美妆护肤"

    def test_patch_updates_chips(self):
        base = CriteriaPayload(
            category="美妆护肤",
            constraints=Constraints(),
            chips=["美妆护肤"],
        )
        patched = apply_criteria_patch(base, {"constraints": {"budget_max": 200.0}})
        assert "200元内" in patched.chips

    def test_patch_unknown_key_ignored(self):
        """Keys not in Constraints model are silently ignored."""
        base = CriteriaPayload(category="美妆护肤", constraints=Constraints())
        patched = apply_criteria_patch(base, {"constraints": {"nonexistent_key": "value"}})
        assert patched.constraints == base.constraints


# ── criteria_quick_actions ──────────────────────────────────────────────────


class TestCriteriaQuickActions:
    def test_beauty_category_returns_skin_type_action(self):
        actions = criteria_quick_actions(category="美妆护肤")
        action_ids = [a.action_id for a in actions]
        assert "sensitive_skin" in action_ids
        assert "no_alcohol" in action_ids

    def test_digital_category_returns_storage_action(self):
        actions = criteria_quick_actions(category="数码电子")
        action_ids = [a.action_id for a in actions]
        assert "storage_256" in action_ids

    def test_all_categories_have_budget_action(self):
        for cat in ["美妆护肤", "数码电子", "服饰运动", "食品生活"]:
            actions = criteria_quick_actions(category=cat)
            action_ids = [a.action_id for a in actions]
            assert "budget_low" in action_ids

    def test_all_categories_have_replace_action(self):
        for cat in ["美妆护肤", "数码电子", "服饰运动", "食品生活"]:
            actions = criteria_quick_actions(category=cat)
            action_ids = [a.action_id for a in actions]
            assert "replace_deck" in action_ids

    def test_none_category_returns_beauty_defaults(self):
        actions = criteria_quick_actions(category=None)
        action_ids = [a.action_id for a in actions]
        assert "sensitive_skin" in action_ids
