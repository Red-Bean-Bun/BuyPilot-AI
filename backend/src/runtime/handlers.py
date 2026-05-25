"""Intent handlers for the chat stream."""

from __future__ import annotations

from collections.abc import AsyncGenerator, Callable
from dataclasses import dataclass
from typing import Generic, TypeVar

from src.config.tuning import (
    CHEAPER_BUDGET_FALLBACK_MAX,
    CHEAPER_BUDGET_MIN_MAX,
    CHEAPER_BUDGET_RATIO,
)
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
from src.services.cart import add_product_to_cart
from src.services.conversation_state import get_previous_product_ids, save_recommendation_turn
from src.services.evidence import get_evidence
from src.services.fallbacks import get_fallback_events
from src.services.feedback import get_feedback_context, record_feedback
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
    del body
    product_id = intent.target_product_id if intent.target_product_id else await _last_product_id(ctx.session_id)
    if product_id is None:
        yield ctx.thinking("clarifying", "需要确认要加购的商品。")
        yield ClarificationEvent(
            session_id=ctx.session_id,
            turn_id=ctx.turn_id,
            seq=ctx.seq.next(),
            event_id=ctx.seq.event_id(),
            node_id=f"clarification_{ctx.turn_id}",
            created_at_ms=now_ms(),
            question="你想把哪个商品加入购物车？",
            required_slots=["target_product"],
            suggested_options=[],
        )
        yield ctx.done()
        return
    await add_product_to_cart(ctx.session_id, product_id)
    await record_audit_event(
        "cart.item_added",
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        resource_type="cart_item",
        resource_id=product_id,
        metadata={"quantity": 1, "source": "chat_intent"},
    )
    ctx.ensure_active()
    yield CartActionEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"cart_{ctx.turn_id}",
        created_at_ms=now_ms(),
        action="add",
        product_id=product_id,
        quantity=1,
        status="success",
    )
    yield ctx.done()


async def handle_clarification(ctx: StreamContext, missing_slots: list[str]) -> AsyncGenerator[SSEEventBase, None]:
    question, options = build_clarification_question(missing_slots)
    yield ctx.thinking("clarifying", "需要补充一个关键信息。")
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
        await _record_feedback_intent(ctx, intent)

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

    yield _criteria_card_event(ctx, criteria)

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
    async for event in _product_card_events(ctx, products, evidences_by_product):
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


async def _record_feedback_intent(ctx: StreamContext, intent: IntentResult) -> None:
    reason = intent.extracted_constraints.get("feedback_text", "feedback")
    await record_feedback(
        ctx.session_id,
        action="feedback",
        reason=reason,
    )
    await record_audit_event(
        "feedback.created",
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        resource_type="feedback",
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
    products: list[ProductPayload],
    evidences_by_product: dict[str, list[EvidencePayload]],
) -> AsyncGenerator[ProductCardEvent, None]:
    for rank, product in enumerate(products, start=1):
        ctx.ensure_active()
        evidence = evidences_by_product.get(product.product_id)
        if evidence is None:
            evidence = await get_evidence(product)
        evidences_by_product[product.product_id] = evidence
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
            reason=_reason_for_product(product),
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
    await record_retrieval_trace(
        criteria,
        products,
        evidences_by_product,
        conversation_id=conversation_id,
        stage_timings_ms=ctx.stage_timings_ms,
        fallback_events=get_fallback_events(),
        trace_details=retrieval.trace_details,
    )
    await record_evidence_links(products, evidences_by_product, conversation_id=conversation_id)
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
    "view_cart": handle_view_cart,
    "add_to_cart": handle_add_to_cart,
}


def _reason_for_product(product: ProductPayload) -> str:
    if product.skin_type_match:
        return f"{product.skin_type_match[0]}适用，{product.sub_category or product.category}匹配。"
    return f"{product.category}下综合匹配度较高。"


async def _last_product_id(session_id: str) -> str | None:
    last_ids = await get_previous_product_ids(session_id)
    return last_ids[0] if last_ids else None


def _cheaper_budget_max(criteria: CriteriaPayload) -> float:
    current = criteria.constraints.budget_max
    if current is None:
        return CHEAPER_BUDGET_FALLBACK_MAX
    return max(CHEAPER_BUDGET_MIN_MAX, round(current * CHEAPER_BUDGET_RATIO, 2))
