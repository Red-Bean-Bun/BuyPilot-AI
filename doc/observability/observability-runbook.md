# Chat Stream 可观测性实操指南

> 实际使用指南，包含真实命令、输出示例和调试流程。

## 快速开始

### 1. 启动后端（默认启用观测）

```bash
cd backend
uv run uvicorn src.api.app:app --reload --port 8000
```

观测默认启用，无需额外配置。

### 2. 发送聊天请求

```bash
curl -X POST http://localhost:8000/chat/stream \
  -H "Content-Type: application/json" \
  -d '{
    "session_id": "test_session_001",
    "client_turn_id": "turn_001",
    "message": "推荐适合油皮的洗面奶，200元以内"
  }' \
  --no-buffer
```

### 3. 获取 turn_id

响应 header 中包含 `X-Request-ID`，SSE 事件中包含 `turn_id`：

```text
event: thinking
data: {"event":"thinking","turn_id":"turn_abc123","seq":1,...}

event: done
data: {"event":"done","turn_id":"turn_abc123","seq":15,...}
```

记下 `turn_id`（如 `turn_abc123`）。

### 4. 查询 Debug Bundle

```bash
curl "http://localhost:8000/admin/observability/turns/turn_abc123" | jq .
```

## Debug Bundle 结构

完整响应示例（已脱敏）：

```json
{
  "turn_id": "turn_abc123",
  "requests": [
    {
      "method": "POST",
      "path": "/chat/stream",
      "status_code": 200,
      "duration_ms": 2847.5,
      "created_at": "2026-06-01T10:23:45.123Z"
    }
  ],
  "audit_events": [
    {
      "action": "chat.turn_started",
      "resource_type": "turn",
      "resource_id": "turn_abc123",
      "created_at": "2026-06-01T10:23:45.125Z"
    },
    {
      "action": "chat.context_diagnostic",
      "resource_type": "criteria",
      "metadata": {
        "diagnostic_code": "BUDGET_PATCH_LOST",
        "severity": "warning",
        "user_budget_max": 200,
        "before_budget_max": 300,
        "after_budget_max": 300
      },
      "created_at": "2026-06-01T10:23:46.500Z"
    },
    {
      "action": "chat.turn_completed",
      "metadata": {
        "stage_timings_ms": {
          "intent": 156.2,
          "criteria": 892.3,
          "retrieval": 423.1,
          "recommendation": 1205.8
        }
      },
      "created_at": "2026-06-01T10:23:47.970Z"
    }
  ],
  "llm_calls": [
    {
      "task": "analyze_intent",
      "profile": "doubao_seed_lite",
      "model": "doubao-seed-2.0-lite",
      "provider": "Doubao",
      "status": "success",
      "duration_ms": 156.2,
      "prompt_hash": "a1b2c3d4e5f6...",
      "prompt_preview": "[{\"role\":\"system\",\"content\":\"你是购物意图识别助手...\"},{\"role\":\"user\",\"content\":\"推荐适合油皮的洗面奶，200元以内\"}]",
      "response_preview": "{\"intent\":\"recommend\",\"category\":\"美妆护肤\",\"extracted_constraints\":{\"skin_type\":\"油皮\",\"budget_max\":200,\"product_type\":\"洗面奶\"}}",
      "parsed_json": {
        "intent": "recommend",
        "category": "美妆护肤",
        "extracted_constraints": {
          "skin_type": "油皮",
          "budget_max": 200,
          "product_type": "洗面奶"
        }
      },
      "validation_error": null,
      "fallback_from": null,
      "created_at": "2026-06-01T10:23:45.280Z"
    },
    {
      "task": "generate_criteria",
      "profile": "qwen_plus",
      "model": "qwen-plus",
      "provider": "Qwen",
      "status": "success",
      "duration_ms": 892.3,
      "prompt_hash": "f6e5d4c3b2a1...",
      "parsed_json": {
        "criteria_id": "crit_xyz789",
        "category": "美妆护肤",
        "constraints": {
          "skin_type": "油皮",
          "budget_max": 200,
          "product_type": "洗面奶",
          "brand_avoid": [],
          "ingredient_avoid": []
        },
        "chips": ["美妆护肤", "油皮", "≤200元", "洗面奶"]
      },
      "validation_error": null,
      "created_at": "2026-06-01T10:23:46.180Z"
    },
    {
      "task": "generate_recommendation",
      "profile": "qwen_plus",
      "model": "qwen-plus",
      "provider": "Qwen",
      "status": "success",
      "duration_ms": 1205.8,
      "response_preview": "这款洗面奶专为油性肌肤设计，含有水杨酸成分...",
      "created_at": "2026-06-01T10:23:47.400Z"
    }
  ],
  "sse_events": [
    {
      "event_type": "thinking",
      "seq": 1,
      "stage": "intent",
      "created_at": "2026-06-01T10:23:45.130Z"
    },
    {
      "event_type": "criteria_card",
      "seq": 2,
      "criteria_id": "crit_xyz789",
      "created_at": "2026-06-01T10:23:46.200Z"
    },
    {
      "event_type": "text_delta",
      "seq": 3,
      "message_id": "msg_001",
      "delta_preview": "这款洗面奶专为",
      "delta_hash": "9f8e7d6c5b4a...",
      "created_at": "2026-06-01T10:23:47.410Z"
    },
    {
      "event_type": "product_card",
      "seq": 4,
      "deck_id": "deck_001",
      "product_ids": ["p_skincare_001"],
      "created_at": "2026-06-01T10:23:47.500Z"
    },
    {
      "event_type": "done",
      "seq": 5,
      "finish_reason": "completed",
      "created_at": "2026-06-01T10:23:47.970Z"
    }
  ],
  "context_diagnostics": [
    {
      "action": "chat.context_diagnostic",
      "metadata": {
        "diagnostic_code": "BUDGET_PATCH_LOST",
        "severity": "warning",
        "user_budget_max": 200,
        "before_budget_max": 300,
        "after_budget_max": 300
      }
    }
  ]
}
```

## 调试流程

### 场景 1：排查幻觉（LLM 输出了错误信息）

**症状**：Android 显示的商品价格/描述与数据库不符。

**调试步骤**：

```bash
# 1. 获取 turn_id（从 Android 日志或 SSE done 事件）
TURN_ID="turn_abc123"

# 2. 查看 LLM 调用链
curl "http://localhost:8000/admin/observability/turns/$TURN_ID" | jq '.llm_calls[] | {task, status, duration_ms, parsed_json}'

# 3. 定位问题阶段
#    - analyze_intent 错？→ 意图识别有问题
#    - generate_criteria 错？→ 标准生成有问题
#    - generate_recommendation 错？→ 推荐文案有问题

# 4. 查看完整 prompt（需要 OBSERVABILITY_CAPTURE_FULL_PAYLOAD=1）
curl "http://localhost:8000/admin/observability/turns/$TURN_ID" | jq '.llm_calls[] | select(.task=="generate_recommendation") | .prompt_json'
```

**判断**：
- `parsed_json` 已错 → 问题在 prompt 或模型输出
- `parsed_json` 正确但 Android 显示错 → 问题在 Android 渲染

### 场景 2：多轮上下文丢失（Demo 3 核心场景）

**症状**：用户说"预算降到 200"，但推荐结果仍按 300 元筛选。

**调试步骤**：

```bash
# 1. 查看 context_diagnostics
curl "http://localhost:8000/admin/observability/turns/$TURN_ID" | jq '.context_diagnostics'

# 2. 如果有 BUDGET_PATCH_LOST
#    输出示例：
#    {
#      "diagnostic_code": "BUDGET_PATCH_LOST",
#      "user_budget_max": 200,
#      "before_budget_max": 300,
#      "after_budget_max": 300
#    }
#    → 说明 criteria 合并逻辑没应用预算变更

# 3. 查看 criteria LLM 调用
curl "http://localhost:8000/admin/observability/turns/$TURN_ID" | jq '.llm_calls[] | select(.task=="generate_criteria") | .parsed_json'

# 4. 对比前后两轮 criteria
#    Turn 1: budget_max=300
#    Turn 2: budget_max=300（应该变成 200）
```

### 场景 3：前后端 SSE 不一致

**症状**：Android 没收到某个商品卡片，但后端日志显示已发送。

**调试步骤**：

```bash
# 1. 查看服务端实际发出的 SSE 事件
curl "http://localhost:8000/admin/observability/turns/$TURN_ID" | jq '.sse_events[] | {event_type, seq, product_ids}'

# 2. 对比 Android 日志
#    - 服务端有 product_card (seq=4)，Android 没收到
#    → 问题在 SSE 传输或 Android 解析

# 3. 查看 product_card 详情
curl "http://localhost:8000/admin/observability/turns/$TURN_ID" | jq '.sse_events[] | select(.event_type=="product_card")'

# 4. 检查 delta_hash（文本聚合场景）
#    如果 hash 对不上，说明文本在传输中被篡改
```

### 场景 4：LLM fallback 链路追踪

**症状**：响应慢，怀疑走了 fallback。

**调试步骤**：

```bash
# 1. 查看所有 LLM 调用（包括失败的）
curl "http://localhost:8000/admin/observability/turns/$TURN_ID" | jq '.llm_calls[] | {task, profile, status, fallback_from, error_type, duration_ms}'

# 2. 典型 fallback 输出
#    {
#      "task": "generate_criteria",
#      "profile": "doubao_seed_lite",
#      "status": "failed",
#      "fallback_from": null,
#      "error_type": "LiveLLMUnavailable",
#      "duration_ms": 5000.2
#    },
#    {
#      "task": "generate_criteria",
#      "profile": "qwen_plus",
#      "status": "success",
#      "fallback_from": "doubao_seed_lite",
#      "error_type": null,
#      "duration_ms": 892.3
#    }
#    → 说明 Doubao 超时，fallback 到 Qwen

# 3. 查看 fallback 统计
curl "http://localhost:8000/admin/observability/fallbacks" | jq '.[] | {task, profile, reason}'
```

## 配置选项

### 环境变量

```bash
# 启用/禁用观测（默认启用）
export OBSERVABILITY_LOCAL_ENABLED=1

# 保存完整 payload（默认关闭，只存 preview + hash）
export OBSERVABILITY_CAPTURE_FULL_PAYLOAD=0

# preview 截断字符数（默认 4000）
export OBSERVABILITY_PREVIEW_CHARS=4000
```

### 生产环境建议

```bash
# 生产环境：关闭 full payload，避免存储爆炸
export OBSERVABILITY_CAPTURE_FULL_PAYLOAD=0

# 调试环境：开启 full payload，方便排查
export OBSERVABILITY_CAPTURE_FULL_PAYLOAD=1
```

## 性能影响

### 首 token 延迟

观测写入采用 fire-and-forget，**不阻塞主链路**。

验证：

```bash
# 1. 开启观测
export OBSERVABILITY_LOCAL_ENABLED=1

# 2. 发送请求，记录 stage_timings_ms
curl "http://localhost:8000/admin/observability/turns/$TURN_ID" | jq '.audit_events[] | select(.action=="chat.turn_completed") | .metadata.stage_timings_ms'

# 3. 关闭观测
export OBSERVABILITY_LOCAL_ENABLED=0

# 4. 再次发送请求，对比 timings
#    差异应 < 50ms（网络抖动范围）
```

### 存储估算

按每次 chat 请求：
- 3-5 条 LLM 调用记录（intent/criteria/recommendation/decision）
- 10-20 条 SSE 事件记录（thinking/text_delta/product_card/done）
- 每条记录 ~1KB（preview 模式）或 ~10KB（full payload 模式）

**1000 次请求**：
- preview 模式：~30MB
- full payload 模式：~300MB

## 常见问题

### Q: 为什么 `llm_calls` 为空？

**A**: 检查以下几点：

```bash
# 1. 观测是否启用
echo $OBSERVABILITY_LOCAL_ENABLED

# 2. 数据库是否可写
uv run python -c "from src.repos.database import get_async_engine; print(get_async_engine())"

# 3. 查看后端日志
tail -f backend.log | grep "Observability task"
```

### Q: `parsed_json` 为 null？

**A**: `parsed_json` 由 `llm_client.py` 异步补充，可能有短暂延迟。等待 1-2 秒后重新查询。

### Q: 如何清空观测数据？

```bash
# PostgreSQL
psql -d buypilot -c "TRUNCATE observability_llm_calls, observability_sse_events;"

# SQLite
sqlite3 backend/buypilot.db "DELETE FROM observability_llm_calls; DELETE FROM observability_sse_events;"
```

### Q: 如何导出 Debug Bundle？

```bash
# 保存为 JSON
curl "http://localhost:8000/admin/observability/turns/$TURN_ID" > debug_bundle.json

# 美化输出
jq . debug_bundle.json > debug_bundle_pretty.json
```

## 下一步

- 设计文档：`doc/observability/chat-stream-observability-design.md`
- 测试用例：`backend/tests/test_observability_*.py`
- 代码实现：`backend/src/services/observability_llm.py`
