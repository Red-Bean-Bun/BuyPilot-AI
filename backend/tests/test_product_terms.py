from src.config.domain_terms import avoid_trait_matches_text, extract_feedback_avoid_terms, extract_terms
from src.repos.products import INGREDIENT_TERMS


def test_extract_terms_ignores_negated_ingredients():
    text = "适合敏感肌使用，不含酒精和香精。"

    terms = extract_terms(text, INGREDIENT_TERMS)

    assert "酒精" not in terms
    assert "香精" not in terms


def test_extract_terms_keeps_positive_ingredient_mentions_after_scope_breaker():
    text = "不含酒精但含香精，适合耐受肌。"

    terms = extract_terms(text, INGREDIENT_TERMS)

    assert "酒精" not in terms
    assert "香精" in terms


def test_feedback_avoid_terms_drop_nested_brand_aliases():
    assert extract_feedback_avoid_terms("不要日系品牌，还有什么") == ["日系品牌"]
    assert extract_feedback_avoid_terms("不喜欢Nike") == ["Nike"]


def test_avoid_trait_matches_domain_brand_aliases():
    assert avoid_trait_matches_text("日系品牌", "安热沙 防晒霜")
    assert avoid_trait_matches_text("耐克", "Nike Pegasus 跑鞋")
