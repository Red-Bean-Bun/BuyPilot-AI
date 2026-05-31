# chat_turn_cancellations

## 服务对象

`chat_turn_cancellations` 保存用户对流式 turn 发起的取消请求，用于 `runtime` 在阶段间检查并尽快停止输出。它和 `active_chat_turns` 一起构成 best-effort cancellation。

## 为什么这样设计

- SSE 流是长连接，请求取消通常来自另一个 HTTP 请求。把取消请求落库，可以跨请求边界传递状态。
- 取消记录是短生命周期控制数据，turn 清理时会同步删除，因此运行库中行数长期接近 0 是预期状态。

## 字段说明

| 字段 | 类型 | 含义和作用 | 设计原因 |
| --- | --- | --- | --- |
| `id` | `VARCHAR`, PK | 取消请求记录 ID。 | 独立标识一次取消请求。 |
| `session_id` | `VARCHAR`, not null | 会话 ID。 | 与 active turn 匹配，防止跨会话误取消。 |
| `turn_id` | `VARCHAR`, not null | 要取消的 turn。 | 前端取消按钮传入的核心目标。 |
| `requested_at` | `DATETIME`, not null | 取消请求时间。 | 调试取消是否及时，后续可做超时清理。 |

## 关系和索引

- 索引：`ix_chat_turn_cancellations_session_id`、`ix_chat_turn_cancellations_turn_id`。
- 没有外键到 `active_chat_turns`，因为 active turn 也会被快速删除，强外键会让清理顺序复杂化。

## Review 关注点

- 取消是 best-effort，不保证中断已经发出的 token 或已经完成的后台写入。
- 当前没有唯一约束，同一 turn 多次取消可能产生多条记录；服务层会先查 existing，正常路径不会重复写。
