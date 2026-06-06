"""Tests for ShoppingStrategyService (P0 deterministic rules).

Covers:
- Gift + interest → scenario_strategy with fear_wrong_choice barrier
- Pure filter query → None
- Mixed scenario+filter → scenario_filter preserving explicit constraints
- retrieval_probe empty → no unsupported direction returned
"""

from __future__ import annotations

import pytest

from src.services.shopping_strategy import (
    RetrievalProbeFn,
    _classify_scene_type,
    _compute_filter_score,
    _compute_scene_score,
    _detect_barrier,
    build_scenario_reason_hint,
    build_shopping_strategy_plan,
    is_likely_shopping_strategy_request,
)
from src.types.schemas import ChatStreamRequest, IntentResult
from src.types.sse_events import Constraints, CriteriaPayload


class _FakeRetrievalResult:
    """Structurally compatible with RetrievalProbeResult protocol."""

    def __init__(self, products: list | None = None):
        self.products = products or []


def _request(message: str) -> ChatStreamRequest:
    return ChatStreamRequest(message=message)


def _intent(
    *,
    category: str | None = None,
    product_type: str | None = None,
    budget_max: float | None = None,
    brand_prefer: list[str] | None = None,
    extracted: dict | None = None,
) -> IntentResult:
    ext = dict(extracted or {})
    if product_type:
        ext["product_type"] = product_type
    if budget_max is not None:
        ext["budget_max"] = budget_max
    if brand_prefer:
        ext["brand_prefer"] = brand_prefer
    return IntentResult(intent="recommend", category=category, extracted_constraints=ext)


def _criteria(
    *,
    category: str = "",
    product_type: str | None = None,
    budget_max: float | None = None,
) -> CriteriaPayload:
    return CriteriaPayload(
        criteria_id="test_001",
        category=category,
        constraints=Constraints(product_type=product_type, budget_max=budget_max),
    )


def _probe_factory(product_count: int) -> RetrievalProbeFn:
    async def probe(criteria: CriteriaPayload) -> _FakeRetrievalResult:
        return _FakeRetrievalResult(products=[None] * product_count)

    return probe


# ---------------------------------------------------------------------------
# Scoring & classification unit tests (pure functions)
# ---------------------------------------------------------------------------


class TestScoring:
    def test_gift_scene_score(self):
        text = "男朋友生日，喜欢电子产品，不知道选什么"
        score = _compute_scene_score(text)
        # recipient(+2) + occasion(+2) + uncertainty(+2) + interest(+1) = 7
        assert score == 7

    def test_pure_filter_score(self):
        text = "2000 内蓝牙耳机有哪些"
        score = _compute_filter_score(text, {"product_type": "蓝牙耳机", "budget_max": 2000})
        # product_type(+3) + budget(+2) = 5
        assert score == 5

    def test_negated_terms_not_counted(self):
        text = "不要护肤品"
        score = _compute_scene_score(text)
        # "护肤" matched by _INTEREST_ALL but negated by "不要" → no score
        assert score == 0

    def test_classify_gift(self):
        assert _classify_scene_type("男朋友生日，喜欢电子产品") == "gift"

    def test_classify_interest(self):
        assert _classify_scene_type("我喜欢足球") == "interest"

    def test_classify_none(self):
        assert _classify_scene_type("蓝牙耳机") is None

    def test_barrier_fear_wrong_choice(self):
        barrier = _detect_barrier("男朋友生日", {}, "gift")
        assert barrier is not None
        assert barrier.barrier_type == "fear_wrong_choice"

    def test_barrier_price_sensitive(self):
        barrier = _detect_barrier("预算 2000", {"budget_max": 2000}, "gift")
        assert barrier is not None
        assert barrier.barrier_type == "price_sensitive"

    def test_barrier_fit_uncertainty(self):
        barrier = _detect_barrier("敏感肌", {}, "gift")
        assert barrier is not None
        assert barrier.barrier_type == "fit_uncertainty"


# ---------------------------------------------------------------------------
# Integration: build_shopping_strategy_plan
# ---------------------------------------------------------------------------


class TestBuildShoppingStrategyPlan:
    @pytest.mark.parametrize(
        ("message", "category", "probe_count"),
        [
            ("男朋友生日，喜欢电子产品", "数码电子", 3),
            ("男朋友喜欢足球，生日送什么", "服饰运动", 3),
            ("送妈妈一款护肤品，不知道怎么选", "美妆护肤", 3),
        ],
    )
    async def test_gift_scenarios(self, message: str, category: str, probe_count: int):
        plan = await build_shopping_strategy_plan(
            _request(message),
            _intent(category=category),
            _criteria(),
            retrieval_probe=_probe_factory(probe_count),
        )
        assert plan is not None
        assert plan.shopping_strategy.scene_type == "gift"
        assert plan.shopping_strategy.decision_barrier is not None
        assert plan.shopping_strategy.decision_barrier.barrier_type == "fear_wrong_choice"
        assert plan.shopping_strategy.primary_direction.available_in_catalog is True
        assert plan.shopping_strategy.primary_direction.supporting_product_count == probe_count

    async def test_pure_filter_returns_none(self):
        plan = await build_shopping_strategy_plan(
            _request("2000 内蓝牙耳机有哪些"),
            _intent(category="数码电子", product_type="蓝牙耳机", budget_max=2000),
            _criteria(category="数码电子", product_type="蓝牙耳机", budget_max=2000),
            retrieval_probe=_probe_factory(5),
        )
        assert plan is None

    async def test_scenario_filter_preserves_explicit_constraints(self):
        plan = await build_shopping_strategy_plan(
            _request("送男朋友一个 2000 内蓝牙耳机，怎么选"),
            _intent(category="数码电子", product_type="蓝牙耳机", budget_max=2000),
            _criteria(category="数码电子", product_type="蓝牙耳机", budget_max=2000),
            retrieval_probe=_probe_factory(2),
        )
        assert plan is not None
        assert plan.route == "scenario_filter"
        # Explicit user constraints preserved
        assert plan.criteria.constraints.product_type == "蓝牙耳机"
        assert plan.criteria.constraints.budget_max == 2000
        # Strategy must not override
        assert plan.shopping_strategy.primary_direction.available_in_catalog is True

    async def test_empty_probe_returns_none(self):
        plan = await build_shopping_strategy_plan(
            _request("男朋友生日，喜欢电子产品"),
            _intent(category="数码电子"),
            _criteria(),
            retrieval_probe=_probe_factory(0),
        )
        assert plan is None

    async def test_non_recommend_intent_returns_none(self):
        intent = IntentResult(intent="add_to_cart")
        plan = await build_shopping_strategy_plan(
            _request("把这个加到购物车"),
            intent,
            _criteria(),
        )
        assert plan is None

    async def test_vague_query_returns_none(self):
        plan = await build_shopping_strategy_plan(
            _request("有推荐吗"),
            _intent(),
            _criteria(),
        )
        assert plan is None

    async def test_strategy_fills_unspecified_category(self):
        """When criteria has no category but strategy direction has one, it's filled."""
        plan = await build_shopping_strategy_plan(
            _request("男朋友生日，喜欢电子产品"),
            _intent(category="数码电子"),
            _criteria(),  # empty category
            retrieval_probe=_probe_factory(2),
        )
        assert plan is not None
        # Category should be filled from strategy direction
        assert plan.criteria.category == "数码电子"

    async def test_gift_electronics_fills_catalog_supported_product_type(self):
        plan = await build_shopping_strategy_plan(
            _request("男朋友生日，喜欢电子产品"),
            _intent(category="数码电子"),
            _criteria(),
            retrieval_probe=_probe_factory(2),
        )
        assert plan is not None
        assert plan.shopping_strategy.primary_direction.search_strategy.product_type == "真无线耳机"
        assert plan.criteria.constraints.product_type == "真无线耳机"

    async def test_football_gift_uses_real_catalog_product_type(self):
        plan = await build_shopping_strategy_plan(
            _request("男朋友喜欢足球，生日送什么"),
            _intent(category="服饰运动"),
            _criteria(),
            retrieval_probe=_probe_factory(2),
        )
        assert plan is not None
        assert plan.shopping_strategy.primary_direction.search_strategy.product_type == "短袖T恤"
        assert plan.criteria.constraints.product_type == "短袖T恤"

    async def test_strategy_does_not_override_explicit_category(self):
        """When criteria already has category, strategy doesn't override."""
        plan = await build_shopping_strategy_plan(
            _request("男朋友生日，喜欢电子产品"),
            _intent(category="数码电子"),
            _criteria(category="数码电子"),
            retrieval_probe=_probe_factory(2),
        )
        assert plan is not None
        assert plan.criteria.category == "数码电子"

    async def test_plan_has_reason_hint(self):
        plan = await build_shopping_strategy_plan(
            _request("男朋友生日，喜欢电子产品"),
            _intent(category="数码电子"),
            _criteria(),
            retrieval_probe=_probe_factory(2),
        )
        assert plan is not None
        assert plan.reason_hint
        assert plan.scene_judgement_text
        assert plan.reason_hint == build_scenario_reason_hint(plan.shopping_strategy)
        assert len(plan.reason_hint) <= 28

    async def test_plan_emits_natural_strategy_narration_not_field_dump(self):
        plan = await build_shopping_strategy_plan(
            _request("男朋友生日，喜欢电子产品"),
            _intent(category="数码电子"),
            _criteria(),
            retrieval_probe=_probe_factory(2),
        )
        assert plan is not None
        assert "不建议一上来送手机、电脑这类核心设备" in plan.scene_judgement_text
        assert "更稳的方向：低踩雷的黑科技小件" in plan.scene_judgement_text
        assert "· 日常使用频率高，不容易闲置" in plan.scene_judgement_text
        assert "所以我会先避开" in plan.scene_judgement_text
        assert "下面先给你几款候选" in plan.scene_judgement_text
        assert "低踩雷的黑科技小件" in plan.scene_judgement_text
        assert "真无线耳机" in plan.scene_judgement_text
        assert "**" not in plan.scene_judgement_text
        assert "顾虑" not in plan.scene_judgement_text
        assert "假设" not in plan.scene_judgement_text
        assert "###" not in plan.scene_judgement_text

    async def test_avoid_risks_present_for_gift(self):
        plan = await build_shopping_strategy_plan(
            _request("男朋友生日，喜欢电子产品"),
            _intent(category="数码电子"),
            _criteria(),
            retrieval_probe=_probe_factory(2),
        )
        assert plan is not None
        assert len(plan.shopping_strategy.avoid_risks) >= 1

    @pytest.mark.parametrize(
        ("message", "category", "expected", "forbidden"),
        [
            ("男朋友喜欢足球，生日送什么", "服饰运动", "专业器材", "手机、电脑"),
            ("送妈妈一款护肤品，不知道怎么选", "美妆护肤", "猛功效", "手机、电脑"),
        ],
    )
    async def test_gift_risk_copy_matches_direction(
        self,
        message: str,
        category: str,
        expected: str,
        forbidden: str,
    ):
        plan = await build_shopping_strategy_plan(
            _request(message),
            _intent(category=category),
            _criteria(),
            retrieval_probe=_probe_factory(2),
        )
        assert plan is not None
        assert expected in plan.scene_judgement_text
        assert forbidden not in plan.scene_judgement_text

    async def test_without_probe_direction_marked_available(self):
        """When no probe is provided (offline mode), direction is still marked available."""
        plan = await build_shopping_strategy_plan(
            _request("男朋友生日，喜欢电子产品"),
            _intent(category="数码电子"),
            _criteria(),
            retrieval_probe=None,
        )
        assert plan is not None
        assert plan.shopping_strategy.primary_direction.available_in_catalog is True

    def test_intro_precheck_distinguishes_scenario_from_plain_filter(self):
        assert is_likely_shopping_strategy_request(
            _request("男朋友生日，喜欢电子产品"),
            _intent(category="数码电子"),
        )
        assert not is_likely_shopping_strategy_request(
            _request("2000 内蓝牙耳机有哪些"),
            _intent(category="数码电子", product_type="蓝牙耳机", budget_max=2000),
        )
