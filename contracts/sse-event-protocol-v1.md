# BuyPilot-AI 统一 SSE 事件协议 v1.0

> **本文件是前后端唯一的接口合约真相来源**。两份 PRD 中的 SSE 事件定义如有不一致，以此文件为准。
>
> 决策原则：前端为主，后端对齐。后端内部数据模型自由设计，但 SSE 输出必须严格遵循此协议。

---

## 全局规则

1. **每个 SSE 事件必须包含 `seq: Int`**（正整数递增，从 1 开始），用于前端检测丢帧
2. **每个 SSE 事件必须包含 `session_id: String`**（必填，非可选）。首次请求时后端生成 UUID，后续所有事件携带
3. **字段命名统一使用 snake_case**，前端 Kotlin class 用 `@SerialName` 映射

---

## SSE 事件完整定义

### 1. `thinking` — 管道阶段状态

```json
{
  "seq": 1,
  "event": "thinking",
  "session_id": "sess_001",
  "stage": "understanding",
  "message": "正在理解年龄、场景、预算与安全约束"
}
```

**stage 枚举值（统一）**：

| stage 值 | 中文映射 | 管道阶段 |
|-----------|----------|----------|
| `understanding` | 正在理解需求 | slot_checker + intent |
| `clarifying` | 需要更多信息 | 澄清阶段 |
| `searching` | 正在检索商品 | embedding + 硬过滤 + 向量召回 |
| `generating` | 正在生成推荐 | criteria / recommendation 流式输出 |
| `decision_making` | 正在整理结论 | final_decision 生成 |

> 前端顶部状态区按 stage → 中文映射渲染，150–250ms 去抖防闪烁。

---

### 2. `clarification` — 需求澄清（多问题模式）

```json
{
  "seq": 2,
  "event": "clarification",
  "session_id": "sess_001",
  "questions": [
    {
      "slot": "age",
      "question": "孩子多大呢？",
      "suggested_options": ["3岁", "4岁", "5-6岁", "7岁以上"]
    },
    {
      "slot": "scenario",
      "question": "主要在哪里玩呢？",
      "suggested_options": ["室内", "户外", "都行"]
    }
  ],
  "required_slots": ["age", "scenario"],
  "partial_criteria": {"budget_max": 200}
}
```

**字段说明**：
- `questions`：每个问题独立带 `slot`（关联 slot 定义）+ `suggested_options`（可点选选项）
- `required_slots`：缺失的必填 slot 列表
- `partial_criteria`：后端已从用户输入推断出的部分标准（展平格式，同 CriteriaPayload 结构）

---

### 3. `criteria_card` — 购买标准卡

```json
{
  "seq": 3,
  "event": "criteria_card",
  "session_id": "sess_001",
  "criteria_id": "crit_001",
  "editable": true,
  "criteria": {
    "age": 4,
    "scenario": "indoor",
    "budget_min": null,
    "budget_max": 200,
    "toy_type": null,
    "education_dimensions": ["logic", "fine_motor"],
    "safety_features": ["no_small_parts"],
    "messiness_level": "low",
    "requires_battery": false
  },
  "risks": ["4岁儿童使用磁力玩具需家长陪同收纳"],
  "quick_actions": [
    {
      "action_id": "budget_low",
      "label": "预算压低",
      "action": "criteria_patch",
      "criteria_patch": {"budget_max": 150}
    },
    {
      "action_id": "more_educational",
      "label": "更偏益智",
      "action": "criteria_patch",
      "criteria_patch": {"education_dimensions": ["logic", "focus"]}
    }
  ]
}
```

**关键决策**：
- `criteria` 使用扁平 `CriteriaPayload` 格式（前端直接吃）
- 后端内部 LLM 生成约束列表 `[{dimension, value, weight}]`，管道内做一次展平转换输出 `CriteriaPayload`
- `criteria_id`：评测闭环 + Debug 页关联 trace/evidence 的标识
- `risks`：购买标准级别的风险提示（区别于 product_card 的 `risk_notes`）
- 删除 `user_profile`（画像信息已展平在 `CriteriaPayload` 各字段中）
- 删除顶层 `scenario`（已在 `criteria.scenario` 中）

---

### 4. `text_delta` — AI 文本流式增量

```json
{
  "seq": 4,
  "event": "text_delta",
  "session_id": "sess_001",
  "message_id": "msg_ai_01",
  "delta": "我先按 4 岁、室内、200 元以内来筛选。",
  "done": false
}
```

```json
{
  "seq": 7,
  "event": "text_delta",
  "session_id": "sess_001",
  "message_id": "msg_ai_01",
  "delta": "",
  "done": true
}
```

**字段说明**：
- `message_id`：标识同一条 AI 消息气泡。前端所有同 `message_id` 的 delta 拼接在同一条消息节点中
- `done`：`true` 表示该 `message_id` 的文本流结束，前端做最终 Markdown 渲染和排版
- 后端管道每条 text_delta 都必须携带 `message_id` 和 `done`

---

### 5. `product_card` — 商品推荐卡

```json
{
  "seq": 5,
  "event": "product_card",
  "session_id": "sess_001",
  "rank": 1,
  "product": {
    "product_id": "toy_1001",
    "name": "大颗粒磁力积木",
    "brand": "Magformers",
    "price": 169,
    "currency": "CNY",
    "image_url": "https://example.com/1.jpg",
    "age_min": 4,
    "age_max": 6,
    "toy_type": "building",
    "education_dimensions": ["logic", "creativity"],
    "safety_features": ["no_small_parts"],
    "play_scenario": ["indoor"],
    "messiness_level": "low",
    "requires_battery": false
  },
  "reason": "适合 4 岁儿童进行室内安静拼搭，兼顾逻辑与动手能力。",
  "risk_notes": ["含磁性部件，建议家长陪同收纳。"],
  "evidence": [
    {
      "source_type": "product_chunk",
      "snippet": "适合 4-6 岁，大颗粒设计，减少误吞风险。",
      "source_id": "chunk_001"
    }
  ],
  "actions": [
    {
      "action_id": "show_evidence",
      "label": "看证据",
      "action": "open_evidence"
    },
    {
      "action_id": "dislike_product",
      "label": "不喜欢这个",
      "action": "feedback",
      "feedback_type": "not_interested"
    },
    {
      "action_id": "show_alternatives",
      "label": "换相似的",
      "action": "feedback",
      "feedback_type": "show_alternatives"
    }
  ]
}
```

**关键决策**：
- 商品信息嵌套在 `product` 对象内（不是扁平），`rank` 在外层
- 字段名统一：`reason`（不是 `recommend_reason`），`risk_notes`（不是 `risks`）
- `brand`：新增字段，前端 ProductPayload 加 `brand: String?`
- `currency`：前端有但后端 products 表没有。后端固定传 `"CNY"` 或从配置取
- `image_url`：后端 products 表存 `image_urls TEXT[]`，SSE 输出取第一个元素

---

### 6. `final_decision` — 最终决策卡

```json
{
  "seq": 8,
  "event": "final_decision",
  "session_id": "sess_001",
  "winner_product_id": "toy_1001",
  "summary": "如果你更看重综合益智性和可玩时长，优先选大颗粒磁力积木。",
  "why": ["适龄匹配 4 岁", "预算内", "无电池", "室内友好"],
  "not_for": ["若你希望绝对避免磁性件，不建议 Top1"],
  "alternatives": [
    {"product_id": "toy_1002", "name": "木质拼图启蒙盒"}
  ],
  "next_actions": [
    {
      "action_id": "cheaper",
      "label": "再便宜一点",
      "action": "criteria_patch",
      "criteria_patch": {"budget_max": 150}
    },
    {
      "action_id": "compare",
      "label": "加入对比",
      "action": "compare"
    }
  ]
}
```

**关键决策**：
- 命名统一为前端定义：`winner_product_id`（不是 `recommended_id`），`not_for`（不是 `not_suitable_for`），`summary`（后端新增），`next_actions`（后端新增）
- 删除 `recommended_name`——前端从已收到的 product_card 取商品名
- 删除 `suitable_for`——`why` 已包含推荐理由
- `alternatives` 保留 `name` 字段（备选商品名，便于决策卡直接展示）

---

### 7. `done` — 流结束

```json
{
  "seq": 9,
  "event": "done",
  "session_id": "sess_001",
  "criteria_id": "crit_001",
  "total_products": 2
}
```

**关键决策**：
- 字段：`criteria_id` + `total_products`（不是前端的 `session_id`）
- `session_id` 不在 done 里重复（已在全局必填字段中）
- `criteria_id`：供 Debug 页和评测闭环关联
- `total_products`：本次推荐的商品数量，Debug 页展示

---

### 8. `error` — 错误事件

```json
{
  "seq": 2,
  "event": "error",
  "session_id": "sess_001",
  "code": "CLARIFICATION_FAILED",
  "message": "意图识别失败，请重新描述您的需求",
  "retryable": true
}
```

**错误码枚举（统一）**：

| code | HTTP 等价 | retryable | 说明 |
|------|-----------|-----------|------|
| `INTENT_FAILED` | 500 | true | 意图识别 LLM 失败 |
| `CLARIFICATION_FAILED` | 400 | true | 澄清阶段异常 |
| `RETRIEVAL_EMPTY` | 404 | true | 检索无结果 |
| `LLM_TIMEOUT` | 504 | true | LLM 调用超时 |
| `RATE_LIMITED` | 429 | false | 请求限频 |
| `INTERNAL_ERROR` | 500 | false | 不可恢复内部错误 |

---

## HTTP API 端点统一定义

### `POST /chat/stream` — SSE 流式导购对话

**请求**：
```json
{
  "message": "给4岁孩子买益智玩具，预算200以内",
  "session_id": null,
  "history": [{"role": "user", "content": "..."}, {"role": "assistant", "content": "..."}],
  "image_url": null,
  "criteria_patch": null,
  "skip_stages": null
}
```

**字段说明**：
- `session_id`：可选。`null` = 新会话（后端生成 UUID，第一个 SSE 事件携带），非 `null` = 续接已有会话
- `criteria_patch`：用户通过 quick_actions 修正标准时携带
- `skip_stages`：快捷路径跳过已完成阶段（如 `["intent", "criteria"]`）
- 删除 `client_trace_id`——Debug 用 `session_id + seq` 追踪

**响应**：`Content-Type: text/event-stream`，按上述 8 种 SSE 事件序列输出

---

### `POST /feedback` — 用户反馈记录

**请求**：
```json
{
  "session_id": "sess_001",
  "product_id": "toy_1001",
  "action": "feedback",
  "feedback_type": "not_interested",
  "reason": "不喜欢磁性玩具",
  "criteria_patch": null
}
```

**action 枚举（统一）**：

| action | 说明 |
|--------|------|
| `criteria_patch` | 修正购买标准（携带 criteria_patch） |
| `feedback` | 反馈（携带 feedback_type） |
| `open_evidence` | 查看证据 |
| `compare` | 加入对比 |

**feedback_type 枚举**：

| feedback_type | 说明 |
|---------------|------|
| `like` | 喜欢 |
| `dislike` | 不喜欢 |
| `not_interested` | 不感兴趣 |
| `show_alternatives` | 换相似的 |

**响应**：
```json
{"status": "ok"}
```

---

### `POST /upload/image` — 图片上传与解析

**请求**：`multipart/form-data`，字段 `file`

**响应**：
```json
{
  "image_url": "https://example.com/upload/abc.jpg",
  "width": 1280,
  "height": 960,
  "mime_type": "image/jpeg",
  "ocr_text": "Magformers 磁力积木 4-6岁"
}
```

---

### `GET /sessions/{id}/history` — 恢复对话历史（新增 P0）

**响应**：
```json
{
  "session_id": "sess_001",
  "messages": [
    {"role": "user", "content": "给4岁孩子买益智玩具", "timestamp": "2026-05-20T10:00:00Z"},
    {"role": "assistant", "content": "我为您筛选了...", "timestamp": "2026-05-20T10:00:05Z"}
  ]
}
```

---

### `POST /chat/cancel` — 停止当前管道（新增 P1）

**请求**：
```json
{"session_id": "sess_001"}
```

**响应**：
```json
{"status": "cancelled"}
```

---

## 删除项

| 项目 | 原因 |
|------|------|
| `POST /stream/token` | Stream SDK 残留，与 OkHttp SSE 直连策略矛盾 |
| `client_trace_id` | Debug 追踪用 session_id + seq 足够 |
| criteria_card `user_profile` | 画像信息已展平在 CriteriaPayload 各字段中 |
| criteria_card 顶层 `scenario` | 已在 criteria.scenario 中 |
| final_decision `recommended_name` | 前端从 product_card 取商品名 |
| final_decision `suitable_for` | `why` 已包含推荐理由 |
| product_card `match_details` | `reason` + `evidence` 已覆盖 |