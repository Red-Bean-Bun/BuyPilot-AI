import pytest

from src.runtime.stages.slot_checker import build_clarification_question, check_required_slots
from src.services.llm_client import analyze_intent


@pytest.mark.asyncio
async def test_clear_beauty_request_has_no_missing_slots():
    intent = await analyze_intent("推荐适合油皮的洗面奶，200元以内，日常护肤")
    assert check_required_slots("推荐适合油皮的洗面奶，200元以内，日常护肤", intent) == []


@pytest.mark.asyncio
async def test_non_shopping_ambiguous_text_missing_category():
    intent = await analyze_intent("随便看看")
    missing = check_required_slots("随便看看", intent)
    assert "category" in missing


def test_clarification_question_for_category():
    question, options = build_clarification_question(["category"])
    assert "哪一类" in question
    assert "美妆护肤" in options

