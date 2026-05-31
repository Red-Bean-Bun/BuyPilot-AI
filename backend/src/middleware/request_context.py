"""FastAPI middleware for request correlation and access logging."""

from __future__ import annotations

import time
import uuid

from starlette.datastructures import Headers, MutableHeaders
from starlette.types import ASGIApp, Message, Receive, Scope, Send

from src.services.audit import record_api_request
from src.services.request_context import RequestContext, clear_request_context, get_request_context, set_request_context

# Exact-match paths to exclude from request logging (noise / self-referential).
REQUEST_LOG_EXCLUDED_PATHS = {
    "/health", "/health/",
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
        started_at = time.perf_counter()
        status_code = 500
        error_type = None

        async def send_with_correlation_headers(message: Message) -> None:
            nonlocal status_code
            if message["type"] == "http.response.start":
                status_code = message["status"]
                context = get_request_context()
                response_headers = MutableHeaders(scope=message)
                response_headers["X-Request-ID"] = context.request_id if context else request_id
                response_headers["X-Trace-ID"] = (context.trace_id if context else trace_id) or request_id
            await send(message)

        try:
            await self.app(scope, receive, send_with_correlation_headers)
        except Exception as exc:
            error_type = type(exc).__name__
            raise
        finally:
            context = get_request_context()
            duration_ms = round((time.perf_counter() - started_at) * 1000, 2)
            client = scope.get("client")
            client_ip = client[0] if client else None
            if _should_log_request(path):
                await record_api_request(
                    method=scope["method"],
                    path=path,
                    status_code=status_code,
                    duration_ms=duration_ms,
                    client_ip=client_ip,
                    user_agent=request_headers.get("user-agent"),
                    error_type=error_type,
                )
            if context is not None:
                clear_request_context()
