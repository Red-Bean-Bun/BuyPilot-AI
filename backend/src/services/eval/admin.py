"""Admin-facing eval use cases."""

from __future__ import annotations

from typing import Any

from src.config.settings import PROJECT_DIR
from src.repos.eval_runs import get_by_id, list_all
from src.repos.eval_samples import list_all as list_all_samples, seed_from_json


async def list_eval_runs(limit: int = 20) -> list[dict]:
    runs = await list_all(limit=limit)
    return [
        {
            "id": run.id,
            "run_name": run.run_name,
            "strategy_tag": run.strategy_tag,
            "prompt_version": run.prompt_version,
            "git_commit": run.git_commit,
            "sample_count": run.sample_count,
            "overall_score": (run.metrics or {}).get("overall_score"),
            "created_at": run.created_at.isoformat(),
        }
        for run in runs
    ]


async def get_eval_run(run_id: str) -> dict[str, Any]:
    run = await get_by_id(run_id)
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


async def list_eval_samples() -> list[dict]:
    samples = await list_all_samples()
    return [
        {
            "id": sample.id,
            "question": sample.question,
            "scenario_type": sample.scenario_type,
            "difficulty": sample.difficulty,
            "tags": sample.tags,
            "ground_truth": sample.ground_truth,
        }
        for sample in samples
    ]


async def seed_eval_samples() -> dict[str, int | str]:
    json_path = PROJECT_DIR / "data" / "eval" / "eval_samples.json"
    count = await seed_from_json(str(json_path))
    return {"status": "ok", "count": count}


async def trigger_eval_run(
    strategy_tag: str = "baseline",
    run_name: str | None = None,
    prompt_version: str | None = None,
) -> dict[str, str]:
    """Trigger an evaluation run. Returns the run_id for tracking."""
    from src.runtime.eval_runner import run_eval

    result = await run_eval(
        strategy_tag=strategy_tag,
        run_name=run_name,
        prompt_version=prompt_version,
    )
    return {
        "status": "completed",
        "run_id": result.get("run_id", ""),
        "sample_count": str(result.get("sample_count", 0)),
    }
