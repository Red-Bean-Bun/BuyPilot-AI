import hashlib
import uuid

from fastapi import APIRouter
from fastapi.responses import StreamingResponse

from src.runtime.pipeline import chat_stream
from src.services.observability_llm import schedule_sse_event_recording
from src.services.request_context import set_request_context, update_request_context
from src.types.schemas import ChatStreamRequest
from src.types.sse_events import SSEEventBase, format_sse

chat_router = APIRouter(tags=["chat"])


def _extract_sse_event_fields(event: SSEEventBase) -> dict:
    """Extract relevant fields from an SSE event for observability recording."""
    fields: dict = {
        "event_type": event.event,
        "seq": event.seq,
        "node_id": getattr(event, "node_id", None),
        "deck_id": getattr(event, "deck_id", None),
    }

    # criteria_card
    criteria = getattr(event, "criteria", None)
    if criteria and hasattr(criteria, "criteria_id"):
        fields["criteria_id"] = criteria.criteria_id

    # product_card
    product = getattr(event, "product", None)
    if product and hasattr(product, "product_id"):
        fields["product_ids"] = [product.product_id]

    # cart_action
    cart_product_id = getattr(event, "product_id", None)
    if cart_product_id and event.event == "cart_action":
        fields["product_ids"] = [cart_product_id]

    # final_decision
    winner = getattr(event, "winner_product_id", None)
    if winner and event.event == "final_decision":
        fields["product_ids"] = [winner]

    # text_delta
    message_id = getattr(event, "message_id", None)
    if message_id:
        fields["message_id"] = message_id
    delta = getattr(event, "delta", None)
    if delta:
        fields["delta_preview"] = delta[:100]
        fields["delta_hash"] = hashlib.sha256(delta.encode()).hexdigest()

    # done
    finish_reason = getattr(event, "finish_reason", None)
    if finish_reason:
        fields["finish_reason"] = finish_reason

    return fields


@chat_router.post("/stream")
async def stream_chat(body: ChatStreamRequest):
    sid = body.session_id or f"sess_{uuid.uuid4().hex}"
    turn_id = body.client_turn_id or f"turn_{uuid.uuid4().hex[:8]}"
    stream_body = body if body.client_turn_id else body.model_copy(update={"client_turn_id": turn_id})
    stream_context = update_request_context(
        trace_id=body.client_trace_id,
        session_id=sid,
        turn_id=turn_id,
    )

    async def event_generator():
        set_request_context(stream_context)
        async for event in chat_stream(sid, stream_body):
            # Record SSE event for observability (fire-and-forget)
            fields = _extract_sse_event_fields(event)
            schedule_sse_event_recording(**fields)
            yield format_sse(event)

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
            "X-Request-ID": stream_context.request_id,
            "X-Trace-ID": stream_context.trace_id or "",
        },
    )
