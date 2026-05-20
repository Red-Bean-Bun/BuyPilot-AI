# BuyPilot-AI Android前端与Agent-to-UI PRD

**Executive Summary**

本 PRD 面向 **3 周赛制**下的 **亲子玩具 Toys & Games 导购 Agent** Android 客户端实现，目标是把当前已确认的整体策略口径与后端 & Agent PRD 口径，落成一份**可以直接交付给 Android 开发者开工**的前端执行文档。文档默认采用当前对话中已经确认的后端事件集与字段口径：`thinking`、`clarification`、`criteria_card`、`text_delta`、`product_card`、`final_decision`、`done/error`，并围绕“**模糊需求 → 澄清 → 购买标准 → 商品推荐 → 决策结论 → 反馈修正**”构建流式 UI 主链路。

文档中的状态说明如下：**已确认**表示当前对话中两份 PDF 已明确的方向或字段口径；**未指定**表示原材料未给出；**本 PRD 决策**表示为保证 Android 工程可实施而新增的前端约定。对外部技术栈、SDK 能力、版本与平台兼容信息，优先采用官方或原始资料。Jetpack Compose 官方建议以 BOM 统一管理 Compose 依赖版本；Photo Picker 支持 Compose 启动器并在不支持设备上回退到 `ACTION_OPEN_DOCUMENT`；CameraX 是 Google 推荐新应用优先使用的相机库；ML Kit 文本识别、Coil Compose 与 OkHttp SSE 均有官方或原始资料可引用。

## 产品定位与范围

### 一句话定位

**BuyPilot-AI Android 端是一款面向家长与送礼用户的原生流式导购界面，用 Agent 化的多轮问答与证据化卡片，帮助用户从模糊需求中快速形成玩具购买决策。**

### 目标用户与核心体验

本项目的**目标用户**是为儿童购买玩具的家长、亲友送礼用户，以及对年龄适配、安全约束、教育价值和预算平衡有明确需求的人群。当前赛题的核心不是“搜索更多商品”，而是把用户的模糊表达还原为可执行的购买标准，再用结构化推荐和证据解释完成决策闭环。这一定位与 Android 架构上强调的“稳健、可测试、可维护”的分层实现是相容的，也适合用 `ViewModel + StateFlow` 承载可持续更新的 UI 状态。

### 设计原则

<sheet sheet-id="pjFAv8" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 不可做功能清单

为了匹配三周比赛节奏，前端必须严格收缩范围。**P0 不做**：商城首页、类目浏览页、商品详情长页、购物车、支付、订单系统、消息通知中心、账号体系深集成、客服系统、多端同登同步。**P1 也不建议做**：复杂筛选页、长列表瀑布流、活动会场、优惠券、多图多视频理解、完整埋点后台。上述能力在原始材料中**未指定**，本 PRD 决策为**全部降级为非目标项**。

## 技术栈与工程结构

Android 端建议统一采用 **Kotlin + Jetpack Compose**，并通过官方 **Compose BOM** 统一 Compose 依赖版本；官方文档示例给出了 `androidx.compose:compose-bom:2026.04.01`。图片选择优先用系统 Photo Picker；官方明确其在 Compose 中可直接通过 `rememberLauncherForActivityResult(PickVisualMedia())` 启动，在不支持设备上会回退至 `ACTION_OPEN_DOCUMENT`，并可覆盖到 Android 4.4+ 的设备层。相机拍摄如果进入 P1/P2，则优先用 CameraX，Google 官方建议新应用从 CameraX 起步，且其向后兼容到 Android 5.0。图片渲染建议用 Coil Compose；官方当前文档给出 `io.coil-kt.coil3:coil-compose:3.4.0`，并推荐大多数场景优先使用 `AsyncImage`。SSE 通道建议用 OkHttp SSE；Square 官方模块说明其为实验性支持，当前 README 示例依赖为 `com.squareup.okhttp3:okhttp-sse:5.3.0`。如果需要端上 OCR 兜底，可选 ML Kit Text Recognition；Google 官方当前 Android 文档给出拉丁文与中文识别依赖 `16.0.1`，Play services 版本也有对应依赖。UI 状态管理建议用 `ViewModel + StateFlow`，并在 UI 层使用 `repeatOnLifecycle` 收集，避免不可见时仍处理事件。

<sheet sheet-id="lDp1Lm" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 推荐模块划分

<sheet sheet-id="ui4Yvl" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 版本锁定原则

比赛周期只有三周，**所有依赖必须固定版本**，不得在赛中追新；Compose 统一通过 BOM 锁定，OkHttp SSE、Coil、ML Kit 必须在第 3 天前冻结版本。Compose BOM 的官方设计目标正是统一 Compose 族依赖版本并减少冲突，这与比赛环境下的快速集成高度匹配。

## 页面与导购聊天页规范

### 页面范围与职责

<sheet sheet-id="XAY9gg" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 导购聊天页布局分区

导购聊天页采用**单栏主流布局**，从上到下分为五个区域：

1. **顶部状态区**  
固定在顶部 AppBar 下方，展示当前阶段状态，例如“正在理解需求”“正在生成购买标准”“正在检索商品”“正在整理结论”。状态文本由 `thinking` 或内部阶段映射驱动，不单独占用聊天流中的气泡位置。建议使用一行文本 + 小型 loading 指示器，状态切换做 150–250ms 去抖，避免多阶段快速跳变时闪烁。
2. **消息列表与卡片流**  
使用单一 `LazyColumn` 承载，**文本消息与结构化卡片必须进入同一条时间线**。不能把产品卡单独放在聊天流外，否则会破坏用户对“这是 Agent 逐步思考结果”的感知。  
列表元素建议统一抽象为 `ChatUiNode`：

   - 用户文本消息
   - 用户图片消息
   - AI 文本流消息
   - `clarification` 澄清卡
   - `criteria_card`
   - `product_card`
   - `final_decision`
   - 系统错误气泡
3. **输入区**  
底部固定。包含：

   - 文本输入框，支持 1–4 行扩展
   - 图片按钮，优先调用 Photo Picker；支持单图预览
   - 发送按钮
   - 停止生成按钮，仅在流式进行中显示
   - 重试按钮，仅在错误态显示
4. **反馈区**  
仅在出现 `product_card` 或 `final_decision` 后显示，采用轻量行内按钮或卡片底部操作区，包含点赞、点踩、不喜欢、换一批、看证据、加入对比等动作。用户在 `criteria_card` 上的 quick action 也属于反馈闭环的一部分。
5. **浮层/底板**  
用于图片预览、证据详情、商品对比与错误详情。P0 只要求图片预览与证据底板；对比页可到 P1 实现。

### 交互流程

当用户发送文本或图片后，前端先做**本地回显**，随后创建或续用 `session_id`，发起 `/chat/stream`。一旦连接建立，顶部状态区立即进入“连接中 / 正在理解需求”。如果收到 `clarification`，输入框保持可编辑，用户可以直接回答，当前 stream 结束；如果收到 `criteria_card`，则在时间线上插入购买标准卡，并允许用户点按 quick action 直接回流 `criteria_patch`。`text_delta` 会持续增量更新同一条 AI 气泡；每到一个 `product_card`，就在对应文本之后插入一张完整商品卡；`final_decision` 到达时，在流末插入总结卡并关闭 loading。整个过程中，用户点击“停止生成”应立即关闭 SSE，并将当前状态改为 `Canceled`，保留已生成内容供继续追问。

### 输入区规范

系统 Photo Picker 的优势在于，它以系统级安全方式只向应用授予用户选中的图片/视频，而不是整个媒体库访问权限；官方同时给出了 Compose 场景下的启动方式。P0 阶段建议**只做单图上传**；多图、视频与实时拍摄均不进入主链路。对于不支持 Photo Picker 的设备，系统会回退到 `ACTION_OPEN_DOCUMENT`，因此前端不需要自己再维护第二套文件选择 UI。

输入区行为要求如下：

<sheet sheet-id="U0OTLF" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 消息列表与卡片流渲染规则

渲染层必须满足三条硬规则。第一，**仅有一个时间线**；第二，`text_delta` **永远更新同一条 AI 流式消息节点**，不得每个 delta 新增一条列表项；第三，**所有结构化卡片都使用稳定 key**，插入后尽量只做内容增量更新，避免重组造成抖动。

建议渲染策略如下：

<sheet sheet-id="NriNH0" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 三类核心卡片 UI 规范

<sheet sheet-id="dzyNci" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

`criteria_card` 的目标不是“展示系统内部理解”，而是让用户看见**当前购买标准已被结构化**，并可一键修正。`product_card` 的目标不是穷举参数，而是把用户最关心的适龄、安全、预算和教育价值前置。`final_decision` 则必须给出**可执行结论**，而不是只重复前文。

推荐 quick action 最小集合如下：

<sheet sheet-id="uEasQp" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### UI示意图

仅仅草稿，不代表最终效果



<grid>
<column width-ratio="0.502754">
![](https://internal-api-drive-stream.feishu.cn/space/api/box/stream/download/authcode/?code=YjI2MzVlYTQ3YzZhNDlmYzkxZTgwMGZjYTUxMzNhMjNfMWRkMzczYzUyMDNkZGUwMTcyMjcwNjlhNzBjMzdhYTlfSUQ6NzYzOTc0Njg5ODAyMjUzNDA5Ml8xNzc5MTI0MzQ5OjE3NzkxMjc5NDlfVjM)
</column>
<column width-ratio="0.497246">
![](https://internal-api-drive-stream.feishu.cn/space/api/box/stream/download/authcode/?code=ZTYwYmI5MTg0NGViYjk2ZDMxODBmYTJjOWQwZGVmZmRfZjI2OTJjMDRkZDUwNDc3YzZlNjc1NjRjMWI0OTAyMjhfSUQ6NzYzOTc0NzEwMDEzMzAzNDk0Nl8xNzc5MTI0MzQ5OjE3NzkxMjc5NDlfVjM)
</column>
</grid>

                                **01 导购聊天首页                                                                                          02 Thinking 状态**



<grid>
<column width-ratio="0.512394">
![](https://internal-api-drive-stream.feishu.cn/space/api/box/stream/download/authcode/?code=OTUwMTFhZjc2NGFmMmIyOTdmMDQ1MTI0OGUxZjJkZmJfYTY2ODEzY2E0Y2FkODIxNmRkMmNkN2NiMjIwMGM2YjlfSUQ6NzYzOTc0NzQ3NTI1MDcxMTc3MF8xNzc5MTI0MzQ5OjE3NzkxMjc5NDlfVjM)
</column>
<column width-ratio="0.487606">
![](https://internal-api-drive-stream.feishu.cn/space/api/box/stream/download/authcode/?code=YWM5ZjkzMjg2ZmNmYmRkNGYzM2IzN2JjOTVhZDQzZTVfMWVhOWE2N2I3NTUwMzBkM2FjNDVlZmIwMjQ4NjAyNTBfSUQ6NzYzOTc0NzczMzk4MDU4MDgwOV8xNzc5MTI0MzQ5OjE3NzkxMjc5NDlfVjM)
</column>
</grid>

                         **03 Clarification 澄清卡                                                                                         04 criteria_card**



<grid>
<column width-ratio="0.501588">
![](https://internal-api-drive-stream.feishu.cn/space/api/box/stream/download/authcode/?code=Mjg4NThiM2YzODg4ZDM2MTNlZWE0YjYzNjAyMmQ3NDNfN2VkMGVmZDY5NDk2MTdmYTNjZDgxZDEwNDQ0ZjdlNDNfSUQ6NzYzOTc0ODIxODY3ODU3ODQwMF8xNzc5MTI0MzQ5OjE3NzkxMjc5NDlfVjM)
</column>
<column width-ratio="0.498412">
![](https://internal-api-drive-stream.feishu.cn/space/api/box/stream/download/authcode/?code=OGM0MzY2MTA0OGRhMGYwYmVkY2NhNzRmNDgwNDI1YWNfM2M3ODhjOGUxNDY5OGE4YTJjNTk4OGQ2NjM1NzI3ODRfSUQ6NzYzOTc0ODM2MTc4MTM5ODQ1Nl8xNzc5MTI0MzQ5OjE3NzkxMjc5NDlfVjM)
</column>
</grid>

                                             **05商品推荐卡                                                                         06商品推荐卡--详情**

##  SSE 协议与接口契约

### SSE 流式交互图

![](https://internal-api-drive-stream.feishu.cn/space/api/box/stream/download/authcode/?code=OTgzODQyNmNjMDZiNjI4M2M2NzVkYzY5NDBiNDg1ZGVfNmZkNmU3M2I3NTc3ZTMyN2Y0MjE1NDI3MTZiYjFjNzhfSUQ6NzYzOTc0NDkzMjc5ODQwMTcyOV8xNzc5MTI0MzQ5OjE3NzkxMjc5NDlfVjM)



### SSE 事件到 UI 组件映射表

<sheet sheet-id="Ik1UZM" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 完整流式示例

下面给出一组**从用户请求到完整 SSE 序列**的工程样例。为便于 Android 侧直接联调，示例包含原始请求体、SSE 帧内容与事件顺序。每个事件携带 `seq`（正整数递增）和 `session_id`（必填）；首次请求 `session_id` 为 `null` 时后端生成 UUID 并在第一个事件中返回。

**请求**

json

复制

```JSON
{"message": "给4岁孩子买一个室内玩的益智玩具，预算200元以内，不要小零件，也尽量不要电池","session_id": null,"history": [],"image_url": null}
```

**响应流**

text

复制

```Plain Text
event: thinking
data: {"seq":1,"event":"thinking","session_id":"sess_demo_001","stage":"understanding","message":"正在理解年龄、场景、预算与安全约束"}

event: criteria_card
data: {"seq":2,"event":"criteria_card","session_id":"sess_demo_001","criteria_id":"crit_001","editable":true,"criteria":{"age":4,"scenario":"indoor","budget_max":200,"requires_battery":false,"safety_features":["no_small_parts"],"education_dimensions":["logic","fine_motor"]},"risks":["4岁儿童使用磁力玩具需家长陪同收纳"],"quick_actions":[{"action_id":"budget_low","label":"预算压低","action":"criteria_patch","criteria_patch":{"budget_max":150}},{"action_id":"more_educational","label":"更偏益智","action":"criteria_patch","criteria_patch":{"education_dimensions":["logic","focus"]}}]}

event: text_delta
data: {"seq":3,"event":"text_delta","session_id":"sess_demo_001","message_id":"msg_ai_01","delta":"我先按 4 岁、室内、200 元以内、无小零件、尽量不要电池来筛选。","done":false}

event: text_delta
data: {"seq":4,"event":"text_delta","session_id":"sess_demo_001","message_id":"msg_ai_01","delta":"下面给你两种更适合在家安静玩的选择。","done":false}

event: product_card
data: {"seq":5,"event":"product_card","session_id":"sess_demo_001","rank":1,"product":{"product_id":"toy_1001","name":"大颗粒磁力积木","brand":"Magformers","price":169,"currency":"CNY","image_url":"https://example.com/1.jpg","age_min":4,"age_max":6,"toy_type":"building","education_dimensions":["logic","creativity"],"safety_features":["no_small_parts"],"play_scenario":["indoor"],"requires_battery":false,"messiness_level":"low"},"reason":"适合 4 岁儿童进行室内安静拼搭，兼顾逻辑与动手能力。","risk_notes":["含磁性部件，建议家长陪同收纳。"],"evidence":[{"source_type":"product_chunk","snippet":"适合 4-6 岁，大颗粒设计，减少误吞风险。","source_id":"chunk_001"}],"actions":[{"action_id":"show_evidence","label":"看证据","action":"open_evidence"},{"action_id":"dislike_product","label":"不喜欢这个","action":"feedback","feedback_type":"not_interested"}]}

event: product_card
data: {"seq":6,"event":"product_card","session_id":"sess_demo_001","rank":2,"product":{"product_id":"toy_1002","name":"木质拼图启蒙盒","brand":"Hape","price":129,"currency":"CNY","image_url":"https://example.com/2.jpg","age_min":4,"age_max":5,"toy_type":"puzzle","education_dimensions":["focus","fine_motor"],"safety_features":["rounded_edge"],"play_scenario":["indoor"],"requires_battery":false,"messiness_level":"low"},"reason":"如果你更在意安静与收纳方便，这个比磁力积木更省心。","risk_notes":[],"evidence":[{"source_type":"product_chunk","snippet":"木质圆角处理，适合家庭室内启蒙游戏。","source_id":"chunk_002"}],"actions":[{"action_id":"show_evidence","label":"看证据","action":"open_evidence"},{"action_id":"show_alternatives","label":"换相似的","action":"feedback","feedback_type":"show_alternatives"}]}

event: final_decision
data: {"seq":7,"event":"final_decision","session_id":"sess_demo_001","winner_product_id":"toy_1001","summary":"如果你更看重综合益智性和可玩时长，优先选大颗粒磁力积木；如果你更看重安静和收纳成本，木质拼图启蒙盒更省心。","why":["适龄匹配 4 岁","预算内","无电池","室内友好"],"not_for":["若你希望绝对避免磁性件，不建议 Top1"],"alternatives":[{"product_id":"toy_1002","name":"木质拼图启蒙盒"}],"next_actions":[{"action_id":"cheaper","label":"再便宜一点","action":"criteria_patch","criteria_patch":{"budget_max":150}},{"action_id":"compare","label":"加入对比","action":"compare"}]}

event: done
data: {"seq":8,"event":"done","session_id":"sess_demo_001","criteria_id":"crit_001","total_products":2}
```

### 接口契约

#### `POST /chat/stream`

- **用途**：发起或续接一次导购对话，响应为 `text/event-stream`
- **请求头**：`Content-Type: application/json`
- **返回头**：`Content-Type: text/event-stream`

<sheet sheet-id="hQzV2M" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

**超时策略**

- 连接超时：10 秒
- 写超时：15 秒
- 读超时：SSE 通道设置为无限或 0；客户端以“首事件软超时”管理 UX
- 首个 `thinking` 事件软超时：2 秒
- 首个结构化事件（`clarification` / `criteria_card` / `error`）软超时：8 秒  
如果超过软超时仍无事件，顶部状态区改为“响应较慢，仍在等待”，但**不应自动中断**。

#### `POST /upload/image`

<sheet sheet-id="CYGFih" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

**建议返回**

json

复制

```JSON
{"image_url": "https://example.com/upload/abc.jpg","width": 1280,"height": 960,"mime_type": "image/jpeg","ocr_text": null}
```

#### `POST /feedback`

<sheet sheet-id="odnsUX" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

#### `GET /sessions/{id}/history` — 恢复对话历史（新增 P0）

- **用途**：App 重启/清数据后恢复历史对话
- **请求**：`GET /sessions/{session_id}/history`
- **响应**：

```JSON
{
  "session_id": "sess_001",
  "messages": [
    {"role": "user", "content": "给4岁孩子买益智玩具", "timestamp": "2026-05-20T10:00:00Z"},
    {"role": "assistant", "content": "我为您筛选了...", "timestamp": "2026-05-20T10:00:05Z"}
  ]
}
```

#### `POST /chat/cancel` — 停止当前管道（新增 P1）

- **用途**：前端点击"停止生成"时通知后端终止当前管道，节省 token 和算力
- **请求**：

```JSON
{"session_id": "sess_001"}
```

- **响应**：

```JSON
{"status": "cancelled"}
```

### 错误码约定

后端原材料对错误码粒度**未指定**；本 PRD 做前端可执行约定：

<sheet sheet-id="MmzQmp" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### Android 数据模型与示例 Kotlin data class

kotlin

复制

```Kotlin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// === 全局规则 ===
// 1. 每个 SSE 事件必带 seq: Int（正整数递增）和 session_id: String（必填）
// 2. 首次请求 session_id=null 时后端生成 UUID，第一个事件携带
// 3. 字段命名 JSON 侧统一 snake_case，Kotlin 侧用 @SerialName 映射

@Serializable
sealed interface AgentSseEvent {
    val event: String
    val seq: Int
    @SerialName("session_id")
    val sessionId: String
}

@Serializable
data class ThinkingEvent(
    override val event: String = "thinking",
    override val seq: Int,
    @SerialName("session_id") override val sessionId: String,
    val stage: String,  // understanding | clarifying | searching | generating | decision_making
    val message: String
) : AgentSseEvent

@Serializable
data class ClarificationEvent(
    override val event: String = "clarification",
    override val seq: Int,
    @SerialName("session_id") override val sessionId: String,
    val questions: List<ClarificationQuestion>,
    @SerialName("required_slots") val requiredSlots: List<String> = emptyList(),
    @SerialName("partial_criteria") val partialCriteria: CriteriaPayload? = null
) : AgentSseEvent

@Serializable
data class ClarificationQuestion(
    val slot: String,
    val question: String,
    @SerialName("suggested_options") val suggestedOptions: List<String> = emptyList()
)

@Serializable
data class CriteriaCardEvent(
    override val event: String = "criteria_card",
    override val seq: Int,
    @SerialName("session_id") override val sessionId: String,
    @SerialName("criteria_id") val criteriaId: String,
    val editable: Boolean = true,
    val criteria: CriteriaPayload,
    val risks: List<String> = emptyList(),
    @SerialName("quick_actions") val quickActions: List<QuickActionPayload> = emptyList()
) : AgentSseEvent

@Serializable
data class TextDeltaEvent(
    override val event: String = "text_delta",
    override val seq: Int,
    @SerialName("session_id") override val sessionId: String,
    @SerialName("message_id") val messageId: String,
    val delta: String,
    val done: Boolean = false
) : AgentSseEvent

@Serializable
data class ProductCardEvent(
    override val event: String = "product_card",
    override val seq: Int,
    @SerialName("session_id") override val sessionId: String,
    val rank: Int,
    val product: ProductPayload,
    val reason: String,
    @SerialName("risk_notes") val riskNotes: List<String> = emptyList(),
    val evidence: List<EvidencePayload> = emptyList(),
    val actions: List<QuickActionPayload> = emptyList()
) : AgentSseEvent

@Serializable
data class FinalDecisionEvent(
    override val event: String = "final_decision",
    override val seq: Int,
    @SerialName("session_id") override val sessionId: String,
    @SerialName("winner_product_id") val winnerProductId: String? = null,
    val summary: String,
    val why: List<String> = emptyList(),
    @SerialName("not_for") val notFor: List<String> = emptyList(),
    val alternatives: List<AlternativePayload> = emptyList(),
    @SerialName("next_actions") val nextActions: List<QuickActionPayload> = emptyList()
) : AgentSseEvent

@Serializable
data class DoneEvent(
    override val event: String = "done",
    override val seq: Int,
    @SerialName("session_id") override val sessionId: String,
    @SerialName("criteria_id") val criteriaId: String,
    @SerialName("total_products") val totalProducts: Int
) : AgentSseEvent

@Serializable
data class ErrorEvent(
    override val event: String = "error",
    override val seq: Int,
    @SerialName("session_id") override val sessionId: String,
    val code: String,
    val message: String,
    val retryable: Boolean = true
) : AgentSseEvent

// === Payload 数据类 ===

@Serializable
data class CriteriaPayload(
    val age: Int? = null,
    val scenario: String? = null,
    @SerialName("budget_min") val budgetMin: Int? = null,
    @SerialName("budget_max") val budgetMax: Int? = null,
    @SerialName("toy_type") val toyType: String? = null,
    @SerialName("education_dimensions") val educationDimensions: List<String> = emptyList(),
    @SerialName("safety_features") val safetyFeatures: List<String> = emptyList(),
    @SerialName("messiness_level") val messinessLevel: String? = null,
    @SerialName("requires_battery") val requiresBattery: Boolean? = null
)

@Serializable
data class ProductPayload(
    @SerialName("product_id") val productId: String,
    val name: String,
    val brand: String? = null,
    val price: Int? = null,
    val currency: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("age_min") val ageMin: Int? = null,
    @SerialName("age_max") val ageMax: Int? = null,
    @SerialName("toy_type") val toyType: String? = null,
    @SerialName("education_dimensions") val educationDimensions: List<String> = emptyList(),
    @SerialName("safety_features") val safetyFeatures: List<String> = emptyList(),
    @SerialName("play_scenario") val playScenario: List<String> = emptyList(),
    @SerialName("messiness_level") val messinessLevel: String? = null,
    @SerialName("requires_battery") val requiresBattery: Boolean? = null
)

@Serializable
data class EvidencePayload(
    @SerialName("source_type") val sourceType: String,
    val snippet: String,
    @SerialName("source_id") val sourceId: String? = null
)

@Serializable
data class AlternativePayload(
    @SerialName("product_id") val productId: String,
    val name: String
)

@Serializable
data class QuickActionPayload(
    @SerialName("action_id") val actionId: String,
    val label: String,
    val action: String,  // criteria_patch | feedback | open_evidence | compare
    @SerialName("feedback_type") val feedbackType: String? = null,
    @SerialName("criteria_patch") val criteriaPatch: JsonObject? = null
)

// === 请求体 ===

@Serializable
data class ChatStreamRequest(
    val message: String,
    @SerialName("session_id") val sessionId: String? = null,  // null = 新会话，后端生成
    val history: List<MessageLite> = emptyList(),
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("criteria_patch") val criteriaPatch: JsonObject? = null,
    @SerialName("skip_stages") val skipStages: List<String>? = null
)

@Serializable
data class MessageLite(
    val role: String,
    val content: String
)
```

## 状态机 视觉交互与异常降级

Android 官方架构指南建议以状态持有者承载 UI 状态，而 `StateFlow` 适合承担可观察的 UI 状态；在 UI 层收集时，应通过 `repeatOnLifecycle` 让收集行为与可见生命周期一致，避免页面不可见时仍继续处理事件。对于这种高频、流式、可中断的导购页面，这一模式尤其关键。

### 前端状态机定义

<sheet sheet-id="SwaqBq" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 停止生成交互

停止生成必须是**显式且即时**的。点击后前端直接关闭 SSE 连接，并将当前状态切为 `Canceled`。如果后端支持取消接口，则同步发送 cancel；若后端未指定取消接口，前端至少要做到本地停止渲染、恢复输入、保留已有内容。

### 无闪烁渲染策略

<sheet sheet-id="JdIO8S" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 错误兜底与降级策略

<sheet sheet-id="hFIZKU" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 视觉与交互注意事项

- **流式文本动画**：不要逐字打印动画到影响可读性；建议块级 delta 平滑追加。
- **卡片入场**：轻量淡入即可，不做大位移动画。
- **停止生成**：按钮必须放在输入区显著位置，且点按后 100ms 内有界面反馈。
- **占位策略**：`criteria_card` 和 `final_decision` 不做长时间 skeleton；应在真实事件到达时再插入。
- **证据展示**：底板优先展示 snippet，不跳出应用打开长链接。
- **弱网体验**：显示“仍在处理中”而不是假死；任何情况下都不能只剩一个无响应 loading。

## 里程碑 测试与交付

### P0 / P1 / P2 功能里程碑

<sheet sheet-id="JBMamx" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 第 7 / 14 / 21 天验收标准

<sheet sheet-id="9Zxj4M" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 前端测试用例清单

<sheet sheet-id="kOhEHG" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 答辩 Demo 剧本

#### Demo 路径一

- **用户话术**：  
“给 4 岁孩子买一个室内玩的益智玩具，预算 200 元以内，不要小零件，也尽量不要电池。”
- **预期 SSE 流**：  
`thinking → criteria_card → text_delta → product_card ×2 → final_decision → done`
- **验收点**：  
年龄/场景/预算/安全约束被正确显示在 `criteria_card`；Top1 与备选都能渲染；结论卡明确指出“不适合什么情况”。

#### Demo 路径二

- **用户话术**：  
上传一张玩具包装图，然后问：“这个适合 3 岁孩子吗？会不会有吞咽风险？”
- **预期 SSE 流**：  
`/upload/image` 成功 → `thinking → clarification`（若缺信息）或 `text_delta → final_decision → done`
- **验收点**：  
图片预览存在；若后端可识别则直接进入判断；若识别失败也能兜底为文字建议，不出现空白页。

#### Demo 路径三

- **用户话术**：  
在首轮推荐后点按“不要电池”“再便宜一点”“不喜欢这个”。
- **预期 SSE 流**：  
`feedback/criteria_patch → thinking → criteria_card → product_card → final_decision → done`
- **验收点**：  
用户反馈会真实改变下一轮结果，而不是重复返回同一商品；Debug 页能看到新的 `session_id` 或同 session 下的新 trace。

### 管理后台与 Debug 页最小需求

后端材料中已出现 `retrieval_traces`、`evidence_links` 与 prompt/version 相关的评测思路，但具体 Android 调试页字段粒度**未指定**。本 PRD 要求至少有一个本地 Debug 页，最小展示如下：

<sheet sheet-id="2RcIXK" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 开发交付清单与 21 天任务分解

<sheet sheet-id="i6KDTK" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 最终交付物清单

<sheet sheet-id="DcwuUW" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

本 PRD 的核心目标不是做一个“大而全”的电商 App，而是用最少页面和最稳的流式工程实现，把亲子玩具导购 Agent 的**理解能力、证据能力、反馈能力和可展示性**完整地做出来。对于三周赛制而言，这比扩页面、堆功能、追求“像商城”更重要。