"""Runtime prompt template loading."""

from __future__ import annotations

import json
from functools import lru_cache
from pathlib import Path
from typing import Any

from src.config.settings import BACKEND_DIR


PROMPTS_DIR = BACKEND_DIR / "prompts"


class PromptTemplateMissing(RuntimeError):
    """Raised when a required runtime prompt template is absent."""


class PromptStore:
    def __init__(self, prompts_dir: Path = PROMPTS_DIR) -> None:
        self.prompts_dir = prompts_dir

    @lru_cache(maxsize=32)
    def load(self, name: str) -> str | None:
        path = self.prompts_dir / f"{name}.md"
        if not path.exists():
            return None
        return path.read_text(encoding="utf-8").strip()

    def render(self, name: str, variables: dict[str, Any] | None = None) -> str:
        template = self.load(name)
        if template is None:
            raise PromptTemplateMissing(f"Prompt template not found: {name}")
        return _render_template(template, variables or {})


def get_prompt_store() -> PromptStore:
    return PromptStore()


def _render_template(template: str, variables: dict[str, Any]) -> str:
    rendered = template
    for key, value in variables.items():
        rendered = rendered.replace("{" + key + "}", _stringify(value))
    return rendered


def _stringify(value: Any) -> str:
    if isinstance(value, str):
        return value
    return json.dumps(value, ensure_ascii=False, default=str)
