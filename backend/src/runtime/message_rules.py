"""Deterministic message routing rules used by the chat pipeline."""

from __future__ import annotations

import re
from typing import Any

from src.config.domain_terms import (
    PRODUCT_TYPE_ALIASES,
    category_from_text,
    extract_skin_types,
    normalize_category,
    normalize_product_type,
)
from src.config.tuning import CHEAPER_BUDGET_DEFAULT_MAX, CHEAPER_BUDGET_MIN_MAX, CHEAPER_BUDGET_RATIO
from src.services.conversation_state import get_previous_criteria
from src.types.schemas import ChatStreamRequest, IntentResult

COMMERCIAL_CLAIM_REPLY = "当前商品库无该字段，不能确认。"

_COMMERCIAL_CLAIM_MARKERS = (
    "优惠券",
    "领券",
    "满减",
    "折扣",
    "打折",
    "包邮",
    "免邮",
    "运费",
    "下单",
    "购买链接",
    "怎么买",
    "怎么下单",
    "哪里买",
    "库存",
    "现货",
    "有货",
    "缺货",
    "有货吗",
    "订单",
    "我的订单",
    "订单状态",
    "物流",
    "快递",
    "发货",
    "什么时候到",
    "几天到",
    "送到",
)

_BUDGET_REDUCTION_MARKERS = (
    "预算降到",
    "预算压低到",
    "预算控制在",
    "预算不超过",
    "便宜点",
    "便宜一点",
    "再便宜点",
    "再便宜一点",
    "便宜些",
    "降价",
    "降低预算",
    "压低预算",
)

_CHEAPER_MARKERS = ("便宜点", "便宜一点", "再便宜点", "再便宜一点", "便宜些", "降价")

_BUDGET_CAP_PATTERNS: tuple[re.Pattern[str], ...] = (
    re.compile(r"(\d+(?:\.\d+)?)\s*(?:元)?\s*(?:以内|以下|内|之内)"),
    re.compile(r"(?:预算|价格).*?(\d+(?:\.\d+)?)"),
)


def message_with_image_context(message: str, image_analysis: dict[str, Any] | None) -> str:
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
    retrieval_constraints = image_analysis_to_retrieval_constraints(image_analysis)
    if retrieval_constraints:
        parts.append("检索条件=" + "，".join(f"{key}={value}" for key, value in retrieval_constraints.items() if value))
    return f"{message}\n图片分析：" + "；".join(parts) if parts else message


def image_analysis_to_retrieval_constraints(image_analysis: dict[str, Any]) -> dict[str, str]:
    text = _image_analysis_text(image_analysis)
    category = normalize_category(image_analysis.get("category_hint")) or category_from_text(text)
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


def is_commercial_claim_question(message: str) -> bool:
    lowered = message.strip().lower()
    return any(marker in lowered for marker in _COMMERCIAL_CLAIM_MARKERS)


async def maybe_intercept_budget_patch(
    session_id: str,
    body: ChatStreamRequest,
) -> tuple[ChatStreamRequest, IntentResult | None]:
    if body.criteria_patch:
        return body, None
    if not _is_budget_reduction_phrase(body.message):
        return body, None
    previous = await get_previous_criteria(session_id)
    if previous is None or not previous.category:
        return body, None
    budget_value = _parse_budget_from_message(body.message, previous.constraints.budget_max)
    if budget_value is None:
        return body, None
    skip = list(body.skip_stages)
    if "recommendation" not in skip:
        skip.append("recommendation")
    patched = body.model_copy(
        update={
            "criteria_patch": {"constraints": {"budget_max": budget_value}},
            "skip_stages": skip,
        }
    )
    synthetic_intent = IntentResult(intent="recommend", category=previous.category or "")
    return patched, synthetic_intent


def _image_analysis_text(image_analysis: dict[str, Any]) -> str:
    traits = image_analysis.get("visible_traits")
    trait_text = " ".join(str(item) for item in traits) if isinstance(traits, list) else ""
    return " ".join(
        str(part)
        for part in (image_analysis.get("category_hint"), image_analysis.get("description"), trait_text)
        if part
    )


def _product_type_from_image_text(text: str) -> str | None:
    lowered = text.casefold()
    for canonical, aliases in PRODUCT_TYPE_ALIASES.items():
        if canonical.casefold() in lowered or any(alias.casefold() in lowered for alias in aliases):
            return normalize_product_type(canonical)
    return None


def _is_budget_reduction_phrase(message: str) -> bool:
    lowered = message.strip().lower()
    if any(marker in lowered for marker in _BUDGET_REDUCTION_MARKERS):
        return True
    return any(pattern.search(message) for pattern in _BUDGET_CAP_PATTERNS)


# ── Natural language adjustment hints (PRD 05 §6.5) ──────────────────────

_ADJUST_SOFTEN_PATTERN = re.compile(r"再(.{1,4})(?:一点|些|点儿)")
_ADJUST_NOT_TOO_PATTERN = re.compile(r"不要太(.{1,6})")
_ADJUST_AVOID_PATTERN = re.compile(r"不要([^，。！？；、,.!?;:\n]{1,8})")
_ADJUST_BUDGET_CAP_PATTERN = re.compile(r"预算[^0-9]{0,3}(\d+(?:\.\d+)?)")
_ADJUST_BUDGET_LOWER = ("预算再低", "再便宜", "便宜点", "便宜些")
_ADJUST_BUDGET_HIGHER = ("预算再高", "贵一点", "好一点")


def extract_adjustment_hints(message: str) -> dict[str, Any]:
    """Extract fuzzy adjustment hints from natural-language follow-up messages.

    Returns a dict that can be merged into IntentResult.extracted_constraints
    to influence criteria generation.
    """
    hints: dict[str, Any] = {}
    text = message.strip()

    m = _ADJUST_SOFTEN_PATTERN.search(text)
    if m:
        hints["preference"] = [m.group(1)]

    m = _ADJUST_NOT_TOO_PATTERN.search(text)
    if m:
        hints.setdefault("avoid_trait", []).append(m.group(1))

    m = _ADJUST_AVOID_PATTERN.search(text)
    if m:
        term = m.group(1).strip()
        # Stop-word prefixes: terms starting with these are degree/intensity
        # modifiers, not ingredients to avoid (e.g. "太贵" = "too expensive",
        # not an ingredient named "太贵").
        _AVOID_STOP_PREFIXES = ("太", "再", "那么", "这么", "很", "特别", "非常", "最", "含", "含有")
        if not any(term.startswith(p) for p in _AVOID_STOP_PREFIXES):
            hints.setdefault("ingredient_avoid", []).append(term)

    if any(phrase in text for phrase in _ADJUST_BUDGET_LOWER):
        hints["budget_direction"] = "lower"
    elif any(phrase in text for phrase in _ADJUST_BUDGET_HIGHER):
        hints["budget_direction"] = "higher"

    m = _ADJUST_BUDGET_CAP_PATTERN.search(text)
    if m:
        try:
            hints["budget_max"] = float(m.group(1))
        except ValueError:
            pass

    return hints


# ── Replace-deck detection (PRD 06 §5.2) ─────────────────────────────────

_REPLACE_DECK_PATTERNS = (
    "换一组",
    "换一批",
    "再来一组",
    "再来一批",
    "不看这些了",
    "这些都不喜欢",
    "都不合适",
    "换几个",
)


def is_replace_deck_phrase(message: str) -> bool:
    """True when the user is asking to replace the current deck with fresh candidates."""
    lowered = message.strip().lower()
    return any(pattern in lowered for pattern in _REPLACE_DECK_PATTERNS)


def _parse_budget_from_message(message: str, current_budget_max: float | None) -> float | None:
    for pattern in _BUDGET_CAP_PATTERNS:
        match = pattern.search(message)
        if not match:
            continue
        try:
            return float(match.group(1))
        except (ValueError, IndexError):
            continue
    if any(token in message for token in _CHEAPER_MARKERS):
        if current_budget_max is not None:
            return max(CHEAPER_BUDGET_MIN_MAX, round(current_budget_max * CHEAPER_BUDGET_RATIO, 2))
        return CHEAPER_BUDGET_DEFAULT_MAX
    return None
