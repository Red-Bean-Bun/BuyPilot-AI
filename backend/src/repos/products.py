"""Product repository backed by the official ecommerce dataset.

The raw JSON files under ``data/raw/ecommerce_agent_dataset`` are the single
source of truth. Runtime payloads and database seed rows are derived from them.
"""

from __future__ import annotations

import json
from functools import lru_cache
from pathlib import Path
from typing import Any

from src.config.domain_terms import (
    INGREDIENT_TERMS,
    SKIN_TERMS,
    scenario_from_text,
    extract_skin_types,
    extract_terms,
    has_negation_prefix,
    normalize_category,
)
from src.config.settings import get_settings
from src.types.sse_events import ProductPayload


SKIN_TYPES = list(SKIN_TERMS)
PRODUCT_ASSET_URL_PREFIX = "/assets/products"


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
    return [product.model_copy(deep=True) for product in _product_payloads()]


def get_product(product_id: str) -> ProductPayload | None:
    product = _product_index().get(product_id)
    return product.model_copy(deep=True) if product else None


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
    source_category = str(raw.get("category") or "")
    sku_options = _normalize_sku_options(raw.get("skus"))
    return ProductPayload(
        product_id=str(raw["product_id"]),
        name=str(raw["title"]),
        price=float(raw["base_price"]) if raw.get("base_price") is not None else None,
        currency="CNY",
        image_url=public_product_image_url(raw.get("image_path")),
        category=normalize_category(source_category) or source_category,
        sub_category=raw.get("sub_category"),
        brand=raw.get("brand"),
        skin_type_match=_extract_skin_types(text),
        ingredient_tags=_extract_terms(text, INGREDIENT_TERMS),
        ingredient_avoid=[],
        use_scenario=_extract_scenario(text),
        sku_options=sku_options,
    )


def public_product_image_url(image_path: Any) -> str | None:
    path = str(image_path or "").strip().lstrip("/")
    if not path:
        return None
    return f"{PRODUCT_ASSET_URL_PREFIX}/{path}"


def _normalize_sku_options(skus: Any) -> list[dict[str, Any]] | None:
    """Convert raw SKU list to sku_options format.

    Raw format: [{sku_id, properties: {key: value, ...}, price}]
    Target format: [{sku_id, properties: {key: value, ...}, price}]
    They're structurally identical — just validate that the input is a non-empty list.
    """
    if not skus or not isinstance(skus, list):
        return None
    options: list[dict[str, Any]] = []
    for sku in skus:
        if not isinstance(sku, dict) or "sku_id" not in sku:
            continue
        options.append(
            {
                "sku_id": str(sku["sku_id"]),
                "properties": sku.get("properties") or {},
                "price": sku.get("price"),
            }
        )
    return options or None


@lru_cache(maxsize=1)
def _product_payloads() -> tuple[ProductPayload, ...]:
    return tuple(_payload_from_raw(raw) for raw in load_raw_products())


@lru_cache(maxsize=1)
def _product_index() -> dict[str, ProductPayload]:
    return {product.product_id: product for product in _product_payloads()}


def _extract_terms(text: str, terms: list[str] | tuple[str, ...]) -> list[str]:
    return extract_terms(text, terms)


def _has_negation_prefix(text: str, term: str) -> bool:
    return has_negation_prefix(text, term)


def _extract_skin_types(text: str) -> list[str]:
    return extract_skin_types(text)


def _extract_scenario(text: str) -> str | None:
    return scenario_from_text(text)
