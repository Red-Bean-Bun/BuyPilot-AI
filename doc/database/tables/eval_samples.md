# eval_samples

## 服务对象

`eval_samples` 保存固定评测样本。评测 runner 会读取这些样本，执行 `chat_stream`，再根据 `ground_truth` 计算意图、约束、召回、证据覆盖和 LLM judge 指标。

## 为什么这样设计

- 样本入库后可以通过管理接口查看和复用，避免每次评测只依赖本地 JSON 文件。
- `ground_truth` 用 JSON 保存不同场景的期望意图、约束、相关商品和期望 criteria chips，适配多品类、多模态、多轮场景；当前要求带 `schema_version=2026-05-31`。
- 旧字段 `must_have/preferred/forbidden` 已从当前 schema 中清理；新代码写入和读取统一使用 `ground_truth/context/tags`。

## 字段说明

| 字段 | 类型 | 含义和作用 | 设计原因 |
| --- | --- | --- | --- |
| `id` | `VARCHAR`, PK | 样本 ID，例如 `eval_001`。 | 稳定引用和重复 seed 时 upsert。 |
| `question` | `VARCHAR`, not null | 用户问题文本。 | 评测输入。 |
| `difficulty` | `VARCHAR`, nullable | 难度，例如 `easy`、`medium`、`hard`。 | 分层查看评测表现。 |
| `scenario_type` | `VARCHAR`, nullable | 场景类型，例如 `budget_scenario`、`negative_exclusion`。 | 分场景分析系统短板。 |
| `image_path` | `VARCHAR`, nullable | 多模态样本图片路径。 | 支撑拍照找货或图片理解评测。 |
| `context` | `JSON`, nullable | 多轮上下文或额外输入。 | 评测多轮一致性和上下文理解。 |
| `ground_truth` | `JSON`, nullable | 期望答案结构，包括 `schema_version`、`intent_type`、`constraints`、`relevant_product_ids`、`expected_criteria_chips` 等。 | 一张表支持多种评测指标，避免 schema 高频变更。 |
| `tags` | `JSON`, nullable | 标签列表，例如 `beauty`、`budget`。 | 便于筛选和分组分析。 |
| `created_at` | `DATETIME`, nullable | 样本创建时间。 | 新 schema 字段；旧库迁移后允许为空。 |

## 关系和索引

- 当前只有主键索引。
- 样本源文件是 `data/eval/eval_samples.json`，通过 `repos/eval_samples.py` seed 入库。

## Review 关注点

- 新库需要调用 `/admin/eval/samples/seed` 或相关脚本后才会有样本。
- `ensure_eval_schema()` 会为旧开发库补齐当前字段，并删除 `must_have/preferred/forbidden` 旧列，避免人工 review 时看到两套评测契约。
- `repos/eval_samples.py` seed 时会把旧约束名迁移到当前契约，例如 `max_price -> budget_max`、`use_case -> use_scenario`、`must_have_features -> must_match_terms`。
