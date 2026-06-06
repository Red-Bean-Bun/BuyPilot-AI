"""Cart intent handlers for the chat stream."""

from __future__ import annotations

from collections.abc import AsyncGenerator

from src.config import user_messages as msg
from src.runtime.cart_rules import quantity_from_intent, referenced_product_id
from src.runtime.streaming import StreamContext
from src.services.audit import record_audit_event
from src.services.cart import add_product_to_cart, get_session_cart, remove_product_from_cart, update_product_quantity
from src.types.schemas import CartResponse, ChatStreamRequest, IntentResult
from src.types.sse_events import (
    CartActionEvent,
    CartItemEventPayload,
    CartSummaryPayload,
    ClarificationEvent,
    SSEEventBase,
    now_ms,
)


async def handle_view_cart(
    ctx: StreamContext, body: ChatStreamRequest, intent: IntentResult
) -> AsyncGenerator[SSEEventBase, None]:
    del body, intent
    yield ctx.thinking("generating", msg.THINKING_VIEWING_CART)
    yield await _cart_action_event(ctx, "view", "", 0, "success")
    yield ctx.done()


async def handle_add_to_cart(
    ctx: StreamContext, body: ChatStreamRequest, intent: IntentResult
) -> AsyncGenerator[SSEEventBase, None]:
    product_id = await referenced_product_id(ctx.session_id, intent, body.message)
    if product_id is None:
        async for event in _clarify_cart_target(ctx, msg.CART_CLARIFY_ADD):
            yield event
        yield ctx.done()
        return
    quantity = quantity_from_intent(intent, body.message, default=1)
    try:
        await add_product_to_cart(ctx.session_id, product_id, quantity=quantity)
    except ValueError:
        yield await _cart_action_event(ctx, "add", product_id, 0, "failed")
        yield ctx.done()
        return
    await record_audit_event(
        "cart.item_added",
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        resource_type="cart_item",
        resource_id=product_id,
        metadata={"quantity": quantity, "source": "chat_intent"},
    )
    ctx.ensure_active()
    yield await _cart_action_event(ctx, "add", product_id, quantity, "success")
    yield ctx.done()


async def handle_remove_from_cart(
    ctx: StreamContext, body: ChatStreamRequest, intent: IntentResult
) -> AsyncGenerator[SSEEventBase, None]:
    product_id = await referenced_product_id(ctx.session_id, intent, body.message)
    if product_id is None:
        async for event in _clarify_cart_target(ctx, msg.CART_CLARIFY_REMOVE):
            yield event
        yield ctx.done()
        return
    try:
        item = await remove_product_from_cart(ctx.session_id, product_id)
    except ValueError:
        item = None
    if item is None:
        yield await _cart_action_event(ctx, "remove", product_id, 0, "failed")
        yield ctx.done()
        return
    await record_audit_event(
        "cart.item_removed",
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        resource_type="cart_item",
        resource_id=product_id,
        metadata={"source": "chat_intent"},
    )
    yield await _cart_action_event(ctx, "remove", product_id, 0, "success")
    yield ctx.done()


async def handle_update_cart_quantity(
    ctx: StreamContext, body: ChatStreamRequest, intent: IntentResult
) -> AsyncGenerator[SSEEventBase, None]:
    product_id = await referenced_product_id(ctx.session_id, intent, body.message)
    if product_id is None:
        async for event in _clarify_cart_target(ctx, msg.CART_CLARIFY_UPDATE):
            yield event
        yield ctx.done()
        return
    quantity = quantity_from_intent(intent, body.message, default=1)
    try:
        item = await update_product_quantity(ctx.session_id, product_id, quantity)
    except ValueError:
        item = None
    if item is None:
        yield await _cart_action_event(ctx, "update_quantity", product_id, quantity, "failed")
        yield ctx.done()
        return
    await record_audit_event(
        "cart.quantity_updated",
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        resource_type="cart_item",
        resource_id=product_id,
        metadata={"quantity": quantity, "source": "chat_intent"},
    )
    yield await _cart_action_event(ctx, "update_quantity", product_id, quantity, "success")
    yield ctx.done()


async def handle_checkout_preview(
    ctx: StreamContext, body: ChatStreamRequest, intent: IntentResult
) -> AsyncGenerator[SSEEventBase, None]:
    del body, intent
    yield ctx.thinking("checkout", msg.THINKING_VIEWING_CART)
    cart = await get_session_cart(ctx.session_id)
    status = "success" if cart.total_items > 0 else "failed"
    if status == "success":
        await _record_checkout_audit(ctx, "cart.checkout_previewed", cart)
    yield _cart_action_event_from_cart(ctx, "checkout_preview", "", 0, status, cart)
    yield ctx.done()


async def handle_checkout_confirm(
    ctx: StreamContext, body: ChatStreamRequest, intent: IntentResult
) -> AsyncGenerator[SSEEventBase, None]:
    del body, intent
    yield ctx.thinking("checkout", msg.THINKING_VIEWING_CART)
    cart = await get_session_cart(ctx.session_id)
    status = "success" if cart.total_items > 0 else "failed"
    if status == "success":
        await _record_checkout_audit(ctx, "cart.checkout_confirmed", cart)
    yield _cart_action_event_from_cart(ctx, "checkout_confirm", "", 0, status, cart)
    yield ctx.done()


async def handle_checkout_cancel(
    ctx: StreamContext, body: ChatStreamRequest, intent: IntentResult
) -> AsyncGenerator[SSEEventBase, None]:
    del body, intent
    yield ctx.thinking("checkout", msg.THINKING_VIEWING_CART)
    cart = await get_session_cart(ctx.session_id)
    await _record_checkout_audit(ctx, "cart.checkout_cancelled", cart)
    yield _cart_action_event_from_cart(ctx, "checkout_cancel", "", 0, "success", cart)
    yield ctx.done()


# ── Helpers ──────────────────────────────────────────────────────────────────


async def _clarify_cart_target(ctx: StreamContext, question: str) -> AsyncGenerator[SSEEventBase, None]:
    yield ctx.thinking("clarifying", msg.THINKING_CONFIRM_CART)
    yield ClarificationEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"clarification_{ctx.turn_id}",
        created_at_ms=now_ms(),
        question=question,
        required_slots=["target_product"],
        suggested_options=[],
    )


async def _cart_action_event(
    ctx: StreamContext, action: str, product_id: str, quantity: int, status: str
) -> CartActionEvent:
    cart = _cart_summary_payload(await get_session_cart(ctx.session_id))
    return _cart_action_event_from_summary(ctx, action, product_id, quantity, status, cart)


def _cart_action_event_from_cart(
    ctx: StreamContext,
    action: str,
    product_id: str,
    quantity: int,
    status: str,
    cart: CartResponse,
) -> CartActionEvent:
    return _cart_action_event_from_summary(ctx, action, product_id, quantity, status, _cart_summary_payload(cart))


def _cart_action_event_from_summary(
    ctx: StreamContext,
    action: str,
    product_id: str,
    quantity: int,
    status: str,
    cart: CartSummaryPayload,
) -> CartActionEvent:
    return CartActionEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"cart_{ctx.turn_id}",
        created_at_ms=now_ms(),
        action=action,
        product_id=product_id,
        quantity=quantity,
        status=status,
        cart=cart,
    )


async def _record_checkout_audit(ctx: StreamContext, action: str, cart: CartResponse) -> None:
    await record_audit_event(
        action,
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        resource_type="cart",
        resource_id=ctx.session_id,
        metadata=_checkout_metadata(cart),
    )


def _checkout_metadata(cart: CartResponse) -> dict[str, object]:
    return {
        "source": "chat_intent",
        "total_items": cart.total_items,
        "total_price": cart.total_price,
        "product_ids": [item.product_id for item in cart.items],
        "real_payment": False,
    }


def _cart_summary_payload(cart: CartResponse) -> CartSummaryPayload:
    return CartSummaryPayload(
        items=[
            CartItemEventPayload(
                product_id=item.product_id,
                name=item.name,
                price=item.price,
                quantity=item.quantity,
                added_at=item.added_at,
                product=item.product,
            )
            for item in cart.items
        ],
        total_items=cart.total_items,
        total_price=cart.total_price,
    )
