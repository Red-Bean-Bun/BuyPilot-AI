# BuyPilot-AI 后端完成状态

> 最后更新：2026-05-22
> 维护方式：AI 完成后端功能开发后自动更新此文档，见 CLAUDE.md "Agent 工作指引" 第 8 条

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
| 7 | 数据入库（100 商品 × 1092 chunk） | ✅ 已完成 | 3.5 | `repos/ingest.py` `scripts/reindex_embeddings.py` | 百炼 text-embedding-v3 1024 维，已验证通过 |
| 8 | API 端点注册 | ⚠️ 部分完成 | 5.2 | `api/app.py` | 5 个 router 已注册，但 cancel 是空壳，upload 是 stub |
| 9 | 管道编排 | ⚠️ 部分完成 | 4.1 | `runtime/pipeline.py` | 事件顺序正确，但**全串行执行**，PRD 要求的 asyncio.gather 并行化未实现 |
| 10 | 混合检索（硬过滤 + 向量召回 + Rerank） | ⚠️ 部分完成 | 4.2 | `services/retriever.py` | 三阶段骨架完整，但底层用 SQLite 非 pgvector，余弦相似度在 Python 内存算 |

## P1：效果与可靠性（评审权重 20%）+ 加分项启动（20%）

| # | 功能 | 状态 | PRD 章节 | 代码路径 | 备注 |
|---|------|------|---------|---------|------|
| 11 | 双轨模型 + fallback 机制 | ✅ 已完成 | 2 | `services/llm_client.py` `config/settings.py` | Doubao+Qwen 双轨，TASK_MODEL_MAP 控制 primary/fallback |
| 12 | Embedding 服务 | ✅ 已完成 | 2 | `services/embedding.py` | 百炼 text-embedding-v3 primary，Doubao fallback，有 deterministic fallback |
| 13 | Rerank 服务 | ✅ 已完成 | 2 | `services/reranker.py` | gte-rerank 接入，API 失败时返回原序 |
| 14 | 证据绑定 | ⚠️ 部分完成 | 4.2 | `services/evidence.py` `repos/documents.py` | product_card 带 evidence 字段，但 chunk_id 逻辑脆弱 |
| 15 | 检索追踪 + 证据链接 | ✅ 已完成 | 6.1 | `repos/traces.py` | retrieval_traces + evidence_links 写入 SQLite |
| 16 | 多轮上下文 | ⚠️ 部分完成 | 4.3 | `repos/conversations.py` | **in-memory 实现**，重启丢失。Conversations 表未使用 |
| 17 | 反馈闭环 | ⚠️ 部分完成 | 7 | `repos/feedbacks.py` | **in-memory 实现**，重启丢失。Feedbacks 表未使用 |
| 18 | 反选排除（⭐⭐） | ⚠️ 部分完成 | — | `stages/criteria.py` | 通过 criteria_patch + ingredient_avoid 可工作，但需多轮会话上下文支撑 |
| 19 | 图片上传 + Qwen-VL-Plus 理解 | ❌ 未完成 | 5.2 | `api/upload.py` `api/chat.py` | upload 是 stub（返回 mock URL）；`stages/multimodal.py` 存在但 pipeline 从未调用 |
| 20 | 对话式加购（⭐入门） | ⚠️ 部分完成 | — | `repos/cart_items.py` `api/cart.py` | add_to_cart 意图 + cart_action 事件可用，但无 Remove/Update，**in-memory** 重启丢失 |

## P2：打磨与稳定性

| # | 功能 | 状态 | PRD 章节 | 代码路径 | 备注 |
|---|------|------|---------|---------|------|
| 21 | Thinking 心跳（800ms） | ❌ 未完成 | 4.4 | `runtime/pipeline.py` | 当前只在阶段边界发 thinking，LLM 等待期间无事件 |
| 22 | 取消生成（Cancel） | ❌ 未完成 | 5.2 | `api/cancel.py` | 端点返回 `{"canceled": true}` 但不做任何事 |
| 23 | 评测模块（13 个指标） | ✅ 已完成 | 6.1 | `services/eval/` | 6 个确定性指标 + 7 个 LLM Judge 指标，Qwen-Plus 做 Judge，无需额外依赖 |
| 24 | 评测样本（15 条） | ✅ 已完成 | 6.3 | `data/eval/eval_samples.json` | 15 条覆盖 8 种场景 × 4 品类 × 3 难度 |
| 25 | 管理后台 API | ✅ 已完成 | 5.2 | `api/admin_eval.py` | GET/POST /admin/eval/runs, GET /admin/eval/samples, POST /admin/eval/samples/seed |
| 26 | 4 条 Demo 路径稳定性打磨 | ⚠️ 部分完成 | — | — | 仅 Demo 1 已验证跑通，Demo 2/3/4 有 gap |

## 工程质量（评审权重 25%）

| # | 项目 | 状态 | 备注 |
|---|------|------|------|
| 27 | 测试覆盖 | ✅ 62 测试全通过 | 13 个测试文件，覆盖 model/schema/pipeline/API/retrieval |
| 28 | 架构分层（AGENTS.md） | ✅ 符合 | UI→Runtime→Service→Repo→Config/Types 分层清晰，依赖方向正确 |
| 29 | 配置集中管理 | ✅ 符合 | `settings.py` + `llm_profiles.yaml`，禁止散落 `os.getenv()` |
| 30 | 错误处理 | ✅ 有 | pipeline try/except 兜底，ErrorEvent 返回 |
| 31 | Docker Compose | ✅ 有 | `deploy/docker-compose.yml`（FastAPI + PostgreSQL） |
| 32 | Prompt 文件 | ⚠️ 半成品 | `backend/prompts/` 下 6 个 .md 文件已写好，但运行时从未加载（llm_client.py 用硬编码字符串） |
| 33 | 接口契约文档 | ✅ 有 | `contracts/sse-events.schema.json` + 3 个 golden trace 示例 |
| 34 | 评测看板（Streamlit） | ✅ 已完成 | `static/eval_dashboard.py`，4 页：总览/版本对比/样本详情/错误分析，`streamlit run` 启动 |

## 4 条 Demo 路径状态

| # | Demo 路径 | 后端状态 | 备注 |
|---|-----------|---------|------|
| 1 | "推荐适合油皮的洗面奶，200元以内，日常护肤" | ✅ **可跑通** | 已端到端验证，返回 4 商品 + 最终决策 |
| 2 | 上传护肤品图片 + "这个适合敏感肌吗？" | ❌ **不可用** | upload 是 stub，multimodal stage 未接入 pipeline |
| 3 | "不要含酒精的防晒霜" + "预算降到200" | ⚠️ **部分可用** | 反选通过 criteria_patch 可工作，但多轮依赖 in-memory 会话 |
| 4 | "把这个加到购物车" | ⚠️ **部分可用** | add_to_cart 可识别和写入，但重启丢失且无 Remove |

## 已知架构差距

| # | 问题 | 影响 | 建议优先级 |
|---|------|------|-----------|
| A | **全串行管道**：intent→criteria→retrieval→recommendation 无并行 | 首字延迟 > LLM 响应时间（~2s），达不到 PRD 承诺的 <100ms | P0 |
| B | **Cancel 空壳**：无法真正中断正在生成的 SSE 流 | 用户体验差，用户无法停止不需要的生成 | P0 |
| C | **SQLite 替代 pgvector**：向量相似度在 Python 内存计算 | 100 商品没问题，但架构不可扩展 | P1 |
| D | **In-memory 状态存储**：conversations/feedbacks/cart 重启丢失 | 多轮会话和购物车 Demo 不稳定 | P1 |
| E | **Prompt 文件孤岛**：6 个 .md 文件写了但运行时没加载 | 修改 prompt 需改代码，无法单独迭代 | P1 |
| F | **死代码**：PipelineState TypedDict 无人引用；multimodal stage 未接入 | 维护负担 | P2 |
