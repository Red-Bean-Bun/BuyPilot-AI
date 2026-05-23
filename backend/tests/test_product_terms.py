from src.repos.products import INGREDIENT_TERMS, _extract_terms


def test_extract_terms_ignores_negated_ingredients():
    text = "适合敏感肌使用，不含酒精和香精。"

    terms = _extract_terms(text, INGREDIENT_TERMS)

    assert "酒精" not in terms
    assert "香精" not in terms


def test_extract_terms_keeps_positive_ingredient_mentions_after_scope_breaker():
    text = "不含酒精但含香精，适合耐受肌。"

    terms = _extract_terms(text, INGREDIENT_TERMS)

    assert "酒精" not in terms
    assert "香精" in terms
