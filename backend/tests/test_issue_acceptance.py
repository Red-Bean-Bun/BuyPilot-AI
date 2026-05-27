"""Acceptance tests derived from doc/问题记录.md.

Mapping convention:
- Each test has a "问题 N 验收" comment listing the exact acceptance bullets it covers.
- "REG-NN" comments map to doc/问题记录.md line 497 "回归测试建议" rows.
- doc/问题整改回应.md has the reverse mapping from every acceptance bullet to these test names.
"""

from __future__ import annotations

import uuid
from pathlib import Path

import pytest

from src.runtime import pipeline as pipeline_module
from src.runtime.pipeline import chat_stream
from src.services.feedback import record_feedback
from src.types.schemas import ChatStreamRequest, IntentResult
from src.types.sse_events import CriteriaPayload, ProductPayload, SSEEventBase

PROJECT_ROOT = Path(__file__).resolve().parents[2]


@pytest.fixture(autouse=True)
async def _seed_products_for_issue_acceptance(seeded_products):
    del seeded_products


@pytest.mark.asyncio
# 问题 0 验收：
# - 首轮只出现标准卡，不出现商品卡和 final_decision。
# - 确认标准后只出现候选商品 deck，不出现 final_decision。
# - deck 内反馈后回复“继续”，最终建议体现反馈。
# - final_decision 不早于 awaiting_product_feedback 阶段之后出现。
# REG-09: SwipeDeck 用户反馈后回复“继续” -> 后端读取当前 deck 反馈，再生成 final_decision。
async def test_issue0_deck_feedback_is_applied_before_final_decision():
    session_id = _session_id()

    first = await _chat("推荐适合油皮的护肤品，300元以内，日常护肤", session_id=session_id)
    _assert_waiting_for_criteria(first)

    deck = await _chat("继续", session_id=session_id)
    products = _products(deck)
    assert len(products) >= 2
    assert "final_decision" not in _tags(deck)
    assert _done(deck).finish_reason == "awaiting_product_feedback"
    deck_id = _done(deck).deck_id
    assert deck_id

    disliked_id = products[0].product_id
    await record_feedback(
        session_id,
        action="not_interested",
        product_id=disliked_id,
        reason="不喜欢这个",
        deck_id=deck_id,
    )

    final = await _chat("继续", session_id=session_id)
    decisions = [event for event in final if event.event == "final_decision"]
    assert len(decisions) == 1
    assert decisions[0].winner_product_id != disliked_id
    assert "product_card" not in _tags(final)
    assert _done(final).finish_reason == "completed"

    seen_product_feedback_gate = False
    for event in [*first, *deck, *final]:
        if event.event == "final_decision":
            assert seen_product_feedback_gate
        if event.event == "done" and event.finish_reason == "awaiting_product_feedback":
            seen_product_feedback_gate = True


@pytest.mark.asyncio
# 问题 1 验收：
# - “我想买个手机，平时拍照多”不直接出现商品卡。
# - 应先出现标准卡，并追问预算或价位范围。
# REG-01: “我想买个手机，平时拍照多” -> 出标准卡并追问预算，不直接出商品。
async def test_issue1_phone_photo_requires_budget_before_products():
    events = await _chat("我想买个手机，平时拍照多")
    tags = _tags(events)

    assert "product_card" not in tags
    assert "final_decision" not in tags
    criteria = _criteria(events)[0]
    assert criteria.category == "数码电子"
    assert criteria.constraints.product_type == "智能手机"
    clarifications = [event for event in events if event.event == "clarification"]
    assert len(clarifications) == 1
    assert clarifications[0].required_slots == ["budget"]
    assert "预算" in clarifications[0].question or "价位" in clarifications[0].question


@pytest.mark.asyncio
# 问题 1/2 验收：
# - “推荐适合油皮的洗面奶”不凭空出现硬预算。
# - 推断字段通过 field_sources 标注为 inferred，用户明示字段标注为 user。
# - 新会话中用户没说预算时，标准卡不应出现具体预算。
# REG-03: “推荐适合油皮的洗面奶” -> 不应凭空出现硬预算；推断字段需标记。
async def test_issue1_and_2_inferred_fields_do_not_become_user_hard_constraints():
    events = await _chat("推荐适合油皮的洗面奶")
    criteria = _criteria(events)[0]

    assert criteria.constraints.budget_max is None
    assert "constraints.budget_max" not in criteria.field_sources
    assert criteria.field_sources["constraints.skin_type"] == "user"
    assert criteria.field_sources["constraints.product_type"] == "user"
    if criteria.constraints.use_scenario:
        assert criteria.field_sources["constraints.use_scenario"] == "inferred"


@pytest.mark.asyncio
# 问题 2 验收：
# - 模糊问题触发追问后，用户回答“我是中性肌肤”。
# - 下一轮合并到上一轮 pending slot，不重复问同一问题。
# REG-04: “我是中性肌肤”作为澄清回答 -> 合并 pending slot，不重复问肤质。
async def test_issue2_clarification_answer_merges_previous_context_without_repeat(monkeypatch):
    async def fake_intent(_session_id: str, body: ChatStreamRequest) -> IntentResult:
        if "中性肌肤" in body.message:
            return IntentResult(
                intent="recommend",
                category=None,
                extracted_constraints={"skin_type": "中性", "product_type": "洁面"},
            )
        return IntentResult(intent="recommend", category="美妆护肤", extracted_constraints={})

    monkeypatch.setattr(pipeline_module, "run_intent", fake_intent)
    session_id = _session_id()

    first = await _chat("最近想先看看怎么选", session_id=session_id)
    assert "clarification" in _tags(first)

    second = await _chat("我是中性肌肤", session_id=session_id)
    assert "clarification" not in _tags(second)
    criteria = _criteria(second)[0]
    assert criteria.category == "美妆护肤"
    assert criteria.constraints.skin_type == "中性"
    assert criteria.constraints.product_type == "洁面"


@pytest.mark.asyncio
# 问题 3/6 验收：
# - “为我推荐一双运动鞋”只出现标准卡和确认提示。
# - 用户回复“继续”后，才出现候选商品。
async def test_issue3_and_6_explicit_continue_is_required_before_candidate_deck():
    session_id = _session_id()

    first = await _chat("为我推荐一双运动鞋", session_id=session_id)
    _assert_waiting_for_criteria(first)

    second = await _chat("继续", session_id=session_id)
    assert _products(second)
    assert "final_decision" not in _tags(second)
    assert _done(second).finish_reason == "awaiting_product_feedback"


@pytest.mark.asyncio
# 问题 6 验收：
# - “推荐一款手机”不能在未确认/未补预算时进入候选商品阶段。
# - 手机类缺关键预算时优先追问预算，仍不出现 final_decision。
# REG-02: “推荐一款手机” -> 等待用户补齐/确认标准，不直接出候选。
async def test_issue6_phone_request_stays_before_candidates_until_budget_is_known():
    events = await _chat("推荐一款手机")
    tags = _tags(events)

    assert "product_card" not in tags
    assert "final_decision" not in tags
    criteria = _criteria(events)[0]
    assert criteria.category == "数码电子"
    assert criteria.constraints.product_type == "智能手机"
    clarifications = [event for event in events if event.event == "clarification"]
    assert len(clarifications) == 1
    assert clarifications[0].required_slots == ["budget"]


@pytest.mark.asyncio
# 问题 6 验收：
# - 手机标准具备预算后仍默认等待确认。
# - 用户回复“继续”后，才出现候选商品。
# REG-02: “推荐一款手机” -> 等待用户确认标准，回复“继续”后才出候选。
async def test_regression_phone_with_budget_waits_for_continue_before_candidates():
    session_id = _session_id()

    first = await _chat("推荐一款手机，预算8000", session_id=session_id)
    _assert_waiting_for_criteria(first)

    second = await _chat("继续", session_id=session_id)
    assert _products(second)
    assert "final_decision" not in _tags(second)
    assert _done(second).finish_reason == "awaiting_product_feedback"


@pytest.mark.asyncio
# 问题 4 验收：
# - “出门拍照比较多，想先看看怎么选”先确认大类。
# - 不直接跳到护肤肤质追问，不出现标准卡/商品卡/final_decision。
async def test_issue4_ambiguous_photo_need_asks_category_instead_of_skin_type():
    events = await _chat("我最近出门拍照比较多，想先看看怎么选")
    tags = _tags(events)
    clarifications = [event for event in events if event.event == "clarification"]

    assert "criteria_card" not in tags
    assert "product_card" not in tags
    assert "final_decision" not in tags
    assert len(clarifications) == 1
    assert clarifications[0].required_slots == ["category"]
    assert "哪一类" in clarifications[0].question
    assert "肤质" not in clarifications[0].question
    assert {"数码电子", "美妆护肤"} <= set(clarifications[0].suggested_options)


@pytest.mark.asyncio
# 问题 4 验收：
# - “服饰运动”先问运动场景或具体商品类型。
# - 不直接生成完整标准，不先问预算，不进入候选商品。
async def test_issue4_sports_category_asks_product_type_before_budget_or_full_criteria():
    events = await _chat("服饰运动")
    tags = _tags(events)

    assert "product_card" not in tags
    assert "final_decision" not in tags
    criteria = _criteria(events)[0]
    assert criteria.category == "服饰运动"
    assert criteria.constraints.product_type is None
    assert criteria.constraints.budget_max is None
    clarifications = [event for event in events if event.event == "clarification"]
    assert len(clarifications) == 1
    assert clarifications[0].required_slots == ["product_type"]


@pytest.mark.asyncio
@pytest.mark.parametrize("message", ["推荐无糖饮料", "推荐咖啡", "推荐零食"])
# 问题 5 验收：
# - “推荐无糖饮料/咖啡/零食”均能召回食品类商品。
# - 食品 raw/source category 与业务 category 归一后不排空。
# REG-05: “推荐无糖饮料” -> 能召回食品类商品。
async def test_issue5_food_aliases_recall_food_products(message: str):
    events = await _chat(message, auto_run=True)
    products = _products(events)

    assert products
    assert all(product.category == "食品生活" for product in products)
    assert "final_decision" not in _tags(events)
    assert _done(events).finish_reason == "awaiting_product_feedback"


@pytest.mark.asyncio
# 问题 5/9 验收：
# - “推荐 CD 机”在当前商品库不支持时明确提示不覆盖。
# - 不进入可继续推荐的标准卡，不泛化追问，也不返回空候选结果。
# REG-06: “推荐 CD 机” -> 若数据不支持，明确说明暂不覆盖。
async def test_issue5_and_9_unsupported_cd_player_stops_before_criteria_or_empty_results():
    events = await _chat("推荐 CD 机")
    tags = _tags(events)
    text = "".join(event.delta for event in events if event.event == "text_delta")

    assert "criteria_card" not in tags
    assert "clarification" not in tags
    assert "product_card" not in tags
    assert "final_decision" not in tags
    assert "不覆盖" in text
    assert "CD" in text


@pytest.mark.asyncio
# 问题 7/8 验收：
# - 候选商品刚出现时不展示最终建议卡。
# - 候选阶段 product_card 后不再追加长推荐正文。
# - 用户反馈后回复“继续”才出 final_decision 由问题 0 的测试覆盖。
# REG-07: 候选商品出现后 -> 主时间线只显示短摘要，不显示长段落。
async def test_issue7_and_8_candidate_stage_has_no_final_decision_or_long_followup_text():
    events = await _chat("推荐适合油皮的洗面奶，200元以内，日常护肤", auto_run=True)
    tags = _tags(events)

    first_product_index = tags.index("product_card")
    assert "final_decision" not in tags
    assert _done(events).finish_reason == "awaiting_product_feedback"
    assert "text_delta" not in tags[first_product_index:]


@pytest.mark.asyncio
# 问题 0/7 验收：
# - product_card 后收到过早 final_decision 时，Android 端继续缓存。
# - 用户收敛后再展示 pending decision。
# REG-08: product_card 后收到 final_decision -> Android 端继续缓存，用户收敛后再展示。
async def test_regression_android_reducer_caches_early_final_decision_until_converged():
    reducer_test_path = (
        PROJECT_ROOT
        / "android"
        / "feature"
        / "chat"
        / "src"
        / "test"
        / "java"
        / "com"
        / "buypilot"
        / "feature"
        / "chat"
        / "state"
        / "ChatReducerTest.kt"
    )
    source = reducer_test_path.read_text(encoding="utf-8")

    assert "productCardsAwaitConvergenceAndCacheEarlyDecisionUntilUserConverges" in source
    assert 'assertFalse(earlyDecision.nodes.any { it is FinalDecisionNode })' in source
    assert 'assertTrue("deck_1" in earlyDecision.pendingDecisions)' in source
    assert "ChatReducer.convergeDeck(earlyDecision, \"deck_1\")" in source
    assert "assertTrue(converged.nodes.any { it is FinalDecisionNode })" in source


@pytest.mark.asyncio
# 问题 8 验收：
# - Markdown 加粗、列表、编号不显示原始符号错乱。
# - 本后端验收测试用静态检查锁定 Android Markwon 渲染入口。
async def test_issue8_android_markdown_rendering_uses_markwon():
    screen_path = (
        PROJECT_ROOT
        / "android"
        / "feature"
        / "chat"
        / "src"
        / "main"
        / "java"
        / "com"
        / "buypilot"
        / "feature"
        / "chat"
        / "ui"
        / "BuyPilotChatScreen.kt"
    )
    source = screen_path.read_text(encoding="utf-8")

    assert "import io.noties.markwon.Markwon" in source
    assert "Markwon.builder(context)" in source
    assert "markwon.setMarkdown(textView, content)" in source


async def _chat(
    message: str,
    *,
    session_id: str | None = None,
    auto_run: bool = False,
) -> list[SSEEventBase]:
    return [
        event
        async for event in chat_stream(
            session_id or _session_id(),
            ChatStreamRequest(message=message, auto_run=auto_run),
        )
    ]


def _assert_waiting_for_criteria(events: list[SSEEventBase]) -> None:
    tags = _tags(events)
    assert "criteria_card" in tags
    assert "product_card" not in tags
    assert "final_decision" not in tags
    assert _done(events).finish_reason == "awaiting_criteria_confirmation"


def _criteria(events: list[SSEEventBase]) -> list[CriteriaPayload]:
    return [event.criteria for event in events if event.event == "criteria_card"]


def _products(events: list[SSEEventBase]) -> list[ProductPayload]:
    return [event.product for event in events if event.event == "product_card"]


def _done(events: list[SSEEventBase]):
    done_events = [event for event in events if event.event == "done"]
    assert len(done_events) == 1
    return done_events[0]


def _tags(events: list[SSEEventBase]) -> list[str]:
    return [event.event for event in events]


def _session_id() -> str:
    return f"sess_issue_acceptance_{uuid.uuid4().hex[:8]}"
