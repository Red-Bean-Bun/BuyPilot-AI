# BuyPilot-AI 项目开发指引
## 项目概要

**BuyPilot-AI**：基于 RAG 的多模态电商智能导购 Agent
**赛事**：ByteDance AI Full-Stack Challenge（3 周 / 3 人）
**一句话定位**：把用户模糊购物需求转化为可解释决策路径的亲子玩具专家级决策智能体

| 决策项 | 选择 |
|--------|------|
| 品类 | 亲子玩具（Toys & Games），深水区验证品类 |
| 客户端 | Android 原生（Kotlin + Jetpack Compose + OkHttp SSE 直连） |
| LLM | **双轨并行**：火山引擎 Doubao（意图识别主力）+ 百炼 Qwen（生成主力） |
| 后端 | Python FastAPI + PostgreSQL + pgvector + SQLModel |
| 流式协议 | SSE（OkHttp SSE 直连 FastAPI `/chat/stream`） |
| 模型切换 | 配置层 dict + call_llm() 函数，按任务类型选 primary/fallback |

### 模型-任务映射（双轨策略）

| 任务 | Primary | Fallback | 原因 |
|------|---------|----------|------|
| 意图识别/路由 | Doubao-Seed-2.0-lite | Qwen-Turbo | 意图识别不需要很强，Doubao免费额度高 |
| 购买标准生成 | Qwen-Plus | Doubao | 核心亮点，JSON schema强约束输出，Qwen-Plus更稳定 |
| 推荐解释生成 | Qwen-Plus | Doubao | 解释需要高质量 |
| 多模态图片理解 | Qwen-VL-Plus | 无 | Doubao VL生态不确定，Qwen-VL-Plus成熟 |
| Embedding | text-embedding-v3 (1024维) | Doubao-embedding-vision | 百炼有Key+维度确定 |
| Rerank | gte-rerank (百炼) | 无 | Doubao无对应服务 |

> Doubao API 配置见 `.env.example`（含 BaseAPI / Model / Key / Limit）。不限制具体使用模型，可自由选择。

---

## 评审权重对齐（所有 P0/P1/P2 划分的底层逻辑）

| 评审维度 | 权重 | 评审要点 |
|----------|------|---------|
| 基础功能完整性 | **35%** | 端到端链路跑通：对话→RAG检索→模型生成→流式返回→商品卡片展示 |
| 工程质量 | **25%** | 代码结构清晰、接口设计合理、错误处理完善、文档齐全 |
| 效果与可靠性 | **20%** | 流畅、美观、无Bug；检索准确率、无幻觉、复杂场景处理 |
| 加分项深度 | **20%** | 选做但要有深度，做精一项胜过浅尝三项 |

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
- 前端表格附录 → `doc/prd/01-附录-表格内容.md`（错误码、状态机、渲染策略、测试用例等）
- 后端接口契约 → `doc/prd/02-后端与AgentPRD.md`（数据库 Schema、管道编排、API 端点）
- 后端 Prompt 模板 → `backend/prompts/`（运行时 LLM prompt，开发时直接读取使用）
- 产品定义 → `PRODUCT.md`（用户画像、产品目的、设计原则、Anti-references）
- 设计系统 → `DESIGN.md`（颜色、字体、间距、圆角、组件规范，开发 UI 时必须遵循）
- 风险清单 → `doc/risk/卡点与风险清单.md`（开发前必读）
- 设计决策说明 → `design-decisions.md`（每个核心决策3句话：选了什么/为什么/反过来会怎样）

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

### P0/P1/P2 — 评审权重驱动

**P0 = 基础功能完整性(35%) + 工程质量底线(25%)**
- 评委拿起App能走通完整链路：输入文字 → 意图识别 → 购买标准生成(初版) → 检索 → 推荐生成(初版) → SSE流式返回 → 商品卡片渲染
- docker-compose up + Android Studio Run 一键启动
- 依赖版本锁定(requirements.txt固定版本) + 核心逻辑注释(RAG链路/Prompt构造)

**P1 = 效果与可靠性(20%) + 加分项启动(20%)**
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

- **80条**商品数据（50-100范围内偏多，覆盖3-4个玩具子类目）
- **双数据源**：导师提供的脱敏电商数据（兼容通用5字段）+ 自主构造80条（含玩具专属字段）
- 玩具专属字段必须有：age_min/age_max、safety_features、education_dimensions、requires_battery、play_scenario——没有这些字段，硬过滤和反选排除就是假的
- 数据质量比数据数量重要

### 止损规则（评审权重驱动）

| 时间节点 | 验收标准 | 否则 |
|----------|---------|------|
| 第 7 天 | **评委拿起App能走通完整链路** + 一键启动 | 砍掉所有P1/P2，只追P0到能跑为止 |
| 第 14 天 | **4条Demo路径全部可演示** + 无幻觉无Bug | 砍掉复杂后台和增强功能 |
| 第 18 天 | 冻结功能 + design-decisions.md完成 | 只修Bug、打磨体验、准备答辩 |

### 减分项防御

| 减分项 | 防御措施 |
|--------|---------|
| AI编造商品/价格/优惠（幻觉） | 混合检索+硬过滤+证据绑定 |
| 纯Web/H5替代原生App | Android原生开发 |
| Demo无法运行或需大量手动配置 | docker-compose up一键启动 + README运行说明 |
| 代码完全依赖AI生成无法解释原理 | design-decisions.md + 答辩前自读一遍，能脱口而出每个核心决策的why |

### 四条 Demo 路径

| # | Demo路径 | 核心演示能力 | 对应官方场景 |
|---|---------|-------------|-------------|
| 1 | "推荐适合4岁孩子的益智玩具" | 模糊推荐→意图识别→购买标准→推荐 | 单轮模糊推荐 + 条件筛选 |
| 2 | 上传玩具包装图+"这个适合3岁孩子吗？有吞咽风险吗？" | 多模态VL→RAG安全知识→避坑推荐 | 拍照找货 |
| 3 | 首轮推荐后"不要电池款"+"预算降到150" | 多轮约束变化+反选排除 | 多轮追问 + 反选排除 |
| 4 | "把这个加到购物车" | 对话式CRUD+结构化数据操作 | 购物车⭐入门 |

---

## 技术栈总览

### 客户端（Android）

| 组件 | 技术 | 用途 |
|------|------|------|
| UI 框架 | Jetpack Compose | 声明式 UI |
| 聊天容器 | 自研 LazyColumn + ChatUiNode 抽象 | 消息列表/输入框/卡片流 |
| 流式通信 | OkHttp SSE | 直连 FastAPI `/chat/stream` |
| 状态管理 | ViewModel + StateFlow | UI 状态 |
| 本地缓存 | Room | 会话/卡片/反馈/购物车缓存 |
| 图片采集 | Android Photo Picker | 图片选择 |
| 图片加载 | Coil Compose | 图片渲染 |

### 后端（Python）

| 组件 | 技术 | 用途 |
|------|------|------|
| Web 框架 | FastAPI | HTTP + SSE |
| ORM | SQLModel | 数据库模型 |
| 向量检索 | pgvector | 文档向量检索 |
| LLM 接入 | 双轨：火山引擎 Doubao + 百炼 Qwen | 意图/生成/多模态 |
| Embedding | text-embedding-v3 (1024维, 百炼) | 文档向量化 |
| Rerank | gte-rerank (百炼) | 检索重排 |
| 模型切换 | 配置层 dict + call_llm() | 按任务类型选primary/fallback |
| 部署 | Docker Compose | FastAPI + PostgreSQL |

---

## 核心数据流（Agent 编码时必读）

```
用户输入 (Android)
  ↓
意图识别 (Doubao primary / Qwen-Turbo fallback) + 槽位检查
  ↓ add_to_cart意图 → cart_action事件 → 加购操作
  ↓ 需要澄清时 → clarification 事件（多问题模式，每题带 suggested_options）
  ↓ 并行
购买标准生成 (Qwen-Plus primary / Doubao fallback) → 约束列表 → 展平为 CriteriaPayload  |  投机检索 (embedding + 硬过滤)
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

## Agent 工作指引

当你收到开发任务时：

1. **先读文档**：判断任务属于哪个 doc 文件的职责范围
2. **再读契约**：查看 PRD 中对应的接口/事件/数据模型定义
3. **思考再动手**：按 Linus 三问过滤过度设计
4. **检查风险**：扫一眼卡点清单，避免已知的坑
5. **最小实现**：只解决当前问题，不加抽象层和灵活性
6. **不破坏已有**：已有契约和接口不能随意改动
7. **评审维度校验**：当前改动服务于哪个评审维度(35%/25%/20%/20%)？如果不服务任何维度，别做

如果你不确定某件事，直接问用户。不要臆测。
