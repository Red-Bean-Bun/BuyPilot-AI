import json
from unittest.mock import AsyncMock, patch

import pytest

from src.runtime.stages.criteria import run_criteria
from src.services.llm_task_payloads import criteria_messages
from src.types.schemas import ChatStreamRequest, IntentResult, MessageLite
from src.types.sse_events import CriteriaPayload


def test_criteria_messages_include_history():
    history = [
        {"role": "user", "content": "推荐适合油皮的洁面\x00"},
        {"role": "assistant", "content": "可以，先看洁面。"},
    ]

    messages = criteria_messages(
        "再便宜点的呢",
        {"intent": "recommend", "category": "美妆护肤", "extracted_constraints": {}},
        feedback=None,
        existing_dump=None,
        conversation_context="上一轮是洁面推荐",
        history=history,
    )

    assert "推荐适合油皮的洁面" in messages[0]["content"]
    assert "\x00" not in messages[0]["content"]
    payload = json.loads(messages[1]["content"])
    assert payload["history"][0]["content"] == "推荐适合油皮的洁面"
    assert payload["history"][1]["content"] == "可以，先看洁面。"


@pytest.mark.asyncio
async def test_run_criteria_passes_request_history_to_llm():
    captured = {}

    async def fake_generate_criteria(*args, **kwargs):
        captured["message"] = args[0]
        captured["history"] = kwargs["history"]
        return CriteriaPayload(category="美妆护肤", summary="测试")

    body = ChatStreamRequest(
        message="再便宜点的呢",
        history=[
            MessageLite(role="user", content="推荐适合油皮的洁面"),
            MessageLite(role="assistant", content="先看这些洁面。"),
        ],
    )
    intent = IntentResult(intent="recommend", category="美妆护肤")

    with (
        patch("src.runtime.stages.criteria.get_previous_criteria", new=AsyncMock(return_value=None)),
        patch("src.runtime.stages.criteria.get_feedback_context", new=AsyncMock(return_value={})),
        patch("src.runtime.stages.criteria.get_conversation_summary", new=AsyncMock(return_value="summary")),
        patch("src.runtime.stages.criteria.diagnose_criteria_context", new=AsyncMock(return_value=None)),
        patch("src.runtime.stages.criteria.generate_criteria", new=fake_generate_criteria),
    ):
        await run_criteria("sess_history", body, intent)

    assert captured["message"] == "再便宜点的呢"
    assert captured["history"] == [
        {"role": "user", "content": "推荐适合油皮的洁面"},
        {"role": "assistant", "content": "先看这些洁面。"},
    ]
