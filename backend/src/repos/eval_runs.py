"""Eval run persistence — save and query evaluation run records."""

from __future__ import annotations

from typing import Any

from sqlmodel import Session, select

from src.repos.database import create_db_and_tables, get_engine
from src.repos.models import EvalRun


def save_run(
    run_name: str,
    strategy_tag: str,
    aggregate_metrics: dict[str, Any],
    samples_detail: list[dict[str, Any]],
    sample_count: int,
    prompt_version: str | None = None,
    git_commit: str | None = None,
) -> EvalRun:
    """Persist a completed eval run to the database."""
    create_db_and_tables()
    run = EvalRun(
        run_name=run_name,
        strategy_tag=strategy_tag,
        prompt_version=prompt_version,
        git_commit=git_commit,
        metrics=aggregate_metrics,
        samples_detail=samples_detail,
        sample_count=sample_count,
    )
    with Session(get_engine()) as session:
        session.add(run)
        session.commit()
        session.refresh(run)
        return run


def list_all(limit: int = 50) -> list[EvalRun]:
    """Return recent eval runs, newest first."""
    create_db_and_tables()
    with Session(get_engine()) as session:
        return list(session.exec(select(EvalRun).order_by(EvalRun.created_at.desc()).limit(limit)).all())


def get_by_id(run_id: str) -> EvalRun | None:
    """Get a single eval run by its id."""
    create_db_and_tables()
    with Session(get_engine()) as session:
        return session.get(EvalRun, run_id)
