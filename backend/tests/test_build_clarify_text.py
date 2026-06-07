"""Direct tests for handlers._build_clarify_text — pure function, no mocks."""

from src.config import user_messages as msg
from src.runtime.handlers import _build_clarify_text


def test_clarify_text_category_missing():
    text = _build_clarify_text(["category"])
    assert "品类" in text
    assert text == msg.CLARIFICATION_CATEGORY_HINT


def test_clarify_text_budget_with_category():
    text = _build_clarify_text(["budget"], "数码电子")
    assert "数码电子" in text
    assert "预算" in text


def test_clarify_text_budget_no_category_falls_back():
    assert _build_clarify_text(["budget"]) == msg.CLARIFICATION_ANALYSIS


def test_clarify_text_unknown_slot_falls_back():
    assert _build_clarify_text(["preference_dimension"]) == msg.CLARIFICATION_ANALYSIS


def test_clarify_text_empty_slots_falls_back():
    assert _build_clarify_text([]) == msg.CLARIFICATION_ANALYSIS
