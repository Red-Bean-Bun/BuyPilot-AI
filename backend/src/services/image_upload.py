"""Image upload storage and provider URL helpers."""

from __future__ import annotations

import base64
import mimetypes
import uuid
from dataclasses import dataclass
from email.parser import BytesParser
from email.policy import default
from http import HTTPStatus
from pathlib import Path

from src.config.settings import get_settings
from src.types.schemas import ImageUploadResponse

MAX_IMAGE_BYTES = 5 * 1024 * 1024
UPLOAD_URL_PREFIX = "/uploads"
ALLOWED_MIME_TYPES = {
    "image/jpeg": ".jpg",
    "image/jpg": ".jpg",
    "image/png": ".png",
    "image/webp": ".webp",
}


class ImageUploadError(ValueError):
    def __init__(self, code: str, message: str, status_code: int = 400) -> None:
        super().__init__(message)
        self.code = code
        self.status_code = status_code


@dataclass(frozen=True)
class ParsedUpload:
    file_name: str
    content_type: str
    data: bytes
    fields: dict[str, str]


def parse_multipart_image(body: bytes, content_type: str) -> ParsedUpload:
    message = BytesParser(policy=default).parsebytes(
        f"Content-Type: {content_type}\r\nMIME-Version: 1.0\r\n\r\n".encode("utf-8") + body
    )
    fields: dict[str, str] = {}
    file_name = ""
    file_content_type = ""
    file_data = b""

    for part in message.iter_parts():
        disposition = part.get_content_disposition()
        if disposition != "form-data":
            continue
        raw_name = part.get_param("name", header="content-disposition")
        name = raw_name if isinstance(raw_name, str) else None
        filename = part.get_filename()
        raw_payload = part.get_payload(decode=True)
        payload = raw_payload if isinstance(raw_payload, bytes) else b""
        if filename:
            file_name = filename
            file_content_type = part.get_content_type()
            file_data = payload
        elif name:
            fields[name] = payload.decode("utf-8", errors="ignore")

    if not file_name or not file_data:
        raise ImageUploadError("IMAGE_FILE_REQUIRED", "Multipart field 'file' is required.")
    return ParsedUpload(file_name=file_name, content_type=file_content_type, data=file_data, fields=fields)


def save_uploaded_image(file_name: str, content_type: str, data: bytes) -> ImageUploadResponse:
    normalized_type = _normalize_content_type(content_type)
    if normalized_type not in ALLOWED_MIME_TYPES:
        raise ImageUploadError("IMAGE_FORMAT_INVALID", "Only JPEG, PNG, and WebP images are supported.")
    if len(data) > MAX_IMAGE_BYTES:
        raise ImageUploadError("IMAGE_TOO_LARGE", "Image exceeds the 5MB limit.", status_code=HTTPStatus.REQUEST_ENTITY_TOO_LARGE)

    upload_dir = get_settings().upload_dir
    upload_dir.mkdir(parents=True, exist_ok=True)
    stored_name = _stored_file_name(file_name, normalized_type)
    target = upload_dir / stored_name
    target.write_bytes(data)
    width, height = _image_dimensions(data, normalized_type)
    return ImageUploadResponse(
        image_url=f"{UPLOAD_URL_PREFIX}/{stored_name}",
        width=width,
        height=height,
        mime_type=normalized_type,
        ocr_text=None,
        analysis={"status": "stored", "original_file_name": Path(file_name).name},
    )


def image_url_to_provider_url(image_url: str) -> str:
    if not image_url.startswith(f"{UPLOAD_URL_PREFIX}/"):
        return image_url
    file_name = Path(image_url).name
    path = get_settings().upload_dir / file_name
    if not path.exists() or not path.is_file():
        raise ImageUploadError("IMAGE_FILE_NOT_FOUND", f"Uploaded image is not available: {file_name}", status_code=HTTPStatus.NOT_FOUND)
    mime_type = mimetypes.guess_type(path.name)[0] or "image/jpeg"
    encoded = base64.b64encode(path.read_bytes()).decode("ascii")
    return f"data:{mime_type};base64,{encoded}"


def _normalize_content_type(content_type: str | None) -> str:
    if not content_type:
        return "image/jpeg"
    return content_type.split(";", 1)[0].strip().lower()


def _stored_file_name(file_name: str, content_type: str) -> str:
    original_suffix = Path(file_name).suffix.lower()
    suffix = (
        original_suffix if original_suffix in {".jpg", ".jpeg", ".png", ".webp"} else ALLOWED_MIME_TYPES[content_type]
    )
    if suffix == ".jpeg":
        suffix = ".jpg"
    return f"upload_{uuid.uuid4().hex}{suffix}"


def _image_dimensions(data: bytes, content_type: str) -> tuple[int | None, int | None]:
    if content_type == "image/png" and data.startswith(b"\x89PNG\r\n\x1a\n") and len(data) >= 24:
        return int.from_bytes(data[16:20], "big"), int.from_bytes(data[20:24], "big")
    if content_type in {"image/jpeg", "image/jpg"}:
        return _jpeg_dimensions(data)
    if content_type == "image/webp":
        return _webp_dimensions(data)
    return None, None


def _jpeg_dimensions(data: bytes) -> tuple[int | None, int | None]:
    index = 2
    while index + 9 < len(data):
        if data[index] != 0xFF:
            index += 1
            continue
        marker = data[index + 1]
        index += 2
        if marker in {0xD8, 0xD9}:
            continue
        if index + 2 > len(data):
            break
        segment_length = int.from_bytes(data[index : index + 2], "big")
        if marker in {0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7, 0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF}:
            if index + 7 <= len(data):
                height = int.from_bytes(data[index + 3 : index + 5], "big")
                width = int.from_bytes(data[index + 5 : index + 7], "big")
                return width, height
        index += max(segment_length, 2)
    return None, None


def _webp_dimensions(data: bytes) -> tuple[int | None, int | None]:
    if len(data) < 30 or not data.startswith(b"RIFF") or data[8:12] != b"WEBP":
        return None, None
    chunk = data[12:16]
    if chunk == b"VP8X" and len(data) >= 30:
        width = 1 + int.from_bytes(data[24:27], "little")
        height = 1 + int.from_bytes(data[27:30], "little")
        return width, height
    if chunk == b"VP8 " and len(data) >= 30:
        width = int.from_bytes(data[26:28], "little") & 0x3FFF
        height = int.from_bytes(data[28:30], "little") & 0x3FFF
        return width, height
    if chunk == b"VP8L" and len(data) >= 25:
        bits = int.from_bytes(data[21:25], "little")
        width = (bits & 0x3FFF) + 1
        height = ((bits >> 14) & 0x3FFF) + 1
        return width, height
    return None, None
