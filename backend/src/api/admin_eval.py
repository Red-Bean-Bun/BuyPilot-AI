"""Admin API for evaluation — list runs, view details, trigger new runs."""

from __future__ import annotations

from fastapi import APIRouter, BackgroundTasks, Depends

from src.api.admin_auth import require_admin_key
from src.services.audit import record_audit_event
from src.services.eval.admin import (
    get_eval_run,
    list_eval_runs,
    list_eval_samples,
    seed_eval_samples,
    trigger_eval_run,
)
from src.types.schemas import EvalRunRequest

admin_eval_router = APIRouter(
    tags=["admin_eval"],
    prefix="/admin/eval",
    dependencies=[Depends(require_admin_key)],
)


@admin_eval_router.get("/runs")
async def list_runs(limit: int = 20):
    """Return recent eval runs, newest first."""
    return await list_eval_runs(limit=limit)


@admin_eval_router.get("/runs/{run_id}")
async def get_run(run_id: str):
    """Return full detail for a single eval run, including per-sample metrics."""
    return await get_eval_run(run_id)


@admin_eval_router.post("/runs")
async def create_run(
    request: EvalRunRequest,
    background_tasks: BackgroundTasks,
):
    """Trigger a new evaluation run. Returns immediately with run_id."""
    result = await trigger_eval_run(
        strategy_tag=request.strategy_tag,
        run_name=request.run_name,
        prompt_version=request.prompt_version,
    )
    await record_audit_event(
        "eval.run_triggered",
        resource_type="eval_run",
        side_effect=True,
        metadata=result,
    )
    return result


@admin_eval_router.get("/samples")
async def list_samples():
    """Return all eval samples with their ground truth."""
    return await list_eval_samples()


@admin_eval_router.post("/samples/seed")
async def seed_samples():
    """Seed eval_samples table from data/eval/eval_samples.json."""
    result = await seed_eval_samples()
    await record_audit_event(
        "eval.samples_seeded",
        resource_type="eval_samples",
        side_effect=True,
        metadata=result,
    )
    return result
