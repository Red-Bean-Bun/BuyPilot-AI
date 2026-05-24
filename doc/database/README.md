# BuyPilot 后端数据表总览

本文档对应当前开发库 `backend/buypilot-dev.db`，用于人工 review 数据表含义、字段作用和设计原因。当前库是 SQLite 开发库；生产目标仍是 `PostgreSQL + pgvector`。因此文档里同时标注了 SQLite 的实际列类型，以及设计上在 PostgreSQL 中对应的 JSONB/vector 意图。

## 当前表清单

| 表名 | 行数 | 主要服务对象 | 文档 |
| --- | ---: | --- | --- |
| `products` | 100 | 商品事实源、硬过滤、商品卡片 | [products.md](tables/products.md) |
| `product_chunks` | 1292 | RAG 召回、证据片段、风险提示 | [product_chunks.md](tables/product_chunks.md) |
| `conversations` | 769 | 多轮上下文、推荐历史、trace 关联 | [conversations.md](tables/conversations.md) |
| `feedbacks` | 97 | 同会话反馈闭环、反选排除 | [feedbacks.md](tables/feedbacks.md) |
| `cart_items` | 5 | 对话式加购、购物车查看 | [cart_items.md](tables/cart_items.md) |
| `active_chat_turns` | 0 | 流式对话进行中状态、取消能力 | [active_chat_turns.md](tables/active_chat_turns.md) |
| `chat_turn_cancellations` | 0 | 跨请求/跨进程取消请求 | [chat_turn_cancellations.md](tables/chat_turn_cancellations.md) |
| `eval_runs` | 0 | 评测运行结果、版本对比 | [eval_runs.md](tables/eval_runs.md) |
| `eval_samples` | 0 | 评测样本集、指标 ground truth | [eval_samples.md](tables/eval_samples.md) |
| `retrieval_traces` | 818 | 检索链路解释、fallback/耗时观测 | [retrieval_traces.md](tables/retrieval_traces.md) |
| `evidence_links` | 3408 | 推荐证据绑定、证据覆盖率 | [evidence_links.md](tables/evidence_links.md) |
| `api_request_logs` | 171 | HTTP 请求观测、错误定位 | [api_request_logs.md](tables/api_request_logs.md) |
| `audit_events` | 683 | 业务副作用审计、调试回放 | [audit_events.md](tables/audit_events.md) |

行数来自 2026-05-24 对 `backend/buypilot-dev.db` 的本地检查，只用于说明当前开发库状态，运行 demo 或测试后会继续变化。

## 分层设计

| 分层 | 表 | 设计目的 |
| --- | --- | --- |
| 商品事实层 | `products`, `product_chunks` | 把官方 100 条商品数据拆成结构化商品表和可检索知识 chunk，防止 LLM 编造商品事实。 |
| 会话状态层 | `conversations`, `feedbacks`, `cart_items` | 支撑多轮上下文、同会话反馈修正和对话式加购，不引入复杂登录系统。 |
| 流式控制层 | `active_chat_turns`, `chat_turn_cancellations` | 支撑 `/chat/stream` 的 best-effort cancel，避免只靠进程内内存。 |
| 评测闭环层 | `eval_samples`, `eval_runs` | 固化样本、版本、指标和逐样本明细，让效果迭代可对比。 |
| 可解释与观测层 | `retrieval_traces`, `evidence_links`, `api_request_logs`, `audit_events` | 给评委和开发者查看“为什么推荐这些商品”“请求哪里慢/失败”“哪些副作用发生过”。 |

## 关键关系

```text
products.id
  -> product_chunks.product_id
  -> cart_items.product_id
  -> evidence_links.product_id

product_chunks.id
  -> evidence_links.chunk_id

conversations.id
  -> retrieval_traces.conversation_id
  -> evidence_links.conversation_id

session_id
  -> conversations / feedbacks / cart_items / active_chat_turns /
     chat_turn_cancellations / api_request_logs / audit_events 的轻量关联键
```

`session_id` 是客户端传入的轻量会话标识，不单独建 `sessions` 表。这样能满足 hackathon 目标里的多轮上下文和反馈闭环，同时避免 JWT、用户体系、长期画像等非核心复杂度。

## 重要设计取舍

- 商品品类差异通过 `products.metadata` 和 `product_chunks.metadata` 承载，而不是为肤质、存储、尺码、口味等建大量 nullable 列。当前数据只有 4 个品类，JSON 更适合快速迭代和 RAG 证据绑定。
- `product_chunks.embedding` 在 SQLite 中是 JSON；生产 PostgreSQL 中通过 `EmbeddingType` 映射到 `vector(1024)` 并创建 HNSW 索引。当前 dev.db 样例里存在 16 维 deterministic fallback embedding，这是开发态兜底，不是生产目标。
- `retrieval_traces` 和 `evidence_links` 是评审解释性的核心表。前者记录检索决策链路，后者记录推荐卡片引用了哪些证据。
- `api_request_logs` 记录请求事实，`audit_events` 记录业务副作用。二者拆开是为了避免把普通请求日志和“加购、反馈、取消、推荐持久化”等业务事件混在一起。
- `eval_samples` 已清理旧 schema 的 `must_have/preferred/forbidden` 列，当前只保留 `ground_truth/context/tags` 这一版评测契约。

## 代码入口

| 方向 | 主要文件 |
| --- | --- |
| 表模型 | `backend/src/repos/models.py` |
| 建表与迁移 | `backend/src/repos/database.py` |
| 商品入库 | `backend/src/services/product_ingest.py`, `backend/src/services/chunking.py` |
| 检索与证据 | `backend/src/services/retriever.py`, `backend/src/repos/documents.py`, `backend/src/repos/traces.py` |
| 会话/反馈/购物车 | `backend/src/services/conversation_state.py`, `backend/src/repos/conversations.py`, `backend/src/repos/feedbacks.py`, `backend/src/repos/cart_items.py` |
| 取消 | `backend/src/services/cancellation.py`, `backend/src/repos/cancellations.py` |
| 评测 | `backend/src/services/eval/`, `backend/src/repos/eval_samples.py`, `backend/src/repos/eval_runs.py` |
| 观测与审计 | `backend/src/middleware/request_context.py`, `backend/src/services/audit.py`, `backend/src/repos/audit.py` |
