"""Intent stage wrapper."""

from __future__ import annotations

from src.services.llm_client import analyze_intent
from src.types.schemas import ChatStreamRequest, IntentResult


async def run_intent(body: ChatStreamRequest) -> IntentResult:
    return await analyze_intent(
        body.message,
        history=[item.model_dump() for item in body.history],
        image_url=body.image_url,
    )

