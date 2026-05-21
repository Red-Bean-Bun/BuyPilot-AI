"""Hybrid retrieval service: DB chunk vectors plus hard filters/rerank."""

from __future__ import annotations

import asyncio
import math
from contextvars import ContextVar
from dataclasses import dataclass

from src.repos.documents import ChunkDocument, evidence_for_chunk, list_embedded_chunks
from src.repos.products import get_product, list_products
from src.services.embedding import embed_text
from src.services.reranker import rerank, rerank_texts
from src.types.sse_events import CriteriaPayload, EvidencePayload, ProductPayload


@dataclass(frozen=True)
class ProductHit:
    product: ProductPayload
    vector_score: float
    filter_score: float
    chunk: ChunkDocument | None = None


_RETRIEVAL_EVIDENCE_BY_PRODUCT: ContextVar[dict[str, EvidencePayload]] = ContextVar(
    "retrieval_evidence_by_product",
    default={},
)


async def retrieve(criteria: CriteriaPayload, top_n: int = 5) -> list[ProductPayload]:
    query_embedding = await embed_text(_query_text(criteria))
    chunk_hits = _vector_recall_from_db(criteria, query_embedding)
    if chunk_hits:
        candidate_hits = _rank_hits(criteria, chunk_hits)[: max(top_n * 8, top_n)]
        ranked_hits = await _rerank_chunk_hits(criteria, candidate_hits, top_n=top_n)
        _bind_evidence(ranked_hits)
        return [hit.product for hit in ranked_hits]

    hard_task = asyncio.create_task(_hard_filter(criteria))
    vector_task = asyncio.create_task(_vector_recall_fallback(criteria))
    hard_results, vector_results = await asyncio.gather(hard_task, vector_task)
    merged = _merge_dedup(hard_results + vector_results)
    _RETRIEVAL_EVIDENCE_BY_PRODUCT.set({})
    return await rerank(criteria, merged, top_n=top_n)


def cached_evidence_for_product(product_id: str) -> EvidencePayload | None:
    return _RETRIEVAL_EVIDENCE_BY_PRODUCT.get().get(product_id)


async def _hard_filter(criteria: CriteriaPayload) -> list[ProductPayload]:
    products = list_products()
    results: list[ProductPayload] = []
    for product in products:
        if criteria.category and product.category != criteria.category:
            continue
        if criteria.constraints.budget_max is not None and product.price is not None and product.price > criteria.constraints.budget_max:
            continue
        haystack = " ".join(product.ingredient_tags + product.ingredient_avoid)
        if any(token in haystack for token in criteria.constraints.ingredient_avoid):
            continue
        results.append(product)
    return results


async def _vector_recall_fallback(criteria: CriteriaPayload) -> list[ProductPayload]:
    await embed_text(criteria.summary)
    products = list_products()
    if not criteria.category:
        return products
    return [product for product in products if product.category == criteria.category] or products


def _vector_recall_from_db(criteria: CriteriaPayload, query_embedding: list[float]) -> list[ProductHit]:
    chunks = list_embedded_chunks()
    if not chunks or not query_embedding:
        return []

    hits: list[ProductHit] = []
    for chunk in chunks:
        if len(chunk.embedding) != len(query_embedding):
            continue
        product = get_product(chunk.product_id)
        if product is None or not _passes_hard_filters(criteria, product):
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


def _passes_hard_filters(criteria: CriteriaPayload, product: ProductPayload) -> bool:
    if criteria.category and product.category != criteria.category:
        return False
    if criteria.constraints.budget_max is not None and product.price is not None and product.price > criteria.constraints.budget_max:
        return False
    haystack = " ".join(product.ingredient_tags + product.ingredient_avoid)
    return not any(token in haystack for token in criteria.constraints.ingredient_avoid)


def _filter_score(criteria: CriteriaPayload, product: ProductPayload) -> float:
    score = 0.0
    if product.category == criteria.category:
        score += 3.0
    if criteria.constraints.skin_type and criteria.constraints.skin_type in product.skin_type_match:
        score += 2.0
    if criteria.constraints.budget_max is not None and product.price is not None and product.price <= criteria.constraints.budget_max:
        score += 1.0
    if criteria.constraints.use_scenario and product.use_scenario and criteria.constraints.use_scenario in product.use_scenario:
        score += 0.5
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


def _merge_dedup(products: list[ProductPayload]) -> list[ProductPayload]:
    seen: set[str] = set()
    merged: list[ProductPayload] = []
    for product in products:
        if product.product_id in seen:
            continue
        seen.add(product.product_id)
        merged.append(product)
    return merged
