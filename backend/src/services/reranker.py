"""Rerank service facade."""

from __future__ import annotations

import logging
from dataclasses import dataclass
from typing import Any

from src.config.settings import get_settings
from src.config.tuning import DEFAULT_SERVICE_TIMEOUT_SECONDS
from src.services.http_client import get_http_client
from src.services.llm_profiles import task_profile_names
from src.services.retrieval_features import (
    criteria_query_text,
    product_document_text,
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
    return await _call_rerank(criteria, products, top_n)


async def rerank_texts(criteria: CriteriaPayload, documents: list[str], top_n: int = 5) -> list[int]:
    if not documents:
        return []
    return await _call_rerank_texts(criteria, documents, top_n)


async def _call_rerank(criteria: CriteriaPayload, products: list[ProductPayload], top_n: int) -> list[ProductPayload]:
    profile_name = _primary_profile_name("rerank")
    try:
        profile = _resolve_rerank_profile(profile_name)
        indexes = await _rerank_request(
            profile, criteria_query_text(criteria), [product_document_text(product) for product in products], top_n
        )
    except RerankUnavailable:
        logger.info("Rerank profile unavailable for %s", profile_name, exc_info=True)
        raise
    except Exception as exc:
        logger.warning("Rerank provider failed for profile %s", profile_name, exc_info=True)
        raise RerankUnavailable("Live product rerank provider failed.") from exc
    ranked = [products[index] for index in indexes if 0 <= index < len(products)]
    if not ranked:
        logger.warning("Rerank profile %s returned no valid product ranking", profile_name)
        raise RerankUnavailable(f"Rerank profile {profile_name} returned no valid product ranking.")
    return ranked


async def _call_rerank_texts(criteria: CriteriaPayload, documents: list[str], top_n: int) -> list[int]:
    profile_name = _primary_profile_name("rerank")
    try:
        profile = _resolve_rerank_profile(profile_name)
        indexes = await _rerank_request(profile, criteria_query_text(criteria), documents, top_n)
    except RerankUnavailable:
        logger.info("Rerank profile unavailable for %s", profile_name, exc_info=True)
        raise
    except Exception as exc:
        logger.warning("Rerank provider failed for profile %s", profile_name, exc_info=True)
        raise RerankUnavailable("Live text rerank provider failed.") from exc
    ranked = [index for index in indexes if 0 <= index < len(documents)]
    if not ranked:
        logger.warning("Rerank profile %s returned no valid text ranking", profile_name)
        raise RerankUnavailable(f"Rerank profile {profile_name} returned no valid text ranking.")
    return ranked


def _primary_profile_name(task: str) -> str:
    names = task_profile_names(task)
    if not names:
        raise RerankUnavailable(f"No profile configured for task: {task}")
    return names[0]


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
        timeout_seconds=float(raw.get("timeout_seconds", DEFAULT_SERVICE_TIMEOUT_SECONDS)),
    )


async def _rerank_request(profile: RerankProfile, query: str, documents: list[str], top_n: int) -> list[int]:
    payload: dict[str, Any] = {
        "model": profile.model,
        "input": {
            "query": query,
            "documents": documents,
        },
        "parameters": {
            "top_n": min(top_n, len(documents)),
        },
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
    results = data.get("output", {}).get("results") if isinstance(data, dict) else None
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
