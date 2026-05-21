"""Product text chunking helpers for ingestion."""

from __future__ import annotations


def clean_text(text: str, max_chars: int) -> str:
    return " ".join(text.split())[:max_chars]


def chunk_product_text(text: str) -> list[tuple[str, int]]:
    parts = [part.strip() for part in text.split("|") if part.strip()]
    if not parts:
        return []
    header = parts[0]
    features = parts[1:]
    main = clean_text(" | ".join([header] + features[:3]), 400)
    chunks = [(main, 0)]
    for index, offset in enumerate(range(3, len(features), 2), start=1):
        chunk = clean_text(f"{header[:50]} | " + " | ".join(features[offset:offset + 2]), 200)
        if chunk:
            chunks.append((chunk, index))
    return chunks

