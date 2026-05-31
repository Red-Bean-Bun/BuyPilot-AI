"""Decision scoring algorithm for SwipeDeck convergence (PRD 06).

Computes a weighted final_score for each candidate product based on:
- retrieval relevance
- criteria match
- user behaviour signals (swipes, views, cart adds)
- evidence quality
- risk penalty

LLM is only responsible for explaining the result, not choosing the winner.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

from src.config.tuning import DECISION_LOW_CONFIDENCE_RATIO, DECISION_MIN_USER_SIGNALS_FOR_HIGH_CONFIDENCE
from src.types.sse_events import CriteriaPayload, EvidencePayload, ProductPayload

# ── PRD 06 weights ────────────────────────────────────────────────────────
WEIGHT_RETRIEVAL = 0.35
WEIGHT_CRITERIA_MATCH = 0.25
WEIGHT_USER_SIGNAL = 0.25
WEIGHT_EVIDENCE = 0.10
WEIGHT_RISK = 0.05  # applied as penalty (subtracted)

# ── User behaviour signal scores ───────────────────────────────────────────
SIGNAL_ADD_TO_CART = 1.5
SIGNAL_LIKE = 1.0
SIGNAL_VIEW_DETAIL = 0.35
SIGNAL_OPEN_EVIDENCE = 0.25
SIGNAL_NOT_INTERESTED = -1.2
SIGNAL_EXPLICIT_DISLIKE = -1.5

# ── Criteria match scoring ─────────────────────────────────────────────
BASE_CRITERIA_SCORE = 1.0
BUDGET_UNDER_MAX_BONUS = 0.1
OVER_BUDGET_PENALTY = 0.3
SKIN_TYPE_MATCH_BONUS = 0.15
SCENARIO_MATCH_BONUS = 0.1
INGREDIENT_AVOID_PENALTY_PER_HIT = 0.2
MAX_CRITERIA_SCORE = 2.0

# ── Default retrieval score decay ──────────────────────────────────────
RETRIEVAL_DECAY_FACTOR = 0.8
DEFAULT_RETRIEVAL_TOTAL = 5

# ── Evidence scoring ───────────────────────────────────────────────────
EVIDENCE_PER_PIECE_WEIGHT = 0.15
EVIDENCE_PER_CHAR_WEIGHT = 0.001
MAX_EVIDENCE_SCORE = 1.0

# ── Risk penalty ───────────────────────────────────────────────────────
RISK_AVOID_PRODUCT_PENALTY = 1.0
RISK_DISLIKED_PRODUCT_PENALTY = 1.5
RISK_BUDGET_MIN_VIOLATION = 0.5

# ── Confidence epsilon ─────────────────────────────────────────────────
CONFIDENCE_EPSILON = 0.001


@dataclass
class ScoredCandidate:
    product_id: str
    retrieval_score: float = 0.0
    criteria_match_score: float = 0.0
    user_signal_score: float = 0.0
    evidence_score: float = 0.0
    risk_penalty: float = 0.0
    final_score: float = 0.0
    score_breakdown: dict[str, Any] = field(default_factory=dict)


def score_candidates(
    products: list[ProductPayload],
    criteria: CriteriaPayload,
    feedback: dict[str, Any] | None = None,
    retrieval_scores: dict[str, float] | None = None,
    evidence_by_product: dict[str, list[EvidencePayload]] | None = None,
) -> list[ScoredCandidate]:
    """Score and rank candidates using the PRD 06 weighted formula.

    Returns a list sorted by final_score descending.
    """
    feedback = feedback or {}
    retrieval_scores = retrieval_scores or {}
    evidence_by_product = evidence_by_product or {}

    user_signals = _compute_user_signal_scores(feedback)
    scored: list[ScoredCandidate] = []

    for rank, product in enumerate(products):
        pid = product.product_id

        retrieval = retrieval_scores.get(pid, _default_retrieval_score(rank))
        criteria_match = _compute_criteria_match(product, criteria)
        user_signal = user_signals.get(pid, 0.0)
        evidence = _compute_evidence_score(evidence_by_product.get(pid, []))
        risk = _compute_risk_penalty(product, criteria, feedback)

        final = (
            retrieval * WEIGHT_RETRIEVAL
            + criteria_match * WEIGHT_CRITERIA_MATCH
            + user_signal * WEIGHT_USER_SIGNAL
            + evidence * WEIGHT_EVIDENCE
            - risk * WEIGHT_RISK
        )

        scored.append(
            ScoredCandidate(
                product_id=pid,
                retrieval_score=retrieval,
                criteria_match_score=criteria_match,
                user_signal_score=user_signal,
                evidence_score=evidence,
                risk_penalty=risk,
                final_score=round(final, 4),
                score_breakdown={
                    "retrieval": retrieval,
                    "criteria_match": criteria_match,
                    "user_signal": user_signal,
                    "evidence": evidence,
                    "risk_penalty": risk,
                    "final_score": round(final, 4),
                    "rank": rank + 1,
                },
            )
        )

    scored.sort(key=lambda s: s.final_score, reverse=True)
    return scored


def decision_confidence(
    scored: list[ScoredCandidate],
    user_signal_count: int = 0,
) -> tuple[str, str | None]:
    """Determine decision_status and confidence from scored candidates.

    Returns (decision_status, confidence).
    """
    if not scored:
        return "no_match", None

    if user_signal_count >= DECISION_MIN_USER_SIGNALS_FOR_HIGH_CONFIDENCE:
        return "selected", "high"

    if len(scored) == 1:
        return "selected", "medium"

    gap = scored[0].final_score - scored[1].final_score if len(scored) > 1 else scored[0].final_score
    max_score = max(abs(scored[0].final_score), CONFIDENCE_EPSILON)
    ratio = gap / max_score

    if ratio >= DECISION_LOW_CONFIDENCE_RATIO:
        return "selected", "medium"
    if user_signal_count == 0:
        return "needs_more_signal", None
    return "selected", "low"


# ── Internal helpers ───────────────────────────────────────────────────────


def _compute_user_signal_scores(feedback: dict[str, Any]) -> dict[str, float]:
    """Convert feedback actions into per-product signal scores.

    Processes both negative signals (avoid_products) and positive signals
    (liked_products, add_to_cart_products, viewed_products) per PRD 06.
    """
    scores: dict[str, float] = {}
    # Per-product negative signals from avoid_products
    for pid in feedback.get("avoid_products", []):
        scores[pid] = scores.get(pid, 0.0) + SIGNAL_NOT_INTERESTED
    # Per-product positive signals from liked_products (like/right_swipe)
    for pid in feedback.get("liked_products", []):
        scores[pid] = scores.get(pid, 0.0) + SIGNAL_LIKE
    # Per-product positive signals from add_to_cart_products
    for pid in feedback.get("add_to_cart_products", []):
        scores[pid] = scores.get(pid, 0.0) + SIGNAL_ADD_TO_CART
    # Per-product positive signals from viewed_products (view_detail/open_evidence)
    for pid in feedback.get("viewed_products", []):
        scores[pid] = scores.get(pid, 0.0) + SIGNAL_VIEW_DETAIL
    return scores


def _default_retrieval_score(rank: int, total: int = DEFAULT_RETRIEVAL_TOTAL) -> float:
    """Fallback retrieval score when no explicit score is available."""
    return max(0.0, 1.0 - (rank / max(total, 1)) * RETRIEVAL_DECAY_FACTOR)


def _compute_criteria_match(product: ProductPayload, criteria: CriteriaPayload) -> float:
    """Compute how well a product matches the active criteria."""
    score = BASE_CRITERIA_SCORE
    constraints = criteria.constraints
    if constraints.budget_max is not None and product.price is not None:
        if product.price <= constraints.budget_max:
            score += BUDGET_UNDER_MAX_BONUS
        else:
            score -= OVER_BUDGET_PENALTY
    if constraints.skin_type and product.skin_type_match:
        if constraints.skin_type in product.skin_type_match:
            score += SKIN_TYPE_MATCH_BONUS
    if constraints.use_scenario and product.use_scenario:
        if constraints.use_scenario in product.use_scenario:
            score += SCENARIO_MATCH_BONUS
    # Penalise if product contains avoided ingredients
    avoided = set(constraints.ingredient_avoid or [])
    product_ingredients = set(product.ingredient_avoid or [])
    if avoided & product_ingredients:
        score -= INGREDIENT_AVOID_PENALTY_PER_HIT * len(avoided & product_ingredients)
    return max(0.0, min(score, MAX_CRITERIA_SCORE))


def _compute_evidence_score(evidence: list[EvidencePayload]) -> float:
    """Score evidence quality by coverage and snippet richness."""
    if not evidence:
        return 0.0
    total_chars = sum(len(e.snippet) for e in evidence if e.snippet)
    return min(MAX_EVIDENCE_SCORE, len(evidence) * EVIDENCE_PER_PIECE_WEIGHT + total_chars * EVIDENCE_PER_CHAR_WEIGHT)


def _compute_risk_penalty(
    product: ProductPayload,
    criteria: CriteriaPayload,
    feedback: dict[str, Any],
) -> float:
    """Penalty for hard-constraint conflicts and user dislikes."""
    penalty = 0.0
    pid = product.product_id
    if pid in set(feedback.get("avoid_products", [])):
        penalty += RISK_AVOID_PRODUCT_PENALTY
    if pid in set(feedback.get("disliked_products", [])):
        penalty += RISK_DISLIKED_PRODUCT_PENALTY
    # Budget hard violation
    if criteria.constraints.budget_min is not None and product.price is not None:
        if product.price < criteria.constraints.budget_min:
            penalty += RISK_BUDGET_MIN_VIOLATION
    return penalty
