"""Embedding service facade."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any

import httpx

from src.config.settings import get_settings


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
    for profile_name in _task_profile_names("embedding"):
        try:
            profile = _resolve_embedding_profile(profile_name)
            vectors = await _embedding_request(profile, texts)
            if len(vectors) == len(texts):
                return vectors
        except EmbeddingUnavailable:
            continue
        except Exception:
            continue
    return [_deterministic_vector(text) for text in texts]


def _deterministic_vector(text: str) -> list[float]:
    values = [float((ord(ch) % 31) / 31) for ch in text[:16]]
    return values + [0.0] * (16 - len(values))


def _task_profile_names(task: str) -> list[str]:
    mapping = get_settings().task_model_map[task]
    return [name for name in (mapping.get("primary"), mapping.get("fallback")) if name]


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
        timeout_seconds=float(raw.get("timeout_seconds", 30)),
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
    async with httpx.AsyncClient(timeout=profile.timeout_seconds) as client:
        response = await client.post(endpoint, headers=headers, json=payload)
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
