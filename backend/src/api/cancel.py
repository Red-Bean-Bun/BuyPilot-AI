"""Cancel endpoint for best-effort stream cancellation."""

from __future__ import annotations

from fastapi import APIRouter

from src.runtime.cancel_registry import cancel_turn
from src.types.schemas import CancelRequest, CancelResponse

cancel_router = APIRouter(tags=["cancel"])


@cancel_router.post("/chat/cancel")
async def cancel_chat(body: CancelRequest) -> CancelResponse:
    canceled = cancel_turn(body.session_id, body.turn_id)
    return CancelResponse(session_id=body.session_id, turn_id=body.turn_id, canceled=canceled)
