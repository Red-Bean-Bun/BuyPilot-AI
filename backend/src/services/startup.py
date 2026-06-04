"""Application startup use cases."""

from __future__ import annotations

import logging
import os

from src.repos.database import create_db_and_tables
from src.services.product_ingest import EXPECTED_EMBEDDING_DIMENSIONS, seed_products_if_needed

logger = logging.getLogger(__name__)


def _check_api_key_configuration() -> None:
    """Validate that required API keys are configured for RAG pipeline."""
    bailian_key = os.getenv("BAILIAN_API_KEY", "")
    if not bailian_key or bailian_key.startswith("sk-your"):
        logger.warning(
            "BAILIAN_API_KEY 未配置或为占位符。"
            "RAG 链路（意图识别/标准生成/推荐解释）将无法工作。"
            "请复制 .env.example 到 .env 并填写真实的百炼 API Key。"
            "获取地址: https://bailian.console.aliyun.com/"
        )
    else:
        logger.info("BAILIAN_API_KEY 已配置")


async def initialize_database(auto_seed: bool = False, strict_embeddings: bool = False) -> dict[str, int | bool] | None:
    # Check API key configuration before database initialization
    _check_api_key_configuration()

    await create_db_and_tables()
    if not auto_seed:
        return None
    expected_dimensions = EXPECTED_EMBEDDING_DIMENSIONS if strict_embeddings else None
    return await seed_products_if_needed(expected_embedding_dimensions=expected_dimensions)
