import pytest

import src.config.settings as settings_module
from src.services import llm_client
from src.services.llm_gateway import LiveLLMUnavailable
from src.services.llm_task_payloads import normalize_intent_payload
from src.services.prompts import PromptStore
from src.types.schemas import IntentResult


def _reset_settings() -> None:
    settings_module._settings = None


def test_prompt_store_renders_template_variables(tmp_path):
    prompts_dir = tmp_path / "prompts"
    prompts_dir.mkdir()
    (prompts_dir / "demo.md").write_text("hello {name}: {payload}", encoding="utf-8")

    rendered = PromptStore(prompts_dir).render("demo", "fallback", {"name": "buyer", "payload": {"a": 1}})

    assert rendered == 'hello buyer: {"a": 1}'


def test_prompt_store_uses_fallback_when_file_missing(tmp_path):
    rendered = PromptStore(tmp_path).render("missing", "fallback {value}", {"value": "ok"})

    assert rendered == "fallback ok"


def test_normalize_intent_payload_accepts_legacy_prompt_shape():
    normalized = normalize_intent_payload(
        {
            "intent_type": "filter",
            "is_shopping_related": True,
            "category": "null",
            "constraints": {"budget_max": 200},
            "user_intent_summary": "寻找200元以内的洗面奶",
            "confidence": "high",
            "target_product_id": 123,
        }
    )

    result = IntentResult.model_validate(normalized)

    assert result.intent == "recommend"
    assert result.confidence == 0.9
    assert result.category is None
    assert result.extracted_constraints == {"budget_max": 200}
    assert result.soft_preferences == ["寻找200元以内的洗面奶"]
    assert result.target_product_id == "123"


def test_normalize_intent_payload_sanitizes_common_bad_shapes():
    normalized = normalize_intent_payload(
        {
            "intent": "不喜欢",
            "is_shopping_related": "true",
            "category": ["美妆护肤"],
            "extracted_constraints": None,
            "soft_preferences": "不要这个牌子",
            "confidence": "90%",
            "target_product_id": "",
        }
    )

    result = IntentResult.model_validate(normalized)

    assert result.intent == "feedback"
    assert result.confidence == 0.9
    assert result.category is None
    assert result.extracted_constraints == {}
    assert result.soft_preferences == ["不要这个牌子"]
    assert result.target_product_id is None


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
async def test_live_llm_strict_mode_raises_when_profile_config_missing(monkeypatch):
    monkeypatch.setenv("STRICT_RUNTIME", "1")
    monkeypatch.delenv("BAILIAN_BASE_URL", raising=False)
    monkeypatch.delenv("BAILIAN_API_KEY", raising=False)
    monkeypatch.delenv("DOUBAO_BASE_URL", raising=False)
    monkeypatch.delenv("DOUBAO_API_KEY", raising=False)
    _reset_settings()

    with pytest.raises(LiveLLMUnavailable):
        await llm_client._call_chat_task("generate_criteria", [], json_object=True)
    _reset_settings()


@pytest.mark.asyncio
async def test_analyze_intent_does_not_parse_age_as_budget():
    result = await llm_client.analyze_intent("给4岁孩子买零食")

    assert "budget_max" not in result.extracted_constraints


@pytest.mark.asyncio
async def test_analyze_intent_parses_budget_without_yuan_after_budget_keyword():
    result = await llm_client.analyze_intent("推荐跑鞋，预算500以内，日常训练")

    assert result.extracted_constraints["budget_max"] == 500
