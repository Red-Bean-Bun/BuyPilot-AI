import pytest

from src.runtime.cancel_registry import register_turn, unregister_turn


@pytest.mark.asyncio
async def test_cancel_endpoint_sets_active_turn_token(test_client):
    token = register_turn("s_cancel_api", "turn_cancel_api")
    try:
        async with test_client as client:
            response = await client.post(
                "/chat/cancel",
                json={"session_id": "s_cancel_api", "turn_id": "turn_cancel_api"},
            )
        assert response.status_code == 200
        assert response.json()["canceled"] is True
        assert token.cancelled is True
    finally:
        unregister_turn("s_cancel_api", "turn_cancel_api")


@pytest.mark.asyncio
async def test_cancel_endpoint_reports_missing_turn(test_client):
    async with test_client as client:
        response = await client.post(
            "/chat/cancel",
            json={"session_id": "missing", "turn_id": "turn_missing"},
        )
    assert response.status_code == 200
    assert response.json()["canceled"] is False
