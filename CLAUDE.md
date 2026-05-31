# BuyPilot-AI 项目开发指引
## 项目概要

**BuyPilot-AI**：基于 RAG 的多模态电商智能导购 Agent
**赛事**：ByteDance AI Full-Stack Challenge（3 周 / 3 人）
**一句话定位**：把用户模糊购物需求转化为可解释决策路径的多品类智能导购决策智能体

| 决策项 | 选择 |
|--------|------|
| 品类 | 多品类（美妆护肤/数码电子/服饰运动/食品生活），导师提供官方数据 |
| 客户端 | Android 原生（Kotlin + Jetpack Compose + OkHttp SSE 直连） |
| LLM | **双轨并行**：火山引擎 Doubao（意图识别主力）+ 百炼 Qwen（生成主力） |
| 后端 | Python FastAPI + PostgreSQL + pgvector + SQLModel |
| 流式协议 | SSE（OkHttp SSE 直连 FastAPI `/chat/stream`） |
| 模型切换 | task-oriented interface（analyze_intent / generate_criteria / generate_recommendation / analyze_image），内部按 TASK_MODEL_MAP 选 primary/fallback |

### 模型-任务映射（双轨策略）

| 任务 | Primary | Fallback | 选型原因 |
|------|---------|----------|---------|
| 意图识别/路由 | Doubao-Seed-2.0-lite | Qwen-Turbo | 意图识别不需要很强，Doubao 免费额度高 |
| 购买标准生成 | Qwen-Plus | Doubao | 核心亮点，JSON schema 强约束输出，Qwen-Plus 更稳定 |
| 推荐解释生成 | Qwen-Plus | Doubao | 解释需要高质量文本生成 |
| 多模态图片理解 | Qwen-VL-Plus | 无 | Doubao VL 生态不确定，Qwen-VL-Plus 成熟 |
| Embedding | text-embedding-v3 (1024维) | 确定性 fallback (16维) | 百炼有 Key + 维度确定；fallback 仅供开发态 |
| Rerank | qwen3-rerank (百炼) | 无 | Doubao 无对应服务 |

> Doubao API 配置见 `.env.example`（含 BaseAPI / Model / Key / Limit）。模型可自由选择，上表是当前决策。

## 项目核心约束

### P0/P1/P2 — 评审权重驱动

**P0 = 基础功能完整性(35%) + 工程质量底线(25%)**
- 评委拿起App能走通完整链路：输入文字 → 意图识别 → 购买标准生成(初版) → 检索 → 推荐生成(初版) → SSE流式返回 → 商品卡片渲染
- 代码结构清晰、接口设计合理、错误处理完善、文档齐全
- docker-compose up + Android Studio Run 一键启动
- 依赖版本锁定(requirements.txt固定版本) + 核心逻辑注释(RAG链路/Prompt构造)

**P1 = 效果与可靠性(20%) + 加分项启动(20%)**
- 流畅、美观、无Bug；检索准确率、无幻觉、复杂场景处理
- 做精一项胜过浅尝三项
- 正式版卡片渲染(标准卡/商品卡/决策卡) + 流式动画
- 混合检索(硬过滤+向量+Rerank) + 证据绑定
- 多轮上下文 + 反馈记录 + 反选排除
- 图片上传 + Qwen-VL-Plus理解
- 对话式加购⭐入门(cart_action SSE事件 + cart_items表)
- 双轨模型+fallback机制

**P2 = 打磨 + 稳定性**
- Thinking骨架屏/流式动画打磨
- 卡片交互细节打磨(滑动/收藏状态变化)
- 评测页面最小版(4个核心指标数字+版本对比)
- 4条Demo路径稳定性打磨

### 绝对不做（明确裁剪）

- 支付与下单确认流程（⭐⭐⭐挑战档不做，但⭐入门对话式加购已在P1）
- 商家入驻与商品管理后台
- Multi-Agent 蜂群架构
- 长期用户画像系统
- 全量真实电商平台接入
- Celery + Redis 异步任务
- JWT 复杂认证
- 完整商家后台（订单/库存/支付/权限）
- 语音输入（多模态⭐入门档，已有⭐⭐⭐拍照找货覆盖）
- 知识图谱增强检索（GraphRAG）
- 热门查询缓存/首屏极速优化（工程质量加分方向不做）
- 完整Web管理后台（只做最小评测页面）

### 加分项策略

| 加分方向 | 目标档位 | 定位 |
|----------|---------|------|
| 4.2 多模态交互 | ⭐⭐⭐拍照找货 | 深度方向1 |
| 4.3 对话智能与RAG增强 | ⭐⭐反选排除 + ⭐⭐⭐多商品对比 | 深度方向2 |
| 4.1 业务闭环 | ⭐入门对话式加购 | 轻量覆盖方向 |
| 4.4 工程质量与性能 | **不做** | 不投入 |

### 数据策略

- **100条**商品数据（导师提供的脱敏电商数据，4品类×25，中文）
- 每条含：product_id, title, brand, category, sub_category, base_price, image_path, skus(多规格), rag_knowledge{marketing_description, official_faq[], user_reviews[]}
- rag_knowledge 天然 chunking：marketing_description + 每个 FAQ + 每个 review = 独立 chunk
- 数据质量比数据数量重要
- products 表 Schema 使用 metadata JSONB 承载品类结构化属性（如护肤品的肤质适用、数码的存储规格等）

### 止损规则（评审权重驱动）

| 时间节点 | 验收标准 | 否则 |
|----------|---------|------|
| 5/27（第 7 天） | **评委拿起App能走通完整链路** + 一键启动 | 砍掉所有P1/P2，只追P0到能跑为止 |
| 6/03（第 14 天） | **4条Demo路径全部可演示** + 无幻觉无Bug | 砍掉复杂后台和增强功能 |
| 6/07（第 18 天） | 冻结功能 + design-decisions.md完成 | 只修Bug、打磨体验、准备答辩 |

### 减分项防御

| 减分项 | 防御措施 |
|--------|---------|
| AI编造商品/价格/优惠（幻觉） | 混合检索+硬过滤+证据绑定 |
| 纯Web/H5替代原生App | Android原生开发 |
| Demo无法运行或需大量手动配置 | docker-compose up一键启动 + README运行说明 |
| 代码完全依赖AI生成无法解释原理 | design-decisions.md + 答辩前自读一遍，能脱口而出每个核心决策的why |

### 四条 Demo 路径

| # | Demo路径 | 品类 | 核心演示能力 | 对应官方场景 |
|---|---------|------|-------------|-------------|
| 1 | "推荐适合油皮的洗面奶，200元以内" | 美妆护肤 | 模糊推荐+条件筛选 | 单轮模糊推荐 + 条件筛选 |
| 2 | 上传护肤品图片+"这个适合敏感肌吗？" | 美妆护肤 | 拍照找货⭐⭐⭐+VL理解 | 拍照找货 |
| 3 | "不要含酒精的防晒霜"+"预算降到200" | 美妆护肤 | 反选排除⭐⭐+多轮约束 | 多轮追问 + 反选排除 |
| 4 | "把这个加到购物车" | 通用 | 对话式CRUD+加购⭐入门 | 购物车⭐入门 |

---
## 文档索引

所有文档在 `doc/` 目录下，按语义分层：

```
doc/
├── strategy/          战略层（为什么做）
│   ├── 01-比赛背景与战略决策.md    ← 比赛背景 + 最终决策
│   └── 02-策略研究报告.md         ← 完整战略报告，MVP 边界，Demo 路径
├── prd/               产品需求（做什么）
│   ├── 01-Android前端PRD.md       ← Android 客户端执行文档（SSE 事件、卡片规范、Kotlin 契约）
│   └── 02-后端与AgentPRD.md       ← 后端 & Agent 执行文档（数据库 Schema、管道编排、API 契约）
├── status/            完成状态（做到哪了）
│   └── backend-completion.md      ← 后端功能完成状态，按 P0/P1/P2 分层，AI 每次开发后自动更新
├── research/          调研参考
│   └── 队友原始调研-多模态电商导购.md
├── risk/              风险预判
│   └── 卡点与风险清单.md          ← 10+ 卡点 + 止损规则
└── prompts/           提示词工具包
    ├── 01-DeepResearch提示词.md    ← 索引版
    ├── 02-DeepResearch提示词拆分.md ← 12 个分段 prompt
    └── 03-AI编码助手系统提示词.md  ← Linus 角色 + AGENTS.md 架构 + 编码原则
```

**编码相关指引**：
- 前端接口契约 → `doc/prd/01-Android前端PRD.md`（SSE 事件类型、Kotlin data class、卡片规范）
- 前端表格附录 → `doc/prd/01-附录-表格内容.md`（错误码、状态机、渲染策略、测试用例等）
- 后端接口契约 → `doc/prd/02-后端与AgentPRD.md`（数据库 Schema、管道编排、API 端点）
- 后端 Prompt 模板 → `backend/prompts/`（已由 `PromptStore` 运行时加载；优先改 .md 文件，硬编码字符串在 `llm_task_payloads.py` 中仅作 fallback）
- 产品定义 → `PRODUCT.md`（用户画像、产品目的、设计原则、Anti-references）
- 设计系统 → `DESIGN.md`（颜色、字体、间距、圆角、组件规范，开发 UI 时必须遵循）
- 风险清单 → `doc/risk/卡点与风险清单.md`（开发前必读）
- 设计决策 → `design-decisions.md`（每个核心决策 3 句话：选了什么/为什么/反过来会怎样）
- 后端完成状态 → `doc/status/backend-completion.md`（P0/P1/P2 功能完成度，AI 每次开发后更新）
- 前后端契约 → `contracts/sse-events.schema.json`（SSE 事件 JSON Schema，铁律 1 的 source of truth）
- 实现差距与踩坑 → `260524-handoff.md`（实现 vs PRD 差距表 + 已知陷阱）
- 评测模块 → `eval-module-handoff.md`（评测设计决策 + 运行方式）

## Codemap
### 分层架构
```
UI → Runtime → Service → Repo → Config/Types
```
- 依赖只能自上而下流动，禁止反向/横向/循环依赖
- 业务逻辑只能在 Service 层
- 所有 LLM 调用和数据库查询必须通过 Service 层的 task-oriented interface 执行。禁止在 Runtime 层直接调用 LLM SDK 或在 API 层直接执行 SQL。违反即架构错误。
- 配置集中管理，禁止 `os.getenv()` 散落在业务代码中

### 后端入口层

| 路径 | 职责 | 注意 |
| --- | --- | --- |
| `backend/src/api/app.py` | FastAPI app 装配；注册 router/middleware，挂载 `/uploads` 与 `/assets/products`，启动时初始化数据库。 | 应用启动边界。 |
| `backend/src/api/chat.py` | `POST /chat/stream`；处理 HTTP/SSE 边界。 | 核心交给 `runtime.pipeline.chat_stream`。 |
| `backend/src/api/upload.py` | 图片上传。 | multipart 是真实路径，JSON 兼容路径偏 legacy。 |
| `backend/src/api/cart.py`、`feedback.py`、`cancel.py` | 购物车查询、反馈提交、流式 turn 取消。 | API 边界，不放深层业务。 |
| `backend/src/api/admin_eval.py`、`observability.py` | 评测、调试、观测接口。 | 不放生产聊天业务。 |
| `backend/src/middleware/request_context.py` | 请求上下文中间件。 | 给 service/audit/trace 传递 request context。 |

### 后端运行时层

| 路径 | 职责 | 注意 |
| --- | --- | --- |
| `backend/src/runtime/pipeline.py` | 单轮对话编排入口；负责 turn/session、图片预分析、意图阶段、槽位澄清、handler 分发、审计事件、错误脱敏与清理。 | 一轮 chat 的总 owner。 |
| `backend/src/runtime/handlers.py` | 按意图执行处理；推荐流会发 criteria/product/text/final/cart/feedback 等事件。 | 保持事件顺序和兼容性。 |
| `backend/src/runtime/streaming.py` | thinking/heartbeat、阶段计时、取消检查。 | 不放业务规则。 |
| `backend/src/runtime/cancel_registry.py` | 进程内取消 token 注册表。 | best-effort cancellation。 |
| `backend/src/runtime/stages/intent.py` | 意图识别，注入会话摘要。 | 调 `services.llm_client.analyze_intent`。 |
| `backend/src/runtime/stages/criteria.py` | 合并历史标准、反馈与本轮标准 patch。 | 维护 CriteriaPayload 语义。 |
| `backend/src/runtime/stages/recommendation.py` | 检索包装与推荐文案生成。 | 调 retriever 与 recommendation LLM。 |
| `backend/src/runtime/stages/decision.py` | 最终决策生成。 | 决策卡/收尾文本相关。 |
| `backend/src/runtime/stages/multimodal.py` | 图片理解包装。 | 调 `analyze_image`。 |
| `backend/src/runtime/stages/slot_checker.py` | 确定性槽位澄清规则。 | 优先代码化，不放 prompt。 |

Runtime 只做编排；业务规则优先放 Service。

### 后端服务层

| 路径 | 职责 | 注意 |
| --- | --- | --- |
| `backend/src/services/llm_client.py` | LLM 任务门面：`analyze_intent`、`generate_criteria`、`generate_recommendation`、`generate_decision`、`analyze_image`。 | Runtime 调这里，不直接调 provider。 |
| `backend/src/services/llm_gateway.py`、`llm_profiles.py` | OpenAI-compatible provider 传输、profile 解析与 fallback。 | provider 细节集中在这里。 |
| `backend/src/services/llm_task_payloads.py` | prompt message 构造、JSON 解析、payload 规范化。 | 结构化输出边界。 |
| `backend/src/services/prompts.py` | 运行时加载 `backend/prompts/*.md`。 | 硬编码 prompt 只是 fallback。 |
| `backend/src/services/embedding.py`、`reranker.py` | embedding/rerank 门面。 | `STRICT_RUNTIME=0` 时才允许确定性 fallback。 |
| `backend/src/services/retriever.py` | 混合检索核心；embedding 查询、pgvector/SQLite chunk 召回、硬过滤、rerank、证据绑定。 | 硬过滤核心是 `_passes_hard_filters`。 |
| `backend/src/services/retrieval_features.py` | 查询/文档文本与评分特征工具。 | retriever 共享逻辑。 |
| `backend/src/services/product_ingest.py`、`chunking.py` | 官方商品数据入库、chunk、知识包、embedding。 | 数据事实来自 `data/raw/`。 |
| `backend/src/services/conversation_state.py` | 会话状态、历史 criteria、历史商品、压缩摘要。 | 多轮上下文 owner。 |
| `backend/src/services/cart.py`、`feedback.py`、`cancellation.py` | 购物车、反馈、取消相关业务用例。 | API 不直接操作 repo。 |
| `backend/src/services/evidence.py`、`trace_recorder.py`、`fallbacks.py` | 证据兜底、trace 持久化、fallback 事件。 | 解释性和可观测性相关。 |
| `backend/src/services/eval/` | 评测 runner、指标、LLM judge、admin helper。 | 评测域。 |
| `backend/src/services/request_context.py`、`audit.py`、`observability.py` | 请求上下文、审计、调试观测。 | 横切能力。 |
| `backend/src/services/image_upload.py`、`startup.py`、`http_client.py` | 图片落盘/URL、启动 seed、共享 HTTP client。 | 基础设施型 service。 |

### 后端仓储层

| 路径 | 职责 | 注意 |
| --- | --- | --- |
| `backend/src/repos/database.py` | engine、建表、pgvector extension/index、eval schema helper。 | 不放业务逻辑。 |
| `backend/src/repos/models.py` | SQLModel 表：商品、chunk、会话、反馈、购物车、取消、评测、trace、证据、请求日志、审计事件。 | 表结构集中处。 |
| `backend/src/repos/products.py` | 官方 raw JSON 商品源；转换为 `ProductPayload` 与商品图片 URL。 | 商品事实入口。 |
| `backend/src/repos/documents.py` | chunk 读取、pgvector 相似度查询、证据 payload。 | 检索持久化入口。 |
| `backend/src/repos/conversations.py`、`feedbacks.py`、`cart_items.py` | 会话、反馈、购物车持久化。 | 状态型 repo。 |
| `backend/src/repos/traces.py`、`audit.py`、`cancellations.py`、`eval_*` | trace、审计、取消、评测持久化。 | 运维/评测型 repo。 |
| `backend/src/repos/vector.py` | pgvector SQLAlchemy 类型。 | SQLite 用 JSON fallback。 |

Repo 不能 import service。

### 后端类型与配置

| 路径 | 职责 | 注意 |
| --- | --- | --- |
| `backend/src/types/sse_events.py` | Python SSE event model、`CriteriaPayload`、`ProductPayload`、`Constraints`、`format_sse`、`parse_sse_event`。 | 必须对齐 `contracts/sse-events.schema.json`。 |
| `backend/src/types/schemas.py` | HTTP 与 service DTO。 | API/service 数据边界。 |
| `backend/src/types/slot_defs.py`、`pipeline_state.py` | 槽位定义与 pipeline 状态类型。 | Runtime 类型支撑。 |
| `backend/src/config/settings.py` | 环境变量集中入口。 | 不要在业务代码散落 `os.getenv()`。 |
| `backend/src/config/tuning.py`、`domain_terms.py`、`llm_profiles.yaml` | 调参、领域词、LLM profile。 | 规则/参数优先配置化。 |

### 数据、脚本、测试

| 路径 | 职责 | 注意 |
| --- | --- | --- |
| `data/raw/ecommerce_agent_dataset/` | 官方 100 条商品数据，4 类 x 25。 | 运行时商品事实源。 |
| `data/processed/` | `data/scripts/process_data.py` 生成的 `products.json` 与 `chunks.json`。 | 派生产物。 |
| `data/eval/eval_samples.json` | 评测样例种子。 | eval 输入。 |
| `backend/prompts/*.md` | 运行时 prompt 编辑面。 | 优先改这里。 |
| `backend/src/scripts/reindex_embeddings.py` | 重建 chunks 与 embedding。 | 需要真实 provider key。 |
| `backend/src/scripts/smoke_live_rag.py` | 严格 live RAG 检查：索引、live embedding、chat stream。 | 验证 live provider 与 1024 维索引。 |
| `backend/src/scripts/demo_smoke.py` | Demo 路径 smoke。 | 演示前检查。 |
| `scripts/chat_stream_demo.py` | 对已启动后端发 chat stream 的 CLI helper。 | 本地调试。 |
| `contracts/examples/*.sse` | 契约测试 golden trace。 | 和 schema/Python/Android 对齐。 |
| `backend/tests/test_architecture_layers.py` | 分层架构守卫。 | 依赖方向检查。 |
| `deploy/docker-compose.yml` | Postgres/pgvector + FastAPI demo 环境。 | demo 基础设施。 |
---
## 运营操作

从 repo root 执行，除非命令里显式 `cd`。

```bash
# 后端开发
cd backend
uv sync --extra dev
uv run uvicorn src.api.app:app --reload --port 8000

# 后端测试
uv run pytest -q
uv run ruff check src tests
uv run ruff format --check src tests

# 重建商品 chunks + embedding，需要真实 key
uv run -m src.scripts.reindex_embeddings

# 严格 live RAG 检查，需要真实 key + 可用 DB/index
uv run -m src.scripts.smoke_live_rag

# Demo smoke
uv run -m src.scripts.demo_smoke

# Compose demo 环境
cd ../deploy
docker-compose up
```

环境要点：

- `.env` 从项目根目录加载，不是 `backend/.env`。
- `DATABASE_URL` 默认可用 SQLite；demo/live 应使用 PostgreSQL + pgvector。
- `AUTO_SEED_ON_STARTUP=1` 会在 API 启动时 seed DB。
- `AUTO_SEED_STRICT_EMBEDDINGS=1` 要求 1024 维 embedding。
- `STRICT_RUNTIME=1` 禁用确定性 fallback，暴露 live 失败。
- `ALLOW_MEMORY_STATE_FALLBACK=1` 只适合开发态，strict runtime 下无效。

## 已知坑

- 百炼 `text-embedding-v3` batch size 是 10，不要盲目调高。
- `gte_rerank` profile 当前实际 model 是 `qwen3-rerank`。
- 确定性 embedding fallback 是 16 维；live/demo 校验必须证明 1024 维索引可用。
- `ProductPayload.image_url` 是 `/assets/products/{raw_image_path}`。
- 上传图片从 `/uploads/{file}` 服务；本地图片会转 data URL 给 VLM。
- `backend/prompts/*.md` 已由 `PromptStore` 运行时加载；优先改这些文件，不要先改 fallback 字符串。
- `create_db_and_tables()` 不应做破坏性迁移；派生表重建用显式脚本/flag。
- 单元测试会 mock 外部 AI，不证明 provider live 可用；live 验证用 `smoke_live_rag`。
- `data/raw/` 是商品事实源。不要编造商品、价格、优惠、库存。

## 编码原则

### Linus 核心哲学

1. **"好品味"（Good Taste）** — 消除边界情况永远优于增加条件判断
2. **"Never break userspace"** — 不破坏已有功能，向后兼容是铁律
3. **实用主义** — 解决实际问题，不为假想场景写代码
4. **简洁执念** — 超过 3 层缩进就该重新设计

### 编码前思考流程

1. **Linus 三个问题**：这是个真问题还是臆想的？有更简单的方法吗？会破坏什么吗？
2. **数据结构分析**：核心数据是什么？数据流向哪里？谁拥有它？
3. **特殊情况识别**：哪些 if/else 是真正的业务逻辑，哪些是糟糕设计的补丁？
4. **复杂度审查**：这个功能本质是什么？能否减少一半概念？
5. **破坏性分析**：哪些已有功能会受影响？
6. **实用性验证**：方案复杂度是否与问题严重性匹配？

### 编码行为规范

- **编码前先思考**：陈述假设，如有不确定之处直接提问
- **简约至上**：仅编写解决问题的最低限度代码，不加臆测特性
- **精准修改**：仅触碰必须修改之处，不擅自"优化"相邻代码
- **目标导向**：将任务转化为可验证目标，循环迭代直至通过验证
- **业务/实现分离**：先明确业务模型，再讨论实现模型

---

## 核心数据流（Agent 编码时必读）

```
用户输入 (Android)
  ↓
意图识别 (Doubao primary / Qwen-Turbo fallback) + 槽位检查
  ↓ add_to_cart意图 → cart_action事件 → 加购操作
  ↓ 需要澄清时 → clarification 事件（多问题模式，每题带 suggested_options）
  ↓ 并行
购买标准生成 (Qwen-Plus primary / Doubao fallback) → 约束列表 → 展平为 CriteriaPayload（封闭 DSL，constraints 为显式枚举字段）  |  投机检索 (embedding + 硬过滤)
  ↓
混合检索：硬过滤(SQL) + 向量召回(pgvector) + Rerank(gte)
  ↓
推荐解释生成 (Qwen-Plus) + 证据绑定
  ↓
SSE 事件流（每事件必带 seq + session_id）：
  thinking → clarification → criteria_card → text_delta → product_card → cart_action → final_decision → done
  ↓
Android Compose + LazyColumn 卡片渲染
  ↓
用户反馈（quick_actions: criteria_patch/feedback/open_evidence/compare/add_to_cart） → feedbacks 表 → 下一轮推荐注入约束
```

> SSE 事件协议以 `doc/prd/01-Android前端PRD.md` 和 `doc/prd/02-后端与AgentPRD.md` 为准，两份 PRD 已对齐。

---

## 系统铁律（System Invariants）

违反以下任何一条即架构错误，必须立即修复，不可通过 patch 绕过。

### 铁律 1：SSE 事件协议封闭性

- SSE event type 禁止新增。现有的 8+1 个 type（thinking / clarification / criteria_card / text_delta / product_card / cart_action / final_decision / done + error）是全集。新功能无法映射到现有 type 时，先改业务设计，不是加新 type。
- 每个 event 的 field 变更必须同步更新 `contracts/` 目录的 JSON Schema 和 Android ChatUiNode。单边修改即架构错误。

**SSE 变更流程**（改 event 字段时必走）：
1. 先改 `contracts/sse-events.schema.json` — 这是 source of truth
2. 再改 `backend/src/types/sse_events.py` — Python 端实现
3. 最后改 Android 侧（`AgentPayload.kt` + `SseEventParser.kt` + `ChatUiNode.kt`）
4. 验证：`cd backend && uv run pytest tests/test_sse_events.py -v`

### 铁律 2：CriteriaPayload 封闭 DSL

- `Constraints` 使用平铺字段 + `| None`，所有允许的约束维度在 schema 中显式枚举。禁止 `dict[str, Any]`。
- 前端只消费 `chips: list[str]`，不依赖 constraints 键名语义。constraints 只约束后端检索硬过滤。
- 不能进入 schema 的需求，不值得自动化。

### 铁律 3：Prompt 边界

- Prompt 只能做三件事：组织语言、提取结构化意图、指导生成质量。
- 路由逻辑和硬过滤条件必须代码化。品类关键词映射表属于路由逻辑，禁止写进 prompt。

### 铁律 4：LLM 调用 task-oriented interface

- LLM 调用必须是 task-oriented interface（analyze_intent / generate_criteria / generate_recommendation / analyze_image），每个接口返回结构化的 Pydantic model，禁止返回 raw str 让调用方自己解析。
- 所有 LLM 调用和数据库查询必须通过 Service 层的 task-oriented interface 执行。禁止在 Runtime 层直接调用 LLM SDK 或在 API 层直接执行 SQL。

### 铁律 5：测试必须验证源码行为

写测试之前先问：**"如果源码这行逻辑写错了，这个 assert 会失败吗？"** 不会失败的测试 = 不存在的测试。

- **同义反复测试 100% 通过但毫无价值。** mock 返回 `{"category": "美妆护肤"}`，断言 `result.category == "美妆护肤"` —— 无论源码怎么改，只要 mock 不变，测试永远通过。这不是在测试代码，是在测试 mock 框架。
- **mock 是隔离手段，不是控制变量的捷径。** mock 的正确用途是消除对不可控外部系统的依赖（网络、API Key、时钟）。如果你 mock 了一个函数的输入再断言输出等于 mock 的返回值，你只是在验证 mock 本身。
- **纯函数直接考，不要绕远路。** 想测 `check_required_slots`，就直接构造 `IntentResult` 传入。不要先跑 `analyze_intent` 再传给 `check_required_slots` —— 那样测的不是目标函数的逻辑，而是整条链路的组合行为。

具体规则：

- **禁止同义反复测试。** 断言的期望值不得从 mock 的返回值复制。期望值必须从业务规则、输入数据的变换、或手动计算中独立推导。如果源码逻辑写错了测试仍会通过，这个测试就是装饰品。
- **禁止 Pydantic 存取测试。** 禁止断言 model 的字段值等于构造时传入的值。Pydantic model 只能测三件事：(1) 非法输入是否抛 `ValidationError`；(2) `model_dump()` / `model_dump_json()` 的输出结构；(3) 与 JSON Schema 契约的对齐。
- **mock 只能打在 I/O 边界。** LLM / embedding / rerank 测试 mock `httpx.AsyncClient.post`，数据库测试用内存 SQLite，文件测试用 `tmp_path`。禁止 mock `_chat_completion`、`_embedding_request`、`_rerank_request` 等内部函数。pipeline 编排测试允许 mock stage 入口（`run_criteria` / `run_retrieval` 等），但必须在 `MOCK_ALLOWED_INTERNAL` 白名单中显式声明。`test_architecture_layers.py` 中的 AST 守卫会自动检查违规。
- **纯函数必须有直接测试。** 每个包含复杂逻辑的纯函数（如 `_passes_hard_filters`、`_normalize_intent_payload`、`_deterministic_rerank`、`_has_negation_prefix`），必须至少有 3 个直接测试用例（正常路径 + 2 个边界），不得通过调用上层 pipeline 间接测试。

---

## 熵增 Kill Switch

出现以下情况必须暂停开发、先重构：

| Kill Switch | 含义 | 典型表现 |
|------------|------|---------|
| 同一语义出现 3+ 表示方式 | AI 编码最常见的熵源 | 同一"价格范围"叫 budget_range / price_filter / cost_constraint |
| 同一 event type 出现条件分支语义 | SSE 铁律的预警信号 | ThinkingEvent 既表示 UI 加载又表示 Agent 推理状态 |
| 单个 prompt 超 300 行 | 可量化红线 | prompt 里堆叠了多种业务规则和特殊情况 |
| 无法画出清晰数据流 | 架构崩坏的核心信号 | 某个功能的数据来源和去向说不清楚 |
| 单品类 constraints 超 8 个 `| None` 字段 | 封闭 DSL 的健康指标 | DSL 设计过于膨胀，需要重新审视品类约束维度 |
| 含复杂逻辑的纯函数缺少直接测试 | 测试覆盖的核心信号 | `_passes_hard_filters`、`_deterministic_rerank` 等函数没有 ≥3 个直接用例 |
| mock 目标不在 I/O 边界白名单 | 同义反复测试的根因 | `monkeypatch.setattr` 打在 `_chat_completion` 而非 `httpx.AsyncClient.post` |

---

## Agent 工作指引

当你收到开发任务时：

0. **先看现状**：读 `doc/status/backend-completion.md` 了解当前完成度，CLAUDE.md 的 P0/P1/P2 是计划快照，不是当前状态
1. **先读文档**：判断任务属于哪个 doc 文件的职责范围
2. **再读契约**：查看 PRD 中对应的接口/事件/数据模型定义
3. **思考再动手**：按 Linus 三问过滤过度设计
4. **检查风险**：扫一眼卡点清单，避免已知的坑
5. **最小实现**：只解决当前问题，不加抽象层和灵活性
6. **不破坏已有**：已有契约和接口不能随意改动
7. **评审维度校验**：当前改动服务于哪个评审维度(35%/25%/20%/20%)？如果不服务任何维度，别做
8. **状态文档同步**：后端功能开发完成后，必须更新 `doc/status/backend-completion.md` 中对应功能的状态。新增功能需在清单中追加新行。只改状态和备注，不改结构。
9. **修复 BUG**：先看本质。必须通过梳理 Data Flow 与 State Translation 锁定root cause，严禁在未看清状态演变前盲目打补丁。

如果你不确定某件事，直接问用户。不要臆测。
