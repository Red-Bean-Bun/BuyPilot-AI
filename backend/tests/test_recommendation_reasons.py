from src.repos.documents import ChunkDocument
from src.services.recommendation_reasons import build_reason_atoms, build_risk_notes, reason_from_atoms
from src.types.sse_events import Constraints, CriteriaPayload, EvidencePayload, ProductPayload


def test_reason_atoms_are_criteria_aware_and_keep_multiple_skin_matches():
    criteria = CriteriaPayload(
        category="美妆护肤",
        constraints=Constraints(skin_type="混合性", budget_max=200, product_type="洗面奶"),
    )
    product = ProductPayload(
        product_id="p1",
        name="测试洁面",
        category="美妆护肤",
        sub_category="洁面",
        price=99,
        skin_type_match=["油性", "混合性"],
    )
    evidence = [EvidencePayload(source_type="product_chunk", source_id="chunk_1", snippet="适合油性和混合性肌肤")]

    atoms = build_reason_atoms(criteria, product, evidence)
    reason = reason_from_atoms(product, atoms)

    assert atoms[0].dimension == "skin_type"
    assert atoms[0].value == "混合性、油性"
    assert atoms[0].evidence_id == "chunk_1"
    assert "混合性肤质匹配" in reason
    assert any(atom.dimension == "budget" and atom.text == "99元符合200元预算" for atom in atoms)


def test_reason_atoms_show_two_skin_matches_when_user_did_not_specify_skin():
    criteria = CriteriaPayload(category="美妆护肤")
    product = ProductPayload(
        product_id="p1",
        name="测试面霜",
        category="美妆护肤",
        sub_category="面霜",
        skin_type_match=["油性", "混合性", "敏感"],
    )

    atoms = build_reason_atoms(criteria, product, [])
    reason = reason_from_atoms(product, atoms)

    assert atoms[0].text == "油性、混合性等适用"
    assert reason.startswith("油性、混合性等适用")


def test_reason_atoms_fall_back_to_category_fact():
    criteria = CriteriaPayload(category="服饰运动")
    product = ProductPayload(product_id="p1", name="测试商品", category="服饰运动")

    atoms = build_reason_atoms(criteria, product, [])

    assert atoms[0].dimension == "category"
    assert reason_from_atoms(product, atoms) == "服饰运动类目匹配。"


# ── build_risk_notes tests ──────────────────────────────────────────────


def _risk_chunk(chunk_type: str, text: str, **meta) -> ChunkDocument:
    metadata = {"chunk_type": chunk_type, "retrieval_role": "risk", "evidence_kind": "risk", **meta}
    return ChunkDocument(
        id=f"chunk_{chunk_type}",
        product_id="p1",
        chunk_text=text,
        chunk_index=0,
        embedding=[],
        metadata=metadata,
    )


def test_build_risk_notes_extracts_negative_review_with_rating():
    chunks = [_risk_chunk("negative_review", "[小明 评分:2] 用了一周脸上起红疹，刺痛感明显", nickname="小明", rating=2)]
    notes = build_risk_notes(chunks)

    assert len(notes) == 1
    assert "小明" in notes[0]
    assert "评分2" in notes[0]
    assert "红疹" in notes[0]


def test_build_risk_notes_extracts_warning_text():
    chunks = [_risk_chunk("warning", "不建议敏感肌使用，含酒精成分可能引起刺激")]
    notes = build_risk_notes(chunks)

    assert len(notes) == 1
    assert "敏感肌" in notes[0]


def test_build_risk_notes_empty_input_returns_empty():
    assert build_risk_notes([]) == []


def test_build_risk_notes_deduplicates():
    chunks = [
        _risk_chunk("warning", "含酒精成分"),
        _risk_chunk("faq", "含酒精成分"),
    ]
    notes = build_risk_notes(chunks)
    assert len(notes) == 1


def test_build_risk_notes_respects_limit():
    chunks = [
        _risk_chunk("warning", "风险一"),
        _risk_chunk("warning", "风险二"),
        _risk_chunk("warning", "风险三"),
        _risk_chunk("warning", "风险四"),
    ]
    notes = build_risk_notes(chunks)
    assert len(notes) == 3


def test_build_risk_notes_truncates_long_text():
    long_text = "这是一段非常长的风险描述" * 20
    chunks = [_risk_chunk("warning", long_text)]
    notes = build_risk_notes(chunks)
    assert len(notes) == 1
    assert notes[0].endswith("…")
    assert len(notes[0]) <= 180
