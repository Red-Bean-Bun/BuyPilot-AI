# Demo Runbook — 2026-06-08

> 版本：v2.1（对齐真实数据 + 技术讲解亮点）
> 日期：2026-06-08
> 目的：供评委或答辩人按步骤操作 Demo 路径，验证全链路可演示
> 视频脚本：`doc/Delivery materials/demo-video-runbook.md`

---

## 场景覆盖矩阵

对照课题说明会 9 个典型用户场景：

| # | 场景 | 难度 | 演示路径 | 状态 |
|---|------|------|---------|------|
| 1 | 单轮模糊推荐 | 基础 | Demo 1 | ✅ |
| 2 | 条件筛选 | 基础 | Demo 1（"200元以内"） | ✅ |
| 3 | 多轮追问与细化 | 进阶 | Demo 3（"预算降到1200"） | ✅ |
| 4 | 对比决策 | 进阶 | Demo X | ✅ |
| 5 | 主动反问 | 进阶 | Demo 0 | ✅ |
| 6 | 反选/排除约束 | 高级 | Demo 3（"不要耐克的"） | ✅ |
| 7 | 场景化组合推荐 | 高级 | Demo 5（Bonus） | ✅ |
| 8 | 购物车与下单 | 高级 | Demo 4（add + remove + re-add） | ✅ |
| 9 | 拍照找货（多模态） | 高级 | Demo 2 | ✅ |

---

## Demo 0：主动反问（进阶场景）

| 项目 | 内容 |
|------|------|
| 目标 | 验证信息不足时主动澄清：clarification 事件 + required_slots/suggested_options |
| 输入话术 | `推荐一款手机` |
| 预期 SSE 事件 | `thinking`(understanding) → `criteria_card`(pending: 数码电子/智能手机，无预算) → `thinking`(clarifying) → `text_delta`(分析文案) → `clarification`(required_slots: budget，suggested_options: 1000-2000元/2000-4000元/4000元以上) → `done(completed)` |
| 第二轮输入 | 输入 `拍照优先，预算8000` |
| 第二轮预期 | `thinking` → `criteria_card`(数码电子/8000元内/智能手机) → `product_card`×5 → `text_delta`(推荐文案) → `done` |
| 预期商品类别 | 数码电子 > 智能手机；此轮不检索，等用户选择后再触发 |
| Android 展示点 | 先出现 pending 态 Criteria 卡（只有品类芯片）；随后 Clarification 卡片展示预算选项按钮；用户点击后注入下一轮 criteria |
| 失败兜底话术 | `如果你还没想好预算，我可以先按主流价位推荐` |

**技术讲解关联**：slot_checker 检测 required slots（category/scenario）缺失时触发反问，这是确定性规则而非 LLM 判断。

---

## Demo 1：模糊推荐 + 证据绑定（基础场景）

| 项目 | 内容 |
|------|------|
| 目标 | 验证模糊推荐 + 条件筛选 + 证据可溯源：意图识别 → 购买标准 → 混合检索 → 推荐生成 → 商品卡渲染 |
| 输入话术 | `推荐适合油皮的洗面奶，200元以内` |
| 预期 SSE 事件 | `thinking`(understanding) → `text_delta`(开场白) → `thinking`(criteria→ranking) → `product_card`×1-3 → `criteria_card`(chips: 美妆护肤/油性肌肤/200元内/洁面) → `thinking`(decision) → `final_decision` → `done(completed)` |
| 预期商品类别 | 美妆护肤 > 洁面；当前数据集匹配到珊珂洁面 p_beauty_011 等候选 |
| Android 展示点 | Criteria 卡片显示筛选条件 chips；商品卡展示证据标签；**点开 evidence 弹窗，展示 chunk 原文（高亮 `p_beauty_018`）**；决策卡展示 winner + 理由 |
| 失败兜底话术 | 若检索无结果：`当前资料没有找到完全匹配的商品，建议放宽预算或肤质条件` |

**技术讲解关联**：
- 混合检索三级流水线：SQL 硬过滤（品类/预算/肤质）→ pgvector 1024 维语义召回 → qwen3-rerank 精排
- 证据绑定：每个 product_card 的 evidence 可溯源到具体 chunk（marketing_description/official_faq/user_reviews）

---

## Demo 3：反选排除 + 多轮约束（高级 + 进阶场景）

| 项目 | 内容 |
|------|------|
| 目标 | 验证否定语义解析 + 多轮上下文记忆 + 约束累积合并 |
| 第一轮输入 | `推荐跑步鞋，不要耐克的` |
| 第一轮预期 SSE | `thinking` → `criteria_card`(服饰运动/不要耐克/跑步鞋, brand_avoid=["耐克"]) → `product_card`×3（特步 ¥999、HOKA ¥1099、阿迪达斯 ¥1399，耐克被排除） → `text_delta` → `done` |
| 第二轮输入 | `预算降到1200`（同一 session） |
| 第二轮预期 SSE | `thinking` → `criteria_card`(服饰运动/1200元内/不要耐克/跑步鞋, brand_avoid=["耐克"], budget_max=1200) → `product_card`×1（HOKA ¥1099） → `text_delta` → `done` |
| 预期 criteria 变化 | 第二轮 criteria 自动合并第一轮的 brand_avoid + 新增 budget_max，**两轮约束累积不丢失**；候选从 3 个收敛到 1 个 |
| Android 展示点 | 第一轮 Criteria 卡 chips 显示"不要耐克"；第二轮 chips 同时显示"1200元内"+"不要耐克"；商品卡候选集精确收敛 |
| 失败兜底话术 | `如果候选变化不大，可以试试换一个排除条件或放宽预算` |

**技术讲解关联**：
- 否定语义解析：`has_negation_prefix()` 检测"不要/不含/没有"前缀，标点感知作用域，`domain_terms.py:353-383`
- 多轮约束累积：`criteria.py` 合并历史 criteria + 本轮 patch，列表约束（brand_avoid/ingredient_avoid）累积去重；hard constraint 字段（brand_avoid/budget 等）从历史继承
- 反馈闭环：用户"不喜欢"直接进入下一轮检索的 SQL 硬过滤条件

---

## Demo 2：拍照找货 ⭐⭐⭐（高级场景）

| 项目 | 内容 |
|------|------|
| 目标 | 验证多模态双通道检索：VLM 属性理解 + image embedding 视觉召回 + 文本 RAG |
| 输入话术 | 上传一张护肤品图片 + `这个适合敏感肌吗` |
| 预期 SSE 事件 | `thinking(analyzing_image)` ×2 → `thinking`(understanding) → `text_delta`(开场白) → `thinking`(criteria→ranking) → `product_card`×1-3 → `criteria_card`(美妆护肤/敏感肌肤) → `thinking`(decision) → `final_decision` → `done(completed)` |
| 预期商品类别 | 美妆护肤；VLM 从图片识别品类 + 属性，"敏感肌"从文本注入 skin_type=敏感 |
| Android 展示点 | 图片缩略图在用户气泡展示；商品卡展示图片分析后召回的候选与证据标签；后端 trace 可核对 visual_recall |
| 失败兜底话术 | `图片识别不够稳定，可以换一张更清晰的商品正面图再试` |

**技术讲解关联**：
- 多模态双通道：文本 RAG（text-embedding-v3 → pgvector）+ 视觉召回（qwen3-vl-embedding → image 相似度）
- 100 张商品图片全部预建 1024 维视觉索引，`product_image_embeddings` 表
- 评委验证记录：`doc/official/0606/live-smoke-visual-recall-evidence-20260606.md`

---

## Demo 4：购物车 CRUD ⭐⭐（高级场景）

| 项目 | 内容 |
|------|------|
| 目标 | 验证对话式购物车 CRUD：add → remove → re-add + 购买意向确认 |
| 前置 | 完成 Demo 1 或 Demo 0，已有推荐结果 |
| 第 1 步输入 | `把第一个加入购物车` |
| 第 1 步预期 SSE | `thinking`(understanding) → `cart_action`(add, status=success, cart.total_items=1) → `done(completed)` |
| 第 2 步输入 | `删掉刚才那个` |
| 第 2 步预期 SSE | `thinking`(understanding) → `cart_action`(remove, status=success, cart.total_items=0) → `done(completed)` |
| 第 3 步输入 | `再加回来` |
| 第 3 步预期 SSE | `thinking`(understanding) → `cart_action`(add, status=success, cart.total_items=1) → `done(completed)` |
| 第 4 步输入（可选） | `就买这个` → 点击"确认购买意向" |
| 第 4 步预期 SSE | `cart_action`(checkout_preview) → `done` → [确认] → `cart_action`(checkout_confirm) → `done` |
| Android 展示点 | 每次操作显示 CartActionCard 回执；打开购物车 sheet 查看明细；CheckoutPreviewCard 展示商品明细 + 确认/取消按钮 |
| 注意事项 | 购物车操作不涉及真实支付、不生成订单、不清空购物车；文案统一为"购买意向确认" |
| 失败兜底话术 | `购物车操作没有成功，可以试试直接说"查看购物车"` |

**技术讲解关联**：
- 购物车目标解析：Android 端 `resolveAddToCartTarget()` 模糊匹配（序数词提取 + 品牌/品类/名称多维打分 + 歧义检测），`ChatViewModel.kt:144-262`
- 购买意向闭环：checkout_preview/confirm/cancel 复用 `cart_action` 事件，不接真实支付

---

## Demo X：对比决策 ⭐⭐⭐（进阶场景）

| 项目 | 内容 |
|------|------|
| 目标 | 验证多商品对比决策：compare_card + narration + conclusion |
| 前置 | 先用 `推荐拍照好的手机，预算8000` 触发推荐（返回 6 款手机：OPPO Reno ¥3299、小米 ¥6499、华为 Pura ¥6999、OPPO Find ¥6999、vivo ¥6999、小米 Ultra ¥7499） |
| 输入话术 | 在商品卡片上勾选 2 个商品，点击"货比三家"按钮；或输入 `对比第一个和第二个` |
| 预期 SSE 事件 | `thinking(comparing)` → `compare_card`(4 axes + winner + tradeoffs, confidence=high) → `text_delta`(对比分析) → `text_delta`(结论建议) → `done(completed)` |
| 预期对比数据 | OPPO Reno 16 Pro vs 小米 17 Max。axes: 核心参数(75 vs 75)/性能(60 vs 50)/续航(75 vs 75)/价格(¥3299 vs ¥6499)。winner=OPPO Reno 16 Pro，tradeoffs: OPPO 在性能和价格上更有优势 |
| Android 展示点 | CompareSummaryCard 展示对比表格 + 雷达图/评分条 + 胜出者 + 取舍说明 + narration 文本 |
| 失败兜底话术 | `对比需要至少两个候选，先推荐一些商品再对比` |

**技术讲解关联**：
- 品类感知对比轴：`COMPARE_AXES` 根据品类自动选择 4 个对比维度（美妆=肤质/成分/场景/价格，数码=参数/性能/续航/价格）
- 对比评分基于 evidence 真实数据，不是 LLM 拍脑袋；同品类对比（手机 vs 手机）置信度为 high

---

## Demo 5：场景化组合推荐（Bonus，不在视频主线）

| 项目 | 内容 |
|------|------|
| 目标 | 验证 travel 场景化组合推荐：跨品类 criteria + 组合推荐 |
| 输入话术 | `下周去三亚度假，帮我搭配一套从防晒到穿搭的方案` |
| 预期 SSE 事件 | `thinking`(understanding) → `text_delta`(开场白) → `criteria_card`(美妆护肤/度假/防晒, shopping_strategy.scene_type=travel) → `thinking`(ranking) → `product_card` ×6 → `text_delta`(推荐解释) → `done(awaiting_product_feedback)` |
| 预期商品类别 | 美妆护肤(防晒 ×3) + 服饰运动(T恤 ×3)；实际返回 6 个跨品类候选 |
| Android 展示点 | Criteria 卡 chips 展示"度假""防晒"；商品卡涵盖多品类；shopping_strategy 标记为 travel 场景 |
| 失败兜底话术 | `跨品类检索还在完善，当前先给出防晒方向的推荐` |

---

## 执行顺序建议

### 视频录制顺序（6 条路径，4 分钟内）

```
Demo 0 → Demo 1 → Demo 3 → Demo 2 → Demo 4 → Demo X
```

### 现场答辩演示顺序（如需 live demo）

```
Demo 1（基础能力）→ Demo 3（多轮+反选）→ Demo 2（多模态）→ Demo 4（购物车）
```

先走通基础链路（P0 核心），再做高级场景，最后对比。Demo 0 和 Demo X 根据时间选做。

---

## 技术讲解亮点速查

> 详见视频脚本 ACT 3（`doc/Delivery materials/demo-video-runbook.md`）

| 编号 | 技术点 | 时长 | 对应 Demo | 评委报告验证 |
|------|--------|------|----------|-------------|
| 5.1 | 总架构 + 数据流 | 20s | 全局 | "不是 mock 项目" |
| 5.2 | 混合检索 + 幻觉防御 | 30s | Demo 1 | "商品事实有边界" |
| 5.3 | 多模态双通道检索 | 15s | Demo 2 | "视觉召回已验证" |
| 5.4 | 决策评分算法 | 12s | Demo X | "结构化返回足够支撑 Android" |
| 5.5 | Prompt 契约 + 可观测性 | 18s | 全局 | "多轮与反馈有真实状态" |

### 答辩追问备用（10 个点）

| 编号 | 追问场景 | 技术点 |
|------|---------|--------|
| 11.1 | "检索结果为空怎么办" | 检索松弛策略（只放宽预算，不放宽品类） |
| 11.2 | "数据怎么处理的" | 语义 Chunking（1292 chunk，按角色标记） |
| 11.3 | "多轮怎么记住上一轮" | 反馈闭环（avoid_products 进入 SQL 硬过滤） |
| 11.4 | "'帮我选'被误分类" | Convergence 协议（前端 converge=true 强制 continue） |
| 11.5 | "开发态和生产态区分" | Strict Runtime（禁用确定性 fallback） |
| 11.6 | "不同品类策略一样吗" | 品类感知 Chunking + 对比轴自动选择 |
| 11.7 | "'不要含酒精'怎么实现" | 否定语义解析（标点感知作用域检测） |
| 11.8 | "'把第一个加进去'怎么理解" | 购物车目标解析（模糊匹配 + 歧义检测） |
| 11.9 | "重复查询怎么处理" | 检索缓存（热点 key + 智能淘汰） |
| 11.10 | "前后端协议怎么保证一致" | SSE 铁律 + 三层自动化守卫（Python import-time 启动即校验 / Kotlin assemble 强制跑测试 / CI 三端联合校验） |

---

## 注意事项

1. **API 认证（必须）**：所有 `/chat/stream` 请求必须携带 Bearer Token：`Authorization: Bearer <ADMIN_API_KEY>`。Android 客户端已在 `AdminAuthInterceptor` 中自动注入；若用 curl 或 Postman 测试，必须手动加 Header。
2. 每条 Demo 开始前确认后端已启动且 SSE 连接正常。
3. Demo 3 的第二轮必须在 Demo 3 第一轮的**同一会话**中执行（复用 session_id），否则无法触发多轮上下文。
4. Demo 4 的购买意向确认文案必须统一为"确认购买意向"，不能出现"下单""支付""订单"等误导性表述。
5. Demo 2 需要准备一张清晰的护肤品商品图。
6. 如果 LLM 响应不稳定，可以重试，但记录重试次数。

---

## Pre-flight 检查清单

```bash
# 1. 后端健康
curl localhost:8000/health

# 2. 数据库 chunks 存在
docker compose exec postgres psql -c "SELECT count(*) FROM product_chunks"
# 预期：1292

# 3. 图片 embedding 存在
docker compose exec postgres psql -c "SELECT count(*) FROM product_image_embeddings"
# 预期：100

# 4. Demo smoke 通过
cd backend && uv run -m src.scripts.demo_smoke

# 5. 全量测试通过
cd backend && uv run pytest -q
# 预期：140+ passed
```

---

## 历史修复记录

### v1.8 修复记录（2026-06-08 验证发现）

| # | Bug | 根因 | 修复 |
|---|-----|------|------|
| 1 | `budget_min` 不显示在 criteria chips 中 | `_constraint_chips` 只处理了 `budget_max`，漏了 `budget_min` | `backend/src/runtime/stages/criteria.py:230-231` 新增 `budget_min` 分支 |
| 2 | "确认购买"按钮点击无响应 | `sendMessage` 的 convergence 守卫把"确认购买"误判为"收敛"指令 | `ChatViewModel.kt:979-982` 显式拦截 `checkout_confirm`/`checkout_cancel` |
| 3 | 对比结论流式输出报 KeyError | `llm_profiles.yaml` 未注册 `generate_comparison_conclusion` task | `backend/src/config/llm_profiles.yaml` 新增该 task 映射 |
| 4 | 对比表格轴评分全部显示"基本够用" | `_score_from_evidence` 用关键词匹配，所有轴命中同一条 chunk | `backend/src/services/compare.py` 新增三个专用 scorer |

### v2.0 变更记录（2026-06-08）

- 对齐视频脚本 6 路径结构（Demo 0/1/3/2/4/X）
- 新增场景覆盖矩阵（9/9 场景全覆盖）
- 新增技术讲解亮点速查表（5 个视频点 + 10 个追问备用）
- 新增 Pre-flight 检查清单
- 新增 SSE 铁律三层自动化守卫（Python import-time / Kotlin assemble 强制测试 / CI 三端校验）
- 合并旧 Demo 2（多轮决策）到新 Demo 3（反选+多轮）
- 合并旧 Demo 7（购买意向确认）到新 Demo 4（购物车 CRUD）

### v2.1 数据对齐修正（2026-06-08）

基于 100 条真实商品数据逐条 curl 验证，修正预期行为使其与实际输出一致：

| # | 路径 | 修正前 | 修正后 | 原因 |
|---|------|--------|--------|------|
| 1 | Demo 0 R2 | "预算4000"，product_card×2-3 | "预算8000"，product_card×5 | ¥4000 内智能手机仅 1 款（OPPO ¥3299），¥8000 内有 5 款 |
| 2 | Demo 3 R1 | "不要含酒精的防晒霜"，product_card×2-4 | "推荐跑步鞋，不要耐克的"，product_card×3 | 防晒品类仅 3 款，排除酒精后剩 1 款；跑步鞋 4 款，排除耐克后剩 3 款，演示效果更清晰 |
| 3 | Demo 3 R2 | "预算降到200" | "预算降到1200" | 对齐跑步鞋场景；验证 brand_avoid 跨轮累积 + budget 新增 |
| 4 | Demo X 前置 | "我要买电子产品"（返回混合品类） | "推荐拍照好的手机，预算8000"（返回 6 款手机） | 同品类对比 confidence=high；跨品类（耳机 vs 手机）只有 medium |
| 5 | Demo 5 品类 | "防晒 + T恤/短裤" | "防晒 ×3 + T恤 ×3" | 数据集中无短裤品类触发 combo 检索 |
