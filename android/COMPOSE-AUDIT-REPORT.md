# Jetpack Compose Audit Report

Target: `/mnt/disk1/LZJ/project/BuyPilot-AI/android`
Date: 2026-05-31
Scope: `:app` and `:feature:chat` production Compose sources, with supporting reads in `core:*` state/model/data code and `feature:history` ViewModel code.
Excluded from scoring: `*/src/test/*`, generated `*/build/*` outputs except Compose compiler diagnostics, non-Compose core modules except for state/model context.
Confidence: High
Overall Score: 60/100

## Scorecard

| Category | Score | Weight | Status | Notes |
|----------|-------|--------|--------|-------|
| Performance | 6/10 | 35% | needs work | Named composables are fully skippable under Strong Skipping, but hot timeline derivation, markdown parsing, per-frame animation reads, and missing keys in chip rows hold the score down. |
| State management | 6/10 | 25% | needs work | ViewModel and reducer ownership are good, but Android flows are collected without lifecycle awareness and several user-visible UI states are plain `remember` or keyed too narrowly. |
| Side effects | 7/10 | 20% | solid | Most imperative work is in effects or handlers; remaining risk is stale captures in long-lived effects. |
| Composable API quality | 5/10 | 20% | needs work | Some components follow `modifier` conventions, but route screens and reusable controls omit `modifier`, put it late, or bundle too much component responsibility into one file. |

## Critical Findings

1. **Performance: hot chat rendering recomputes derived collections and reads animation progress in composition**
   - Why it matters: `state.nodes` changes during streaming, so timeline/product maps and convergence sets are rebuilt on the composition thread. Several animations expose `Animatable.value` as a `Float`, causing the caller and content to recompose every frame instead of deferring reads to layout/draw.
   - Evidence: `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:1549`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:1550`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:1575`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:3605`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:4747`.
   - Fix direction: move timeline/product/convergence derivation into a presentation state holder or ViewModel reducer keyed by semantic changes, and keep per-frame animation reads inside lambda modifiers such as `graphicsLayer { ... }` where the value does not need to rebuild child content.
   - References: <https://developer.android.com/develop/ui/compose/performance/bestpractices>, <https://developer.android.com/develop/ui/compose/performance/phases>, <https://developer.android.com/develop/ui/compose/animation/value-based>

2. **State management: `StateFlow` is collected without lifecycle awareness in Android UI**
   - Why it matters: `collectAsState()` keeps collecting according to composition, not the Android lifecycle. These route destinations can collect while not started, wasting work and increasing risk around navigation transitions.
   - Evidence: `feature/chat/src/main/java/com/buypilot/feature/chat/ChatRoute.kt:15`, `app/src/main/java/com/buypilot/navigation/AppNavGraph.kt:83`, `app/src/main/java/com/buypilot/navigation/AppNavGraph.kt:135`, `app/src/main/java/com/buypilot/navigation/AppNavGraph.kt:167`.
   - Fix direction: add `androidx.lifecycle:lifecycle-runtime-compose` and replace these call sites with `collectAsStateWithLifecycle()`.
   - References: <https://developer.android.com/develop/ui/compose/state>

3. **State management: user-visible UI state is either not saveable or keyed too narrowly**
   - Why it matters: composer text and sheet/reveal state reset on configuration changes, while criteria editor fields only key off `criteriaId`; a changed criteria payload with the same ID can keep stale text in the editor.
   - Evidence: `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:588`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:590`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:593`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:8613`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:8617`.
   - Fix direction: use `rememberSaveable` for small Bundle-safe text/selection state, provide a `Saver` or hoist non-Bundle state, and key editor fields by the actual criteria values or reset them in `LaunchedEffect(payload.criteria)`.
   - References: <https://developer.android.com/develop/ui/compose/state>, <https://developer.android.com/develop/ui/compose/state-hoisting>

4. **Composable API quality: reusable screens and controls do not consistently expose or place `modifier`**
   - Why it matters: missing or late `modifier` parameters make components harder to host, test, and reuse in different layouts. This is visible in public route composables and AndroidView-backed controls.
   - Evidence: `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:5945`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:6300`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:6306`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:6941`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:7578`.
   - Fix direction: add `modifier: Modifier = Modifier` to reusable screens/components and place it after required parameters and before optional behavior parameters. Apply it once to the root emitted layout.
   - References: <https://developer.android.com/develop/ui/compose/api-guidelines>, <https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md>

## Category Details

### Performance - 6/10

**Ceiling check**

- Strong Skipping: on in both measured Compose modules (`feature/chat/build/compose_audit/chat_release-module.json:25`, `app/build/compose_audit/app_release-module.json:25`).
- Ceiling table applied: SSM-on. Kotlin is `2.1.0`, and both module reports set `"StrongSkipping": true`.
- Module-wide `skippable%`: `(207 + 8) / (397 + 15) = 215/412 = 52.2%`. This is anchored by lambda rows and is not the binding metric under Strong Skipping.
- Named-only `skippable%`: `(144 + 4) / (144 + 4) = 148/148 = 100.0%`.
- Unstable shared types from compiler: 5 inferred unstable classes in `feature/chat` (`ChatViewModel`, `TurnNodeVisibilityState`, `ProductCardViewHolder`, `Criteria`, `DecisionEvidence`) plus unstable collection properties on otherwise stable state classes. Evidence: `feature/chat/build/compose_audit/chat_release-classes.txt:1`, `feature/chat/build/compose_audit/chat_release-classes.txt:109`, `feature/chat/build/compose_audit/chat_release-classes.txt:146`, `feature/chat/build/compose_audit/chat_release-classes.txt:164`, `feature/chat/build/compose_audit/chat_release-classes.txt:184`.
- SSM-on binding evidence: widespread hot-path source churn and composition-thread derived work around `state.nodes`, plus per-frame animation values read before `graphicsLayer` and passed to child content.
- Qualitative score: 6/10
- Ceiling: cap at 6 under the SSM-on table because the source shows recurring instance/collection recreation and animation recomposition in hot reusable paths.
- Applied score: 6/10

**What is working**

- Strong Skipping is active and named restartable composables are skippable in both measured modules. Evidence: `feature/chat/build/compose_audit/chat_release-composables.csv:2`, `app/build/compose_audit/app_release-composables.csv:2`. References: <https://developer.android.com/develop/ui/compose/performance/stability/strongskipping>
- The main timeline `LazyColumn` uses stable item keys and `contentType`, which is the right shape for heterogeneous chat content. Evidence: `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:1699`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:1701`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:1702`. References: <https://developer.android.com/develop/ui/compose/lists>
- Several target-driven animations use `remember { Animatable(...) }` plus `LaunchedEffect(key)`, which gives correct cancellation semantics. Evidence: `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:3611`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:3612`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:4729`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:4733`. References: <https://developer.android.com/develop/ui/compose/animation/value-based>, <https://developer.android.com/develop/ui/compose/side-effects>

**What is hurting the score**

- `ChatTimeline` derives timeline items, product maps, reveal keys, and convergence sets from `state.nodes` during composition. Because streaming text updates replace a node on each delta, this work repeats on a hot path. References: <https://developer.android.com/develop/ui/compose/performance/bestpractices>
- Animation helpers expose `progress.value` as a `Float` and pass it into child content, so per-frame changes recompose more of the tree than necessary. References: <https://developer.android.com/develop/ui/compose/performance/phases>, <https://developer.android.com/develop/ui/compose/animation/value-based>
- `MarkdownTextBlock` parses Markwon output in a `remember` block on the composition thread. `remember` caches repeat calls but does not move first render or key changes off the UI composition path. References: <https://developer.android.com/develop/ui/compose/performance/bestpractices>
- Two dynamic horizontal chip rows omit stable lazy item keys. References: <https://developer.android.com/develop/ui/compose/lists>

**Evidence**

- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:1550` - `state.nodes.toTimelineRenderItems()` is recomputed whenever the streaming node list changes. References: <https://developer.android.com/develop/ui/compose/performance/bestpractices>
- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:1557` - product deck extraction is keyed to the whole node list rather than to product/decision changes. References: <https://developer.android.com/develop/ui/compose/performance/bestpractices>
- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:1575` - `state.nodes.convergedProductDeckIds()` is computed directly inside the `remember` key list and again in the value block. References: <https://developer.android.com/develop/ui/compose/performance/bestpractices>
- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:3470` - Markwon parsing runs in composition via `remember(appContext, displayContent)`. References: <https://developer.android.com/develop/ui/compose/performance/bestpractices>
- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:3623` - `rememberRouteEnterProgress` returns `Animatable.value`, forcing callers to observe animation frames in composition. References: <https://developer.android.com/develop/ui/compose/performance/phases>, <https://developer.android.com/develop/ui/compose/animation/value-based>
- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:4747` - `StructuredCardMotion` reads `progress.value` before `graphicsLayer` and passes `t` to `content(t)`, causing content recomposition during the reveal. References: <https://developer.android.com/develop/ui/compose/performance/phases>, <https://developer.android.com/develop/ui/compose/animation/value-based>
- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:9716` - `ScrollableChipRow` uses `itemsIndexed(labels)` without `key =`. References: <https://developer.android.com/develop/ui/compose/lists>
- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:9752` - `ScrollableQuickActionRow` uses `items(actions)` without `key =`. References: <https://developer.android.com/develop/ui/compose/lists>
- `app/build.gradle.kts:26` - release has `isMinifyEnabled = false`; this weakens release-performance hygiene for an app nearing demo/release polish. References: <https://developer.android.com/develop/ui/compose/performance/baseline-profiles>

### State Management - 6/10

**What is working**

- `ChatViewModel` owns a single `StateFlow<ChatUiState>` and reducers return copied immutable state instead of mutating UI state in composables. Evidence: `feature/chat/src/main/java/com/buypilot/feature/chat/ChatViewModel.kt:48`, `feature/chat/src/main/java/com/buypilot/feature/chat/ChatViewModel.kt:54`, `feature/chat/src/main/java/com/buypilot/feature/chat/state/ChatReducer.kt:29`. References: <https://developer.android.com/develop/ui/compose/architecture>
- Chat nodes and UI state are modeled as immutable data classes/sealed interfaces, which keeps ownership mostly unidirectional. Evidence: `feature/chat/src/main/java/com/buypilot/feature/chat/model/ChatUiNode.kt:10`, `feature/chat/src/main/java/com/buypilot/feature/chat/state/ChatUiState.kt:12`. References: <https://developer.android.com/develop/ui/compose/state-hoisting>
- Primitive Compose state uses typed factories in several hot places. Evidence: `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:607`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:9249`. References: <https://developer.android.com/develop/ui/compose/state>

**What is hurting the score**

- Android UI collects `StateFlow` with `collectAsState()` instead of `collectAsStateWithLifecycle()`. References: <https://developer.android.com/develop/ui/compose/state>
- Root chat UI owns many pieces of user-visible state with plain `remember`, including composer text, sheet state, dismissed clarification keys, and reveal keys. Some of this should be saveable or hoisted. References: <https://developer.android.com/develop/ui/compose/state>, <https://developer.android.com/develop/ui/compose/state-hoisting>
- Criteria editor fields key their remembered state only by `criteriaId`, which can make the editor stale if the payload values change under the same criteria ID. References: <https://developer.android.com/develop/ui/compose/state>

**Evidence**

- `feature/chat/src/main/java/com/buypilot/feature/chat/ChatRoute.kt:15` - `viewModel.uiState.collectAsState()` should be lifecycle-aware in Android UI. References: <https://developer.android.com/develop/ui/compose/state>
- `app/src/main/java/com/buypilot/navigation/AppNavGraph.kt:83` - product deck route collects with `collectAsState()` while hosted by Navigation Compose. References: <https://developer.android.com/develop/ui/compose/state>
- `app/src/main/java/com/buypilot/navigation/AppNavGraph.kt:135` - product detail route repeats the same non-lifecycle-aware collection. References: <https://developer.android.com/develop/ui/compose/state>
- `app/src/main/java/com/buypilot/navigation/AppNavGraph.kt:167` - evidence overlay route repeats the same non-lifecycle-aware collection. References: <https://developer.android.com/develop/ui/compose/state>
- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:588` - composer input is plain `remember`, so partially typed text is lost on recreation. References: <https://developer.android.com/develop/ui/compose/state>
- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:590` - bottom-sheet content is held locally in the screen root rather than a saveable state holder or route-level state. References: <https://developer.android.com/develop/ui/compose/state-hoisting>
- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:8613` - editor fields cache values using only `payload.criteria.criteriaId`. References: <https://developer.android.com/develop/ui/compose/state>
- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:8617` - exclusions repeat the same narrow keying and may keep stale text. References: <https://developer.android.com/develop/ui/compose/state>

### Side Effects - 7/10

**What is working**

- Navigation and product selection side effects are generally event-driven or inside `LaunchedEffect`. Evidence: `app/src/main/java/com/buypilot/navigation/AppNavGraph.kt:87`, `app/src/main/java/com/buypilot/navigation/AppNavGraph.kt:95`, `app/src/main/java/com/buypilot/navigation/AppNavGraph.kt:144`. References: <https://developer.android.com/develop/ui/compose/side-effects>, <https://developer.android.com/develop/ui/compose/navigation>
- Long-running text reveal uses `rememberUpdatedState` for changing callbacks and content, which avoids stale lambda capture while preserving effect lifetime. Evidence: `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:3193`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:3206`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:3209`. References: <https://developer.android.com/develop/ui/compose/side-effects>
- Scroll snapping uses `snapshotFlow` inside `LaunchedEffect`, which is the correct state-to-flow boundary. Evidence: `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:7047`. References: <https://developer.android.com/develop/ui/compose/side-effects>

**What is hurting the score**

- A clarification dismissal effect is keyed only by `dismissing` but captures `nodeKey` and `onCardDismissed`, so a node/callback change during the delay can call the wrong target. References: <https://developer.android.com/develop/ui/compose/side-effects>
- The splash one-shot effect captures `onFinished` without `rememberUpdatedState`; this is a light risk because it is a run-once animation, but the official pattern is to keep the latest callback when an effect should not restart. References: <https://developer.android.com/develop/ui/compose/side-effects>

**Evidence**

- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:3740` - `LaunchedEffect(dismissing)` captures `nodeKey` but omits it from keys. References: <https://developer.android.com/develop/ui/compose/side-effects>
- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:3744` - delayed callback uses the captured `nodeKey`. References: <https://developer.android.com/develop/ui/compose/side-effects>
- `app/src/main/java/com/buypilot/ui/StartupSplash.kt:121` - `LaunchedEffect(Unit)` captures `onFinished` for a delayed call. References: <https://developer.android.com/develop/ui/compose/side-effects>
- `app/src/main/java/com/buypilot/ui/StartupSplash.kt:124` - the captured callback is invoked after `SplashHoldMillis`; use `rememberUpdatedState(onFinished)` if the effect should remain one-shot. References: <https://developer.android.com/develop/ui/compose/side-effects>

### Composable API Quality - 5/10

**What is working**

- Several smaller components do expose `modifier: Modifier = Modifier` and apply it to the root node. Evidence: `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:1216`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:1359`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:10134`. References: <https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md>
- The main chat route is stateful and delegates rendering to a state-parameterized `BuyPilotChatScreen`, which is a good app-level route/screen split. Evidence: `feature/chat/src/main/java/com/buypilot/feature/chat/ChatRoute.kt:17`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:575`. References: <https://developer.android.com/develop/ui/compose/architecture>

**What is hurting the score**

- Public route-like composables and reusable controls are inconsistent about `modifier`, which limits host control and testability. References: <https://developer.android.com/develop/ui/compose/api-guidelines>, <https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md>
- `ProductCardStackView` places `modifier` after behavior callbacks, contrary to the required-params, `modifier`, optional-params ordering guideline. References: <https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md>
- The single `BuyPilotChatScreen.kt` file contains route screens, screen state, reusable controls, AndroidView interop, markdown rendering, product details, evidence overlays, and editor sheets. This makes component purpose and layering hard to audit and evolve. References: <https://developer.android.com/develop/ui/compose/api-guidelines>, <https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md>

**Evidence**

- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:5945` - `ProductSwipeModeScreen` has no `modifier` parameter. References: <https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md>
- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:6300` - `ProductCardStackView` is reusable AndroidView interop but places `modifier` after callbacks. References: <https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md>
- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:6941` - `ProductHeroDetailScreen` has no `modifier` parameter. References: <https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md>
- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:7578` - `ProductEvidenceOverlayScreen` has no `modifier` parameter. References: <https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md>
- `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:9654` - reusable section/chip helpers are defined without modifier parameters in this area and nearby rows. References: <https://developer.android.com/develop/ui/compose/api-guidelines>

## Prioritized Fixes

1. Replace route-level `collectAsState()` with `collectAsStateWithLifecycle()` in `feature/chat/src/main/java/com/buypilot/feature/chat/ChatRoute.kt:15` and `app/src/main/java/com/buypilot/navigation/AppNavGraph.kt:83`, `app/src/main/java/com/buypilot/navigation/AppNavGraph.kt:135`, `app/src/main/java/com/buypilot/navigation/AppNavGraph.kt:167`. References: <https://developer.android.com/develop/ui/compose/state>. Expected impact: lifecycle-correct collection and less offscreen work during navigation.
2. Move hot timeline/product derivation and convergence calculation out of composition, starting with `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:1549` to `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:1575`, and stop returning per-frame animation progress from `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:3605` and `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:4719` when layout/draw-phase reads are enough. References: <https://developer.android.com/develop/ui/compose/performance/bestpractices>, <https://developer.android.com/develop/ui/compose/performance/phases>. Expected impact under SSM-on: removes the source-level churn that currently binds the Performance cap at 6.
3. Make user-visible local state saveable or hoisted, and key editor fields by the actual criteria payload: `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:588`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:590`, `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:8613` to `feature/chat/src/main/java/com/buypilot/feature/chat/ui/BuyPilotChatScreen.kt:8617`. References: <https://developer.android.com/develop/ui/compose/state>, <https://developer.android.com/develop/ui/compose/state-hoisting>. Expected impact: avoids lost draft input and stale criteria edit values after recreation or refreshed payloads.
4. Normalize reusable component APIs by adding/root-applying `modifier` and placing it before optional behavior callbacks in `ProductSwipeModeScreen`, `ProductHeroDetailScreen`, `ProductEvidenceOverlayScreen`, and `ProductCardStackView`. References: <https://developer.android.com/develop/ui/compose/api-guidelines>, <https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md>.

## Notes And Limits

- The audited Compose production surface is concentrated in `app` and `feature:chat`; `feature:history` currently exposes only ViewModel/state and no Compose UI.
- Confidence is high because production Compose files, state holders, reducers, build files, and compiler diagnostics were read. The largest UI file is still too broad for a line-by-line design review; this audit sampled representative sections and verified every scored deduction against source.
- Adjacent coverage notes: unit tests exist for reducer/timeline store, database, and SSE parsing. No Compose UI tests, screenshot tests, `@Preview` functions, or `androidTest` Compose test rules were found. Focus APIs are present for the composer, but no key/focus tests were found. No KMP/CMP source sets were found.
- Strong Skipping mode: on in both compiler-reporting modules.
- Weight choice: default 35/25/20/20.
- Renormalization: none.
- Compiler diagnostics used: yes. Reports generated by `./gradlew :app:compileReleaseKotlin --init-script ... --no-daemon --quiet` under `app/build/compose_audit/` and `feature/chat/build/compose_audit/`. Modules contributing: `:app`, `:feature:chat`. Module-wide skippability and named-only skippability were available; SSM-on ceiling table was applied.

## Suggested Follow-Up

- Run `material-3` audit next. This codebase has many custom colors, hardcoded text sizes, and bespoke components; design-system and Material 3 compliance are intentionally out of scope for this audit.
- Run `compose-agent focus on testing` next if UI behavior needs confidence. There are no Compose UI tests or previews for the large chat surface.
- Run `compose-agent focus on focus` if hardware keyboard, ChromeOS, D-pad, or focus restoration matter for the demo. Focus APIs are present, but no focus/key tests were found.
- `compose-agent focus on kmp` is not needed now; no KMP or Compose Multiplatform source sets were observed.
