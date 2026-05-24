"""Eval sample persistence — load and query evaluation test cases."""

from __future__ import annotations

import json
from pathlib import Path

from sqlmodel import Session, select

from src.repos.database import ensure_eval_schema, get_engine
from src.repos.models import EvalSample


def seed_from_json(json_path: str | Path) -> int:
    """Insert eval samples from a JSON file. Returns count of samples seeded."""
    ensure_eval_schema()
    path = Path(json_path)
    if not path.exists():
        raise FileNotFoundError(f"Eval samples file not found: {path}")
    samples_data = json.loads(path.read_text(encoding="utf-8"))
    engine = get_engine()
    count = 0
    with Session(engine) as session:
        for sample_data in samples_data:
            existing = session.get(EvalSample, sample_data["id"])
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
        session.commit()
    return count


def list_all() -> list[EvalSample]:
    """Return all eval samples ordered by id."""
    ensure_eval_schema()
    with Session(get_engine()) as session:
        return list(session.exec(select(EvalSample).order_by(EvalSample.id)).all())


def get_by_id(sample_id: str) -> EvalSample | None:
    """Get a single eval sample by its id."""
    ensure_eval_schema()
    with Session(get_engine()) as session:
        return session.get(EvalSample, sample_id)
