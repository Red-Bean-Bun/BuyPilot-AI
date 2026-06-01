from src.config.domain_terms import infer_category_from_product_type, normalize_category
from src.runtime.pipeline import _unsupported_product_type
from src.runtime.stages.criteria import annotate_criteria_sources, apply_criteria_patch, criteria_from_intent
from src.services.llm_task_payloads import criteria_from_live_payload, normalize_intent_payload
from src.services.retriever import RetrievalFilters, _passes_hard_filters
from src.types.schemas import IntentResult
from src.types.sse_events import Constraints, CriteriaPayload, ProductPayload


def test_criteria_marks_user_and_history_sources_without_budget_hallucination():
    existing = CriteriaPayload(
        criteria_id="c_old",
        category="美妆护肤",
        constraints=Constraints(product_type="洁面", budget_max=150),
    )
    generated = CriteriaPayload(
        criteria_id="c_new",
        category="美妆护肤",
        constraints=Constraints(product_type="洁面", skin_type="油性", budget_max=150),
    )
    intent = IntentResult(intent="recommend", category="美妆护肤", extracted_constraints={"skin_type": "油性"})

    criteria = annotate_criteria_sources(generated, intent, existing)

    assert criteria.field_sources["category"] == "user"
    assert criteria.field_sources["constraints.skin_type"] == "user"
    assert criteria.field_sources["constraints.budget_max"] == "history"


def test_criteria_strips_inferred_budget_in_new_session():
    generated = CriteriaPayload(
        criteria_id="c_new",
        category="美妆护肤",
        constraints=Constraints(product_type="洁面", skin_type="油性", budget_max=200),
    )
    intent = IntentResult(intent="recommend", category="美妆护肤", extracted_constraints={"skin_type": "油性"})

    criteria = annotate_criteria_sources(generated, intent, None)

    assert criteria.constraints.budget_max is None
    assert "constraints.budget_max" not in criteria.field_sources


def test_criteria_strips_inferred_product_type_in_new_session():
    """product_type is no longer in HARD_CONSTRAINT_FIELDS — kept as 'inferred'."""
    generated = CriteriaPayload(
        criteria_id="c_new",
        category="数码电子",
        constraints=Constraints(product_type="智能手机", use_scenario="日常使用"),
    )
    intent = IntentResult(intent="recommend", category="数码电子", extracted_constraints={})

    criteria = annotate_criteria_sources(generated, intent, None)

    assert criteria.constraints.product_type == "智能手机"
    assert criteria.field_sources["constraints.product_type"] == "inferred"
    assert "智能手机" in criteria.chips


def test_criteria_keeps_user_explicit_product_type():
    generated = CriteriaPayload(
        criteria_id="c_new",
        category="数码电子",
        constraints=Constraints(product_type="智能手机"),
    )
    intent = IntentResult(intent="recommend", category="数码电子", extracted_constraints={"product_type": "手机"})

    criteria = annotate_criteria_sources(generated, intent, None)

    assert criteria.constraints.product_type == "智能手机"
    assert criteria.field_sources["constraints.product_type"] == "user"


def test_criteria_keeps_history_product_type():
    existing = CriteriaPayload(
        criteria_id="c_old",
        category="数码电子",
        constraints=Constraints(product_type="智能手机"),
    )
    generated = CriteriaPayload(
        criteria_id="c_new",
        category="数码电子",
        constraints=Constraints(product_type="智能手机", use_scenario="日常使用"),
    )
    intent = IntentResult(intent="recommend", category="数码电子", extracted_constraints={"use_scenario": "日常使用"})

    criteria = annotate_criteria_sources(generated, intent, existing)

    assert criteria.constraints.product_type == "智能手机"
    assert criteria.field_sources["constraints.product_type"] == "history"


def test_category_name_product_type_is_not_explicit_source():
    """Intent product_type equal to category name is not explicit, but criteria's correct value is kept."""
    generated = CriteriaPayload(
        criteria_id="c_new",
        category="数码电子",
        constraints=Constraints(product_type="智能手机"),
    )
    intent = IntentResult(intent="recommend", category="数码电子", extracted_constraints={"product_type": "数码电子"})

    criteria = annotate_criteria_sources(generated, intent, None)

    assert criteria.constraints.product_type == "智能手机"
    assert criteria.field_sources["constraints.product_type"] == "inferred"


def test_food_category_normalizes_to_business_taxonomy():
    normalized = normalize_intent_payload(
        {
            "intent": "recommend",
            "category": "食品饮料",
            "extracted_constraints": {"product_type": "茶饮"},
        }
    )

    assert normalized["category"] == "食品生活"


def test_food_product_types_are_not_confused_with_category_aliases():
    for raw, expected in (("气泡水", "碳酸饮料"), ("零食", "坚果/零食"), ("调味品", "调味品")):
        normalized = normalize_intent_payload(
            {
                "intent": "recommend",
                "category": "食品生活",
                "extracted_constraints": {"product_type": raw},
            }
        )

        assert normalized["extracted_constraints"]["product_type"] == expected


def test_food_category_accepts_raw_dataset_alias_in_hard_filter():
    criteria = CriteriaPayload(category="食品生活", constraints=Constraints(product_type="调味品"))
    product = ProductPayload(
        product_id="p_food_020",
        name="测试酱油",
        category="食品饮料",
        sub_category="调味品",
    )

    assert _passes_hard_filters(criteria, product, RetrievalFilters())


def test_product_type_can_infer_business_category_for_sauce():
    assert normalize_category("食品饮料") == "食品生活"
    assert infer_category_from_product_type("酱油") == "食品生活"


def test_intent_payload_drops_cross_category_product_type():
    normalized = normalize_intent_payload(
        {
            "intent": "recommend",
            "category": "数码电子",
            "extracted_constraints": {"product_type": "坚果"},
        }
    )

    assert normalized["category"] == "数码电子"
    assert "product_type" not in normalized["extracted_constraints"]


def test_intent_payload_drops_category_name_as_product_type():
    normalized = normalize_intent_payload(
        {
            "intent": "recommend",
            "category": "数码电子",
            "extracted_constraints": {"product_type": "数码电子"},
        }
    )

    assert normalized["category"] == "数码电子"
    assert "product_type" not in normalized["extracted_constraints"]


def test_intent_payload_preserves_unknown_product_type_for_unsupported_check():
    normalized = normalize_intent_payload(
        {
            "intent": "recommend",
            "category": "数码电子",
            "extracted_constraints": {"product_type": "CD机"},
        }
    )

    assert normalized["category"] == "数码电子"
    assert normalized["extracted_constraints"]["product_type"] == "CD机"


def test_criteria_from_live_payload_drops_cross_category_product_type():
    criteria = criteria_from_live_payload(
        {
            "category": "数码电子",
            "summary": "数码电子 / 坚果/零食",
            "chips": ["数码电子", "坚果/零食"],
            "constraints": {"product_type": "坚果", "use_scenario": "日常"},
        },
        existing=None,
    )

    assert criteria is not None
    assert criteria.category == "数码电子"
    assert criteria.constraints.product_type is None
    assert "坚果/零食" not in criteria.chips
    assert "坚果" not in criteria.summary


def test_criteria_from_live_payload_drops_category_name_as_product_type():
    criteria = criteria_from_live_payload(
        {
            "category": "数码电子",
            "summary": "数码电子，日常，数码电子",
            "chips": ["数码电子", "日常", "数码电子"],
            "constraints": {"product_type": "数码电子", "use_scenario": "日常"},
        },
        existing=None,
    )

    assert criteria is not None
    assert criteria.category == "数码电子"
    assert criteria.constraints.product_type is None
    assert criteria.chips == ["数码电子", "日常"]
    assert criteria.summary == "数码电子，日常"


def test_criteria_from_intent_drops_cross_category_product_type():
    intent = IntentResult(intent="recommend", category="数码电子", extracted_constraints={"product_type": "坚果"})

    criteria = criteria_from_intent(intent)

    assert criteria.category == "数码电子"
    assert criteria.constraints.product_type is None
    assert "坚果/零食" not in criteria.chips


def test_criteria_from_intent_drops_category_name_as_product_type():
    intent = IntentResult(intent="recommend", category="数码电子", extracted_constraints={"product_type": "数码电子"})

    criteria = criteria_from_intent(intent)

    assert criteria.category == "数码电子"
    assert criteria.constraints.product_type is None
    assert criteria.chips == ["数码电子"]


def test_non_string_product_type_does_not_crash_criteria_paths():
    intent = IntentResult(intent="recommend", category="数码电子", extracted_constraints={"product_type": 123})

    criteria = criteria_from_intent(intent)
    patched = apply_criteria_patch(
        CriteriaPayload(category="数码电子", constraints=Constraints(product_type="智能手机")),
        {"constraints": {"product_type": 123}},
    )
    live = criteria_from_live_payload(
        {"category": "数码电子", "constraints": {"product_type": 123}},
        existing=None,
    )

    assert criteria.constraints.product_type is None
    assert patched.constraints.product_type is None
    assert live is not None
    assert live.constraints.product_type is None


def test_criteria_patch_clears_cross_category_product_type():
    existing = CriteriaPayload(category="数码电子", constraints=Constraints(product_type="智能手机"))

    criteria = apply_criteria_patch(existing, {"constraints": {"product_type": "坚果"}})

    assert criteria.constraints.product_type is None
    assert "坚果/零食" not in criteria.chips


def test_criteria_patch_clears_category_name_product_type():
    existing = CriteriaPayload(category="数码电子", constraints=Constraints(product_type="智能手机"))

    criteria = apply_criteria_patch(existing, {"constraints": {"product_type": "数码电子"}})

    assert criteria.constraints.product_type is None
    assert criteria.chips == ["数码电子"]


def test_unsupported_product_type_is_stopped_before_criteria():
    intent = IntentResult(intent="recommend", category="数码电子", extracted_constraints={"product_type": "CD机"})

    assert _unsupported_product_type(intent) is True


def test_category_name_product_type_is_not_treated_as_unsupported():
    intent = IntentResult(intent="recommend", category="数码电子", extracted_constraints={"product_type": "数码电子"})

    assert _unsupported_product_type(intent) is False


# ── product_type retention after removal from _HARD_CONSTRAINT_FIELDS ──────


def test_product_type_retained_as_inferred_when_not_explicit():
    """LLM-inferred product_type should survive annotation, marked as 'inferred'."""
    generated = CriteriaPayload(
        criteria_id="c_test",
        category="食品生活",
        summary="食品生活，坚果",
        chips=["食品生活", "坚果"],
        constraints=Constraints(product_type="坚果/零食"),
    )
    intent = IntentResult(intent="recommend", category="食品生活", extracted_constraints={})

    result = annotate_criteria_sources(generated, intent, existing=None)

    assert result.constraints.product_type == "坚果/零食"
    assert result.field_sources.get("constraints.product_type") == "inferred"


def test_product_type_still_cleared_on_category_switch():
    """Category switch clears product_type even without HARD_CONSTRAINT (category_switched check)."""
    existing = CriteriaPayload(category="食品生活", constraints=Constraints(product_type="坚果"))
    generated = CriteriaPayload(
        criteria_id="c_test",
        category="数码电子",
        chips=["数码电子", "智能手机"],
        constraints=Constraints(product_type="智能手机"),
    )
    intent = IntentResult(intent="recommend", category="数码电子", extracted_constraints={})

    result = annotate_criteria_sources(generated, intent, existing=existing)

    assert result.constraints.product_type is None
    assert "constraints.product_type" not in result.field_sources
