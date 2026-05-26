"""Evaluation runner — orchestrates pipeline execution and metric computation."""

from __future__ import annotations

import logging
import statistics
import uuid
from dataclasses import dataclass, field
from typing import Any

logger = logging.getLogger(__name__)

from src.runtime.pipeline import chat_stream
from src.services.eval.llm_judge import (
    evaluate_answer_correctness,
    evaluate_constraint_satisfaction,
    evaluate_context_precision,
    evaluate_context_recall,
    evaluate_faithfulness,
    evaluate_hallucination_rate,
    evaluate_multi_turn_consistency,
    evaluate_ranking_reasonableness,
)
from src.services.eval.metrics import (
    compute_constraint_extraction_accuracy,
    compute_constraint_satisfaction,
    compute_criteria_coverage,
    compute_evidence_coverage,
    compute_intent_accuracy,
    compute_recall_at_k,
)
from src.services.eval.persistence import load_eval_samples, save_eval_run
from src.types.schemas import ChatStreamRequest
from src.types.sse_events import (
    CriteriaCardEvent,
    FinalDecisionEvent,
    ProductCardEvent,
)


@dataclass
class PipelineCapture:
    """Outputs collected from a single chat_stream invocation."""

    intent_type: str = ""
    extracted_constraints: dict[str, Any] = field(default_factory=dict)
    criteria_chips: list[str] = field(default_factory=list)
    criteria_constraints: dict[str, Any] = field(default_factory=dict)
    text_full: str = ""
    products: list[dict[str, Any]] = field(default_factory=list)
    product_ids: list[str] = field(default_factory=list)
    evidence_count: int = 0
    winner_product_id: str = ""
    error: str = ""


async def run_eval(
    strategy_tag: str = "baseline",
    run_name: str | None = None,
    prompt_version: str | None = None,
    git_commit: str | None = None,
) -> dict[str, Any]:
    """Execute a full evaluation run over all samples.

    Returns the aggregate_metrics dict for API consumption.
    """
    samples = await load_eval_samples()
    if not samples:
        raise ValueError("No eval samples found. Seed the database first.")

    sample_results: list[dict[str, Any]] = []
    session_id = f"eval_{uuid.uuid4().hex[:8]}"

    for sample in samples:
        sample_results.append(await _evaluate_sample(sample, session_id))

    aggregate = _aggregate(sample_results)
    run_name = run_name or f"eval_{strategy_tag}_{uuid.uuid4().hex[:6]}"
    await save_eval_run(
        run_name=run_name,
        strategy_tag=strategy_tag,
        aggregate_metrics=aggregate,
        samples_detail=sample_results,
        sample_count=len(samples),
        prompt_version=prompt_version,
        git_commit=git_commit,
    )
    return aggregate


async def _evaluate_sample(sample: Any, session_id: str) -> dict[str, Any]:
    capture = await _run_pipeline(sample.question, sample.context, session_id)
    gt = sample.ground_truth
    contexts = _build_context_texts(capture)
    metrics = {
        **_deterministic_metrics(capture, gt),
        **await _llm_metrics(sample, capture, contexts, gt),
    }
    metrics["overall_score"] = _compute_overall(metrics)
    return {
        "sample_id": sample.id,
        "question": sample.question,
        "scenario_type": sample.scenario_type,
        "difficulty": sample.difficulty,
        "metrics": metrics,
        "details": {
            "actual_intent": capture.intent_type,
            "expected_intent": gt.get("intent_type", ""),
            "retrieved_product_ids": capture.product_ids,
            "error": capture.error,
        },
    }


def _deterministic_metrics(capture: PipelineCapture, gt: dict[str, Any]) -> dict[str, float]:
    metrics = {
        "intent_accuracy": compute_intent_accuracy(capture.intent_type, gt.get("intent_type", "recommend")),
        "constraint_extraction_accuracy": compute_constraint_extraction_accuracy(
            capture.extracted_constraints, gt.get("constraints", {})
        ),
        "criteria_coverage": compute_criteria_coverage(capture.criteria_chips, gt.get("expected_criteria_chips", [])),
        "recall_at_5": compute_recall_at_k(capture.product_ids, gt.get("relevant_product_ids", []), k=5),
        "recall_at_10": compute_recall_at_k(capture.product_ids, gt.get("relevant_product_ids", []), k=10),
        "evidence_coverage": compute_evidence_coverage(capture.evidence_count, len(capture.products)),
    }
    metrics["constraint_satisfaction"] = _deterministic_constraint_satisfaction(capture, gt)
    return metrics


def _deterministic_constraint_satisfaction(capture: PipelineCapture, gt: dict[str, Any]) -> float:
    if not capture.products or not gt.get("constraints"):
        return 1.0
    sat_scores = [compute_constraint_satisfaction(product, gt["constraints"]) for product in capture.products]
    return statistics.mean(sat_scores) if sat_scores else 0.0


async def _llm_metrics(
    sample: Any, capture: PipelineCapture, contexts: list[str], gt: dict[str, Any]
) -> dict[str, float]:
    metrics = (
        await _text_llm_metrics(sample.question, capture.text_full, contexts)
        if capture.text_full and contexts
        else _default_text_llm_metrics()
    )
    metrics["llm_constraint_satisfaction"] = await _llm_constraint_satisfaction(capture, gt)
    metrics["multi_turn_consistency"] = await _multi_turn_consistency(sample, capture, contexts)
    metrics["ranking_reasonableness"] = await _ranking_reasonableness(sample.question, capture, gt)
    return metrics


def _default_text_llm_metrics() -> dict[str, float]:
    return {
        "faithfulness": 1.0,
        "context_precision": 0.0,
        "context_recall": 0.0,
        "answer_correctness": 0.0,
        "hallucination_rate": 0.0,
    }


async def _text_llm_metrics(question: str, answer: str, contexts: list[str]) -> dict[str, float]:
    faith = await evaluate_faithfulness(answer, contexts)
    cp = await evaluate_context_precision(question, contexts)
    cr = await evaluate_context_recall(answer, contexts)
    corr = await evaluate_answer_correctness(question, answer, contexts)
    hallu = await evaluate_hallucination_rate(answer, contexts)
    return {
        "faithfulness": faith["score"],
        "context_precision": cp["score"],
        "context_recall": cr["score"],
        "answer_correctness": corr["score"],
        "hallucination_rate": hallu["hallucination_rate"],
    }


async def _llm_constraint_satisfaction(capture: PipelineCapture, gt: dict[str, Any]) -> float:
    if not capture.products or not gt.get("constraints"):
        return 1.0
    result = await evaluate_constraint_satisfaction(capture.text_full, capture.products, gt["constraints"])
    return result["score"]


async def _multi_turn_consistency(sample: Any, capture: PipelineCapture, contexts: list[str]) -> float:
    if not sample.context or not capture.text_full:
        return 1.0
    result = await evaluate_multi_turn_consistency(
        sample.context.get("conversation_history", []),
        capture.text_full,
        contexts,
    )
    return result["score"]


async def _ranking_reasonableness(question: str, capture: PipelineCapture, gt: dict[str, Any]) -> float:
    if len(capture.products) <= 1 or not gt.get("constraints"):
        return 1.0
    result = await evaluate_ranking_reasonableness(question, capture.products, gt["constraints"])
    return result["score"]


async def _run_pipeline(
    question: str,
    context: dict[str, Any] | None,
    session_id: str,
) -> PipelineCapture:
    """Run the chat pipeline and capture all relevant outputs."""
    capture = PipelineCapture()

    history = []
    if context and context.get("conversation_history"):
        history = context["conversation_history"]

    body = ChatStreamRequest(message=question, session_id=session_id, history=history)

    # Capture intent directly from the intent stage before the full pipeline.
    from src.runtime.stages.intent import run_intent

    intent_result = await run_intent(session_id, body)
    capture.intent_type = intent_result.intent
    capture.extracted_constraints = intent_result.extracted_constraints

    try:
        async for event in chat_stream(session_id, body):
            _collect_event(event, capture)
    except Exception as exc:
        capture.error = str(exc)

    # Merge criteria_constraints into extracted_constraints if richer
    if capture.criteria_constraints:
        for k, v in capture.criteria_constraints.items():
            if v is not None and k not in capture.extracted_constraints:
                capture.extracted_constraints[k] = v

    return capture


def _collect_event(event: Any, capture: PipelineCapture) -> None:
    """Extract relevant data from each SSE event."""
    event_type = getattr(event, "event", "")

    if event_type == "criteria_card" and isinstance(event, CriteriaCardEvent):
        capture.criteria_chips = event.criteria.chips
        capture.criteria_constraints = event.criteria.constraints.model_dump()
        capture.extracted_constraints = capture.criteria_constraints

    elif event_type == "text_delta":
        capture.text_full += getattr(event, "delta", "")

    elif event_type == "product_card" and isinstance(event, ProductCardEvent):
        product = event.product
        product_dict = {
            "product_id": product.product_id,
            "name": product.name,
            "price": product.price,
            "category": product.category,
        }
        capture.products.append(product_dict)
        capture.product_ids.append(product.product_id)
        if event.evidence:
            capture.evidence_count += 1

    elif event_type == "final_decision" and isinstance(event, FinalDecisionEvent):
        capture.winner_product_id = event.winner_product_id

    # Intent is captured from the pipeline's extracted constraints via the
    # intent stage output saved to the stage context.
    if event_type == "thinking":
        pass  # skip thinking events
    elif event_type == "clarification":
        if not capture.intent_type:
            capture.intent_type = "clarify"
    elif event_type == "product_card":
        if not capture.intent_type:
            capture.intent_type = "recommend"


def _build_context_texts(capture: PipelineCapture) -> list[str]:
    """Build context chunks from captured products for LLM judge evaluation."""
    texts: list[str] = []
    for p in capture.products:
        texts.append(f"商品: {p.get('name', '')} | 价格: {p.get('price', 'N/A')}元 | 品类: {p.get('category', '')}")
    return texts


def _compute_overall(metrics: dict[str, float]) -> float:
    """Weighted overall evaluation score."""
    weights = {
        "faithfulness": 0.25,
        "recall_at_10": 0.20,
        "constraint_satisfaction": 0.20,
        "context_precision": 0.15,
        "intent_accuracy": 0.10,
    }
    anti_hallu = 1.0 - metrics.get("hallucination_rate", 0)
    score = 0.0
    for key, weight in weights.items():
        score += metrics.get(key, 0) * weight
    score += anti_hallu * 0.10
    return round(score, 4)


def _aggregate(
    sample_results: list[dict[str, Any]],
) -> dict[str, Any]:
    """Compute mean and std for each metric across all samples."""
    all_keys: set[str] = set()
    for s in sample_results:
        all_keys.update(s["metrics"].keys())

    aggregate: dict[str, Any] = {}
    for key in sorted(all_keys):
        values = [s["metrics"].get(key, 0) for s in sample_results]
        aggregate[key] = {
            "mean": round(statistics.mean(values), 4),
            "std": round(statistics.stdev(values), 4) if len(values) > 1 else 0.0,
            "min": round(min(values), 4),
            "max": round(max(values), 4),
        }

    overall_values = [s["metrics"].get("overall_score", 0) for s in sample_results]
    aggregate["overall_score"] = round(statistics.mean(overall_values), 4)
    return aggregate
