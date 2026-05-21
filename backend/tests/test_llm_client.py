import json

import pytest

import src.config.settings as settings_module
from src.services import llm_client
from src.services.llm_client import generate_criteria, generate_recommendation
from src.types.schemas import IntentResult
from src.types.sse_events import CriteriaPayload, ProductPayload


def _reset_settings() -> None:
    settings_module._settings = None


@pytest.mark.asyncio
async def test_live_llm_falls_back_when_profile_config_missing(monkeypatch):
    monkeypatch.delenv("BAILIAN_BASE_URL", raising=False)
    monkeypatch.delenv("BAILIAN_API_KEY", raising=False)
    monkeypatch.delenv("DOUBAO_BASE_URL", raising=False)
    monkeypatch.delenv("DOUBAO_API_KEY", raising=False)
    _reset_settings()

    result = await llm_client._call_chat_task("generate_criteria", [], json_object=True)

    assert result is None


@pytest.mark.asyncio
async def test_generate_criteria_uses_live_json_when_enabled(monkeypatch):
    monkeypatch.setenv("BAILIAN_BASE_URL", "https://example.test/v1")
    monkeypatch.setenv("BAILIAN_API_KEY", "test-key")
    _reset_settings()

    async def fake_chat_completion(profile, messages, json_object=False):
        assert profile.name == "qwen_plus"
        assert json_object is True
        return json.dumps(
            {
                "criteria_id": "c_live_001",
                "category": "美妆护肤",
                "summary": "油性肌肤，200元内",
                "chips": ["美妆护肤", "油性肌肤", "200元内"],
                "constraints": {"skin_type": "油性", "budget_max": 200},
            },
            ensure_ascii=False,
        )

    monkeypatch.setattr(llm_client, "_chat_completion", fake_chat_completion)

    criteria = await generate_criteria(
        "推荐适合油皮的洗面奶，200元以内",
        IntentResult(intent="recommend", category="美妆护肤"),
    )

    assert criteria.criteria_id == "c_live_001"
    assert criteria.constraints.skin_type == "油性"
    assert criteria.constraints.budget_max == 200


@pytest.mark.asyncio
async def test_generate_recommendation_keeps_retrieved_products(monkeypatch):
    monkeypatch.setenv("BAILIAN_BASE_URL", "https://example.test/v1")
    monkeypatch.setenv("BAILIAN_API_KEY", "test-key")
    _reset_settings()

    async def fake_chat_completion(profile, messages, json_object=False):
        return json.dumps({"text_chunks": ["这两款都来自真实检索结果。"]}, ensure_ascii=False)

    monkeypatch.setattr(llm_client, "_chat_completion", fake_chat_completion)
    product = ProductPayload(product_id="p1", name="测试商品", category="美妆护肤")

    result = await generate_recommendation(CriteriaPayload(category="美妆护肤"), [product])

    assert result.text_chunks == ["这两款都来自真实检索结果。"]
    assert result.products == [product]
