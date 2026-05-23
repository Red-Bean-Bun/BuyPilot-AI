"""Admin API for evaluation — list runs, view details, trigger new runs."""

from __future__ import annotations

from fastapi import APIRouter

from src.services.async_io import run_sync_io
from src.services.eval.admin import get_eval_run, list_eval_runs, list_eval_samples, seed_eval_samples

admin_eval_router = APIRouter(tags=["admin_eval"], prefix="/admin/eval")


@admin_eval_router.get("/runs")
async def list_runs(limit: int = 20):
    """Return recent eval runs, newest first."""
    return await run_sync_io(list_eval_runs, limit=limit)


@admin_eval_router.get("/runs/{run_id}")
async def get_run(run_id: str):
    """Return full detail for a single eval run, including per-sample metrics."""
    return await run_sync_io(get_eval_run, run_id)


@admin_eval_router.get("/samples")
async def list_samples():
    """Return all eval samples with their ground truth."""
    return await run_sync_io(list_eval_samples)


@admin_eval_router.post("/samples/seed")
async def seed_samples():
    """Seed eval_samples table from data/eval/eval_samples.json."""
    return await run_sync_io(seed_eval_samples)
