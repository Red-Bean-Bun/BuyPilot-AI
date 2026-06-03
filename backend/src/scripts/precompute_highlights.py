"""Precompute product highlights from rag_knowledge using LLM.

Reads marketing_description + official_faq + user_reviews for each product,
asks LLM to extract 3-5 concise selling points, writes them to
Product.metadata["highlights"].

Run from backend/:
    uv run -m src.scripts.precompute_highlights
"""

from __future__ import annotations

import asyncio
import json
import logging

from sqlmodel import select
from sqlmodel.ext.asyncio.session import AsyncSession

from src.repos.database import create_db_and_tables, get_async_engine
from src.repos.models import Product
from src.services.llm_gateway import _call_chat_task

logger = logging.getLogger(__name__)

SYSTEM_PROMPT = """\
你是一个电商数据分析师。从商品信息中提取 3-5 条核心卖点。
要求：
- 每条卖点不超过 20 个汉字
- 必须是客观事实，来自提供的商品信息，禁止编造
- 优先提取：核心成分/材质、功效/性能、适用人群、使用场景、性价比
- 不要提取负面信息或风险提示
格式：直接输出 JSON 数组，如 ["卖点1", "卖点2", "卖点3"]，不要输出其他内容。"""


def _build_user_prompt(
    name: str,
    marketing_description: str | None,
    faqs: list[dict[str, str]],
    reviews: list[dict],
) -> str:
    parts = [f"商品名：{name}"]
    if marketing_description:
        parts.append(f"商品描述：{marketing_description}")
    if faqs:
        faq_text = "\n".join(f"Q: {f.get('question', '')}\nA: {f.get('answer', '')}" for f in faqs)
        parts.append(f"官方问答：\n{faq_text}")
    if reviews:
        review_text = "\n".join(
            f"[{r.get('rating', '?')}星] {r.get('content', '')}" for r in reviews
        )
        parts.append(f"用户评价：\n{review_text}")
    return "\n\n".join(parts)


def _parse_highlights(text: str) -> list[str]:
    """Extract a JSON string array from LLM output, tolerating markdown fences."""
    cleaned = text.strip()
    if cleaned.startswith("```"):
        lines = cleaned.split("\n")
        cleaned = "\n".join(lines[1:-1] if lines[-1].strip() == "```" else lines[1:])
    try:
        result = json.loads(cleaned)
    except json.JSONDecodeError:
        logger.warning("Failed to parse highlights JSON: %s", cleaned[:200])
        return []
    if not isinstance(result, list):
        return []
    return [str(item) for item in result if isinstance(item, str) and item.strip()]


async def precompute_highlights() -> dict[str, int]:
    await create_db_and_tables()

    async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
        products = (await session.exec(select(Product))).all()

    total = len(products)
    updated = 0
    skipped = 0

    for idx, product in enumerate(products):
        metadata = dict(product.product_metadata or {})
        if metadata.get("highlights"):
            skipped += 1
            print(f"[{idx + 1}/{total}] SKIP {product.id} (already has highlights)", flush=True)
            continue

        faqs = product.official_faq or []
        reviews = product.user_reviews or []
        desc = product.marketing_description

        if not desc and not faqs and not reviews:
            skipped += 1
            print(f"[{idx + 1}/{total}] SKIP {product.id} (no rag_knowledge)", flush=True)
            continue

        user_prompt = _build_user_prompt(product.name, desc, faqs, reviews)
        messages = [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_prompt},
        ]

        try:
            raw = await _call_chat_task("generate_recommendation", messages, json_object=True)
            highlights = _parse_highlights(raw)
            if not highlights:
                skipped += 1
                print(f"[{idx + 1}/{total}] FAIL {product.id} (empty highlights)", flush=True)
                continue

            highlights = [h[:20] for h in highlights[:5]]
            metadata["highlights"] = highlights

            async with AsyncSession(get_async_engine(), expire_on_commit=False) as session:
                db_product = (await session.exec(
                    select(Product).where(Product.id == product.id)
                )).one()
                db_product.product_metadata = metadata
                session.add(db_product)
                await session.commit()

            updated += 1
            print(
                f"[{idx + 1}/{total}] OK {product.id}: {json.dumps(highlights, ensure_ascii=False)}",
                flush=True,
            )
        except Exception as exc:
            skipped += 1
            print(f"[{idx + 1}/{total}] ERROR {product.id}: {exc}", flush=True)

    return {"total": total, "updated": updated, "skipped": skipped}


def main() -> None:
    logging.basicConfig(level=logging.INFO)
    stats = asyncio.run(precompute_highlights())
    print(json.dumps({"check": "precompute_highlights", **stats}, ensure_ascii=False))


if __name__ == "__main__":
    main()
