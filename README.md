# BuyPilot-AI

基于 RAG 的多模态电商智能导购 Agent —— 字节跳动 AI 全栈挑战赛参赛项目

**一句话定位**：把用户模糊购物需求转化为可解释决策路径的多品类智能导购决策智能体

## 技术决策

| 决策项 | 选择 |
|--------|------|
| 品类 | 多品类（美妆护肤/数码电子/服饰运动/食品生活），导师提供官方数据 |
| 客户端 | Android 原生（Kotlin + Jetpack Compose + OkHttp SSE 直连） |
| LLM | 双轨并行：火山引擎 Doubao（意图识别主力）+ 百炼 Qwen（生成/多模态/Embedding/Rerank 主力） |
| 后端 | Python FastAPI + PostgreSQL + pgvector + SQLModel |
| 流式协议 | SSE（OkHttp SSE 直连 FastAPI `/chat/stream`） |
| 商品数据源 | 导师官方脱敏电商数据（100条，4品类×25） |
| 图片处理 | 使用官方数据自带本地 jpg |
| 商品筛选 | 不需要，官方数据直接入库 |
| 前端架构 | 1 人负责，sealed interface + ChatUiNode |
| 后端架构 | 2 人（主开发 + 算法），分模块（routers/services/models/db） |
| SSE 管道 | async generator stage 模式 |
| 降级策略 | cascading fallback（跳过意图 → 放宽检索 → 坦诚告知） |
| 图片上传 | 两步走（先 `/upload/image` 后 `/chat/stream`） |
| LLM 调用 | Profile 配置驱动（YAML） |
| 开发环境 | Docker Compose 统一（PG + pgvector + FastAPI） |
| 演示环境 | 本地笔记本 + WiFi |
| 数据库 | 9 张表一次性建好（含 cart_items） |

完整决策记录 → [doc/decisions/决策记录.md](doc/decisions/决策记录.md)

---

## 目录结构

### 文档层（`doc/`）

```
doc/
├── strategy/          战略与决策          ← 先读 02；01 为 Pivot 前历史稿
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
├── research/          调研参考           ← 队友原始调研
│   └── 队友原始调研-多模态电商导购.md
├── risk/              风险预判           ← 卡点清单，开发前必读
│   └── 卡点与风险清单.md
└── prompts/           提示词工具包       ← DeepResearch / AI Coding 用
    ├── 01-DeepResearch提示词.md
    ├── 02-DeepResearch提示词拆分.md
    └── 03-AI编码助手系统提示词.md
```

### 代码层

```
├── android/                     ← Android 客户端（MVVM 风格）
│   └── app/src/main/java/com/buypilot/
│       ├── ui/                  ← Compose 组件（UI 层）
│       │   ├── components/      ← 可复用组件
│       │   │   └── cards/       ← Criteria/Product/FinalDecision/Clarification 卡片
│       │   └── theme/           ← 主题
│       ├── viewmodel/           ← ViewModel + StateFlow
│       ├── network/             ← OkHttp SSE + API 调用
│       ├── data/                ← Room DAO + Entity
│       ├── model/               ← Kotlin data class（ChatUiNode sealed interface）
│       └── util/                ← 工具类
│
├── backend/                     ← FastAPI 后端（严格 AGENTS.md 分层）
│   ├── src/
│   │   ├── types/               ← DTO, Schema, 数据契约
│   │   ├── config/              ← 集中配置（环境变量、LLM Profile YAML）
│   │   ├── repos/               ← 数据持久化（SQLModel, pgvector）
│   │   ├── services/            ← 业务逻辑（意图/标准/检索/推荐/视觉/反馈）
│   │   ├── runtime/             ← SSE 管道编排（async generator stage 链）
│   │   │   └── stages/          ← 意图/标准/检索/推荐 各阶段
│   │   └── api/                 ← FastAPI routers
│   └── tests/                   ← 按层对应的测试
│
├── data/                        ← 数据层
│   ├── official/                ← 导师官方 100 条脱敏电商数据
│   ├── images/                  ← 官方数据自带本地商品图片
│   ├── processed/               ← 入库前清洗/metadata 推断结果
│   └── scripts/                 ← 官方 JSON 入库与评测样本生成脚本
│
├── eval/                        ← 评测
│   ├── datasets/                ← 测试样本
│   └── scripts/                 ← RAGAS 评测脚本
│
└── deploy/                      ← 部署
    └── docker-compose.yml       ← PG + pgvector + FastAPI
```

## 文档角色定位

| 文档 | 谁看 | 什么时候看 |
|------|------|-----------|
| **02-策略研究报告** | 全员 | 当前战略与架构选型依据 |
| **01-比赛背景与战略决策** | 全员 | 历史背景参考，已被 2026-05-20 Pivot 覆盖 |
| **决策记录** | 全员 | 开发前 + 有分歧时对照 |
| **01-Android前端PRD** | 前端开发 | 开工前 + 联调时对照 |
| **02-后端与AgentPRD** | 后端/Agent开发 | 开工前 + 联调时对照 |
| **卡点与风险清单** | 全员 | 每天开发前扫一眼 |
| **01-DeepResearch提示词** | 需要调研时 | 按需要跑对应 prompt |
| **02-DeepResearch提示词拆分** | 需要调研时 | 按 4 批次顺序跑 |
| **03-AI编码助手系统提示词** | AI Coding 工具 | 编码时作为 context |
| **队友原始调研** | 了解队友思路 | 需要时参考 |

## 团队分工

| 角色 | 人数 | 职责 |
|------|------|------|
| 前端 Android | 1 | Jetpack Compose + OkHttp SSE + LazyColumn + ChatUiNode |
| 后端主开发 | 1 | FastAPI 路由 + SSE 管道编排 + 接口契约 + 全链路串联 |
| 后端算法/检索 | 1 | LLM Profile 封装 + pgvector + Rerank + Prompt 调优 |

## 核心数据流

```
用户输入 (Android)
  ↓
意图识别 (Doubao primary / Qwen-Turbo fallback) + 槽位检查
  ↓ add_to_cart意图 → cart_action事件 → 购物车状态更新
  ↓ 并行
购买标准生成 (Qwen-Plus primary / Doubao fallback)  |  投机检索 (embedding + 硬过滤)
  ↓
混合检索：硬过滤(SQL) + 向量召回(pgvector) + Rerank(gte)
  ↓
推荐解释生成 (Qwen-Plus) + 证据绑定
  ↓
SSE 事件流：thinking → clarification → criteria_card → text_delta → product_card → cart_action → final_decision → done
  ↓
Android Compose + LazyColumn 卡片渲染
  ↓
用户反馈 → feedbacks 表 → 下一轮推荐注入约束
```

## 止损规则

| 时间节点 | 状态要求 | 否则 |
|---------|---------|------|
| 第 7 天 | 端到端主链路跑通（输入 → SSE → 卡片） | 砍掉所有 P2 |
| 第 14 天 | 稳定主链路（多模态 + 反馈 + 混合检索） | 砍掉复杂后台和增强功能 |
| 第 18 天 | 冻结大功能 | 只修 bug、做评测、打磨答辩 |
