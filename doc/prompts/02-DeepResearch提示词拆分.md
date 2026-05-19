<title>提示词拆分</title>

你的细化版 prompt 内容已经很完整，但问题也正因为太完整：它把**赛题理解、开源调研、技术选型、架构设计、代码契约、三周排期、答辩包装**全塞进了一次 DeepResearch 里。

模型会像被塞进年终总结会议的人类一样，表面点头，实际开始糊弄。

你的 prompt 里已经明确了几个关键边界：

客户端确定为 **iOS Swift 原生 / SwiftUI 优先**，系统包含 **用户端 + 轻量 Web 后台 + 后端服务**，

后台不做订单、库存、支付、商家入驻等完整电商后台，只服务于商品数据、知识库和评测闭环。

这个边界非常适合拆成多个专项研究任务，而不是一次全量吞下。

下面给你一套**可直接复制使用的 DeepResearch 分段提示词包**。建议按顺序跑，不要一次全跑。前几轮做“找材料与决策”，后几轮做“系统整合与执行”。

---

# 一、推荐拆分方式

拆成 12 个提示词：

1. **总策略与 MVP 边界**
2. **iOS SwiftUI 用户端开源组件调研**
3. **后端 FastAPI 与流式服务调研**
4. **RAG 框架、向量数据库、混合检索调研**
5. **Rerank 与多模态模型 API 调研**
6. **轻量 Web 管理后台与评测看板调研**
7. **商品数据集与知识库构造调研**
8. **评测框架与指标体系调研**
9. **开源胶水集成风险与 License 风险调研**
10. **最终技术选型与架构整合**
11. **代码级 API / Schema / Prompt 契约生成**
12. **三周执行计划与答辩包装**

前 1-9 个可以部分并行，10-12 必须等前面结果出来后再跑。否则就是让 DeepResearch 先预言未来，再引用证据，人类学术界已经示范过这条路通向哪里了。

---

# 二、公共上下文块

每次单独跑 DeepResearch 时，建议先贴这一段，再贴下面对应的专项提示词。

```Plain Text
公共上下文：

我们正在参加 ByteDance AI Full-Stack Challenge / AI 全栈挑战赛。

课题：
Agent 智能体：实现一个基于 RAG 的多模态电商智能导购 Agent。

目标：
3 人组队，在 3 周内完成一个高完成度、可演示、可答辩、可评测的比赛作品。

赛题要求：
1. 基于 iOS 原生框架、Node.js/Python/Go 后端服务、向量数据库和大模型 OpenAPI，构建“意图理解 - 智能咨询 - 决策辅助”的电商 AI 导购系统。
2. 支持上传非结构化商品详情与营销文档，构建专属知识库。
3. 使用 RAG 保证回复专业性和准确性。
4. 客户端需要接近“豆包”的流式交互体验。
5. 支持商品卡片实时渲染。
6. 支持多模态输入，至少包括文字和图片。
7. 构建端到端质量评测与反馈闭环。
8. 评估回答准确率、知识检索精度、多轮对话逻辑，并反哺 Prompt 策略优化与知识库迭代。

团队约束：
1. 3 人团队。
2. 开发周期 3 周。
3. 使用 AI Coding / vibe coding。
4. 不适合从 0 完整开发所有模块。
5. 优先使用成熟开源项目、模板、组件做胶水集成。
6. 客户端已确定采用 iOS Swift 原生开发，优先 SwiftUI。
7. 系统按三部分设计：
   - 用户端：iOS Swift 原生导购 App，负责文字/图片输入、流式对话、商品卡片实时渲染、swipe 反馈、商品详情和最终决策卡。
   - 后台端：轻量 Web 管理后台，负责商品结构化数据维护、商品详情/营销文档上传、知识库切分与入库状态查看、用户反馈查看、评测任务触发与评测结果可视化。
   - 后端服务：负责会话管理、意图理解、购买标准生成、商品结构化检索、RAG、rerank、多模态解析、流式输出、反馈记录和评测接口。
8. 后台端不是完整商家后台，不做订单、库存、支付、商家入驻、多角色审核等复杂电商能力。

产品战略：
1. 不做真正全品类泛电商。
2. 前台可以包装成“泛电商导购 Agent”，后台优先聚焦 3C / 数码配件 / 平板 / 耳机 / 手机 / 键盘鼠标等高参数、强对比、易结构化品类。
3. 不做普通聊天机器人。
4. 核心不是“聊天”，而是“购物决策 Agent”。
5. 用户购物的本质不是搜索商品，而是降低决策不确定性。
6. 用户缺的不是更多信息，而是判断标准。
7. 系统核心能力包括：
   - 用户意图理解
   - 用户约束抽取
   - 购买标准生成
   - 商品结构化检索
   - 非结构化文档 RAG
   - rerank
   - 可解释推荐
   - 商品卡片实时渲染
   - 用户反馈收集
   - 质量评测闭环
8. 不能只靠向量检索。
9. 商品参数、价格、品牌、规格走结构化检索。
10. 商品详情、FAQ、营销文档、评价摘要走语义检索。
11. 最终采用 hybrid retrieval：
    - SQL / JSON filter / metadata filter
    - vector retrieval
    - rerank
    - LLM 生成推荐解释

MVP 交互：
保留：
- 文字输入
- 图片输入
- 流式对话
- 商品卡片
- 左滑不喜欢
- 右滑喜欢
- 点击详情
- 最终决策卡

暂缓：
- 上滑/下滑复杂手势
- 复杂抽屉
- 长期用户画像
- 完整商家后台
- 全量真实电商接入
- 复杂多 Agent 编排炫技

研究要求：
1. 输出中文。
2. 每个关键结论必须给出来源链接或证据。
3. 优先引用 GitHub 仓库、官方文档、官方 release note、license、论文、技术文档。
4. 避免空泛表述。
5. 必须站在 3 人、3 周、比赛交付负责人角度做取舍。
6. 稳定交付优先级高于技术炫技。
```

---

# 提示词 1：总策略与 MVP 边界

用途：先让 DeepResearch 判断“比赛到底该做什么、不该做什么”。这一步不是找 GitHub 项目，而是定战略边界。

```Plain Text
请基于公共上下文，进行一次“比赛策略与 MVP 边界”专项研究。

目标：
帮助 3 人团队在 3 周内完成一个基于 RAG 的多模态电商智能导购 Agent。请不要泛泛讲 AI Agent，而是判断什么范围最可能在比赛中赢，什么范围必须砍掉。

请重点回答：

1. 赛题真正考察什么？
   - 工程完整度？
   - 端到端交付？
   - AI Coding 使用能力？
   - RAG 能力？
   - 多模态能力？
   - 原生客户端体验？
   - 评测闭环？
   - 业务理解？

2. 我们当前战略是否合理？
   - 前台包装泛电商，后台聚焦 3C / 数码 / 外设高参数品类，是否合理？
   - “决策辅助”优先于“商品搜索”，是否合理？
   - “购买标准生成”作为核心模块，是否合理？
   - 混合检索优先于纯向量检索，是否合理？
   - 轻量后台只做知识库和评测闭环，是否合理？

3. 最小可赢 MVP 应该是什么？
   请区分：
   - 最小可运行版本
   - 最小可演示版本
   - 最小可赢版本

4. 请列出 P0 / P1 / P2 功能。
   P0 必须能在第 7 天跑通端到端。
   P1 必须能在第 14 天前完成。
   P2 只在前面顺利时再做。

5. 请列出必须砍掉的功能。
   对每个砍掉项说明：
   - 为什么 3 周内不做
   - 不做会不会影响答辩
   - 如何在答辩中解释这个取舍

6. 请给出 3 条最强 Demo 场景：
   - 预算型导购
   - 图片输入识别
   - 商品对比 / 避坑

7. 请给出最终一句话定位。
   例如：
   “我们不是做 AI 客服，而是做一个把模糊购物需求转化为可解释决策路径的多模态导购 Agent。”

输出格式：
1. 执行摘要
2. 赛题本质判断
3. 产品战略评估
4. 最小可赢 MVP
5. P0/P1/P2 功能表
6. 必砍功能表
7. 三条 Demo 路径
8. 最大风险与止损策略
9. 最终建议

要求：
- 必须明确判断，不要说“都可以”。
- 必须从 3 人、3 周、vibe coding、开源胶水集成角度做取舍。
- 不需要大量技术细节，重点是战略和边界。
```

---

# 提示词 2：iOS SwiftUI 用户端开源组件调研

用途：专门调研 iOS 用户端。避免 DeepResearch 又跑去研究 Android、Flutter、React Native，像技术选型自助餐一样拿满一盘。

```Plain Text
请基于公共上下文，进行一次“iOS SwiftUI 用户端开源组件与实现方案”专项 Deep Research。

研究目标：
为比赛用户端选择可快速复用的 SwiftUI / UIKit 开源组件、示例代码和官方能力，帮助 3 人团队在 3 周内完成 iOS 原生导购 App。

用户端必须支持：
1. 文字输入
2. 图片选择、预览、上传
3. 流式对话展示，接近豆包式响应体验
4. 商品卡片实时渲染
5. swipe card 交互：
   - 左滑不喜欢
   - 右滑喜欢
   - 点击详情
6. 商品详情页
7. 最终决策卡
8. SSE 或 WebSocket 接收后端流式响应

请重点研究：

A. SwiftUI 聊天 UI / Message List
- 支持流式增量消息渲染
- 支持 Markdown 或富文本
- 支持图片消息
- 支持自定义消息气泡
- 是否适合魔改为导购旁白

B. SwiftUI swipe card / Tinder-like card stack
- 是否支持 DragGesture
- 是否可控制左滑/右滑反馈
- 是否易于嵌入商品卡片
- 是否维护活跃
- 是否适合比赛快速集成

C. 商品卡片 UI
- 是否有 SwiftUI card component 示例
- 是否适合展示图片、价格、参数、推荐理由、风险标签
- 是否可做实时插入和动画

D. 图片选择与预览
- PhotosPicker
- PHPickerViewController
- 相机/相册能力
- 图片压缩、上传 multipart/form-data
- 和 SwiftUI 集成难度

E. 流式通信
- URLSession SSE 支持方式
- Swift EventSource 开源库
- Starscream WebSocket
- 是否推荐 SSE 还是 WebSocket
- 对比赛场景哪个更稳

F. iOS App 架构
- SwiftUI + MVVM 是否足够
- 是否需要 Combine / async-await
- 网络层如何组织
- 状态管理如何避免复杂化

请输出候选项目清单，每个项目包括：
1. 项目名称
2. GitHub URL / 官方文档 URL
3. 能力类别：聊天 UI / swipe card / 图片选择 / SSE / WebSocket / 卡片组件 / 架构模板
4. 技术栈：SwiftUI / UIKit / Swift Package / CocoaPods 等
5. License
6. 最近维护活跃度：
   - 最近 commit
   - release
   - issue/PR 活跃度
7. 接入难度：低 / 中 / 高
8. 魔改难度：低 / 中 / 高
9. 对本项目复用方式：
   - 直接集成
   - 复制部分代码
   - 参考实现
   - 不推荐
10. 风险
11. 推荐指数 1-5
12. 推荐理由

请至少给出：
- 聊天 UI 组件或示例 3-5 个
- swipe card 组件或示例 3-5 个
- SSE/WebSocket 方案 3-5 个
- 图片选择/上传方案 2-4 个
- SwiftUI 架构示例 2-3 个

最后请给出：
1. 最推荐的 iOS 用户端技术组合
2. 页面结构建议：
   - 首页 / 任务输入页
   - 聊天 + 卡片流页
   - 商品详情页
   - 候选商品页
   - 最终决策卡页
3. 第 1 周 iOS 端最小可演示目标
4. 哪些 UI 功能必须砍掉
5. 为什么这套方案适合 3 周比赛

要求：
- 不要重点研究 Android、Flutter、React Native。
- 如果引用非 iOS 技术，只能作为交互参考，不能作为主路线。
- 必须给来源链接。
```

---

# 提示词 3：后端 FastAPI 与流式服务调研

用途：专门解决后端骨架、SSE、文件上传、任务接口。不要让它在这个阶段纠缠 RAG 平台大比拼。

```Plain Text
请基于公共上下文，进行一次“Python FastAPI 后端服务模板与流式接口”专项 Deep Research。

研究目标：
找到适合 3 周比赛快速落地的后端服务架构和开源模板。后端需要支撑 iOS 用户端、轻量 Web 后台、RAG pipeline、流式输出、文件上传、反馈记录和评测接口。

请重点研究：

1. FastAPI 是否适合作为本项目主后端？
   请与 Node.js / Go 简要比较，但重点研究 FastAPI。

2. FastAPI 开源模板
   需要覆盖：
   - 项目目录结构
   - SQLAlchemy / SQLModel
   - Alembic migration
   - PostgreSQL
   - Docker Compose
   - 文件上传
   - 后台管理接口
   - 简单鉴权
   - 日志
   - 环境变量管理

3. 流式输出方案
   比较：
   - SSE
   - WebSocket
   - HTTP chunked streaming
   对本项目场景判断：
   - iOS 端接入复杂度
   - 服务端实现复杂度
   - 稳定性
   - 断线重连
   - 是否适合 LLM token streaming
   - 是否适合同时发送 text_delta / card_delta / status_update

4. 文件上传方案
   需要支持：
   - 商品文档 PDF / Markdown / TXT / HTML
   - 商品图片
   - 用户上传购物截图
   - 后台上传营销文档
   - multipart/form-data
   - 文件存储策略：本地文件、对象存储、数据库 metadata

5. 后端模块划分
   请设计：
   - auth 可选简化
   - tasks
   - chat
   - cards
   - products
   - documents
   - retrieval
   - feedback
   - evaluation
   - admin

6. 后端部署
   - Docker Compose
   - PostgreSQL
   - pgvector 或外部 Qdrant
   - Redis 是否必要
   - 本地开发与云端部署的最小方案

请输出：

1. FastAPI 与 Node.js / Go 对比表
2. 推荐 FastAPI 模板清单，每个包括：
   - 名称
   - GitHub URL
   - 技术栈
   - License
   - 最近维护情况
   - 接入难度
   - 魔改难度
   - 是否适合本项目
3. SSE / WebSocket 选择结论
4. 后端目录结构建议
5. API 初版设计：
   - POST /tasks
   - POST /chat/stream
   - POST /upload
   - GET /cards
   - POST /feedback
   - POST /evaluate
   - GET /evaluation/results
   - admin 相关接口
6. 流式消息事件设计：
   - text_delta
   - status_update
   - card_delta
   - evidence_delta
   - final_decision
   - error
7. 第 1 周后端最小可演示目标
8. 最大风险和止损策略

要求：
- 不要泛泛介绍 FastAPI。
- 必须围绕比赛交付。
- 优先推荐文档完整、Docker 友好、PostgreSQL 友好的模板。
- 每个关键结论必须给来源链接。
```

---

# 提示词 4：RAG 框架、向量数据库、混合检索调研

用途：核心技术专项。让它聚焦“结构化商品检索 + 文档向量检索 + rerank”的链路。

```Plain Text
请基于公共上下文，进行一次“RAG 框架、向量数据库与混合检索方案”专项 Deep Research。

研究目标：
为基于 RAG 的多模态电商智能导购 Agent 选择最适合 3 周比赛的 RAG 技术组合。

核心判断：
本项目不能只靠向量检索。商品价格、品牌、型号、参数、规格必须走结构化检索；商品详情、FAQ、营销文档、评价摘要走语义检索；最后通过 rerank 与 LLM 生成可解释推荐。

请重点研究：

1. RAG 框架
比较：
- LangChain
- 其他适合比赛快速集成的 RAG 项目

评价维度：
- 是否适合嵌入自定义 FastAPI 后端
- 是否适合商品导购场景
- 是否支持文件上传
- 是否支持文档切分
- 是否支持 metadata filter
- 是否支持 query rewrite
- 是否支持 rerank
- 是否支持多轮对话
- 是否容易输出结构化 JSON
- 是否容易和商品结构化数据库结合
- 是否支持私有部署
- 魔改难度
- License
- 3 周交付风险

2. 向量数据库 / 检索组件
比较：
- PostgreSQL + pgvector
- Milvus
- 其他适合比赛快速集成的 检索组件 项目

评价维度：
- 3 周比赛部署难度
- 本地开发体验
- Docker Compose 支持
- 文档质量
- Python FastAPI 集成难度
- metadata filter 支持
- 是否适合商品结构化表 + 文档向量表混合检索
- 是否适合小规模 30-100 商品 + 数百文档 chunk
- 运维复杂度
- License

3. 混合检索架构
请设计一个适合本项目的 pipeline：
- 用户输入
- 意图识别
- 用户约束抽取
- 购买标准生成
- 查询改写
- 商品结构化过滤
- 文档向量召回
- metadata filter
- rerank
- evidence compression
- recommendation generation
- card JSON generation

4. 数据建模建议
请给出最小数据库结构：
- products
- product_attributes
- product_documents
- document_chunks
- embeddings / vector column
- tasks
- messages
- feedback
- evaluations

5. 文档切分策略
请比较：
- fixed-size chunk
- markdown heading-aware chunk
- product-level chunk
- attribute-aware chunk
- FAQ pair chunk
并给出比赛最稳方案。

6. 最终推荐组合
必须明确选择：
- RAG 框架
- 向量数据库
- 检索策略
- 文档切分策略
- metadata 设计
- 是否使用 all-in-one RAG 平台

输出格式：
1. 执行摘要
2. RAG 框架对比表
3. 向量数据库对比表
4. 推荐混合检索架构图，用 Mermaid
5. 数据模型建议
6. 文档切分策略
7. 最终技术组合
8. 不推荐方案及原因
9. 第 1 周可交付目标
10. 第 2 周增强目标

要求：
- 必须给来源链接。
- 必须明确判断，不要“都可以”。
- 请特别说明为什么不建议或建议使用 Dify / RAGFlow / FastGPT 作为主后端。
```

---

# 提示词 5：Rerank 与多模态模型 API 调研

用途：把模型 API、图片理解、rerank 单独研究。它们变化快，适合单独 DeepResearch。

```Plain Text
请基于公共上下文，进行一次“Rerank 与多模态模型 API”专项 Deep Research。

研究目标：
为 3 周比赛选择最适合快速集成的 rerank 与图片理解方案。

本项目需要：
1. 用户上传商品图、商品截图、购物页面截图。
2. 系统识别商品类型、品牌、型号、风格、关键文字、可能意图。
3. 图片理解结果进入导购任务解析链路。
4. 检索结果需要通过 rerank 重新排序，提高推荐准确性和可解释性。

请研究：

A. Rerank 方案
比较：
- bge-reranker
- Cohere Rerank
- Jina Reranker
- Voyage Rerank
- LlamaIndex / LangChain 内置 rerank
- 其他可快速接入的 rerank 服务或本地模型

评价维度：
- 中文效果
- 英文资料成熟度
- 接入复杂度
- API 成本
- 本地部署成本
- 是否支持商品导购场景
- 是否适合 30-100 商品小规模 Demo
- 和 FastAPI / LangChain / LlamaIndex 集成难度
- 3 周比赛风险
- License / 商用限制

B. 多模态图片理解 API
比较：
- OpenAI 视觉模型 API
- Qwen-VL / Qwen2.5-VL API 或开源方案
- Gemini 视觉模型 API
- Claude 视觉能力
- 其他可快速集成方案

评价维度：
- 图片商品识别能力
- 中文输入输出能力
- OCR 能力
- 截图理解能力
- API 稳定性
- 成本
- 接入复杂度
- 是否支持结构化 JSON 输出
- 是否适合比赛 Demo
- 是否需要本地 GPU
- 风险

C. 图片理解输出 schema
请设计一个简洁 JSON：
- image_type
- detected_category
- detected_brand
- detected_model
- visual_features
- extracted_text
- inferred_intent
- confidence
- followup_question

D. 和导购链路的集成方式
请说明：
- 图片输入如何进入 task parsing
- 如何和用户文字输入合并
- 什么时候只做 OCR
- 什么时候调用视觉大模型
- 如何避免视觉模型幻觉
- 如何把图片识别结果转成检索条件

输出格式：
1. 执行摘要
2. Rerank 方案对比表
3. 多模态模型 API 对比表
4. 最终推荐 rerank 方案
5. 最终推荐多模态方案
6. 图片理解 JSON schema
7. 集成流程图 Mermaid
8. 成本与风险分析
9. 3 周落地建议

要求：
- 必须引用官方文档、GitHub、模型说明或可靠资料。
- 必须给出明确推荐。
- 如果某方案很强但接入重，请明确降级为 P2。
```

---

# 提示词 6：轻量 Web 管理后台与评测看板调研

用途：后台单独研究。你的细化 prompt 里后台已经加进来了，这个模块非常容易被 DeepResearch 在大 prompt 里忽略。

```Plain Text
请基于公共上下文，进行一次“轻量 Web 管理后台 / 评测看板”专项 Deep Research。

研究目标：
为比赛设计一个轻量 Web 后台。后台不是完整商家后台，不做订单、库存、支付、商家入驻、多角色审核，只服务于：
1. 商品结构化数据维护
2. 商品详情 / 营销文档上传
3. 知识库切分状态查看
4. 向量入库状态查看
5. 用户反馈查看
6. 评测任务触发
7. 评测结果可视化
8. Prompt / RAG 策略版本对比

请比较以下方案：

1. Next.js / React + shadcn/ui + TanStack Table
2. FastAPI Admin
3. SQLAdmin
4. Starlette Admin
5. Streamlit
6. Gradio
7. Retool-like 开源替代方案
8. 简单自研 Web Admin 模板

评价维度：
- 3 周内开发速度
- 和 FastAPI / PostgreSQL 集成难度
- 表格 CRUD 能力
- 文件上传能力
- 图表展示能力
- 评测结果可视化能力
- Prompt / 策略版本对比展示能力
- 是否适合答辩演示
- 是否会引入过重前端复杂度
- 部署难度
- License 风险
- UI 观感
- 代码可控性

请输出：

1. 方案对比表
2. 每个方案的推荐指数 1-5
3. 最推荐的后台技术方案
4. 如果时间紧张，最小后台应做哪些页面：
   - 商品列表页
   - 商品编辑页
   - 文档上传页
   - 文档 chunk / 入库状态页
   - 用户反馈页
   - 评测任务页
   - 评测结果看板页
   - Prompt / RAG 策略版本对比页
5. 后台页面信息架构
6. 后台 API 设计建议：
   - GET /admin/products
   - POST /admin/products
   - PUT /admin/products/{product_id}
   - POST /admin/documents/upload
   - GET /admin/documents
   - GET /admin/chunks
   - GET /admin/feedback
   - POST /admin/evaluation/run
   - GET /admin/evaluation/results
   - GET /admin/evaluation/compare
   - GET /admin/prompt-versions
7. 第 1 周最小后台版本
8. 第 2 周增强版本
9. 第 3 周答辩展示版本

要求：
- 不要研究完整电商后台。
- 不要加入订单、支付、库存、商家入驻等功能。
- 必须给来源链接。
- 必须从“比赛展示效果 + 开发速度”角度做最终判断。
```

---

# 提示词 7：商品数据集与知识库构造调研

用途：解决“数据从哪里来”。比赛作品败在没数据上非常常见，毕竟模型不会凭空长出商品宇宙。

```Plain Text
请基于公共上下文，进行一次“电商商品数据集与知识库构造”专项 Deep Research。

研究目标：
为比赛快速构造一个适合 RAG 与导购决策的商品知识库。优先聚焦 3C / 数码配件 / 平板 / 耳机 / 手机 / 键盘鼠标等高参数品类。

数据目标：
1. 30-100 个商品样本。
2. 每个商品有结构化属性。
3. 每个商品有非结构化文档：
   - 商品详情
   - 营销文案
   - FAQ
   - 评价摘要
   - 避坑说明
4. 数据能支撑：
   - 预算型导购
   - 参数对比
   - 商品推荐解释
   - 商品避坑
   - RAG evidence
   - 评测样本构造

请研究：

1. 公开电商数据集
   - Amazon 商品数据
   - BestBuy / Cdiscount / Kaggle 电商商品数据
   - 中文电商商品数据
   - 商品评价数据
   - 3C 产品参数数据
   - 其他可合法使用的数据源

2. 数据许可证
   - 是否可以用于比赛 Demo
   - 是否可以公开展示
   - 是否需要引用来源
   - 是否存在爬虫风险

3. 数据构造方法
   请比较：
   - 使用公开数据集
   - 手工整理真实商品参数
   - 用 LLM 生成模拟商品数据
   - 真实参数 + LLM 生成营销文档 / FAQ / 评价摘要
   - 完全模拟商品品牌与参数
   - 混合方案

4. 推荐数据 schema
   请设计最小商品 schema：
   - product_id
   - category
   - brand
   - model
   - price
   - specs
   - selling_points
   - weaknesses
   - suitable_for
   - not_suitable_for
   - tags
   - image_url
   - source
   - documents

5. 非结构化文档模板
   请设计每个商品对应的文档模板：
   - 商品详情文档
   - 营销文档
   - FAQ
   - 用户评价摘要
   - 避坑说明
   - 对比说明

6. 评测数据构造
   请设计：
   - 预算型问题
   - 场景型问题
   - 对比型问题
   - 避坑型问题
   - 图片输入型问题
   - 多轮约束变化问题
   并说明如何从商品库生成 ground truth。

输出格式：
1. 执行摘要
2. 数据源候选清单
3. 数据源 License / 可用性分析
4. 推荐数据构造方案
5. 商品结构化 schema
6. 非结构化文档模板
7. 评测样本构造方法
8. 推荐的 30-100 商品数据范围
9. 数据风险与规避方法
10. 第一周如何最快生成可用数据

要求：
- 必须给来源链接。
- 如果数据许可证不清晰，请明确标注风险。
- 最终必须给出一个 3 周比赛最稳的数据方案。
```

---

# 提示词 8：评测框架与指标体系调研

用途：专门设计“评测闭环”。这应该成为答辩亮点，而不是最后一天 Excel 续命。

```Plain Text
请基于公共上下文，进行一次“RAG 导购 Agent 评测框架与指标体系”专项 Deep Research。

研究目标：
为比赛构建一个端到端质量评测与反馈闭环。评测结果需要能在答辩中展示，并能说明 Prompt、检索策略、知识库切分、rerank 优化后效果变好。

请比较以下框架：
- RAGAS
- DeepEval
- TruLens
- Phoenix / Arize Phoenix
- LangSmith
- LlamaIndex Evaluation
- 自建轻量评测脚本

评价维度：
- 3 周快速落地能力
- 是否支持中文
- 是否能评测 retrieval recall / recall@k
- 是否能评测 faithfulness / groundedness
- 是否能评测 answer correctness
- 是否能评测 context precision / context recall
- 是否能评测多轮对话一致性
- 是否能评测约束满足率
- 是否能评测 unsupported claim / 幻觉率
- 是否适合本地脚本运行
- 是否适合答辩看板展示
- 是否依赖云服务
- 接入 FastAPI 难度
- License / 成本风险

请设计本项目评测指标：

1. 意图识别准确率
2. 用户约束抽取准确率
3. 购买标准覆盖率
4. 检索命中率 Recall@K
5. Context Precision
6. Context Recall
7. 答案事实准确率
8. 引用证据覆盖率
9. 约束满足率
10. 多轮一致性
11. 幻觉率 / Unsupported Claim Rate
12. 推荐排序合理性
13. 用户反馈后推荐变化率

请设计评测闭环：
- 评测样本表
- 评测运行表
- 评测结果表
- Prompt 版本表
- RAG 策略版本表
- 每次变更后如何跑评测
- 如何对比 baseline / +metadata filter / +rerank / +prompt optimization
- 如何在 Web 后台展示曲线和对比表
- 如何把用户反馈转成新的评测样本

输出格式：
1. 执行摘要
2. 评测框架对比表
3. 推荐评测方案
4. 指标体系定义
5. 评测样本 JSON schema
6. 评测结果 JSON schema
7. 评测流程图 Mermaid
8. 后台看板展示建议
9. 第 1 周 / 第 2 周 / 第 3 周评测建设计划
10. 答辩时如何讲评测闭环

要求：
- 必须给来源链接。
- 必须明确哪些评测框架 P0 用，哪些 P2 用。
- 如果框架过重，请建议自建轻量脚本替代。
```

---

# 提示词 9：开源胶水集成风险与 License 风险调研

用途：专门研究“别把自己胶死”。这个提示词很有必要，因为开源集成不是捡现成，是捡债务。

```Plain Text
请基于公共上下文，进行一次“开源胶水集成风险与 License 风险”专项 Deep Research。

研究目标：
我们计划使用开源项目进行胶水集成，而不是从 0 开发。请从比赛交付负责人角度分析这种策略的风险、边界和答辩包装。

请回答：

1. 哪些模块适合直接使用开源？
   例如：
   - SwiftUI UI 组件
   - SSE/WebSocket 库
   - FastAPI 模板
   - RAG 框架
   - 向量数据库
   - 评测框架
   - 后台 UI 模板

2. 哪些模块不适合直接套开源，必须自研？
   请重点分析：
   - 购买标准生成
   - 商品卡片 JSON
   - 用户反馈驱动推荐调整
   - 商品结构化检索与 RAG 融合
   - 最终决策卡
   - 评测指标与样本设计
   - 答辩 Demo 编排

3. 哪些模块只能参考，不能直接套？
   例如：
   - all-in-one RAG 平台
   - 完整电商后台
   - 复杂 Agent 编排系统
   - 通用聊天 UI

4. 开源项目之间最可能出现哪些集成冲突？
   请逐项分析：
   - API 风格冲突
   - 数据结构冲突
   - 认证体系冲突
   - Docker 部署冲突
   - 前后端协议冲突
   - License 冲突
   - 依赖版本冲突
   - 数据迁移冲突
   - 流式输出格式冲突
   - 结构化 JSON 输出与 UI 渲染冲突

5. License 风险
   请解释常见 License 对比赛项目的影响：
   - MIT
   - Apache-2.0
   - BSD
   - GPL
   - AGPL
   - LGPL
   - Elastic License
   - 商业 API Terms

6. 如何避免变成“只是套壳”？
   请给出答辩表达：
   - 我们复用了什么
   - 我们自研了什么
   - 我们的系统集成创新在哪里
   - 我们如何围绕导购决策链路做工程优化
   - 我们如何通过评测证明有效

7. 请给出“开源使用记录表”模板。
   字段包括：
   - 项目名称
   - URL
   - License
   - 使用模块
   - 修改内容
   - 是否直接引入代码
   - 是否保留版权声明
   - 风险等级
   - 替代方案

输出格式：
1. 执行摘要
2. 适合直接复用的模块
3. 必须自研的模块
4. 只可参考的模块
5. 集成冲突风险表
6. License 风险表
7. 避免套壳的工程创新表述
8. 开源使用记录模板
9. 最终风险控制建议

要求：
- 必须给来源链接，尤其是 License 官方解释或项目 License 文件。
- 必须从 3 周比赛交付角度判断，不要做企业级合规长篇废话。
```

---

# 提示词 10：最终技术选型与架构整合

用途：在跑完前面调研之后，把结果收束成一套架构。这个提示词要喂前面各轮摘要。

```Plain Text
请基于公共上下文，以及我将提供的前序调研摘要，进行“最终技术选型与系统架构整合”。

你要做的不是继续发散调研，而是做取舍和整合。

前序调研摘要如下：
【在这里粘贴提示词 1-9 的关键结论，尤其是推荐项目、风险、最终组合】

请完成以下任务：

1. 给出最终推荐技术组合：
   - 用户端：
   - 后台端：
   - 后端：
   - 数据库：
   - 向量检索：
   - RAG 框架：
   - Rerank：
   - 多模态：
   - 评测：
   - 部署：
   - 数据集 / 数据构造：

2. 请给出“不推荐组合”
   例如：
   - 为什么不推荐完整 all-in-one RAG 平台作为主后端
   - 为什么不推荐真正泛品类
   - 为什么不推荐复杂多 Agent 编排
   - 为什么不推荐完整商家后台
   - 为什么不推荐过复杂前端交互

3. 请输出技术选型决策矩阵。
   维度包括：
   - 开发速度
   - 稳定性
   - 学习成本
   - 魔改成本
   - 部署难度
   - 中文支持
   - 多模态支持
   - 流式输出支持
   - RAG 能力
   - metadata filter 支持
   - rerank 支持
   - 评测支持
   - 移动端适配
   - 答辩亮点
   - License 风险
   - 综合推荐指数

4. 请设计最终系统架构。
   必须包含：
   - iOS SwiftUI 用户端
   - 轻量 Web 管理后台
   - FastAPI 后端
   - PostgreSQL / pgvector 或其他最终数据库
   - RAG pipeline
   - 多模态图片理解
   - rerank
   - 评测闭环
   - 用户反馈闭环

5. 请用 Mermaid 输出：
   - 总架构图
   - 用户请求流程图
   - RAG 检索流程图
   - 评测闭环流程图
   - 数据流图

6. 请给出模块边界：
   - iOS 端负责什么
   - Web 后台负责什么
   - 后端业务服务负责什么
   - Agent / RAG 模块负责什么
   - 评测模块负责什么

7. 请给出三人分工：
   A. iOS / 产品体验
   B. 业务后端 / 数据 / 轻量后台
   C. Agent / RAG / 评测

8. 请给出第 7 天、第 14 天、第 21 天应达到的系统状态。

输出格式：
1. 最终推荐结论
2. 技术组合总表
3. 不推荐方案
4. 决策矩阵
5. 系统架构
6. Mermaid 图
7. 模块边界
8. 三人分工
9. 阶段性交付目标
10. 最大风险与止损策略

要求：
- 必须做明确取舍。
- 不要再列十几种“都可以”的选择。
- 输出应能直接指导后续代码开发。
```

---

# 提示词 11：代码级 API / Schema / Prompt 契约生成

用途：让 DeepResearch 从“研究报告”进入“工程契约”。这个结果可以直接喂给 Cursor / Trae / Claude Code 之类工具。

```Plain Text
请基于公共上下文，以及最终技术架构，生成“代码级落地契约”。

最终技术架构如下：
【粘贴提示词 10 生成的最终推荐架构】

目标：
让 AI Coding 工具可以直接根据本契约生成后端、iOS 端、后台端的代码骨架。请优先简单稳定，不要设计企业级复杂架构。

请输出：

1. Monorepo 推荐目录结构
   包括：
   - apps/ios
   - apps/admin-web
   - services/api
   - services/rag
   - packages/shared-schema
   - data/sample-products
   - eval
   - deploy

2. 后端 API 设计
   用户端接口：
   - POST /tasks
   - GET /tasks/{task_id}
   - POST /chat/stream
   - GET /cards
   - GET /cards/{card_id}
   - POST /feedback
   - POST /upload/image

   后台端接口：
   - GET /admin/products
   - POST /admin/products
   - PUT /admin/products/{product_id}
   - POST /admin/documents/upload
   - GET /admin/documents
   - GET /admin/chunks
   - GET /admin/feedback
   - POST /admin/evaluation/run
   - GET /admin/evaluation/results
   - GET /admin/evaluation/compare
   - GET /admin/prompt-versions

3. 每个 API 请给出：
   - method
   - path
   - description
   - request JSON
   - response JSON
   - error format
   - 是否需要鉴权，比赛版可简化

4. SSE 消息协议
   请定义 event 类型：
   - status_update
   - text_delta
   - task_summary
   - card_delta
   - evidence_delta
   - final_decision
   - evaluation_hint
   - error
   每个 event 给出 JSON 示例。

5. 数据库表设计
   请给出字段：
   - products
   - product_documents
   - document_chunks
   - tasks
   - messages
   - cards
   - feedback
   - prompt_versions
   - rag_strategy_versions
   - evaluation_samples
   - evaluation_runs
   - evaluation_results

6. JSON Schema
   请设计：
   - Product
   - ProductCard
   - UserConstraint
   - BuyingCriteria
   - RAGEvidence
   - RecommendationResult
   - FinalDecisionCard
   - UserFeedback
   - EvaluationSample
   - EvaluationResult
   - ImageUnderstandingResult

7. Prompt 模板
   请生成：
   - 意图识别 Prompt
   - 用户约束抽取 Prompt
   - 购买标准生成 Prompt
   - 查询改写 Prompt
   - 图片理解 Prompt
   - 推荐生成 Prompt
   - 最终决策卡 Prompt
   - LLM-as-judge 评测 Prompt
   - 多轮一致性评测 Prompt
   - unsupported claim 检测 Prompt

8. RAG pipeline 伪代码
   请写出 Python 风格伪代码：
   - parse_user_input
   - extract_constraints
   - generate_buying_criteria
   - structured_filter_products
   - retrieve_documents
   - rerank_results
   - generate_cards
   - stream_response
   - record_feedback
   - run_evaluation

9. iOS 数据模型
   请给出 Swift struct 示例：
   - TaskSummary
   - ChatMessage
   - ProductCard
   - Evidence
   - FinalDecisionCard
   - FeedbackEvent
   - StreamEvent

10. 后台页面数据结构
   - ProductTableRow
   - DocumentUploadStatus
   - ChunkStatus
   - FeedbackRow
   - EvaluationDashboardData
   - StrategyCompareData

要求：
- 所有 schema 保持简单，足够 Demo 和答辩即可。
- 不要过度设计权限、支付、订单、库存。
- 输出应能直接交给 AI Coding 工具生成代码。
```

---

# 提示词 12：三周执行计划与答辩包装

用途：最后收束到排期和答辩。别让技术报告最后躺在 Notion 里像考古文献。

```Plain Text
请基于公共上下文、最终技术架构和代码级契约，生成“三人三周执行计划与答辩包装”。

最终技术架构：
【粘贴提示词 10 结果】

代码级契约：
【粘贴提示词 11 结果摘要】

团队角色：
A. iOS / 产品体验
- SwiftUI 用户端
- 流式对话
- 商品卡片
- 图片上传
- swipe card
- 最终决策卡
- Demo 体验

B. 业务后端 / 数据 / 轻量后台
- FastAPI 后端
- 商品数据模型
- 文档上传
- 任务接口
- 卡片接口
- 用户反馈
- 数据库
- 部署
- 轻量 Web 管理后台
- 商品库管理
- 知识库上传
- 反馈记录
- 评测结果看板

C. Agent / RAG / 评测
- 意图识别
- 购买标准生成
- RAG
- 混合检索
- rerank
- Prompt
- 多模态解析
- 评测脚本
- 评测指标
- 评测结果接口

请输出：

1. 三周总目标
   - 第 1 周：端到端跑通
   - 第 2 周：导购逻辑变强
   - 第 3 周：打磨、评测、答辩

2. 具体到天的开发计划
   Day 1 - Day 21
   每天必须包含：
   - A 的任务
   - B 的任务
   - C 的任务
   - 当天交付物
   - 当天联调点
   - 风险
   - 止损策略

3. 关键里程碑
   - Day 3
   - Day 7：必须可演示
   - Day 10
   - Day 14：冻结主要架构
   - Day 18：冻结大功能
   - Day 20-21：只修稳定性和答辩

4. 并行开发策略
   - 哪些接口先 mock
   - 哪些 schema 必须先冻结
   - iOS 如何使用 mock 数据并行开发
   - 后台如何先接静态评测数据
   - RAG 如何先用小数据集跑通

5. 风险清单
   请按严重程度排序：
   - iOS 流式渲染不稳定
   - swipe card 状态管理复杂
   - RAG 效果不好
   - 数据集不足
   - rerank 接入超时
   - 多模态 API 不稳定
   - Web 后台做太多
   - 部署失败
   - 最后两天还在加功能

6. 答辩包装
   请输出：
   - 一句话定位
   - 3 分钟 Demo 脚本
   - 5 分钟 Demo 脚本
   - 10 分钟 Demo 脚本
   - 技术亮点页结构
   - 架构页结构
   - 评测闭环页结构
   - 开源复用与自研创新页结构
   - 失败案例与反思页结构

7. 评委 Q&A
   请准备问题与回答：
   - 你们和普通聊天机器人有什么区别？
   - RAG 体现在哪里？
   - 为什么不是纯向量检索？
   - 多模态有什么实际作用？
   - 如何证明推荐效果变好了？
   - 开源项目用了哪些？
   - 你们自己的创新在哪里？
   - 为什么只聚焦 3C / 数码品类？
   - 如果推广到泛电商怎么做？
   - 3 周内如何保证稳定？

8. 最终行动清单
   请列出明天第一天必须做的 10 件事。

要求：
- 不要空泛鼓励。
- 每天交付物必须可验证。
- 第 7 天必须有可演示版本。
- 第 18 天后不允许新增大功能。
- 答辩包装必须突出“决策辅助、混合检索、购买标准生成、评测闭环”。
```

---

# 三、使用顺序建议

最省时间的跑法：

```Plain Text
第一批并行：
1 总策略与 MVP 边界
2 iOS 用户端
3 FastAPI 后端
4 RAG / 向量数据库
5 Rerank / 多模态
6 轻量后台
7 数据集
8 评测框架
9 开源风险

第二批整合：
10 最终技术选型与架构整合

第三批落地：
11 代码级契约

第四批执行：
12 三周计划与答辩包装
```

如果时间不够，最低限度跑这 5 个：

```Plain Text
1 总策略与 MVP 边界
2 iOS 用户端
4 RAG / 向量数据库
8 评测框架
10 最终技术选型与架构整合
```

但不跑后端和数据集调研，后面很容易出现“前端很美，后端像临时搭的木桥，数据像样板间假书”的惨状。

---

# 四、每轮 DeepResearch 后的压缩模板

每次 DeepResearch 输出通常会很长。为了后续整合，建议让它最后附一个“可转交摘要”。你可以在每个提示词末尾追加：

```Plain Text
最后请额外输出一个“供下一轮研究使用的压缩摘要”，不超过 800 字，包含：
1. 本轮最终推荐
2. 不推荐方案
3. 关键证据链接
4. 最大风险
5. 后续研究需要继承的约束
```

这很重要。否则第 10 轮整合时，你会把 9 份长报告一起塞进去，然后 DeepResearch 再次表演“阅读很多，理解很少”。

---

# 五、最关键的拆分原则

你的原 prompt 不是不好，而是它同时要求模型扮演：

- 调研员
- 架构师
- 产品经理
- iOS 工程师
- 后端工程师
- RAG 工程师
- 数据工程师
- 评测工程师
- 项目经理
- 答辩教练

这不是 DeepResearch，这是让一个虚拟顾问模拟一家创业公司，还不发工资。拆分后，每一轮只解决一个决策面，最后再收束，质量会明显更稳。

真正的产出链应该是：

```Plain Text
边界收缩
→ 模块调研
→ 技术选型
→ 架构整合
→ API / Schema 契约
→ 三周执行
→ 答辩包装
```

你们现在最需要的不是更多想法，而是把想法变成**可交付的约束系统**。比赛里，约束比灵感值钱。灵感通常负责制造事故。