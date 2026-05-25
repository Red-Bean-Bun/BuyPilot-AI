from src.services.recommendation_reasons import build_reason_atoms, reason_from_atoms
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
