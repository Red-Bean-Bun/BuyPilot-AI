from src.runtime.stages.slot_checker import build_clarification_question, check_required_slots
from src.types.schemas import IntentResult


def test_category_present_no_missing_slots():
    intent = IntentResult(intent="recommend", category="美妆护肤")
    assert check_required_slots("日常护肤", intent) == []


def test_category_missing_returns_category():
    intent = IntentResult(intent="recommend", category=None)
    assert check_required_slots("日常护肤", intent) == ["category"]


def test_clarification_question_for_category():
    question, options = build_clarification_question(["category"])
    assert "哪一类" in question
    assert "美妆护肤" in options
