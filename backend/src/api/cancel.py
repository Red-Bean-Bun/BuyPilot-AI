"""Cancel endpoint for best-effort stream cancellation."""

from __future__ import annotations

from fastapi import APIRouter

from src.types.schemas import CancelRequest, CancelResponse

cancel_router = APIRouter(tags=["cancel"])


@cancel_router.post("/chat/cancel")
async def cancel_chat(body: CancelRequest) -> CancelResponse:
    return CancelResponse(session_id=body.session_id, turn_id=body.turn_id)

