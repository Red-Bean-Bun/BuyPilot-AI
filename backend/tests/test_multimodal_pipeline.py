import pytest

from src.runtime import pipeline as pipeline_module
from src.types.schemas import ChatStreamRequest


@pytest.fixture(autouse=True)
async def _seed_products_for_multimodal(seeded_products):
    del seeded_products


@pytest.mark.asyncio
async def test_pipeline_injects_multimodal_analysis_into_criteria(monkeypatch):
    async def fake_run_multimodal(image_url: str):
        return {
            "image_url": image_url,
            "category_hint": "食品饮料",
            "description": "一瓶无糖茶饮料，适合日常饮用",
            "visible_traits": ["无糖", "茶饮料"],
        }

    monkeypatch.setattr(pipeline_module, "run_multimodal", fake_run_multimodal)

    events = [
        event
        async for event in pipeline_module.chat_stream(
            "sess_multimodal",
            ChatStreamRequest(message="这个适合日常喝吗？", image_url="/uploads/test.png"),
        )
    ]
    tags = [event.event for event in events]
    criteria = [event.criteria for event in events if event.event == "criteria_card"][0]

    assert tags[0] == "thinking"
    assert events[0].stage == "analyzing_image"
    assert criteria.category == "食品饮料"
    assert "食品饮料" in criteria.chips
    assert criteria.constraints.product_type == "茶饮"
    assert "无糖" in criteria.constraints.dietary
    products = [event.product for event in events if event.event == "product_card"]
    assert products
    assert all(product.sub_category == "茶饮" for product in products)
