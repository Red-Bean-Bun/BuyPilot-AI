import pytest

import src.config.settings as settings_module
from src.repos.audit import (
    insert_api_request_log,
    insert_audit_event,
    list_api_request_logs,
    list_audit_events,
)
from src.services.audit import list_request_log_payloads
from tests.conftest import collect_sse_stream


@pytest.fixture
async def observability_database(monkeypatch, tmp_path):
    monkeypatch.setenv("DATABASE_URL", f"sqlite:///{tmp_path / 'observability.db'}")
    monkeypatch.setenv("ADMIN_API_KEY", "test-key")
    settings_module._settings = None
    from src.services.product_ingest import seed_products_if_needed

    await seed_products_if_needed()
    yield
    settings_module._settings = None


@pytest.mark.asyncio
async def test_request_middleware_skips_health_request_log(test_client, observability_database):
    del observability_database

    async with test_client as c:
        response = await c.get("/health", headers={"X-Request-ID": "req_test_001", "X-Trace-ID": "trace_test_001"})

    assert response.status_code == 200
    assert response.headers["X-Request-ID"] == "req_test_001"
    assert response.headers["X-Trace-ID"] == "trace_test_001"

    rows = await list_api_request_logs(trace_id="trace_test_001")
    assert rows == []


@pytest.mark.asyncio
async def test_feedback_endpoint_records_audit_event_and_session_request(test_client, observability_database):
    del observability_database

    async with test_client as c:
        response = await c.post(
            "/feedback",
            headers={"X-Trace-ID": "trace_feedback_001"},
            json={
                "session_id": "sess_feedback_obs",
                "feedback_type": "not_interested",
                "action": "not_interested",
                "product_id": "p_beauty_011",
                "reason": "不喜欢香味",
            },
        )
        assert response.status_code == 200

        request_rows = await list_api_request_logs(session_id="sess_feedback_obs")
        request_response = await c.get(
            f"/admin/observability/requests/{request_rows[0].request_id}",
            headers={"Authorization": "Bearer test-key"},
        )
        filtered_response = await c.get(
            "/admin/observability/requests?method=POST&path=/feedback",
            headers={"Authorization": "Bearer test-key"},
        )

    audit_rows = await list_audit_events(session_id="sess_feedback_obs", action="feedback.created")
    assert len(audit_rows) == 1
    assert audit_rows[0].resource_type == "feedback"
    assert audit_rows[0].resource_id == "p_beauty_011"
    assert audit_rows[0].audit_metadata["reason"] == "不喜欢香味"

    assert [row.path for row in request_rows] == ["/feedback"]
    assert request_rows[0].trace_id == "trace_feedback_001"
    assert request_rows[0].request_body_json["session_id"] == "sess_feedback_obs"
    assert request_rows[0].request_body_json["product_id"] == "p_beauty_011"
    assert request_rows[0].response_body_json["status"] == "received"

    assert request_response.status_code == 200
    request_payload = request_response.json()
    assert request_payload["request_id"] == request_rows[0].request_id
    assert request_payload["requests"][0]["request_body_json"]["reason"] == "不喜欢香味"
    assert request_payload["requests"][0]["response_body_json"]["session_id"] == "sess_feedback_obs"
    assert filtered_response.status_code == 200
    assert any(row["request_id"] == request_rows[0].request_id for row in filtered_response.json())


@pytest.mark.asyncio
async def test_chat_turn_audit_can_be_queried_by_observability_api(test_client, observability_database):
    del observability_database

    async with test_client as c:
        async with c.stream(
            "POST",
            "/chat/stream",
            headers={"X-Request-ID": "req_chat_001"},
            json={
                "message": "推荐适合油皮的洗面奶，200元以内，日常护肤",
                "session_id": "sess_chat_obs",
                "client_turn_id": "turn_chat_obs",
                "client_trace_id": "trace_chat_obs",
            },
        ) as response:
            assert response.status_code == 200
            assert response.headers["X-Request-ID"] == "req_chat_001"
            assert response.headers["X-Trace-ID"] == "trace_chat_obs"
            events = await collect_sse_stream(response)

        turn_response = await c.get(
            "/admin/observability/turns/turn_chat_obs",
            headers={"Authorization": "Bearer test-key"},
        )
        session_response = await c.get(
            "/admin/observability/sessions/sess_chat_obs",
            headers={"Authorization": "Bearer test-key"},
        )

    assert events[-1][0] == "done"

    audit_actions = {row.action for row in await list_audit_events(turn_id="turn_chat_obs")}
    assert "chat.turn_started" in audit_actions
    assert "chat.turn_completed" in audit_actions
    assert "chat.recommendation_persisted" in audit_actions

    turn_payload = turn_response.json()
    assert turn_payload["turn_id"] == "turn_chat_obs"
    assert "retrieval_traces" in turn_payload
    assert "evidence_links" in turn_payload
    assert {event["action"] for event in turn_payload["audit_events"]} >= {
        "chat.turn_started",
        "chat.turn_completed",
    }
    assert [row.path for row in await list_api_request_logs(turn_id="turn_chat_obs")] == ["/chat/stream"]

    session_payload = session_response.json()
    assert session_payload["session_id"] == "sess_chat_obs"
    assert "retrieval_traces" in session_payload
    assert "evidence_links" in session_payload
    assert any(event["turn_id"] == "turn_chat_obs" for event in session_payload["audit_events"])


@pytest.mark.asyncio
async def test_request_log_payload_backfills_generated_turn_id_from_audit(observability_database):
    del observability_database

    await insert_api_request_log(
        request_id="req_generated_turn",
        trace_id="trace_generated_turn",
        session_id="sess_generated_turn",
        turn_id=None,
        method="POST",
        path="/chat/stream",
        status_code=200,
        duration_ms=12.3,
    )
    await insert_audit_event(
        action="chat.turn_started",
        request_id="req_generated_turn",
        trace_id="trace_generated_turn",
        session_id="sess_generated_turn",
        turn_id="turn_generated_obs",
        resource_type="chat_turn",
        resource_id="turn_generated_obs",
    )

    payloads = await list_request_log_payloads(trace_id="trace_generated_turn")

    assert len(payloads) == 1
    assert payloads[0]["turn_id"] == "turn_generated_obs"

    turn_payloads = await list_request_log_payloads(turn_id="turn_generated_obs")
    assert len(turn_payloads) == 1
    assert turn_payloads[0]["request_id"] == "req_generated_turn"
    assert turn_payloads[0]["turn_id"] == "turn_generated_obs"


@pytest.mark.asyncio
async def test_cache_stats_endpoint(test_client, observability_database):
    """Test /admin/observability/cache returns cache stats with admin key."""
    del observability_database

    from src.services.retrieval_cache import get_retrieval_cache
    from src.types.sse_events import Constraints, CriteriaPayload

    # Populate cache with some entries
    cache = get_retrieval_cache()
    cache.clear()
    crit = CriteriaPayload(criteria_id="test", category="美妆护肤", constraints=Constraints())
    cache.set(crit, None, "test_value")
    cache.get(crit, None)  # hit

    async with test_client as c:
        # Without admin key → 401/403
        response_no_key = await c.get("/admin/observability/cache")
        assert response_no_key.status_code in (401, 403)

        # With admin key → 200 with stats
        response = await c.get(
            "/admin/observability/cache",
            headers={"Authorization": "Bearer test-key"},
        )
        assert response.status_code == 200
        data = response.json()
        assert "total_keys" in data
        assert "hits" in data
        assert "misses" in data
        assert "hit_rate" in data
        assert "hot_keys" in data
        assert isinstance(data["hot_keys"], list)

    # Cleanup
    cache.clear()
