import pytest

import src.config.settings as settings_module
from src.runtime.cancel_registry import register_turn, unregister_turn
from src.services.cancellation import (
    clear_chat_turn,
    is_chat_turn_cancellation_requested,
    register_chat_turn,
)


@pytest.fixture
def cancellation_database(monkeypatch, tmp_path):
    monkeypatch.setenv("DATABASE_URL", f"sqlite:///{tmp_path / 'cancel.db'}")
    settings_module._settings = None
    yield
    settings_module._settings = None


@pytest.mark.asyncio
async def test_cancel_endpoint_sets_active_turn_token(test_client, cancellation_database):
    del cancellation_database
    token = register_turn("s_cancel_api", "turn_cancel_api")
    try:
        await register_chat_turn("s_cancel_api", "turn_cancel_api")
        async with test_client as client:
            response = await client.post(
                "/chat/cancel",
                json={"session_id": "s_cancel_api", "turn_id": "turn_cancel_api"},
            )
        assert response.status_code == 200
        assert response.json()["canceled"] is True
        assert token.cancelled is True
        assert await is_chat_turn_cancellation_requested("s_cancel_api", "turn_cancel_api")
    finally:
        await clear_chat_turn("s_cancel_api", "turn_cancel_api")
        unregister_turn("s_cancel_api", "turn_cancel_api")


@pytest.mark.asyncio
async def test_cancel_endpoint_reports_missing_turn(test_client, cancellation_database):
    del cancellation_database
    async with test_client as client:
        response = await client.post(
            "/chat/cancel",
            json={"session_id": "missing", "turn_id": "turn_missing"},
        )
    assert response.status_code == 200
    assert response.json()["canceled"] is False


@pytest.mark.asyncio
async def test_cancel_endpoint_records_remote_active_turn(test_client, cancellation_database):
    del cancellation_database
    await register_chat_turn("s_remote_cancel", "turn_remote_cancel")
    try:
        async with test_client as client:
            response = await client.post(
                "/chat/cancel",
                json={"session_id": "s_remote_cancel", "turn_id": "turn_remote_cancel"},
            )
        assert response.status_code == 200
        assert response.json()["canceled"] is True
        assert await is_chat_turn_cancellation_requested("s_remote_cancel", "turn_remote_cancel")
    finally:
        await clear_chat_turn("s_remote_cancel", "turn_remote_cancel")
