"""Direct tests for retriever internal pure functions (P0-1)."""

from __future__ import annotations

from src.services.retriever import (
    ProductHit,
    _distance_to_score,
    _elevate_brand_preference,
    _filter_score,
    _merge_text_and_visual,
    _rank_hits,
)
from src.types.sse_events import Constraints, CriteriaPayload, ProductPayload


# ── Helpers ──────────────────────────────────────────────────────────────────


def _product(**kwargs) -> ProductPayload:
    defaults: dict = {
        "product_id": "p1",
        "name": "Test Product",
        "price": 100.0,
        "currency": "CNY",
        "image_url": None,
        "category": "美妆护肤",
        "sub_category": None,
        "brand": "TestBrand",
        "skin_type_match": [],
        "ingredient_tags": [],
        "ingredient_avoid": [],
        "use_scenario": None,
        "sku_options": None,
    }
    defaults.update(kwargs)
    return ProductPayload(**defaults)


def _hit(product=None, vector_score=0.5, filter_score=1.0, chunk=None, **kwargs):
    return ProductHit(
        product=product or _product(**kwargs),
        vector_score=vector_score,
        filter_score=filter_score,
        chunk=chunk,
    )


def _criteria(**constraints) -> CriteriaPayload:
    defaults: dict = {
        "budget_min": None,
        "budget_max": None,
        "use_scenario": None,
        "brand_avoid": [],
        "brand_prefer": [],
        "origin_avoid": [],
        "product_type": None,
        "skin_type": None,
        "ingredient_avoid": [],
        "ingredient_prefer": [],
        "storage": None,
        "screen_size": None,
        "sport_type": None,
        "season": None,
        "dietary": [],
    }
    defaults.update(constraints)
    return CriteriaPayload(
        criteria_id="test_criteria",
        category="美妆护肤",
        summary="",
        chips=[],
        constraints=Constraints(**defaults),
        field_sources={},
    )


# ── _distance_to_score ───────────────────────────────────────────────────────


class TestDistanceToScore:
    def test_perfect_match(self):
        assert _distance_to_score(0.0) == 1.0

    def test_distance_one(self):
        assert _distance_to_score(1.0) == 0.5

    def test_negative_distance_clamped(self):
        assert _distance_to_score(-0.5) == 1.0

    def test_large_distance(self):
        assert _distance_to_score(9.0) == 0.1


# ── _rank_hits ───────────────────────────────────────────────────────────────


class TestRankHits:
    def test_sorted_by_filter_score_desc(self):
        criteria = _criteria()
        hits = [
            _hit(product_id="p1", filter_score=1.0, vector_score=0.5, price=100),
            _hit(product_id="p2", filter_score=3.0, vector_score=0.5, price=100),
            _hit(product_id="p3", filter_score=2.0, vector_score=0.5, price=100),
        ]
        ranked = _rank_hits(criteria, hits)
        assert [h.product.product_id for h in ranked] == ["p2", "p3", "p1"]

    def test_tie_breaks_by_vector_score(self):
        criteria = _criteria()
        hits = [
            _hit(product_id="p1", filter_score=2.0, vector_score=0.3, price=100),
            _hit(product_id="p2", filter_score=2.0, vector_score=0.9, price=100),
        ]
        ranked = _rank_hits(criteria, hits)
        assert [h.product.product_id for h in ranked] == ["p2", "p1"]

    def test_tie_breaks_by_price_lower_first(self):
        criteria = _criteria()
        hits = [
            _hit(product_id="p_expensive", filter_score=2.0, vector_score=0.5, price=200),
            _hit(product_id="p_cheap", filter_score=2.0, vector_score=0.5, price=50),
        ]
        ranked = _rank_hits(criteria, hits)
        assert [h.product.product_id for h in ranked] == ["p_cheap", "p_expensive"]

    def test_empty_list(self):
        assert _rank_hits(_criteria(), []) == []


# ── _merge_text_and_visual ───────────────────────────────────────────────────


class TestMergeTextAndVisual:
    def test_disjoint_products_keeps_all(self):
        text = [_hit(product_id="p1", vector_score=0.5, filter_score=2.0)]
        visual = [_hit(product_id="p2", vector_score=0.8, filter_score=1.0)]
        merged = _merge_text_and_visual(text, visual)
        assert len(merged) == 2
        ids = {h.product.product_id for h in merged}
        assert ids == {"p1", "p2"}

    def test_overlap_visual_higher_vector_score_upgrades(self):
        chunk = object()  # sentinel to verify chunk retention
        text = [_hit(product_id="p1", vector_score=0.5, filter_score=2.0, chunk=chunk)]
        visual = [_hit(product_id="p1", vector_score=0.9, filter_score=1.0)]
        merged = _merge_text_and_visual(text, visual)
        assert len(merged) == 1
        h = merged[0]
        assert h.vector_score == 0.9  # visual wins
        assert h.filter_score == 2.0  # max(2.0, 1.0)
        assert h.chunk is chunk  # text chunk preserved

    def test_overlap_text_higher_vector_score_keeps_text(self):
        text = [_hit(product_id="p1", vector_score=0.9, filter_score=2.0)]
        visual = [_hit(product_id="p1", vector_score=0.3, filter_score=1.0)]
        merged = _merge_text_and_visual(text, visual)
        assert len(merged) == 1
        h = merged[0]
        assert h.vector_score == 0.9
        assert h.filter_score == 2.0

    def test_empty_visual_returns_text(self):
        text = [_hit(product_id="p1")]
        merged = _merge_text_and_visual(text, [])
        assert merged == text

    def test_both_empty(self):
        assert _merge_text_and_visual([], []) == []


# ── _filter_score ────────────────────────────────────────────────────────────


class TestFilterScore:
    def test_category_match(self):
        criteria = _criteria()
        product = _product(category="美妆护肤")
        score = _filter_score(criteria, product)
        assert score >= 3.0  # FILTER_SCORE_CATEGORY

    def test_brand_prefer_match(self):
        product = _product(brand="PreferredBrand", category="数码电子")
        criteria = CriteriaPayload(
            criteria_id="test",
            category="数码电子",
            summary="",
            chips=[],
            constraints=Constraints(brand_prefer=["PreferredBrand"]),
            field_sources={},
        )
        score = _filter_score(criteria, product)
        assert score >= 3.0  # FILTER_SCORE_BRAND_PREFER

    def test_no_match_zero(self):
        product = _product(category="食品生活", brand="NoMatch")
        criteria = CriteriaPayload(
            criteria_id="test",
            category="数码电子",
            summary="",
            chips=[],
            constraints=Constraints(),
            field_sources={},
        )
        score = _filter_score(criteria, product)
        assert score == 0.0


# ── _elevate_brand_preference ────────────────────────────────────────────────


class TestElevateBrandPreference:
    def test_empty_brand_prefer_returns_same_object(self):
        criteria = _criteria(brand_prefer=[])
        ranked = [_hit(product_id="p1", brand="A")]
        result = _elevate_brand_preference(criteria, ranked, [])
        assert result is ranked

    def test_preferred_in_ranked_moved_to_front(self):
        criteria = _criteria(brand_prefer=["B"])
        ranked = [
            _hit(product_id="p1", brand="A"),
            _hit(product_id="p2", brand="B"),
            _hit(product_id="p3", brand="C"),
        ]
        result = _elevate_brand_preference(criteria, ranked, ranked)
        assert [h.product.product_id for h in result] == ["p2", "p1", "p3"]

    def test_preferred_missing_from_ranked_injected_from_all_reranked(self):
        criteria = _criteria(brand_prefer=["B"])
        ranked = [
            _hit(product_id="p1", brand="A"),
            _hit(product_id="p3", brand="C"),
        ]
        all_reranked = [
            _hit(product_id="p1", brand="A"),
            _hit(product_id="p2", brand="B"),
            _hit(product_id="p3", brand="C"),
        ]
        result = _elevate_brand_preference(criteria, ranked, all_reranked)
        assert [h.product.product_id for h in result] == ["p2", "p1", "p3"]

    def test_multiple_preferred_all_at_front(self):
        criteria = _criteria(brand_prefer=["B", "D"])
        ranked = [
            _hit(product_id="p1", brand="A"),
            _hit(product_id="p2", brand="B"),
            _hit(product_id="p3", brand="C"),
            _hit(product_id="p4", brand="D"),
        ]
        result = _elevate_brand_preference(criteria, ranked, ranked)
        ids = [h.product.product_id for h in result]
        assert ids[0] in ("p2", "p4")
        assert ids[1] in ("p2", "p4")
        assert ids[0] != ids[1]

    def test_preferred_not_anywhere_no_effect(self):
        criteria = _criteria(brand_prefer=["Z"])
        ranked = [_hit(product_id="p1", brand="A")]
        result = _elevate_brand_preference(criteria, ranked, ranked)
        assert [h.product.product_id for h in result] == ["p1"]
