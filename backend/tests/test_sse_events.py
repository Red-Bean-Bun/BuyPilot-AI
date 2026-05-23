import jsonschema

from src.types.sse_events import (
    CartActionEvent,
    ClarificationEvent,
    Constraints,
    CriteriaCardEvent,
    CriteriaPayload,
    DoneEvent,
    ErrorEvent,
    EventSeq,
    EvidencePayload,
    FinalDecisionEvent,
    ProductCardEvent,
    ProductPayload,
    TextDeltaEvent,
    ThinkingEvent,
    format_sse,
    parse_sse_event,
)


# Helper to construct events with required envelope fields
def _seq() -> EventSeq:
    return EventSeq("turn_test")


def _base(session_id="s1", turn_id="turn_test", seq_obj=None) -> dict:
    if seq_obj is None:
        seq_obj = _seq()
    s = seq_obj.next()
    return {
        "session_id": session_id,
        "turn_id": turn_id,
        "seq": s,
        "event_id": seq_obj.event_id(),
    }


class TestEnvelopeFields:
    def test_event_seq_increment(self):
        seq = EventSeq("turn_001")
        assert seq.next() == 1
        assert seq.next() == 2
        assert seq.event_id() == "turn_001:2"


class TestFormatSSE:
    def test_format_sse_wire_format(self):
        e = ThinkingEvent(
            session_id="s1", turn_id="t1", seq=1, event_id="t1:1", node_id="thinking_t1", stage="x", message="y"
        )
        s = format_sse(e)
        assert s.startswith("event: thinking\n")
        assert 'data: {"schema_version"' in s
        assert s.endswith("\n\n")

    def test_format_sse_all_types(self):
        events = [
            DoneEvent(session_id="s1", turn_id="t1", seq=1, event_id="t1:1", node_id="done_t1"),
            ErrorEvent(
                session_id="s1", turn_id="t1", seq=2, event_id="t1:2", node_id="error_t1", code="X", message="Y"
            ),
            TextDeltaEvent(
                session_id="s1", turn_id="t1", seq=3, event_id="t1:3", node_id="text_t1", message_id="m1", delta="hi"
            ),
            CartActionEvent(
                session_id="s1",
                turn_id="t1",
                seq=4,
                event_id="t1:4",
                node_id="cart_t1",
                action="add",
                product_id="p1",
                status="success",
            ),
        ]
        for e in events:
            s = format_sse(e)
            assert s.startswith(f"event: {e.event}\n")
            assert s.endswith("\n\n")


class TestParseSSEEvent:
    def test_parse_thinking(self):
        e = ThinkingEvent(
            session_id="s1", turn_id="t1", seq=1, event_id="t1:1", node_id="thinking_t1", stage="x", message="y"
        )
        data = e.model_dump_json()
        parsed = parse_sse_event(data)
        assert isinstance(parsed, ThinkingEvent)
        assert parsed.stage == "x"
        assert parsed.turn_id == "t1"
        assert parsed.seq == 1

    def test_parse_unknown_tag(self):
        assert parse_sse_event('{"event":"unknown","session_id":"s1"}') is None


class TestSchemaValidation:
    def test_all_events_valid_against_schema(self, sse_schema):
        seq = EventSeq("turn_schema_test")
        events = [
            ThinkingEvent(
                session_id="s1",
                turn_id="turn_schema_test",
                seq=seq.next(),
                event_id=seq.event_id(),
                node_id="thinking_turn_schema_test",
                stage="x",
                message="y",
            ),
            ClarificationEvent(
                session_id="s1",
                turn_id="turn_schema_test",
                seq=seq.next(),
                event_id=seq.event_id(),
                node_id="clarification_turn_schema_test",
                question="q?",
            ),
            CriteriaCardEvent(
                session_id="s1",
                turn_id="turn_schema_test",
                seq=seq.next(),
                event_id=seq.event_id(),
                node_id="criteria_c1",
                criteria=CriteriaPayload(category="美妆护肤", constraints=Constraints(skin_type="油性")),
            ),
            TextDeltaEvent(
                session_id="s1",
                turn_id="turn_schema_test",
                seq=seq.next(),
                event_id=seq.event_id(),
                node_id="ai_text_turn_schema_test",
                message_id="m1",
                delta="d",
            ),
            ProductCardEvent(
                session_id="s1",
                turn_id="turn_schema_test",
                seq=seq.next(),
                event_id=seq.event_id(),
                node_id="product_p1",
                deck_id="deck_turn_schema_test",
                rank=1,
                product=ProductPayload(product_id="p1", name="T", category="美妆护肤"),
                reason="r",
                evidence=[EvidencePayload(source_type="chunk", snippet="s")],
            ),
            CartActionEvent(
                session_id="s1",
                turn_id="turn_schema_test",
                seq=seq.next(),
                event_id=seq.event_id(),
                node_id="cart_turn_schema_test",
                action="add",
                product_id="p1",
                status="success",
            ),
            FinalDecisionEvent(
                session_id="s1",
                turn_id="turn_schema_test",
                seq=seq.next(),
                event_id=seq.event_id(),
                node_id="decision_turn_schema_test",
                winner_product_id="p1",
                summary="s",
            ),
            DoneEvent(
                session_id="s1",
                turn_id="turn_schema_test",
                seq=seq.next(),
                event_id=seq.event_id(),
                node_id="done_turn_schema_test",
            ),
            ErrorEvent(
                session_id="s1",
                turn_id="turn_schema_test",
                seq=seq.next(),
                event_id=seq.event_id(),
                node_id="error_turn_schema_test",
                code="X",
                message="Y",
            ),
        ]
        for e in events:
            data = e.model_dump()
            jsonschema.validate(data, sse_schema)

    def test_golden_trace_valid(self, sse_schema, golden_budget_beauty):
        from tests.conftest import parse_sse_stream

        events = parse_sse_stream(golden_budget_beauty)
        assert len(events) >= 5
        for tag, data in events:
            assert data["event"] == tag
            jsonschema.validate(data, sse_schema)

    def test_golden_trace_clarification_valid(self, sse_schema, golden_clarification):
        from tests.conftest import parse_sse_stream

        events = parse_sse_stream(golden_clarification)
        assert len(events) >= 2
        for tag, data in events:
            assert data["event"] == tag
            jsonschema.validate(data, sse_schema)

    def test_golden_trace_error_valid(self, sse_schema, golden_error):
        from tests.conftest import parse_sse_stream

        events = parse_sse_stream(golden_error)
        assert len(events) >= 2
        for tag, data in events:
            assert data["event"] == tag
            jsonschema.validate(data, sse_schema)
