import sqlite3

import pytest

import src.config.settings as settings_module
from src.repos import eval_runs, eval_samples


@pytest.mark.asyncio
async def test_eval_list_queries_upgrade_old_empty_tables(monkeypatch, tmp_path):
    db_path = tmp_path / "old_eval.db"
    with sqlite3.connect(db_path) as conn:
        conn.execute(
            """
            CREATE TABLE eval_runs (
                id VARCHAR PRIMARY KEY,
                run_name VARCHAR NOT NULL,
                strategy_tag VARCHAR,
                metrics JSON,
                sample_count INTEGER,
                created_at DATETIME NOT NULL
            )
            """
        )
        conn.execute(
            """
            CREATE TABLE eval_samples (
                id VARCHAR PRIMARY KEY,
                question VARCHAR NOT NULL,
                must_have JSON,
                preferred JSON,
                forbidden JSON,
                difficulty VARCHAR,
                scenario_type VARCHAR
            )
            """
        )

    monkeypatch.setenv("DATABASE_URL", f"sqlite:///{db_path}")
    settings_module._settings = None

    assert await eval_runs.list_all(limit=5) == []
    assert await eval_samples.list_all() == []

    with sqlite3.connect(db_path) as conn:
        run_columns = {row[1] for row in conn.execute("PRAGMA table_info(eval_runs)").fetchall()}
        sample_columns = {row[1] for row in conn.execute("PRAGMA table_info(eval_samples)").fetchall()}

    assert {"prompt_version", "git_commit", "samples_detail"} <= run_columns
    assert {"image_path", "context", "ground_truth", "tags", "created_at"} <= sample_columns
    assert {"must_have", "preferred", "forbidden"}.isdisjoint(sample_columns)

    settings_module._settings = None


def test_eval_ground_truth_normalizes_legacy_constraints():
    normalized = eval_samples.normalize_ground_truth(
        {
            "intent_type": "recommend",
            "constraints": {
                "category": "美妆护肤",
                "max_price": 200,
                "use_case": "日常护肤",
                "must_have_features": ["洁面"],
                "forbidden_features": ["酒精"],
                "brands": ["华为"],
            },
        }
    )

    assert normalized["schema_version"] == eval_samples.GROUND_TRUTH_SCHEMA_VERSION
    assert normalized["constraints"]["budget_max"] == 200
    assert normalized["constraints"]["use_scenario"] == "日常护肤"
    assert normalized["constraints"]["must_match_terms"] == ["洁面"]
    assert normalized["constraints"]["forbidden_terms"] == ["酒精"]
    assert normalized["constraints"]["brand_prefer"] == ["华为"]


def test_eval_ground_truth_rejects_unknown_constraint_key():
    with pytest.raises(ValueError, match="Unsupported eval ground_truth constraints"):
        eval_samples.normalize_ground_truth({"constraints": {"unknown_key": "value"}})
