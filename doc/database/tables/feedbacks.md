# feedbacks

## 服务对象

`feedbacks` 保存同一会话内的用户反馈，用于下一轮推荐时排除商品、提取反选约束或偏好词。它对应 P1 的“反选排除”和“反馈影响同会话下一轮推荐”能力。

## 为什么这样设计

- 反馈只影响当前 `session_id`，不做长期用户画像，避免隐私和复杂推荐系统问题。
- `product_id` 允许为空，因为用户可能说“太贵了”“不要含酒精的”这类针对标准的反馈，不一定针对某个商品。
- `reason` 保留原始文本，服务层再从中提取 `avoid_traits` 或 `prefer_traits`。

## 字段说明

| 字段 | 类型 | 含义和作用 | 设计原因 |
| --- | --- | --- | --- |
| `id` | `VARCHAR`, PK | 反馈记录 ID。 | 独立追踪每次反馈。 |
| `session_id` | `VARCHAR`, not null | 所属会话。 | 反馈只在同会话生效。 |
| `product_id` | `VARCHAR`, nullable | 被反馈的商品 ID。 | 支撑“不喜欢这个商品”后排除该商品。允许为空以支持标准级反馈。 |
| `action` | `VARCHAR`, not null | 反馈动作，例如 `dislike`、`not_interested`、`feedback`。 | 简化为字符串，方便前端 quick action 和自然语言反馈统一处理。 |
| `reason` | `VARCHAR`, nullable | 用户反馈原因或自然语言补充。 | 用于提取排除词和偏好词。 |
| `created_at` | `DATETIME`, not null | 创建时间。 | 按时间聚合同会话反馈。 |

## 关系和索引

- 索引：`ix_feedbacks_session_id`。
- 当前 `product_id` 没有声明外键。这是为了允许历史商品、空商品和自然语言反馈共用一张表。

## Review 关注点

- 如果未来要做长期画像，需要新增用户维度和过期策略，不能直接把这张表当永久偏好表。
- 当前动作值没有数据库枚举约束，依赖服务层和前端契约约束；hackathon 阶段更利于快速迭代。
