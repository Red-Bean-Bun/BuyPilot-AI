# conversations

## 服务对象

`conversations` 保存已完成推荐轮次，用于多轮上下文、上一轮商品引用、历史购买标准合并，以及和检索 trace、证据链接建立关联。

## 为什么这样设计

- 不做复杂登录和长期画像，使用客户端传入的 `session_id` 做轻量会话。
- 每轮保存 `criteria_json` 和 `product_ids`，下一轮可以理解“预算降到 200”“把这个加购”“不要刚才那个”等上下文。
- `ai_response` 预留给完整文本回答，目前主要通过 SSE 流式事件返回，运行库里通常为空。

## 字段说明

| 字段 | 类型 | 含义和作用 | 设计原因 |
| --- | --- | --- | --- |
| `id` | `VARCHAR`, PK | 推荐轮次记录 ID。 | 给 `retrieval_traces` 和 `evidence_links` 做关联。 |
| `session_id` | `VARCHAR`, not null | 客户端会话 ID。 | 无登录态下串联同一用户的一段对话。 |
| `message_id` | `VARCHAR`, not null | 消息级 ID，默认形如 `msg_xxxxxxxx`。 | 区分同一 session 内多条消息，便于前端和 trace 定位。 |
| `user_message` | `VARCHAR`, not null | 用户本轮原始输入。 | 生成会话摘要、调试意图识别和评测回放。 |
| `criteria_json` | `JSON`, nullable | `CriteriaPayload` 的 JSON，包括 `criteria_id`、`category`、`summary`、`chips`、`constraints`。 | 保存 LLM/规则生成的购买标准，支持多轮标准合并。 |
| `ai_response` | `VARCHAR`, nullable | AI 完整回复文本。当前实现暂未系统写入。 | 为非流式回放或历史详情预留。 |
| `product_ids` | `JSON`, nullable | 本轮推荐商品 ID 列表。 | 支撑“这个/刚才推荐的”引用和购物车默认商品选择。 |
| `created_at` | `DATETIME`, not null | 创建时间。 | 按时间取最近轮次。 |

## 关系和索引

- 索引：`ix_conversations_session_id`。
- 被 `retrieval_traces.conversation_id` 和 `evidence_links.conversation_id` 外键引用。
- 没有 `sessions` 表，`session_id` 是跨表软关联。

## Review 关注点

- 当前只在推荐持久化阶段写入，不会记录每个 clarification 或 view_cart 事件；这些事件主要通过 `audit_events` 观察。
- `criteria_json` 是 JSON，字段变化对 schema 影响小，但需要靠 Pydantic 模型和测试保证结构稳定。
