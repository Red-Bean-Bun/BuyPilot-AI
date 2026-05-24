"""Shared task-to-profile resolution helpers."""

from __future__ import annotations

from src.config.settings import get_settings


def task_profile_names(task: str) -> list[str]:
    mapping = get_settings().task_model_map[task]
    return [name for name in (mapping.get("primary"), mapping.get("fallback")) if name]
