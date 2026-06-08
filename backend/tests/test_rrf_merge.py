from src.services.rrf_merge import rrf_merge


def test_single_ranking_preserves_order():
    result = rrf_merge([["a", "b", "c"]])
    assert result == ["a", "b", "c"]


def test_two_rankings_merges_by_rrf_score():
    # a appears first in both → highest RRF score
    result = rrf_merge([["a", "b"], ["a", "c"]])
    assert result[0] == "a"
    assert set(result) == {"a", "b", "c"}


def test_disjoint_rankings_all_included():
    result = rrf_merge([["a", "b"], ["c", "d"]])
    assert set(result) == {"a", "b", "c", "d"}


def test_empty_input():
    assert rrf_merge([]) == []


def test_empty_sublist():
    result = rrf_merge([[], ["a", "b"]])
    assert result == ["a", "b"]
