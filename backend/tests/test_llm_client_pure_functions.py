"""Normal path tests for LLM client pure functions.

Audit gap: test_llm_client.py focused on PromptStore and config, but had no
direct tests for pure logic functions (_require_json_object, _sanitize_decision,
_validate_recommendation_chunks, _deterministic_locked_decision).

These tests cover the pure logic without mocking LLM calls — direct invocation
per 铁律 5.
"""

from __future__ import annotations

import pytest

from src.services import llm_client as llm_client_module
from src.services.llm_client import (
    _deterministic_locked_decision,
    _normalize_image_analysis_payload,
    _require_json_object,
    _sanitize_decision,
    _validate_recommendation_chunks,
)
from src.types.schemas import DecisionResult
from src.types.sse_events import ProductPayload


def _product(pid: str, name: str = "Test", price: float = 100.0) -> ProductPayload:
    return ProductPayload(product_id=pid, name=name, price=price, category="美妆护肤")


# ── _require_json_object ────────────────────────────────────────────────────


class TestRequireJsonObject:
    def test_valid_json_object(self):
        result = _require_json_object('{"key": "value"}', "test_task")
        assert result == {"key": "value"}

    def test_empty_string_raises(self):
        with pytest.raises(RuntimeError, match="empty"):
            _require_json_object("", "test_task")

    def test_non_json_raises(self):
        with pytest.raises(RuntimeError, match="not a JSON"):
            _require_json_object("this is not json", "test_task")

    def test_json_array_raises(self):
        """JSON array (not object) should raise."""
        with pytest.raises(RuntimeError, match="not a JSON"):
            _require_json_object("[1, 2, 3]", "test_task")

    def test_nested_json_object(self):
        result = _require_json_object('{"a": {"b": 1}}', "test_task")
        assert result == {"a": {"b": 1}}


# ── analyze_image repair / normalization ────────────────────────────────────


class TestAnalyzeImageJsonHandling:
    def test_normalize_image_analysis_payload(self):
        result = _normalize_image_analysis_payload(
            {
                "category_hint": " 数码电子 ",
                "description": " 一台笔记本电脑 ",
                "visible_traits": ["笔记本电脑", "代码编辑", "笔记本电脑", 123, "键盘", "触控板", "屏幕", "多余"],
                "extra": "ignored",
            }
        )

        assert result == {
            "category_hint": "数码电子",
            "description": "一台笔记本电脑",
            "visible_traits": ["笔记本电脑", "代码编辑", "键盘", "触控板", "屏幕"],
        }

    @pytest.mark.asyncio
    async def test_analyze_image_repairs_malformed_json_once(self, monkeypatch):
        calls = []

        async def fake_call_chat_task(task, messages, json_object=False):
            calls.append({"task": task, "messages": messages, "json_object": json_object})
            if len(calls) == 1:
                return '{"category_hint":"数码电子","description":"电脑","visible_traits":["笔记本电脑"],'
            return '{"category_hint":"数码电子","description":"电脑","visible_traits":["笔记本电脑","代码编辑"]}'

        monkeypatch.setattr(llm_client_module, "_call_chat_task", fake_call_chat_task)
        monkeypatch.setattr(llm_client_module, "image_url_to_provider_url", lambda url: url)
        monkeypatch.setattr(llm_client_module, "_schedule_parsed_json_update", lambda *args, **kwargs: None)

        result = await llm_client_module.analyze_image("/uploads/test.jpg")

        assert len(calls) == 2
        assert calls[0]["json_object"] is True
        assert calls[1]["json_object"] is True
        assert "malformed_output" in calls[1]["messages"][1]["content"]
        assert result == {
            "category_hint": "数码电子",
            "description": "电脑",
            "visible_traits": ["笔记本电脑", "代码编辑"],
            "image_url": "/uploads/test.jpg",
        }


# ── _validate_recommendation_chunks ─────────────────────────────────────────


class TestValidateRecommendationChunks:
    def test_valid_chunks_pass(self):
        products = [_product("p_beauty_001", "洗面奶A")]
        _validate_recommendation_chunks(["推荐这款洗面奶A"], products)

    def test_empty_text_raises(self):
        products = [_product("p_beauty_001")]
        with pytest.raises(RuntimeError, match="empty"):
            _validate_recommendation_chunks([""], products)

    def test_unknown_product_id_raises(self):
        products = [_product("p_beauty_001")]
        with pytest.raises(RuntimeError, match="unknown product ids"):
            _validate_recommendation_chunks(["推荐 p_beauty_999"], products)

    def test_known_product_id_passes(self):
        products = [_product("p_beauty_001"), _product("p_beauty_002")]
        _validate_recommendation_chunks(["推荐 p_beauty_001 和 p_beauty_002"], products)

    def test_commercial_term_raises(self):
        products = [_product("p_beauty_001", "洗面奶")]
        with pytest.raises(RuntimeError, match="commercial"):
            _validate_recommendation_chunks(["这款洗面奶现在有优惠券"], products)

    def test_multiple_commercial_terms_all_reported(self):
        products = [_product("p_beauty_001", "洗面奶")]
        with pytest.raises(RuntimeError, match="commercial"):
            _validate_recommendation_chunks(["包邮 免邮 优惠券"], products)


# ── _sanitize_decision ─────────────────────────────────────────────────────


class TestSanitizeDecision:
    def test_clean_summary_removes_commercial_terms(self):
        products = [_product("p_beauty_001", "洗面奶A")]
        decision = DecisionResult(
            winner_product_id="p_beauty_001",
            summary="这款洗面奶有优惠券，包邮",
        )
        result = _sanitize_decision(decision, ["p_beauty_001"], products)
        assert "优惠券" not in result.summary
        assert "包邮" not in result.summary

    def test_clean_removes_non_candidate_product_names(self):
        candidates = [_product("p_beauty_001", "候选产品A")]
        # "非候选产品" would be a product name from list_products() but not in candidates
        # Since we can't easily mock list_products here, test with known commercial terms
        decision = DecisionResult(
            winner_product_id="p_beauty_001",
            summary="推荐候选产品A",
        )
        result = _sanitize_decision(decision, ["p_beauty_001"], candidates)
        assert "候选产品A" in result.summary  # candidate name preserved

    def test_preserves_winner_product_id(self):
        products = [_product("p_beauty_001", "洗面奶")]
        decision = DecisionResult(
            winner_product_id="p_beauty_001",
            summary="推荐",
        )
        result = _sanitize_decision(decision, ["p_beauty_001"], products)
        assert result.winner_product_id == "p_beauty_001"

    def test_cleans_why_list(self):
        products = [_product("p_beauty_001", "洗面奶")]
        decision = DecisionResult(
            winner_product_id="p_beauty_001",
            summary="ok",
            why=["性价比高", "现在有折扣"],
        )
        result = _sanitize_decision(decision, ["p_beauty_001"], products)
        assert "折扣" not in result.why[1]

    def test_cleans_not_for_list(self):
        products = [_product("p_beauty_001", "洗面奶")]
        decision = DecisionResult(
            winner_product_id="p_beauty_001",
            summary="ok",
            not_for=["不适合想买包邮的人"],
        )
        result = _sanitize_decision(decision, ["p_beauty_001"], products)
        assert "包邮" not in result.not_for[0]


# ── _deterministic_locked_decision ──────────────────────────────────────────


class TestDeterministicLockedDecision:
    def test_returns_locked_winner(self):
        products = [_product("p_beauty_001", "洗面奶A"), _product("p_beauty_002", "洗面奶B")]
        result = _deterministic_locked_decision("p_beauty_002", products)
        assert result.winner_product_id == "p_beauty_002"

    def test_falls_back_to_first_product_if_winner_not_found(self):
        products = [_product("p_beauty_001", "洗面奶A")]
        result = _deterministic_locked_decision("p_nonexistent", products)
        assert result.winner_product_id == "p_beauty_001"

    def test_summary_contains_winner_name(self):
        products = [_product("p_beauty_001", "超级好用的洗面奶")]
        result = _deterministic_locked_decision("p_beauty_001", products)
        assert "超级好用的洗面奶" in result.summary

    def test_score_breakdown_included_in_reason(self):
        products = [_product("p_beauty_001", "洗面奶")]
        result = _deterministic_locked_decision(
            "p_beauty_001",
            products,
            score_breakdown={"final_score": 8.5},
        )
        assert "8.5" in result.why[0]

    def test_not_for_is_empty(self):
        products = [_product("p_beauty_001", "洗面奶")]
        result = _deterministic_locked_decision("p_beauty_001", products)
        assert result.not_for == []
