"""Criteria generation, criteria_patch merging, and feedback injection."""

from __future__ import annotations

from typing import Any

from src.config.tuning import CHEAPER_BUDGET_FALLBACK_MAX
from src.services.conversation_state import get_conversation_summary, get_previous_criteria
from src.services.feedback import get_feedback_context
from src.services.llm_client import generate_criteria
from src.types.schemas import ChatStreamRequest, IntentResult
from src.types.sse_events import Constraints, CriteriaPayload, QuickActionPayload


async def run_criteria(session_id: str, body: ChatStreamRequest, intent: IntentResult) -> CriteriaPayload:
    existing = get_previous_criteria(session_id)
    if body.criteria_patch:
        return apply_criteria_patch(existing or CriteriaPayload(criteria_id="c_auto_001"), body.criteria_patch)
    feedback = get_feedback_context(session_id)
    ctx_summary = get_conversation_summary(session_id)
    return await generate_criteria(body.message, intent, feedback=feedback, existing=existing,
                                    conversation_context=ctx_summary)


def apply_criteria_patch(criteria: CriteriaPayload, patch: dict[str, Any]) -> CriteriaPayload:
    raw_constraints = patch.get("constraints", patch)
    allowed = set(Constraints.model_fields)
    updates = {key: value for key, value in raw_constraints.items() if key in allowed}
    constraints = criteria.constraints.model_copy(update=updates)
    chips = [criteria.category] if criteria.category else []
    if constraints.skin_type:
        chips.append(f"{constraints.skin_type}肌肤")
    if constraints.budget_max is not None:
        chips.append(f"{constraints.budget_max:g}元内")
    if constraints.use_scenario:
        chips.append(constraints.use_scenario)
    for item in constraints.ingredient_avoid:
        chips.append(f"不要{item}")
    return criteria.model_copy(update={"constraints": constraints, "chips": chips, "summary": "，".join(chips)})


def criteria_quick_actions() -> list[QuickActionPayload]:
    return [
        QuickActionPayload(
            action_id="budget_low",
            label="预算压低",
            action="criteria_patch",
            criteria_patch={"constraints": {"budget_max": CHEAPER_BUDGET_FALLBACK_MAX}},
        ),
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
    ]
