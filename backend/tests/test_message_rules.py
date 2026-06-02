"""Tests for deterministic message routing functions (铁律5)."""

from __future__ import annotations

import pytest

from src.runtime.message_rules import (
    _looks_like_brand,
    extract_adjustment_hints,
    extract_brand_prefer_from_message,
    extract_product_lookup_hints,
    extract_product_type_hint,
    has_shopping_signal,
)


# ── _looks_like_brand ────────────────────────────────────────────────

@pytest.fixture(autouse=True)
def _seed_test_brands(monkeypatch):
    """Provide a controlled brand set for tests, independent of real product data."""
    from src.runtime import message_rules as mr

    monkeypatch.setattr(
        mr,
        "get_known_brands",
        lambda: frozenset({"华为", "huawei", "oppo", "nike", "apple", "小米"}),
    )


class TestLooksLikeBrand:
    def test_known_brand_exact(self):
        assert _looks_like_brand("华为") is True
        assert _looks_like_brand("oppo") is True

    def test_ingredient_blocks_fallback(self):
        """Terms containing ingredient keywords are NOT treated as brands."""
        assert _looks_like_brand("酒精") is False  # contains "精"
        assert _looks_like_brand("面霜") is False  # contains "霜"

    def test_ascii_fallback(self):
        """ASCII uppercase terms not in catalog still look like brands."""
        assert _looks_like_brand("Lenovo") is True

    def test_cjk_fallback(self):
        """2-4 char CJK terms look like brands even if not in catalog."""
        assert _looks_like_brand("三星") is True

    def test_single_char_not_brand(self):
        assert _looks_like_brand("裤") is False


# ── extract_brand_prefer_from_message ─────────────────────────────────

class TestExtractBrandPrefer:
    def test_extracts_known_brands(self):
        result = extract_brand_prefer_from_message("我要华为手机")
        assert "华为" in result

    def test_does_not_extract_non_brands(self):
        result = extract_brand_prefer_from_message("我要一个便宜的手机")
        assert result == []

    def test_multiple_brands(self):
        result = extract_brand_prefer_from_message("华为和oppo哪个好")
        assert "华为" in result
        assert "oppo" in result

    def test_case_insensitive(self):
        result = extract_brand_prefer_from_message("我要huawei")
        assert "huawei" in result

    def test_longer_brand_matches_first(self):
        """'小米' should match even in short messages."""
        result = extract_brand_prefer_from_message("小米")
        assert "小米" in result


# ── extract_product_type_hint ────────────────────────────────────────

class TestExtractProductTypeHint:
    def test_hierarchy_parent(self):
        assert extract_product_type_hint("有裤子吗") == "裤子"

    def test_direct_alias(self):
        """Known sub_category alias appears in message."""
        assert extract_product_type_hint("推荐户外裤") == "户外裤"

    def test_no_match(self):
        assert extract_product_type_hint("今天天气怎么样") is None

    def test_shoes(self):
        assert extract_product_type_hint("我想买鞋") == "鞋"

    def test_phone_parent(self):
        """'手机' is an alias for canonical '智能手机', so hint returns the canonical."""
        assert extract_product_type_hint("推荐手机") == "智能手机"


# ── has_shopping_signal ──────────────────────────────────────────────

class TestHasShoppingSignal:
    def test_product_type_detected(self):
        assert has_shopping_signal("有裤子吗") is True

    def test_brand_prefer_detected(self):
        assert has_shopping_signal("我要华为") is True

    def test_shopping_marker(self):
        assert has_shopping_signal("想喝牛奶") is True

    def test_no_signal(self):
        assert has_shopping_signal("今天天气怎么样") is False


# ── extract_product_lookup_hints ─────────────────────────────────────

class TestExtractProductLookupHints:
    def test_you_ma(self):
        result = extract_product_lookup_hints("有鼠标吗")
        assert result == {"product_type": "鼠标"}

    def test_you_meiyou(self):
        result = extract_product_lookup_hints("有没有咖啡")
        assert result == {"product_type": "咖啡"}

    def test_no_match(self):
        assert extract_product_lookup_hints("推荐手机") is None

    def test_filters_generic(self):
        assert extract_product_lookup_hints("有什么吗") is None


# ── extract_adjustment_hints ────────────────────────────────────────

class TestExtractAdjustmentHints:
    def test_brand_avoid(self):
        result = extract_adjustment_hints("不要OPPO")
        assert "brand_avoid" in result
        assert "OPPO" in result["brand_avoid"]

    def test_brand_avoid_not_in_ingredient(self):
        result = extract_adjustment_hints("不要OPPO")
        assert "ingredient_avoid" not in result

    def test_ingredient_avoid(self):
        result = extract_adjustment_hints("不要酒精")
        assert "ingredient_avoid" in result
        assert "酒精" in result["ingredient_avoid"]

    def test_budget_direction(self):
        result = extract_adjustment_hints("便宜点")
        assert result.get("budget_direction") == "lower"

    def test_origin_avoid(self):
        result = extract_adjustment_hints("不要日系")
        assert "origin_avoid" in result
        assert "日系" in result["origin_avoid"]

    def test_stopword_filtered(self):
        """'不要' + stopword prefix is filtered out."""
        result = extract_adjustment_hints("不要太贵")
        assert "ingredient_avoid" not in result
