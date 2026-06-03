"""Cancel endpoint for best-effort stream cancellation."""

from __future__ import annotations

import logging

from fastapi import APIRouter

from src.runtime.cancel_registry import cancel_turn
from src.services.audit import record_audit_event
from src.services.cancellation import request_chat_turn_cancellation
from src.services.request_context import update_request_context
from src.types.schemas import CancelRequest, CancelResponse

logger = logging.getLogger(__name__)

cancel_router = APIRouter(tags=["cancel"])


@cancel_router.post("/chat/cancel")
async def cancel_chat(body: CancelRequest) -> CancelResponse:
    update_request_context(session_id=body.session_id, turn_id=body.turn_id)
    local_cancelled = cancel_turn(body.session_id, body.turn_id)
    persisted_cancelled = await request_chat_turn_cancellation(body.session_id, body.turn_id)
    canceled = local_cancelled or persisted_cancelled
    try:
        await record_audit_event(
            "chat.cancel_requested",
            session_id=body.session_id,
            turn_id=body.turn_id,
            resource_type="chat_turn",
            resource_id=body.turn_id,
            side_effect=canceled,
            metadata={
                "canceled": canceled,
                "local_cancelled": local_cancelled,
                "persisted_cancelled": persisted_cancelled,
            },
        )
    except Exception:
        logger.warning("Failed to record audit event: chat.cancel_requested", exc_info=True)
    return CancelResponse(session_id=body.session_id, turn_id=body.turn_id, canceled=canceled)
