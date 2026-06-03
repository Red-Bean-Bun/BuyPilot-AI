"""FastAPI middleware for request correlation and access logging."""

from __future__ import annotations

import json
import time
import uuid
from typing import Any

from starlette.datastructures import Headers, MutableHeaders
from starlette.types import ASGIApp, Message, Receive, Scope, Send

from src.services.audit import record_api_request
from src.services.request_context import RequestContext, clear_request_context, get_request_context, set_request_context

# Exact-match paths to exclude from request logging (noise / self-referential).
REQUEST_LOG_EXCLUDED_PATHS = {
    "/health",
    "/health/",
    "/",
    "/favicon.ico",
    "/sitemap.xml",
    "/robots.txt",
}

# Prefix-match paths to exclude. Admin/observability queries are self-referential
# and pollute the request log with zero-information entries.
REQUEST_LOG_EXCLUDED_PREFIXES = ("/admin",)

# Static file paths — skip all middleware processing (UUID, audit, context).
# These are immutable content-addressed files; per-request overhead is wasted.
_STATIC_PATH_PREFIXES = ("/assets/", "/uploads/")
_MAX_LOG_BODY_BYTES = 256 * 1024


def _should_log_request(path: str) -> bool:
    """Decide whether a request path should be persisted to api_request_logs."""
    if path in REQUEST_LOG_EXCLUDED_PATHS:
        return False
    if path.startswith(REQUEST_LOG_EXCLUDED_PREFIXES):
        return False
    return True


class RequestContextMiddleware:
    def __init__(self, app: ASGIApp) -> None:
        self.app = app

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        # Skip all middleware overhead for static assets — no UUID, no audit, no context.
        path = scope["path"]
        if path.startswith(_STATIC_PATH_PREFIXES):
            await self.app(scope, receive, send)
            return

        request_headers = Headers(scope=scope)
        request_id = request_headers.get("x-request-id") or f"req_{uuid.uuid4().hex[:12]}"
        trace_id = (
            request_headers.get("x-trace-id")
            or request_headers.get("x-client-trace-id")
            or request_headers.get("traceparent")
            or request_id
        )
        set_request_context(RequestContext(request_id=request_id, trace_id=trace_id))
        should_capture_request_body = (
            scope["method"] in {"POST", "PUT", "PATCH", "DELETE"}
            and (request_headers.get("content-length") not in {None, "0"} or bool(request_headers.get("content-type")))
        )
        if should_capture_request_body:
            request_messages, request_body, request_body_truncated = await _capture_request_body(receive)
        else:
            request_messages, request_body, request_body_truncated = [], b"", False
        request_body_json, request_body_text = _body_payload(
            request_body,
            request_headers.get("content-type"),
            truncated=request_body_truncated,
            request_like=True,
        )
        response_content_type: str | None = None
        response_chunks: list[bytes] = []
        response_body_truncated = False
        started_at = time.perf_counter()
        status_code = 500
        error_type = None
        replay_index = 0

        async def replay_receive() -> Message:
            nonlocal replay_index
            if not should_capture_request_body:
                return await receive()
            if replay_index < len(request_messages):
                message = request_messages[replay_index]
                replay_index += 1
                return message
            return await receive()

        async def send_with_correlation_headers(message: Message) -> None:
            nonlocal response_body_truncated, response_content_type, status_code
            if message["type"] == "http.response.start":
                status_code = message["status"]
                context = get_request_context()
                response_headers = MutableHeaders(scope=message)
                response_headers["X-Request-ID"] = context.request_id if context else request_id
                response_headers["X-Trace-ID"] = (context.trace_id if context else trace_id) or request_id
                response_content_type = response_headers.get("content-type")
            elif message["type"] == "http.response.body":
                body = message.get("body", b"")
                if body and not response_body_truncated:
                    remaining = _MAX_LOG_BODY_BYTES - sum(len(chunk) for chunk in response_chunks)
                    if remaining > 0:
                        response_chunks.append(body[:remaining])
                    if len(body) > remaining:
                        response_body_truncated = True
            await send(message)

        try:
            await self.app(scope, replay_receive, send_with_correlation_headers)
        except Exception as exc:
            error_type = type(exc).__name__
            raise
        finally:
            context = get_request_context()
            duration_ms = round((time.perf_counter() - started_at) * 1000, 2)
            client = scope.get("client")
            client_ip = client[0] if client else None
            if _should_log_request(path):
                response_body = b"".join(response_chunks)
                response_body_json, response_body_text = _body_payload(
                    response_body,
                    response_content_type,
                    truncated=response_body_truncated,
                    request_like=False,
                )
                await record_api_request(
                    method=scope["method"],
                    path=path,
                    status_code=status_code,
                    duration_ms=duration_ms,
                    client_ip=client_ip,
                    user_agent=request_headers.get("user-agent"),
                    request_content_type=request_headers.get("content-type"),
                    request_body_json=request_body_json,
                    request_body_text=request_body_text,
                    request_body_truncated=request_body_truncated,
                    response_content_type=response_content_type,
                    response_body_json=response_body_json,
                    response_body_text=response_body_text,
                    response_body_truncated=response_body_truncated,
                    error_type=error_type,
                )
            if context is not None:
                clear_request_context()


async def _capture_request_body(receive: Receive) -> tuple[list[Message], bytes, bool]:
    messages: list[Message] = []
    chunks: list[bytes] = []
    truncated = False
    captured_bytes = 0
    while True:
        message = await receive()
        messages.append(message)
        if message["type"] != "http.request":
            break
        body = message.get("body", b"")
        if body and not truncated:
            remaining = _MAX_LOG_BODY_BYTES - captured_bytes
            if remaining > 0:
                chunks.append(body[:remaining])
                captured_bytes += min(len(body), remaining)
            if len(body) > remaining:
                truncated = True
        if not message.get("more_body", False):
            break
    return messages, b"".join(chunks), truncated


def _body_payload(
    body: bytes,
    content_type: str | None,
    *,
    truncated: bool,
    request_like: bool,
) -> tuple[dict[str, Any] | list[Any] | None, str | None]:
    if not body:
        return None, None
    normalized = (content_type or "").split(";", 1)[0].strip().lower()
    if normalized == "application/json" or normalized.endswith("+json"):
        try:
            parsed = json.loads(body.decode("utf-8"))
            return parsed, None
        except (UnicodeDecodeError, json.JSONDecodeError):
            return None, body.decode("utf-8", errors="replace")
    if request_like and normalized == "multipart/form-data":
        return {
            "omitted": "multipart/form-data",
            "captured_bytes": len(body),
            "truncated": truncated,
        }, None
    if normalized.startswith("text/") or normalized in {"text/event-stream", "application/x-ndjson"}:
        return None, body.decode("utf-8", errors="replace")
    return {
        "omitted": "non-text body",
        "content_type": content_type,
        "captured_bytes": len(body),
        "truncated": truncated,
    }, None
