"""Image analysis stage wrapper.

当前能力：VLM 属性抽取驱动 RAG 检索。
- 用户上传图片 → Qwen-VL-Plus 分析 → 提取可见属性（品类/肤质/成分/颜色等）
- 分析结果拼入用户 message，参与后续 criteria 生成和 RAG 检索
- 不是图片 embedding 视觉相似召回（无 image-to-product matching）

答辩表述：VLM 提取可见属性驱动 RAG 检索。
"""

from __future__ import annotations

from src.services.llm_client import analyze_image


async def run_multimodal(image_url: str | None) -> dict | None:
    if not image_url:
        return None
    return await analyze_image(image_url)
