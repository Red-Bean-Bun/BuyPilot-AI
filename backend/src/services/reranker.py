"""Rerank service facade."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any

import httpx

from src.config.settings import get_settings
from src.types.sse_events import CriteriaPayload, ProductPayload


@dataclass(frozen=True)
class RerankProfile:
    name: str
    model: str
    base_url: str
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
    return _deterministic_rerank(criteria, products, top_n)


async def rerank_texts(criteria: CriteriaPayload, documents: list[str], top_n: int = 5) -> list[int]:
    if not documents:
        return []
    live = await _call_rerank_texts(criteria, documents, top_n)
    if live:
        return live
    return _deterministic_rerank_texts(criteria, documents, top_n)


async def _call_rerank(criteria: CriteriaPayload, products: list[ProductPayload], top_n: int) -> list[ProductPayload] | None:
    for profile_name in _task_profile_names("rerank"):
        try:
            profile = _resolve_rerank_profile(profile_name)
            indexes = await _rerank_request(profile, _query_text(criteria), [_document_text(product) for product in products], top_n)
            ranked = [products[index] for index in indexes if 0 <= index < len(products)]
            if ranked:
                return ranked
        except RerankUnavailable:
            continue
        except Exception:
            continue
    return None


async def _call_rerank_texts(criteria: CriteriaPayload, documents: list[str], top_n: int) -> list[int] | None:
    for profile_name in _task_profile_names("rerank"):
        try:
            profile = _resolve_rerank_profile(profile_name)
            indexes = await _rerank_request(profile, _query_text(criteria), documents, top_n)
            ranked = [index for index in indexes if 0 <= index < len(documents)]
            if ranked:
                return ranked
        except RerankUnavailable:
            continue
        except Exception:
            continue
    return None


def _deterministic_rerank(criteria: CriteriaPayload, products: list[ProductPayload], top_n: int = 5) -> list[ProductPayload]:
    def score(product: ProductPayload) -> tuple[int, float]:
        hard = 0
        if product.category == criteria.category:
            hard += 3
        if criteria.constraints.skin_type and criteria.constraints.skin_type in product.skin_type_match:
            hard += 2
        if criteria.constraints.budget_max is not None and product.price is not None and product.price <= criteria.constraints.budget_max:
            hard += 1
        if any(item in " ".join(product.ingredient_tags + product.ingredient_avoid) for item in criteria.constraints.ingredient_avoid):
            hard -= 5
        return hard, -(product.price or 0.0)

    return sorted(products, key=score, reverse=True)[:top_n]


def _deterministic_rerank_texts(criteria: CriteriaPayload, documents: list[str], top_n: int = 5) -> list[int]:
    query_tokens = set(_query_text(criteria).split())

    def score(row: tuple[int, str]) -> tuple[int, int]:
        index, document = row
        exact_matches = sum(1 for token in query_tokens if token and token in document)
        avoid_penalty = sum(1 for token in criteria.constraints.ingredient_avoid if token and token in document)
        return exact_matches - (avoid_penalty * 3), -index

    rows = list(enumerate(documents))
    return [index for index, _ in sorted(rows, key=score, reverse=True)[:top_n]]


def _task_profile_names(task: str) -> list[str]:
    mapping = get_settings().task_model_map[task]
    return [name for name in (mapping.get("primary"), mapping.get("fallback")) if name]


def _resolve_rerank_profile(profile_name: str) -> RerankProfile:
    settings = get_settings()
    raw = settings.llm_profiles.get("profiles", {}).get(profile_name)
    if not raw:
        raise RerankUnavailable(f"Unknown rerank profile: {profile_name}")
    base_url = settings.env_value(raw.get("base_url_env"))
    api_key = settings.env_value(raw.get("api_key_env"))
    model = raw.get("model")
    if not base_url or not api_key or not model:
        raise RerankUnavailable(f"Incomplete rerank profile: {profile_name}")
    return RerankProfile(
        name=profile_name,
        model=model,
        base_url=base_url,
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
    endpoint = f"{profile.base_url.rstrip('/')}/reranks"
    headers = {
        "Authorization": f"Bearer {profile.api_key}",
        "Content-Type": "application/json",
    }
    async with httpx.AsyncClient(timeout=profile.timeout_seconds) as client:
        response = await client.post(endpoint, headers=headers, json=payload)
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


def _document_text(product: ProductPayload) -> str:
    parts = [
        product.name,
        product.brand or "",
        product.category,
        product.sub_category or "",
        f"{product.price:g}元" if product.price is not None else "",
        " ".join(product.skin_type_match),
        " ".join(product.ingredient_tags),
        product.use_scenario or "",
    ]
    return " | ".join(part for part in parts if part)
