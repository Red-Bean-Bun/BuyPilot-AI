from src.config.domain_terms import infer_category_from_product_type, normalize_category
from src.runtime.pipeline import _unsupported_product_type
from src.runtime.stages.criteria import annotate_criteria_sources
from src.services.llm_task_payloads import normalize_intent_payload
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


def test_food_category_normalizes_to_business_taxonomy():
    normalized = normalize_intent_payload(
        {
            "intent": "recommend",
            "category": "食品饮料",
            "extracted_constraints": {"product_type": "茶饮"},
        }
    )

    assert normalized["category"] == "食品生活"


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


def test_unsupported_product_type_is_stopped_before_criteria():
    intent = IntentResult(intent="recommend", category="数码电子", extracted_constraints={"product_type": "CD机"})

    assert _unsupported_product_type(intent) is True
