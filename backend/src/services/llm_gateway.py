"""Provider transport and profile resolution for chat-completion tasks."""

from __future__ import annotations

import json
import logging
from dataclasses import dataclass
from typing import Any

from src.config.settings import get_settings
from src.config.tuning import LLM_TEMPERATURE
from src.services.fallbacks import record_fallback
from src.services.http_client import get_http_client
from src.services.llm_profiles import task_profile_names as _task_profile_names

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class ChatProfile:
    name: str
    model: str
    base_url: str
    api_key: str
    timeout_seconds: float


class LiveLLMUnavailable(RuntimeError):
    """Raised when live provider config is incomplete or unavailable."""


async def _call_chat_task(task: str, messages: list[dict[str, Any]], json_object: bool = False) -> str:
    last_error: Exception | None = None
    for profile_name in _task_profile_names(task):
        try:
            profile = _resolve_chat_profile(profile_name)
            return await _chat_completion(profile, messages, json_object=json_object)
        except LiveLLMUnavailable as exc:
            last_error = exc
            record_fallback(f"llm.{task}", "profile_unavailable", profile=profile_name, detail=str(exc))
            logger.info("LLM profile unavailable for task %s profile %s: %s", task, profile_name, exc)
            continue
        except Exception as exc:
            last_error = exc
            record_fallback(
                f"llm.{task}",
                "provider_error",
                profile=profile_name,
                error_type=type(exc).__name__,
            )
            logger.warning(
                "LLM provider failed for task %s profile %s; trying next configured profile",
                task,
                profile_name,
                exc_info=True,
            )
            continue
    raise LiveLLMUnavailable(f"Live LLM task '{task}' has no usable provider.") from last_error


def _resolve_chat_profile(profile_name: str) -> ChatProfile:
    settings = get_settings()
    raw = settings.llm_profiles.get("profiles", {}).get(profile_name)
    if not raw:
        raise LiveLLMUnavailable(f"Unknown LLM profile: {profile_name}")
    base_url = settings.env_value(raw.get("base_url_env"))
    api_key = settings.env_value(raw.get("api_key_env"))
    model = raw.get("model")
    if not base_url or not api_key or not model:
        raise LiveLLMUnavailable(f"Incomplete LLM profile: {profile_name}")
    return ChatProfile(
        name=profile_name,
        model=model,
        base_url=base_url,
        api_key=api_key,
        timeout_seconds=float(raw.get("timeout_seconds", 30)),
    )


async def _chat_completion(profile: ChatProfile, messages: list[dict[str, Any]], json_object: bool = False) -> str:
    payload: dict[str, Any] = {
        "model": profile.model,
        "messages": messages,
        "temperature": LLM_TEMPERATURE,
    }
    if json_object:
        payload["response_format"] = {"type": "json_object"}

    endpoint = f"{profile.base_url.rstrip('/')}/chat/completions"
    headers = {
        "Authorization": f"Bearer {profile.api_key}",
        "Content-Type": "application/json",
    }
    client = get_http_client()
    response = await client.post(endpoint, headers=headers, json=payload, timeout=profile.timeout_seconds)
    response.raise_for_status()
    data = response.json()
    choices = data.get("choices") if isinstance(data, dict) else None
    if not choices:
        return ""
    message = choices[0].get("message", {})
    content = message.get("content", "")
    return content if isinstance(content, str) else json.dumps(content, ensure_ascii=False)
