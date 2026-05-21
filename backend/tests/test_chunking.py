from src.services.chunking import chunk_product_text


def test_chunk_product_text_main_and_supplementary_chunks():
    chunks = chunk_product_text("洁面乳 | 油性适用 | 52元 | 氨基酸 | 泡沫丰富 | 日常护肤")
    assert chunks[0][1] == 0
    assert "洁面乳" in chunks[0][0]
    assert len(chunks) == 2

