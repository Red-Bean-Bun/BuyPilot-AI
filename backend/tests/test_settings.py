from src.config.settings import BACKEND_DIR, TASK_MODEL_MAP, get_settings
import src.config.settings as settings_module


def test_task_model_map_has_required_tasks():
    required = {
        "analyze_intent",
        "generate_criteria",
        "generate_recommendation",
        "analyze_image",
        "embedding",
        "rerank",
    }
    assert required.issubset(TASK_MODEL_MAP)
    for task in required:
        assert TASK_MODEL_MAP[task]["primary"]


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


def test_relative_sqlite_database_url_resolves_under_backend(monkeypatch):
    monkeypatch.setenv("DATABASE_URL", "sqlite:///./buypilot-dev.db")
    settings_module._settings = None

    settings = get_settings()

    assert settings.database_url == f"sqlite:///{BACKEND_DIR / 'buypilot-dev.db'}"


def test_startup_seed_flags(monkeypatch):
    monkeypatch.setenv("AUTO_SEED_ON_STARTUP", "1")
    monkeypatch.setenv("AUTO_SEED_STRICT_EMBEDDINGS", "1")
    settings_module._settings = None

    settings = get_settings()

    assert settings.auto_seed_on_startup is True
    assert settings.auto_seed_strict_embeddings is True
