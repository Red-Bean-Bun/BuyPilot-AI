import uuid

import pytest

from src.repos.audit import list_audit_events
from src.runtime.pipeline import chat_stream
from src.services.cart import add_product_to_cart, get_session_cart
from src.services.llm_task_payloads import normalize_intent_payload
from src.services.message_rules import maybe_checkout_intent
from src.types.schemas import ChatStreamRequest, IntentResult
from src.types.sse_events import SSEEventBase


@pytest.fixture(autouse=True)
async def _seed_products_for_checkout(seeded_products):
    del seeded_products


@pytest.mark.asyncio
async def test_checkout_empty_cart_fails_preview():
    session_id = _session_id()

    events = await _collect_chat("下单", session_id=session_id, turn_id="turn_checkout_empty")
    action = _single_cart_action(events)

    assert action.action == "checkout_preview"
    assert action.status == "failed"
    assert action.cart is not None
    assert action.cart.total_items == 0
    assert await list_audit_events(turn_id="turn_checkout_empty", action="cart.checkout_previewed") == []


@pytest.mark.asyncio
async def test_checkout_preview_from_non_empty_cart_records_audit():
    session_id = _session_id()
    await add_product_to_cart(session_id, "p_beauty_011", quantity=2)

    events = await _collect_chat("就买这个", session_id=session_id, turn_id="turn_checkout_preview")
    action = _single_cart_action(events)
    audit_rows = await list_audit_events(turn_id="turn_checkout_preview", action="cart.checkout_previewed")

    assert action.action == "checkout_preview"
    assert action.status == "success"
    assert action.cart is not None
    assert action.cart.total_items == 2
    assert [(item.product_id, item.quantity) for item in action.cart.items] == [("p_beauty_011", 2)]
    assert len(audit_rows) == 1
    assert audit_rows[0].audit_metadata == {
        "source": "chat_intent",
        "total_items": 2,
        "total_price": action.cart.total_price,
        "product_ids": ["p_beauty_011"],
        "real_payment": False,
    }


@pytest.mark.asyncio
async def test_checkout_confirm_records_audit_and_keeps_cart():
    session_id = _session_id()
    await add_product_to_cart(session_id, "p_beauty_011", quantity=1)

    events = await _collect_chat("确认", session_id=session_id, turn_id="turn_checkout_confirm")
    action = _single_cart_action(events)
    cart = await get_session_cart(session_id)
    audit_rows = await list_audit_events(turn_id="turn_checkout_confirm", action="cart.checkout_confirmed")

    assert action.action == "checkout_confirm"
    assert action.status == "success"
    assert [(item.product_id, item.quantity) for item in cart.items] == [("p_beauty_011", 1)]
    assert len(audit_rows) == 1
    assert audit_rows[0].audit_metadata["real_payment"] is False
    assert audit_rows[0].audit_metadata["product_ids"] == ["p_beauty_011"]


@pytest.mark.asyncio
async def test_checkout_cancel_records_audit_and_keeps_cart():
    session_id = _session_id()
    await add_product_to_cart(session_id, "p_beauty_011", quantity=1)

    events = await _collect_chat("算了不买", session_id=session_id, turn_id="turn_checkout_cancel")
    action = _single_cart_action(events)
    cart = await get_session_cart(session_id)
    audit_rows = await list_audit_events(turn_id="turn_checkout_cancel", action="cart.checkout_cancelled")

    assert action.action == "checkout_cancel"
    assert action.status == "success"
    assert [(item.product_id, item.quantity) for item in cart.items] == [("p_beauty_011", 1)]
    assert len(audit_rows) == 1
    assert audit_rows[0].audit_metadata["total_items"] == 1
    assert audit_rows[0].audit_metadata["real_payment"] is False


@pytest.mark.asyncio
async def test_short_checkout_confirm_without_cart_keeps_continue_semantics():
    session_id = _session_id()

    events = await _collect_chat("确认", session_id=session_id, turn_id="turn_confirm_no_cart")

    assert not [event for event in events if event.event == "cart_action"]


def test_checkout_keywords_do_not_hijack_confirm_criteria():
    assert maybe_checkout_intent("确认标准") is None
    assert maybe_checkout_intent("确认一下预算") is None
    assert maybe_checkout_intent("怎么下单") is None

    normalized = normalize_intent_payload({"intent": "确认", "is_shopping_related": True})
    assert IntentResult.model_validate(normalized).intent == "continue"


def test_checkout_confirm_matches_with_trailing_punctuation():
    """Users naturally add punctuation or filler words after confirm phrases."""
    result_period = maybe_checkout_intent("确认了。")
    assert result_period is not None
    assert result_period.intent == "checkout_confirm"

    result_filler = maybe_checkout_intent("就这样吧")
    assert result_filler is not None
    assert result_filler.intent == "checkout_confirm"

    result_exclaim = maybe_checkout_intent("确认！")
    assert result_exclaim is not None
    assert result_exclaim.intent == "checkout_confirm"

    result_intent_copy = maybe_checkout_intent("确认购买意向")
    assert result_intent_copy is not None
    assert result_intent_copy.intent == "checkout_confirm"

    # Bare forms still work
    result_bare = maybe_checkout_intent("确认")
    assert result_bare is not None
    assert result_bare.intent == "checkout_confirm"


async def _collect_chat(message: str, *, session_id: str, turn_id: str) -> list[SSEEventBase]:
    return [
        event
        async for event in chat_stream(
            session_id,
            ChatStreamRequest(message=message, client_turn_id=turn_id),
        )
    ]


def _single_cart_action(events: list[SSEEventBase]):
    actions = [event for event in events if event.event == "cart_action"]
    assert len(actions) == 1
    assert not [event for event in events if event.event == "error"]
    return actions[0]


def _session_id() -> str:
    return f"sess_checkout_{uuid.uuid4().hex[:8]}"
