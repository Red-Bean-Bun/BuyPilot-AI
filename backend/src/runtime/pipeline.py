"""Real chat pipeline owner.

This async generator emits the same SSE event shape as runtime.mock_pipeline,
with deterministic P0 services behind the stages.
"""

from __future__ import annotations

import uuid
from typing import AsyncGenerator

from src.repos.cart_items import add_to_cart
from src.repos.conversations import get_last_product_ids, save_turn
from src.repos.feedbacks import add_feedback
from src.repos.traces import write_evidence_links, write_retrieval_trace
from src.runtime.stages.criteria import criteria_quick_actions, run_criteria
from src.runtime.stages.decision import run_decision
from src.runtime.stages.intent import run_intent
from src.runtime.stages.recommendation import run_recommendation
from src.runtime.stages.slot_checker import build_clarification_question, check_required_slots
from src.services.evidence import get_evidence
from src.types.schemas import ChatStreamRequest
from src.types.sse_events import (
    AlternativePayload,
    CartActionEvent,
    CriteriaCardEvent,
    DoneEvent,
    ErrorEvent,
    EventSeq,
    FinalDecisionEvent,
    ProductCardEvent,
    QuickActionPayload,
    SSEEventBase,
    TextDeltaEvent,
    ThinkingEvent,
    now_ms,
)


async def chat_stream(session_id: str, body: ChatStreamRequest) -> AsyncGenerator[SSEEventBase, None]:
    turn_id = f"turn_{uuid.uuid4().hex[:8]}"
    deck_id = f"deck_{turn_id}"
    seq = EventSeq(turn_id)

    try:
        yield _thinking(session_id, turn_id, seq, "understanding", "正在理解您的需求...")
        intent = await run_intent(body)

        if intent.intent == "view_cart":
            yield _thinking(session_id, turn_id, seq, "generating", "正在查看购物车...")
            yield CartActionEvent(
                session_id=session_id,
                turn_id=turn_id,
                seq=seq.next(),
                event_id=seq.event_id(),
                node_id=f"cart_{turn_id}",
                created_at_ms=now_ms(),
                action="view",
                product_id="",
                quantity=0,
                status="success",
            )
            yield _done(session_id, turn_id, seq)
            return

        if intent.intent == "add_to_cart":
            product_id = intent.target_product_id or _last_or_default_product(session_id)
            add_to_cart(session_id, product_id)
            yield CartActionEvent(
                session_id=session_id,
                turn_id=turn_id,
                seq=seq.next(),
                event_id=seq.event_id(),
                node_id=f"cart_{turn_id}",
                created_at_ms=now_ms(),
                action="add",
                product_id=product_id,
                quantity=1,
                status="success",
            )
            yield _done(session_id, turn_id, seq)
            return

        if intent.intent == "feedback":
            add_feedback(
                session_id,
                action="feedback",
                reason=intent.extracted_constraints.get("feedback_text", "feedback"),
            )

        missing_slots = [] if body.criteria_patch else check_required_slots(body.message, intent)
        if missing_slots:
            question, options = build_clarification_question(missing_slots)
            yield _thinking(session_id, turn_id, seq, "clarifying", "需要补充一个关键信息。")
            from src.types.sse_events import ClarificationEvent

            yield ClarificationEvent(
                session_id=session_id,
                turn_id=turn_id,
                seq=seq.next(),
                event_id=seq.event_id(),
                node_id=f"clarification_{turn_id}",
                created_at_ms=now_ms(),
                question=question,
                required_slots=missing_slots,
                suggested_options=options,
            )
            yield _done(session_id, turn_id, seq)
            return

        yield _thinking(session_id, turn_id, seq, "criteria", "正在生成购买标准...")
        criteria = await run_criteria(session_id, body, intent)
        yield CriteriaCardEvent(
            session_id=session_id,
            turn_id=turn_id,
            seq=seq.next(),
            event_id=seq.event_id(),
            node_id=f"criteria_{criteria.criteria_id}",
            created_at_ms=now_ms(),
            criteria=criteria,
            quick_actions=criteria_quick_actions(),
        )

        recommendation = await run_recommendation(criteria)
        message_id = f"msg_{turn_id}"
        for index, chunk in enumerate(recommendation.text_chunks):
            yield TextDeltaEvent(
                session_id=session_id,
                turn_id=turn_id,
                seq=seq.next(),
                event_id=seq.event_id(),
                node_id=f"ai_text_{turn_id}",
                created_at_ms=now_ms(),
                message_id=message_id,
                delta=chunk,
                done=index == len(recommendation.text_chunks) - 1,
            )

        yield _thinking(session_id, turn_id, seq, "searching", f"找到{len(recommendation.products)}个匹配商品...")
        evidences_by_product = {}
        for rank, product in enumerate(recommendation.products, start=1):
            evidence = await get_evidence(product)
            evidences_by_product[product.product_id] = evidence
            yield ProductCardEvent(
                session_id=session_id,
                turn_id=turn_id,
                seq=seq.next(),
                event_id=seq.event_id(),
                node_id=f"product_{product.product_id}",
                deck_id=deck_id,
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

        decision = await run_decision(criteria, recommendation.products)
        alternatives = [
            AlternativePayload(product_id=p.product_id, name=p.name)
            for p in recommendation.products
            if p.product_id != decision.winner_product_id
        ][:2]
        yield FinalDecisionEvent(
            session_id=session_id,
            turn_id=turn_id,
            seq=seq.next(),
            event_id=seq.event_id(),
            node_id=f"decision_{turn_id}",
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
                    criteria_patch={"constraints": {"budget_max": 100}},
                ),
                QuickActionPayload(action_id="compare", label="加入对比", action="compare"),
            ],
        )
        save_turn(session_id, criteria, [product.product_id for product in recommendation.products])
        write_retrieval_trace(criteria, recommendation.products, evidences_by_product)
        write_evidence_links(recommendation.products, evidences_by_product)
        yield _done(session_id, turn_id, seq)
    except Exception as exc:
        yield ErrorEvent(
            session_id=session_id,
            turn_id=turn_id,
            seq=seq.next(),
            event_id=seq.event_id(),
            node_id=f"error_{turn_id}",
            created_at_ms=now_ms(),
            code="PIPELINE_ERROR",
            message=str(exc),
            retryable=True,
        )
        yield _done(session_id, turn_id, seq)


def _thinking(session_id: str, turn_id: str, seq: EventSeq, stage: str, message: str) -> ThinkingEvent:
    return ThinkingEvent(
        session_id=session_id,
        turn_id=turn_id,
        seq=seq.next(),
        event_id=seq.event_id(),
        node_id=f"thinking_{turn_id}",
        created_at_ms=now_ms(),
        stage=stage,
        message=message,
    )


def _done(session_id: str, turn_id: str, seq: EventSeq) -> DoneEvent:
    return DoneEvent(
        session_id=session_id,
        turn_id=turn_id,
        seq=seq.next(),
        event_id=seq.event_id(),
        node_id=f"done_{turn_id}",
        created_at_ms=now_ms(),
    )


def _reason_for_product(product) -> str:
    if product.skin_type_match:
        return f"{product.skin_type_match[0]}适用，{product.sub_category or product.category}匹配。"
    return f"{product.category}下综合匹配度较高。"


def _last_or_default_product(session_id: str) -> str:
    last_ids = get_last_product_ids(session_id)
    return last_ids[0] if last_ids else "p_beauty_011"
