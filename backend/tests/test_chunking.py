from src.services.chunking import build_product_chunks, build_product_knowledge_package, chunk_product_text


def test_chunk_product_text_main_and_supplementary_chunks():
    chunks = chunk_product_text("洁面乳 | 油性适用 | 52元 | 氨基酸 | 泡沫丰富 | 日常护肤")
    assert chunks[0][1] == 0
    assert "洁面乳" in chunks[0][0]
    assert len(chunks) == 2


def test_build_product_chunks_keeps_positive_sensitive_skin_faq_in_primary_recall():
    chunks = build_product_chunks(
        {
            "product_id": "p_test",
            "title": "温和洁面",
            "brand": "测试品牌",
            "category": "美妆护肤",
            "sub_category": "洁面",
            "base_price": 88,
            "rag_knowledge": {
                "marketing_description": "专为敏感肌日常清洁设计，主打温和保湿。",
                "official_faq": [
                    {
                        "question": "这款适合敏感肌吗？",
                        "answer": "适合，敏感肌可以放心使用，不含酒精和香精。",
                    }
                ],
                "user_reviews": [{"rating": 1, "nickname": "小敏", "content": "我用后泛红，敏感肌不适合。"}],
            },
        }
    )

    faq_chunks = [chunk for chunk in chunks if chunk.metadata["chunk_type"] == "faq"]
    negative_chunks = [chunk for chunk in chunks if chunk.metadata["chunk_type"] == "negative_review"]
    assert faq_chunks[0].metadata["retrieval_role"] == "primary"
    assert negative_chunks[0].metadata["retrieval_role"] == "risk"


def test_knowledge_package_ignores_negated_ingredient_aliases():
    package = build_product_knowledge_package(
        {
            "product_id": "p_test",
            "title": "温和洁面",
            "brand": "测试品牌",
            "category": "美妆护肤",
            "sub_category": "洁面",
            "base_price": 88,
            "rag_knowledge": {
                "marketing_description": "适合敏感肌日常清洁，不含酒精和香精。",
                "official_faq": [],
                "user_reviews": [],
            },
        }
    )

    assert "酒精" not in package["attributes"]["ingredient_terms"]
    assert "香精" not in package["attributes"]["ingredient_terms"]
