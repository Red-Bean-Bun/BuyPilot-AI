"""Intent stage wrapper."""

from __future__ import annotations

from src.services.conversation_state import get_conversation_summary
from src.services.llm_client import analyze_intent
from src.types.schemas import ChatStreamRequest, IntentResult


async def run_intent(session_id: str, body: ChatStreamRequest) -> IntentResult:
    ctx_summary = await get_conversation_summary(session_id)
    return await analyze_intent(
        body.message,
        history=[item.model_dump() for item in body.history],
        image_url=body.image_url,
        conversation_context=ctx_summary,
    )
