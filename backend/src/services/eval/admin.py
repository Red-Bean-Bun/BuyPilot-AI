"""Admin-facing eval use cases."""

from __future__ import annotations

from src.config.settings import PROJECT_DIR
from src.repos.eval_runs import get_by_id, list_all
from src.repos.eval_samples import list_all as list_all_samples, seed_from_json


def list_eval_runs(limit: int = 20) -> list[dict]:
    runs = list_all(limit=limit)
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


def get_eval_run(run_id: str) -> dict:
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


def list_eval_samples() -> list[dict]:
    samples = list_all_samples()
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


def seed_eval_samples() -> dict[str, int | str]:
    json_path = PROJECT_DIR / "data" / "eval" / "eval_samples.json"
    count = seed_from_json(str(json_path))
    return {"status": "ok", "count": count}
