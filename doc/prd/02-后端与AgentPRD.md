<title>BuyPilot-AI 后端 &amp; Agent PRD</title>

# 泛电商智能导购 Agent — 后端与 Agent 模块需求规格

---

## **1. 项目定位**



**一句话定位**：利用混合 RAG 架构，构建一个能将用户模糊购物渴望精准转化为可量化购买标准，通过多模态解析实现强可解释性推荐，并自带端到端质量评测进化闭环的决策型导购 Agent。



**品类策略**：使用导师提供的官方脱敏电商数据（4品类×25：美妆护肤/数码电子/服饰运动/食品生活），品类与课题说明会场景完美匹配。

**答辩叙事顺序**：

1. 为什么用导师数据？——品类与课题场景完美匹配（油皮洗面奶/蓝牙耳机/推荐跑鞋）
2. 每条商品有 marketing_description + FAQ + reviews，天然适合 RAG chunking
3. 反选排除在美妆品类更真实——"不要含酒精的"需要语义理解，不是 SQL 字段过滤
4. 我们用 4 品类验证了"一套可迁移的导购决策架构"

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

| 1 | Agent/RAG 框架 | **自定义函数链 + pgvector 直连** | 编排层用 asyncio.gather 并行化，检索层直接用 pgvector SQL + OpenAI 兼容 SDK |

| 2 | 向量存储 | **PostgreSQL + pgvector** | 一库双用，结构化表 + 向量表 |

| 3 | 路由层 LLM | **Doubao-Seed-2.0-lite**（primary）/ **Qwen-Turbo**（fallback） | 双轨策略，意图识别用Doubao免费额度，失败时fallback到Qwen-Turbo |

| 4 | 生成层 LLM | **Qwen-Plus**（primary）/ **Doubao**（fallback） | 购买标准生成/推荐解释/决策卡需要强JSON约束输出，Qwen-Plus更稳定 |

| 5 | 多模态 LLM | **Qwen-VL-Plus**（primary，无fallback） | Doubao VL生态不确定，Qwen-VL-Plus成熟方案 |

| 6 | LLM 接入方式 | **双轨：火山引擎Ark + 百炼 OpenAI兼容** | 配置层 dict + call_llm() 函数，按任务类型选 primary/fallback |

| 7 | Embedding | **text-embedding-v3**（1024维，primary）/ **Doubao-embedding-vision**（fallback） | 百炼有Key+维度确定 |

| 8 | Rerank | **gte-rerank**（百炼，无fallback） | Doubao无对应服务 |

| 9 | 流式推进策略 | **默认流式推进，不等用户确认** | criteria_card 带 editable + quick_actions，允许轻量修正 |

| 10 | 澄清触发机制 | **硬规则兜底 + LLM 增强** | required slots 硬检查（category 必须），LLM 只负责生成问题和推断 partial criteria |

| 11 | Agent 链路编排 | **自定义 Python 函数链 + asyncio.gather 并行化** | 流水线式重叠执行，首字延迟 < 100ms |

| 12 | ORM 方案 | **SQLModel**（Pydantic + SQLAlchemy） | 减少重复代码 |

| 13 | 部署方案 | **Docker Compose**（FastAPI + PostgreSQL） | 不加 Celery/Redis |

| 14 | 反馈闭环 | **方案 B：反馈影响同一会话下一轮推荐** | 后端通过 session_id 查询 feedbacks 表获取历史反馈，注入 criteria 约束 |

| 15 | Session 策略 | **轻量持久化 session trace** | 客户端携带 session_id，后端持久化轨迹但不维护登录态 |

| 16 | 模型切换机制 | **配置层 dict + call_llm(task_type, prompt)** | settings.py 定义 providers 和 task_routing，call_llm() 读配置选 provider，primary失败自动retry fallback |

| 17 | 对话式加购 | **⭐入门：cart_items表 + add_to_cart意图 + cart_action事件** | P1实现，不做⭐⭐⭐下单确认 |

| 18 | 数据策略 | **官方100条，直接入库** | 导师提供4品类×25脱敏数据（含 rag_knowledge），不自构造补充数据 |

| 19 | Doubao API 配置 | BaseAPI/Model/Key/Limit 见 `.env.example` | TPM 80万/RPM 700，不限制具体使用模型 |



---



## **3. 数据层设计**



### **3.1 数据入库分级策略**



**P0**：导师提供的**100条**脱敏电商数据（4品类×25，中文，含 marketing_description + FAQ + reviews + SKUs + 本地图片）直接入库。足以支撑多品类 Demo + 评测 + 条件筛选 + 反选排除。数据入库流程：官方数据JSON → products 表 + product_chunks 表拆分，rag_knowledge 天然 chunking，metadata 从 SKU 和品类推断。

**P1**：扩展非结构化文档（成分说明、使用指南、FAQ），增强 RAG 证据源。

**P2**：不再追求规模数量，数据质量比数量重要。



### **3.2 商品结构化属性字段（metadata JSONB）**



官方数据每条商品已有 JSON 结构，`rag_knowledge` 直接拆为 chunks，metadata 从 SKU 和品类推断。

**metadata JSONB 品类属性示例**：

```JSON
// 美妆护肤
{
  "skin_type": ["油性", "混合性"],
  "ingredient_tags": ["无酒精", "含透明质酸", "含二裂酵母"],
  "use_scenario": "日常护肤",
  "spf": null,
  "suitable_age": "25+"
}

// 数码电子
{
  "storage": ["256GB", "512GB"],
  "screen_size": "6.3英寸",
  "use_scenario": "日常/商务",
  "chip": "A19 Pro"
}

// 服饰运动
{
  "sport_type": "跑步",
  "season": "春夏",
  "material": "速干面料",
  "use_scenario": "日常训练"
}

// 飶品生活
{
  "dietary": ["无糖", "0脂0卡"],
  "taste": "原味",
  "use_scenario": "日常饮用"
}
```

**设计说明**：

- metadata 用 JSONB 承载品类属性，不建品类特有 nullable 列——4个NULL列比一个JSONB列更糟
- 品类属性不做硬编码枚举，LLM 根据用户意图和商品数据动态提取
- `ingredient_tags` 是反选排除的核心载体——"不要含酒精的" → `ingredient_avoid: ["含酒精"]`
- `skin_type` 是硬过滤载体——"推荐适合油皮的" → `WHERE metadata @> '{"skin_type": ["油性"]}'`

**澄清机制 slot 定义**：

```JSON
{
  "category": "required",    // 品类信息缺失时必须触发澄清
  "budget": "optional",      // 缺失时默认宽范围
  "scenario": "required",    // 使用场景/目的缺失时必须触发澄清
  "category_constraint": "optional"  // 品类相关约束（肤质/用途/口味等）
}
```



**意图类型定义**：

```Plain Text
recommend:   用户表达购物需求（模糊或清晰），进入导购管道
clarify:     用户回答澄清问题，补充缺失约束后继续管道
feedback:    用户对已有推荐表达偏好修正（like/dislike/criteria_patch），影响下一轮
add_to_cart: 用户要求将推荐商品加入购物车，触发 cart_action 事件
view_cart:   用户查看当前购物车内容，触发 cart_action(view) 事件
```



### **3.3 数据库 Schema（9 张表）**



```SQL
-- 核心数据层
products (
  id            TEXT PRIMARY KEY,
  name          TEXT NOT NULL,
  category      TEXT NOT NULL,
  sub_category  TEXT,
  price         DECIMAL,
  brand         TEXT,
  image_urls    TEXT[],
  product_url   TEXT,
  amazon_seller BOOLEAN,
  metadata      JSONB DEFAULT '{}'       -- 品类结构化属性（如肤质适用、存储规格、运动类型等）
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

cart_items (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  session_id    TEXT NOT NULL,              -- 关联 conversations.session_id
  product_id    TEXT NOT NULL REFERENCES products(id),
  quantity      INT DEFAULT 1,
  added_at      TIMESTAMPTZ DEFAULT now()
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
  must_have       JSONB NOT NULL,           -- {"category":"美妆护肤", "skin_type":["油性"], "price_lte":200}
  preferred       JSONB,                    -- {"use_scenario":["日常护肤"], "sub_category":["洁面"]}
  forbidden       JSONB,                    -- {"ingredient_avoid":["酒精", "香精"]}
  difficulty      TEXT,                     -- "simple"/"multi_constraint"/"ambiguous"/"image"
  scenario_type   TEXT                      -- "budget_guide"/"image_identify"/"comparison"
)
```



**Schema 调整说明**：

- `products.metadata` 字段改为 JSONB 承载品类属性，不建品类特有 nullable 列
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
官方数据 JSON → 精选 100 条（P0，官方数据直接入库）
         → rag_knowledge 天然 chunking
         → metadata 从 SKU 和品类推断
         → 混合切分 product_text
         → 分批 Embedding（50条/批 + 重试）
         → 写入 products 表 + product_chunks 表
         → 幂等（先删后插）
```



P1 扩展非结构化文档，P2 不追求规模数量。



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
async def chat_stream(user_input, image_url=None, history=None):
    turn_id = new_turn_id()
    deck_id = f"deck_{turn_id}"
    seq = EventSeq(turn_id)

    # ====== T0: 立即响应 ======
    yield A2UIEvent(
        "thinking",
        turn_id=turn_id,
        seq=seq.next(),
        node_id=f"thinking_{turn_id}",
        display_mode="inline_thinking",
        payload={"phase": "analyzing", "message": "正在理解您的需求..."},
    )

    # ====== T0: slot_checker + intent 并行 ======
    slot_result, intent_result = await asyncio.gather(
        check_required_slots(user_input),        # ~0ms
        analyze_intent(user_input, history),      # ~1.5s (Qwen-Turbo)
    )

    if slot_result.needs_clarification:
        yield A2UIEvent(
            "clarification",
            turn_id=turn_id,
            seq=seq.next(),
            node_id=f"clarification_{turn_id}",
            display_mode="inline_card",
            payload={...},
        )
        return

    # ====== T1.5: criteria + embedding + 初步硬过滤 三路并行 ======
    criteria_task = asyncio.create_task(generate_criteria(user_input, intent_result, history))
    embedding_task = asyncio.create_task(compute_query_embedding(intent_result, user_input))
    initial_filter_task = asyncio.create_task(initial_hard_filter(intent_result))

    yield A2UIEvent(
        "thinking",
        turn_id=turn_id,
        seq=seq.next(),
        node_id=f"thinking_{turn_id}",
        display_mode="inline_thinking",
        payload={"phase": "generating", "message": "正在生成购买标准..."},
    )

    criteria = await criteria_task
    yield A2UIEvent(
        "criteria_card",
        turn_id=turn_id,
        seq=seq.next(),
        node_id=f"criteria_{criteria.criteria_id}",
        display_mode="summary_card",
        payload={
            "summary": criteria.to_summary(),
            "detail": criteria.to_detail(),
            "quick_actions": criteria.quick_actions,
        },
    )

    # ====== 投机检索结果已提前准备好 ======
    query_embedding = await embedding_task
    hard_filter_ids = await initial_filter_task
    final_filter_ids = await refine_hard_filter(criteria, hard_filter_ids)

    # ====== 并行：向量召回 + 硬过滤候选集 ======
    vector_results, filtered_results = await asyncio.gather(
        vector_retrieve(query_embedding, top_k=50),
        get_filtered_products(final_filter_ids),
    )
    candidates = merge_and_dedup(vector_results, filtered_results)

    # rerank
    ranked = await rerank(candidates, criteria, top_n=5)

    yield A2UIEvent(
        "thinking",
        turn_id=turn_id,
        seq=seq.next(),
        node_id=f"thinking_{turn_id}",
        display_mode="inline_thinking",
        payload={"phase": "searching", "message": "找到5个匹配商品..."},
    )

    # ====== 并行：5个商品evidence检索 ======
    evidences = await asyncio.gather(*[fetch_evidence(p, criteria) for p in ranked])

    # ====== 并行：推荐流式输出 + 后台写入 ======
    background_tasks = asyncio.gather(
        write_retrieval_trace(criteria, ranked, evidences),
        write_evidence_links(ranked, evidences),
    )

    for chunk in generate_recommendation_stream(criteria, ranked, evidences):
        if chunk.type == "text":
            yield A2UIEvent(
                "text_delta",
                turn_id=turn_id,
                seq=seq.next(),
                node_id=f"ai_text_{turn_id}",
                display_mode="inline_text",
                payload={"message_id": f"msg_{turn_id}", "delta": chunk.content},
            )
        elif chunk.type == "product":
            yield A2UIEvent(
                "product_card",
                turn_id=turn_id,
                seq=seq.next(),
                node_id=f"product_{chunk.product.product_id}",
                deck_id=deck_id,
                display_mode="swipe_deck_item",
                payload=build_product_card_payload(chunk.product, evidences),
            )

    await background_tasks

    decision = await generate_decision(criteria, ranked)
    yield A2UIEvent(
        "final_decision",
        turn_id=turn_id,
        seq=seq.next(),
        node_id=f"decision_{turn_id}",
        display_mode="summary_card",
        payload=build_decision_payload(decision, evidences),
    )
    yield A2UIEvent(
        "done",
        turn_id=turn_id,
        seq=seq.next(),
        node_id=f"done_{turn_id}",
        display_mode="none",
        payload={"criteria_id": criteria.criteria_id, "deck_id": deck_id, "total_products": len(ranked)},
    )
```



特殊链路：

\- **图片输入**：Qwen-VL-Plus 图片分析与 Qwen-Turbo 文本意图识别并行，合并后进入正常管道

\- **反馈影响推荐**：后端通过 session_id 查询 feedbacks 表获取历史反馈 → 注入 criteria 约束（同一会话下一轮）

\- **需求澄清**：硬规则 slot 检查兜底，LLM 只负责生成自然问题和推断 partial criteria

\- **澄清回复流程**：用户回答澄清问题后，前端以同一 `session_id` 发送新的 `/chat/stream` 请求。后端通过 `session_id` 查询上一轮的 `partial_criteria` 自动恢复上下文，与用户新回答合并后继续管道。前端无需回传 `partial_criteria`。



### **4.2 混合检索策略（硬过滤 + 软偏好分离 + 并行执行）**



```Plain Text
criteria.constraints
  → 硬过滤（不可违反的刚性条件，与向量召回并行执行）:
      WHERE price <= budget AND category = '美妆护肤'
           AND metadata @> '{"skin_type": ["油性"]}'
           AND NOT metadata->>'ingredient_tags' LIKE '%酒精%'
  → candidate_pool
  → 向量召回 top_k=50（与硬过滤并行执行，软偏好由向量语义处理）
  → merge_and_dedup(硬过滤结果 + 向量召回结果)
  → Rerank: gte-rerank top_n=5（综合硬条件+软偏好+evidence）
  → 相似度阈值硬过滤: cosine_distance < 阈值
  → 选中商品
```



**关键设计**：

1. 用户偏好（"油性适用"、"日常护肤"）不变成 SQL WHERE，由向量召回和 Rerank 处理。硬过滤只管刚性条件。

2. 硬过滤和向量召回**并行执行**（asyncio.gather），检索延迟从4s降到2s。

1. 投机检索：在criteria生成过程中，提前用intent.extracted的初步约束做硬过滤和query_embedding，criteria完成时检索已准备好。

### **4.3 PipelineState TypedDict**



```Python
class PipelineState(TypedDict):
    user_message: str
    image_url: Optional[str]
    history: List[Dict]
    intent: Optional[Dict]
    criteria: Optional[Dict]
    needs_clarification: bool
    clarification_questions: Optional[List[str]]
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

在 LLM 调用等待期间，每隔 800ms yield 一个 `thinking` 事件更新当前轮次的 inline thinking 气泡，用户始终感知系统在工作，不会出现空白等待。`thinking` 不是顶部状态条日志，而是当前 AI 回复节点的一部分；同一轮 `thinking` 必须复用稳定 `node_id`，由客户端 upsert 更新：

```Plain Text
T0ms:    thinking → "正在理解您的需求..."
T800ms:  thinking → "正在分析您的偏好..."
T1.5s:   thinking → "正在生成购买标准..."
T3s:     thinking → "正在检索匹配商品..."
```

状态文字按管道进度自然推进，不是假动画——每个文字对应后台真实计算阶段。TopBar 只展示连接状态、标题等全局信息，不消费 `thinking.message`。



**细节2：quick_actions 修正快捷路径**

用户点击 criteria_card 的 quick_actions 按钮（如"预算调低"），客户端发新 `/chat/stream` 请求带上 patch：

```JSON
{
  "message": "修正购买标准",
  "session_id": "s_001",
  "criteria_patch": {"budget_max": 150},
  "skip_stages": ["intent", "criteria"]  // 跳过已完成的阶段
}
```

后端收到 patch 后用现有 criteria + patch 更新约束，直接跳到检索阶段（不重新跑 intent + criteria），延迟降到 \~2s。

---



## **5. API 契约**

### **5.1 SSE / A2UI 事件协议**

P0 客户端接收 8 种业务事件（去掉 retrieval，trace 写后台），但所有事件必须使用统一 Agent-to-UI envelope。后端不能只返回业务 JSON 让前端猜渲染方式；必须提供排序、幂等、稳定 key、分组和展示语义。

**统一 envelope：**

```JSON
{
  "schema_version": "2026-05-20",
  "event": "thinking",
  "session_id": "sess_demo_001",
  "turn_id": "turn_001",
  "seq": 1,
  "event_id": "turn_001:0001",
  "node_id": "thinking_turn_001",
  "deck_id": null,
  "display_mode": "inline_thinking",
  "created_at_ms": 1780000000000,
  "payload": {}
}
```

**字段规则：**

- `schema_version`：协议版本，破坏性调整必须升级。
- `turn_id`：一次用户输入对应一次 Agent 回复轮次；同一轮事件必须一致。后端自行生成 `turn_id`，与前端发送的 `client_turn_id` 无关。`done` 事件 payload 中回传 `client_turn_id`，供前端关联请求与响应。
- `seq`：同一 `turn_id` 内单调递增，客户端按它排序。
- `event_id`：幂等去重 key，建议 `${turn_id}:${seq}`。
- `node_id`：聊天流节点稳定 key，用于 upsert，而不是 append-only。
- `deck_id`：商品卡堆稳定 key；同一轮多个 `product_card` 必须共用同一 `deck_id`。
- `display_mode`：后端声明展示语义，允许值包括 `inline_thinking`、`inline_card`、`inline_text`、`summary_card`、`swipe_deck_item`、`none`。
- `payload`：业务载荷，必须分为对话区摘要、底板详情和证据引用三层。

**事件 payload 规范：**

```Plain Text
thinking:
  display_mode: inline_thinking
  node_id: thinking_{turn_id}
  payload: {phase: "analyzing|clarifying|criteria|searching|reranking|generating|deciding", message: "..."}

clarification:
  display_mode: inline_card
  node_id: clarification_{turn_id}
  payload: {question, required_slots, suggested_options, partial_criteria?}

criteria_card:
  display_mode: summary_card
  node_id: criteria_{criteria_id}
  payload: {
    criteria_id,
    summary: {title: "已理解你的需求", chips: ["油性肌肤", "200元内", "日常护肤", "洁面类"]},
    detail: {category, skin_type?, budget_min?, budget_max?, ingredient_tags, ingredient_avoid, use_scenario, weights?},
    quick_actions: [...]
  }

text_delta:
  display_mode: inline_text
  node_id: ai_text_{turn_id}
  payload: {message_id, delta, done?}

product_card:
  display_mode: swipe_deck_item
  node_id: product_{product_id}
  deck_id: deck_{turn_id}
  payload: {
    product_id,
    rank,
    summary: {name, price, image_url, chips, reason_short, risk_short?},
    detail: {skin_type_match?, ingredient_tags?, ingredient_avoid?, use_scenario, sub_category?, match_details?, risk_notes},
    evidence_refs: [{evidence_id, source_type, trust_label?, snippet?}],
    actions: [...]
  }

final_decision:
  display_mode: summary_card
  node_id: decision_{turn_id}
  payload: {
    summary: {winner_product_id, verdict, why_chips, not_for_short?},
    detail: {why, not_for, alternatives, comparison?},
    evidence_refs: [{evidence_id, source_type, trust_label?, snippet?}]
  }

cart_action:
  display_mode: inline_card
  node_id: cart_{turn_id}
  payload: {action: "add"|"remove"|"view", product_id, cart_id, quantity, status: "success"|"failed"}

done:
  display_mode: none
  payload: {criteria_id?, deck_id?, total_products?, client_turn_id?, finish_reason: "completed|canceled|error"}

error:
  display_mode: inline_card
  node_id: error_{turn_id}
  payload: {code, message, retryable, recover_action?}
```

**工程约束：**

- `retrieval` 事件不发给客户端；技术 trace 写入 `retrieval_traces` 后台表。
- 证据完整 snippet 不直接作为聊天流节点输出，只通过 `evidence_refs` 绑定到商品详情或最终依据 Bottom Sheet。
- `criteria_card` 的 `quick_actions` 可以存在，但对话区只显示摘要入口；完整编辑项由客户端在 Bottom Sheet 渲染。
- `product_card` 是 SwipeDeck 的 item，不是聊天流里的独立长卡；同一轮推荐必须使用同一 `deck_id`。
- 服务端必须保证 `event_id` 幂等、`seq` 单调、`node_id` 稳定，客户端才能做无闪烁 upsert。

### **5.2 HTTP API 端点**

```Plain Text
POST /chat/stream          — SSE 流式导购对话
  请求: {message, session_id, image_url?, history, criteria_patch?, skip_stages?, client_turn_id?}
  响应: SSE / A2UI envelope 事件流（8 种业务事件）
  注: criteria_patch + skip_stages 用于quick_actions快捷修正，跳过已完成阶段，延迟降至~2s

POST /chat/cancel          — 取消当前轮 Agent 生成
  请求: {session_id, turn_id}
  响应: {session_id, turn_id, canceled: true}
  注: 取消机制采用双保险——前端关闭 SSE 连接为主要取消信号，同时 best-effort 调用此端点。
      后端同时支持两种检测方式：1) SSE 连接断开时自动中断后续任务；2) 收到显式 cancel 请求时中断。
      两种方式都应中断后续 LLM/RAG 任务，避免继续推送 product_card/final_decision。

POST /feedback             — 用户反馈记录
  请求: {session_id, product_id?, action, reason?}
  响应: {status: "ok"}

POST /upload/image         — 图片上传与多模态解析
  请求: multipart/form-data
  响应: {image_url, analysis: {...}}

GET /cart/{session_id}     — 查询购物车内容
  响应: {items: [{product_id, name, price, quantity, added_at}], total_items, total_price}
  注: Demo 4 需要"查看购物车底板"，前端通过此端点获取购物车内容用于渲染
```

客户端携带 session_id，后端持久化轨迹但不维护登录态。

**session_id 创建规则**：首次请求 `session_id` 为 null 时，后端生成新的 `session_id`（格式 `sess_{uuid}`）并在所有响应事件的 envelope `session_id` 字段中返回。前端必须从首个 SSE 事件中提取 `session_id` 并缓存，后续请求携带同一 `session_id` 以维持会话连续性。



---

## **6. 评测闭环设计**

### **6.1 P0：自建 deterministic 指标（可直接计算，不依赖 LLM）**

| 指标 | 计算方式 | 说明 |
|-|-|-|
| category_constraint_match_rate | `metadata @> category-specific constraints` | 推荐商品品类约束是否匹配（如肤质、适用场景） |
| budget_match_rate | `price <= budget` | 推荐价格是否在预算内 |
| constraint_satisfaction_rate | 每条约束的满足比例 | 购买标准约束满足率 |
| evidence_cited_rate | `len(evidence_links) > 0` | 推荐是否引用至少 1 条证据 |

| unsafe_recommendation_rate | 推荐含用户明确排除的成分/特性 | **严重错误**，必须为 0 |

| decision_diversity_score | 首选和备选的 sub_category 分布 | 不能全是同质商品 |

| clarification_trigger_rate | 合理触发 vs 过度触发 | 不该触发时触发 = 过度澄清 |

### **6.2 P1：RAGAS 标准指标（核心链路跑通后加入）**

- Faithfulness → 推荐是否有证据支撑
- Answer Relevance → 回答是否与用户问题相关
- Context Precision → 检索上下文精确度（加 Rerank 前后对比）
- Context Recall → 检索是否覆盖回答所需信息

### **6.3 评测样本设计（P0 先做 15 条高质量）**

| 场景类型 | 数量 | 示例问题 |
|-|-|-|
| 品类约束 | 5 | "推荐适合油皮的洗面奶，不要含酒精" |
| 预算场景 | 4 | "预算200元以内的防晒霜，油性适用" |
| 使用场景 | 3 | "日常护肤的洁面产品，适合混合性皮肤" |
| 礼物场景 | 2 | "送妈妈的护肤品，她皮肤偏干" |
| 多轮反馈 | 1 | "不要含香精的，换个温和的" |

每条样本的结构：

```JSON
{
  "question": "推荐适合油皮的洗面奶，200元以内",
  "must_have": {
    "category": "美妆护肤",
    "skin_type": ["油性"],
    "price_lte": 200
  },
  "preferred": {
    "use_scenario": ["日常护肤"],
    "sub_category": ["洁面"]
  },
  "forbidden": {
    "ingredient_avoid": ["酒精", "香精"]
  }
}
```



### **6.4 答辩展示**



- 后台可视化：baseline → 加硬过滤 → 加 Rerank → Prompt v2，每步指标变化
- retrieval_traces 展示完整检索决策链路
- evidence_links 展示推荐证据绑定
- "我们不是 Demo，是工业级原型：可评估、可解释、可迭代"

---



## **7. 反馈闭环设计**



用户反馈（like/dislike/view_detail/click_alternative）记录到 feedbacks 表。



同一会话的下一轮对话，后端通过 `session_id` 查询 feedbacks 表获取本会话历史反馈，注入 criteria 约束：

**注意**：反馈数据从 feedbacks 表按 session_id 查询获取，不从 history 字段解析（history 只是 `[{role, content}]` 文本对，不含行为数据）。



```Python
extracted_feedback = extract_feedback_from_session(session_id)  # 查询 feedbacks 表
# → {"avoid_products": ["p_beauty_010"], "avoid_traits": ["含酒精"], "prefer_traits": ["油性适用", "控油"]}

criteria.constraints.append(
  {"dimension": "成分偏好", "value": "不要含酒精", "weight": "high", "source": "user_feedback"}
)
```



criteria_card 的 quick_actions 也允许用户轻量修正标准，无需打长文本。



---



## **8. 项目代码结构**



```Plain Text
backend/
├── src/
│   ├── main.py                  # FastAPI 入口
│   │
│   ├── config/                  # 配置管理
│   │   ├── __init__.py
│   │   ├── settings.py          # Pydantic-Settings 配置
│   │   ├── llm_profiles.yaml    # LLM Profile 配置（intent/generation/vision）
│   │
│   ├── api/                     # HTTP 层（路由）
│   │   ├── __init__.py
│   │   ├── chat.py              # /chat/stream SSE 端点（含 criteria_patch 快捷路径）
│   │   ├── cancel.py            # /chat/cancel
│   │   ├── feedback.py          # /feedback
│   │   ├── upload.py            # /upload/image
│   │   ├── admin_products.py    # 管理后台：商品数据
│   │   ├── admin_documents.py   # 管理后台：文档/知识库
│   │   ├── admin_feedback.py    # 管理后台：用户反馈
│   │   ├── admin_eval.py        # 管理后台：评测数据
│   │   ├── admin_traces.py      # 管理后台：检索trace
│   │
│   ├── runtime/                 # 管道编排层（SSE 事件流生成）
│   │   ├── __init__.py
│   │   ├── pipeline.py          # chat_stream 主函数（asyncio.gather并行编排）
│   │   ├── stages/              # 管道各阶段
│   │   │   ├── __init__.py
│   │   │   ├── slot_checker.py  # 硬规则 slot 检查（required slots）
│   │   │   ├── intent.py        # 意图识别 + 约束抽取
│   │   │   ├── criteria.py      # 购买标准生成（含反馈注入 + patch合并）
│   │   │   ├── recommendation.py # 推荐生成 + 流式输出
│   │   │   ├── decision.py      # 最终决策卡生成
│   │   │   ├── multimodal.py    # 图片解析
│   │
│   ├── services/                # 业务逻辑层（检索、LLM、评测）
│   │   ├── __init__.py
│   │   ├── llm_client.py        # 百炼平台 LLM 客户端（Profile驱动，支持流式 async generator）
│   │   ├── retriever.py         # 混合检索（硬过滤+向量召回并行 + pgvector + rerank）
│   │   ├── embedding.py         # 百炼 embedding 客户端（含投机预计算接口）
│   │   ├── reranker.py          # gte-rerank 客户端
│   │   ├── evidence.py          # 并行evidence检索（5商品asyncio.gather）
│   │   ├── chunking.py          # product_text 切分
│   │   ├── eval/                # 评测闭环
│   │   │   ├── runner.py        # 评测运行器
│   │   │   ├── metrics.py       # deterministic 指标计算（P0）
│   │   │   ├── ragas_metrics.py # RAGAS 指标（P1）
│   │
│   ├── repos/                   # 数据访问层（CRUD）
│   │   ├── __init__.py
│   │   ├── models.py            # SQLModel 模型（9 张表）
│   │   ├── database.py          # 数据库连接
│   │   ├── products.py          # 商品读写
│   │   ├── documents.py         # 文档/chunk 读写
│   │   ├── conversations.py     # 会话读写
│   │   ├── feedbacks.py         # 反馈读写（含按 session_id 查询历史反馈）
│   │   ├── eval_runs.py         # 评测读写
│   │
│   ├── types/                   # 契约层（Pydantic 模型 + 类型定义）
│   │   ├── __init__.py
│   │   ├── schemas.py           # 请求/响应 Pydantic 模型
│   │   ├── sse_events.py        # SSE 事件类型定义（A2UI envelope）
│   │   ├── pipeline_state.py    # PipelineState TypedDict
│   │   ├── slot_defs.py         # required slots 定义
│   │
├── prompts/                     # Prompt 独立管理（git 版本控制）
│   ├── intent_analysis.md
│   ├── criteria_generation.md
│   ├── recommendation.md
│   ├── decision.md
│   ├── clarification.md
│
├── scripts/                     # 一次性脚本
│   ├── ingest.py                # 数据入库（P0 官方100条 / P1 非结构化文档 / P2 不追求规模）
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
│   ├── test_ingest.py           # P1: metadata JSON 合法性
│
├── pyproject.toml
├── Dockerfile
├── .env.example
```



**结构说明**：

- `runtime/` 负责管道编排和 SSE 事件 yield，是 async generator 的 owner
- `services/` 负责具体业务逻辑（LLM 调用、检索、rerank、evidence），被 runtime 调用
- `repos/` 负责数据库 CRUD，被 services 和 runtime 调用
- `types/` 定义所有 Pydantic 模型和类型，被所有层引用
- `config/` 集中管理配置，禁止 `os.getenv()` 散落在业务代码中
- 依赖方向：`api → runtime → services → repos → types/config`（只能自上而下）
- `stream_token.py` 已删除（Stream SDK 已砍，见决策记录 #1）
- `repos/feedbacks.py` 提供 `get_session_feedbacks(session_id)` 接口，供 criteria 阶段注入反馈约束

---



## **9. TDD 策略**



**P0 必须有测试（核心链路）**：

- slot_checker.py → 硬规则检查：各种输入场景的 required slot 缺失判断
- sse_events.py → JSON schema 验证
- intent.py → 5 个场景（模糊输入/清晰输入/需要澄清/图片输入/闲聊）
- criteria.py → 3 个场景（美妆导购/跨品类/多约束 + 反馈注入）
- retriever.py → mock DB，验证硬过滤 + 向量召回 + rerank 组合逻辑
- pipeline.py → mock 各阶段函数，验证 SSE 事件 yield 顺序和格式

**P1 有测试但可简略（关键辅助）**：

- chat.py API → SSE 端点格式
- feedback.py API → 反馈写入 DB
- chunking.py → 切分数量和格式
- ingest.py → LLM 提取 JSON 合法性
- cart_action → 3 个场景：add_to_cart 意图识别 → cart_action 事件 schema 合法性 → add 成功后 GET /cart 返回正确内容

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
# deploy/docker-compose.yml
services:
  postgres:
    image: pgvector/pgvector:pg16
    ports: ["5432:5432"]
    environment:
      POSTGRES_DB: buypilot
      POSTGRES_USER: buypilot
      POSTGRES_PASSWORD: buypilot
    volumes: ["pgdata:/var/lib/postgresql/data"]

  api:
    build:
      context: ../backend
      dockerfile: Dockerfile
    command: uvicorn src.api.app:app --host 0.0.0.0 --port 8000 --reload
    ports: ["8000:8000"]
    environment:
      DATABASE_URL: postgresql+psycopg://buypilot:buypilot@postgres:5432/buypilot
    volumes: ["../backend/src:/app/src"]
    depends_on: [postgres]

volumes:
  pgdata:
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
| PipelineState TypedDict | E-commerce-Smart-Agent | 状态管理 |
| 硬逻辑与软逻辑分离 | E-commerce-Smart-Agent + 反馈意见 | 结构化过滤是 SQL 硬逻辑，软偏好由向量处理 |
| 相似度阈值硬过滤 | E-commerce-Smart-Agent | retriever.py 距离过滤 |
| base64 图像编码 | multimodal-rag-ecommerce | upload.py |



---



## **12. 三周开发计划（后端 & Agent 负责人视角）**

### **第 1 周：官方数据入库 + 核心管道跑通**



| 天 | 任务 | 验证标准 |
|-|-|-|
| 1 | 项目骨架 + Docker Compose + SQLModel 9 张表 | `docker compose up` 成功，表创建 |

| 2 | **官方 100 条数据入库**（JSON→PG 拆分 chunks + metadata 推断） | 100 条商品+chunks 写入 DB，可检索 |

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
| 13 | 非结构化文档入库（成分说明、使用指南、FAQ）+ 管道性能优化 | RAG 证据源增强，流式延迟达标 |

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
- Celery + Redis 异步任务系统
- JWT 复杂认证系统
- RAGAS 作为 P0 评测依赖（P1 加入）
- retrieval 事件发给客户端（trace 放后台）
- 第一天全量大规模数据入库（分级入库）

---



## **14. SSE 事件 → Android UI 组件映射（供队友对齐）**

| SSE 事件 | Android UI 组件 | 说明 |
|-|-|-|
| thinking | 对话流 inline thinking 气泡 / 骨架节点 | 不进 TopBar；同一 `node_id` upsert 阶段文案 |
| clarification | 澄清完整小卡 | 显示缺失维度和追问问题，可直接在对话区完成 |
| criteria_card | 购买标准摘要卡 + 编辑 Bottom Sheet | 对话区只放摘要；完整字段、quick_actions、可编辑项进底板 |
| text_delta | 当前轮 AI 文本节点 | 流式 Markdown 渲染；同一 `node_id` 追加 delta |
| product_card | SwipeDeck item | 同一 `deck_id` 聚合为一个商品卡堆；详情和证据进 Bottom Sheet |
| final_decision | 最终决策摘要卡 | 对话区展示中等完整结论；”查看依据”打开 Bottom Sheet |
| cart_action | 加购/购物车操作节点 | 对话区显示加购成功/失败反馈；”查看购物车”打开 Bottom Sheet |
| done | 当前轮结束态 | 停止 inline thinking 与底部输入区 loading |
| error | 错误气泡 + 可恢复动作 | 可同时触发 Toast，但聊天流必须保留错误节点 |

---



## **15. 答辩核心话术**



**开场**：我们使用导师提供的官方脱敏电商数据驱动多品类导购，因为用户购买美妆护肤的决策痛点真实——肤质匹配、成分安全、场景适配。这不是关键词搜索能解决的问题，需要语义理解和结构化决策。

**核心展示**：

1. 购买标准生成："推荐适合油皮的洗面奶，200元以内" → 系统生成结构化标准（油性适用、日常护肤、不含酒精、200元预算）
2. 防幻觉机制：每个推荐绑定文档证据，不是 LLM 编的
3. 反馈闭环：左滑不喜欢 → 下一轮自动调整推荐
4. 评测进化：后台展示 baseline → 加硬过滤 → 加 Rerank → Prompt v2 的指标变化
5. 流式体验：首字延迟 < 100ms（thinking立即响应），criteria_card \~4s，第一个product_card \~6s，流水线并行执行媲美豆包

**裁剪话术**：订单与支付是成熟标准基础设施，我们预留接口但主动裁剪，把全部精力投入决策辅助和评测闭环。
