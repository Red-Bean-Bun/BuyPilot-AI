# BuyPilot-AI 前后端接口契约 v0.3

本文档描述 Android 前端与 FastAPI 后端的全部 HTTP 端点和 SSE 事件协议。以实际代码为准，与 `contracts/sse-events.schema.json`（机器可读 source of truth）和 3 个 golden trace 示例保持一致。

**对应后端代码：**
- HTTP 请求/响应类型：`backend/src/types/schemas.py`
- SSE 事件类型：`backend/src/types/sse_events.py`
- API 路由：`backend/src/api/*.py`

**更新摘要（v0.2 → v0.3）：**
- 修正 thinking stage 枚举值（与实际代码对齐）
- 新增 `awaiting_criteria_confirmation` finish_reason
- 新增 `reason_atoms`、`field_sources`、`sku_options`、`ingredient_avoid` 字段
- 新增购物车变更端点（PATCH/DELETE）
- 新增 SSE 自定义响应头
- 新增 admin 鉴权机制说明
- **新增公开 API 端点鉴权（当 ADMIN_API_KEY 配置时，chat/cancel/feedback/upload/cart 也需要 admin key）**
- 修正 health 响应 `fallback_policy` 值

---

## 1. 基础约定

**本地服务地址：** `http://localhost:8000`

**启动：**
```bash
# 推荐（自动 seed text embedding）
make rebuild

# 或本地开发
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
| PATCH | `/cart/{session_id}/items/{product_id}` | 修改购物车商品数量 | ✅ |
| DELETE | `/cart/{session_id}/items/{product_id}` | 删除购物车商品 | ✅ |
| GET | `/admin/eval/runs` | 评测运行列表（?limit=20） | ✅ |
| GET | `/admin/eval/runs/{run_id}` | 单次评测运行详情 | ✅ |
| GET | `/admin/eval/samples` | 评测样本列表 | ✅ |
| POST | `/admin/eval/samples/seed` | 从 `data/eval/eval_samples.json` 填充样本表 | ✅ |
| GET | `/admin/observability/dashboard` | 可观测性看板（HTML） | ✅ |
| GET | `/admin/observability/requests` | API 请求日志查询 | ✅ |
| GET | `/admin/observability/audit` | 审计事件查询 | ✅ |
| GET | `/admin/observability/turns/{turn_id}` | 回合详情（含 SSE 事件序列） | ✅ |
| GET | `/admin/observability/sessions/{session_id}` | 会话详情 | ✅ |
| GET | `/admin/observability/fallbacks` | 降级事件查询 | ✅ |

**静态文件：**
- `/uploads` — 上传的图片（bind mount 到宿主机 `backend/uploads/`）
- `/assets/products` — 商品原始图片（只读挂载 `data/raw/`）

**鉴权机制：**

*Admin 端点（`/admin/*`）：*
- 始终需要 `Authorization: Bearer <ADMIN_API_KEY>` 或 `?token=<ADMIN_API_KEY>`
- 未配置 `ADMIN_API_KEY` 时返回 **404**（隐藏端点存在）
- 已配置但 key 错误/缺失时返回 **401**

*公开 API 端点（`/chat/*`、`/upload/*`、`/feedback`、`/cart/*`）：*
- 当 `ADMIN_API_KEY` **未配置**时：无需鉴权，行为不变
- 当 `ADMIN_API_KEY` **已配置**时：同样需要 `Authorization: Bearer <ADMIN_API_KEY>` 或 `?token=<ADMIN_API_KEY>`，缺失或错误返回 **401**

> **Android 客户端注意：** 如果部署环境配置了 `ADMIN_API_KEY`，所有 API 请求都必须携带 `Authorization: Bearer <key>` 头。客户端已内置 `AdminAuthInterceptor` 自动注入，但需要确保项目根目录 `.env` 文件包含：
> ```env
> ADMIN_API_KEY=b72d57075018654b8d7ad1ab6e71fb0be3f2ec02fc300015
> ```
> 构建时 Gradle 会自动读取并注入 `BuildConfig.ADMIN_API_KEY`，运行时所有请求自动携带 Bearer token。本地开发若不需要鉴权，留空或不配置即可。

---

## 3. 端点详细说明

### 3.1 `GET /health`

**响应：**
```json
{
  "status": "ok",
  "service": "buypilot-api",
  "strict_runtime": false,
  "fallback_policy": "llm_provider_fallback_only",
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
| `message` | string | 是* | 用户输入文本（0-2000 字符）。*当 `image_url` 非空时 message 可为空串；至少需要 message 或 image_url 之一 |
| `session_id` | string \| null | 否 | 会话 ID，为空时自动生成 `sess_<uuid>` |
| `history` | MessageLite[] | 否 | 历史消息，每条含 `role` + `content` |
| `image_url` | string \| null | 否 | 图片 URL（来自 `/upload/image` 返回的 `image_url`） |
| `criteria_patch` | object \| null | 否 | 用户反选/修正的约束 patch |
| `skip_stages` | string[] | 否 | 跳过的 pipeline stage |
| `client_turn_id` | string \| null | 否 | 客户端回合 ID |
| `client_trace_id` | string \| null | 否 | 客户端追踪 ID |

**响应：** `Content-Type: text/event-stream`

**自定义响应头：**

| Header | 说明 |
|--------|------|
| `X-Accel-Buffering` | 固定 `"no"`（禁用 Nginx 缓冲） |
| `X-Request-ID` | 请求唯一标识 |
| `X-Trace-ID` | 追踪 ID（可为空） |

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
  "image_url": "/uploads/upload_5c1c914a77a04ee191f18631a572ba3f.jpg",
  "width": 800,
  "height": 800,
  "mime_type": "image/jpeg",
  "ocr_text": null,
  "analysis": {
    "status": "stored",
    "original_file_name": "p_beauty_001_live.jpg"
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `image_url` | string | 上传后的图片路径，**直接传给 `/chat/stream` 的 `image_url` 字段** |
| `width` | int \| null | 图片宽度（px），用于前端预渲染占位 |
| `height` | int \| null | 图片高度（px） |
| `mime_type` | string | `"image/jpeg"` / `"image/png"` / `"image/webp"` |
| `ocr_text` | string \| null | 当前始终为 null（保留字段） |
| `analysis` | object | 存储状态，前端一般不消费 |

**限制：** 最大 5MB（`MAX_IMAGE_BYTES = 5 * 1024 * 1024`），超出返回 413。

**错误响应：**

| HTTP | code | 原因 |
|------|------|------|
| 413 | `IMAGE_TOO_LARGE` | 文件超过 5MB |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | Content-Type 不是 multipart/form-data |
| 400 | `IMAGE_FILE_REQUIRED` | 缺少 `file` 字段 |
| 400 | `IMAGE_FORMAT_INVALID` | 不支持的图片格式（只接受 JPEG/PNG/WebP） |

```json
{ "detail": { "code": "IMAGE_TOO_LARGE", "message": "Request exceeds 5242880 bytes" } }
```

**联调流程：**
```
1. POST /upload/image  →  拿到 image_url
2. POST /chat/stream   →  body 带 image_url + message
```

后端会用 `image_url` 找到上传文件，转 base64 data URL 发给 Qwen-VL-Plus 做多模态理解，然后走正常推荐链路。

---

### 3.5 `POST /feedback` — 用户反馈

**请求体 (`FeedbackRequest`)：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `session_id` | string | 是 | 会话 ID |
| `feedback_type` | string \| null | 否 | 反馈类型 |
| `action` | string \| null | 否 | 动作：avoid_product / avoid_trait 等 |
| `product_id` | string \| null | 否 | 关联商品 ID |
| `reason` | string \| null | 否 | 反馈原因 |
| `deck_id` | string \| null | 否 | 关联商品 deck ID（用于绑定当前推荐批次） |

```json
{
  "session_id": "sess_xxx",
  "feedback_type": "dislike",
  "action": "avoid_product",
  "product_id": "p_beauty_018",
  "reason": "含酒精",
  "deck_id": "deck_turn_abc123"
}
```

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
      "product": { "..." : "ProductPayload" }
    }
  ],
  "total_items": 1,
  "total_price": 89.0
}
```

同一购物车能力也会通过 `cart_action` SSE 事件返回最新摘要，便于聊天流内直接渲染。

---

### 3.7 `PATCH /cart/{session_id}/items/{product_id}` — 修改购物车数量

**请求体 (`CartMutationRequest`)：**
```json
{
  "quantity": 2
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `quantity` | int | 是 | 新数量（≥0，0 表示删除） |

**响应 (`CartItemPayload`)：**
```json
{
  "product_id": "p_beauty_018",
  "name": "氨基酸洁面乳",
  "price": 89.0,
  "quantity": 2,
  "added_at": "2026-05-24T10:30:00",
  "product": { "..." : "ProductPayload" }
}
```

---

### 3.8 `DELETE /cart/{session_id}/items/{product_id}` — 删除购物车商品

**响应：** `204 No Content`

---

### 3.9 Admin Eval API

所有 admin 端点需要 `Authorization: Bearer <ADMIN_API_KEY>` 或 `?token=<ADMIN_API_KEY>`。

**`GET /admin/eval/runs?limit=20`** — 评测运行列表（最新在前）

**`GET /admin/eval/runs/{run_id}`** — 单次运行详情（含 per-sample 指标）

**`GET /admin/eval/samples`** — 所有评测样本（含 ground truth）

**`POST /admin/eval/samples/seed`** — 从 `data/eval/eval_samples.json` 填充样本表

### 3.10 Admin Observability API

所有端点需要 `Authorization: Bearer <ADMIN_API_KEY>` 或 `?token=<ADMIN_API_KEY>`。

**`GET /admin/observability/dashboard`** — 可观测性看板（HTML 页面）

**`GET /admin/observability/requests`** — API 请求日志查询

| Query 参数 | 说明 |
|------------|------|
| `trace_id` | 按 trace_id 过滤 |
| `session_id` | 按 session_id 过滤 |
| `turn_id` | 按 turn_id 过滤 |
| `limit` | 返回条数（默认 50） |

**`GET /admin/observability/audit`** — 审计事件查询

| Query 参数 | 说明 |
|------------|------|
| `trace_id` | 按 trace_id 过滤 |
| `session_id` | 按 session_id 过滤 |
| `turn_id` | 按 turn_id 过滤 |
| `action` | 按 action 类型过滤 |
| `limit` | 返回条数（默认 50） |

**`GET /admin/observability/turns/{turn_id}`** — 回合详情（含 SSE 事件序列、LLM 调用明细）

**`GET /admin/observability/sessions/{session_id}`** — 会话详情（含所有 turn）

**`GET /admin/observability/fallbacks`** — 降级事件查询

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
| `deck_id` | string \| null | 商品卡所属 deck（product_card 必填，其他事件可选） |
| `display_mode` | string \| null | 前端渲染模式提示 |
| `created_at_ms` | int \| null | 创建时间戳（毫秒） |

### 4.1 `thinking` — 处理中状态

```json
{
  "event": "thinking",
  "display_mode": "inline_thinking",
  "stage": "understanding",
  "message": "正在理解您的需求..."
}
```

`stage` 实际取值（以代码为准）：

| stage | 含义 | 触发场景 |
|-------|------|---------|
| `understanding` | 意图理解 | pipeline 入口、intent 分析 |
| `analyzing_image` | 图片分析 | 上传了图片时，Qwen-VL-Plus 多模态理解 |
| `clarifying` | 澄清追问 | 缺少关键槽位或购物车确认 |
| `criteria` | 标准生成 | 购买标准 LLM 调用 |
| `searching` | 商品检索 | 混合检索（embedding + 硬过滤 + rerank） |
| `decision` | 最终决策 | 决策 LLM 调用 |
| `generating` | 内容生成 | 购物车查看等非推荐场景 |

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
    },
    "field_sources": {
      "category": "user",
      "budget_max": "user",
      "skin_type": "inferred",
      "ingredient_prefer": "history"
    }
  },
  "quick_actions": [
    { "action_id": "patch_1", "label": "预算太高", "action": "criteria_patch", "criteria_patch": { "budget_max": 150 } }
  ]
}
```

**`field_sources` 字段（v0.3 新增）：** 标记每个 criteria 字段来源，前端可用于 UI 信任指示。

| 值 | 含义 |
|----|------|
| `"user"` | 用户本轮明确提供 |
| `"inferred"` | 系统从用户输入推断 |
| `"history"` | 从会话历史继承 |

**`constraints` 完整字段（按品类可选）：**

| 字段 | 类型 | 适用场景 |
|------|------|---------|
| `budget_max` | float \| null | 通用 |
| `budget_min` | float \| null | 通用 |
| `category` | str \| null | 通用 |
| `skin_type` | str \| null | 美妆护肤 |
| `ingredient_prefer` | list[str] | 美妆护肤（偏好成分） |
| `ingredient_avoid` | list[str] | 美妆护肤（避免成分） |
| `brand_avoid` | list[str] | 通用（避免品牌） |
| `origin_avoid` | list[str] | 通用（避免产地） |
| `product_type` | str \| null | 通用 |
| `use_scenario` | str \| null | 通用（使用场景） |
| `storage` | str \| null | 数码电子（存储规格） |
| `screen_size` | str \| null | 数码电子（屏幕尺寸） |
| `sport_type` | str \| null | 服饰运动 |
| `season` | str \| null | 服饰运动 |
| `dietary` | list[str] | 食品生活（饮食偏好） |

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
    "image_url": "/assets/products/1_美妆护肤/images/p_beauty_018_live.jpg",
    "category": "美妆护肤",
    "sub_category": "洁面",
    "brand": "薇诺娜",
    "skin_type_match": ["油性"],
    "ingredient_tags": ["氨基酸", "无酒精"],
    "ingredient_avoid": [],
    "use_scenario": "日常护肤",
    "sku_options": null
  },
  "reason": "氨基酸配方温和清洁，适合油性肤质日常使用",
  "reason_atoms": [
    {
      "dimension": "skin_type",
      "value": "油性",
      "text": "专为油性肤质设计",
      "evidence_id": "p_beauty_018:1"
    }
  ],
  "risk_notes": ["含少量香精，极度敏感肌建议先试用"],
  "evidence": [
    { "source_type": "user_review", "snippet": "油皮用了不紧绷，很舒服", "source_id": "p_beauty_018:1" }
  ],
  "actions": [
    { "action_id": "add_cart_1", "label": "加入购物车", "action": "add_to_cart" },
    { "action_id": "evidence_1", "label": "看证据", "action": "open_evidence" },
    { "action_id": "dislike_1", "label": "不喜欢", "action": "feedback", "feedback_type": "not_interested" }
  ]
}
```

**`reason_atoms` 字段（v0.3 新增）：** 结构化推荐理由，前端可用于高亮展示。

| 字段 | 类型 | 说明 |
|------|------|------|
| `dimension` | string | 维度（如 `"skin_type"`, `"budget"`, `"ingredient"`） |
| `value` | string | 维度值 |
| `text` | string | 人可读描述 |
| `evidence_id` | string \| null | 关联的 evidence source_id |

**`ProductPayload` 完整字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `product_id` | string | 商品 ID |
| `name` | string | 商品名 |
| `price` | float \| null | 价格 |
| `currency` | string \| null | 货币（默认 CNY） |
| `image_url` | string \| null | 商品图片路径 |
| `category` | string | 品类 |
| `sub_category` | string \| null | 子品类 |
| `brand` | string \| null | 品牌 |
| `skin_type_match` | list[str] | 适用肤质 |
| `ingredient_tags` | list[str] | 成分标签 |
| `ingredient_avoid` | list[str] | 需避免的成分（v0.3 新增） |
| `use_scenario` | string \| null | 使用场景 |
| `sku_options` | list[object] \| null | SKU 规格选项（v0.3 新增） |

### 4.6 `cart_action` — 购物车操作

```json
{
  "event": "cart_action",
  "display_mode": "inline_card",
  "action": "add",
  "product_id": "p_beauty_018",
  "quantity": 1,
  "status": "success",
  "cart": {
    "items": [
      {
        "product_id": "p_beauty_018",
        "name": "氨基酸洁面乳",
        "price": 89.0,
        "quantity": 1,
        "added_at": "2026-05-24T10:30:00",
        "product": { "..." : "ProductPayload" }
      }
    ],
    "total_items": 1,
    "total_price": 89.0
  }
}
```

`action` 取值：`add` / `remove` / `update_quantity` / `view`。`status` 取值：`success` / `failed`。

`cart` 为本次操作后的购物车快照，**可以为 null**（如操作失败时）。`view` 事件也会返回该字段，空购物车时 `items=[]`、`total_items=0`、`total_price=0.0`。

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

| 字段 | 类型 | 说明 |
|------|------|------|
| `decision_status` | string \| null | `selected` / `no_match` / `no_suitable_winner` / `needs_more_signal` |
| `confidence` | string \| null | `high` / `medium` / `low` |
| `next_step` | string \| null | `adjust_criteria` / `replace_deck` / `continue_current_deck` / `accept_recommendation` |

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
- `awaiting_criteria_confirmation` — 标准已生成，等待用户确认（v0.3 新增）
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

### 5.1 正常推荐流程（两轮）

推荐流程分为两轮：第一轮展示候选商品，第二轮在用户确认后生成最终决策。

**第一轮（展示候选）：**
```
thinking (stage=understanding)
  → thinking (stage=criteria)
  → criteria_card
  → thinking (stage=searching)
  → product_card (rank=1)
  → product_card (rank=2)
  → ...
  → thinking (stage=searching)
  → text_delta (流式推荐文案, done=false)
  → text_delta (done=true)
  → done (finish_reason=awaiting_product_feedback)
```

**第二轮（用户说"继续"后触发最终决策）：**
```
thinking (stage=decision)
  → final_decision
  → done (finish_reason=completed)
```

### 5.2 澄清流程

```
thinking (stage=understanding)
  → thinking (stage=clarifying)
  → clarification (缺少关键槽位)
  → done (finish_reason=completed)
```

### 5.3 加购流程

```
thinking (stage=understanding)
  → thinking (stage=clarifying)     ← 购物车操作确认
  → cart_action (action=add)
  → done (finish_reason=completed)
```

### 5.4 图片理解流程

```
thinking (stage=analyzing_image)     ← Qwen-VL-Plus 多模态分析
  → thinking (stage=understanding)
  → thinking (stage=criteria)
  → criteria_card
  → thinking (stage=searching)
  → product_card ...
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
- **运维命令：** `Makefile`（`make help` 查看全部）
