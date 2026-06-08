"""Normal path tests for reranker service.

Audit gap: test_reranker.py had only 3 tests (all config-missing paths).
Overrides conftest mock_external_ai to test full HTTP path.
"""

from __future__ import annotations

from unittest.mock import AsyncMock, MagicMock, patch

import httpx
import pytest

import src.config.settings as settings_module
from src.services.reranker import RerankUnavailable, rerank, rerank_texts
from src.types.sse_events import Constraints, CriteriaPayload, ProductPayload


@pytest.fixture(autouse=True)
def mock_external_ai(monkeypatch):
    """Override conftest: only set env vars, don't mock internal functions."""
    monkeypatch.setenv("BAILIAN_BASE_URL", "https://example.test/v1")
    monkeypatch.setenv("BAILIAN_API_KEY", "test-key")
    monkeypatch.setenv("DOUBAO_BASE_URL", "https://example.test/v1")
    monkeypatch.setenv("DOUBAO_API_KEY", "test-key")
    monkeypatch.setenv("OBSERVABILITY_LOCAL_ENABLED", "0")
    settings_module._settings = None
    import src.services.http_client as hc

    hc._CLIENT = None


def _product(pid: str, name: str = "Test", price: float = 100.0) -> ProductPayload:
    return ProductPayload(product_id=pid, name=name, price=price, category="美妆护肤")


def _criteria(**kw) -> CriteriaPayload:
    return CriteriaPayload(category="美妆护肤", constraints=Constraints(**kw))


def _fake_response(json_data: dict):
    resp = MagicMock()
    resp.json.return_value = json_data
    resp.status_code = 200
    resp.raise_for_status = lambda: None
    return resp


class TestRerankEmptyInput:
    @pytest.mark.asyncio
    async def test_empty_products_returns_empty(self):
        result = await rerank(_criteria(), [])
        assert result == []

    @pytest.mark.asyncio
    async def test_rerank_texts_empty_returns_empty(self):
        result = await rerank_texts(_criteria(), [])
        assert result == []


class TestRerankNormalPath:
    @pytest.mark.asyncio
    async def test_rerank_returns_sorted_products(self):
        products = [_product("p1", "A"), _product("p2", "B"), _product("p3", "C")]
        with patch.object(httpx.AsyncClient, "post", new_callable=AsyncMock) as mock_post:
            mock_post.return_value = _fake_response(
                {
                    "output": {
                        "results": [
                            {"index": 2, "relevance_score": 0.95},
                            {"index": 0, "relevance_score": 0.80},
                            {"index": 1, "relevance_score": 0.50},
                        ]
                    }
                }
            )
            result = await rerank(_criteria(), products)
        assert len(result) == 3
        assert result[0].product_id == "p3"
        assert result[1].product_id == "p1"
        assert result[2].product_id == "p2"

    @pytest.mark.asyncio
    async def test_rerank_filters_out_of_range_indexes(self):
        products = [_product("p1"), _product("p2")]
        with patch.object(httpx.AsyncClient, "post", new_callable=AsyncMock) as mock_post:
            mock_post.return_value = _fake_response(
                {
                    "output": {
                        "results": [
                            {"index": 0, "relevance_score": 0.9},
                            {"index": 99, "relevance_score": 0.8},
                        ]
                    }
                }
            )
            result = await rerank(_criteria(), products)
        assert len(result) == 1
        assert result[0].product_id == "p1"

    @pytest.mark.asyncio
    async def test_top_n_in_payload(self):
        products = [_product("p1"), _product("p2"), _product("p3")]
        with patch.object(httpx.AsyncClient, "post", new_callable=AsyncMock) as mock_post:
            mock_post.return_value = _fake_response({"output": {"results": [{"index": 0, "relevance_score": 0.9}]}})
            await rerank(_criteria(), products, top_n=2)
            payload = mock_post.call_args.kwargs.get("json") or mock_post.call_args[1]["json"]
            assert payload["parameters"]["top_n"] == 2


class TestRerankTexts:
    @pytest.mark.asyncio
    async def test_rerank_texts_returns_sorted_indexes(self):
        with patch.object(httpx.AsyncClient, "post", new_callable=AsyncMock) as mock_post:
            mock_post.return_value = _fake_response(
                {
                    "output": {
                        "results": [
                            {"index": 1, "relevance_score": 0.95},
                            {"index": 0, "relevance_score": 0.80},
                        ]
                    }
                }
            )
            result = await rerank_texts(_criteria(), ["doc_a", "doc_b"])
        assert result == [1, 0]


class TestRerankFailurePaths:
    @pytest.mark.asyncio
    async def test_all_indexes_out_of_range_raises(self):
        products = [_product("p1")]
        with patch.object(httpx.AsyncClient, "post", new_callable=AsyncMock) as mock_post:
            mock_post.return_value = _fake_response(
                {
                    "output": {
                        "results": [
                            {"index": 99, "relevance_score": 0.9},
                        ]
                    }
                }
            )
            with pytest.raises(RerankUnavailable, match="no valid"):
                await rerank(_criteria(), products)
