# BuyPilot-AI 赛前改进 PRD（合并版）

版本：v1.0
日期：2026-06-06
状态：待执行
目标读者：项目负责人、后端开发、Android 开发、答辩材料负责人

## Executive Summary

当前 Codex 估算分数为 **93 / 100**。剩余 7 分分布在四个评审维度。本 PRD 基于课题说明会场景覆盖差距分析，整合所有待办事项为一份统一执行文档。

**剩余 4 天，目标从 93 提升到 97+。**

核心工作分三层：

1. **补齐课题说明会场景覆盖**（多轮上下文、主动反问、下单确认、热门缓存、场景组合、首屏极速）—— 让评委找不到扣分点。
2. **固化证据包**（demo runbook、pytest 日志、live smoke 录屏）—— 让评委快速复核。
3. **多模态与端侧展示增强**（TTS 语音播报、截图）—— 加分项。

## 1. 背景与差距分析

### 1.1 课题说明会场景 vs 当前实现

| 场景 | 难度 | 课题说明会要求 | 当前状态 | 差距 |
|------|------|---------------|---------|------|
| 单轮模糊推荐 | 基础 | 意图识别 + 属性匹配 | ✅ 已完成 | — |
| 条件筛选 | 基础 | 结构化参数提取 + 范围过滤 | ✅ 已完成 | — |
| 多轮追问与细化 | 进阶 | 多轮上下文管理，渐进收敛 | ⚠️ 部分 | criteria 阶段只看压缩摘要，丢用户原话 |
| 对比决策 | 进阶 | 多商品并行检索 + 属性对比 | ✅ 已完成 | compare_card 全链路 |
| 主动反问 | 进阶 | 识别信息不足时主动澄清 | ⚠️ 不足 | 只问品类/预算，不问偏好权衡 |
| 反选/排除约束 | 高级 | 否定条件解析 | ✅ 已完成 | — |
| 场景化组合推荐 | 高级 | 跨类目检索 + 场景理解 | ❌ 缺失 | shopping_strategy 只识别 gift/interest |
| 购物车与下单 | 高级 | 对话式 CRUD + 业务闭环 | ⚠️ 部分 | CRUD 完整，下单确认未做 |
| 拍照找货 | 高级 | 图像理解 + 视觉检索 | ✅ 已完成 | — |

### 1.2 加分项 vs 当前实现

| 加分项 | 难度 | 当前状态 | 差距 |
|--------|------|---------|------|
| 对话式加购 | ⭐ | ✅ | — |
| 购物车自然语言管理 | ⭐⭐ | ✅ | 支持序号解析、数量修改 |
| 下单确认流程 | ⭐⭐⭐ | ⏳ | PRD 08 已规划 |
| 语音输入（ASR） | ⭐ | ❌ | 不投入 |
| TTS 语音播报 | ⭐⭐ | ❌ | 可投入 |
| 拍照找货 | ⭐⭐⭐ | ✅ | — |
| 多轮上下文记忆 | ⭐ | ⚠️ | criteria 阶段缺完整历史 |
| 反选与排除 | ⭐⭐ | ✅ | — |
| 多商品对比决策 | ⭐⭐⭐ | ✅ | — |
| 热门查询缓存 | ⭐ | ⚠️ | 有通用检索缓存，缺频率追踪 |
| 首屏极速响应 | ⭐⭐ | ❌ | 商品卡仍需等串行链路 |
| 端侧体验打磨 | ⭐⭐⭐ | — | Android 侧，本 PRD 不展开 |

### 1.3 得分拆解与提升目标

| 维度 | 权重 | 当前 | 目标 | 提升路径 |
|------|------|-----:|-----:|---------|
| 基础功能完整性 | 35% | 34 | 35 | 下单确认闭环 + 主动反问补齐 |
| 工程质量 | 25% | 23 | 25 | 证据包 + 热门缓存 + 首屏极速 |
| 效果与可靠性 | 20% | 19 | 20 | 多轮上下文补全 + 场景组合 |
| 加分项深度 | 20% | 17 | 19 | TTS + 多模态证据 + 缓存可观测 |
| **总计** | **100%** | **93** | **99** | — |

### 1.4 赛前约束

1. 不做真实支付。
2. 不引入新外部服务（不用新 SDK、新 API Key）。
3. 不重构 RAG 主链路核心算法。
4. 不改变现有 Android 主交互范式。
5. 不牺牲已有 live smoke、pytest 和 Docker 部署稳定性。

---

## 2. 拍板决策（已确认）

| 编号 | 问题 | 决策 |
|------|------|------|
| D1 | 是否做下单确认闭环 | 做，轻量确认购买意向 |
| D2 | 复用 `cart_action` 还是新事件 | 复用，扩展 action 字符串 |
| D3 | 确认后是否清空购物车 | 不清空，只记录意向 |
| D4 | CLAUDE.md 裁剪放松 | 确认，热门缓存/首屏极速/TTS 均可做 |
| D5 | 分数来源 | Codex 估算，非模拟评委 |

---

## 3. P0：补齐课题说明会场景覆盖

### 3.1 多轮上下文补全

**问题**：criteria 生成阶段只看压缩摘要（4 轮 × 100 字截断），丢失用户原话细节。用户第二轮说"再便宜点的呢"，criteria 生成器不知道"再"指的是什么。

**改法**：`stages/criteria.py` 的 LLM payload 也传入 `history`，和 intent 阶段对齐。

**影响范围**：
- `backend/src/runtime/stages/criteria.py` — 传 `history` 给 payload 构造
- `backend/src/services/llm_task_payloads.py` — criteria payload 接收 history

**工作量**：5-10 行改动

**验收**：
- 用户第一轮："推荐适合油皮的洁面"
- 用户第二轮："再便宜点的呢"
- criteria_card 的 `constraints.budget_max` 应反映降价意图
- pytest 增加多轮 criteria 合并测试

### 3.2 主动反问增强（偏好权衡澄清）

**问题**：slot_checker 只检查品类和预算，4 个固定问题。用户说"推荐一款手机"，不会问"更看重拍照、续航还是性价比"。课题说明会明确列出这是进阶场景。

**改法**：

1. 在 `stages/slot_checker.py` 新增偏好权衡类澄清：
   - 数码品类（手机/耳机/平板）：询问偏好维度（拍照/续航/性价比/游戏）
   - 护肤品类（面霜/精华）：询问肤质（油皮/干皮/混合/敏感）
   - 运动品类（跑鞋/运动服）：询问运动类型（跑步/健身/户外）

2. `ClarificationEvent` 的 `suggested_options` 携带偏好选项列表

3. 触发条件：用户输入含品类关键词但无明确偏好维度

**影响范围**：
- `backend/src/runtime/stages/slot_checker.py` — 新增偏好槽位检查
- `backend/src/types/slot_defs.py` — 新增偏好槽位定义
- `backend/src/types/sse_events.py` — ClarificationEvent 的 suggested_options 格式（已有）

**工作量**：30-50 行

**验收**：
- 用户："推荐一款手机"
- 系统发 `clarification` 事件，`question="请问您更看重哪个方面？"`，`suggested_options=["拍照","续航","性价比","游戏"]`
- 用户选择后，偏好注入 criteria
- pytest 覆盖至少 3 个品类的偏好澄清

### 3.3 下单确认闭环

**问题**：购物车 CRUD 完整，但缺少从购物车到"确认购买"的闭环。课题说明会列此为高级场景 ⭐⭐⭐。

**业务边界**：
- 做：购物车非空时，用户说"就买这个""确认购买"→ 返回确认购买意向卡
- 不做：不接真实支付，不生成真实订单号，不处理地址/发票/库存

**改法**：

1. **意图识别**：新增 `checkout_preview` / `checkout_confirm` / `checkout_cancel` 三个意图
   - LLM prompt 加入
   - 硬规则兜底：关键词"就买这个""确认购买""确认下单""下单""买它""算了不买""取消购买"

2. **SSE 事件**：复用 `cart_action`，扩展 `action` 值：
   - `checkout_preview`：展示购物车预览（商品、数量、总价、风险提示、"非真实支付"说明）
   - `checkout_confirm`：确认完成，audit 记录
   - `checkout_cancel`：取消，保留购物车

3. **审计记录**：
   - `cart.checkout_previewed` / `cart.checkout_confirmed` / `cart.checkout_cancelled`
   - metadata: `{source, total_items, total_price, product_ids, real_payment: false}`

4. **购物车状态**：确认后不清空，只记录意向

**影响范围**：
- `backend/src/services/llm_client.py` — intent 扩展
- `backend/src/services/llm_task_payloads.py` — intent prompt 扩展
- `backend/src/runtime/handlers.py` — 新增 3 个 handler
- `backend/src/services/cart.py` — checkout 业务逻辑
- `backend/src/types/sse_events.py` — action 值文档（保持 str）
- Android — 新增 3 种 action 的卡片渲染

**工作量**：后端 0.5-1 天，Android 0.5-1 天

**验收**：
1. 推荐 → 加购 → "就买这个" → `cart_action(checkout_preview, status=success)`
2. 点确认 → `cart_action(checkout_confirm, status=success)`
3. 购物车为空时"确认购买" → `clarification` 或 `status=failed`
4. 取消后购物车不丢失
5. pytest 覆盖 4 个路径
6. Android 能展示预览卡和确认卡

### 3.4 热门查询缓存增强

**问题**：当前 `retrieval_cache.py` 是通用 TTL 缓存（SHA256(criteria+feedback), 5 分钟, 128 条, FIFO 淘汰），无频率追踪。课题说明会 4.4 的"热门查询缓存"要求对高频查询做缓存优化。

**改法**：

1. **频率追踪**：每次 `cache.get()` 命中时记录 hit_count per key
2. **优先淘汰**：淘汰时优先淘汰 hit_count 最低的条目（而非 oldest-first）
3. **热门标记**：hit_count >= 3 的 key 标记为 hot，TTL 延长到 10 分钟（普通 5 分钟）
4. **可观测性**：cache stats 暴露命中率、热门 key 列表，通过 `/admin/observability/cache` 查看

**影响范围**：
- `backend/src/services/retrieval_cache.py` — 核心改造
- `backend/src/api/observability.py` — 新增 cache stats 端点
- `backend/src/repos/models.py` — 新增 cache_stats 表（可选，内存追踪即可）

**工作量**：3-4 小时

**验收**：
- 相同 criteria 连续请求 3 次，第 4 次 TTL 延长
- 缓存满时 hit_count 最低的条目先被淘汰
- `/admin/observability/cache` 返回 `{total_keys, hit_rate, hot_keys: [...]}`
- pytest 覆盖频率追踪 + 优先淘汰

### 3.5 场景化组合推荐

**问题**：`shopping_strategy.py` 只识别 gift（送礼）和 interest（兴趣），完全不处理旅行/搭配/跨品类组合。课题说明会列"下周去三亚度假，帮我搭配一套从防晒到穿搭的方案"为高级场景。

**改法**：

1. **新增 travel 场景类型**：
   - 关键词："度假""旅行""出差""出游""海边""沙滩""滑雪"
   - 输出：跨品类 criteria 组合（防晒 + 泳衣/穿搭 + 墨镜/装备）

2. **组合检索策略**：
   - 按场景拆分为多个子品类 criteria
   - 每个子品类独立检索，合并输出
   - criteria_card 展示"搭配方案"叙事

3. **叙事生成**：
   - 为组合方案生成一段整体推荐文案（"这套三亚度假搭配包含..."）

**影响范围**：
- `backend/src/services/shopping_strategy.py` — 新增 travel 场景 + 组合逻辑
- `backend/src/runtime/handlers.py` — 组合检索的多品类路由
- `backend/src/services/retriever.py` — 支持多 criteria 批量检索（或串行调用合并结果）

**工作量**：3-4 小时

**验收**：
- 用户："下周去三亚度假，帮我搭配一套从防晒到穿搭的方案"
- 系统识别 travel 场景
- 输出跨品类 criteria（防晒 + 泳装 + 配饰）
- 推荐结果包含多个品类商品
- criteria_card 展示"三亚度假搭配方案"

### 3.6 首屏极速响应

**问题**：当前商品卡仍需等 intent → criteria → retrieval 串行完成。课题说明会 4.4 的"首屏极速响应"要求首 Token < 1s。

**改法**：

1. **投机检索提前**（已有基础，需优化）：
   - 当前：criteria 生成和投机检索并行，但商品卡等检索完成
   - 优化：投机检索用宽松 criteria（仅 category + budget），结果先出第一批商品卡
   - criteria 精化后如有变化，再出第二批（标记为"更新推荐"）

2. **流式商品卡**：
   - 检索结果返回后，不等待 rerank 全部完成，先出 top-3 商品卡
   - rerank 完成后更新排序（发 product_card_update 事件，或重新发 product_card）

3. **thinking 事件细化**：
   - 当前 thinking 只有 stage 名称
   - 优化：发 `thinking(stage="analyzing_intent")` → `thinking(stage="generating_criteria")` → `thinking(stage="searching_products")`，让 UI 展示进度

**影响范围**：
- `backend/src/runtime/stages/recommendation.py` — 投机检索提前 + 流式商品卡
- `backend/src/runtime/streaming.py` — thinking 阶段细化
- `backend/src/services/retriever.py` — 支持 streaming 模式返回（batch 返回前 N 个）
- Android — 处理流式商品卡 + thinking 阶段展示

**工作量**：4-6 小时

**验收**：
- 首商品卡时间 < 2s（当前约 3-5s）
- thinking 事件至少 3 个阶段
- pytest 覆盖投机检索提前 + 流式返回
- live smoke 对比首卡延迟

---

## 4. P0：评委证据包与演示 Runbook

### 4.1 目标

让评委不需要读代码，也能快速确认：
1. 不是 mock。
2. 使用 Postgres + pgvector。
3. 使用真实 text embedding 和 image embedding。
4. 使用真实 SSE 结构化事件。
5. 全量测试可通过。
6. 多模态视觉召回是真实链路。

### 4.2 文档产物

| 文件 | 目的 | 状态 |
|------|------|------|
| `doc/official/0606/模拟赛前评委审查报告-20260606.md` | 总体审查结论 | 已有，持续更新 |
| `doc/official/0606/live-smoke-visual-recall-evidence-20260606.md` | live smoke 与视觉召回证据 | 已有 |
| `doc/official/0606/full-pytest-evidence-20260606.md` | 全量 pytest 原始通过日志 | 待补 |
| `doc/official/0606/demo-runbook-20260606.md` | 答辩演示脚本 | 待补 |

### 4.3 Demo Runbook 内容要求

每条 demo 固定包含：
1. 目标。
2. 输入话术。
3. 预期 SSE 事件。
4. 预期商品 ID 或商品类别。
5. 展示点。
6. 失败时兜底话术。

**推荐 Demo 列表（更新后）**：

| # | Demo | 输入 | 展示点 |
|---|------|------|--------|
| 1 | 文本导购 | `推荐适合油性肌肤的日常洁面，200 元内` | criteria、RAG、product card、evidence |
| 2 | 多轮决策 | 第二轮 `有没有更温和一点的` | session state、criteria patch、候选替换 |
| 3 | 主动反问 | `推荐一款手机` | clarification 事件 + suggested_options |
| 4 | 图片导购 | 上传商品图 + `这个适合敏感肌吗` | VLM、image embedding、visual recall |
| 5 | 场景组合 | `下周去三亚度假，帮我搭配一套方案` | travel 场景、跨品类 criteria、组合推荐 |
| 6 | 购物车管理 | `删除第二个` / `数量改成 2` | cart_action remove / update_quantity |
| 7 | 购买意向确认 | `加入购物车` → `就买这个` → `确认` | cart_action add → checkout_preview → checkout_confirm |
| 8 | 对比决策 | `A 和 B 哪个好` | compare_card + narration |

---

## 5. P1：扩展 Live Smoke 覆盖

### 5.1 断言原则

为降低 LLM 随机性，测试不断言完整自然语言文案，只断言结构化事件：
1. `has_error=false`
2. 出现 `criteria_card`
3. 出现 `product_card`
4. `product_id` 属于本地商品库
5. `evidence` 存在
6. `done_reason=completed`
7. 多模态场景存在 `visual_recall`
8. 购物车场景存在目标 `cart_action`

### 5.2 建议用例（更新后）

| 用例 | 输入 | 必须断言 |
|------|------|---------|
| beauty_budget | `推荐适合油性肌肤的洁面，200 元内` | criteria、product_card、evidence |
| beauty_sensitive_followup | 第二轮 `有没有更温和一点的` | session 复用、product_card、无 error |
| proactive_clarification | `推荐一款手机` | clarification、suggested_options |
| image_visual_recall | 上传图片后询问适配性 | image embedding、visual_recall、product_card |
| travel_combo | `下周去三亚度假，帮我搭配方案` | travel 场景、跨品类 product_card |
| cart_checkout | `加入购物车` → `就买这个` → `确认` | add、checkout_preview、checkout_confirm |
| compare | `对比 A 和 B` | compare_card、narration |

---

## 6. P1：多模态展示增强

### 6.1 当前事实

已验证：
- image_embedding_count = 100
- query_image_embedding_dim = 1024
- trace_visual_recall.pre_filter_count = 1
- trace_visual_recall.post_filter_count = 1

### 6.2 改进目标

让评委更直观看到图片链路不是"图片描述 + 文本搜索"，而是：
```text
上传图片 -> VLM 理解 -> query image embedding -> visual_recall 命中 -> 合并 RAG -> product_card + evidence
```

### 6.3 产物要求

1. demo runbook 中写清图片链路展示话术。
2. 保留 visual recall trace 文本证据。
3. 补终端截图或 Android 截图。

---

## 7. P1：TTS 语音播报（Android 侧）

**问题**：课题说明会 4.2 多模态 ⭐⭐ 要求 TTS 语音播报。

**改法**：
1. Android 端使用 `android.speech.tts.TextToSpeech` API
2. 对 `text_delta` 事件做朗读（累积到句号后朗读，避免逐字播报太碎）
3. 设置开关：用户可开关 TTS
4. `final_decision` 和 `clarification` 事件触发朗读

**影响范围**：
- Android `ChatViewModel` 或 `TtsManager` — TTS 初始化 + 朗读队列
- Android UI — 新增 TTS 开关按钮

**工作量**：2-3 小时（纯 Android）

**验收**：
- 开启 TTS 后，AI 回复自动朗读
- 关闭 TTS 后，无声音
- 不影响 SSE 流式文本渲染

---

## 8. 工程质量要求

### 8.1 测试要求

| 功能 | 测试 | 覆盖 |
|------|------|------|
| 多轮上下文 | `test_criteria_merges_history_from_prior_turn` | criteria 阶段看到用户原话 |
| 主动反问 | `test_clarification_asks_preference_for_vague_phone` | 数码品类偏好澄清 |
| 下单确认 | `test_checkout_empty_cart_requires_item` | 空购物车不可确认 |
| | `test_checkout_preview_from_non_empty_cart` | 预览生成 |
| | `test_checkout_confirm_records_audit` | 审计记录 |
| | `test_checkout_cancel_keeps_cart` | 取消后购物车不丢失 |
| 热门缓存 | `test_cache_evicts_lowest_hit_count_first` | 优先淘汰 |
| | `test_cache_extends_ttl_for_hot_keys` | 热门 TTL 延长 |
| 场景组合 | `test_travel_scene_generates_cross_category_criteria` | travel 跨品类 |
| 首屏极速 | `test_speculative_retrieval_returns_early_product_cards` | 投机检索提前 |

### 8.2 兼容性要求

1. 不破坏现有 `cart_action` 的 `add/view/remove/update_quantity`。
2. 旧客户端不理解新 action 不崩溃。
3. `cart_action.action` 保持字符串字段。
4. 所有新增测试能在现有 pytest / Android test 框架中运行。

### 8.3 观测与日志

新增 audit event：
- `cart.checkout_previewed` / `cart.checkout_confirmed` / `cart.checkout_cancelled`
- cache stats 端点 `/admin/observability/cache`

---

## 9. 不做范围

赛前不做：
1. 真实支付。
2. 地址管理与物流状态。
3. 真实订单中心。
4. 优惠券和库存锁定。
5. 多商家拆单。
6. 退款售后。
7. 大规模视觉算法重构。
8. 语音输入（ASR）— 已有拍照找货覆盖多模态。
9. 新增复杂后台管理台。

---

## 10. 排期建议（4 天）

| 阶段 | 工作 | 预计时间 | 风险 |
|------|------|--------:|------|
| **Day 1 上午** | 3.1 多轮上下文补全 + 3.2 主动反问增强 | 3h | 低 |
| **Day 1 下午** | 3.3 下单确认（后端） | 4h | 中低 |
| **Day 2 上午** | 3.3 下单确认（Android） + 测试 | 3h | 中 |
| **Day 2 下午** | 3.4 热门缓存增强 + 3.5 场景组合推荐 | 4h | 低 |
| **Day 3 上午** | 3.6 首屏极速响应 | 4h | 中 |
| **Day 3 下午** | 4.0 证据包 + demo runbook | 3h | 低 |
| **Day 4 上午** | 5.0 扩展 live smoke + 6.0 多模态证据 | 3h | 低 |
| **Day 4 下午** | 7.0 TTS 语音播报（Android） + 收尾 | 3h | 低 |
| **缓冲** | bug fix + 最终验证 | 2h | — |

推荐顺序（如果时间不够）：
```text
3.1 多轮上下文 → 3.2 主动反问 → 3.3 下单确认
→ 4.0 证据包 → 3.4 热门缓存 → 3.5 场景组合
→ 3.6 首屏极速 → 5.0 live smoke → 6.0 多模态 → 7.0 TTS
```

---

## 11. 成功指标

### 11.1 必达指标

1. 课题说明会 Section 1.2 所有"基础 + 进阶"场景可演示。
2. 高级场景中至少 4/5 可演示（除语音输入外全做）。
3. Docker live smoke 仍通过。
4. 全量 pytest 仍通过。
5. Android 不崩溃，能展示所有新增功能。
6. demo runbook 覆盖 8 条主链路。

### 11.2 预期评分提升

| 改进 | 对应维度 | 预计提升 |
|------|---------|--------:|
| 下单确认闭环 | 基础功能 +1 | +1 |
| 主动反问 + 场景组合 | 效果与可靠性 +1 | +1 |
| 多轮上下文补全 | 效果与可靠性稳定 | 0（防扣分） |
| 热门缓存 + 首屏极速 | 工程质量 +2 | +2 |
| TTS + 多模态证据 | 加分项深度 +2 | +2 |
| 证据包 + live smoke | 工程质量可观测 | +1 |
| **合计** | — | **+7** |

目标分数：**93 → 99+**。

---

## 12. 风险与回滚

### 12.1 主要风险

| 风险 | 表现 | 缓解 |
|------|------|------|
| 意图识别误判 | 普通咨询被识别为确认购买 | 只在购物车非空且命中明确关键词时触发 |
| 首屏极速引入竞态 | 投机检索和精检索结果冲突 | 投机结果标记为"初步推荐"，精检索后更新 |
| 场景组合检索过慢 | 多品类串行检索延迟叠加 | 并行检索 + 超时截断 |
| Android 未识别新 action | UI 无法展示确认卡 | reducer 默认兜底 |
| 评委误解为真实下单 | 追问支付合法性 | 文案统一"确认购买意向"，明确无真实支付 |
| live smoke 受 LLM 随机性影响 | 断言失败 | 只断言结构化事件，不断言文案 |

### 12.2 回滚策略

如果某项在赛前不稳定：
1. 不影响其他项继续交付。
2. 不展示该功能。
3. demo runbook 跳过对应 demo。

---

## 13. 变更清单（对照 CLAUDE.md）

本次 PRD 涉及对 CLAUDE.md 裁剪表的调整：

| 原裁剪项 | 调整 |
|---------|------|
| ~~支付与下单确认流程~~ | 改为"真实支付不做，轻量确认购买意向可做" |
| ~~热门查询缓存/首屏极速优化~~ | 放松，本次 PRD 纳入 |
| ~~语音输入~~ | 仍不做（已有拍照找货覆盖多模态） |

CLAUDE.md 已更新对应裁剪表。
