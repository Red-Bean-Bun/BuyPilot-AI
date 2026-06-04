"""Task-oriented LLM client.

Runtime stages call these task methods only. They must not choose raw model
names or call provider SDKs directly.
"""

from __future__ import annotations

import asyncio
import json
import logging
import re
from collections.abc import AsyncGenerator
from typing import Any

from pydantic import ValidationError

from src.config import user_messages as msg
from src.config.settings import get_settings
from src.repos.observability_llm import update_llm_call_parsed_json
from src.repos.products import list_products
from src.services.image_upload import image_url_to_provider_url
from src.services.llm_gateway import (
    LiveLLMUnavailable,
    _call_chat_task,
    _stream_chat_task,
)
from src.services.llm_task_payloads import (
    comparison_narration_messages,
    criteria_from_live_payload as _criteria_from_live_payload,
    criteria_messages,
    decision_messages,
    image_messages,
    intent_messages,
    normalize_intent_payload as _normalize_intent_payload,
    parse_json_object as _parse_json_object,
    recommendation_messages,
    recommendation_stream_messages,
)
from src.services.recommendation_reasons import build_reason_atoms
from src.services.request_context import get_request_context
from src.types.schemas import DecisionResult, IntentResult, RecommendationResult
from src.types.sse_events import CriteriaPayload, EvidencePayload, ProductPayload

logger = logging.getLogger(__name__)
_LOG_PAYLOAD_PREVIEW_CHARS = 2000
_PARSED_JSON_UPDATE_RETRY_DELAYS_SECONDS = (0.02, 0.05, 0.1, 0.2, 0.4)
_PRODUCT_ID_RE = re.compile(r"\bp_[a-z]+_\d+\b")
_FORBIDDEN_COMMERCIAL_TERMS = (
    "库存",
    "现货",
    "有货",
    "缺货",
    "优惠",
    "优惠券",
    "领券",
    "满减",
    "折扣",
    "打折",
    "包邮",
    "免邮",
    "运费",
    "次日达",
    "当日达",
    "购买链接",
    "下单",
    "立即购买",
    "物流",
    "快递",
    "发货",
    "几天到",
    "送到",
)

# Backward-compatible alias for recommendation validation.
_FORBIDDEN_RECOMMENDATION_TERMS = _FORBIDDEN_COMMERCIAL_TERMS


def _schedule_parsed_json_update(
    task: str,
    parsed_json: dict[str, Any] | None,
    validation_error: str | None,
) -> asyncio.Task[None] | None:
    """Fire-and-forget update of parsed_json for the most recent LLM call."""
    if not get_settings().observability_local_enabled:
        return None

    context = get_request_context()
    turn_id = context.turn_id if context else None
    if not turn_id:
        return None

    async def _update():
        attempts = len(_PARSED_JSON_UPDATE_RETRY_DELAYS_SECONDS) + 1
        for attempt in range(attempts):
            try:
                updated = await update_llm_call_parsed_json(turn_id, task, parsed_json, validation_error)
            except Exception as exc:
                logger.debug("Failed to update parsed_json for %s: %s", task, exc)
                return
            if updated:
                return
            if attempt < len(_PARSED_JSON_UPDATE_RETRY_DELAYS_SECONDS):
                await asyncio.sleep(_PARSED_JSON_UPDATE_RETRY_DELAYS_SECONDS[attempt])
        logger.debug(
            "No LLM observability row found for parsed_json update after %s attempts: turn_id=%s task=%s",
            attempts,
            turn_id,
            task,
        )

    return asyncio.create_task(_update())


__all__ = [
    "LiveLLMUnavailable",
    "analyze_image",
    "analyze_intent",
    "generate_criteria",
    "generate_decision",
    "generate_recommendation",
    "stream_comparison_narration",
    "stream_recommendation",
]


async def analyze_intent(
    message: str,
    history: list[dict[str, Any]] | None = None,
    image_url: str | None = None,
    conversation_context: str = "",
) -> IntentResult:
    live = await _call_chat_task(
        "analyze_intent",
        intent_messages(message, history, image_url, conversation_context),
        json_object=True,
    )
    parsed = _require_json_object(live, "analyze_intent")
    try:
        result = IntentResult.model_validate(_normalize_intent_payload(parsed))
        _schedule_parsed_json_update("analyze_intent", result.model_dump(), None)
        return result
    except ValidationError as exc:
        _schedule_parsed_json_update("analyze_intent", None, str(exc.errors()))
        logger.warning(
            "Live intent payload failed schema validation. validation_errors=%s payload_preview=%s",
            _json_preview(exc.errors()),
            _json_preview(parsed),
        )
        raise RuntimeError("Live intent payload failed schema validation.") from exc


async def generate_criteria(
    message: str,
    intent: IntentResult,
    feedback: dict[str, list[str]] | None = None,
    existing: CriteriaPayload | None = None,
    conversation_context: str = "",
) -> CriteriaPayload:
    live = await _call_chat_task(
        "generate_criteria",
        criteria_messages(
            message,
            intent.model_dump(),
            feedback,
            existing.model_dump() if existing else None,
            conversation_context,
        ),
        json_object=True,
    )
    parsed = _require_json_object(live, "generate_criteria")
    criteria = _criteria_from_live_payload(parsed, existing)
    if criteria:
        _schedule_parsed_json_update("generate_criteria", criteria.model_dump(), None)
        return criteria
    _schedule_parsed_json_update("generate_criteria", parsed, "criteria validation failed")
    raise RuntimeError("Live criteria payload failed schema validation.")


async def generate_recommendation(
    criteria: CriteriaPayload,
    products: list[ProductPayload],
    evidence_by_product: dict[str, list[EvidencePayload]] | None = None,
) -> RecommendationResult:
    if not products:
        return RecommendationResult(text_chunks=[msg.NO_MATCH], products=[])
    live = await _call_chat_task(
        "generate_recommendation",
        recommendation_messages(
            criteria,
            products,
            evidence_by_product,
            {
                product.product_id: build_reason_atoms(
                    criteria,
                    product,
                    (evidence_by_product or {}).get(product.product_id, []),
                )
                for product in products
            },
        ),
        json_object=True,
    )
    parsed = _require_json_object(live, "generate_recommendation")
    chunks = parsed.get("text_chunks")
    if isinstance(chunks, list) and all(isinstance(chunk, str) for chunk in chunks):
        validated_chunks = chunks[:4]
        _validate_recommendation_chunks(validated_chunks, products)
        result = RecommendationResult(text_chunks=validated_chunks, products=products)
        _schedule_parsed_json_update("generate_recommendation", result.model_dump(), None)
        return result
    _schedule_parsed_json_update("generate_recommendation", parsed, "recommendation validation failed")
    raise RuntimeError("Live recommendation payload failed schema validation.")


async def stream_recommendation(
    criteria: CriteriaPayload,
    products: list[ProductPayload],
    evidence_by_product: dict[str, list[EvidencePayload]] | None = None,
) -> AsyncGenerator[str, None]:
    if not products:
        yield msg.NO_MATCH
        return
    reason_atoms_by_product = {
        product.product_id: build_reason_atoms(
            criteria,
            product,
            (evidence_by_product or {}).get(product.product_id, []),
        )
        for product in products
    }
    messages = recommendation_stream_messages(
        criteria,
        products,
        evidence_by_product,
        reason_atoms_by_product,
    )
    emitted = False
    try:
        async for delta in _stream_chat_task("generate_recommendation", messages):
            if not delta:
                continue
            emitted = True
            yield delta
    except Exception:
        if emitted:
            raise
        logger.warning(
            "Live recommendation stream failed before first delta; falling back to non-stream response.",
            exc_info=True,
        )
        fallback = await generate_recommendation(criteria, products, evidence_by_product)
        for chunk in fallback.text_chunks:
            if chunk:
                yield chunk


async def analyze_image(image_url: str) -> dict[str, Any]:
    provider_image_url = image_url_to_provider_url(image_url)
    live = await _call_chat_task(
        "analyze_image",
        image_messages(image_url, provider_image_url),
        json_object=True,
    )
    parsed = _require_json_object(live, "analyze_image")
    parsed["image_url"] = image_url
    _schedule_parsed_json_update("analyze_image", parsed, None)
    return parsed


async def generate_decision(
    criteria: CriteriaPayload,
    products: list[ProductPayload],
    evidence_by_product: dict[str, list[EvidencePayload]] | None = None,
    *,
    locked_winner_product_id: str | None = None,
    score_breakdown: dict[str, Any] | None = None,
) -> DecisionResult:
    if not products:
        return DecisionResult(winner_product_id="", summary=msg.NO_MATCH)
    valid_ids = [p.product_id for p in products]
    authoritative_winner = locked_winner_product_id if locked_winner_product_id in valid_ids else None
    live = await _call_chat_task(
        "generate_decision",
        decision_messages(
            criteria,
            products,
            evidence_by_product,
            locked_winner_product_id=authoritative_winner,
            score_breakdown=score_breakdown,
        ),
        json_object=True,
    )
    parsed = _require_json_object(live, "generate_decision")
    _schedule_parsed_json_update("generate_decision", parsed, None)
    winner_id = parsed.get("winner_product_id", "")
    if authoritative_winner is not None and winner_id != authoritative_winner:
        logger.warning(
            "Decision LLM winner_id %s differed from locked winner %s; using deterministic locked decision.",
            winner_id,
            authoritative_winner,
        )
        return _deterministic_locked_decision(authoritative_winner, products, score_breakdown)
    if winner_id not in valid_ids:
        logger.warning(
            "Decision winner_id %s not in candidate list %s; falling back to first candidate.",
            winner_id,
            valid_ids,
        )
        winner_id = valid_ids[0]
    decision = DecisionResult(
        winner_product_id=winner_id,
        summary=parsed.get("summary", msg.DECISION_SUMMARY_FALLBACK_TEMPLATE.format(winner_id=winner_id)),
        why=parsed.get("why", [msg.DECISION_WHY_DEFAULT])
        if isinstance(parsed.get("why"), list)
        else [msg.DECISION_WHY_DEFAULT],
        not_for=parsed.get("not_for", []) if isinstance(parsed.get("not_for"), list) else [],
    )
    return _sanitize_decision(decision, valid_ids, products)


def _deterministic_locked_decision(
    winner_product_id: str,
    products: list[ProductPayload],
    score_breakdown: dict[str, Any] | None = None,
) -> DecisionResult:
    winner = next((product for product in products if product.product_id == winner_product_id), products[0])
    score = (score_breakdown or {}).get("final_score")
    score_text = f"，综合评分 {score}" if isinstance(score, int | float) else ""
    reasons = [
        msg.DECISION_LOCKED_REASON_TEMPLATE.format(name=winner.name, score_text=score_text),
        msg.DECISION_LOCKED_REASON_SOURCE,
    ]
    return DecisionResult(
        winner_product_id=winner.product_id,
        summary=msg.DECISION_LOCKED_SUMMARY_TEMPLATE.format(name=winner.name),
        why=reasons,
        not_for=[],
    )


def _require_json_object(text: str, task: str) -> dict[str, Any]:
    if not text:
        raise RuntimeError(f"Live {task} response was empty.")
    parsed = _parse_json_object(text)
    if parsed is None:
        raise RuntimeError(f"Live {task} response was not a JSON object.")
    return parsed


def _validate_recommendation_chunks(chunks: list[str], products: list[ProductPayload]) -> None:
    text = "\n".join(chunks)
    if not text.strip():
        raise RuntimeError("Live recommendation payload had empty text_chunks.")

    valid_ids = {product.product_id for product in products}
    unknown_ids = sorted(set(_PRODUCT_ID_RE.findall(text)) - valid_ids)
    if unknown_ids:
        raise RuntimeError(f"Live recommendation text referenced unknown product ids: {unknown_ids}.")

    candidate_names = {product.name for product in products}
    known_names = {product.name for product in list_products()}
    unknown_names = sorted(name for name in known_names - candidate_names if name and name in text)
    if unknown_names:
        raise RuntimeError("Live recommendation text referenced products outside the candidate set.")

    forbidden_terms = [term for term in _FORBIDDEN_RECOMMENDATION_TERMS if term in text]
    if forbidden_terms:
        raise RuntimeError(f"Live recommendation text contained unsupported commercial claims: {forbidden_terms}.")


# ── Decision text sanitisation (总评 #7) ────────────────────────────────────

_DECISION_FORBIDDEN_TERMS = _FORBIDDEN_COMMERCIAL_TERMS
_DECISION_SAFE_REPLACEMENT = msg.DECISION_SAFE_REPLACEMENT


def _sanitize_decision(
    decision: DecisionResult,
    valid_ids: list[str],
    products: list[ProductPayload],
) -> DecisionResult:
    """Remove commercial claims and non-candidate product references from decision text."""
    candidate_names = {p.name for p in products}
    all_names = {p.name for p in list_products()}
    non_candidate_names = sorted(name for name in all_names - candidate_names if name and len(name) >= 2)

    def _clean(text: str) -> str:
        for term in _DECISION_FORBIDDEN_TERMS:
            if term in text:
                text = text.replace(term, _DECISION_SAFE_REPLACEMENT)
        for name in non_candidate_names:
            if name in text:
                text = text.replace(name, _DECISION_SAFE_REPLACEMENT)
        return text

    # Also check the why/not_for lists
    return DecisionResult(
        winner_product_id=decision.winner_product_id,
        summary=_clean(decision.summary),
        why=[_clean(w) for w in decision.why],
        not_for=[_clean(n) for n in decision.not_for],
        decision_status=decision.decision_status,
        confidence=decision.confidence,
        next_step=decision.next_step,
    )


async def stream_comparison_narration(
    products: list[ProductPayload],
    axes: list,
    winner_product_id: str | None,
    winner_reason: str | None,
    tradeoffs: list[str],
    risk_notes: list,
    mode: str,
) -> AsyncGenerator[str, None]:
    """Stream comparison narration text from the LLM."""
    messages = comparison_narration_messages(
        products, axes, winner_product_id, winner_reason, tradeoffs, risk_notes, mode,
    )
    emitted = False
    try:
        async for delta in _stream_chat_task("generate_comparison", messages):
            if not delta:
                continue
            emitted = True
            yield delta
    except Exception:
        if emitted:
            raise
        logger.warning(
            "Comparison narration stream failed; yielding fallback text.",
            exc_info=True,
        )
        if winner_reason:
            yield winner_reason
        elif tradeoffs:
            yield "、".join(tradeoffs[:2])
        else:
            yield "两款商品各有特点，建议根据个人偏好选择。"


def _json_preview(value: Any) -> str:
    try:
        text = json.dumps(value, ensure_ascii=False, default=str, separators=(",", ":"))
    except TypeError:
        text = str(value)
    if len(text) <= _LOG_PAYLOAD_PREVIEW_CHARS:
        return text
    return f"{text[:_LOG_PAYLOAD_PREVIEW_CHARS]}...<truncated>"
