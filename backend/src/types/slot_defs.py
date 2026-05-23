"""Required slot definitions for hard-rule clarification checks."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Literal

SlotRequirement = Literal["required", "optional"]


@dataclass(frozen=True)
class SlotDef:
    name: str
    requirement: SlotRequirement
    description: str


SLOT_DEFS: dict[str, SlotDef] = {
    "category": SlotDef("category", "required", "商品品类"),
    "scenario": SlotDef("scenario", "required", "使用场景或购买目的"),
    "budget": SlotDef("budget", "optional", "预算范围"),
    "category_constraint": SlotDef("category_constraint", "optional", "品类相关约束"),
}

REQUIRED_SLOTS = tuple(name for name, slot in SLOT_DEFS.items() if slot.requirement == "required")


def is_required_slot(slot_name: str) -> bool:
    return slot_name in REQUIRED_SLOTS
