import pytest

from src.runtime.pipeline import chat_stream
from src.types.schemas import ChatStreamRequest


@pytest.mark.asyncio
async def test_pipeline_event_order_and_deck_id():
    events = [
        event
        async for event in chat_stream(
            "s1",
            ChatStreamRequest(message="推荐适合油皮的洗面奶，200元以内，日常护肤"),
        )
    ]
    tags = [event.event for event in events]
    assert tags.index("thinking") < tags.index("criteria_card")
    assert tags.index("criteria_card") < tags.index("text_delta")
    assert tags.index("product_card") < tags.index("final_decision")
    assert tags[-1] == "done"
    product_deck_ids = {event.deck_id for event in events if event.event == "product_card"}
    assert len(product_deck_ids) == 1
    assert [event.seq for event in events] == sorted(event.seq for event in events)


@pytest.mark.asyncio
async def test_pipeline_clarification_short_circuit():
    events = [event async for event in chat_stream("s2", ChatStreamRequest(message="随便看看"))]
    tags = [event.event for event in events]
    assert "clarification" in tags
    assert "product_card" not in tags
    assert tags[-1] == "done"

