"""Deterministic message routing rules used by the chat pipeline."""

from __future__ import annotations

import re
from typing import Any

from src.config import user_messages as msg
from src.config.domain_terms import (
    BRAND_SYNONYMS,
    INTENT_TERMS,
    PRODUCT_TYPE_ALIASES,
    PRODUCT_TYPE_HIERARCHY,
    category_from_text,
    extract_skin_types,
    get_brand_case,
    get_known_brands,
    has_negation_prefix,
    is_known_brand_or_synonym,
    normalize_category,
    normalize_product_type,
    resolve_brand_prefer,
)
from src.config.tuning import CHEAPER_BUDGET_DEFAULT_MAX, CHEAPER_BUDGET_MIN_MAX, CHEAPER_BUDGET_RATIO
from src.services.conversation_state import get_previous_criteria
from src.types.schemas import ChatStreamRequest, IntentResult

COMMERCIAL_CLAIM_REPLY = msg.COMMERCIAL_CLAIM_REPLY

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

_CHEAPER_MARKERS = (
    "便宜点",
    "便宜一点",
    "再便宜点",
    "再便宜一点",
    "便宜些",
    "降价",
    "降低预算",
    "压低预算",
    "预算低一点",
    "预算降一点",
    "预算低一些",
)

_BUDGET_CAP_PATTERNS: tuple[re.Pattern[str], ...] = (
    re.compile(r"(\d+(?:\.\d+)?)\s*(?:元)?\s*(?:以内|以下|内|之内)"),
    re.compile(r"(?:预算|价格).*?(\d+(?:\.\d+)?)"),
)

# "4000元以上", "5000元以上", "1万以上" → budget_min
_BUDGET_MIN_PATTERN = re.compile(r"(\d+(?:\.\d+)?)\s*(?:元|块)?\s*以上")

# Budget range: "1000-1500元", "2000到3000"
_BUDGET_RANGE_PATTERN = re.compile(r"(\d+)\s*[-~到至]\s*(\d+)\s*(?:元)?")


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
    budget_min = _parse_budget_min_from_message(body.message)
    skip = list(body.skip_stages)
    if "recommendation" not in skip:
        skip.append("recommendation")
    patch_constraints: dict[str, Any] = {"budget_max": budget_value}
    if budget_min is not None:
        patch_constraints["budget_min"] = budget_min
    patched = body.model_copy(
        update={
            "criteria_patch": {"constraints": patch_constraints},
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

_ORIGIN_MARKERS = ("日系", "国产", "进口", "欧美", "韩系", "国货", "日本", "韩国", "美国", "德国")


def _looks_like_brand(term: str) -> bool:
    """Check if term matches a known brand in the product catalog.

    Primary: exact/substring match against runtime product data.
    Fallback: ASCII uppercase or 2-4 CJK chars (catches brands not in current catalog).
    """
    term_lower = term.strip().lower()
    known = get_known_brands()
    if term_lower in known:
        return True
    for brand in known:
        if len(term_lower) >= 2 and (term_lower in brand or brand in term_lower):
            return True
    # Heuristic fallback for brands not in current product data.
    # CJK terms that look like ingredient/feature words are excluded.
    _INGREDIENT_LIKE = {
        "油",
        "糖",
        "精",
        "素",
        "醇",
        "酸",
        "碱",
        "粉",
        "剂",
        "胶",
        "脂",
        "酶",
        "香",
        "钠",
        "钙",
        "铁",
        "锌",
        "维",
        "胺",
        "酚",
        "醛",
        "乳",
        "蜜",
        "霜",
        "露",
        "液",
        "膏",
        "膜",
    }
    if re.match(r"^[A-Z][A-Za-z0-9\- ]{1,20}$", term):
        return True
    if re.match(r"^[一-鿿]{2,4}$", term) and not any(kw in term for kw in _INGREDIENT_LIKE):
        return True
    return False


def _looks_like_origin(term: str) -> bool:
    """True when a user-specified avoid-term looks like an origin/country marker."""
    return any(origin in term for origin in _ORIGIN_MARKERS)


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
            if _looks_like_origin(term):
                hints.setdefault("origin_avoid", []).append(term)
            elif _looks_like_brand(term):
                hints.setdefault("brand_avoid", []).append(term)
            else:
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

    # Deterministic budget_min extraction: "4000元以上" → budget_min=4000.
    # The LLM sometimes misinterprets "以上" as a cap ("以内") — override it.
    budget_min = extract_budget_min_from_message(text)
    if budget_min is not None:
        hints["budget_min"] = budget_min
        # If the message is clearly a floor ("以上"), clear any budget_max
        # the LLM may have hallucinated. Otherwise budget_min==budget_max
        # would filter to exact-price-only products.
        hints["budget_max"] = None
        hints["budget_direction"] = "higher"

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
    "还有别的吗",
    "还有其他的吗",
    "别的呢",
    "其他的呢",
    "还有没有",
)


def is_replace_deck_phrase(message: str) -> bool:
    """True when the user is asking to replace the current deck with fresh candidates."""
    lowered = message.strip().lower()
    return any(pattern in lowered for pattern in _REPLACE_DECK_PATTERNS)


def _parse_budget_from_message(message: str, current_budget_max: float | None) -> float | None:
    # Try range pattern first: "1000-1500元" → extract budget_max=1500
    m = _BUDGET_RANGE_PATTERN.search(message)
    if m:
        try:
            return float(m.group(2))
        except (ValueError, IndexError):
            pass
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


def extract_budget_min_from_message(message: str) -> float | None:
    """Deterministic extraction of budget_min from patterns like '4000元以上'.

    The LLM (qwen-turbo) sometimes misinterprets '4000元以上' as a budget
    cap instead of a floor. This deterministic extractor provides a reliable
    fallback that the pipeline can use to override the LLM.
    """
    m = _BUDGET_MIN_PATTERN.search(message)
    if m:
        try:
            return float(m.group(1))
        except (ValueError, TypeError):
            return None
    return None


def _parse_budget_min_from_message(message: str) -> float | None:
    """Extract budget_min from range formats like '1000-1500元'."""
    m = _BUDGET_RANGE_PATTERN.search(message)
    if m:
        try:
            return float(m.group(1))
        except (ValueError, IndexError):
            pass
    return None


# ── Shopping-intent deterministic pre-check ──────────────────────────

_SHORT_GREETING_PATTERNS: tuple[str, ...] = (
    "你好",
    "hi",
    "hello",
    "嗨",
    "在吗",
    "在不在",
)

_CAPABILITY_QUESTION_MARKERS: tuple[str, ...] = (
    "你能做什么",
    "有什么功能",
    "能帮我做什么",
    "会做什么",
    "你是做什么的",
    "怎么用",
)

_SHOPPING_SIGNAL_MARKERS: tuple[str, ...] = (
    "想喝",
    "想吃",
    "想买",
    "想要",
    "想用",
    "推荐",
    "帮我选",
    "找一下",
    "有没有",
    "买个",
    "求推荐",
)


_CHECKOUT_QUESTION_MARKERS = ("怎么", "如何", "哪里", "链接", "状态", "物流", "订单")
_CONFIRM_TRAILING_PUNCT = "。！？!?,，~啊呢哦额嗯"


def maybe_checkout_intent(message: str) -> IntentResult | None:
    text = message.strip()
    if not text:
        return None
    if any(term in text for term in INTENT_TERMS["checkout_cancel"]):
        return IntentResult(intent="checkout_cancel", confidence=1.0)
    # Exact match after stripping trailing punctuation — avoids hijacking
    # "确认标准" while accepting "确认了。" or "就这样吧".
    normalized = text.rstrip(_CONFIRM_TRAILING_PUNCT)
    if normalized in INTENT_TERMS["checkout_confirm"] or text in INTENT_TERMS["checkout_confirm"]:
        return IntentResult(intent="checkout_confirm", confidence=1.0)
    if any(marker in text for marker in _CHECKOUT_QUESTION_MARKERS):
        return None
    if any(term in text for term in INTENT_TERMS["checkout_preview"]):
        return IntentResult(intent="checkout_preview", confidence=1.0)
    return None


def maybe_shopping_intent(message: str) -> IntentResult | None:
    """Deterministic pre-check for clearly non-shopping inputs only.

    Only short-circuits for obvious mismatches (greetings, capability questions).
    Does NOT short-circuit for shopping signals — those go through the LLM
    for proper intent classification and constraint extraction.
    """
    text = message.strip()

    lowered = text.lower()
    if len(text) <= 5 and any(g in lowered for g in _SHORT_GREETING_PATTERNS):
        return IntentResult(intent="chitchat", confidence=1.0)

    if any(m in text for m in _CAPABILITY_QUESTION_MARKERS):
        return IntentResult(intent="chitchat", confidence=1.0)

    return None


def has_shopping_signal(message: str) -> bool:
    """True when the message contains strong shopping-intent signals.

    Used as a post-LLM safety net: if the LLM classified a message as chitchat
    or feedback but it clearly contains shopping signals, the pipeline overrides
    to clarify/recommend.
    """
    text = message.strip()
    if any(m in text for m in _SHOPPING_SIGNAL_MARKERS):
        return True
    if extract_brand_prefer_from_message(text):
        return True
    # A detectable product type is itself a shopping signal (e.g. "一条裤子")
    if extract_product_type_hint(text):
        return True
    # Product lookup queries ("有XX吗")
    if extract_product_lookup_hints(text):
        return True
    return False


def extract_brand_prefer_from_message(message: str) -> list[str]:
    """Extract brand preference by matching known brands and synonyms.

    Returns catalog brand names (original case) found in the user's message.
    Checks both the product catalog brand list and BRAND_SYNONYMS for user
    expressions that map to catalog brands (e.g. "Mac" → "Apple 苹果").

    Longer brand names / synonym keys are checked first to avoid partial matches.
    """
    brands = get_known_brands()
    text_lower = message.strip().lower()
    text_original = message.strip()
    seen_brands: set[str] = set()

    # 1. Direct catalog brand matches — use case-preserved catalog name
    for brand_lower in sorted(brands, key=len, reverse=True):
        if len(brand_lower) < 2:
            continue
        if brand_lower not in text_lower:
            # Multi-word brand (e.g. "apple 苹果"): also try matching any
            # individual token so "Apple" alone triggers the match.
            if " " in brand_lower:
                tokens = [t for t in brand_lower.split() if len(t) >= 3]
                if not any(t in text_lower for t in tokens):
                    continue
            else:
                continue
        original_case = get_brand_case(brand_lower)
        if has_negation_prefix(text_original, original_case) or has_negation_prefix(text_lower, brand_lower):
            continue
        seen_brands.add(original_case)

    # 2. User synonym matches (e.g. "Mac" → "Apple 苹果")
    for synonym in sorted(BRAND_SYNONYMS, key=len, reverse=True):
        if len(synonym) >= 2 and synonym in text_lower:
            # Check negation with both forms — the text may use different case
            if has_negation_prefix(text_original, synonym) or has_negation_prefix(text_lower, synonym):
                continue
            seen_brands.add(BRAND_SYNONYMS[synonym])

    return list(seen_brands)


# ── Product-lookup extraction ────────────────────────────────────────

_PRODUCT_LOOKUP_PATTERNS: tuple[re.Pattern[str], ...] = (
    re.compile(r"有(.{1,8})吗[？?]?"),
    re.compile(r"有没有(.{1,8})[？?]?"),
    re.compile(r"(.{1,8})有吗[？?]?"),
)


def extract_product_lookup_hints(message: str) -> dict[str, Any] | None:
    """Detect product-existence queries like '有鼠标吗' or '有没有咖啡'.

    When the extracted entity is a known brand or brand synonym, returns
    brand_prefer instead of product_type (e.g. "有阿迪达斯吗" → brand_prefer).
    When the entity is a compound like "苹果手机", splits into brand_prefer
    + product_type.

    Returns a dict with 'product_type' and/or 'brand_prefer', or None when no
    product-lookup pattern is detected.
    """
    text = message.strip()
    for pat in _PRODUCT_LOOKUP_PATTERNS:
        m = pat.search(text)
        if m:
            product = m.group(1).strip()
            if len(product) < 2:
                return None
            if product in {
                "什么",
                "哪些",
                "哪个",
                "那种",
                "好的",
                "便宜",
                "贵的",
                "新的",
                "旧的",
                "大的",
                "小的",
            }:
                return None
            # Pure brand query: "有阿迪达斯吗" → brand_prefer
            if is_known_brand_or_synonym(product):
                resolved = resolve_brand_prefer(product)
                if resolved:
                    return {"brand_prefer": [resolved]}
            # Compound entity: "苹果手机" → brand_prefer + product_type
            compound = _try_split_compound_entity(product)
            if compound:
                return compound
            return {"product_type": product}
    return None


def _try_split_compound_entity(entity: str) -> dict[str, Any] | None:
    """Split a compound entity like "苹果手机" into brand + product_type.

    Checks if any known brand or synonym appears as a prefix/suffix of the
    entity, and the remainder is a recognizable product type.
    """
    entity_lower = entity.strip().lower()
    if len(entity_lower) < 3:
        return None

    # Collect all known brand/synonym strings (lowercased)
    candidates: list[tuple[str, str]] = []  # (match_key_lower, catalog_brand)
    for brand_lower in get_known_brands():
        if len(brand_lower) >= 2:
            candidates.append((brand_lower, get_brand_case(brand_lower)))
    for syn_key, syn_value in BRAND_SYNONYMS.items():
        if len(syn_key) >= 2:
            candidates.append((syn_key, syn_value))
    candidates.sort(key=lambda x: len(x[0]), reverse=True)

    for match_key, catalog_brand in candidates:
        if len(match_key) >= len(entity_lower):
            continue
        if entity_lower.startswith(match_key):
            remainder = entity[len(match_key) :].strip()
            if len(remainder) >= 1:
                normalized = normalize_product_type(remainder)
                if normalized:
                    return {"brand_prefer": [catalog_brand], "product_type": normalized}
        if entity_lower.endswith(match_key):
            remainder = entity[: len(entity) - len(match_key)].strip()
            if len(remainder) >= 1:
                normalized = normalize_product_type(remainder)
                if normalized:
                    return {"brand_prefer": [catalog_brand], "product_type": normalized}
    return None


# ── Deterministic product_type extraction ────────────────────────────


def extract_product_type_hint(message: str) -> str | None:
    """Deterministic product_type extraction from user message.

    Walks all PRODUCT_TYPE_ALIASES to find if any alias is a substring of the
    user's message. Also checks PRODUCT_TYPE_HIERARCHY for parent→child matching.

    Returns the canonical product_type string, or None.
    Used as a safety net when the LLM fails to extract product_type.
    """
    text = message.strip()
    # Direct alias match: known alias appears in user message
    for canonical, aliases in PRODUCT_TYPE_ALIASES.items():
        for alias in aliases:
            if len(alias) >= 2 and alias in text:
                return canonical
    # Hierarchy parent match: user mentioned a parent term like "裤子"
    for parent, children in PRODUCT_TYPE_HIERARCHY.items():
        if parent in text:
            return parent  # parent will be expanded to children by product_type_aliases
        for child in children:
            if child in text:
                return child
    return None


# ── Compare intent deterministic rules ────────────────────────────────

_COMPARE_MARKERS = (
    "对比",
    "比较",
    "比一下",
    "pk",
    "PK",
    "vs",
    "VS",
    "哪个好",
    "哪个更好",
    "哪个更",
    "哪款好",
    "哪款更",
    "怎么选",
    "选哪个",
    "选哪款",
)

# Patterns for extracting multiple ordinal references: "第一个和第二个"
_ORDINAL_PATTERN = re.compile(r"第\s*(\d+)\s*(?:个|款|件|项)")
_CN_ORDINAL_MAP = {
    "一": 0,
    "二": 1,
    "三": 2,
    "四": 3,
    "五": 4,
    "两": 1,  # "前两个"
}
_CN_ORDINAL_PATTERN = re.compile(r"第([一二三四五])")


def is_compare_phrase(message: str) -> bool:
    """Deterministic check: does this message look like a compare request?"""
    text = message.strip().lower()
    return any(marker.lower() in text for marker in _COMPARE_MARKERS)


def resolve_compare_targets(message: str, previous_product_ids: list[str]) -> list[str]:
    """Resolve ordinal references in a compare message to actual product IDs.

    Handles patterns like:
    - "第一个和第二个" -> [ids[0], ids[1]]
    - "前三款" -> ids[:3]
    - "对比1和3" -> [ids[0], ids[2]]

    Returns the resolved product IDs, or empty list if resolution fails.
    """
    if not previous_product_ids:
        return []

    indices: list[int] = []

    # Arabic numerals: "第1个和第3个" or "1和3"
    for match in _ORDINAL_PATTERN.finditer(message):
        idx = max(0, int(match.group(1)) - 1)
        if idx not in indices:
            indices.append(idx)

    # Chinese ordinals: "第一个和第三个"
    for match in _CN_ORDINAL_PATTERN.finditer(message):
        idx = _CN_ORDINAL_MAP.get(match.group(1))
        if idx is not None and idx not in indices:
            indices.append(idx)

    # "前N个" pattern
    top_n_match = re.search(r"前\s*(\d+|[一二三四五])\s*(?:个|款|件)", message)
    if top_n_match and not indices:
        raw = top_n_match.group(1)
        n = _CN_ORDINAL_MAP.get(raw, 0) + 1 if not raw.isdigit() else int(raw)
        indices = list(range(min(n, len(previous_product_ids))))

    if not indices:
        return []

    resolved = []
    for idx in indices:
        if 0 <= idx < len(previous_product_ids):
            resolved.append(previous_product_ids[idx])

    return resolved


def resolve_compare_ids_mixed(
    compare_product_ids: list,
    message: str,
    previous_product_ids: list[str],
) -> list[str]:
    """Resolve compare_product_ids (mixed format) to actual product IDs.

    Priority:
    1. Integer indices from LLM (e.g., [1, 2] → [ids[0], ids[1]])
    2. String product IDs (pass-through)
    3. Regex fallback on message (if above fail)

    Args:
        compare_product_ids: Mixed list of ints (indices) and/or strings (IDs or text)
        message: Original user message (for regex fallback)
        previous_product_ids: Previously recommended product IDs

    Returns:
        List of resolved product IDs
    """
    if not previous_product_ids:
        return []

    resolved: list[str] = []
    has_unresolved_text = False

    for item in compare_product_ids:
        if isinstance(item, int):
            # LLM output index (1-based) → convert to 0-based
            idx = item - 1
            if 0 <= idx < len(previous_product_ids):
                pid = previous_product_ids[idx]
                if pid not in resolved:
                    resolved.append(pid)
        elif isinstance(item, str):
            # Check if it looks like a product ID (starts with "p_")
            if item.startswith("p_") and item in previous_product_ids:
                if item not in resolved:
                    resolved.append(item)
            else:
                # Text reference like "第一个" — needs regex fallback
                has_unresolved_text = True

    # If we have enough resolved IDs, return them
    if len(resolved) >= 2:
        return resolved[:4]  # Cap at 4 products

    # Fallback: regex parse the message
    if has_unresolved_text or not resolved:
        regex_resolved = resolve_compare_targets(message, previous_product_ids)
        if len(regex_resolved) >= 2:
            return regex_resolved[:4]

    return resolved
