"""Expanded multimodal pipeline tests — failure modes and edge cases.

Audit gap: test_multimodal_pipeline.py had only 1 test. These cover:
- No image URL → skip multimodal (no VL call)
- VL analysis timeout/exception → graceful degradation
- Empty/invalid image URL handling
- run_image_embedding graceful degradation on failure
"""

from __future__ import annotations

import pytest

from src.runtime.stages.multimodal import run_image_embedding, run_multimodal
from src.services.embedding import EmbeddingUnavailable


# ── run_multimodal ──────────────────────────────────────────────────────────


class TestRunMultimodal:
    @pytest.mark.asyncio
    async def test_none_image_url_returns_none(self, monkeypatch):
        """No image → no VL call, returns None immediately."""
        called = False

        async def fake_analyze_image(url):
            nonlocal called
            called = True
            return {}

        monkeypatch.setattr("src.runtime.stages.multimodal.analyze_image", fake_analyze_image)
        result = await run_multimodal(None)
        assert result is None
        assert called is False

    @pytest.mark.asyncio
    async def test_empty_string_image_url_returns_none(self, monkeypatch):
        """Empty string is treated as no image."""
        called = False

        async def fake_analyze_image(url):
            nonlocal called
            called = True
            return {}

        monkeypatch.setattr("src.runtime.stages.multimodal.analyze_image", fake_analyze_image)
        result = await run_multimodal("")
        assert result is None
        assert called is False

    @pytest.mark.asyncio
    async def test_valid_image_url_calls_analyze_image(self, monkeypatch):
        """Valid URL triggers VL analysis."""
        captured_url = None

        async def fake_analyze_image(url):
            nonlocal captured_url
            captured_url = url
            return {"category": "美妆护肤", "constraints": {"skin_type": "油性"}}

        monkeypatch.setattr("src.runtime.stages.multimodal.analyze_image", fake_analyze_image)
        result = await run_multimodal("https://example.com/image.jpg")
        assert result is not None
        assert result["category"] == "美妆护肤"
        assert captured_url == "https://example.com/image.jpg"

    @pytest.mark.asyncio
    async def test_vl_exception_propagates(self, monkeypatch):
        """VL analysis failure propagates (pipeline handles at higher level)."""

        async def fake_analyze_image(url):
            raise RuntimeError("VL API timeout")

        monkeypatch.setattr("src.runtime.stages.multimodal.analyze_image", fake_analyze_image)
        with pytest.raises(RuntimeError, match="VL API timeout"):
            await run_multimodal("https://example.com/image.jpg")


# ── run_image_embedding ─────────────────────────────────────────────────────


class TestRunImageEmbedding:
    @pytest.mark.asyncio
    async def test_none_image_url_returns_none(self, monkeypatch):
        """No image → no embedding call."""
        called = False

        async def fake_embed_image(url):
            nonlocal called
            called = True
            return [0.1] * 1024

        monkeypatch.setattr("src.runtime.stages.multimodal.embed_image", fake_embed_image)
        result = await run_image_embedding(None)
        assert result is None
        assert called is False

    @pytest.mark.asyncio
    async def test_empty_string_returns_none(self, monkeypatch):
        """Empty string treated as no image."""
        called = False

        async def fake_embed_image(url):
            nonlocal called
            called = True
            return [0.1] * 1024

        monkeypatch.setattr("src.runtime.stages.multimodal.embed_image", fake_embed_image)
        result = await run_image_embedding("")
        assert result is None
        assert called is False

    @pytest.mark.asyncio
    async def test_embedding_failure_returns_none_gracefully(self, monkeypatch):
        """EmbeddingUnavailable → graceful degradation to text-only (returns None)."""

        async def fake_embed_image(url):
            raise EmbeddingUnavailable("provider down")

        monkeypatch.setattr("src.runtime.stages.multimodal.embed_image", fake_embed_image)
        monkeypatch.setattr(
            "src.runtime.stages.multimodal.image_url_to_provider_url",
            lambda url: url,
        )
        result = await run_image_embedding("https://example.com/image.jpg")
        assert result is None

    @pytest.mark.asyncio
    async def test_generic_exception_returns_none_gracefully(self, monkeypatch):
        """Any exception → graceful degradation (returns None, doesn't crash)."""

        async def fake_embed_image(url):
            raise ConnectionError("network down")

        monkeypatch.setattr("src.runtime.stages.multimodal.embed_image", fake_embed_image)
        monkeypatch.setattr(
            "src.runtime.stages.multimodal.image_url_to_provider_url",
            lambda url: url,
        )
        result = await run_image_embedding("https://example.com/image.jpg")
        assert result is None

    @pytest.mark.asyncio
    async def test_success_returns_embedding_vector(self, monkeypatch):
        """Successful embedding returns 1024-dim vector."""
        expected = [0.1] * 1024

        async def fake_embed_image(url):
            return expected

        monkeypatch.setattr("src.runtime.stages.multimodal.embed_image", fake_embed_image)
        monkeypatch.setattr(
            "src.runtime.stages.multimodal.image_url_to_provider_url",
            lambda url: url,
        )
        result = await run_image_embedding("https://example.com/image.jpg")
        assert result == expected
        assert len(result) == 1024
