"""Tests for criteria context diagnostic rules (diagnose_criteria_context)."""

import pytest

from src.runtime.stages.criteria import diagnose_criteria_context
from src.services.audit import list_audit_event_payloads, record_audit_event
from src.services.observability import get_turn_debug_bundle
from src.services.request_context import RequestContext, set_request_context
from src.types.schemas import IntentResult
from src.types.sse_events import Constraints, CriteriaPayload


@pytest.fixture
async def diagnostics_database():
    """Seed test database for context diagnostics tests."""
    from src.services.product_ingest import seed_products_if_needed

    await seed_products_if_needed()
    yield


@pytest.fixture
def request_ctx():
    """Set a RequestContext so record_audit_event picks up turn_id/session_id."""
    ctx = RequestContext(
        request_id="req-diag-test",
        trace_id="trace-diag-test",
        session_id="sess-diag-test",
        turn_id="turn-diag-test",
    )
    set_request_context(ctx)
    yield ctx
    set_request_context(None)


# ---------------------------------------------------------------------------
# BUDGET_PATCH_LOST
# ---------------------------------------------------------------------------


@pytest.mark.asyncio
async def test_budget_patch_lost_detected(diagnostics_database, request_ctx):
    """User lowered budget_max from 300 to 200 but merge kept 300 — must fire diagnostic."""
    del diagnostics_database

    existing = CriteriaPayload(
        constraints=Constraints(budget_max=300),
    )
    final = CriteriaPayload(
        constraints=Constraints(budget_max=300),
    )
    intent = IntentResult(
        intent="recommend",
        extracted_constraints={"budget_max": 200},
    )

    await diagnose_criteria_context(existing, final, intent)

    events = await list_audit_event_payloads(
        turn_id=request_ctx.turn_id,
        action="chat.context_diagnostic",
    )
    assert len(events) == 1
    meta = events[0]["metadata"]
    assert meta["diagnostic_code"] == "BUDGET_PATCH_LOST"
    assert meta["severity"] == "warning"
    assert meta["user_budget_max"] == 200
    assert meta["before_budget_max"] == 300
    assert meta["after_budget_max"] == 300


@pytest.mark.asyncio
async def test_budget_patch_applied_no_diagnostic(diagnostics_database, request_ctx):
    """Budget correctly applied from 300 to 200 — no diagnostic should fire."""
    del diagnostics_database

    existing = CriteriaPayload(
        constraints=Constraints(budget_max=300),
    )
    final = CriteriaPayload(
        constraints=Constraints(budget_max=200),
    )
    intent = IntentResult(
        intent="recommend",
        extracted_constraints={"budget_max": 200},
    )

    await diagnose_criteria_context(existing, final, intent)

    events = await list_audit_event_payloads(
        turn_id=request_ctx.turn_id,
        action="chat.context_diagnostic",
    )
    assert len(events) == 0


# ---------------------------------------------------------------------------
# EXCLUSION_LOST
# ---------------------------------------------------------------------------


@pytest.mark.asyncio
async def test_exclusion_lost_detected(diagnostics_database, request_ctx):
    """User had ingredient_avoid=['酒精'] but merge dropped it — must fire diagnostic."""
    del diagnostics_database

    existing = CriteriaPayload(
        constraints=Constraints(ingredient_avoid=["酒精"]),
    )
    final = CriteriaPayload(
        constraints=Constraints(ingredient_avoid=[]),
    )
    intent = IntentResult(
        intent="recommend",
        extracted_constraints={"ingredient_avoid": ["酒精"]},
    )

    await diagnose_criteria_context(existing, final, intent)

    events = await list_audit_event_payloads(
        turn_id=request_ctx.turn_id,
        action="chat.context_diagnostic",
    )
    assert len(events) == 1
    meta = events[0]["metadata"]
    assert meta["diagnostic_code"] == "EXCLUSION_LOST"
    assert meta["severity"] == "warning"
    assert meta["field"] == "ingredient_avoid"
    assert meta["lost_item"] == "酒精"
    assert meta["before_value"] == ["酒精"]
    assert meta["after_value"] == []


@pytest.mark.asyncio
async def test_exclusion_preserved_no_diagnostic(diagnostics_database, request_ctx):
    """Exclusion preserved through merge — no diagnostic should fire."""
    del diagnostics_database

    existing = CriteriaPayload(
        constraints=Constraints(ingredient_avoid=["酒精"]),
    )
    final = CriteriaPayload(
        constraints=Constraints(ingredient_avoid=["酒精"]),
    )
    intent = IntentResult(
        intent="recommend",
        extracted_constraints={"ingredient_avoid": ["酒精"]},
    )

    await diagnose_criteria_context(existing, final, intent)

    events = await list_audit_event_payloads(
        turn_id=request_ctx.turn_id,
        action="chat.context_diagnostic",
    )
    assert len(events) == 0


# ---------------------------------------------------------------------------
# First turn (no existing criteria)
# ---------------------------------------------------------------------------


@pytest.mark.asyncio
async def test_no_existing_criteria_no_diagnostic(diagnostics_database, request_ctx):
    """First turn has no existing criteria — nothing can be lost, no diagnostic."""
    del diagnostics_database

    final = CriteriaPayload(
        constraints=Constraints(budget_max=200),
    )
    intent = IntentResult(
        intent="recommend",
        extracted_constraints={"budget_max": 200},
    )

    await diagnose_criteria_context(None, final, intent)

    events = await list_audit_event_payloads(
        turn_id=request_ctx.turn_id,
        action="chat.context_diagnostic",
    )
    assert len(events) == 0


# ---------------------------------------------------------------------------
# Debug bundle integration
# ---------------------------------------------------------------------------


@pytest.mark.asyncio
async def test_debug_bundle_includes_context_diagnostics(diagnostics_database, request_ctx):
    """get_turn_debug_bundle must include context_diagnostics key with matching events."""
    del diagnostics_database

    await record_audit_event(
        action="chat.context_diagnostic",
        metadata={
            "diagnostic_code": "BUDGET_PATCH_LOST",
            "severity": "warning",
            "user_budget_max": 200,
            "before_budget_max": 300,
            "after_budget_max": 300,
        },
    )

    bundle = await get_turn_debug_bundle(request_ctx.turn_id)

    assert "context_diagnostics" in bundle
    assert isinstance(bundle["context_diagnostics"], list)
    assert len(bundle["context_diagnostics"]) == 1

    diag = bundle["context_diagnostics"][0]
    assert diag["action"] == "chat.context_diagnostic"
    assert diag["metadata"]["diagnostic_code"] == "BUDGET_PATCH_LOST"
