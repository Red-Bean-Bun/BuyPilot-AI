# BuyPilot-AI Backend

FastAPI 后端，负责 SSE 对话管道、RAG 检索、pgvector 索引、模型调用、评测与最小购物车闭环。

最后本地验证：2026-05-26，`111 passed`。

## Quick Start

### Judge / Demo Gate

```bash
# From project root: create .env from .env.example and fill real BAILIAN_API_KEY first.
cp .env.example .env

# Start the demo-grade database/API path. This uses PostgreSQL + pgvector and auto-seeds data.
cd deploy
docker-compose up --build

# In a second terminal, run the live RAG gate. It prints per-stage JSON
# for embedding index, embedding provider, and chat stream.
cd ../backend
uv run -m src.scripts.smoke_live_rag
uv run -m src.scripts.demo_smoke
```

PostgreSQL + pgvector is required for runtime development and judge/demo verification. SQLite is only permitted inside pytest isolation, where tests use temporary databases and mocked vector search.

### Cloudflare Tunnel

Deploy the same compose runtime behind Cloudflare with the override in `deploy/docker-compose.cloudflare.yml`.
See `deploy/cloudflare.md` from the project root for the Cloudflare dashboard steps and Android endpoint update.

```bash
docker compose --env-file .env \
  -f deploy/docker-compose.yml \
  -f deploy/docker-compose.cloudflare.yml \
  up --build -d
```

### Local Development

```bash
cd backend

# 安装依赖
uv sync --extra dev

# 单元测试：外部 LLM / embedding / rerank 默认 mock，不需要 API Key
./.venv/bin/python -m pytest -q

# 重建商品 chunk + embedding，需要 .env 中有真实模型 Key
./.venv/bin/python -m src.scripts.reindex_embeddings

# 如果旧 Postgres 里 product_chunks.embedding 仍是 JSON，需要显式允许删除派生表
./.venv/bin/python -m src.scripts.reindex_embeddings --drop-derived-tables

# 严格 live smoke，需要真实 Key + 可用数据库
./.venv/bin/python -m src.scripts.smoke_live_rag

# 启动 API
./.venv/bin/python -m uvicorn src.api.app:app --reload --port 8000
```

## Environment

`.env` 从项目根目录加载：`BuyPilot-AI/.env`，不是 `backend/.env`。
模板见项目根目录 `.env.example`，真实 Key 只放本地 `.env`。

关键变量：

- `DATABASE_URL`：必填，且运行时必须指向 PostgreSQL + pgvector，例如 `postgresql+psycopg://buypilot:buypilot@localhost:5432/buypilot`。SQLite 仅允许 pytest 隔离测试使用临时库。
- `ECOMMERCE_DATASET_DIR`：官方 100 条商品数据目录，默认 `data/raw/ecommerce_agent_dataset`。
- `AUTO_SEED_ON_STARTUP=1`：启动时自动入库。
- `AUTO_SEED_STRICT_EMBEDDINGS=1`：启动入库时要求 embedding 维度匹配 1024。
- `STRICT_RUNTIME=1`：兼容旧配置；当前非 LLM 依赖已默认显性失败，不再靠 strict 才关闭兜底。
- 商品图片通过 `/assets/products/{raw_image_path}` 暴露给 Android；上传图片仍通过 `/uploads/{file}` 暴露。

## Architecture

当前分层约束：

```text
API -> Runtime -> Service -> Repo -> Config/Types
```

自动守护：

- `tests/test_architecture_layers.py` 禁止 `api/`、`runtime/` 直接 import `src.repos`。
- `tests/test_architecture_layers.py` 禁止 `repos/` 反向 import `src.services`。

职责边界：

- `api/`：FastAPI router，只做 HTTP/SSE 边界。
- `runtime/`：对话 turn 编排，产出 SSE 事件。
- `services/`：业务逻辑、LLM task interface、检索、上传、评测；只保留 LLM provider fallback telemetry。
- `repos/`：SQLModel/pgvector/官方数据读取，不承载业务决策。
- `config/`：环境变量、模型 profile、调参常量、领域词典。
- `types/`：HTTP schema 和 SSE event schema。

## Data And Retrieval

官方 raw JSON 是运行时商品真相源。入库流程：

1. `services.product_ingest` 读取 raw JSON。
2. `services.chunking` 生成 `knowledge_package` 和 typed chunks。
3. `services.embedding` 生成 1024 维 embedding。
4. `repos.models.ProductChunk.embedding` 在 PostgreSQL 下编译为 `VECTOR(1024)`；SQLite JSON embedding 仅用于 pytest 临时库兼容。
5. `services.retriever` 优先走 pgvector `<=>` 召回，再 rerank，再绑定 evidence。
6. 如果没有持久化 chunk embedding，检索直接失败，不退回原始商品表或内存结果。

`create_db_and_tables()` 不再做破坏性迁移。旧表结构清理必须通过 `reindex_embeddings --drop-derived-tables` 明确触发。

## Tests

本地完整测试：

```bash
./.venv/bin/python -m pytest -q
```

CI 直接跑全量 pytest（`data/raw/` 已入 git）。

## Notes

- 单元测试通过不代表 live provider 可用；真实链路要跑 `smoke_live_rag` 或 `demo_smoke`。
- 非 LLM provider fallback 已移除；演示前仍应跑真实 `smoke_live_rag` 验证 provider、embedding、rerank 和向量库。
- `/chat/cancel` 同时使用本进程 token 和数据库 cancel request；多实例共用同一个数据库时可以跨进程取消。
- Android 由客户端同学负责；本目录只维护后端契约和实现。
