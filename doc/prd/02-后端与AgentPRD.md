<title>BuyPilot-AI 后端 &amp; Agent PRD</title>

# 泛电商智能导购 Agent — 后端与 Agent 模块需求规格

---

## **1. 项目定位**



**一句话定位**：利用混合 RAG 架构，构建一个能将用户模糊购物渴望精准转化为可量化购买标准，通过多模态解析实现强可解释性推荐，并自带端到端质量评测进化闭环的决策型导购 Agent。



**品类策略**：选择亲子玩具作为深水区验证品类，验证该导购决策架构在泛电商中的可迁移性。前台展示跨品类导购能力，后台真正跑深的是 Toys & Games。



**答辩叙事顺序**：

1. 为什么选玩具？——家长购买决策的痛点最强（年龄安全、教育价值、场景适配）
2. 玩具购买为什么更需要决策辅助？——不是关键词搜索能解决的问题
3. 家长关心的是年龄、安全、教育价值、使用场景，不是商品数量
4. 我们用 Toys 作为验证品类，实现一套可迁移到泛电商的导购决策架构

**核心差异化**：

- 购买标准生成（模糊需求 → 结构化决策标准）
- 需求澄清机制（硬规则兜底 + LLM 增强）
- 混合检索防幻觉（硬过滤 + 软偏好向量召回 + Rerank + 证据绑定）
- 流式推进 + Agent-to-UI 结构化卡片（可编辑修正）
- 流水线式并行化执行（首字延迟 < 100ms，媲美豆包体验）
- 端到端评测进化闭环（自建 deterministic 指标 + RAGAS P1）

---



## **2. 技术架构决策汇总**



| # | 决策点 | 选择 | 备注 |
|-|-|-|-|

| 1 | Agent/RAG 框架 | **LlamaIndex** | 检索层用 LlamaIndex，编排层用自定义函数链 |

| 2 | 向量存储 | **PostgreSQL + pgvector** | 一库双用，结构化表 + 向量表 |

| 3 | 路由层 LLM | **Qwen-Turbo**（百炼） | 意图识别、约束抽取、澄清问题生成 |

| 4 | 生成层 LLM | **Qwen-Plus**（百炼） | 购买标准生成、推荐解释、决策卡 |

| 5 | 多模态 LLM | **Qwen-VL-Plus**（百炼） | 图片理解、商品截图识别 |

| 6 | LLM 接入方式 | **百炼 OpenAI 兼容接口** | 统一 SDK，换模型改参数 |

| 7 | Embedding | **text-embedding-v3**，1024 维 | 百炼平台，pgvector 表 vector(1024) |

| 8 | Rerank | **百炼 gte-rerank** | Cross-Encoder 中文重排 |

| 9 | 流式推进策略 | **默认流式推进，不等用户确认** | criteria_card 带 editable + quick_actions，允许轻量修正 |

| 10 | 澄清触发机制 | **硬规则兜底 + LLM 增强** | required slots 硬检查（age 必须），LLM 只负责生成问题和推断 partial criteria |

| 11 | Agent 链路编排 | **自定义 Python 函数链 + asyncio.gather 并行化** | 流水线式重叠执行，首字延迟 < 100ms |

| 12 | ORM 方案 | **SQLModel**（Pydantic + SQLAlchemy） | 减少重复代码 |

| 13 | 部署方案 | **Docker Compose**（FastAPI + PostgreSQL） | 不加 Celery/Redis |

| 14 | 反馈闭环 | **方案 B：反馈影响同一会话下一轮推荐** | 从 history 提取反馈，注入 criteria 约束 |

| 15 | Session 策略 | **轻量持久化 session trace** | 客户端携带 session_id，后端持久化轨迹但不维护登录态 |



---



## **3. 数据层设计**



### **3.1 数据入库分级策略**



**P0**：精选 100-200 条干净 Toys 商品，人工/半自动清洗，保证字段质量。足以支撑 Demo + 评测。



**P1**：扩展到 1000 条，展示规模感。



**P2**：完整 6484 条入库，用于"系统支持规模化处理"的说明，但不作为主 Demo 依赖。



**核心原则**：先跑通小闭环，再扩大规模。6484 条全量入库放到第 2 周后台任务，不阻塞主链路。



### **3.2 商品结构化属性字段（LLM 提取）**



对精选 Toys 商品，用 Qwen-Plus 从 product_text 批量提取：



```JSON
{
  "age_min": 3,
  "age_max": 6,
  "age_label": "3-6 years",
  "gender_preference": "neutral",
  "toy_type": "puzzle",
  "education_dimensions": ["logic", "creativity", "fine_motor"],
  "safety_features": ["no_small_parts", "non_toxic", "choking_hazard_free"],
  "brand": "LEGO",
  "key_features": ["60 pieces", "colorful illustrations"],
  "play_scenario": "indoor",
  "parent_concern": "age_appropriate",
  "requires_battery": false,
  "messiness_level": "low"
}
```



**字段调整说明**：

- `age_range` 改为 `age_min/age_max/age_label`：数值型才能做 `WHERE age_min <= 4 AND age_max >= 4`，字符串 `"3+"` vs `"36 months+"` 无法可靠过滤
- 新增 `requires_battery`（布尔值）：家长非常关心"会不会吵"
- 新增 `messiness_level`（low/medium/high）：家长关心"会不会脏/乱"
- `safety_features` 枚举增加 `"choking_hazard_free"`：吞咽风险是低龄玩具最严重的安全问题，作为枚举值而非独立字段避免膨胀
- `key_features` 保留但不作为核心 filter，仅用于展示和向量检索语义增强
- 总字段 12 个（原 9 个 + age_min/age_max 替代 age_range + requires_battery + messiness_level）

**澄清机制硬规则 slot 定义**：



```JSON
{
  "age": "required",      // 年龄信息缺失时必须触发澄清
  "budget": "optional",   // 缺失时默认宽范围
  "scenario": "required", // 使用场景/目的缺失时必须触发澄清
  "safety_concern": "optional"
}
```



### **3.3 数据库 Schema（8 张表）**



```SQL
-- 核心数据层
products (
  id            TEXT PRIMARY KEY,
  name          TEXT NOT NULL,
  category      TEXT NOT NULL,
  price         DECIMAL,
  brand         TEXT,
  image_urls    TEXT[],
  product_url   TEXT,
  amazon_seller BOOLEAN,
  metadata      JSONB DEFAULT '{}'       -- 12 个结构化属性字段（含 age_min/age_max/age_label）
)

product_chunks (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id    TEXT REFERENCES products(id),
  chunk_text    TEXT NOT NULL,
  chunk_index   INT NOT NULL,
  embedding     vector(1024) NOT NULL,
  metadata      JSONB
)

-- 评测闭环层（轻量持久化 session trace）
conversations (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  session_id    TEXT NOT NULL,
  message_id    TEXT NOT NULL,            -- 每条消息独立ID
  user_message  TEXT NOT NULL,
  criteria_json JSONB,
  ai_response   TEXT,
  product_ids   TEXT[],
  created_at    TIMESTAMPTZ DEFAULT now()
)

feedbacks (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  session_id    TEXT NOT NULL,
  product_id    TEXT,
  action        TEXT NOT NULL,              -- like/dislike/view_detail/click_alternative
  reason        TEXT,
  created_at    TIMESTAMPTZ DEFAULT now()
)

eval_runs (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  run_name      TEXT NOT NULL,
  strategy_tag  TEXT,
  metrics       JSONB NOT NULL,
  sample_count  INT,
  created_at    TIMESTAMPTZ DEFAULT now()
)

-- 评委亮点层
retrieval_traces (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  conversation_id UUID REFERENCES conversations(id),
  criteria_id     TEXT,
  filters_applied JSONB,
  vector_top_k    JSONB,
  rerank_top_n    JSONB,
  selected_ids    TEXT[],
  hit_count       INT,
  vector_count    INT,
  created_at      TIMESTAMPTZ DEFAULT now()
)

evidence_links (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  conversation_id UUID REFERENCES conversations(id),
  product_id      TEXT REFERENCES products(id),
  chunk_id        UUID REFERENCES product_chunks(id),
  evidence_type   TEXT,
  relevance_score FLOAT,
  cited_in        TEXT
)

eval_samples (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  question        TEXT NOT NULL,
  must_have       JSONB NOT NULL,           -- {"age_min_lte":4, "age_max_gte":4, "price_lte":30, "safety_features":["no_small_parts"]}
  preferred       JSONB,                    -- {"education_dimensions":["logic","fine_motor"], "play_scenario":["indoor"]}
  forbidden       JSONB,                    -- {"safety_features_missing":["no_small_parts"]}
  difficulty      TEXT,                     -- "simple"/"multi_constraint"/"ambiguous"/"image"
  scenario_type   TEXT                      -- "budget_guide"/"image_identify"/"comparison"
)
```



**Schema 调整说明**：

- `products.metadata` 字段改为 12 个（含 age_min/age_max）
- `conversations` 增加 `message_id` 字段，用于消息级追踪
- `eval_samples` 改为 `must_have/preferred/forbidden` 结构，比简单 expected_product_ids 更可计算

### **3.4 product_text 切分策略（混合切分）**



```Python
def chunk_product_text(text):
    # 解析: [Category] Name | Feature1 | Feature2 | ...
    parts = text.split("|")
    header = parts[0]
    features = [p.strip() for p in parts[1:] if p.strip()]

    # 主chunk: header + 前3个核心feature（商品身份信息）
    main_chunk = header + " | " + " | ".join(features[:3])
    main_chunk = clean_text(main_chunk, max_chars=400)

    # 补充chunks: 剩余feature按2-3个一组打包
    supplementary_chunks = []
    for i in range(3, len(features), 2):
        chunk_text = f"{header[:50]} | " + " | ".join(features[i:i+2])
        supplementary_chunks.append(clean_text(chunk_text, max_chars=200))

    return [(main_chunk, 0)] + [(c, i+1) for i, c in enumerate(supplementary_chunks)]
```



### **3.5 数据入库流程**



```Plain Text
CSV 数据 → 精选 100-200 条（P0）
         → clean_text 噪音清洗
         → LLM 批量提取 metadata（12 字段）
         → 混合切分 product_text
         → 分批 Embedding（50条/批 + 重试）
         → 写入 products 表 + product_chunks 表
         → 幂等（先删后插）
```



P1 扩展到 1000 条，P2 完整 6484 条。借鉴 Amazon-Multimodal-RAG-Assistant 的 clean_text 正则过滤。



---



## **4. Agent / RAG 管道设计**



### **4.1 管道链路（流水线式并行执行）**



**串行 vs 并行延迟对比**：



| 指标 | 串行管道 | 并行化后 | 改善 |
|-|-|-|-|

| 首字延迟（thinking） | \~1-2s | **<100ms** | 立即响应，媲美豆包 |

| criteria_card | \~6-8s | **\~4s** | intent期间thinking动画填充 |

| 第一个 product_card | \~10-15s | **\~6s** | 投机检索提前准备 |

| 总完成 | \~15-25s | **\~17s** | 流式体验让用户感知不到等待 |



**并行化时间轴**：



```Plain Text
T0ms:   ──→ yield thinking（立即，不等任何计算）
T0ms:   ──→ slot_checker + intent(LLM) 并行启动
         │   slot_checker ~0ms完成
         │   intent ~1.5s完成
T1.5s:  ──→ intent完成，同时启动三路并行：
         │   ① criteria(LLM ~3s)         ← 必须等intent结果
         │   ② query_embedding(~0.5s)    ← 用user_input+intent.extracted，不等criteria
         │   ③ 初步硬过滤(SQL ~1s)        ← 用intent.extracted初步约束，不等criteria
T2s:    ──→ embedding完成（提前准备好）
T2.5s:  ──→ 初步硬过滤完成（提前准备好）
T4.5s:  ──→ criteria完成 → yield criteria_card
         │   最终硬过滤修正（基于criteria精确约束）+ merge候选集 + rerank ~1s
T5.5s:  ──→ rerank完成 → 5个商品evidence并行检索
T6s:    ──→ yield 第一个 product_card
T6-15s: ──→ recommendation流式输出 + 后台并行写入evidence/trace
T15s:   ──→ decision开始
T17s:   ──→ yield final_decision + done
```



**可并行的计算对**：



| 并行化点 | 两个并行计算 | 数据依赖 | 延迟节省 |
|-|-|-|-|
| slot_checker + intent | slot_checker(纯Python \~0ms) + intent(LLM \~1.5s) | slot_checker只需user_input | slot_checker不阻塞 |
| criteria + embedding + 初步硬过滤 | criteria(LLM) + query_embedding + SQL hard_filter | embedding和硬过滤只需intent.extracted | 检索提前2.5s准备好 |
| 硬过滤 + 向量召回 | SQL hard_filter + pgvector vector_retrieve | 两者无数据依赖 | 检索4s→2s |
| 5个商品evidence | evidence_1 + evidence_2 + ... + evidence_5 | 每个商品独立查询 | evidence总延迟从5s→1s |
| 推荐流式 + 后台写入 | recommendation_stream + write_trace/evidence | 写入不影响客户端 | 后台任务不阻塞流式输出 |
| 图片分析 + 文本意图 | Qwen-VL-Plus(\~3-5s) + Qwen-Turbo(\~1.5s) | 图片和文本独立 | 多模态从8s→5s |



**管道伪代码**：



```Python
async def chat_stream(user_input, image_url=None, history=None, session_id=None, criteria_patch=None, skip_stages=None):
    # ====== session_id 管理 ======
    if session_id is None:
        session_id = generate_session_id()  # UUID
    seq_counter = 0

    def next_seq():
        seq_counter += 1
        return seq_counter

    # ====== T0: 立即响应 ======
    yield SSEEvent("thinking", seq=next_seq(), session_id=session_id, stage="understanding", message="正在理解您的需求...")

    # ====== T0: slot_checker + intent 并行 ======
    slot_result, intent_result = await asyncio.gather(
        check_required_slots(user_input),        # ~0ms
        analyze_intent(user_input, history),      # ~1.5s (Qwen-Turbo)
    )

    if slot_result.needs_clarification:
        yield SSEEvent("clarification", seq=next_seq(), session_id=session_id,
                        questions=[{"slot": s, "question": q, "suggested_options": opts} for s, q, opts in slot_result.clarification_data],
                        required_slots=slot_result.missing_slots,
                        partial_criteria=flatten_constraints(intent_result.partial_constraints))
        return

    # ====== T1.5: criteria + embedding + 初步硬过滤 三路并行 ======
    criteria_task = asyncio.create_task(generate_criteria(user_input, intent_result, history))
    embedding_task = asyncio.create_task(compute_query_embedding(intent_result, user_input))
    initial_filter_task = asyncio.create_task(initial_hard_filter(intent_result))

    yield SSEEvent("thinking", seq=next_seq(), session_id=session_id, stage="generating", message="正在生成购买标准...")

    criteria_raw = await criteria_task  # LLM 输出 constraints 列表
    criteria_payload = flatten_constraints(criteria_raw.constraints)  # 展平为前端 CriteriaPayload
    yield SSEEvent("criteria_card", seq=next_seq(), session_id=session_id,
                    criteria_id=criteria_raw.criteria_id, editable=True,
                    criteria=criteria_payload, risks=criteria_raw.risks,
                    quick_actions=criteria_raw.quick_actions)

    # ====== 投机检索结果已提前准备好 ======
    query_embedding = await embedding_task
    hard_filter_ids = await initial_filter_task
    final_filter_ids = await refine_hard_filter(criteria_payload, hard_filter_ids)

    # ====== 并行：向量召回 + 硬过滤候选集 ======
    vector_results, filtered_results = await asyncio.gather(
        vector_retrieve(query_embedding, top_k=50),
        get_filtered_products(final_filter_ids),
    )
    candidates = merge_and_dedup(vector_results, filtered_results)

    # rerank
    ranked = await rerank(candidates, criteria_payload, top_n=5)

    yield SSEEvent("thinking", seq=next_seq(), session_id=session_id, stage="searching", message="找到5个匹配商品...")

    # ====== 并行：5个商品evidence检索 ======
    evidences = await asyncio.gather(*[fetch_evidence(p, criteria_payload) for p in ranked])

    # ====== 并行：推荐流式输出 + 后台写入 ======
    background_tasks = asyncio.gather(
        write_retrieval_trace(criteria_raw, ranked, evidences),
        write_evidence_links(ranked, evidences),
    )

    msg_id = generate_message_id()
    for chunk in generate_recommendation_stream(criteria_payload, ranked, evidences):
        if chunk.type == "text":
            yield SSEEvent("text_delta", seq=next_seq(), session_id=session_id, message_id=msg_id, delta=chunk.content, done=False)
        elif chunk.type == "product":
            yield SSEEvent("product_card", seq=next_seq(), session_id=session_id, rank=chunk.rank, product=chunk.product_payload, reason=chunk.reason, risk_notes=chunk.risk_notes, evidence=chunk.evidence, actions=chunk.actions)
    yield SSEEvent("text_delta", seq=next_seq(), session_id=session_id, message_id=msg_id, delta="", done=True)

    await background_tasks

    yield SSEEvent("thinking", seq=next_seq(), session_id=session_id, stage="decision_making", message="正在整理结论...")
    decision = await generate_decision(criteria_payload, ranked)
    yield SSEEvent("final_decision", seq=next_seq(), session_id=session_id, winner_product_id=decision.winner_id, summary=decision.summary, why=decision.why, not_for=decision.not_for, alternatives=decision.alternatives, next_actions=decision.next_actions)
    yield SSEEvent("done", seq=next_seq(), session_id=session_id, criteria_id=criteria_raw.criteria_id, total_products=len(ranked))
```



特殊链路：

\- **图片输入**：Qwen-VL-Plus 图片分析与 Qwen-Turbo 文本意图识别并行，合并后进入正常管道

\- **反馈影响推荐**：从 history 提取 feedback → 注入 criteria 约束（同一会话下一轮）

\- **需求澄清**：硬规则 slot 检查兜底，LLM 只负责生成自然问题和推断 partial criteria



### **4.2 混合检索策略（硬过滤 + 软偏好分离 + 并行执行）**



```Plain Text
criteria.constraints
  → 硬过滤（不可违反的刚性条件，与向量召回并行执行）:
      WHERE price <= budget AND metadata->>'age_min' <= user_age
           AND metadata->>'age_max' >= user_age
           AND metadata->>'safety_features' 包含 required safety tags
  → candidate_pool
  → 向量召回 top_k=50（与硬过滤并行执行，软偏好由向量语义处理）
  → merge_and_dedup(硬过滤结果 + 向量召回结果)
  → Rerank: gte-rerank top_n=5（综合硬条件+软偏好+evidence）
  → 相似度阈值硬过滤: cosine_distance < 阈值
  → 选中商品
```



**关键设计**：

1. 用户偏好（"更偏益智"、"亲子场景"）不变成 SQL WHERE，由向量召回和 Rerank 处理。硬过滤只管刚性条件。

2. 硬过滤和向量召回**并行执行**（asyncio.gather），检索延迟从4s降到2s。

1. 投机检索：在criteria生成过程中，提前用intent.extracted的初步约束做硬过滤和query_embedding，criteria完成时检索已准备好。

### **4.3 PipelineState TypedDict**



```Python
class PipelineState(TypedDict):
    session_id: str                        # 必填，后端生成或前端携带
    seq_counter: int                       # SSE 事件序号递增器
    user_message: str
    image_url: Optional[str]
    history: List[Dict]
    intent: Optional[Dict]
    criteria_raw: Optional[Dict]           # LLM 输出的约束列表 [{dimension, value, weight}]
    criteria_payload: Optional[Dict]       # 展平后的前端 CriteriaPayload 格式
    needs_clarification: bool
    clarification_data: Optional[List[Dict]]  # [{slot, question, suggested_options}]
    partial_criteria: Optional[Dict]       # 已推断的部分标准（展平格式）
    filters: Optional[Dict]           # 硬过滤条件
    soft_preferences: Optional[Dict]  # 软偏好
    query_embedding: Optional[List[float]]  # 投机预计算的query embedding
    retrieval_results: Optional[List[Dict]]
    reranked_results: Optional[List[Dict]]
    recommendation_text: Optional[str]
    decision: Optional[Dict]
```



### **4.4 豆包级流式体验细节**



**细节1：thinking 心跳填充**

在 LLM 调用等待期间，每隔 800ms yield 一个 thinking 事件更新状态文字，用户始终感知系统在工作，不会出现空白等待：

```Plain Text
T0ms:    thinking → stage="understanding" → "正在理解您的需求..."
T800ms:  thinking → stage="understanding" → "正在分析您的偏好..."
T1.5s:   thinking → stage="generating" → "正在生成购买标准..."
T3s:     thinking → stage="searching" → "正在检索匹配商品..."
```

状态文字按管道进度自然推进，不是假动画——每个文字对应后台真实计算阶段。



**细节2：quick_actions 修正快捷路径**

用户点击 criteria_card / product_card / final_decision 的 quick_actions 按钮，客户端发新 `/chat/stream` 请求带上 patch：

```JSON
{
  "message": "",
  "session_id": "s_001",
  "criteria_patch": {"budget_max": 30},
  "skip_stages": ["intent", "criteria"]
}
```

quick_actions 统一为 4 维结构 `{action_id, label, action, feedback_type?, criteria_patch?}`：
- `action: "criteria_patch"` → 修正购买标准，携带 criteria_patch
- `action: "feedback"` → 反馈，携带 feedback_type (like/dislike/not_interested/show_alternatives)
- `action: "open_evidence"` → 查看证据（前端底板展示）
- `action: "compare"` → 加入对比

后端收到 patch 后用现有 criteria + patch 更新约束，直接跳到检索阶段（不重新跑 intent + criteria），延迟降到 \~2s。

---



## **5. API 契约**

### **5.1 SSE 事件协议**

> **统一协议参见 `contracts/sse-event-protocol-v1.md`**——本节为后端视角的摘要，详细字段定义以合约文件为准。

全局规则：每个 SSE 事件必带 `seq: Int`（正整数递增）和 `session_id: String`（必填）。首次请求 `session_id=null` 时后端生成 UUID 并在第一个事件中携带。

P0 客户端接收 8 种事件（去掉 retrieval，trace 写后台）：

```Plain Text
thinking:        {"seq":1, "event":"thinking", "session_id":"...", "stage":"understanding|clarifying|searching|generating|decision_making", "message":"..."}
clarification:   {"seq":2, "event":"clarification", "session_id":"...", "questions":[{"slot":"age", "question":"孩子多大呢？", "suggested_options":["3岁","4岁","5-6岁","7岁以上"]}], "required_slots":["age","scenario"], "partial_criteria":{"budget_max":200}}
criteria_card:   {"seq":3, "event":"criteria_card", "session_id":"...", "criteria_id":"crit_001", "editable":true,
                  "criteria":{"age":4,"scenario":"indoor","budget_max":200,"requires_battery":false,"safety_features":["no_small_parts"],"education_dimensions":["logic","fine_motor"]},
                  "risks":["4岁儿童使用磁力玩具需家长陪同收纳"],
                  "quick_actions":[{"action_id":"budget_low","label":"预算压低","action":"criteria_patch","criteria_patch":{"budget_max":150}}]}
text_delta:      {"seq":4, "event":"text_delta", "session_id":"...", "message_id":"msg_ai_01", "delta":"...", "done":false}
product_card:    {"seq":5, "event":"product_card", "session_id":"...", "rank":1,
                  "product":{"product_id":"...", "name":"...", "brand":"...", "price":169, "currency":"CNY", "image_url":"...", "age_min":4, "age_max":6, "toy_type":"building", "education_dimensions":[...], "safety_features":[...], "play_scenario":[...], "requires_battery":false, "messiness_level":"low"},
                  "reason":"...", "risk_notes":[...], "evidence":[...],
                  "actions":[{"action_id":"show_evidence","label":"看证据","action":"open_evidence"}]}
final_decision:  {"seq":7, "event":"final_decision", "session_id":"...", "winner_product_id":"...", "summary":"...",
                  "why":["..."], "not_for":["..."], "alternatives":[...],
                  "next_actions":[{"action_id":"cheaper","label":"再便宜一点","action":"criteria_patch","criteria_patch":{"budget_max":150}}]}
done:            {"seq":8, "event":"done", "session_id":"...", "criteria_id":"crit_001", "total_products":2}
error:           {"seq":2, "event":"error", "session_id":"...", "code":"...", "message":"...", "retryable":true}
```

**关键调整说明**：

- `thinking` 字段名统一为 `stage`（不是 `phase`），枚举扩展为 5 值：`understanding | clarifying | searching | generating | decision_making`
- `clarification` 采用多问题模式，每个问题独立带 `slot` + `suggested_options`，顶层带 `partial_criteria` + `required_slots`
- `criteria_card.criteria` 输出扁平 `CriteriaPayload` 格式（不是 `constraints` 列表）——后端内部 LLM 生成约束列表，管道内做展平转换后输出；新增 `criteria_id`（评测关联）和 `risks`（标准级风险提示）；删除 `user_profile`（冗余）和顶层 `scenario`（已在 criteria 内）
- `text_delta` 必带 `message_id` + `done`——前端据此拼同一条 AI 消息气泡和判断文本流结束
- `product_card` 采用嵌套结构（`product: ProductPayload` 在内，`rank/reason/risk_notes/evidence/actions` 在外）；新增 `brand` 字段；字段名统一为 `reason`（不是 `recommend_reason`）、`risk_notes`（不是 `risks`）；删除 `match_details`
- `final_decision` 命名统一为前端定义：`winner_product_id`（不是 `recommended_id`）、`not_for`（不是 `not_suitable_for`）；新增 `summary` 和 `next_actions`；删除 `recommended_name`（前端从 product_card 取）和 `suitable_for`（`why` 已覆盖）
- `done` 事件字段：`criteria_id` + `total_products`（不是 `session_id`——已在全局必填字段中）
- `error` 新增 `retryable: Boolean`，错误码枚举：INTENT_FAILED / CLARIFICATION_FAILED / RETRIEVAL_EMPTY / LLM_TIMEOUT / RATE_LIMITED / INTERNAL_ERROR
- `quick_actions` 统一为 4 维结构 `{action_id, label, action, feedback_type?, criteria_patch?}`，扩展到 criteria_card / product_card / final_decision 三类卡片
- `retrieval` 事件不再发给客户端，技术细节放后台 retrieval_traces 展示

### **5.2 HTTP API 端点**

```Plain Text
POST /chat/stream          — SSE 流式导购对话
  请求: {message, session_id?, image_url?, history, criteria_patch?, skip_stages?}
  响应: SSE 事件流（8 种事件，每个事件必带 seq + session_id）
  注: session_id=null 时后端生成 UUID；criteria_patch + skip_stages 用于quick_actions快捷修正

POST /feedback             — 用户反馈记录
  请求: {session_id, product_id?, action, feedback_type?, reason?, criteria_patch?}
  响应: {status: "ok"}
  action 枚举: criteria_patch | feedback | open_evidence | compare
  feedback_type 枚举: like | dislike | not_interested | show_alternatives

POST /upload/image         — 图片上传与多模态解析
  请求: multipart/form-data
  响应: {image_url, width, height, mime_type, ocr_text}

GET /sessions/{id}/history — 恢复对话历史（P0 新增）
  响应: {session_id, messages: [{role, content, timestamp}]}

POST /chat/cancel          — 停止当前管道（P1 新增）
  请求: {session_id}
  响应: {status: "cancelled"}
```

客户端携带 session_id，后端持久化轨迹但不维护登录态。



---

## **6. 评测闭环设计**

### **6.1 P0：自建 deterministic 指标（可直接计算，不依赖 LLM）**

| 指标 | 计算方式 | 说明 |
|-|-|-|
| age_match_rate | `age_min <= user_age AND age_max >= user_age` | 推荐商品年龄段是否匹配 |
| budget_match_rate | `price <= budget` | 推荐价格是否在预算内 |
| constraint_satisfaction_rate | 每条约束的满足比例 | 购买标准约束满足率 |
| evidence_cited_rate | `len(evidence_links) > 0` | 推荐是否引用至少 1 条证据 |

| unsafe_recommendation_rate | 推荐给低龄但含 choking hazard / small parts | **严重错误**，必须为 0 |

| decision_diversity_score | 首选和备选的 toy_type 分布 | 不能全是同质商品 |

| clarification_trigger_rate | 合理触发 vs 过度触发 | 不该触发时触发 = 过度澄清 |

### **6.2 P1：RAGAS 标准指标（核心链路跑通后加入）**

- Faithfulness → 推荐是否有证据支撑
- Answer Relevance → 回答是否与用户问题相关
- Context Precision → 检索上下文精确度（加 Rerank 前后对比）
- Context Recall → 检索是否覆盖回答所需信息

### **6.3 评测样本设计（P0 先做 15 条高质量）**

| 场景类型 | 数量 | 示例问题 |
|-|-|-|
| 年龄安全 | 5 | "给4岁孩子买益智玩具，不要小零件" |
| 预算场景 | 4 | "预算30美元以内，适合6岁男孩的户外玩具" |
| 教育目标 | 3 | "锻炼专注力的室内玩具，给5岁女孩" |
| 礼物场景 | 2 | "送侄子的生日礼物，他3岁，喜欢恐龙" |
| 多轮反馈 | 1 | "不喜欢太吵的，换个安静的" |

每条样本的结构：

```JSON
{
  "question": "给4岁孩子买一个室内益智玩具，预算30美元以内，不要小零件",
  "must_have": {
    "age_min_lte": 4,
    "age_max_gte": 4,
    "price_lte": 30,
    "safety_features": ["no_small_parts"]
  },
  "preferred": {
    "education_dimensions": ["logic", "fine_motor"],
    "play_scenario": ["indoor"]
  },
  "forbidden": {
    "safety_features_missing": ["no_small_parts"]
  }
}
```



### **6.4 答辩展示**



- 后台可视化：baseline → 加硬过滤 → 加 Rerank → Prompt v2，每步指标变化
- retrieval_traces 展示完整检索决策链路
- evidence_links 展示推荐证据绑定
- "我们不是玩具 Demo，是工业级原型：可评估、可解释、可迭代"

---



## **7. 反馈闭环设计**



用户反馈（like/dislike/view_detail/click_alternative）记录到 feedbacks 表。



同一会话的下一轮对话，从 history 中提取反馈，注入 criteria 约束：



```Python
extracted_feedback = extract_feedback_from_history(history)
# → {"avoid_types": ["in_ear"], "avoid_products": ["p_001"], "prefer_traits": ["open_ear"]}

criteria.constraints.append(
  {"dimension": "佩戴方式", "value": "非入耳式", "weight": "high", "source": "user_feedback"}
)
```



criteria_card 的 quick_actions 也允许用户轻量修正标准，无需打长文本。



---



## **8. 项目代码结构**



```Plain Text
buypilot-backend/
├── app/
│   ├── main.py                  # FastAPI 入口
│   ├── config.py                # Pydantic-Settings 配置管理
│   │
│   ├── api/                     # HTTP 层（提前拆分，避免膨胀）
│   │   ├── chat.py              # /chat/stream SSE 端点（含 criteria_patch 快捷路径）+ /chat/cancel
│   │   ├── sessions.py          # /sessions/{id}/history 会话历史恢复（P0 新增）
│   │   ├── feedback.py          # /feedback（action 枚举扩展 + feedback_type + criteria_patch）
│   │   ├── upload.py            # /upload/image（响应展平为 {image_url, width, height, mime_type, ocr_text}）
│   │   ├── admin_products.py    # 管理后台：商品数据
│   │   ├── admin_documents.py   # 管理后台：文档/知识库
│   │   ├── admin_feedback.py    # 管理后台：用户反馈
│   │   ├── admin_eval.py        # 管理后台：评测数据
│   │   ├── admin_traces.py      # 管理后台：检索trace
│   │
│   ├── agent/                   # 核心智能层
│   │   ├── pipeline.py          # chat_stream 主函数（asyncio.gather并行编排）
│   │   ├── slot_checker.py      # 硬规则 slot 检查（required slots）
│   │   ├── intent.py            # 意图识别 + 约束抽取
│   │   ├── criteria.py          # 购买标准生成（含反馈注入 + patch合并）
│   │   ├── recommendation.py    # 推荐生成 + 流式输出
│   │   ├── decision.py          # 最终决策卡生成
│   │   ├── multimodal.py        # 图片解析
│   │   ├── llm_client.py        # 百炼平台 LLM 客户端（支持流式 async generator）
│   │
│   ├── rag/                     # 检索层
│   │   ├── retriever.py         # 混合检索（硬过滤+向量召回并行 + pgvector + rerank）
│   │   ├── embedding.py         # 百炼 embedding 客户端（含投机预计算接口）
│   │   ├── reranker.py          # gte-rerank 客户端
│   │   ├── evidence.py          # 并行evidence检索（5商品asyncio.gather）
│   │   ├── chunking.py          # product_text 切分
│   │
│   ├── db/                      # 数据层（提前拆分 crud）
│   │   ├── models.py            # SQLModel 模型（8 张表）
│   │   ├── session.py           # 数据库连接
│   │   ├── crud_products.py     # 商品读写
│   │   ├── crud_documents.py    # 文档/chunk 读写
│   │   ├── crud_conversations.py # 会话读写
│   │   ├── crud_eval.py         # 评测读写
│   │
│   ├── models/                  # 契约层
│   │   ├── schemas.py           # 请求/响应 Pydantic 模型
│   │   ├── sse_events.py        # SSE 事件类型定义
│   │   ├── pipeline_state.py    # PipelineState TypedDict
│   │   ├── slot_defs.py         # required slots 定义
│   │
│   ├── eval/                    # 评测闭环
│   │   ├── runner.py            # 评测运行器
│   │   ├── metrics.py           # deterministic 指标计算（P0）
│   │   ├── ragas_metrics.py     # RAGAS 指标（P1）
│   │
├── prompts/                     # Prompt 独立管理（git 版本控制）
│   ├── intent_analysis.md
│   ├── criteria_generation.md
│   ├── recommendation.md
│   ├── decision.md
│   ├── clarification.md
│   ├── metadata_extraction.md
│
├── scripts/                     # 一次性脚本
│   ├── ingest.py                # 数据入库（分级：P0 100条 / P1 1000条 / P2 全量）
│   ├── eval.py                  # 运行评测
│
├── tests/                       # TDD
│   ├── test_slot_checker.py     # P0: 硬规则 slot 检查
│   ├── test_intent.py           # P0: intent + clarification 场景
│   ├── test_criteria.py         # P0: criteria_card JSON 合法性
│   ├── test_retrieval.py        # P0: 混合检索逻辑（mock DB）
│   ├── test_pipeline.py         # P0: SSE 事件 yield 顺序和格式
│   ├── test_sse_events.py       # P0: 所有事件 JSON schema 验证
│   ├── test_api.py              # P1: SSE 端点格式
│   ├── test_feedback.py         # P1: 反馈写入 DB
│   ├── test_chunking.py         # P1: 切分数量和格式
│   ├── test_ingest.py           # P1: LLM 提取 metadata JSON
│
├── docker-compose.yaml
├── Dockerfile
├── pyproject.toml
├── .env.example
```



**结构调整说明**：

- 新增 `slot_checker.py` 和 `slot_defs.py`：硬规则 slot 检查独立模块
- `pipeline.py` 从串行改为 `asyncio.gather` + `create_task` 流水线式并行编排
- `retriever.py` 拆为 `hard_filter()` + `vector_retrieve()` 两个可独立调用的异步函数（并行执行）
- `embedding.py` 增加 `compute_query_embedding()` 投机预计算接口
- 新增 `evidence.py`：5个商品evidence并行fetch
- `llm_client.py` 支持流式 async generator 返回
- `criteria_card` 增加 `editable` + `quick_actions` 字段，`/chat/stream` 支持 `criteria_patch` + `skip_stages` 快捷路径
- admin API 拆为 5 个文件，crud 拆为 4 个文件
- eval 模块分 `metrics.py`（P0 deterministic）和 `ragas_metrics.py`（P1）
- ingest.py 支持分级入库参数（`--count 100` / `--count 1000` / `--count all`）

---



## **9. TDD 策略**



**P0 必须有测试（核心链路）**：

- slot_checker.py → 硬规则检查：各种输入场景的 required slot 缺失判断
- sse_events.py → JSON schema 验证
- intent.py → 5 个场景（模糊输入/清晰输入/需要澄清/图片输入/闲聊）
- criteria.py → 3 个场景（Toys 导购/跨品类/多约束 + 反馈注入）
- retriever.py → mock DB，验证硬过滤 + 向量召回 + rerank 组合逻辑
- pipeline.py → mock 各阶段函数，验证 SSE 事件 yield 顺序和格式

**P1 有测试但可简略（关键辅助）**：

- chat.py API → SSE 端点格式
- feedback.py API → 反馈写入 DB
- chunking.py → 切分数量和格式
- ingest.py → LLM 提取 JSON 合法性

**P2 不写测试**：

- admin API、RAGAS eval、config.py、prompt 文件

**开发顺序（TDD 驱动）**：

1. slot_defs.py + slot_checker.py → required slots 定义 + 硬规则检查测试
2. sse_events.py Pydantic 模型 → schema 验证测试
3. intent.py → 测试 → prompt → 函数
4. criteria.py → 测试 → prompt → 函数
5. retriever.py → 测试 → 混合检索逻辑
6. pipeline.py → 测试 → SSE 事件编排
7. chat.py → API 集成测试

**LLM 阶段测试原则**：断言 JSON 结构合法性 + 关键字段存在，不断言具体文本内容。



---



## **10. 部署方案**

```YAML
services:
  api:
    build: .
    ports: ["8000:8000"]
    depends_on: [db]
    env_file: .env

  db:
    image: pgvector/pgvector:pg16
    ports: ["5432:5432"]
    environment:
      POSTGRES_DB: buypilot
      POSTGRES_USER: buypilot
      POSTGRES_PASSWORD: buypilot
    volumes: ["pgdata:/var/lib/postgresql/data"]
```

数据入库脚本一次性命令运行。



---

## **11. 借鉴清单**

| 借鉴点 | 来源 | 用途 |
|-|-|-|
| QwenEmbeddings 适配器（OpenAI 兼容） | E-commerce-Smart-Agent | embedding.py 客户端 |
| pgvector HNSW 索引（m=16, ef_construction=64） | E-commerce-Smart-Agent | 数据库索引优化 |
| SSE event_generator 模式 | E-commerce-Smart-Agent | chat.py 流式输出 |
| asyncio.gather 流水线并行 | 豋包体验分析 | pipeline.py 并行编排（首字延迟<100ms） |
| ETL 分批 + 重试 + 幂等清理 | E-commerce-Smart-Agent | ingest.py 数据入库 |
| SQLModel ORM | E-commerce-Smart-Agent | 减少重复代码 |
| Pydantic-Settings 配置管理 | E-commerce-Smart-Agent | config.py |
| clean_text 噪音过滤 | Amazon-Multimodal-RAG | product_text 清洗 |
| PipelineState TypedDict | E-commerce-Smart-Agent | 状态管理 |
| 硬逻辑与软逻辑分离 | E-commerce-Smart-Agent + 反馈意见 | 结构化过滤是 SQL 硬逻辑，软偏好由向量处理 |
| 相似度阈值硬过滤 | E-commerce-Smart-Agent | retriever.py 距离过滤 |
| 图像描述拼接查询 | Amazon-Multimodal-RAG | multimodal.py |
| base64 图像编码 | multimodal-rag-ecommerce | upload.py |
| Recall@K 评测 | Amazon-Multimodal-RAG | eval/metrics.py |



---



## **12. 三周开发计划（后端 & Agent 负责人视角）**

### **第 1 周：精选数据入库 + 核心管道跑通**



| 天 | 任务 | 验证标准 |
|-|-|-|
| 1 | 项目骨架 + Docker Compose + SQLModel 8 张表 | `docker compose up` 成功，表创建 |

| 2 | **精选 100 条 Toys 商品入库**（半自动清洗 + LLM 提取 metadata） | 100 条商品+chunks 写入 DB，可检索 |

| 3 | sse_events.py + slot_defs.py + slot_checker.py | JSON schema + slot 检查测试通过 |

| 4 | intent.py（Qwen-Turbo）+ clarification 机制 + 5 个测试 | 测试通过 |

| 5 | criteria.py（Qwen-Plus）+ 反馈注入 + 3 个测试 | 测试通过 |

| 6 | retriever.py 混合检索（硬过滤 + pgvector + rerank）+ mock 测试 | 测试通过 |

| 7 | pipeline.py + chat.py SSE 端点 → **第一次内部 Demo** | 主链路跑通：输入 → SSE 流 → 结构化卡片 |



### **第 2 周：导购逻辑变强 + 多模态 + 反馈 + 扩展数据**



| 天 | 任务 | 验证标准 |
|-|-|-|
| 8 | recommendation.py 流式推荐生成 | 流式 text_delta + product_card 正确输出 |
| 9 | decision.py 最终决策卡 | final_decision 事件正确输出 |
| 10 | multimodal.py 图片上传 + Qwen-VL-Plus 解析 | 图片→描述→检索链路跑通 |
| 11 | feedback.py 反馈记录 + 反馈注入 criteria 逻辑 | 反馈影响下一轮推荐 |
| 12 | retrieval_traces + evidence_links 写入 | 每次推荐有完整检索追踪和证据 |
| 13 | stream_token.py + 扩展数据到 1000 条 | Android 能连 Stream Chat，数据规模感提升 |

| 14 | **第二次内部 Demo** | 多模态 + 反馈闭环 + 结构化卡片完整 |



### **第 3 周：评测 + 打磨 + 答辩**



| 天 | 任务 | 验证标准 |
|-|-|-|
| 15 | eval 模块：deterministic 指标 + 15 条评测样本 | P0 评测可运行 |
| 16 | admin API（5 个文件拆分） | 管理后台能展示评测闭环 |
| 17 | baseline 评测运行，记录指标 | eval_runs 表有数据 |
| 18 | 优化策略运行（加硬过滤 → 加 Rerank → Prompt v2） | 版本对比数据 |
| 19 | 3 条 Demo 剧本 + RAGAS P1 尝试 | Demo 稳定，RAGAS 数据可选展示 |
| 20 | 完整彩排 + 全量数据入库（P2） | 端到端流畅 |
| 21 | 冻结功能，只修 bug | 系统稳定 |



---



## **13. 不做的事（明确裁剪）**



- 全量支付与订单系统
- 完整商家入驻与商品管理后台
- Multi-Agent 蜂群架构
- 长期用户画像系统
- 全量真实电商平台接入
- 复杂端侧大模型推理
- 通过 Stream Channel 转发流式 token
- `/stream/token` 端点（Stream SDK 残留，已删除；采用 OkHttp SSE 直连）
- Celery + Redis 异步任务系统
- JWT 复杂认证系统
- RAGAS 作为 P0 评测依赖（P1 加入）
- retrieval 事件发给客户端（trace 放后台）
- 第一天全量 6484 条数据入库（分级入库）

---



## **14. SSE 事件 → Android UI 组件映射（供队友对齐）**

> 统一协议详见 `contracts/sse-event-protocol-v1.md`

| SSE 事件 | 关键字段 | Android UI 组件 | 说明 |
|-|-|-|-|
| thinking | `stage` (understanding/clarifying/searching/generating/decision_making) | 顶部状态条（按 stage→中文映射） | 简单 loading indicator，150–250ms 去抖 |
| clarification | `questions[{slot,question,suggested_options}]`, `required_slots`, `partial_criteria` | 多问题澄清卡（每题一组选项按钮） | partial_criteria 小字展示已推断标准 |
| criteria_card | `criteria_id`, `editable`, `criteria`(CriteriaPayload), `risks`, `quick_actions` | 购买标准卡 + quick_actions 按钮 | criteria 为扁平格式；risks 显示标准级风险 |
| text_delta | `message_id`, `delta`, `done` | AI 旁白文本（同 message_id 拼同一条气泡） | done=true 时做最终 Markdown 渲染 |
| product_card | `rank`, `product`(ProductPayload), `reason`, `risk_notes`, `evidence`, `actions` | 商品推荐卡（嵌套 product 对象） | product 包含 brand；actions 含看证据/反馈/换相似 |
| final_decision | `winner_product_id`, `summary`, `why`, `not_for`, `alternatives`, `next_actions` | 最终决策卡 | next_actions 含 criteria_patch/compare |
| done | `criteria_id`, `total_products` | 结束态 | Debug 页用 criteria_id 关联 trace |
| error | `code`, `message`, `retryable` | Toast + fallback 提示 | retryable 决定是否显示重试按钮 |

---



## **15. 答辩核心话术**



**开场**：我们选择亲子玩具作为深水区验证品类，因为家长购买玩具的决策痛点最强——年龄安全、教育价值、场景适配。这不是关键词搜索能解决的问题。

**核心展示**：

1. 购买标准生成："给4岁孩子推荐益智玩具" → 系统生成结构化标准（age 3-6、益智、无小零件、室内）
2. 防幻觉机制：每个推荐绑定文档证据，不是 LLM 编的
3. 反馈闭环：左滑不喜欢 → 下一轮自动调整推荐
4. 评测进化：后台展示 baseline → 加硬过滤 → 加 Rerank → Prompt v2 的指标变化
5. 流式体验：首字延迟 < 100ms（thinking立即响应），criteria_card \~4s，第一个product_card \~6s，流水线并行执行媲美豆包

**裁剪话术**：订单与支付是成熟标准基础设施，我们预留接口但主动裁剪，把全部精力投入决策辅助和评测闭环。