# BuyPilot-AI 前后端接口契约 v0.1

本文档是 Android 前端与 FastAPI 后端的唯一接口真相源。前端联调、SSE 解析、卡片渲染和接口 mock 都以本文为准。

对应后端代码：

- HTTP request/response: `backend/src/types/schemas.py`
- SSE event: `backend/src/types/sse_events.py`
- API router: `backend/src/api/*.py`

## 1. 基础约定

本地服务默认地址：

```text
http://localhost:8000
```

启动：

```bash
cd backend
.venv/bin/uvicorn src.api.app:app --host 0.0.0.0 --port 8000 --reload
```

健康检查：

```http
GET /health
```

响应：

```json
{"status":"ok","service":"buypilot-api"}
```

兼容性规则：

- 后端字段只增不删，event 类型只增不改名。
- 前端必须忽略未知字段。
- 前端必须用 `event` 字段分发 SSE，不要依赖 JSON 字段顺序。
- `seq` 只在同一个 `turn_id` 内递增。
- `event_id` 当前格式为 `{turn_id}:{seq}`。
- `done` 是一次流式响应的结束信号，收到后关闭 loading 状态。

## 2. HTTP API 总览

| Method | Path | 说明 | 状态 |
|---|---|---|---|
| GET | `/health` | 健康检查 | 可用 |
| POST | `/chat/stream` | SSE 流式导购主链路 | 可用 |
| POST | `/chat/cancel` | 取消指定 turn，当前为 best-effort P0 | 可用 |
| POST | `/upload/image` | 图片上传占位接口 | 可用 |
| POST | `/chat/upload/image` | 图片上传兼容路径 | 可用 |
| POST | `/feedback` | 用户反馈 | 可用 |
| POST | `/chat/feedback` | 用户反馈兼容路径 | 可用 |
| GET | `/cart/{session_id}` | 查询购物车 | 可用 |
| GET | `/chat/cart/{session_id}` | 查询购物车兼容路径 | 可用 |

## 3. POST /chat/stream

主链路接口。响应为标准 SSE：

```http
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive
X-Accel-Buffering: no
```

请求体 `ChatStreamRequest`：

```json
{
  "message": "推荐适合油皮的洗面奶，200元以内，日常护肤",
  "session_id": "s1",
  "history": [
    {"role": "user", "content": "之前的用户消息"},
    {"role": "assistant", "content": "之前的助手消息"}
  ],
  "image_url": "https://example.com/image.jpg",
  "criteria_patch": {"budget_max": 150},
  "skip_stages": [],
  "client_turn_id": "android-turn-001",
  "client_trace_id": "trace-001"
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `message` | string | 是 | 用户本轮输入 |
| `session_id` | string/null | 否 | 缺省时后端生成 `sess_{uuid}` |
| `history` | `MessageLite[]` | 否 | 多轮历史，当前 P0 可接受 |
| `image_url` | string/null | 否 | 图片理解输入 |
| `criteria_patch` | object/null | 否 | 前端编辑标准卡后提交的约束 patch |
| `skip_stages` | string[] | 否 | 预留字段 |
| `client_turn_id` | string/null | 否 | 客户端 turn id，预留追踪 |
| `client_trace_id` | string/null | 否 | 客户端 trace id，预留追踪 |

curl 示例：

```bash
curl -N -X POST http://localhost:8000/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message":"推荐适合油皮的洗面奶，200元以内，日常护肤","session_id":"s1"}'
```

SSE block 格式：

```text
event: thinking
data: {"schema_version":"2026-05-20","event":"thinking",...}

event: done
data: {"schema_version":"2026-05-20","event":"done",...}

```

完整推荐场景的典型顺序：

```text
thinking(understanding)
thinking(criteria)
criteria_card
text_delta
text_delta
thinking(searching)
product_card x N
final_decision
done
```

信息不足场景的典型顺序：

```text
thinking(understanding)
clarification
done
```

购物车场景：

```text
thinking(understanding)
cart_action 或 text_delta
done
```

## 4. SSE 公共字段

所有 SSE event 都包含以下公共字段：

```json
{
  "schema_version": "2026-05-20",
  "event": "thinking",
  "session_id": "s1",
  "turn_id": "turn_xxx",
  "seq": 1,
  "event_id": "turn_xxx:1",
  "node_id": "node_xxx",
  "deck_id": null,
  "display_mode": "inline_thinking",
  "created_at_ms": 1770000000000
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `schema_version` | string | 当前为 `2026-05-20` |
| `event` | string | event 类型，见下文 |
| `session_id` | string | 会话 ID |
| `turn_id` | string | 本轮响应 ID |
| `seq` | int | 本轮内递增序号 |
| `event_id` | string | 当前事件 ID |
| `node_id` | string | UI 节点 ID |
| `deck_id` | string/null | 商品卡组 ID；`product_card` 必填 |
| `display_mode` | string/null | 前端渲染模式建议 |
| `created_at_ms` | int/null | 毫秒时间戳 |

`display_mode` 可选值：

```text
inline_thinking
inline_card
inline_text
summary_card
swipe_deck_item
none
```

## 5. SSE Event 类型

### thinking

用途：显示阶段状态。

```json
{
  "event": "thinking",
  "display_mode": "inline_thinking",
  "stage": "understanding",
  "message": "正在理解你的需求"
}
```

前端建议：同一个 turn 内可更新状态行，也可作为轻量状态节点展示。

### clarification

用途：用户信息不足时追问。

```json
{
  "event": "clarification",
  "display_mode": "inline_card",
  "question": "你想买哪个品类？",
  "required_slots": ["category"],
  "suggested_options": ["美妆护肤", "数码电子", "服饰运动", "食品生活"]
}
```

前端建议：用户点击选项后，把选项文本作为下一轮 `message` 发送，沿用同一个 `session_id`。

### criteria_card

用途：展示和编辑购买标准。

```json
{
  "event": "criteria_card",
  "display_mode": "summary_card",
  "editable": true,
  "criteria": {
    "criteria_id": "criteria_xxx",
    "category": "美妆护肤",
    "summary": "油性肌肤，预算200元以内，日常护肤",
    "chips": ["油性", "200元以内", "日常护肤"],
    "constraints": {
      "budget_min": null,
      "budget_max": 200,
      "use_scenario": "日常护肤",
      "skin_type": "油性",
      "ingredient_avoid": [],
      "ingredient_prefer": [],
      "storage": null,
      "screen_size": null,
      "sport_type": null,
      "season": null,
      "dietary": []
    }
  },
  "quick_actions": [
    {
      "action_id": "budget_low",
      "label": "预算压低",
      "action": "criteria_patch",
      "criteria_patch": {"budget_max": 150}
    }
  ]
}
```

注意：约束字段统一在 `criteria.constraints` 下，不要读取旧文档里扁平的 `skin_type` / `budget_max`。

### text_delta

用途：流式文本输出。

```json
{
  "event": "text_delta",
  "display_mode": "inline_text",
  "message_id": "msg_ai_xxx",
  "delta": "我先按油皮、200元以内来筛选。",
  "done": false
}
```

前端建议：

- 按 `message_id` 拼接 `delta`。
- 不要每个 delta 新建一条消息。
- `done:true` 表示该文本消息完成，但整个 SSE 流仍以 `done` event 为最终结束。

### product_card

用途：推荐商品卡。

```json
{
  "event": "product_card",
  "display_mode": "swipe_deck_item",
  "deck_id": "deck_xxx",
  "rank": 1,
  "product": {
    "product_id": "p_beauty_011",
    "name": "珊珂洗颜专科绵润泡沫洁面乳",
    "price": 52.0,
    "currency": "CNY",
    "image_url": "1_美妆护肤/images/p_beauty_011_live.jpg",
    "category": "美妆护肤",
    "sub_category": "洁面",
    "brand": "珊珂",
    "skin_type_match": ["油性", "混合性"],
    "ingredient_tags": ["氨基酸", "控油"],
    "ingredient_avoid": [],
    "use_scenario": "日常"
  },
  "reason": "符合当前预算和使用场景。",
  "risk_notes": [],
  "evidence": [
    {
      "source_type": "product_chunk",
      "snippet": "商品知识片段摘要",
      "source_id": "chunk_p_beauty_011"
    }
  ],
  "actions": [
    {
      "action_id": "dislike_product",
      "label": "不喜欢这个",
      "action": "feedback",
      "feedback_type": "not_interested"
    }
  ]
}
```

注意：

- `product.image_url` 当前是 dataset 相对路径，不是公网 URL。
- 后续需要 FastAPI 静态文件或图片代理后，前端才能直接远程加载。
- `deck_id` 对同一组商品卡保持一致。

### cart_action

用途：加购/移除/查看购物车动作反馈。

```json
{
  "event": "cart_action",
  "display_mode": "inline_card",
  "action": "add",
  "product_id": "p_beauty_011",
  "quantity": 1,
  "status": "success"
}
```

字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `action` | string | `add` / `remove` / `view` 等 |
| `product_id` | string | 商品 ID |
| `quantity` | int | 数量 |
| `status` | string | `success` / `failed` |

### final_decision

用途：总结推荐结论。由 LLM 生成（Qwen-Plus primary / Doubao fallback），winner 从传入商品中选择并经幻觉验证。

```json
{
  "event": "final_decision",
  "display_mode": "summary_card",
  "winner_product_id": "p_beauty_011",
  "summary": "珊珂洗颜专科绵润泡沫洁面乳是符合油性皮肤、200元以内、日常清洁需求的平价洁面产品。",
  "why": ["价格52元，远低于200元预算上限", "明确标注适配油性皮肤", "泡沫细腻丰富，兼顾清洁力与肤感"],
  "not_for": ["追求强效祛痘的人群", "需要卸妆功能的用户"],
  "alternatives": [
    {"product_id": "p_beauty_018", "name": "The Ordinary烟酰胺10%锌1%精华液"}
  ],
  "next_actions": [
    {
      "action_id": "cheaper",
      "label": "再便宜一点",
      "action": "criteria_patch",
      "criteria_patch": {"constraints": {"budget_max": 100}}
    }
  ]
}
```

注意：

- `winner_product_id` 由 LLM 从传入商品中选择，经代码验证确保不在传入列表时 fallback 到 products[0]。
- `alternatives` 为排除 winner 后的前2个商品，由 pipeline 构造。
- `why` 和 `not_for` 由 LLM 生成具体理由，不是模板文本。
- LLM 失败时 fallback：winner 取 products[0]，why 为规则生成。

### done

用途：流结束。

```json
{
  "event": "done",
  "display_mode": "none"
}
```

前端收到后：

- 停止 loading。
- 关闭当前 SSE 解析状态。
- 不要再等待更多 event。

### error

用途：错误提示。

```json
{
  "event": "error",
  "display_mode": "inline_card",
  "code": "LLM_TIMEOUT",
  "message": "后端服务暂时不可用，请稍后重试",
  "retryable": true
}
```

前端建议：

- `retryable:true` 显示重试入口。
- `retryable:false` 只显示错误提示。
- 即使出现 `error`，后端仍应尽量发送 `done` 收尾。

## 6. 数据模型

### MessageLite

```json
{
  "role": "user",
  "content": "消息内容"
}
```

### Constraints

```json
{
  "budget_min": null,
  "budget_max": 200,
  "use_scenario": "日常护肤",
  "skin_type": "油性",
  "ingredient_avoid": ["酒精"],
  "ingredient_prefer": ["氨基酸"],
  "storage": "256GB",
  "screen_size": "6.3英寸",
  "sport_type": "跑步",
  "season": "夏季",
  "dietary": ["低糖"]
}
```

### QuickActionPayload

```json
{
  "action_id": "no_alcohol",
  "label": "不要含酒精",
  "action": "criteria_patch",
  "feedback_type": null,
  "criteria_patch": {"ingredient_avoid": ["酒精"]}
}
```

当前常见 `action`：

| action | 说明 | 前端行为 |
|---|---|---|
| `criteria_patch` | 修改购买标准 | 发送下一轮 `/chat/stream`，带上 `criteria_patch` |
| `feedback` | 用户反馈 | 调 `/feedback` 或作为下一轮 message |
| `open_evidence` | 展开证据 | 本地展开 evidence |
| `compare` | 加入对比 | P1 对比浮层，可先占位 |
| `add_to_cart` | 加购 | 可发送“把这个加到购物车”或走后续加购 API |

### ProductPayload

```json
{
  "product_id": "p_beauty_001",
  "name": "商品标题",
  "price": 720.0,
  "currency": "CNY",
  "image_url": "1_美妆护肤/images/p_beauty_001_live.jpg",
  "category": "美妆护肤",
  "sub_category": "精华",
  "brand": "雅诗兰黛",
  "skin_type_match": ["敏感"],
  "ingredient_tags": ["透明质酸", "修护"],
  "ingredient_avoid": [],
  "use_scenario": "夜间"
}
```

字段说明：

| 字段 | 类型 | 说明 |
|---|---|---|
| `product_id` | string | dataset 商品 ID |
| `name` | string | 商品标题 |
| `price` | number/null | 基础价格 |
| `currency` | string/null | 当前固定 `CNY` |
| `image_url` | string/null | 当前为 dataset 相对路径 |
| `category` | string | 一级品类 |
| `sub_category` | string/null | 子品类 |
| `brand` | string/null | 品牌 |
| `skin_type_match` | string[] | 从商品知识中派生的适用肤质 |
| `ingredient_tags` | string[] | 从商品知识中派生的成分/功效标签 |
| `ingredient_avoid` | string[] | 当前多为空，保留字段 |
| `use_scenario` | string/null | 使用场景 |

## 7. POST /chat/cancel

请求：

```json
{
  "session_id": "s1",
  "turn_id": "turn_xxx"
}
```

响应：

```json
{
  "session_id": "s1",
  "turn_id": "turn_xxx",
  "canceled": true
}
```

当前 P0 是 best-effort 响应，不保证已经中止服务端 generator。

## 8. POST /upload/image 和 /chat/upload/image

请求 `ImageUploadRequest`：

```json
{
  "file_name": "skincare.jpg",
  "content_type": "image/jpeg"
}
```

响应 `ImageUploadResponse`：

```json
{
  "image_url": "https://example.com/upload/mock_skincare.jpg",
  "width": 1280,
  "height": 960,
  "mime_type": "image/jpeg",
  "ocr_text": null,
  "analysis": {"status": "received"}
}
```

注意：当前不是 multipart 上传，只是 P0 占位请求体。真实文件上传后续需要另行扩展。

## 9. POST /feedback 和 /chat/feedback

请求 `FeedbackRequest`：

```json
{
  "session_id": "s1",
  "feedback_type": "not_interested",
  "action": null,
  "product_id": "p_beauty_011",
  "reason": "不喜欢这个品牌"
}
```

响应 `FeedbackResponse`：

```json
{
  "status": "received",
  "session_id": "s1",
  "feedback_type": "not_interested",
  "action": null
}
```

后端当前会写入内存 feedback repository，用于同进程内会话。

## 10. GET /cart/{session_id} 和 /chat/cart/{session_id}

响应 `CartResponse`：

```json
{
  "items": [
    {
      "product_id": "p_beauty_011",
      "name": "珊珂洗颜专科绵润泡沫洁面乳",
      "price": 52.0,
      "quantity": 1,
      "added_at": "2026-05-22T01:00:00Z",
      "product": {
        "product_id": "p_beauty_011",
        "name": "珊珂洗颜专科绵润泡沫洁面乳",
        "price": 52.0,
        "currency": "CNY",
        "image_url": "1_美妆护肤/images/p_beauty_011_live.jpg",
        "category": "美妆护肤",
        "sub_category": "洁面",
        "brand": "珊珂",
        "skin_type_match": ["油性"],
        "ingredient_tags": ["氨基酸"],
        "ingredient_avoid": [],
        "use_scenario": "日常"
      }
    }
  ],
  "total_items": 1,
  "total_price": 52.0
}
```

当前购物车为内存 repository，服务重启后清空。

## 11. Android SSE 处理建议

前端最小落地清单：

- 建立 `ChatStreamRequest`、`MessageLite`、`Constraints`、`CriteriaPayload`、`ProductPayload`、`QuickActionPayload`、`CartResponse` 等 data class。
- 建立 SSE sealed class，至少覆盖 `thinking`、`clarification`、`criteria_card`、`text_delta`、`product_card`、`cart_action`、`final_decision`、`done`、`error`。
- 用 `event` 字段做分发，未知 event 忽略并记录日志。
- UI 层按 `node_id` / `message_id` / `deck_id` 合并节点，避免 delta 和商品卡重复插入。
- 所有接口异常都能落到错误气泡或 toast，不阻塞下一轮输入。

解析策略：

1. 按空行分割 SSE block。
2. 读取 `event:` 行作为事件名。
3. 读取 `data:` 行作为 JSON。
4. 反序列化时先读 JSON 的 `event` 字段，再分发到具体 data class。
5. 未知 event 显示为 no-op，并记录日志。
6. 同一个 `message_id` 的 `text_delta` 合并为一条 AI 文本消息。
7. 同一个 `deck_id` 的 `product_card` 合并为一组可滑动商品卡。
8. 收到 `done` 后结束本轮 loading。

最小状态机：

```text
Idle
 -> Streaming
 -> WaitingForUserClarification  当收到 clarification + done
 -> Completed                    当收到 done
 -> Failed                       当收到 error + done
```

Golden trace 示例：

| 文件 | 场景 | 用途 |
|---|---|---|
| `contracts/examples/demo_budget_beauty.sse` | 完整推荐链路 | UI 主路径验收 |
| `contracts/examples/demo_clarification.sse` | 信息不足追问 | 追问卡片验收 |
| `contracts/examples/demo_error.sse` | 错误响应 | 错误态验收 |

Demo 输入建议：

| 输入 | 预期 |
|---|---|
| `推荐适合油皮的洗面奶，200元以内，日常护肤` | 标准卡、商品卡、最终决策 |
| `随便看看` | 追问卡片 |
| `把这个加到购物车` | 购物车动作或购物车相关文本 |
| `查看购物车` | 购物车摘要 |

## 12. 前端验收标准

前端拿到本文后，至少应能完成以下联调验收：

- `GET /health` 显示服务可用。
- `POST /chat/stream` 能持续读取 SSE，而不是等待整个响应结束。
- `thinking` 能显示阶段状态。
- `criteria_card` 能渲染购买标准和快捷操作。
- `text_delta` 能按 `message_id` 拼接为同一条 AI 文本。
- `product_card` 能按 `deck_id` 聚合为一组商品卡，展示价格、品牌、品类、证据和操作。
- `clarification` 能展示问题和选项，点击选项后沿用 `session_id` 发起下一轮。
- `final_decision` 能展示推荐结论、原因、排除项和下一步操作。
- `done` 能正确结束 loading。
- `error` 能显示错误态，并按 `retryable` 控制是否展示重试。

## 13. 当前限制

- LLM decision、criteria、recommendation 已接真实模型（Qwen-Plus primary / Doubao fallback），仍有 deterministic fallback兜底。
- embedding、rerank 已接真实百炼调用，仍有 deterministic fallback兜底。
- 商品 runtime 来源已切到 `data/raw/ecommerce_agent_dataset`，但图片仍是 dataset 相对路径。
- 购物车、反馈、会话上下文当前是内存实现，服务重启会丢失。
- `/upload/image` 当前不是实际文件上传。
- `/chat/cancel` 当前不保证中止已经开始的 SSE generator。
- feedback 注入了 criteria prompt 但未注入 retrieval 硬过滤。

## 14. 契约验证命令

```bash
cd backend
.venv/bin/pytest tests/test_chat_api.py tests/test_pipeline.py tests/test_api_contracts.py tests/test_product_dataset.py -q
```

全量基线：

```bash
cd backend
.venv/bin/pytest tests -q
```

当前已验证基线：`62 passed`。
