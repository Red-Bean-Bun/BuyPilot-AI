"""Direct tests for retrieval_features pure functions."""

from __future__ import annotations

import pytest
from src.services.retrieval_features import _brand_in_summary, product_match_score
from src.types.sse_events import Constraints, CriteriaPayload, ProductPayload


# ---------------------------------------------------------------------------
# _brand_in_summary
# ---------------------------------------------------------------------------


class TestBrandInSummary:
    """Tests for brand name substring matching in criteria summary."""

    def test_brand_mentioned_in_summary(self) -> None:
        assert _brand_in_summary("可口可乐", "无糖可口可乐饮料，15瓶装") is True

    def test_brand_not_mentioned(self) -> None:
        assert _brand_in_summary("元气森林", "无糖可口可乐饮料，15瓶装") is False

    def test_brand_is_none(self) -> None:
        assert _brand_in_summary(None, "无糖可口可乐饮料") is False

    def test_brand_too_short(self) -> None:
        # Single-character brand should not match (too prone to false positives)
        assert _brand_in_summary("A", "含有A成分的产品") is False

    def test_empty_summary(self) -> None:
        assert _brand_in_summary("可口可乐", "") is False

    def test_both_empty(self) -> None:
        assert _brand_in_summary("", "") is False

    def test_partial_brand_match_not_counted(self) -> None:
        # "小米" should not match "小米辣" (different entity)
        # But our simple substring check would match — this is a known limitation
        # For now, we accept this and document it
        assert _brand_in_summary("小米", "小米手机推荐") is True

    def test_exact_brand_match(self) -> None:
        assert _brand_in_summary("李宁", "李宁跑步鞋") is True


# ---------------------------------------------------------------------------
# product_match_score with brand_weight
# ---------------------------------------------------------------------------


class TestProductMatchScoreWithBrand:
    """Tests for product_match_score brand matching integration."""

    def _make_criteria(self, summary: str = "", **kwargs: object) -> CriteriaPayload:
        return CriteriaPayload(
            criteria_id="c_test",
            category=kwargs.get("category", "食品生活"),
            summary=summary,
            chips=[],
            constraints=kwargs.get("constraints", Constraints()),
        )

    def _make_product(self, brand: str | None = None, **kwargs: object) -> ProductPayload:
        return ProductPayload(
            product_id=kwargs.get("product_id", "p_test_001"),
            name=kwargs.get("name", "测试商品"),
            price=kwargs.get("price"),
            category=kwargs.get("category", "食品生活"),
            brand=brand,
        )

    def test_brand_match_adds_weight(self) -> None:
        criteria = self._make_criteria(summary="推荐可口可乐无糖饮料")
        product = self._make_product(brand="可口可乐")
        score = product_match_score(
            criteria,
            product,
            category_weight=3.0,
            skin_type_weight=2.0,
            budget_weight=1.0,
            brand_weight=1.5,
        )
        # category match (3.0) + brand match (1.5) = 4.5
        assert score == 4.5

    def test_no_brand_match_no_bonus(self) -> None:
        criteria = self._make_criteria(summary="推荐可口可乐无糖饮料")
        product = self._make_product(brand="元气森林")
        score = product_match_score(
            criteria,
            product,
            category_weight=3.0,
            skin_type_weight=2.0,
            budget_weight=1.0,
            brand_weight=1.5,
        )
        # category match (3.0) only
        assert score == 3.0

    def test_brand_weight_zero_disables_matching(self) -> None:
        criteria = self._make_criteria(summary="推荐可口可乐无糖饮料")
        product = self._make_product(brand="可口可乐")
        score = product_match_score(
            criteria,
            product,
            category_weight=3.0,
            skin_type_weight=2.0,
            budget_weight=1.0,
            brand_weight=0.0,
        )
        # category match (3.0) only, brand_weight=0 disables
        assert score == 3.0

    def test_brand_match_with_budget_composes(self) -> None:
        criteria = self._make_criteria(
            summary="推荐可口可乐无糖饮料",
            constraints=Constraints(budget_max=100.0),
        )
        product = self._make_product(brand="可口可乐", price=75.0)
        score = product_match_score(
            criteria,
            product,
            category_weight=3.0,
            skin_type_weight=2.0,
            budget_weight=1.0,
            brand_weight=1.5,
        )
        # category (3.0) + budget (1.0, price 75 <= 100) + brand (1.5) = 5.5
        assert score == 5.5

    def test_brand_match_without_budget(self) -> None:
        criteria = self._make_criteria(
            summary="推荐可口可乐无糖饮料",
            constraints=Constraints(budget_max=50.0),
        )
        product = self._make_product(brand="可口可乐", price=75.0)
        score = product_match_score(
            criteria,
            product,
            category_weight=3.0,
            skin_type_weight=2.0,
            budget_weight=1.0,
            brand_weight=1.5,
        )
        # category (3.0) + brand (1.5) = 4.5 (budget exceeded, no bonus)
        assert score == 4.5

    def test_product_without_brand(self) -> None:
        criteria = self._make_criteria(summary="推荐可口可乐无糖饮料")
        product = self._make_product(brand=None)
        score = product_match_score(
            criteria,
            product,
            category_weight=3.0,
            skin_type_weight=2.0,
            budget_weight=1.0,
            brand_weight=1.5,
        )
        # category match (3.0) only
        assert score == 3.0


# ---------------------------------------------------------------------------
# Integration: brand matching fixes the Coca-Cola ranking issue
# ---------------------------------------------------------------------------


class TestBrandMatchingFixesRanking:
    """Verify brand matching resolves the 元气森林 vs 可口可乐 ranking."""

    def test_cocacola_outranks_yuanqi_when_brand_mentioned(self) -> None:
        """When summary mentions 可口可乐, the Coca-Cola product should score higher."""
        criteria = CriteriaPayload(
            criteria_id="c_001",
            category="食品生活",
            summary="无糖可口可乐饮料，15瓶装，330ml每瓶，用于运动补给",
            chips=["无糖", "15瓶装", "330ml", "运动补给"],
            constraints=Constraints(
                use_scenario="运动补给",
                product_type="碳酸饮料",
                dietary=["无糖"],
            ),
        )

        cocacola = ProductPayload(
            product_id="p_food_015",
            name="可口可乐 零度汽水 无糖碳酸饮料 330ml×24 罐装",
            price=75.0,
            category="食品生活",
            sub_category="碳酸饮料",
            brand="可口可乐",
        )

        yuanqi = ProductPayload(
            product_id="p_food_004",
            name="元气森林 0糖0脂0卡 白桃味气泡水480ml 碳酸饮料即饮苏打型饮品",
            price=4.5,
            category="食品生活",
            sub_category="碳酸饮料",
            brand="元气森林",
        )

        score_cocacola = product_match_score(
            criteria,
            cocacola,
            category_weight=3.0,
            skin_type_weight=2.0,
            budget_weight=1.0,
            scenario_weight=0.5,
            brand_weight=1.5,
        )

        score_yuanqi = product_match_score(
            criteria,
            yuanqi,
            category_weight=3.0,
            skin_type_weight=2.0,
            budget_weight=1.0,
            scenario_weight=0.5,
            brand_weight=1.5,
        )

        # 可口可乐: category(3.0) + brand(1.5) = 4.5
        # 元气森林: category(3.0) = 3.0
        # Coca-Cola should rank higher due to brand match
        assert score_cocacola > score_yuanqi
        assert score_cocacola == 4.5
        assert score_yuanqi == 3.0
