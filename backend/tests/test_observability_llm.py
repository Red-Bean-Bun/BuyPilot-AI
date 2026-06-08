"""Tests for LLM call observability feature."""

import pytest
from sqlalchemy.exc import SQLAlchemyError

from src.repos.observability_llm import (
    insert_llm_call,
    list_llm_calls_by_turn,
    update_llm_call_parsed_json,
)
from src.services.observability import get_turn_debug_bundle
from src.services.observability_llm import schedule_llm_call_recording


@pytest.fixture
async def observability_llm_database():
    """Test database for LLM observability tests."""
    from src.services.product_ingest import seed_products_if_needed

    await seed_products_if_needed()
    yield


@pytest.mark.asyncio
async def test_insert_llm_call_completes_and_retrieves(observability_llm_database):
    """Verify that a complete LLM call record can be inserted and retrieved."""
    del observability_llm_database

    record_id = await insert_llm_call(
        turn_id="turn_test_001",
        session_id="sess_test_001",
        task="analyze_intent",
        profile="doubao-lite",
        model="doubao-seed-2.0-lite",
        provider="Doubao",
        status="success",
        duration_ms=123.45,
        prompt_hash="abc123def456",
        prompt_preview="测试提示预览",
        prompt_json='{"messages": [{"role": "user", "content": "测试"}]}',
        response_preview="测试响应预览",
        response_json='{"category": "美妆护肤"}',
        parsed_json={"category": "美妆护肤", "confidence": 0.95},
        validation_error=None,
        token_usage={"prompt_tokens": 100, "completion_tokens": 50},
        fallback_from=None,
        error_type=None,
        error_message=None,
        error_raw=None,
    )

    assert record_id is not None

    rows = await list_llm_calls_by_turn("turn_test_001")
    assert len(rows) == 1

    row = rows[0]
    assert row["id"] == record_id
    assert row["turn_id"] == "turn_test_001"
    assert row["session_id"] == "sess_test_001"
    assert row["task"] == "analyze_intent"
    assert row["profile"] == "doubao-lite"
    assert row["model"] == "doubao-seed-2.0-lite"
    assert row["provider"] == "Doubao"
    assert row["status"] == "success"
    assert row["duration_ms"] == 123.45
    assert row["prompt_hash"] == "abc123def456"
    assert row["prompt_preview"] == "测试提示预览"
    assert row["prompt_json"] == '{"messages": [{"role": "user", "content": "测试"}]}'
    assert row["response_preview"] == "测试响应预览"
    assert row["response_json"] == '{"category": "美妆护肤"}'
    assert row["parsed_json"] == {"category": "美妆护肤", "confidence": 0.95}
    assert row["validation_error"] is None
    assert row["token_usage"] == {"prompt_tokens": 100, "completion_tokens": 50}
    assert row["fallback_from"] is None
    assert row["error_type"] is None
    assert row["error_message"] is None
    assert row["error_raw"] is None
    assert row["created_at"] is not None


@pytest.mark.asyncio
async def test_list_llm_calls_by_turn_filters_and_orders(observability_llm_database):
    """Verify records can be queried by turn_id and are ordered by created_at ASC."""
    del observability_llm_database

    await insert_llm_call(
        turn_id="turn_A",
        session_id="sess_1",
        task="analyze_intent",
        profile="doubao-lite",
        model="doubao-seed-2.0-lite",
        provider="Doubao",
        status="success",
        duration_ms=100.0,
        prompt_hash="hash1",
    )

    await insert_llm_call(
        turn_id="turn_A",
        session_id="sess_1",
        task="generate_criteria",
        profile="qwen-plus",
        model="qwen-plus",
        provider="Qwen",
        status="success",
        duration_ms=200.0,
        prompt_hash="hash2",
    )

    await insert_llm_call(
        turn_id="turn_B",
        session_id="sess_1",
        task="analyze_intent",
        profile="doubao-lite",
        model="doubao-seed-2.0-lite",
        provider="Doubao",
        status="success",
        duration_ms=150.0,
        prompt_hash="hash3",
    )

    rows_A = await list_llm_calls_by_turn("turn_A")
    assert len(rows_A) == 2
    assert rows_A[0]["task"] == "analyze_intent"
    assert rows_A[1]["task"] == "generate_criteria"
    assert rows_A[0]["duration_ms"] == 100.0
    assert rows_A[1]["duration_ms"] == 200.0

    rows_B = await list_llm_calls_by_turn("turn_B")
    assert len(rows_B) == 1
    assert rows_B[0]["task"] == "analyze_intent"

    rows_C = await list_llm_calls_by_turn("turn_C")
    assert len(rows_C) == 0


@pytest.mark.asyncio
async def test_update_llm_call_parsed_json_updates_record(observability_llm_database):
    """Verify parsed_json can be updated after initial insertion."""
    del observability_llm_database

    await insert_llm_call(
        turn_id="turn_update_test",
        session_id="sess_1",
        task="generate_criteria",
        profile="qwen-plus",
        model="qwen-plus",
        provider="Qwen",
        status="success",
        duration_ms=200.0,
        prompt_hash="hash_update",
        parsed_json=None,
        validation_error=None,
    )

    new_parsed_json = {"category": "美妆护肤", "constraints": {"budget_max": 200}}
    new_validation_error = "field mismatch"

    success = await update_llm_call_parsed_json(
        turn_id="turn_update_test",
        task="generate_criteria",
        parsed_json=new_parsed_json,
        validation_error=new_validation_error,
    )

    assert success is True

    rows = await list_llm_calls_by_turn("turn_update_test")
    assert len(rows) == 1
    assert rows[0]["parsed_json"] == new_parsed_json
    assert rows[0]["validation_error"] == new_validation_error


@pytest.mark.asyncio
async def test_update_llm_call_parsed_json_returns_false_for_nonexistent(observability_llm_database):
    """Verify update returns False when no matching record exists."""
    del observability_llm_database

    success = await update_llm_call_parsed_json(
        turn_id="turn_nonexistent",
        task="analyze_intent",
        parsed_json={"test": "data"},
        validation_error=None,
    )

    assert success is False


@pytest.mark.asyncio
async def test_insert_llm_call_returns_none_on_database_error(monkeypatch):
    """Verify insert_llm_call gracefully returns None on SQLAlchemyError."""

    class FailingSession:
        def __init__(self, *args, **kwargs):
            pass

        async def __aenter__(self):
            return self

        async def __aexit__(self, exc_type, exc_val, exc_tb):
            pass

        def add(self, obj):
            raise SQLAlchemyError("Simulated database failure")

        async def commit(self):
            raise SQLAlchemyError("Simulated database failure")

        async def refresh(self, obj):
            pass

    monkeypatch.setattr("src.repos.observability_llm.AsyncSession", FailingSession)

    result = await insert_llm_call(
        turn_id="turn_error_test",
        session_id="sess_1",
        task="analyze_intent",
        profile="doubao-lite",
        model="doubao-seed-2.0-lite",
        provider="Doubao",
        status="success",
        duration_ms=100.0,
        prompt_hash="hash_error",
    )

    assert result is None


@pytest.mark.asyncio
async def test_schedule_llm_call_recording_fire_and_forget(monkeypatch):
    """Verify schedule_llm_call_recording returns immediately without blocking."""
    import asyncio
    from src.services.request_context import RequestContext, set_request_context
    from src.config import settings as settings_module

    monkeypatch.setenv("OBSERVABILITY_LOCAL_ENABLED", "1")
    settings_module._settings = None

    from src.services.product_ingest import seed_products_if_needed

    await seed_products_if_needed()

    set_request_context(
        RequestContext(
            request_id="req_fire_forget",
            trace_id="trace_fire_forget",
            session_id="sess_fire_forget",
            turn_id="turn_fire_forget",
        )
    )

    import time

    start_time = time.time()

    schedule_llm_call_recording(
        task="analyze_intent",
        profile="doubao-lite",
        model="doubao-seed-2.0-lite",
        provider="Doubao",
        status="success",
        duration_ms=100.0,
        messages=[{"role": "user", "content": "测试消息"}],
        response="测试响应",
        parsed_json={"category": "美妆护肤"},
        validation_error=None,
        token_usage={"prompt_tokens": 50},
        fallback_from=None,
        error_type=None,
        error_message=None,
        error_raw=None,
    )

    elapsed = time.time() - start_time
    assert elapsed < 0.1, "schedule_llm_call_recording should return immediately"

    await asyncio.sleep(0.2)

    rows = await list_llm_calls_by_turn("turn_fire_forget")
    assert len(rows) == 1
    assert rows[0]["task"] == "analyze_intent"
    assert rows[0]["turn_id"] == "turn_fire_forget"


@pytest.mark.asyncio
async def test_observability_local_enabled_zero_disables_recording(monkeypatch):
    """Verify OBSERVABILITY_LOCAL_ENABLED=0 prevents record creation."""
    import asyncio
    from src.services.request_context import RequestContext, set_request_context
    from src.config import settings as settings_module

    monkeypatch.setenv("OBSERVABILITY_LOCAL_ENABLED", "0")
    settings_module._settings = None

    from src.services.product_ingest import seed_products_if_needed

    await seed_products_if_needed()

    set_request_context(
        RequestContext(
            request_id="req_disabled",
            trace_id="trace_disabled",
            session_id="sess_disabled",
            turn_id="turn_disabled",
        )
    )

    schedule_llm_call_recording(
        task="analyze_intent",
        profile="doubao-lite",
        model="doubao-seed-2.0-lite",
        provider="Doubao",
        status="success",
        duration_ms=100.0,
        messages=[{"role": "user", "content": "测试消息"}],
        response="测试响应",
        parsed_json=None,
        validation_error=None,
        token_usage=None,
        fallback_from=None,
        error_type=None,
        error_message=None,
        error_raw=None,
    )

    await asyncio.sleep(0.2)

    rows = await list_llm_calls_by_turn("turn_disabled")
    assert len(rows) == 0, "No records should be created when OBSERVABILITY_LOCAL_ENABLED=0"


@pytest.mark.asyncio
async def test_debug_bundle_includes_llm_calls(observability_llm_database):
    """Verify get_turn_debug_bundle includes llm_calls in the response."""
    del observability_llm_database

    await insert_llm_call(
        turn_id="turn_bundle_test",
        session_id="sess_bundle",
        task="analyze_intent",
        profile="doubao-lite",
        model="doubao-seed-2.0-lite",
        provider="Doubao",
        status="success",
        duration_ms=100.0,
        prompt_hash="hash_bundle",
        prompt_preview="bundle test prompt",
        parsed_json={"category": "美妆护肤"},
    )

    await insert_llm_call(
        turn_id="turn_bundle_test",
        session_id="sess_bundle",
        task="generate_criteria",
        profile="qwen-plus",
        model="qwen-plus",
        provider="Qwen",
        status="success",
        duration_ms=200.0,
        prompt_hash="hash_bundle_2",
        prompt_preview="bundle test prompt 2",
        parsed_json={"category": "美妆护肤", "constraints": {}},
    )

    bundle = await get_turn_debug_bundle("turn_bundle_test")

    assert "llm_calls" in bundle
    assert isinstance(bundle["llm_calls"], list)
    assert len(bundle["llm_calls"]) == 2

    llm_call_tasks = [call["task"] for call in bundle["llm_calls"]]
    assert "analyze_intent" in llm_call_tasks
    assert "generate_criteria" in llm_call_tasks

    first_call = bundle["llm_calls"][0]
    assert first_call["turn_id"] == "turn_bundle_test"
    assert first_call["session_id"] == "sess_bundle"
    assert first_call["prompt_preview"] == "bundle test prompt"
    assert first_call["parsed_json"] == {"category": "美妆护肤"}

    assert "requests" in bundle
    assert "audit_events" in bundle
