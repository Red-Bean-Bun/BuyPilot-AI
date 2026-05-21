"""Root upload endpoint matching the backend PRD."""

from __future__ import annotations

from fastapi import APIRouter

from src.types.schemas import ImageUploadRequest, ImageUploadResponse

upload_router = APIRouter(tags=["upload"])


@upload_router.post("/upload/image")
async def upload_image(body: ImageUploadRequest) -> ImageUploadResponse:
    return ImageUploadResponse(
        image_url=f"https://example.com/upload/mock_{body.file_name}",
        width=1280,
        height=960,
        mime_type=body.content_type,
        analysis={"status": "received"},
    )
