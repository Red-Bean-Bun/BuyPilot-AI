# BuyPilot-AI Backend

FastAPI 后端，负责 SSE 对话管道、RAG 检索、pgvector 索引、模型调用、评测与最小购物车闭环。

最后本地验证：2026-05-24，`90 passed`。

## Quick Start

```bash
cd /mnt/disk1/LZJ/project/BuyPilot-AI/backend

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

- `DATABASE_URL`：默认 `sqlite:///backend/buypilot-dev.db`；真实演示建议用 PostgreSQL + pgvector。
- `ECOMMERCE_DATASET_DIR`：官方 100 条商品数据目录，默认 `data/raw/ecommerce_agent_dataset`。
- `AUTO_SEED_ON_STARTUP=1`：启动时自动入库。
- `AUTO_SEED_STRICT_EMBEDDINGS=1`：启动入库时要求 embedding 维度匹配 1024。
- `STRICT_RUNTIME=1`：关闭 demo 级静默降级；LLM、embedding、rerank、DB/pgvector 失败会显性失败。
- `ALLOW_MEMORY_STATE_FALLBACK=1`：仅开发调试用。显式开启后，cart/feedback/conversation 在 DB 异常时可退到进程内内存状态；默认关闭。
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
- `services/`：业务逻辑、LLM task interface、检索、fallback、上传、评测。
- `repos/`：SQLModel/pgvector/官方数据读取，不承载业务决策。
- `config/`：环境变量、模型 profile、调参常量、领域词典。
- `types/`：HTTP schema 和 SSE event schema。

## Data And Retrieval

官方 raw JSON 是运行时商品真相源。入库流程：

1. `services.product_ingest` 读取 raw JSON。
2. `services.chunking` 生成 `knowledge_package` 和 typed chunks。
3. `services.embedding` 生成 1024 维 embedding。
4. `repos.models.ProductChunk.embedding` 在 PostgreSQL 下编译为 `VECTOR(1024)`，SQLite 下作为 JSON fallback。
5. `services.retriever` 优先走 pgvector `<=>` 召回，再 rerank，再绑定 evidence。

`create_db_and_tables()` 不再做破坏性迁移。旧表结构清理必须通过 `reindex_embeddings --drop-derived-tables` 明确触发。

## Tests

本地完整测试：

```bash
./.venv/bin/python -m pytest -q
```

CI 直接跑全量 pytest（`data/raw/` 已入 git）。

## Notes

- 单元测试通过不代表 live provider 可用；真实链路要跑 `smoke_live_rag` 或 `demo_smoke`。
- `STRICT_RUNTIME=1` 用于演示前验真，不适合默认开发体验。
- `/chat/cancel` 同时使用本进程 token 和数据库 cancel request；多实例共用同一个数据库时可以跨进程取消。
- Android 由客户端同学负责；本目录只维护后端契约和实现。
