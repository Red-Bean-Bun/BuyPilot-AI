"""Persistence facade for runtime evaluation orchestration."""

from __future__ import annotations

from typing import Any

from src.repos.eval_samples import list_all as list_all_samples
from src.repos.eval_runs import EvalRun, save_run


async def load_eval_samples():
    return await list_all_samples()


async def save_eval_run(
    *,
    run_name: str,
    strategy_tag: str,
    aggregate_metrics: dict[str, Any],
    samples_detail: list[dict[str, Any]],
    sample_count: int,
    prompt_version: str | None,
    git_commit: str | None,
) -> EvalRun:
    return await save_run(
        run_name=run_name,
        strategy_tag=strategy_tag,
        aggregate_metrics=aggregate_metrics,
        samples_detail=samples_detail,
        sample_count=sample_count,
        prompt_version=prompt_version,
        git_commit=git_commit,
    )
