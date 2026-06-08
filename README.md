# BuyPilot-AI

多模态电商导购 Agent：把模糊的购物需求转化为可解释的决策路径。

**核心能力**：
- 多品类检索（美妆/数码/服饰/食品，100 商品）
- 多模态理解（拍照找货，Qwen-VL-Plus）
- 对话式交互（意图澄清、标准生成、推荐解释）
- 混合检索（硬过滤 + 向量召回 + Rerank）
- 流式响应（SSE，支持多商品对比、购物车加购）

---

## 技术决策

| 决策项 | 选择 |
|--------|------|
| 品类 | 多品类（美妆护肤/数码电子/服饰运动/食品生活），官方脱敏数据（100 条，4 品类 × 25） |
| 客户端 | Android 原生（Kotlin + Jetpack Compose + OkHttp SSE 直连） |
| LLM | **百炼主力 + Doubao 兜底**：Qwen-Turbo 做意图/标准/推荐/决策主力；Qwen-VL-Plus 做图片理解 |
| 后端 | Python FastAPI + PostgreSQL + pgvector + SQLModel |
| 流式协议 | SSE（OkHttp SSE 直连 FastAPI `/chat/stream`，10 种事件类型） |
| Embedding | text-embedding-v3（1024 维，百炼），确定性 fallback 仅用于开发态 |
| Rerank | qwen3-rerank（百炼 DashScope API） |
| 降级策略 | 仅保留 LLM provider fallback；embedding/rerank/retrieval 显性失败；运行时必须使用 PostgreSQL + pgvector |
| SSE 管道 | async generator stage 模式（推荐文案与检索后台并行） |
| LLM 调用 | task-oriented interface + Profile 配置驱动（YAML）+ PromptStore 运行时加载 |
| 图片上传 | multipart `/upload/image`，本地 jpg + 上传转 data URL 进入 Qwen-VL 多模态理解 |
| 数据库 | SQLModel 自动建表，含 cart_items/eval_runs/retrieval_traces/evidence_links 等 |

完整决策记录 → [doc/decisions/](doc/decisions/) · 设计决策 → [design-decisions.md](design-decisions.md)

---

## 快速开始

### 前置要求

- Docker Desktop 已安装并运行
- 百炼 API Key（访问 https://bailian.console.aliyun.com/ 获取）
- 项目根目录有写权限

### 启动步骤

```bash
# 1) 准备配置
cp .env.example .env
# 编辑 .env，填写 BAILIAN_API_KEY=sk-your-real-key

# 2) 启动服务
#    首次启动会自动：建表 → 入库商品 → 生成 text embedding → 生成 image embedding
#    启动耗时约 2-5 分钟（取决于网络和 API 响应）
make rebuild

# 3) 验证启动
make db-stats     # 检查数据完整性
make smoke        # 运行端到端验证
```

### 预期输出

**`make db-stats`** 应输出：
```
products: 100
chunks: 1292
image_embeddings: 100
```

**`make smoke`** 应输出：
```
[PASS] All 6 demo scenarios passed
```

如果输出不符合预期，运行 `make reset` 全量重置后重试。

### 常见问题

| 问题 | 解决方案 |
|------|---------|
| `BAILIAN_API_KEY 未配置` | 编辑 `.env`，填写 `BAILIAN_API_KEY=sk-xxx` |
| `make rebuild` 卡住 | 首次启动需要生成 embedding，耐心等待 2-5 分钟 |
| `image_embeddings: 0` | 运行 `make seed-image` 手动构建图片索引 |
| 端口 5432/8000 被占用 | 修改 `deploy/docker-compose.yml` 端口映射 |
| 服务启动超时（unhealthy） | 检查 `.env` 中的 API Key 是否正确，网络是否可达 |

---

## 运维命令

所有命令从**项目根目录**执行：

```bash
make help          # 显示所有命令
make rebuild       # 重建镜像并启动
make db-stats      # 查看数据库统计
make smoke         # 运行端到端验证
make reset         # 删库 + 重建（全量重置）
make seed-image    # 手动构建图片 embedding 索引
make logs          # 查看 API 日志
make shell         # 进入容器 shell
```

---

## 项目结构

### 文档层（`doc/`）

```
doc/
├── strategy/          战略与决策
├── prd/               产品需求文档（前后端 PRD）
├── decisions/         架构决策记录
├── status/            完成状态
├── risk/              风险预判
├── prompts/           提示词工具包
├── research/          调研参考
└── ui/                设计稿
```

### 代码层

```
├── android/                     # Android 客户端（Kotlin + Compose）
│   └── app/src/main/java/com/buypilot/
│       ├── ui/                  # Compose 组件 + cards/
│       ├── viewmodel/           # ViewModel + StateFlow
│       ├── network/             # OkHttp SSE + API 调用
│       ├── data/                # Room DAO + Entity
│       ├── model/               # ChatUiNode sealed interface
│       └── util/
│
├── backend/                     # FastAPI 后端
│   ├── src/
│   │   ├── api/                 # HTTP 路由（chat/cancel/upload/feedback/cart/admin）
│   │   ├── runtime/             # SSE 管道编排 + stages/
│   │   ├── services/            # 业务逻辑（LLM gateway/RAG/embedding/retriever/eval）
│   │   ├── repos/               # 数据持久化（SQLModel + pgvector）
│   │   ├── types/               # DTO、SSE 事件定义、Schema
│   │   ├── config/              # settings + llm_profiles.yaml + domain_terms
│   │   └── middleware/          # 请求上下文中间件
│   ├── prompts/                 # 运行时 Prompt 模板（7 个 .md，PromptStore 加载）
│   └── tests/                   # 按层对应的测试 + fixtures/
│
├── data/                        # 数据层
│   ├── raw/ecommerce_agent_dataset/   # 官方 100 条脱敏电商数据（4 品类 × 25）
│   └── eval/eval_samples.json         # 评测样本
│
├── contracts/                   # 接口契约
│   ├── sse-events.schema.json       # SSE 事件 JSON Schema（source of truth）
│   ├── frontend-integration.md      # 前后端接口对照
│   └── examples/                    # Golden trace 示例（.sse）
│
├── deploy/                      # 部署配置
│   ├── docker-compose.yml           # PostgreSQL + pgvector + FastAPI
│   └── docker-compose.cloudflare.yml # Cloudflare Tunnel
│
├── scripts/                     # 开发工具（非运行时组件）
│   ├── stress_test.py               # 压测脚本
│   ├── auto-deploy.sh               # 生产 CD 脚本（cron 驱动）
│   └── check_sse_protocol.py        # SSE 协议一致性检查
│
├── Makefile                     # 运维命令入口（make help 查看全部）
│
└── .github/workflows/           # CI（pytest + ruff）
```

---

## 核心数据流

```
用户输入（文字或图片）
  ↓
意图识别 + 槽位检查
  ↓ 需要澄清时 → clarification 事件
  ↓
购买标准生成  ←→  投机检索
  ↓
混合检索：硬过滤 + 向量召回 + Rerank
  ↓
推荐解释生成 + 证据绑定
  ↓
SSE 事件流：
  thinking → criteria_card → text_delta → product_card → final_decision → done
  ↓
Android 卡片渲染
  ↓
用户反馈（标准调整/排除/加购/对比）→ 下一轮推荐
```

---

## SSE 事件协议

共 10 种事件类型，完整定义见 `contracts/sse-events.schema.json`：

| 事件 | 用途 |
|------|------|
| `thinking` | Agent 思考中，前端显示加载状态 |
| `clarification` | 需要用户澄清（槽位缺失） |
| `criteria_card` | 购买标准卡片 |
| `text_delta` | 流式文本增量 |
| `product_card` | 商品卡片 |
| `compare_card` | 多商品对比卡片 |
| `cart_action` | 购物车操作（加购/删除） |
| `final_decision` | 最终决策 |
| `done` | 本轮对话结束 |
| `error` | 错误信息 |

---

## 开发指南

### 后端开发

```bash
cd backend
uv sync --extra dev
uv run uvicorn src.api.app:app --reload --port 8000
```

### 测试

```bash
# 单元测试
uv run pytest -q

# 端到端验证（需要 Postgres/pgvector + 真实 API Key）
uv run -m src.scripts.smoke_live_rag
```

### 架构约束

- **分层依赖**：API → Runtime → Service → Repo → Config/Types（禁止反向依赖）
- **SSE 协议**：修改事件类型必须三端对齐（Schema → Python → Kotlin）
- **LLM 调用**：必须通过 task-oriented interface，禁止在 Runtime 层直接调用 SDK
- **数据库**：运行时必须使用 PostgreSQL + pgvector，SQLite 仅用于 pytest 隔离测试

详见 `CLAUDE.md`。

---

## 文档索引

| 文档 | 用途 |
|------|------|
| `CLAUDE.md` | 开发指引（架构约束、编码规范） |
| `doc/prd/` | 产品需求（前后端 PRD） |
| `doc/decisions/` | 架构决策记录 |
| `doc/status/` | 完成状态 |
| `contracts/sse-events.schema.json` | SSE 事件契约 |
| `design-decisions.md` | 核心设计决策 |

---

## 演示场景

4 条端到端 Demo 路径：

1. **模糊推荐**："推荐适合油皮的洗面奶，200 元以内"
2. **拍照找货**：上传商品图片 + "这个适合敏感肌吗？"
3. **多轮对话**："不要含酒精的防晒霜" + "预算降到 200"
4. **对话式加购**："把这个加到购物车"

运行 `make smoke` 验证所有场景。
