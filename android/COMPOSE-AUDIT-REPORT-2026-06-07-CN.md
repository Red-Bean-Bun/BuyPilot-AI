# Jetpack Compose 审查报告

**审查目标：** /mnt/disk1/LZJ/project/BuyPilot-AI/android  
**审查日期：** 2026-06-07  
**审查范围：** app/, feature/chat/ 生产 Compose surface；feature/history 仅 state/viewmodel 边界核对（当前无生产 @Composable）  
**排除范围：** core/* 的 UI 模式评分、测试源码、构建产物；core/model 仅作为 feature/chat 参数稳定性输入参考  
**置信度：** 高  
**总体评分：** 64/100（原始审查）；第一批整改后暂估 72/100；第二批整改后暂估 76/100；第 10 批 Agent 1-3 后暂估 78/100；B 批 API 小步收口后仍暂估 78/100

---

## 当前整改状态总览（重新梳理）

本节是当前应优先阅读的版本；后文保留原始审查发现、批次复验记录和逐项 checklist 作为证据链。33 项中，当前 **21 项已完成**，**9 项部分完成**，**2 项未执行**，**1 项调整为观察项**。整体从“副作用与性能风险优先修”推进到“API 质量和发布性能收尾”阶段。

### 已明显改进

| 类别 | 对应项 | 改进内容 | 为什么能算闭环 |
|------|--------|----------|----------------|
| 副作用正确性 | #1-#8、#29、#31 | composition body 写缓存移入 effect；长生命周期 effect / AndroidView listener 改 latest callback；启动 fully drawn 信号接入 | 这些是明确的运行时正确性问题，修复点集中、验收标准清晰，已通过 release compile 和单测 |
| 性能实例重建 | #9-#15 | `.toSet()`、高频 Regex、字符串清理链、`TimelineNodeContent` 直接接收大集合等问题已收口 | 主要 hot path 不再反复创建关键对象；`TimelineNodeContent` 已改为 `node + props + inputs + callbacks` |
| 用户可见状态恢复 | #17 | 图片预览状态改为 `rememberSaveable` + saver | 旋转后图片预览目标可恢复，不再直接丢失 |
| API 小步改进 | #23、#27 | 动画组件暴露可选 `animationSpec`；部分 modifier 参数顺序修正 | 组件 API 形状已改善，不改变默认行为 |
| 生命周期/收集策略 | #28 | `TimelinePresentationState` 改为 `SharingStarted.WhileSubscribed(5_000)` | 无订阅时不再 eager 收集，符合 ViewModel UI state 生命周期预期 |

### 部分改进但未闭环

| 类别 | 对应项 | 当前已做 | 还没完全改的原因 |
|------|--------|----------|------------------|
| core payload 稳定性边界 | #16 | 第 10 批后 compiler report 曾显示 app/chat 无 unstable class；未污染 core/model | 不能为了 Compose 直接给 core/model 加 Compose runtime 依赖；更合理的是 feature:chat UI model、immutable collections 或 stability config，需要单独设计 |
| 澄清交互状态 | #18 | 保存了 `ClarificationFlightSemantic`、pending answer、hidden message keys 等语义状态 | 运行中 `Offset` / `Size` 动画坐标不适合跨旋转恢复，强行保存会带来重复发送或错误飞行动画 |
| 巨型 composable 状态作用域 | #19/#20 | 抽出了 `ClarificationInteractionState`、route return / clarification freeze holder | `BuyPilotChatScreen` / `ChatTimeline` 仍很大；继续拆需要架构级改造，风险高，不适合和 API 文案清理混做 |
| modifier 覆盖 | #21 | Criteria / Compare / Evidence / BuyPilotChatScreen 中一批核心组件已补 modifier | 仍有 `AssistantText`、`StreamingAssistantText`、`ProductDeckArrivalMotion`、`ProductSwipeTopBar` 等未系统处理；需要按文件小批量推进 |
| Preview 覆盖 | #22 | `@Preview` 从 3 个增至 7 个 | UI 面很宽，现有 preview 只覆盖 Criteria / Compare / Evidence 核心视觉组件；还缺聊天主路径、商品详情、购物车状态 |
| 设计令牌策略 | #24/#25 | `BuyPilotDesign.kt` 明确静态 token 策略；触达组件开始用 `BuyPilotDimens` / `BuyPilotType` | 全量 CompositionLocal 主题迁移和 dp/sp token 化会影响大量 UI，当前不是低风险收尾任务 |
| 文案资源化 | #26 | Criteria / Compare / Evidence 以及 B 批候选区、决策卡、图片预览等固定文案已进 `strings.xml` | 购物车回执、商品详情、证据 sheet 仍有大量动态拼接/映射文案；全量抽资源需要格式化资源和回归测试，不能机械替换 |

### 尚未执行

| 类别 | 对应项 | 未改内容 | 为什么没改 |
|------|--------|----------|------------|
| Baseline Profile | #30 | 未增加 baseline profile / macrobenchmark 路径 | 需要新建 profile/benchmark 流程并验证启动路径，属于发布性能工程，不应混入 UI API 清理批次 |
| R8 / resource shrinking | #32 | release 未启用 `minifyEnabled` / `shrinkResources` 策略 | 需要确认混淆规则、资源反射/动态引用和 `:app:assembleRelease` 结果；贸然开启可能影响演示稳定性 |

### 不再作为修复项

| 项 | 结论 | 原因 |
|----|------|------|
| #33 `HistoryUiState` | 观察项，已关闭 | `feature/history` 当前没有生产 Compose UI；未来如果接入 Compose UI，再处理稳定性注解 |

### 当前判断

- **评分为什么没继续上调：** P0/P1 的真实风险大多已收口，但 API 质量仍只有局部改善，preview、文案资源化、modifier、设计 token、发布性能策略都没完全闭环，所以总分仍暂估 `78/100`。
- **下一步最值得做：** 如果目标是比赛演示稳定，先做 #32 的 release 策略评估和 assembleRelease 验证；如果目标是压 Android 代码膨胀，继续按 B 的方式小批处理 #21/#26，不要再开大范围机械重构。
- **不建议现在做：** 全量拆 `BuyPilotChatScreen` / `ChatTimeline` 或全量 CompositionLocal 主题迁移。它们确实是债务，但当前回归面明显大于收益。

---

## 第一批整改复验（2026-06-07）

**整改范围：** 按 `doc/parallel-agents/08-compose-audit-fixes/` 并行执行，优先处理 P0 副作用正确性和 P1 低风险实例重建点。

**已完成：**

- P0 #1-#7 全部完成：`ChatTimeline` composition body 写缓存已移入 `LaunchedEffect`；长生命周期 effect / `AndroidView(factory)` listener 均改为 latest callback。
- P1 #8-#13 完成：`StartupSplash` effect key、`.toSet()` 派生缓存、`whyItems` 清理链缓存、`EvidenceOverlay` / `MarkdownRendering` regex 常量化、`CriteriaComponents` 渐变颜色常量化均已落地。
- P1 #14 部分完成：`CompareComponents.kt:1534-1582` 已常量化；`BuyPilotChatScreen.kt:8222-8240` 的 decision helper regex 尚未处理。
- Strong Skipping 口径已修正：unstable 参数按实例相等 `===` 比较；stable 大状态仍可能有 `equals()` 成本。

**复验命令：**

```bash
cd /mnt/disk1/LZJ/project/BuyPilot-AI/android
timeout 600s ./gradlew :feature:chat:compileReleaseKotlin :app:compileReleaseKotlin --init-script /mnt/disk1/LZJ/project/compose_skill/jetpack-compose-audit/skills/jetpack-compose-audit/scripts/compose-reports.init.gradle --no-daemon --quiet
timeout 180s ./gradlew testDebugUnitTest
```

**复验结果：** 以上命令均通过。Compose compiler diagnostics 复算：app named-only `3/3 = 100%`；chat named-only `217/217 = 100%`；当前仅剩 `ProductCardViewHolder` 被报告为 unstable class。

## 第二批整改复验（2026-06-07）

**整改范围：** 按 `doc/parallel-agents/09-compose-audit-next-fixes/` 并行执行，聚焦剩余 P1、低风险 P2 和发布性能信号。

**已完成 / 兜底：**

- P1 #14 完成：`CompareComponents` 与 `BuyPilotChatScreen` decision helper regex 已提取为顶层常量；`BuyPilotChatScreen` 中仅保留价格局部解析的 `remember(price) { Regex(...).find(price) }`。
- P1 #17 完成：`imagePreviewAttachment` 已改为 `rememberSaveable` + saver。
- P1 #18 部分完成：`pendingClarificationAnswer` 与 `hiddenFlightMessageKeys` 已改为 `rememberSaveable`；`activeClarificationFlight` 仍保留 `remember`，因为它包含运行中坐标/尺寸动画状态，不适合直接跨旋转恢复。
- P1 #15 部分完成：`TimelineNodeContent` 已压缩为 `TimelineNodeRenderProps` + `TimelineNodeCallbacks` 两个稳定 holder，但仍直接接收若干 `List`/`Map`/`Set` 参数，尚未完成 immutable collections / UI model 边界收口。
- P2 #28/#29/#31 完成：`TimelinePresentationState` 改为 `SharingStarted.WhileSubscribed(5_000)`；`rememberUpdatedState(1f)` 改为直接稳定 `State<Float>`；`ReportDrawnWhen` 已接入并等待 `BuyPilotStartupHost` 上报 home ready。

**复验命令：**

```bash
cd /mnt/disk1/LZJ/project/BuyPilot-AI/android
timeout 600s ./gradlew :feature:chat:compileReleaseKotlin :app:compileReleaseKotlin --init-script /mnt/disk1/LZJ/project/compose_skill/jetpack-compose-audit/skills/jetpack-compose-audit/scripts/compose-reports.init.gradle --no-daemon --quiet
timeout 180s ./gradlew testDebugUnitTest
```

**复验结果：** 以上命令均通过。Compose compiler diagnostics 复算：app named-only `3/3 = 100%`；chat named-only `127/127 = 100%`；app/chat 两个模块当前均无 `unstable class` 报告。模块级指标：app `skippableComposables=5` / `restartableComposables=8`；chat `skippableComposables=197` / `restartableComposables=422`。

## 第 10 批 Agent 1-3 整改复验（2026-06-07）

**整改范围：** 按 `doc/parallel-agents/10-compose-audit-remaining-fixes/` 执行 Agent 1-3。Agent 4 发布性能策略未执行，因此 #30/#32 保持未完成。

**已完成 / 兜底：**

- #15 基本闭环：`TimelineNodeContent` 改为 `node + props + inputs + callbacks`，不再直接接收整张 `List` / `Map` / `Set`；节点所需 compare/reveal/dismiss 状态已在上游解析为 `TimelineNodeInputs`。
- #18 继续降风险：新增 saveable 的 `ClarificationFlightSemantic`，只恢复澄清交互语义，不保存运行中的 `Offset` / `Size` 动画坐标，避免旋转后重复发送。
- #19/#20 部分完成：`BuyPilotChatScreen` 增加 `ClarificationInteractionState`；`ChatTimeline` 增加 route return / clarification freeze 状态 holder。
- #21/#22/#26/#27 部分推进：核心 criteria / compare / evidence 组件补 `modifier`，固定文案部分提取到 `strings.xml`，`@Preview` 从 3 个增加到 7 个；我将全限定 preview 注解兜底改为标准 `@Preview`，便于静态验收。
- #23 完成：`StructuredCardMotion` / `StaggeredRevealMotion` 暴露可选 `animationSpec`，并继续保留默认行为；shimmer duration 已常量化。
- #24/#25 部分完成：`BuyPilotDesign.kt` 明确静态 token 策略，已触达组件开始使用现有 `BuyPilotDimens` / `BuyPilotType`，未做 CompositionLocal 大迁移。

**复验命令：**

```bash
cd /mnt/disk1/LZJ/project/BuyPilot-AI/android
timeout 600s ./gradlew :feature:chat:compileReleaseKotlin :app:compileReleaseKotlin --init-script /mnt/disk1/LZJ/project/compose_skill/jetpack-compose-audit/skills/jetpack-compose-audit/scripts/compose-reports.init.gradle --no-daemon --quiet
timeout 180s ./gradlew testDebugUnitTest
```

**复验结果：** 以上命令均通过。Compose compiler diagnostics 复算：app named-only `3/3 = 100%`；chat named-only `81/81 = 100%`；app/chat 两个模块当前均无 `unstable class` 报告。模块级指标：app `skippableComposables=3` / `restartableComposables=6`；chat `skippableComposables=124` / `restartableComposables=248`。

## B 批 API 质量小步收口（2026-06-07）

**整改范围：** 用户决策后选择 B 方向，按低 churn 策略只处理 `BuyPilotChatScreen.kt` 中高频、可独立验证的 API 质量债务，避免在巨型文件中继续做大面积机械改造。

**已完成：**

- #21 继续推进：`ProductRecommendationStrip`、`DecisionSummaryCard`、`CartActionCard`、`CheckoutPreviewCard`、`ErrorCard`、`SwipeRoundButton` 已补 `modifier: Modifier = Modifier`，并在根节点应用传入 modifier。
- #26 继续推进：商品候选区标题/动作、决策卡标题/按钮、过期推荐返回、图片预览 contentDescription 等固定文案已提取到 `strings.xml`。
- 保持克制：购物车回执、商品详情、证据 sheet 中仍有大量动态拼接/映射文案，本批未做全量格式化资源迁移，避免引入更大的回归面。

**复验命令：**

```bash
cd /mnt/disk1/LZJ/project/BuyPilot-AI/android
timeout 600s ./gradlew :feature:chat:compileReleaseKotlin :app:compileReleaseKotlin --init-script /mnt/disk1/LZJ/project/compose_skill/jetpack-compose-audit/skills/jetpack-compose-audit/scripts/compose-reports.init.gradle --no-daemon --quiet
timeout 600s ./gradlew clean :feature:chat:compileReleaseKotlin :app:compileReleaseKotlin --init-script /mnt/disk1/LZJ/project/compose_skill/jetpack-compose-audit/skills/jetpack-compose-audit/scripts/compose-reports.init.gradle --no-daemon --quiet
timeout 180s ./gradlew testDebugUnitTest
```

**复验结果：** 以上命令均通过；`git diff --check` 通过。clean 后本地未重新产出 `build/compose_audit` 目录，因此本批不重算 compiler diagnostics 和评分，只按源码核查 + release compile + 单测作为验收依据。

---

## 评分卡

| 类别 | 得分 | 权重 | 状态 | 说明 |
|------|------|------|------|------|
| 性能 | 8/10 | 35% | 良好 | 主要实例重建点已收掉；`TimelineNodeContent` 不再直接接收整张集合，剩余风险集中在上游大状态派生和 release 性能策略 |
| 状态管理 | 8/10 | 25% | 良好 | 用户可见的预览/澄清状态恢复已改善；screen/timeline 已抽出部分 holder，但两个巨型 composable 仍偏大 |
| 副作用 | 9/10 | 20% | 优秀 | P0 stale callback、composition body 写缓存、常量 state holder 和启动 fully drawn 信号已整改 |
| 可组合 API 质量 | 6/10 | 20% | 需改进 | 部分核心组件已补 `modifier` / preview / stringResource；B 批继续收掉一组 `BuyPilotChatScreen` 高频组件，但固定文案和 preview 覆盖仍不足 |

---

## 二次 Review 补充

- `ChatTimeline.kt:1546-1550` 使用的是普通 `mutableMapOf`，不是 Compose snapshot state；因此不应严格定性为 snapshot backwards write 或已确认的 recomposition loop。它仍然是 composition body 中的副作用式缓存更新，应该修，但严重性从“临界 performance bug”降为“重大副作用/可维护性风险”。
- 编译器 CSV 的 named-only skippability 需要按 `sum(skippable) / sum(restartable)` 计算。原始审计 chat 模块为 `226/226 = 100%`，不是 `226/235 = 96.2%`；第一批整改后复算为 `217/217 = 100%`，第二批整改后复算为 `127/127 = 100%`，第 10 批 Agent 1-3 后复算为 `81/81 = 100%`。
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

**影响：** 在 Strong Skipping 模式下，unstable 参数按实例相等 `===` 比较；因此在 composable body 中分配的新对象（`.toSet()`、`listOf(...)`、`Regex(...)`）会造成实例 churn，导致 skipping 失效。stable data class 参数仍可能走 `equals()`，例如 `ChatUiState` 这类被标为 stable/immutable 的大状态。

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

**`TimelineNodeContent` 44+ 参数 + 实例 churn / stable 大状态 equals 风险**

这个 composable 在 `LazyColumn` 的每个可见 item 中调用。在 streaming 期间，父级 `ChatUiState` 频繁更新（每个 text_delta 事件），导致：
- 44 个参数中约 10 个是不稳定集合类型（`List`、`Map`、`Set`）
- Strong Skipping 下 unstable 参数按实例相等 `===` 比较，集合/对象实例 churn 会让 skip 失效
- stable data class 参数仍可能走 `equals()`；`ChatUiState.nodes` 这类大状态如果进入比较路径，成本会随列表长度增长
- 一旦参数比较判定为变化，整个子树 recompose

**影响**：streaming 密集期可能掉帧。不是每帧都卡，但在商品卡片多的 timeline 中可感知。

**修复**：将 44 个参数分组为 `@Immutable` data class 减少参数数量；或对高频变化的参数使用 `kotlinx.collections.immutable` 的 `PersistentList`/`PersistentMap` 获得 O(log N) 的结构比较。

---

**`BuyPilotChatScreen.kt:421`** — `.toSet()` 无 `remember`

```kotlin
val defaultSkinTypeOptions = stringArrayResource(R.array.default_skin_type_options).toSet()
```

每次 recomposition 分配新 `Set`。这个 Set 如果作为 unstable 参数传给下游 composable，在 SSM 下会因实例 `===` 不同而无法 skip。

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

**结论**：没有确认到必然的 performance bug；第 10 批 Agent 1-3 后，P0 副作用正确性、主要实例重建点和 `TimelineNodeContent` 直接集合参数问题已闭环。剩余主要风险集中在上游大状态派生、巨型 composable 的状态作用域、组件 API 质量余量，以及 baseline profile / R8 / resource shrinking 发布策略。

---

## 分类详情

### 性能 — 8/10

#### 天花板检查

- **Strong Skipping：** 已启用 (Kotlin 2.1.0, Compose Compiler plugin)
- **应用的天花板表：** SSM-on — `skippable%` 趋近 ~100%；约束因素是实例重建、unstable 参数实例 churn 和 stable 大状态 `equals()` 成本
- **模块级 `skippable%`：** app = 3/6 = 50.0%, chat = 124/248 = 50.0%（第 10 批 Agent 1-3 后复算；包含 lambda / helper 后分母会被拉低）
- **仅命名 composable 的 `skippable%`：** app = 3/3 = 100%, chat = 81/81 = 100%（chat 当前 85 个 named 中有 4 个非 restartable helper，不进入 `sum(skippable) / sum(restartable)` 分母）
- **编译器检测的不稳定共享类型：** app/chat 模块均为 0 个 `unstable class`（第 10 批 Agent 1-3 后复算）
- **SSM-on 约束证据：** named-only skippability 仍为 100%，且 compiler 不再报告 unstable class。约束不再是 `TimelineNodeContent` 直接集合参数，而是上游大状态派生、remaining known-unstable arguments 和 release 性能策略。
- **定性得分：** 8/10（第 10 批 Agent 1-3 后暂估）
- **天花板：** 上限 8 (仅命名 skippable% ≥ 95%，但存在可观察的实例重建)
- **应用得分：** 8/10

#### 做得好的地方

- `LazyColumn` 同时使用 `key = { it.key }` 和 `contentType = { it.timelineContentType }` — 异构列表的最佳实践。
- `Animatable` 始终保存在 `remember { Animatable(...) }` 中，并由 `LaunchedEffect(key)` 驱动 — 正确的取消和状态存活。
- `rememberInfiniteTransition` 具有有意义的标签，并正确作用域在渲染动画的 composable 内部（离屏时停止）。
- 动画值通过 lambda modifier 读取（`Modifier.graphicsLayer { }`、`Modifier.offset { }`）— 每帧工作保持在 draw/layout 阶段，不触发 recomposition。
- `derivedStateOf` 正确用于滚动派生的布尔值（`canScrollBackward`、`canScrollForward`、`isNearTimelineEnd`）。
- 广泛使用类型化状态工厂：`mutableIntStateOf`、`mutableFloatStateOf` — 无自动装箱。
- 所有状态模型类都有 `@Immutable` 注解 (ChatUiState、ChatUiNode 层级、TimelinePresentationState)。
- 使用 `enableEdgeToEdge()` 而非 Accompanist。

#### 扣分项 / 整改状态

- **已整改**：`SystemClock.uptimeMillis()` + 普通 mutable map 缓存更新已从 composition body 移入 `LaunchedEffect` (ChatTimeline.kt:1546)。
- **已整改**：`stringArrayResource(...).toSet()` 已拆成资源读取 + `remember(contentHashCode())` 派生 Set (BuyPilotChatScreen.kt:421)。
- **已整改**：`DecisionSummaryCard` / `DecisionEvidenceSheet` 的 `whyItems` 字符串处理已用 `remember(payload.why)` 缓存。
- **已整改**：`EvidenceOverlay` whitespace regex 和 `MarkdownRendering` 高频 regex 已提取顶层常量。
- **已整改**：compare / decision 文本 helper 中的高频 `Regex` 已提取为顶层常量。
- **已整改**：`TimelineNodeContent` 已收口为 `node + TimelineNodeRenderProps + TimelineNodeInputs + TimelineNodeCallbacks`，不再直接接收整张 `List`、`Map`、`Set`。
- **部分缓解**：第 10 批后 compiler report 仍无 `unstable class`；未引入 core/model Compose 依赖，后续若继续优化可考虑更明确的 immutable collections / stability config 策略。
- `HistoryUiState` 缺少 `@Immutable` 仅作为未来接入 Compose UI 的注意项；当前 feature/history 无生产 @Composable，不作为 P1 扣分。
- 无 baseline profiles；release 配置中无 `minifyEnabled`/`shrinkResources`。

#### 证据

- `feature/chat/.../ChatTimeline.kt:1546` — composition body 中的 `SystemClock.uptimeMillis()` + 普通 mutable map 缓存更新 · 参考: <https://developer.android.com/develop/ui/compose/performance/bestpractices>
- `feature/chat/.../BuyPilotChatScreen.kt:421` — 未使用 `remember` 的 `.toSet()` · 参考: <https://developer.android.com/develop/ui/compose/performance/bestpractices>
- `feature/chat/.../BuyPilotChatScreen.kt:3421-3426` — composition 中的字符串处理链 · 参考: <https://developer.android.com/develop/ui/compose/performance/bestpractices>
- `feature/chat/.../EvidenceOverlay.kt:751` — composable 中的 `Regex(...)` 分配 · 参考: <https://developer.android.com/develop/ui/compose/performance/bestpractices>
- `feature/chat/.../ChatTimeline.kt:2046-2089` — `TimelineNodeContent` 有 44+ 个不稳定集合参数 · 参考: <https://developer.android.com/develop/ui/compose/performance/stability/strongskipping>
- `core/model/.../AgentPayload.kt:11-244` — payload data class 在 feature:chat composable 参数中被报告为不稳定；修复应优先选择 UI model / immutable collections / stability config，而不是直接污染 core model · 参考: <https://developer.android.com/develop/ui/compose/performance/stability/fix>

---

### 状态管理 — 8/10

#### 做得好的地方

- ViewModel 正确使用 `MutableStateFlow` 支持属性模式 — 私有可变，公共只读 `StateFlow`。
- UI 中所有 Flow 收集使用 `collectAsStateWithLifecycle()` — 未发现裸 `collectAsState()`。
- ViewModel 作用域在父导航图入口（`hiltViewModel(parentEntry)`）— 跨子路由的单一事实来源。
- `ChatUiState`、`ChatImageAttachmentState`、`ChatCartUiState`、`ChatRetryRequest` 有 `@Immutable` 注解。
- 复杂 UI 状态有自定义 `Saver` 实现（`ChatSheetContentSaver`、`StringSetSaver`、`TimelineRevealSnapshotHolderSaver`）。
- `rememberSaveable` 正确用于滚动位置恢复状态（ChatTimeline 中的路由返回变量）。
- `imagePreviewAttachment`、`pendingClarificationAnswer`、`hiddenFlightMessageKeys` 已改为 `rememberSaveable`，旋转后用户可见的预览/澄清状态不再直接丢失。
- `ClarificationFlightSemantic` 已作为 saveable 语义状态保存，避免恢复运行中坐标动画的同时保留用户选择和发送状态。
- `BuyPilotChatScreen` / `ChatTimeline` 已分别抽出 `ClarificationInteractionState`、route return / clarification freeze holder，降低局部状态散落程度。
- `TimelinePresentationState` 已改为 `SharingStarted.WhileSubscribed(5_000)`，避免无订阅时继续 eager 收集。
- `derivedStateOf` 正确用于计算滚动状态（`isNearTimelineEnd`、`canScrollForward`、`canScrollBackward`）。
- composable 参数中无 `MutableState<T>` 或 `State<T>` 暴露 — 全部使用普通 data class 或原始值。

#### 扣分项

- `BuyPilotChatScreen` 和 `ChatTimeline` 仍是巨型 composable；第 10 批只抽出部分 holder，未完成全量状态作用域收敛。
- `activeClarificationFlight` 仍使用运行时 `remember`，因为它保存运行中的坐标/尺寸动画状态。旋转时会重启动画，但语义状态、隐藏消息 key 和发送状态已可恢复。

#### 证据

- `feature/chat/.../BuyPilotChatScreen.kt:354-397` — 单个 composable 中有 40+ 个局部状态变量 · 参考: <https://developer.android.com/develop/ui/compose/state-hoisting>
- `feature/chat/.../ChatTimeline.kt:355-384` — 30+ 个局部状态变量 · 参考: <https://developer.android.com/develop/ui/compose/state>
- `feature/chat/.../BuyPilotChatScreen.kt:411,435-437` — 图片预览 / 澄清交互状态的 saveable 边界 · 参考: <https://developer.android.com/develop/ui/compose/state>
- `feature/chat/.../ChatViewModel.kt:302-308` — 正确的 MutableStateFlow 支持属性模式 · 参考: <https://developer.android.com/topic/libraries/architecture/stateflow>
- `feature/chat/.../ChatRoute.kt:15-16` — 正确的 `collectAsStateWithLifecycle` 使用 · 参考: <https://developer.android.com/develop/ui/compose/state>

---

### 副作用 — 9/10

#### 做得好的地方

- `rememberUpdatedState` 正确用于动画 composable 中的回调：`latestOnRevealComplete`、`latestOnRevealActiveChange`、`latestOnRevealProgress` (MarkdownRendering.kt:525-527)，`latestOnBack` (ProductSwipeComponents.kt:109)，`latestProducts`、`latestOnLike`、`latestOnDislike` (ProductSwipeComponents.kt:310-312)，`latestOnEntered` (TimelineMotion.kt:62,139)。
- `DisposableEffect` 在 `onDispose` 中有正确的清理 — 生命周期观察器 (ChatTimeline.kt:637-665)，reveal 状态清理 (MarkdownRendering.kt:531-535)。
- `rememberCoroutineScope()` 仅用于事件驱动的工作（底部弹窗隐藏动画，BuyPilotChatScreen.kt:498）。
- `snapshotFlow { detailListState.isScrollInProgress }.distinctUntilChanged()` 在 `LaunchedEffect` 内部 — 正确的 State → Flow 转换。
- `Animatable.animateTo` 由 `LaunchedEffect(key)` 驱动 — 正确的取消语义。
- `LaunchedEffect` 的 key 有意义（`nodeKey`、`deckId`、`payload.compareId`、`item.key`）— 正确的重启行为。
- composition body 中无网络/数据库/SharedPreferences 调用。

#### 扣分项 / 整改状态

- **已整改**：composition body 中的副作用式缓存更新 — `SystemClock.uptimeMillis()` + 普通 mutable map 上的 `putIfAbsent`/`retainAll` 已移入 `LaunchedEffect` (ChatTimeline.kt:1546-1550)。
- **已整改**：`onCardEntered`、`onCardDismissed` 在 `LaunchedEffect` 中使用 `rememberUpdatedState` 后的 latest callback (ChatTimeline.kt:2979-2984, 3048-3052)。
- **已整改**：`onAssistantTurnVisualActiveChange` 在 `LaunchedEffect` 和 `DisposableEffect.onDispose` 中使用 latest callback (ChatTimeline.kt:1534-1538)。
- **已整改**：`onDismiss` 在 `LaunchedEffect(dismissRequested)` 中使用 latest callback (BuyPilotChatScreen.kt:8030-8038)。
- **已整改**：`onRequestProductDetail` 在 `LaunchedEffect(product.productId)` 中使用 latest callback (BuyPilotChatScreen.kt:4076-4078)。
- **已整改**：`AndroidView(factory=...)` 中的 `CardStackListener` 调用 latest `onSwiped` / `onStackPositionChanged` (ProductSwipeComponents.kt:451-487)。
- **已整改**：`StartupSplash` 的启动延迟 effect key 已改为 `showStartupSplash` (StartupSplash.kt:102-105)。
- **已整改**：`rememberUpdatedState(1f)` 常量持有者已改为直接稳定 `State<Float>`，避免把 latest-state API 用作常量容器。
- **已整改**：`ReportDrawnWhen` 已接入 `MainActivity`，并通过 `BuyPilotStartupHost` 的 `onHomeReady` 回调等待首页内容挂载后上报。

#### 证据

- `feature/chat/.../ChatTimeline.kt:1546-1550` — composition body 中的普通 mutable map 缓存更新 · 参考: <https://developer.android.com/develop/ui/compose/side-effects#side-effects-in-composition>
- `feature/chat/.../ChatTimeline.kt:2979-2984` — `onCardEntered` 闭包捕获过期 · 参考: <https://developer.android.com/develop/ui/compose/side-effects#rememberupdatedstate>
- `feature/chat/.../ChatTimeline.kt:1534-1538` — DisposableEffect 中 `onAssistantTurnVisualActiveChange` 闭包捕获过期 · 参考: <https://developer.android.com/develop/ui/compose/side-effects#rememberupdatedstate>
- `feature/chat/.../BuyPilotChatScreen.kt:8030-8038` — `onDismiss` 闭包捕获过期 · 参考: <https://developer.android.com/develop/ui/compose/side-effects#rememberupdatedstate>
- `feature/chat/.../BuyPilotChatScreen.kt:4076-4078` — `onRequestProductDetail` 闭包捕获过期 · 参考: <https://developer.android.com/develop/ui/compose/side-effects#rememberupdatedstate>
- `feature/chat/.../ProductSwipeComponents.kt:451-487` — `AndroidView(factory=...)` listener 捕获旧回调 · 参考: <https://developer.android.com/develop/ui/compose/side-effects#rememberupdatedstate>
- `app/.../StartupSplash.kt:102-105` — `LaunchedEffect(Unit)` 捕获变化的 `showStartupSplash` · 参考: <https://developer.android.com/develop/ui/compose/side-effects#launchedeffect>
- `app/.../MainActivity.kt:32-39` / `StartupSplash.kt:49-111` — fully drawn 信号等待 home ready · 参考: <https://developer.android.com/topic/performance/vitals/launch-time>

---

### 可组合 API 质量 — 6/10

#### 做得好的地方

- 可复用 composable 参数中无 `MutableState<T>` 或 `State<T>` — 全部接受普通 data class 或原始值。
- 多个 composable 正确接受并应用 `modifier: Modifier = Modifier`：`ProductImage`、`M3TopAppBarRow`、`ProductHeroCard`、`CompareSelectionTray`、`ProductDeckMarkdownCompareTable`、`MarkdownTextBlock`、`ProductEvidenceOverlayScreen`、`ProductSwipeModeScreen`、`BuyPilotLayeredSplash`、`EdgeFadedLazyRow`、`ClarificationOptionScroller`、`TimelineItemMotion`、`CriteriaSummaryCard`、`ScenarioFilterReceiptCard`、`CompareSummaryCard`、`ProductRecommendationStrip`、`DecisionSummaryCard`、`CartActionCard`、`ErrorCard`、`SwipeRoundButton`。
- `@Preview` 已从 3 个增加到 7 个，覆盖 Criteria / Compare / Evidence 的核心视觉组件。
- Criteria / Compare / Evidence / BuyPilotChatScreen 中部分固定文案已提取到 `strings.xml`。
- 组件用途/分层分离清晰：设计令牌 (BuyPilotDesign.kt)、动画组件 (TimelineMotion.kt)、显示组件 (ProductDisplay.kt)、屏幕编排器 (BuyPilotChatScreen.kt)。
- 共享组件一致使用 `internal` 可见性修饰符。

#### 扣分项

- **重大**：仍有一批内部 composable 缺少 `modifier` 参数或未系统检查顺序，例如 `EvidenceLead`、`EvidenceReportSurface`、`AssistantInlineStatus`、`AssistantPendingDots`、`ProductDeckArrivalMotion`、`AssistantText`、`StreamingAssistantText`、`BuyPilotChatScreen`、`ProductSwipeTopBar` 等。
- **重大**：生产源码中仍有大量 composable 固定文案；`@Preview` 目前 7 个。相比第二批有改善，但对于视觉丰富的 UI，预览覆盖仍明显不足。
- **次要**：`StructuredCardMotion` / `StaggeredRevealMotion` 已暴露可选 `animationSpec`；`StreamingAssistantText` 等仍有硬编码动画参数。
- **次要**：设计令牌当前明确为静态 `internal object` 策略；CompositionLocal 主题切换仍是后续设计系统债务。
- **次要**：UI 中仍有大量硬编码 dp/sp 值，只有触达组件开始使用 `BuyPilotDimens` / `BuyPilotType`。
- **次要**：Criteria / Compare / Evidence 和 `BuyPilotChatScreen` 中部分固定中文文案已提取；购物车回执、商品详情、证据 sheet 等区域仍有大量固定文案未走 `stringResource`。

#### 证据

- `feature/chat/.../CriteriaComponents.kt`, `CompareComponents.kt`, `EvidenceOverlay.kt` — 第 10 批只补了部分核心组件 modifier / preview / stringResource · 参考: <https://developer.android.com/develop/ui/compose/components>
- `feature/chat/.../BuyPilotChatScreen.kt` — B 批已补 `ProductRecommendationStrip`、`DecisionSummaryCard`、`CartActionCard`、`ErrorCard`、`SwipeRoundButton` 等组件 modifier；remaining modifier 需继续分批检查 · 参考: <https://developer.android.com/develop/ui/compose/components>
- `feature/chat/.../ui` — 当前 `@Preview` 为 7 个，仍不足 · 参考: <https://developer.android.com/develop/ui/compose/tooling/previews>
- `feature/chat/.../BuyPilotChatScreen.kt` — 仍有大量固定中文文案未提取 · 参考: <https://developer.android.com/develop/ui/compose/resources>
- `feature/chat/.../ChatTimeline.kt:1247-1252` — 硬编码 dp 值而非 BuyPilotDimens · 参考: <https://developer.android.com/develop/ui/compose/designsystems/material3>

---

## 整改 Checklist（是否修复）

勾选约定：`[ ]` 未修复，`[~]` 部分完成或已降风险但未闭环，`[x]` 已修复并通过验收。第 10 批 Agent 1-3 后，Timeline 直接集合参数和部分 API 质量问题已推进；B 批继续收掉 `BuyPilotChatScreen` 一组高频 API 质量债务；发布性能策略仍未执行。

| # | 修复? | 优先级 | 类别 | 问题 | 落地文件 | 验收标准 |
|---|------|--------|------|------|----------|----------|
| 1 | [x] | **P0** | 副作用 | composition body 中更新 thinking first-seen 普通 map 缓存 | `ChatTimeline.kt:1546-1550` | composition body 不再调用 `putIfAbsent`/`retainAll`；计时同步在 `LaunchedEffect` 或 holder 方法中完成 |
| 2 | [x] | **P0** | 副作用 | `onAssistantTurnVisualActiveChange` 在 effect / dispose 中捕获旧回调 | `ChatTimeline.kt:1534-1538` | 使用 `rememberUpdatedState`；`LaunchedEffect` 和 `onDispose` 都调用 latest 回调 |
| 3 | [x] | **P0** | 副作用 | `onCardEntered` 在 `LaunchedEffect` 中捕获旧回调 | `ChatTimeline.kt:2979-2984` | 使用 `rememberUpdatedState`；延迟后调用 latest 回调 |
| 4 | [x] | **P0** | 副作用 | `onCardDismissed` 在 `LaunchedEffect` 中捕获旧回调 | `ChatTimeline.kt:3048-3052` | 使用 `rememberUpdatedState`；延迟后调用 latest 回调 |
| 5 | [x] | **P0** | 副作用 | `CompareActionPickerDialog` 的 `onDismiss` 在 `LaunchedEffect(dismissRequested)` 中捕获旧回调 | `BuyPilotChatScreen.kt:8030-8038` | 使用 `rememberUpdatedState(onDismiss)`；退出动画后调用 latest 回调 |
| 6 | [x] | **P0** | 副作用 | 商品详情页 `onRequestProductDetail` 在 `LaunchedEffect(product.productId)` 中捕获旧回调 | `BuyPilotChatScreen.kt:4076-4078` | 使用 `rememberUpdatedState(onRequestProductDetail)`；productId 变化时调用 latest 回调 |
| 7 | [x] | **P0** | 副作用 | `AndroidView(factory=...)` 的 `CardStackListener` 捕获旧 `onSwiped` / `onStackPositionChanged` | `ProductSwipeComponents.kt:451-487` | 在 composable 中保存 latest callbacks；factory listener 调用 latest；手动/自动 swipe 行为不变 |
| 8 | [x] | **P1** | 副作用 | `StartupSplash` 的 `LaunchedEffect(Unit)` 捕获 `showStartupSplash` 初始值 | `StartupSplash.kt:102-105` | key 改为 `showStartupSplash` 或用 `rememberUpdatedState`；启动延迟逻辑不被旧值锁死 |
| 9 | [x] | **P1** | 性能 | `stringArrayResource(...).toSet()` 每次 recomposition 分配 | `BuyPilotChatScreen.kt:421` | 先调用 `stringArrayResource`，再用 `remember(array) { array.toSet() }` 缓存；下游收到稳定 Set |
| 10 | [x] | **P1** | 性能 | 商品详情页 `swipedProductIds.orEmpty().toSet()` 每次 recomposition 分配 | `BuyPilotChatScreen.kt:3961` | 用 `remember(state.productSwipeStates, deckId)` 或 presenter 派生缓存 |
| 11 | [x] | **P1** | 性能 | `DecisionSummaryCard` 的 `whyItems` 字符串清理链每次 recomposition 运行 | `BuyPilotChatScreen.kt:3421-3426` | 用 `remember(payload.why)` 缓存；输出列表与原逻辑一致 |
| 12 | [x] | **P1** | 性能 | `EvidenceOverlay` snippet 处理中即时构造 whitespace `Regex` | `EvidenceOverlay.kt:751` | 提取顶层 `private val`；相关文本清理测试/手测输出不变 |
| 13 | [x] | **P1** | 性能 | Markdown 高频清理 / 判定 helper 中多处即时构造 `Regex` | `MarkdownRendering.kt:98-119,188-206` | 高频 regex 提取顶层常量；streaming / final markdown 渲染结果不变 |
| 14 | [x] | **P1** | 性能 | compare / decision 文本 helper 中多处即时构造 `Regex` | `CompareComponents.kt:1534-1582`, `BuyPilotChatScreen.kt:8222-8240` | helper regex 已提取顶层常量；保留局部价格解析的 `remember(price)` |
| 15 | [x] | **P1** | 性能 | `TimelineNodeContent` 44+ 参数含不稳定 `List`/`Map`/`Set`，streaming 时存在实例 churn；stable 大状态参数仍可能有 `equals()` 成本 | `ChatTimeline.kt:2099`, `TimelineNodeInputs.kt`, `TimelineRenderProps.kt` | 已收口为 `node + props + inputs + callbacks`；`TimelineNodeContent` 不再直接接收整张集合；compiler report 无 unstable class |
| 16 | [~] | **P1** | 性能 | core payload 在 feature:chat 参数中表现为不稳定 | `AgentPayload.kt:11-244`, `feature/chat` UI model 边界 | 第 10 批后 compiler report 无 unstable class，且未污染 core/model；仍未做全局 immutable collections / stability config 策略 |
| 17 | [x] | **P1** | 状态 | `imagePreviewAttachment` 旋转后丢失 | `BuyPilotChatScreen.kt:411` | 已改为 `rememberSaveable` + saver；旋转后图片预览目标可恢复 |
| 18 | [~] | **P1** | 状态 | 澄清交互中 `pendingClarificationAnswer` / `activeClarificationFlight` / `hiddenFlightMessageKeys` 旋转后丢失 | `BuyPilotChatScreen.kt:316-351,484-486` | 语义状态已 saveable；运行中坐标动画仍不保存，避免重复发送 |
| 19 | [~] | **P2** | 状态 | `BuyPilotChatScreen` 40+ 局部状态集中在一个 composable | `BuyPilotChatScreen.kt:424-432` | 已抽 `ClarificationInteractionState`；仍未完成全量状态作用域收敛 |
| 20 | [~] | **P2** | 状态 | `ChatTimeline` 30+ 局部状态集中在一个 composable | `TimelineStateHolders.kt` | 已抽 route return / clarification freeze holder；滚动恢复和 reveal 大状态仍在主 composable |
| 21 | [~] | **P2** | API | 30+ 可复用 composable 缺少 `modifier` 参数 | `CriteriaComponents.kt`, `CompareComponents.kt`, `EvidenceOverlay.kt`, `BuyPilotChatScreen.kt` | Criteria / Compare 部分核心组件已补；B 批补了 `ProductRecommendationStrip`、`DecisionSummaryCard`、`CartActionCard`、`ErrorCard`、`SwipeRoundButton`；其余组件仍需分批推进 |
| 22 | [~] | **P2** | API | 主要视觉组件 preview 覆盖不足 | 全仓库 | `@Preview` 从 3 增至 7；覆盖仍不足 |
| 23 | [x] | **P2** | API | 动画组件硬编码时长，缺少可配置 `animationSpec` | `TimelineMotion.kt`, `BuyPilotChatScreen.kt` | `StructuredCardMotion` / `StaggeredRevealMotion` 已暴露可选 `animationSpec`；shimmer 时长已常量化 |
| 24 | [~] | **P2** | API | 设计令牌为 `internal object`，不是 tree-scoped 主题 | `BuyPilotDesign.kt` | 已明确静态 token 策略；CompositionLocal 迁移仍为后续设计系统债务 |
| 25 | [~] | **P2** | API | 大量硬编码 dp/sp 未走 token | 多处 UI 文件 | 触达组件已开始用 `BuyPilotDimens` / `BuyPilotType`；未全量替换 |
| 26 | [~] | **P2** | API | 硬编码中文字符串未走 `stringResource` | `EvidenceOverlay.kt`, `BuyPilotChatScreen.kt`, `CompareComponents.kt` | Criteria / Compare / Evidence 部分固定文案已提取；B 批补了候选区、决策卡、图片预览等固定文案；购物车回执、商品详情、证据 sheet 仍未全量处理 |
| 27 | [x] | **P2** | API | 部分 composable 的 `modifier` 参数顺序不符合约定 | `ProductDisplay.kt:41`, `BuyPilotChatScreen.kt:3059,3318` | `ProductDisplay` 已验证；`CandidateActionButton` / `ProductRecommendationThumb` 已修正顺序；编译通过 |
| 28 | [x] | **P2** | 状态 | `TimelinePresentationState` 使用 `SharingStarted.Eagerly` | `ChatViewModel.kt:309-317` | 已改为 `WhileSubscribed(5_000)`；编译和单测通过 |
| 29 | [x] | **P2** | 副作用 | `rememberUpdatedState(1f)` 用作常量 state holder | `BuyPilotChatScreen.kt:2554,2633` | 已改为直接稳定 `State<Float>`；无行为变化 |
| 30 | [ ] | **P2** | 发布性能 | 无 baseline profiles | 全仓库 | 增加 baseline profile / macrobenchmark 路径或明确不做 |
| 31 | [x] | **P2** | 发布性能 | 无 `ReportDrawnWhen` / fully drawn signal | `MainActivity.kt` | 已添加 home ready 条件；启动指标可读 |
| 32 | [ ] | **P2** | 发布性能 | release 未启用 R8 / resource shrinking | `app/build.gradle.kts` | `isMinifyEnabled` / `isShrinkResources` 策略明确；release 构建通过 |
| 33 | [x] | **观察** | 范围修正 | `HistoryUiState` 当前不是生产 Compose 参数 | `feature/history` | 不作为 P1 修复；若未来新增 history Compose UI，再处理稳定性注解 |

### 下一步修复方案

下一批建议只开两个方向，避免继续多 agent 同改 UI 巨型文件：

1. **发布性能策略 / Agent 4 补跑**：处理 #30/#32，评估 baseline profile / macrobenchmark 路径，以及 R8 + resource shrinking。若启用 R8/shrink，必须以 `:app:assembleRelease` 通过作为合并条件。
2. **API 质量收尾**：继续 #21/#22/#24-#26，但按文件单写推进。下一小步优先 `ProductSwipeComponents.kt` 与 `BuyPilotChatScreen.kt` 的商品详情/证据 sheet 文案，不和状态 holder 重构混做。
3. **状态 holder 深水区暂缓**：#18/#19/#20 已降风险，除非要专门做 screen/timeline 架构重构，否则不建议和发布性能同批混做。

### 复验命令

```bash
cd /mnt/disk1/LZJ/project/BuyPilot-AI/android
timeout 600s ./gradlew :feature:chat:compileReleaseKotlin :app:compileReleaseKotlin --init-script /mnt/disk1/LZJ/project/compose_skill/jetpack-compose-audit/skills/jetpack-compose-audit/scripts/compose-reports.init.gradle --no-daemon --quiet
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
- **使用编译器诊断：** **是**。通过 `--init-script` 和 `reportsDestination` 为 app 和 feature:chat 模块生成报告。第 10 批 Agent 1-3 后复算：App 模块 `3/3` 个 restartable named composable 可跳过；Chat 模块 `81/81` 个 restartable named composable 可跳过，85 个 named 中另有 4 个非 restartable helper。App/chat 模块当时均无 `unstable class` 报告。B 批 clean 后本地未重新产出 `build/compose_audit`，因此未用增量/缺失 diagnostics 覆盖该口径。

---

## 后续建议

- 如果仓库也显示可能存在设计系统或 Material 3 问题，运行 `material-3` 审查 — 当前使用 `internal object` 常量作为设计令牌而非 `CompositionLocal` 主题系统，表明 Material 3 集成可能较浅。
- 运行 `compose-agent focus on testing` — 目前新增了 clarification 文案单测，但仍未发现 UI 测试、截图测试或 Compose 测试规则。当前 `@Preview` 为 7 个，视觉测试/预览覆盖仍不足。
- 缺少 baseline profiles、`minifyEnabled`/`shrinkResources` 策略 — 关于发布性能工具链的后续审查将补充本次审计。
