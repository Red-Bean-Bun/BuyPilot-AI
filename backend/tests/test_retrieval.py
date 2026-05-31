import pytest

from src.services.product_ingest import seed_products
from src.config.domain_terms import normalize_product_type
from src.repos.documents import ChunkDocument, VectorChunkHit
from src.repos.products import list_products
from src.services.retriever import RetrievalFilters, _sql_filters_for_recall, retrieve, retrieve_with_evidence
from src.types.sse_events import Constraints, CriteriaPayload


@pytest.fixture(autouse=True)
async def _seed_products_for_retrieval(seeded_products):
    del seeded_products


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
async def test_retrieve_normalizes_product_type_aliases():
    criteria = CriteriaPayload(
        category="美妆护肤",
        summary="油皮洗面奶 200元内 日常护肤",
        constraints=Constraints(skin_type="油性", budget_max=200, product_type="洗面奶"),
    )

    products = await retrieve(criteria, top_n=3)

    assert products
    assert all(normalize_product_type(product.sub_category) == "洁面" for product in products)


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
    assert all(
        "耐克" not in f"{product.brand} {product.name}" and "Nike" not in f"{product.brand} {product.name}"
        for product in products
    )


@pytest.mark.asyncio
async def test_retrieve_uses_db_chunk_embedding_and_binds_evidence(monkeypatch, tmp_path):
    database_url = f"sqlite:///{tmp_path / 'retrieval.db'}"
    monkeypatch.setenv("DATABASE_URL", database_url)

    from src.config import settings as settings_module

    settings_module._settings = None
    await seed_products()

    criteria = CriteriaPayload(
        category="美妆护肤",
        summary="油皮洗面奶 200元内 日常护肤",
        constraints=Constraints(skin_type="油性", budget_max=200),
    )

    retrieval = await retrieve_with_evidence(criteria, top_n=3)
    products = retrieval.products
    evidence = retrieval.evidence_by_product[products[0].product_id]

    assert products
    assert all(product.category == "美妆护肤" for product in products)

    assert evidence[0].source_type == "product_chunk"
    assert evidence[0].source_id
    assert evidence[0].source_id.startswith(products[0].product_id)
    assert evidence[0].snippet
    sql_filters = retrieval.trace_details["filters_applied"]["sql_filters_applied"]
    assert sql_filters["category"] == "美妆护肤"
    assert sql_filters["budget_max"] == 200
    filters_applied = retrieval.trace_details["filters_applied"]
    assert filters_applied["pre_filter_count"] >= filters_applied["post_filter_count"]


@pytest.mark.asyncio
async def test_retrieve_relaxes_budget_but_not_product_type():
    """budget_max relaxes when strict returns empty; product_type stays hard."""
    criteria = CriteriaPayload(
        category="数码电子",
        summary="蓝牙耳机 500元内",
        constraints=Constraints(budget_max=500, product_type="蓝牙耳机"),
    )

    retrieval = await retrieve_with_evidence(criteria, top_n=2)

    # budget_max=500 is too low for matching 真无线耳机 (1699/1899 CNY).
    # budget_max relaxes, product_type stays hard — we still get 真无线耳机.
    assert retrieval.products
    assert all(normalize_product_type(product.sub_category) == "真无线耳机" for product in retrieval.products)
    steps = retrieval.trace_details["filters_applied"]["relaxation_steps"]
    assert steps[0]["step"] == "strict"
    assert any(step["step"] == "without_budget_max" for step in steps)


def test_sql_filters_include_structured_hard_constraints():
    criteria = CriteriaPayload(
        category="美妆护肤",
        constraints=Constraints(
            budget_max=200,
            product_type="洗面奶",
            brand_avoid=["SK-II"],
            origin_avoid=["日系"],
        ),
    )

    filters = _sql_filters_for_recall(criteria, RetrievalFilters(avoid_products=frozenset({"p_beauty_001"})))

    assert filters.category == "美妆护肤"
    assert filters.budget_max == 200
    assert "洁面" in filters.product_type_aliases
    assert "洗面奶" in filters.product_type_aliases
    assert "SK-II" in filters.brand_avoid
    assert "资生堂" in filters.brand_avoid
    assert filters.avoid_product_ids == ("p_beauty_001",)


@pytest.mark.asyncio
async def test_retrieve_prefers_pgvector_hits(monkeypatch):
    product = list_products()[0]

    async def fake_pgvector_hits(query_embedding, limit, filters=None):
        assert query_embedding
        assert limit == 200
        assert filters is not None
        assert filters.category == product.category
        return [
            VectorChunkHit(
                document=ChunkDocument(
                    id=f"{product.product_id}:pgvector",
                    product_id=product.product_id,
                    chunk_text="pgvector 命中的商品证据",
                    chunk_index=0,
                    embedding=query_embedding,
                    metadata={},
                ),
                distance=0.1,
            )
        ]

    monkeypatch.setattr("src.services.retriever.list_vector_chunks_by_similarity", fake_pgvector_hits)

    criteria = CriteriaPayload(
        category=product.category,
        summary=product.name,
        constraints=Constraints(budget_max=(product.price or 0) + 1),
    )

    products = await retrieve(criteria, top_n=1)

    assert products[0].product_id == product.product_id


@pytest.mark.asyncio
async def test_retrieve_returns_empty_without_db_vector_hits(monkeypatch, tmp_path):
    monkeypatch.setenv("DATABASE_URL", f"sqlite:///{tmp_path / 'strict_retrieval.db'}")

    from src.config import settings as settings_module

    settings_module._settings = None

    async def fake_embed_text(text):
        return [1.0, 0.0]

    monkeypatch.setattr("src.services.retriever.embed_text", fake_embed_text)

    criteria = CriteriaPayload(category="美妆护肤", summary="油皮洗面奶")

    result = await retrieve_with_evidence(criteria, top_n=1)
    assert result.products == []
    settings_module._settings = None
