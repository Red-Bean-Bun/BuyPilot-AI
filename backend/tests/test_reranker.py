import pytest

import src.config.settings as settings_module
from src.services import reranker
from src.services.reranker import rerank
from src.types.sse_events import CriteriaPayload, ProductPayload


def _reset_settings() -> None:
    settings_module._settings = None


@pytest.mark.asyncio
async def test_rerank_raises_when_config_missing(monkeypatch):
    monkeypatch.delenv("BAILIAN_BASE_URL", raising=False)
    monkeypatch.delenv("BAILIAN_API_KEY", raising=False)
    _reset_settings()

    products = [
        ProductPayload(product_id="p1", name="A", category="食品饮料", price=100),
        ProductPayload(product_id="p2", name="B", category="美妆护肤", price=80),
    ]

    with pytest.raises(reranker.RerankUnavailable):
        await rerank(CriteriaPayload(category="美妆护肤"), products, top_n=1)


@pytest.mark.asyncio
async def test_rerank_strict_mode_raises_when_config_missing(monkeypatch):
    monkeypatch.setenv("STRICT_RUNTIME", "1")
    monkeypatch.delenv("BAILIAN_BASE_URL", raising=False)
    monkeypatch.delenv("BAILIAN_API_KEY", raising=False)
    _reset_settings()

    products = [ProductPayload(product_id="p1", name="A", category="美妆护肤")]

    with pytest.raises(reranker.RerankUnavailable):
        await rerank(CriteriaPayload(category="美妆护肤"), products, top_n=1)
    _reset_settings()


def test_qwen3_rerank_uses_compatible_api_endpoint(monkeypatch):
    monkeypatch.setenv("BAILIAN_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")
    monkeypatch.setenv("BAILIAN_API_KEY", "test-key")
    _reset_settings()

    profile = reranker._resolve_rerank_profile("gte_rerank")

    assert reranker._rerank_endpoint(profile) == "https://dashscope.aliyuncs.com/compatible-api/v1/reranks"
    _reset_settings()
