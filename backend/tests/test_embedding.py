import pytest

import src.config.settings as settings_module
from src.services import embedding
from src.services.embedding import embed_text, embed_texts


def _reset_settings() -> None:
    settings_module._settings = None


@pytest.mark.asyncio
async def test_embed_texts_uses_live_embeddings_when_configured(monkeypatch):
    monkeypatch.setenv("BAILIAN_BASE_URL", "https://example.test/v1")
    monkeypatch.setenv("BAILIAN_API_KEY", "test-key")
    _reset_settings()

    async def fake_embedding_request(profile, texts):
        assert profile.name == "qwen_embedding"
        assert profile.model == "text-embedding-v3"
        assert profile.dimensions == 1024
        return [[1.0, 0.0], [0.0, 1.0]]

    monkeypatch.setattr(embedding, "_embedding_request", fake_embedding_request)

    assert await embed_texts(["a", "b"]) == [[1.0, 0.0], [0.0, 1.0]]


@pytest.mark.asyncio
async def test_embed_text_falls_back_when_config_missing(monkeypatch):
    monkeypatch.delenv("BAILIAN_BASE_URL", raising=False)
    monkeypatch.delenv("BAILIAN_API_KEY", raising=False)
    monkeypatch.delenv("DOUBAO_BASE_URL", raising=False)
    monkeypatch.delenv("DOUBAO_API_KEY", raising=False)
    _reset_settings()

    vector = await embed_text("油皮洗面奶")

    assert len(vector) == 16
