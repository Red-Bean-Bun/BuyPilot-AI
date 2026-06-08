"""Normal path tests for embedding service.

Audit gap: test_embedding.py only tested exception paths.
These tests cover: empty input, single/batch vectors, count mismatch, malformed response.

Note: Overrides conftest mock_external_ai to avoid pre-mocked internal functions —
we test the full HTTP path via httpx.AsyncClient.post.
"""

from __future__ import annotations

from unittest.mock import AsyncMock, MagicMock, patch

import httpx
import pytest

import src.config.settings as settings_module
from src.services.embedding import EmbeddingUnavailable, embed_text, embed_texts


@pytest.fixture(autouse=True)
def mock_external_ai(monkeypatch):
    """Override conftest: only set env vars, don't mock internal functions."""
    monkeypatch.setenv("BAILIAN_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")
    monkeypatch.setenv("BAILIAN_API_KEY", "test-key")
    monkeypatch.setenv("DOUBAO_BASE_URL", "https://example.test/v1")
    monkeypatch.setenv("DOUBAO_API_KEY", "test-key")
    monkeypatch.setenv("OBSERVABILITY_LOCAL_ENABLED", "0")
    settings_module._settings = None
    import src.services.http_client as hc

    hc._CLIENT = None


def _fake_response(json_data: dict):
    resp = MagicMock()
    resp.json.return_value = json_data
    resp.status_code = 200
    resp.raise_for_status = lambda: None
    return resp


class TestEmbedTexts:
    @pytest.mark.asyncio
    async def test_empty_list_returns_empty(self):
        result = await embed_texts([])
        assert result == []

    @pytest.mark.asyncio
    async def test_single_text_returns_single_vector(self):
        vector = [0.125] * 1024
        with patch.object(httpx.AsyncClient, "post", new_callable=AsyncMock) as mock_post:
            mock_post.return_value = _fake_response({"data": [{"embedding": vector, "index": 0}]})
            result = await embed_texts(["hello"])
        assert len(result) == 1
        assert len(result[0]) == 1024
        assert result[0][0] == 0.125

    @pytest.mark.asyncio
    async def test_batch_texts_returns_matching_count(self):
        vectors = [[0.1] * 1024, [0.2] * 1024, [0.3] * 1024]
        with patch.object(httpx.AsyncClient, "post", new_callable=AsyncMock) as mock_post:
            mock_post.return_value = _fake_response(
                {"data": [{"embedding": v, "index": i} for i, v in enumerate(vectors)]}
            )
            result = await embed_texts(["a", "b", "c"])
        assert len(result) == 3

    @pytest.mark.asyncio
    async def test_vector_count_mismatch_raises(self):
        with patch.object(httpx.AsyncClient, "post", new_callable=AsyncMock) as mock_post:
            mock_post.return_value = _fake_response(
                {"data": [{"embedding": [0.1] * 1024, "index": i} for i in range(2)]}
            )
            with pytest.raises(EmbeddingUnavailable, match="vectors.*texts"):
                await embed_texts(["a", "b", "c"])

    @pytest.mark.asyncio
    async def test_malformed_response_raises(self):
        with patch.object(httpx.AsyncClient, "post", new_callable=AsyncMock) as mock_post:
            mock_post.return_value = _fake_response({"data": "not_a_list"})
            with pytest.raises(EmbeddingUnavailable):
                await embed_texts(["test"])

    @pytest.mark.asyncio
    async def test_payload_includes_dimensions(self):
        with patch.object(httpx.AsyncClient, "post", new_callable=AsyncMock) as mock_post:
            mock_post.return_value = _fake_response({"data": [{"embedding": [0.1] * 1024, "index": 0}]})
            await embed_texts(["test"])
            call_kwargs = mock_post.call_args
            payload = call_kwargs.kwargs.get("json") or call_kwargs[1].get("json", {})
            assert payload.get("dimensions") == 1024


class TestEmbedText:
    @pytest.mark.asyncio
    async def test_embed_text_returns_first_vector(self):
        vector = [0.5] * 1024
        with patch.object(httpx.AsyncClient, "post", new_callable=AsyncMock) as mock_post:
            mock_post.return_value = _fake_response({"data": [{"embedding": vector, "index": 0}]})
            result = await embed_text("hello")
        assert len(result) == 1024
        assert result[0] == 0.5


class TestEmbeddingProfileResolution:
    @pytest.mark.asyncio
    async def test_missing_api_key_raises(self, monkeypatch):
        monkeypatch.setenv("BAILIAN_API_KEY", "")
        settings_module._settings = None
        import src.services.http_client as hc

        hc._CLIENT = None
        with pytest.raises(EmbeddingUnavailable):
            await embed_texts(["test"])
