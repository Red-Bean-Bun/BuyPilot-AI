"""Central configuration for backend services.

Business code must depend on this module instead of calling os.getenv directly.
"""

from __future__ import annotations

import os
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


TASK_MODEL_MAP: dict[str, dict[str, str | None]] = {
    "analyze_intent": {"primary": "doubao_intent", "fallback": "qwen_turbo"},
    "generate_criteria": {"primary": "qwen_plus", "fallback": "doubao_generation"},
    "generate_recommendation": {"primary": "qwen_plus", "fallback": "doubao_generation"},
    "generate_decision": {"primary": "qwen_plus", "fallback": "doubao_generation"},
    "analyze_image": {"primary": "qwen_vl_plus", "fallback": None},
    "embedding": {"primary": "qwen_embedding", "fallback": "doubao_embedding"},
    "rerank": {"primary": "gte_rerank", "fallback": None},
}


class Settings:
    def __init__(self) -> None:
        self.app_name = os.getenv("APP_NAME", "buypilot-api")
        self.debug = os.getenv("DEBUG", "0") == "1"
        self.database_url = _resolve_database_url(os.getenv("DATABASE_URL"))
        self.strict_runtime = os.getenv("STRICT_RUNTIME", "0") == "1"
        self.auto_seed_on_startup = os.getenv("AUTO_SEED_ON_STARTUP", "0") == "1"
        self.auto_seed_strict_embeddings = os.getenv("AUTO_SEED_STRICT_EMBEDDINGS", "0") == "1"
        self.dataset_dir = Path(
            os.getenv("ECOMMERCE_DATASET_DIR", PROJECT_DIR / "data" / "raw" / "ecommerce_agent_dataset")
        )
        self.upload_dir = Path(os.getenv("UPLOAD_DIR", BACKEND_DIR / "uploads"))
        self.llm_profiles_path = BACKEND_DIR / "src" / "config" / "llm_profiles.yaml"
        self.task_model_map = TASK_MODEL_MAP

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


def _resolve_database_url(raw_url: str | None) -> str:
    if not raw_url:
        return f"sqlite:///{BACKEND_DIR / 'buypilot-dev.db'}"
    if raw_url == "sqlite:///:memory:":
        return raw_url

    sqlite_prefix = "sqlite:///"
    if raw_url.startswith(sqlite_prefix) and not raw_url.startswith("sqlite:////"):
        raw_path = raw_url[len(sqlite_prefix) :]
        db_path = Path(raw_path)
        if not db_path.is_absolute():
            return f"sqlite:///{BACKEND_DIR / db_path}"
    return raw_url
