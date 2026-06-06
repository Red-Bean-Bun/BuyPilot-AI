"""TTL cache for retrieval results with hot-key tracking.

Caches RetrievalOutput by criteria hash to avoid redundant embedding + pgvector
+ rerank calls for identical or similar queries within a short time window.
Hot keys (hit_count >= 3) get extended TTL (10 min vs 5 min default).
"""

from __future__ import annotations

import hashlib
import json
import time
from collections.abc import Mapping
from dataclasses import dataclass, field
from typing import Any

from src.config.settings import get_settings
from src.types.sse_events import CriteriaPayload

_HOT_KEY_THRESHOLD = 3
_HOT_KEY_TTL_SECONDS = 600.0  # 10 minutes


@dataclass
class _CacheEntry:
    """Single cache entry with TTL and access tracking."""

    value: Any
    created_at: float
    ttl_seconds: float
    hit_count: int = 0
    last_accessed_at: float = field(default_factory=time.time)

    def is_expired(self) -> bool:
        return (time.time() - self.created_at) > self.ttl_seconds


class RetrievalCache:
    """In-memory TTL cache with hot-key tracking and frequency-based eviction.

    Cache key is SHA-256 hash of criteria JSON + feedback hash.
    Cache value is RetrievalOutput (products + evidence + trace_details).

    Hot keys (hit_count >= 3) get extended TTL (10 min).
    Eviction priority: expired first → lowest hit_count → earliest last_accessed_at.
    """

    def __init__(self, max_size: int = 128, default_ttl_seconds: float = 300.0) -> None:
        self._cache: dict[str, _CacheEntry] = {}
        self._max_size = max_size
        self._default_ttl = default_ttl_seconds
        self._hits = 0
        self._misses = 0

    def get(self, criteria: CriteriaPayload, feedback: Mapping[str, list[str]] | None) -> Any | None:
        """Retrieve cached result if available and not expired."""
        if not get_settings().retrieval_cache_enabled:
            return None

        key = self._make_key(criteria, feedback)
        entry = self._cache.get(key)
        if entry is None:
            self._misses += 1
            return None
        if entry.is_expired():
            del self._cache[key]
            self._misses += 1
            return None
        self._hits += 1
        entry.hit_count += 1
        entry.last_accessed_at = time.time()
        # Extend TTL for hot keys
        if entry.hit_count >= _HOT_KEY_THRESHOLD and entry.ttl_seconds < _HOT_KEY_TTL_SECONDS:
            entry.ttl_seconds = _HOT_KEY_TTL_SECONDS
            entry.created_at = time.time()
        return entry.value

    def set(
        self,
        criteria: CriteriaPayload,
        feedback: Mapping[str, list[str]] | None,
        value: Any,
        ttl_seconds: float | None = None,
    ) -> None:
        """Store result in cache with TTL."""
        if not get_settings().retrieval_cache_enabled:
            return

        key = self._make_key(criteria, feedback)
        ttl = ttl_seconds if ttl_seconds is not None else self._default_ttl

        # Evict if at capacity
        if len(self._cache) >= self._max_size and key not in self._cache:
            self._evict_lowest_hit()

        self._cache[key] = _CacheEntry(
            value=value,
            created_at=time.time(),
            ttl_seconds=ttl,
            hit_count=0,
            last_accessed_at=time.time(),
        )

    def clear(self) -> None:
        """Clear all cache entries."""
        self._cache.clear()
        self._hits = 0
        self._misses = 0

    def stats(self) -> dict[str, Any]:
        """Return cache statistics including hot keys."""
        total = self._hits + self._misses
        hit_rate = self._hits / total if total > 0 else 0.0
        hot_keys = [k[:16] for k, v in self._cache.items() if v.hit_count >= _HOT_KEY_THRESHOLD]
        return {
            "total_keys": len(self._cache),
            "hits": self._hits,
            "misses": self._misses,
            "hit_rate": round(hit_rate, 3),
            "hot_keys": hot_keys,
        }

    def _make_key(self, criteria: CriteriaPayload, feedback: Mapping[str, list[str]] | None) -> str:
        """Generate cache key from criteria + feedback."""
        criteria_json = criteria.model_dump_json()
        feedback_json = json.dumps(feedback or {}, sort_keys=True, default=str)
        combined = f"{criteria_json}|{feedback_json}"
        return hashlib.sha256(combined.encode()).hexdigest()

    def _evict_lowest_hit(self) -> None:
        """Evict expired entries in batch; if none expired, evict lowest-hit entry."""
        expired_keys = [k for k, v in self._cache.items() if v.is_expired()]
        if expired_keys:
            for key in expired_keys:
                del self._cache[key]
            return

        if self._cache:
            victim = min(self._cache, key=lambda k: (self._cache[k].hit_count, self._cache[k].last_accessed_at))
            del self._cache[victim]


# Global cache instance
_retrieval_cache = RetrievalCache()


def get_retrieval_cache() -> RetrievalCache:
    """Get the global retrieval cache instance."""
    return _retrieval_cache
