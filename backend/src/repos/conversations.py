"""Lightweight in-memory conversation trace repository for P0."""

from __future__ import annotations

from src.types.sse_events import CriteriaPayload

_LAST_CRITERIA: dict[str, CriteriaPayload] = {}
_LAST_PRODUCT_IDS: dict[str, list[str]] = {}


def save_turn(session_id: str, criteria: CriteriaPayload | None, product_ids: list[str]) -> None:
    if criteria is not None:
        _LAST_CRITERIA[session_id] = criteria.model_copy(deep=True)
    _LAST_PRODUCT_IDS[session_id] = list(product_ids)


def get_last_criteria(session_id: str) -> CriteriaPayload | None:
    criteria = _LAST_CRITERIA.get(session_id)
    return criteria.model_copy(deep=True) if criteria else None


def get_last_product_ids(session_id: str) -> list[str]:
    return list(_LAST_PRODUCT_IDS.get(session_id, []))

