import src.config.settings as settings_module
from src.scripts import demo_smoke
from src.types.schemas import ChatStreamRequest


def test_demo_smoke_evaluate_requires_expected_events():
    ok, failures = demo_smoke._evaluate(
        {
            "errors": [],
            "has_criteria": True,
            "has_decision": False,
            "product_count": 1,
            "first_evidence_source_id": "p1:0",
            "cart_actions": [],
        },
        {"criteria_card": True, "final_decision": True, "product_card_min": 2},
    )

    assert ok is False
    assert "missing final_decision" in failures
    assert "product_count<2" in failures


def test_demo_smoke_accepts_single_product_completed_recommendation():
    ok, failures = demo_smoke._evaluate(
        {
            "errors": [],
            "has_criteria": True,
            "has_decision": True,
            "product_count": 1,
            "first_evidence_source_id": "p1:0",
            "cart_actions": [],
            "done_reason": "completed",
        },
        {
            "criteria_card": True,
            "product_card_min": 1,
            "recommendation_done_reason": True,
            "first_evidence_source_id": True,
        },
    )

    assert ok is True
    assert failures == []


def test_demo_smoke_accepts_multi_product_awaiting_feedback_recommendation():
    ok, failures = demo_smoke._evaluate(
        {
            "errors": [],
            "has_criteria": True,
            "has_decision": False,
            "product_count": 2,
            "first_evidence_source_id": "p1:0",
            "cart_actions": [],
            "done_reason": "awaiting_product_feedback",
        },
        {
            "criteria_card": True,
            "product_card_min": 1,
            "recommendation_done_reason": True,
            "first_evidence_source_id": True,
        },
    )

    assert ok is True
    assert failures == []


def test_demo_smoke_evaluate_structural_demo_signals():
    ok, failures = demo_smoke._evaluate(
        {
            "errors": [],
            "has_criteria": True,
            "has_decision": False,
            "product_count": 2,
            "product_categories": ["服饰运动", "美妆护肤"],
            "cart_actions": [{"action": "checkout_preview"}],
            "thinking_stages": ["analyzing_image"],
            "has_clarification": True,
            "clarification_required_slots": ["budget"],
            "shopping_strategy_scene_type": "travel",
            "has_compare": True,
        },
        {
            "criteria_card": True,
            "thinking_stage": "analyzing_image",
            "clarification": True,
            "required_slot": "budget",
            "shopping_strategy_scene_type": "travel",
            "product_category_min": 2,
            "compare_card": True,
            "cart_action": "checkout_preview",
        },
    )

    assert ok is True
    assert failures == []


def test_demo_smoke_evaluate_reports_missing_structural_demo_signals():
    ok, failures = demo_smoke._evaluate(
        {
            "errors": [],
            "has_criteria": False,
            "has_decision": False,
            "product_count": 1,
            "product_categories": ["美妆护肤"],
            "cart_actions": [],
            "thinking_stages": ["understanding"],
            "has_clarification": False,
            "clarification_required_slots": [],
            "shopping_strategy_scene_type": None,
            "has_compare": False,
        },
        {
            "thinking_stage": "analyzing_image",
            "clarification": True,
            "required_slot": "budget",
            "shopping_strategy_scene_type": "travel",
            "product_category_min": 2,
            "compare_card": True,
            "cart_action": "checkout_confirm",
        },
    )

    assert ok is False
    assert "missing thinking_stage:analyzing_image" in failures
    assert "missing clarification" in failures
    assert "missing required_slot:budget" in failures
    assert "shopping_strategy_scene_type mismatch: expected travel, got None" in failures
    assert "product_category_count<2" in failures
    assert "missing compare_card" in failures
    assert "missing cart_action:checkout_confirm" in failures


async def test_demo_smoke_main_uses_stable_compare_and_checkout_order(monkeypatch):
    calls = []

    async def fake_run_turn(name: str, session_id: str, request: ChatStreamRequest, expect: dict):
        calls.append(
            {
                "name": name,
                "session_id": session_id,
                "message": request.message,
                "compare_product_ids": request.compare_product_ids,
                "expect": expect,
            }
        )
        result = {
            "name": name,
            "ok": True,
            "product_ids": ["p_beauty_006"],
        }
        if name == "travel_combo_strategy":
            result["product_ids"] = ["p_beauty_006", "p_clothes_001", "p_clothes_002"]
        return result

    class FakeCart:
        total_items = 1
        total_price = 170.0
        items = []

    async def fake_get_cart(_session_id: str):
        return FakeCart()

    monkeypatch.setattr(demo_smoke, "_check_live_provider", lambda: None)
    monkeypatch.setattr(demo_smoke, "_check_postgres", lambda: None)
    monkeypatch.setattr(demo_smoke, "_demo_image_url", lambda: "/uploads/demo.jpg")
    monkeypatch.setattr(demo_smoke, "_run_turn", fake_run_turn)
    monkeypatch.setattr(demo_smoke, "get_cart", fake_get_cart)

    report = await demo_smoke.main_async(write_report=False)

    names = [call["name"] for call in calls]
    assert report["ok"] is True
    assert names.index("travel_combo_strategy") < names.index("compare_first_two")
    assert names.index("cart_add") < names.index("checkout_preview") < names.index("checkout_confirm")

    travel_call = next(call for call in calls if call["name"] == "travel_combo_strategy")
    compare_call = next(call for call in calls if call["name"] == "compare_first_two")
    assert compare_call["session_id"] == travel_call["session_id"]
    assert compare_call["compare_product_ids"] == ["p_beauty_006", "p_clothes_001"]


async def test_demo_smoke_run_turn_times_out(monkeypatch):
    async def never_returns(_session_id, _request):
        await demo_smoke.asyncio.sleep(3600)
        return []

    monkeypatch.setattr(demo_smoke, "TURN_TIMEOUT_SECONDS", 0.01)
    monkeypatch.setattr(demo_smoke, "_collect_turn_events", never_returns)

    result = await demo_smoke._run_turn(
        "timeout_case",
        "sess_timeout",
        ChatStreamRequest(message="推荐洗面奶"),
        {"product_card_min": 1},
    )

    assert result["ok"] is False
    assert result["failures"] == ["turn timeout after 0.01s"]


def test_demo_smoke_uses_upload_url_for_official_image(monkeypatch, tmp_path):
    dataset_dir = tmp_path / "dataset"
    image_path = dataset_dir / demo_smoke.DEFAULT_IMAGE_PATH
    image_path.parent.mkdir(parents=True)
    image_path.write_bytes(b"fake-jpeg")
    upload_dir = tmp_path / "uploads"

    monkeypatch.setenv("ECOMMERCE_DATASET_DIR", str(dataset_dir))
    monkeypatch.setenv("UPLOAD_DIR", str(upload_dir))
    settings_module._settings = None

    image_url = demo_smoke._demo_image_url()

    assert image_url == "/uploads/demo_p_beauty_012_live.jpg"
    assert (upload_dir / "demo_p_beauty_012_live.jpg").read_bytes() == b"fake-jpeg"
