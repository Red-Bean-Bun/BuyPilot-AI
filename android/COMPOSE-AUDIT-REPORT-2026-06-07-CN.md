# Jetpack Compose 审查报告

**审查目标：** /mnt/disk1/LZJ/project/BuyPilot-AI/android  
**审查日期：** 2026-06-07  
**审查范围：** app/, feature/chat/ 生产 Compose surface；feature/history 仅 state/viewmodel 边界核对（当前无生产 @Composable）  
**排除范围：** core/* 的 UI 模式评分、测试源码、构建产物；core/model 仅作为 feature/chat 参数稳定性输入参考  
**置信度：** 高  
**总体评分：** 64/100

---

## 评分卡

| 类别 | 得分 | 权重 | 状态 | 说明 |
|------|------|------|------|------|
| 性能 | 7/10 | 35% | 良好 | Strong Skipping 已启用，动画模式正确；但存在 composition 期间的副作用式缓存、实例重建和不稳定参数 `equals()` 成本 |
| 状态管理 | 7/10 | 25% | 良好 | UDF 和生命周期收集正确；两个巨型 composable 中局部状态过多 |
| 副作用 | 6/10 | 20% | 需改进 | composition 中存在副作用式缓存更新；多处 lambda 闭包捕获过期 |
| 可组合 API 质量 | 5/10 | 20% | 需改进 | 30+ 个可复用 composable 缺少 `modifier` 参数；254 个 composable 仅有 3 个 @Preview |

---

## 二次 Review 补充

- `ChatTimeline.kt:1546-1550` 使用的是普通 `mutableMapOf`，不是 Compose snapshot state；因此不应严格定性为 snapshot backwards write 或已确认的 recomposition loop。它仍然是 composition body 中的副作用式缓存更新，应该修，但严重性从“临界 performance bug”降为“重大副作用/可维护性风险”。
- 编译器 CSV 的 named-only skippability 需要按 `sum(skippable) / sum(restartable)` 计算。chat 模块实际为 `226/226 = 100%`，不是 `226/235 = 96.2%`；模块级 `341/702 = 48.6%` 仍成立。
- `feature/history` 当前没有生产 Compose UI，也没有 `build/compose_audit` 产物；`HistoryUiState` 缺少 `@Immutable` 不应作为 P1 Compose 性能扣分，只作为未来接入 Compose UI 时的注意项。
- `core/model` 是纯 model 模块，当前没有 Compose runtime 依赖；不要为了 `@Immutable` 直接污染 core 边界。优先在 `feature:chat` 做 UI model 转换、引入 immutable collections，或使用 Compose compiler stability configuration 标记可信模型。

## 关键发现

### 1. 副作用：composition body 中的副作用式缓存更新

**影响：** `SystemClock.uptimeMillis()` 每次 recomposition 返回不同值；composition 期间更新普通 `mutableMapOf` 虽不会触发 Compose snapshot recomposition loop，但仍违反 composition 应尽量无副作用的原则，容易让 thinking handoff 计时和 UI 生命周期耦合不清。

**证据：** `feature/chat/.../ChatTimeline.kt:1546-1550` — 在 `mutableMapOf` 上调用 `putIfAbsent`/`retainAll`，同时内联读取 `SystemClock.uptimeMillis()`。

**修复方向：** 将时间戳记录和清理移入 `LaunchedEffect(item.key, currentThinkingKeys)`，或改成以 `item.nodes` 派生的稳定 first-seen map，避免 composition body 写缓存。

**参考文档：** 
- <https://developer.android.com/develop/ui/compose/side-effects#side-effects-in-composition>
- <https://developer.android.com/develop/ui/compose/performance/bestpractices>

---

### 2. 副作用：长生命周期 effect / AndroidView listener 中的 lambda 闭包捕获过期

**影响：** 在 `LaunchedEffect`/`DisposableEffect` 或 `AndroidView(factory=...)` listener 中直接捕获的回调，在父级 recomposition 生成新 lambda 实例时，会调用过期的闭包。

**证据：** 
- `ChatTimeline.kt:2979-2984` (`onCardEntered`)
- `ChatTimeline.kt:3048-3052` (`onCardDismissed`)
- `ChatTimeline.kt:1534-1538` (`onAssistantTurnVisualActiveChange`)
- `BuyPilotChatScreen.kt:8030-8038` (`onDismiss`)
- `BuyPilotChatScreen.kt:4076-4078` (`onRequestProductDetail`)
- `ProductSwipeComponents.kt:451-487` (`CardStackListener` 捕获 `onSwiped` / `onStackPositionChanged`)

**修复方向：** 使用 `rememberUpdatedState` 包装每个回调，并在协程/onDispose 内部读取 `latest*` 委托。

**参考文档：** <https://developer.android.com/develop/ui/compose/side-effects#rememberupdatedstate>

---

### 3. 性能：composable body 中的实例重建问题

**影响：** 在 Strong Skipping 模式下，每次 recomposition 都会对不稳定参数调用 `==`。在 composable body 中分配的对象（`.toSet()`、`listOf(...)`、`Regex(...)`）始终与上一帧的值 `!=`，导致 skipping 失效。

**证据：** 
- `BuyPilotChatScreen.kt:421` (`stringArrayResource(...).toSet()`)
- `BuyPilotChatScreen.kt:3961` (`swipedProductIds.orEmpty().toSet()`)
- `BuyPilotChatScreen.kt:3421-3426` (字符串处理链)
- `EvidenceOverlay.kt:751` (`Regex("""\s+""")`)
- `MarkdownRendering.kt:98-119,188-206` (高频文本清理/markdown 判定 helper 中即时构造 `Regex`)
- `CriteriaComponents.kt:123,133,425` (`listOf(...)` 用于渐变颜色)

**修复方向：** 使用 `remember` 包装分配，并以稳定输入作为 key；将昂贵的字符串处理移到 presenter/ViewModel。

**参考文档：** <https://developer.android.com/develop/ui/compose/performance/stability/strongskipping>

---

## Performance Bug 分层分析

Performance bug 的判定标准不是"有没有违反最佳实践"，而是"用户能不能感受到掉帧或卡顿"。按此标准分为三层。

### 第一层：已确认真实 performance bug（0 处）

本次二次 review 没有发现可以仅凭当前源码确认的“必然掉帧 / recomposition loop”级别 performance bug。原报告中的 `ChatTimeline.kt:1546-1550` 是普通 `mutableMapOf` 写入，不是 snapshot state 写入，因此不按 snapshot backwards write 定性。

---

### 第二层：特定场景下的性能 / 副作用问题（3 处）

**`ChatTimeline.kt:1546-1550`** — composition body 中的副作用式计时缓存更新

```kotlin
val nowMs = SystemClock.uptimeMillis()  // 每次 recomposition 值不同
currentThinkingKeys.forEach { key ->
    thinkingFirstSeenAtMs.putIfAbsent(key, nowMs)  // 写普通 mutableMap 缓存
}
thinkingFirstSeenAtMs.keys.retainAll(currentThinkingKeys)  // 写普通 mutableMap 缓存
```

这不是 snapshot backwards write，但仍是 composition body 中的副作用式缓存更新。thinking animation 和 reveal handoff 的计时逻辑最好放在 effect 或明确的 state holder 中，避免 recomposition 次数改变 first-seen 行为。

**修复**：移入 `LaunchedEffect(item.key, currentThinkingKeys)`，或由 `remember(item.key)` 创建专门 holder 并通过 effect 同步 keys。

---

**`TimelineNodeContent` 44+ 参数 + 深层 equals**

这个 composable 在 `LazyColumn` 的每个可见 item 中调用。在 streaming 期间，父级 `ChatUiState` 频繁更新（每个 text_delta 事件），导致：
- 44 个参数中约 10 个是不稳定集合类型（`List`、`Map`、`Set`）
- SSM 下每帧对每个不稳定参数调用 `equals()`
- `ChatUiState.nodes` 可能有几十个元素 — 深层 `equals()` 开销随列表长度线性增长
- 如果 `equals()` 判定为不等，整个子树 recompose

**影响**：streaming 密集期可能掉帧。不是每帧都卡，但在商品卡片多的 timeline 中可感知。

**修复**：将 44 个参数分组为 `@Immutable` data class 减少参数数量；或对高频变化的参数使用 `kotlinx.collections.immutable` 的 `PersistentList`/`PersistentMap` 获得 O(log N) 的结构比较。

---

**`BuyPilotChatScreen.kt:421`** — `.toSet()` 无 `remember`

```kotlin
val defaultSkinTypeOptions = stringArrayResource(R.array.default_skin_type_options).toSet()
```

每次 recomposition 分配新 `Set`。这个 Set 如果作为参数传给下游 composable，在 SSM 下 `==` 永远失败，下游无法 skip。

**影响**：取决于下游使用方式。如果下游 composable 有子组件依赖这个值，会导致不必要的 recomposition。

**修复**：先取 `val options = stringArrayResource(...)`，再用 `remember(options) { options.toSet() }` 缓存派生 Set。

---

### 第三层：hygiene 问题，不是 bug（其余问题）

| 问题 | 为什么不是 bug |
|------|--------------|
| 多处 stale lambda capture | 运行时 bug（逻辑错误），不是 performance bug |
| `Regex("\s+")` 在 composable 中分配 | 编译极快，影响纳秒级 |
| 字符串处理链无 `remember` | 只在 FinalDecision 渲染时触发，非 hot path |
| 40+ 局部状态变量 | 架构 smell，但 Compose compiler 的 skippable 分析会缓解 |
| 缺少 `@Preview` | 开发效率问题，不是运行时性能 |
| 缺少 modifier 参数 | API 质量问题 |
| 缺少 baseline profiles | 冷启动优化，不是每帧性能 |

---

**结论**：没有确认到必然的 performance bug；最需要立即修的是副作用正确性和实例重建：`ChatTimeline` 的 composition body 缓存更新、stale callback、streaming 期间的深层 equals 开销，以及几个可低成本 `remember` / 顶层常量化的分配点。

---

## 分类详情

### 性能 — 7/10

#### 天花板检查

- **Strong Skipping：** 已启用 (Kotlin 2.1.0, Compose Compiler plugin)
- **应用的天花板表：** SSM-on — `skippable%` 趋近 ~100%；约束因素是实例重建和不稳定参数的 `equals()` 成本
- **模块级 `skippable%`：** app = 8/15 = 53.3%, chat = 341/702 = 48.6%
- **仅命名 composable 的 `skippable%`：** app = 4/4 = 100%, chat = 226/226 = 100%（235 个 named 里有 9 个非 restartable helper，不进入 `sum(skippable) / sum(restartable)` 分母）
- **编译器检测的不稳定共享类型：** chat 模块 7 个 (ChatViewModel, TtsManager, Criteria, DecisionEvidence, $serializer, ProductCardViewHolder, TurnNodeVisibilityState)，app 模块 0 个
- **SSM-on 约束证据：** named-only skippability 已达 100%，约束不再是“能否 skip”，而是实例重建 churn（`.toSet()`、`listOf(...)`、`Regex(...)`）和不稳定集合 / payload 参数的 `equals()` 成本。
- **定性得分：** 7/10
- **天花板：** 上限 8 (仅命名 skippable% ≥ 95%，但存在可观察的实例重建)
- **应用得分：** 7/10

#### 做得好的地方

- `LazyColumn` 同时使用 `key = { it.key }` 和 `contentType = { it.timelineContentType }` — 异构列表的最佳实践。
- `Animatable` 始终保存在 `remember { Animatable(...) }` 中，并由 `LaunchedEffect(key)` 驱动 — 正确的取消和状态存活。
- `rememberInfiniteTransition` 具有有意义的标签，并正确作用域在渲染动画的 composable 内部（离屏时停止）。
- 动画值通过 lambda modifier 读取（`Modifier.graphicsLayer { }`、`Modifier.offset { }`）— 每帧工作保持在 draw/layout 阶段，不触发 recomposition。
- `derivedStateOf` 正确用于滚动派生的布尔值（`canScrollBackward`、`canScrollForward`、`isNearTimelineEnd`）。
- 广泛使用类型化状态工厂：`mutableIntStateOf`、`mutableFloatStateOf` — 无自动装箱。
- 所有状态模型类都有 `@Immutable` 注解 (ChatUiState、ChatUiNode 层级、TimelinePresentationState)。
- 使用 `enableEdgeToEdge()` 而非 Accompanist。

#### 扣分项

- `SystemClock.uptimeMillis()` + 普通 mutable map 缓存更新直接在 composition body 中 (ChatTimeline.kt:1546) — 重大副作用式缓存问题；不再按 snapshot backwards write 定性。
- `stringArrayResource(...).toSet()` 每次 recomposition 分配新 Set，未使用 `remember` (BuyPilotChatScreen.kt:421)。
- 带正则的字符串处理管道（`withoutMarkdownMarkup`、`withoutInternalDebugTokens`）每次 recomposition 运行 (BuyPilotChatScreen.kt:3421-3426)。
- `Regex("""\s+""")` 在 composable 调用链中分配 (EvidenceOverlay.kt:751)。
- `TimelineNodeContent` 有 44+ 个参数，包括 `List`、`Map`、`Set` — 即使 data class 有 `@Immutable`，Kotlin 集合接口默认不稳定。在 SSM 下，这意味着每次 recomposition 都会进行深度 `equals()`。
- 核心模型 payload 类（`AgentPayload` 层级）作为 `feature:chat` 参数在编译器报告中表现为不稳定；但 core/model 是纯 model 模块，不建议直接引入 Compose runtime 只为添加 `@Immutable`。
- `HistoryUiState` 缺少 `@Immutable` 仅作为未来接入 Compose UI 的注意项；当前 feature/history 无生产 @Composable，不作为 P1 扣分。
- 无 baseline profiles，无 `ReportDrawnWhen`，release 配置中无 `minifyEnabled`/`shrinkResources`。

#### 证据

- `feature/chat/.../ChatTimeline.kt:1546` — composition body 中的 `SystemClock.uptimeMillis()` + 普通 mutable map 缓存更新 · 参考: <https://developer.android.com/develop/ui/compose/performance/bestpractices>
- `feature/chat/.../BuyPilotChatScreen.kt:421` — 未使用 `remember` 的 `.toSet()` · 参考: <https://developer.android.com/develop/ui/compose/performance/bestpractices>
- `feature/chat/.../BuyPilotChatScreen.kt:3421-3426` — composition 中的字符串处理链 · 参考: <https://developer.android.com/develop/ui/compose/performance/bestpractices>
- `feature/chat/.../EvidenceOverlay.kt:751` — composable 中的 `Regex(...)` 分配 · 参考: <https://developer.android.com/develop/ui/compose/performance/bestpractices>
- `feature/chat/.../ChatTimeline.kt:2046-2089` — `TimelineNodeContent` 有 44+ 个不稳定集合参数 · 参考: <https://developer.android.com/develop/ui/compose/performance/stability/strongskipping>
- `core/model/.../AgentPayload.kt:11-244` — payload data class 在 feature:chat composable 参数中被报告为不稳定；修复应优先选择 UI model / immutable collections / stability config，而不是直接污染 core model · 参考: <https://developer.android.com/develop/ui/compose/performance/stability/fix>

---

### 状态管理 — 7/10

#### 做得好的地方

- ViewModel 正确使用 `MutableStateFlow` 支持属性模式 — 私有可变，公共只读 `StateFlow`。
- UI 中所有 Flow 收集使用 `collectAsStateWithLifecycle()` — 未发现裸 `collectAsState()`。
- ViewModel 作用域在父导航图入口（`hiltViewModel(parentEntry)`）— 跨子路由的单一事实来源。
- `ChatUiState`、`ChatImageAttachmentState`、`ChatCartUiState`、`ChatRetryRequest` 有 `@Immutable` 注解。
- 复杂 UI 状态有自定义 `Saver` 实现（`ChatSheetContentSaver`、`StringSetSaver`、`TimelineRevealSnapshotHolderSaver`）。
- `rememberSaveable` 正确用于滚动位置恢复状态（ChatTimeline 中的路由返回变量）。
- `derivedStateOf` 正确用于计算滚动状态（`isNearTimelineEnd`、`canScrollForward`、`canScrollBackward`）。
- composable 参数中无 `MutableState<T>` 或 `State<T>` 暴露 — 全部使用普通 data class 或原始值。

#### 扣分项

- `BuyPilotChatScreen` 在单个 composable body 中声明 40+ 个局部 `remember`/`rememberSaveable` 状态变量（第 354-397 行）。任何单个状态变化都会触发整个函数作用域的 recomposition。应该将它们分组到 `@Stable` 持有者类中。
- `ChatTimeline` 声明约 30 个局部状态变量（第 355-384 行），其中许多可以折叠为单个 `@Immutable` data class 并使用自定义 `Saver`。
- 几个表示用户可见 UI 的状态使用 `remember` 而非 `rememberSaveable`：
  - `imagePreviewAttachment` (BuyPilotChatScreen.kt:356) — 用户期望在旋转时保持。
  - `pendingClarificationAnswer`、`activeClarificationFlight`、`hiddenFlightMessageKeys` (第 380-382 行) — 旋转时丢失进行中的澄清交互会感觉异常。
- `TimelinePresentationState` 使用 `SharingStarted.Eagerly` 而非 `WhileSubscribed(5_000)` — 次要问题，可接受，因为 ViewModel 始终绑定到可见路由。

#### 证据

- `feature/chat/.../BuyPilotChatScreen.kt:354-397` — 单个 composable 中有 40+ 个局部状态变量 · 参考: <https://developer.android.com/develop/ui/compose/state-hoisting>
- `feature/chat/.../ChatTimeline.kt:355-384` — 30+ 个局部状态变量 · 参考: <https://developer.android.com/develop/ui/compose/state>
- `feature/chat/.../BuyPilotChatScreen.kt:356` — `imagePreviewAttachment` 使用 `remember` 而非 `rememberSaveable` · 参考: <https://developer.android.com/develop/ui/compose/state>
- `feature/chat/.../ChatViewModel.kt:302-308` — 正确的 MutableStateFlow 支持属性模式 · 参考: <https://developer.android.com/topic/libraries/architecture/stateflow>
- `feature/chat/.../ChatRoute.kt:15-16` — 正确的 `collectAsStateWithLifecycle` 使用 · 参考: <https://developer.android.com/develop/ui/compose/state>

---

### 副作用 — 6/10

#### 做得好的地方

- `rememberUpdatedState` 正确用于动画 composable 中的回调：`latestOnRevealComplete`、`latestOnRevealActiveChange`、`latestOnRevealProgress` (MarkdownRendering.kt:525-527)，`latestOnBack` (ProductSwipeComponents.kt:109)，`latestProducts`、`latestOnLike`、`latestOnDislike` (ProductSwipeComponents.kt:310-312)，`latestOnEntered` (TimelineMotion.kt:62,139)。
- `DisposableEffect` 在 `onDispose` 中有正确的清理 — 生命周期观察器 (ChatTimeline.kt:637-665)，reveal 状态清理 (MarkdownRendering.kt:531-535)。
- `rememberCoroutineScope()` 仅用于事件驱动的工作（底部弹窗隐藏动画，BuyPilotChatScreen.kt:498）。
- `snapshotFlow { detailListState.isScrollInProgress }.distinctUntilChanged()` 在 `LaunchedEffect` 内部 — 正确的 State → Flow 转换。
- `Animatable.animateTo` 由 `LaunchedEffect(key)` 驱动 — 正确的取消语义。
- `LaunchedEffect` 的 key 有意义（`nodeKey`、`deckId`、`payload.compareId`、`item.key`）— 正确的重启行为。
- composition body 中无网络/数据库/SharedPreferences 调用。

#### 扣分项

- **重大**：composition body 中的副作用式缓存更新 — `SystemClock.uptimeMillis()` + 普通 mutable map 上的 `putIfAbsent`/`retainAll` (ChatTimeline.kt:1546-1550)。
- **重大**：lambda 闭包捕获过期 — `onCardEntered`、`onCardDismissed` 在 `LaunchedEffect` 中直接捕获，未使用 `rememberUpdatedState` (ChatTimeline.kt:2979-2984, 3048-3052)。
- **重大**：lambda 闭包捕获过期 — `onAssistantTurnVisualActiveChange` 在 `LaunchedEffect` 和 `DisposableEffect.onDispose` 中直接捕获 (ChatTimeline.kt:1534-1538)。
- **重大**：lambda 闭包捕获过期 — `onDismiss` 在 `LaunchedEffect(dismissRequested)` 中直接捕获 (BuyPilotChatScreen.kt:8030-8038)。
- **重大**：lambda 闭包捕获过期 — `onRequestProductDetail` 在 `LaunchedEffect(product.productId)` 中直接捕获 (BuyPilotChatScreen.kt:4076-4078)。
- **重大**：`AndroidView(factory=...)` 中的 `CardStackListener` 捕获 `onSwiped` / `onStackPositionChanged`，factory 不随 recomposition 更新，可能调用旧回调 (ProductSwipeComponents.kt:451-487)。
- **重大**：`LaunchedEffect(Unit)` 捕获变化的值 `showStartupSplash` (StartupSplash.kt:102-105) — delay 值从初始 composition 的闭包中读取。
- **次要**：`rememberUpdatedState` 用作常量持有者（`rememberUpdatedState(1f)`）而非 `remember { mutableStateOf(1f) }` (BuyPilotChatScreen.kt:2554, 2633)。

#### 证据

- `feature/chat/.../ChatTimeline.kt:1546-1550` — composition body 中的普通 mutable map 缓存更新 · 参考: <https://developer.android.com/develop/ui/compose/side-effects#side-effects-in-composition>
- `feature/chat/.../ChatTimeline.kt:2979-2984` — `onCardEntered` 闭包捕获过期 · 参考: <https://developer.android.com/develop/ui/compose/side-effects#rememberupdatedstate>
- `feature/chat/.../ChatTimeline.kt:1534-1538` — DisposableEffect 中 `onAssistantTurnVisualActiveChange` 闭包捕获过期 · 参考: <https://developer.android.com/develop/ui/compose/side-effects#rememberupdatedstate>
- `feature/chat/.../BuyPilotChatScreen.kt:8030-8038` — `onDismiss` 闭包捕获过期 · 参考: <https://developer.android.com/develop/ui/compose/side-effects#rememberupdatedstate>
- `feature/chat/.../BuyPilotChatScreen.kt:4076-4078` — `onRequestProductDetail` 闭包捕获过期 · 参考: <https://developer.android.com/develop/ui/compose/side-effects#rememberupdatedstate>
- `feature/chat/.../ProductSwipeComponents.kt:451-487` — `AndroidView(factory=...)` listener 捕获旧回调 · 参考: <https://developer.android.com/develop/ui/compose/side-effects#rememberupdatedstate>
- `app/.../StartupSplash.kt:102-105` — `LaunchedEffect(Unit)` 捕获变化的 `showStartupSplash` · 参考: <https://developer.android.com/develop/ui/compose/side-effects#launchedeffect>

---

### 可组合 API 质量 — 5/10

#### 做得好的地方

- 可复用 composable 参数中无 `MutableState<T>` 或 `State<T>` — 全部接受普通 data class 或原始值。
- 多个 composable 正确接受并应用 `modifier: Modifier = Modifier`：`ProductImage`、`M3TopAppBarRow`、`ProductHeroCard`、`CompareSelectionTray`、`ProductDeckMarkdownCompareTable`、`MarkdownTextBlock`、`ProductEvidenceOverlayScreen`、`ProductSwipeModeScreen`、`BuyPilotLayeredSplash`、`EdgeFadedLazyRow`、`ClarificationOptionScroller`、`TimelineItemMotion`。
- 组件用途/分层分离清晰：设计令牌 (BuyPilotDesign.kt)、动画组件 (TimelineMotion.kt)、显示组件 (ProductDisplay.kt)、屏幕编排器 (BuyPilotChatScreen.kt)。
- 共享组件一致使用 `internal` 可见性修饰符。

#### 扣分项

- **重大**：30+ 个可复用内部 composable 完全缺少 `modifier` 参数：`CriteriaSummaryCard`、`ScenarioFilterReceiptCard`、`CompareSummaryCard`、`EvidenceLead`、`EvidenceReportSurface`、`ErrorCard`、`CartActionCard`、`SwipeRoundButton`、`AssistantInlineStatus`、`AssistantPendingDots`、`StructuredCardMotion`、`StaggeredRevealMotion`、`ProductDeckArrivalMotion`、`AssistantText`、`StreamingAssistantText`、`BuyPilotChatScreen`、`ProductRecommendationStrip`、`DecisionSummaryCard`、`ProductSwipeTopBar` 等。
- **重大**：254 个 @Composable 函数中仅有 3 个 `@Preview` 注解 — 对于视觉丰富的 UI，预览覆盖率接近零。
- **次要**：动画组件硬编码动画时长，无 `animationSpec` 参数（`StructuredCardMotion`、`StaggeredRevealMotion`、`StreamingAssistantText`、`SwipeRoundButton`）。
- **次要**：设计令牌定义为 `internal object` 常量（`BuyPilotColors`、`BuyPilotDimens`、`BuyPilotType`）而非 `CompositionLocal` 主题系统 — 无法切换到深色模式或动态颜色。
- **次要**：UI 中硬编码 dp/sp 值（`.padding(16.dp)`、`.size(58.dp)`）而非通过 `BuyPilotDimens` 或维度资源路由。
- **次要**：硬编码中文字符串，未使用 `stringResource`：`Text("商品资料")`、`Text("查看决策依据 ›")`、`Text("返回聊天")`、`Text("正在同步购物车")`、`Text("移除")`、`Text("取消")`。
- **次要**：某些 composable 将 `modifier` 放在必需参数之后，而非作为第一个可选参数。

#### 证据

- `feature/chat/.../CriteriaComponents.kt:88` — `CriteriaSummaryCard` 缺少 modifier · 参考: <https://developer.android.com/develop/ui/compose/components>
- `feature/chat/.../BuyPilotChatScreen.kt:8757` — `SwipeRoundButton` 硬编码 `Modifier.size(58.dp)` · 参考: <https://developer.android.com/develop/ui/compose/components>
- `feature/chat/.../BuyPilotChatScreen.kt` — 总共只有 3 个 @Preview 注解 · 参考: <https://developer.android.com/develop/ui/compose/tooling/previews>
- `feature/chat/.../EvidenceOverlay.kt:529` — 硬编码字符串 `Text("商品资料")` · 参考: <https://developer.android.com/develop/ui/compose/resources>
- `feature/chat/.../ChatTimeline.kt:1247-1252` — 硬编码 dp 值而非 BuyPilotDimens · 参考: <https://developer.android.com/develop/ui/compose/designsystems/material3>

---

## 明日修复 Checklist（是否修复）

勾选约定：`[ ]` 未修复，`[x]` 已修复并通过验收。明天先做 **P0/P1**，P2 保留为后续质量债。

| # | 修复? | 优先级 | 类别 | 问题 | 落地文件 | 验收标准 |
|---|------|--------|------|------|----------|----------|
| 1 | [ ] | **P0** | 副作用 | composition body 中更新 thinking first-seen 普通 map 缓存 | `ChatTimeline.kt:1546-1550` | composition body 不再调用 `putIfAbsent`/`retainAll`；计时同步在 `LaunchedEffect` 或 holder 方法中完成 |
| 2 | [ ] | **P0** | 副作用 | `onAssistantTurnVisualActiveChange` 在 effect / dispose 中捕获旧回调 | `ChatTimeline.kt:1534-1538` | 使用 `rememberUpdatedState`；`LaunchedEffect` 和 `onDispose` 都调用 latest 回调 |
| 3 | [ ] | **P0** | 副作用 | `onCardEntered` 在 `LaunchedEffect` 中捕获旧回调 | `ChatTimeline.kt:2979-2984` | 使用 `rememberUpdatedState`；延迟后调用 latest 回调 |
| 4 | [ ] | **P0** | 副作用 | `onCardDismissed` 在 `LaunchedEffect` 中捕获旧回调 | `ChatTimeline.kt:3048-3052` | 使用 `rememberUpdatedState`；延迟后调用 latest 回调 |
| 5 | [ ] | **P0** | 副作用 | `CompareActionPickerDialog` 的 `onDismiss` 在 `LaunchedEffect(dismissRequested)` 中捕获旧回调 | `BuyPilotChatScreen.kt:8030-8038` | 使用 `rememberUpdatedState(onDismiss)`；退出动画后调用 latest 回调 |
| 6 | [ ] | **P0** | 副作用 | 商品详情页 `onRequestProductDetail` 在 `LaunchedEffect(product.productId)` 中捕获旧回调 | `BuyPilotChatScreen.kt:4076-4078` | 使用 `rememberUpdatedState(onRequestProductDetail)`；productId 变化时调用 latest 回调 |
| 7 | [ ] | **P0** | 副作用 | `AndroidView(factory=...)` 的 `CardStackListener` 捕获旧 `onSwiped` / `onStackPositionChanged` | `ProductSwipeComponents.kt:451-487` | 在 composable 中保存 latest callbacks；factory listener 调用 latest；手动/自动 swipe 行为不变 |
| 8 | [ ] | **P1** | 副作用 | `StartupSplash` 的 `LaunchedEffect(Unit)` 捕获 `showStartupSplash` 初始值 | `StartupSplash.kt:102-105` | key 改为 `showStartupSplash` 或用 `rememberUpdatedState`；启动延迟逻辑不被旧值锁死 |
| 9 | [ ] | **P1** | 性能 | `stringArrayResource(...).toSet()` 每次 recomposition 分配 | `BuyPilotChatScreen.kt:421` | 先调用 `stringArrayResource`，再用 `remember(array) { array.toSet() }` 缓存；下游收到稳定 Set |
| 10 | [ ] | **P1** | 性能 | 商品详情页 `swipedProductIds.orEmpty().toSet()` 每次 recomposition 分配 | `BuyPilotChatScreen.kt:3961` | 用 `remember(state.productSwipeStates, deckId)` 或 presenter 派生缓存 |
| 11 | [ ] | **P1** | 性能 | `DecisionSummaryCard` 的 `whyItems` 字符串清理链每次 recomposition 运行 | `BuyPilotChatScreen.kt:3421-3426` | 用 `remember(payload.why)` 缓存；输出列表与原逻辑一致 |
| 12 | [ ] | **P1** | 性能 | `EvidenceOverlay` snippet 处理中即时构造 whitespace `Regex` | `EvidenceOverlay.kt:751` | 提取顶层 `private val`；相关文本清理测试/手测输出不变 |
| 13 | [ ] | **P1** | 性能 | Markdown 高频清理 / 判定 helper 中多处即时构造 `Regex` | `MarkdownRendering.kt:98-119,188-206` | 高频 regex 提取顶层常量；streaming / final markdown 渲染结果不变 |
| 14 | [ ] | **P1** | 性能 | compare / decision 文本 helper 中多处即时构造 `Regex` | `CompareComponents.kt:1534-1582`, `BuyPilotChatScreen.kt:8222-8240` | 热路径 regex 提取顶层常量；对比卡和决策卡文案不变 |
| 15 | [ ] | **P1** | 性能 | `TimelineNodeContent` 44+ 参数含不稳定 `List`/`Map`/`Set`，streaming 时 `equals()` 成本高 | `ChatTimeline.kt:2046-2089` | 至少先分组高频参数为稳定 render props；复跑 compiler report，确认无新增 non-skippable named composable |
| 16 | [ ] | **P1** | 性能 | core payload 在 feature:chat 参数中表现为不稳定 | `AgentPayload.kt:11-244`, `feature/chat` UI model 边界 | 不直接给 core/model 加 Compose runtime；选择 UI model / immutable collections / stability config 之一并复跑 compiler report |
| 17 | [ ] | **P1** | 状态 | `imagePreviewAttachment` 旋转后丢失 | `BuyPilotChatScreen.kt:356` | 改为 `rememberSaveable` + saver；旋转后图片预览目标仍在 |
| 18 | [ ] | **P1** | 状态 | 澄清交互中 `pendingClarificationAnswer` / `activeClarificationFlight` / `hiddenFlightMessageKeys` 旋转后丢失 | `BuyPilotChatScreen.kt:380-382` | 改为 `rememberSaveable` + saver；旋转后澄清动画/隐藏状态不重置 |
| 19 | [ ] | **P2** | 状态 | `BuyPilotChatScreen` 40+ 局部状态集中在一个 composable | `BuyPilotChatScreen.kt:354-397` | 抽出 `@Stable` holder 或拆分状态作用域；不改变现有交互 |
| 20 | [ ] | **P2** | 状态 | `ChatTimeline` 30+ 局部状态集中在一个 composable | `ChatTimeline.kt:355-384` | 折叠为 holder / snapshot state owner；滚动恢复和 reveal 行为不回退 |
| 21 | [ ] | **P2** | API | 30+ 可复用 composable 缺少 `modifier` 参数 | `CriteriaComponents.kt`, `CompareComponents.kt`, `EvidenceOverlay.kt`, `BuyPilotChatScreen.kt` | 主要复用组件补 `modifier: Modifier = Modifier`；调用点编译通过 |
| 22 | [ ] | **P2** | API | 主要视觉组件 preview 覆盖不足 | 全仓库 | 为核心卡片/动效组件补 preview；`rg -n "@Preview"` 明显增加 |
| 23 | [ ] | **P2** | API | 动画组件硬编码时长，缺少可配置 `animationSpec` | `TimelineMotion.kt`, `BuyPilotChatScreen.kt:8757` | 共享动画组件暴露合理 `animationSpec` 或集中常量；默认行为不变 |
| 24 | [ ] | **P2** | API | 设计令牌为 `internal object`，不是 tree-scoped 主题 | `BuyPilotDesign.kt` | 明确是否迁移 CompositionLocal；若不迁移，在报告中标注为设计系统债务 |
| 25 | [ ] | **P2** | API | 大量硬编码 dp/sp 未走 token | 多处 UI 文件 | 高复用组件优先改为 token；一次不做全量机械替换 |
| 26 | [ ] | **P2** | API | 硬编码中文字符串未走 `stringResource` | `EvidenceOverlay.kt`, `BuyPilotChatScreen.kt`, `CompareComponents.kt` | 用户可见固定文案提取到 strings；动态 LLM 文案不提取 |
| 27 | [ ] | **P2** | API | 部分 composable 的 `modifier` 参数顺序不符合约定 | `ProductDisplay.kt:41`, `BuyPilotChatScreen.kt:3059,3318` | `modifier` 移到第一个可选参数位置；调用点编译通过 |
| 28 | [ ] | **P2** | 状态 | `TimelinePresentationState` 使用 `SharingStarted.Eagerly` | `ChatViewModel.kt:309-317` | 评估并改为 `WhileSubscribed(5_000)`；确认导航返回不丢 timeline presentation |
| 29 | [ ] | **P2** | 副作用 | `rememberUpdatedState(1f)` 用作常量 state holder | `BuyPilotChatScreen.kt:2554,2633` | 改为更直接的 stable value/state；无行为变化 |
| 30 | [ ] | **P2** | 发布性能 | 无 baseline profiles | 全仓库 | 增加 baseline profile / macrobenchmark 路径或明确不做 |
| 31 | [ ] | **P2** | 发布性能 | 无 `ReportDrawnWhen` / fully drawn signal | `MainActivity.kt` | 添加首屏 ready 条件；启动指标可读 |
| 32 | [ ] | **P2** | 发布性能 | release 未启用 R8 / resource shrinking | `app/build.gradle.kts` | `isMinifyEnabled` / `isShrinkResources` 策略明确；release 构建通过 |
| 33 | [ ] | **观察** | 范围修正 | `HistoryUiState` 当前不是生产 Compose 参数 | `feature/history` | 不作为明日 P1 修复；若未来新增 history Compose UI，再处理稳定性注解 |

### 建议执行顺序

1. 先修 #1-#8：全部是副作用正确性，改动小、收益明确。
2. 再修 #9-#14：把低成本分配点用 `remember` 或顶层 `Regex` 常量收掉。
3. 评估 #15-#16：只做不会污染 core 边界的稳定性方案，避免为了分数引入错误依赖。
4. #17-#18 若时间够，补旋转恢复；#19 之后属于结构性优化，不建议明天硬重构。

### 明日复验命令

```bash
cd /mnt/disk1/LZJ/project/BuyPilot-AI/android
timeout 600s ./gradlew :feature:chat:compileReleaseKotlin --init-script /mnt/disk1/LZJ/project/compose_skill/jetpack-compose-audit/skills/jetpack-compose-audit/scripts/compose-reports.init.gradle --no-daemon --quiet
timeout 600s ./gradlew :app:compileReleaseKotlin --init-script /mnt/disk1/LZJ/project/compose_skill/jetpack-compose-audit/skills/jetpack-compose-audit/scripts/compose-reports.init.gradle --no-daemon --quiet
timeout 180s ./gradlew testDebugUnitTest
```

---

## 注意事项和限制

- 审查覆盖了 app/、feature/chat/ 的生产 Compose surface；feature/history 当前没有生产 @Composable，仅做 state/viewmodel 边界核对。核心模块（core/model、core/data、core/network、core/database、core/common）未做 UI 模式评分。
- 置信度：**高** — 充足的 Compose 表面积，读取了多个代表性模块/文件，生成了编译器报告。
- 所有类别都有充足的审查表面积；无类别标记为 N/A。
- 相邻覆盖说明：
  - **UI 测试：** 未观察到（未发现 `createComposeRule`、`createAndroidComposeRule`、`onNodeWithText` 使用）。
  - **截图测试：** 未观察到。
  - **焦点/键盘导航：** `FocusRequester` 用于聊天输入框的键盘管理；未观察到 D-pad/TV/桌面焦点处理。
  - **KMP/CMP：** 不存在 — 纯 Android 项目。
- **Strong Skipping 模式：** 所有模块已启用（Kotlin 2.1.0 默认）。无需显式 opt-in 标志。
- **权重选择：** 应用默认 35/25/20/20。这是一个聊天密集型应用，有大量动画，因此默认的性能权重是合适的。
- **归一化：** 不需要 — 所有类别都有得分。
- **使用编译器诊断：** **是**。通过 `--init-script` 和 `reportsDestination` 为 app 和 feature:chat 模块生成报告。模块级 `skippable%` 和仅命名 `skippable%` 都可用。应用 SSM-on 天花板表。App 模块：4/4 个 restartable named composable 可跳过。Chat 模块：226/226 个 restartable named composable 可跳过；235 个 named 中另有 9 个非 restartable helper。Chat 模块中有 7 个不稳定类（ChatViewModel、TtsManager、Criteria、DecisionEvidence、$serializer、ProductCardViewHolder、TurnNodeVisibilityState）。

---

## 后续建议

- 如果仓库也显示可能存在设计系统或 Material 3 问题，运行 `material-3` 审查 — 当前使用 `internal object` 常量作为设计令牌而非 `CompositionLocal` 主题系统，表明 Material 3 集成可能较浅。
- 运行 `compose-agent focus on testing` — 未发现 UI 测试、截图测试或 Compose 测试规则。考虑到有 254 个 composable，这是一个显著的差距。
- 缺少 baseline profiles、`ReportDrawnWhen`、`minifyEnabled`/`shrinkResources` — 关于发布性能工具链的后续审查将补充本次审计。
