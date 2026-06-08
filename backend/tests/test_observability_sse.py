"""Tests for SSE event observability recording and retrieval."""

import pytest
from sqlalchemy.exc import SQLAlchemyError

from src.repos.observability_llm import (
    insert_sse_event,
    list_sse_events_by_turn,
)
from src.services.observability import get_turn_debug_bundle
from src.services.observability_llm import schedule_sse_event_recording


@pytest.fixture
async def observability_sse_database():
    """Test database for SSE observability tests."""
    from src.services.product_ingest import seed_products_if_needed

    await seed_products_if_needed()
    yield


@pytest.mark.asyncio
async def test_insert_sse_event_and_retrieve(observability_sse_database):
    """Verify that a complete SSE event record can be inserted and retrieved."""
    del observability_sse_database

    record_id = await insert_sse_event(
        turn_id="turn_sse_001",
        session_id="sess_sse_001",
        event_type="product_card",
        seq=1,
        node_id="node_abc",
        deck_id="deck_1",
        criteria_id="crit_1",
        product_ids=["prod_1", "prod_2"],
        message_id="msg_001",
        delta_preview="推荐商品",
        delta_hash="sha256hash",
        finish_reason="stop",
    )

    assert record_id is not None

    rows = await list_sse_events_by_turn("turn_sse_001")
    assert len(rows) == 1

    row = rows[0]
    assert row["id"] == record_id
    assert row["turn_id"] == "turn_sse_001"
    assert row["session_id"] == "sess_sse_001"
    assert row["event_type"] == "product_card"
    assert row["seq"] == 1
    assert row["node_id"] == "node_abc"
    assert row["deck_id"] == "deck_1"
    assert row["criteria_id"] == "crit_1"
    assert row["product_ids"] == ["prod_1", "prod_2"]
    assert row["message_id"] == "msg_001"
    assert row["delta_preview"] == "推荐商品"
    assert row["delta_hash"] == "sha256hash"
    assert row["finish_reason"] == "stop"
    assert row["created_at"] is not None


@pytest.mark.asyncio
async def test_list_sse_events_by_turn_ordered_by_seq(observability_sse_database):
    """Verify events are returned ordered by seq ASC regardless of insertion order."""
    del observability_sse_database

    await insert_sse_event(
        turn_id="turn_order",
        session_id="sess_1",
        event_type="text_delta",
        seq=3,
        delta_preview="third",
    )
    await insert_sse_event(
        turn_id="turn_order",
        session_id="sess_1",
        event_type="thinking",
        seq=1,
        delta_preview="first",
    )
    await insert_sse_event(
        turn_id="turn_order",
        session_id="sess_1",
        event_type="criteria_card",
        seq=2,
        delta_preview="second",
    )

    rows = await list_sse_events_by_turn("turn_order")
    assert len(rows) == 3
    assert rows[0]["seq"] == 1
    assert rows[0]["event_type"] == "thinking"
    assert rows[1]["seq"] == 2
    assert rows[1]["event_type"] == "criteria_card"
    assert rows[2]["seq"] == 3
    assert rows[2]["event_type"] == "text_delta"


@pytest.mark.asyncio
async def test_list_sse_events_filters_by_turn_id(observability_sse_database):
    """Verify filtering isolates events per turn_id."""
    del observability_sse_database

    await insert_sse_event(
        turn_id="turn_alpha",
        session_id="sess_1",
        event_type="thinking",
        seq=1,
    )
    await insert_sse_event(
        turn_id="turn_alpha",
        session_id="sess_1",
        event_type="text_delta",
        seq=2,
    )
    await insert_sse_event(
        turn_id="turn_beta",
        session_id="sess_1",
        event_type="done",
        seq=1,
    )

    rows_alpha = await list_sse_events_by_turn("turn_alpha")
    assert len(rows_alpha) == 2
    assert all(r["turn_id"] == "turn_alpha" for r in rows_alpha)

    rows_beta = await list_sse_events_by_turn("turn_beta")
    assert len(rows_beta) == 1
    assert rows_beta[0]["turn_id"] == "turn_beta"

    rows_gamma = await list_sse_events_by_turn("turn_gamma")
    assert len(rows_gamma) == 0


@pytest.mark.asyncio
async def test_insert_sse_event_returns_none_on_database_error(monkeypatch):
    """Verify insert_sse_event gracefully returns None on SQLAlchemyError."""

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

    result = await insert_sse_event(
        turn_id="turn_error",
        session_id="sess_1",
        event_type="thinking",
        seq=1,
    )

    assert result is None


@pytest.mark.asyncio
async def test_schedule_sse_event_recording_fire_and_forget(monkeypatch):
    """Verify schedule_sse_event_recording returns immediately and records asynchronously."""
    import asyncio
    import time

    from src.services.request_context import RequestContext, set_request_context
    from src.config import settings as settings_module

    monkeypatch.setenv("OBSERVABILITY_LOCAL_ENABLED", "1")
    settings_module._settings = None

    from src.services.product_ingest import seed_products_if_needed

    await seed_products_if_needed()

    set_request_context(
        RequestContext(
            request_id="req_sse_fire",
            trace_id="trace_sse_fire",
            session_id="sess_sse_fire",
            turn_id="turn_sse_fire",
        )
    )

    start_time = time.time()

    schedule_sse_event_recording(
        event_type="criteria_card",
        seq=1,
        criteria_id="crit_fire",
        delta_preview="购买标准",
    )

    elapsed = time.time() - start_time
    assert elapsed < 0.1, "schedule_sse_event_recording should return immediately"

    await asyncio.sleep(0.2)

    rows = await list_sse_events_by_turn("turn_sse_fire")
    assert len(rows) == 1
    assert rows[0]["event_type"] == "criteria_card"
    assert rows[0]["turn_id"] == "turn_sse_fire"
    assert rows[0]["criteria_id"] == "crit_fire"


@pytest.mark.asyncio
async def test_observability_disabled_skips_sse_recording(monkeypatch):
    """Verify OBSERVABILITY_LOCAL_ENABLED=0 prevents SSE event record creation."""
    import asyncio

    from src.services.request_context import RequestContext, set_request_context
    from src.config import settings as settings_module

    monkeypatch.setenv("OBSERVABILITY_LOCAL_ENABLED", "0")
    settings_module._settings = None

    from src.services.product_ingest import seed_products_if_needed

    await seed_products_if_needed()

    set_request_context(
        RequestContext(
            request_id="req_sse_off",
            trace_id="trace_sse_off",
            session_id="sess_sse_off",
            turn_id="turn_sse_off",
        )
    )

    schedule_sse_event_recording(
        event_type="thinking",
        seq=1,
    )

    await asyncio.sleep(0.2)

    rows = await list_sse_events_by_turn("turn_sse_off")
    assert len(rows) == 0, "No records should be created when OBSERVABILITY_LOCAL_ENABLED=0"


@pytest.mark.asyncio
async def test_debug_bundle_includes_sse_events(observability_sse_database):
    """Verify get_turn_debug_bundle includes sse_events in the response."""
    del observability_sse_database

    await insert_sse_event(
        turn_id="turn_bundle",
        session_id="sess_bundle",
        event_type="thinking",
        seq=1,
        delta_preview="推理中",
    )
    await insert_sse_event(
        turn_id="turn_bundle",
        session_id="sess_bundle",
        event_type="product_card",
        seq=2,
        product_ids=["prod_a", "prod_b"],
        criteria_id="crit_bundle",
    )

    bundle = await get_turn_debug_bundle("turn_bundle")

    assert "sse_events" in bundle
    assert isinstance(bundle["sse_events"], list)
    assert len(bundle["sse_events"]) == 2

    assert bundle["sse_events"][0]["seq"] == 1
    assert bundle["sse_events"][0]["event_type"] == "thinking"
    assert bundle["sse_events"][0]["delta_preview"] == "推理中"

    assert bundle["sse_events"][1]["seq"] == 2
    assert bundle["sse_events"][1]["event_type"] == "product_card"
    assert bundle["sse_events"][1]["product_ids"] == ["prod_a", "prod_b"]
    assert bundle["sse_events"][1]["criteria_id"] == "crit_bundle"

    assert "requests" in bundle
    assert "audit_events" in bundle
    assert "llm_calls" in bundle
