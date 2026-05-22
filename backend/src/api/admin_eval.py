"""Admin API for evaluation — list runs, view details, trigger new runs."""

from __future__ import annotations

from fastapi import APIRouter

from src.repos.eval_runs import get_by_id, list_all
from src.repos.eval_samples import list_all as list_all_samples, seed_from_json

admin_eval_router = APIRouter(tags=["admin_eval"], prefix="/admin/eval")


@admin_eval_router.get("/runs")
async def list_runs(limit: int = 20):
    """Return recent eval runs, newest first."""
    runs = list_all(limit=limit)
    return [
        {
            "id": r.id,
            "run_name": r.run_name,
            "strategy_tag": r.strategy_tag,
            "prompt_version": r.prompt_version,
            "git_commit": r.git_commit,
            "sample_count": r.sample_count,
            "overall_score": (r.metrics or {}).get("overall_score"),
            "created_at": r.created_at.isoformat(),
        }
        for r in runs
    ]


@admin_eval_router.get("/runs/{run_id}")
async def get_run(run_id: str):
    """Return full detail for a single eval run, including per-sample metrics."""
    run = get_by_id(run_id)
    if not run:
        return {"error": "not_found", "run_id": run_id}
    return {
        "id": run.id,
        "run_name": run.run_name,
        "strategy_tag": run.strategy_tag,
        "prompt_version": run.prompt_version,
        "git_commit": run.git_commit,
        "sample_count": run.sample_count,
        "created_at": run.created_at.isoformat(),
        "metrics": run.metrics,
        "samples_detail": run.samples_detail,
    }


@admin_eval_router.get("/samples")
async def list_samples():
    """Return all eval samples with their ground truth."""
    samples = list_all_samples()
    return [
        {
            "id": s.id,
            "question": s.question,
            "scenario_type": s.scenario_type,
            "difficulty": s.difficulty,
            "tags": s.tags,
            "ground_truth": s.ground_truth,
        }
        for s in samples
    ]


@admin_eval_router.post("/samples/seed")
async def seed_samples():
    """Seed eval_samples table from data/eval/eval_samples.json."""
    from pathlib import Path

    json_path = Path(__file__).resolve().parents[4] / "data" / "eval" / "eval_samples.json"
    count = seed_from_json(str(json_path))
    return {"status": "ok", "count": count}
