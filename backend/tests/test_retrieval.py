import asyncio

import pytest

from src.repos.ingest import seed_products
from src.services.evidence import get_evidence
from src.services.retriever import retrieve
from src.types.sse_events import Constraints, CriteriaPayload


@pytest.mark.asyncio
async def test_retrieve_applies_budget_and_category():
    criteria = CriteriaPayload(
        category="美妆护肤",
        summary="油性肌肤，200元内，日常护肤",
        constraints=Constraints(skin_type="油性", budget_max=200),
    )
    products = await retrieve(criteria)
    assert products
    assert all(product.category == "美妆护肤" for product in products)
    assert products[0].price <= 200


@pytest.mark.asyncio
async def test_retrieve_filters_feedback_avoid_products():
    criteria = CriteriaPayload(
        category="美妆护肤",
        summary="油性肌肤，200元内，日常护肤",
        constraints=Constraints(skin_type="油性", budget_max=200),
    )
    first_pass = await retrieve(criteria, top_n=5)
    avoided_id = first_pass[0].product_id

    second_pass = await retrieve(
        criteria,
        top_n=5,
        feedback={"avoid_products": [avoided_id], "avoid_traits": []},
    )

    assert second_pass
    assert avoided_id not in {product.product_id for product in second_pass}


@pytest.mark.asyncio
async def test_retrieve_filters_feedback_avoid_brand_traits():
    criteria = CriteriaPayload(
        category="服饰运动",
        summary="跑步训练，日常使用",
        constraints=Constraints(use_scenario="日常使用"),
    )

    products = await retrieve(
        criteria,
        top_n=10,
        feedback={"avoid_products": [], "avoid_traits": ["耐克"]},
    )

    assert products
    assert all("耐克" not in f"{product.brand} {product.name}" and "Nike" not in f"{product.brand} {product.name}" for product in products)


def test_retrieve_uses_db_chunk_embedding_and_binds_evidence(monkeypatch, tmp_path):
    database_url = f"sqlite:///{tmp_path / 'retrieval.db'}"
    monkeypatch.setenv("DATABASE_URL", database_url)

    from src.config import settings as settings_module

    settings_module._settings = None
    seed_products()

    criteria = CriteriaPayload(
        category="美妆护肤",
        summary="油皮洗面奶 200元内 日常护肤",
        constraints=Constraints(skin_type="油性", budget_max=200),
    )

    async def run_retrieval_with_evidence():
        retrieved = await retrieve(criteria, top_n=3)
        bound_evidence = await get_evidence(retrieved[0])
        return retrieved, bound_evidence

    products, evidence = asyncio.run(run_retrieval_with_evidence())

    assert products
    assert all(product.category == "美妆护肤" for product in products)

    assert evidence[0].source_type == "product_chunk"
    assert evidence[0].source_id
    assert evidence[0].source_id.startswith(products[0].product_id)
    assert evidence[0].snippet
