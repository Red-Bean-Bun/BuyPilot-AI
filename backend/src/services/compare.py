"""Multi-product comparison service.

Builds structured comparison results from product fields and RAG evidence.
Scoring is deterministic — LLM only narrates, never scores.
"""

from __future__ import annotations

import logging
from dataclasses import dataclass, field
from typing import Any, Literal

from src.repos.products import get_product
from src.services.chunking import COMPARE_AXES
from src.services.evidence import get_evidence
from src.types.sse_events import (
    CompareAxisPayload,
    CompareAxisValuePayload,
    CompareRiskNotePayload,
    CriteriaPayload,
    EvidencePayload,
    ProductPayload,
)

logger = logging.getLogger(__name__)

# ── Default axes for unknown categories ───────────────────────────────
_DEFAULT_AXES = ("功能", "场景", "价格")

# ── Minimum score gap to declare a winner ─────────────────────────────
_MIN_WINNER_GAP = 5.0

# ── Axis weight: price gets slightly less to avoid over-dominating ────
_PRICE_WEIGHT = 0.8
_AXIS_WEIGHT = 1.0

CompareMode = Literal["exploratory", "decision"]
CompareConfidence = Literal["high", "medium", "low"]


@dataclass(frozen=True)
class CompareResult:
    """Structured comparison output, consumed by the compare handler."""

    compare_id: str
    source_deck_id: str | None
    mode: CompareMode
    products: list[ProductPayload]
    axes: list[CompareAxisPayload]
    winner_product_id: str | None
    winner_reason: str | None
    tradeoffs: list[str]
    risk_notes: list[CompareRiskNotePayload]
    confidence: CompareConfidence | None
    focus: str | None = None


async def build_comparison(
    product_ids: list[str],
    category: str | None = None,
    *,
    source_deck_id: str | None = None,
    criteria: CriteriaPayload | None = None,
    focus: str | None = None,
    has_final_decision: bool = False,
    compare_id: str = "",
) -> CompareResult:
    """Build a full comparison result from product IDs.

    This is the main entry point called by the compare handler.
    """
    products = [p for pid in product_ids if (p := get_product(pid)) is not None]
    if len(products) < 2:
        raise ValueError("Comparison requires at least 2 valid products")

    # Resolve category from criteria or first product
    resolved_category = category or (criteria.category if criteria else None) or products[0].category

    # Fetch evidence for each product
    evidence_by_product: dict[str, list[EvidencePayload]] = {}
    for product in products:
        evidence_by_product[product.product_id] = await get_evidence(product)

    # Build axes
    axis_names = _get_axis_names(resolved_category)
    axes = [
        _score_axis(name, products, evidence_by_product, criteria)
        for name in axis_names
    ]

    # Determine mode
    mode: CompareMode = "decision" if has_final_decision else "exploratory"

    # Determine winner with degradation
    winner_id, winner_reason, confidence = _determine_winner(axes, products)

    # Generate tradeoffs and risk notes
    tradeoffs = _generate_tradeoffs(axes, products)
    risk_notes = _generate_risk_notes(products, evidence_by_product)

    return CompareResult(
        compare_id=compare_id,
        source_deck_id=source_deck_id,
        mode=mode,
        products=products,
        axes=axes,
        winner_product_id=winner_id,
        winner_reason=winner_reason,
        tradeoffs=tradeoffs,
        risk_notes=risk_notes,
        confidence=confidence,
        focus=focus,
    )


# ── Axis resolution ──────────────────────────────────────────────────


def _get_axis_names(category: str | None) -> tuple[str, ...]:
    """Get comparison axis names for a category."""
    if category and category in COMPARE_AXES:
        return COMPARE_AXES[category]
    return _DEFAULT_AXES


# ── Per-axis scoring ──────────────────────────────────────────────────


def _score_axis(
    axis_name: str,
    products: list[ProductPayload],
    evidence_by_product: dict[str, list[EvidencePayload]],
    criteria: CriteriaPayload | None,
) -> CompareAxisPayload:
    """Score each product on one axis. Returns axis with per-product values."""
    values = [
        _score_product_on_axis(axis_name, product, evidence_by_product.get(product.product_id, []), criteria)
        for product in products
    ]
    return CompareAxisPayload(name=axis_name, values=values)


def _score_product_on_axis(
    axis_name: str,
    product: ProductPayload,
    evidence: list[EvidencePayload],
    criteria: CriteriaPayload | None,
) -> CompareAxisValuePayload:
    """Score one product on one axis using deterministic rules."""

    # Route to category-specific scoring by axis name
    scorer = _AXIS_SCORERS.get(axis_name)
    if scorer is not None:
        return scorer(product, evidence, criteria)

    # Fallback: evidence-based scoring for unknown axes
    return _score_from_evidence(axis_name, product, evidence)


def _score_skin_type(
    product: ProductPayload,
    evidence: list[EvidencePayload],
    criteria: CriteriaPayload | None,
) -> CompareAxisValuePayload:
    """Score skin type match (美妆护肤: 肤质匹配)."""
    target_skin = criteria.constraints.skin_type if criteria else None
    skin_types = product.skin_type_match or []

    if target_skin:
        if target_skin in skin_types:
            score = 95.0
            detail = f"明确适用{target_skin}肌肤"
        elif "通用" in skin_types or "所有肤质" in skin_types:
            score = 75.0
            detail = "通用型，适合所有肤质"
        elif skin_types:
            score = 45.0
            detail = f"标注适用{'/'.join(skin_types)}，与目标不完全匹配"
        else:
            score = 50.0
            detail = "未标注具体适用肤质"
    else:
        if skin_types:
            score = 60.0
            detail = f"标注适用{'/'.join(skin_types)}"
        else:
            score = 50.0
            detail = None

    label = "/".join(skin_types) if skin_types else "未标注"
    return CompareAxisValuePayload(
        product_id=product.product_id,
        score=score,
        label=label,
        detail=detail,
    )


def _score_ingredient_risk(
    product: ProductPayload,
    evidence: list[EvidencePayload],
    criteria: CriteriaPayload | None,
) -> CompareAxisValuePayload:
    """Score ingredient risk (美妆护肤: 成分风险)."""
    avoid_list = criteria.constraints.ingredient_avoid if criteria else []
    conflicts = [ing for ing in avoid_list if ing in (product.ingredient_tags or [])]

    if not avoid_list:
        # No avoidance criteria — check evidence for risk signals
        risk_evidence = [e for e in evidence if "风险" in e.snippet or "刺激" in e.snippet or "过敏" in e.snippet]
        if risk_evidence:
            score = 55.0
            detail = "部分用户反馈存在刺激风险"
        else:
            score = 80.0
            detail = "未见明显成分风险反馈"
    elif len(conflicts) == 0:
        score = 90.0
        detail = "不含需规避成分"
    elif len(conflicts) == 1:
        score = 50.0
        detail = f"含{conflicts[0]}，需注意"
    else:
        score = 25.0
        detail = f"含{'/'.join(conflicts)}等需规避成分"

    label = "安全" if score >= 80 else ("需注意" if score >= 50 else "有风险")
    return CompareAxisValuePayload(
        product_id=product.product_id,
        score=score,
        label=label,
        detail=detail,
    )


def _score_use_scenario(
    product: ProductPayload,
    evidence: list[EvidencePayload],
    criteria: CriteriaPayload | None,
) -> CompareAxisValuePayload:
    """Score use scenario match (通用: 使用场景/运动场景)."""
    target_scenario = criteria.constraints.use_scenario if criteria else None
    product_scenario = product.use_scenario

    if target_scenario and product_scenario:
        if target_scenario in product_scenario or product_scenario in target_scenario:
            score = 90.0
            detail = f"适合{product_scenario}场景"
        else:
            score = 50.0
            detail = f"标注{product_scenario}，与目标{target_scenario}不完全匹配"
    elif product_scenario:
        score = 65.0
        detail = f"适合{product_scenario}场景"
    else:
        score = 50.0
        detail = "未标注具体使用场景"

    return CompareAxisValuePayload(
        product_id=product.product_id,
        score=score,
        label=product_scenario or "未标注",
        detail=detail,
    )


def _score_price(
    product: ProductPayload,
    evidence: list[EvidencePayload],
    criteria: CriteriaPayload | None,
) -> CompareAxisValuePayload:
    """Price scoring is done relatively — caller must post-process.

    Returns raw price as score placeholder; _normalize_price_scores handles relative scoring.
    """
    price = product.price
    if price is None:
        return CompareAxisValuePayload(
            product_id=product.product_id,
            score=50.0,
            label="价格未知",
            detail="暂无价格信息",
        )

    budget_max = criteria.constraints.budget_max if criteria else None
    within_budget = budget_max is None or price <= budget_max
    detail = f"¥{price:.0f}" + ("" if within_budget else f"，超出预算¥{budget_max:.0f}")

    return CompareAxisValuePayload(
        product_id=product.product_id,
        score=price,  # raw price, normalized later
        label=f"¥{price:.0f}",
        detail=detail,
    )


def _score_from_evidence(
    axis_name: str,
    product: ProductPayload,
    evidence: list[EvidencePayload],
) -> CompareAxisValuePayload:
    """Fallback scoring from evidence chunks for axes without dedicated scorers."""
    # Look for compare-type evidence mentioning this axis
    compare_evidence = [
        e for e in evidence
        if axis_name in e.snippet
    ]
    why_buy_evidence = [
        e for e in evidence
        if "好评" in e.snippet or "推荐" in e.snippet or "满意" in e.snippet
    ]

    if compare_evidence:
        # Positive signal in compare evidence
        positive_signals = ["强", "好", "优", "出色", "领先", "高"]
        negative_signals = ["弱", "差", "不足", "一般", "较低"]
        text = " ".join(e.snippet for e in compare_evidence)
        pos_count = sum(1 for s in positive_signals if s in text)
        neg_count = sum(1 for s in negative_signals if s in text)

        if pos_count > neg_count:
            score = 75.0 + min(pos_count * 5, 15)
        elif neg_count > pos_count:
            score = 45.0 - min(neg_count * 5, 15)
        else:
            score = 60.0
        detail = compare_evidence[0].snippet[:80]
    elif why_buy_evidence:
        score = 65.0
        detail = "用户评价较正面"
    else:
        score = None
        detail = "暂无数据"

    return CompareAxisValuePayload(
        product_id=product.product_id,
        score=score,
        label=None,
        detail=detail,
        evidence_ids=[e.source_id for e in compare_evidence if e.source_id],
    )


# ── Axis scorer dispatch ─────────────────────────────────────────────

_AXIS_SCORERS: dict[str, Any] = {
    # 美妆护肤
    "肤质匹配": _score_skin_type,
    "成分风险": _score_ingredient_risk,
    "使用场景": _score_use_scenario,
    "价格": _score_price,
    # 数码电子
    "核心参数": lambda p, e, c: _score_from_evidence("核心参数", p, e),
    "性能": lambda p, e, c: _score_from_evidence("性能", p, e),
    "续航": lambda p, e, c: _score_from_evidence("续航", p, e),
    # 服饰运动
    "运动场景": _score_use_scenario,
    "材质/脚感": lambda p, e, c: _score_from_evidence("材质", p, e),
    "尺码适配": lambda p, e, c: _score_from_evidence("尺码", p, e),
    # 食品生活
    "配料": lambda p, e, c: _score_from_evidence("配料", p, e),
    "糖分/热量": lambda p, e, c: _score_from_evidence("糖", p, e),
    "储存方式": lambda p, e, c: _score_from_evidence("储存", p, e),
    # 默认
    "功能": lambda p, e, c: _score_from_evidence("功能", p, e),
    "场景": _score_use_scenario,
}


def _normalize_price_scores(axis: CompareAxisPayload) -> CompareAxisPayload:
    """Convert raw price values to 0-100 relative scores within the comparison set.

    Cheapest product gets 100, most expensive gets 40, linear interpolation.
    """
    prices = [(v.product_id, v.score) for v in axis.values if v.score is not None]
    if len(prices) < 2:
        return axis

    min_price = min(p for _, p in prices)
    max_price = max(p for _, p in prices)

    new_values = []
    for v in axis.values:
        if v.score is None:
            new_values.append(v)
            continue
        if max_price == min_price:
            normalized = 100.0  # all same price
        else:
            # Linear: min_price → 100, max_price → 40
            ratio = (v.score - min_price) / (max_price - min_price)
            normalized = 100.0 - ratio * 60.0
        new_values.append(CompareAxisValuePayload(
            product_id=v.product_id,
            score=round(normalized, 1),
            label=v.label,
            detail=v.detail,
            evidence_ids=v.evidence_ids,
        ))

    return CompareAxisPayload(name=axis.name, values=new_values)


# ── Winner determination with degradation ─────────────────────────────


def _determine_winner(
    axes: list[CompareAxisPayload],
    products: list[ProductPayload],
) -> tuple[str | None, str | None, CompareConfidence | None]:
    """Determine the winner with degradation logic.

    Returns (winner_product_id, winner_reason, confidence).
    - No winner when score gap < 5 or > 50% axes lack data.
    """
    if not axes or not products:
        return None, None, None

    # Normalize price axis first
    processed_axes = []
    for axis in axes:
        if axis.name == "价格":
            processed_axes.append(_normalize_price_scores(axis))
        else:
            processed_axes.append(axis)

    # Count axes with data per product
    total_axes = len(processed_axes)
    if total_axes == 0:
        return None, None, None

    # Check if too many axes lack data
    missing_counts: dict[str, int] = {p.product_id: 0 for p in products}
    for axis in processed_axes:
        for v in axis.values:
            if v.score is None:
                missing_counts[v.product_id] = missing_counts.get(v.product_id, 0) + 1

    # If any product has > 50% missing axes, degrade
    for pid, count in missing_counts.items():
        if count > total_axes * 0.5:
            return None, "部分维度数据不足，无法给出明确结论", None

    # Compute weighted totals
    totals: dict[str, float] = {p.product_id: 0.0 for p in products}
    weight_counts: dict[str, int] = {p.product_id: 0 for p in products}

    for axis in processed_axes:
        weight = _PRICE_WEIGHT if axis.name == "价格" else _AXIS_WEIGHT
        for v in axis.values:
            if v.score is not None:
                totals[v.product_id] += v.score * weight
                weight_counts[v.product_id] += 1

    # Normalize totals by number of scored axes
    normalized: dict[str, float] = {}
    for pid in totals:
        wc = weight_counts[pid]
        normalized[pid] = totals[pid] / wc if wc > 0 else 0.0

    # Sort by total score descending
    ranked = sorted(normalized.items(), key=lambda x: x[1], reverse=True)
    top_id, top_score = ranked[0]
    second_id, second_score = ranked[1] if len(ranked) > 1 else (None, 0.0)

    gap = top_score - second_score

    if gap < _MIN_WINNER_GAP:
        return None, "两款商品综合评分接近，建议根据个人偏好选择", None

    # Determine confidence from gap ratio
    gap_ratio = gap / max(top_score, 1.0)
    if gap_ratio >= 0.15:
        confidence: CompareConfidence = "high"
    elif gap_ratio >= 0.08:
        confidence = "medium"
    else:
        confidence = "low"

    # Find product name for reason
    top_product = next((p for p in products if p.product_id == top_id), None)
    top_name = top_product.name if top_product else top_id

    # Build winner reason from tradeoff axes
    lead_axes = _find_leading_axes(processed_axes, top_id)
    if lead_axes:
        winner_reason = f"{top_name}在{'、'.join(lead_axes)}方面更优"
    else:
        winner_reason = f"{top_name}综合评分更高"

    return top_id, winner_reason, confidence


def _find_leading_axes(axes: list[CompareAxisPayload], product_id: str) -> list[str]:
    """Find axes where a product leads (highest score)."""
    leading = []
    for axis in axes:
        scores = [(v.product_id, v.score) for v in axis.values if v.score is not None]
        if not scores:
            continue
        best_id = max(scores, key=lambda x: x[1])[0]
        if best_id == product_id:
            leading.append(axis.name)
    return leading[:3]  # max 3


# ── Tradeoffs ─────────────────────────────────────────────────────────


def _generate_tradeoffs(
    axes: list[CompareAxisPayload],
    products: list[ProductPayload],
) -> list[str]:
    """Generate tradeoff statements."""
    if len(products) < 2:
        return []

    name_map = {p.product_id: p.name for p in products}
    tradeoffs: list[str] = []

    # Normalize price axis for tradeoff analysis
    processed_axes = []
    for axis in axes:
        if axis.name == "价格":
            processed_axes.append(_normalize_price_scores(axis))
        else:
            processed_axes.append(axis)

    for axis in processed_axes:
        scores = [(v.product_id, v.score) for v in axis.values if v.score is not None]
        if len(scores) < 2:
            continue

        best_id, best_score = max(scores, key=lambda x: x[1])
        worst_id, worst_score = min(scores, key=lambda x: x[1])

        if best_score - worst_score >= _MIN_WINNER_GAP:
            best_name = name_map.get(best_id, best_id)
            tradeoffs.append(f"{best_name}在{axis.name}方面更有优势")

    return tradeoffs[:4]  # max 4 tradeoffs


# ── Risk notes ────────────────────────────────────────────────────────


def _generate_risk_notes(
    products: list[ProductPayload],
    evidence_by_product: dict[str, list[EvidencePayload]],
) -> list[CompareRiskNotePayload]:
    """Extract risk notes from risk-type evidence chunks."""
    risk_notes: list[CompareRiskNotePayload] = []

    for product in products:
        evidence = evidence_by_product.get(product.product_id, [])
        risk_evidence = [
            e for e in evidence
            if any(kw in e.snippet for kw in ("风险", "注意", "不适合", "踩坑", "差评", "退货"))
        ]
        if risk_evidence:
            # Take the first risk evidence snippet, truncated
            note = risk_evidence[0].snippet[:100]
            risk_notes.append(CompareRiskNotePayload(
                product_id=product.product_id,
                note=note,
            ))

    return risk_notes
