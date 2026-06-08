from src.config.settings import PROJECT_DIR, get_settings
import pytest
import src.config.settings as settings_module


def test_llm_profiles_load():
    profiles = get_settings().llm_profiles["profiles"]
    assert "qwen_plus" in profiles
    assert "doubao_intent" in profiles


def test_env_configures_bailian_profile():
    settings = get_settings()
    profiles = settings.llm_profiles["profiles"]
    qwen_profile = profiles["qwen_plus"]

    assert qwen_profile["base_url_env"] == "BAILIAN_BASE_URL"
    assert qwen_profile["api_key_env"] == "BAILIAN_API_KEY"
    assert settings.dataset_dir.name == "ecommerce_agent_dataset"


def test_runtime_rejects_non_postgresql_database_url(monkeypatch):
    monkeypatch.setenv("BAILIAN_API_KEY", "real-key-not-test")
    monkeypatch.setenv("DATABASE_URL", "mysql://localhost/test")
    settings_module._settings = None

    with pytest.raises(SystemExit, match="Only PostgreSQL is supported"):
        get_settings()


def test_relative_dataset_dir_resolves_under_project(monkeypatch):
    monkeypatch.setenv("ECOMMERCE_DATASET_DIR", "./data/raw/ecommerce_agent_dataset")
    settings_module._settings = None

    settings = get_settings()

    assert settings.dataset_dir == PROJECT_DIR / "data" / "raw" / "ecommerce_agent_dataset"


def test_strict_runtime_flag(monkeypatch):
    monkeypatch.setenv("STRICT_RUNTIME", "1")
    settings_module._settings = None

    settings = get_settings()

    assert settings.strict_runtime is True
    settings_module._settings = None


def test_non_llm_tasks_do_not_configure_provider_fallbacks():
    settings = get_settings()

    assert settings.task_model_map["embedding"]["fallback"] is None
    assert settings.task_model_map["rerank"]["fallback"] is None
    settings_module._settings = None
