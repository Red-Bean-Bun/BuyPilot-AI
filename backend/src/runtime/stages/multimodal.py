"""Image analysis stage wrapper."""

from __future__ import annotations

from src.services.llm_client import analyze_image


async def run_multimodal(image_url: str | None) -> dict | None:
    if not image_url:
        return None
    return await analyze_image(image_url)
