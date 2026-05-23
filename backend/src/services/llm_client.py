"""Task-oriented LLM client.

Runtime stages call these task methods only. They must not choose raw model
names or call provider SDKs directly.
"""

from __future__ import annotations

import json
import re
from dataclasses import dataclass
from typing import Any

import httpx
from pydantic import ValidationError

from src.config.settings import get_settings
from src.services.image_upload import image_url_to_provider_url
from src.types.schemas import DecisionResult, IntentResult, RecommendationResult
from src.types.sse_events import Constraints, CriteriaPayload, ProductPayload


@dataclass(frozen=True)
class ChatProfile:
    name: str
    model: str
    base_url: str
    api_key: str
    timeout_seconds: float


class LiveLLMUnavailable(RuntimeError):
    """Raised internally when live provider config is incomplete or disabled."""


def _category_from_text(text: str) -> str | None:
    if any(token in text for token in ("洗面奶", "防晒", "护肤", "肤", "洁面", "面霜")):
        return "美妆护肤"
    if any(token in text for token in ("耳机", "手机", "电脑", "数码")):
        return "数码电子"
    if any(token in text for token in ("跑鞋", "运动", "衣服", "服饰")):
        return "服饰运动"
    if any(token in text for token in ("食品", "饮料", "零食", "无糖")):
        return "食品饮料"
    return None


def _budget_from_text(text: str) -> float | None:
    match = re.search(r"预算\s*(?:降到|控制在|不超过|约|大概)?\s*(\d+(?:\.\d+)?)", text)
    if match:
        return float(match.group(1))
    match = re.search(r"(\d+(?:\.\d+)?)\s*元\s*(?:以内|以下|内)?", text)
    if match:
        return float(match.group(1))
    match = re.search(r"(\d+(?:\.\d+)?)\s*(?:以内|以下|内)", text)
    return float(match.group(1)) if match else None


def _skin_type_from_text(text: str) -> str | None:
    mapping = {
        "油皮": "油性",
        "油性": "油性",
        "混合": "混合性",
        "敏感": "敏感",
        "干皮": "干性",
        "干性": "干性",
    }
    for token, value in mapping.items():
        if token in text:
            return value
    return None


def _ingredient_avoid_from_text(text: str) -> list[str]:
    avoids: list[str] = []
    for token in ("酒精", "香精", "小零件"):
        if token in text and any(prefix in text for prefix in ("不要", "不含", "避免", "无")):
            avoids.append(token)
    return avoids


async def analyze_intent(message: str, history: list[dict[str, Any]] | None = None, image_url: str | None = None) -> IntentResult:
    live = await _call_chat_task(
        "analyze_intent",
        [
            {
                "role": "system",
                "content": (
                    "你是电商导购意图识别器。只输出 JSON，字段为 intent、confidence、category、"
                    "extracted_constraints、soft_preferences、target_product_id。intent 只能是 "
                    "recommend/clarify/feedback/add_to_cart/view_cart/chitchat。"
                ),
            },
            {"role": "user", "content": _history_prompt(message, history, image_url)},
        ],
        json_object=True,
    )
    if live:
        parsed = _parse_json_object(live)
        if parsed:
            try:
                return IntentResult.model_validate(parsed)
            except ValidationError:
                pass
    if any(token in message for token in ("加到购物车", "加入购物车", "加购", "买这个")):
        return IntentResult(intent="add_to_cart")
    if any(token in message for token in ("购物车", "看看车", "查看车")):
        return IntentResult(intent="view_cart")
    if any(token in message for token in ("不喜欢", "不要这个", "换一个", "太贵")):
        return IntentResult(intent="feedback", extracted_constraints={"feedback_text": message})

    constraints: dict[str, Any] = {}
    category = _category_from_text(message)
    if category is None and any(token in message for token in ("买", "推荐", "预算", "适合")):
        category = "美妆护肤"
    budget = _budget_from_text(message)
    skin_type = _skin_type_from_text(message)
    ingredient_avoid = _ingredient_avoid_from_text(message)
    if budget is not None:
        constraints["budget_max"] = budget
    if skin_type:
        constraints["skin_type"] = skin_type
    if ingredient_avoid:
        constraints["ingredient_avoid"] = ingredient_avoid
    if image_url:
        constraints["image_url"] = image_url
    return IntentResult(
        intent="recommend",
        category=category,
        extracted_constraints=constraints,
        soft_preferences=[message],
    )


async def generate_criteria(
    message: str,
    intent: IntentResult,
    feedback: dict[str, list[str]] | None = None,
    existing: CriteriaPayload | None = None,
) -> CriteriaPayload:
    live = await _call_chat_task(
        "generate_criteria",
        [
            {
                "role": "system",
                "content": (
                    "你是电商导购购买标准生成器。只输出 JSON，字段为 criteria_id、category、summary、"
                    "chips、constraints。constraints 必须只使用允许字段：budget_min,budget_max,"
                    "use_scenario,skin_type,ingredient_avoid,ingredient_prefer,storage,screen_size,"
                    "sport_type,season,dietary。不要输出商品。"
                ),
            },
            {
                "role": "user",
                "content": json.dumps(
                    {
                        "message": message,
                        "intent": intent.model_dump(),
                        "feedback": feedback or {},
                        "existing": existing.model_dump() if existing else None,
                    },
                    ensure_ascii=False,
                ),
            },
        ],
        json_object=True,
    )
    if live:
        parsed = _parse_json_object(live)
        criteria = _criteria_from_live_payload(parsed, existing)
        if criteria:
            return criteria
    base = existing.model_copy(deep=True) if existing else CriteriaPayload()
    category = intent.category or base.category or _category_from_text(message) or "美妆护肤"
    constraints = base.constraints.model_copy(deep=True) if base.constraints else Constraints()
    updates = intent.extracted_constraints
    constraints = constraints.model_copy(update={k: v for k, v in updates.items() if hasattr(constraints, k)})
    if constraints.use_scenario is None:
        constraints.use_scenario = _scenario_from_text(message)
    if feedback:
        constraints.ingredient_avoid = list(dict.fromkeys(constraints.ingredient_avoid + feedback.get("avoid_traits", [])))

    chips = _chips_for_constraints(category, constraints)
    summary = "，".join(chips) if chips else f"{category}导购"
    return CriteriaPayload(
        criteria_id=base.criteria_id or "c_auto_001",
        category=category,
        summary=summary,
        chips=chips,
        constraints=constraints,
    )


async def generate_recommendation(criteria: CriteriaPayload, products: list[ProductPayload]) -> RecommendationResult:
    live = await _call_chat_task(
        "generate_recommendation",
        [
            {
                "role": "system",
                "content": (
                    "你是电商导购推荐解释生成器。只输出 JSON，字段为 text_chunks。"
                    "只能解释传入商品，不得编造商品、价格、优惠或库存。"
                ),
            },
            {
                "role": "user",
                "content": json.dumps(
                    {
                        "criteria": criteria.model_dump(),
                        "products": [product.model_dump() for product in products],
                    },
                    ensure_ascii=False,
                ),
            },
        ],
        json_object=True,
    )
    if live:
        parsed = _parse_json_object(live)
        chunks = parsed.get("text_chunks") if parsed else None
        if isinstance(chunks, list) and all(isinstance(chunk, str) for chunk in chunks):
            return RecommendationResult(text_chunks=chunks[:4], products=products)
    if not products:
        return RecommendationResult(text_chunks=["没有找到完全匹配的商品，我会放宽软偏好再试。"])
    category = criteria.category or "商品"
    return RecommendationResult(
        text_chunks=[
            f"我先按{criteria.summary or category}来筛选。",
            "下面给你几个更匹配的选择。",
        ],
        products=products,
    )


async def analyze_image(image_url: str) -> dict[str, Any]:
    provider_image_url = image_url_to_provider_url(image_url)
    live = await _call_chat_task(
        "analyze_image",
        [
            {
                "role": "system",
                "content": "你是商品图片理解器。只输出 JSON，字段为 category_hint、description、visible_traits。",
            },
            {
                "role": "user",
                "content": [
                    {"type": "text", "text": "请识别这张商品图片，输出适合导购检索的简短结构化信息。"},
                    {"type": "image_url", "image_url": {"url": provider_image_url}},
                ],
            },
        ],
        json_object=True,
    )
    if live:
        parsed = _parse_json_object(live)
        if parsed:
            parsed["image_url"] = image_url
            return parsed
    return {"image_url": image_url, "category_hint": "美妆护肤", "description": "图片已接收，P0 使用文本约束继续导购。"}


async def generate_decision(criteria: CriteriaPayload, products: list[ProductPayload]) -> DecisionResult:
    if not products:
        return DecisionResult(winner_product_id="", summary="暂时没有找到合适商品。")
    valid_ids = [p.product_id for p in products]
    live = await _call_chat_task(
        "generate_decision",
        [
            {
                "role": "system",
                "content": (
                    "你是电商导购决策器。只输出 JSON，字段为 winner_product_id、summary、why、not_for。"
                    "winner_product_id 必须是传入商品之一，不得编造。"
                    "why 是选择该商品的理由列表（每条一句话）。"
                    "not_for 是不适合人群或场景列表。"
                ),
            },
            {
                "role": "user",
                "content": json.dumps(
                    {
                        "criteria": criteria.model_dump(),
                        "products": [p.model_dump() for p in products],
                        "valid_winner_ids": valid_ids,
                    },
                    ensure_ascii=False,
                ),
            },
        ],
        json_object=True,
    )
    if live:
        parsed = _parse_json_object(live)
        if parsed:
            winner_id = parsed.get("winner_product_id", "")
            if winner_id in valid_ids:
                return DecisionResult(
                    winner_product_id=winner_id,
                    summary=parsed.get("summary", f"优先选{winner_id}。"),
                    why=parsed.get("why", ["综合匹配度最高"]) if isinstance(parsed.get("why"), list) else ["综合匹配度最高"],
                    not_for=parsed.get("not_for", []) if isinstance(parsed.get("not_for"), list) else [],
                )
    winner = products[0]
    why = []
    if winner.price is not None:
        why.append(f"{winner.price:g}元")
    if criteria.constraints.skin_type and criteria.constraints.skin_type in winner.skin_type_match:
        why.append(f"{criteria.constraints.skin_type}适用")
    if winner.use_scenario:
        why.append(winner.use_scenario)
    if not why:
        why.append("综合匹配度最高")
    return DecisionResult(
        winner_product_id=winner.product_id,
        summary=f"优先选{winner.name}。",
        why=why,
        not_for=[],
    )


def _scenario_from_text(text: str) -> str | None:
    if any(token in text for token in ("日常", "每天", "通勤")):
        return "日常护肤" if _category_from_text(text) == "美妆护肤" else "日常使用"
    if any(token in text for token in ("户外", "防晒")):
        return "户外防晒"
    if any(token in text for token in ("送", "礼物")):
        return "送礼"
    if _category_from_text(text):
        return "日常使用"
    return None


def _chips_for_constraints(category: str, constraints: Constraints) -> list[str]:
    chips = [category]
    if constraints.skin_type:
        chips.append(f"{constraints.skin_type}肌肤")
    if constraints.budget_max is not None:
        chips.append(f"{constraints.budget_max:g}元内")
    if constraints.use_scenario:
        chips.append(constraints.use_scenario)
    for item in constraints.ingredient_avoid:
        chips.append(f"不要{item}")
    return chips


def _history_prompt(message: str, history: list[dict[str, Any]] | None, image_url: str | None) -> str:
    return json.dumps(
        {
            "message": message,
            "history": history or [],
            "image_url": image_url,
        },
        ensure_ascii=False,
    )


async def _call_chat_task(task: str, messages: list[dict[str, Any]], json_object: bool = False) -> str | None:
    for profile_name in _task_profile_names(task):
        try:
            profile = _resolve_chat_profile(profile_name)
            return await _chat_completion(profile, messages, json_object=json_object)
        except LiveLLMUnavailable:
            continue
        except Exception:
            continue
    return None


def _task_profile_names(task: str) -> list[str]:
    mapping = get_settings().task_model_map[task]
    return [name for name in (mapping.get("primary"), mapping.get("fallback")) if name]


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
        "temperature": 0.2,
    }
    if json_object:
        payload["response_format"] = {"type": "json_object"}

    endpoint = f"{profile.base_url.rstrip('/')}/chat/completions"
    headers = {
        "Authorization": f"Bearer {profile.api_key}",
        "Content-Type": "application/json",
    }
    async with httpx.AsyncClient(timeout=profile.timeout_seconds) as client:
        response = await client.post(endpoint, headers=headers, json=payload)
        response.raise_for_status()
    data = response.json()
    choices = data.get("choices") if isinstance(data, dict) else None
    if not choices:
        return ""
    message = choices[0].get("message", {})
    content = message.get("content", "")
    return content if isinstance(content, str) else json.dumps(content, ensure_ascii=False)


def _parse_json_object(text: str) -> dict[str, Any] | None:
    try:
        parsed = json.loads(text)
        return parsed if isinstance(parsed, dict) else None
    except json.JSONDecodeError:
        match = re.search(r"\{.*\}", text, flags=re.S)
        if not match:
            return None
        try:
            parsed = json.loads(match.group(0))
        except json.JSONDecodeError:
            return None
        return parsed if isinstance(parsed, dict) else None


def _criteria_from_live_payload(payload: dict[str, Any] | None, existing: CriteriaPayload | None) -> CriteriaPayload | None:
    if not payload:
        return None
    base = existing.model_copy(deep=True) if existing else CriteriaPayload()
    raw_constraints = payload.get("constraints") if isinstance(payload.get("constraints"), dict) else {}
    try:
        constraints = Constraints.model_validate(
            {
                **base.constraints.model_dump(),
                **raw_constraints,
            }
        )
        criteria = CriteriaPayload.model_validate(
            {
                "criteria_id": payload.get("criteria_id") or base.criteria_id or "c_auto_001",
                "category": payload.get("category") or base.category or "美妆护肤",
                "summary": payload.get("summary") or base.summary,
                "chips": payload.get("chips") if isinstance(payload.get("chips"), list) else [],
                "constraints": constraints.model_dump(),
            }
        )
    except ValidationError:
        return None
    if not criteria.chips:
        criteria.chips = _chips_for_constraints(criteria.category, criteria.constraints)
    if not criteria.summary:
        criteria.summary = "，".join(criteria.chips) if criteria.chips else f"{criteria.category}导购"
    return criteria
