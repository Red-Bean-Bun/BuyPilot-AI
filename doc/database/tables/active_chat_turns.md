# active_chat_turns

## 服务对象

`active_chat_turns` 记录正在执行的 `/chat/stream` turn，用于取消接口判断某个 turn 是否仍在进行。正常情况下流式请求结束后会清理，所以当前 dev.db 行数为 0 是预期状态。

## 为什么这样设计

- 流式取消不能只依赖进程内 dict。写入数据库后，取消请求可以在另一个 HTTP 请求中确认当前 turn 是否存在。
- 不建外键到 `conversations`，因为 active turn 是“进行中状态”，而 `conversations` 只保存推荐持久化后的已完成记录。

## 字段说明

| 字段 | 类型 | 含义和作用 | 设计原因 |
| --- | --- | --- | --- |
| `id` | `VARCHAR`, PK | active turn 记录 ID。 | 独立标识数据库记录。 |
| `session_id` | `VARCHAR`, not null | 会话 ID。 | 与取消请求中的 session 匹配。 |
| `turn_id` | `VARCHAR`, not null | 流式 turn ID。 | 与前端 `client_turn_id` 或后端生成 ID 对齐。 |
| `trace_id` | `VARCHAR`, nullable | 请求链路 trace ID。 | 和请求日志、审计事件串联。 |
| `started_at` | `DATETIME`, not null | turn 开始时间。 | 调试卡住的流式请求，后续可用于清理超时记录。 |

## 关系和索引

- 索引：`ix_active_chat_turns_session_id`、`ix_active_chat_turns_turn_id`、`ix_active_chat_turns_trace_id`。
- 没有外键，避免进行中状态和完成态记录强耦合。

## Review 关注点

- 当前没有后台 TTL 清理任务，因为 `finally` 会清理；如果服务崩溃可能残留，需要后续加启动时清理或超时清理。
- 这张表是控制面表，不是业务历史表，行数长期接近 0 才正常。
