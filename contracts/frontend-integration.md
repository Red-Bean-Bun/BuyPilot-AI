# BuyPilot-AI 前后端接口契约 v0.2

本文档描述 Android 前端与 FastAPI 后端的全部 HTTP 端点和 SSE 事件协议。以实际代码为准，与 `contracts/sse-events.schema.json`（机器可读 source of truth）和 3 个 golden trace 示例保持一致。

**对应后端代码：**
- HTTP 请求/响应类型：`backend/src/types/schemas.py`
- SSE 事件类型：`backend/src/types/sse_events.py`
- API 路由：`backend/src/api/*.py`

---

## 1. 基础约定

**本地服务地址：** `http://localhost:8000`

**启动：**
```bash
cd backend && uv run uvicorn src.api.app:app --reload --port 8000
```

**健康检查：** `GET /health`

**兼容性规则（铁律 1）：**
- SSE event type 共 9 种（thinking / clarification / criteria_card / text_delta / product_card / cart_action / final_decision / done / error），**禁止新增**。这是全集。
- 后端字段只增不删，event 类型不改名。
- 前端必须忽略未知字段。
- 前端用 `event` 字段分发 SSE，不依赖 JSON 字段顺序。
- `seq` 在同一 `turn_id` 内递增；`event_id` 格式为 `{turn_id}:{seq}`。
- `done` 是一次流式响应的结束信号，收到后关闭 loading 状态。

---

## 2. HTTP API 总览

| Method | Path | 说明 | 状态 |
|--------|------|------|------|
| GET | `/health` | 健康检查 | ✅ |
| POST | `/chat/stream` | SSE 流式导购主链路 | ✅ |
| POST | `/chat/cancel` | 取消指定 turn（best-effort 持久化） | ✅ |
| POST | `/upload/image` | 图片上传（仅支持 multipart/form-data） | ✅ |
| POST | `/chat/upload/image` | 图片上传兼容路径（不在 OpenAPI schema 中） | ✅ |
| POST | `/feedback` | 用户反馈 | ✅ |
| POST | `/chat/feedback` | 用户反馈兼容路径（不在 OpenAPI schema 中） | ✅ |
| GET | `/cart/{session_id}` | 查询购物车 | ✅ |
| GET | `/admin/eval/runs` | 评测运行列表（?limit=20） | ✅ |
| GET | `/admin/eval/runs/{run_id}` | 单次评测运行详情 | ✅ |
| GET | `/admin/eval/samples` | 评测样本列表 | ✅ |
| POST | `/admin/eval/samples/seed` | 从 `data/eval/eval_samples.json` 填充样本表 | ✅ |

**静态文件：** `/uploads` 已挂载为 FastAPI StaticFiles，上传的图片可通过 `/uploads/<filename>` 访问。

---

## 3. 端点详细说明

### 3.1 `GET /health`

**响应：**
```json
{
  "status": "ok",
  "service": "buypilot-api",
  "strict_runtime": false,
  "fallback_policy": "demo_visible_degradation",
  "active_turns": 0
}
```

- `strict_runtime`：`true` 时关键降级会显性失败（`STRICT_RUNTIME=1` 环境变量控制）
- `active_turns`：当前活跃的 SSE 流数量

---

### 3.2 `POST /chat/stream` — SSE 流式导购主链路

**请求体 (`ChatStreamRequest`)：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `message` | string | 是 | 用户输入文本 |
| `session_id` | string \| null | 否 | 会话 ID，为空时自动生成 `sess_<uuid>` |
| `history` | MessageLite[] | 否 | 历史消息，每条含 `role` + `content` |
| `image_url` | string \| null | 否 | 图片 URL（用于多模态分析） |
| `criteria_patch` | object \| null | 否 | 用户反选/修正的约束 patch |
| `skip_stages` | string[] | 否 | 跳过的 pipeline stage |
| `client_turn_id` | string \| null | 否 | 客户端回合 ID |
| `client_trace_id` | string \| null | 否 | 客户端追踪 ID |

**响应：** `Content-Type: text/event-stream`

SSE 事件序列格式为：
```
event: <event_type>
data: <JSON>

```

具体事件类型和字段见第 4 节。

---

### 3.3 `POST /chat/cancel` — 取消生成

**请求体 (`CancelRequest`)：**
```json
{
  "session_id": "sess_xxx",
  "turn_id": "turn_xxx"
}
```

**响应 (`CancelResponse`)：**
```json
{
  "session_id": "sess_xxx",
  "turn_id": "turn_xxx",
  "canceled": true
}
```

> Best-effort cancellation：写入 DB cancel request，pipeline heartbeat 跨进程检测。同一进程内通过 cancel_token 快速中断。

---

### 3.4 `POST /upload/image` — 图片上传

仅支持 **multipart/form-data**（非 multipart 请求返回 415）：

```
POST /upload/image
Content-Type: multipart/form-data; boundary=...

--boundary
Content-Disposition: form-data; name="file"; filename="product.jpg"
Content-Type: image/jpeg

<binary image data>
--boundary--
```

**响应 (`ImageUploadResponse`)：**
```json
{
  "image_url": "/uploads/abc123_product.jpg",
  "width": 800,
  "height": 600,
  "mime_type": "image/jpeg",
  "ocr_text": null,
  "analysis": {}
}
```

**限制：** 最大文件大小由 `MAX_IMAGE_BYTES` 控制（`services/image_upload.py`），超出返回 `413`。

**错误响应：**
```json
{ "detail": { "code": "IMAGE_TOO_LARGE", "message": "Request exceeds N bytes" } }
```

---

### 3.5 `POST /feedback` — 用户反馈

**请求体 (`FeedbackRequest`)：**
```json
{
  "session_id": "sess_xxx",
  "feedback_type": "dislike",
  "action": "avoid_product",
  "product_id": "p_beauty_018",
  "reason": "含酒精"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `session_id` | string | 是 | 会话 ID |
| `feedback_type` | string \| null | 否 | 反馈类型 |
| `action` | string \| null | 否 | 动作：avoid_product / avoid_trait 等 |
| `product_id` | string \| null | 否 | 关联商品 ID |
| `reason` | string \| null | 否 | 反馈原因 |

**响应 (`FeedbackResponse`)：**
```json
{
  "status": "received",
  "session_id": "sess_xxx",
  "feedback_type": "dislike",
  "action": "avoid_product"
}
```

反馈会持久化到 `feedbacks` 表，并在同一 session 下一轮推荐中注入 retrieval 硬过滤（avoid_products / avoid_traits）。

---

### 3.6 `GET /cart/{session_id}` — 查询购物车

**路径参数：** `session_id` — 会话 ID

**响应 (`CartResponse`)：**
```json
{
  "items": [
    {
      "product_id": "p_beauty_018",
      "name": "氨基酸洁面乳",
      "price": 89.0,
      "quantity": 1,
      "added_at": "2026-05-24T10:30:00",
      "product": { ... }
    }
  ],
  "total_items": 1,
  "total_price": 89.0
}
```

> ⚠️ 当前支持 add/view，Remove/Update 未实现。

---

### 3.7 Admin Eval API

**`GET /admin/eval/runs?limit=20`** — 评测运行列表（最新在前）

**`GET /admin/eval/runs/{run_id}`** — 单次运行详情（含 per-sample 指标）

**`GET /admin/eval/samples`** — 所有评测样本（含 ground truth）

**`POST /admin/eval/samples/seed`** — 从 `data/eval/eval_samples.json` 填充样本表

---

## 4. SSE 事件类型（9 种）

所有事件共享基础字段（`SSEEventBase`）：

| 字段 | 类型 | 说明 |
|------|------|------|
| `schema_version` | string | 固定 `"2026-05-20"` |
| `event` | string | 事件类型（9 种之一） |
| `session_id` | string | 会话 ID |
| `turn_id` | string | 回合 ID |
| `seq` | int | 回合内递增序号 |
| `event_id` | string | `{turn_id}:{seq}` |
| `node_id` | string | 节点标识 |
| `deck_id` | string \| null | 商品卡所属 deck（product_card 必填） |
| `display_mode` | string \| null | 前端渲染模式提示 |
| `created_at_ms` | int \| null | 创建时间戳（毫秒） |

### 4.1 `thinking` — 处理中状态

```json
{
  "event": "thinking",
  "display_mode": "inline_thinking",
  "stage": "intent_analysis",
  "message": "正在理解您的需求..."
}
```

`stage` 取值：`intent_analysis` / `multimodal_analysis` / `criteria_generation` / `retrieval` / `recommendation_generation` / `decision_generation`

### 4.2 `clarification` — 澄清追问

```json
{
  "event": "clarification",
  "display_mode": "inline_card",
  "question": "请问您的肤质是？",
  "required_slots": ["skin_type"],
  "suggested_options": ["油性", "干性", "混合性", "敏感性"]
}
```

### 4.3 `criteria_card` — 购买标准卡片

```json
{
  "event": "criteria_card",
  "display_mode": "summary_card",
  "editable": true,
  "criteria": {
    "criteria_id": "c_auto_001",
    "category": "美妆护肤",
    "summary": "油皮适用的氨基酸洁面，200元以内",
    "chips": ["油性肤质", "氨基酸成分", "200元以内"],
    "constraints": {
      "budget_max": 200,
      "skin_type": "油性",
      "ingredient_prefer": ["氨基酸"]
    }
  },
  "quick_actions": [
    { "action_id": "patch_1", "label": "预算太高", "action": "criteria_patch", "criteria_patch": { "budget_max": 150 } }
  ]
}
```

### 4.4 `text_delta` — 流式文本增量

```json
{
  "event": "text_delta",
  "display_mode": "inline_text",
  "message_id": "msg_001",
  "delta": "根据您的肤质和预算，推荐以下商品：",
  "done": false
}
```

- `done: true` 表示该消息流结束
- 同一 `message_id` 的多个 `text_delta` 按 `seq` 拼接

### 4.5 `product_card` — 商品卡片

```json
{
  "event": "product_card",
  "display_mode": "swipe_deck_item",
  "deck_id": "deck_main",
  "rank": 1,
  "product": {
    "product_id": "p_beauty_018",
    "name": "氨基酸洁面乳",
    "price": 89.0,
    "currency": "CNY",
    "image_url": "/uploads/p_beauty_018.jpg",
    "category": "美妆护肤",
    "sub_category": "洁面",
    "brand": "薇诺娜",
    "skin_type_match": ["油性"],
    "ingredient_tags": ["氨基酸", "无酒精"],
    "use_scenario": "日常护肤"
  },
  "reason": "氨基酸配方温和清洁，适合油性肤质日常使用",
  "risk_notes": ["含少量香精，极度敏感肌建议先试用"],
  "evidence": [
    { "source_type": "user_review", "snippet": "油皮用了不紧绷，很舒服", "source_id": "p_beauty_018:1" }
  ],
  "actions": [
    { "action_id": "add_cart_1", "label": "加入购物车", "action": "add_to_cart" },
    { "action_id": "dislike_1", "label": "不喜欢", "action": "feedback", "feedback_type": "dislike" }
  ]
}
```

### 4.6 `cart_action` — 购物车操作

```json
{
  "event": "cart_action",
  "display_mode": "inline_card",
  "action": "add",
  "product_id": "p_beauty_018",
  "quantity": 1,
  "status": "success"
}
```

`action` 取值：`add` / `view`。`status` 取值：`success` / `failed`。

### 4.7 `final_decision` — 最终决策

```json
{
  "event": "final_decision",
  "display_mode": "summary_card",
  "winner_product_id": "p_beauty_018",
  "summary": "综合油皮适用、预算和成分安全，推荐薇诺娜氨基酸洁面乳",
  "why": ["氨基酸配方温和不刺激", "专为油性肤质设计", "200元以内性价比最高"],
  "not_for": ["极度敏感肌", "偏好泡沫丰富质地"],
  "alternatives": [
    { "product_id": "p_beauty_005", "name": "水杨酸洁面啫喱" }
  ],
  "next_actions": [
    { "action_id": "compare", "label": "对比商品", "action": "compare" },
    { "action_id": "restart", "label": "重新推荐", "action": "feedback", "feedback_type": "restart" }
  ],
  "decision_status": "selected",
  "confidence": "high",
  "next_step": "accept_recommendation"
}
```

**新增字段（v0.3）：**
- `decision_status` (string | null)：`selected` / `no_match` / `no_suitable_winner` / `needs_more_signal`
- `confidence` (string | null)：`high` / `medium` / `low`
- `next_step` (string | null)：`adjust_criteria` / `replace_deck` / `continue_current_deck` / `accept_recommendation`

### 4.8 `done` — 流结束信号

```json
{
  "event": "done",
  "display_mode": "none",
  "finish_reason": "awaiting_product_feedback",
  "deck_id": "deck_turn_abc123"
}
```

`finish_reason` 取值：
- `awaiting_product_feedback` — 候选已展示，等待用户反馈/继续
- `awaiting_criteria_adjustment` — 无匹配或全部排除，等待用户调整筛选
- `completed` — 流正常结束（含 final_decision）
- `cancelled` — 用户取消
- `error` — 异常终止

收到 `done` 后前端关闭 loading 状态，本次 SSE 流完整结束。

### 4.9 `error` — 错误事件

```json
{
  "event": "error",
  "display_mode": "inline_card",
  "code": "LLM_TIMEOUT",
  "message": "模型响应超时，请重试",
  "retryable": true
}
```

`retryable: true` 时前端可展示重试按钮。

---

## 5. 典型 SSE 流序列

### 5.1 正常推荐流程

```
thinking (stage=intent_analysis)
  → thinking (stage=criteria_generation)
  → thinking (stage=retrieval)
  → criteria_card
  → product_card (rank=1)
  → product_card (rank=2)
  → ...
  → thinking (stage=recommendation_generation)
  → text_delta (流式推荐文案)
  → thinking (stage=decision_generation)
  → final_decision
  → done
```

### 5.2 澄清流程

```
thinking (stage=intent_analysis)
  → clarification (缺少关键槽位)
  → done
```

### 5.3 加购流程

```
thinking (stage=intent_analysis)
  → cart_action (action=add)
  → done
```

---

## 6. 参考资源

- **SSE JSON Schema（机器可读 source of truth）：** `contracts/sse-events.schema.json`
- **Golden trace 示例：** `contracts/examples/`（demo_budget_beauty.sse / demo_clarification.sse / demo_error.sse）
- **后端 PRD：** `doc/prd/02-后端与AgentPRD.md`
- **前端 PRD：** `doc/prd/01-Android前端PRD.md`
- **后端完成状态：** `doc/status/backend-completion.md`
- **SSE 事件 Python 定义：** `backend/src/types/sse_events.py`
- **HTTP 请求/响应类型：** `backend/src/types/schemas.py`