"""Run the four demo paths and write a reproducible JSON report.

Run from backend/:
    .venv/bin/python -m src.scripts.demo_smoke
"""

from __future__ import annotations

import sys

if sys.platform == "win32":
    import asyncio as _asyncio

    _asyncio.set_event_loop_policy(_asyncio.WindowsSelectorEventLoopPolicy())

import argparse
import asyncio
import json
import os
import shutil
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from src.config.settings import BACKEND_DIR, get_settings
from src.repos.cart_items import get_cart
from src.runtime.pipeline import chat_stream
from src.types.schemas import ChatStreamRequest
from src.types.sse_events import SSEEventBase


def _check_live_provider() -> None:
    """Fail fast if demo smoke would use mock/fake providers."""
    if "pytest" in sys.modules:
        return
    bailian_url = os.getenv("BAILIAN_BASE_URL")
    bailian_key = os.getenv("BAILIAN_API_KEY")
    if not bailian_url or not bailian_key:
        raise SystemExit(
            "DEMO SMOKE GATE FAILED: BAILIAN_BASE_URL/BAILIAN_API_KEY not configured. "
            "Demo smoke requires a real AI provider."
        )
    if bailian_key == "test-key":
        raise SystemExit(
            "DEMO SMOKE GATE FAILED: BAILIAN_API_KEY is set to mock value 'test-key'. "
            "Use real credentials for demo smoke."
        )


def _check_postgres() -> None:
    """Fail fast if demo smoke would not exercise pgvector."""
    if "pytest" in sys.modules:
        return
    url = os.getenv("DATABASE_URL", "")
    if "postgresql" not in url:
        raise SystemExit(f"DEMO SMOKE GATE FAILED: DATABASE_URL must use PostgreSQL + pgvector. Got: {url[:80]}...")


REPORTS_DIR = BACKEND_DIR / "reports"
DEFAULT_IMAGE_PATH = "1_美妆护肤/images/p_beauty_012_live.jpg"
TURN_TIMEOUT_SECONDS = 150


async def main_async(write_report: bool = True) -> dict[str, Any]:
    _check_live_provider()
    _check_postgres()
    started = datetime.now(timezone.utc)
    started_perf = time.perf_counter()
    session_id = f"demo_smoke_{started.strftime('%Y%m%d_%H%M%S')}"
    image_url = _demo_image_url()

    scenarios: list[dict[str, Any]] = []
    # Product-first: recommendation rounds emit product_card + criteria_card.
    # Multi-product decks wait for feedback; a single strong candidate may
    # converge immediately with final_decision + done(completed).
    text_budget_beauty = await _run_turn(
        name="text_budget_beauty",
        session_id=session_id,
        request=ChatStreamRequest(message="推荐适合油皮的洗面奶，200元以内，日常护肤"),
        expect={
            "product_card_min": 1,
            "criteria_card": True,
            "recommendation_done_reason": True,
            "first_evidence_source_id": True,
        },
    )
    scenarios.append(text_budget_beauty)
    scenarios.append(
        await _run_turn(
            name="continue_beauty_decision",
            session_id=session_id,
            request=ChatStreamRequest(message="继续"),
            expect={
                "final_decision": True,
            },
        )
    )
    scenarios.append(
        await _run_turn(
            name="image_sensitive_skin",
            session_id=session_id,
            request=ChatStreamRequest(message="这个适合敏感肌吗？", image_url=image_url),
            expect={
                "thinking_stage": "analyzing_image",
                "product_card_min": 1,
                "criteria_card": True,
                "recommendation_done_reason": True,
                "image_url": bool(image_url),
            },
        )
    )
    scenarios.append(
        await _run_turn(
            name="proactive_phone_clarification",
            session_id=f"{session_id}_clarify",
            request=ChatStreamRequest(message="推荐一款手机"),
            expect={
                "clarification": True,
                "required_slot": "budget",
            },
        )
    )
    travel_session_id = f"{session_id}_travel"
    travel_combo_strategy = await _run_turn(
        name="travel_combo_strategy",
        session_id=travel_session_id,
        request=ChatStreamRequest(message="下周去三亚度假，帮我搭配一套从防晒到穿搭的方案"),
        expect={
            "criteria_card": True,
            "shopping_strategy_scene_type": "travel",
            "product_card_min": 2,
            "product_category_min": 2,
            "recommendation_done_reason": True,
        },
    )
    scenarios.append(travel_combo_strategy)
    compare_ids = travel_combo_strategy.get("product_ids", [])[:2]
    scenarios.append(
        await _run_turn(
            name="compare_first_two",
            session_id=travel_session_id,
            request=ChatStreamRequest(message="对比第一个和第二个", compare_product_ids=compare_ids),
            expect={"compare_card": True},
        )
        if len(compare_ids) >= 2
        else _precondition_failed_result(
            name="compare_first_two",
            message="对比第一个和第二个",
            expect={"compare_card": True, "precondition_product_ids_min": 2},
            failures=["need at least two products from travel_combo_strategy"],
        )
    )
    scenarios.append(
        await _run_turn(
            name="multi_turn_avoid_alcohol",
            session_id=session_id,
            request=ChatStreamRequest(message="不要含酒精的防晒霜"),
            expect={
                "product_card_min": 1,
                "criteria_card": True,
                "recommendation_done_reason": True,
            },
        )
    )
    scenarios.append(
        await _run_turn(
            name="multi_turn_budget_patch",
            session_id=session_id,
            request=ChatStreamRequest(message="预算降到200"),
            expect={
                "product_card_min": 1,
                "criteria_card": True,
                "recommendation_done_reason": True,
            },
        )
    )
    scenarios.append(
        await _run_turn(
            name="continue_avoid_decision",
            session_id=session_id,
            request=ChatStreamRequest(message="继续"),
            expect={
                "final_decision": True,
            },
        )
    )
    scenarios.append(
        await _run_turn(
            name="cart_add",
            session_id=session_id,
            request=ChatStreamRequest(message="把这个加到购物车"),
            expect={"cart_action": "add"},
        )
    )
    scenarios.append(
        await _run_turn(
            name="checkout_preview",
            session_id=session_id,
            request=ChatStreamRequest(message="就买这个"),
            expect={"cart_action": "checkout_preview"},
        )
    )
    scenarios.append(
        await _run_turn(
            name="checkout_confirm",
            session_id=session_id,
            request=ChatStreamRequest(message="确认"),
            expect={"cart_action": "checkout_confirm"},
        )
    )
    scenarios.append(
        await _run_turn(
            name="cart_view",
            session_id=session_id,
            request=ChatStreamRequest(message="查看购物车"),
            expect={"cart_action": "view"},
        )
    )

    cart = await get_cart(session_id)
    report = {
        "check": "demo_smoke",
        "ok": all(item["ok"] for item in scenarios) and cart.total_items >= 1,
        "created_at": started.isoformat(),
        "duration_ms": round((time.perf_counter() - started_perf) * 1000, 2),
        "database_url": get_settings().database_url,
        "session_id": session_id,
        "image_url": image_url,
        "scenarios": scenarios,
        "cart": {
            "total_items": cart.total_items,
            "total_price": cart.total_price,
            "product_ids": [item.product_id for item in cart.items],
        },
    }
    report["report_path"] = str(_write_report(report)) if write_report else None
    return report


async def _run_turn(
    name: str,
    session_id: str,
    request: ChatStreamRequest,
    expect: dict[str, Any],
) -> dict[str, Any]:
    started = time.perf_counter()
    _print_progress(
        {
            "check": "demo_smoke",
            "stage": "turn_start",
            "name": name,
            "message": request.message,
            "timeout_seconds": TURN_TIMEOUT_SECONDS,
        }
    )
    try:
        events = await asyncio.wait_for(_collect_turn_events(session_id, request), timeout=TURN_TIMEOUT_SECONDS)
    except TimeoutError:
        duration_ms = round((time.perf_counter() - started) * 1000, 2)
        result = {
            "name": name,
            "ok": False,
            "duration_ms": duration_ms,
            "message": request.message,
            "expect": expect,
            "failures": [f"turn timeout after {TURN_TIMEOUT_SECONDS}s"],
            "events": [],
            "thinking_stages": [],
            "event_count": 0,
            "product_count": 0,
            "product_ids": [],
            "product_categories": [],
            "has_criteria": False,
            "criteria_summary": None,
            "shopping_strategy_scene_type": None,
            "shopping_strategy_title": None,
            "has_decision": False,
            "winner_product_id": None,
            "has_clarification": False,
            "clarification_required_slots": [],
            "clarification_suggested_options": [],
            "has_compare": False,
            "compare_count": 0,
            "compare_winner_product_id": None,
            "cart_actions": [],
            "first_evidence_source_id": None,
            "first_evidence_chars": 0,
            "errors": [],
            "done_reason": None,
        }
        _print_progress(_turn_progress_payload("turn_timeout", result))
        return result
    summary = _summarize_events(events)
    ok, failures = _evaluate(summary, expect)
    result = {
        "name": name,
        "ok": ok,
        "duration_ms": round((time.perf_counter() - started) * 1000, 2),
        "message": request.message,
        "expect": expect,
        "failures": failures,
        **summary,
    }
    _print_progress(_turn_progress_payload("turn_end", result))
    return result


async def _collect_turn_events(session_id: str, request: ChatStreamRequest) -> list[SSEEventBase]:
    return [event async for event in chat_stream(session_id, request)]


def _summarize_events(events: list[SSEEventBase]) -> dict[str, Any]:
    tags = [event.event for event in events]
    thinking_events = [event for event in events if event.event == "thinking"]
    product_events = [event for event in events if event.event == "product_card"]
    criteria_events = [event for event in events if event.event == "criteria_card"]
    decision_events = [event for event in events if event.event == "final_decision"]
    cart_events = [event for event in events if event.event == "cart_action"]
    clarification_events = [event for event in events if event.event == "clarification"]
    compare_events = [event for event in events if event.event == "compare_card"]
    error_events = [event for event in events if event.event == "error"]
    done_events = [event for event in events if event.event == "done"]
    first_product = product_events[0] if product_events else None
    first_evidence = first_product.evidence[0] if first_product and first_product.evidence else None
    last_done_reason = done_events[-1].finish_reason if done_events else None
    last_criteria = criteria_events[-1] if criteria_events else None
    shopping_strategy = last_criteria.shopping_strategy if last_criteria else None
    product_categories = sorted({event.product.category for event in product_events if event.product.category})
    required_slots = []
    suggested_options = []
    for event in clarification_events:
        required_slots.extend(event.required_slots)
        suggested_options.extend(event.suggested_options)
    return {
        "events": tags,
        "thinking_stages": [event.stage for event in thinking_events],
        "event_count": len(events),
        "product_count": len(product_events),
        "product_ids": [event.product.product_id for event in product_events],
        "product_categories": product_categories,
        "has_criteria": bool(criteria_events),
        "criteria_summary": criteria_events[-1].criteria.summary if criteria_events else None,
        "shopping_strategy_scene_type": shopping_strategy.scene_type if shopping_strategy else None,
        "shopping_strategy_title": (
            shopping_strategy.primary_direction.title if shopping_strategy else None
        ),
        "has_decision": bool(decision_events),
        "winner_product_id": decision_events[-1].winner_product_id if decision_events else None,
        "has_clarification": bool(clarification_events),
        "clarification_required_slots": sorted(set(required_slots)),
        "clarification_suggested_options": sorted(set(suggested_options)),
        "has_compare": bool(compare_events),
        "compare_count": len(compare_events),
        "compare_winner_product_id": compare_events[-1].winner_product_id if compare_events else None,
        "cart_actions": [
            {
                "action": event.action,
                "product_id": event.product_id,
                "quantity": event.quantity,
                "status": event.status,
                "cart": event.cart.model_dump(mode="json") if event.cart else None,
            }
            for event in cart_events
        ],
        "first_evidence_source_id": first_evidence.source_id if first_evidence else None,
        "first_evidence_chars": len(first_evidence.snippet) if first_evidence else 0,
        "errors": [{"code": event.code, "message": event.message} for event in error_events],
        "done_reason": last_done_reason,
    }


def _evaluate(summary: dict[str, Any], expect: dict[str, Any]) -> tuple[bool, list[str]]:
    failures: list[str] = []
    if summary.get("errors"):
        failures.append("has error event")
    if expect.get("criteria_card") and not summary.get("has_criteria"):
        failures.append("missing criteria_card")
    if expect.get("final_decision") and not summary.get("has_decision"):
        failures.append("missing final_decision")
    product_card_min = expect.get("product_card_min")
    if isinstance(product_card_min, int) and summary.get("product_count", 0) < product_card_min:
        failures.append(f"product_count<{product_card_min}")
    if expect.get("first_evidence_source_id") and not summary.get("first_evidence_source_id"):
        failures.append("missing first evidence source_id")
    expected_cart_action = expect.get("cart_action")
    if expected_cart_action and not any(action["action"] == expected_cart_action for action in summary.get("cart_actions", [])):
        failures.append(f"missing cart_action:{expected_cart_action}")
    if expect.get("clarification") and not summary.get("has_clarification"):
        failures.append("missing clarification")
    required_slot = expect.get("required_slot")
    if required_slot and required_slot not in summary.get("clarification_required_slots", []):
        failures.append(f"missing required_slot:{required_slot}")
    expected_stage = expect.get("thinking_stage")
    if expected_stage and expected_stage not in summary.get("thinking_stages", []):
        failures.append(f"missing thinking_stage:{expected_stage}")
    expected_scene = expect.get("shopping_strategy_scene_type")
    if expected_scene and summary.get("shopping_strategy_scene_type") != expected_scene:
        failures.append(
            f"shopping_strategy_scene_type mismatch: expected {expected_scene}, "
            f"got {summary.get('shopping_strategy_scene_type')}"
        )
    product_category_min = expect.get("product_category_min")
    if isinstance(product_category_min, int) and len(summary.get("product_categories", [])) < product_category_min:
        failures.append(f"product_category_count<{product_category_min}")
    if expect.get("compare_card") and not summary.get("has_compare"):
        failures.append("missing compare_card")
    precondition_product_ids_min = expect.get("precondition_product_ids_min")
    if isinstance(precondition_product_ids_min, int) and len(summary.get("product_ids", [])) < precondition_product_ids_min:
        failures.append(f"precondition_product_ids<{precondition_product_ids_min}")
    if expect.get("image_url") is False:
        failures.append("missing demo image")
    expected_done = expect.get("done_reason")
    if expected_done and summary.get("done_reason") != expected_done:
        failures.append(f"done_reason mismatch: expected {expected_done}, got {summary.get('done_reason')}")
    if expect.get("recommendation_done_reason") and not _recommendation_done_reason_ok(summary):
        failures.append(
            "recommendation done_reason mismatch: expected completed for single-product decision "
            "or awaiting_product_feedback for multi-product deck, "
            f"got product_count={summary.get('product_count')} "
            f"has_decision={summary.get('has_decision')} done_reason={summary.get('done_reason')}"
        )
    return not failures, failures


def _recommendation_done_reason_ok(summary: dict[str, Any]) -> bool:
    product_count = int(summary.get("product_count") or 0)
    done_reason = summary.get("done_reason")
    has_decision = bool(summary.get("has_decision"))
    if product_count == 1 and has_decision:
        return done_reason == "completed"
    if product_count >= 2 and not has_decision:
        return done_reason == "awaiting_product_feedback"
    return False


def _precondition_failed_result(
    *,
    name: str,
    message: str,
    expect: dict[str, Any],
    failures: list[str],
) -> dict[str, Any]:
    return {
        "name": name,
        "ok": False,
        "duration_ms": 0.0,
        "message": message,
        "expect": expect,
        "failures": failures,
        "events": [],
        "thinking_stages": [],
        "event_count": 0,
        "product_count": 0,
        "product_ids": [],
        "product_categories": [],
        "has_criteria": False,
        "criteria_summary": None,
        "shopping_strategy_scene_type": None,
        "shopping_strategy_title": None,
        "has_decision": False,
        "winner_product_id": None,
        "has_clarification": False,
        "clarification_required_slots": [],
        "clarification_suggested_options": [],
        "has_compare": False,
        "compare_count": 0,
        "compare_winner_product_id": None,
        "cart_actions": [],
        "first_evidence_source_id": None,
        "first_evidence_chars": 0,
        "errors": [],
        "done_reason": None,
    }


def _turn_progress_payload(stage: str, result: dict[str, Any]) -> dict[str, Any]:
    return {
        "check": "demo_smoke",
        "stage": stage,
        "name": result["name"],
        "ok": result["ok"],
        "duration_ms": result["duration_ms"],
        "event_count": result["event_count"],
        "product_count": result["product_count"],
        "has_criteria": result["has_criteria"],
        "has_decision": result["has_decision"],
        "has_clarification": result.get("has_clarification", False),
        "has_compare": result.get("has_compare", False),
        "shopping_strategy_scene_type": result.get("shopping_strategy_scene_type"),
        "done_reason": result["done_reason"],
        "failures": result["failures"],
    }


def _print_progress(payload: dict[str, Any]) -> None:
    print(json.dumps(payload, ensure_ascii=False), flush=True)


def _demo_image_url() -> str | None:
    image_path = get_settings().dataset_dir / DEFAULT_IMAGE_PATH
    if not image_path.exists():
        return None
    upload_dir = get_settings().upload_dir
    upload_dir.mkdir(parents=True, exist_ok=True)
    target = upload_dir / "demo_p_beauty_012_live.jpg"
    shutil.copyfile(image_path, target)
    return f"/uploads/{target.name}"


def _write_report(report: dict[str, Any]) -> Path:
    REPORTS_DIR.mkdir(parents=True, exist_ok=True)
    path = REPORTS_DIR / f"demo-smoke-{datetime.now(timezone.utc).strftime('%Y%m%d-%H%M%S')}.json"
    path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    return path


def main() -> None:
    parser = argparse.ArgumentParser(description="Run BuyPilot demo smoke paths.")
    parser.add_argument("--no-write", action="store_true", help="Do not write backend/reports JSON report.")
    args = parser.parse_args()
    report = asyncio.run(main_async(write_report=not args.no_write))
    print(json.dumps(report, ensure_ascii=False))
    if not report["ok"]:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
