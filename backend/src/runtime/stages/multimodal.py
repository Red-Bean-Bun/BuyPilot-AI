"""Image analysis + embedding stage wrappers.

当前能力：VLM 属性抽取驱动 RAG 检索 + 视觉相似召回。
- 用户上传图片 → Qwen-VL-Plus 分析 → 提取可见属性（品类/肤质/成分/颜色等）
- 分析结果拼入用户 message，参与后续 criteria 生成和 RAG 检索
- 图片 → qwen3-vl-embedding → image embedding 用于视觉相似召回
"""

from __future__ import annotations

import logging

from src.services.embedding import EmbeddingUnavailable, embed_image
from src.services.image_upload import image_url_to_provider_url
from src.services.llm_client import analyze_image

logger = logging.getLogger(__name__)


async def run_multimodal(image_url: str | None) -> dict | None:
    if not image_url:
        return None
    return await analyze_image(image_url)


async def run_image_embedding(image_url: str | None) -> list[float] | None:
    """Compute image embedding for visual similarity retrieval.

    Converts local upload URL to provider-accessible data URI,
    then calls qwen3-vl-embedding. Returns None on any failure
    (graceful degradation — text-only retrieval continues).
    """
    if not image_url:
        return None
    try:
        provider_url = image_url_to_provider_url(image_url)
        return await embed_image(provider_url)
    except (EmbeddingUnavailable, Exception) as exc:
        logger.info("Image embedding unavailable, degrading to text-only: %s", exc)
        return None
