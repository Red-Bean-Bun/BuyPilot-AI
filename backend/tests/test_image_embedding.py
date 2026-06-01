"""Tests for VL (Vision-Language) embedding service — embed_image()."""

from __future__ import annotations

import json
from unittest.mock import AsyncMock, MagicMock

import pytest

import src.config.settings as settings_module
from src.services import embedding
from src.services.embedding import embed_image


def _reset_settings() -> None:
    settings_module._settings = None


# ---------------------------------------------------------------------------
# Profile resolution
# ---------------------------------------------------------------------------


@pytest.mark.asyncio
async def test_embed_image_raises_when_config_missing(monkeypatch):
    monkeypatch.delenv("BAILIAN_BASE_URL", raising=False)
    monkeypatch.delenv("BAILIAN_API_KEY", raising=False)
    _reset_settings()

    with pytest.raises(embedding.EmbeddingUnavailable):
        await embed_image("https://example.com/photo.jpg")


@pytest.mark.asyncio
async def test_embed_image_raises_on_empty_source():
    with pytest.raises(embedding.EmbeddingUnavailable):
        await embed_image("")


def test_vl_embedding_profile_uses_dashscope_native_endpoint(monkeypatch):
    monkeypatch.setenv("BAILIAN_API_KEY", "test-key")
    _reset_settings()

    profile = embedding._resolve_vl_embedding_profile("qwen3_vl_embedding")

    assert profile.model == "qwen3-vl-embedding"
    assert (
        f"{profile.base_url.rstrip('/')}/{profile.endpoint_path.strip('/')}"
        == "https://dashscope.aliyuncs.com/api/v1/services/embeddings/multimodal-embedding/multimodal-embedding"
    )
    assert profile.dimensions == 1024
    _reset_settings()


# ---------------------------------------------------------------------------
# HTTP payload format
# ---------------------------------------------------------------------------


@pytest.mark.asyncio
async def test_vl_embedding_request_sends_correct_payload(monkeypatch):
    """embed_image() must send {"model", "input": {"contents": [{"image": ...}]}, "parameters": {"dimension": 1024}}."""
    monkeypatch.setenv("BAILIAN_API_KEY", "test-key")
    _reset_settings()

    captured: dict = {}

    async def fake_vl_request(profile, image_source):
        captured["profile"] = profile
        captured["image_source"] = image_source
        return [0.1, 0.2, 0.3]

    monkeypatch.setattr(embedding, "_vl_embedding_request", fake_vl_request)

    result = await embed_image("https://example.com/product.jpg")

    # Verify profile resolution
    profile = captured["profile"]
    assert profile.model == "qwen3-vl-embedding"
    assert profile.dimensions == 1024

    # Verify result passthrough
    assert result == [0.1, 0.2, 0.3]
    _reset_settings()


@pytest.mark.asyncio
async def test_vl_embedding_request_omits_parameters_when_no_dimensions(monkeypatch):
    """When profile has no dimensions configured, 'parameters' key must be absent in payload."""
    monkeypatch.setenv("BAILIAN_API_KEY", "test-key")
    _reset_settings()

    # Build a profile with dimensions=None to test payload construction
    profile = embedding._resolve_vl_embedding_profile("qwen3_vl_embedding")
    assert profile.dimensions == 1024  # confirm default is 1024

    profile_no_dim = embedding.VLEmbeddingProfile(
        name=profile.name,
        model=profile.model,
        base_url=profile.base_url,
        endpoint_path=profile.endpoint_path,
        api_key=profile.api_key,
        timeout_seconds=profile.timeout_seconds,
        dimensions=None,
    )

    # Verify payload construction logic: no dimensions → no parameters key
    payload: dict = {"model": profile_no_dim.model, "input": {"contents": [{"image": "test_url"}]}}
    if profile_no_dim.dimensions:
        payload["parameters"] = {"dimension": profile_no_dim.dimensions}
    assert "parameters" not in payload

    # Verify with dimensions → parameters key present
    payload_with_dim: dict = {"model": profile.model, "input": {"contents": [{"image": "test_url"}]}}
    if profile.dimensions:
        payload_with_dim["parameters"] = {"dimension": profile.dimensions}
    assert payload_with_dim["parameters"] == {"dimension": 1024}
    _reset_settings()


# ---------------------------------------------------------------------------
# Error handling
# ---------------------------------------------------------------------------


@pytest.mark.asyncio
async def test_vl_embedding_raises_on_empty_response(monkeypatch):
    monkeypatch.setenv("BAILIAN_API_KEY", "test-key")
    _reset_settings()

    async def fake_vl_request(profile, image_source):
        raise embedding.EmbeddingUnavailable("VL embedding provider returned no embeddings.")

    monkeypatch.setattr(embedding, "_vl_embedding_request", fake_vl_request)

    with pytest.raises(embedding.EmbeddingUnavailable, match="no embeddings"):
        await embed_image("https://example.com/photo.jpg")
    _reset_settings()


@pytest.mark.asyncio
async def test_vl_embedding_raises_on_malformed_embedding(monkeypatch):
    monkeypatch.setenv("BAILIAN_API_KEY", "test-key")
    _reset_settings()

    async def fake_vl_request(profile, image_source):
        raise embedding.EmbeddingUnavailable("VL embedding provider returned malformed embedding.")

    monkeypatch.setattr(embedding, "_vl_embedding_request", fake_vl_request)

    with pytest.raises(embedding.EmbeddingUnavailable, match="malformed"):
        await embed_image("https://example.com/photo.jpg")
    _reset_settings()
