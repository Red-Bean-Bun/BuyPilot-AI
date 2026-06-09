"""Direct tests for grounding_guard pure functions (铁律5)."""

from __future__ import annotations

from src.services.grounding_guard import validate_price_claims
from src.types.sse_events import ProductPayload


def _product(price: float | None = None, sku_prices: list[float] | None = None):
    skus = None
    if sku_prices:
        skus = [{"sku_id": f"sku_{i}", "price": p} for i, p in enumerate(sku_prices)]
    return ProductPayload(product_id="test", name="Test Product", price=price, sku_options=skus)


class TestValidatePriceClaims:
    def test_valid_price_passes(self):
        assert validate_price_claims("仅售128元", [_product(128.0)]) is None

    def test_fake_price_blocked(self):
        correction = validate_price_claims("仅售99元", [_product(128.0)])
        assert correction is not None
        assert "99元" in correction

    def test_budget_in_whitelist(self):
        assert validate_price_claims("200元以内", [_product(300.0)], budget_max=200) is None

    def test_sku_price_in_whitelist(self):
        correction = validate_price_claims("SKU价150元", [_product(100.0, sku_prices=[150.0])])
        assert correction is None

    def test_multiple_prices_all_valid(self):
        assert validate_price_claims("基础款128元，升级款256元", [_product(128.0), _product(256.0)]) is None

    def test_multiple_prices_one_fake(self):
        correction = validate_price_claims("基础款128元，特价99元", [_product(128.0), _product(256.0)])
        assert correction is not None
        assert "99元" in correction

    def test_no_price_mention_passes(self):
        """Text without any price figures should pass through."""
        assert validate_price_claims("这款产品非常好用，值得推荐", [_product(128.0)]) is None

    def test_empty_text_passes(self):
        assert validate_price_claims("", [_product(128.0)]) is None

    def test_empty_products_flags_all_prices(self):
        """Edge case: no products = empty whitelist = all prices flagged.
        This shouldn't happen in practice (guard only runs when products exist)."""
        assert validate_price_claims("仅售99元", []) is not None

    def test_decimal_price(self):
        """99.4元 rounds to 99, which is not in the whitelist {100}."""
        correction = validate_price_claims("仅售99.4元", [_product(100.0)])
        assert correction is not None

    def test_close_price_rounds_to_allowed(self):
        """99.6元 rounds to 100 = in whitelist. Guard should not block."""
        assert validate_price_claims("仅售99.6元", [_product(100.0)]) is None

    def test_rounded_price_matches(self):
        """99.6元 rounds to 100, which is in the whitelist."""
        assert validate_price_claims("仅售99.6元", [_product(100.0)]) is None
