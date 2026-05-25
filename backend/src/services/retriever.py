"""Hybrid retrieval service: DB chunk vectors plus hard filters/rerank."""

from __future__ import annotations

import math
from dataclasses import dataclass, field
from typing import Mapping

from src.config.domain_terms import avoid_trait_matches_text, normalize_product_type, product_type_aliases
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
from src.repos.database import get_async_engine, is_postgres_engine
from src.repos.products import get_product
from src.services.embedding import embed_text
from src.services.retrieval_features import criteria_query_text, product_document_text, product_match_score
from src.services.reranker import rerank_texts
from src.types.sse_events import CriteriaPayload, EvidencePayload, ProductPayload

TRACE_VECTOR_TOP_K_LIMIT = 50


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


@dataclass(frozen=True)
class RetrievalOutput:
    products: list[ProductPayload]
    evidence_by_product: dict[str, list[EvidencePayload]]
    trace_details: dict[str, object] = field(default_factory=dict)


async def retrieve(
    criteria: CriteriaPayload,
    top_n: int = 5,
    feedback: Mapping[str, list[str]] | None = None,
) -> list[ProductPayload]:
    return (await retrieve_with_evidence(criteria, top_n=top_n, feedback=feedback)).products


async def retrieve_with_evidence(
    criteria: CriteriaPayload,
    top_n: int = 5,
    feedback: Mapping[str, list[str]] | None = None,
) -> RetrievalOutput:
    filters = _retrieval_filters(feedback)
    query_embedding = await embed_text(criteria_query_text(criteria))
    chunk_hits = await _vector_recall_from_db(criteria, query_embedding, filters)
    recall_criteria = criteria
    recall_filters = filters
    relaxation_steps: list[dict[str, object]] = [
        {
            "step": "strict",
            "reason": "category/budget/product_type/exclusion hard filters",
            "relaxed_fields": [],
            "candidate_count": len(chunk_hits),
        }
    ]
    if not chunk_hits:
        for step, relaxed_criteria, relaxed_filters, relaxed_fields in _relaxation_attempts(criteria, filters):
            relaxed_hits = await _vector_recall_from_db(relaxed_criteria, query_embedding, relaxed_filters)
            relaxation_steps.append(
                {
                    "step": step,
                    "reason": "DB vector chunks only; non-DB product or memory fallback remains disabled",
                    "relaxed_fields": relaxed_fields,
                    "candidate_count": len(relaxed_hits),
                }
            )
            if relaxed_hits:
                chunk_hits = relaxed_hits
                recall_criteria = relaxed_criteria
                recall_filters = relaxed_filters
                break
    if not chunk_hits:
        raise RuntimeError("DB vector retrieval returned no chunk hits; non-DB retrieval fallback is disabled.")

    candidate_hits = _rank_hits(criteria, chunk_hits)[: max(top_n * RETRIEVAL_CANDIDATE_MULTIPLIER, top_n)]
    ranked_hits, all_reranked_hits = await _rerank_chunk_hits(criteria, candidate_hits, top_n=top_n)
    return RetrievalOutput(
        products=[hit.product for hit in ranked_hits],
        evidence_by_product=_evidence_by_product(ranked_hits, all_reranked_hits),
        trace_details=_trace_details_for_chunk_retrieval(
            criteria=criteria,
            recall_criteria=recall_criteria,
            filters=recall_filters,
            chunk_hits=chunk_hits,
            candidate_hits=candidate_hits,
            ranked_hits=ranked_hits,
            relaxation_steps=relaxation_steps,
        ),
    )


async def _vector_recall_from_db(
    criteria: CriteriaPayload,
    query_embedding: list[float],
    filters: RetrievalFilters,
) -> list[ProductHit]:
    pgvector_hits = await _vector_recall_from_pgvector(criteria, query_embedding, filters)
    if pgvector_hits:
        return pgvector_hits
    if is_postgres_engine(get_async_engine()):
        return []

    chunks = await list_embedded_chunks()
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


async def _vector_recall_from_pgvector(
    criteria: CriteriaPayload,
    query_embedding: list[float],
    filters: RetrievalFilters,
) -> list[ProductHit]:
    hits: list[ProductHit] = []
    for vector_hit in await list_vector_chunks_by_similarity(query_embedding, limit=PGVECTOR_RECALL_LIMIT):
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


_EVIDENCE_KIND_PRIORITY = ("why_buy", "faq", "risk", "compare")


def _evidence_by_product(
    ranked_hits: list[ProductHit], all_reranked_hits: list[ProductHit]
) -> dict[str, list[EvidencePayload]]:
    evidence_by_product: dict[str, list[EvidencePayload]] = {}
    for hit in ranked_hits:
        product_id = hit.product.product_id
        groups: dict[str, ChunkDocument] = {}
        seen_chunk_ids: set[str] = set()
        for candidate in all_reranked_hits:
            if candidate.product.product_id != product_id or candidate.chunk is None:
                continue
            if candidate.chunk.id in seen_chunk_ids:
                continue
            seen_chunk_ids.add(candidate.chunk.id)
            kind = candidate.chunk.metadata.get("evidence_kind") or "other"
            if kind not in groups:
                groups[kind] = candidate.chunk
        selected_chunks = [groups[kind] for kind in _EVIDENCE_KIND_PRIORITY if kind in groups]
        if not selected_chunks and groups:
            selected_chunks = [next(iter(groups.values()))]
        evidence_by_product[product_id] = [evidence_for_chunk(chunk) for chunk in selected_chunks]
    return evidence_by_product


def _trace_details_for_chunk_retrieval(
    criteria: CriteriaPayload,
    recall_criteria: CriteriaPayload,
    filters: RetrievalFilters,
    chunk_hits: list[ProductHit],
    candidate_hits: list[ProductHit],
    ranked_hits: list[ProductHit],
    relaxation_steps: list[dict[str, object]],
) -> dict[str, object]:
    vector_hits = sorted(chunk_hits, key=lambda hit: hit.vector_score, reverse=True)[:TRACE_VECTOR_TOP_K_LIMIT]
    return {
        "filters_applied": {
            "_candidate_product_ids": _unique_product_ids(candidate_hits),
            "category": criteria.category or None,
            "budget_max": criteria.constraints.budget_max,
            "product_type": criteria.constraints.product_type,
            "product_type_aliases": list(product_type_aliases(criteria.constraints.product_type)),
            "avoid_products": sorted(filters.avoid_products),
            "avoid_traits": list(filters.avoid_traits),
            "effective_budget_max": recall_criteria.constraints.budget_max,
            "effective_product_type": recall_criteria.constraints.product_type,
            "relaxation_steps": relaxation_steps,
        },
        "vector_top_k": [_trace_hit_payload(hit, rank) for rank, hit in enumerate(vector_hits, start=1)],
        "rerank_top_n": [_trace_hit_payload(hit, rank) for rank, hit in enumerate(ranked_hits, start=1)],
        "selected_ids": [hit.product.product_id for hit in ranked_hits],
        "hit_count": len(ranked_hits),
        "vector_count": len(chunk_hits),
    }


def _trace_hit_payload(hit: ProductHit, rank: int) -> dict[str, object]:
    payload: dict[str, object] = {
        "rank": rank,
        "product_id": hit.product.product_id,
        "vector_score": round(hit.vector_score, 6),
        "filter_score": round(hit.filter_score, 6),
    }
    if hit.chunk is not None:
        payload.update(
            {
                "chunk_id": hit.chunk.id,
                "chunk_index": hit.chunk.chunk_index,
                "chunk_type": hit.chunk.metadata.get("chunk_type"),
                "retrieval_role": hit.chunk.metadata.get("retrieval_role"),
            }
        )
    return payload


def _relaxation_attempts(
    criteria: CriteriaPayload, filters: RetrievalFilters
) -> list[tuple[str, CriteriaPayload, RetrievalFilters, list[str]]]:
    attempts: list[tuple[str, CriteriaPayload, RetrievalFilters, list[str]]] = []
    if criteria.constraints.product_type:
        attempts.append(
            (
                "without_product_type",
                _criteria_with_constraints(criteria, product_type=None),
                filters,
                ["product_type"],
            )
        )
    if criteria.constraints.budget_max is not None:
        attempts.append(
            (
                "without_budget_max",
                _criteria_with_constraints(criteria, budget_max=None),
                filters,
                ["budget_max"],
            )
        )
    if criteria.constraints.product_type and criteria.constraints.budget_max is not None:
        attempts.append(
            (
                "without_product_type_and_budget_max",
                _criteria_with_constraints(criteria, product_type=None, budget_max=None),
                filters,
                ["product_type", "budget_max"],
            )
        )
    if filters.avoid_traits:
        attempts.append(
            (
                "without_feedback_avoid_traits",
                criteria,
                RetrievalFilters(avoid_products=filters.avoid_products),
                ["feedback.avoid_traits"],
            )
        )
    return attempts


def _criteria_with_constraints(criteria: CriteriaPayload, **updates: object) -> CriteriaPayload:
    constraints = criteria.constraints.model_copy(update=updates)
    return criteria.model_copy(update={"constraints": constraints})


def _unique_product_ids(hits: list[ProductHit]) -> list[str]:
    seen: set[str] = set()
    result: list[str] = []
    for hit in hits:
        product_id = hit.product.product_id
        if product_id in seen:
            continue
        seen.add(product_id)
        result.append(product_id)
    return result


async def _rerank_chunk_hits(
    criteria: CriteriaPayload, hits: list[ProductHit], top_n: int
) -> tuple[list[ProductHit], list[ProductHit]]:
    documents = [_chunk_document_text(hit) for hit in hits]
    index_order = await rerank_texts(criteria, documents, top_n=len(hits))

    all_reranked_hits = [hits[index] for index in index_order if 0 <= index < len(hits)]

    selected: list[ProductHit] = []
    seen_products: set[str] = set()
    for hit in all_reranked_hits:
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
    return selected, all_reranked_hits


def _chunk_document_text(hit: ProductHit) -> str:
    chunk_text = hit.chunk.chunk_text if hit.chunk is not None else ""
    return product_document_text(hit.product, extra_text=chunk_text)


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
    constraints = criteria.constraints
    if constraints.brand_avoid and product.brand:
        product_brand_lower = product.brand.lower()
        if any(_brand_matches(brand, product_brand_lower) for brand in constraints.brand_avoid):
            return False
    if constraints.origin_avoid and product.brand:
        if any(avoid_trait_matches_text(origin, product.brand) for origin in constraints.origin_avoid):
            return False
    criteria_product_type = normalize_product_type(constraints.product_type)
    product_type = normalize_product_type(product.sub_category)
    if criteria_product_type and product_type != criteria_product_type:
        return False
    avoid_traits = tuple(constraints.ingredient_avoid) + filters.avoid_traits
    return not any(_matches_avoid_trait(product, token) for token in avoid_traits)


def _brand_matches(avoid_brand: str, product_brand_lower: str) -> bool:
    return avoid_brand.strip().lower() == product_brand_lower


def _matches_avoid_trait(product: ProductPayload, token: str) -> bool:
    return avoid_trait_matches_text(token, _product_haystack(product))


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


def _retrieval_filters(feedback: Mapping[str, list[str]] | None) -> RetrievalFilters:
    if not feedback:
        return RetrievalFilters()
    return RetrievalFilters(
        avoid_products=frozenset(feedback.get("avoid_products", [])),
        avoid_traits=tuple(dict.fromkeys(feedback.get("avoid_traits", []))),
    )


def _filter_score(criteria: CriteriaPayload, product: ProductPayload) -> float:
    return product_match_score(
        criteria,
        product,
        category_weight=FILTER_SCORE_CATEGORY,
        skin_type_weight=FILTER_SCORE_SKIN_TYPE,
        budget_weight=FILTER_SCORE_BUDGET,
        scenario_weight=FILTER_SCORE_SCENARIO,
    )


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
