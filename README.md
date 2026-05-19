# BuyPilot-AI

基于 RAG 的多模态电商智能导购 Agent —— 字节跳动 AI 全栈挑战赛参赛项目

**一句话定位**：把用户模糊购物需求转化为可解释决策路径的亲子玩具专家级决策智能体

## 技术决策

| 决策项 | 选择 |
|--------|------|
| 品类 | 亲子玩具（Toys & Games），深水区验证品类 |
| 客户端 | Android 原生（Kotlin + Jetpack Compose + OkHttp SSE 直连） |
| LLM | 百炼平台（Qwen-Turbo / Qwen-Plus / Qwen-VL-Plus / text-embedding-v3 / gte-rerank） |
| 后端 | Python FastAPI + PostgreSQL + pgvector + SQLModel |
| 流式协议 | SSE（OkHttp SSE 直连 FastAPI `/chat/stream`） |
| 商品数据源 | Amazon 多模态 RAG 数据集（6488 条 Toys & Games） |
| 图片处理 | 全量下载存 `data/images/{uniq_id}.jpg` |
| 商品筛选 | Qwen-Turbo 批量打分 + 人工复核 Top 500 |
| 前端架构 | 1 人负责，sealed interface + ChatUiNode |
| 后端架构 | 2 人（主开发 + 算法），分模块（routers/services/models/db） |
| SSE 管道 | async generator stage 模式 |
| 降级策略 | cascading fallback（跳过意图 → 放宽检索 → 坦诚告知） |
| 图片上传 | 两步走（先 `/upload/image` 后 `/chat/stream`） |
| LLM 调用 | Profile 配置驱动（YAML） |
| 开发环境 | Docker Compose 统一（PG + pgvector + FastAPI） |
| 演示环境 | 本地笔记本 + WiFi |
| 数据库 | 8 张表一次性建好 |

完整决策记录 → [doc/decisions/决策记录.md](doc/decisions/决策记录.md)

---

## 目录结构

```
doc/
├── strategy/          战略与决策          ← 先读这里，理解"为什么做"
│   ├── 01-比赛背景与战略决策.md
│   └── 02-策略研究报告.md
├── prd/               产品需求文档        ← 开工依据，定义"做什么"
│   ├── 01-Android前端PRD.md
│   └── 02-后端与AgentPRD.md
├── decisions/         架构决策记录        ← 开发过程中的关键决策
│   └── 决策记录.md
├── research/          调研参考           ← 队友原始调研
│   └── 队友原始调研-多模态电商导购.md
├── risk/              风险预判           ← 卡点清单，开发前必读
│   └── 卡点与风险清单.md
└── prompts/           提示词工具包       ← DeepResearch / AI Coding 用
    ├── 01-DeepResearch提示词.md
    ├── 02-DeepResearch提示词拆分.md
    └── 03-AI编码助手系统提示词.md
```

## 文档角色定位

| 文档 | 谁看 | 什么时候看 |
|------|------|-----------|
| **01-比赛背景与战略决策** | 全员 | 第一天，定调 |
| **02-策略研究报告** | 全员 | 架构选型前 |
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
意图识别 (Qwen-Turbo) + 槽位检查
  ↓ 并行
购买标准生成 (Qwen-Plus)  |  投机检索 (embedding + 硬过滤)
  ↓
混合检索：硬过滤(SQL) + 向量召回(pgvector) + Rerank(gte)
  ↓
推荐解释生成 (Qwen-Plus) + 证据绑定
  ↓
SSE 事件流：thinking → criteria_card → text_delta → product_card → final_decision → done
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
