# BuyPilot-AI 评测模块 Handoff — 2026-05-22

> 作者：MilanKing | 分支：feat/backend-dev | git：c963577
> 目标读者：队友接手后端开发 / 运行评测 / 数据库迁移

---

## 1. 模块概览

评测模块实现了端到端 RAG 导购质量评测闭环：**样本 → Pipeline 执行 → 指标计算 → 结果持久化 → 看板展示**。

### 1.1 核心设计决策

| 决策 | 选择 | 原因 |
|------|------|------|
| 指标计算库 | **自建**，不依赖 RAGAS | 复用项目已有 Qwen-Plus 做 Judge，零新增 API 成本；中文电商 prompt 比通用 RAGAS prompt 更准确 |
| 依赖 | 零新增强制 pip 包 | streamlit/plotly 仅看板需要，评测 CLI 和 API 不需 |
| Judge LLM | 复用 `llm_client._call_chat_task("generate_recommendation", ...)` | 走 Qwen-Plus 的 profile |
| 新增 API Key | **零** | Judge 用的 BAILIAN_API_KEY 是项目已配置的 |
| 数据库 | 沿用项目 SQLite | 评测表无数据量压力 |

### 1.2 文件清单

```
backend/src/services/eval/
├── __init__.py          # 模块入口
├── metrics.py           # 6 个确定性指标（不需要 LLM，纯计算）
├── llm_judge.py         # 7 个 LLM-as-Judge 指标（调 Qwen-Plus 打分）
└── runner.py            # 评测执行器（遍历样本 → 调 pipeline → 算指标 → 写库）

backend/src/repos/
├── eval_runs.py         # 评测结果 CRUD（第 1 次重写，此前为空壳）
├── eval_samples.py      # 评测样本查询 + JSON 种子导入
└── models.py            # EvalRun/EvalSample 字段扩展（旧表为空，无数据丢失）

backend/src/api/
├── admin_eval.py        # 管理后台 API（4 个端点）
└── app.py               # 注册 admin_eval_router（+1 行 import，+1 行 include_router）

backend/src/scripts/
└── eval.py              # CLI：python -m src.scripts.eval --strategy baseline

backend/src/repos/database.py
                         # 新增 migrate_eval_tables() / migrate_eval_runs_table()

backend/static/
└── eval_dashboard.py    # Streamlit 看板（4 页：总览/对比/样本/错误分析）

backend/pyproject.toml   # 新增 [project.optional-dependencies] eval 组

data/eval/
└── eval_samples.json    # 15 条评测样本（8 场景 × 4 品类 × 3 难度）

doc/status/
└── backend-completion.md
                         # 更新 #23（评测模块）、#24（评测样本）、#25（管理后台 API）为已完成
```

### 1.3 对已有代码的影响

**零破坏。** 修改了 4 个已有文件，全部是最小改动：

| 文件 | 改动 | 风险 |
|------|------|------|
| `repos/models.py` | EvalRun 加 3 个新字段，EvalSample 重构字段 | 旧表为空，无数据丢失 |
| `repos/database.py` | 加 2 个迁移函数 | 新函数，不改变已有函数签名 |
| `api/app.py` | +1 行 import，+1 行 include_router | 只注册新路由 |
| `pyproject.toml` | +eval 可选依赖组 | 不影响已有依赖 |

**验证：** 全部 62 个已有测试通过。

---

## 2. 指标全景

### 2.1 确定性指标（6 个，不需要 LLM）

| 指标 | 函数 | 输入 | 计算方式 |
|------|------|------|---------|
| `intent_accuracy` | `compute_intent_accuracy()` | 实际意图 + 预期意图 | binary match |
| `constraint_extraction_accuracy` | `compute_constraint_extraction_accuracy()` | 提取的约束 dict + 预期约束 dict | 逐字段比对 |
| `criteria_coverage` | `compute_criteria_coverage()` | 生成的 chips + 预期 chips | set intersection |
| `recall_at_5` / `recall_at_10` | `compute_recall_at_k()` | 检索商品 IDs + 相关商品 IDs | `|retrieved ∩ relevant| / |relevant|` |
| `evidence_coverage` | `compute_evidence_coverage()` | 有 evidence 的商品数 / 总商品数 | 比例 |
| `constraint_satisfaction` | `compute_constraint_satisfaction()` | 商品 metadata + 约束 dict | 逐约束检查 |

### 2.2 LLM Judge 指标（7 个，调 Qwen-Plus 打分）

| 指标 | 函数 | 评测 prompt 问什么 |
|------|------|-------------------|
| `faithfulness` | `evaluate_faithfulness()` | 答案中的 claim 是否在检索上下文中有证据支撑 |
| `context_precision` | `evaluate_context_precision()` | 检索到的 chunk 与用户问题相关吗 |
| `context_recall` | `evaluate_context_recall()` | 答案所需信息在检索 chunk 中都能找到吗 |
| `answer_correctness` | `evaluate_answer_correctness()` | 答案是否事实正确，有没有编造商品/价格 |
| `hallucination_rate` | `evaluate_hallucination_rate()` | 1 - faithfulness |
| `multi_turn_consistency` | `evaluate_multi_turn_consistency()` | 多轮回答是否与前几轮建立的约束一致 |
| `ranking_reasonableness` | `evaluate_ranking_reasonableness()` | 排名靠前的商品真的比靠后的更匹配吗 |
| `llm_constraint_satisfaction` | `evaluate_constraint_satisfaction()` | LLM 视角逐约束检查推荐商品是否满足需求 |

### 2.3 综合分公式

```
overall_score = 0.25 × faithfulness
              + 0.20 × recall_at_10
              + 0.20 × constraint_satisfaction
              + 0.15 × context_precision
              + 0.10 × intent_accuracy
              + 0.10 × (1 - hallucination_rate)
```

---

## 3. 评测样本

15 条中文电商评测样本，存储在 `data/eval/eval_samples.json`。

每条样本结构：
```json
{
  "id": "eval_001",
  "question": "推荐适合油皮的洗面奶，200元以内",
  "scenario_type": "category_constraint",
  "difficulty": "easy",
  "tags": ["beauty", "budget", "skin_type"],
  "ground_truth": {
    "intent_type": "recommend",
    "constraints": {
      "category": "美妆护肤",
      "max_price": 200,
      "skin_type": "油性"
    },
    "relevant_product_ids": ["p_beauty_011"],
    "expected_criteria_chips": ["美妆护肤", "油性肌肤", "200元内"],
    "forbidden_in_answer": []
  }
}
```

### 场景分布

| 场景类型 | 数量 | 示例 ID |
|---------|------|---------|
| 品类约束 + 硬过滤 | 6 | eval_001, eval_007, eval_009, eval_011, eval_014, eval_015 |
| 预算场景 | 1 | eval_002 |
| 使用场景 | 3 | eval_003, eval_010, eval_012 |
| 礼物场景 | 2 | eval_004, eval_006 |
| 反选排除 | 1 | eval_005 |
| 需要澄清 | 1 | eval_013 |
| 边界/不存在 | 1 | eval_008 |

---

## 4. 开发思路（给队友了解代码结构）

### 4.1 为什么不用 RAGAS

项目已部署 Qwen-Plus，把它当 Judge LLM 直接用就行。RAGAS 的价值在于提供经过学术验证的 prompt 模板，但：
- RAGAS 需要 LangChain 兼容的 LLM wrapper，项目用的是自己的 `_call_chat_task`
- RAGAS 的 prompt 是英文通用的，中文电商场景不如自己写 prompt 准确
- 引入 RAGAS 加一个 pip 依赖，多了潜在的版本冲突

所以 `llm_judge.py` 自己写了 7 个 Judge prompt，走 `_call_chat_task("generate_recommendation", ...)` 复用 Qwen-Plus profile。

### 4.2 评测执行流程

```
python -m src.scripts.eval --strategy baseline
  │
  ├─ 1. list_all() 从 eval_samples 表加载 15 条样本
  │
  ├─ 2. 逐样本执行：
  │     ├─ run_intent(body)            ← 单独调意图识别，拿到 intent_type + extracted_constraints
  │     ├─ chat_stream(session_id, body)  ← 跑完整 pipeline，异步收集 SSE 事件
  │     │     └─ _collect_event()       ← 从事件中提取 criteria_chips、products、text、evidence
  │     ├─ metrics.py 算 6 个确定性指标
  │     └─ llm_judge.py 算 7 个 LLM Judge 指标
  │
  ├─ 3. _aggregate() 算均值/标准差/最大/最小
  │
  └─ 4. save_run() 写入 eval_runs 表
```

### 4.3 Pipeline 数据捕获方式

评测 runner 不修改 pipeline 代码，而是通过两步捕获数据：
1. **意图**：单独调用 `run_intent(body)` 直接拿到 `IntentResult.intent` 和 `.extracted_constraints`
2. **SSE 事件**：遍历 `chat_stream()` 的异步生成器，按事件类型提取字段：
   - `criteria_card` → chips + constraints
   - `text_delta` → 拼接成完整文本
   - `product_card` → 商品列表 + evidence 计数
   - `final_decision` → winner_product_id

### 4.4 依赖边界

```
┌─ API 启动（uvicorn）─┐     ┌─ CLI 评测 ────────────┐     ┌─ 看板（streamlit）─┐
│ 不需要 streamlit     │     │ 不需要 streamlit      │     │ 需要 streamlit     │
│ 不需要 plotly        │     │ 不需要 plotly         │     │ 需要 plotly        │
│                      │     │                       │     │                    │
│ api/app.py           │     │ scripts/eval.py        │     │ static/            │
│   └─ admin_eval.py   │     │   └─ runner.py         │     │   eval_dashboard.py│
│        └─ repos/     │     │        └─ metrics.py   │     │                    │
│             eval_*   │     │        └─ llm_judge.py │     │                    │
└──────────────────────┘     └───────────────────────┘     └────────────────────┘
```

`llm_judge.py` 不会在 API 启动时被 import———它只有跑评测 CLI 时才加载。所以队友改 `llm_client.py` 不会影响 API 启动，但如果改了 `_call_chat_task` 的函数签名，跑评测时会挂。

---

## 5. 如何运行

### 5.1 前置条件

- `.env` 中 `BAILIAN_API_KEY` 和 `BAILIAN_BASE_URL` 已配置（项目已有，不需要新 key）
- 后端数据库已初始化（`create_db_and_tables()` 已执行过）

### 5.2 首次使用

```bash
cd backend

# 步骤 1：导入 15 条评测样本（一次性）
python -c "
from src.repos.eval_samples import seed_from_json
from pathlib import Path
seed_from_json(str(Path('..')/'data'/'eval'/'eval_samples.json'))
"

# 验证：应输出 15
python -c "from src.repos.eval_samples import list_all; print(len(list_all()))"

# 步骤 2：运行第一次评测（约 2-3 分钟，15 样本 × LLM pipeline + Judge）
python -m src.scripts.eval --strategy baseline
```

### 5.3 日常使用

```bash
# 跑不同策略对比
python -m src.scripts.eval --strategy baseline
python -m src.scripts.eval --strategy "baseline+rerank"
python -m src.scripts.eval --strategy "prompt_v2" --prompt-version v2.0.0

# 输出 JSON（便于脚本处理）
python -m src.scripts.eval --strategy baseline --json > result.json

# 查看历史记录
curl http://localhost:8000/admin/eval/runs
```

### 5.4 看板（可选，需安装 streamlit）

```bash
pip install streamlit plotly
cd backend
streamlit run static/eval_dashboard.py
# 浏览器打开 http://localhost:8501
```

4 个页面：
- **总览**：最新评测综合分 + P0 指标雷达图 + 趋势柱状图
- **版本对比**：选中 2~5 个 run，并排对比全部指标
- **样本详情**：展开查看每个样本的 13 个指标得分
- **错误分析**：筛选 faithfulness < 阈值的低分样本

---

## 6. 如何验证

### 6.1 快速冒烟（不调 LLM）

```bash
cd backend
python -c "
from src.services.eval.metrics import compute_recall_at_k, compute_intent_accuracy
# 应输出 0.666...
print('Recall@5:', compute_recall_at_k(['a','b','c','d','e'], ['b','e','z'], k=5))
# 应输出 1.0
print('Intent OK:', compute_intent_accuracy('recommend', 'recommend'))
# 应输出 0.0
print('Intent FAIL:', compute_intent_accuracy('clarify', 'recommend'))
"
```

### 6.2 完整评测（调 LLM，约 2-3 分钟）

```bash
cd backend
python -m src.scripts.eval --strategy baseline
# 预期输出：Overall Score 约 0.60~0.65
# faithfulness 约 0.85~0.95
# hallucination_rate 约 0.05~0.20
```

### 6.3 API 验证

```bash
# 启动后端
uvicorn src.main:app --reload &

# 列出评测运行
curl http://localhost:8000/admin/eval/runs

# 查看某次运行详情（替换为实际 run_id）
curl http://localhost:8000/admin/eval/runs/<run_id>

# 查看所有样本
curl http://localhost:8000/admin/eval/samples
```

### 6.4 已有测试（无外网依赖）

```bash
cd backend
python -m pytest tests/ -q
# 62 passed
```

---

## 7. 数据库迁移：SQLite → PostgreSQL

### 7.1 不需要改的

| 组件 | 兼容性 |
|------|--------|
| `metrics.py` | ✅ 纯计算，无数据库依赖 |
| `llm_judge.py` | ✅ 只调 LLM API |
| `runner.py` | ✅ 只调 pipeline + 指标函数 |
| `scripts/eval.py` | ✅ 只调 runner |
| `api/admin_eval.py` | ✅ 只调 repos 函数 |
| `static/eval_dashboard.py` | ✅ 只调 API |
| `repos/eval_samples.py` | ✅ 用 SQLModel，自动适配 PostgreSQL |

### 7.2 需要改的（共约 2~4 行）

**唯一改点：`repos/models.py` 中的 JSON 列声明**

当前：
```python
class EvalRun(SQLModel, table=True):
    ...
    metrics: dict[str, Any] = Field(default_factory=dict, sa_column=Column(JSON))
    samples_detail: list[dict[str, Any]] = Field(default_factory=list, sa_column=Column(JSON))

class EvalSample(SQLModel, table=True):
    ...
    context: dict[str, Any] | None = Field(default=None, sa_column=Column(JSON))
    ground_truth: dict[str, Any] = Field(default_factory=dict, sa_column=Column(JSON))
    tags: list[str] = Field(default_factory=list, sa_column=Column(JSON))
```

PostgreSQL 迁移时，建议改为：
```python
from sqlalchemy.dialects.postgresql import JSONB

# EvalRun
metrics: dict[str, Any] = Field(default_factory=dict, sa_column=Column(JSONB))
samples_detail: list[dict[str, Any]] = Field(default_factory=list, sa_column=Column(JSONB))

# EvalSample
context: dict[str, Any] | None = Field(default=None, sa_column=Column(JSONB))
ground_truth: dict[str, Any] = Field(default_factory=dict, sa_column=Column(JSONB))
tags: list[str] = Field(default_factory=list, sa_column=Column(JSONB))
```

**不改也能用：** `Column(JSON)` 在 PostgreSQL 上自动映射到 `json` 类型（非 `jsonb`），功能正常但不支持索引查询。评测表数据量极小（几十条 run × 几百条样本 detail），不需要索引。

**`database.py` 迁移函数：** `DROP TABLE IF EXISTS` 是标准 SQL，SQLite 和 PostgreSQL 都支持。不需要改。

**`eval_runs.py` 和 `eval_samples.py`：** 全部使用 SQLModel 的 `Session` + `select`，兼容两种数据库。不需要改。

### 7.3 迁移检查清单

- [ ] `.env` 中 `DATABASE_URL` 改为 `postgresql+psycopg://...`
- [ ] `pyproject.toml` 中 `psycopg[binary]` 已在 dependencies（已有）
- [ ] 运行 `create_db_and_tables()` 重建全部表
- [ ] 重新导入评测样本：`seed_from_json()`
- [ ] 跑一次评测确认：`python -m src.scripts.eval --strategy baseline`
- [ ] （可选）`Column(JSON)` → `Column(JSONB)` 优化

---

## 8. 当前 Baseline 结果参考

```
策略: baseline
Git: c963577
时间: 2026-05-22

Overall Score:           0.64
Faithfulness:            0.92  ← 超过目标 0.85
Constraint Satisfaction: 0.88  ← 接近目标 0.90
Hallucination Rate:      0.08  ← 低于目标 10%
Intent Accuracy:         0.87
Context Precision:       0.05  ← retrieval 质量待改善
Context Recall:          0.32  ← retrieval 质量待改善
Recall@10:               0.22  ← ground truth 需精标注
Evidence Coverage:       0.33  ← 证据绑定待完善
```

**解读：**
- 生成质量（faithfulness/hallucination）已经很好，说明 Qwen-Plus 生成的推荐文本基本上是基于检索结果的
- 检索质量（context precision/recall）偏低，部分因为 contexts 仅是商品摘要（非完整 rag_knowledge chunks），部分因为 pipeline 的检索策略还有优化空间
- Recall@10 低主要因为 ground truth 的 `relevant_product_ids` 是基于价格 + 品类粗略匹配的，没有经过人工精标注

---

## 9. 常见问题

**Q: 跑评测时报 "No eval samples found"**
A: 先执行 `POST /admin/eval/samples/seed` 或直接运行 Python 种子导入脚本。

**Q: 评测很慢，能加速吗？**
A: 每个样本要调 1 次意图 LLM + 1 次完整 pipeline（含 criteria/recommendation/decision LLM）+ 6 次 Judge LLM ≈ 8~10 次 API 调用。15 个样本 ≈ 120~150 次 API 调用。可以先减少样本数做快速迭代，完整评测留给最终验证。

**Q: Judge LLM 评分不准怎么办？**
A: 查看 `eval_runs.samples_detail[].details` 中的 judge 原始输出。如果明显不合理，调 `llm_judge.py` 中 `_JUDGE_PROMPTS` 对应的 prompt。

**Q: 队友改了 `llm_client.py` 会影响评测吗？**
A: 评测依赖 `_call_chat_task(task, messages, json_object)` 这个私有函数。只要这个签名不变就没问题。如果改了，`llm_judge.py` 里只需改一行调用。

**Q: 如何增加新的评测样本？**
A: 编辑 `data/eval/eval_samples.json`，按已有格式新增条目，重新运行 `seed_from_json()`（已存在的样本会被跳过，只插入新增的）。
