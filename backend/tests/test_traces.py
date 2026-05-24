from sqlmodel import Session, select

from src.services.product_ingest import seed_products
from src.repos.models import EvidenceLink, RetrievalTrace
from src.runtime.pipeline import chat_stream
from src.types.schemas import ChatStreamRequest


def test_pipeline_persists_retrieval_trace_and_evidence_links(monkeypatch, tmp_path):
    database_url = f"sqlite:///{tmp_path / 'trace.db'}"
    monkeypatch.setenv("DATABASE_URL", database_url)

    from src.config import settings as settings_module

    settings_module._settings = None
    seed_products()

    async def run_pipeline():
        return [
            event
            async for event in chat_stream(
                "sess_trace",
                ChatStreamRequest(message="推荐适合油皮的洗面奶，200元以内，日常护肤"),
            )
        ]

    import asyncio

    events = asyncio.run(run_pipeline())
    assert events[-1].event == "done"

    from src.repos.database import get_engine

    with Session(get_engine()) as session:
        traces = session.exec(select(RetrievalTrace)).all()
        links = session.exec(select(EvidenceLink)).all()

    assert traces
    assert traces[0].selected_ids
    timings = traces[0].filters_applied.get("_stage_timings_ms", {})
    assert {"intent", "criteria", "retrieve", "recommendation", "decision"} <= set(timings)
    fallbacks = traces[0].filters_applied.get("_fallbacks", [])
    assert any(item.get("component") == "rerank.texts" for item in fallbacks)
    assert links
    assert links[0].product_id in traces[0].selected_ids
