# BuyPilot-AI

基于 RAG 的多模态电商智能导购 Agent —— 字节跳动 AI 全栈挑战赛参赛项目

**一句话定位**：把用户模糊购物需求转化为可解释决策路径的多品类智能导购决策智能体

## 技术决策

| 决策项 | 选择 |
|--------|------|
| 品类 | 多品类（美妆护肤/数码电子/服饰运动/食品生活），导师提供官方数据 |
| 客户端 | Android 原生（Kotlin + Jetpack Compose + OkHttp SSE 直连） |
| LLM | **双轨并行**：火山引擎 Doubao（意图识别主力）+ 百炼 Qwen（生成/多模态/Embedding/Rerank 主力） |
| 后端 | Python FastAPI + SQLite（日常开发）/ PostgreSQL + pgvector（演示） + SQLModel |
| 流式协议 | SSE（OkHttp SSE 直连 FastAPI `/chat/stream`） |
| 商品数据 | 导师官方脱敏电商数据（100条，4品类×25） |
| 图片处理 | 本地 jpg + 上传转 data URL 进入 Qwen-VL 多模态理解 |
| 前端架构 | 1 人负责，sealed interface + ChatUiNode |
| 后端架构 | 2 人（主开发 + 算法），AGENTS.md 分层（API → Runtime → Service → Repo → Config/Types） |
| SSE 管道 | async generator stage 模式（推荐文案与决策后台并行） |
| 降级策略 | 仅保留 LLM provider fallback；embedding/rerank/retrieval/state/audit 默认显性失败；SQLite 是持久化向量后端兼容，不是 memory fallback |
| 图片上传 | multipart `/upload/image` |
| LLM 调用 | task-oriented interface + Profile 配置驱动（YAML）+ PromptStore 运行时加载 |
| 开发环境 | `uv run uvicorn`（默认 SQLite）；`deploy/docker-compose.yml`（Postgres + pgvector 演示） |
| 数据库 | SQLModel 自动建表，含 cart_items/eval_runs/eval_samples/retrieval_traces/evidence_links |

完整决策记录 → [doc/decisions/决策记录.md](doc/decisions/决策记录.md) · 设计决策 → [design-decisions.md](design-decisions.md)

---

## 当前状态

| 指标 | 状态 |
|------|------|
| 测试 | `136 passed`（2026-05-26） |
| CI | pytest + ruff check/format（`.github/workflows/backend-tests.yml`） |
| Demo Smoke | 6/6 场景通过（Postgres/pgvector + 真实模型） |
| 后端完成度 | P0 完成 / P1 基本完成 / P2 部分完成 → [详细状态](doc/status/backend-completion.md) |
| 交接文档 | [260524-handoff.md](260524-handoff.md) |

---

## 目录结构

### 文档层（`doc/`）

```
doc/
├── strategy/          战略与决策          ← 01 为 Pivot 前历史稿；先读 02
│   ├── 01-比赛背景与战略决策.md
│   └── 02-策略研究报告.md
├── prd/               产品需求文档        ← 开工依据，定义"做什么"
│   ├── 01-Android前端PRD.md
│   ├── 01-附录-表格内容.md
│   ├── 02-后端与AgentPRD.md
│   └── 03-后端同步变更说明.md
├── decisions/         架构决策记录        ← 开发过程中的关键决策
│   ├── 决策记录.md
│   ├── 2026-05-20-官方文档对齐迭代.md
│   └── 2026-05-20-数据品类Pivot.md
├── status/            完成状态            ← AI 每次开发后更新
│   ├── backend-completion.md
│   └── demo-readiness.md
├── risk/              风险预判            ← 卡点清单，开发前必读
│   └── 卡点与风险清单.md
├── prompts/           提示词工具包        ← DeepResearch / AI Coding 用
│   ├── 01-DeepResearch提示词.md
│   ├── 02-DeepResearch提示词拆分.md
│   └── 03-AI编码助手系统提示词.md
├── research/          调研参考
│   └── 队友原始调研-多模态电商导购.md
└── ui/                设计稿
    ├── README.md
    └── *.png           (7 张界面截图)
```

### 代码层

```
├── android/                     ← Android 客户端（Kotlin + Compose）
│   └── app/src/main/java/com/buypilot/
│       ├── ui/                  ← Compose 组件
│       │   ├── components/      ← 可复用组件 + cards/
│       │   └── theme/
│       ├── viewmodel/           ← ViewModel + StateFlow
│       ├── network/             ← OkHttp SSE + API 调用
│       ├── data/                ← Room DAO + Entity
│       ├── model/               ← ChatUiNode sealed interface
│       └── util/
│
├── backend/                     ← FastAPI 后端
│   ├── src/
│   │   ├── types/               ← DTO、SSE 事件、Schema
│   │   ├── config/              ← settings.py + llm_profiles.yaml + tuning.py + domain_terms.py
│   │   ├── repos/               ← 数据持久化（SQLModel、pgvector）
│   │   ├── services/            ← 业务逻辑（LLM gateway/fallbacks/prompts + RAG + eval）
│   │   ├── runtime/             ← SSE 管道编排 + stages/
│   │   └── api/                 ← FastAPI routers（chat/cancel/upload/feedback/cart/admin_eval）
│   ├── prompts/                 ← 运行时 Prompt 模板（7 个 .md，PromptStore 加载）
│   ├── tests/                   ← 按层对应的测试 + fixtures/
│   └── reports/                 ← smoke 测试报告
│
├── data/                        ← 数据层
│   ├── raw/ecommerce_agent_dataset/  ← 导师官方 100 条脱敏电商数据（4品类×25）
│   ├── processed/                    ← products.json + chunks.json（入库前清洗产物）
│   ├── eval/eval_samples.json        ← 评测样本（15 条）
│   └── scripts/process_data.py       ← 原始数据加工脚本
│
├── contracts/                   ← 接口契约
│   ├── sse-events.schema.json       ← SSE 事件 JSON Schema（source of truth）
│   ├── frontend-integration.md      ← 前后端接口对照
│   └── examples/                    ← Golden trace 示例（3 个 .sse）
│
├── deploy/                      ← 部署
│   └── docker-compose.yml           ← PG + pgvector + FastAPI
│
└── .github/workflows/           ← CI
    └── backend-tests.yml             ← pytest + ruff
```

## 快速开始

### 评委验收路径（推荐）

```bash
# 1) 准备真实模型配置：复制模板到项目根目录 .env，并填写 BAILIAN_API_KEY。
cp .env.example .env

# 2) 启动 Postgres/pgvector + FastAPI；首次启动会自动建表、入库并生成 1024 维 embedding。
cd deploy
docker-compose up --build

# 3) 另开终端执行 live RAG 门禁；输出每个阶段的 JSON 状态，任一阶段失败会非 0 退出。
cd ../backend
uv run -m src.scripts.smoke_live_rag

# 4) 执行完整 Demo smoke。
uv run -m src.scripts.demo_smoke
```

说明：本地 SQLite 只作为日常开发路径；答辩/评委验收优先使用 `deploy/docker-compose.yml` 的 PostgreSQL + pgvector 环境。

### 开发路径

```bash
# 后端
cd backend
uv run uvicorn src.api.app:app --reload --port 8000

# 测试
uv run pytest -q

# Live RAG 验真（答辩必过门禁，需 Postgres/pgvector + 真实 API Key）
uv run -m src.scripts.smoke_live_rag

# Demo smoke（6 条 Demo 路径端到端，需 Postgres/pgvector + 真实 API Key）
uv run -m src.scripts.demo_smoke

# Docker Compose 演示环境
cd deploy && docker-compose up
```

## 文档角色定位

| 文档 | 谁看 | 什么时候看 |
|------|------|-----------|
| **CLAUDE.md** | AI 编码工具 + 全员 | 每次开发前 |
| **260524-handoff.md** | 接手开发者 | 接手时第一份读 |
| **backend/README.md** | 后端开发者 | 了解后端全景地图 |
| **doc/status/backend-completion.md** | 全员 | 了解当前完成度 |
| **doc/status/demo-readiness.md** | 全员 | Demo 准备状态 |
| **02-策略研究报告** | 全员 | 当前战略与架构选型依据 |
| **决策记录** | 全员 | 开发前 + 有分歧时对照 |
| **01-Android前端PRD** | 前端开发 | 开工前 + 联调时对照 |
| **02-后端与AgentPRD** | 后端/Agent开发 | 开工前 + 联调时对照 |
| **contracts/sse-events.schema.json** | 前后端 | SSE 事件字段变更时对照 |
| **卡点与风险清单** | 全员 | 每天开发前扫一眼 |
| **design-decisions.md** | 全员 | 答辩前自读 |

## 团队分工

| 角色 | 人数 | 职责 |
|------|------|------|
| 前端 Android | 1 | Jetpack Compose + OkHttp SSE + LazyColumn + ChatUiNode |
| 后端主开发 | 1 | FastAPI 路由 + SSE 管道编排 + 接口契约 + 全链路串联 |
| 后端算法/检索 | 1 | LLM Profile 封装 + pgvector + Rerank + Prompt 调优 + 评测 |

## 核心数据流

```
用户输入 (Android)
  ↓
意图识别 (Doubao primary / Qwen-Turbo fallback) + 槽位检查
  ↓ add_to_cart意图 → cart_action事件 → 购物车状态更新
  ↓ 需要澄清时 → clarification 事件
  ↓ 并行
购买标准生成 (Qwen-Plus primary / Doubao fallback)  |  投机检索 (embedding + 硬过滤)
  ↓
混合检索：硬过滤(SQL) + 向量召回(pgvector/SQLite) + Rerank(qwen3-rerank)
  ↓
推荐解释生成 (Qwen-Plus) + 证据绑定
  ↓
SSE 事件流：thinking → clarification → criteria_card → text_delta → product_card → cart_action → final_decision → done
  ↓
Android Compose + LazyColumn 卡片渲染
  ↓
用户反馈 → feedbacks 表 → 下一轮 retrieval 硬过滤
```

## 止损规则

| 时间节点 | 状态要求 | 否则 |
|---------|---------|------|
| 5/27（第 7 天） | 评委拿起App能走通完整链路 + 一键启动 | 砍掉所有P1/P2，只追P0 |
| 6/03（第 14 天） | 4条Demo路径全部可演示 + 无幻觉无Bug | 砍掉复杂后台和增强功能 |
| 6/07（第 18 天） | 冻结功能 + design-decisions.md完成 | 只修Bug、打磨体验、准备答辩 |
