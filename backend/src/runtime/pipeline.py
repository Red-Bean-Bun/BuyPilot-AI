"""Real chat pipeline owner.

This async generator emits the same SSE event shape as runtime.mock_pipeline,
with deterministic P0 services behind the stages.
"""

from __future__ import annotations

import asyncio
import time
import uuid
from dataclasses import dataclass
from typing import Any, AsyncGenerator, Awaitable

from src.repos.cart_items import add_to_cart
from src.repos.conversations import get_last_product_ids, save_turn
from src.repos.feedbacks import add_feedback, extract_feedback_from_session
from src.repos.traces import write_evidence_links, write_retrieval_trace
from src.runtime.stages.criteria import criteria_quick_actions, run_criteria
from src.runtime.stages.decision import run_decision
from src.runtime.stages.intent import run_intent
from src.runtime.stages.multimodal import run_multimodal
from src.runtime.stages.recommendation import run_recommendation_text, run_retrieval
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

HEARTBEAT_INTERVAL_SECONDS = 0.8


@dataclass(frozen=True)
class _StageResult:
    value: Any


@dataclass(frozen=True)
class _TimedTask:
    task: asyncio.Task[Any]
    started_at: float


async def chat_stream(session_id: str, body: ChatStreamRequest) -> AsyncGenerator[SSEEventBase, None]:
    turn_id = f"turn_{uuid.uuid4().hex[:8]}"
    deck_id = f"deck_{turn_id}"
    seq = EventSeq(turn_id)
    stage_timings_ms: dict[str, float] = {}
    background_tasks: list[_TimedTask] = []

    try:
        pipeline_body = body
        if body.image_url:
            yield _thinking(session_id, turn_id, seq, "analyzing_image", "正在分析图片...")
            image_analysis = None
            async for item in _run_with_heartbeat(
                run_multimodal(body.image_url),
                session_id,
                turn_id,
                seq,
                "analyzing_image",
                "正在分析图片...",
                stage_timings_ms,
                "image_analysis",
            ):
                if isinstance(item, _StageResult):
                    image_analysis = item.value
                else:
                    yield item
            pipeline_body = body.model_copy(update={"message": _message_with_image_context(body.message, image_analysis)})

        yield _thinking(session_id, turn_id, seq, "understanding", "正在理解您的需求...")
        intent = None
        async for item in _run_with_heartbeat(
            run_intent(pipeline_body),
            session_id,
            turn_id,
            seq,
            "understanding",
            "正在理解您的需求...",
            stage_timings_ms,
            "intent",
        ):
            if isinstance(item, _StageResult):
                intent = item.value
            else:
                yield item

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

        missing_slots = [] if body.criteria_patch or body.image_url else check_required_slots(pipeline_body.message, intent)
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
        criteria = None
        async for item in _run_with_heartbeat(
            run_criteria(session_id, pipeline_body, intent),
            session_id,
            turn_id,
            seq,
            "criteria",
            "正在生成购买标准...",
            stage_timings_ms,
            "criteria",
        ):
            if isinstance(item, _StageResult):
                criteria = item.value
            else:
                yield item
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

        feedback = extract_feedback_from_session(session_id)
        yield _thinking(session_id, turn_id, seq, "searching", "正在检索匹配商品...")
        retrieval = None
        async for item in _run_with_heartbeat(
            run_retrieval(criteria, feedback=feedback),
            session_id,
            turn_id,
            seq,
            "searching",
            "正在检索匹配商品...",
            stage_timings_ms,
            "retrieve",
        ):
            if isinstance(item, _StageResult):
                retrieval = item.value
            else:
                yield item

        products = retrieval.products
        evidences_by_product = dict(retrieval.evidence_by_product)
        recommendation_task = _start_stage_task(
            run_recommendation_text(criteria, products),
            stage_timings_ms,
            "recommendation",
        )
        decision_task = _start_stage_task(run_decision(criteria, products), stage_timings_ms, "decision")
        background_tasks = [recommendation_task, decision_task]

        yield _thinking(session_id, turn_id, seq, "searching", f"找到{len(products)}个匹配商品...")
        for rank, product in enumerate(products, start=1):
            evidence = evidences_by_product.get(product.product_id)
            if evidence is None:
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

        recommendation = None
        async for item in _run_timed_task_with_heartbeat(
            recommendation_task,
            session_id,
            turn_id,
            seq,
            "recommending",
            "正在生成推荐解释...",
            stage_timings_ms,
            "recommendation",
        ):
            if isinstance(item, _StageResult):
                recommendation = item.value
            else:
                yield item
        message_id = f"msg_{turn_id}"
        text_chunks = recommendation.text_chunks or ["我先把匹配商品列出来，方便你快速比较。"]
        for index, chunk in enumerate(text_chunks):
            yield TextDeltaEvent(
                session_id=session_id,
                turn_id=turn_id,
                seq=seq.next(),
                event_id=seq.event_id(),
                node_id=f"ai_text_{turn_id}",
                created_at_ms=now_ms(),
                message_id=message_id,
                delta=chunk,
                done=index == len(text_chunks) - 1,
            )

        decision = None
        async for item in _run_timed_task_with_heartbeat(
            decision_task,
            session_id,
            turn_id,
            seq,
            "decision",
            "正在生成最终决策...",
            stage_timings_ms,
            "decision",
        ):
            if isinstance(item, _StageResult):
                decision = item.value
            else:
                yield item
        background_tasks = []
        alternatives = [
            AlternativePayload(product_id=p.product_id, name=p.name)
            for p in products
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
        save_turn(session_id, criteria, [product.product_id for product in products], user_message=body.message)
        write_retrieval_trace(criteria, products, evidences_by_product, stage_timings_ms=stage_timings_ms)
        write_evidence_links(products, evidences_by_product)
        yield _done(session_id, turn_id, seq)
    except Exception as exc:
        _cancel_background_tasks(background_tasks)
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


def _start_stage_task(
    awaitable: Awaitable[Any],
    stage_timings_ms: dict[str, float] | None = None,
    timing_key: str | None = None,
) -> _TimedTask:
    started_at = time.perf_counter()
    task = asyncio.create_task(awaitable)
    if stage_timings_ms is not None and timing_key:
        task.add_done_callback(lambda _: _record_stage_timing(stage_timings_ms, timing_key, started_at))
    return _TimedTask(task=task, started_at=started_at)


async def _run_with_heartbeat(
    awaitable: Awaitable[Any],
    session_id: str,
    turn_id: str,
    seq: EventSeq,
    stage: str,
    message: str,
    stage_timings_ms: dict[str, float] | None = None,
    timing_key: str | None = None,
) -> AsyncGenerator[SSEEventBase | _StageResult, None]:
    timed_task = _start_stage_task(awaitable, stage_timings_ms, timing_key)
    async for item in _run_timed_task_with_heartbeat(
        timed_task,
        session_id,
        turn_id,
        seq,
        stage,
        message,
        stage_timings_ms,
        timing_key,
    ):
        yield item


async def _run_timed_task_with_heartbeat(
    timed_task: _TimedTask,
    session_id: str,
    turn_id: str,
    seq: EventSeq,
    stage: str,
    message: str,
    stage_timings_ms: dict[str, float] | None = None,
    timing_key: str | None = None,
) -> AsyncGenerator[SSEEventBase | _StageResult, None]:
    task = timed_task.task
    try:
        while True:
            done, _ = await asyncio.wait({task}, timeout=HEARTBEAT_INTERVAL_SECONDS)
            if task in done:
                if stage_timings_ms is not None and timing_key:
                    _record_stage_timing(stage_timings_ms, timing_key, timed_task.started_at)
                yield _StageResult(task.result())
                return
            yield _thinking(session_id, turn_id, seq, stage, message)
    except BaseException:
        if not task.done():
            task.cancel()
        raise


def _cancel_background_tasks(timed_tasks: list[_TimedTask]) -> None:
    for timed_task in timed_tasks:
        if not timed_task.task.done():
            timed_task.task.cancel()


def _record_stage_timing(stage_timings_ms: dict[str, float], timing_key: str, started_at: float) -> None:
    stage_timings_ms.setdefault(timing_key, round((time.perf_counter() - started_at) * 1000, 2))


def _reason_for_product(product) -> str:
    if product.skin_type_match:
        return f"{product.skin_type_match[0]}适用，{product.sub_category or product.category}匹配。"
    return f"{product.category}下综合匹配度较高。"


def _last_or_default_product(session_id: str) -> str:
    last_ids = get_last_product_ids(session_id)
    return last_ids[0] if last_ids else "p_beauty_011"


def _message_with_image_context(message: str, image_analysis: dict | None) -> str:
    if not image_analysis:
        return message
    parts = []
    category_hint = image_analysis.get("category_hint")
    description = image_analysis.get("description")
    visible_traits = image_analysis.get("visible_traits")
    if category_hint:
        parts.append(f"品类={category_hint}")
    if description:
        parts.append(f"描述={description}")
    if isinstance(visible_traits, list) and visible_traits:
        parts.append("可见特征=" + "，".join(str(item) for item in visible_traits))
    return f"{message}\n图片分析：" + "；".join(parts) if parts else message
