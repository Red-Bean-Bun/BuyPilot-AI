"""Live RAG smoke check.

Run from backend/:
    .venv/bin/python -m src.scripts.smoke_live_rag
"""

from __future__ import annotations

import asyncio
import json
import os
import sys
import time
import uuid
from collections.abc import Awaitable
from typing import TypeVar

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


def _check_postgres() -> None:
    """Fail fast if smoke would not exercise pgvector."""
    if "pytest" in sys.modules:
        return
    url = os.getenv("DATABASE_URL", "")
    if "postgresql" not in url:
        raise SystemExit(f"SMOKE GATE FAILED: DATABASE_URL must use PostgreSQL + pgvector. Got: {url[:80]}...")


EXPECTED_EMBEDDING_DIMENSIONS = 1024
INDEX_TIMEOUT_SECONDS = 20
EMBEDDING_TIMEOUT_SECONDS = 45
CHAT_TIMEOUT_SECONDS = 150
T = TypeVar("T")


async def run_checks() -> None:
    _check_live_provider()
    _check_postgres()
    # Print dialect diagnostic so reviewers can confirm pgvector is active
    from src.repos.database import get_async_engine

    _print_json({"check": "database_engine", "dialect": get_async_engine().dialect.name})
    index_stats = await _run_step(
        "embedding_index",
        chunk_embedding_stats(),
        timeout_seconds=INDEX_TIMEOUT_SECONDS,
    )
    index_ok = (
        index_stats["embedded_chunks"] > 0 and index_stats["embedding_dimensions"] == EXPECTED_EMBEDDING_DIMENSIONS
    )
    _print_json({"check": "embedding_index", "ok": index_ok, **index_stats})
    if not index_ok:
        sys.exit(1)

    vector = await _run_step(
        "embedding",
        embed_text("油皮洗面奶 200元以内 日常护肤"),
        timeout_seconds=EMBEDDING_TIMEOUT_SECONDS,
    )
    embedding_ok = len(vector) == EXPECTED_EMBEDDING_DIMENSIONS
    _print_json({"check": "embedding", "ok": embedding_ok, "dimensions": len(vector)})
    if not embedding_ok:
        sys.exit(1)

    session_id = f"sess_smoke_live_rag_{uuid.uuid4().hex[:8]}"
    events = await _run_step(
        "chat_stream",
        _collect_chat_events(
            session_id,
            ChatStreamRequest(
                message="推荐适合油皮的洗面奶，200元以内，日常护肤",
                client_turn_id=f"turn_smoke_{uuid.uuid4().hex[:8]}",
            ),
        ),
        timeout_seconds=CHAT_TIMEOUT_SECONDS,
    )
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
    _print_json(
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
        }
    )
    if not chat_ok:
        sys.exit(1)


async def _collect_chat_events(session_id: str, request: ChatStreamRequest):
    return [event async for event in chat_stream(session_id, request)]


async def _run_step(name: str, awaitable: Awaitable[T], *, timeout_seconds: float) -> T:
    started = time.perf_counter()
    _print_json({"check": name, "stage": "start", "timeout_seconds": timeout_seconds})
    try:
        result = await asyncio.wait_for(awaitable, timeout=timeout_seconds)
    except TimeoutError as exc:
        _print_json(
            {
                "check": name,
                "ok": False,
                "stage": "timeout",
                "timeout_seconds": timeout_seconds,
                "duration_ms": round((time.perf_counter() - started) * 1000, 2),
            }
        )
        raise SystemExit(1) from exc
    except Exception as exc:
        _print_json(
            {
                "check": name,
                "ok": False,
                "stage": "failed",
                "error_type": type(exc).__name__,
                "error": str(exc),
                "duration_ms": round((time.perf_counter() - started) * 1000, 2),
            }
        )
        raise SystemExit(1) from exc
    _print_json(
        {
            "check": name,
            "stage": "complete",
            "duration_ms": round((time.perf_counter() - started) * 1000, 2),
        }
    )
    return result


def _print_json(payload: dict) -> None:
    print(json.dumps(payload, ensure_ascii=False), flush=True)


def main() -> None:
    asyncio.run(run_checks())


if __name__ == "__main__":
    main()
