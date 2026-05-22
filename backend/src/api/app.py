from fastapi import FastAPI

from src.api.admin_eval import admin_eval_router
from src.api.cart import cart_router
from src.api.cancel import cancel_router
from src.api.chat import chat_router
from src.api.feedback import feedback_router
from src.api.upload import upload_router
from src.config.settings import get_settings

settings = get_settings()

app = FastAPI(title="BuyPilot-AI", version="0.1.0")
app.include_router(chat_router, prefix="/chat")
app.include_router(cancel_router)
app.include_router(feedback_router)
app.include_router(upload_router)
app.include_router(cart_router)
app.include_router(admin_eval_router)


@app.get("/health")
async def health():
    return {"status": "ok", "service": settings.app_name}
