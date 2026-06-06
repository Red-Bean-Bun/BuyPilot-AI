"""Direct tests for domain_terms pure functions (P0-1)."""

from __future__ import annotations

from src.config.domain_terms import has_negation_prefix


class TestHasNegationPrefix:
    def test_no_negation(self):
        assert has_negation_prefix("我需要防晒", "防晒") is False

    def test_with_negation(self):
        assert has_negation_prefix("我不要防晒", "防晒") is True

    def test_negation_broken_by_scope_breaker(self):
        # "但" breaks the negation scope from "不"
        assert has_negation_prefix("不要太贵但防晒要好", "防晒") is False

    def test_punctuation_resets_scope(self):
        # Second "防晒" after "。" is in a new scope with no negation
        assert has_negation_prefix("不要防晒。建议防晒", "防晒") is False

    def test_term_not_in_text(self):
        assert has_negation_prefix("随机文本", "防晒") is False

    def test_prefix_at_start_of_text(self):
        assert has_negation_prefix("不防晒", "防晒") is True

    def test_no_prefix_negation(self):
        assert has_negation_prefix("不含酒精的洗面奶", "酒精") is True

    def test_double_negation_without_breaker(self):
        # Multiple negated occurrences, no breaker → True
        assert has_negation_prefix("不含酒精不含香精的洗面奶", "酒精") is True
