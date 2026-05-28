"""Criteria generation, criteria_patch merging, and feedback injection."""

from __future__ import annotations

from typing import Any

from src.config.tuning import CHEAPER_BUDGET_DEFAULT_MAX
from src.services.conversation_state import get_conversation_summary, get_previous_criteria
from src.services.feedback import get_feedback_context
from src.services.llm_client import generate_criteria
from src.types.schemas import ChatStreamRequest, IntentResult
from src.types.sse_events import Constraints, CriteriaPayload, QuickActionPayload


async def run_criteria(session_id: str, body: ChatStreamRequest, intent: IntentResult) -> CriteriaPayload:
    existing = await get_previous_criteria(session_id)
    if body.criteria_patch:
        return apply_criteria_patch(existing or CriteriaPayload(criteria_id="c_auto_001"), body.criteria_patch)
    feedback = await get_feedback_context(session_id)
    ctx_summary = await get_conversation_summary(session_id)
    criteria = await generate_criteria(
        body.message, intent, feedback=feedback, existing=existing, conversation_context=ctx_summary
    )
    return annotate_criteria_sources(criteria, intent, existing)


_MERGE_LIST_FIELDS = {"ingredient_avoid", "ingredient_prefer", "brand_avoid", "origin_avoid", "dietary"}


def apply_criteria_patch(criteria: CriteriaPayload, patch: dict[str, Any]) -> CriteriaPayload:
    raw_constraints = patch.get("constraints", patch)
    allowed = set(Constraints.model_fields)
    updates = {key: value for key, value in raw_constraints.items() if key in allowed}
    for key in _MERGE_LIST_FIELDS & updates.keys():
        updates[key] = list(dict.fromkeys([*(getattr(criteria.constraints, key) or []), *(updates[key] or [])]))
    constraints = criteria.constraints.model_copy(update=updates)

    old_constraint_chips = set(_constraint_chips(criteria.constraints))
    chips = [c for c in criteria.chips if c not in old_constraint_chips and c != criteria.category]
    if criteria.category:
        chips.insert(0, criteria.category)
    chips.extend(_constraint_chips(constraints))

    field_sources = dict(criteria.field_sources)
    for key in updates:
        field_sources[f"constraints.{key}"] = "user"
    return criteria.model_copy(
        update={"constraints": constraints, "chips": chips, "summary": "，".join(chips), "field_sources": field_sources}
    )


def annotate_criteria_sources(
    criteria: CriteriaPayload,
    intent: IntentResult,
    existing: CriteriaPayload | None,
) -> CriteriaPayload:
    explicit_fields = _explicit_source_fields(intent)
    history_fields = _history_source_fields(criteria, existing)
    field_sources: dict[str, str] = {}
    if criteria.category:
        field_sources["category"] = (
            "user" if "category" in explicit_fields else "history" if "category" in history_fields else "inferred"
        )

    constraints_update: dict[str, Any] = {}
    for key in Constraints.model_fields:
        field = f"constraints.{key}"
        value = getattr(criteria.constraints, key)
        if not _has_value(value):
            continue
        if field in explicit_fields:
            field_sources[field] = "user"
            continue
        if field in history_fields:
            field_sources[field] = "history"
            continue
        if key in _HARD_CONSTRAINT_FIELDS:
            constraints_update[key] = [] if isinstance(value, list) else None
            continue
        field_sources[field] = "inferred"

    if constraints_update:
        constraints = criteria.constraints.model_copy(update=constraints_update)
        criteria = criteria.model_copy(update={"constraints": constraints})
    chips = [criteria.category] if criteria.category else []
    chips.extend(_constraint_chips(criteria.constraints))
    return criteria.model_copy(update={"field_sources": field_sources, "chips": chips, "summary": "，".join(chips)})


_HARD_CONSTRAINT_FIELDS = {"budget_min", "budget_max", "brand_avoid", "origin_avoid", "ingredient_avoid"}


def _explicit_source_fields(intent: IntentResult) -> set[str]:
    fields: set[str] = set()
    if intent.category:
        fields.add("category")
    for key, value in (intent.extracted_constraints or {}).items():
        if key in Constraints.model_fields and _has_value(value):
            fields.add(f"constraints.{key}")
    return fields


def _history_source_fields(criteria: CriteriaPayload, existing: CriteriaPayload | None) -> set[str]:
    if existing is None:
        return set()
    fields: set[str] = set()
    if criteria.category and criteria.category == existing.category:
        fields.add("category")
    for key in Constraints.model_fields:
        value = getattr(criteria.constraints, key)
        old_value = getattr(existing.constraints, key)
        if _has_value(value) and value == old_value:
            fields.add(f"constraints.{key}")
    return fields


def _has_value(value: Any) -> bool:
    if value is None:
        return False
    if isinstance(value, list | tuple | set | dict):
        return bool(value)
    return True


def _constraint_chips(constraints: Constraints) -> list[str]:
    chips: list[str] = []
    if constraints.skin_type:
        chips.append(f"{constraints.skin_type}肌肤")
    if constraints.budget_max is not None:
        chips.append(f"{constraints.budget_max:g}元内")
    if constraints.use_scenario:
        chips.append(constraints.use_scenario)
    for item in constraints.ingredient_avoid:
        chips.append(f"不要{item}")
    for item in constraints.brand_avoid:
        chips.append(f"不要{item}")
    for item in constraints.origin_avoid:
        chips.append(f"不要{item}")
    if constraints.product_type:
        chips.append(constraints.product_type)
    return chips


def criteria_quick_actions(category: str | None = None) -> list[QuickActionPayload]:
    """Post-hoc filter adjustment actions for criteria_card, per category.

    Returns category-specific quick-adjust actions plus a shared "换一组".
    When category is None, defaults to the beauty-skincare set for backward
    compatibility.
    """
    budget_action = QuickActionPayload(
        action_id="budget_low",
        label="预算压低",
        action="criteria_patch",
        criteria_patch={"constraints": {"budget_max": CHEAPER_BUDGET_DEFAULT_MAX}},
    )
    replace_action = QuickActionPayload(
        action_id="replace_deck",
        label="换一组",
        action="criteria_patch",
        criteria_patch={"replace_deck": True, "constraints": {}},
    )

    if not category:
        # Fallback: generic + beauty defaults
        return [
            budget_action,
            QuickActionPayload(
                action_id="sensitive_skin",
                label="敏感肌适用",
                action="criteria_patch",
                criteria_patch={"constraints": {"skin_type": "敏感"}},
            ),
            QuickActionPayload(
                action_id="no_alcohol",
                label="不要含酒精",
                action="criteria_patch",
                criteria_patch={"constraints": {"ingredient_avoid": ["酒精"]}},
            ),
            replace_action,
        ]

    _ACTIONS: dict[str, list[QuickActionPayload]] = {
        "美妆护肤": [
            budget_action,
            QuickActionPayload(
                action_id="sensitive_skin",
                label="敏感肌适用",
                action="criteria_patch",
                criteria_patch={"constraints": {"skin_type": "敏感"}},
            ),
            QuickActionPayload(
                action_id="no_alcohol",
                label="不要含酒精",
                action="criteria_patch",
                criteria_patch={"constraints": {"ingredient_avoid": ["酒精"]}},
            ),
        ],
        "数码电子": [
            budget_action,
            QuickActionPayload(
                action_id="storage_256",
                label="256G以上",
                action="criteria_patch",
                criteria_patch={"constraints": {"storage": "256GB"}},
            ),
            QuickActionPayload(
                action_id="large_screen",
                label="大屏幕",
                action="criteria_patch",
                criteria_patch={"constraints": {"screen_size": "6.5英寸以上"}},
            ),
        ],
        "服饰运动": [
            budget_action,
            QuickActionPayload(
                action_id="sport_running",
                label="跑步",
                action="criteria_patch",
                criteria_patch={"constraints": {"sport_type": "跑步"}},
            ),
            QuickActionPayload(
                action_id="season_spring",
                label="春夏款",
                action="criteria_patch",
                criteria_patch={"constraints": {"season": "春夏"}},
            ),
        ],
        "食品生活": [
            budget_action,
            QuickActionPayload(
                action_id="dietary_sugar_free",
                label="无糖",
                action="criteria_patch",
                criteria_patch={"constraints": {"dietary": ["无糖"]}},
            ),
            QuickActionPayload(
                action_id="dietary_low_fat",
                label="低脂",
                action="criteria_patch",
                criteria_patch={"constraints": {"dietary": ["低脂"]}},
            ),
        ],
    }
    return _ACTIONS.get(category, _ACTIONS["美妆护肤"]) + [replace_action]
