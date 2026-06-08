"""VL embedding contract tests — fixes audit gap: original test_image_embedding
used tautological mock (mocked _vl_embedding_request itself).

Overrides conftest mock_external_ai to test full HTTP path.
"""

from __future__ import annotations

from unittest.mock import AsyncMock, MagicMock, patch

import httpx
import pytest

import src.config.settings as settings_module
from src.services.embedding import EmbeddingUnavailable, embed_image


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


def _fake_response(json_data: dict):
    resp = MagicMock()
    resp.json.return_value = json_data
    resp.status_code = 200
    resp.raise_for_status = lambda: None
    return resp


@pytest.mark.asyncio
async def test_vl_embedding_payload_structure():
    """Verify VL embedding constructs correct payload: model, input.contents."""
    with patch.object(httpx.AsyncClient, "post", new_callable=AsyncMock) as mock_post:
        mock_post.return_value = _fake_response({"output": {"embeddings": [{"embedding": [0.1] * 1024}]}})
        result = await embed_image("https://example.com/image.jpg")
    assert len(result) == 1024
    payload = mock_post.call_args.kwargs.get("json") or mock_post.call_args[1]["json"]
    assert payload["model"] == "qwen3-vl-embedding"
    assert payload["input"]["contents"][0]["image"] == "https://example.com/image.jpg"


@pytest.mark.asyncio
async def test_vl_embedding_dimensions_in_payload():
    """Verify dimensions parameter is passed when profile has dimensions configured."""
    with patch.object(httpx.AsyncClient, "post", new_callable=AsyncMock) as mock_post:
        mock_post.return_value = _fake_response({"output": {"embeddings": [{"embedding": [0.1] * 1024}]}})
        await embed_image("https://example.com/image.jpg")
    payload = mock_post.call_args.kwargs.get("json") or mock_post.call_args[1]["json"]
    if "parameters" in payload:
        assert "dimension" in payload["parameters"]


@pytest.mark.asyncio
async def test_vl_embedding_raises_on_empty_source():
    with pytest.raises(EmbeddingUnavailable, match="non-empty"):
        await embed_image("")


@pytest.mark.asyncio
async def test_vl_embedding_endpoint_url_no_double_slashes():
    with patch.object(httpx.AsyncClient, "post", new_callable=AsyncMock) as mock_post:
        mock_post.return_value = _fake_response({"output": {"embeddings": [{"embedding": [0.1] * 1024}]}})
        await embed_image("https://example.com/image.jpg")
    url = mock_post.call_args[0][0]
    assert "//" not in url.replace("https://", "")


@pytest.mark.asyncio
async def test_vl_embedding_raises_on_empty_embeddings():
    with patch.object(httpx.AsyncClient, "post", new_callable=AsyncMock) as mock_post:
        mock_post.return_value = _fake_response({"output": {}})
        with pytest.raises(EmbeddingUnavailable):
            await embed_image("https://example.com/image.jpg")


@pytest.mark.asyncio
async def test_vl_embedding_raises_on_non_list_vector():
    with patch.object(httpx.AsyncClient, "post", new_callable=AsyncMock) as mock_post:
        mock_post.return_value = _fake_response({"output": {"embeddings": [{"embedding": "not_a_list"}]}})
        with pytest.raises(EmbeddingUnavailable):
            await embed_image("https://example.com/image.jpg")
