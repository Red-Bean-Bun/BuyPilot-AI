import src.config.settings as settings_module
from src.scripts import demo_smoke


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
