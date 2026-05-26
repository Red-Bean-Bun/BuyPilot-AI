from src.runtime.stages.slot_checker import build_clarification_question, check_required_slots
from src.types.schemas import IntentResult


def test_category_present_product_type_present_no_missing_slots():
    intent = IntentResult(
        intent="recommend",
        category="美妆护肤",
        extracted_constraints={"product_type": "洁面"},
    )
    assert check_required_slots("油皮洗面奶", intent) == []


def test_category_present_product_type_missing_no_constraints():
    """No product_type and no narrowing constraints → ask for product_type."""
    intent = IntentResult(intent="recommend", category="美妆护肤")
    assert check_required_slots("推荐护肤品", intent) == ["product_type"]


def test_category_present_product_type_missing_but_has_budget():
    """No product_type but has budget + skin_type → narrowed enough, don't ask."""
    intent = IntentResult(
        intent="recommend",
        category="美妆护肤",
        extracted_constraints={"budget_max": 200, "skin_type": "油性"},
    )
    assert check_required_slots("推荐适合油皮的护肤品，200元以内", intent) == []


def test_category_missing_returns_category():
    intent = IntentResult(intent="recommend", category=None)
    assert check_required_slots("随便看看", intent) == ["category"]


def test_non_recommend_intent_returns_empty():
    intent = IntentResult(intent="add_to_cart", category="美妆护肤")
    assert check_required_slots("加入购物车", intent) == []


def test_clarification_question_for_category():
    question, options = build_clarification_question(["category"])
    assert "哪一类" in question
    assert "美妆护肤" in options


def test_clarification_question_for_product_type():
    question, options = build_clarification_question(["product_type"])
    assert "哪一类" in question
    assert "洁面" in options
