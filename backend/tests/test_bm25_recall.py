"""Test BM25Index.search with a manually built index (no DB dependency)."""

from src.services.bm25_recall import BM25Index, Bm25Hit


def _build_test_index() -> BM25Index:
    """Build a BM25Index with hardcoded data for testing."""
    from rank_bm25 import BM25Okapi
    import jieba

    docs = [
        "巴黎欧莱雅防晒霜 控油保湿 SPF50",
        "理肤泉敏感肌修复霜 不含酒精",
        "安热沙防晒乳 防水防汗 户外专用",
        "珊珂洗面奶 绵密泡沫 温和清洁",
    ]
    tokenized = [list(jieba.cut_for_search(d)) for d in docs]

    idx = BM25Index()
    idx._bm25 = BM25Okapi(tokenized)
    idx._chunk_ids = [f"chunk_{i}" for i in range(len(docs))]
    idx._product_ids = [f"product_{i}" for i in range(len(docs))]
    idx._ready = True
    return idx


def test_bm25_finds_keyword_match():
    idx = _build_test_index()
    hits = idx.search("防晒霜", limit=3)
    assert len(hits) > 0
    # 防晒霜 should match chunk_0 (欧莱雅防晒霜) and chunk_2 (安热沙防晒乳)
    matched_ids = {h.chunk_id for h in hits[:3]}
    assert "chunk_0" in matched_ids or "chunk_2" in matched_ids


def test_bm25_returns_empty_when_not_ready():
    idx = BM25Index()
    assert idx.search("anything") == []


def test_bm25_respects_limit():
    idx = _build_test_index()
    hits = idx.search("防晒", limit=2)
    assert len(hits) <= 2
