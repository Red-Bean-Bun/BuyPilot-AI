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
    """Generic non-PRD broad candidates still go product-first."""
    intent = IntentResult(intent="recommend", category="食品生活")
    assert check_required_slots("推荐食品", intent) == []


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


def test_phone_requires_budget_before_recommendation():
    intent = IntentResult(
        intent="recommend",
        category="数码电子",
        extracted_constraints={"product_type": "手机", "use_scenario": "拍照"},
    )
    assert check_required_slots("我想买个手机，平时拍照多", intent) == ["budget"]


def test_vague_phone_keeps_budget_as_blocking_slot():
    intent = IntentResult(
        intent="recommend",
        category="数码电子",
        extracted_constraints={"product_type": "手机"},
    )

    missing = check_required_slots("推荐一款手机", intent)
    question, options = build_clarification_question(missing)

    assert missing == ["budget"]
    assert "预算" in question
    assert options


def test_budgeted_phone_stays_product_first():
    intent = IntentResult(
        intent="recommend",
        category="数码电子",
        extracted_constraints={"product_type": "手机", "budget_max": 3000},
    )

    assert check_required_slots("推荐一款3000元内手机", intent) == []


def test_phone_with_preference_and_budget_does_not_clarify():
    intent = IntentResult(
        intent="recommend",
        category="数码电子",
        extracted_constraints={"product_type": "手机", "budget_max": 3000, "use_scenario": "拍照"},
    )

    assert check_required_slots("推荐一款3000元内拍照好的手机", intent) == []


def test_clarification_asks_audio_preference_for_vague_headphones():
    intent = IntentResult(
        intent="recommend",
        category="数码电子",
        extracted_constraints={"product_type": "耳机"},
    )

    missing = check_required_slots("推荐一款耳机", intent)
    _, options = build_clarification_question(missing)

    assert missing == ["preference_dimension_digital"]
    assert options == ["音质", "续航", "轻便", "性价比"]


def test_budgeted_headphones_do_not_block_existing_judge_query():
    intent = IntentResult(
        intent="recommend",
        category="数码电子",
        extracted_constraints={"product_type": "耳机", "budget_max": 2000},
    )

    assert check_required_slots("推荐蓝牙耳机，预算2000以内", intent) == []


def test_clarification_asks_skin_type_for_vague_skincare():
    intent = IntentResult(
        intent="recommend",
        category="美妆护肤",
        extracted_constraints={"product_type": "面霜"},
    )

    missing = check_required_slots("推荐一款面霜", intent)
    question, options = build_clarification_question(missing)

    assert missing == ["skin_type"]
    assert "肤质" in question
    assert options == ["油性", "干性", "混合", "敏感"]


def test_oily_skin_cleanser_does_not_trigger_preference_clarification():
    intent = IntentResult(
        intent="recommend",
        category="美妆护肤",
        extracted_constraints={"product_type": "洁面", "skin_type": "油性"},
    )

    assert check_required_slots("推荐适合油皮的洗面奶", intent) == []


def test_vague_running_shoes_stay_product_first():
    intent = IntentResult(
        intent="recommend",
        category="服饰运动",
        extracted_constraints={"product_type": "跑鞋"},
    )

    assert check_required_slots("推荐一双跑鞋", intent) == []


def test_clarification_question_for_category():
    question, options = build_clarification_question(["category"])
    assert "哪一类" in question
    assert "美妆护肤" in options
    assert "食品生活" in options


def test_clarification_question_for_budget():
    question, options = build_clarification_question(["budget"])
    assert "预算" in question
    assert "2000-4000元" in options


def test_clarification_question_for_product_type():
    question, options = build_clarification_question(["product_type"])
    assert "哪一类" in question
    assert "洁面" in options
