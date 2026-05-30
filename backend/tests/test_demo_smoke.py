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
