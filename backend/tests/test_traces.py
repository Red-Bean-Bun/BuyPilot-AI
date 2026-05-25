import pytest
from sqlmodel import select
from sqlmodel.ext.asyncio.session import AsyncSession

from src.repos.models import EvidenceLink, RetrievalTrace
from src.runtime.pipeline import chat_stream
from src.services.product_ingest import seed_products
from src.types.schemas import ChatStreamRequest


@pytest.mark.asyncio
async def test_pipeline_persists_retrieval_trace_and_evidence_links(monkeypatch, tmp_path):
    database_url = f"sqlite:///{tmp_path / 'trace.db'}"
    monkeypatch.setenv("DATABASE_URL", database_url)

    from src.config import settings as settings_module

    settings_module._settings = None
    await seed_products()

    events = [
        event
        async for event in chat_stream(
            "sess_trace",
            ChatStreamRequest(message="推荐适合油皮的洗面奶，200元以内，日常护肤"),
        )
    ]
    assert events[-1].event == "done"

    from src.repos.database import get_async_engine

    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        traces = (await session.exec(select(RetrievalTrace))).all()
        links = (await session.exec(select(EvidenceLink))).all()

    assert traces
    assert traces[0].selected_ids
    assert traces[0].vector_count >= traces[0].hit_count
    assert traces[0].vector_top_k[0]["chunk_id"]
    assert traces[0].vector_top_k[0]["vector_score"] > 0
    timings = traces[0].filters_applied.get("_stage_timings_ms", {})
    assert {"intent", "criteria", "retrieve", "recommendation", "decision"} <= set(timings)
    fallbacks = traces[0].filters_applied.get("_fallbacks", [])
    assert any(item.get("component") == "rerank.texts" for item in fallbacks)
    assert links
    assert links[0].product_id in traces[0].selected_ids
    assert links[0].chunk_id
    assert links[0].source_id_raw == links[0].chunk_id
    assert links[0].snippet
