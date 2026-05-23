"""Root upload endpoint matching the backend PRD."""

from __future__ import annotations

from fastapi import APIRouter, HTTPException, Request

from src.services.image_upload import (
    MAX_IMAGE_BYTES,
    ImageUploadError,
    legacy_upload_response,
    parse_multipart_image,
    save_uploaded_image,
)
from src.types.schemas import ImageUploadRequest, ImageUploadResponse

upload_router = APIRouter(tags=["upload"])


@upload_router.post("/upload/image")
async def upload_image(request: Request) -> ImageUploadResponse:
    return await handle_upload_image(request)


@upload_router.post("/chat/upload/image", include_in_schema=False)
async def upload_image_legacy(request: Request) -> ImageUploadResponse:
    return await handle_upload_image(request)


async def handle_upload_image(request: Request) -> ImageUploadResponse:
    content_length = request.headers.get("content-length")
    if content_length is not None and int(content_length) > MAX_IMAGE_BYTES:
        raise HTTPException(
            status_code=413,
            detail={"code": "IMAGE_TOO_LARGE", "message": f"Request exceeds {MAX_IMAGE_BYTES} bytes"},
        )

    content_type = request.headers.get("content-type", "")
    try:
        if content_type.startswith("multipart/form-data"):
            parsed = parse_multipart_image(await request.body(), content_type)
            return save_uploaded_image(parsed.file_name, parsed.content_type, parsed.data)

        body = ImageUploadRequest.model_validate(await request.json())
        return legacy_upload_response(body.file_name, body.content_type)
    except ImageUploadError as exc:
        raise HTTPException(status_code=exc.status_code, detail={"code": exc.code, "message": str(exc)}) from exc
