# BuyPilot-AI 最新 UI 设计稿说明

> 更新时间：2026-05-26  
> 来源：基础聊天流来自 `/Users/ziggy/Downloads/1213`，商品推荐链路更新来自 `/Users/ivy/Downloads/11111`  
> 说明：当前文件名按 `doc/prd/01-Android前端PRD.md` 与 SSE/A2UI 组件命名整理。商品推荐部分已更新为“聊天内缩略推荐带 → 独立 Swipe 挑选模式 → 商品大图详情 → 下滑详情 → 更多证据”的完整链路。

## 1. 设计稿索引

| 顺序 | 文件 | PRD / 组件映射 | UI 状态 | 来源目录 | 设计用途 |
|---|---|---|---|---|---|
| 01 | `01-onboarding-home.png` | ChatScreen / EmptyHome | 初始欢迎态 | `1213/buypilot_ai_yolo_3` | 用豆沙包吉祥物和一句强引导文案建立“AI 导购助手”心智。 |
| 02 | `02-chat-thinking.png` | `thinking` / ThinkingBubble | AI 推理中 | `1213/buypilot_ai_1` | 用户发送需求后的等待态，展示“正在思考中...”而不是让页面空转。 |
| 03 | `03-clarification-card.png` | `clarification` / ClarificationCard | 澄清追问 | `1213/buypilot_ai_3` | 缺少关键槽位时只追问一个最重要问题，例如肤质。 |
| 04 | `04-criteria-summary-card.png` | `criteria_card` / CriteriaSummaryCard | 购买标准摘要 | `1213/buypilot_ai_yolo_1` | 把模糊需求结构化为可确认、可修改的购买标准。 |
| 05 | `05-criteria-edit-sheet.png` | Criteria Edit Bottom Sheet | 标准编辑底板 | `1213/buypilot_ai_1_1` | 展示完整字段和 quick actions，承接“修改标准”。 |
| 06 | `06-product-recommendation-strip.png` | `product_card` / ProductRecommendationStrip | 聊天内缩略推荐带 | `11111/buypilot_ai_1` | 在聊天流内展示可横向滑动的缩小商品图，作为进入挑选模式的入口。 |
| 07 | `07-product-swipe-mode.png` | ProductSwipeMode | 独立 Swipe 挑选模式 | `11111/buypilot_ai_3` | 点击聊天内推荐带后进入，用户通过左滑/右滑表达不喜欢或喜欢。 |
| 08 | `08-product-hero-detail.png` | ProductHeroDetail | 商品大图详情 | `11111/buypilot_ai_2` | 在 Swipe 模式中点击商品主图后进入，聚焦单个商品的视觉、名称、价格和选择动作。 |
| 09 | `09-product-scroll-detail.png` | ProductScrollDetail | 下滑查看商品详情 | `11111/buypilot_ai_4` | 商品大图详情继续下滑后展示更完整的卖点、成分/参数和推荐理由。 |
| 10 | `10-product-evidence-overlay.png` | ProductEvidenceOverlay | 商品推荐证据 | `11111/buypilot_ai_5` | 在商品详情右上角点击更多后显示证据，解释为什么推荐这个商品。 |
| 11 | `11-decision-summary-card.png` | `final_decision` / DecisionSummaryCard | 最终决策摘要 | `1213/buypilot_ai_13` | 给出首选、理由、不适合情况和下一步动作。 |
| 12 | `12-decision-evidence-sheet.png` | Decision Evidence Bottom Sheet | 决策依据底板 | `1213/buypilot_ai_bottom_sheet` | 承接最终决策卡的“查看决策依据”，展示理由、风险、备选和细节。 |
| 13 | `13-input-attachment-menu.png` | InputAttachmentMenu | 输入扩展菜单 | `1213/buypilot_ai_5` | 展示图片输入入口，并预留语音/实时视频的未来能力。 |

豆沙包吉祥物素材统一放在 `brand/redbean-bun-mascot/`：

| 文件 | 用途 |
|---|---|
| `brand/redbean-bun-mascot/redbean-bun-logo-front.png` | 正面豆沙包 logo 资产，可用于 App 图标探索或品牌规范。 |
| `brand/redbean-bun-mascot/redbean-bun-mascot-white.png` | 白底豆沙包吉祥物，可用于欢迎页、thinking 气泡或空状态。 |
| `brand/redbean-bun-mascot/redbean-bun-mascot-soft.png` | 柔和版本豆沙包吉祥物，可用于轻量插画或加载态。 |
| `brand/redbean-bun-mascot/redbean-bun-character-01.png` | 豆沙包角色独立素材 01。 |
| `brand/redbean-bun-mascot/redbean-bun-character-02.png` | 豆沙包角色独立素材 02。 |
| `brand/redbean-bun-mascot/redbean-bun-character-03.png` | 豆沙包角色独立素材 03。 |
| `brand/redbean-bun-mascot/redbean-bun-mascot-smile-white.png` | 白底微笑版本，可用于更轻量的空状态或提示态。 |
| `brand/redbean-bun-mascot/redbean-bun-mascot-eyes-variant.png` | 眼睛调整版本，用于后续品牌探索对比。 |

## 2. 主流程设计

最新设计稿对应的核心链路是：

```text
01 OnboardingHome
→ 02 ThinkingBubble
→ 03 ClarificationCard（如缺关键槽位）
→ 04 CriteriaSummaryCard
→ 05 CriteriaEditSheet（可选修改）
→ 06 ProductRecommendationStrip（聊天内缩略推荐带，可横向滑动查看）
→ 07 ProductSwipeMode（点击推荐带进入挑选模式）
→ 08 ProductHeroDetail（在 Swipe 模式中点击商品大图）
→ 09 ProductScrollDetail（商品详情内下滑查看更多信息）
→ 10 ProductEvidenceOverlay（点击右上角更多查看推荐证据）
→ 11 DecisionSummaryCard
→ 12 DecisionEvidenceSheet（查看最终决策依据）
```

这条链路与 PRD 的“模糊需求 → 澄清 → 购买标准 → 商品推荐 → 决策结论 → 反馈修正”一致。页面不是传统商城首页，也不是商品列表搜索结果页，而是一个轻量聊天流加结构化决策卡片的导购界面。

## 3. 整体设计原则

### 3.1 单时间线

聊天区只有一条主时间线。用户消息、AI thinking、澄清卡、购买标准卡、商品推荐入口和最终决策卡都在同一条流里出现。这样能避免用户在“聊天、搜索、详情、筛选页”之间来回跳转。

### 3.2 摘要在主流，详情进底板

主时间线只展示摘要：澄清问题、购买标准摘要、商品缩略推荐带、最终决策摘要。长证据、完整参数、编辑表单和决策细节进入挑选模式、详情页或 Bottom Sheet。这和 `DESIGN.md` 里的“summary-first in the timeline, detail in bottom sheets”一致。

### 3.3 决策导向，不做商城噪音

界面不突出促销、频道、榜单、瀑布流和复杂筛选。所有视觉层级都服务于一个目标：让用户知道“AI 理解了什么、推荐了什么、为什么推荐、什么情况下不适合”。

### 3.4 温暖可信的品牌感

主色使用 coral orange，承接发送按钮、主 CTA、强调状态和豆沙包吉祥物。背景和卡片保持浅色、留白和低阴影，降低“导购压迫感”，让用户愿意继续补充需求。

## 4. 关键组件设计说明

### 4.1 OnboardingHome

对应 `01-onboarding-home.png`。

首页使用大号欢迎语“开启你的购物新体验”和豆沙包吉祥物，重点不是铺功能，而是建立“你可以直接说需求”的入口心智。顶部只保留 BuyPilot 品牌、购物车/会话入口等必要元素，避免做成电商频道首页。

实现上可作为 `ChatScreen` 的 empty state：当没有历史消息时展示；用户输入第一句话后收起，切入标准聊天时间线。

### 4.2 ThinkingBubble

对应 `02-chat-thinking.png`。

用户发送“我想找一款适合油皮的洗面奶，预算 200 元以内”后，AI 区域立即出现豆沙包吉祥物和“正在思考中...”文案。它不是单独的系统 toast，而是当前 AI 回复节点的流式状态。

实现上对应 SSE `thinking`，`payload.message` 可驱动阶段文案，例如“正在理解需求”“正在生成购买标准”“正在检索商品”。注意不要每个 thinking 阶段都新增一条消息，应更新同一个当前 AI 节点。

### 4.3 ClarificationCard

对应 `03-clarification-card.png`。

澄清卡用于缺少关键槽位时追问，设计目标是“只问一个最有价值的问题”。例如当前图中追问“请问你的肤质是？”，并给出“油性、干性、混合性、敏感性”四个 chip。底部保留“也可以直接输入补充”，避免用户被选项限制住。

实现上对应 SSE `clarification` / `ClarificationCard`。它应该是 inline card，完整展示问题和候选选项。用户点 chip 后可直接发送回答；如果用户手输，也进入同一轮澄清补全流程。

### 4.4 CriteriaSummaryCard

对应 `04-criteria-summary-card.png`。

购买标准卡不是展示模型中间过程，而是把用户需求翻译成可确认的决策标准。当前图用“已理解你的需求”作为标题，并用类似决策矩阵的结构展示：

| 槽位 | 示例值 | 设计目的 |
|---|---|---|
| 核心诉求 | 洁面类 | 明确用户到底在买什么。 |
| 状态 | 油性肌肤 | 明确适配人群或使用条件。 |
| 约束 | 200 元以内 | 明确硬性预算/价格边界。 |
| 频次 | 日常护肤 | 明确使用场景和频率。 |
| 排除项 | 不要含酒精 | 明确风险偏好和反向条件。 |

主卡只展示摘要和“修改标准”入口，不展示权重、长字段、全部候选值。这样卡片在聊天流中足够轻，但用户仍然知道 AI 的理解是否正确。

实现上对应 SSE `criteria_card` / `CriteriaSummaryCard`。`summary.chips` 用于主卡；`detail`、`quick_actions` 和可编辑字段进入 `05-criteria-edit-sheet.png` 所示底板。主卡的稳定 key 使用 `node_id` 或 `criteria_id`，更新时应 upsert，避免列表抖动。

### 4.5 CriteriaEditSheet

对应 `05-criteria-edit-sheet.png`。

编辑底板承接 CriteriaSummaryCard 的“修改标准”。图中按“基础信息 / 适用人群 / 筛选条件”分组，并提供品类、预算等字段，以及“再便宜一点、温和亲肤、大容量、注重品牌”等 quick action。

设计上它解决两个问题：一是用户可以精确修正结构化标准；二是用户不必理解复杂筛选表单，也能通过 quick action 快速改条件。保存后触发 `criteria_patch`，后端重新推荐。

### 4.6 ProductRecommendationStrip

对应 `06-product-recommendation-strip.png`。

聊天流中的商品推荐不是完整挑选页，而是一个 `ProductRecommendationStrip`。它显示 AI 的简短说明和几张缩小商品卡，支持横向滑动快速预览，作用是告诉用户“已经找到一组候选商品”，并提供进入挑选模式的入口。

缩略卡只展示商品图、品牌、名称、价格和 1 到 2 个核心标签。这里不要展示完整参数、长推荐理由或证据，避免聊天流变成商品列表。

实现上多个 SSE `product_card` 必须通过同一个 `deck_id` 聚合为一个推荐带节点，内部按 `rank` 排序。点击推荐带或其中任一商品卡，进入 `07-product-swipe-mode.png` 所示的独立挑选模式。

### 4.7 ProductSwipeMode

对应 `07-product-swipe-mode.png`。

ProductSwipeMode 是正式挑选界面，不在聊天流内直接展开。页面一次聚焦一个商品，顶部保留返回和历史/撤销入口，主体展示商品卡与核心标签，底部用明确的 dislike / like 操作承载左滑、右滑心智。

实现上它应复用 `deck_id` 的同一组商品数据，并保留当前 index。左滑可发送 `feedback:not_interested`，右滑可记录喜欢/收藏。点击商品图片或主卡区域进入 `08-product-hero-detail.png` 所示的大图详情；返回后仍回到原 deck 和当前卡片位置。

### 4.8 ProductHeroDetail

对应 `08-product-hero-detail.png`。

商品大图详情是从 Swipe 模式进入的一级商品详情。它以商品大图作为第一视觉，叠加商品名称、价格和当前推荐标签，让用户在不离开挑选语境的情况下认真查看单个商品。

页面底部保留 dislike / like 操作，与 Swipe 模式一致。右上角更多入口打开 `10-product-evidence-overlay.png`；向下滑动进入 `09-product-scroll-detail.png` 所示的深层详情内容。

### 4.9 ProductScrollDetail

对应 `09-product-scroll-detail.png`。

下滑详情页是商品大图详情的纵向延展，用于承载更完整的商品说明、成分/参数、利益点、风险信息和推荐理由。它的角色不是“聊天流里的商品长卡”，而是用户已经对某个商品感兴趣后继续探索的内容层。

这部分可以实现为同一个商品详情页中的下半段内容，也可以在 Compose 中拆成独立 section。关键是不要把这些长详情直接塞回 Swipe 卡或聊天流，否则会破坏主流程的轻量感。

### 4.10 ProductEvidenceOverlay

对应 `10-product-evidence-overlay.png`。

商品证据详情属于 Swipe 推荐卡的解释层，回答的是“为什么推荐这个商品”。它展示用户评价、证据 ID、snippet、数据来源等信息，证据来源应来自当前商品的 `product_card.evidence_refs`。

这个证据层由商品详情右上角更多入口打开。它解释单个推荐商品；最终决策依据解释为什么最终首选某个商品。实现时可以复用证据组件，但入口、标题和返回关系必须区分。

### 4.11 DecisionSummaryCard

对应 `11-decision-summary-card.png`。

最终决策卡要给出可执行结论，而不是再列一堆商品。当前图结构是：首选推荐、价格、推荐理由、不适合情况、查看决策依据、下一步动作。它回答的是“我现在应该选哪个，为什么，有什么坑”。

实现上对应 SSE `final_decision` / `DecisionSummaryCard`。主时间线展示 `verdict`、`why_chips`、`not_for_short` 和“查看依据”。完整的 `why`、`not_for`、`alternatives`、`evidence_refs` 进入证据底板。

### 4.12 DecisionEvidenceSheet

对应 `12-decision-evidence-sheet.png`。

证据底板承接最终决策卡的“查看决策依据”。它把长信息分成“为什么选它”“不适合这些情况”“备选方案”“决策详情”等区块，让用户能追溯 AI 的判断，但不打断主聊天流。

实现上决策证据不作为独立聊天卡出现。`final_decision.evidence_refs` 应打开该底板或详情页展示，避免聊天流被长 snippet 淹没。

### 4.13 InputAttachmentMenu

对应 `13-input-attachment-menu.png`。

输入扩展菜单展示“图片输入、语音输入、实时视频”。其中图片输入与 PRD 的多模态 P1/P2 路径一致；语音输入和实时视频在当前项目裁剪中不是 P0，需要作为 disabled/future affordance 处理，或在首版隐藏。

如果首版保留视觉入口，建议加“即将支持”状态，避免用户点进去遇到空功能。

## 5. 与 PRD / SSE 契约的对应关系

| SSE 事件 | UI 组件 | 对应设计稿 | 备注 |
|---|---|---|---|
| `thinking` | ThinkingBubble | `02-chat-thinking.png` | 更新当前 AI 回复节点，不新增多条 loading。 |
| `clarification` | ClarificationCard | `03-clarification-card.png` | 缺关键槽位时出现，用户回答后进入下一轮。 |
| `criteria_card` | CriteriaSummaryCard | `04-criteria-summary-card.png` | 主流展示摘要，编辑进入 `05-criteria-edit-sheet.png`。 |
| `product_card` | ProductRecommendationStrip / ProductSwipeMode | `06-product-recommendation-strip.png` / `07-product-swipe-mode.png` | 多个商品按 `deck_id` 聚合。聊天流先显示缩略推荐带，点击后进入独立 Swipe 挑选模式。 |
| `product_card.summary.image_url` | ProductHeroDetail / ProductScrollDetail | `08-product-hero-detail.png` / `09-product-scroll-detail.png` | 在 Swipe 模式中点击商品主图进入大图详情，下滑查看更多商品信息。 |
| `product_card.evidence_refs` | ProductEvidenceOverlay | `10-product-evidence-overlay.png` | 从商品详情右上角更多入口打开，解释单个推荐商品为什么被推荐。 |
| `final_decision` | DecisionSummaryCard | `11-decision-summary-card.png` | 展示首选、理由、不适合情况和下一步动作。 |
| `cart_action` | CartActionCard | 暂无最新视觉稿 | PRD/SSE 中存在，后续需要补图或降级为轻量 action chip。 |
| `done` | 无可见组件 | 无 | 关闭 loading，状态机回到 Idle。 |
| `error` | ErrorBubble | 暂无最新视觉稿 | 建议按 inline card 实现，保留重试入口。 |

## 6. 设计落地注意点

1. 最新图中的样例数据以洁面/护肤为主，但组件必须保持多品类通用。字段名称可根据品类映射，例如数码可把“肤质”替换为“设备/性能诉求”，食品可替换为“口味/成分偏好”。
2. `ProductRecommendationStrip` 是聊天流里的 deck 入口，不是完整 Swipe 挑选页。前端实现时要以 `deck_id` 为聚合 key，并在进入 `ProductSwipeMode` 后复用同一组商品数据。
3. 证据卡不是主流程卡片。Swipe 商品证据通过商品详情右上角更多入口进入 `10-product-evidence-overlay.png`；最终决策证据通过“查看决策依据”进入 `12-decision-evidence-sheet.png`。
4. 语音输入和实时视频目前不属于 P0，首版需要隐藏、禁用或标注“即将支持”。
5. 商品推荐现在分为五层：聊天内缩略推荐带、独立 Swipe 挑选模式、商品大图详情、下滑详情、更多证据。不要把五层内容压缩到聊天流的一张大卡里。
6. 主色 coral orange 只用于主操作、强调和品牌状态，不要把所有标签都染成橙色。

## 7. 推荐实现顺序

1. 先实现 `ThinkingBubble`、`ClarificationCard`、`CriteriaSummaryCard`，打通“输入 → 理解 → 澄清/标准确认”。
2. 再实现 `ProductRecommendationStrip` 和 `ProductSwipeMode`，确保多个 `product_card` 可按 `deck_id` 聚合，并能从聊天流进入独立挑选模式。
3. 然后实现 `ProductHeroDetail`、`ProductScrollDetail` 和 `ProductEvidenceOverlay`，补齐单个 Swipe 推荐商品的详情与证据解释。
4. 最后实现 `DecisionSummaryCard` 和 `DecisionEvidenceSheet`，补齐最终可解释决策闭环。
5. `CartActionCard`、语音输入、实时视频入口作为后续补充，不阻塞核心 Demo 链路。
