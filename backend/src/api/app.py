from contextlib import asynccontextmanager
import asyncio
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
from src.repos.database import create_db_and_tables
from src.repos.ingest import EXPECTED_EMBEDDING_DIMENSIONS, seed_products_if_needed

settings = get_settings()
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    await asyncio.to_thread(_initialize_database)
    yield


def _initialize_database() -> None:
    create_db_and_tables()
    if not settings.auto_seed_on_startup:
        return
    expected_dimensions = EXPECTED_EMBEDDING_DIMENSIONS if settings.auto_seed_strict_embeddings else None
    stats = seed_products_if_needed(expected_embedding_dimensions=expected_dimensions)
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
    return {"status": "ok", "service": settings.app_name}
