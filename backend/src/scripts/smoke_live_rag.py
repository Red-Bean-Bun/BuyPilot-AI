"""Live RAG smoke check.

Run from backend/:
    .venv/bin/python -m src.scripts.smoke_live_rag
"""

from __future__ import annotations

import asyncio
import json
import sys

from src.services.product_ingest import chunk_embedding_stats
from src.runtime.pipeline import chat_stream
from src.services.embedding import embed_text
from src.types.schemas import ChatStreamRequest

EXPECTED_EMBEDDING_DIMENSIONS = 1024


async def run_checks() -> None:
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

    events = [
        event
        async for event in chat_stream(
            "sess_smoke_live_rag",
            ChatStreamRequest(message="推荐适合油皮的洗面奶，200元以内，日常护肤"),
        )
    ]
    tags = [event.event for event in events]
    product_events = [event for event in events if event.event == "product_card"]
    first_evidence = product_events[0].evidence[0] if product_events and product_events[0].evidence else None
    print(
        json.dumps(
            {
                "check": "chat_stream",
                "events": tags,
                "product_count": len(product_events),
                "has_criteria": "criteria_card" in tags,
                "has_decision": "final_decision" in tags,
                "first_evidence_source_id": first_evidence.source_id if first_evidence else None,
                "first_evidence_chars": len(first_evidence.snippet) if first_evidence else 0,
            },
            ensure_ascii=False,
        )
    )


def main() -> None:
    asyncio.run(run_checks())


if __name__ == "__main__":
    main()
