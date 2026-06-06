"""Tests for enhanced RetrievalCache with hot-key tracking.

Covers:
- hit_count increment on cache hit
- Hot key detection (hit_count >= 3)
- Hot key TTL extension
- Eviction priority: expired → lowest hit_count → earliest last_accessed_at
- stats() output: total_keys, hits, misses, hit_rate, hot_keys
"""

from __future__ import annotations

import time
from unittest.mock import patch

import pytest

from src.services.retrieval_cache import RetrievalCache, _HOT_KEY_THRESHOLD, _HOT_KEY_TTL_SECONDS
from src.types.sse_events import Constraints, CriteriaPayload


def _criteria(category: str = "美妆护肤", product_type: str | None = None) -> CriteriaPayload:
    return CriteriaPayload(
        criteria_id="test_001",
        category=category,
        constraints=Constraints(product_type=product_type),
    )


@pytest.fixture
def cache():
    """Fresh cache with small size for eviction tests."""
    return RetrievalCache(max_size=3, default_ttl_seconds=5.0)


class TestHitCount:
    def test_hit_count_starts_at_zero(self, cache: RetrievalCache):
        cache.set(_criteria(), None, "value1")
        entry = cache._cache[cache._make_key(_criteria(), None)]
        assert entry.hit_count == 0

    def test_hit_count_increments_on_hit(self, cache: RetrievalCache):
        cache.set(_criteria(), None, "value1")
        crit = _criteria()
        cache.get(crit, None)
        cache.get(crit, None)
        entry = cache._cache[cache._make_key(crit, None)]
        assert entry.hit_count == 2

    def test_miss_does_not_increment_hit_count(self, cache: RetrievalCache):
        crit = _criteria()
        cache.get(crit, None)  # miss
        cache.set(crit, None, "value1")
        entry = cache._cache[cache._make_key(crit, None)]
        assert entry.hit_count == 0


class TestHotKeys:
    def test_hot_key_threshold(self, cache: RetrievalCache):
        crit = _criteria()
        cache.set(crit, None, "value1")
        # Hit 3 times to trigger hot
        for _ in range(_HOT_KEY_THRESHOLD):
            cache.get(crit, None)
        s = cache.stats()
        assert len(s["hot_keys"]) == 1
        # hot_keys should be hash prefix (16 chars)
        assert len(s["hot_keys"][0]) == 16

    def test_below_threshold_not_hot(self, cache: RetrievalCache):
        crit = _criteria()
        cache.set(crit, None, "value1")
        for _ in range(_HOT_KEY_THRESHOLD - 1):
            cache.get(crit, None)
        s = cache.stats()
        assert len(s["hot_keys"]) == 0

    def test_hot_key_ttl_extended(self, cache: RetrievalCache):
        crit = _criteria()
        cache.set(crit, None, "value1", ttl_seconds=5.0)
        entry = cache._cache[cache._make_key(crit, None)]
        original_ttl = entry.ttl_seconds
        # Hit threshold times
        for _ in range(_HOT_KEY_THRESHOLD):
            cache.get(crit, None)
        entry = cache._cache[cache._make_key(crit, None)]
        assert entry.ttl_seconds == _HOT_KEY_TTL_SECONDS
        assert entry.ttl_seconds > original_ttl


class TestEviction:
    def test_evicts_all_expired_entries_in_batch(self):
        """When cache is at capacity with multiple expired entries, a single set()
        should evict ALL expired entries, not just one."""
        cache = RetrievalCache(max_size=3, default_ttl_seconds=0.05)
        crit_a = _criteria("美妆护肤")
        crit_b = _criteria("数码电子")
        crit_c = _criteria("服饰运动")

        cache.set(crit_a, None, "a")
        cache.set(crit_b, None, "b")
        cache.set(crit_c, None, "c")
        # All three are now in cache, none expired yet

        # Wait for all entries to expire
        time.sleep(0.08)

        # Insert a new entry — should evict ALL 3 expired entries in batch
        crit_d = _criteria("食品生活")
        cache.set(crit_d, None, "d")

        # All expired entries should be gone; only crit_d remains
        assert cache._make_key(crit_a, None) not in cache._cache
        assert cache._make_key(crit_b, None) not in cache._cache
        assert cache._make_key(crit_c, None) not in cache._cache
        assert cache._make_key(crit_d, None) in cache._cache
        assert len(cache._cache) == 1

    def test_evicts_lowest_hit_count_first(self):
        cache = RetrievalCache(max_size=2, default_ttl_seconds=5.0)
        crit_a = _criteria("美妆护肤")
        crit_b = _criteria("数码电子")
        crit_c = _criteria("服饰运动")

        cache.set(crit_a, None, "a")
        cache.set(crit_b, None, "b")
        # Hit crit_b twice
        cache.get(crit_b, None)
        cache.get(crit_b, None)
        # crit_a has hit_count=0, crit_b has hit_count=2

        # Adding crit_c should evict crit_a (lowest hit_count)
        cache.set(crit_c, None, "c")
        assert cache._make_key(crit_a, None) not in cache._cache
        assert cache._make_key(crit_b, None) in cache._cache
        assert cache._make_key(crit_c, None) in cache._cache

    def test_same_hit_count_evicts_earliest_access(self):
        cache = RetrievalCache(max_size=2, default_ttl_seconds=5.0)
        crit_a = _criteria("美妆护肤")
        crit_b = _criteria("数码电子")
        crit_c = _criteria("服饰运动")

        cache.set(crit_a, None, "a")
        cache.set(crit_b, None, "b")
        # Both have hit_count=0, but crit_a was accessed earlier

        # Touch crit_b to update its last_accessed_at
        time.sleep(0.01)
        cache.get(crit_b, None)

        # Adding crit_c should evict crit_a (earlier last_accessed_at)
        cache.set(crit_c, None, "c")
        assert cache._make_key(crit_a, None) not in cache._cache
        assert cache._make_key(crit_b, None) in cache._cache


class TestStats:
    def test_stats_basic_fields(self, cache: RetrievalCache):
        crit = _criteria()
        cache.set(crit, None, "value1")
        cache.get(crit, None)  # hit
        cache.get(_criteria("数码电子"), None)  # miss

        s = cache.stats()
        assert s["total_keys"] == 1
        assert s["hits"] == 1
        assert s["misses"] == 1
        assert s["hit_rate"] == 0.5
        assert isinstance(s["hot_keys"], list)

    def test_stats_hit_rate_zero_access(self, cache: RetrievalCache):
        s = cache.stats()
        assert s["hit_rate"] == 0.0
        assert s["hits"] == 0
        assert s["misses"] == 0

    def test_stats_multiple_hot_keys(self, cache: RetrievalCache):
        crit_a = _criteria("美妆护肤")
        crit_b = _criteria("数码电子")
        cache.set(crit_a, None, "a")
        cache.set(crit_b, None, "b")
        # Hit both threshold times
        for _ in range(_HOT_KEY_THRESHOLD):
            cache.get(crit_a, None)
            cache.get(crit_b, None)
        s = cache.stats()
        assert len(s["hot_keys"]) == 2


class TestDisabledCache:
    def test_disabled_cache_returns_none(self):
        cache = RetrievalCache()
        with patch("src.services.retrieval_cache.get_settings") as mock_settings:
            mock_settings.return_value.retrieval_cache_enabled = False
            cache.set(_criteria(), None, "value")
            result = cache.get(_criteria(), None)
            assert result is None
