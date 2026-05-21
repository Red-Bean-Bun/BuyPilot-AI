"""Product repository backed by the official ecommerce dataset.

The raw JSON files under ``data/raw/ecommerce_agent_dataset`` are the single
source of truth. Runtime payloads and database seed rows are derived from them.
"""

from __future__ import annotations

import json
from functools import lru_cache
from pathlib import Path
from typing import Any

from src.config.settings import get_settings
from src.types.sse_events import ProductPayload


SKIN_TYPES = ["油性", "混合", "混油", "干性", "敏感", "中性", "痘肌"]
INGREDIENT_TERMS = [
    "酒精",
    "香精",
    "烟酰胺",
    "透明质酸",
    "氨基酸",
    "水杨酸",
    "视黄醇",
    "神经酰胺",
    "二裂酵母",
    "马齿苋",
    "防晒",
    "控油",
    "保湿",
    "修护",
    "抗初老",
]


def dataset_dir() -> Path:
    return get_settings().dataset_dir


@lru_cache(maxsize=1)
def load_raw_products() -> tuple[dict[str, Any], ...]:
    root = dataset_dir()
    if not root.exists():
        raise FileNotFoundError(f"Product dataset not found: {root}")

    rows: list[dict[str, Any]] = []
    for path in sorted(root.glob("*/data/*.json")):
        with path.open(encoding="utf-8") as f:
            raw = json.load(f)
        raw["_source_file"] = str(path.relative_to(root))
        rows.append(raw)
    return tuple(rows)


def list_raw_products() -> list[dict[str, Any]]:
    return [dict(row) for row in load_raw_products()]


def get_raw_product(product_id: str) -> dict[str, Any] | None:
    for raw in load_raw_products():
        if raw.get("product_id") == product_id:
            return dict(raw)
    return None


def list_products() -> list[ProductPayload]:
    return [_payload_from_raw(raw) for raw in load_raw_products()]


def get_product(product_id: str) -> ProductPayload | None:
    raw = get_raw_product(product_id)
    if raw is None:
        return None
    return _payload_from_raw(raw)


def build_product_text(raw: dict[str, Any]) -> str:
    knowledge = raw.get("rag_knowledge") or {}
    parts = [
        raw.get("title", ""),
        raw.get("brand", ""),
        raw.get("category", ""),
        raw.get("sub_category", ""),
        knowledge.get("marketing_description", ""),
    ]
    for item in knowledge.get("official_faq") or []:
        parts.append(item.get("question", ""))
        parts.append(item.get("answer", ""))
    for item in knowledge.get("user_reviews") or []:
        parts.append(str(item.get("rating", "")))
        parts.append(item.get("content", ""))
    return " | ".join(str(part) for part in parts if part)


def evidence_snippet(product_id: str, max_chars: int = 180) -> str | None:
    raw = get_raw_product(product_id)
    if raw is None:
        return None
    knowledge = raw.get("rag_knowledge") or {}
    text = knowledge.get("marketing_description") or build_product_text(raw)
    return " ".join(text.split())[:max_chars]


def _payload_from_raw(raw: dict[str, Any]) -> ProductPayload:
    text = build_product_text(raw)
    return ProductPayload(
        product_id=str(raw["product_id"]),
        name=str(raw["title"]),
        price=float(raw["base_price"]) if raw.get("base_price") is not None else None,
        currency="CNY",
        image_url=str(raw.get("image_path") or ""),
        category=str(raw.get("category") or ""),
        sub_category=raw.get("sub_category"),
        brand=raw.get("brand"),
        skin_type_match=_extract_skin_types(text),
        ingredient_tags=_extract_terms(text, INGREDIENT_TERMS),
        ingredient_avoid=[],
        use_scenario=_extract_scenario(text),
    )


def _extract_terms(text: str, terms: list[str]) -> list[str]:
    return [term for term in terms if term in text]


def _extract_skin_types(text: str) -> list[str]:
    found = _extract_terms(text, SKIN_TYPES)
    normalized: list[str] = []
    for item in found:
        value = "混合性" if item in {"混合", "混油"} else item
        if value not in normalized:
            normalized.append(value)
    return normalized


def _extract_scenario(text: str) -> str | None:
    for scenario in ["通勤", "户外", "运动", "日常", "夜间", "办公", "送礼", "旅行", "露营"]:
        if scenario in text:
            return scenario
    return None
