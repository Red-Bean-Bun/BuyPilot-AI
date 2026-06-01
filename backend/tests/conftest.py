import asyncio
from contextlib import suppress
import json
import os
import re
import sys
from pathlib import Path

import httpx
import pytest

from src.api.app import app

# Windows: psycopg async requires SelectorEventLoop
if sys.platform == "win32":
    asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())

PROJECT_ROOT = Path(__file__).resolve().parents[2]
CONTRACTS_DIR = PROJECT_ROOT / "contracts"


@pytest.fixture
def test_client():
    return httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test")


@pytest.fixture(autouse=True)
def mock_external_ai(monkeypatch):
    monkeypatch.setenv("BAILIAN_BASE_URL", "https://example.test/v1")
    monkeypatch.setenv("BAILIAN_API_KEY", "test-key")
    monkeypatch.setenv("DOUBAO_BASE_URL", "https://example.test/v1")
    monkeypatch.setenv("DOUBAO_API_KEY", "test-key")
    monkeypatch.setenv("OBSERVABILITY_LOCAL_ENABLED", "0")

    import src.config.settings as settings_module

    settings_module._settings = None

    async def fake_chat_completion(profile, messages, json_object=False):
        del profile, json_object
        return json.dumps(_fake_chat_payload(messages), ensure_ascii=False)

    async def fake_chat_completion_stream(profile, messages):
        del profile
        payload = _fake_chat_payload(messages)
        chunks = payload.get("text_chunks") if isinstance(payload, dict) else None
        text = "\n".join(chunks) if isinstance(chunks, list) else "测试流式响应。"
        for index in range(0, len(text), 6):
            yield text[index : index + 6]

    async def fake_embedding_request(profile, texts):
        dimensions = profile.dimensions or 1024
        vectors = []
        for text in texts:
            values = [float((ord(ch) % 31) / 31) for ch in text[:16]]
            vectors.append((values + [0.0] * dimensions)[:dimensions])
        return vectors

    async def fake_rerank_request(profile, query, documents, top_n):
        del profile, query
        return list(range(min(top_n, len(documents))))

    async def fake_vl_embedding_request(profile, image_source):
        del profile, image_source
        return [0.01] * 1024

    from src.services import embedding, llm_gateway, reranker

    monkeypatch.setattr(llm_gateway, "_chat_completion", fake_chat_completion)
    monkeypatch.setattr(llm_gateway, "_chat_completion_stream", fake_chat_completion_stream)
    monkeypatch.setattr(embedding, "_embedding_request", fake_embedding_request)
    monkeypatch.setattr(embedding, "_vl_embedding_request", fake_vl_embedding_request)
    monkeypatch.setattr(reranker, "_rerank_request", fake_rerank_request)


@pytest.fixture(autouse=True)
async def reset_database_engine():
    loop_ticker = asyncio.create_task(_tick_event_loop())
    try:
        yield
    finally:
        from src.repos.database import dispose_async_engine
        from src.services.observability_llm import drain_observability_tasks

        await drain_observability_tasks()
        await dispose_async_engine()
        loop_ticker.cancel()
        with suppress(asyncio.CancelledError):
            await loop_ticker


async def _tick_event_loop() -> None:
    while True:
        await asyncio.sleep(0.001)


@pytest.fixture(autouse=True)
def _patch_vector_search_for_sqlite_tests(monkeypatch, mock_external_ai):
    """When mock_external_ai is active (BAILIAN_API_KEY=test-key), the database is
    SQLite and pgvector queries don't work. Patch list_vector_chunks_by_similarity
    to use list_embedded_chunks + deterministic cosine scoring instead.

    This is a test-only compatibility shim — production code has no SQLite fallback.
    Must run AFTER mock_external_ai to see BAILIAN_API_KEY=test-key."""
    del mock_external_ai  # just ensures ordering
    if os.getenv("BAILIAN_API_KEY") != "test-key":
        return

    import math

    from src.repos.documents import list_embedded_chunks

    async def _sqlite_vector_similarity(query_embedding, limit, filters=None):
        del filters
        chunks = await list_embedded_chunks()
        if not chunks or not query_embedding:
            return []
        hits = []
        for chunk in chunks:
            if not chunk.embedding or len(chunk.embedding) != len(query_embedding):
                continue
            dim = len(query_embedding)
            dot = sum(query_embedding[i] * chunk.embedding[i] for i in range(dim))
            q_norm = math.sqrt(sum(v * v for v in query_embedding))
            c_norm = math.sqrt(sum(v * v for v in chunk.embedding))
            if q_norm == 0 or c_norm == 0:
                continue
            similarity = dot / (q_norm * c_norm)
            if similarity > 0:
                hits.append(
                    type(
                        "VectorChunkHit",
                        (),
                        {"document": chunk, "distance": 1.0 - similarity},
                    )
                )
        hits.sort(key=lambda h: h.distance)
        return hits[:limit]

    from src.services import retriever

    monkeypatch.setattr(retriever, "list_vector_chunks_by_similarity", _sqlite_vector_similarity)


@pytest.fixture
async def seeded_products(monkeypatch, tmp_path):
    """Seed products with mock embeddings. Uses a temp SQLite DB so mock embedding
    vectors are compared against identically-generated chunk vectors, not against
    real semantic embeddings in the configured PostgreSQL database."""
    import os

    from src.config import settings as settings_module

    if os.getenv("BAILIAN_API_KEY") == "test-key":
        db_path = tmp_path / "test.db"
        monkeypatch.setenv("DATABASE_URL", f"sqlite:///{db_path}")
        settings_module._settings = None

    from src.services.product_ingest import seed_products

    await seed_products()
    yield
    settings_module._settings = None


@pytest.fixture
def sse_schema():
    path = CONTRACTS_DIR / "sse-events.schema.json"
    with open(path, encoding="utf-8") as f:
        return json.load(f)


@pytest.fixture
def golden_budget_beauty():
    path = CONTRACTS_DIR / "examples" / "demo_budget_beauty.sse"
    if not path.exists():
        pytest.skip("golden trace not found")
    with open(path, encoding="utf-8") as f:
        return f.read()


@pytest.fixture
def golden_clarification():
    path = CONTRACTS_DIR / "examples" / "demo_clarification.sse"
    if not path.exists():
        pytest.skip("golden trace not found")
    with open(path, encoding="utf-8") as f:
        return f.read()


@pytest.fixture
def golden_error():
    path = CONTRACTS_DIR / "examples" / "demo_error.sse"
    if not path.exists():
        pytest.skip("golden trace not found")
    with open(path, encoding="utf-8") as f:
        return f.read()


def parse_sse_stream(text: str) -> list[tuple[str, dict]]:
    events = []
    blocks = re.split(r"\n\n+", text.strip())
    for block in blocks:
        lines = block.strip().split("\n")
        event_type = None
        data = None
        for line in lines:
            if line.startswith("event: "):
                event_type = line[len("event: ") :].strip()
            elif line.startswith("data: "):
                data = json.loads(line[len("data: ") :].strip())
        if event_type and data:
            events.append((event_type, data))
    return events


async def collect_sse_stream(response: httpx.Response) -> list[tuple[str, dict]]:
    content = b""
    async for chunk in response.aiter_bytes():
        content += chunk
    text = content.decode("utf-8")
    return parse_sse_stream(text)


def _fake_chat_payload(messages):
    user_content = messages[-1].get("content", "") if messages else ""
    system_content = messages[0].get("content", "") if messages and messages[0].get("role") == "system" else ""
    if isinstance(user_content, list):
        return {"category_hint": "美妆护肤", "description": "测试图片", "visible_traits": ["护肤"]}
    if "faithfulness_score" in str(user_content):
        return {"faithfulness_score": 1.0, "unsupported_claims": 0, "reasoning": "test"}
    if "context_precision_score" in str(user_content):
        return {"context_precision_score": 1.0, "relevant_count": 1, "reasoning": "test"}
    if "context_recall_score" in str(user_content):
        return {"context_recall_score": 1.0, "covered_count": 1, "reasoning": "test"}
    if "correctness_score" in str(user_content):
        return {"correctness_score": 1.0, "error_count": 0, "reasoning": "test"}
    if "constraint_satisfaction_score" in str(user_content):
        return {"constraint_satisfaction_score": 1.0, "violations": 0, "reasoning": "test"}
    if "consistency_score" in str(user_content):
        return {"consistency_score": 1.0, "inconsistencies": 0, "reasoning": "test"}
    if "ranking_score" in str(user_content):
        return {"ranking_score": 1.0, "misplacements": 0, "reasoning": "test"}

    payload = _json_payload(user_content)
    if "valid_winner_ids" in payload:
        valid_ids = payload.get("valid_winner_ids") or []
        winner = valid_ids[0] if valid_ids else ""
        return {"winner_product_id": winner, "summary": f"优先选{winner}。", "why": ["综合匹配度最高"], "not_for": []}
    if "products" in payload and "criteria" in payload:
        return {"text_chunks": ["我会按你的约束筛选。", "下面是匹配度较高的商品。"]}
    if "intent" in payload or "existing" in payload:
        return _fake_criteria_payload(payload)
    if "message" in payload and "history" in payload:
        return _fake_intent_payload(payload, system_content)
    return {"text_chunks": ["测试响应。"]}


def _json_payload(value):
    if not isinstance(value, str):
        return {}
    try:
        parsed = json.loads(value)
        return parsed if isinstance(parsed, dict) else {}
    except json.JSONDecodeError:
        return {}


def _fake_intent_payload(payload, system_content):
    message = str(payload.get("message") or "")
    if _contains(message, "移出购物车", "从购物车删除", "删掉", "删除", "移除") and _contains(
        message, "购物车", "商品", "这个", "刚才"
    ):
        target = _product_id(message)
        return _intent("remove_from_cart", message, target_product_id=target)
    if _contains(message, "改成", "改为", "设置为", "调整为", "修改数量") and _contains(message, "购物车", "件", "个"):
        target = _product_id(message)
        return _intent(
            "update_cart_quantity",
            message,
            constraints={"quantity": _quantity(message)},
            target_product_id=target,
        )
    if _contains(message, "加购", "加入购物车", "买这个"):
        target = _product_id(message)
        quantity = _quantity(message)
        constraints = {"quantity": quantity} if quantity is not None else None
        return _intent("add_to_cart", message, constraints=constraints, target_product_id=target)
    if _contains(message, "购物车", "看看车", "查看车"):
        return _intent("view_cart", message)
    if _contains(message, "不喜欢", "不要这个", "不要刚才", "换一个", "排除", "去掉"):
        return _intent("feedback", message, constraints={"feedback_text": message})
    if _contains(message, "继续", "确认", "没问题", "可以", "开始推荐", "收敛"):
        return _intent("continue", message)

    if _contains(message, "随便看看"):
        return _intent("recommend", message)

    category = _category(message) or (_category(system_content) if _is_followup(message) else None)
    constraints = _constraints(message)
    if payload.get("image_url"):
        constraints["image_url"] = payload["image_url"]
    return _intent("recommend", message, category=category, constraints=constraints)


def _fake_criteria_payload(payload):
    message = str(payload.get("message") or "")
    intent = payload.get("intent") if isinstance(payload.get("intent"), dict) else {}
    existing = payload.get("existing") if isinstance(payload.get("existing"), dict) else {}
    existing_constraints = existing.get("constraints") if isinstance(existing.get("constraints"), dict) else {}
    constraints = {**existing_constraints, **intent.get("extracted_constraints", {}), **_constraints(message)}
    feedback = payload.get("feedback") if isinstance(payload.get("feedback"), dict) else {}
    avoid_traits = feedback.get("avoid_traits") if isinstance(feedback.get("avoid_traits"), list) else []
    if avoid_traits:
        constraints["ingredient_avoid"] = list(dict.fromkeys([*constraints.get("ingredient_avoid", []), *avoid_traits]))
    category = intent.get("category") or existing.get("category") or _category(message) or "美妆护肤"
    if "use_scenario" not in constraints or constraints.get("use_scenario") is None:
        constraints["use_scenario"] = _scenario(message, category)
    if "product_type" not in constraints or constraints.get("product_type") is None:
        constraints["product_type"] = _product_type(message, category)
    chips = _chips(category, constraints)
    return {
        "criteria_id": existing.get("criteria_id") or "c_auto_001",
        "category": category,
        "summary": "，".join(chips),
        "chips": chips,
        "constraints": constraints,
    }


def _intent(intent, message, category=None, constraints=None, target_product_id=None):
    return {
        "intent": intent,
        "confidence": 0.95,
        "category": category,
        "extracted_constraints": constraints or {},
        "soft_preferences": [message] if message else [],
        "target_product_id": target_product_id,
    }


def _constraints(message):
    constraints = {}
    budget = _budget(message)
    if budget is not None:
        constraints["budget_max"] = budget
    if _contains(message, "油皮", "油性"):
        constraints["skin_type"] = "油性"
    if _contains(message, "中性肌肤", "中性肌", "中性肤质", "中性"):
        constraints["skin_type"] = "中性"
    if _contains(message, "干性肌肤", "干性肌", "干性肤质", "干皮"):
        constraints["skin_type"] = "干性"
    if _contains(message, "敏感肌", "敏感"):
        constraints["skin_type"] = "敏感"
    avoid = []
    for token in ("酒精", "香精", "耐克", "Nike"):
        if token in message and _contains(message, f"不要{token}", f"不要含{token}", f"不含{token}", f"避开{token}"):
            avoid.append(token)
    if avoid:
        constraints["ingredient_avoid"] = avoid
    if any(token in avoid for token in ("耐克", "Nike")):
        constraints["brand_avoid"] = [token for token in avoid if token in {"耐克", "Nike"}]
    dietary = [token for token in ("无糖", "低糖") if token in message]
    if dietary:
        constraints["dietary"] = dietary
    storage = re.search(r"(\d+)\s*(?:G|GB|g|gb)", message)
    if storage:
        constraints["storage"] = f"{storage.group(1)}G"
    scenario = _scenario(message, _category(message) or "")
    if scenario:
        constraints["use_scenario"] = scenario
    product_type = _product_type(message, _category(message) or "")
    if product_type:
        constraints["product_type"] = product_type
    if _contains(message, "跑步"):
        constraints["sport_type"] = "跑步"
    return constraints


def _budget(message):
    match = re.search(r"预算\s*(?:降到|控制在|不超过|约|大概)?\s*(\d+(?:\.\d+)?)", message)
    if match:
        return float(match.group(1))
    match = re.search(r"(\d+(?:\.\d+)?)\s*元\s*(?:以内|以下|内)?", message)
    if match:
        return float(match.group(1))
    match = re.search(r"(\d+(?:\.\d+)?)\s*(?:以内|以下|内)", message)
    return float(match.group(1)) if match else None


def _category(text):
    if _contains(text, "洗面奶", "洁面", "防晒", "护肤", "油皮", "敏感肌"):
        return "美妆护肤"
    if _contains(text, "手机", "耳机", "电脑", "笔记本", "CD机", "CD 机", "cd机", "cd 机"):
        return "数码电子"
    if _contains(text, "跑鞋", "跑步", "训练", "瑜伽", "运动"):
        return "服饰运动"
    if _contains(
        text,
        "零食",
        "咖啡",
        "麦片",
        "孩子",
        "饮料",
        "茶饮料",
        "无糖茶",
        "日常喝",
        "气泡水",
        "酸奶",
        "酱油",
        "调味品",
    ):
        return "食品生活"
    return None


def _scenario(message, category):
    if _contains(message, "日常", "护肤", "洁面"):
        return "日常护肤" if category == "美妆护肤" else "日常使用"
    if _contains(message, "防晒", "户外"):
        return "户外防晒"
    if _contains(message, "训练", "跑步"):
        return "日常训练"
    if _contains(message, "孩子"):
        return "儿童零食"
    if _contains(message, "早餐"):
        return "早餐"
    if _contains(message, "游戏"):
        return "游戏"
    return None


def _product_type(message, category):
    if _contains(message, "洗面奶", "洁面"):
        return "洁面"
    if "防晒" in message:
        return "防晒"
    if _contains(message, "蓝牙耳机", "无线耳机", "耳机"):
        return "耳机"
    if _contains(message, "笔记本", "电脑"):
        return "笔记本电脑"
    if "平板" in message:
        return "平板电脑"
    if _contains(message, "跑鞋", "运动鞋"):
        return "跑步鞋"
    if "手机" in message:
        return "手机"
    if _contains(message, "CD机", "CD 机", "cd机", "cd 机"):
        return "CD机"
    if category == "食品生活" and _contains(message, "茶饮料", "茶饮", "无糖茶", "乌龙茶"):
        return "茶饮"
    if category == "食品生活" and _contains(message, "气泡水", "汽水", "可乐"):
        return "碳酸饮料"
    if category == "食品生活" and "咖啡" in message:
        return "咖啡"
    if category == "食品生活" and _contains(message, "方便面", "泡面", "速食"):
        return "方便食品"
    if category == "食品生活" and "零食" in message:
        return "零食"
    if category == "食品生活" and _contains(message, "酱油", "生抽", "老抽", "调味品"):
        return "调味品"
    return None


def _chips(category, constraints):
    chips = [category]
    if constraints.get("skin_type"):
        chips.append(f"{constraints['skin_type']}肌肤")
    if constraints.get("budget_max") is not None:
        chips.append(f"{constraints['budget_max']:g}元内")
    if constraints.get("use_scenario"):
        chips.append(constraints["use_scenario"])
    for item in constraints.get("ingredient_avoid") or []:
        chips.append(f"不要{item}")
    if constraints.get("product_type"):
        chips.append(constraints["product_type"])
    return chips


def _product_id(message):
    match = re.search(r"p_[a-z]+_\d+", message)
    return match.group(0) if match else None


def _quantity(message):
    match = re.search(r"(?:改成|改为|设置为|调整为|变成)\s*(\d+)", message)
    if match:
        return int(match.group(1))
    match = re.search(r"(\d+)\s*(?:件|个|份)", message)
    return int(match.group(1)) if match else None


def _contains(text, *tokens):
    return any(token in text for token in tokens)


def _is_followup(message):
    return _contains(message, "预算", "再", "这个", "换", "降到", "以内")
