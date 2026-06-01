"""Embedding service facade."""

from __future__ import annotations

import logging
from dataclasses import dataclass
from typing import Any

from src.config.settings import get_settings
from src.config.tuning import DEFAULT_SERVICE_TIMEOUT_SECONDS
from src.services.http_client import get_http_client
from src.services.llm_profiles import task_profile_names

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class EmbeddingProfile:
    name: str
    model: str
    base_url: str
    api_key: str
    timeout_seconds: float
    dimensions: int | None = None


class EmbeddingUnavailable(RuntimeError):
    """Raised internally when live embedding config is incomplete."""


async def embed_text(text: str) -> list[float]:
    vectors = await embed_texts([text])
    return vectors[0]


async def embed_texts(texts: list[str]) -> list[list[float]]:
    if not texts:
        return []
    profile_name = _primary_profile_name("embedding")
    try:
        profile = _resolve_embedding_profile(profile_name)
        vectors = await _embedding_request(profile, texts)
    except EmbeddingUnavailable:
        logger.info("Embedding profile unavailable for %s", profile_name, exc_info=True)
        raise
    except Exception as exc:
        logger.warning("Embedding provider failed for profile %s", profile_name, exc_info=True)
        raise EmbeddingUnavailable("Live embedding provider failed.") from exc
    if len(vectors) != len(texts):
        logger.warning(
            "Embedding profile %s returned %s vectors for %s texts",
            profile_name,
            len(vectors),
            len(texts),
        )
        raise EmbeddingUnavailable(
            f"Embedding profile {profile_name} returned {len(vectors)} vectors for {len(texts)} texts."
        )
    return vectors


def _primary_profile_name(task: str) -> str:
    names = task_profile_names(task)
    if not names:
        raise EmbeddingUnavailable(f"No profile configured for task: {task}")
    return names[0]


def _resolve_embedding_profile(profile_name: str) -> EmbeddingProfile:
    settings = get_settings()
    raw = settings.llm_profiles.get("profiles", {}).get(profile_name)
    if not raw:
        raise EmbeddingUnavailable(f"Unknown embedding profile: {profile_name}")
    base_url = settings.env_value(raw.get("base_url_env"))
    api_key = settings.env_value(raw.get("api_key_env"))
    model = raw.get("model")
    if not base_url or not api_key or not model:
        raise EmbeddingUnavailable(f"Incomplete embedding profile: {profile_name}")
    return EmbeddingProfile(
        name=profile_name,
        model=model,
        base_url=base_url,
        api_key=api_key,
        timeout_seconds=float(raw.get("timeout_seconds", DEFAULT_SERVICE_TIMEOUT_SECONDS)),
        dimensions=raw.get("dimensions"),
    )


async def _embedding_request(profile: EmbeddingProfile, texts: list[str]) -> list[list[float]]:
    payload: dict[str, Any] = {
        "model": profile.model,
        "input": texts,
    }
    if profile.dimensions:
        payload["dimensions"] = profile.dimensions

    endpoint = f"{profile.base_url.rstrip('/')}/embeddings"
    headers = {
        "Authorization": f"Bearer {profile.api_key}",
        "Content-Type": "application/json",
    }
    client = get_http_client()
    response = await client.post(endpoint, headers=headers, json=payload, timeout=profile.timeout_seconds)
    response.raise_for_status()
    data = response.json()
    rows = data.get("data") if isinstance(data, dict) else None
    if not isinstance(rows, list):
        return []
    vectors: list[list[float]] = []
    for row in sorted(rows, key=lambda item: item.get("index", 0) if isinstance(item, dict) else 0):
        embedding = row.get("embedding") if isinstance(row, dict) else None
        if not isinstance(embedding, list):
            return []
        vectors.append([float(value) for value in embedding])
    return vectors


# ---------------------------------------------------------------------------
# VL (Vision-Language) Embedding — DashScope native API
# ---------------------------------------------------------------------------


@dataclass(frozen=True)
class VLEmbeddingProfile:
    name: str
    model: str
    base_url: str
    endpoint_path: str
    api_key: str
    timeout_seconds: float
    dimensions: int | None = None


async def embed_image(image_source: str) -> list[float]:
    """Embed a single image via qwen3-vl-embedding.

    Args:
        image_source: Image URL or base64 data URI.

    Returns:
        Embedding vector (1024-dim by default).

    Raises:
        EmbeddingUnavailable: If profile is missing/incomplete or API call fails.
    """
    if not image_source:
        raise EmbeddingUnavailable("embed_image requires a non-empty image source.")
    profile_name = _primary_profile_name("vl_embedding")
    try:
        profile = _resolve_vl_embedding_profile(profile_name)
        vector = await _vl_embedding_request(profile, image_source)
    except EmbeddingUnavailable:
        logger.info("VL embedding profile unavailable for %s", profile_name, exc_info=True)
        raise
    except Exception as exc:
        logger.warning("VL embedding provider failed for profile %s", profile_name, exc_info=True)
        raise EmbeddingUnavailable("Live VL embedding provider failed.") from exc
    return vector


def _resolve_vl_embedding_profile(profile_name: str) -> VLEmbeddingProfile:
    settings = get_settings()
    raw = settings.llm_profiles.get("profiles", {}).get(profile_name)
    if not raw:
        raise EmbeddingUnavailable(f"Unknown VL embedding profile: {profile_name}")
    base_url = raw.get("base_url") or settings.env_value(raw.get("base_url_env"))
    api_key = settings.env_value(raw.get("api_key_env"))
    model = raw.get("model")
    if not base_url or not api_key or not model:
        raise EmbeddingUnavailable(f"Incomplete VL embedding profile: {profile_name}")
    return VLEmbeddingProfile(
        name=profile_name,
        model=model,
        base_url=base_url,
        endpoint_path=str(raw.get("endpoint_path") or "/embeddings/multimodal-embedding/multimodal-embedding"),
        api_key=api_key,
        timeout_seconds=float(raw.get("timeout_seconds", DEFAULT_SERVICE_TIMEOUT_SECONDS)),
        dimensions=raw.get("dimensions"),
    )


async def _vl_embedding_request(profile: VLEmbeddingProfile, image_source: str) -> list[float]:
    payload: dict[str, Any] = {
        "model": profile.model,
        "input": {
            "contents": [
                {"image": image_source}
            ]
        },
    }
    if profile.dimensions:
        payload["parameters"] = {"dimension": profile.dimensions}

    endpoint = f"{profile.base_url.rstrip('/')}/{profile.endpoint_path.strip('/')}"
    headers = {
        "Authorization": f"Bearer {profile.api_key}",
        "Content-Type": "application/json",
    }
    client = get_http_client()
    response = await client.post(endpoint, headers=headers, json=payload, timeout=profile.timeout_seconds)
    response.raise_for_status()
    data = response.json()
    embeddings = data.get("output", {}).get("embeddings") if isinstance(data, dict) else None
    if not isinstance(embeddings, list) or not embeddings:
        raise EmbeddingUnavailable("VL embedding provider returned no embeddings.")
    first = embeddings[0]
    vector = first.get("embedding") if isinstance(first, dict) else None
    if not isinstance(vector, list):
        raise EmbeddingUnavailable("VL embedding provider returned malformed embedding.")
    return [float(v) for v in vector]
