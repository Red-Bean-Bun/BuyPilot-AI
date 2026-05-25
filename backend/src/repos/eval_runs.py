"""Eval run persistence — save and query evaluation run records."""

from __future__ import annotations

from typing import Any

from sqlmodel import select
from sqlmodel.ext.asyncio.session import AsyncSession

from src.repos.database import ensure_eval_schema, get_async_engine
from src.repos.models import EvalRun


async def save_run(
    run_name: str,
    strategy_tag: str,
    aggregate_metrics: dict[str, Any],
    samples_detail: list[dict[str, Any]],
    sample_count: int,
    prompt_version: str | None = None,
    git_commit: str | None = None,
) -> EvalRun:
    """Persist a completed eval run to the database."""
    await ensure_eval_schema()
    run = EvalRun(
        run_name=run_name,
        strategy_tag=strategy_tag,
        prompt_version=prompt_version,
        git_commit=git_commit,
        metrics=aggregate_metrics,
        samples_detail=samples_detail,
        sample_count=sample_count,
    )
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        session.add(run)
        await session.commit()
        await session.refresh(run)
        return run


async def list_all(limit: int = 50) -> list[EvalRun]:
    """Return recent eval runs, newest first."""
    await ensure_eval_schema()
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        return list((await session.exec(select(EvalRun).order_by(EvalRun.created_at.desc()).limit(limit))).all())


async def get_by_id(run_id: str) -> EvalRun | None:
    """Get a single eval run by its id."""
    await ensure_eval_schema()
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        return await session.get(EvalRun, run_id)
