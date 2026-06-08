---

# BuyPilot-AI 实现评价报告

> 评价维度：字节跳动官方比赛要求 vs 当前实现完成度  
> 同步更新：2026-06-08  
> 本次依据：源码核实 + Docker/Postgres live smoke + demo smoke 门禁复跑

---

## 一、总体评分矩阵

| 评审维度 | 权重 | 当前状态 | 预估得分率 | 关键判断 |
| --- | --- | --- | --- | --- |
| **基础功能完整性** | 35% | ✅ 完全覆盖 | **95%** | 端到端链路、SSE、RAG、商品卡、加购闭环均可跑通；Postgres/pgvector 运行态已验证 |
| **工程质量** | 25% | ✅ 优秀 | **90%** | 分层架构、契约文档、CI、可观测性、门禁脚本齐全；本次补强 demo smoke 稳定性 |
| **效果与可靠性** | 20% | ✅ 较稳 | **85%** | live RAG smoke 已通过；多轮反选与预算调整已在 Docker demo smoke 中验证；Android 真机 UI 仍需人工走查 |
| **加分项深度** | 20% | ✅ 深度足够 | **90%** | 拍照找货、反选排除、多商品对比、旅行组合策略、对话式加购均有可演示路径 |

**综合预估：88-92 分**

说明：分数上调的原因是原报告中的两个主要不确定项已经复核：`make smoke` 通过，Docker `demo_smoke` 修复后通过。仍不建议宣称接近满分，因为 Android 真机体验、评测触发入口、长对话边界还没有完全闭环。

---

## 二、本次复核结果

### 2.1 Live RAG 门禁

已执行：

```bash
make smoke
```

结果：

| 检查项 | 结果 |
| --- | --- |
| database_engine | ✅ PostgreSQL |
| embedding_index | ✅ 1292 chunks，1292 embedded chunks，1024 维 |
| live embedding | ✅ 1024 维 |
| chat_stream_turn1 | ✅ product_card + criteria_card + final_decision + done |
| evidence | ✅ source_id 命中，如 `p_beauty_011:12` |
| fallback | ✅ 无 critical fallback |
| chat_stream_turn2 | ✅ 多轮继续 + final decision |

结论：原 R1 “live 检索效果未验证”已关闭。

### 2.2 Demo Smoke 门禁

已修复并执行 Docker 环境：

```bash
docker compose -f deploy/docker-compose.yml -f deploy/docker-compose.cloudflare.yml --env-file .env exec api python -m src.scripts.demo_smoke
```

结果：✅ `ok: true`

覆盖场景：

| 场景 | 状态 | 说明 |
| --- | --- | --- |
| 文字推荐 | ✅ | 油皮洁面、200 元以内，返回商品、标准、证据和最终决策 |
| 图片理解 | ✅ | 上传官方商品图，触发 `analyzing_image`，返回敏感肌相关推荐 |
| 主动反问 | ✅ | “推荐一款手机”触发 budget 澄清 |
| 旅行组合策略 | ✅ | “三亚度假”返回 travel 场景策略和 6 个跨品类商品 |
| 多商品对比 | ✅ | 基于旅行搭配多商品候选触发 `compare_card` |
| 多轮反选 | ✅ | “不要含酒精的防晒霜”保留排除约束 |
| 预算调整 | ✅ | “预算降到200”延续上一轮防晒/无酒精约束 |
| 加购 | ✅ | “把这个加到购物车”触发 `cart_action:add` |
| 购买意向闭环 | ✅ | 加购后 `checkout_preview`、`checkout_confirm` 均成功 |
| 查看购物车 | ✅ | 返回非空购物车 |

本次修复点：

- `compare_first_two` 不再依赖“洗面奶 200 内”这个只返回 1 个强候选的查询，改为使用旅行组合策略产生的多商品候选。
- `checkout_preview/checkout_confirm` 调整到 `cart_add` 之后验证，符合现有业务规则：空购物车时短句“确认”不应劫持为 checkout。
- 新增 `test_demo_smoke_main_uses_stable_compare_and_checkout_order`，防止门禁脚本再次退化。

---

## 三、逐项分析

### 3.1 基础功能完整性（35%）— ✅ 95%

| 官方要求 | 当前实现 | 评分 |
| --- | --- | --- |
| 50-100 条脱敏电商数据 | ✅ 运行态 `products=100` | 满足 |
| 商品 RAG chunk | ✅ 运行态 `chunks=1292`，全部 1024 维 embedding | 超额 |
| iOS/Android 原生 App | ✅ Kotlin + Jetpack Compose + OkHttp SSE | 满足 |
| 对话窗口 + 流式渲染 | ✅ SSE 流式 + thinking 心跳 + text_delta | 超额 |
| 商品卡片可点击 | ✅ ProductPayload 含 image/title/price/evidence/sku_options | 满足 |
| Python/Go/Node.js 后端 | ✅ FastAPI | 满足 |
| 向量数据库 | ✅ PostgreSQL + pgvector，SQLite 仅测试/开发 fallback | 超额 |
| RAG 基本链路 | ✅ 硬过滤 + 向量召回 + rerank + 证据绑定 | 超额 |
| SSE/WebSocket 流式 API | ✅ SSE 事件协议 + JSON Schema 契约 | 超额 |
| 理解模糊需求 | ✅ LLM intent + 确定性规则 fallback | 满足 |
| 检索库内商品 | ✅ live smoke 已验证 | 满足 |
| 合理推荐理由 | ✅ 推荐解释与 evidence 绑定 | 满足 |
| 不编造虚假信息 | ✅ 商品白名单、证据绑定、claims 校验、strict runtime 门禁 | 满足 |

剩余注意：

- 演示必须使用 Docker Compose/PostgreSQL/pgvector 路径；直接本地 `uv run` 只有在显式配置 `DATABASE_URL` 时才等价。
- 本地 health 返回 `strict_runtime=false`，但 live smoke 已证明当前 provider/index 可用；答辩前建议再跑一次 `STRICT_RUNTIME=1` 的门禁或使用 CI live-smoke job 留证。

### 3.2 工程质量（25%）— ✅ 90%

| 评审点 | 当前实现 |
| --- | --- |
| 代码结构清晰 | ✅ API → Runtime → Service → Repo → Config/Types 分层，有架构守卫 |
| 接口设计合理 | ✅ task-oriented LLM interface、SSE 契约 JSON Schema 化 |
| 错误处理 | ✅ pipeline 兜底、ErrorEvent 脱敏、关键 fallback 显性化 |
| 文档 | ✅ CLAUDE.md、PRD、design-decisions、README、状态文档齐全 |
| 依赖锁定 | ✅ `uv.lock`、CI `uv sync --locked` |
| 核心逻辑注释 | ✅ RAG、Prompt、状态合并、门禁脚本有必要注释 |
| Docker 一键启动 | ✅ `make rebuild` / `make smoke` / `make db-stats` |
| 测试 | ✅ 本次聚焦回归 `25 passed`；历史全量测试需在提交前再跑一次 |
| CI | ✅ pytest + ruff + mypy 子集 + 手动/定时 live provider smoke |

本次新增工程改进：

- `demo_smoke` 门禁从“偶然依赖某个查询返回多候选”改为“显式使用多商品场景验证 compare”。
- checkout 门禁改为验证真实业务闭环，而不是验证一个空购物车下被业务规则禁止的确认动作。

### 3.3 效果与可靠性（20%）— ✅ 85%

| 评审点 | 当前状态 | 风险 |
| --- | --- | --- |
| 运行流畅 | ✅ live smoke 中主链路约 3-4 秒，demo smoke 各场景可跑通 | Android 真机体感仍需确认 |
| 界面美观 | ⚠️ 有 DESIGN.md 和 Compose 实现，但本次未真机截图 | 中 |
| 无明显 Bug | ✅ 本次发现并修复 demo smoke 两个门禁问题 | 低-中 |
| 检索准确率 | ✅ live RAG smoke 通过，证据命中真实 chunk | 仍缺系统化 precision/recall 报表 |
| 无幻觉 | ✅ evidence + 白名单 + claims 校验 | 对抗样本仍可补 |
| 多轮上下文 | ✅ request history 已注入 intent/criteria，DB 摘要注入最近 4 轮；Demo 反选/预算调整通过 | 长对话细节仍可能被摘要压缩 |

重要更正：

原报告中的“完整多轮消息历史尚未用于 LLM prompt”表述已经不准确。当前实现是：

- `ChatStreamRequest.history` 已进入 `run_intent` 和 `run_criteria`。
- `intent_messages` 与 `criteria_messages` 都会把 history 写入 prompt/payload。
- `get_conversation_summary()` 会读取 `conversations` 最近 4 轮，注入用户消息、criteria summary、商品 ID 和反馈摘要。

真实剩余边界是：数据库没有完整保存所有 assistant 原文消息；recommendation/decision 阶段主要依赖 criteria、候选商品和 evidence，不直接消费完整原始消息历史。

### 3.4 加分项深度（20%）— ✅ 90%

| 加分方向 | 当前实现 | 评价 |
| --- | --- | --- |
| 4.1 业务闭环 | ✅ 加购 + checkout_preview/confirm/cancel，明确不接真实支付 | 入门闭环完整 |
| 4.2 多模态 | ✅ 图片上传、Qwen-VL、image embedding 索引，`image_embeddings=100` | 深度足够 |
| 4.3 对话智能与 RAG 增强 | ✅ 反选排除、多轮预算调整、多商品对比、主动反问 | 深度够 |
| 4.4 工程质量与性能 | ✅ 检索缓存、可观测性、live smoke、demo smoke、CI | 虽非主攻方向，但有支撑 |
| 场景化组合推荐 | ✅ travel 场景策略已实现并通过 demo smoke | 原报告“未实现”已过时 |

---

## 四、关键风险清单

### 已关闭风险

| # | 原风险 | 当前状态 |
| --- | --- | --- |
| R1 | live 检索效果未验证 | ✅ 已通过 `make smoke` |
| R2 | Demo 3 “预算降到200” 丢失“不要酒精” | ✅ Docker demo smoke 已验证约束延续 |
| R4 | README 未强调 PostgreSQL/pgvector | ✅ README 已明确 Docker Compose/PostgreSQL/pgvector 是验收路径 |

### 仍需处理

| # | 风险 | 影响 | 解决方案 |
| --- | --- | --- | --- |
| R3 | Android 真机 UI 未截图验收 | 评审要求“接近商业产品水平”，后端通过不等于前端体验通过 | 真机走 6 条路径，保存截图/录屏；重点看商品卡、compare_card、cart_action、流式动画 |
| R5 | 无 `POST /admin/eval/runs` | 答辩现场不能一键触发评测，只能展示历史或手动脚本 | 补一个受 admin key 保护的触发接口，或提前跑评测并固定报告截图 |
| R6 | 状态文档/报告测试数量不完全统一 | 评审追问时容易被“136/140/25 passed”混淆 | 提交前跑全量 pytest，并统一 README、backend-completion、答辩材料中的测试数量 |
| R7 | 当前容器 health 为 `strict_runtime=false` | 评委追问 fallback 时需要解释清楚 | 答辩前用 `STRICT_RUNTIME=1` 环境复跑 smoke，或说明 smoke 本身已检查无 critical fallback |

---

## 五、官方场景覆盖度

| 官方场景 | 难度 | 当前覆盖 | 评价 |
| --- | --- | --- | --- |
| 单轮模糊推荐 | 基础 | ✅ `text_budget_beauty` | 完全覆盖 |
| 条件筛选 | 基础 | ✅ budget/category/product_type/ingredient_avoid 硬过滤 | 完全覆盖 |
| 多轮追问与细化 | 进阶 | ✅ history + conversation summary + criteria 合并 | 核心 Demo 已过 |
| 对比决策 | 进阶 | ✅ `compare_card`，基于多商品旅行候选 | 完全覆盖 |
| 主动反问 | 进阶 | ✅ 手机预算澄清 | 完全覆盖 |
| 反选/排除约束 | 高级 | ✅ ingredient_avoid/brand_avoid/origin_avoid | Demo 已过 |
| 场景化组合推荐 | 高级 | ✅ travel strategy + 跨品类商品 | 已覆盖 |
| 购物车与下单 | 高级 | ✅ 加购 + checkout preview/confirm/cancel | 入门闭环覆盖 |
| 拍照找货 | 高级 | ✅ Qwen-VL + image embedding + 图片 Demo | 已覆盖 |

**覆盖度：9/9 核心场景。**

---

## 六、解决方案优先级

### P0：答辩前必须完成

1. **真机 UI 走查**
   - 路径：文字推荐、图片敏感肌、手机主动反问、旅行组合、多商品对比、加购/购买意向。
   - 产物：录屏或截图，作为“接近商业产品水平”的证据。

2. **全量测试与文档数字统一**
   - 执行 `cd backend && uv run pytest -q`。
   - 将最终测试数量同步到 README、`backend-completion.md` 和答辩材料。

3. **答辩前门禁命令固定**
   ```bash
   make db-stats
   make smoke
   docker compose -f deploy/docker-compose.yml -f deploy/docker-compose.cloudflare.yml --env-file .env exec api python -m src.scripts.demo_smoke
   ```

### P1：有时间就做

1. **补 `POST /admin/eval/runs`**
   - 最小实现即可：admin key 鉴权，触发 eval runner，返回 run_id。
   - 价值：现场可展示评测闭环，不只靠静态报告。

2. **补 strict runtime 门禁记录**
   - 用 `STRICT_RUNTIME=1` 复跑 smoke 或加一条 CI 手动记录。
   - 价值：回应“是否用了 fallback/mock”的评审追问。

3. **更新答辩话术**
   - 多轮上下文不要再说“完整消息历史未注入”；应说“请求 history + DB 摘要已注入 intent/criteria，recommendation/decision 依赖结构化 criteria 与 evidence”。

### P2：不建议赛前再大改

- 不建议为了“完整消息历史入库”做 schema 迁移。当前核心 Demo 已过，赛前大改状态模型风险大。
- 不建议扩展真实订单/支付/地址。现有轻量购买意向闭环符合止损策略。
- 不建议继续扩展更多加分方向。当前重点应是稳定性、截图和答辩证据。

---

## 七、总结判断

项目当前已经从“方案到位但 live 未验证”推进到“后端 live/demo 门禁已通过”。原报告中最大的两个风险已基本关闭：

- live RAG 已通过；
- 多轮反选/预算调整已通过；
- 多商品对比和场景化组合推荐已纳入稳定 demo smoke。

当前最大不确定性转移到前端真机体验和评测展示入口。建议停止新增业务功能，把剩余时间投入真机录屏、全量测试、评测触发接口或固定评测报告。
