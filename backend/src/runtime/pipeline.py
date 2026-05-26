"""Real chat pipeline owner."""

from __future__ import annotations

import logging
import re
import uuid
import weakref
from collections.abc import AsyncGenerator, Awaitable, Callable
from dataclasses import dataclass
from typing import Any

from src.config.domain_terms import PRODUCT_TYPE_ALIASES, category_from_text, extract_skin_types, normalize_product_type
from src.config.tuning import CHEAPER_BUDGET_DEFAULT_MAX, CHEAPER_BUDGET_MIN_MAX, CHEAPER_BUDGET_RATIO
from src.runtime.cancel_registry import StreamCancelled, register_turn, unregister_turn
from src.runtime.handlers import INTENT_HANDLERS, handle_clarification
from src.runtime.stages.criteria import _constraint_chips, run_criteria
from src.runtime.stages.decision import run_decision
from src.runtime.stages.intent import run_intent
from src.runtime.stages.multimodal import run_multimodal
from src.runtime.stages.recommendation import run_recommendation_text, run_retrieval
from src.runtime.stages.slot_checker import check_required_slots
from src.runtime.streaming import RunRetrieval, StageResult, StreamContext, cancel_background_tasks, run_with_heartbeat
from src.services.audit import record_audit_event
from src.services.cancellation import clear_chat_turn, register_chat_turn
from src.services.conversation_state import get_previous_criteria, save_recommendation_turn
from src.services.fallbacks import reset_fallback_events
from src.services.request_context import update_request_context
from src.types.schemas import ChatStreamRequest, DecisionResult, IntentResult, RecommendationResult
from src.types.sse_events import (
    Constraints,
    CriteriaPayload,
    ErrorEvent,
    EventSeq,
    EvidencePayload,
    ProductPayload,
    SSEEventBase,
    TextDeltaEvent,
    now_ms,
)

HEARTBEAT_INTERVAL_SECONDS = 0.8
PUBLIC_PIPELINE_ERROR_MESSAGE = "本轮导购处理失败，请稍后重试。"

logger = logging.getLogger(__name__)


class _TurnGuard:
    """Ensures cancel_token cleanup even if the generator is abandoned without iteration."""

    def __init__(self, session_id: str, turn_id: str) -> None:
        self.session_id = session_id
        self.turn_id = turn_id
        self._finalizer = weakref.finalize(self, unregister_turn, session_id, turn_id)

    def detach(self) -> None:
        self._finalizer.detach()


@dataclass(frozen=True)
class PipelineStages:
    run_multimodal: Callable[[str | None], Awaitable[dict[str, Any] | None]]
    run_intent: Callable[[str, ChatStreamRequest], Awaitable[IntentResult]]
    run_criteria: Callable[[str, ChatStreamRequest, IntentResult], Awaitable[CriteriaPayload]]
    run_retrieval: RunRetrieval
    run_recommendation_text: Callable[
        [CriteriaPayload, list[ProductPayload], dict[str, list[EvidencePayload]] | None],
        Awaitable[RecommendationResult],
    ]
    run_decision: Callable[
        [CriteriaPayload, list[ProductPayload], dict[str, list[EvidencePayload]] | None], Awaitable[DecisionResult]
    ]


async def chat_stream(session_id: str, body: ChatStreamRequest) -> AsyncGenerator[SSEEventBase, None]:
    reset_fallback_events()
    turn_id = body.client_turn_id or f"turn_{uuid.uuid4().hex[:8]}"
    update_request_context(trace_id=body.client_trace_id, session_id=session_id, turn_id=turn_id)
    cancel_token = register_turn(session_id, turn_id)
    guard = _TurnGuard(session_id, turn_id)
    ctx = StreamContext(
        session_id=session_id,
        turn_id=turn_id,
        deck_id=f"deck_{turn_id}",
        seq=EventSeq(turn_id),
        cancel_token=cancel_token,
        stages=_current_stages(),
        heartbeat_interval_seconds=HEARTBEAT_INTERVAL_SECONDS,
    )

    try:
        await register_chat_turn(session_id, turn_id, trace_id=body.client_trace_id)
        await record_audit_event(
            "chat.turn_started",
            session_id=session_id,
            turn_id=turn_id,
            trace_id=body.client_trace_id,
            resource_type="chat_turn",
            resource_id=turn_id,
            metadata={"has_image": bool(body.image_url), "message_chars": len(body.message)},
        )
        async for event in _run_chat_turn(ctx, body):
            yield event
        await record_audit_event(
            "chat.turn_completed",
            session_id=session_id,
            turn_id=turn_id,
            trace_id=body.client_trace_id,
            resource_type="chat_turn",
            resource_id=turn_id,
            metadata={"stage_timings_ms": ctx.stage_timings_ms},
        )
    except StreamCancelled:
        cancel_background_tasks(ctx.background_tasks)
        await record_audit_event(
            "chat.turn_cancelled",
            session_id=session_id,
            turn_id=turn_id,
            trace_id=body.client_trace_id,
            resource_type="chat_turn",
            resource_id=turn_id,
            metadata={"stage_timings_ms": ctx.stage_timings_ms},
        )
        yield ctx.done()
    except Exception as exc:
        cancel_background_tasks(ctx.background_tasks)
        logger.exception("chat_stream failed: session_id=%s turn_id=%s", session_id, turn_id)
        await record_audit_event(
            "chat.turn_failed",
            session_id=session_id,
            turn_id=turn_id,
            trace_id=body.client_trace_id,
            resource_type="chat_turn",
            resource_id=turn_id,
            metadata={"error_type": type(exc).__name__, "stage_timings_ms": ctx.stage_timings_ms},
        )
        yield ErrorEvent(
            session_id=session_id,
            turn_id=turn_id,
            seq=ctx.seq.next(),
            event_id=ctx.seq.event_id(),
            node_id=f"error_{turn_id}",
            created_at_ms=now_ms(),
            code="PIPELINE_ERROR",
            message=f"{PUBLIC_PIPELINE_ERROR_MESSAGE} trace_id={turn_id}",
            retryable=True,
        )
        yield ctx.done()
    finally:
        guard.detach()
        await clear_chat_turn(session_id, turn_id)
        unregister_turn(session_id, turn_id)


async def _run_chat_turn(ctx: StreamContext, body: ChatStreamRequest) -> AsyncGenerator[SSEEventBase, None]:
    pipeline_body = body
    if body.image_url:
        yield ctx.thinking("analyzing_image", "正在分析图片...")
        image_analysis: dict[str, Any] | None = None
        ctx.ensure_active()
        async for image_item in run_with_heartbeat(
            ctx,
            ctx.stages.run_multimodal(body.image_url),
            "analyzing_image",
            "正在分析图片...",
            timing_key="image_analysis",
        ):
            if isinstance(image_item, StageResult):
                image_analysis = image_item.value
            else:
                yield image_item
        pipeline_body = body.model_copy(update={"message": _message_with_image_context(body.message, image_analysis)})

    if _is_commercial_claim_question(pipeline_body.message):
        async for event in _emit_commercial_claim_reply(ctx):
            yield event
        return

    # Deterministic budget-patch intercept before LLM intent (铁律3)
    pipeline_body, synthetic_intent = await _maybe_intercept_budget_patch(ctx, pipeline_body)
    if synthetic_intent is not None:
        intent = synthetic_intent
    else:
        yield ctx.thinking("understanding", "正在理解您的需求...")
        intent = None
        ctx.ensure_active()
        async for intent_item in run_with_heartbeat(
            ctx,
            ctx.stages.run_intent(ctx.session_id, pipeline_body),
            "understanding",
            "正在理解您的需求...",
            timing_key="intent",
        ):
            if isinstance(intent_item, StageResult):
                intent = intent_item.value
            else:
                yield intent_item
        if intent is None:
            raise RuntimeError("intent stage completed without a result.")

    missing_slots = [] if pipeline_body.criteria_patch or pipeline_body.image_url else check_required_slots(pipeline_body.message, intent)
    if missing_slots:
        async for event in handle_clarification(ctx, missing_slots):
            yield event
        partial = _intent_to_partial_criteria(intent, pipeline_body.message)
        await save_recommendation_turn(ctx.session_id, partial, [], user_message=pipeline_body.message)
        return

    handler = INTENT_HANDLERS.get(intent.intent)
    if handler is None:
        logger.warning("Unhandled intent '%s' — falling back to clarification.", intent.intent)
        async for event in handle_clarification(ctx, ["category"]):
            yield event
        return
    async for event in handler(ctx, pipeline_body, intent):
        yield event


def _current_stages() -> PipelineStages:
    """Resolve stage implementations via module attributes so tests can monkeypatch them."""
    import sys

    mod = sys.modules[__name__]
    return PipelineStages(
        run_multimodal=getattr(mod, "run_multimodal"),
        run_intent=getattr(mod, "run_intent"),
        run_criteria=getattr(mod, "run_criteria"),
        run_retrieval=getattr(mod, "run_retrieval"),
        run_recommendation_text=getattr(mod, "run_recommendation_text"),
        run_decision=getattr(mod, "run_decision"),
    )


def _message_with_image_context(message: str, image_analysis: dict[str, Any] | None) -> str:
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
    retrieval_constraints = _image_analysis_to_retrieval_constraints(image_analysis)
    if retrieval_constraints:
        parts.append("检索条件=" + "，".join(f"{key}={value}" for key, value in retrieval_constraints.items() if value))
    return f"{message}\n图片分析：" + "；".join(parts) if parts else message


def _image_analysis_to_retrieval_constraints(image_analysis: dict[str, Any]) -> dict[str, str]:
    text = _image_analysis_text(image_analysis)
    category = _normalized_image_category(str(image_analysis.get("category_hint") or "")) or category_from_text(text)
    constraints: dict[str, str] = {}
    if category:
        constraints["category"] = category
    product_type = _product_type_from_image_text(text)
    if product_type:
        constraints["product_type"] = product_type
    skin_types = extract_skin_types(text)
    if skin_types:
        constraints["skin_type"] = skin_types[0]
    dietary = [term for term in ("无糖", "低糖") if term in text]
    if "0糖" in text or "零糖" in text:
        dietary.append("无糖")
    if dietary:
        constraints["dietary"] = " ".join(dict.fromkeys(dietary))
    if any(token in text for token in ("日常喝", "日常饮用", "日常使用")):
        constraints["use_scenario"] = "日常使用"
    return constraints


def _image_analysis_text(image_analysis: dict[str, Any]) -> str:
    traits = image_analysis.get("visible_traits")
    trait_text = " ".join(str(item) for item in traits) if isinstance(traits, list) else ""
    return " ".join(
        str(part)
        for part in (image_analysis.get("category_hint"), image_analysis.get("description"), trait_text)
        if part
    )


def _normalized_image_category(value: str) -> str | None:
    if not value:
        return None
    if "食品生活" in value:
        return "食品饮料"
    for category in ("美妆护肤", "数码电子", "服饰运动", "食品饮料"):
        if category in value:
            return category
    return None


def _product_type_from_image_text(text: str) -> str | None:
    lowered = text.casefold()
    for canonical, aliases in PRODUCT_TYPE_ALIASES.items():
        if canonical.casefold() in lowered or any(alias.casefold() in lowered for alias in aliases):
            return normalize_product_type(canonical)
    return None


# ── Deterministic commercial-claim intercept (铁律3: routing in code, not prompt) ──

_COMMERCIAL_CLAIM_MARKERS = (
    "优惠券", "领券", "满减", "折扣", "打折", "包邮", "免邮", "运费",
    "下单", "购买链接", "怎么买", "怎么下单", "哪里买",
    "库存", "现货", "有货", "缺货", "有货吗",
    "订单", "我的订单", "订单状态", "物流", "快递", "发货",
    "什么时候到", "几天到", "送到",
)

COMMERCIAL_CLAIM_REPLY = "当前商品库无该字段，不能确认。"


def _is_commercial_claim_question(message: str) -> bool:
    lowered = message.strip().lower()
    return any(marker in lowered for marker in _COMMERCIAL_CLAIM_MARKERS)


async def _emit_commercial_claim_reply(ctx: StreamContext) -> AsyncGenerator[SSEEventBase, None]:
    yield ctx.thinking("understanding", "正在处理...")
    yield TextDeltaEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"ai_text_{ctx.turn_id}",
        created_at_ms=now_ms(),
        message_id=f"msg_{ctx.turn_id}",
        delta=COMMERCIAL_CLAIM_REPLY,
        done=True,
    )
    yield ctx.done()


# ── Deterministic budget-phrase routing (铁律3: routing in code, not prompt) ──

_BUDGET_REDUCTION_MARKERS = (
    "预算降到", "预算压低到", "预算控制在", "预算不超过",
    "便宜点", "便宜一点", "再便宜点", "再便宜一点",
    "便宜些", "降价", "降低预算", "压低预算",
)

_BUDGET_CAP_PATTERNS: tuple[re.Pattern[str], ...] = (
    re.compile(r"(\d+(?:\.\d+)?)\s*(?:元)?\s*(?:以内|以下|内|之内)"),
    re.compile(r"(?:预算|价格).*?(\d+(?:\.\d+)?)"),
)


def _is_budget_reduction_phrase(message: str) -> bool:
    lowered = message.strip().lower()
    if any(marker in lowered for marker in _BUDGET_REDUCTION_MARKERS):
        return True
    for pattern in _BUDGET_CAP_PATTERNS:
        if pattern.search(message):
            return True
    return False


def _parse_budget_from_message(message: str, previous: CriteriaPayload) -> float | None:
    for pattern in _BUDGET_CAP_PATTERNS:
        match = pattern.search(message)
        if match:
            try:
                return float(match.group(1))
            except (ValueError, IndexError):
                continue
    if any(token in message for token in ("便宜点", "便宜一点", "再便宜点", "再便宜一点", "便宜些", "降价")):
        current = previous.constraints.budget_max
        if current is not None:
            return max(CHEAPER_BUDGET_MIN_MAX, round(current * CHEAPER_BUDGET_RATIO, 2))
        return CHEAPER_BUDGET_DEFAULT_MAX
    return None


async def _maybe_intercept_budget_patch(
    ctx: StreamContext,
    body: ChatStreamRequest,
) -> tuple[ChatStreamRequest, IntentResult | None]:
    if body.criteria_patch:
        return body, None
    if not _is_budget_reduction_phrase(body.message):
        return body, None
    previous = await get_previous_criteria(ctx.session_id)
    if previous is None or not previous.category:
        return body, None
    budget_value = _parse_budget_from_message(body.message, previous)
    if budget_value is None:
        return body, None
    skip = list(body.skip_stages)
    if "recommendation" not in skip:
        skip.append("recommendation")
    patched = body.model_copy(update={
        "criteria_patch": {"constraints": {"budget_max": budget_value}},
        "skip_stages": skip,
    })
    synthetic_intent = IntentResult(intent="recommend", category=previous.category or "")
    return patched, synthetic_intent


def _intent_to_partial_criteria(intent: IntentResult, message: str) -> CriteriaPayload:
    """Construct a partial CriteriaPayload from intent for multi-turn clarification continuity.

    The resulting payload captures the category and any constraints already extracted
    from the user's message. It reuses the existing conversation state machinery —
    get_conversation_summary and get_previous_criteria both pick it up automatically.
    """
    allowed = set(Constraints.model_fields)
    constraint_kwargs: dict[str, Any] = {}
    for key, value in (intent.extracted_constraints or {}).items():
        if key in allowed and value is not None:
            constraint_kwargs[key] = value
    constraints = Constraints(**constraint_kwargs)
    category = intent.category or ""
    chips = [category] if category else []
    chips.extend(_constraint_chips(constraints))
    return CriteriaPayload(
        criteria_id=f"pending_{uuid.uuid4().hex[:8]}",
        category=category,
        summary="，".join(chips) if chips else "",
        chips=chips,
        constraints=constraints,
    )
