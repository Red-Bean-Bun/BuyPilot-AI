"""Criteria generation, criteria_patch merging, and feedback injection."""

from __future__ import annotations

import asyncio
import uuid
from typing import Any

from src.config import user_messages as msg
from src.config.domain_terms import normalize_product_type
from src.config.tuning import CHEAPER_BUDGET_DEFAULT_MAX, DEFAULT_CRITERIA_ID
from src.services.audit import record_audit_event
from src.services.criteria_sanitizer import sanitize_criteria_product_type, sanitize_product_type_constraint
from src.services.conversation_state import get_conversation_summary, get_previous_criteria
from src.services.feedback import get_feedback_context
from src.services.llm_client import generate_criteria
from src.types.schemas import ChatStreamRequest, IntentResult
from src.types.sse_events import Constraints, CriteriaPayload, QuickActionPayload, ShoppingStrategyPayload


async def run_criteria(session_id: str, body: ChatStreamRequest, intent: IntentResult) -> CriteriaPayload:
    # Fire all 3 DB reads concurrently — none depend on each other
    existing_task = asyncio.create_task(get_previous_criteria(session_id))
    feedback_task = asyncio.create_task(get_feedback_context(session_id))
    summary_task = asyncio.create_task(get_conversation_summary(session_id))

    existing = await existing_task
    if body.criteria_patch:
        feedback_task.cancel()
        summary_task.cancel()
        final = apply_criteria_patch(existing or CriteriaPayload(criteria_id=DEFAULT_CRITERIA_ID), body.criteria_patch)
        await diagnose_criteria_context(existing, final, intent)
        return final

    feedback = await feedback_task
    ctx_summary = await summary_task
    criteria = await generate_criteria(
        body.message,
        intent,
        feedback=feedback,
        existing=existing,
        conversation_context=ctx_summary,
        history=[item.model_dump() for item in body.history],
    )
    final = annotate_criteria_sources(criteria, intent, existing)
    # When the user explicitly specifies a product_type (via lookup hints or
    # deterministic extraction) that differs from what the LLM produced, use
    # the intent's value. The LLM prompt Rule 7 ("在它的基础上修改") causes
    # it to retain the previous turn's product_type even when the user clearly
    # switched topics (e.g. "有苹果手机吗" → "有电脑吗").
    intent_pt = (intent.extracted_constraints or {}).get("product_type")
    if intent_pt and normalize_product_type(intent_pt) != normalize_product_type(final.constraints.product_type):
        updated = final.constraints.model_copy(update={"product_type": intent_pt})
        chips = [c for c in final.chips if normalize_product_type(c) != normalize_product_type(final.constraints.product_type)]
        if intent_pt not in chips:
            chips.append(intent_pt)
        final = final.model_copy(update={"constraints": updated, "chips": chips})
    await diagnose_criteria_context(existing, final, intent)
    return final


def criteria_from_intent(
    intent: IntentResult,
    *,
    summary: str = "",
) -> CriteriaPayload:
    """Build a CriteriaPayload from intent results without an LLM call.

    Used for:
    1. Clarification continuity   — summary defaults to chip concatenation
    2. Speculative retrieval       — summary uses natural-language template
    """
    _LIST_FIELDS = {"brand_avoid", "brand_prefer", "origin_avoid", "ingredient_avoid", "ingredient_prefer", "dietary"}
    allowed = set(Constraints.model_fields)
    category = intent.category or ""
    constraint_kwargs: dict[str, Any] = {}
    for key, value in (intent.extracted_constraints or {}).items():
        if key not in allowed or value is None:
            continue
        if key in _LIST_FIELDS and isinstance(value, str):
            constraint_kwargs[key] = [value]
        else:
            constraint_kwargs[key] = value
    sanitize_product_type_constraint(constraint_kwargs, category)
    constraints = Constraints(**constraint_kwargs)
    chips = [category] if category else []
    chips.extend(_constraint_chips(constraints))

    return CriteriaPayload(
        criteria_id=f"pending_{uuid.uuid4().hex[:8]}",
        category=category,
        summary=summary or "，".join(chips) if chips else "",
        chips=chips,
        constraints=constraints,
        field_sources={
            **({"category": "user"} if category else {}),
            **{f"constraints.{key}": "user" for key in constraint_kwargs},
        },
    )


_MERGE_LIST_FIELDS = {"ingredient_avoid", "ingredient_prefer", "brand_avoid", "brand_prefer", "origin_avoid", "dietary"}


def apply_criteria_patch(criteria: CriteriaPayload, patch: dict[str, Any]) -> CriteriaPayload:
    raw_constraints = patch.get("constraints", patch)
    allowed = set(Constraints.model_fields)
    updates = {key: value for key, value in raw_constraints.items() if key in allowed}
    if "product_type" in updates:
        sanitize_product_type_constraint(updates, criteria.category, clear_invalid=True)
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
    criteria = sanitize_criteria_product_type(criteria, chips_for_constraints=_criteria_chips)
    explicit_fields = _explicit_source_fields(intent)
    history_fields = _history_source_fields(criteria, existing)
    # Category switch: only keep constraints the user explicitly stated this
    # turn. All other fields are semantically irrelevant to the new category.
    category_switched = existing is not None and criteria.category and criteria.category != existing.category
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
        if category_switched:
            # Non-explicit field on category switch: discard it
            constraints_update[key] = [] if isinstance(value, list) else None
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


_HARD_CONSTRAINT_FIELDS = {
    "budget_min",
    "budget_max",
    "brand_avoid",
    "origin_avoid",
    "ingredient_avoid",
}


def _explicit_source_fields(intent: IntentResult) -> set[str]:
    fields: set[str] = set()
    if intent.category:
        fields.add("category")
    for key, value in (intent.extracted_constraints or {}).items():
        if key in Constraints.model_fields and _has_value(value):
            if key == "product_type" and not _valid_product_type_source(value, intent.category):
                continue
            fields.add(f"constraints.{key}")
    return fields


def _valid_product_type_source(value: Any, category: str | None) -> bool:
    probe: dict[str, Any] = {"product_type": value}
    sanitize_product_type_constraint(probe, category)
    return bool(probe.get("product_type"))


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
    if constraints.budget_min is not None:
        chips.append(f"{constraints.budget_min:g}元以上")
    if constraints.use_scenario:
        chips.append(constraints.use_scenario)
    for item in constraints.ingredient_avoid:
        chips.append(f"不要{item}")
    for item in constraints.brand_prefer:
        chips.append(item)
    for item in constraints.brand_avoid:
        chips.append(f"不要{item}")
    for item in constraints.origin_avoid:
        chips.append(f"不要{item}")
    if constraints.product_type:
        chips.append(constraints.product_type)
    return chips


def _criteria_chips(category: str, constraints: Constraints) -> list[str]:
    chips = [category] if category else []
    chips.extend(_constraint_chips(constraints))
    return chips


def criteria_quick_actions(
    category: str | None = None,
    shopping_strategy: ShoppingStrategyPayload | None = None,
) -> list[QuickActionPayload]:
    """Post-hoc filter adjustment actions for criteria_card, per category.

    Returns category-specific quick-adjust actions plus a shared "换一组".
    When category is None, defaults to the beauty-skincare set for backward
    compatibility.
    When shopping_strategy is a travel/combo scene, returns only generic
    actions (budget + replace) since category-specific filters don't apply
    to cross-category results.
    """
    budget_action = QuickActionPayload(
        action_id="budget_low",
        label=msg.QA_BUDGET_LOW,
        action="criteria_patch",
        criteria_patch={"constraints": {"budget_max": CHEAPER_BUDGET_DEFAULT_MAX}},
    )
    replace_action = QuickActionPayload(
        action_id="replace_deck",
        label=msg.QA_REPLACE_DECK,
        action="criteria_patch",
        criteria_patch={"replace_deck": True, "constraints": {}},
    )

    # Travel/combo scene: only generic actions (budget + replace),
    # category-specific filters like "敏感肌" don't apply to cross-category results.
    if shopping_strategy is not None:
        return [budget_action, replace_action]

    if not category:
        # Fallback: generic + beauty defaults
        return [
            budget_action,
            QuickActionPayload(
                action_id="sensitive_skin",
                label=msg.QA_SENSITIVE_SKIN,
                action="criteria_patch",
                criteria_patch={"constraints": {"skin_type": "敏感"}},
            ),
            QuickActionPayload(
                action_id="no_alcohol",
                label=msg.QA_NO_ALCOHOL,
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
                label=msg.QA_SENSITIVE_SKIN,
                action="criteria_patch",
                criteria_patch={"constraints": {"skin_type": "敏感"}},
            ),
            QuickActionPayload(
                action_id="no_alcohol",
                label=msg.QA_NO_ALCOHOL,
                action="criteria_patch",
                criteria_patch={"constraints": {"ingredient_avoid": ["酒精"]}},
            ),
        ],
        "数码电子": [
            budget_action,
            QuickActionPayload(
                action_id="storage_256",
                label=msg.QA_STORAGE_256,
                action="criteria_patch",
                criteria_patch={"constraints": {"storage": "256GB"}},
            ),
            QuickActionPayload(
                action_id="large_screen",
                label=msg.QA_LARGE_SCREEN,
                action="criteria_patch",
                criteria_patch={"constraints": {"screen_size": "6.5英寸以上"}},
            ),
        ],
        "服饰运动": [
            budget_action,
            QuickActionPayload(
                action_id="sport_running",
                label=msg.QA_RUNNING,
                action="criteria_patch",
                criteria_patch={"constraints": {"sport_type": "跑步"}},
            ),
            QuickActionPayload(
                action_id="season_spring",
                label=msg.QA_SPRING_SUMMER,
                action="criteria_patch",
                criteria_patch={"constraints": {"season": "春夏"}},
            ),
        ],
        "食品生活": [
            budget_action,
            QuickActionPayload(
                action_id="dietary_sugar_free",
                label=msg.QA_SUGAR_FREE,
                action="criteria_patch",
                criteria_patch={"constraints": {"dietary": ["无糖"]}},
            ),
            QuickActionPayload(
                action_id="dietary_low_fat",
                label=msg.QA_LOW_FAT,
                action="criteria_patch",
                criteria_patch={"constraints": {"dietary": ["低脂"]}},
            ),
        ],
    }
    return _ACTIONS.get(category, _ACTIONS[msg.DEFAULT_CATEGORY]) + [replace_action]


async def diagnose_criteria_context(
    existing: CriteriaPayload | None,
    final: CriteriaPayload,
    intent: IntentResult,
) -> None:
    """Check for context loss in multi-turn criteria merging.

    Records audit events for:
    - BUDGET_PATCH_LOST: User mentioned budget change but it wasn't applied
    - EXCLUSION_LOST: User mentioned exclusion but it was lost in merge
    """
    if existing is None:
        return

    extracted = intent.extracted_constraints or {}

    # Rule 1: BUDGET_PATCH_LOST
    # User mentioned budget_max but final criteria didn't change it (or made it larger)
    user_budget_max = extracted.get("budget_max")
    if user_budget_max is not None and isinstance(user_budget_max, (int, float)):
        before_budget_max = existing.constraints.budget_max
        after_budget_max = final.constraints.budget_max

        # Check if budget didn't change or got larger (user wanted to reduce)
        if before_budget_max is not None and after_budget_max is not None:
            if after_budget_max >= before_budget_max:
                await record_audit_event(
                    action="chat.context_diagnostic",
                    metadata={
                        "diagnostic_code": "BUDGET_PATCH_LOST",
                        "severity": "warning",
                        "user_budget_max": user_budget_max,
                        "before_budget_max": before_budget_max,
                        "after_budget_max": after_budget_max,
                    },
                )

    # Rule 2: EXCLUSION_LOST
    # User mentioned exclusion (brand_avoid, origin_avoid, ingredient_avoid) but it was lost
    for field in ("brand_avoid", "origin_avoid", "ingredient_avoid"):
        user_exclusions = extracted.get(field)
        if not user_exclusions or not isinstance(user_exclusions, list):
            continue

        before_value = getattr(existing.constraints, field) or []
        after_value = getattr(final.constraints, field) or []

        for item in user_exclusions:
            if item in before_value and item not in after_value:
                await record_audit_event(
                    action="chat.context_diagnostic",
                    metadata={
                        "diagnostic_code": "EXCLUSION_LOST",
                        "severity": "warning",
                        "field": field,
                        "lost_item": item,
                        "before_value": before_value,
                        "after_value": after_value,
                    },
                )
