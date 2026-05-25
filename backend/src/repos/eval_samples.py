"""Eval sample persistence — load and query evaluation test cases."""

from __future__ import annotations

import json
from pathlib import Path

from sqlmodel import select
from sqlmodel.ext.asyncio.session import AsyncSession

from src.repos.database import ensure_eval_schema, get_async_engine
from src.repos.models import EvalSample


async def seed_from_json(json_path: str | Path) -> int:
    """Insert eval samples from a JSON file. Returns count of samples seeded."""
    await ensure_eval_schema()
    path = Path(json_path)
    if not path.exists():
        raise FileNotFoundError(f"Eval samples file not found: {path}")
    samples_data = json.loads(path.read_text(encoding="utf-8"))
    count = 0
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        for sample_data in samples_data:
            existing = await session.get(EvalSample, sample_data["id"])
            sample = EvalSample(
                id=sample_data["id"],
                question=sample_data["question"],
                image_path=sample_data.get("image_path"),
                context=sample_data.get("context"),
                scenario_type=sample_data.get("scenario_type"),
                difficulty=sample_data.get("difficulty"),
                ground_truth=sample_data.get("ground_truth", {}),
                tags=sample_data.get("tags", []),
            )
            if existing:
                for key, value in sample.model_dump().items():
                    setattr(existing, key, value)
                continue
            session.add(sample)
            count += 1
        await session.commit()
    return count


async def list_all() -> list[EvalSample]:
    """Return all eval samples ordered by id."""
    await ensure_eval_schema()
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        return list((await session.exec(select(EvalSample).order_by(EvalSample.id))).all())


async def get_by_id(sample_id: str) -> EvalSample | None:
    """Get a single eval sample by its id."""
    await ensure_eval_schema()
    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        return await session.get(EvalSample, sample_id)
