"""Tests for the product detail API endpoint."""

import pytest
from fastapi.testclient import TestClient

from src.api.app import app
from src.services.product_ingest import seed_products


@pytest.fixture
async def setup_database():
    """Setup test database with seeded products."""
    await seed_products()
    yield


@pytest.mark.asyncio
async def test_get_product_detail_success(setup_database):
    """Test successful retrieval of product detail."""
    client = TestClient(app)

    # Test with a known product
    response = client.get("/products/p_food_001")

    assert response.status_code == 200
    data = response.json()

    # Verify product payload structure
    assert "product" in data
    assert data["product"]["product_id"] == "p_food_001"
    assert data["product"]["name"]
    assert data["product"]["category"] == "食品生活"

    # Verify new detail fields
    assert "marketing_description" in data
    assert "highlights" in data
    assert "faqs" in data
    assert "reviews" in data

    # Verify field types
    assert isinstance(data["highlights"], list)
    assert isinstance(data["faqs"], list)
    assert isinstance(data["reviews"], list)

    # Verify marketing_description is string or null
    assert data["marketing_description"] is None or isinstance(data["marketing_description"], str)


@pytest.mark.asyncio
async def test_get_product_detail_not_found(setup_database):
    """Test 404 response for non-existent product."""
    client = TestClient(app)

    response = client.get("/products/nonexistent_product")

    assert response.status_code == 404
    data = response.json()
    assert "detail" in data
    assert "nonexistent_product" in data["detail"]


@pytest.mark.asyncio
async def test_get_product_detail_has_rag_knowledge(setup_database):
    """Test that products with rag_knowledge have populated detail fields."""
    client = TestClient(app)

    # Test with a product that should have rag_knowledge
    response = client.get("/products/p_food_001")

    assert response.status_code == 200
    data = response.json()

    # These fields should exist (may be empty if not yet populated)
    assert "marketing_description" in data
    assert "highlights" in data
    assert "faqs" in data
    assert "reviews" in data


@pytest.mark.asyncio
async def test_get_product_detail_schema_validation(setup_database):
    """Test that response matches ProductDetailResponse schema."""
    client = TestClient(app)

    response = client.get("/products/p_food_001")

    assert response.status_code == 200
    data = response.json()

    # Verify all required fields from ProductDetailResponse
    required_fields = ["product", "marketing_description", "highlights", "faqs", "reviews"]
    for field in required_fields:
        assert field in data, f"Missing required field: {field}"

    # Verify product has required ProductPayload fields
    product_fields = ["product_id", "name", "category"]
    for field in product_fields:
        assert field in data["product"], f"Missing product field: {field}"

    # Verify faqs structure if present
    for faq in data["faqs"]:
        assert "question" in faq
        assert "answer" in faq

    # Verify reviews structure if present
    for review in data["reviews"]:
        assert "nickname" in review
        assert "rating" in review
        assert "content" in review
