# api_request_logs

## 服务对象

`api_request_logs` 是 HTTP 请求日志表，由请求上下文中间件写入，用于查看接口路径、状态码、耗时、trace/session/turn 关联和错误类型。

## 为什么这样设计

- 请求日志是技术观测，不等同于业务副作用，因此和 `audit_events` 分表。
- `request_id`、`trace_id`、`session_id`、`turn_id` 都可索引查询，方便从前端问题定位到后端某次请求。
- 错误码和错误类型单独列出，便于排查失败请求，不用解析日志文本。

## 字段说明

| 字段 | 类型 | 含义和作用 | 设计原因 |
| --- | --- | --- | --- |
| `id` | `VARCHAR`, PK | 请求日志记录 ID。 | 独立标识一条请求日志。 |
| `request_id` | `VARCHAR`, not null | 单次 HTTP 请求 ID，通常来自 `X-Request-ID` 或自动生成。 | 和响应头、前端日志对齐。 |
| `trace_id` | `VARCHAR`, nullable | 链路追踪 ID。 | 跨请求串联一个用户动作或一个 turn。 |
| `session_id` | `VARCHAR`, nullable | 会话 ID。 | 查询某个会话下所有请求。 |
| `turn_id` | `VARCHAR`, nullable | chat turn ID。 | 查询某个流式 turn 的请求记录。 |
| `method` | `VARCHAR`, not null | HTTP 方法。 | 基础请求事实。 |
| `path` | `VARCHAR`, not null | 请求路径，例如 `/chat/stream`、`/upload/image`。 | 定位接口。 |
| `status_code` | `INTEGER`, not null | HTTP 状态码。 | 区分成功和失败。 |
| `duration_ms` | `FLOAT`, not null | 请求耗时毫秒。 | 性能观测。 |
| `client_ip` | `VARCHAR`, nullable | 客户端 IP。 | 调试来源，开发环境常为 `127.0.0.1`。 |
| `user_agent` | `VARCHAR`, nullable | User-Agent。 | 区分 Android、测试客户端或 httpx。 |
| `error_code` | `VARCHAR`, nullable | 业务错误码。 | 结构化记录失败原因。 |
| `error_type` | `VARCHAR`, nullable | 异常类型或错误分类。 | 调试异常来源。 |
| `created_at` | `DATETIME`, not null | 记录创建时间。 | 最近请求列表和时间排序。 |

## 关系和索引

- 索引：`ix_api_request_logs_request_id`、`ix_api_request_logs_trace_id`、`ix_api_request_logs_session_id`、`ix_api_request_logs_turn_id`。
- 没有外键到业务表，因为请求可能发生在业务对象创建失败之前。

## Review 关注点

- 当前记录完整 `user_agent`，开发库无隐私压力；生产环境需要日志保留周期和脱敏策略。
- SSE 的 `/chat/stream` 请求耗时会覆盖整个流持续时间，不等同于首 token 延迟；首 token/阶段耗时看 `retrieval_traces.filters_applied._stage_timings_ms`。
