"""Intent handlers for the chat stream."""

from __future__ import annotations

import asyncio
from collections.abc import AsyncGenerator, Callable
from dataclasses import dataclass
from typing import Generic, TypeVar

from src.config.tuning import (
    CHEAPER_BUDGET_DEFAULT_MAX,
    CHEAPER_BUDGET_MIN_MAX,
    CHEAPER_BUDGET_RATIO,
)
from src.runtime.cart_rules import message_refers_to_previous_product, quantity_from_intent, referenced_product_id
from src.runtime.stages.criteria import criteria_quick_actions
from src.runtime.stages.recommendation import RetrievalResult
from src.runtime.stages.slot_checker import build_clarification_question
from src.runtime.streaming import (
    StageResult,
    StreamContext,
    run_timed_task_with_heartbeat,
    run_with_heartbeat,
    start_stage_task,
)
from src.services.audit import record_audit_event
from src.services.cart import add_product_to_cart, remove_product_from_cart, update_product_quantity
from src.services.conversation_state import save_recommendation_turn
from src.services.evidence import get_evidence
from src.services.fallbacks import get_fallback_events
from src.services.feedback import get_feedback_context, record_feedback
from src.services.recommendation_reasons import build_reason_atoms, reason_from_atoms
from src.services.trace_recorder import record_evidence_links, record_retrieval_trace
from src.types.schemas import ChatStreamRequest, DecisionResult, IntentResult, RecommendationResult
from src.types.sse_events import (
    AlternativePayload,
    CartActionEvent,
    ClarificationEvent,
    CriteriaCardEvent,
    CriteriaPayload,
    EvidencePayload,
    FinalDecisionEvent,
    ProductCardEvent,
    ProductPayload,
    QuickActionPayload,
    SSEEventBase,
    TextDeltaEvent,
    now_ms,
)

IntentHandler = Callable[[StreamContext, ChatStreamRequest, IntentResult], AsyncGenerator[SSEEventBase, None]]
T = TypeVar("T")
CRITERIA_CONFIRMATION_PROMPT = "你可以先确认或修改这些标准。没问题的话回复「继续」，我再开始推荐。"
CLARIFICATION_ANALYSIS_TEXT = "我先看了一下你的需求，已经能判断大方向，但还缺一个会影响推荐标准的关键信息。补齐后再生成购买标准会更稳。"
CRITERIA_ANALYSIS_TEXT = "我先把你的需求拆成可执行的购买标准，包含品类、预算、适用对象、使用场景和排除项。你可以先确认这版标准。"


@dataclass
class CapturedStage(Generic[T]):
    value: T | None = None

    def require(self, stage: str) -> T:
        if self.value is None:
            raise RuntimeError(f"{stage} stage completed without a result.")
        return self.value


async def handle_view_cart(
    ctx: StreamContext, body: ChatStreamRequest, intent: IntentResult
) -> AsyncGenerator[SSEEventBase, None]:
    del body, intent
    yield ctx.thinking("generating", "正在查看购物车...")
    yield CartActionEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"cart_{ctx.turn_id}",
        created_at_ms=now_ms(),
        action="view",
        product_id="",
        quantity=0,
        status="success",
    )
    yield ctx.done()


async def handle_add_to_cart(
    ctx: StreamContext, body: ChatStreamRequest, intent: IntentResult
) -> AsyncGenerator[SSEEventBase, None]:
    product_id = await referenced_product_id(ctx.session_id, intent, body.message)
    if product_id is None:
        async for event in _clarify_cart_target(ctx, "你想把哪个商品加入购物车？"):
            yield event
        yield ctx.done()
        return
    quantity = quantity_from_intent(intent, body.message, default=1)
    try:
        await add_product_to_cart(ctx.session_id, product_id, quantity=quantity)
    except ValueError:
        yield _cart_action_event(ctx, "add", product_id, 0, "failed")
        yield ctx.done()
        return
    await record_audit_event(
        "cart.item_added",
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        resource_type="cart_item",
        resource_id=product_id,
        metadata={"quantity": quantity, "source": "chat_intent"},
    )
    ctx.ensure_active()
    yield _cart_action_event(ctx, "add", product_id, quantity, "success")
    yield ctx.done()


async def handle_remove_from_cart(
    ctx: StreamContext, body: ChatStreamRequest, intent: IntentResult
) -> AsyncGenerator[SSEEventBase, None]:
    product_id = await referenced_product_id(ctx.session_id, intent, body.message)
    if product_id is None:
        async for event in _clarify_cart_target(ctx, "你想从购物车移出哪个商品？"):
            yield event
        yield ctx.done()
        return
    try:
        item = await remove_product_from_cart(ctx.session_id, product_id)
    except ValueError:
        item = None
    if item is None:
        yield _cart_action_event(ctx, "remove", product_id, 0, "failed")
        yield ctx.done()
        return
    await record_audit_event(
        "cart.item_removed",
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        resource_type="cart_item",
        resource_id=product_id,
        metadata={"source": "chat_intent"},
    )
    yield _cart_action_event(ctx, "remove", product_id, 0, "success")
    yield ctx.done()


async def handle_update_cart_quantity(
    ctx: StreamContext, body: ChatStreamRequest, intent: IntentResult
) -> AsyncGenerator[SSEEventBase, None]:
    product_id = await referenced_product_id(ctx.session_id, intent, body.message)
    if product_id is None:
        async for event in _clarify_cart_target(ctx, "你想修改哪个商品的数量？"):
            yield event
        yield ctx.done()
        return
    quantity = quantity_from_intent(intent, body.message, default=1)
    try:
        item = await update_product_quantity(ctx.session_id, product_id, quantity)
    except ValueError:
        item = None
    if item is None:
        yield _cart_action_event(ctx, "update_quantity", product_id, quantity, "failed")
        yield ctx.done()
        return
    await record_audit_event(
        "cart.quantity_updated",
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        resource_type="cart_item",
        resource_id=product_id,
        metadata={"quantity": quantity, "source": "chat_intent"},
    )
    yield _cart_action_event(ctx, "update_quantity", product_id, quantity, "success")
    yield ctx.done()


async def handle_chitchat(
    ctx: StreamContext, body: ChatStreamRequest, intent: IntentResult
) -> AsyncGenerator[SSEEventBase, None]:
    del body, intent
    yield ctx.thinking("understanding", "正在处理...")
    yield TextDeltaEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"ai_text_{ctx.turn_id}",
        created_at_ms=now_ms(),
        message_id=f"msg_{ctx.turn_id}",
        delta="我是电商导购助手，可以帮你推荐商品、添加购物车。请问你想买什么？",
        done=True,
    )
    yield ctx.done()


async def handle_clarification(ctx: StreamContext, missing_slots: list[str]) -> AsyncGenerator[SSEEventBase, None]:
    question, options = build_clarification_question(missing_slots)
    yield ctx.thinking("clarifying", "需要补充一个关键信息。")
    for event in _text_delta_from_text(
        ctx,
        message_id=f"clarification_analysis_{ctx.turn_id}",
        node_id=f"clarification_analysis_{ctx.turn_id}",
        text=CLARIFICATION_ANALYSIS_TEXT,
    ):
        yield event
    yield ClarificationEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"clarification_{ctx.turn_id}",
        created_at_ms=now_ms(),
        question=question,
        required_slots=missing_slots,
        suggested_options=options,
    )
    yield ctx.done()


async def handle_recommendation(
    ctx: StreamContext, body: ChatStreamRequest, intent: IntentResult
) -> AsyncGenerator[SSEEventBase, None]:
    if intent.intent == "feedback":
        await _record_feedback_intent(ctx, intent, body.message)

    yield ctx.thinking("criteria", "正在生成购买标准...")
    criteria_capture: CapturedStage[CriteriaPayload] = CapturedStage()
    ctx.ensure_active()
    async for event in _capture_stage_result(
        criteria_capture,
        run_with_heartbeat(
            ctx,
            ctx.stages.run_criteria(ctx.session_id, body, intent),
            "criteria",
            "正在生成购买标准...",
            timing_key="criteria",
        ),
    ):
        yield event
    criteria = criteria_capture.require("criteria")

    for event in _text_delta_from_text(
        ctx,
        message_id=f"criteria_analysis_{ctx.turn_id}",
        node_id=f"criteria_analysis_{ctx.turn_id}",
        text=CRITERIA_ANALYSIS_TEXT,
    ):
        yield event

    yield _criteria_card_event(ctx, criteria)
    if not _should_continue_after_criteria(body):
        yield _criteria_confirmation_event(ctx)
        await save_recommendation_turn(ctx.session_id, criteria, [], user_message=body.message)
        yield ctx.done()
        return

    async for event in continue_recommendation_from_criteria(ctx, body, criteria):
        yield event


async def continue_recommendation_from_criteria(
    ctx: StreamContext,
    body: ChatStreamRequest,
    criteria: CriteriaPayload,
) -> AsyncGenerator[SSEEventBase, None]:
    ctx.ensure_active()

    feedback = await get_feedback_context(ctx.session_id)
    yield ctx.thinking("searching", "正在检索匹配商品...")
    retrieval_capture: CapturedStage[RetrievalResult] = CapturedStage()
    ctx.ensure_active()
    async for event in _capture_stage_result(
        retrieval_capture,
        run_with_heartbeat(
            ctx,
            ctx.stages.run_retrieval(criteria, feedback=feedback),
            "searching",
            "正在检索匹配商品...",
            timing_key="retrieve",
        ),
    ):
        yield event
    retrieval = retrieval_capture.require("retrieval")

    products = retrieval.products
    if not products:
        yield TextDeltaEvent(
            session_id=ctx.session_id,
            turn_id=ctx.turn_id,
            seq=ctx.seq.next(),
            event_id=ctx.seq.event_id(),
            node_id=f"ai_text_{ctx.turn_id}",
            created_at_ms=now_ms(),
            message_id=f"msg_{ctx.turn_id}",
            delta="在您给定的条件下暂时没有匹配的商品，可以放宽预算或品类试试。",
            done=True,
        )
        await save_recommendation_turn(ctx.session_id, criteria, [], user_message=body.message)
        yield ctx.done()
        return

    evidences_by_product = dict(retrieval.evidence_by_product)
    ctx.ensure_active()
    recommendation_task = start_stage_task(
        ctx,
        ctx.stages.run_recommendation_text(criteria, products, evidences_by_product),
        timing_key="recommendation",
        background=True,
    )
    decision_task = start_stage_task(
        ctx,
        ctx.stages.run_decision(criteria, products, evidences_by_product),
        timing_key="decision",
        background=True,
    )

    yield ctx.thinking("searching", f"找到{len(products)}个匹配商品...")
    async for event in _product_card_events(ctx, criteria, products, evidences_by_product):
        yield event

    recommendation_capture: CapturedStage[RecommendationResult] = CapturedStage()
    async for event in _capture_stage_result(
        recommendation_capture,
        run_timed_task_with_heartbeat(
            ctx,
            recommendation_task,
            "recommending",
            "正在生成推荐解释...",
            timing_key="recommendation",
        ),
    ):
        yield event
    recommendation = recommendation_capture.require("recommendation")

    for event in _text_delta_events(ctx, recommendation):
        yield event

    decision_capture: CapturedStage[DecisionResult] = CapturedStage()
    async for event in _capture_stage_result(
        decision_capture,
        run_timed_task_with_heartbeat(
            ctx,
            decision_task,
            "decision",
            "正在生成最终决策...",
            timing_key="decision",
        ),
    ):
        yield event
    decision = decision_capture.require("decision")
    _drop_completed_background_tasks(ctx)

    yield _final_decision_event(ctx, criteria, products, decision)

    await _persist_recommendation(ctx, body, criteria, retrieval)
    yield ctx.done()


async def _record_feedback_intent(ctx: StreamContext, intent: IntentResult, message: str) -> None:
    reason = intent.extracted_constraints.get("feedback_text", "feedback")
    product_id = None
    if intent.target_product_id or message_refers_to_previous_product(message):
        product_id = await referenced_product_id(ctx.session_id, intent, message)
    await record_feedback(
        ctx.session_id,
        action="not_interested" if product_id else "feedback",
        product_id=product_id,
        reason=reason,
    )
    await record_audit_event(
        "feedback.created",
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        resource_type="feedback",
        resource_id=product_id,
        metadata={"source": "chat_intent", "reason": reason},
    )


async def _capture_stage_result(
    captured: CapturedStage[T],
    items: AsyncGenerator[SSEEventBase | StageResult[T], None],
) -> AsyncGenerator[SSEEventBase, None]:
    async for item in items:
        if isinstance(item, StageResult):
            captured.value = item.value
        else:
            yield item


def _criteria_card_event(ctx: StreamContext, criteria: CriteriaPayload) -> CriteriaCardEvent:
    return CriteriaCardEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"criteria_{criteria.criteria_id}",
        created_at_ms=now_ms(),
        criteria=criteria,
        quick_actions=criteria_quick_actions(),
    )


async def _product_card_events(
    ctx: StreamContext,
    criteria: CriteriaPayload,
    products: list[ProductPayload],
    evidences_by_product: dict[str, list[EvidencePayload]],
) -> AsyncGenerator[ProductCardEvent, None]:
    for rank, product in enumerate(products, start=1):
        ctx.ensure_active()
        evidence = evidences_by_product.get(product.product_id)
        if evidence is None:
            evidence = await get_evidence(product)
        evidences_by_product[product.product_id] = evidence
        reason_atoms = build_reason_atoms(criteria, product, evidence)
        yield ProductCardEvent(
            session_id=ctx.session_id,
            turn_id=ctx.turn_id,
            seq=ctx.seq.next(),
            event_id=ctx.seq.event_id(),
            node_id=f"product_{product.product_id}",
            deck_id=ctx.deck_id,
            created_at_ms=now_ms(),
            rank=rank,
            product=product,
            reason=reason_from_atoms(product, reason_atoms),
            reason_atoms=reason_atoms,
            risk_notes=[],
            evidence=evidence,
            actions=[
                QuickActionPayload(action_id="show_evidence", label="看证据", action="open_evidence"),
                QuickActionPayload(action_id="add_to_cart", label="加入购物车", action="add_to_cart"),
                QuickActionPayload(
                    action_id="dislike_product",
                    label="不喜欢这个",
                    action="feedback",
                    feedback_type="not_interested",
                ),
            ],
        )


def _text_delta_events(ctx: StreamContext, recommendation: RecommendationResult) -> list[TextDeltaEvent]:
    message_id = f"msg_{ctx.turn_id}"
    text_chunks = recommendation.text_chunks or ["我先把匹配商品列出来，方便你快速比较。"]
    events: list[TextDeltaEvent] = []
    for index, chunk in enumerate(text_chunks):
        ctx.ensure_active()
        events.append(
            TextDeltaEvent(
                session_id=ctx.session_id,
                turn_id=ctx.turn_id,
                seq=ctx.seq.next(),
                event_id=ctx.seq.event_id(),
                node_id=f"ai_text_{ctx.turn_id}",
                created_at_ms=now_ms(),
                message_id=message_id,
                delta=chunk,
                done=index == len(text_chunks) - 1,
            )
        )
    return events


def _text_delta_from_text(
    ctx: StreamContext,
    *,
    message_id: str,
    node_id: str,
    text: str,
    chunk_size: int = 18,
) -> list[TextDeltaEvent]:
    chunks = [text[index : index + chunk_size] for index in range(0, len(text), chunk_size)] or [""]
    events: list[TextDeltaEvent] = []
    for index, chunk in enumerate(chunks):
        ctx.ensure_active()
        events.append(
            TextDeltaEvent(
                session_id=ctx.session_id,
                turn_id=ctx.turn_id,
                seq=ctx.seq.next(),
                event_id=ctx.seq.event_id(),
                node_id=node_id,
                created_at_ms=now_ms(),
                message_id=message_id,
                delta=chunk,
                done=index == len(chunks) - 1,
            )
        )
    return events


def _criteria_confirmation_event(ctx: StreamContext) -> TextDeltaEvent:
    return TextDeltaEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"criteria_confirmation_{ctx.turn_id}",
        created_at_ms=now_ms(),
        message_id=f"criteria_confirmation_{ctx.turn_id}",
        delta=CRITERIA_CONFIRMATION_PROMPT,
        done=True,
    )


def _should_continue_after_criteria(body: ChatStreamRequest) -> bool:
    return "recommendation" in body.skip_stages or _is_continue_command(body.message)


def _is_continue_command(message: str) -> bool:
    text = message.strip().lower()
    if any(
        token in text
        for token in (
            "继续",
            "可以",
            "确认",
            "没问题",
            "开始推荐",
            "推荐吧",
            "go",
            "ok",
            "continue",
        )
    ):
        return True
    return _has_shopping_constraints(text)


def _has_shopping_constraints(text: str) -> bool:
    """Auto-continue when the message already specifies concrete product requirements."""
    constraint_markers = (
        "以内",
        "以下",
        "预算",
        "元",
        "块",
        "推荐",
        "想要",
        "帮我",
        "适合",
        "油皮",
        "干皮",
        "敏感肌",
        "混合",
        "日常",
        "洁面",
        "防晒",
        "洗面奶",
        "面霜",
        "精华",
        "面膜",
        "手机",
        "耳机",
        "电脑",
        "平板",
        "笔记本",
        "跑鞋",
        "篮球鞋",
        "t恤",
        "运动裤",
        "瑜伽",
        "茶饮",
        "咖啡",
        "零食",
        "酸奶",
        "方便",
        # Follow-up / feedback cues
        "不要",
        "排除",
        "去掉",
        "刚才",
        "第一个",
        "第二个",
        "换",
    )
    return any(marker in text for marker in constraint_markers)


def _drop_completed_background_tasks(ctx: StreamContext) -> None:
    ctx.background_tasks[:] = [timed_task for timed_task in ctx.background_tasks if not timed_task.task.done()]


def _final_decision_event(
    ctx: StreamContext,
    criteria: CriteriaPayload,
    products: list[ProductPayload],
    decision: DecisionResult,
) -> FinalDecisionEvent:
    alternatives = [
        AlternativePayload(product_id=p.product_id, name=p.name)
        for p in products
        if p.product_id != decision.winner_product_id
    ][:2]
    return FinalDecisionEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"decision_{ctx.turn_id}",
        created_at_ms=now_ms(),
        winner_product_id=decision.winner_product_id,
        summary=decision.summary,
        why=decision.why,
        not_for=decision.not_for,
        alternatives=alternatives,
        next_actions=[
            QuickActionPayload(
                action_id="cheaper",
                label="再便宜一点",
                action="criteria_patch",
                criteria_patch={"constraints": {"budget_max": _cheaper_budget_max(criteria)}},
            ),
            QuickActionPayload(action_id="compare", label="加入对比", action="compare"),
        ],
    )


async def _persist_recommendation(
    ctx: StreamContext,
    body: ChatStreamRequest,
    criteria: CriteriaPayload,
    retrieval: RetrievalResult,
) -> None:
    products = retrieval.products
    evidences_by_product = retrieval.evidence_by_product
    conversation_id = await save_recommendation_turn(
        ctx.session_id,
        criteria,
        [product.product_id for product in products],
        user_message=body.message,
    )
    await asyncio.gather(
        record_retrieval_trace(
            criteria,
            products,
            evidences_by_product,
            conversation_id=conversation_id,
            stage_timings_ms=ctx.stage_timings_ms,
            fallback_events=get_fallback_events(),
            trace_details=retrieval.trace_details,
        ),
        record_evidence_links(products, evidences_by_product, conversation_id=conversation_id),
    )
    await record_audit_event(
        "chat.recommendation_persisted",
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        resource_type="conversation",
        resource_id=conversation_id,
        metadata={
            "criteria_id": criteria.criteria_id,
            "selected_ids": [product.product_id for product in products],
            "fallbacks": get_fallback_events(),
        },
    )


INTENT_HANDLERS: dict[str, IntentHandler] = {
    "recommend": handle_recommendation,
    "clarify": handle_recommendation,
    "feedback": handle_recommendation,
    "view_cart": handle_view_cart,
    "add_to_cart": handle_add_to_cart,
    "remove_from_cart": handle_remove_from_cart,
    "update_cart_quantity": handle_update_cart_quantity,
    "chitchat": handle_chitchat,
}


async def _clarify_cart_target(ctx: StreamContext, question: str) -> AsyncGenerator[SSEEventBase, None]:
    yield ctx.thinking("clarifying", "需要确认购物车商品。")
    yield ClarificationEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"clarification_{ctx.turn_id}",
        created_at_ms=now_ms(),
        question=question,
        required_slots=["target_product"],
        suggested_options=[],
    )


def _cart_action_event(ctx: StreamContext, action: str, product_id: str, quantity: int, status: str) -> CartActionEvent:
    return CartActionEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"cart_{ctx.turn_id}",
        created_at_ms=now_ms(),
        action=action,
        product_id=product_id,
        quantity=quantity,
        status=status,
    )


def _cheaper_budget_max(criteria: CriteriaPayload) -> float:
    current = criteria.constraints.budget_max
    if current is None:
        return CHEAPER_BUDGET_DEFAULT_MAX
    return max(CHEAPER_BUDGET_MIN_MAX, round(current * CHEAPER_BUDGET_RATIO, 2))
