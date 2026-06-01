# Jetpack Compose 审计报告

目标路径：`/mnt/disk1/LZJ/project/BuyPilot-AI/android`
审计日期：2026-06-01
审计范围：`:app` 和 `:feature:chat` 生产环境 Compose 源码，辅助阅读 `core:*` 状态/模型/数据层代码和 `feature:history` ViewModel 代码
排除范围：`*/src/test/*`、生成的 `*/build/*` 输出、非 Compose 核心模块（仅状态/模型上下文除外）
置信度：中等（因 Java 版本不匹配导致编译器诊断不可用）
总分：62/100（较 2026-05-31 审计提升 +2）

## 评分卡

| 类别 | 得分 | 权重 | 状态 | 备注 |
|------|------|------|------|------|
| 性能 | 6/10 | 35% | 需要改进 | 热路径时间线推导仍在 composition 内执行；动画进度值在 `graphicsLayer` 之前读取；release 构建未启用代码压缩 |
| 状态管理 | 6/10 | 25% | 需要改进 | `collectAsStateWithLifecycle()` 已正确使用；部分用户可见状态仍使用 `remember` 而非 `rememberSaveable` |
| 副作用 | 7/10 | 20% | 良好 | Splash 屏幕已使用 `rememberUpdatedState`；大多数 effect 键正确；存在轻微的状态捕获风险 |
| Composable API 质量 | 6/10 | 20% | 需要改进 | 关键屏幕已暴露 `modifier`；11K 行单文件、67 个硬编码颜色、零 `@Preview` 注解拉低分数 |

## 关键发现

1. **性能：热路径聊天渲染在 composition 内重新计算派生集合**
   - 为什么重要：`state.nodes` 在流式传输期间频繁变化，导致时间线项、商品映射和渲染上下文在每个 delta 更新时都在 composition 线程重建。这是主聊天界面的性能瓶颈。
   - 证据：`feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:1690`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:1726-1761`
   - 修复方向：将时间线/商品/收敛推导移至 presentation state holder 或 ViewModel reducer，按语义变化触发，而非在 composition 内执行。
   - 参考文档：<https://developer.android.com/develop/ui/compose/performance/bestpractices>

2. **性能：动画进度值在 composition body 内读取**
   - 为什么重要：`progress.value` 被读取并传递给 content lambda，导致子内容逐帧重组，而非将读取延迟到 layout/draw 阶段。
   - 证据：`feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:4727-4728`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:5383`
   - 修复方向：将逐帧动画读取保持在 lambda modifier 内（如 `graphicsLayer { ... }`），避免值变化触发子内容重建。
   - 参考文档：<https://developer.android.com/develop/ui/compose/performance/phases>, <https://developer.android.com/develop/ui/compose/animation/value-based>

3. **状态管理：用户可见 UI 状态使用 `remember` 而非 `rememberSaveable`**
   - 为什么重要：sheet 内容、已关闭的澄清卡片键等用户可见状态在配置变更时重置。
   - 证据：`feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:603-607`
   - 修复方向：对小型 Bundle 安全状态使用 `rememberSaveable`；对非 Bundle 状态提供 `Saver` 或提升状态层级。
   - 参考文档：<https://developer.android.com/develop/ui/compose/state>

4. **Composable API 质量：单文件过大、硬编码颜色、无预览**
   - 为什么重要：`BuyPilotChatScreen.kt` 包含 11,351 行代码，涵盖路由屏幕、控件、AndroidView 互操作、Markdown 渲染、商品详情、证据覆盖层和编辑器 sheet。这使得组件职责和分层难以审计和演进。67 个硬编码 `Color(0x...)` 值破坏了深色模式兼容性。零 `@Preview` 注解意味着没有视觉回归安全网。
   - 证据：`feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt`（文件大小），67 处 `Color(0x...)` 出现
   - 修复方向：按领域拆分为专注文件（时间线、商品、标准、证据），颜色通过 `MaterialTheme.colorScheme` 路由，为提取的组件添加 `@Preview`。
   - 参考文档：<https://developer.android.com/develop/ui/compose/api-guidelines>, <https://developer.android.com/develop/ui/compose/designsystems/material3>, <https://developer.android.com/develop/ui/compose/tooling/previews>

## 类别详情

### 性能 - 6/10

**封顶检查**

- Strong Skipping：推断为开启（`libs.versions.toml` 中 Kotlin 2.1.0），但因 Java 版本不匹配导致编译器诊断不可用
- 应用的封顶表：SSM-on（推断）。无法验证模块级或命名级 `skippable%`
- 编译器报告的不稳定共享类型：不可用（诊断不可用）
- SSM-on 约束证据：源码显示热路径存在围绕 `state.nodes` 的重复派生工作，以及在 `graphicsLayer` 之前读取的逐帧动画值
- 定性评分：6/10
- 封顶：因观察到热路径实例重建 churn，按 SSM-on 表封顶在 6
- 应用评分：6/10

**做得好的地方**

- 基于 Kotlin 2.1.0 推断 Strong Skipping 已启用。参考：<https://developer.android.com/develop/ui/compose/performance/stability/strongskipping>
- 主时间线 `LazyColumn` 使用稳定的 item key 和 `contentType`，对异构聊天内容是正确的做法。证据：`feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:2174-2177`。参考：<https://developer.android.com/develop/ui/compose/lists>
- 多个目标驱动的动画使用 `remember { Animatable(...) }` 加 `LaunchedEffect(key)`，提供正确的取消语义。证据：`feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:4246`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:5365`。参考：<https://developer.android.com/develop/ui/compose/animation/value-based>
- `ScrollableChipRow` 和 `ScrollableQuickActionRow` 现在使用 `itemsIndexed(..., key = ...)` 提供稳定的 lazy item key。证据：`feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:10402`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:10440`。参考：<https://developer.android.com/develop/ui/compose/lists>
- 状态类使用 `@Immutable` 注解提供稳定性提示。证据：`feature/chat/src/main/java/com/buypilot/feature/chat/state/ChatUiState.kt:8,15`, `feature/chat/src/main/java/com/buypilot/feature/chat/model/ChatUiNode.kt:15-96`。参考：<https://developer.android.com/develop/ui/compose/performance/stability>
- 原始类型状态使用类型化工厂（`mutableIntStateOf`）。证据：`feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:621`。参考：<https://developer.android.com/develop/ui/compose/state>

**拉低分数的地方**

- `ChatTimeline` 在 composition 期间从 `state.nodes` 派生时间线项、商品映射、显示键和收敛集合。因为流式文本更新会在每个 delta 替换节点，这项工作在热路径上重复执行。参考：<https://developer.android.com/develop/ui/compose/performance/bestpractices>
- `StructuredCardMotion` 和 `rememberProductDeckArrivalProgress` 等动画辅助函数将 `progress.value` 作为 `Float` 暴露并通过 `content(t)` 传递给子内容，导致逐帧变化触发更多树节点重组。参考：<https://developer.android.com/develop/ui/compose/performance/phases>, <https://developer.android.com/develop/ui/compose/animation/value-based>
- `MarkdownTextBlock` 在 composition 线程的 `remember` 块中解析 Markwon 输出。`remember` 缓存重复调用，但不会将首次渲染或键变更移出 UI composition 路径。参考：<https://developer.android.com/develop/ui/compose/performance/bestpractices>
- Release 构建设置 `isMinifyEnabled = false`，削弱了 release 性能优化。参考：<https://developer.android.com/develop/ui/compose/performance/baseline-profiles>

**证据**

- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:1690` - `state.nodes.toTimelineRenderItems()` 在流式节点列表变化时重新计算。参考：<https://developer.android.com/develop/ui/compose/performance/bestpractices>
- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:1726-1761` - 商品 deck 提取、渲染上下文和收敛集合以整个节点列表为键，而非以商品/决策变化为键。参考：<https://developer.android.com/develop/ui/compose/performance/bestpractices>
- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:4727-4728` - `ClarificationFlightOverlay` 在 `graphicsLayer` 之前读取 `progress.value` 和 `birthProgress.value`，并将值传递给子 composition。参考：<https://developer.android.com/develop/ui/compose/performance/phases>
- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:5383` - `StructuredCardMotion` 在 `graphicsLayer` 之前读取 `progress.value` 并将 `t` 传递给 `content(t)`，导致显示期间内容重组。参考：<https://developer.android.com/develop/ui/compose/performance/phases>
- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:3971` - `MarkdownTextBlock` 通过 `remember(appContext, displayContent)` 在 composition 中运行 Markwon 解析。参考：<https://developer.android.com/develop/ui/compose/performance/bestpractices>
- `app/build.gradle.kts:26` - release 设置 `isMinifyEnabled = false`；削弱了 release 性能优化。参考：<https://developer.android.com/develop/ui/compose/performance/baseline-profiles>

### 状态管理 - 6/10

**做得好的地方**

- `ChatViewModel` 拥有单个 `StateFlow<ChatUiState>`，reducer 返回复制的不可变状态而非在 composable 中修改 UI 状态。证据：`feature/chat/src/main/java/com/buypilot/feature/chat/ChatViewModel.kt:63-69`, `feature/chat/src/main/java/com/buypilot/feature/chat/state/ChatReducer.kt:30`。参考：<https://developer.android.com/develop/ui/compose/architecture>
- 聊天节点和 UI 状态建模为带 `@Immutable` 注解的不可变 data class/sealed interface，保持单向数据流。证据：`feature/chat/src/main/java/com/buypilot/feature/chat/model/ChatUiNode.kt:10-96`, `feature/chat/src/main/java/com/buypilot/feature/chat/state/ChatUiState.kt:12-37`。参考：<https://developer.android.com/develop/ui/compose/state-hoisting>
- 路由级 `StateFlow` 收集现在使用 `collectAsStateWithLifecycle()` 实现生命周期感知观察。证据：`feature/chat/src/main/java/com/buypilot/feature/chat/ChatRoute.kt:15`, `app/src/main/java/com/buypilot/navigation/AppNavGraph.kt:54,90,157,196`。参考：<https://developer.android.com/develop/ui/compose/state>
- 输入框文本和已显示消息键现在使用 `rememberSaveable` 确保重建后恢复。证据：`feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:601,608`。参考：<https://developer.android.com/develop/ui/compose/state>
- 时间线滚动状态使用 `rememberSaveable(saver = LazyListState.Saver)`。证据：`feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:1656`。参考：<https://developer.android.com/develop/ui/compose/state>

**拉低分数的地方**

- 多个用户可见 UI 状态使用 `remember` 而非 `rememberSaveable`，包括 sheet 内容、已关闭的澄清卡片键和输入中消息键。这些状态在配置变更时重置。参考：<https://developer.android.com/develop/ui/compose/state>
- 商品详情屏幕对部分状态使用 `rememberSaveable(deckId, productId)`，但如果 payload 值在相同 ID 下变化，键可能过于狭窄。参考：<https://developer.android.com/develop/ui/compose/state>

**证据**

- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:603` - `sheetContent` 使用 `remember`，底部 sheet 状态在重建时丢失。参考：<https://developer.android.com/develop/ui/compose/state>
- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:606` - `dismissedClarificationKeys` 使用 `remember`，已关闭的澄清卡片在旋转后重新出现。参考：<https://developer.android.com/develop/ui/compose/state>
- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:610` - `typingMessageKeys` 使用 `remember`，输入动画状态在重建时重置。参考：<https://developer.android.com/develop/ui/compose/state>

### 副作用 - 7/10

**做得好的地方**

- 导航和商品选择副作用是事件驱动的或在 `LaunchedEffect` 内。证据：`app/src/main/java/com/buypilot/navigation/AppNavGraph.kt:94,106`。参考：<https://developer.android.com/develop/ui/compose/side-effects>
- 长时间运行的文本显示使用 `rememberUpdatedState` 处理变化的回调和内容，避免过期 lambda 捕获同时保持 effect 生命周期。证据：`feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:3567,5367,5404`。参考：<https://developer.android.com/develop/ui/compose/side-effects>
- Splash 屏幕现在使用 `rememberUpdatedState(onFinished)` 避免一次性 effect 中的过期回调捕获。证据：`app/src/main/java/com/buypilot/ui/StartupSplash.kt:121`。参考：<https://developer.android.com/develop/ui/compose/side-effects>
- 滚动吸附使用 `snapshotFlow` 在 `LaunchedEffect` 内，这是正确的状态到 flow 边界。证据：`feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:2063-2081`。参考：<https://developer.android.com/develop/ui/compose/side-effects>
- 商品 deck 自动关闭使用 `rememberUpdatedState` 处理回调，并正确以 `deckId` 和 `deckFullyHandled` 为键。证据：`feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:6646-6655`。参考：<https://developer.android.com/develop/ui/compose/side-effects>

**拉低分数的地方**

- 部分 `LaunchedEffect` 块捕获了多个可能变化的值，但这些值既不在键列表中，也未用 `rememberUpdatedState` 包装，存在潜在的状态捕获风险。参考：<https://developer.android.com/develop/ui/compose/side-effects>

**证据**

- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:1886-1938` - 多个滚动恢复的 `LaunchedEffect` 块捕获可能变化的状态值；部分使用 `rememberUpdatedState`，其他依赖键列表。参考：<https://developer.android.com/develop/ui/compose/side-effects>

### Composable API 质量 - 6/10

**做得好的地方**

- 关键可复用屏幕现在暴露 `modifier: Modifier = Modifier` 并应用到根节点。证据：`feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:6618`（ProductSwipeModeScreen），`feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:7612`（ProductHeroDetailScreen），`feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:8274`（ProductEvidenceOverlayScreen）。参考：<https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md>
- `ProductCardStackView` 将 `modifier` 放在行为回调之前，遵循必需参数、`modifier`、可选参数的顺序指南。证据：`feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:6971`。参考：<https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md>
- `ScrollableChipRow` 和 `ScrollableQuickActionRow` 等较小组件暴露 `modifier` 并遵循约定。证据：`feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:10374-10446`。参考：<https://developer.android.com/develop/ui/compose/api-guidelines>
- 主聊天路由是有状态的，将渲染委托给状态参数化的 `BuyPilotChatScreen`，这是良好的应用级路由/屏幕分离。证据：`feature/chat/src/main/java/com/buypilot/feature/chat/ChatRoute.kt:17`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:588`。参考：<https://developer.android.com/develop/ui/compose/architecture>

**拉低分数的地方**

- 单个 `BuyPilotChatScreen.kt` 文件包含 11,351 行，涵盖路由屏幕、屏幕状态、可复用控件、AndroidView 互操作、Markdown 渲染、商品详情、证据覆盖层和编辑器 sheet。这使得组件职责和分层难以审计和演进。参考：<https://developer.android.com/develop/ui/compose/api-guidelines>
- `BuyPilotChatScreen.kt` 中 67 个硬编码 `Color(0x...)` 值破坏了深色模式兼容性和可访问性。参考：<https://developer.android.com/develop/ui/compose/designsystems/material3>
- 生产代码中未发现 `@Preview` 注解。可复用组件缺乏视觉回归安全网。参考：<https://developer.android.com/develop/ui/compose/tooling/previews>
- 部分硬编码的 `dp` 和 `sp` 值未通过 `MaterialTheme` token 或尺寸资源路由。参考：<https://developer.android.com/develop/ui/compose/designsystems/material3>

**证据**

- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt` - 单文件 11,351 行，涵盖多个领域。参考：<https://developer.android.com/develop/ui/compose/api-guidelines>
- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt` - 67 处硬编码 `Color(0x...)` 值。参考：<https://developer.android.com/develop/ui/compose/designsystems/material3>
- 所有生产 Compose 文件 - 未发现 `@Preview` 注解。参考：<https://developer.android.com/develop/ui/compose/tooling/previews>

## 优先修复项

1. **将热路径时间线/商品推导移出 composition** — `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:1690-1761`
   - 具体变更：创建 `TimelinePresenter` 或扩展 `ChatViewModel`，在协程中从 `state.nodes` 推导 `timelineItems`、`productsById` 和 `renderContext`，通过 `StateFlow` 发射派生状态
   - 参考文档：<https://developer.android.com/develop/ui/compose/performance/bestpractices>
   - 预期影响：消除当前将 Performance 封顶在 6 的源码级 churn

2. **将动画进度读取延迟到 layout/draw 阶段** — `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:4727-4728` 和 `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:5383`
   - 具体变更：将 `content(t)` 改为 `content: @Composable () -> Unit`，在 `graphicsLayer { ... }` 或类似 lambda modifier 内读取 `progress.value`
   - 参考文档：<https://developer.android.com/develop/ui/compose/performance/phases>
   - 预期影响：消除动画期间子内容的逐帧重组

3. **将用户可见的本地状态改为 saveable** — `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:603-610`
   - 具体变更：将 `sheetContent`、`dismissedClarificationKeys` 和 `typingMessageKeys` 改为使用 `rememberSaveable`，对非 Bundle 类型提供适当的 `Saver`
   - 参考文档：<https://developer.android.com/develop/ui/compose/state>
   - 预期影响：在配置变更时保留用户可见状态

4. **将 `BuyPilotChatScreen.kt` 拆分为专注文件** — 按领域拆分：`ChatTimeline.kt`、`ProductSwipeComponents.kt`、`CriteriaComponents.kt`、`EvidenceOverlay.kt`、`MarkdownRendering.kt`。提取共享颜色到 `BuyPilotColors.kt` 并通过 `MaterialTheme.colorScheme` 路由。为提取的组件添加 `@Preview` 注解。
   - 参考文档：<https://developer.android.com/develop/ui/compose/api-guidelines>, <https://developer.android.com/develop/ui/compose/designsystems/material3>, <https://developer.android.com/develop/ui/compose/tooling/previews>

## 备注与限制

- 审计的 Compose 生产表面集中在 `app` 和 `feature:chat`；`feature:history` 目前仅暴露 ViewModel/状态，无 Compose UI
- 置信度为中等，因为无法生成编译器诊断（Java 11 vs Hilt Java 17 不匹配）。稳定性结论基于源码推断，而非编译器报告测量
- 相邻覆盖说明：存在 reducer/时间线存储、数据库和 SSE 解析的单元测试。未发现 Compose UI 测试、截图测试、`@Preview` 函数或 `androidTest` Compose 测试规则。输入框存在 Focus API，但未发现键盘/focus 测试。未发现 KMP/CMP 源码集
- Strong Skipping 模式：推断为开启（根据 `libs.versions.toml` 中的 Kotlin 2.1.0），但未通过编译器报告验证
- 权重选择：默认 35/25/20/20
- 重新归一化：无
- 编译器诊断使用：否。构建失败，错误为 `java.lang.UnsupportedClassVersionError: dagger/hilt/android/plugin/HiltGradlePlugin has been compiled by a more recent version of the Java Runtime (class file version 61.0)`。系统为 Java 11 但 Hilt 需要 Java 17。稳定性结论基于源码推断，非测量值

## 建议后续审计

- 运行 `material-3` 审计。此代码库有 67+ 硬编码颜色和自定义组件；设计系统和 Material 3 合规性有意未在本次审计范围内
- 如果 UI 行为需要保障，运行 `compose-agent focus on testing`。大型聊天界面没有 Compose UI 测试或 `@Preview`
- 如果硬件键盘、ChromeOS、D-pad 或焦点恢复对演示很重要，运行 `compose-agent focus on focus`。存在 Focus API，但未发现键盘/focus 测试
- `compose-agent focus on kmp` 目前不需要；未观察到 KMP 或 Compose Multiplatform 源码集

## 自上次审计（2026-05-31）的改进

| 问题 | 状态 | 证据 |
|------|------|------|
| `collectAsState()` 无生命周期感知 | ✓ 已修复 | `ChatRoute.kt:15`, `AppNavGraph.kt:54,90,157,196` 现在使用 `collectAsStateWithLifecycle()` |
| 输入框文本在重建时丢失 | ✓ 已修复 | `BuyPilotChatScreen.kt:601` 现在使用 `rememberSaveable` |
| 关键屏幕缺少 `modifier` | ✓ 已修复 | `ProductSwipeModeScreen:6618`, `ProductHeroDetailScreen:7612`, `ProductEvidenceOverlayScreen:8274` 现在暴露 `modifier` |
| `ProductCardStackView` modifier 顺序 | ✓ 已修复 | `BuyPilotChatScreen.kt:6971` 现在将 `modifier` 放在回调之前 |
| Splash 屏幕过期回调捕获 | ✓ 已修复 | `StartupSplash.kt:121` 现在使用 `rememberUpdatedState(onFinished)` |
| chip 行缺少 lazy list key | ✓ 已修复 | `ScrollableChipRow:10402`, `ScrollableQuickActionRow:10440` 现在使用 `itemsIndexed(..., key = ...)` |

---

**报告生成时间**：2026-06-01  
**审计工具**：Jetpack Compose Audit Skill v2.1.1  
**审计人员**：Claude（AI 辅助审计）
