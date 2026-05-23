"""Product knowledge package and semantic chunking helpers for ingestion."""

from __future__ import annotations

import re
from dataclasses import dataclass
from typing import Any

from src.config.domain_terms import (
    INGREDIENT_TERMS,
    POSITIVE_MARKERS,
    SCENARIO_TERMS,
    SKIN_TERMS,
    WARNING_MARKERS,
    extract_terms,
    has_negation_prefix,
)
from src.config.tuning import (
    ALIAS_MAX_COUNT,
    DEDUP_VALUE_MAX_CHARS,
    LEGACY_HEADER_MAX_CHARS,
    LEGACY_MAIN_CHUNK_MAX_CHARS,
    LEGACY_SUB_CHUNK_MAX_CHARS,
    MARKETING_CHUNK_MAX_CHARS,
    SEMANTIC_CHUNK_MAX_CHARS,
    SUMMARY_SENTENCE_MAX_CHARS,
)


@dataclass(frozen=True)
class SemanticChunk:
    chunk_text: str
    chunk_index: int
    metadata: dict[str, Any]


def clean_text(text: str, max_chars: int) -> str:
    return " ".join(str(text).split())[:max_chars]


def chunk_product_text(text: str) -> list[tuple[str, int]]:
    """Legacy fixed-window chunker kept for compatibility."""

    parts = [part.strip() for part in text.split("|") if part.strip()]
    if not parts:
        return []
    header = parts[0]
    features = parts[1:]
    main = clean_text(" | ".join([header] + features[:3]), LEGACY_MAIN_CHUNK_MAX_CHARS)
    chunks = [(main, 0)]
    for index, offset in enumerate(range(3, len(features), 2), start=1):
        chunk = clean_text(
            f"{header[:LEGACY_HEADER_MAX_CHARS]} | " + " | ".join(features[offset : offset + 2]),
            LEGACY_SUB_CHUNK_MAX_CHARS,
        )
        if chunk:
            chunks.append((chunk, index))
    return chunks


def build_product_knowledge_package(raw: dict[str, Any]) -> dict[str, Any]:
    """Build a deterministic guide-facing package from one official product JSON."""

    text = _raw_text(raw)
    category = str(raw.get("category") or "")
    sub_category = str(raw.get("sub_category") or "")
    brand = str(raw.get("brand") or "")
    title = str(raw.get("title") or "")
    positive_alias = _dedup(
        [
            title,
            brand,
            category,
            sub_category,
            *_category_aliases(category, sub_category),
            *_matching_terms(text, SCENARIO_TERMS),
            *_matching_terms(text, INGREDIENT_TERMS),
            *[f"{skin}适用" for skin in _skin_types(text)],
        ]
    )
    risk_terms = _risk_terms(raw)
    why_buy = _summary_sentences(raw, positive=True, limit=3)
    risk = _summary_sentences(raw, positive=False, limit=3)
    return {
        "basic": {
            "product_id": str(raw.get("product_id") or ""),
            "name": title,
            "category": category,
            "sub_category": sub_category,
            "brand": brand,
            "price": raw.get("base_price"),
        },
        "attributes": {
            "skin_type": _skin_types(text),
            "use_scenario": _matching_terms(text, SCENARIO_TERMS),
            "ingredient_terms": _matching_terms(text, INGREDIENT_TERMS),
            "risk_terms": risk_terms,
        },
        "retrieval_alias": {
            "positive_alias": positive_alias,
            "risk_terms": risk_terms,
        },
        "evidence_summary": {
            "why_buy": why_buy,
            "risk": risk,
            "compare_axis": _compare_axis(category, sub_category),
            "not_suitable_for": _not_suitable_for(risk),
        },
    }


def build_product_chunks(raw: dict[str, Any]) -> list[SemanticChunk]:
    """Split one product into typed chunks aligned to ecommerce guide use cases."""

    package = build_product_knowledge_package(raw)
    chunks: list[SemanticChunk] = []
    base_meta = {
        "source": "ecommerce_agent_dataset",
        "category": package["basic"]["category"],
        "sub_category": package["basic"]["sub_category"],
        "brand": package["basic"]["brand"],
        "price": package["basic"]["price"],
    }

    def add(chunk_type: str, text: str, retrieval_role: str, **metadata: Any) -> None:
        cleaned = clean_text(text, SEMANTIC_CHUNK_MAX_CHARS)
        if not cleaned:
            return
        chunks.append(
            SemanticChunk(
                chunk_text=cleaned,
                chunk_index=len(chunks),
                metadata={**base_meta, "chunk_type": chunk_type, "retrieval_role": retrieval_role, **metadata},
            )
        )

    add("profile", _profile_text(package), "primary", evidence_kind="profile")

    rag = raw.get("rag_knowledge") or {}
    for index, chunk in enumerate(
        _split_long_text(str(rag.get("marketing_description") or ""), max_chars=MARKETING_CHUNK_MAX_CHARS)
    ):
        add("marketing", chunk, "primary", evidence_kind="why_buy", section_index=index)

    for index, faq in enumerate(rag.get("official_faq") or []):
        question = str(faq.get("question") or "")
        answer = str(faq.get("answer") or "")
        role = "risk" if _has_warning_text(f"{question} {answer}") else "primary"
        add(
            "faq",
            f"Q: {question}\nA: {answer}",
            role,
            question=question,
            evidence_kind="risk" if role == "risk" else "faq",
            section_index=index,
        )

    for index, review in enumerate(rag.get("user_reviews") or []):
        rating = review.get("rating")
        chunk_type = "negative_review" if _is_negative_rating(rating) else "positive_review"
        role = "risk" if chunk_type == "negative_review" else "evidence"
        add(
            chunk_type,
            f"[{review.get('nickname', '匿名')} 评分:{rating or '?'}] {review.get('content', '')}",
            role,
            rating=rating,
            nickname=review.get("nickname", ""),
            evidence_kind="risk" if role == "risk" else "why_buy",
            section_index=index,
        )

    warning_text = " ".join(package["evidence_summary"]["risk"])
    if warning_text:
        add("warning", warning_text, "risk", evidence_kind="risk")

    compare_text = "；".join(package["evidence_summary"]["compare_axis"])
    if compare_text:
        add("compare", compare_text, "evidence", evidence_kind="compare")

    return chunks


def _profile_text(package: dict[str, Any]) -> str:
    basic = package["basic"]
    attributes = package["attributes"]
    aliases = package["retrieval_alias"]["positive_alias"]
    parts = [
        basic["name"],
        basic["brand"],
        basic["category"],
        basic["sub_category"],
        f"{basic['price']:g}元" if isinstance(basic["price"], int | float) else "",
        "适用肤质：" + "、".join(attributes["skin_type"]) if attributes["skin_type"] else "",
        "场景：" + "、".join(attributes["use_scenario"]) if attributes["use_scenario"] else "",
        "别名：" + "、".join(aliases[:ALIAS_MAX_COUNT]) if aliases else "",
    ]
    return " | ".join(part for part in parts if part)


def _raw_text(raw: dict[str, Any]) -> str:
    rag = raw.get("rag_knowledge") or {}
    parts = [
        raw.get("title", ""),
        raw.get("brand", ""),
        raw.get("category", ""),
        raw.get("sub_category", ""),
        rag.get("marketing_description", ""),
    ]
    for faq in rag.get("official_faq") or []:
        parts.append(faq.get("question", ""))
        parts.append(faq.get("answer", ""))
    for review in rag.get("user_reviews") or []:
        parts.append(review.get("content", ""))
    return " ".join(str(part) for part in parts if part)


def _category_aliases(category: str, sub_category: str) -> list[str]:
    aliases = {
        "洁面": ["洗面奶", "洁面乳", "日常清洁", "控油洁面"],
        "防晒": ["防晒霜", "防晒乳", "通勤防晒", "户外防晒"],
        "跑鞋": ["跑步鞋", "日常训练", "公路跑", "缓震跑鞋"],
        "耳机": ["蓝牙耳机", "降噪耳机", "通勤耳机"],
        "饮料": ["无糖饮料", "低糖饮品", "办公室饮料"],
    }
    result = [category, sub_category]
    for key, values in aliases.items():
        if key and key in sub_category:
            result.extend(values)
    return result


def _skin_types(text: str) -> list[str]:
    return _dedup(
        [value for token, value in SKIN_TERMS.items() if token in text and not has_negation_prefix(text, token)]
    )


def _matching_terms(text: str, terms: tuple[str, ...]) -> list[str]:
    return _dedup(extract_terms(text, terms))


def _risk_terms(raw: dict[str, Any]) -> list[str]:
    risk_text = " ".join(_summary_sentences(raw, positive=False, limit=8))
    return _dedup([term for term in (*INGREDIENT_TERMS, *WARNING_MARKERS) if term in risk_text])


def _summary_sentences(raw: dict[str, Any], positive: bool, limit: int) -> list[str]:
    rag = raw.get("rag_knowledge") or {}
    candidates: list[str] = []
    candidates.extend(_sentences(str(rag.get("marketing_description") or "")))
    for faq in rag.get("official_faq") or []:
        candidates.extend(_sentences(str(faq.get("answer") or "")))
    for review in rag.get("user_reviews") or []:
        rating = review.get("rating")
        if positive and _is_positive_rating(rating):
            candidates.extend(_sentences(str(review.get("content") or "")))
        if not positive and _is_negative_rating(rating):
            candidates.extend(_sentences(str(review.get("content") or "")))

    markers = POSITIVE_MARKERS if positive else WARNING_MARKERS
    selected = [sentence for sentence in candidates if any(marker in sentence for marker in markers)]
    if not selected and candidates:
        selected = candidates[:limit]
    return [clean_text(sentence, SUMMARY_SENTENCE_MAX_CHARS) for sentence in selected[:limit]]


def _split_long_text(text: str, max_chars: int) -> list[str]:
    sentences = _sentences(text)
    chunks: list[str] = []
    current = ""
    for sentence in sentences:
        next_text = f"{current}{sentence}" if not current else f"{current} {sentence}"
        if len(next_text) > max_chars and current:
            chunks.append(current)
            current = sentence
        else:
            current = next_text
    if current:
        chunks.append(current)
    return chunks or ([clean_text(text, max_chars)] if text else [])


def _sentences(text: str) -> list[str]:
    normalized = " ".join(str(text).split())
    if not normalized:
        return []
    parts = re.split(r"(?<=[。！？!?])", normalized)
    return [part.strip() for part in parts if part.strip()]


def _compare_axis(category: str, sub_category: str) -> list[str]:
    if category == "美妆护肤":
        return ["肤质匹配", "成分风险", "使用场景", "价格"]
    if category == "数码电子":
        return ["核心参数", "性能", "续航", "价格"]
    if category == "服饰运动":
        return ["运动场景", "材质/脚感", "尺码适配", "价格"]
    if category in {"食品饮料", "食品生活"}:
        return ["配料", "糖分/热量", "储存方式", "价格"]
    return [sub_category, "场景", "价格"]


def _not_suitable_for(risk_sentences: list[str]) -> list[str]:
    text = " ".join(risk_sentences)
    result: list[str] = []
    if "敏感肌" in text:
        result.append("敏感肌或屏障受损用户")
    if "酒精" in text:
        result.append("酒精敏感用户")
    if "孕妇" in text:
        result.append("孕妇")
    if "未成年人" in text:
        result.append("未成年人")
    return _dedup(result)


def _has_warning_text(text: str) -> bool:
    return any(marker in text for marker in WARNING_MARKERS)


def _is_positive_rating(rating: Any) -> bool:
    return isinstance(rating, int | float) and rating >= 4


def _is_negative_rating(rating: Any) -> bool:
    return isinstance(rating, int | float) and rating <= 2


def _dedup(values: list[str]) -> list[str]:
    result: list[str] = []
    for value in values:
        cleaned = clean_text(value, DEDUP_VALUE_MAX_CHARS)
        if cleaned and cleaned not in result:
            result.append(cleaned)
    return result
