# BuyPilot-AI 后端完成状态

> 最后核实：2026-05-24
> 维护方式：AI 完成后端功能开发后自动更新此文档，见 CLAUDE.md "Agent 工作指引" 第 8 条
> 核实命令：`backend/.venv/bin/python -m pytest -q`（76 passed）；`uv run ruff check src tests` 通过；`uv run ruff format --check src tests` 通过；`uv run mypy -p ...` 覆盖 8 个核心模块并通过；Postgres/pgvector reindex 通过（100 products / 1292 chunks / 1292 embedded / 1024 dimensions）；Postgres Demo smoke 通过（6/6 场景通过，报告 `backend/reports/demo-smoke-20260523-160717.json`）

---

## P0：基础功能完整性（评审权重 35%）

| # | 功能 | 状态 | PRD 章节 | 代码路径 | 备注 |
|---|------|------|---------|---------|------|
| 1 | SSE 流式对话端点 `/chat/stream` | ✅ 已完成 | 5.1, 5.2 | `api/chat.py` | StreamingResponse + format_sse，已验证可跑通 |
| 2 | SSE 事件协议（9 种事件类型） | ✅ 已完成 | 5.1 | `types/sse_events.py` | SSEEventBase + 9 个子类 + EventSeq + Constraints DSL，JSON Schema 已对齐 |
| 3 | 意图识别（6 种意图） | ✅ 已完成 | 4.1 | `services/llm_client.py` `services/llm_gateway.py` `services/llm_fallbacks.py` `stages/intent.py` | LLM primary + 关键词规则 fallback，含 `view_cart`/`add_to_cart`/`feedback`；`STRICT_RUNTIME=1` 下坏 JSON/无 provider 会显性失败 |
| 4 | 购买标准生成 | ✅ 已完成 | 4.1 | `services/llm_client.py` `services/llm_gateway.py` `stages/criteria.py` | LLM + 规则 fallback，含 `criteria_patch` 合并 + feedback 注入；strict 下不再静默兜底 |
| 5 | 推荐解释生成 | ✅ 已完成 | 4.1 | `services/llm_client.py` `stages/recommendation.py` | LLM + 规则 fallback，流式 text_delta；strict 下要求有效 live 响应 |
| 6 | 最终决策生成 | ✅ 已完成 | 4.1 | `services/llm_client.py` `stages/decision.py` | LLM + 规则 fallback，含 winner/why/not_for/alternatives；strict 下 winner/schema 不合法会失败 |
| 7 | 数据入库（100 商品 × 1292 semantic chunk） | ✅ 已完成 | 3.5 | `services/product_ingest.py` `services/chunking.py` `scripts/reindex_embeddings.py` | seed 已按 profile/marketing/faq/review/warning/compare 语义 chunk 入库，并在 product_metadata 写入 `knowledge_package`；Postgres reindex 已通过；旧派生表清理必须显式传 `--drop-derived-tables` |
| 8 | API 端点注册 | ✅ 已完成 | 5.2 | `api/app.py` | 6 个 router 已注册（chat/cancel/feedback/upload/cart/admin_eval），upload 支持 multipart 图片上传；cancel 已接 active turn registry |
| 9 | 管道编排 | ✅ 已完成 | 4.1 | `runtime/pipeline.py` `runtime/handlers.py` `runtime/streaming.py` | pipeline 只负责 turn 生命周期、图片/意图前置和 handler 路由；检索完成后立即发 product_card，推荐文案与最终决策后台并行，阶段耗时写入 retrieval trace |
| 10 | 混合检索（硬过滤 + pgvector/SQLite 向量召回 + Rerank） | ✅ 已完成 | 4.2 | `services/retriever.py` `repos/documents.py` | PostgreSQL 下使用 pgvector `<=>` 库内 top-k + HNSW index，SQLite 下保留 JSON embedding + Python 余弦 fallback；`retrieval_role=risk` 的负评/风险 chunk 不进入主召回 |

## P1：效果与可靠性（评审权重 20%）+ 加分项启动（20%）

| # | 功能 | 状态 | PRD 章节 | 代码路径 | 备注 |
|---|------|------|---------|---------|------|
| 11 | 双轨模型 + fallback 机制 | ✅ 已完成 | 2 | `services/llm_client.py` `services/llm_gateway.py` `config/settings.py` | Doubao+Qwen 双轨，TASK_MODEL_MAP 控制 primary/fallback；transport/profile 已从 task 逻辑拆出 |
| 12 | Embedding 服务 | ✅ 已完成 | 2 | `services/embedding.py` | 百炼 text-embedding-v3 primary，Doubao fallback，有 deterministic fallback；strict 下禁用 deterministic fallback |
| 13 | Rerank 服务 | ✅ 已完成 | 2 | `services/reranker.py` | qwen3-rerank 接入，API 失败时 deterministic rerank；strict 下禁用 deterministic rerank |
| 14 | 证据绑定 | ⚠️ 部分完成 | 4.2 | `services/evidence.py` `repos/documents.py` | product_card 带 evidence；DB/pgvector chunk 命中时 source_id 可关联真实 chunk，chunk metadata 已带 chunk_type/evidence_kind/retrieval_role；fallback source_id 不可关联 `product_chunks` |
| 15 | 检索追踪 + 证据链接 | ✅ 已完成 | 6.1 | `repos/traces.py` `services/fallbacks.py` | retrieval_traces + evidence_links 写入 SQLite，filters_applied 内记录 intent/criteria/retrieve/recommendation/decision 阶段耗时和 `_fallbacks` 降级事件 |
| 16 | 多轮上下文 | ⚠️ 部分完成 | 4.3 | `services/conversation_state.py` `repos/conversations.py` | 最新 criteria/product_ids 已持久化到 Conversations 表，Demo 多轮可恢复；内存兜底上移到 Service；完整消息历史尚未用于 LLM |
| 17 | 反馈闭环 | ✅ 已完成 | 7 | `services/feedback.py` `repos/feedbacks.py` `services/retriever.py` | Feedbacks 表已持久化，avoid_products/avoid_traits 已进入 retrieval 硬过滤；内存兜底上移到 Service；覆盖“不喜欢这个/除了耐克/不要含酒精” |
| 18 | 反选排除（⭐⭐） | ✅ 已完成 | — | `stages/criteria.py` | criteria_patch + ingredient_avoid + DB 会话恢复已覆盖 Demo 3 |
| 19 | 图片上传 + Qwen-VL-Plus 理解 | ✅ 已完成 | 5.2 | `api/upload.py` `services/image_upload.py` `runtime/pipeline.py` | multipart 上传、静态 `/uploads`、本地图片 data URL 转 VL、multimodal analysis 注入 criteria；旧 JSON 占位请求仍兼容 |
| 20 | 对话式加购（⭐入门） | ⚠️ 部分完成 | — | `services/cart.py` `repos/cart_items.py` `api/cart.py` | add/view 已持久化到 cart_items 表并覆盖 Demo 4；内存兜底上移到 Service；Remove/Update 未做 |

## P2：打磨与稳定性

| # | 功能 | 状态 | PRD 章节 | 代码路径 | 备注 |
|---|------|------|---------|---------|------|
| 21 | Thinking 心跳（800ms） | ✅ 已完成 | 4.4 | `runtime/streaming.py` | LLM/VL/retrieval/decision 等待期通过 `run_with_heartbeat` 持续发 thinking，证据绑定已显式随 RecommendationResult 回传 |
| 22 | 取消生成（Cancel） | ✅ 已完成 | 5.2 | `api/cancel.py` `runtime/cancel_registry.py` | active turn registry + cancellation token；`/chat/cancel` 命中后 pipeline 取消后台 stage 并 `done` 收尾；单进程有效 |
| 23 | 评测模块 | ✅ 已完成 | 6.1 | `services/eval/` | 代码实际包含 7 个确定性指标 + 8 个 LLM Judge 指标 + overall_score，Qwen-Plus 做 Judge，无需额外依赖 |
| 24 | 评测样本（15 条） | ✅ 已完成 | 6.3 | `data/eval/eval_samples.json` | 15 条覆盖 6 种 scenario_type × 4 品类 × 3 难度 |
| 25 | 管理后台 API | ⚠️ 部分完成 | 5.2 | `api/admin_eval.py` | 已有 GET /admin/eval/runs、GET /admin/eval/runs/{id}、GET /admin/eval/samples、POST /admin/eval/samples/seed；无 POST /admin/eval/runs 触发评测 |
| 26 | 4 条 Demo 路径稳定性打磨 | ✅ 已完成 | — | `src/scripts/demo_smoke.py` | 真实 Postgres/pgvector + 真实模型 Demo smoke 已验证 6/6 场景通过，覆盖文字推荐、图片理解、多轮约束、加购、查看购物车 |

## 工程质量（评审权重 25%）

| # | 项目 | 状态 | 备注 |
|---|------|------|------|
| 27 | 测试覆盖 | ✅ 76 测试全通过 | 2026-05-24 核实：`backend/.venv/bin/python -m pytest -q`；覆盖 model/schema/pipeline/API/retrieval/state repos/startup seed/image upload/multimodal/heartbeat/cancel/错误脱敏/fast product cards/feedback hard filter/semantic chunk/pgvector DDL/runtime prompt loading/demo smoke/architecture layer guards/否定词抽取/fallback trace/strict runtime |
| 28 | 架构分层（AGENTS.md） | ✅ 基本符合 | API/Runtime 已通过 Service 访问业务能力；`repos.ingest` 反向依赖已移到 `services/product_ingest.py`；新增测试防止 API/Runtime 直接 import Repo、Repo 反向 import Service |
| 29 | 配置集中管理 | ✅ 符合 | `settings.py` + `llm_profiles.yaml` + `config/tuning.py` + `config/domain_terms.py`，运行时调参常量和领域词典已集中，禁止散落 `os.getenv()` |
| 30 | 错误处理 | ✅ 有 | pipeline try/except 兜底，ErrorEvent 对外只返回稳定文案 + trace_id，内部异常进日志；LLM/embedding/rerank/cart/trace 等可降级路径已加日志；推荐链路 trace 记录 `_fallbacks`；`STRICT_RUNTIME=1` 下关键降级会显性失败 |
| 31 | Docker Compose | ✅ 基本可用 | `deploy/docker-compose.yml` 已配置 `pgvector/pgvector:pg16`；Postgres 启动时自动 `CREATE EXTENSION vector`，`product_chunks.embedding` 在 Postgres 下为 `VECTOR(1024)`；旧 JSON chunk 表不再运行时自动 drop，需通过 `reindex_embeddings --drop-derived-tables` 显式清理；SQLite 测试环境仍为 JSON |
| 32 | Prompt 文件 | ✅ 已接入 | `analyze_intent`/`generate_criteria`/`generate_recommendation`/`generate_decision`/`analyze_image` 已通过 `PromptStore` 运行时加载 `backend/prompts/*.md`，缺失时 fallback 到硬编码 schema；intent 兼容旧 prompt 的 `intent_type` 字段 |
| 33 | 接口契约文档 | ✅ 有 | `contracts/sse-events.schema.json` + 3 个 golden trace 示例 |
| 34 | 评测看板（Streamlit） | ✅ 已完成 | `static/eval_dashboard.py`，4 页：总览/版本对比/样本详情/错误分析，`streamlit run` 启动 |
| 35 | Pytest + Ruff + Mypy CI | ✅ 已完成 | `.github/workflows/backend-tests.yml`；使用 `uv sync --locked`；CI 运行 `ruff check` + `ruff format --check` + 目标 mypy + 全量 pytest；另有手动/定时 live provider + pgvector smoke job（需要 secrets） |

## 4 条 Demo 路径状态

| # | Demo 路径 | 后端状态 | 备注 |
|---|-----------|---------|------|
| 1 | "推荐适合油皮的洗面奶，200元以内，日常护肤" | ✅ **可跑通** | 2026-05-23 Postgres Demo smoke 验证：5 个 product_card + criteria + final_decision，首个 evidence `p_beauty_018:1` |
| 2 | 上传护肤品图片 + "这个适合敏感肌吗？" | ✅ **可跑通** | 2026-05-23 Postgres Demo smoke 验证：官方商品图通过 `/uploads/demo_p_beauty_012_live.jpg` 转 data URL，Qwen-VL + RAG 返回 5 个商品 |
| 3 | "不要含酒精的防晒霜" + "预算降到200" | ✅ **可跑通** | 2026-05-23 Postgres Demo smoke 验证：上一轮无酒精防晒约束延续到预算降到 200 的下一轮 |
| 4 | "把这个加到购物车" | ✅ **可跑通** | 2026-05-23 Postgres Demo smoke 验证：cart_action add + view 均成功，最终 cart `total_items=1` |

## 已知架构差距

| # | 问题 | 影响 | 建议优先级 |
|---|------|------|-----------|
| A | **首张商品卡仍需等待 intent/criteria/retrieval**：推荐文案与决策已并行，但检索前置依赖仍在 | 用户可见商品卡延迟仍取决于标准生成和检索速度 | P1 |
| B | ~~Cancel 空壳：无法真正中断正在生成的 SSE 流~~ 已接单进程 active turn registry 和 cancellation token | ✅ 已解决；多实例部署仍需外部 registry | P2 |
| C | **本地 `.env` 默认仍指向 SQLite**：Postgres/pgvector 已验证通过，但直接运行后端命令如果不显式传 `DATABASE_URL` 仍会使用 `backend/buypilot-dev.db` | 演示环境需使用 compose API 环境变量，或显式传 `DATABASE_URL=postgresql+psycopg://buypilot:buypilot@localhost:5432/buypilot` | P1 |
| D | **状态存储仍不完整**：conversations/feedbacks/cart 已持久化核心状态，feedback 已进入检索硬过滤；完整多轮消息历史尚未用于 LLM | 复杂多轮表达理解仍有限 | P1 |
| E | **Prompt 覆盖仍未全量统一**：主链路 intent/criteria/recommendation/decision/image 已接入文件 prompt，但 clarification/metadata_extraction 暂未接入运行时 | 核心演示链路已可调 prompt；剩余文件属于非当前主链路 | P2 |
| F | **死代码**：PipelineState TypedDict 无人引用 | 维护负担 | P2 |
| G | **Service 仍是薄门面为主**：cart/feedback/conversation/trace 当前主要做层级隔离，复杂业务不多 | 分层方向已收口，但后续新增业务要继续进 Service，避免 Runtime/API 再次膨胀 | P2 |
| H | ~~CI 全量测试依赖官方 raw 数据：`data/raw/` 被 gitignore~~ `data/raw/` 已入 git（`520d2c2`），CI 远端可跑全量 pytest | ✅ 已解决 | P1 |
