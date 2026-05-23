# BuyPilot-AI 后端完成状态

> 最后核实：2026-05-23
> 维护方式：AI 完成后端功能开发后自动更新此文档，见 CLAUDE.md "Agent 工作指引" 第 8 条
> 核实命令：`backend/.venv/bin/python -m pytest -q`（84 passed）；`backend/.venv/bin/python -m src.scripts.smoke_live_rag`（需网络，live RAG 绿灯，product_card 已提前到 text_delta 前）；官方商品图 live 多模态 smoke 通过

---

## P0：基础功能完整性（评审权重 35%）

| # | 功能 | 状态 | PRD 章节 | 代码路径 | 备注 |
|---|------|------|---------|---------|------|
| 1 | SSE 流式对话端点 `/chat/stream` | ✅ 已完成 | 5.1, 5.2 | `api/chat.py` | StreamingResponse + format_sse，已验证可跑通 |
| 2 | SSE 事件协议（9 种事件类型） | ✅ 已完成 | 5.1 | `types/sse_events.py` | SSEEventBase + 9 个子类 + EventSeq + Constraints DSL，JSON Schema 已对齐 |
| 3 | 意图识别（6 种意图） | ✅ 已完成 | 4.1 | `services/llm_client.py` `stages/intent.py` | LLM primary + 关键词规则 fallback，含 `view_cart`/`add_to_cart`/`feedback` |
| 4 | 购买标准生成 | ✅ 已完成 | 4.1 | `services/llm_client.py` `stages/criteria.py` | LLM + 规则 fallback，含 `criteria_patch` 合并 + feedback 注入 |
| 5 | 推荐解释生成 | ✅ 已完成 | 4.1 | `services/llm_client.py` `stages/recommendation.py` | LLM + 规则 fallback，流式 text_delta |
| 6 | 最终决策生成 | ✅ 已完成 | 4.1 | `services/llm_client.py` `stages/decision.py` | LLM + 规则 fallback，含 winner/why/not_for/alternatives |
| 7 | 数据入库（100 商品 × 1092 chunk） | ✅ 已完成 | 3.5 | `repos/ingest.py` `scripts/reindex_embeddings.py` | `backend/buypilot-dev.db` 已入库，百炼 text-embedding-v3 1024 维；相对 SQLite URL 已统一解析到 backend 目录 |
| 8 | API 端点注册 | ⚠️ 部分完成 | 5.2 | `api/app.py` | 6 个 router 已注册（chat/cancel/feedback/upload/cart/admin_eval），upload 已支持 multipart 图片上传；cancel 仍是空壳 |
| 9 | 管道编排 | ✅ 已完成 | 4.1 | `runtime/pipeline.py` | intent/criteria/retrieval 按依赖串行；检索完成后立即发 product_card，推荐文案与最终决策后台并行，阶段耗时写入 retrieval trace |
| 10 | 混合检索（硬过滤 + 向量召回 + Rerank） | ⚠️ 部分完成 | 4.2 | `services/retriever.py` | 三阶段骨架完整，但底层用 SQLite/JSON embedding 非 pgvector，硬过滤和余弦相似度都在 Python 内存算 |

## P1：效果与可靠性（评审权重 20%）+ 加分项启动（20%）

| # | 功能 | 状态 | PRD 章节 | 代码路径 | 备注 |
|---|------|------|---------|---------|------|
| 11 | 双轨模型 + fallback 机制 | ✅ 已完成 | 2 | `services/llm_client.py` `config/settings.py` | Doubao+Qwen 双轨，TASK_MODEL_MAP 控制 primary/fallback |
| 12 | Embedding 服务 | ✅ 已完成 | 2 | `services/embedding.py` | 百炼 text-embedding-v3 primary，Doubao fallback，有 deterministic fallback |
| 13 | Rerank 服务 | ✅ 已完成 | 2 | `services/reranker.py` | gte-rerank 接入，API 失败时返回原序 |
| 14 | 证据绑定 | ⚠️ 部分完成 | 4.2 | `services/evidence.py` `repos/documents.py` | product_card 带 evidence；DB chunk 命中时 source_id 可关联真实 chunk，fallback source_id 不可关联 `product_chunks` |
| 15 | 检索追踪 + 证据链接 | ✅ 已完成 | 6.1 | `repos/traces.py` | retrieval_traces + evidence_links 写入 SQLite，filters_applied 内记录 intent/criteria/retrieve/recommendation/decision 阶段耗时 |
| 16 | 多轮上下文 | ⚠️ 部分完成 | 4.3 | `repos/conversations.py` | 最新 criteria/product_ids 已持久化到 Conversations 表，Demo 多轮可恢复；完整消息历史尚未用于 LLM |
| 17 | 反馈闭环 | ✅ 已完成 | 7 | `repos/feedbacks.py` `services/retriever.py` | Feedbacks 表已持久化，avoid_products/avoid_traits 已进入 retrieval 硬过滤；覆盖“不喜欢这个/除了耐克/不要含酒精” |
| 18 | 反选排除（⭐⭐） | ✅ 已完成 | — | `stages/criteria.py` | criteria_patch + ingredient_avoid + DB 会话恢复已覆盖 Demo 3 |
| 19 | 图片上传 + Qwen-VL-Plus 理解 | ✅ 已完成 | 5.2 | `api/upload.py` `services/image_upload.py` `runtime/pipeline.py` | multipart 上传、静态 `/uploads`、本地图片 data URL 转 VL、multimodal analysis 注入 criteria；旧 JSON 占位请求仍兼容 |
| 20 | 对话式加购（⭐入门） | ⚠️ 部分完成 | — | `repos/cart_items.py` `api/cart.py` | add/view 已持久化到 cart_items 表并覆盖 Demo 4；Remove/Update 未做 |

## P2：打磨与稳定性

| # | 功能 | 状态 | PRD 章节 | 代码路径 | 备注 |
|---|------|------|---------|---------|------|
| 21 | Thinking 心跳（800ms） | ✅ 已完成 | 4.4 | `runtime/pipeline.py` | LLM/VL/retrieval/decision 等待期通过 `_run_with_heartbeat` 持续发 thinking，证据绑定已显式随 RecommendationResult 回传 |
| 22 | 取消生成（Cancel） | ❌ 未完成 | 5.2 | `api/cancel.py` | 端点返回 `{"canceled": true}` 但不做任何事 |
| 23 | 评测模块 | ✅ 已完成 | 6.1 | `services/eval/` | 代码实际包含 7 个确定性指标 + 8 个 LLM Judge 指标 + overall_score，Qwen-Plus 做 Judge，无需额外依赖 |
| 24 | 评测样本（15 条） | ✅ 已完成 | 6.3 | `data/eval/eval_samples.json` | 15 条覆盖 6 种 scenario_type × 4 品类 × 3 难度 |
| 25 | 管理后台 API | ⚠️ 部分完成 | 5.2 | `api/admin_eval.py` | 已有 GET /admin/eval/runs、GET /admin/eval/runs/{id}、GET /admin/eval/samples、POST /admin/eval/samples/seed；无 POST /admin/eval/runs 触发评测 |
| 26 | 4 条 Demo 路径稳定性打磨 | ✅ 已完成 | — | — | Demo 1/2 live smoke 验证，Demo 3/4 pipeline 测试覆盖 |

## 工程质量（评审权重 25%）

| # | 项目 | 状态 | 备注 |
|---|------|------|------|
| 27 | 测试覆盖 | ✅ 84 测试全通过 | 2026-05-23 核实：`backend/.venv/bin/python -m pytest -q`；覆盖 model/schema/pipeline/API/retrieval/state repos/startup seed/image upload/multimodal/heartbeat/fast product cards/feedback hard filter |
| 28 | 架构分层（AGENTS.md） | ⚠️ 部分符合 | 主链路大体分层清晰，但 `api/chat.py`/`api/feedback.py`/`api/cart.py` 直接调 Repo，`repos/ingest.py` 反向调 Service |
| 29 | 配置集中管理 | ✅ 符合 | `settings.py` + `llm_profiles.yaml`，禁止散落 `os.getenv()` |
| 30 | 错误处理 | ✅ 有 | pipeline try/except 兜底，ErrorEvent 返回 |
| 31 | Docker Compose | ⚠️ 基本可用 | `deploy/docker-compose.yml` 已配置 Postgres healthcheck、`.env`、数据集挂载、启动自动建表/seed；模型仍用 JSON embedding，不是 pgvector 列 |
| 32 | Prompt 文件 | ⚠️ 半成品 | `backend/prompts/` 下 6 个 .md 文件已写好，但运行时从未加载（llm_client.py 用硬编码字符串） |
| 33 | 接口契约文档 | ✅ 有 | `contracts/sse-events.schema.json` + 3 个 golden trace 示例 |
| 34 | 评测看板（Streamlit） | ✅ 已完成 | `static/eval_dashboard.py`，4 页：总览/版本对比/样本详情/错误分析，`streamlit run` 启动 |

## 4 条 Demo 路径状态

| # | Demo 路径 | 后端状态 | 备注 |
|---|-----------|---------|------|
| 1 | "推荐适合油皮的洗面奶，200元以内，日常护肤" | ✅ **可跑通** | 2026-05-23 live smoke 验证，先返回 4 个 product_card，再返回推荐文案和最终决策；真实 chunk evidence |
| 2 | 上传护肤品图片 + "这个适合敏感肌吗？" | ✅ **可跑通** | 官方商品图 live 多模态 smoke 通过：Qwen-VL 返回图片描述，criteria 结合敏感肌约束生成 |
| 3 | "不要含酒精的防晒霜" + "预算降到200" | ✅ **可跑通** | pipeline 测试覆盖 DB 恢复上一轮 criteria，保留 `ingredient_avoid=["酒精"]` 并更新预算；feedback 反选会进入下一轮检索硬过滤 |
| 4 | "把这个加到购物车" | ✅ **可跑通** | 修复 add/view_cart 规则优先级；cart_action add + cart_items 持久化已测试 |

## 已知架构差距

| # | 问题 | 影响 | 建议优先级 |
|---|------|------|-----------|
| A | **首张商品卡仍需等待 intent/criteria/retrieval**：推荐文案与决策已并行，但检索前置依赖仍在 | 用户可见商品卡延迟仍取决于标准生成和检索速度 | P1 |
| B | **Cancel 空壳**：无法真正中断正在生成的 SSE 流 | 用户体验差，但比赛核心 Demo 链路不依赖 | P2/最后 |
| C | **SQLite 替代 pgvector**：向量相似度在 Python 内存计算 | 100 商品没问题，但架构不可扩展 | P1 |
| D | **状态存储仍不完整**：conversations/feedbacks/cart 已持久化核心状态，feedback 已进入检索硬过滤；完整多轮消息历史尚未用于 LLM | 复杂多轮表达理解仍有限 | P1 |
| E | **Prompt 文件孤岛**：6 个 .md 文件写了但运行时没加载 | 修改 prompt 需改代码，无法单独迭代 | P1 |
| F | **死代码**：PipelineState TypedDict 无人引用 | 维护负担 | P2 |
| G | **架构分层违规**：部分 API 直接调 Repo，`repos/ingest.py` 调 Service | 和 AGENTS.md 分层要求不完全一致，后续扩展容易继续绕层 | P1 |
