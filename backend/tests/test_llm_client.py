import pytest

import src.config.settings as settings_module
from src.services import llm_client
from src.services.llm_gateway import LiveLLMUnavailable
from src.services.llm_task_payloads import normalize_intent_payload, recommendation_messages
from src.services.prompts import PromptStore, PromptTemplateMissing
from src.repos.products import list_products
from src.types.schemas import IntentResult
from src.types.sse_events import CriteriaPayload, ProductPayload, ReasonAtomPayload


def _reset_settings() -> None:
    settings_module._settings = None


def test_prompt_store_renders_template_variables(tmp_path):
    prompts_dir = tmp_path / "prompts"
    prompts_dir.mkdir()
    (prompts_dir / "demo.md").write_text("hello {name}: {payload}", encoding="utf-8")

    rendered = PromptStore(prompts_dir).render("demo", {"name": "buyer", "payload": {"a": 1}})

    assert rendered == 'hello buyer: {"a": 1}'


def test_prompt_store_raises_when_file_missing(tmp_path):
    with pytest.raises(PromptTemplateMissing):
        PromptStore(tmp_path).render("missing", {"value": "ok"})


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
async def test_live_llm_raises_when_profile_config_missing(monkeypatch):
    monkeypatch.delenv("BAILIAN_BASE_URL", raising=False)
    monkeypatch.delenv("BAILIAN_API_KEY", raising=False)
    monkeypatch.delenv("DOUBAO_BASE_URL", raising=False)
    monkeypatch.delenv("DOUBAO_API_KEY", raising=False)
    _reset_settings()

    with pytest.raises(LiveLLMUnavailable):
        await llm_client._call_chat_task("generate_criteria", [], json_object=True)


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


def test_recommendation_guard_rejects_unknown_product_id():
    product = ProductPayload(product_id="p_beauty_011", name="测试洁面", category="美妆护肤")

    with pytest.raises(RuntimeError, match="unknown product ids"):
        llm_client._validate_recommendation_chunks(["推荐 p_fake_001，因为它很适合。"], [product])


def test_recommendation_guard_rejects_non_candidate_product_name():
    products = list_products()

    with pytest.raises(RuntimeError, match="outside the candidate set"):
        llm_client._validate_recommendation_chunks([f"也可以考虑{products[1].name}。"], [products[0]])


def test_recommendation_guard_rejects_unsupported_commercial_claims():
    product = ProductPayload(product_id="p_beauty_011", name="测试洁面", category="美妆护肤")

    with pytest.raises(RuntimeError, match="unsupported commercial claims"):
        llm_client._validate_recommendation_chunks(["这款库存充足，还有优惠券。"], [product])


def test_recommendation_prompt_includes_reason_atoms():
    messages = recommendation_messages(
        CriteriaPayload(category="美妆护肤"),
        [ProductPayload(product_id="p1", name="测试洁面", category="美妆护肤")],
        reason_atoms_by_product={
            "p1": [
                ReasonAtomPayload(
                    dimension="skin_type",
                    value="油性",
                    text="油性肤质匹配",
                    evidence_id="chunk_1",
                )
            ]
        },
    )

    assert "已校验推荐理由事实原子" in messages[0]["content"]
    assert "油性肤质匹配" in messages[-1]["content"]
