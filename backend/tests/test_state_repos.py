import pytest

import src.config.settings as settings_module
from src.repos import cart_items, conversations, feedbacks
from src.runtime.pipeline import chat_stream
from src.types.schemas import ChatStreamRequest
from src.types.sse_events import Constraints, CriteriaPayload


@pytest.fixture
def temp_database(monkeypatch, tmp_path):
    monkeypatch.setenv("DATABASE_URL", f"sqlite:///{tmp_path / 'state.db'}")
    settings_module._settings = None
    yield
    settings_module._settings = None


def test_conversation_repo_persists_latest_criteria(temp_database):
    criteria = CriteriaPayload(
        criteria_id="c_state_001",
        category="美妆护肤",
        summary="油性肌肤，200元内",
        constraints=Constraints(skin_type="油性", budget_max=200),
    )

    conversations.save_turn("sess_state", criteria, ["p_beauty_011"], user_message="推荐洗面奶")
    conversations._LAST_CRITERIA.clear()
    conversations._LAST_PRODUCT_IDS.clear()

    restored = conversations.get_last_criteria("sess_state")

    assert restored is not None
    assert restored.criteria_id == "c_state_001"
    assert restored.constraints.budget_max == 200
    assert conversations.get_last_product_ids("sess_state") == ["p_beauty_011"]


def test_feedback_repo_persists_and_extracts_avoid_terms(temp_database):
    feedbacks.add_feedback("sess_feedback", action="feedback", reason="不要含酒精")
    feedbacks._FEEDBACKS.clear()

    records = feedbacks.get_session_feedbacks("sess_feedback")
    extracted = feedbacks.extract_feedback_from_session("sess_feedback")

    assert len(records) == 1
    assert records[0].reason == "不要含酒精"
    assert extracted["avoid_traits"] == ["酒精"]


def test_feedback_repo_extracts_avoid_products_and_brand_terms(temp_database):
    feedbacks.add_feedback("sess_feedback_filter", action="not_interested", product_id="p_beauty_011")
    feedbacks.add_feedback("sess_feedback_filter", action="feedback", reason="除了耐克还有什么")
    feedbacks._FEEDBACKS.clear()

    extracted = feedbacks.extract_feedback_from_session("sess_feedback_filter")

    assert extracted["avoid_products"] == ["p_beauty_011"]
    assert extracted["avoid_traits"] == ["耐克"]


def test_cart_repo_persists_add_and_view(temp_database):
    cart_items.add_to_cart("sess_cart", "p_beauty_011")
    cart_items.add_to_cart("sess_cart", "p_beauty_011", quantity=2)
    cart_items._CARTS.clear()

    cart = cart_items.get_cart("sess_cart")

    assert cart.total_items == 3
    assert len(cart.items) == 1
    assert cart.items[0].product_id == "p_beauty_011"
    assert cart.items[0].quantity == 3


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

    conversations._LAST_CRITERIA.clear()
    conversations._LAST_PRODUCT_IDS.clear()
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
async def test_pipeline_cart_action_persists_to_database(temp_database):
    events = [
        event
        async for event in chat_stream(
            "sess_pipeline_cart",
            ChatStreamRequest(message="把这个加到购物车"),
        )
    ]
    cart_items._CARTS.clear()

    assert any(event.event == "cart_action" and event.action == "add" and event.status == "success" for event in events)
    cart = cart_items.get_cart("sess_pipeline_cart")
    assert cart.total_items == 1
    assert cart.items[0].product_id == "p_beauty_011"


@pytest.mark.asyncio
async def test_pipeline_feedback_excludes_disliked_product(temp_database):
    first_events = [
        event
        async for event in chat_stream(
            "sess_feedback_pipeline",
            ChatStreamRequest(message="推荐适合油皮的洗面奶，200元以内，日常护肤"),
        )
    ]
    first_product_id = [event.product.product_id for event in first_events if event.event == "product_card"][0]

    feedbacks.add_feedback(
        "sess_feedback_pipeline",
        action="not_interested",
        product_id=first_product_id,
        reason="不喜欢这个",
    )
    second_events = [
        event
        async for event in chat_stream(
            "sess_feedback_pipeline",
            ChatStreamRequest(message="再推荐适合油皮的洗面奶，200元以内，日常护肤"),
        )
    ]
    second_product_ids = [event.product.product_id for event in second_events if event.event == "product_card"]

    assert second_product_ids
    assert first_product_id not in second_product_ids
