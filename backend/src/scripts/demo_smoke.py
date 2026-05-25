"""Run the four demo paths and write a reproducible JSON report.

Run from backend/:
    .venv/bin/python -m src.scripts.demo_smoke
"""

from __future__ import annotations

import argparse
import asyncio
import json
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


REPORTS_DIR = BACKEND_DIR / "reports"
DEFAULT_IMAGE_PATH = "1_美妆护肤/images/p_beauty_012_live.jpg"


async def main_async(write_report: bool = True) -> dict[str, Any]:
    started = datetime.now(timezone.utc)
    started_perf = time.perf_counter()
    session_id = f"demo_smoke_{started.strftime('%Y%m%d_%H%M%S')}"
    image_url = _demo_image_url()

    scenarios: list[dict[str, Any]] = []
    scenarios.append(
        await _run_turn(
            name="text_budget_beauty",
            session_id=session_id,
            request=ChatStreamRequest(message="推荐适合油皮的洗面奶，200元以内，日常护肤"),
            expect={
                "product_card_min": 1,
                "criteria_card": True,
                "final_decision": True,
                "first_evidence_source_id": True,
            },
        )
    )
    scenarios.append(
        await _run_turn(
            name="image_sensitive_skin",
            session_id=session_id,
            request=ChatStreamRequest(message="这个适合敏感肌吗？", image_url=image_url),
            expect={
                "product_card_min": 1,
                "criteria_card": True,
                "final_decision": True,
                "image_url": bool(image_url),
            },
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
                "final_decision": True,
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
    events = [event async for event in chat_stream(session_id, request)]
    summary = _summarize_events(events)
    ok, failures = _evaluate(summary, expect)
    return {
        "name": name,
        "ok": ok,
        "duration_ms": round((time.perf_counter() - started) * 1000, 2),
        "message": request.message,
        "expect": expect,
        "failures": failures,
        **summary,
    }


def _summarize_events(events: list[SSEEventBase]) -> dict[str, Any]:
    tags = [event.event for event in events]
    product_events = [event for event in events if event.event == "product_card"]
    criteria_events = [event for event in events if event.event == "criteria_card"]
    decision_events = [event for event in events if event.event == "final_decision"]
    cart_events = [event for event in events if event.event == "cart_action"]
    error_events = [event for event in events if event.event == "error"]
    first_product = product_events[0] if product_events else None
    first_evidence = first_product.evidence[0] if first_product and first_product.evidence else None
    return {
        "events": tags,
        "event_count": len(events),
        "product_count": len(product_events),
        "product_ids": [event.product.product_id for event in product_events],
        "has_criteria": bool(criteria_events),
        "criteria_summary": criteria_events[-1].criteria.summary if criteria_events else None,
        "has_decision": bool(decision_events),
        "winner_product_id": decision_events[-1].winner_product_id if decision_events else None,
        "cart_actions": [
            {"action": event.action, "product_id": event.product_id, "quantity": event.quantity, "status": event.status}
            for event in cart_events
        ],
        "first_evidence_source_id": first_evidence.source_id if first_evidence else None,
        "first_evidence_chars": len(first_evidence.snippet) if first_evidence else 0,
        "errors": [{"code": event.code, "message": event.message} for event in error_events],
    }


def _evaluate(summary: dict[str, Any], expect: dict[str, Any]) -> tuple[bool, list[str]]:
    failures: list[str] = []
    if summary["errors"]:
        failures.append("has error event")
    if expect.get("criteria_card") and not summary["has_criteria"]:
        failures.append("missing criteria_card")
    if expect.get("final_decision") and not summary["has_decision"]:
        failures.append("missing final_decision")
    product_card_min = expect.get("product_card_min")
    if isinstance(product_card_min, int) and summary["product_count"] < product_card_min:
        failures.append(f"product_count<{product_card_min}")
    if expect.get("first_evidence_source_id") and not summary["first_evidence_source_id"]:
        failures.append("missing first evidence source_id")
    expected_cart_action = expect.get("cart_action")
    if expected_cart_action and not any(action["action"] == expected_cart_action for action in summary["cart_actions"]):
        failures.append(f"missing cart_action:{expected_cart_action}")
    if expect.get("image_url") is False:
        failures.append("missing demo image")
    return not failures, failures


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
