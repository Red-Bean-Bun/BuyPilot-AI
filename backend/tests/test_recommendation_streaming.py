import pytest

from src.runtime.cancel_registry import CancellationToken
from src.runtime.handlers import _pop_recommendation_stream_chunk, _stream_recommendation_text_events
from src.runtime.streaming import StreamContext
from src.services.llm_gateway import _data_line_payload, _stream_delta_content
from src.types.sse_events import EventSeq


def test_llm_gateway_extracts_openai_stream_delta_content():
    assert _data_line_payload('data: {"choices":[{"delta":{"content":"你好"}}]}') == (
        '{"choices":[{"delta":{"content":"你好"}}]}'
    )
    assert _stream_delta_content({"choices": [{"delta": {"content": "你好"}}]}) == "你好"


def test_recommendation_stream_chunk_smoothing_prefers_sentence_boundaries():
    chunk, rest = _pop_recommendation_stream_chunk("首选测试洁面乳，适合油皮")

    assert chunk == "首选测试洁面乳，"
    assert rest == "适合油皮"


def test_recommendation_stream_chunk_smoothing_caps_large_chunks():
    chunk, rest = _pop_recommendation_stream_chunk("首选测试洁面乳适合油皮日常清洁")

    assert chunk == "首选测试洁面乳适"
    assert len(chunk) == 8
    assert rest == "合油皮日常清洁"


@pytest.mark.asyncio
async def test_recommendation_stream_events_emit_small_text_delta_chunks():
    async def deltas():
        yield "首选测试洁面乳，适合油皮日常清洁。"

    ctx = StreamContext(
        session_id="s1",
        turn_id="t1",
        deck_id="d1",
        seq=EventSeq("t1"),
        cancel_token=CancellationToken(session_id="s1", turn_id="t1"),
        stages=object(),
        heartbeat_interval_seconds=0.01,
    )

    events = [event async for event in _stream_recommendation_text_events(ctx, deltas())]
    text_events = [event for event in events if event.event == "text_delta"]

    assert len(text_events) > 1
    assert all(len(event.delta) <= 8 for event in text_events if event.delta)
    assert text_events[-1].done is True
