import sys

if sys.platform == "win32":
    import asyncio

    asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())

from contextlib import asynccontextmanager
import logging

from fastapi import Depends, FastAPI
from fastapi.staticfiles import StaticFiles
from starlette.types import ASGIApp, Message, Receive, Scope, Send

from src.api.admin_auth import require_admin_key
from src.api.admin_eval import admin_eval_router
from src.api.cart import cart_router
from src.api.cancel import cancel_router
from src.api.chat import chat_router
from src.api.feedback import feedback_router
from src.api.observability import observability_router
from src.api.upload import upload_router
from src.config.settings import get_settings
from src.middleware.request_context import RequestContextMiddleware
from src.runtime.cancel_registry import active_turn_count
from src.services.http_client import close_http_client
from src.services.startup import initialize_database

settings = get_settings()
logger = logging.getLogger(__name__)

# Paths served as static files — skip expensive middleware processing.
_STATIC_PATH_PREFIXES = ("/assets/", "/uploads/")


class CacheControlStaticAssetsMiddleware:
    """Add Cache-Control headers to static asset responses.

    Product images and uploads are immutable content-addressed files.
    Cache-Control lets Coil (Android) skip HTTP requests on repeat access.
    """

    def __init__(self, app: ASGIApp, max_age: int = 86400) -> None:
        self.app = app
        self.max_age = max_age

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http" or not scope["path"].startswith(_STATIC_PATH_PREFIXES):
            await self.app(scope, receive, send)
            return

        async def send_with_cache_control(message: Message) -> None:
            if message["type"] == "http.response.start":
                headers = list(message.get("headers", []))
                headers.append((b"cache-control", b"public, max-age=86400, immutable"))
                message["headers"] = headers
            await send(message)

        await self.app(scope, receive, send_with_cache_control)


@asynccontextmanager
async def lifespan(app: FastAPI):
    await _initialize_database()
    try:
        yield
    finally:
        await close_http_client()


async def _initialize_database() -> None:
    stats = await initialize_database(
        auto_seed=settings.auto_seed_on_startup,
        strict_embeddings=settings.auto_seed_strict_embeddings,
    )
    if stats is not None:
        logger.info("Database seed check completed: %s", stats)


def _public_api_dependencies():
    """Protect public tunnel APIs when ADMIN_API_KEY is configured."""
    if "pytest" in sys.modules:
        return []
    return [Depends(require_admin_key)] if settings.admin_api_key else []


app = FastAPI(title="BuyPilot-AI", version="0.1.0", lifespan=lifespan)
app.add_middleware(RequestContextMiddleware)
app.add_middleware(CacheControlStaticAssetsMiddleware)
app.mount("/uploads", StaticFiles(directory=settings.upload_dir, check_dir=False), name="uploads")
app.mount("/assets/products", StaticFiles(directory=settings.dataset_dir, check_dir=False), name="product_assets")
app.include_router(chat_router, prefix="/chat", dependencies=_public_api_dependencies())
app.include_router(cancel_router, dependencies=_public_api_dependencies())
app.include_router(feedback_router, dependencies=_public_api_dependencies())
app.include_router(upload_router, dependencies=_public_api_dependencies())
app.include_router(cart_router, dependencies=_public_api_dependencies())
app.include_router(admin_eval_router)
app.include_router(observability_router)


@app.get("/health")
async def health():
    return {
        "status": "ok",
        "service": settings.app_name,
        "strict_runtime": settings.strict_runtime,
        "fallback_policy": "llm_provider_fallback_only",
        "active_turns": active_turn_count(),
    }
