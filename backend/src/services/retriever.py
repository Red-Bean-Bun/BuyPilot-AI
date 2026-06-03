"""Hybrid retrieval service: DB chunk vectors plus hard filters/rerank."""

from __future__ import annotations

import asyncio
from collections.abc import Callable
from dataclasses import dataclass, field
from typing import Mapping

from src.config.domain_terms import (
    avoid_trait_aliases,
    avoid_trait_matches_text,
    normalize_category,
    normalize_product_type,
    product_type_aliases,
)
from src.config.tuning import (
    FILTER_SCORE_BRAND,
    FILTER_SCORE_BRAND_PREFER,
    FILTER_SCORE_BUDGET,
    FILTER_SCORE_CATEGORY,
    FILTER_SCORE_SCENARIO,
    FILTER_SCORE_SKIN_TYPE,
    PGVECTOR_RECALL_LIMIT,
    RETRIEVAL_CANDIDATE_MULTIPLIER,
)
from src.repos.documents import (
    ChunkDocument,
    ImageSimilarityHit,
    VectorSearchFilters,
    evidence_for_chunk,
    evidence_for_product,
    list_products_by_image_similarity,
    list_vector_chunks_by_similarity,
)
from src.repos.products import get_product, list_products
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


@dataclass(frozen=True)
class VectorRecallResult:
    hits: list[ProductHit]
    sql_filters_applied: dict[str, object]
    pre_filter_count: int
    post_filter_count: int


async def retrieve(
    criteria: CriteriaPayload,
    top_n: int = 5,
    feedback: Mapping[str, list[str]] | None = None,
    image_embedding: list[float] | None = None,
) -> list[ProductPayload]:
    return (await retrieve_with_evidence(criteria, top_n=top_n, feedback=feedback, image_embedding=image_embedding)).products


async def retrieve_with_evidence(
    criteria: CriteriaPayload,
    top_n: int = 5,
    feedback: Mapping[str, list[str]] | None = None,
    image_embedding: list[float] | None = None,
) -> RetrievalOutput:
    filters = _retrieval_filters(feedback)
    query_embedding = await embed_text(criteria_query_text(criteria))

    # Launch visual recall in parallel if image_embedding provided
    visual_task = None
    if image_embedding:
        sql_filters = _sql_filters_for_recall(criteria, filters)
        visual_task = asyncio.create_task(
            list_products_by_image_similarity(image_embedding, limit=10, filters=sql_filters)
        )

    recall_result = await _vector_recall_from_db(criteria, query_embedding, filters)
    chunk_hits = recall_result.hits
    recall_criteria = criteria
    recall_filters = filters
    recall_stats = recall_result
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
            relaxed_result = await _vector_recall_from_db(relaxed_criteria, query_embedding, relaxed_filters)
            relaxed_hits = relaxed_result.hits
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
                recall_stats = relaxed_result
                break

    # Merge visual recall results
    visual_hits: list[ProductHit] = []
    visual_recall_stats: dict[str, object] = {}
    if visual_task is not None:
        try:
            image_hits = await visual_task
            visual_hits, visual_recall_stats = _build_visual_hits(image_hits, criteria, filters)
        except Exception:
            # Visual recall failure is non-fatal — degrade to text-only
            visual_recall_stats = {"error": "visual recall failed, degraded to text-only"}

    # Merge text and visual hits
    merged_hits = _merge_text_and_visual(chunk_hits, visual_hits)

    # Brand-preference direct fetch: when brand_prefer is set, explicitly
    # fetch matching products from the catalog and merge into candidates.
    # Vector similarity alone cannot guarantee brand-preferred products
    # appear — this is a hard recall safety net.
    brand_hits = _fetch_brand_preference_products(criteria, filters)
    if brand_hits:
        merged_hits = _merge_text_and_visual(merged_hits, brand_hits)

    if not merged_hits:
        return RetrievalOutput(products=[], evidence_by_product={}, trace_details=_visual_trace(visual_recall_stats))

    candidate_hits = _rank_hits(criteria, merged_hits)[: max(top_n * RETRIEVAL_CANDIDATE_MULTIPLIER, top_n)]
    ranked_hits, all_reranked_hits = await _rerank_chunk_hits(criteria, candidate_hits, top_n=top_n)

    # Hard guarantee: brand_prefer products always appear first in results,
    # regardless of how the reranker scored them. Without this, preferred
    # brands can be buried by vector similarity or reranker preference.
    ranked_hits = _elevate_brand_preference(criteria, ranked_hits, all_reranked_hits)

    # Supplement evidence for visual-only hits (no chunk)
    evidence_map = _evidence_by_product(ranked_hits, all_reranked_hits)
    await _supplement_visual_evidence(ranked_hits, evidence_map)

    trace = _trace_details_for_chunk_retrieval(
        criteria=criteria,
        recall_criteria=recall_criteria,
        filters=recall_filters,
        chunk_hits=chunk_hits,
        candidate_hits=candidate_hits,
        ranked_hits=ranked_hits,
        relaxation_steps=relaxation_steps,
        recall_stats=recall_stats,
    )
    if visual_recall_stats:
        trace["visual_recall"] = visual_recall_stats
    return RetrievalOutput(
        products=[hit.product for hit in ranked_hits],
        evidence_by_product=evidence_map,
        trace_details=trace,
    )


async def _vector_recall_from_db(
    criteria: CriteriaPayload,
    query_embedding: list[float],
    filters: RetrievalFilters,
) -> VectorRecallResult:
    return await _vector_recall_from_pgvector(criteria, query_embedding, filters)


async def _vector_recall_from_pgvector(
    criteria: CriteriaPayload,
    query_embedding: list[float],
    filters: RetrievalFilters,
) -> VectorRecallResult:
    hits: list[ProductHit] = []
    sql_filters = _sql_filters_for_recall(criteria, filters)
    vector_hits = await list_vector_chunks_by_similarity(
        query_embedding,
        limit=PGVECTOR_RECALL_LIMIT,
        filters=sql_filters,
    )
    for vector_hit in vector_hits:
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
    return VectorRecallResult(
        hits=hits,
        sql_filters_applied=_sql_filter_payload(sql_filters),
        pre_filter_count=len(vector_hits),
        post_filter_count=len(hits),
    )


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
    recall_stats: VectorRecallResult,
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
            "sql_filters_applied": recall_stats.sql_filters_applied,
            "pre_filter_count": recall_stats.pre_filter_count,
            "post_filter_count": recall_stats.post_filter_count,
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
    """Relax budget_max when strict returns empty, but never silently relax category or product_type.

    category and product_type are hard constraints — showing a 防晒 for a 洁面 request is worse than showing nothing.
    budget_max is a threshold constraint — showing a 220 CNY item for a 200 CNY budget is helpful with a note.
    """
    attempts: list[tuple[str, CriteriaPayload, RetrievalFilters, list[str]]] = []
    if criteria.constraints.budget_max is not None:
        attempts.append(
            (
                "without_budget_max",
                _criteria_with_constraints(criteria, budget_max=None),
                filters,
                ["budget_max"],
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


FilterCheck = Callable[[CriteriaPayload, ProductPayload, RetrievalFilters], bool]


def _passes_hard_filters(criteria: CriteriaPayload, product: ProductPayload, filters: RetrievalFilters) -> bool:
    return all(check(criteria, product, filters) for check in _FILTER_CHECKS)


def _passes_feedback_product_filter(
    criteria: CriteriaPayload, product: ProductPayload, filters: RetrievalFilters
) -> bool:
    del criteria
    return product.product_id not in filters.avoid_products


def _passes_category_filter(criteria: CriteriaPayload, product: ProductPayload, filters: RetrievalFilters) -> bool:
    del filters
    criteria_category = normalize_category(criteria.category)
    product_category = normalize_category(product.category)
    return not criteria_category or product_category == criteria_category


def _passes_budget_filter(criteria: CriteriaPayload, product: ProductPayload, filters: RetrievalFilters) -> bool:
    del filters
    if product.price is None:
        return True
    constraints = criteria.constraints
    if constraints.budget_max is not None and product.price > constraints.budget_max:
        return False
    if constraints.budget_min is not None and product.price < constraints.budget_min:
        return False
    return True


def _passes_brand_filter(criteria: CriteriaPayload, product: ProductPayload, filters: RetrievalFilters) -> bool:
    del filters
    constraints = criteria.constraints
    if not constraints.brand_avoid or not product.brand:
        return True
    product_brand_lower = product.brand.lower()
    return not any(_brand_matches(brand, product_brand_lower) for brand in constraints.brand_avoid)


def _passes_origin_filter(criteria: CriteriaPayload, product: ProductPayload, filters: RetrievalFilters) -> bool:
    del filters
    constraints = criteria.constraints
    if not constraints.origin_avoid or not product.brand:
        return True
    return not any(avoid_trait_matches_text(origin, product.brand) for origin in constraints.origin_avoid)


def _passes_product_type_filter(criteria: CriteriaPayload, product: ProductPayload, filters: RetrievalFilters) -> bool:
    del filters
    criteria_product_type = normalize_product_type(criteria.constraints.product_type)
    product_type = normalize_product_type(product.sub_category)
    if not criteria_product_type:
        return True
    if product_type == criteria_product_type:
        return True
    # Containment match for hierarchy: "裤子" matches "户外裤" via substring
    if criteria_product_type in (product_type or ""):
        return True
    if (product_type or "") in criteria_product_type:
        return True
    return False


def _passes_avoid_trait_filter(criteria: CriteriaPayload, product: ProductPayload, filters: RetrievalFilters) -> bool:
    avoid_traits = tuple(criteria.constraints.ingredient_avoid) + filters.avoid_traits
    return not any(_matches_avoid_trait(product, token) for token in avoid_traits)


_FILTER_CHECKS: tuple[FilterCheck, ...] = (
    _passes_feedback_product_filter,
    _passes_category_filter,
    _passes_budget_filter,
    _passes_brand_filter,
    _passes_origin_filter,
    _passes_product_type_filter,
    _passes_avoid_trait_filter,
)


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


def _sql_filters_for_recall(criteria: CriteriaPayload, filters: RetrievalFilters) -> VectorSearchFilters:
    constraints = criteria.constraints
    return VectorSearchFilters(
        category=normalize_category(criteria.category),
        budget_max=constraints.budget_max,
        product_type_aliases=tuple(product_type_aliases(constraints.product_type)),
        brand_avoid=_brand_avoid_terms(criteria),
        avoid_product_ids=tuple(sorted(filters.avoid_products)),
    )


def _brand_avoid_terms(criteria: CriteriaPayload) -> tuple[str, ...]:
    terms: list[str] = []
    terms.extend(criteria.constraints.brand_avoid)
    for origin in criteria.constraints.origin_avoid:
        terms.extend(avoid_trait_aliases(origin))
    return tuple(dict.fromkeys(term for term in terms if term))


def _sql_filter_payload(filters: VectorSearchFilters) -> dict[str, object]:
    return {
        "category": filters.category,
        "budget_max": filters.budget_max,
        "product_type_aliases": list(filters.product_type_aliases),
        "brand_avoid": list(filters.brand_avoid),
        "avoid_product_ids": list(filters.avoid_product_ids),
    }


def filter_products(
    products: list[ProductPayload],
    criteria: CriteriaPayload,
    feedback: Mapping[str, list[str]] | None = None,
    max_products: int = 5,
) -> list[ProductPayload]:
    """Apply hard filters to an already-retrieved product list. O(n).

    Used by speculative retrieval post-filtering: re-screen speculatively
    retrieved candidates against the full criteria (which may include
    brand_avoid / origin_avoid that the speculative CriteriaPayload lacked).
    """
    filters = _retrieval_filters(feedback)
    return [p for p in products if _passes_hard_filters(criteria, p, filters)][:max_products]


def _filter_score(criteria: CriteriaPayload, product: ProductPayload) -> float:
    return product_match_score(
        criteria,
        product,
        category_weight=FILTER_SCORE_CATEGORY,
        skin_type_weight=FILTER_SCORE_SKIN_TYPE,
        budget_weight=FILTER_SCORE_BUDGET,
        scenario_weight=FILTER_SCORE_SCENARIO,
        brand_weight=FILTER_SCORE_BRAND,
        brand_prefer_weight=FILTER_SCORE_BRAND_PREFER,
    )


def _distance_to_score(distance: float) -> float:
    return 1.0 / (1.0 + max(distance, 0.0))


# ---------------------------------------------------------------------------
# Visual similarity recall (dual-channel)
# ---------------------------------------------------------------------------


def _fetch_brand_preference_products(
    criteria: CriteriaPayload,
    filters: RetrievalFilters,
) -> list[ProductHit]:
    """Direct catalog lookup for brand_prefer products.

    When the user expresses a brand preference (brand_prefer is non-empty),
    vector similarity alone cannot guarantee those brands' products appear
    in the candidate pool. This function fetches matching products directly
    from the catalog and scores them, ensuring preferred brands are always
    represented regardless of embedding/reranker behavior.
    """
    brand_prefer = criteria.constraints.brand_prefer
    if not brand_prefer:
        return []

    brand_lower = {b.strip().lower() for b in brand_prefer}
    hits: list[ProductHit] = []
    seen: set[str] = set()
    for product in list_products():
        if product.product_id in seen:
            continue
        product_brand_lower = (product.brand or "").strip().lower()
        if product_brand_lower not in brand_lower:
            continue
        if not _passes_hard_filters(criteria, product, filters):
            continue
        seen.add(product.product_id)
        hits.append(
            ProductHit(
                product=product,
                vector_score=0.0,
                filter_score=_filter_score(criteria, product),
                chunk=None,
            )
        )
    return hits


def _build_visual_hits(
    image_hits: list[ImageSimilarityHit],
    criteria: CriteriaPayload,
    filters: RetrievalFilters,
) -> tuple[list[ProductHit], dict[str, object]]:
    """Convert ImageSimilarityHit to ProductHit, applying hard filters."""
    hits: list[ProductHit] = []
    for hit in image_hits:
        product = get_product(hit.product_id)
        if product is None or not _passes_hard_filters(criteria, product, filters):
            continue
        hits.append(
            ProductHit(
                product=product,
                vector_score=_distance_to_score(hit.distance),
                filter_score=_filter_score(criteria, product),
                chunk=None,  # visual hits have no chunk
            )
        )
    stats: dict[str, object] = {
        "pre_filter_count": len(image_hits),
        "post_filter_count": len(hits),
    }
    return hits, stats


def _elevate_brand_preference(
    criteria: CriteriaPayload,
    ranked: list[ProductHit],
    all_reranked: list[ProductHit],
) -> list[ProductHit]:
    """Guarantee brand_prefer products appear first in results.

    After reranking, preferred-brand products may be entirely excluded
    from the top-N if the reranker scored them below other products.
    This function:
    1. Injects any missing brand_prefer products from the full reranked list
    2. Moves all brand_prefer products to the front
    """
    brand_prefer = criteria.constraints.brand_prefer
    if not brand_prefer:
        return ranked
    brand_set = {b.strip().lower() for b in brand_prefer}

    seen_ids = {h.product.product_id for h in ranked}
    # Inject brand_prefer products that were excluded from top-N
    for h in all_reranked:
        if (h.product.brand or "").strip().lower() not in brand_set:
            continue
        if h.product.product_id in seen_ids:
            continue
        seen_ids.add(h.product.product_id)
        ranked.append(h)

    preferred = [h for h in ranked if (h.product.brand or "").strip().lower() in brand_set]
    rest = [h for h in ranked if (h.product.brand or "").strip().lower() not in brand_set]
    return preferred + rest


def inject_brand_preference_products(
    products: list[ProductPayload],
    criteria: CriteriaPayload,
    feedback: Mapping[str, list[str]] | None = None,
) -> list[ProductPayload]:
    """Inject missing brand_prefer products and move them to the front.

    Public API for runtime handlers — call after retrieval to guarantee
    brand_prefer products appear regardless of vector search / reranker behavior.
    """
    brand_prefer = criteria.constraints.brand_prefer
    if not brand_prefer:
        return products
    brand_set = {b.strip().lower() for b in brand_prefer}
    filters = _retrieval_filters(feedback)
    present_ids = {p.product_id for p in products}
    for p in list_products():
        if p.product_id in present_ids:
            continue
        if (p.brand or "").strip().lower() not in brand_set:
            continue
        if not _passes_hard_filters(criteria, p, filters):
            continue
        products.append(p)
        present_ids.add(p.product_id)
    preferred = [p for p in products if (p.brand or "").strip().lower() in brand_set]
    rest = [p for p in products if (p.brand or "").strip().lower() not in brand_set]
    return preferred + rest


def _merge_text_and_visual(text_hits: list[ProductHit], visual_hits: list[ProductHit]) -> list[ProductHit]:
    """Merge text and visual hits, dedup by product_id, keep higher vector_score."""
    seen: dict[str, ProductHit] = {}
    for hit in text_hits:
        pid = hit.product.product_id
        if pid not in seen:
            seen[pid] = hit
    for hit in visual_hits:
        pid = hit.product.product_id
        existing = seen.get(pid)
        if existing is None:
            seen[pid] = hit
        elif hit.vector_score > existing.vector_score:
            # Keep text hit's chunk if it has one
            seen[pid] = ProductHit(
                product=hit.product,
                vector_score=hit.vector_score,
                filter_score=max(hit.filter_score, existing.filter_score),
                chunk=existing.chunk,
            )
    return list(seen.values())


async def _supplement_visual_evidence(
    ranked_hits: list[ProductHit],
    evidence_by_product: dict[str, list[EvidencePayload]],
) -> None:
    """For visual-only hits (no chunk), load evidence from product chunks."""
    for hit in ranked_hits:
        pid = hit.product.product_id
        if hit.chunk is None and not evidence_by_product.get(pid):
            evidence = await evidence_for_product(hit.product)
            if evidence:
                evidence_by_product[pid] = evidence


def _visual_trace(visual_recall_stats: dict[str, object]) -> dict[str, object]:
    """Minimal trace for empty-result case when visual recall was attempted."""
    trace: dict[str, object] = {"selected_ids": [], "hit_count": 0, "vector_count": 0}
    if visual_recall_stats:
        trace["visual_recall"] = visual_recall_stats
    return trace
