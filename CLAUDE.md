# BuyPilot-AI 项目开发指引
## 项目概要

**BuyPilot-AI**：基于 RAG 的多模态电商智能导购 Agent
**赛事**：ByteDance AI Full-Stack Challenge（3 周 / 3 人）
**一句话定位**：把用户模糊购物需求转化为可解释决策路径的亲子玩具专家级决策智能体

| 决策项 | 选择 |
|--------|------|
| 品类 | 亲子玩具（Toys & Games），深水区验证品类 |
| 客户端 | Android 原生（Kotlin + Jetpack Compose + OkHttp SSE 直连） |
| LLM | 百炼平台（Qwen-Turbo / Qwen-Plus / Qwen-VL-Plus / text-embedding-v3 / gte-rerank） |
| 后端 | Python FastAPI + PostgreSQL + pgvector + SQLModel |
| 流式协议 | SSE（OkHttp SSE 直连 FastAPI `/chat/stream`） |

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
- 编码风格和架构原则 → `doc/prompts/03-AI编码助手系统提示词.md`
- 前端接口契约 → `doc/prd/01-Android前端PRD.md`（SSE 事件类型、Kotlin data class、卡片规范）
- 后端接口契约 → `doc/prd/02-后端与AgentPRD.md`（8 张表 Schema、管道编排、API 端点）
- 风险清单 → `doc/risk/卡点与风险清单.md`（开发前必读）

---

## 编码原则（来自 03-AI编码助手系统提示词）

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

### AGENTS.md 分层架构

```
UI → Runtime → Service → Repo → Config/Types
```

- 依赖只能自上而下流动，禁止反向/横向/循环依赖
- 业务逻辑只能在 Service 层
- 副作用（IO/DB/网络/外部 API）必须可定位、可替换、可测试
- 所有边界必须显式类型化，禁止隐式 dict 和无 schema JSON
- 配置集中管理，禁止 `os.getenv()` 散落在业务代码中

---

## 项目核心约束

### 绝对不做（明确裁剪）

- 支付与订单系统
- 商家入驻与商品管理后台
- Multi-Agent 蜂群架构
- 长期用户画像系统
- 全量真实电商平台接入
- Celery + Redis 异步任务
- JWT 复杂认证
- 完整商家后台（订单/库存/支付/权限）

### 止损规则

| 时间节点 | 状态要求 | 否则 |
|---------|---------|------|
| 第 7 天 | 端到端主链路跑通（输入 → SSE → 卡片） | 砍掉所有 P2 |
| 第 14 天 | 稳定主链路（多模态 + 反馈 + 混合检索） | 砍掉复杂后台和增强功能 |
| 第 18 天 | 冻结大功能 | 只修 bug、做评测、打磨答辩 |

### 三条 Demo 路径

1. **预算型导购**："给4岁孩子买室内益智玩具，预算200，不要小零件"
2. **图片输入识别**：上传玩具包装图 + "这个适合3岁孩子吗？有吞咽风险吗？"
3. **对比/避坑**：首轮推荐后点击"不要电池款" → 系统调整推荐 + 避坑说明

---

## 技术栈总览

### 客户端（Android）

| 组件 | 技术 | 用途 |
|------|------|------|
| UI 框架 | Jetpack Compose | 声明式 UI |
| 聊天容器 | 自研 LazyColumn + ChatUiNode 抽象 | 消息列表/输入框/卡片流 |
| 流式通信 | OkHttp SSE | 直连 FastAPI `/chat/stream` |
| 状态管理 | ViewModel + StateFlow | UI 状态 |
| 本地缓存 | Room | 会话/卡片/反馈缓存 |
| 图片采集 | Android Photo Picker | 图片选择 |
| 图片加载 | Coil Compose | 图片渲染 |

### 后端（Python）

| 组件 | 技术 | 用途 |
|------|------|------|
| Web 框架 | FastAPI | HTTP + SSE |
| ORM | SQLModel | 数据库模型 |
| 向量检索 | pgvector | 文档向量检索 |
| LLM 接入 | 百炼 OpenAI 兼容接口 | 意图/生成/多模态 |
| Embedding | text-embedding-v3 (1024 维) | 文档向量化 |
| Rerank | gte-rerank | 检索重排 |
| 部署 | Docker Compose | FastAPI + PostgreSQL |

---

## 核心数据流（Agent 编码时必读）

```
用户输入 (Android)
  ↓
意图识别 (Qwen-Turbo) + 槽位检查
  ↓ 需要澄清时 → clarification 事件（多问题模式，每题带 suggested_options）
  ↓ 并行
购买标准生成 (Qwen-Plus) → 约束列表 → 展平为 CriteriaPayload  |  投机检索 (embedding + 硬过滤)
  ↓
混合检索：硬过滤(SQL) + 向量召回(pgvector) + Rerank(gte)
  ↓
推荐解释生成 (Qwen-Plus) + 证据绑定
  ↓
SSE 事件流（每事件必带 seq + session_id）：
  thinking → criteria_card → text_delta(message_id+done) → product_card → final_decision → done(criteria_id+total_products)
  ↓
Android Compose + LazyColumn 卡片渲染
  ↓
用户反馈（quick_actions: criteria_patch/feedback/open_evidence/compare） → feedbacks 表 → 下一轮推荐注入约束
```

> 统一 SSE 事件协议详见 `contracts/sse-event-protocol-v1.md`

---

## Agent 工作指引

当你收到开发任务时：

1. **先读文档**：判断任务属于哪个 doc 文件的职责范围
2. **再读契约**：查看 PRD 中对应的接口/事件/数据模型定义
3. **思考再动手**：按 Linus 三问过滤过度设计
4. **检查风险**：扫一眼卡点清单，避免已知的坑
5. **最小实现**：只解决当前问题，不加抽象层和灵活性
6. **不破坏已有**：已有契约和接口不能随意改动

如果你不确定某件事，直接问用户。不要臆测。
