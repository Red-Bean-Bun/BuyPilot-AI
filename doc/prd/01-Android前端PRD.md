# BuyPilot-AI Android前端与Agent-to-UI PRD

**Executive Summary**

本 PRD 面向 **3 周赛制**下的 **多品类智能导购 Agent** Android 客户端实现，目标是把当前已确认的整体策略口径与后端 & Agent PRD 口径，落成一份**可以直接交付给 Android 开发者开工**的前端执行文档。文档默认采用当前对话中已经确认的后端事件集与字段口径：`thinking`、`clarification`、`criteria_card`、`text_delta`、`product_card`、`cart_action`、`final_decision`、`done/error`，并围绕”**模糊需求 → 澄清 → 购买标准 → 商品推荐 → 加购操作 → 决策结论 → 反馈修正**”构建流式 UI 主链路。

文档中的状态说明如下：**已确认**表示当前对话中两份 PDF 已明确的方向或字段口径；**未指定**表示原材料未给出；**本 PRD 决策**表示为保证 Android 工程可实施而新增的前端约定。对外部技术栈、SDK 能力、版本与平台兼容信息，优先采用官方或原始资料。Jetpack Compose 官方建议以 BOM 统一管理 Compose 依赖版本；Photo Picker 支持 Compose 启动器并在不支持设备上回退到 `ACTION_OPEN_DOCUMENT`；CameraX 是 Google 推荐新应用优先使用的相机库；ML Kit 文本识别、Coil Compose 与 OkHttp SSE 均有官方或原始资料可引用。

## 产品定位与范围

### 一句话定位

**BuyPilot-AI Android 端是一款多品类智能导购的原生流式界面，用 Agent 化的多轮问答与证据化卡片，帮助用户从模糊需求中快速形成购买决策。**

### 目标用户与核心体验

本项目的**目标用户**是对产品适配度、成分安全、使用场景和预算平衡有明确需求的人群，涵盖美妆护肤、数码电子、服饰运动、食品生活等多个品类。当前赛题的核心不是”搜索更多商品”，而是把用户的模糊表达还原为可执行的购买标准，再用结构化推荐和证据解释完成决策闭环。这一定位与 Android 架构上强调的“稳健、可测试、可维护”的分层实现是相容的，也适合用 `ViewModel + StateFlow` 承载可持续更新的 UI 状态。

### 设计原则

<sheet sheet-id="pjFAv8" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 不可做功能清单

为了匹配三周比赛节奏，前端必须严格收缩范围。**P0 不做**：商城首页、类目浏览页、商品详情长页、支付、订单系统、消息通知中心、账号体系深集成、客服系统、多端同登同步。**P1 也不建议做**：复杂筛选页、长列表瀑布流、活动会场、优惠券、多图多视频理解、完整埋点后台。购物车仅做⭐入门档（对话式加购 + 购物车图标 + 购物车底板查看），不做⭐⭐进阶（NLP操作购物车）和⭐⭐⭐挑战（下单确认流程）。上述能力在原始材料中**未指定**，本 PRD 决策为**除购物车⭐入门外全部降级为非目标项**。

## 技术栈与工程结构

Android 端建议统一采用 **Kotlin + Jetpack Compose**，并通过官方 **Compose BOM** 统一 Compose 依赖版本；官方文档示例给出了 `androidx.compose:compose-bom:2026.04.01`。图片选择优先用系统 Photo Picker；官方明确其在 Compose 中可直接通过 `rememberLauncherForActivityResult(PickVisualMedia())` 启动，在不支持设备上会回退至 `ACTION_OPEN_DOCUMENT`，并可覆盖到 Android 4.4+ 的设备层。相机拍摄如果进入 P1/P2，则优先用 CameraX，Google 官方建议新应用从 CameraX 起步，且其向后兼容到 Android 5.0。图片渲染建议用 Coil Compose；官方当前文档给出 `io.coil-kt.coil3:coil-compose:3.4.0`，并推荐大多数场景优先使用 `AsyncImage`。SSE 通道建议用 OkHttp SSE；Square 官方模块说明其为实验性支持，当前 README 示例依赖为 `com.squareup.okhttp3:okhttp-sse:5.3.0`。如果需要端上 OCR 兜底，可选 ML Kit Text Recognition；Google 官方当前 Android 文档给出拉丁文与中文识别依赖 `16.0.1`，Play services 版本也有对应依赖。UI 状态管理建议用 `ViewModel + StateFlow`，并在 UI 层使用 `repeatOnLifecycle` 收集，避免不可见时仍处理事件。后端 LLM 接入采用**双轨：火山引擎 Doubao + 百炼 Qwen**，意图识别用 Doubao（免费额度），购买标准生成用 Qwen-Plus（JSON schema 级约束更稳定），前端无需关心具体模型路由，只需消费 SSE 事件。

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

1. **顶部导航区**
固定在顶部 AppBar，展示返回、页面标题、会话/连接状态点与必要的全局工具入口。TopBar 不展示“正在理解需求”“正在生成购买标准”“正在检索商品”等过程文案；这些过程状态属于当前 AI 回复的一部分，必须进入聊天流。**停止生成按钮不放在 TopBar，统一放在底部输入区**，与发送按钮共享生成态位置。
2. **消息列表与卡片流**
使用单一 `LazyColumn` 承载，**文本消息、thinking 动画与结构化结果必须进入同一条时间线**。不能把产品卡单独放在聊天流外，否则会破坏用户对“这是 Agent 逐步思考结果”的感知。
列表元素建议统一抽象为 `ChatUiNode`：

   - 用户文本消息
   - 用户图片消息
   - AI thinking 动画气泡 / 骨架节点
   - AI 文本流消息
   - `clarification` 澄清完整小卡
   - `criteria_card` 购买标准摘要卡
   - `product_card` 商品 `SwipeDeck` 卡堆节点
   - `cart_action` 加购/购物车操作反馈节点
   - `final_decision` 最终决策摘要卡
   - 系统错误气泡
3. **输入区**  
底部固定。包含：

   - 文本输入框，支持 1–4 行扩展
   - 图片按钮，优先调用 Photo Picker；支持单图预览
   - 发送按钮
   - 停止生成按钮，仅在流式进行中显示
   - 重试按钮，仅在错误态显示
4. **反馈区**  
仅在出现 `criteria_card`、`product_card` 或 `final_decision` 后显示轻量操作。`clarification` 可以直接在对话区完整展示；`criteria_card` 在对话区只展示摘要确认卡与“修改/展开”等入口；`product_card` 进入独立 SwipeDeck，通过左右滑表达喜欢/不喜欢，通过点击或上滑进入详情；`final_decision` 展示中等完整摘要，点击“查看依据”进入底板。证据不直接塞进聊天流。
5. **浮层/底板**  
用于购买标准编辑、商品详情、证据详情、图片预览、商品对比与错误详情。P0 要求购买标准编辑底板、商品详情/证据底板与图片预览；对比页可到 P1 实现。

### 交互流程

当用户发送文本或图片后，前端先做**本地回显**，随后创建或续用 `session_id`，发起 `/chat/stream`。**session_id 规则**：首次对话时 `session_id` 为 null，后端会生成新的 session_id 并在所有响应事件的 envelope 中返回；前端必须从首个 SSE 事件中提取 `session_id` 并缓存到 ViewModel，后续请求携带同一 `session_id`。一旦连接建立，聊天流中立即插入或更新一条 AI thinking 动画气泡，`thinking.message` 用于驱动该气泡内的阶段文案，例如”正在理解需求””正在生成购买标准””正在检索商品”。TopBar 只展示会话/连接状态，不承载这些阶段文案。如果收到 `clarification`，输入框保持可编辑，用户可以直接回答，当前 stream 结束；如果收到 `criteria_card`，则在时间线上插入购买标准摘要卡，完整字段、quick actions 与可编辑项进入 Bottom Sheet，用户修改后通过 `criteria_patch` 回流。`text_delta` 会持续增量更新同一条 AI 气泡；多个 `product_card` 到达时进入同一个商品 `SwipeDeck` 数据源，而不是在聊天流中连续插入多张完整商品详情卡；`final_decision` 到达时，在流末插入最终决策摘要卡并关闭 loading。整个过程中，用户点击底部输入区的”停止生成”应立即关闭 SSE 连接（主要取消信号），同时 best-effort 调用 `POST /chat/cancel`，并将当前状态改为 `Canceled`，保留已生成内容供继续追问。

### 输入区规范

系统 Photo Picker 的优势在于，它以系统级安全方式只向应用授予用户选中的图片/视频，而不是整个媒体库访问权限；官方同时给出了 Compose 场景下的启动方式。P0 阶段建议**只做单图上传**；多图、视频与实时拍摄均不进入主链路。对于不支持 Photo Picker 的设备，系统会回退到 `ACTION_OPEN_DOCUMENT`，因此前端不需要自己再维护第二套文件选择 UI。

输入区行为要求如下：

<sheet sheet-id="U0OTLF" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 消息列表与卡片流渲染规则

渲染层必须满足四条硬规则。第一，**仅有一个时间线**；第二，`thinking` 与 `text_delta` **永远更新同一条当前 AI 回复节点**，不得每个 delta 或阶段变化新增一条列表项；第三，**所有结构化卡片都使用后端提供的稳定 `node_id` 或 `deck_id`**，插入后以 upsert 方式更新，避免重组造成抖动；第四，多个 `product_card` 必须聚合到同一个 `ProductSwipeDeck` 节点，不能在聊天流中堆叠成多张详情卡。

建议渲染策略如下：

<sheet sheet-id="NriNH0" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### Agent-to-UI 渲染契约

后端 SSE 不是普通日志流，而是前端状态机可以直接消费的 **Agent-to-UI 事件流**。每个事件必须带统一 envelope，用于幂等、排序、稳定 key 和分组渲染：

```JSON
{
  "schema_version": "2026-05-20",
  "event": "thinking",
  "session_id": "sess_demo_001",
  "turn_id": "turn_001",
  "seq": 1,
  "event_id": "turn_001:0001",
  "node_id": "thinking_turn_001",
  "deck_id": null,
  "display_mode": "inline_thinking",
  "created_at_ms": 1780000000000
}
```

- `turn_id`：一次用户输入对应一次 Agent 回复轮次；同一轮所有事件必须一致。
- `seq` / `event_id`：后端按轮次单调递增，客户端用来排序和去重。
- `node_id`：聊天流节点稳定 key。`thinking` 与本轮 AI 文本可以共享同一个回复节点，也可以分别使用稳定节点；不得每次阶段变化生成新节点。
- `deck_id`：商品推荐卡堆稳定 key。所有同一轮推荐商品必须带同一个 `deck_id`，客户端据此创建或更新一个 `ProductSwipeDeck`。
- `display_mode`：后端明确建议前端如何渲染，例如 `inline_thinking`、`inline_card`、`inline_text`、`summary_card`、`swipe_deck_item`、`none`。前端可以按设计系统实现样式，但不能反向猜测数据用途。
- `summary` / `detail` / `evidence`：结构化卡片必须拆成对话区摘要、底板详情和证据三层；证据只进入底板或详情页，不直接作为聊天节点。

### 三类核心卡片 UI 规范

<sheet sheet-id="dzyNci" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

`criteria_card` 的目标不是”展示系统内部理解”，而是让用户看见**当前购买标准已被结构化**，并可一键修正。对话区中的购买标准只展示摘要，例如”已理解你的需求：油性肌肤｜200元内｜日常护肤｜洁面类”，完整字段、权重、quick actions 与编辑控件进入 Bottom Sheet。`product_card` 的目标不是穷举参数，而是进入 SwipeDeck 后把用户最关心的肌肤适配、成分标签、使用场景和预算前置；商品完整详情、风险说明和证据在详情底板展示。`final_decision` 则必须给出**可执行结论**，对话区展示中等完整摘要，”查看依据”进入 Bottom Sheet。

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

下面给出一组**从用户请求到完整 SSE / A2UI 序列**的工程样例。为便于 Android 侧直接联调，示例包含原始请求体、SSE 帧内容与事件顺序。后端必须提供 `turn_id`、`seq`、`event_id`、`node_id`、`display_mode`；商品推荐必须提供统一 `deck_id`，客户端据此把多个 `product_card` 聚合为一个 SwipeDeck。

**请求**

json

复制

```JSON
{"message": "推荐适合油性肌肤的日常洁面产品，预算200元以内","session_id": "sess_demo_001","history": [],"image_url": null,"client_turn_id":"turn_client_001"}
```

**响应流**

text

复制

```Plain Text
event: thinking
data: {"schema_version":"2026-05-20","event":"thinking","session_id":"sess_demo_001","turn_id":"turn_001","seq":1,"event_id":"turn_001:0001","node_id":"thinking_turn_001","deck_id":null,"display_mode":"inline_thinking","created_at_ms":1780000000000,"payload":{"phase":"analyzing","message":"正在理解肤质、场景、预算与成分约束"}}

event: criteria_card
data: {"schema_version":"2026-05-20","event":"criteria_card","session_id":"sess_demo_001","turn_id":"turn_001","seq":2,"event_id":"turn_001:0002","node_id":"criteria_001","deck_id":null,"display_mode":"summary_card","created_at_ms":1780000000100,"payload":{"criteria_id":"criteria_001","summary":{"title":"已理解你的需求","chips":["油性肌肤","200元内","日常护肤","洁面类"]},"detail":{"category":"美妆护肤","skin_type":"油性","budget_max":200,"ingredient_avoid":[],"use_scenario":"日常护肤"},"quick_actions":[{"action_id":"budget_low","label":"预算压低","action":"criteria_patch","criteria_patch":{"budget_max":150}},{"action_id":"sensitive_skin","label":"敏感肌适用","action":"criteria_patch","criteria_patch":{"skin_type":"敏感"}},{"action_id":"no_alcohol","label":"不要含酒精","action":"criteria_patch","criteria_patch":{"ingredient_avoid":["酒精"]}}]}}

event: text_delta
data: {"schema_version":"2026-05-20","event":"text_delta","session_id":"sess_demo_001","turn_id":"turn_001","seq":3,"event_id":"turn_001:0003","node_id":"ai_text_turn_001","deck_id":null,"display_mode":"inline_text","created_at_ms":1780000000200,"payload":{"message_id":"msg_ai_01","delta":"我先按油性肌肤、200元以内、日常护肤来筛选。","done":false}}

event: text_delta
data: {"schema_version":"2026-05-20","event":"text_delta","session_id":"sess_demo_001","turn_id":"turn_001","seq":4,"event_id":"turn_001:0004","node_id":"ai_text_turn_001","deck_id":null,"display_mode":"inline_text","created_at_ms":1780000000300,"payload":{"message_id":"msg_ai_01","delta":"下面给你两款更适合油皮日常洁面的选择。","done":false}}

event: product_card
data: {"schema_version":"2026-05-20","event":"product_card","session_id":"sess_demo_001","turn_id":"turn_001","seq":5,"event_id":"turn_001:0005","node_id":"product_p_beauty_011","deck_id":"deck_turn_001","display_mode":"swipe_deck_item","created_at_ms":1780000000400,"payload":{"product_id":"p_beauty_011","rank":1,"summary":{"name":"珊珂洗颜专科绵润泡沫洁面乳","price":52,"currency":"CNY","image_url":"https://example.com/p_beauty_011.jpg","chips":["油性适用","200元内","控油洁面"],"reason_short":"油皮清洁力强，泡沫绵密不刺激，性价比极高。","risk_short":"含微量香精，极敏感肌需留意。"},"detail":{"skin_type_match":["油性","混合"],"ingredient_tags":["氨基酸","绵润泡沫"],"ingredient_avoid":[],"use_scenario":["日常护肤"],"sub_category":"洁面","risk_notes":["含微量香精，极敏感肌需留意。"]},"evidence_refs":[{"evidence_id":"ev_beauty_011_01","source_type":"product_chunk","trust_label":"商品资料","snippet":"珊珂洗颜专科绵润泡沫洁面乳，油性肌肤适用，氨基酸配方，52元"}],"actions":[{"action_id":"show_evidence","label":"看证据","action":"open_evidence"},{"action_id":"dislike_product","label":"不喜欢这个","action":"feedback","feedback_type":"not_interested"}]}}

event: product_card
data: {"schema_version":"2026-05-20","event":"product_card","session_id":"sess_demo_001","turn_id":"turn_001","seq":6,"event_id":"turn_001:0006","node_id":"product_p_beauty_007","deck_id":"deck_turn_001","display_mode":"swipe_deck_item","created_at_ms":1780000000500,"payload":{"product_id":"p_beauty_007","rank":2,"summary":{"name":"薇诺娜舒敏保湿特护霜","price":268,"currency":"CNY","image_url":"https://example.com/p_beauty_007.jpg","chips":["敏感肌适用","保湿修护"],"reason_short":"敏感肌首选修护面霜，温和不刺激。"},"detail":{"skin_type_match":["敏感","干性"],"ingredient_tags":["马齿苋","青刺果"],"ingredient_avoid":[],"use_scenario":["日常护肤"],"sub_category":"面霜","risk_notes":[]},"evidence_refs":[{"evidence_id":"ev_beauty_007_01","source_type":"product_chunk","trust_label":"商品资料","snippet":"薇诺娜舒敏保湿特护霜，敏感肌适用，马齿苋+青刺果修护配方，268元"}],"actions":[{"action_id":"show_evidence","label":"看证据","action":"open_evidence"},{"action_id":"show_alternatives","label":"换相似的","action":"feedback","feedback_type":"show_alternatives"}]}}

event: final_decision
data: {"schema_version":"2026-05-20","event":"final_decision","session_id":"sess_demo_001","turn_id":"turn_001","seq":7,"event_id":"turn_001:0007","node_id":"decision_turn_001","deck_id":null,"display_mode":"summary_card","created_at_ms":1780000000600,"payload":{"summary":{"winner_product_id":"p_beauty_011","verdict":"优先选珊珂洗颜专科洁面乳","why_chips":["油性适用","200元内","控油效果好","性价比高"],"not_for_short":"若你希望避免含香精产品，建议考虑其他选项"},"detail":{"why":["油性适用","200元内","控油效果好","性价比高"],"not_for":["若你希望避免含香精产品，不建议 Top1"],"alternatives":[{"product_id":"p_beauty_007","name":"薇诺娜舒敏保湿特护霜"}]},"evidence_refs":[{"evidence_id":"ev_beauty_011_01","source_type":"product_chunk","trust_label":"商品资料","snippet":"珊珂洗颜专科绵润泡沫洁面乳，油性肌肤适用，氨基酸配方，52元"}],"next_actions":[{"action_id":"cheaper","label":"再便宜一点","action":"criteria_patch","criteria_patch":{"budget_max":150}},{"action_id":"sensitive_skin","label":"敏感肌适用","action":"criteria_patch","criteria_patch":{"skin_type":"敏感"}}]}}

event: done
data: {"schema_version":"2026-05-20","event":"done","session_id":"sess_demo_001","turn_id":"turn_001","seq":8,"event_id":"turn_001:0008","node_id":"done_turn_001","deck_id":"deck_turn_001","display_mode":"none","created_at_ms":1780000000700,"payload":{"criteria_id":"criteria_001","deck_id":"deck_turn_001","total_products":2,"client_turn_id":"turn_client_001","finish_reason":"completed"}}
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
- 首个 `thinking` 事件软超时：2 秒；超时后在当前 AI 回复位置显示“响应较慢，仍在等待”的 inline thinking 气泡
- 首个结构化事件（`clarification` / `criteria_card` / `error`）软超时：8 秒  
如果超过软超时仍无事件，聊天流中的 thinking 节点改为“响应较慢，仍在等待”，但**不应自动中断**。TopBar 只保持连接状态，不展示阶段文案。

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

#### `POST /feedback`

<sheet sheet-id="odnsUX" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 错误码约定

后端原材料对错误码粒度**未指定**；本 PRD 做前端可执行约定：

<sheet sheet-id="MmzQmp" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### Android 数据模型与示例 Kotlin data class

kotlin

复制

```Kotlin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AgentUiEnvelope<T>(
    @SerialName("schema_version") val schemaVersion: String = "2026-05-20",
    val event: String,
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("turn_id") val turnId: String? = null,
    val seq: Long,
    @SerialName("event_id") val eventId: String,
    @SerialName("node_id") val nodeId: String,
    @SerialName("deck_id") val deckId: String? = null,
    @SerialName("display_mode") val displayMode: String,
    @SerialName("created_at_ms") val createdAtMs: Long,
    val payload: T
)

@Serializable data class ThinkingPayload(
    val phase: String,
    val message: String
)

@Serializable data class ClarificationPayload(
    val question: String,
    @SerialName("required_slots") val requiredSlots: List<String> = emptyList(),
    @SerialName("suggested_options") val suggestedOptions: List<String> = emptyList(),
    @SerialName("partial_criteria") val partialCriteria: JsonElement? = null
)

@Serializable data class CriteriaSummaryPayload(
    val title: String,
    val chips: List<String> = emptyList()
)

@Serializable data class CriteriaCardPayload(
    @SerialName("criteria_id") val criteriaId: String,
    val summary: CriteriaSummaryPayload,
    val detail: JsonElement,
    @SerialName("quick_actions") val quickActions: List<QuickActionPayload> = emptyList()
)

@Serializable data class TextDeltaPayload(
    @SerialName("message_id") val messageId: String,
    val delta: String,
    val done: Boolean = false
)

@Serializable data class ProductSummaryPayload(
    val name: String,
    val price: Double? = null,
    val currency: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val chips: List<String> = emptyList(),
    @SerialName("reason_short") val reasonShort: String? = null,
    @SerialName("risk_short") val riskShort: String? = null
)

@Serializable data class ProductCardPayload(
    @SerialName("product_id") val productId: String,
    val rank: Int,
    val summary: ProductSummaryPayload,
    val detail: JsonElement,
    @SerialName("evidence_refs") val evidenceRefs: List<EvidenceRefPayload> = emptyList(),
    val actions: List<QuickActionPayload> = emptyList()
)

@Serializable data class EvidenceRefPayload(
    @SerialName("evidence_id") val evidenceId: String,
    @SerialName("source_type") val sourceType: String,
    @SerialName("trust_label") val trustLabel: String? = null,
    val snippet: String? = null
)

@Serializable data class DecisionSummaryPayload(
    @SerialName("winner_product_id") val winnerProductId: String? = null,
    val verdict: String,
    @SerialName("why_chips") val whyChips: List<String> = emptyList(),
    @SerialName("not_for_short") val notForShort: String? = null
)

@Serializable data class FinalDecisionPayload(
    val summary: DecisionSummaryPayload,
    val detail: JsonElement,
    @SerialName("evidence_refs") val evidenceRefs: List<EvidenceRefPayload> = emptyList(),
    @SerialName("next_actions") val nextActions: List<QuickActionPayload> = emptyList()
)

@Serializable data class QuickActionPayload(
    @SerialName("action_id") val actionId: String,
    val label: String,
    val action: String,
    @SerialName("feedback_type") val feedbackType: String? = null,
    @SerialName("criteria_patch") val criteriaPatch: JsonElement? = null
)

@Serializable data class CartActionPayload(
    val action: String,              // "add" | "remove" | "view"
    @SerialName("product_id") val productId: String,
    @SerialName("cart_id") val cartId: String? = null,
    val quantity: Int = 1,
    val status: String               // "success" | "failed"
)

@Serializable data class DonePayload(
    @SerialName("criteria_id") val criteriaId: String? = null,
    @SerialName("deck_id") val deckId: String? = null,
    @SerialName("total_products") val totalProducts: Int? = null,
    @SerialName("client_turn_id") val clientTurnId: String? = null,
    @SerialName("finish_reason") val finishReason: String
)

@Serializable data class ErrorPayload(
    val code: String,
    val message: String,
    val retryable: Boolean = true,
    @SerialName("recover_action") val recoverAction: String? = null
)

@Serializable data class ChatStreamRequest(
    val message: String,
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("client_turn_id") val clientTurnId: String? = null,
    val history: List<MessageLite> = emptyList(),
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("criteria_patch") val criteriaPatch: JsonElement? = null,
    @SerialName("skip_stages") val skipStages: List<String>? = null,
    @SerialName("client_trace_id") val clientTraceId: String? = null
)

@Serializable data class MessageLite(
    val role: String,
    val content: String
)
```

## 状态机 视觉交互与异常降级

Android 官方架构指南建议以状态持有者承载 UI 状态，而 `StateFlow` 适合承担可观察的 UI 状态；在 UI 层收集时，应通过 `repeatOnLifecycle` 让收集行为与可见生命周期一致，避免页面不可见时仍继续处理事件。对于这种高频、流式、可中断的导购页面，这一模式尤其关键。

### 前端状态机定义

<sheet sheet-id="SwaqBq" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 停止生成交互

停止生成必须是**显式且即时**的，并且按钮只能出现在底部输入区的生成态位置。点击后前端直接关闭 SSE 连接，并将当前 `turn_id` 切为 `Canceled`。工程化实现建议后端提供 `POST /chat/cancel` 或在 SSE 断开后识别取消信号，服务端停止后续 LLM/RAG 任务与后台写入；若取消请求失败，前端至少要做到本地停止渲染、恢复输入、保留已有内容。

### 无闪烁渲染策略

<sheet sheet-id="JdIO8S" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 错误兜底与降级策略

<sheet sheet-id="hFIZKU" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 视觉与交互注意事项

- **流式文本动画**：不要逐字打印动画到影响可读性；建议块级 delta 平滑追加。
- **卡片入场**：轻量淡入即可，不做大位移动画。
- **停止生成**：按钮必须放在输入区显著位置，且点按后 100ms 内有界面反馈。
- **占位策略**：`criteria_card` 和 `final_decision` 不做长时间 skeleton；应在真实事件到达时再插入。
- **证据展示**：证据只在 Bottom Sheet / 详情层展示，底板优先展示 snippet，不跳出应用打开长链接。
- **弱网体验**：显示“仍在处理中”而不是假死；任何情况下都不能只剩一个无响应 loading。

## 里程碑 测试与交付

### P0 / P1 / P2 功能里程碑

<sheet sheet-id="JBMamx" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 第 7 / 14 / 21 天验收标准

<sheet sheet-id="9Zxj4M" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 前端测试用例清单

<sheet sheet-id="kOhEHG" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 答辩 Demo 剧本

#### Demo 路径一：模糊推荐+条件筛选

- **用户话术**：
“推荐适合油性肌肤的日常洁面产品”
- **预期 SSE 流**：
`thinking → clarification`（若缺约束）或 `thinking → criteria_card → text_delta → product_card ×2 → final_decision → done`
- **验收点**：
肤质/场景/预算/成分约束被正确显示在 `criteria_card`；Top1 与备选都能渲染；结论卡明确指出”不适合什么情况”。

#### Demo 路径二：拍照找货

- **用户话术**：
上传一张护肤品包装图 + “这个适合敏感肌吗？含酒精吗？”
- **预期 SSE 流**：
`/upload/image` 成功 → `thinking → clarification`（若缺信息）或 `text_delta → product_card → final_decision → done`
- **验收点**：
图片预览存在；若后端可识别则直接进入判断；若识别失败也能兜底为文字建议，不出现空白页。

#### Demo 路径三：多轮+反选排除

- **用户话术**：
在首轮推荐后说”不要含酒精” + “预算降到200”
- **预期 SSE 流**：
`feedback/criteria_patch → thinking → criteria_card → product_card → final_decision → done`
- **验收点**：
用户反馈会真实改变下一轮结果，而不是重复返回同一商品；Debug 页能看到新的 `session_id` 或同 session 下的新 trace。

#### Demo 路径四：对话式加购

- **用户话术**：  
“把这个加到购物车”
- **预期 SSE 流**：  
`thinking → cart_action(add) → done`
- **验收点**：  
cart_action 事件正确渲染加购反馈；购物车内容可在底板中查看；加购失败时显示 failed 状态和原因。

### 管理后台与 Debug 页最小需求

后端材料中已出现 `retrieval_traces`、`evidence_links` 与 prompt/version 相关的评测思路，但具体 Android 调试页字段粒度**未指定**。本 PRD 要求至少有一个本地 Debug 页，最小展示如下：

<sheet sheet-id="2RcIXK" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 开发交付清单与 21 天任务分解

<sheet sheet-id="i6KDTK" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

### 最终交付物清单

<sheet sheet-id="DcwuUW" token="F4F5sgj1YhVLR3tDD8YcLTXXnrc"></sheet>

本 PRD 的核心目标不是做一个”大而全”的电商 App，而是用最少页面和最稳的流式工程实现，把多品类智能导购 Agent 的**理解能力、证据能力、反馈能力和可展示性**完整地做出来。对于三周赛制而言，这比扩页面、堆功能、追求”像商城”更重要。
