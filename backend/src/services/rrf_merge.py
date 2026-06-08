"""Reciprocal Rank Fusion (RRF) — pure function for merging multiple ranked lists."""

from __future__ import annotations

from collections import defaultdict


def rrf_merge(
    rankings: list[list[str]],
    k: int = 60,
) -> list[str]:
    """标准 RRF: score(d) = Σ 1/(k + rank_i(d))，返回按分数降序的 chunk_id 列表。

    rank 从 1 开始（第一个元素 rank=1）。
    只出现在一路中的元素也会入选。

    Args:
        rankings: 每路排名列表，每个子列表为 [chunk_id, chunk_id, ...]，按相关性降序排列。
        k: RRF 平滑参数，默认 60（信息检索经典值）。

    Returns:
        按 RRF 分数降序排列的 chunk_id 列表。
    """
    if not rankings:
        return []

    scores: dict[str, float] = defaultdict(float)
    for ranking in rankings:
        for rank, doc_id in enumerate(ranking, start=1):
            scores[doc_id] += 1.0 / (k + rank)

    return sorted(scores.keys(), key=lambda doc_id: scores[doc_id], reverse=True)
