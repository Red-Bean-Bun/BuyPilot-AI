# audit_events

## 服务对象

`audit_events` 保存业务副作用和关键流程事件，例如 chat turn 开始/完成/失败、推荐持久化、取消请求、图片上传、加购、反馈创建。它服务调试回放、评委解释和副作用审计。

## 为什么这样设计

- 请求日志只能说明“接口被调用了”，不能说明“业务上发生了什么”。审计表记录 action、资源、前后状态和 metadata。
- `action` 用字符串而不是枚举，方便新增事件，不需要频繁迁移数据库。
- `before_json` / `after_json` / `metadata` 用 JSON，适合不同业务事件保存不同上下文。

## 字段说明

| 字段 | 类型 | 含义和作用 | 设计原因 |
| --- | --- | --- | --- |
| `id` | `VARCHAR`, PK | 审计事件 ID。 | 独立标识事件。 |
| `request_id` | `VARCHAR`, nullable | 所属 HTTP 请求 ID。 | 和 `api_request_logs` 串联。 |
| `trace_id` | `VARCHAR`, nullable | 链路追踪 ID。 | 跨请求聚合一次用户动作。 |
| `session_id` | `VARCHAR`, nullable | 会话 ID。 | 查询一个会话内的业务事件。 |
| `turn_id` | `VARCHAR`, nullable | chat turn ID。 | 查询一个流式 turn 的生命周期。 |
| `actor_type` | `VARCHAR`, not null | 操作者类型，默认 `anonymous`。 | 当前无登录体系，先保留 actor 维度。 |
| `action` | `VARCHAR`, not null | 事件名，例如 `chat.turn_started`、`cart.item_added`。 | 人工 review 最常用的分类字段。 |
| `resource_type` | `VARCHAR`, nullable | 资源类型，例如 `chat_turn`、`conversation`、`image`。 | 描述 action 操作的对象类型。 |
| `resource_id` | `VARCHAR`, nullable | 资源 ID，例如 turn_id、conversation_id、product_id 或 image_url。 | 让事件能回到具体对象。 |
| `side_effect` | `BOOLEAN`, not null | 是否产生业务副作用。 | 区分只读/占位事件和真实写入事件。 |
| `before_json` | `JSON`, nullable | 修改前状态。当前多数事件未写。 | 为未来状态变更审计预留。 |
| `after_json` | `JSON`, nullable | 修改后状态。当前多数事件未写。 | 为未来状态变更审计预留。 |
| `metadata` | `JSON`, nullable | 事件附加信息，例如耗时、fallback、上传图片尺寸、取消结果。 | 不同 action 的上下文不同，JSON 更合适。 |
| `created_at` | `DATETIME`, not null | 事件时间。 | 时间线回放。 |

## 关系和索引

- 索引：`ix_audit_events_request_id`、`ix_audit_events_trace_id`、`ix_audit_events_session_id`、`ix_audit_events_turn_id`、`ix_audit_events_action`。
- 没有外键到业务表，因为审计事件也要记录失败、取消、占位上传等不一定有业务记录的场景。

## Review 关注点

- 当前常见 action 包括 `chat.turn_started`、`chat.turn_completed`、`chat.recommendation_persisted`、`chat.turn_cancelled`、`chat.turn_failed`、`image.uploaded`、`chat.cancel_requested`、`cart.item_added`、`feedback.created`。
- 如果未来进入生产，需要定义 action 命名规范、敏感字段脱敏和保留周期。
