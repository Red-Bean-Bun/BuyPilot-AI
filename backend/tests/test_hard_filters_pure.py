"""Direct pure function tests for _passes_hard_filters and sub-filters.

Audit gap: _passes_hard_filters is the core retrieval filtering logic but had
no direct pure function tests — all tests went through retrieve() integration path.
铁律 5: 含复杂逻辑的纯函数必须有 ≥3 个直接测试用例。
"""

from __future__ import annotations


from src.services.retriever import (
    RetrievalFilters,
    _passes_brand_filter,
    _passes_budget_filter,
    _passes_category_filter,
    _passes_feedback_product_filter,
    _passes_hard_filters,
    _passes_origin_filter,
    _passes_product_type_filter,
)
from src.types.sse_events import Constraints, CriteriaPayload, ProductPayload


# ── Helpers ──────────────────────────────────────────────────────────────────


def _product(**kwargs) -> ProductPayload:
    defaults = {
        "product_id": "p_test_001",
        "name": "Test Product",
        "price": 100.0,
        "category": "美妆护肤",
        "sub_category": "洁面",
        "brand": "TestBrand",
    }
    defaults.update(kwargs)
    return ProductPayload(**defaults)


def _criteria(category: str = "", **constraint_kwargs) -> CriteriaPayload:
    return CriteriaPayload(
        category=category,
        constraints=Constraints(**constraint_kwargs),
    )


_EMPTY_FILTERS = RetrievalFilters()


# ── _passes_hard_filters (composite) ────────────────────────────────────────


class TestPassesHardFilters:
    def test_empty_constraints_passes_everything(self):
        product = _product(price=9999.0, brand="任意品牌")
        assert _passes_hard_filters(_criteria(), product, _EMPTY_FILTERS) is True

    def test_all_filters_pass(self):
        product = _product(price=150.0, brand="珂润", category="美妆护肤", sub_category="洁面")
        criteria = _criteria(
            category="美妆护肤",
            budget_max=200.0,
            product_type="洁面",
        )
        assert _passes_hard_filters(criteria, product, _EMPTY_FILTERS) is True

    def test_budget_exceeded_blocks(self):
        product = _product(price=250.0)
        criteria = _criteria(budget_max=200.0)
        assert _passes_hard_filters(criteria, product, _EMPTY_FILTERS) is False

    def test_brand_avoid_blocks(self):
        product = _product(brand="资生堂")
        criteria = _criteria(brand_avoid=["资生堂"])
        assert _passes_hard_filters(criteria, product, _EMPTY_FILTERS) is False

    def test_combined_filters_all_must_pass(self):
        """Budget passes but brand_avoid blocks — composite returns False."""
        product = _product(price=150.0, brand="资生堂")
        criteria = _criteria(budget_max=200.0, brand_avoid=["资生堂"])
        assert _passes_hard_filters(criteria, product, _EMPTY_FILTERS) is False

    def test_feedback_avoid_products_blocks(self):
        product = _product(product_id="p_blocked")
        filters = RetrievalFilters(avoid_products=frozenset({"p_blocked"}))
        assert _passes_hard_filters(_criteria(), product, filters) is False


# ── _passes_budget_filter ───────────────────────────────────────────────────


class TestBudgetFilter:
    def test_within_budget_passes(self):
        assert _passes_budget_filter(_criteria(budget_max=200.0), _product(price=150.0), _EMPTY_FILTERS) is True

    def test_exactly_at_budget_passes(self):
        assert _passes_budget_filter(_criteria(budget_max=200.0), _product(price=200.0), _EMPTY_FILTERS) is True

    def test_over_budget_blocked(self):
        assert _passes_budget_filter(_criteria(budget_max=200.0), _product(price=201.0), _EMPTY_FILTERS) is False

    def test_budget_max_none_passes_any_price(self):
        assert _passes_budget_filter(_criteria(budget_max=None), _product(price=99999.0), _EMPTY_FILTERS) is True

    def test_product_price_none_passes(self):
        assert _passes_budget_filter(_criteria(budget_max=100.0), _product(price=None), _EMPTY_FILTERS) is True

    def test_budget_min_below_passes(self):
        assert _passes_budget_filter(_criteria(budget_min=50.0), _product(price=100.0), _EMPTY_FILTERS) is True

    def test_budget_min_above_blocks(self):
        assert _passes_budget_filter(_criteria(budget_min=200.0), _product(price=100.0), _EMPTY_FILTERS) is False

    def test_budget_range(self):
        criteria = _criteria(budget_min=50.0, budget_max=200.0)
        assert _passes_budget_filter(criteria, _product(price=100.0), _EMPTY_FILTERS) is True
        assert _passes_budget_filter(criteria, _product(price=300.0), _EMPTY_FILTERS) is False
        assert _passes_budget_filter(criteria, _product(price=10.0), _EMPTY_FILTERS) is False


# ── _passes_brand_filter ────────────────────────────────────────────────────


class TestBrandFilter:
    def test_no_brand_avoid_passes(self):
        assert _passes_brand_filter(_criteria(), _product(brand="SK-II"), _EMPTY_FILTERS) is True

    def test_exact_brand_avoid_blocks(self):
        criteria = _criteria(brand_avoid=["资生堂"])
        assert _passes_brand_filter(criteria, _product(brand="资生堂"), _EMPTY_FILTERS) is False

    def test_brand_avoid_case_insensitive(self):
        criteria = _criteria(brand_avoid=["SK-II"])
        assert _passes_brand_filter(criteria, _product(brand="sk-ii"), _EMPTY_FILTERS) is False

    def test_different_brand_passes(self):
        criteria = _criteria(brand_avoid=["资生堂"])
        assert _passes_brand_filter(criteria, _product(brand="珂润"), _EMPTY_FILTERS) is True

    def test_product_brand_none_passes(self):
        criteria = _criteria(brand_avoid=["资生堂"])
        assert _passes_brand_filter(criteria, _product(brand=None), _EMPTY_FILTERS) is True


# ── _passes_category_filter ─────────────────────────────────────────────────


class TestCategoryFilter:
    def test_matching_category_passes(self):
        assert _passes_category_filter(_criteria("美妆护肤"), _product(category="美妆护肤"), _EMPTY_FILTERS) is True

    def test_different_category_blocked(self):
        assert _passes_category_filter(_criteria("数码电子"), _product(category="美妆护肤"), _EMPTY_FILTERS) is False

    def test_empty_criteria_category_passes_all(self):
        assert _passes_category_filter(_criteria(""), _product(category="食品生活"), _EMPTY_FILTERS) is True

    def test_category_normalization(self):
        """normalize_category maps aliases — 护肤 should match 美妆护肤."""
        assert _passes_category_filter(_criteria("护肤"), _product(category="美妆护肤"), _EMPTY_FILTERS) is True


# ── _passes_product_type_filter ─────────────────────────────────────────────


class TestProductTypeFilter:
    def test_matching_product_type_passes(self):
        criteria = _criteria(product_type="洁面")
        assert _passes_product_type_filter(criteria, _product(sub_category="洁面"), _EMPTY_FILTERS) is True

    def test_different_product_type_blocked(self):
        criteria = _criteria(product_type="防晒")
        assert _passes_product_type_filter(criteria, _product(sub_category="洁面"), _EMPTY_FILTERS) is False

    def test_empty_product_type_passes_all(self):
        assert (
            _passes_product_type_filter(_criteria(product_type=None), _product(sub_category="洁面"), _EMPTY_FILTERS)
            is True
        )

    def test_product_type_alias_normalization(self):
        """洗面奶 normalizes to 洁面 — should match."""
        criteria = _criteria(product_type="洗面奶")
        assert _passes_product_type_filter(criteria, _product(sub_category="洁面"), _EMPTY_FILTERS) is True

    def test_product_sub_category_empty_string_passes(self):
        """sub_category=None → normalize returns '' → '' in '洁面' is True → passes.

        This is a known design choice: empty product_type is treated as substring match."""
        criteria = _criteria(product_type="洁面")
        result = _passes_product_type_filter(criteria, _product(sub_category=None), _EMPTY_FILTERS)
        assert result is True  # empty is substring of any string


# ── _passes_origin_filter ───────────────────────────────────────────────────


class TestOriginFilter:
    def test_no_origin_avoid_passes(self):
        assert _passes_origin_filter(_criteria(), _product(brand="资生堂"), _EMPTY_FILTERS) is True

    def test_origin_avoid_blocks_matching_brand(self):
        criteria = _criteria(origin_avoid=["日本"])
        assert _passes_origin_filter(criteria, _product(brand="资生堂(日本)"), _EMPTY_FILTERS) is False

    def test_origin_avoid_no_match_passes(self):
        criteria = _criteria(origin_avoid=["日本"])
        assert _passes_origin_filter(criteria, _product(brand="珂润(国产)"), _EMPTY_FILTERS) is True

    def test_product_brand_none_passes(self):
        criteria = _criteria(origin_avoid=["日本"])
        assert _passes_origin_filter(criteria, _product(brand=None), _EMPTY_FILTERS) is True


# ── _passes_feedback_product_filter ─────────────────────────────────────────


class TestFeedbackProductFilter:
    def test_not_in_avoid_list_passes(self):
        filters = RetrievalFilters(avoid_products=frozenset({"p_other"}))
        assert _passes_feedback_product_filter(_criteria(), _product(product_id="p_test"), filters) is True

    def test_in_avoid_list_blocked(self):
        filters = RetrievalFilters(avoid_products=frozenset({"p_blocked"}))
        assert _passes_feedback_product_filter(_criteria(), _product(product_id="p_blocked"), filters) is False

    def test_empty_avoid_list_passes(self):
        assert _passes_feedback_product_filter(_criteria(), _product(), _EMPTY_FILTERS) is True
