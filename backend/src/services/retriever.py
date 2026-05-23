"""Hybrid retrieval service: DB chunk vectors plus hard filters/rerank."""

from __future__ import annotations

import asyncio
import math
from contextvars import ContextVar
from dataclasses import dataclass
from typing import Mapping

from src.config.settings import get_settings
from src.config.tuning import (
    FILTER_SCORE_BUDGET,
    FILTER_SCORE_CATEGORY,
    FILTER_SCORE_SCENARIO,
    FILTER_SCORE_SKIN_TYPE,
    PGVECTOR_RECALL_LIMIT,
    RETRIEVAL_CANDIDATE_MULTIPLIER,
)
from src.repos.documents import (
    ChunkDocument,
    evidence_for_chunk,
    list_embedded_chunks,
    list_vector_chunks_by_similarity,
)
from src.repos.products import get_product, list_products
from src.services.async_io import run_sync_io
from src.services.fallbacks import record_fallback
from src.services.embedding import embed_text
from src.services.reranker import rerank, rerank_texts
from src.types.sse_events import CriteriaPayload, EvidencePayload, ProductPayload


@dataclass(frozen=True)
class ProductHit:
    product: ProductPayload
    vector_score: float
    filter_score: float
    chunk: ChunkDocument | None = None


@dataclass(frozen=True)
class RetrievalFilters:
    avoid_products: frozenset[str] = frozenset()
    avoid_traits: tuple[str, ...] = ()


_RETRIEVAL_EVIDENCE_BY_PRODUCT: ContextVar[dict[str, EvidencePayload] | None] = ContextVar(
    "retrieval_evidence_by_product",
    default=None,
)


async def retrieve(
    criteria: CriteriaPayload,
    top_n: int = 5,
    feedback: Mapping[str, list[str]] | None = None,
) -> list[ProductPayload]:
    filters = _retrieval_filters(feedback)
    query_embedding = await embed_text(_query_text(criteria))
    chunk_hits = await run_sync_io(_vector_recall_from_db, criteria, query_embedding, filters)
    if chunk_hits:
        candidate_hits = _rank_hits(criteria, chunk_hits)[: max(top_n * RETRIEVAL_CANDIDATE_MULTIPLIER, top_n)]
        ranked_hits = await _rerank_chunk_hits(criteria, candidate_hits, top_n=top_n)
        _bind_evidence(ranked_hits)
        return [hit.product for hit in ranked_hits]

    if get_settings().strict_runtime:
        raise RuntimeError("Strict runtime requires pgvector/DB vector retrieval; fallback retrieval is disabled.")

    record_fallback("retrieval", "non_db_vector_fallback")
    hard_task = asyncio.create_task(run_sync_io(_hard_filter, criteria, filters))
    vector_task = asyncio.create_task(_vector_recall_fallback(criteria, filters))
    hard_results, vector_results = await asyncio.gather(hard_task, vector_task)
    merged = _merge_dedup(hard_results + vector_results)
    _RETRIEVAL_EVIDENCE_BY_PRODUCT.set({})
    return await rerank(criteria, merged, top_n=top_n)


def cached_evidence_for_product(product_id: str) -> EvidencePayload | None:
    evidence_by_product = _RETRIEVAL_EVIDENCE_BY_PRODUCT.get()
    return evidence_by_product.get(product_id) if evidence_by_product else None


def _hard_filter(criteria: CriteriaPayload, filters: RetrievalFilters) -> list[ProductPayload]:
    products = list_products()
    results: list[ProductPayload] = []
    for product in products:
        if not _passes_hard_filters(criteria, product, filters):
            continue
        results.append(product)
    return results


async def _vector_recall_fallback(criteria: CriteriaPayload, filters: RetrievalFilters) -> list[ProductPayload]:
    await embed_text(criteria.summary)
    return await run_sync_io(_vector_recall_fallback_sync, criteria, filters)


def _vector_recall_fallback_sync(criteria: CriteriaPayload, filters: RetrievalFilters) -> list[ProductPayload]:
    products = list_products()
    products = [product for product in products if _passes_hard_filters(criteria, product, filters)]
    if not criteria.category:
        return products
    return [product for product in products if product.category == criteria.category] or products


def _vector_recall_from_db(
    criteria: CriteriaPayload,
    query_embedding: list[float],
    filters: RetrievalFilters,
) -> list[ProductHit]:
    pgvector_hits = _vector_recall_from_pgvector(criteria, query_embedding, filters)
    if pgvector_hits:
        return pgvector_hits

    chunks = list_embedded_chunks()
    if not chunks or not query_embedding:
        return []

    hits: list[ProductHit] = []
    for chunk in chunks:
        if not _eligible_for_primary_recall(chunk):
            continue
        if len(chunk.embedding) != len(query_embedding):
            continue
        product = get_product(chunk.product_id)
        if product is None or not _passes_hard_filters(criteria, product, filters):
            continue
        vector_score = _cosine_similarity(query_embedding, chunk.embedding)
        if vector_score <= 0:
            continue
        filter_score = _filter_score(criteria, product)
        hits.append(
            ProductHit(
                product=product,
                vector_score=vector_score,
                filter_score=filter_score,
                chunk=chunk,
            )
        )
    return hits


def _vector_recall_from_pgvector(
    criteria: CriteriaPayload,
    query_embedding: list[float],
    filters: RetrievalFilters,
) -> list[ProductHit]:
    hits: list[ProductHit] = []
    for vector_hit in list_vector_chunks_by_similarity(query_embedding, limit=PGVECTOR_RECALL_LIMIT):
        chunk = vector_hit.document
        product = get_product(chunk.product_id)
        if product is None or not _passes_hard_filters(criteria, product, filters):
            continue
        hits.append(
            ProductHit(
                product=product,
                vector_score=_distance_to_score(vector_hit.distance),
                filter_score=_filter_score(criteria, product),
                chunk=chunk,
            )
        )
    return hits


def _rank_hits(criteria: CriteriaPayload, hits: list[ProductHit]) -> list[ProductHit]:
    return sorted(
        hits,
        key=lambda hit: (
            hit.filter_score,
            hit.vector_score,
            -(hit.product.price or 0.0),
        ),
        reverse=True,
    )


def _bind_evidence(hits: list[ProductHit]) -> None:
    evidence_by_product: dict[str, EvidencePayload] = {}
    for hit in hits:
        if hit.chunk is not None:
            evidence_by_product[hit.product.product_id] = evidence_for_chunk(hit.chunk)
    _RETRIEVAL_EVIDENCE_BY_PRODUCT.set(evidence_by_product)


async def _rerank_chunk_hits(criteria: CriteriaPayload, hits: list[ProductHit], top_n: int) -> list[ProductHit]:
    documents = [_chunk_document_text(hit) for hit in hits]
    reranked_indexes = await rerank_texts(criteria, documents, top_n=len(hits))
    index_order = reranked_indexes or list(range(len(hits)))
    selected: list[ProductHit] = []
    seen_products: set[str] = set()
    for index in index_order:
        if not 0 <= index < len(hits):
            continue
        hit = hits[index]
        if hit.product.product_id in seen_products:
            continue
        seen_products.add(hit.product.product_id)
        selected.append(hit)
        if len(selected) >= top_n:
            break
    if len(selected) < top_n:
        for hit in hits:
            if hit.product.product_id in seen_products:
                continue
            seen_products.add(hit.product.product_id)
            selected.append(hit)
            if len(selected) >= top_n:
                break
    return selected


def _chunk_document_text(hit: ProductHit) -> str:
    chunk_text = hit.chunk.chunk_text if hit.chunk is not None else ""
    product = hit.product
    parts = [
        product.name,
        product.brand or "",
        product.category,
        product.sub_category or "",
        f"{product.price:g}元" if product.price is not None else "",
        chunk_text,
    ]
    return " | ".join(part for part in parts if part)


def _eligible_for_primary_recall(chunk: ChunkDocument) -> bool:
    return chunk.metadata.get("retrieval_role") != "risk"


def _passes_hard_filters(criteria: CriteriaPayload, product: ProductPayload, filters: RetrievalFilters) -> bool:
    if product.product_id in filters.avoid_products:
        return False
    if criteria.category and product.category != criteria.category:
        return False
    if (
        criteria.constraints.budget_max is not None
        and product.price is not None
        and product.price > criteria.constraints.budget_max
    ):
        return False
    avoid_traits = tuple(criteria.constraints.ingredient_avoid) + filters.avoid_traits
    return not any(_matches_avoid_trait(product, token) for token in avoid_traits)


def _matches_avoid_trait(product: ProductPayload, token: str) -> bool:
    normalized = token.strip()
    if not normalized:
        return False
    lowered = normalized.lower()
    if lowered in {"日系", "日系品牌", "日本", "日本品牌"}:
        return (product.brand or "") in {"SK-II", "资生堂", "安热沙", "珊珂", "芳珂", "优衣库"}
    if lowered in {"nike", "耐克"}:
        return _contains_any(_product_haystack(product), ("nike", "耐克"))
    return lowered in _product_haystack(product).lower()


def _product_haystack(product: ProductPayload) -> str:
    parts = [
        product.product_id,
        product.name,
        product.brand or "",
        product.category,
        product.sub_category or "",
        product.use_scenario or "",
        *product.ingredient_tags,
        *product.ingredient_avoid,
    ]
    return " ".join(part for part in parts if part)


def _contains_any(text: str, tokens: tuple[str, ...]) -> bool:
    lowered = text.lower()
    return any(token.lower() in lowered for token in tokens)


def _retrieval_filters(feedback: Mapping[str, list[str]] | None) -> RetrievalFilters:
    if not feedback:
        return RetrievalFilters()
    return RetrievalFilters(
        avoid_products=frozenset(feedback.get("avoid_products", [])),
        avoid_traits=tuple(dict.fromkeys(feedback.get("avoid_traits", []))),
    )


def _filter_score(criteria: CriteriaPayload, product: ProductPayload) -> float:
    score = 0.0
    if product.category == criteria.category:
        score += FILTER_SCORE_CATEGORY
    if criteria.constraints.skin_type and criteria.constraints.skin_type in product.skin_type_match:
        score += FILTER_SCORE_SKIN_TYPE
    if (
        criteria.constraints.budget_max is not None
        and product.price is not None
        and product.price <= criteria.constraints.budget_max
    ):
        score += FILTER_SCORE_BUDGET
    if (
        criteria.constraints.use_scenario
        and product.use_scenario
        and criteria.constraints.use_scenario in product.use_scenario
    ):
        score += FILTER_SCORE_SCENARIO
    return score


def _query_text(criteria: CriteriaPayload) -> str:
    constraints = criteria.constraints
    parts = [
        criteria.category,
        criteria.summary,
        constraints.skin_type or "",
        constraints.use_scenario or "",
        " ".join(constraints.ingredient_prefer),
        " ".join(f"不要{item}" for item in constraints.ingredient_avoid),
        f"{constraints.budget_max:g}元内" if constraints.budget_max is not None else "",
    ]
    return " ".join(part for part in parts if part)


def _cosine_similarity(left: list[float], right: list[float]) -> float:
    size = min(len(left), len(right))
    if size == 0:
        return 0.0
    dot = sum(left[index] * right[index] for index in range(size))
    left_norm = math.sqrt(sum(left[index] * left[index] for index in range(size)))
    right_norm = math.sqrt(sum(right[index] * right[index] for index in range(size)))
    if left_norm == 0 or right_norm == 0:
        return 0.0
    return dot / (left_norm * right_norm)


def _distance_to_score(distance: float) -> float:
    return 1.0 / (1.0 + max(distance, 0.0))


def _merge_dedup(products: list[ProductPayload]) -> list[ProductPayload]:
    seen: set[str] = set()
    merged: list[ProductPayload] = []
    for product in products:
        if product.product_id in seen:
            continue
        seen.add(product.product_id)
        merged.append(product)
    return merged
