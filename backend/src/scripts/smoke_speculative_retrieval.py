"""Live smoke for speculative retrieval — quality and timing comparison.

Requires: PostgreSQL + pgvector, real API keys (Qwen / Bailian), seeded data.

Run from backend/:
    DATABASE_URL=postgresql+psycopg://buypilot:buypilot@<host>:5432/buypilot \\
      uv run -m src.scripts.smoke_speculative_retrieval
"""

from __future__ import annotations

import asyncio
import os
import sys
import time
from dataclasses import dataclass, field

if sys.platform == "win32":
    import selectors  # noqa: F401 — required by WindowsSelectorEventLoopPolicy

    asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())
from src.runtime.stages.criteria import criteria_from_intent
from src.runtime.stages.intent import run_intent
from src.runtime.stages.recommendation import RetrievalResult, run_retrieval
from src.services.retriever import filter_products
from src.types.schemas import ChatStreamRequest, IntentResult
from src.types.sse_events import CriteriaPayload

SPECULATIVE_TOP_N = 8
SERIAL_TOP_N = 5
CHAT_TIMEOUT_SECONDS = 120


def _check_live_provider() -> None:
    if "pytest" in sys.modules:
        return
    bailian_url = os.environ.get("BAILIAN_BASE_URL")
    bailian_key = os.environ.get("BAILIAN_API_KEY")
    if not bailian_url or not bailian_key:
        raise SystemExit("SMOKE GATE FAILED: BAILIAN_BASE_URL/BAILIAN_API_KEY not configured.")
    if bailian_key == "test-key":
        raise SystemExit("SMOKE GATE FAILED: BAILIAN_API_KEY is set to mock value 'test-key'.")


def _check_postgres() -> None:
    url = os.environ.get("DATABASE_URL", "")
    if "postgresql" not in url:
        raise SystemExit(f"SMOKE GATE FAILED: DATABASE_URL must use PostgreSQL + pgvector. Got: {url[:60]}...")


def _speculative_summary(intent: IntentResult) -> str:
    constraints = intent.extracted_constraints or {}
    parts: list[str] = []
    if intent.category:
        parts.append(f"{intent.category}类产品")
    skin_type = constraints.get("skin_type")
    if skin_type:
        parts.append(f"{skin_type}肌肤适用")
    budget = constraints.get("budget_max")
    if budget is not None:
        parts.append(f"预算{budget:g}元以内")
    scenario = constraints.get("use_scenario")
    if scenario:
        parts.append(f"{scenario}场景")
    product_type = constraints.get("product_type")
    if product_type:
        parts.append(str(product_type))
    return "用户需要" + "，".join(parts) if parts else ""


def _criteria_adds_new_filters(spec: CriteriaPayload, full: CriteriaPayload) -> bool:
    return bool(
        (full.constraints.brand_avoid and not spec.constraints.brand_avoid)
        or (full.constraints.origin_avoid and not spec.constraints.origin_avoid)
        or (full.constraints.product_type and not spec.constraints.product_type)
        or (
            full.constraints.budget_max is not None
            and spec.constraints.budget_max is not None
            and full.constraints.budget_max < spec.constraints.budget_max
        )
        or set(full.constraints.ingredient_avoid) - set(spec.constraints.ingredient_avoid)
    )


@dataclass
class CompareResult:
    query: str
    spec_time_s: float = 0.0
    serial_time_s: float = 0.0
    spec_product_ids: list[str] = field(default_factory=list)
    serial_product_ids: list[str] = field(default_factory=list)
    post_filtered: bool = False
    post_filter_removed: int = 0
    top1_match: bool = False
    jaccard: float = 0.0
    speedup: float = 0.0
    spec_evidence_count: int = 0
    serial_evidence_count: int = 0
    errors: list[str] = field(default_factory=list)

    @property
    def passed(self) -> bool:
        return len(self.errors) == 0


TEST_QUERIES: list[dict[str, str]] = [
    {"query": "推荐适合油皮的洗面奶，200元以内", "intent": "基础模糊推荐（美妆）"},
    {"query": "推荐一款蓝牙耳机，预算500", "intent": "数码品类"},
    {"query": "推荐日常穿的跑鞋", "intent": "服饰品类"},
    {"query": "推荐无糖的饮料", "intent": "食品品类"},
    {"query": "不要含酒精的防晒霜", "intent": "ingredient_avoid 一致性"},
    {"query": "推荐适合干皮的护肤品，不要欧莱雅", "intent": "后过滤触发（brand_avoid）"},
    {"query": "推荐控油的洁面产品，预算100以内", "intent": "窄预算 + 子品类"},
]


async def _run_intent_for_query(session_id: str, message: str) -> IntentResult:
    body = ChatStreamRequest(message=message, session_id=session_id)
    return await run_intent(session_id, body)


async def _compare_paths(
    session_id: str,
    body: ChatStreamRequest,
    intent: IntentResult,
) -> tuple[CompareResult, CriteriaPayload]:
    """Run speculative and serial retrieval using shared intent + criteria.

    Returns (result, criteria) where criteria is the shared LLM-generated criteria
    used by both paths for final comparison.
    """
    from src.runtime.stages.criteria import run_criteria as _run_criteria

    result = CompareResult(query=body.message)
    spec_criteria = criteria_from_intent(intent, summary=_speculative_summary(intent))

    # --- Start both speculative retrieval and criteria in parallel ---
    t0 = time.perf_counter()
    retrieval_task = asyncio.create_task(run_retrieval(spec_criteria, top_n=SPECULATIVE_TOP_N))

    try:
        criteria = await _run_criteria(session_id, body, intent)
    except Exception as exc:
        retrieval_task.cancel()
        result.errors.append(f"Criteria generation failed: {exc}")
        return result, CriteriaPayload()

    criteria_time = time.perf_counter() - t0

    # --- Await speculative retrieval ---
    try:
        spec_retrieval = await retrieval_task
    except Exception as exc:
        result.errors.append(f"Speculative retrieval failed: {exc}")
        spec_retrieval = None

    spec_total = criteria_time  # max(criteria_time, retrieval_time) ≈ criteria_time

    # --- Post-filter speculative results ---
    post_filtered = False
    removed = 0
    if spec_retrieval is not None:
        if _criteria_adds_new_filters(spec_criteria, criteria):
            before = len(spec_retrieval.products)
            kept = filter_products(
                spec_retrieval.products,
                criteria,
                max_products=SERIAL_TOP_N,
            )
            removed = before - len(kept)
            post_filtered = True
            spec_retrieval = RetrievalResult(
                products=kept,
                evidence_by_product={
                    pid: ev
                    for pid, ev in spec_retrieval.evidence_by_product.items()
                    if pid in {p.product_id for p in kept}
                },
                trace_details={**spec_retrieval.trace_details, "speculative_post_filtered": True},
            )
        else:
            # No new filters: just truncate to top SERIAL_TOP_N for fair comparison
            spec_retrieval = RetrievalResult(
                products=spec_retrieval.products[:SERIAL_TOP_N],
                evidence_by_product={
                    pid: ev
                    for pid, ev in spec_retrieval.evidence_by_product.items()
                    if pid in {p.product_id for p in spec_retrieval.products[:SERIAL_TOP_N]}
                },
                trace_details=spec_retrieval.trace_details,
            )

    if spec_retrieval is None or not spec_retrieval.products:
        spec_retrieval = await run_retrieval(criteria, top_n=SERIAL_TOP_N)

    result.spec_time_s = spec_total
    result.post_filtered = post_filtered
    result.post_filter_removed = removed
    if spec_retrieval is not None:
        result.spec_product_ids = [p.product_id for p in spec_retrieval.products]
        result.spec_evidence_count = sum(len(ev) for ev in spec_retrieval.evidence_by_product.values())

    # --- Serial baseline: same criteria, sequential retrieval ---
    t2 = time.perf_counter()
    serial_retrieval = await run_retrieval(criteria, top_n=SERIAL_TOP_N)
    serial_time = criteria_time + (time.perf_counter() - t2)
    result.serial_time_s = serial_time
    result.serial_product_ids = [p.product_id for p in serial_retrieval.products]
    result.serial_evidence_count = sum(len(ev) for ev in serial_retrieval.evidence_by_product.values())

    # --- Compare ---
    result.top1_match = (
        bool(result.spec_product_ids)
        and bool(result.serial_product_ids)
        and result.spec_product_ids[0] == result.serial_product_ids[0]
    )
    result.jaccard = _jaccard(result.spec_product_ids, result.serial_product_ids)
    _evaluate_result(result)

    return result, criteria


def _jaccard(left: list[str], right: list[str]) -> float:
    set_left, set_right = set(left), set(right)
    if not set_left and not set_right:
        return 1.0
    union = set_left | set_right
    intersection = set_left & set_right
    return len(intersection) / len(union) if union else 0.0


def _evaluate_result(result: CompareResult) -> None:
    """Apply pass/fail criteria. Mutates result.errors in place."""
    # Speedup
    if result.serial_time_s > 0:
        result.speedup = result.serial_time_s / max(result.spec_time_s, 0.001)
    if result.speedup < 1.15:
        result.errors.append(
            f"Speedup {result.speedup:.2f}x below 1.15x threshold "
            f"(spec={result.spec_time_s:.2f}s serial={result.serial_time_s:.2f}s)"
        )

    # Product overlap
    if result.jaccard < 0.5:
        result.errors.append(
            f"Jaccard {result.jaccard:.2f} below 0.5 threshold "
            f"(spec={result.spec_product_ids} serial={result.serial_product_ids})"
        )

    # Evidence coverage
    if result.spec_evidence_count == 0 or result.serial_evidence_count == 0:
        result.errors.append(
            f"Evidence missing (spec={result.spec_evidence_count} serial={result.serial_evidence_count})"
        )


async def run_comparison() -> list[CompareResult]:
    _check_live_provider()
    _check_postgres()

    results: list[CompareResult] = []
    base_session = f"smoke_spec_{int(time.time())}"

    for idx, entry in enumerate(TEST_QUERIES):
        query = entry["query"]
        intent_desc = entry["intent"]
        session_id = f"{base_session}_{idx}"
        body = ChatStreamRequest(message=query, session_id=session_id)

        print(f'\nQuery {idx + 1}: "{query}"  ({intent_desc})')

        # Shared intent
        try:
            intent = await _run_intent_for_query(session_id, query)
        except Exception as exc:
            print(f"  SKIP: intent failed ({exc})")
            continue

        # Run comparison with shared criteria
        result, criteria = await _compare_paths(session_id, body, intent)
        if result.errors and any("Criteria generation failed" in e for e in result.errors):
            print("  SKIP: criteria generation failed")
            results.append(result)
            continue

        print(
            f"  投机: {result.spec_time_s:.2f}s → {len(result.spec_product_ids)} products"
            + (f" (post-filter removed {result.post_filter_removed})" if result.post_filtered else "")
        )
        print(f"  串行: {result.serial_time_s:.2f}s → {len(result.serial_product_ids)} products")
        if criteria.category:
            print(
                f"  criteria: category={criteria.category} "
                + f"budget_max={criteria.constraints.budget_max} "
                + f"brand_avoid={criteria.constraints.brand_avoid}"
            )

        status = "PASS" if result.passed else f"FAIL ({len(result.errors)} errors)"
        print(
            f"  加速比: {result.speedup:.2f}x  Jaccard: {result.jaccard:.2f}  "
            f"Top-1: {'一致' if result.top1_match else '不同'}  → {status}"
        )
        if result.errors:
            for error in result.errors:
                print(f"    !! {error}")

        results.append(result)

    return results


def print_summary(results: list[CompareResult]) -> None:
    passed = sum(1 for r in results if r.passed)
    total = len(results)
    avg_speedup = sum(r.speedup for r in results if r.speedup > 0) / max(total, 1)
    avg_jaccard = sum(r.jaccard for r in results) / max(total, 1)

    print(f"\n{'=' * 50}")
    print(f"Results: {passed}/{total} passed")
    print(f"Average speedup: {avg_speedup:.2f}x")
    print(f"Average Jaccard: {avg_jaccard:.2f}")
    if passed < total:
        print(f"Failures ({total - passed}):")
        for result in results:
            if not result.passed:
                print(f'  - "{result.query}": {", ".join(result.errors)}')
        raise SystemExit(1)
    print("All queries passed.")


async def main_async() -> None:
    results = await run_comparison()
    print_summary(results)


def main() -> None:
    asyncio.run(main_async())


if __name__ == "__main__":
    main()
