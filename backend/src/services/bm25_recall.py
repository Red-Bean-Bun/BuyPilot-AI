"""BM25 keyword recall — startup-built in-memory index for Chinese e-commerce chunks."""

from __future__ import annotations

import logging
from dataclasses import dataclass

import jieba
from rank_bm25 import BM25Okapi

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class Bm25Hit:
    chunk_id: str
    product_id: str
    score: float


class BM25Index:
    """In-memory BM25 index built from product_chunks at startup.

    Thread-safe for reads after build. build_from_db is async because it reads from DB.
    If build fails, search returns empty list (graceful degradation).
    """

    def __init__(self) -> None:
        self._bm25: BM25Okapi | None = None
        self._chunk_ids: list[str] = []
        self._product_ids: list[str] = []
        self._ready = False

    @property
    def ready(self) -> bool:
        return self._ready

    async def build_from_db(self) -> None:
        """Build BM25 index from all product_chunks in DB. Call once at startup."""
        from sqlmodel import text
        from sqlmodel.ext.asyncio.session import AsyncSession

        from src.repos.database import get_async_engine

        try:
            async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
                rows = (
                    await session.exec(
                        text("SELECT id, product_id, chunk_text FROM product_chunks WHERE embedding IS NOT NULL")
                    )
                ).all()

            if not rows:
                logger.warning("BM25Index: no chunks found, index not built")
                return

            self._chunk_ids = [r[0] for r in rows]
            self._product_ids = [r[1] for r in rows]
            tokenized = [list(jieba.cut_for_search(r[2])) for r in rows]
            self._bm25 = BM25Okapi(tokenized)
            self._ready = True
            logger.info("BM25Index: built with %d chunks", len(rows))
        except Exception as e:
            logger.error("BM25Index: build failed, falling back to vector-only: %s", e)

    async def ensure_ready(self) -> None:
        """Lazy init: build index from DB if not yet ready. Idempotent."""
        if not self._ready:
            logger.info("BM25Index: lazy init triggered, building index...")
            await self.build_from_db()

    def search(self, query: str, limit: int = 200) -> list[Bm25Hit]:
        """Return top-k BM25 hits. Returns empty if index not ready."""
        if not self._ready or self._bm25 is None:
            return []

        tokens = list(jieba.cut_for_search(query))
        scores = self._bm25.get_scores(tokens)

        # Get top-k indices by score
        top_indices = sorted(range(len(scores)), key=lambda i: scores[i], reverse=True)[:limit]

        return [
            Bm25Hit(
                chunk_id=self._chunk_ids[i],
                product_id=self._product_ids[i],
                score=float(scores[i]),
            )
            for i in top_indices
            if scores[i] > 0
        ]


# Module-level singleton
bm25_index = BM25Index()
