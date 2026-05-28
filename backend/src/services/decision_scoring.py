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
    max_score = max(abs(scored[0].final_score), 0.001)
    ratio = gap / max_score

    if ratio >= DECISION_LOW_CONFIDENCE_RATIO:
        return "selected", "medium"
    if user_signal_count == 0:
        return "needs_more_signal", None
    return "selected", "low"


# ── Internal helpers ───────────────────────────────────────────────────────


def _compute_user_signal_scores(feedback: dict[str, Any]) -> dict[str, float]:
    """Convert feedback actions into per-product signal scores."""
    scores: dict[str, float] = {}
    # Per-product negative signals from avoid_products
    for pid in feedback.get("avoid_products", []):
        scores[pid] = scores.get(pid, 0.0) + SIGNAL_NOT_INTERESTED
    return scores


def _default_retrieval_score(rank: int, total: int = 5) -> float:
    """Fallback retrieval score when no explicit score is available."""
    return max(0.0, 1.0 - (rank / max(total, 1)) * 0.8)


def _compute_criteria_match(product: ProductPayload, criteria: CriteriaPayload) -> float:
    """Compute how well a product matches the active criteria."""
    score = 1.0
    constraints = criteria.constraints
    if constraints.budget_max is not None and product.price is not None:
        if product.price <= constraints.budget_max:
            score += 0.1
        else:
            score -= 0.3
    if constraints.skin_type and product.skin_type_match:
        if constraints.skin_type in product.skin_type_match:
            score += 0.15
    if constraints.use_scenario and product.use_scenario:
        if constraints.use_scenario in product.use_scenario:
            score += 0.1
    # Penalise if product contains avoided ingredients
    avoided = set(constraints.ingredient_avoid or [])
    product_ingredients = set(product.ingredient_avoid or [])
    if avoided & product_ingredients:
        score -= 0.2 * len(avoided & product_ingredients)
    return max(0.0, min(score, 2.0))


def _compute_evidence_score(evidence: list[EvidencePayload]) -> float:
    """Score evidence quality by coverage and snippet richness."""
    if not evidence:
        return 0.0
    total_chars = sum(len(e.snippet) for e in evidence if e.snippet)
    return min(1.0, len(evidence) * 0.15 + total_chars * 0.001)


def _compute_risk_penalty(
    product: ProductPayload,
    criteria: CriteriaPayload,
    feedback: dict[str, Any],
) -> float:
    """Penalty for hard-constraint conflicts and user dislikes."""
    penalty = 0.0
    pid = product.product_id
    if pid in set(feedback.get("avoid_products", [])):
        penalty += 1.0
    if pid in set(feedback.get("disliked_products", [])):
        penalty += 1.5
    # Budget hard violation
    if criteria.constraints.budget_min is not None and product.price is not None:
        if product.price < criteria.constraints.budget_min:
            penalty += 0.5
    return penalty
