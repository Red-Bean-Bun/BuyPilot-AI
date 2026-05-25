"""Rebuild DB chunk embeddings from the official ecommerce dataset.

Run from backend/:
    .venv/bin/python -m src.scripts.reindex_embeddings
"""

from __future__ import annotations

import json
import sys
import argparse
import asyncio

from src.repos.database import drop_stale_pgvector_tables
from src.services.product_ingest import reindex_chunk_embeddings

EXPECTED_DIMENSIONS = 1024


def _print_progress(done: int, total: int) -> None:
    print(
        json.dumps(
            {"check": "reindex_embeddings", "phase": "embedding", "embedded": done, "total": total},
            ensure_ascii=False,
        ),
        flush=True,
    )


def main() -> None:
    parser = argparse.ArgumentParser(description="Rebuild product chunk embeddings.")
    parser.add_argument(
        "--drop-derived-tables",
        action="store_true",
        help="Explicitly drop stale product_chunks/evidence_links when migrating old pgvector schemas.",
    )
    args = parser.parse_args()
    asyncio.run(_run_reindex(drop_derived_tables=args.drop_derived_tables))


async def _run_reindex(*, drop_derived_tables: bool) -> None:
    dropped = await drop_stale_pgvector_tables() if drop_derived_tables else False
    stats = await reindex_chunk_embeddings(progress=_print_progress)
    ok = (
        stats["chunks"] > 0
        and stats["embedded_chunks"] == stats["chunks"]
        and stats["embedding_dimensions"] == EXPECTED_DIMENSIONS
    )
    print(
        json.dumps(
            {"check": "reindex_embeddings", "ok": ok, "dropped_derived_tables": dropped, **stats}, ensure_ascii=False
        )
    )
    if not ok:
        sys.exit(1)


if __name__ == "__main__":
    main()
