import pytest

import src.config.settings as settings_module
from src.services import embedding
from src.services.embedding import embed_text


def _reset_settings() -> None:
    settings_module._settings = None


@pytest.mark.asyncio
async def test_embed_text_falls_back_when_config_missing(monkeypatch):
    monkeypatch.delenv("BAILIAN_BASE_URL", raising=False)
    monkeypatch.delenv("BAILIAN_API_KEY", raising=False)
    monkeypatch.delenv("DOUBAO_BASE_URL", raising=False)
    monkeypatch.delenv("DOUBAO_API_KEY", raising=False)
    _reset_settings()

    vector = await embed_text("油皮洗面奶")

    assert len(vector) == 16


@pytest.mark.asyncio
async def test_embed_text_strict_mode_raises_when_config_missing(monkeypatch):
    monkeypatch.setenv("STRICT_RUNTIME", "1")
    monkeypatch.delenv("BAILIAN_BASE_URL", raising=False)
    monkeypatch.delenv("BAILIAN_API_KEY", raising=False)
    monkeypatch.delenv("DOUBAO_BASE_URL", raising=False)
    monkeypatch.delenv("DOUBAO_API_KEY", raising=False)
    _reset_settings()

    with pytest.raises(embedding.EmbeddingUnavailable):
        await embed_text("油皮洗面奶")
    _reset_settings()
