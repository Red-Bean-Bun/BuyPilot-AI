# BuyPilot-AI Backend Handoff - 2026-05-24

## 0. 当前实现 vs PRD 关键差距

| PRD 要求 | 当前实现 | 差距/风险 | 建议优先级 |
|---|---|---|---|
| PostgreSQL + pgvector，`product_chunks.embedding vector(1024)` | 本地 `.env` 默认 `DATABASE_URL=sqlite:///./buypilot-dev.db`；Postgres/pgvector 已通过 `deploy/docker-compose.yml` 验证，reindex（100 products / 1292 chunks / 1024 dims）+ demo smoke（6/6 场景）均通过 | 日常开发仍用 SQLite；演示需切 `DATABASE_URL=postgresql+psycopg://...` | P1 |
| 官方 raw dataset 入库，作为唯一真相源 | 已完成。`data/raw/ecommerce_agent_dataset` -> `products` + `product_chunks`；runtime `products.py` 也从 raw dataset 派生 | 已入 git（`520d2c2`），其他环境无需单独准备 | Done |
| Embedding：`text-embedding-v3` 1024 维 primary | 已完成真实百炼调用。`reindex_embeddings.py` 已把当前 DB 1292 chunks 全部刷成 1024 维 | 百炼 batch size 最大 10，不能改回 32；否则会 400 并 fallback | Done |
| Rerank：百炼 rerank top_n=5 | 已接 `/reranks`，profile 名 `gte_rerank` 实际 model 为 `qwen3-rerank`；retriever 已改为 chunk text rerank 后聚合 product | PRD 原写 `gte-rerank`，官方已临近停用 | P1 |
| 混合检索：硬过滤 + pgvector 向量召回 + rerank | 已实现硬过滤 + DB chunk embedding 召回 + chunk-level rerank + evidence 绑定；Postgres 下使用 pgvector `<=>` 库内 top-k + HNSW index | SQLite 下仍是 JSON embedding + Python 余弦 fallback | P1 |
| SSE envelope 与 mock_pipeline 同形状 | 已有 `types/sse_events.py`、`mock_pipeline.py`、`pipeline.py`；测试覆盖 SSE/API contract | 当前 pipeline 顺序可跑，推荐文案与决策已并行 | P1 |
| LLM task-oriented interface，禁止 runtime/API 直调模型 | 已完成。`llm_client.py` 暴露 `analyze_intent/generate_criteria/generate_recommendation/analyze_image/generate_decision`，全部接真实 LLM | `generate_decision` prompt 约束 winner_product_id 可选范围 + 代码验证防止幻觉 | Done |
| `.env.example` + profiles 配置 | 根目录 `.env` 已存在且被忽略；`settings.py` 自行加载；`llm_profiles.yaml` 已有百炼/Doubao profiles | 还没有 `.env.example`；不要泄露真实 key | P2 |
| 反馈闭环：feedbacks 表影响同一 session 下一轮 criteria + retrieval | ✅ 已完成。`feedbacks.py` 和 `/feedback` 已持久化；avoid_products/avoid_traits 已进入 retrieval 硬过滤；内存兜底上移到 Service | 覆盖"不喜欢这个/除了耐克/不要含酒精" | Done |
| `/chat/cancel` 真取消，断开 SSE 中断后续 LLM/RAG | 有 `cancel.py` stub，返回 `{"canceled": true}` | 没有任务注册表/取消令牌；不能真正中断后端任务 | P2 |
| `/upload/image` 多模态解析 Qwen-VL | ✅ 已完成。multipart 上传、静态 `/uploads`、本地图片 data URL 转 VL、multimodal analysis 注入 criteria | 旧 JSON 占位请求仍兼容 | Done |
| 图片可访问 URL | `ProductPayload.image_url` 仍多为 raw 相对路径 | Android/前端不能稳定加载本地 raw 图片；需要 FastAPI static mount 或 image proxy | P1 |
| 9 张表 + trace/evidence/eval/cart | `models.py` 已有 Product/ProductChunk/Conversation/Feedback/CartItem/EvalRun/RetrievalTrace/EvidenceLink/EvalSample | conversation trace 未完整 FK 串联 | P2 |
| 后台写 trace/evidence，不阻塞流式 | pipeline 在推荐后同步写 `retrieval_traces/evidence_links` | 简单可用，但不是后台任务；慢 DB 会阻塞 done 前收尾 | P2 |
| Admin API、RAGAS eval、baseline eval | ✅ 已完成。15 指标（7 确定性 + 8 LLM Judge），15 条样本，CLI + API + Streamlit 看板 | 见 `eval-module-handoff.md` | Done |

## 1. 当前任务目标

当前目标是把 BuyPilot-AI 后端从"P0 可跑 skeleton"推进到"真实数据 + 真实模型 + 可解释 RAG"的可演示版本，同时保持 Android/前端依赖的 SSE 契约稳定。

目前已达到的完成标准：

- raw ecommerce dataset 是商品与 RAG chunk 的唯一真相源。
- 百炼 `.env`、OpenAI-compatible chat/embedding/rerank 已接入。
- 当前本地 DB 已 reindex：1292 个 `product_chunks` 全部为 1024 维 embedding。
- retrieval 已从"内存列表 + 规则过滤"升级为"DB chunk embedding 召回 + chunk rerank + evidence 绑定"。
- API contract 文档已收敛到 `contracts/sse-events.schema.json` + golden trace 示例。
- 全量测试通过：`75 passed`（2026-05-24 核实）。
- `generate_decision` 已接真实 LLM（Qwen-Plus primary / Doubao fallback）。
- `pipeline.py` alternatives 构造已修复：按 `product_id != winner_product_id` 过滤。
- `backend/README.md` 已创建。
- 图片上传 + Qwen-VL-Plus 多模态理解已完成。
- 反馈闭环已完成（avoid_products/avoid_traits 进入 retrieval 硬过滤）。
- 对话式加购（add/view）已持久化到 cart_items 表。
- Postgres/pgvector reindex + Demo smoke 已验证（6/6 场景通过）。
- Prompt 文件已通过 `PromptStore` 运行时加载 `backend/prompts/*.md`。
- CI（Pytest + Ruff）已配置 `.github/workflows/backend-tests.yml`。
- 评测模块完整（15 指标 + Streamlit 看板）。

下一阶段完成标准建议：

- 补齐图片静态路径（FastAPI static mount 或 image proxy），让前端能加载商品图片。
- 实现真正的 cancel 机制（任务注册表 + 取消令牌）。
- 决定日常开发是否默认切 PostgreSQL + pgvector，减少 SQLite/Postgres 双轨维护成本。

## 2. 当前进展

### 已完成的关键实现

- `backend/src/config/settings.py`
  - 读取项目根目录 `.env`，不覆盖已有 process env。
  - 暴露 `dataset_dir`、`env_value()`、`TASK_MODEL_MAP`。
  - 业务代码不直接散落 `os.getenv()`。

- `backend/src/config/llm_profiles.yaml`
  - 已配置 Doubao/Qwen chat、Qwen VL、Qwen embedding、Qwen rerank。
  - `qwen_embedding.model = text-embedding-v3`，`dimensions = 1024`。
  - `gte_rerank` profile 名保留，但实际 `model = qwen3-rerank`。

- `backend/src/services/llm_client.py`
  - task-oriented interface：`analyze_intent` / `generate_criteria` / `generate_recommendation` / `analyze_image` / `generate_decision`。
  - 内部按 `TASK_MODEL_MAP` 选 primary/fallback。
  - `generate_decision` prompt 约束 winner_product_id 为传入商品之一；代码验证 LLM 返回的 id 在合法列表里。

- `backend/src/services/prompts.py` + `backend/prompts/*.md`
  - `PromptStore` 运行时加载 `backend/prompts/*.md`（7 个文件：intent_analysis / criteria_generation / recommendation / decision / image_analysis / clarification / metadata_extraction）。
  - 缺失时 fallback 到 `llm_task_payloads.py` 硬编码 schema。

- `backend/src/services/embedding.py`
  - `embed_text()` / `embed_texts()` 已接真实 `/embeddings`。
  - 当前环境需要 `httpx[socks]`，否则会因为 `all_proxy=socks5://...` 且缺 `socksio` 导致请求未发出就失败。

- `backend/src/services/retriever.py`
  - 当前链路：`criteria -> query embedding -> DB product_chunks 召回 -> hard filters -> chunk text rerank -> product 去重聚合 -> winning chunk evidence`。
  - PostgreSQL 下使用 pgvector `<=>` 库内 top-k + HNSW index；SQLite 下保留 JSON embedding + Python 余弦 fallback。
  - `retrieval_role=risk` 的负评/风险 chunk 不进入主召回。

- `backend/src/services/reranker.py`
  - `rerank()`：product-level 兼容旧调用。
  - `rerank_texts()`：chunk/passage-level rerank，retriever 正在使用。
  - 真实调用走 `/reranks`；失败会 deterministic fallback。

- `backend/src/services/evidence.py`
  - 优先返回本轮 retrieval 命中的 chunk evidence。
  - 没有 retrieval cache 时返回 raw dataset snippet。

- `backend/src/services/feedback.py`
  - Feedbacks 表已持久化，avoid_products/avoid_traits 已进入 retrieval 硬过滤。
  - 覆盖"不喜欢这个/除了耐克/不要含酒精"。

- `backend/src/services/image_upload.py`
  - multipart 上传、静态 `/uploads`、本地图片 data URL 转 VL、multimodal analysis 注入 criteria。

- `backend/src/repos/traces.py`
  - 写 `retrieval_traces` 和 `evidence_links`。
  - 当前 pipeline 同步写入，未后台化。

- `backend/src/runtime/pipeline.py`
  - `chat_stream()` 是真实 pipeline owner。
  - 支持 recommend、feedback、add_to_cart、view_cart、clarification。
  - SSE shape 对齐 `runtime/mock_pipeline.py`。
  - alternatives 构造已修复：按 `product_id != winner_product_id` 过滤。
  - 推荐文案与最终决策后台并行。

- `contracts/sse-events.schema.json`
  - SSE 事件 JSON Schema，source of truth。
  - 配套 3 个 golden trace 示例。

- `backend/src/scripts/demo_smoke.py`
  - Postgres/pgvector + 真实模型 Demo smoke：6/6 场景通过。
  - 覆盖文字推荐、图片理解、多轮约束、加购、查看购物车。

### 已执行验证

```bash
cd /mnt/disk1/LZJ/project/BuyPilot-AI/backend
uv run pytest -q                                    # 75 passed
uv run -m src.scripts.reindex_embeddings            # 100 products / 1292 chunks / 1024 dims
uv run -m src.scripts.smoke_live_rag                # 端到端 SSE 验证
uv run -m src.scripts.demo_smoke                    # 6/6 场景通过（需 Postgres）
```

## 3. 关键上下文

重要背景：

- 项目是 BuyPilot-AI：基于 RAG 的多模态电商导购 Agent。
- `AGENTS.md` 是 codex 配置文件。
- PRD 文件：`doc/prd/02-后端与AgentPRD.md`。
- Android/前端通过 SSE 消费 `/chat/stream`，事件形状比内部实现更重要。

用户明确要求：

- 按依赖层分批推进，不要一次把 PRD 全灌给模型。
- raw dataset 是唯一真相源。
- 不需要 `BUYPILOT_LIVE_LLM=1` / `BUYPILOT_LIVE_EMBEDDING=1` 这种开关；有 key 就真实调用。
- 不要怕 API 额度浪费，优先真实模型验证。
- mock pipeline 是 SSE golden reference，真实 pipeline 只替换真实计算结果，不改变事件形状。
- 当前用户已经失去对代码掌控感，后续文档和变更要更可解释、少堆复杂度。

已知约束：

- 架构方向：`UI -> Runtime -> Service -> Repo -> Config/Types`。
- Runtime/API 不应直接调用 OpenAI/DashScope SDK，不直接写复杂 SQL。
- LLM 必须通过 service 层 task-oriented APIs。
- 配置集中在 `settings.py` + `llm_profiles.yaml`。
- 普通 pytest 不得依赖外网或真实 API key。
- `.env` 里有真实 key，不要打印、复制进文档、提交信息或最终回答。

重要假设：

- 当前本机 `.env` 可用，包含 `BAILIAN_BASE_URL` 和 `BAILIAN_API_KEY`。
- 当前本机代理环境设置了 `all_proxy=socks5://127.0.0.1:18808`，所以 `httpx[socks]` 是运行时依赖。
- `data/raw/ecommerce_agent_dataset` 已入 git，所有环境均可使用。

## 4. 关键发现

- 百炼 `text-embedding-v3` batch size 上限是 `10`。之前 batch size `32` 会返回 400：`batch size is invalid, it should not be larger than 10`，然后被 fallback 吞掉，导致大部分 chunk 写成 16 维。当前已修复并重建索引。

- `smoke_live_rag.py` 现在是严格 smoke，不是 seed 工具。若 index 不是 1024，会直接失败。

- `retriever.py` 的 evidence 现在绑定的是 rerank 后胜出的 chunk，例如 `p_beauty_018:1`，不是泛泛的 product snippet。

- `backend/pyproject.toml` 已把 `httpx` 从 dev dependency 提到 runtime dependency，并加了 `[socks]` extra。

- 当前 DB 文件 `backend/buypilot-dev.db` 是本地产物，被 `*.db` 忽略；代码仓库里不会带着向量库。

## 5. 未完成事项

按优先级排序：

1. **图片服务化**
   - 当前 product `image_url` 多是 raw 相对路径。
   - 需要 FastAPI static mount 或 `/assets/products/...` 映射，让前端能直接加载图片。

2. **取消机制**
   - `/chat/cancel` 当前是 best-effort stub。
   - 没有中断正在跑的 LLM/RAG task。

3. **trace/evidence 后台化和 conversation FK 串联**
   - 当前同步写 trace/evidence，`conversation_id` 关联不完整。
   - PRD 期望后台写入，不影响 SSE。

4. **对话式加购 Remove/Update**
   - add/view 已持久化到 cart_items 表。
   - Remove/Update 未做。

5. **PostgreSQL 默认化**
   - Postgres/pgvector 已验证通过，但日常开发 `.env` 默认仍指向 SQLite。
   - 建议演示环境默认切 Postgres，减少双轨维护成本。

## 6. 建议接手路径

优先查看这些文件：

- `260524-handoff.md`：先读当前差距表。
- `backend/README.md`：当前后端地图。
- `CLAUDE.md`：项目架构约束。
- `doc/prd/02-后端与AgentPRD.md`：目标形态。
- `doc/status/backend-completion.md`：功能完成状态（P0/P1/P2）。
- `contracts/sse-events.schema.json`：SSE 事件契约 source of truth。
- `backend/src/runtime/mock_pipeline.py`：SSE golden reference。
- `backend/src/runtime/pipeline.py`：真实 pipeline。
- `backend/src/services/llm_client.py`：task-oriented LLM interface。
- `backend/src/services/retriever.py`：当前 RAG 主链路。

先验证：

```bash
cd /mnt/disk1/LZJ/project/BuyPilot-AI/backend
uv run pytest -q
uv run -m src.scripts.smoke_live_rag
```

如果 smoke 失败：

- `embedding_index` 不是 1024：先跑 `uv run -m src.scripts.reindex_embeddings`。
- `embedding` 不是 1024：查 `.env`、代理、`httpx[socks]`、百炼 endpoint。
- 出现 SOCKS 相关错误：确认 `.venv` 里安装了 `socksio`。

推荐下一步动作：

1. 做图片静态路径，因为这是前端立刻需要的体验断点。
2. 做 cancel 真取消。
3. 做购物车 Remove/Update。
4. 做 trace/evidence 后台化。
5. 日常开发默认切 Postgres/pgvector。

## 7. 风险与注意事项

- 不要把 `.env` 的 API key 写进文档、日志、回答或 commit message。

- 不要恢复硬编码 `DEMO_PRODUCTS`。raw dataset 是唯一真相源。

- 不要把 `backend/buypilot-dev.db` 当作源代码资产。它只是本机生成物。

- 不要让 pytest 依赖真实网络。`backend/tests/conftest.py` 会 mock 外部 AI 调用。

- 不要把 embedding batch size 改回 32。百炼限制是 10。

- 不要在 runtime/API 里直接调用 LLM SDK 或 `httpx` 模型接口；必须走 service。

- 不要破坏 SSE 字段和事件名。前端依赖：`thinking` / `criteria_card` / `text_delta` / `product_card` / `final_decision` / `done` / `clarification` / `error` / `cart_action`。

- `settings.get_settings()` 有缓存。测试里改 env 后要重置 `src.config.settings._settings = None`。

- `products.py` 的 raw dataset loading 有 cache。替换 dataset path 时要注意缓存。

## 下一位 Agent 的第一步建议

第一步不要继续写新功能。先从项目根目录运行：

```bash
cd /mnt/disk1/LZJ/project/BuyPilot-AI/backend
uv run pytest -q
uv run -m src.scripts.smoke_live_rag
```

确认之后，先读 `backend/README.md` 获取全景地图，然后按 handoff 优先级推进下一个任务（图片服务化）。