"""Rebuild DB chunk embeddings from the official ecommerce dataset.

Run from backend/:
    .venv/bin/python -m src.scripts.reindex_embeddings
"""

from __future__ import annotations

import json
import sys

from src.repos.ingest import reindex_chunk_embeddings

EXPECTED_DIMENSIONS = 1024


def main() -> None:
    stats = reindex_chunk_embeddings()
    ok = (
        stats["chunks"] > 0
        and stats["embedded_chunks"] == stats["chunks"]
        and stats["embedding_dimensions"] == EXPECTED_DIMENSIONS
    )
    print(json.dumps({"check": "reindex_embeddings", "ok": ok, **stats}, ensure_ascii=False))
    if not ok:
        sys.exit(1)


if __name__ == "__main__":
    main()
