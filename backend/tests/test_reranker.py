import pytest

import src.config.settings as settings_module
from src.services import reranker
from src.services.reranker import rerank
from src.types.sse_events import CriteriaPayload, ProductPayload


def _reset_settings() -> None:
    settings_module._settings = None


@pytest.mark.asyncio
async def test_rerank_uses_live_index_order(monkeypatch):
    monkeypatch.setenv("BAILIAN_BASE_URL", "https://example.test/compatible-mode/v1")
    monkeypatch.setenv("BAILIAN_API_KEY", "test-key")
    _reset_settings()

    async def fake_rerank_request(profile, query, documents, top_n):
        assert profile.name == "gte_rerank"
        assert profile.model == "qwen3-rerank"
        assert query
        assert len(documents) == 3
        assert top_n == 2
        return [2, 0]

    monkeypatch.setattr(reranker, "_rerank_request", fake_rerank_request)
    products = [
        ProductPayload(product_id="p1", name="A", category="美妆护肤"),
        ProductPayload(product_id="p2", name="B", category="美妆护肤"),
        ProductPayload(product_id="p3", name="C", category="美妆护肤"),
    ]

    ranked = await rerank(CriteriaPayload(category="美妆护肤", summary="油皮洗面奶"), products, top_n=2)

    assert [product.product_id for product in ranked] == ["p3", "p1"]


@pytest.mark.asyncio
async def test_rerank_falls_back_when_config_missing(monkeypatch):
    monkeypatch.delenv("BAILIAN_BASE_URL", raising=False)
    monkeypatch.delenv("BAILIAN_API_KEY", raising=False)
    _reset_settings()

    products = [
        ProductPayload(product_id="p1", name="A", category="食品饮料", price=100),
        ProductPayload(product_id="p2", name="B", category="美妆护肤", price=80),
    ]

    ranked = await rerank(CriteriaPayload(category="美妆护肤"), products, top_n=1)

    assert ranked[0].product_id == "p2"
