import base64

import pytest

import src.config.settings as settings_module
from src.services.image_upload import image_url_to_provider_url


PNG_1X1 = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="
)


@pytest.fixture
def upload_dir(monkeypatch, tmp_path):
    monkeypatch.setenv("DATABASE_URL", f"sqlite:///{tmp_path / 'image_upload.db'}")
    monkeypatch.setenv("UPLOAD_DIR", str(tmp_path))
    settings_module._settings = None
    yield tmp_path
    settings_module._settings = None


class TestImageUploadEndpoint:
    @pytest.mark.asyncio
    async def test_root_upload_image_accepts_multipart(self, test_client, upload_dir):
        async with test_client as c:
            resp = await c.post(
                "/upload/image",
                files={"file": ("skin.png", PNG_1X1, "image/png")},
                data={"session_id": "sess_upload", "purpose": "chat_input"},
            )

        assert resp.status_code == 200
        data = resp.json()
        assert data["image_url"].startswith("/uploads/upload_")
        assert data["width"] == 1
        assert data["height"] == 1
        assert data["mime_type"] == "image/png"
        assert (upload_dir / data["image_url"].split("/")[-1]).exists()

    @pytest.mark.asyncio
    async def test_upload_rejects_invalid_mime_type(self, test_client, upload_dir):
        async with test_client as c:
            resp = await c.post(
                "/upload/image",
                files={"file": ("note.txt", b"not an image", "text/plain")},
            )

        assert resp.status_code == 400
        assert resp.json()["detail"]["code"] == "IMAGE_FORMAT_INVALID"


def test_local_upload_url_converts_to_provider_data_url(upload_dir):
    image_path = upload_dir / "upload_test.png"
    image_path.write_bytes(PNG_1X1)

    provider_url = image_url_to_provider_url("/uploads/upload_test.png")

    assert provider_url.startswith("data:image/png;base64,")
