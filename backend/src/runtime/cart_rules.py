"""Deterministic cart intent helpers."""

from __future__ import annotations

import re

from src.services.conversation_state import get_previous_product_ids
from src.types.schemas import IntentResult

ORDINAL_INDEXES = {
    "第一个": 0,
    "第一款": 0,
    "第1个": 0,
    "第1款": 0,
    "第二个": 1,
    "第二款": 1,
    "第2个": 1,
    "第2款": 1,
    "第三个": 2,
    "第三款": 2,
    "第3个": 2,
    "第3款": 2,
}

PREVIOUS_PRODUCT_REFERENCES = ("这个", "这款", "刚才", "第一个", "第二个", "第三个", "第1", "第2", "第3")

QUANTITY_PATTERNS: tuple[re.Pattern[str], ...] = (
    re.compile(r"(?:改成|改为|设置为|调整为|变成)\s*(\d+)\s*(?:件|个|份)?"),
    re.compile(r"(\d+)\s*(?:件|个|份)"),
)


async def referenced_product_id(session_id: str, intent: IntentResult, message: str) -> str | None:
    if intent.target_product_id:
        return intent.target_product_id
    product_ids = await get_previous_product_ids(session_id)
    if not product_ids:
        return None
    index = ordinal_index(message)
    if index is not None and 0 <= index < len(product_ids):
        return product_ids[index]
    return product_ids[0]


def quantity_from_intent(intent: IntentResult, message: str, *, default: int) -> int:
    raw = intent.extracted_constraints.get("quantity") or intent.extracted_constraints.get("target_quantity")
    parsed = _positive_int(raw)
    if parsed is not None:
        return parsed
    for pattern in QUANTITY_PATTERNS:
        match = pattern.search(message)
        if not match:
            continue
        parsed = _positive_int(match.group(1))
        if parsed is not None:
            return parsed
    return default


def message_refers_to_previous_product(message: str) -> bool:
    return any(token in message for token in PREVIOUS_PRODUCT_REFERENCES)


def ordinal_index(message: str) -> int | None:
    for token, index in ORDINAL_INDEXES.items():
        if token in message:
            return index
    match = re.search(r"第\s*(\d+)\s*(?:个|款|件|项)?", message)
    return max(0, int(match.group(1)) - 1) if match else None


def _positive_int(value: object) -> int | None:
    if isinstance(value, int | float):
        parsed = int(value)
    elif isinstance(value, str) and value.strip().isdigit():
        parsed = int(value.strip())
    else:
        return None
    return parsed if parsed > 0 else None
