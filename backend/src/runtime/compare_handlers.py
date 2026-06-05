"""Compare intent handler.

Follows the cart_handlers.py pattern:
  thinking → resolve targets → build comparison → emit compare_card → stream narration → done
"""

from __future__ import annotations

import logging
import uuid
from collections.abc import AsyncGenerator
from typing import TYPE_CHECKING

from src.config import user_messages as msg
from src.runtime.message_rules import is_compare_phrase, resolve_compare_targets
from src.runtime.streaming import StreamContext, now_ms
from src.services.catalog import get_product
from src.services.compare import build_comparison
from src.services.conversation_state import (
    get_previous_deck_id,
    get_previous_product_ids,
)
from src.services.llm_client import stream_comparison_narration
from src.types.schemas import ChatStreamRequest, IntentResult
from src.types.sse_events import (
    CompareCardEvent,
    ClarificationEvent,
    SSEEventBase,
)

if TYPE_CHECKING:
    pass

logger = logging.getLogger(__name__)

_MAX_COMPARE_PRODUCTS = 4


async def handle_compare(
    ctx: StreamContext,
    body: ChatStreamRequest,
    intent: IntentResult,
) -> AsyncGenerator[SSEEventBase, None]:
    """Handle compare intent: build comparison and emit compare_card event."""

    # 1. Thinking
    yield ctx.thinking("comparing", msg.THINKING_COMPARING)
    ctx.ensure_active()

    # 2. Resolve product IDs to compare
    product_ids = await _resolve_compare_product_ids(ctx.session_id, intent, body.message)
    if len(product_ids) < 2:
        yield ClarificationEvent(
            session_id=ctx.session_id,
            turn_id=ctx.turn_id,
            seq=ctx.seq.next(),
            event_id=ctx.seq.event_id(),
            node_id=f"compare_{ctx.turn_id}",
            display_mode="inline_card",
            question=msg.COMPARE_CLARIFY,
            required_slots=["compare_products"],
            suggested_options=["对比第一个和第二个", "对比前两款"],
        )
        yield ctx.done()
        return

    # Cap at max products
    product_ids = product_ids[:_MAX_COMPARE_PRODUCTS]

    # 3. Fetch products from catalog
    products = [p for pid in product_ids if (p := get_product(pid)) is not None]
    if len(products) < 2:
        yield ClarificationEvent(
            session_id=ctx.session_id,
            turn_id=ctx.turn_id,
            seq=ctx.seq.next(),
            event_id=ctx.seq.event_id(),
            node_id=f"compare_{ctx.turn_id}",
            display_mode="inline_card",
            question=msg.COMPARE_CLARIFY,
            required_slots=["compare_products"],
            suggested_options=[],
        )
        yield ctx.done()
        return

    # 4. Gather context
    deck_id = await get_previous_deck_id(ctx.session_id)
    from src.services.conversation_state import get_previous_criteria

    criteria = await get_previous_criteria(ctx.session_id)
    category = criteria.category if criteria else (products[0].category if products else None)
    compare_id = f"cmp_{uuid.uuid4().hex[:12]}"

    # 5. Build comparison (service layer)
    ctx.ensure_active()
    comparison = await build_comparison(
        product_ids=[p.product_id for p in products],
        category=category,
        source_deck_id=deck_id,
        criteria=criteria,
        compare_id=compare_id,
    )

    # 6. Emit CompareCardEvent
    yield CompareCardEvent(
        session_id=ctx.session_id,
        turn_id=ctx.turn_id,
        seq=ctx.seq.next(),
        event_id=ctx.seq.event_id(),
        node_id=f"compare_{ctx.turn_id}",
        created_at_ms=now_ms(),
        deck_id=deck_id,
        compare_id=comparison.compare_id,
        source_deck_id=comparison.source_deck_id,
        mode=comparison.mode,
        focus=comparison.focus,
        products=comparison.products,
        axes=comparison.axes,
        winner_product_id=comparison.winner_product_id,
        winner_reason=comparison.winner_reason,
        tradeoffs=comparison.tradeoffs,
        risk_notes=comparison.risk_notes,
        confidence=comparison.confidence,
    )

    # 7. Stream LLM narration (text_delta)
    message_id = f"compare_narration_{ctx.turn_id}"
    full_text = ""
    try:
        async for delta in stream_comparison_narration(
            products=comparison.products,
            axes=comparison.axes,
            winner_product_id=comparison.winner_product_id,
            winner_reason=comparison.winner_reason,
            tradeoffs=comparison.tradeoffs,
            risk_notes=comparison.risk_notes,
            mode=comparison.mode,
        ):
            if delta:
                full_text += delta
                from src.types.sse_events import TextDeltaEvent

                yield TextDeltaEvent(
                    session_id=ctx.session_id,
                    turn_id=ctx.turn_id,
                    seq=ctx.seq.next(),
                    event_id=ctx.seq.event_id(),
                    node_id=f"compare_narration_{ctx.turn_id}",
                    created_at_ms=now_ms(),
                    display_mode="inline_text",
                    message_id=message_id,
                    delta=delta,
                    done=False,
                )
    except Exception:
        logger.warning("Comparison narration LLM failed; using fallback text.", exc_info=True)

    # Send done=True for text_delta
    if full_text:
        from src.types.sse_events import TextDeltaEvent

        yield TextDeltaEvent(
            session_id=ctx.session_id,
            turn_id=ctx.turn_id,
            seq=ctx.seq.next(),
            event_id=ctx.seq.event_id(),
            node_id=f"compare_narration_{ctx.turn_id}",
            created_at_ms=now_ms(),
            display_mode="inline_text",
            message_id=message_id,
            delta="",
            done=True,
        )

    # 8. Done
    yield ctx.done()


async def _resolve_compare_product_ids(
    session_id: str,
    intent: IntentResult,
    message: str,
) -> list[str]:
    """Resolve which products to compare from intent, message, or previous deck."""

    # Priority 1: LLM-extracted compare_product_ids
    if intent.compare_product_ids:
        previous_ids = await get_previous_product_ids(session_id)
        if previous_ids:
            resolved = resolve_compare_targets(
                " ".join(intent.compare_product_ids), previous_ids
            )
            if len(resolved) >= 2:
                return resolved

    # Priority 2: Ordinal references in message
    previous_ids = await get_previous_product_ids(session_id)
    if previous_ids:
        resolved = resolve_compare_targets(message, previous_ids)
        if len(resolved) >= 2:
            return resolved

        # Priority 3: If compare phrase detected but no ordinals, default to first 2
        if is_compare_phrase(message) and len(previous_ids) >= 2:
            return previous_ids[:2]

    return []
