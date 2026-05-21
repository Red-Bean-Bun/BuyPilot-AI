"""
BuyPilot-AI 数据处理管道
只处理导师官方100条JSON → 统一product格式 + rag_knowledge拆chunk → 输出products.json + chunks.json

用法:
    python data/scripts/process_data.py
"""

import json
import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent
RAW_OFFICIAL = BASE_DIR / "raw" / "ecommerce_agent_dataset"
OUT_DIR = BASE_DIR / "processed"

CATEGORY_MAP = {
    "1_美妆护肤": "美妆护肤",
    "2_数码电子": "数码电子",
    "3_服饰运动": "服饰运动",
    "4_食品生活": "食品生活",
}


def process_official_data():
    """读取官方100条JSON，转换为统一格式"""
    products = []
    chunks = []

    for cat_dir_name, cat_name in CATEGORY_MAP.items():
        data_dir = RAW_OFFICIAL / cat_dir_name / "data"
        img_dir = RAW_OFFICIAL / cat_dir_name / "images"

        if not data_dir.exists():
            print(f"  SKIP: {data_dir} not found")
            continue

        json_files = sorted(data_dir.glob("*.json"))
        print(f"  {cat_name}: {len(json_files)} products")

        for jf in json_files:
            with open(jf) as f:
                raw = json.load(f)

            product = {
                "id": raw["product_id"],
                "name": raw["title"],
                "category": cat_name,
                "sub_category": raw.get("sub_category", ""),
                "price": raw.get("base_price", 0),
                "brand": raw.get("brand", ""),
                "image_urls": [],
                "product_url": "",
                "amazon_seller": None,
                "metadata": {},
            }

            # 图片路径
            img_filename = raw.get("image_path", "")
            if img_filename:
                product["image_urls"] = [str(RAW_OFFICIAL / img_filename)]

            # SKUs → metadata.skus
            if raw.get("skus"):
                product["metadata"]["skus"] = raw["skus"]

            products.append(product)

            # 拆分 rag_knowledge 为 chunks
            rag = raw.get("rag_knowledge", {})
            chunk_idx = 0

            if rag.get("marketing_description"):
                chunks.append({
                    "product_id": raw["product_id"],
                    "chunk_index": chunk_idx,
                    "chunk_text": rag["marketing_description"],
                    "chunk_type": "marketing_description",
                    "metadata": {"category": cat_name, "sub_category": raw.get("sub_category", "")},
                })
                chunk_idx += 1

            for faq in rag.get("official_faq", []):
                chunks.append({
                    "product_id": raw["product_id"],
                    "chunk_index": chunk_idx,
                    "chunk_text": f"Q: {faq['question']}\nA: {faq['answer']}",
                    "chunk_type": "faq",
                    "metadata": {"category": cat_name, "question": faq["question"]},
                })
                chunk_idx += 1

            for review in rag.get("user_reviews", []):
                chunks.append({
                    "product_id": raw["product_id"],
                    "chunk_index": chunk_idx,
                    "chunk_text": f"[{review.get('nickname','匿名')} 评分:{review.get('rating','?')}] {review['content']}",
                    "chunk_type": "review",
                    "metadata": {
                        "category": cat_name,
                        "rating": review.get("rating"),
                        "nickname": review.get("nickname", ""),
                    },
                })
                chunk_idx += 1

    print(f"  Total: {len(products)} products, {len(chunks)} chunks")
    return products, chunks


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    print("=== Processing official data ===")
    products, chunks = process_official_data()

    print(f"\n=== Output ===")
    print(f"Total: {len(products)} products, {len(chunks)} chunks")

    with open(OUT_DIR / "products.json", "w") as f:
        json.dump(products, f, ensure_ascii=False, indent=2)

    with open(OUT_DIR / "chunks.json", "w") as f:
        json.dump(chunks, f, ensure_ascii=False, indent=2)

    print(f"Written to {OUT_DIR / 'products.json'} and {OUT_DIR / 'chunks.json'}")

    cat_counts = {}
    for p in products:
        cat_counts[p["category"]] = cat_counts.get(p["category"], 0) + 1
    print("\nCategory distribution:")
    for cat, count in sorted(cat_counts.items()):
        print(f"  {cat}: {count}")

    chunk_type_counts = {}
    for c in chunks:
        t = c.get("chunk_type", "unknown")
        chunk_type_counts[t] = chunk_type_counts.get(t, 0) + 1
    print("\nChunk type distribution:")
    for t, n in sorted(chunk_type_counts.items()):
        print(f"  {t}: {n}")


if __name__ == "__main__":
    main()