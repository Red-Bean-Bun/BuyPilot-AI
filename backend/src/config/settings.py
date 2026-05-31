"""Central configuration for backend services.

Business code must depend on this module instead of calling os.getenv directly.
"""

from __future__ import annotations

import os
import sys
from functools import lru_cache
from pathlib import Path
from typing import Any

import yaml


BACKEND_DIR = Path(__file__).resolve().parents[2]
PROJECT_DIR = BACKEND_DIR.parent
BASE_DIR = BACKEND_DIR


def _load_env_file(path: Path) -> None:
    """Load a simple KEY=VALUE .env file without overriding process env."""
    if not path.exists():
        return
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        if key and key not in os.environ:
            os.environ[key] = value


_load_env_file(PROJECT_DIR / ".env")


class Settings:
    def __init__(self) -> None:
        self.app_name = os.getenv("APP_NAME", "buypilot-api")
        self.debug = os.getenv("DEBUG", "0") == "1"
        self.database_url = _resolve_database_url(os.getenv("DATABASE_URL"))
        self.strict_runtime = os.getenv("STRICT_RUNTIME", "0") == "1"
        self.auto_seed_on_startup = os.getenv("AUTO_SEED_ON_STARTUP", "0") == "1"
        self.auto_seed_strict_embeddings = os.getenv("AUTO_SEED_STRICT_EMBEDDINGS", "0") == "1"
        self.dataset_dir = _resolve_path(
            os.getenv("ECOMMERCE_DATASET_DIR"),
            PROJECT_DIR / "data" / "raw" / "ecommerce_agent_dataset",
            PROJECT_DIR,
        )
        self.upload_dir = _resolve_path(os.getenv("UPLOAD_DIR"), BACKEND_DIR / "uploads", BACKEND_DIR)
        self.llm_profiles_path = BACKEND_DIR / "src" / "config" / "llm_profiles.yaml"
        profiles_data = load_llm_profiles(self.llm_profiles_path)
        self.task_model_map = profiles_data.get("task_model_map", {})

    @property
    def llm_profiles(self) -> dict[str, Any]:
        return load_llm_profiles(self.llm_profiles_path)

    def env_value(self, name: str | None) -> str | None:
        if not name:
            return None
        return os.getenv(name)


@lru_cache(maxsize=1)
def load_llm_profiles(path: Path | None = None) -> dict[str, Any]:
    profile_path = path or (BACKEND_DIR / "src" / "config" / "llm_profiles.yaml")
    if not profile_path.exists():
        return {"profiles": {}}
    with profile_path.open(encoding="utf-8") as f:
        return yaml.safe_load(f) or {"profiles": {}}


_settings: Settings | None = None


def get_settings() -> Settings:
    global _settings
    if _settings is None:
        _settings = Settings()
    return _settings


def _resolve_path(raw_path: str | None, default: Path, base_dir: Path) -> Path:
    if not raw_path:
        return default
    path = Path(raw_path)
    if path.is_absolute():
        return path
    return base_dir / path


def _resolve_database_url(raw_url: str | None) -> str:
    if not raw_url:
        raise SystemExit(
            "DATABASE_URL is required. PostgreSQL + pgvector is the only supported database.\n"
            "Example: DATABASE_URL=postgresql+psycopg://buypilot:buypilot@localhost:5432/buypilot\n"
            "Or use: docker-compose -f deploy/docker-compose.yml up"
        )
    # Test escape hatches: :memory: and /tmp/ paths under pytest
    if raw_url == "sqlite:///:memory:":
        return raw_url
    if raw_url.startswith("sqlite:///") and not raw_url.startswith("sqlite:////"):
        raw_path = raw_url[len("sqlite:///"):]
        db_path = Path(raw_path)
        if not db_path.is_absolute():
            resolved = f"sqlite:///{BACKEND_DIR / db_path}"
        else:
            resolved = raw_url
        # Allow SQLite under /tmp/ when running inside pytest (test isolation)
        if "/tmp/" in resolved and "pytest" in sys.modules:
            return resolved
        raise SystemExit(
            f"SQLite is not supported for runtime. Use PostgreSQL + pgvector.\n"
            f"Got: {raw_url}"
        )
    return raw_url
