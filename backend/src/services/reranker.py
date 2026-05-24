"""Rerank service facade."""

from __future__ import annotations

import logging
from dataclasses import dataclass
from typing import Any

from src.config.settings import get_settings
from src.config.tuning import (
    RERANK_AVOID_PENALTY,
    RERANK_SCORE_BUDGET,
    RERANK_SCORE_CATEGORY,
    RERANK_SCORE_SKIN_TYPE,
    TEXT_RERANK_AVOID_PENALTY,
)
from src.services.fallbacks import record_fallback
from src.services.http_client import get_http_client
from src.services.llm_profiles import task_profile_names
from src.services.retrieval_features import (
    avoid_trait_penalty,
    criteria_query_text,
    product_document_text,
    product_match_score,
)
from src.types.sse_events import CriteriaPayload, ProductPayload

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class RerankProfile:
    name: str
    model: str
    base_url: str
    endpoint_path: str
    api_key: str
    timeout_seconds: float


class RerankUnavailable(RuntimeError):
    """Raised internally when live rerank config is incomplete."""


async def rerank(criteria: CriteriaPayload, products: list[ProductPayload], top_n: int = 5) -> list[ProductPayload]:
    if not products:
        return []
    live = await _call_rerank(criteria, products, top_n)
    if live:
        return live
    if get_settings().strict_runtime:
        raise RerankUnavailable("Strict runtime requires live product rerank; deterministic fallback is disabled.")
    record_fallback("rerank.products", "deterministic_fallback", count=len(products))
    return _deterministic_rerank(criteria, products, top_n)


async def rerank_texts(criteria: CriteriaPayload, documents: list[str], top_n: int = 5) -> list[int]:
    if not documents:
        return []
    live = await _call_rerank_texts(criteria, documents, top_n)
    if live:
        return live
    if get_settings().strict_runtime:
        raise RerankUnavailable("Strict runtime requires live text rerank; deterministic fallback is disabled.")
    record_fallback("rerank.texts", "deterministic_fallback", count=len(documents))
    return _deterministic_rerank_texts(criteria, documents, top_n)


async def _call_rerank(
    criteria: CriteriaPayload, products: list[ProductPayload], top_n: int
) -> list[ProductPayload] | None:
    for profile_name in task_profile_names("rerank"):
        try:
            profile = _resolve_rerank_profile(profile_name)
            indexes = await _rerank_request(
                profile, criteria_query_text(criteria), [product_document_text(product) for product in products], top_n
            )
            ranked = [products[index] for index in indexes if 0 <= index < len(products)]
            if ranked:
                return ranked
            record_fallback("rerank.products", "empty_live_result", profile=profile_name)
            logger.warning("Rerank profile %s returned no valid product ranking; trying fallback", profile_name)
        except RerankUnavailable as exc:
            record_fallback("rerank.products", "profile_unavailable", profile=profile_name, detail=str(exc))
            logger.info("Rerank profile unavailable for %s: %s", profile_name, exc)
            continue
        except Exception as exc:
            record_fallback("rerank.products", "provider_error", profile=profile_name, error_type=type(exc).__name__)
            logger.warning("Rerank provider failed for profile %s; trying fallback", profile_name, exc_info=True)
            continue
    return None


async def _call_rerank_texts(criteria: CriteriaPayload, documents: list[str], top_n: int) -> list[int] | None:
    for profile_name in task_profile_names("rerank"):
        try:
            profile = _resolve_rerank_profile(profile_name)
            indexes = await _rerank_request(profile, criteria_query_text(criteria), documents, top_n)
            ranked = [index for index in indexes if 0 <= index < len(documents)]
            if ranked:
                return ranked
            record_fallback("rerank.texts", "empty_live_result", profile=profile_name)
            logger.warning("Rerank profile %s returned no valid text ranking; trying fallback", profile_name)
        except RerankUnavailable as exc:
            record_fallback("rerank.texts", "profile_unavailable", profile=profile_name, detail=str(exc))
            logger.info("Rerank profile unavailable for %s: %s", profile_name, exc)
            continue
        except Exception as exc:
            record_fallback("rerank.texts", "provider_error", profile=profile_name, error_type=type(exc).__name__)
            logger.warning("Rerank provider failed for profile %s; trying fallback", profile_name, exc_info=True)
            continue
    return None


def _deterministic_rerank(
    criteria: CriteriaPayload, products: list[ProductPayload], top_n: int = 5
) -> list[ProductPayload]:
    def score(product: ProductPayload) -> tuple[float, float]:
        hard = product_match_score(
            criteria,
            product,
            category_weight=RERANK_SCORE_CATEGORY,
            skin_type_weight=RERANK_SCORE_SKIN_TYPE,
            budget_weight=RERANK_SCORE_BUDGET,
        )
        if avoid_trait_penalty(criteria, product):
            hard -= RERANK_AVOID_PENALTY
        return hard, -(product.price or 0.0)

    return sorted(products, key=score, reverse=True)[:top_n]


def _deterministic_rerank_texts(criteria: CriteriaPayload, documents: list[str], top_n: int = 5) -> list[int]:
    query_tokens = set(criteria_query_text(criteria).split())

    def score(row: tuple[int, str]) -> tuple[int, int]:
        index, document = row
        exact_matches = sum(1 for token in query_tokens if token and token in document)
        avoid_penalty = sum(1 for token in criteria.constraints.ingredient_avoid if token and token in document)
        return exact_matches - (avoid_penalty * TEXT_RERANK_AVOID_PENALTY), -index

    rows = list(enumerate(documents))
    return [index for index, _ in sorted(rows, key=score, reverse=True)[:top_n]]


def _resolve_rerank_profile(profile_name: str) -> RerankProfile:
    settings = get_settings()
    raw = settings.llm_profiles.get("profiles", {}).get(profile_name)
    if not raw:
        raise RerankUnavailable(f"Unknown rerank profile: {profile_name}")
    base_url = raw.get("base_url") or settings.env_value(raw.get("base_url_env"))
    api_key = settings.env_value(raw.get("api_key_env"))
    model = raw.get("model")
    if not base_url or not api_key or not model:
        raise RerankUnavailable(f"Incomplete rerank profile: {profile_name}")
    return RerankProfile(
        name=profile_name,
        model=model,
        base_url=base_url,
        endpoint_path=str(raw.get("endpoint_path") or "/reranks"),
        api_key=api_key,
        timeout_seconds=float(raw.get("timeout_seconds", 30)),
    )


async def _rerank_request(profile: RerankProfile, query: str, documents: list[str], top_n: int) -> list[int]:
    payload: dict[str, Any] = {
        "model": profile.model,
        "query": query,
        "documents": documents,
        "top_n": min(top_n, len(documents)),
        "instruct": "Rank ecommerce product passages by relevance to the shopper's buying criteria.",
    }
    endpoint = _rerank_endpoint(profile)
    headers = {
        "Authorization": f"Bearer {profile.api_key}",
        "Content-Type": "application/json",
    }
    client = get_http_client()
    response = await client.post(endpoint, headers=headers, json=payload, timeout=profile.timeout_seconds)
    response.raise_for_status()
    data = response.json()
    results = data.get("results") if isinstance(data, dict) else None
    if not isinstance(results, list):
        return []
    ranked: list[tuple[int, float]] = []
    for item in results:
        if not isinstance(item, dict):
            continue
        index = item.get("index")
        score = item.get("relevance_score", 0.0)
        if isinstance(index, int):
            ranked.append((index, float(score)))
    return [index for index, _ in sorted(ranked, key=lambda row: row[1], reverse=True)]


def _rerank_endpoint(profile: RerankProfile) -> str:
    return f"{profile.base_url.rstrip('/')}/{profile.endpoint_path.strip('/')}"
