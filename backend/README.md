# BuyPilot-AI Backend — 当前地图

> 最后验证：2026-05-22 · pytest 62 passed · smoke_live_rag 绿灯
> 验证命令见下方 **Quick Start**。

---

## 1. Quick Start

```bash
cd /mnt/disk1/LZJ/project/BuyPilot-AI/backend

# 1. 安装依赖
uv sync --extra dev

# 2. 单元测试（不依赖外网，conftest mock 了所有 LLM/embedding/rerank 调用）
.venv/bin/python -m pytest tests -q

# 3. 数据入库 + embedding 索引重建（需要 .env 有百炼 key）
.venv/bin/python -m src.scripts.reindex_embeddings

# 4. 严格 smoke（需要 .env + 网络；embedding 维度不对会 exit(1))
.venv/bin/python -m src.scripts.smoke_live_rag

# 5. 启动服务
.venv/bin/python -m uvicorn src.api.app:app --reload --port 8000
```

smoke 绿灯输出示例：
```
{"check":"embedding_index","ok":true,"embedded_chunks":1092,"embedding_dimensions":1024}
{"check":"embedding","ok":true,"dimensions":1024}
{"check":"chat_stream","events":["thinking","thinking","criteria_card","text_delta","text_delta","text_delta","thinking","product_card","product_card","product_card","product_card","final_decision","done"],"product_count":4,"has_criteria":true,"has_decision":true}
```

如果 smoke 失败：
- embedding_index 不是 1024 → 先跑 `reindex_embeddings`
- embedding 不是 1024 → 检查 `.env` 百炼 key + 代理 + `httpx[socks]`
- SOCKS 报错 → 确认 `.venv` 有 `socksio`，跑 `uv sync --extra dev`

---

## 2. Architecture

### 分层规则

```
API → Runtime → Service → Repo → Config/Types
```

依赖只能自上而下流动。违反即架构错误。

### 当前实际依赖图

```
┌─────────────────────────────────────────────────────────────┐
│  api/                                                        │
│    app.py ──────── 挂载所有 router                            │
│    chat.py ─────── runtime.pipeline + repos.feedbacks* +     │
│                   repos.cart_items*                           │
│    cancel.py ───── stub，不调任何人                            │
│    upload.py ───── mock，不调任何人                            │
│    feedback.py ─── repos.feedbacks*                           │
│    cart.py ─────── repos.cart_items*                          │
│                                                              │
│  *标记 = 架构违规：API 直接调 Repo，绕过 Runtime/Service     │
└─────────────────────────────────────────────────────────────┘
          ↓ 正常方向
┌─────────────────────────────────────────────────────────────┐
│  runtime/                                                    │
│    pipeline.py ─── stages.* + services.evidence + repos.*    │
│    mock_pipeline.py ─── 独立，不依赖真实服务                  │
│    stages/                                                   │
│      intent.py ───── services.llm_client.analyze_intent      │
│      criteria.py ─── services.llm_client + repos.conv +      │
│                       repos.feedbacks                         │
│      recommendation.py ─ services.retriever + llm_client     │
│      decision.py ─── services.llm_client.generate_decision   │
│      slot_checker.py ─ 纯逻辑，不依赖任何 service            │
│      multimodal.py ── services.llm_client.analyze_image      │
└─────────────────────────────────────────────────────────────┘
          ↓ 正常方向
┌─────────────────────────────────────────────────────────────┐
│  services/                                                   │
│    llm_client.py ─── config.settings + httpx + types.*       │
│    retriever.py ─── services.embedding + services.reranker + │
│                     repos.documents + repos.products         │
│    embedding.py ─── config.settings + httpx                  │
│    reranker.py ──── config.settings + httpx                  │
│    evidence.py ──── repos.documents + repos.products +       │
│                     retriever(ContextVar)                    │
│    chunking.py ──── 纯函数，无外部依赖                        │
└─────────────────────────────────────────────────────────────┘
          ↓ 正常方向
┌─────────────────────────────────────────────────────────────┐
│  repos/                                                      │
│    models.py ────── SQLModel 表定义                           │
│    database.py ───── SQLAlchemy engine/session               │
│    products.py ───── 从 raw JSON dataset 加载                │
│    documents.py ───── DB chunk 查询                           │
│    conversations.py ─ 内存 dict，重启丢失                     │
│    feedbacks.py ──── 内存 list，重启丢失                      │
│    cart_items.py ─── 内存 dict，重启丢失                      │
│    traces.py ─────── DB 写入（retrieval_traces/evidence_links）│
│    ingest.py ─────── services.chunking* + services.embedding*│
│                                                              │
│  *标记 = 架构违规：Repo 调 Service，方向反了                 │
└─────────────────────────────────────────────────────────────┘
          ↓ 正常方向
┌─────────────────────────────────────────────────────────────┐
│  config/                                                     │
│    settings.py ──── .env 加载 + TASK_MODEL_MAP               │
│    llm_profiles.yaml ── LLM endpoint 配置                    │
│                                                              │
│  types/                                                      │
│    schemas.py ────── HTTP 请求/响应 Pydantic model            │
│    sse_events.py ──── SSE 事件类型全集（8+1）                 │
│    pipeline_state.py ── TypedDict，未使用                     │
│    slot_defs.py ───── 槽位定义                                │
└─────────────────────────────────────────────────────────────┘
```

### 架构违规清单

| 位置 | 违规 | 影响 | 修复方向 |
|------|------|------|---------|
| `api/chat.py:stream_chat` | 直接调 `repos.feedbacks` | 绕过 Service 层业务逻辑 | feedback 操作应走 Runtime |
| `api/chat.py:stream_chat` | 直接调 `repos.cart_items` | 同上 | cart 操作应走 Runtime |
| `api/feedback.py:submit_feedback` | 直接调 `repos.feedbacks.add_feedback` | 同上 | 应走 Runtime |
| `api/cart.py:read_cart` | 直接调 `repos.cart_items.get_cart` | 同上 | 应走 Runtime |
| `repos/ingest.py` | 调 `services.chunking` + `services.embedding` | Repo → Service 方向反了 | ingest 应独立为 CLI tool，不属于 Repo 层 |

> 当前不修这些违规，优先功能完整性。但新增代码不能继续违规。

---

## 3. Data Flow

### 推荐主链路（recommend intent）

```
用户输入 ChatStreamRequest{message, session_id, history?, criteria_patch?}
  │
  ▼
┌─ Intent（llm_client.analyze_intent）─────────────────────────┐
│  输入：message + history                                       │
│  输出：IntentResult{intent, category, extracted_constraints}  │
│  真实：LLM 调用 → httpx → 百炼/Doubao                          │
│  失败：regex heuristic fallback                               │
└───────────────────────────────────────────────────────────────┘
  │ intent="recommend" 时继续
  ▼
┌─ Slot Checker（slot_checker.py）─────────────────────────────┐
│  输入：IntentResult + message                                  │
│  输出：missing_slots[] 或 []                                   │
│  纯逻辑：关键词匹配，无 LLM                                    │
│  有缺失 → 发 ClarificationEvent，turn 结束                    │
│  无缺失 → 继续                                                 │
└───────────────────────────────────────────────────────────────┘
  │
  ▼
┌─ Criteria（stages/criteria.py → llm_client.generate_criteria）──┐
│  输入：message + intent + feedback(extract_feedback_from_session) │
│  输出：CriteriaPayload{category, summary, chips, Constraints}     │
│  真实：LLM 调用                                                  │
│  失败：heuristic fallback + feedback 手动注入 ingredient_avoid   │
│  数据所有权：criteria 一旦生成，本 turn 不再修改                  │
│    └─ criteria_patch 路径：apply_criteria_patch 修改现有 criteria │
│    └─ feedback 路径：extract_feedback → 注入 LLM prompt            │
└──────────────────────────────────────────────────────────────────┘
  │
  ▼
┌─ Retriever（services/retriever.py）─────────────────────────────┐
│  输入：CriteriaPayload                                          │
│                                                                  │
│  1. _query_text(criteria) → 拼接查询字符串                       │
│     category + summary + skin_type + ingredient_avoid + budget    │
│                                                                  │
│  2. embed_text(query) → query_embedding 1024维                    │
│     真实：百炼 /embeddings                                        │
│     失败：deterministic hash 向量                                  │
│                                                                  │
│  3. _vector_recall_from_db → cosine similarity against            │
│     ProductChunk.embedding (Python 内存，不是 pgvector)            │
│     [Gap: PRD 要求 pgvector HNSW]                                │
│                                                                  │
│  4. _hard_filter → category 精确匹配 + budget_max +               │
│     ingredient_avoid 排除                                         │
│     真实：Python 列表过滤                                          │
│     [Gap: PRD 要求 SQL 硬过滤]                                    │
│                                                                  │
│  5. _rerank_chunk_hits → chunk text rerank via 百炼               │
│     真实：/reranks API                                            │
│     失败：deterministic 规则排序                                   │
│                                                                  │
│  6. 去重聚合 product → winning chunk evidence                     │
│     evidence 存入 ContextVar（_RETRIEVAL_EVIDENCE_BY_PRODUCT）    │
│                                                                  │
│  输出：[ProductPayload + EvidencePayload]                         │
│  数据所有权：retriever 产出 product 列表，后续只读                 │
└──────────────────────────────────────────────────────────────────┘
  │
  ▼
┌─ Recommendation（stages/recommendation.py → llm_client）─────────┐
│  输入：CriteriaPayload + [ProductPayload]                         │
│  输出：RecommendationResult{text_chunks, products}                │
│  真实：LLM 生成解释文本                                          │
│  失败：generic 模板文本                                           │
│  注意：products 顺序不被 LLM 重排，由 retriever 决定              │
└──────────────────────────────────────────────────────────────────┘
  │
  ▼
┌─ Decision（stages/decision.py → llm_client.generate_decision）───┐
│  输入：CriteriaPayload + [ProductPayload]                         │
│  输出：DecisionResult{winner_product_id, summary, why, not_for}   │
│  真实：Qwen-Plus LLM 生成决策 + 幻觉验证                          │
│  失败：fallback → products[0] + 规则 why                          │
│  幻觉防护：prompt 约束 winner_product_id 可选范围 + 代码验证     │
│  alternatives 由 pipeline 构造（排除 winner，取前2个）            │
└──────────────────────────────────────────────────────────────────┘
  │
  ▼
┌─ SSE 输出 + 持久化 ──────────────────────────────────────────────┐
│  每个 SSE 事件带 seq + session_id + turn_id                      │
│  事件序列：thinking → criteria_card → text_delta →               │
│            product_card(N) → final_decision → done               │
│                                                                    │
│  持久化（同步写入，非后台）：                                     │
│    save_turn → repos.conversations（内存 dict）                    │
│    write_retrieval_trace → repos.traces（DB）                     │
│    write_evidence_links → repos.traces（DB）                       │
│  [Gap: PRD 要求后台化，不阻塞 done]                               │
└──────────────────────────────────────────────────────────────────┘
```

### 数据实体所有权

| 数据实体 | 创建者 | 修改者 | 消费者 | 存储位置 |
|----------|--------|--------|--------|---------|
| IntentResult | llm_client.analyze_intent | 无 | slot_checker, criteria | 不持久化 |
| CriteriaPayload | llm_client.generate_criteria | apply_criteria_patch, feedback 注入 | retriever, recommendation, decision | conversations 内存 dict |
| ProductPayload[] | retriever.retrieve | 无（顺序由 rerank 决定） | recommendation, decision, SSE | 不持久化 |
| EvidencePayload[] | retriever._bind_evidence → ContextVar | 无 | ProductCardEvent | traces DB |
| RecommendationResult | llm_client.generate_recommendation | 无 | TextDeltaEvent | 不持久化 |
| DecisionResult | llm_client.generate_decision（真实 LLM + fallback） | 无 | FinalDecisionEvent | 不持久化 |
| Conversation state | conversations.save_turn | 下轮 get_last_criteria | criteria stage | 内存 dict，重启丢失 |
| Feedback | feedbacks.add_feedback | 累积 | extract_feedback_from_session → criteria | 内存 list，重启丢失 |
| Cart items | cart_items.add_to_cart | 累积 | cart API | 内存 dict，重启丢失 |

---

## 4. State Machine

### SSE 事件类型全集

来自 `types/sse_events.py`，共 **8+1** 类型（铁律：禁止新增 type）：

| type | Pydantic Model | 核心字段 | 触发条件 |
|------|---------------|---------|---------|
| `thinking` | ThinkingEvent | stage, message | 每个处理阶段前 |
| `clarification` | ClarificationEvent | question, required_slots, suggested_options | 缺少必需槽位 |
| `criteria_card` | CriteriaCardEvent | criteria, editable, quick_actions | criteria 生成完成 |
| `text_delta` | TextDeltaEvent | message_id, delta, done | recommendation 文本流式输出 |
| `product_card` | ProductCardEvent | deck_id, rank, product, reason, evidence, actions | 每个推荐商品 |
| `cart_action` | CartActionEvent | action, product_id, quantity, status | 加购/查看购物车 |
| `final_decision` | FinalDecisionEvent | winner_product_id, summary, why, not_for, alternatives | 决策生成完成 |
| `done` | DoneEvent | （继承 base 字段） | turn 结束 |
| `error` | ErrorEvent | code, message, retryable | 异常 |

### Intent 分支流转图

```
用户输入
  │
  ▼
Thinking("understanding")
  │
  ▼ run_intent() → IntentResult
  │
  ├── intent="recommend"
  │     │
  │     ▼ check_required_slots()
  │     │
  │     ├── 有缺失槽位 ──→ ClarificationEvent → DoneEvent
  │     │                    （missing_slots: category 或 scenario）
  │     │
  │     └── 无缺失 ──→ Thinking("criteria")
  │                       │
  │                       ▼ run_criteria()
  │                       │   CriteriaCardEvent + QuickActions
  │                       │
  │                       ▼ Thinking("searching")
  │                       │
  │                       ▼ run_recommendation()
  │                       │   TextDeltaEvent(s) → ProductCardEvent(s)
  │                       │
  │                       ▼ run_decision()
  │                       │   FinalDecisionEvent
  │                       │
  │                       ▼ save_turn + traces
  │                       │   DoneEvent
  │
  ├── intent="add_to_cart"
  │     │
  │     ▼ Thinking("generating")
  │     │   CartActionEvent(action="add") → DoneEvent
  │
  ├── intent="view_cart"
  │     │
  │     ▼ Thinking("generating")
  │     │   CartActionEvent(action="view") → DoneEvent
  │
  ├── intent="feedback"
  │     │
  │     ▼ add_feedback() → 继续推荐流程
  │     │   （feedback 写入内存，下一轮 criteria 注入）
  │     │   [Gap: feedback 未影响 retrieval 硬过滤]
  │
  └── intent="clarify"
        │ 同 recommend 分支，但会触发 clarification
```

### 槽位检查逻辑

来自 `runtime/stages/slot_checker.py`：

- 缺 `category` → "你想买哪一类商品？" 选项：美妆护肤/数码电子/服饰运动/食品生活
- 缺 `scenario` → "主要使用场景是什么？" 选项：日常使用/送礼/通勤/户外
- `_has_scenario()` 检查关键词：日常/使用/送/通勤/户外/护肤/洁面/防晒/跑步/训练/孩子/预算

### Quick Actions 映射

来自 `stages/criteria.py:criteria_quick_actions()`：

| QuickAction | 含义 | 后端处理 |
|-------------|------|---------|
| criteria_patch | 用户修改约束 | apply_criteria_patch → 新 criteria → 重新检索 |
| feedback | 用户反馈偏好 | add_feedback → 下轮 criteria 注入 |
| open_evidence | 查看证据 | 前端展示 evidence snippet |
| add_to_cart | 加入购物车 | CartActionEvent |

---

## 5. Module Status

### API 层 (`src/api/`)

| 端点 | 方法 | Handler | 文件:函数 | 状态 | 备注 |
|------|------|---------|-----------|------|------|
| `/health` | GET | `health()` | `app.py:health` | **真实** | 返回 settings 基本信息 |
| `/chat/stream` | POST | `stream_chat()` | `chat.py:stream_chat` | **真实** | 调 runtime.pipeline.chat_stream |
| `/chat/upload/image` | POST | `upload_image()` | `chat.py:upload_image` | **MOCK** | 返回固定 JSON，未接 multipart/Qwen-VL |
| `/chat/feedback` | POST | `submit_feedback()` | `chat.py:submit_feedback` | **半真实** | 写入内存 feedbacks，未注入 retrieval |
| `/chat/cart/{session_id}` | GET | `read_cart()` | `chat.py:read_cart` | **半真实** | 内存 dict，重启丢失 |
| `/chat/cancel` | POST | `cancel_chat()` | `cancel.py:cancel_chat` | **STUB** | 返回固定响应，不取消任何任务 |

> 注意：`upload.py`, `feedback.py`, `cart.py` 有独立文件但逻辑已合并进 `chat.py`。

### Runtime 层 (`src/runtime/`)

| 函数 | 文件:行 | 状态 | 备注 |
|------|---------|------|------|
| `chat_stream()` | `pipeline.py` | **真实** | 完整推荐/加购/购物车/反馈流程 |
| `mock_pipeline()` | `mock_pipeline.py` | **MOCK** | SSE golden reference |
| `run_intent()` | `stages/intent.py` | **真实** | 包裹 llm_client.analyze_intent |
| `run_criteria()` | `stages/criteria.py` | **真实** | 包裹 llm_client + feedback + patch |
| `apply_criteria_patch()` | `stages/criteria.py` | **真实** | 纯逻辑修改 criteria fields |
| `check_required_slots()` | `stages/slot_checker.py` | **真实** | 纯关键词匹配，无 LLM |
| `run_recommendation()` | `stages/recommendation.py` | **真实** | retrieve + LLM 生成 |
| `run_decision()` | `stages/decision.py` | **真实** | 包裹 llm_client.generate_decision |
| `run_multimodal()` | `stages/multimodal.py` | **真实** | 包裹 llm_client.analyze_image |

### Services 层 (`src/services/`)

| 函数 | 文件 | 状态 | 真实路径 | 失败 fallback |
|------|------|------|---------|---------------|
| `analyze_intent()` | `llm_client.py` | **真实+fallback** | httpx → 百炼/Doubao chat | regex heuristic |
| `generate_criteria()` | `llm_client.py` | **真实+fallback** | httpx → Qwen-Plus chat | heuristic + feedback 注入 |
| `generate_recommendation()` | `llm_client.py` | **真实+fallback** | httpx → Qwen-Plus chat | generic 模板文本 |
| `generate_decision()` | `llm_client.py` | **真实+fallback** | httpx → Qwen-Plus chat（prompt 约束 + 幻觉验证） | products[0] + 规则 why |
| `analyze_image()` | `llm_client.py` | **真实+fallback** | httpx → Qwen-VL-Plus | mock 分析结果 |
| `embed_text()` / `embed_texts()` | `embedding.py` | **真实+fallback** | httpx → 百炼 /embeddings | deterministic hash 向量 |
| `rerank_texts()` | `reranker.py` | **真实+fallback** | httpx → 百炼 /reranks | deterministic 规则排序 |
| `retrieve()` | `retriever.py` | **真实** | DB cosine + hard filter + rerank | 无（子步骤各自 fallback） |
| `get_evidence()` | `evidence.py` | **真实** | ContextVar cache → fallback raw snippet | raw dataset snippet |

### Repo 层 (`src/repos/`)

| 函数 | 文件 | 状态 | 存储方式 | 备注 |
|------|------|------|---------|------|
| `load_raw_products()` | `products.py` | **真实** | JSON dataset 文件 | 有缓存 |
| `list_products()` | `products.py` | **真实** | 从缓存读取 | — |
| `list_embedded_chunks()` | `documents.py` | **真实** | SQLite DB 查询 | SQLModel |
| `evidence_for_chunk()` | `documents.py` | **真实** | DB chunk → EvidencePayload | — |
| `save_turn()` | `conversations.py` | **内存 STUB** | dict[str, CriteriaPayload] | 重启丢失 |
| `get_last_criteria()` | `conversations.py` | **内存 STUB** | dict 查找 | 重启丢失 |
| `add_feedback()` | `feedbacks.py` | **内存 STUB** | list append | 重启丢失 |
| `extract_feedback_from_session()` | `feedbacks.py` | **真实逻辑** | list → dict{avoid,prefer} | 注入 criteria，不注入 retrieval |
| `add_to_cart()` | `cart_items.py` | **内存 STUB** | dict append | 重启丢失 |
| `get_cart()` | `cart_items.py` | **内存 STUB** | dict 查找 | 重启丢失 |
| `write_retrieval_trace()` | `traces.py` | **真实** | SQLite DB 写入 | 同步，非后台 |
| `write_evidence_links()` | `traces.py` | **真实** | SQLite DB 写入 | 同步，非后台 |
| `seed_products()` | `ingest.py` | **真实** | raw dataset → DB | 调 services（违规） |
| `reindex_chunk_embeddings()` | `ingest.py` | **真实** | DB embedding 更新 | 调 services（违规） |

### Config/Types 层

| 文件 | 内容 | 状态 |
|------|------|------|
| `settings.py` | .env 加载 + TASK_MODEL_MAP | **真实** |
| `llm_profiles.yaml` | 百炼/Doubao endpoint 配置 | **真实** |
| `schemas.py` | HTTP 请求/响应 Pydantic model | **真实** |
| `sse_events.py` | SSE 事件类型全集 | **真实** |
| `pipeline_state.py` | TypedDict | **未使用** — pipeline 用显式参数传递 |
| `slot_defs.py` | 槽位定义 | **真实** |

---

## 6. Gaps & Next Steps

### 当前差距（按对 Demo 的影响排序）

| # | 差距 | 影响范围 | 优先级 | 源文件 |
|---|------|---------|--------|--------|
| 1 | **图片 URL 不可访问** — ProductPayload.image_url 多为 raw 相对路径，Android 无法加载 | 前端体验断点 | P0-equivalent | `repos/products.py` → `load_raw_products` |
| 2 | **`/upload/image` 是 mock** — 未接 multipart 上传、本地存储、Qwen-VL 分析 | Demo 路径 2（拍照找货）无法演示 | P1 | `api/chat.py:upload_image` |
| 3 | ~~**generate_decision 是 fallback**~~ ← **已修复** — 接真实 LLM（Qwen-Plus primary / Doubao fallback）+ 幻觉验证 | final_decision 现在有质量 | Done | `services/llm_client.py:generate_decision` |
| 4 | **feedback 未注入 retrieval** — extract_feedback 只注入 criteria prompt，不排除 avoid_products | 反选排除 Demo 路径 3 不完整 | P1 | `services/retriever.py:_hard_filter` |
| 5 | **cancel 是 stub** — 无任务注册表/取消令牌，不能中断 LLM/RAG | 长请求无法停止 | P1 | `api/cancel.py:cancel_chat` |
| 6 | **conversations/feedbacks/cart 是内存** — 重启丢失 | 多轮上下文和加购状态不稳定 | P1 | `repos/conversations.py`, `feedbacks.py`, `cart_items.py` |
| 7 | **向量检索不是 pgvector** — Python 内存 cosine，不是 HNSW | 可 Demo 但不是目标架构 | P1→P2 | `services/retriever.py:_vector_recall_from_db` |
| 8 | **硬过滤不是 SQL** — Python 列表过滤，不是 DB query | 性能和可扩展性 | P1→P2 | `services/retriever.py:_hard_filter` |
| 9 | **trace/evidence 同步写入** — 阻塞 done 之前收尾 | 慢 DB 会拖慢 | P2 | `repos/traces.py` |
| 10 | **PipelineState 未使用** — TypedDict 定义了但 pipeline 用显式参数 | 代码整洁度 | P2 | `types/pipeline_state.py` |
| 11 | **API 直接调 Repo** — 架构违规 | 不影响功能但架构不干净 | P2 | `api/chat.py`, `api/feedback.py`, `api/cart.py` |

### 接手优先级建议

```
1. 图片静态服务 ── 前端立刻需要，否则商品卡片无图
2. 真实 /upload/image ── Demo 路径 2 依赖
3. ~~generate_decision 接真实 LLM~~ ← 已完成
4. feedback 注入 retrieval ── Demo 路径 3 反选排除
5. cancel 真取消 ── 用户体验
6. 切 PostgreSQL ── 演示前不建议引入部署复杂度
```

### 绝对不做

- 把 `.env` key 写进文档/日志/commit
- 恢复硬编码 `DEMO_PRODUCTS`
- 把 `buypilot-dev.db` 当源代码资产
- 让 pytest 依赖真实网络
- 把 embedding batch size 改回 32（百炼限制 10）
- 在 runtime/api 里直接调 LLM SDK
- 破坏 SSE 事件类型和字段名