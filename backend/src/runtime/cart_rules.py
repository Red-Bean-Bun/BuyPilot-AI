"""Deterministic cart intent helpers."""

from __future__ import annotations

import re

from src.repos.products import list_products
from src.services.conversation_state import get_previous_product_ids
from src.types.schemas import IntentResult

ORDINAL_INDEXES = {
    "第一个": 0, "第一款": 0, "第1个": 0, "第1款": 0,
    "第二个": 1, "第二款": 1, "第2个": 1, "第2款": 1,
    "第三个": 2, "第三款": 2, "第3个": 2, "第3款": 2,
    "第四个": 3, "第四款": 3, "第4个": 3, "第4款": 3,
    "第五个": 4, "第五款": 4, "第5个": 4, "第5款": 4,
}

PREVIOUS_PRODUCT_REFERENCES = (
    "这个", "这款", "刚才",
    "第一个", "第二个", "第三个", "第四个", "第五个",
    "第一款", "第二款", "第三款", "第四款", "第五款",
    "第1", "第2", "第3", "第4", "第5",
)

QUANTITY_PATTERNS: tuple[re.Pattern[str], ...] = (
    re.compile(r"(?:改成|改为|设置为|调整为|变成)\s*(\d+)\s*(?:件|个|份)?"),
    re.compile(r"(\d+)\s*(?:件|个|份)"),
)

# Android client embeds product_id in cart commands: "把商品ID p_beauty_023 加入购物车"
_EMBEDDED_PRODUCT_ID = re.compile(r"\b(p_[a-z]+_\d+)\b")


async def referenced_product_id(session_id: str, intent: IntentResult, message: str) -> str | None:
    # 1. Explicit product ID (structured data or API field)
    if intent.target_product_id:
        return intent.target_product_id
    # 2. Embedded product ID in message (Android client protocol)
    embedded = _EMBEDDED_PRODUCT_ID.search(message)
    if embedded:
        return embedded.group(1)
    # 3. Product name/brand → catalog lookup (LLM-extracted)
    if intent.target_product_name:
        resolved = resolve_product_by_name(intent.target_product_name)
        if resolved:
            return resolved
    # 4. Ordinal reference → session history
    product_ids = await get_previous_product_ids(session_id)
    if product_ids:
        index = ordinal_index(message)
        if index is not None and 0 <= index < len(product_ids):
            return product_ids[index]
    # 5. Implicit reference ("这个"/"这款") → last recommended product
    if message_refers_to_previous_product(message) and product_ids:
        return product_ids[-1]
    # 6. Unresolvable → None (handler should clarify, not guess)
    return None


def resolve_product_by_name(name: str) -> str | None:
    """Match a brand/product name against the catalog. Returns product_id if unique.

    Priority: exact brand match → exact name match → substring match.
    Full branded names like "理肤泉特护清盈防晒乳" resolve via exact name match,
    not the loose "brand in query" substring that matches every product of the brand.
    """
    query = name.strip().lower()
    if not query:
        return None
    exact: list[str] = []
    substring: list[str] = []
    for product in list_products():
        brand = (product.brand or "").strip().lower()
        product_name = (product.name or "").strip().lower()
        sub_cat = (product.sub_category or "").strip().lower()
        if query == brand or query == product_name or query == sub_cat:
            exact.append(product.product_id)
        elif query in brand or query in product_name or query in sub_cat:
            substring.append(product.product_id)
    unique_exact = set(exact)
    if len(unique_exact) == 1:
        return exact[0]
    unique_sub = set(substring)
    if len(unique_sub) == 1:
        return substring[0]
    return None


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
