import pytest
from sqlalchemy import text
from sqlalchemy.exc import SQLAlchemyError

import src.config.settings as settings_module
from src.services import cart
from src.repos import cart_items, conversations, feedbacks
from src.repos.database import create_db_and_tables, get_async_engine
from src.runtime.pipeline import chat_stream
from src.types.schemas import ChatStreamRequest
from src.types.sse_events import Constraints, CriteriaPayload


@pytest.fixture
async def temp_database(monkeypatch, tmp_path):
    monkeypatch.setenv("DATABASE_URL", f"sqlite:///{tmp_path / 'state.db'}")
    settings_module._settings = None
    from src.services.product_ingest import seed_products_if_needed

    await seed_products_if_needed()
    yield
    settings_module._settings = None


@pytest.mark.asyncio
async def test_conversation_repo_persists_latest_criteria(temp_database):
    criteria = CriteriaPayload(
        criteria_id="c_state_001",
        category="美妆护肤",
        summary="油性肌肤，200元内",
        constraints=Constraints(skin_type="油性", budget_max=200),
    )

    await conversations.save_turn("sess_state", criteria, ["p_beauty_011"], user_message="推荐洗面奶")

    restored = await conversations.get_last_criteria("sess_state")

    assert restored is not None
    assert restored.criteria_id == "c_state_001"
    assert restored.constraints.budget_max == 200
    assert await conversations.get_last_product_ids("sess_state") == ["p_beauty_011"]


@pytest.mark.asyncio
async def test_cart_repo_persists_add_and_view(temp_database):
    await cart_items.add_to_cart("sess_cart", "p_beauty_011")
    await cart_items.add_to_cart("sess_cart", "p_beauty_011", quantity=2)

    cart = await cart_items.get_cart("sess_cart")

    assert cart.total_items == 3
    assert len(cart.items) == 1
    assert cart.items[0].product_id == "p_beauty_011"
    assert cart.items[0].quantity == 3


@pytest.mark.asyncio
async def test_cart_repo_updates_and_removes_items(temp_database):
    await cart_items.add_to_cart("sess_cart_update", "p_beauty_011")

    updated = await cart_items.update_cart_quantity("sess_cart_update", "p_beauty_011", quantity=4)
    assert updated is not None
    assert updated.quantity == 4

    removed = await cart_items.remove_from_cart("sess_cart_update", "p_beauty_011")
    assert removed is not None
    cart = await cart_items.get_cart("sess_cart_update")
    assert cart.items == []


@pytest.mark.asyncio
async def test_cart_unique_index_merges_existing_duplicate_rows(monkeypatch, tmp_path):
    monkeypatch.setenv("DATABASE_URL", f"sqlite:///{tmp_path / 'legacy_cart.db'}")
    settings_module._settings = None
    engine = get_async_engine()
    async with engine.begin() as conn:
        await conn.execute(
            text(
                """
                CREATE TABLE cart_items (
                    id VARCHAR PRIMARY KEY,
                    session_id VARCHAR NOT NULL,
                    product_id VARCHAR NOT NULL,
                    quantity INTEGER NOT NULL,
                    added_at DATETIME NOT NULL
                )
                """
            )
        )
        await conn.execute(
            text(
                """
                INSERT INTO cart_items (id, session_id, product_id, quantity, added_at)
                VALUES
                    ('legacy_1', 'sess_legacy_cart', 'p_beauty_011', 1, '2026-05-30T00:00:00'),
                    ('legacy_2', 'sess_legacy_cart', 'p_beauty_011', 2, '2026-05-31T00:00:00')
                """
            )
        )

    await create_db_and_tables()
    await cart_items.add_to_cart("sess_legacy_cart", "p_beauty_011", quantity=4)

    restored = await cart_items.get_cart("sess_legacy_cart")

    assert len(restored.items) == 1
    assert restored.items[0].quantity == 7
    settings_module._settings = None


@pytest.mark.asyncio
async def test_cart_db_error_is_not_hidden(monkeypatch, tmp_path):
    monkeypatch.delenv("ALLOW_MEMORY_STATE_FALLBACK", raising=False)
    monkeypatch.setenv("DATABASE_URL", f"sqlite:///{tmp_path / 'missing' / 'cart.db'}")
    settings_module._settings = None

    with pytest.raises(SQLAlchemyError):
        await cart.add_product_to_cart("sess_cart_fallback_disabled", "p_beauty_011")

    settings_module._settings = None


@pytest.mark.asyncio
async def test_cart_db_error_raises_even_when_legacy_memory_flag_set(monkeypatch, tmp_path):
    monkeypatch.setenv("ALLOW_MEMORY_STATE_FALLBACK", "1")
    monkeypatch.setenv("DATABASE_URL", f"sqlite:///{tmp_path / 'missing' / 'cart.db'}")
    settings_module._settings = None

    with pytest.raises(SQLAlchemyError):
        await cart.add_product_to_cart("sess_cart_fallback_enabled", "p_beauty_011")
    settings_module._settings = None


@pytest.mark.asyncio
async def test_pipeline_followup_restores_criteria_from_database(temp_database):
    first_events = [
        event
        async for event in chat_stream(
            "sess_followup",
            ChatStreamRequest(message="不要含酒精的防晒霜"),
        )
    ]
    first_criteria = [event.criteria for event in first_events if event.event == "criteria_card"][0]
    assert first_criteria.constraints.ingredient_avoid == ["酒精"]

    second_events = [
        event
        async for event in chat_stream(
            "sess_followup",
            ChatStreamRequest(message="预算降到200"),
        )
    ]
    second_criteria = [event.criteria for event in second_events if event.event == "criteria_card"][0]

    assert second_criteria.constraints.budget_max == 200
    assert second_criteria.constraints.ingredient_avoid == ["酒精"]


@pytest.mark.asyncio
async def test_pipeline_feedback_excludes_disliked_product(temp_database):
    first_events = [
        event
        async for event in chat_stream(
            "sess_feedback_pipeline",
            ChatStreamRequest(message="推荐适合油皮的护肤品，200元以内，日常护肤"),
        )
    ]
    first_product_id = [event.product.product_id for event in first_events if event.event == "product_card"][0]

    await feedbacks.add_feedback(
        "sess_feedback_pipeline",
        action="not_interested",
        product_id=first_product_id,
        reason="不喜欢这个",
    )
    second_events = [
        event
        async for event in chat_stream(
            "sess_feedback_pipeline",
            ChatStreamRequest(message="再推荐适合油皮的护肤品，200元以内，日常护肤"),
        )
    ]
    second_product_ids = [event.product.product_id for event in second_events if event.event == "product_card"]

    assert second_product_ids
    assert first_product_id not in second_product_ids
