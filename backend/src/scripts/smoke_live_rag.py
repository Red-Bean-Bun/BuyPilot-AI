"""Live RAG smoke check.

Run from backend/:
    .venv/bin/python -m src.scripts.smoke_live_rag
"""

from __future__ import annotations

import asyncio
import json
import os
import sys
import uuid

from src.runtime.pipeline import chat_stream
from src.services.embedding import embed_text
from src.services.fallbacks import get_fallback_events
from src.services.product_ingest import chunk_embedding_stats
from src.types.schemas import ChatStreamRequest


def _check_live_provider() -> None:
    """Fail fast if smoke would use mock/fake providers."""
    if "pytest" in sys.modules:
        return
    bailian_url = os.getenv("BAILIAN_BASE_URL")
    bailian_key = os.getenv("BAILIAN_API_KEY")
    if not bailian_url or not bailian_key:
        raise SystemExit(
            "SMOKE GATE FAILED: BAILIAN_BASE_URL/BAILIAN_API_KEY not configured. "
            "Live smoke requires a real AI provider."
        )
    if bailian_key == "test-key":
        raise SystemExit(
            "SMOKE GATE FAILED: BAILIAN_API_KEY is set to mock value 'test-key'. Use real credentials for live smoke."
        )


EXPECTED_EMBEDDING_DIMENSIONS = 1024


async def run_checks() -> None:
    _check_live_provider()
    index_stats = await chunk_embedding_stats()
    index_ok = (
        index_stats["embedded_chunks"] > 0 and index_stats["embedding_dimensions"] == EXPECTED_EMBEDDING_DIMENSIONS
    )
    print(json.dumps({"check": "embedding_index", "ok": index_ok, **index_stats}, ensure_ascii=False))
    if not index_ok:
        sys.exit(1)

    vector = await embed_text("油皮洗面奶 200元以内 日常护肤")
    embedding_ok = len(vector) == EXPECTED_EMBEDDING_DIMENSIONS
    print(
        json.dumps(
            {"check": "embedding", "ok": embedding_ok, "dimensions": len(vector)},
            ensure_ascii=False,
        )
    )
    if not embedding_ok:
        sys.exit(1)

    session_id = f"sess_smoke_live_rag_{uuid.uuid4().hex[:8]}"
    events = [
        event
        async for event in chat_stream(
            session_id,
            ChatStreamRequest(
                message="推荐适合油皮的洗面奶，200元以内，日常护肤",
                client_turn_id=f"turn_smoke_{uuid.uuid4().hex[:8]}",
            ),
        )
    ]
    tags = [event.event for event in events]
    product_events = [event for event in events if event.event == "product_card"]
    first_evidence = product_events[0].evidence[0] if product_events and product_events[0].evidence else None
    fallback_events = get_fallback_events()
    critical_fallbacks = [event for event in fallback_events if not str(event.get("component", "")).startswith("llm.")]
    evidence_ok = bool(product_events) and all(event.evidence for event in product_events)
    chat_ok = (
        len(product_events) >= 1
        and evidence_ok
        and "criteria_card" in tags
        and "final_decision" in tags
        and "error" not in tags
        and not critical_fallbacks
    )
    print(
        json.dumps(
            {
                "check": "chat_stream",
                "ok": chat_ok,
                "session_id": session_id,
                "events": tags,
                "product_count": len(product_events),
                "has_criteria": "criteria_card" in tags,
                "has_decision": "final_decision" in tags,
                "has_error": "error" in tags,
                "evidence_ok": evidence_ok,
                "first_evidence_source_id": first_evidence.source_id if first_evidence else None,
                "first_evidence_chars": len(first_evidence.snippet) if first_evidence else 0,
                "fallback_events": fallback_events,
                "critical_fallbacks": critical_fallbacks,
            },
            ensure_ascii=False,
        )
    )
    if not chat_ok:
        sys.exit(1)


def main() -> None:
    asyncio.run(run_checks())


if __name__ == "__main__":
    main()
