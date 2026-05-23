import uuid

from fastapi import APIRouter
from fastapi.responses import StreamingResponse

from src.runtime.pipeline import chat_stream
from src.types.schemas import ChatStreamRequest
from src.types.sse_events import format_sse

chat_router = APIRouter(tags=["chat"])


@chat_router.post("/stream")
async def stream_chat(body: ChatStreamRequest):
    sid = body.session_id or f"sess_{uuid.uuid4().hex}"

    async def event_generator():
        async for event in chat_stream(sid, body):
            yield format_sse(event)

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )
