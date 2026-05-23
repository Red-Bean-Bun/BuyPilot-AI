import json
import re
from pathlib import Path

import httpx
import pytest

from src.api.app import app

PROJECT_ROOT = Path(__file__).resolve().parents[2]
CONTRACTS_DIR = PROJECT_ROOT / "contracts"


@pytest.fixture
def test_client():
    return httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test")


@pytest.fixture(autouse=True)
def mock_external_ai(monkeypatch):
    async def fake_chat_completion(*args, **kwargs):
        return None

    async def fake_embedding_request(profile, texts):
        vectors = []
        for text in texts:
            values = [float((ord(ch) % 31) / 31) for ch in text[:16]]
            vectors.append(values + [0.0] * (16 - len(values)))
        return vectors

    async def fake_rerank_request(profile, query, documents, top_n):
        return []

    from src.services import embedding, llm_gateway, reranker

    monkeypatch.setattr(llm_gateway, "_chat_completion", fake_chat_completion)
    monkeypatch.setattr(embedding, "_embedding_request", fake_embedding_request)
    monkeypatch.setattr(reranker, "_rerank_request", fake_rerank_request)


@pytest.fixture
def sse_schema():
    path = CONTRACTS_DIR / "sse-events.schema.json"
    with open(path, encoding="utf-8") as f:
        return json.load(f)


@pytest.fixture
def golden_budget_beauty():
    path = CONTRACTS_DIR / "examples" / "demo_budget_beauty.sse"
    if not path.exists():
        pytest.skip("golden trace not found")
    with open(path, encoding="utf-8") as f:
        return f.read()


@pytest.fixture
def golden_clarification():
    path = CONTRACTS_DIR / "examples" / "demo_clarification.sse"
    if not path.exists():
        pytest.skip("golden trace not found")
    with open(path, encoding="utf-8") as f:
        return f.read()


@pytest.fixture
def golden_error():
    path = CONTRACTS_DIR / "examples" / "demo_error.sse"
    if not path.exists():
        pytest.skip("golden trace not found")
    with open(path, encoding="utf-8") as f:
        return f.read()


def parse_sse_stream(text: str) -> list[tuple[str, dict]]:
    events = []
    blocks = re.split(r"\n\n+", text.strip())
    for block in blocks:
        lines = block.strip().split("\n")
        event_type = None
        data = None
        for line in lines:
            if line.startswith("event: "):
                event_type = line[len("event: ") :].strip()
            elif line.startswith("data: "):
                data = json.loads(line[len("data: ") :].strip())
        if event_type and data:
            events.append((event_type, data))
    return events


async def collect_sse_stream(response: httpx.Response) -> list[tuple[str, dict]]:
    content = b""
    async for chunk in response.aiter_bytes():
        content += chunk
    text = content.decode("utf-8")
    return parse_sse_stream(text)
