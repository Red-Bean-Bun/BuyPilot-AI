# BuyPilot-AI Demo 视频脚本与 Runbook

> 版本：2026-06-08 v2（基于课题说明会 9 场景全覆盖 + 录播剪辑优化）
> 总时长目标：**4 分 25 秒** | 格式：录屏 + AI 配音 + 字幕
> 剪辑策略：关键帧剪辑 + 加速播放（thinking/loading 4-8x）

---

## 一、视频结构总览

| 段落 | 时长 | 占比 | 内容 |
|------|------|------|------|
| ACT 1 开场 | 8s | 3% | Title card + 一句话定位 |
| ACT 2 Demo 演示（6 条） | 117s | 44% | 9/9 场景全覆盖 |
| ACT 3 技术讲解 | 95s | 36% | 5 个工程深度点：幻觉防御 + 多模态双通道 + 决策评分 + 可观测性 + Prompt 契约 |
| ACT 4 结尾 | 10s | 4% | Checklist 总结 |
| 转场缓冲 | ~35s | 13% | 段落间过渡 |
| **总计** | **~265s ≈ 4m25s** | **100%** | |

---

## 二、场景覆盖矩阵

对照课题说明会 9 个典型用户场景：

| # | 场景 | 难度 | 演示路径 | 状态 |
|---|------|------|---------|------|
| 1 | 单轮模糊推荐 | 基础 | Demo 1 | ✅ |
| 2 | 条件筛选 | 基础 | Demo 1（"200元以内"） | ✅ |
| 3 | 多轮追问与细化 | 进阶 | Demo 3（"预算降到200"） | ✅ |
| 4 | 对比决策 | 进阶 | Demo X | ✅ |
| 5 | 主动反问 | 进阶 | Demo 0 | ✅ |
| 6 | 反选/排除约束 | 高级 | Demo 3（"不要含酒精"） | ✅ |
| 7 | 场景化组合推荐 | 高级 | Demo 0 + Demo 1（品类覆盖） | ✅ |
| 8 | 购物车与下单 | 高级 | Demo 4（add + remove + re-add） | ✅ |
| 9 | 拍照找货（多模态） | 高级 | Demo 2 | ✅ |

---

## 三、ACT 1：开场（0:00 – 0:08）

### 画面

```
┌─────────────────────────────────────────┐
│                                         │
│         🛒 BuyPilot-AI                  │
│    多模态电商智能导购 Agent               │
│                                         │
│  基于 RAG · Android 原生 · FastAPI       │
│                                         │
│  团队成员：XXX / XXX / XXX              │
│  ByteDance AI Full-Stack Challenge 2026 │
│                                         │
└─────────────────────────────────────────┘
```

### 配音/字幕

> "BuyPilot，基于 RAG 的多模态电商智能导购 Agent。把用户模糊的购物需求，转化为可解释的决策路径。下面用 6 条真实购物场景，完整演示 Agent 的能力。"

### 制作要点

- 白底或深色底，简洁干净
- 不要花哨动画，3-5 秒淡入淡出
- 字体：思源黑体或系统默认

---

## 四、ACT 2：Demo 演示（0:08 – 2:05）

### Demo 0 — 主动反问（0:08 – 0:33 | 25s）

**展示能力**：Agent 主动引导用户细化需求（课题说明会示例原话）

| 时间 | 画面 | 配音/字幕 | 速度 |
|------|------|-----------|------|
| 0:08-0:10 | 新 session，用户输入"推荐一款手机"，发送 | "用户说'推荐一款手机'——非常模糊的需求。" | 正常 |
| 0:10-0:18 | thinking 动画 → clarification 事件：反问"请问您更看重拍照、续航还是性价比？" + 3 个选项芯片 | "Agent 没有盲目推荐，而是主动反问：您更看重拍照、续航还是性价比？" | thinking 4x 加速 |
| 0:18-0:22 | 用户点击芯片"拍照优先"，手动输入"预算 4000" | "用户选择拍照优先，预算 4000。" | 正常 |
| 0:22-0:33 | criteria_card → 检索 → product_card（2-3 张手机卡片）→ 推荐文案流式输出 | "标准生成、混合检索、商品推荐一气呵成。" | 检索段 4x 加速，product_card 正常 |

**录制操作**：
1. 打开 App，点"新会话"
2. 输入"推荐一款手机"，发送
3. 等 clarification 出现，点"拍照优先"芯片
4. 输入"预算 4000"，发送
5. 等 product_card + 推荐文案完整输出

**预期 SSE 事件序列**：
```
thinking → clarification(3 options) → [用户回复] → thinking → criteria_card → product_card ×2-3 → text_delta(流式) → done
```

---

### Demo 1 — 模糊推荐 + 证据绑定（0:33 – 0:45 | 12s）

**展示能力**：单轮模糊推荐 + 条件筛选 + 证据可溯源

| 时间 | 画面 | 配音/字幕 | 速度 |
|------|------|-----------|------|
| 0:33-0:36 | 新 session，输入"推荐适合油皮的洗面奶，200元以内" | "油皮洗面奶，200 元以内。" | 正常 |
| 0:36-0:40 | thinking → criteria → product_card 出现 | | 6x 加速 |
| 0:40-0:45 | 点击 product_card 的 evidence 按钮，弹窗展示 chunk 原文（高亮 `p_beauty_018`） | "每个推荐理由都可溯源到商品知识库原文，杜绝幻觉。" | 正常，弹窗停留 3s |

**录制操作**：
1. 新 session
2. 输入"推荐适合油皮的洗面奶，200元以内"
3. 等 product_card 出来
4. 点开 evidence 弹窗，滚动展示 chunk 原文

**预期 SSE 事件序列**：
```
thinking → criteria_card → product_card ×3-5 → text_delta(流式) → done
```

---

### Demo 3 — 反选排除 + 多轮约束（0:45 – 0:57 | 12s）

**展示能力**：否定语义解析 + 多轮上下文记忆 + 约束累积合并

| 时间 | 画面 | 配音/字幕 | 速度 |
|------|------|-----------|------|
| 0:45-0:48 | 新 session，输入"不要含酒精的防晒霜" | "不要含酒精的防晒霜——否定约束。" | 正常 |
| 0:48-0:52 | criteria_card（ingredient_avoid 高亮）→ product_card | | 4x 加速 |
| 0:52-0:54 | 同一 session，输入"预算降到200" | "追加预算约束，Agent 记住上一轮的反选条件。" | 正常 |
| 0:54-0:57 | 新 criteria_card（budget + ingredient_avoid 都高亮）→ 收敛的 product_card | "两轮约束自动合并，候选集精确收敛。" | 4x 加速 |

**录制操作**：
1. 新 session
2. 输入"不要含酒精的防晒霜"
3. 等结果出来
4. 同一 session，输入"预算降到200"
5. 确认 criteria_card 同时显示无酒精 + 预算 200

**预期 SSE 事件序列**：
```
[第 1 轮] thinking → criteria_card(ingredient_avoid) → product_card → text_delta → done
[第 2 轮] thinking → criteria_card(ingredient_avoid + budget) → product_card → text_delta → done
```

---

### Demo 2 — 拍照找货 ⭐⭐⭐（0:57 – 1:22 | 25s）

**展示能力**：多模态图片理解 + VLM + RAG 联合检索

| 时间 | 画面 | 配音/字幕 | 速度 |
|------|------|-----------|------|
| 0:57-1:02 | 点击相机图标，选择护肤品图片上传，显示上传进度 | "拍照找货——上传一张护肤品图片。" | 正常 |
| 1:02-1:05 | 图片上传完成，输入"这个适合敏感肌吗？"，发送 | "结合图片理解和文字提问。" | 正常 |
| 1:05-1:12 | thinking(multimodal) → Qwen-VL 分析结果 | | 4x 加速 |
| 1:12-1:17 | product_card 出现 + 推荐理由流式输出 | "VLM 识别出这是一款含烟酰胺的精华液，RAG 检索匹配适合敏感肌的商品。" | 正常 |
| 1:17-1:22 | 推荐文案完整输出 | "多模态输入无缝接入推荐链路。" | 正常 |

**录制操作**：
1. 新 session
2. 点相机图标，从相册选择一张护肤品图片（提前准备好）
3. 等上传完成
4. 输入"这个适合敏感肌吗？"
5. 等完整推荐结果

**预期 SSE 事件序列**：
```
thinking(multimodal) → thinking(retrieving) → product_card ×2-4 → text_delta(流式) → done
```

---

### Demo 4 — 购物车 CRUD ⭐⭐（1:22 – 1:34 | 12s）

**展示能力**：对话式加购 + 删除 + 再加回 = CRUD 能力

| 时间 | 画面 | 配音/字幕 | 速度 |
|------|------|-----------|------|
| 1:22-1:25 | 在有商品卡的 session 中，输入"把第一个加到购物车" | "对话式加购。" | 正常 |
| 1:25-1:28 | cart_action 事件，购物车状态更新显示 | | 4x 加速 |
| 1:28-1:31 | 输入"删掉刚才那个" | "自然语言删除。" | 正常 |
| 1:31-1:34 | cart_action 删除确认 + 输入"再加回来" → 加购成功 | "加购、删除、再加回来——对话式购物车 CRUD。" | 4x 加速 |

**录制操作**：
1. 使用 Demo 1 或 Demo 0 的 session（已有商品卡）
2. 输入"把第一个加到购物车"
3. 等 cart_action 事件
4. 输入"删掉刚才那个"
5. 等确认
6. 输入"再加回来"

**预期 SSE 事件序列**：
```
cart_action(add, success) → cart_action(remove, success) → cart_action(add, success)
```

---

### Demo X — 多商品对比 ⭐⭐⭐（1:34 – 2:05 | 31s）

**展示能力**：结构化多维对比 + 论证式分析 + 置信度结论

| 时间 | 画面 | 配音/字幕 | 速度 |
|------|------|-----------|------|
| 1:34-1:38 | 在商品卡片上勾选 3 个商品，点击"货比三家"按钮 | "用户选了 3 款商品，点击对比。" | 正常 |
| 1:38-1:44 | thinking(comparing) → compare_card 出现（4 轴对比 + winner + tradeoffs） | | 4x 加速 |
| 1:44-1:52 | compare_card 完整展示：功效维度、价格维度、成分维度、口碑维度，缓慢滚动 | "Agent 自动提取 4 个对比维度，结构化呈现每款商品的优劣。" | 正常，缓慢滚动 |
| 1:52-1:58 | narration 流式输出（对比分析文本） | "流式输出对比分析——不是简单列表，而是有逻辑的论证。" | 正常 |
| 1:58-2:05 | conclusion 结论建议 + confidence=high | "最终给出带置信度的结论建议。多商品对比决策完成。" | 正常 |

**录制操作**：
1. 使用有商品卡的 session
2. 在商品卡片上勾选 2-3 个商品
3. 点击"货比三家"按钮
4. 等 compare_card + narration + conclusion 完整输出

**预期 SSE 事件序列**：
```
thinking(comparing) → compare_card(4 axes + winner + tradeoffs, confidence=high) → text_delta(对比分析) → text_delta(结论建议) → done(completed)
```

---

## 五、ACT 3：技术讲解（2:05 – 3:40 | 95s）

> 设计原则：每个技术点满足三个条件 — ①评委报告已验证（不是自吹）②代码事实可追溯 ③展示工程深度而非广度

### 5.1 总架构 + 数据流（2:05 – 2:25 | 20s）

**画面**：五层架构图 + 数据流图合并为一张，逐步高亮

```
┌──────────────────────────────────────────────────────────────┐
│  BuyPilot 架构 + 数据流                                       │
│                                                              │
│  Android ──▶ Runtime(编排) ──▶ Service(业务) ──▶ Repo(持久化) │
│                                                              │
│  数据流（高亮动画）：                                          │
│  用户输入 → 意图识别 → 标准生成 → 混合检索 → 推荐生成 → SSE   │
│                                      ┌─────────┐             │
│                                      │ SQL硬过滤│             │
│                                      │    ↓    │             │
│                                      │pgvector │             │
│                                      │    ↓    │             │
│                                      │ rerank  │             │
│                                      └─────────┘             │
│  依赖方向：自上而下，AST 自动守卫                             │
│  140 测试 · 54 测试文件 · CI 自动验证                         │
└──────────────────────────────────────────────────────────────┘
```

**配音/字幕**：

> "五层架构，依赖严格单向流动，AST 自动守卫禁止反向依赖。一条数据流贯穿全程：意图识别、标准生成、混合检索、推荐生成。140 个测试、54 个测试文件，GitHub Actions 每次提交自动验证。"

**制作要点**：
- 架构图和数据流图合为一张，避免切换
- 层级用颜色区分，数据流用动画逐步高亮
- 底部标注"AST 守卫 + 140 tests"

---

### 5.2 混合检索 + 幻觉防御（2:25 – 2:55 | 30s）

**画面**：数据流图放大混合检索段 + evidence 弹窗截图

**配音/字幕**：

> **[2:25-2:40]** "检索是防幻觉的核心：SQL 硬过滤先排除不满足约束的商品——品类、预算、品牌、成分，一个都不放过。pgvector 做 1024 维语义召回，qwen3-rerank 精排。三级流水线确保召回的商品 100% 来自库内 100 条真实数据。"
>
> **[2:40-2:55]** "每个推荐理由都绑定到商品知识库的具体 chunk——来自官方 FAQ 还是用户评价，来源类型清晰标注。评委可以点开 evidence 弹窗，看到推荐理由对应的原文段落。不编造商品、不编造价格、不编造优惠——幻觉防御从数据源到展示层全链路覆盖。"

**录制操作**：
1. 数据流图动画高亮混合检索段
2. 切到 Demo 1 的 evidence 弹窗截图，展示 chunk 原文 + source_type 标签

**代码路径**：
- 硬过滤：`backend/src/services/retriever.py:423-480` `_passes_hard_filters`
- pgvector 召回：`backend/src/repos/documents.py:75-137` `list_vector_chunks_by_similarity`
- evidence 绑定：`backend/src/services/evidence.py`

---

### 5.3 多模态双通道检索（2:55 – 3:10 | 15s）

**画面**：双通道检索示意图 + Demo 2 截图

```
                ┌─ 文本通道：text-embedding-v3 → pgvector 语义召回 ─┐
用户图片+文字 ──┤                                                    ├──▶ 合并候选 → 推荐
                └─ 视觉通道：qwen3-vl-embedding → image 相似度召回 ─┘
```

**配音/字幕**：

> "多模态不是简单的图片转文字。我们用 Qwen-VL 理解图片属性，同时用 qwen3-vl-embedding 做 1024 维视觉相似度召回。两条通道并行检索，最终合并到同一批候选商品。100 张商品图片全部预建视觉索引，这是真正的多模态 RAG，不是文本 RAG 的图片预处理。"

**录制操作**：
1. 展示双通道示意图
2. 切到 Demo 2 截图（图片上传 + product_card 结果）

**代码路径**：
- 图片 embedding：`backend/src/services/embedding.py:134-210` `embed_image`
- 视觉召回：`backend/src/repos/documents.py:140-188` `list_products_by_image_similarity`
- 图片索引：`backend/src/scripts/reindex_image_embeddings.py`

**评委验证**：`image_embedding_count=100`、`query_image_embedding_dim=1024`、`trace_visual_recall` 命中（见 `doc/official/0606/live-smoke-visual-recall-evidence-20260606.md`）

---

### 5.4 决策评分算法（3:10 – 3:22 | 12s）

**画面**：决策评分公式 + Android DecisionScoreGauge 环形图截图

```
final_score = retrieval × 0.35
            + criteria_match × 0.25
            + user_signal × 0.25     ← like/swipe/add_to_cart/view_detail
            + evidence × 0.10
            - risk_penalty × 0.05    ← avoid/disliked/ingredient_conflict

confidence: high（用户信号充足）/ medium（分差明显）/ low（需更多反馈）
```

**配音/字幕**：

> "最终决策不是 LLM 拍脑袋。5 维加权公式：检索相关性、标准匹配度、用户行为信号、证据质量、风险扣分。LLM 只解释推荐理由，不负责挑选。置信度根据用户信号数量和候选分差计算，high、medium、low 完全可解释。"

**录制操作**：
1. 展示公式图
2. 切到 Android 决策卡片的 DecisionScoreGauge 环形图（5 维仪表盘）

**代码路径**：
- 评分算法：`backend/src/services/decision_scoring.py:76-133` `score_candidates`
- 置信度：`backend/src/services/decision_scoring.py:136-161` `decision_confidence`
- Android 仪表盘：`android/feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt` `DecisionScoreGauge`

---

### 5.5 Prompt 契约 + 可观测性（3:22 – 3:40 | 18s）

**画面**（4 个快速切换）：

1. **[3:22-3:28]** `backend/prompts/` 目录（12 个 .md 文件）+ `schema_overrides.md` 代码片段
2. **[3:28-3:33]** `contracts/examples/*.sse` golden trace + `jsonschema.validate` 测试代码
3. **[3:33-3:38]** 可观测性：debug bundle 页面 + 上下文诊断 `BUDGET_PATCH_LOST` / `EXCLUSION_LOST`
4. **[3:38-3:40]** 评测看板 Streamlit 总览页

**配音/字幕**：

> **[3:22-3:28]** "12 个 Prompt 模板用 Markdown 版本化管理，运行时热加载，每个模板附带 JSON Schema 约束，强制 LLM 输出符合 Pydantic 模型——不靠模型听话，靠结构保障。"
>
> **[3:28-3:33]** "SSE 事件协议用 JSON Schema 定义，3 个 golden trace 做契约测试，三端对齐自动验证。"
>
> **[3:33-3:40]** "可观测性主动检测'预算约束丢失'和'排除条件丢失'——在用户发现 bug 之前，系统已经告警。LLM 调用、SSE 事件、上下文状态三维度全覆盖。"

**录制操作**：
1. 打开 `backend/prompts/` 目录，快速滚动
2. 打开 `contracts/examples/` 目录
3. 打开 debug bundle 页面，展示一条 context_diagnostics 记录
4. `streamlit run static/eval_dashboard.py`，总览页截图

**代码路径**：
- Prompt 加载：`backend/src/services/prompts.py` `PromptStore`
- Schema Override：`backend/src/services/llm_task_payloads.py:34-44` `_schema_override`
- Golden Trace 测试：`backend/tests/test_sse_events.py` + `backend/tests/test_viewmodel_contract.py`
- 上下文诊断：`backend/src/services/observability.py` `BUDGET_PATCH_LOST` / `EXCLUSION_LOST`

---

### 5.x 评委报告已验证亮点（答辩时可口头引用）

| 评委原话 | 对应技术点 | 报告行号 |
|----------|-----------|---------|
| "不是 mock 项目" | 5.2 混合检索真实性 | 217 |
| "商品事实有边界" | 5.2 幻觉防御 | 218 |
| "多轮与反馈有真实状态" | 5.5 可观测性 | 220 |
| "加分项不是只有目录名" | 5.3 多模态双通道 | 221 |
| "多模态视觉召回已验证" | 5.3 视觉索引 | 275 |

---

## 六、ACT 4：结尾（3:40 – 3:50）

### 画面

```
┌─────────────────────────────────────────────────┐
│                                                 │
│              BuyPilot-AI                        │
│                                                 │
│  ✅ 9/9 用户场景全覆盖                          │
│  ✅ 原生 Android + FastAPI + pgvector           │
│  ✅ 混合检索三级流水线 + 证据绑定 + 零幻觉      │
│  ✅ 多模态双通道：文本 RAG + 视觉相似度召回     │
│  ✅ 5 维决策评分算法，LLM 只解释不挑选          │
│  ✅ 140 测试 + 12 个 Prompt 模板 + 契约测试      │
│  ✅ docker-compose up 一键启动                  │
│                                                 │
│  github.com/Red-Bean-Bun/BuyPilot-AI           │
│                                                 │
└─────────────────────────────────────────────────┘
```

### 配音/字幕

> "9 个场景全覆盖，混合检索三级流水线加证据绑定杜绝幻觉，多模态双通道并行检索，5 维决策评分让 LLM 只解释不挑选，140 个测试加契约测试保障工程质量，docker-compose 一键启动。"

### 制作要点

- 白底黑字，简洁
- 每条 ✅ 逐条淡入（配合配音节奏）
- 最后显示 GitHub repo URL
- BGM 渐弱结束

---

## 七、录制清单

| # | 录制内容 | 操作要点 | 预计原始时长 | 剪辑后 |
|---|---------|---------|-------------|--------|
| Demo 0 | 主动反问 | 新 session → "推荐一款手机" → 点芯片 → "预算4000" | ~60s | 25s |
| Demo 1 | 模糊推荐 + evidence | 新 session → 输入完整 prompt → 点 evidence 弹窗 | ~45s | 12s |
| Demo 3 | 反选 + 多轮 | 新 session → "不要含酒精的防晒霜" → "预算降到200" | ~50s | 12s |
| Demo 2 | 图片找货 | 新 session → 上传图片 → "这个适合敏感肌吗？" | ~60s | 25s |
| Demo 4 | 购物车 CRUD | 有商品卡的 session → "加购物车" → "删掉" → "再加回来" | ~40s | 12s |
| Demo X | 对比决策 | 有商品卡的 session → 勾选 3 个 → 点"对比" | ~90s | 31s |
| 架构+数据流图 | 静态素材 | PPT/Figma/draw.io 提前制作（合并为一张） | — | 20s |
| 混合检索+幻觉防御 | 数据流图动画 + evidence 弹窗截图 | 动画高亮三级流水线 | — | 30s |
| 多模态双通道 | 双通道示意图 + Demo 2 截图 | 展示 text + image 两条通道 | — | 15s |
| 决策评分公式 | 静态素材 + Android 仪表盘截图 | 展示 5 维公式 + DecisionScoreGauge | — | 12s |
| Prompt 目录 | 录屏 | 打开 `backend/prompts/` 快速滚动 | ~10s | 6s |
| 评测看板 | Streamlit 录屏 | `streamlit run static/eval_dashboard.py` | ~30s | 5s |
| 可观测性 | 录屏 | 打开 debug bundle + context_diagnostics | ~30s | 7s |
| CI 输出 | 终端录屏 | `uv run pytest -q` + `uv run ruff check` | ~20s | 备用 |

---

## 八、素材制作清单

| 素材 | 工具 | 负责人 | 截止 |
|------|------|--------|------|
| Title card | Figma / PPT | | 录制前 |
| 架构+数据流图（合并） | draw.io / Figma | | 录制前 |
| 混合检索三级流水线（放大动画） | draw.io / Figma | | 录制前 |
| 多模态双通道示意图 | draw.io / Figma | | 录制前 |
| 决策评分公式 + 5 维仪表盘 | draw.io / Figma | | 录制前 |
| 结尾 Checklist | Figma / PPT | | 录制前 |
| AI 配音脚本 | 本文档 | — | 已完成 |
| BGM | 免版权音乐（推荐 Pixabay / Mixkit） | | 剪辑前 |

---

## 九、剪辑注意事项

1. **加速播放标注**：每个 4x/6x 加速段加半透明"⏩"角标，评委秒懂
2. **转场**：段落间用 0.5s 黑场过渡，不花哨
3. **字幕样式**：底部居中，白色描边黑字或半透明黑底白字
4. **分辨率**：1080p，帧率 30fps
5. **导出格式**：MP4 (H.264)，码率 ≥ 8Mbps
6. **AI 配音**：选用自然中文语音（推荐：讯飞 / 微软 Azure TTS / 11Labs）

---

## 十、Demo 路径与场景覆盖追溯

| Demo | 覆盖的课题说明会场景 | 对应加分项 |
|------|---------------------|-----------|
| Demo 0 | 主动反问（进阶） | 4.3 对话智能 ⭐ |
| Demo 1 | 单轮模糊推荐（基础）+ 条件筛选（基础） | — |
| Demo 3 | 反选/排除约束（高级）+ 多轮追问与细化（进阶） | 4.3 反选与排除 ⭐⭐ |
| Demo 2 | 拍照找货（高级） | 4.2 多模态 ⭐⭐⭐ |
| Demo 4 | 购物车与下单（高级） | 4.1 购物车管理 ⭐⭐ |
| Demo X | 对比决策（进阶） | 4.3 多商品对比 ⭐⭐⭐ |

**加分项覆盖汇总**：

| 加分方向 | 目标档位 | 演示覆盖 |
|----------|---------|---------|
| 4.1 业务闭环 | ⭐⭐ 购物车管理 | Demo 4（add + remove + re-add） |
| 4.2 多模态交互 | ⭐⭐⭐ 拍照找货 | Demo 2 |
| 4.3 对话智能 | ⭐⭐ 反选排除 + ⭐⭐⭐ 多商品对比 | Demo 3 + Demo X |
| 4.4 工程质量 | 不做 | — |

---

## 十一、答辩追问备用技术点

> 以下技术点不在视频主线中，但评委追问时可直接引用。每个点都有代码路径 + 一句话话术。

### 11.1 检索松弛策略：宁可不推荐，不可推错品类

**一句话**：当硬过滤返回空集时，我们只放宽预算上限，绝不放宽品类或成分约束。展示一条 220 元的防晒给预算 200 的用户是有帮助的，但展示一条洗面奶给要防晒的用户是灾难。

**代码路径**：`backend/src/services/retriever.py:340-367` `_relaxation_attempts`

```python
# category and product_type are hard constraints — showing a 防晒 for a 洁面 request
# is worse than showing nothing. budget_max is a threshold constraint — showing a
# 220 CNY item for a 200 CNY budget is helpful with a note.
```

---

### 11.2 语义 Chunking：不是暴力切分，是按知识角色结构化

**一句话**：100 个商品生成 1292 个 chunk，每个 chunk 带有 `retrieval_role` 标记——marketing/faq/review/warning/compare。负评和风险 chunk 不进入主召回，只在 evidence 绑定时作为风险提示展示。

**代码路径**：
- `backend/src/services/chunking.py` `SemanticChunk` + `_summary_sentences`
- `backend/src/repos/models.py` `ProductChunk.chunk_metadata` → `retrieval_role`
- 检索排除：`backend/src/repos/documents.py:106` `COALESCE(pc."metadata"->>'retrieval_role', '') <> 'risk'`

---

### 11.3 反馈闭环：用户行为直接进入检索硬过滤

**一句话**：用户的"不喜欢""不要这个"不是记在日志里就完了——`avoid_products` 和 `avoid_traits` 直接进入下一轮检索的 SQL 硬过滤条件，形成真正的闭环。

**代码路径**：
- 反馈写入：`backend/src/services/feedback.py` → `repos/feedbacks.py`
- 反馈读取：`backend/src/services/retriever.py:524-530` `_retrieval_filters`
- 硬过滤：`backend/src/services/retriever.py:427-436` `_passes_feedback_product_filter`

---

### 11.4 Convergence 协议：解决"帮我选"被误分类的真实 bug

**一句话**：前端收敛状态机判断用户要收敛时，显式传 `converge=true`，后端跳过 LLM 意图分类，强制路由到 continue handler。这解决了一个真实 bug——用户说"帮我选"被 LLM 误分类为 recommend，导致前端收敛契约失败。

**代码路径**：
- 前端：`ChatViewModel.kt` `convergeProductDeck()` → `startRealStream(converge=true)`
- 后端：`ChatStreamRequest.converge` 字段 → `runtime/pipeline.py` 强制 continue
- 测试：`test_viewmodel_converge_flag_forces_continue_intent`

---

### 11.5 Strict Runtime 模式：开发态和生产态的开关

**一句话**：`STRICT_RUNTIME=1` 禁用所有确定性 fallback——embedding 必须 1024 维真实向量，rerank 必须真实 API，LLM 坏 JSON 直接报错而非静默兜底。开发时用 fallback 快速迭代，部署时开 strict 暴露真实问题。

**代码路径**：
- `backend/src/config/settings.py` `STRICT_RUNTIME`
- embedding：`backend/src/services/embedding.py` strict 下禁用 deterministic fallback
- rerank：`backend/src/services/reranker.py` strict 下禁用 deterministic rerank

---

### 11.6 品类感知 Chunking + 对比轴自动选择

**一句话**：不同品类的 chunk 策略不同——美妆护肤关注肤质匹配和成分风险，数码电子关注核心参数和性能。对比决策时，4 个对比轴根据品类自动选择，不是固定模板。

**代码路径**：`backend/src/services/chunking.py:46-50` `COMPARE_AXES`

```python
COMPARE_AXES = {
    "美妆护肤": ("肤质匹配", "成分风险", "使用场景", "价格"),
    "数码电子": ("核心参数", "性能", "续航", "价格"),
    "服饰运动": ("运动场景", "材质/脚感", "尺码适配", "价格"),
    "食品生活": ("配料", "糖分/热量", "储存方式", "价格"),
}
```

---

### 11.7 否定语义解析：标点感知的作用域否定检测

**一句话**："不要含酒精的"和"含有酒精的"——同样包含"酒精"，但语义完全相反。我们实现了标点感知的作用域否定检测：否定前缀（不/没有/不要/不含）只在当前标点句法作用域内生效，且遇到"但是""然而"等作用域断裂词时自动终止。这是纯函数实现，有 ≥3 个直接测试用例。

**代码路径**：`backend/src/config/domain_terms.py:353-383`

```python
NEGATION_PREFIXES = ("不", "没", "不要", "不含", "无", "别", "排除", "拒绝")
NEGATION_SCOPE_BREAKERS = ("但", "但是", "不过", "然而", "却", "含有", "添加")

def has_negation_prefix(text: str, term: str) -> bool:
    # 在当前标点作用域内查找否定前缀
    # 遇到 scope breaker 则否定失效
    # "不要酒精但含烟酰胺" → 酒精被否定，烟酰胺不被否定
```

**为什么值得说**：
- 大多数团队用 LLM 处理否定语义，我们用的是确定性代码——更快、更可靠、可测试
- `extract_terms()` 和 `extract_avoided_traits()` 共享同一个否定检测引擎
- 评委追问"不要含酒精的"怎么实现的，可以直接展示这段代码

---

### 11.8 购物车目标解析：Android 端模糊匹配 + 序数词提取

**一句话**：用户说"把第一个加到购物车"或"把那个洗面奶加进去"——Android 端用模糊匹配算法解析目标商品。支持序数词提取（"第一个""第二款"）、品牌/品类/名称多维匹配打分、指代词解析（"这个""这款"）。匹配算法在 Kotlin 端实现，有完整的评分机制和歧义检测。

**代码路径**：`android/feature/chat/src/main/java/com/buypilot/feature/chat/ChatViewModel.kt:144-262`

```kotlin
// resolveAddToCartTarget: 解析用户指代的目标商品
// - 序数词提取: "第一个" → rank=1
// - 模糊匹配打分: productId 120分 > 名称 80分 > 品牌 60分 > 品类 45分
// - 歧义检测: 如果两个候选得分相同，返回 null（不猜）
```

**为什么值得说**：
- 自然语言购物车操作的核心难点不是意图识别，而是**目标解析**
- 大多数团队只做"加到购物车"的意图识别，我们做到了**精确到哪个商品**
- 歧义检测：两个候选得分相同时不猜测，而是要求用户澄清

---

### 11.9 检索缓存：热点 Key 追踪 + 智能淘汰

**一句话**：检索结果缓存带 TTL，但不是简单的 LRU——我们追踪每个 key 的命中次数，命中 ≥3 次的标记为热点 key 并自动延长 TTL。淘汰优先级：过期 → 最低命中数 → 最早访问时间。这是生产级缓存策略。

**代码路径**：`backend/src/services/retrieval_cache.py` + `backend/tests/test_retrieval_cache.py`（198 行测试）

**为什么值得说**：
- 评委追问"如果同一个查询被多次发送怎么办"时的完美回答
- 热点 key 自动延长 TTL，避免高频查询反复调用 LLM 和 pgvector
- 198 行测试覆盖了命中计数、热点检测、TTL 延长、智能淘汰、批量过期清理

---

### 11.10 SSE 铁律 + 三端自动化守卫

**一句话**：SSE 事件协议是封闭 DSL——10 种 event type 是全集。我们不仅定义了铁律，还用 **import-time guard + build-time test + CI 脚本** 三层自动化守卫强制执行：Python 端 import 模块时自动校验 Schema 一致性（不一致则 uvicorn 拒绝启动），Kotlin 端每次 `./gradlew assembleDebug` 强制跑协议测试（不一致则 BUILD FAILED），CI 层 `make protocol-check` 一次命令校验三端。类比 Rust 的 borrow checker——**漂移 = 程序无法启动/编译**，不是"测试可能发现"。

**代码路径**：
- Schema source of truth：`contracts/sse-events.schema.json`
- Python import-time guard：`backend/src/types/sse_events.py:374-421` `_verify_protocol_consistency()`
- Kotlin build-time guard：`android/core/model/build.gradle.kts`（强制 assemble 依赖 testDebugUnitTest）
- Kotlin 测试实现：`android/core/model/src/test/java/.../AgentEventTypeProtocolTest.kt`
- 跨语言校验脚本：`scripts/check_sse_protocol.py`（`make protocol-check`）
- Python 端守卫测试：`backend/tests/test_protocol_guards.py`
- Golden trace 测试：`backend/tests/test_sse_events.py`（`TestSchemaValidation` + `TestGoldenTraceSemanticValidation`）

**三层守卫机制**：

| 层 | 守卫 | 时机 | 失败后果 |
|---|------|------|---------|
| Python import-time | `_verify_protocol_consistency()` | 任何代码 import sse_events 时 | uvicorn 拒绝启动 |
| Kotlin build-time | `AgentEventTypeProtocolTest` | `./gradlew assembleDebug` 自动触发 | BUILD FAILED |
| Cross-language CI | `make protocol-check` | CI / 手动 | exit code 1 |

**为什么值得说**：
- 大多数团队的协议一致性靠人工纪律（"记得同步改三端"），我们靠系统强制——**漂移 = 程序无法启动/编译**
- 类比 Rust：borrow checker 不需要你主动 `cargo check`，它在你 `cargo build` 时自动运行。我们的 guard 在你 `import` / `./gradlew assemble` 时自动运行
- 评委追问"你们怎么保证前后端协议一致"时的完美回答
- 这个铁律在项目 CLAUDE.md 中明确定义为"铁律 1"，违反即架构错误

**答辩话术**：
> "SSE 事件协议是封闭 DSL，10 种 event type 是全集。我们不只写了铁律文档，还实现了三层自动化守卫：Python 端 import 时自动校验 Schema 一致性，不一致则服务无法启动；Kotlin 端每次编译 APK 自动跑协议测试，不一致则编译失败；CI 层一条命令校验三端。"用制度保证质量"——不是靠人的自觉，而是靠系统的强制.这就像 Rust 的 borrow checker——把容易出错的人工纪律变成系统强制的编译时检查。"
