"""Eval sample persistence — load and query evaluation test cases."""

from __future__ import annotations

import json
from pathlib import Path

from sqlmodel import select
from sqlmodel.ext.asyncio.session import AsyncSession

from src.repos.database import ensure_eval_schema, get_async_engine
from src.repos.models import EvalSample
from src.types.sse_events import Constraints


GROUND_TRUTH_SCHEMA_VERSION = "2026-05-31"
_CURRENT_CONSTRAINT_KEYS = set(Constraints.model_fields) | {"category"}
_EVAL_ONLY_CONSTRAINT_KEYS = {"must_match_terms", "forbidden_terms"}
_ALLOWED_CONSTRAINT_KEYS = _CURRENT_CONSTRAINT_KEYS | _EVAL_ONLY_CONSTRAINT_KEYS


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
                ground_truth=normalize_ground_truth(sample_data.get("ground_truth", {})),
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


def normalize_ground_truth(ground_truth: dict) -> dict[str, Any]:
    """Migrate legacy eval constraint names into the current Constraints DSL."""

    if not isinstance(ground_truth, dict):
        return {"schema_version": GROUND_TRUTH_SCHEMA_VERSION, "constraints": {}}

    constraints = ground_truth.get("constraints")
    migrated_constraints = _normalize_constraints(constraints if isinstance(constraints, dict) else {})
    unknown = sorted(set(migrated_constraints) - _ALLOWED_CONSTRAINT_KEYS)
    if unknown:
        raise ValueError(f"Unsupported eval ground_truth constraints: {', '.join(unknown)}")

    return {
        **ground_truth,
        "schema_version": ground_truth.get("schema_version") or GROUND_TRUTH_SCHEMA_VERSION,
        "constraints": migrated_constraints,
    }


def _normalize_constraints(constraints: dict) -> dict[str, Any]:
    migrated: dict = {}
    for key, value in constraints.items():
        if key == "max_price":
            migrated["budget_max"] = value
        elif key == "min_price":
            migrated["budget_min"] = value
        elif key == "use_case":
            migrated["use_scenario"] = value
        elif key == "must_have_features":
            migrated["must_match_terms"] = value
        elif key == "forbidden_features":
            migrated["forbidden_terms"] = value
        elif key == "brands":
            migrated["brand_prefer"] = value
        else:
            migrated[key] = value
    return migrated
