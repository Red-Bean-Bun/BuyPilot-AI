# Chat Stream Observability Design

本文档定义 BuyPilot-AI 后端可观测性方案，重点解决 `/chat/stream` LLM 输出不可见、SSE 事件无记录、上下文不一致难定位的问题。

## 结论

采用**本地 DB 记录 + 扩展现有 Debug Bundle** 作为可观测性方案。不引入 OpenTelemetry、Langfuse 或任何外部观测平台。

理由：

- 现有系统（`ApiRequestLog` + `AuditEvent` + `RetrievalTrace` + `EvidenceLink` + `fallbacks` ContextVar + `stage_timings_ms`）已覆盖 HTTP 请求日志、业务审计、检索链路、阶段耗时和降级事件。
- 真正缺失的只有两件事：**LLM 调用明细**（幻觉排查）和 **SSE 事件记录**（前后端一致性验证）。
- 新增 2 张表 + 扩展 debug bundle 聚合即可补齐盲区，零新依赖、零外部服务 。

OTel + Langfuse 作为生产化路线保留在未来计划中（见§未来路线）。

## 硬约束

可观测性不能影响现有后端服务正常运行：

- 本地观测写库失败时，只打 warning，不影响业务响应。
- 观测写入采用 background task best-effort，不阻塞首 token。
- LLM trace 绝不允许增加首 token 延迟。
- SSE 事件顺序、内容、节奏不能因观测逻辑改变。
- 数据库改动只允许 additive schema，不改现有表语义。
- Debug Bundle 可关闭：`OBSERVABILITY_LOCAL_ENABLED=0`。
- 默认脱敏；`OBSERVABILITY_CAPTURE_FULL_PAYLOAD=1` 才保存完整 payload。

## 架构

```text
/chat/stream
  → 现有链路不变（intent → criteria → retrieval → recommendation → decision）
  → LLM gateway/provider 边界 → best-effort 写 observability_llm_calls
  → API SSE 唯一出口 → best-effort 写 observability_sse_events
  → criteria 统一合并出口 → best-effort 运行 2 条诊断规则

/admin/observability/turns/{turn_id}
  → 现有 requests + audit_events
  → 新增 llm_calls + sse_events + context_diagnostics
```

不引入新中间件、新 instrumentation 框架、新外部依赖。

## 本地观测表

表名使用 `observability_` 前缀，避免和现有业务表混淆。

### `observability_llm_calls`

记录所有 task-oriented LLM 调用。记录点分两层：

- `llm_gateway.py` 记录 provider 调用边界：实际 profile、model、provider、fallback、耗时、供应商错误、原始响应 preview/hash。这里最接近真实网络调用，能准确知道最终用了哪个模型。
- `llm_client.py` 补充 task 语义结果：parsed JSON、schema validation error、业务归一化后的结果摘要。这里最接近业务解析边界，适合定位“模型返回了 A，但系统解析成 B”的问题。

流式推荐必须单独处理：`generate_recommendation_stream` 是 async generator，不是普通函数返回。应在 stream wrapper 中累计 delta preview/hash，记录首 delta 是否产生、最终聚合文本 preview/hash、流中断错误和 fallback。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | UUID PK | 主键 |
| `turn_id` | str | 关联 turn |
| `session_id` | str | 关联会话 |
| `task` | str | `analyze_intent` / `generate_criteria` / `generate_recommendation` / `generate_decision` / `analyze_image` |
| `profile` | str | 当前 LLM profile 名 |
| `model` | str | 实际使用的模型 |
| `provider` | str | Doubao / Qwen |
| `status` | str | `success` / `failed` / `fallback` |
| `duration_ms` | float | 耗时 |
| `prompt_hash` | str | prompt messages SHA-256 |
| `prompt_preview` | str \| None | 默认截断 preview（`OBSERVABILITY_PREVIEW_CHARS`） |
| `prompt_json` | str \| None | full payload 模式时保存完整 prompt |
| `response_preview` | str \| None | 默认截断 preview |
| `response_json` | str \| None | full payload 模式时保存完整 response |
| `parsed_json` | JSON \| None | JSON schema 解析后的结构摘要 |
| `validation_error` | str \| None | 解析/校验错误信息 |
| `token_usage` | JSON \| None | `{"prompt": N, "completion": N, "total": N}`，provider 支持时填充 |
| `fallback_from` | str \| None | fallback 来源 profile |
| `error_type` | str \| None | 错误类型 |
| `error_message` | str \| None | 错误摘要 |
| `error_raw` | str \| None | full payload 模式时保存供应商错误原文 |
| `created_at` | datetime | 创建时间 |

写入方式：统一使用 `safe_observability_task(coro, session_id, turn_id, label)` fire-and-forget。禁止在业务代码里裸写 `asyncio.create_task(...)`。`safe_observability_task` 内部 catch 所有异常并只 `logger.warning`，避免后台写库失败影响主链路；调用时显式传入 `session_id/turn_id`，不要依赖后台任务里的 ContextVar 一定仍然正确。

### `observability_sse_events`

记录服务端实际发出的 SSE 事件摘要。记录点放在 `api/chat.py` 的 SSE 唯一出口，而不是分散写在 `handlers.py`：

```python
async for event in chat_stream(sid, body):
    record_sse_event(event)
    yield format_sse(event)
```

理由：SSE 事件来源包括 `pipeline.py`、`handlers.py`、`ctx.done()` 和异常分支。API 出口能覆盖所有实际发给客户端的事件，最不容易漏，也最不侵入业务逻辑。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | UUID PK | 主键 |
| `turn_id` | str | 关联 turn |
| `session_id` | str | 关联会话 |
| `event_type` | str | `thinking` / `text_delta` / `product_card` / `criteria_card` / `final_decision` / `clarification` / `cart_action` / `done` / `error` |
| `seq` | int | 服务端 seq |
| `node_id` | str \| None | UI node 标识 |
| `deck_id` | str \| None | 商品 deck 标识 |
| `criteria_id` | str \| None | criteria 标识 |
| `product_ids` | JSON \| None | 当前事件关联的商品 ID 列表 |
| `message_id` | str \| None | 文本消息标识（用于 text_delta 聚合） |
| `delta_preview` | str \| None | text delta 截断 preview |
| `delta_hash` | str \| None | text delta SHA-256 |
| `finish_reason` | str \| None | done 事件的 finish reason |
| `created_at` | datetime | 创建时间 |

写入方式同上。`text_delta` 事件逐条记录，不做运行时聚合——聚合在查询侧按需处理。

## 上下文诊断

不新建独立服务。在 `stages/criteria.py` 中增加一个统一 helper，例如 `diagnose_criteria_context(existing, final, body, intent)`。LLM criteria 路径和 `criteria_patch` 提前返回路径都必须经过这个 helper，避免 quick action / patch 场景漏诊断。诊断结果写入 `audit_events` 的 metadata。

只覆盖 Demo 3 的核心场景：

### 规则 1：预算变更丢失（BUDGET_PATCH_LOST）

用户输入包含预算调整意图（"预算降到 200""200 以内"），但合并后 `criteria.constraints.budget_max` 未变化或变大。

```json
{
  "diagnostic_code": "BUDGET_PATCH_LOST",
  "severity": "warning",
  "user_hint": "预算降到200",
  "before_budget_max": 300,
  "after_budget_max": 300
}
```

### 规则 2：排除条件丢失（EXCLUSION_LOST）

用户输入包含排除意图（"不含酒精""不要某品牌"），但最终 criteria 未包含对应的 `ingredient_avoid` / `brand_avoid` / `origin_avoid`。

```json
{
  "diagnostic_code": "EXCLUSION_LOST",
  "severity": "warning",
  "user_hint": "不要含酒精的",
  "missing_field": "ingredient_avoid",
  "expected_contains": "酒精"
}
```

诊断结果作为 `chat.context_diagnostic` action 写入 `audit_events`，metadata 字段承载上述结构。debug bundle 查询时从 audit events 中筛选即可。

## Debug Bundle 扩展

扩展现有 `get_turn_debug_bundle`，新增 3 个字段：

```json
{
  "turn_id": "turn_x",
  "requests": [],
  "audit_events": [],
  "llm_calls": [],
  "sse_events": [],
  "context_diagnostics": []
}
```

- `llm_calls`：从 `observability_llm_calls` 按 `turn_id` 查询，按 `created_at` 排序。
- `sse_events`：从 `observability_sse_events` 按 `turn_id` 查询，按 `seq` 排序。
- `context_diagnostics`：从 `audit_events` 中筛选 `action == "chat.context_diagnostic"` 的事件。

不新增 API 端点。现有 `/admin/observability/turns/{turn_id}` 直接返回扩展后的 bundle。

## 配置

新增环境变量（集中到 `config/settings.py`）：

```env
OBSERVABILITY_LOCAL_ENABLED=1
OBSERVABILITY_CAPTURE_FULL_PAYLOAD=0
OBSERVABILITY_PREVIEW_CHARS=4000
```

默认语义：

- 观测代码路径默认启用。
- full payload 默认关闭，只存 preview + hash。
- headers 永不保存，避免 API key 泄漏。
- `OBSERVABILITY_LOCAL_ENABLED=0` 时跳过所有观测写库。

## 开发人员如何使用

### 单 turn 排查流程

1. Android 或调用方每次请求 `/chat/stream` 时传入稳定的 `session_id` 和 `client_turn_id`。如果没有传，后端会生成 turn id，但开发排查会更麻烦。
2. 复现问题后，从响应 header 或 SSE `done/error` 事件里拿到 `turn_id` / `trace_id`。
3. 调用：

```bash
curl "http://localhost:8000/admin/observability/turns/<turn_id>"
```

4. 按以下顺序看 Debug Bundle：

```text
requests
  → 请求是否进入正确 session/turn
audit_events
  → chat.turn_started / completed / failed / persisted 是否完整
llm_calls
  → intent/criteria/recommendation_stream 的 prompt、response、parsed_json 是否符合预期
sse_events
  → 服务端实际发出的事件顺序、product_ids、criteria_id、finish_reason
context_diagnostics
  → 是否命中预算丢失、排除条件丢失
```

排查判断：

- `llm_calls` 已错：问题在 prompt、模型输出、provider fallback 或 JSON 解析。
- `llm_calls` 正确但 `sse_events` 错：问题在 runtime 编排或 event payload 构造。
- `sse_events` 正确但 Android UI 错：问题大概率在客户端状态合并或渲染。
- `context_diagnostics` 命中：优先看 `criteria_patch`、历史 criteria 合并、`field_sources`。

### Demo 3 推荐检查

多轮上下文问题重点看这条链：

```text
turn 1: "不要含酒精的防晒霜"
  → generate_criteria.parsed_json.constraints.ingredient_avoid contains "酒精"
  → sse_events.criteria_card.criteria_id

turn 2: "预算降到200"
  → previous criteria 被读取
  → final criteria.constraints.budget_max == 200
  → ingredient_avoid 仍 contains "酒精"
  → context_diagnostics 为空
```

如果第二轮预算正确但排除条件丢失，优先查 criteria 合并逻辑；如果 criteria 正确但 product card 不符合，优先查 retrieval hard filters。

## 公网联调建议

可以用 Cloudflare Tunnel 做远程开发/演示联调，但不要把 admin/debug 能力裸露到公网。

推荐方式：

1. 使用 **named tunnel** 绑定正式子域名，例如 `api-dev.example.com` → `http://localhost:8000`。
2. 使用 Cloudflare Access 保护整个开发 API，至少保护 `/admin/*`。
3. 不使用 Quick Tunnel 跑 `/chat/stream`。Cloudflare 官方说明 Quick Tunnel 是测试用途，而且不支持 SSE；本项目核心接口依赖 SSE。
4. 公网联调环境保持 `OBSERVABILITY_CAPTURE_FULL_PAYLOAD=0`，除非短时间定位问题并确认访问受控。
5. 如果只是 Android 真机和本机后端联调，优先使用局域网 IP 或 adb reverse；只有跨网络协作、演示或远程复现时再开 tunnel。

最低安全要求：

```text
Cloudflare Access allowlist: 仅团队成员邮箱
OBSERVABILITY_CAPTURE_FULL_PAYLOAD=0
/admin/observability/* 不允许匿名访问
后端 DATABASE_URL 指向开发库，不使用生产数据
隧道 token 不提交到仓库
```

不建议：

- 用 trycloudflare Quick Tunnel 调试 SSE。
- 把 `/admin/observability/turns/{turn_id}` 直接公开给所有人。
- 在公网环境打开 full payload 后长时间运行。

## 实施计划

### Phase 1：LLM 调用记录（最高价值）

目标：每次 `/chat/stream` 后能在 debug bundle 中看到 LLM 实际输入输出。

改动文件：

- `backend/src/repos/models.py` — 新增 `ObservabilityLLMCall` 表
- `backend/src/repos/observability.py`（新建）— `insert_llm_call` / `list_llm_calls_by_turn`
- `backend/src/services/observability.py` — 新增 `safe_observability_task`、`record_llm_call`（fire-and-forget）+ 扩展 `get_turn_debug_bundle`
- `backend/src/services/llm_gateway.py` — 在 provider 调用边界记录 profile/model/fallback/duration/error/raw response preview
- `backend/src/services/llm_client.py` — 补充 parsed_json / validation_error / 业务归一化结果摘要
- `backend/src/config/settings.py` — 新增 3 个配置项

验收：

- 跑 Demo 1 路径，`/admin/observability/turns/{turn_id}` 返回 `llm_calls` 包含 `analyze_intent` + `generate_criteria` + `generate_recommendation_stream` 记录。
- 流式推荐记录能看到首 delta 状态、聚合文本 preview/hash、流中断错误和 fallback。
- 写库失败不影响 chat 流。
- `OBSERVABILITY_LOCAL_ENABLED=0` 时不写库。
- `uv run pytest -q` 全量通过。

### Phase 2：SSE 事件记录 + 上下文诊断

目标：能在 debug bundle 中看到服务端实际发出的 SSE 事件序列，以及 criteria 合并诊断。

改动文件：

- `backend/src/repos/models.py` — 新增 `ObservabilitySSEEvent` 表
- `backend/src/repos/observability.py` — 新增 `insert_sse_event` / `list_sse_events_by_turn`
- `backend/src/services/observability.py` — 新增 `record_sse_event` + 扩展 `get_turn_debug_bundle`
- `backend/src/api/chat.py` — 在 `chat_stream` 事件格式化前的唯一出口调用 `record_sse_event`
- `backend/src/runtime/stages/criteria.py` — 增加 `diagnose_criteria_context(existing, final, body, intent)`，LLM criteria 和 criteria_patch 两条路径都调用

验收：

- 跑 Demo 1 路径，debug bundle 的 `sse_events` 能看到 `thinking` → `text_delta` → `product_card` → `criteria_card` → `done` 序列。
- 跑 Demo 3 路径（"不要含酒精的防晒霜" + "预算降到200"），`context_diagnostics` 为空（约束正确传递）。
- 故意构造预算丢失场景，`context_diagnostics` 出现 `BUDGET_PATCH_LOST`。
- SSE 事件序列和未接入观测前一致（golden trace 不变）。
- 首 token 延迟不因观测写入增加。

### 不影响现有服务的验收清单

- 观测关闭时，`/chat/stream` SSE golden 测试完全不变。
- 本地观测表写入失败时，chat 流不报错。
- 首 token 延迟不因观测 await 写入增加。
- SSE `seq/event_id/node_id/deck_id/finish_reason` 不变。
- 现有 `tests/test_observability.py`、`tests/test_chat_api.py`、`tests/test_pipeline.py` 保持通过。
- 新增测试覆盖：LLM call 记录写入、SSE event 记录写入、write failure 静默降级、full-payload 开关、debug bundle 聚合。

## 未来路线（赛后生产化）

以下能力在当前比赛阶段不实施，作为项目继续发展时的参考路线：

1. **OpenTelemetry + Langfuse**：一个 turn 一个 trace，stage 为 span，LLM 调用为 generation。通过 OTLP 上报，Langfuse 自托管部署在 `deploy/langfuse/docker-compose.yml`。后端只依赖 OTLP endpoint，不强耦合 Langfuse 部署。
2. **Stage 级日志表**（`observability_stage_logs`）：现有 `stage_timings_ms` 在 audit event metadata 中已够用，stage 级独立表在需要更细粒度查询时再引入。
3. **Context Snapshots 表**（`observability_context_snapshots`）：9 个 snapshot 点完整记录多轮上下文演变。当前用 2 条诊断规则 + audit events 替代。
4. **Android 渲染回传**（`POST /observability/client-render`）：需要客户端改动配合，在需要验证前后端渲染一致性时引入。
5. **JSON/Markdown 导出端点**：当前 debug bundle JSON 响应足够排查，导出功能是锦上添花。
6. **聚合指标 + Prometheus**：`/admin/observability/metrics` 和 Prometheus `/metrics` 端点，需要有消费方（dashboard/告警）时再引入。
7. **更多诊断规则**：品类漂移（CATEGORY_DRIFT）、候选重复使用（DECK_REUSE）、渲染不一致（RENDER_MISMATCH）。

## 参考

- OpenTelemetry FastAPI instrumentation: https://opentelemetry-python-contrib.readthedocs.io/en/latest/instrumentation/fastapi/fastapi.html
- Langfuse OpenTelemetry integration: https://langfuse.com/integrations/native/opentelemetry
- Cloudflare Tunnel overview: https://developers.cloudflare.com/tunnel/
- Cloudflare Tunnel routing: https://developers.cloudflare.com/tunnel/routing/
- Cloudflare Quick Tunnels: https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/do-more-with-tunnels/trycloudflare/
- Cloudflare Access application paths: https://developers.cloudflare.com/cloudflare-one/policies/access/app-paths/
