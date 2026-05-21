import pytest


class TestAdditionalApiContracts:
    @pytest.mark.asyncio
    async def test_root_feedback_endpoint(self, test_client):
        async with test_client as c:
            resp = await c.post("/feedback", json={"session_id": "s1", "action": "dislike"})
        assert resp.status_code == 200
        assert resp.json()["status"] == "received"

    @pytest.mark.asyncio
    async def test_cancel_endpoint(self, test_client):
        async with test_client as c:
            resp = await c.post("/chat/cancel", json={"session_id": "s1", "turn_id": "t1"})
        assert resp.status_code == 200
        assert resp.json()["canceled"] is True

    @pytest.mark.asyncio
    async def test_root_cart_endpoint(self, test_client):
        async with test_client as c:
            resp = await c.get("/cart/s1")
        assert resp.status_code == 200
        assert "items" in resp.json()

