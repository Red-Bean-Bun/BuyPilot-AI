import jsonschema

from src.repos.products import _normalize_sku_options
from src.types.sse_events import (
    CartActionEvent,
    CartItemEventPayload,
    CartSummaryPayload,
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
                cart=CartSummaryPayload(
                    items=[CartItemEventPayload(product_id="p1", name="T", price=12.5, quantity=2)],
                    total_items=2,
                    total_price=25.0,
                ),
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


class TestSkuOptionsNormalization:
    """Issue 8: raw SKU data → ProductPayload.sku_options contract."""

    def test_normalize_valid_sku_list(self):
        raw_skus = [
            {"sku_id": "s_001_1", "properties": {"容量": "30ml"}, "price": 720.0},
            {"sku_id": "s_001_2", "properties": {"容量": "50ml"}, "price": 980.0},
        ]
        result = _normalize_sku_options(raw_skus)
        assert result is not None
        assert len(result) == 2
        assert result[0]["sku_id"] == "s_001_1"
        assert result[0]["properties"]["容量"] == "30ml"
        assert result[0]["price"] == 720.0

    def test_normalize_none_input(self):
        assert _normalize_sku_options(None) is None

    def test_normalize_empty_list(self):
        assert _normalize_sku_options([]) is None

    def test_normalize_non_list_input(self):
        assert _normalize_sku_options("not a list") is None

    def test_normalize_sku_without_sku_id_skipped(self):
        raw_skus = [
            {"properties": {"容量": "30ml"}, "price": 720.0},
            {"sku_id": "s_001_1", "properties": {"容量": "50ml"}, "price": 980.0},
        ]
        result = _normalize_sku_options(raw_skus)
        assert result is not None
        assert len(result) == 1
        assert result[0]["sku_id"] == "s_001_1"

    def test_normalize_sku_missing_properties_defaults_empty_dict(self):
        raw_skus = [{"sku_id": "s_001_1", "price": 720.0}]
        result = _normalize_sku_options(raw_skus)
        assert result is not None
        assert result[0]["properties"] == {}

    def test_product_payload_with_sku_options_validates_against_schema(self, sse_schema):
        """ProductPayload with sku_options must validate against the JSON Schema contract."""
        seq = EventSeq("turn_sku_test")
        product = ProductPayload(
            product_id="p_test",
            name="Test Product",
            category="美妆护肤",
            sku_options=[
                {"sku_id": "s_test_1", "properties": {"容量": "30ml"}, "price": 720.0},
            ],
        )
        event = ProductCardEvent(
            session_id="s1",
            turn_id="turn_sku_test",
            seq=seq.next(),
            event_id=seq.event_id(),
            node_id="product_p_test",
            deck_id="deck_sku_test",
            rank=1,
            product=product,
            reason="test reason",
            evidence=[EvidencePayload(source_type="chunk", snippet="s")],
        )
        data = event.model_dump()
        jsonschema.validate(data, sse_schema)

    def test_final_decision_score_breakdown_serializes_and_validates(self, sse_schema):
        """FinalDecisionEvent with score_breakdown must validate against the JSON Schema."""
        seq = EventSeq("turn_score_test")
        event = FinalDecisionEvent(
            session_id="s1",
            turn_id="turn_score_test",
            seq=seq.next(),
            event_id=seq.event_id(),
            node_id="decision_turn_score_test",
            winner_product_id="p1",
            summary="综合评分最高",
            why=["性价比突出"],
            not_for=["敏感肌慎用"],
            decision_status="selected",
            confidence="high",
            next_step="accept_recommendation",
            score_breakdown={
                "retrieval": 0.85,
                "criteria_match": 1.25,
                "user_signal": 1.0,
                "evidence": 0.45,
                "risk_penalty": 0.0,
                "final_score": 0.8125,
                "rank": 1,
            },
        )
        data = event.model_dump()
        assert data["score_breakdown"]["final_score"] == 0.8125
        assert data["score_breakdown"]["rank"] == 1
        jsonschema.validate(data, sse_schema)
