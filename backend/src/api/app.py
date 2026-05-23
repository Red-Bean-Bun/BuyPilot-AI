from contextlib import asynccontextmanager
import logging

from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles

from src.api.admin_eval import admin_eval_router
from src.api.cart import cart_router
from src.api.cancel import cancel_router
from src.api.chat import chat_router
from src.api.feedback import feedback_router
from src.api.upload import upload_router
from src.config.settings import get_settings
from src.runtime.cancel_registry import active_turn_count
from src.services.async_io import run_sync_io
from src.services.startup import initialize_database

settings = get_settings()
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    await run_sync_io(_initialize_database)
    yield


def _initialize_database() -> None:
    stats = initialize_database(
        auto_seed=settings.auto_seed_on_startup,
        strict_embeddings=settings.auto_seed_strict_embeddings,
    )
    if stats is not None:
        logger.info("Database seed check completed: %s", stats)


app = FastAPI(title="BuyPilot-AI", version="0.1.0", lifespan=lifespan)
app.mount("/uploads", StaticFiles(directory=settings.upload_dir, check_dir=False), name="uploads")
app.include_router(chat_router, prefix="/chat")
app.include_router(cancel_router)
app.include_router(feedback_router)
app.include_router(upload_router)
app.include_router(cart_router)
app.include_router(admin_eval_router)


@app.get("/health")
async def health():
    return {
        "status": "ok",
        "service": settings.app_name,
        "strict_runtime": settings.strict_runtime,
        "fallback_policy": "fail_fast" if settings.strict_runtime else "demo_visible_degradation",
        "active_turns": active_turn_count(),
    }
