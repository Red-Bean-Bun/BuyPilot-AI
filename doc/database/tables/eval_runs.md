# eval_runs

## 服务对象

`eval_runs` 保存一次完整评测运行的聚合结果和逐样本明细，用于比较不同 prompt、模型、检索策略和代码版本的效果变化。

## 为什么这样设计

- 评测结果需要和 `strategy_tag`、`prompt_version`、`git_commit` 绑定，方便答辩时解释“这版为什么更好”。
- 聚合指标和逐样本明细都放 JSON，避免每加一个指标就改表结构。
- 评测表和线上会话表隔离，不影响 demo 主流程。

## 字段说明

| 字段 | 类型 | 含义和作用 | 设计原因 |
| --- | --- | --- | --- |
| `id` | `VARCHAR`, PK | 评测运行 ID。 | 唯一定位一次运行。 |
| `run_name` | `VARCHAR`, not null | 运行名称。 | 人工识别版本或实验。 |
| `strategy_tag` | `VARCHAR`, nullable | 策略标签，例如 `baseline`、`rerank_v2`。 | 对比不同检索或 prompt 策略。 |
| `metrics` | `JSON`, nullable | 聚合指标，如 recall、faithfulness、overall_score。 | 指标集合会变化，JSON 更灵活。 |
| `sample_count` | `INTEGER`, nullable | 本次评测样本数。 | 快速判断指标覆盖范围。 |
| `created_at` | `DATETIME`, not null | 创建时间。 | 按时间查看最近评测。 |
| `prompt_version` | `VARCHAR`, nullable | prompt 版本标识。 | 追踪 prompt 调整带来的效果变化。 |
| `git_commit` | `VARCHAR`, nullable | 代码 commit。 | 将评测结果和代码版本绑定。 |
| `samples_detail` | `JSON`, nullable | 每个样本的实际结果、指标和错误信息。 | 支撑失败样本下钻，不额外建多张明细表。 |

## 关系和索引

- 当前只有主键索引。
- 没有外键到 `eval_samples`，因为 `samples_detail` 是运行时快照，即使样本后续修改也应保留当时结果。

## Review 关注点

- 当前 dev.db 行数为 0，表示还没在此库里持久化评测运行。
- 如果评测样本和指标规模变大，可把 `samples_detail` 拆成 `eval_run_samples` 明细表；当前 JSON 足够支撑轻量评测闭环。
