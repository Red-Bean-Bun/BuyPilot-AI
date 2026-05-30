import pytest

from tests.conftest import collect_sse_stream


@pytest.fixture(autouse=True)
async def _seed_products_for_chat_api(seeded_products):
    del seeded_products


class TestChatStreamEndpoint:
    @pytest.mark.asyncio
    async def test_stream_returns_events(self, test_client):
        async with test_client as c:
            async with c.stream(
                "POST",
                "/chat/stream",
                json={"message": "推荐适合油皮的洗面奶，200元以内，日常护肤"},
            ) as resp:
                assert resp.status_code == 200
                events = await collect_sse_stream(resp)

        assert len(events) > 0
        assert events[0][0] == "thinking"
        assert events[-1][0] == "done"

    @pytest.mark.asyncio
    async def test_session_id_consistency(self, test_client):
        async with test_client as c:
            async with c.stream(
                "POST",
                "/chat/stream",
                json={"message": "test", "session_id": "my-session-123"},
            ) as resp:
                events = await collect_sse_stream(resp)

        for tag, data in events:
            assert data["session_id"] == "my-session-123"

    @pytest.mark.asyncio
    async def test_session_id_generated_when_missing(self, test_client):
        async with test_client as c:
            async with c.stream(
                "POST",
                "/chat/stream",
                json={"message": "test"},
            ) as resp:
                events = await collect_sse_stream(resp)

        sids = [data["session_id"] for _, data in events]
        assert len(set(sids)) == 1
        assert len(sids[0]) > 0

    @pytest.mark.asyncio
    async def test_required_event_types_present(self, test_client):
        """Product-first: default request includes product_card + criteria_card."""
        async with test_client as c:
            async with c.stream(
                "POST",
                "/chat/stream",
                json={"message": "推荐适合油皮的洗面奶，200元以内，日常护肤"},
            ) as resp:
                events = await collect_sse_stream(resp)

        tags = [tag for tag, _ in events]
        assert "thinking" in tags
        assert "criteria_card" in tags
        assert "text_delta" in tags
        assert "done" in tags
        assert "product_card" in tags
        assert events[-1][1]["finish_reason"] in ("awaiting_product_feedback", "completed")

    @pytest.mark.asyncio
    async def test_product_card_has_required_fields(self, test_client):
        async with test_client as c:
            async with c.stream(
                "POST",
                "/chat/stream",
                json={"message": "推荐适合油皮的洗面奶，200元以内，日常护肤"},
            ) as resp:
                events = await collect_sse_stream(resp)

        product_events = [(t, d) for t, d in events if t == "product_card"]
        assert len(product_events) >= 1

        _, data = product_events[0]
        assert "product_id" in data["product"]
        assert "name" in data["product"]
        assert "reason" in data
        assert data["reason_atoms"]
        assert "evidence" in data

    @pytest.mark.asyncio
    async def test_event_order_correct(self, test_client):
        """Product-first: intro text → product_card → criteria_card → done."""
        async with test_client as c:
            async with c.stream(
                "POST",
                "/chat/stream",
                json={"message": "推荐适合油皮的洗面奶，200元以内，日常护肤"},
            ) as resp:
                events = await collect_sse_stream(resp)

        tags = [t for t, _ in events]
        first_thinking = tags.index("thinking")
        first_text_delta = tags.index("text_delta")
        first_product = tags.index("product_card")
        first_criteria = tags.index("criteria_card")
        first_done = tags.index("done")

        assert first_thinking < first_text_delta
        assert first_text_delta < first_product
        assert first_product < first_criteria
        assert first_criteria < first_done

    @pytest.mark.asyncio
    async def test_text_delta_done_marker(self, test_client):
        async with test_client as c:
            async with c.stream(
                "POST",
                "/chat/stream",
                json={"message": "推荐适合油皮的洗面奶，200元以内，日常护肤"},
            ) as resp:
                events = await collect_sse_stream(resp)

        deltas = [d for t, d in events if t == "text_delta"]
        assert any(d["done"] is True for d in deltas)

    @pytest.mark.asyncio
    async def test_product_has_category_fields(self, test_client):
        async with test_client as c:
            async with c.stream(
                "POST",
                "/chat/stream",
                json={"message": "推荐适合油皮的洗面奶"},
            ) as resp:
                events = await collect_sse_stream(resp)

        for t, d in events:
            if t == "product_card":
                assert "category" in d["product"]

    @pytest.mark.asyncio
    async def test_final_decision_has_compare_action(self, test_client):
        async with test_client as c:
            async with c.stream(
                "POST",
                "/chat/stream",
                json={
                    "message": "推荐适合油皮的洗面奶，200元以内，日常护肤",
                    "session_id": "sess_chat_api_decision",
                },
            ) as resp:
                await collect_sse_stream(resp)
            async with c.stream(
                "POST",
                "/chat/stream",
                json={"message": "继续", "session_id": "sess_chat_api_decision"},
            ) as resp:
                events = await collect_sse_stream(resp)

        decisions = [d for t, d in events if t == "final_decision"]
        assert len(decisions) >= 1
        actions = decisions[0]["next_actions"]
        action_types = [a["action"] for a in actions]
        assert "compare" in action_types
