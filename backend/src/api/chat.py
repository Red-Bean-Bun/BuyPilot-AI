import uuid

from fastapi import APIRouter
from fastapi.responses import StreamingResponse

from src.repos.cart_items import get_cart
from src.repos.feedbacks import add_feedback
from src.runtime.pipeline import chat_stream
from src.types.schemas import ChatStreamRequest, FeedbackRequest, FeedbackResponse, ImageUploadRequest, ImageUploadResponse
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


@chat_router.post("/upload/image")
async def upload_image(body: ImageUploadRequest) -> ImageUploadResponse:
    return ImageUploadResponse(
        image_url=f"https://example.com/upload/mock_{body.file_name}",
        width=1280,
        height=960,
        mime_type=body.content_type,
        ocr_text=None,
        analysis={"status": "received"},
    )


@chat_router.post("/feedback")
async def submit_feedback(body: FeedbackRequest) -> FeedbackResponse:
    action = body.feedback_type or body.action or "feedback"
    add_feedback(body.session_id, action=action, product_id=body.product_id, reason=body.reason)
    return FeedbackResponse(session_id=body.session_id, feedback_type=body.feedback_type, action=body.action)


@chat_router.get("/cart/{session_id}")
async def read_cart(session_id: str):
    return get_cart(session_id)
