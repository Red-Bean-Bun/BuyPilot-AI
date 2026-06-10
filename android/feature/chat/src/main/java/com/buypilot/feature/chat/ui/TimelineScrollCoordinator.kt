package com.buypilot.feature.chat.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.buypilot.feature.chat.model.AiStreamNode
import com.buypilot.feature.chat.model.CartActionNode
import com.buypilot.feature.chat.model.ChatUiNode
import com.buypilot.feature.chat.model.ClarificationNode
import com.buypilot.feature.chat.model.CompareCardNode
import com.buypilot.feature.chat.model.CriteriaNode
import com.buypilot.feature.chat.model.ErrorNode
import com.buypilot.feature.chat.model.FinalDecisionNode
import com.buypilot.feature.chat.model.ProductDeckNode
import com.buypilot.feature.chat.model.ThinkingNode
import com.buypilot.feature.chat.model.UserMessageNode
import com.buypilot.feature.chat.presentation.AssistantTurnTimelineItem
import com.buypilot.feature.chat.presentation.StandaloneTimelineItem
import com.buypilot.feature.chat.presentation.TimelineRenderItem
import com.buypilot.feature.chat.presentation.UserTimelineItem
import com.buypilot.feature.chat.presentation.containsNodeKey
import kotlin.math.abs

internal enum class TimelineScrollIntentKind(val priority: Int) {
    UserDrag(100),
    RouteReturn(90),
    ViewportFreeze(85),
    FinalDecision(80),
    UserSent(70),
    AssistantStarted(60),
    KeyboardChanged(50),
    FollowReadableTurn(40),
}

internal data class TimelineScrollIntent(
    val kind: TimelineScrollIntentKind,
    val key: String? = null,
    val turnId: String? = null,
    val index: Int = -1,
    val scrollOffset: Int = 0,
    val deltaPx: Int = 0,
    val allowOffscreenAnchor: Boolean = false,
    val allowWhileAutoFocusSuppressed: Boolean = false,
)

internal data class PendingTimelineScrollIntent(
    val serial: Int,
    val intent: TimelineScrollIntent,
)

@Stable
internal class TimelineViewportController(
    private val coordinator: TimelineScrollCoordinator = TimelineScrollCoordinator(),
) {
    val pendingIntent: PendingTimelineScrollIntent?
        get() = coordinator.pendingIntent

    val pendingIntentCount: Int
        get() = coordinator.pendingIntentCount

    val queuedIntentKinds: List<TimelineScrollIntentKind>
        get() = coordinator.queuedIntentKinds

    private fun request(
        intent: TimelineScrollIntent,
        isRouteReturnSuppressed: Boolean,
        isClarificationFlightActive: Boolean,
        isProductViewportMotionLocked: Boolean = false,
    ): Boolean =
        coordinator.request(
            intent = intent,
            autoFocusSuppressed = isTimelineAutoFocusSuppressedForIntent(
                intentKind = intent.kind,
                isRouteReturnSuppressed = isRouteReturnSuppressed,
                isClarificationFlightActive = isClarificationFlightActive,
                isProductViewportMotionLocked = isProductViewportMotionLocked,
            ),
        )

    fun requestRouteReturn(
        key: String?,
        index: Int,
        scrollOffset: Int,
        isRouteReturnSuppressed: Boolean,
        isClarificationFlightActive: Boolean,
    ): Boolean =
        request(
            intent = TimelineScrollIntent(
                kind = TimelineScrollIntentKind.RouteReturn,
                key = key,
                index = index,
                scrollOffset = scrollOffset,
            ),
            isRouteReturnSuppressed = isRouteReturnSuppressed,
            isClarificationFlightActive = isClarificationFlightActive,
        )

    fun requestViewportFreeze(
        key: String?,
        index: Int,
        scrollOffset: Int,
        isRouteReturnSuppressed: Boolean,
        isClarificationFlightActive: Boolean,
    ): Boolean =
        request(
            intent = TimelineScrollIntent(
                kind = TimelineScrollIntentKind.ViewportFreeze,
                key = key,
                index = index,
                scrollOffset = scrollOffset,
            ),
            isRouteReturnSuppressed = isRouteReturnSuppressed,
            isClarificationFlightActive = isClarificationFlightActive,
        )

    fun requestUserSent(
        key: String,
        index: Int,
        isRouteReturnSuppressed: Boolean,
        isClarificationFlightActive: Boolean,
    ): Boolean =
        request(
            intent = TimelineScrollIntent(
                kind = TimelineScrollIntentKind.UserSent,
                key = key,
                index = index,
            ),
            isRouteReturnSuppressed = isRouteReturnSuppressed,
            isClarificationFlightActive = isClarificationFlightActive,
        )

    fun requestAssistantStarted(
        turnId: String,
        index: Int,
        isRouteReturnSuppressed: Boolean,
        isClarificationFlightActive: Boolean,
        isProductViewportMotionLocked: Boolean = false,
    ): Boolean =
        request(
            intent = TimelineScrollIntent(
                kind = TimelineScrollIntentKind.AssistantStarted,
                turnId = turnId,
                index = index,
            ),
            isRouteReturnSuppressed = isRouteReturnSuppressed,
            isClarificationFlightActive = isClarificationFlightActive,
            isProductViewportMotionLocked = isProductViewportMotionLocked,
        )

    fun requestKeyboardChanged(
        deltaPx: Int,
        isRouteReturnSuppressed: Boolean,
        isClarificationFlightActive: Boolean,
        isProductViewportMotionLocked: Boolean = false,
    ): Boolean =
        request(
            intent = TimelineScrollIntent(
                kind = TimelineScrollIntentKind.KeyboardChanged,
                deltaPx = deltaPx,
            ),
            isRouteReturnSuppressed = isRouteReturnSuppressed,
            isClarificationFlightActive = isClarificationFlightActive,
            isProductViewportMotionLocked = isProductViewportMotionLocked,
        )

    fun requestFinalDecision(
        key: String,
        turnId: String?,
        index: Int,
        isRouteReturnSuppressed: Boolean,
        isClarificationFlightActive: Boolean,
        isProductViewportMotionLocked: Boolean = false,
    ): Boolean =
        request(
            intent = TimelineScrollIntent(
                kind = TimelineScrollIntentKind.FinalDecision,
                key = key,
                turnId = turnId,
                index = index,
            ),
            isRouteReturnSuppressed = isRouteReturnSuppressed,
            isClarificationFlightActive = isClarificationFlightActive,
            isProductViewportMotionLocked = isProductViewportMotionLocked,
        )

    fun requestReadableTurnFollow(
        allowOffscreenAnchor: Boolean,
        isRouteReturnSuppressed: Boolean,
        isClarificationFlightActive: Boolean,
        isProductViewportMotionLocked: Boolean = false,
        allowWhileAutoFocusSuppressed: Boolean = false,
    ): Boolean =
        request(
            intent = TimelineScrollIntent(
                kind = TimelineScrollIntentKind.FollowReadableTurn,
                allowOffscreenAnchor = allowOffscreenAnchor,
                allowWhileAutoFocusSuppressed = allowWhileAutoFocusSuppressed,
            ),
            isRouteReturnSuppressed = isRouteReturnSuppressed,
            isClarificationFlightActive = isClarificationFlightActive,
            isProductViewportMotionLocked = isProductViewportMotionLocked,
        )

    fun interruptWithUserDrag() {
        coordinator.interruptWithUserDrag()
    }

    fun clear(serial: Int) {
        coordinator.clear(serial)
    }
}

@Stable
internal class TimelineScrollCoordinator {
    private var serial by mutableIntStateOf(0)
    private var pendingIntents by mutableStateOf(emptyList<PendingTimelineScrollIntent>())

    val pendingIntent: PendingTimelineScrollIntent?
        get() = pendingIntents.firstOrNull()

    val pendingIntentCount: Int
        get() = pendingIntents.size

    val queuedIntentKinds: List<TimelineScrollIntentKind>
        get() = pendingIntents.map { it.intent.kind }

    fun request(
        intent: TimelineScrollIntent,
        autoFocusSuppressed: Boolean,
    ): Boolean {
        if (autoFocusSuppressed && !intent.canRunWhileAutoFocusSuppressed) return false
        if (
            intent.kind == TimelineScrollIntentKind.RouteReturn &&
            pendingIntents.any { queued -> queued.intent.kind == TimelineScrollIntentKind.UserSent }
        ) {
            return false
        }
        if (
            intent.kind == TimelineScrollIntentKind.ViewportFreeze &&
            pendingIntents.any { queued -> queued.intent.kind.isReadableTurnAnchor }
        ) {
            return false
        }
        if (
            intent.kind.isVolatile &&
            pendingIntents.any { queued -> queued.intent.kind.priority > intent.kind.priority }
        ) {
            return false
        }
        val sameKindIntent = pendingIntents.firstOrNull { pending ->
            pending.intent.kind == intent.kind
        }
        if (
            sameKindIntent != null &&
            sameKindIntent.intent.isSameScrollTarget(intent)
        ) {
            return false
        }
        val next = PendingTimelineScrollIntent(
            serial = nextSerial(),
            intent = intent,
        )
        pendingIntents = (pendingIntents
            .filterNot { queued ->
                queued.intent.kind == intent.kind ||
                    intent.kind.supersedesQueuedIntent(queued.intent.kind)
            } + next)
            .sortedWith(
                compareByDescending<PendingTimelineScrollIntent> { it.intent.kind.priority }
                    .thenBy { it.serial },
            )
        return true
    }

    fun interruptWithUserDrag() {
        pendingIntents = listOf(
            PendingTimelineScrollIntent(
                serial = nextSerial(),
                intent = TimelineScrollIntent(kind = TimelineScrollIntentKind.UserDrag),
            ),
        )
    }

    fun clear(serial: Int) {
        if (pendingIntents.any { it.serial == serial }) {
            pendingIntents = pendingIntents.filterNot { it.serial == serial }
        }
    }

    private fun nextSerial(): Int {
        serial += 1
        return serial
    }

    private fun TimelineScrollIntent.isSameScrollTarget(other: TimelineScrollIntent): Boolean =
        kind == other.kind &&
            key == other.key &&
            turnId == other.turnId &&
            index == other.index &&
            scrollOffset == other.scrollOffset &&
            deltaPx == other.deltaPx &&
            allowOffscreenAnchor == other.allowOffscreenAnchor &&
            allowWhileAutoFocusSuppressed == other.allowWhileAutoFocusSuppressed
}

private val TimelineScrollIntent.canRunWhileAutoFocusSuppressed: Boolean
    get() = kind == TimelineScrollIntentKind.RouteReturn ||
        kind == TimelineScrollIntentKind.ViewportFreeze ||
        kind == TimelineScrollIntentKind.UserSent ||
        (kind == TimelineScrollIntentKind.FollowReadableTurn && allowWhileAutoFocusSuppressed)

private val TimelineScrollIntentKind.isVolatile: Boolean
    get() = this == TimelineScrollIntentKind.ViewportFreeze
        || this == TimelineScrollIntentKind.FollowReadableTurn
        || this == TimelineScrollIntentKind.KeyboardChanged

private fun TimelineScrollIntentKind.supersedesQueuedIntent(queuedKind: TimelineScrollIntentKind): Boolean =
    when (this) {
        TimelineScrollIntentKind.UserDrag -> true
        TimelineScrollIntentKind.RouteReturn -> queuedKind.priority < priority &&
            queuedKind != TimelineScrollIntentKind.UserSent
        TimelineScrollIntentKind.ViewportFreeze -> queuedKind.priority < priority &&
            queuedKind != TimelineScrollIntentKind.RouteReturn &&
            queuedKind != TimelineScrollIntentKind.UserSent
        TimelineScrollIntentKind.FinalDecision -> queuedKind == TimelineScrollIntentKind.UserSent ||
            queuedKind == TimelineScrollIntentKind.AssistantStarted ||
            queuedKind == TimelineScrollIntentKind.KeyboardChanged ||
            queuedKind == TimelineScrollIntentKind.FollowReadableTurn
        TimelineScrollIntentKind.UserSent -> queuedKind == TimelineScrollIntentKind.RouteReturn ||
            queuedKind == TimelineScrollIntentKind.ViewportFreeze ||
            queuedKind == TimelineScrollIntentKind.FinalDecision ||
            queuedKind == TimelineScrollIntentKind.AssistantStarted ||
            queuedKind == TimelineScrollIntentKind.KeyboardChanged ||
            queuedKind == TimelineScrollIntentKind.FollowReadableTurn
        TimelineScrollIntentKind.AssistantStarted -> queuedKind == TimelineScrollIntentKind.ViewportFreeze ||
            queuedKind == TimelineScrollIntentKind.FollowReadableTurn
        TimelineScrollIntentKind.KeyboardChanged -> queuedKind == TimelineScrollIntentKind.FollowReadableTurn
        TimelineScrollIntentKind.FollowReadableTurn -> false
    }

private val TimelineScrollIntentKind.isReadableTurnAnchor: Boolean
    get() = this == TimelineScrollIntentKind.UserSent ||
        this == TimelineScrollIntentKind.AssistantStarted

internal fun shouldScrollTimelineForKeyboardChange(
    deltaPx: Int,
    isComposerFocused: Boolean,
    isUserDragging: Boolean,
    wasNearEndBeforeKeyboard: Boolean,
): Boolean = deltaPx > 0 &&
    isComposerFocused &&
    !isUserDragging &&
    wasNearEndBeforeKeyboard

internal fun shouldReanchorReadableTurnAfterKeyboardCollapse(
    deltaPx: Int,
    currentTurnFollowActive: Boolean,
    isUserDragging: Boolean,
): Boolean = deltaPx < 0 &&
    currentTurnFollowActive &&
    !isUserDragging

internal fun shouldShowTimelineReadableTurnBubble(
    isComposerFocused: Boolean,
    keyboardVisible: Boolean,
    isNearTimelineEnd: Boolean,
    userDetachedFromLatest: Boolean,
    currentTurnFollowActive: Boolean,
): Boolean =
    !isComposerFocused &&
        !keyboardVisible &&
        !isNearTimelineEnd &&
        userDetachedFromLatest &&
        !currentTurnFollowActive

internal fun shouldPauseTimelineFollowForUserDrag(
    isUserDragging: Boolean,
    isNearTimelineEnd: Boolean,
): Boolean = isUserDragging && !isNearTimelineEnd

/**
 * 流式底部占位高度：`ideal = 视口 - 锚位 - 当前 turn 内容高`。
 * 通过 [previousHeightPx] 实现 turn 内单调收缩（只减不增），
 * 避免内容生长与占位伸长互相打架导致视口抖动。
 */
internal fun computeStreamingTrailSpacerHeightPx(
    viewportHeightPx: Int,
    anchorTopPx: Int,
    turnContentHeightPx: Int,
    minBufferPx: Int,
    previousHeightPx: Int,
): Int {
    if (viewportHeightPx <= 0) return 0
    val minHeight = minBufferPx.coerceIn(0, viewportHeightPx)
    val ideal = viewportHeightPx - anchorTopPx - turnContentHeightPx
    return ideal
        .coerceIn(minHeight, viewportHeightPx)
        .coerceAtMost(previousHeightPx)
        .coerceAtLeast(minHeight)
}

internal fun shouldAnchorAssistantStartedTurn(
    turnId: String?,
    lastAnchoredTurnId: String?,
    routeReturnSettledTurnId: String?,
    autoFocusSuppressed: Boolean,
    manualScrollActive: Boolean,
    isClarificationFlightActive: Boolean,
    userDetachedFromLatest: Boolean,
): Boolean {
    val turn = turnId?.takeIf { it.isNotBlank() } ?: return false
    if (autoFocusSuppressed || manualScrollActive || isClarificationFlightActive || userDetachedFromLatest) {
        return false
    }
    return turn != lastAnchoredTurnId && turn != routeReturnSettledTurnId
}

internal fun shouldMarkUserMessageHandledOnRouteReturn(
    capturedKey: String?,
    latestKey: String?,
): Boolean = capturedKey != null && capturedKey == latestKey

internal fun shouldAutoFocusTimelineFinalDecision(
    decisionKey: String?,
    decisionTurnId: String?,
    currentTurnId: String?,
    lastFocusedDecisionKey: String?,
    routeReturnSettledDecisionKey: String?,
    autoFocusSuppressed: Boolean,
    manualScrollActive: Boolean,
    userDetachedFromLatest: Boolean = false,
    currentTurnFollowActive: Boolean = true,
): Boolean {
    val key = decisionKey?.takeIf { it.isNotBlank() } ?: return false
    if (autoFocusSuppressed || manualScrollActive || userDetachedFromLatest || !currentTurnFollowActive) return false
    if (key == routeReturnSettledDecisionKey || key == lastFocusedDecisionKey) return false
    val decisionTurn = decisionTurnId?.takeIf { it.isNotBlank() }
    val currentTurn = currentTurnId?.takeIf { it.isNotBlank() }
    return decisionTurn == null || currentTurn == null || decisionTurn == currentTurn
}

internal fun shouldPreserveUserBubbleAnchorForAssistantStart(
    pendingUserBubbleAnchorKey: String?,
    latestUserMessageKey: String?,
    userItemIndex: Int,
    assistantItemIndex: Int,
): Boolean {
    val anchorKey = pendingUserBubbleAnchorKey?.takeIf { it.isNotBlank() } ?: return false
    val userKey = latestUserMessageKey?.takeIf { it.isNotBlank() } ?: return false
    return anchorKey == userKey && userItemIndex >= 0 && assistantItemIndex > userItemIndex
}

internal fun shouldBlockTimelineAutoFocusForManualScroll(
    intentKind: TimelineScrollIntentKind,
    isManualScrollActive: Boolean,
): Boolean =
    isManualScrollActive &&
        intentKind != TimelineScrollIntentKind.UserSent &&
        intentKind != TimelineScrollIntentKind.RouteReturn &&
        intentKind != TimelineScrollIntentKind.ViewportFreeze

internal fun shouldBlockTimelineAutoFocusForSuppression(
    intentKind: TimelineScrollIntentKind,
    isAutoFocusSuppressed: Boolean,
): Boolean =
    shouldBlockTimelineAutoFocusForSuppression(
        intent = TimelineScrollIntent(kind = intentKind),
        isAutoFocusSuppressed = isAutoFocusSuppressed,
    )

internal fun shouldBlockTimelineAutoFocusForSuppression(
    intent: TimelineScrollIntent,
    isAutoFocusSuppressed: Boolean,
): Boolean =
    isAutoFocusSuppressed && !intent.canRunWhileAutoFocusSuppressed

internal fun isTimelineAutoFocusSuppressedForIntent(
    intentKind: TimelineScrollIntentKind,
    isRouteReturnSuppressed: Boolean,
    isClarificationFlightActive: Boolean,
    isProductViewportMotionLocked: Boolean = false,
): Boolean =
    when (intentKind) {
        TimelineScrollIntentKind.UserDrag,
        TimelineScrollIntentKind.UserSent,
        TimelineScrollIntentKind.RouteReturn,
        TimelineScrollIntentKind.ViewportFreeze -> false

        TimelineScrollIntentKind.AssistantStarted -> isRouteReturnSuppressed ||
            isClarificationFlightActive ||
            isProductViewportMotionLocked

        TimelineScrollIntentKind.FinalDecision,
        TimelineScrollIntentKind.KeyboardChanged,
        TimelineScrollIntentKind.FollowReadableTurn -> isRouteReturnSuppressed ||
            isClarificationFlightActive ||
            isProductViewportMotionLocked
    }

internal fun shouldFreezeTimelineViewport(
    currentIndex: Int,
    currentOffset: Int,
    targetIndex: Int,
    targetOffset: Int,
    tolerancePx: Float,
): Boolean =
    currentIndex != targetIndex ||
        abs(currentOffset - targetOffset) > tolerancePx

internal fun routeAwareFocusedFinalDecisionKey(
    currentFocusedKey: String?,
    routeReturnAnchorCaptured: Boolean,
    capturedFinalDecisionKey: String?,
    latestFinalDecisionKey: String?,
): String? =
    if (routeReturnAnchorCaptured && latestFinalDecisionKey != capturedFinalDecisionKey) {
        capturedFinalDecisionKey
    } else {
        currentFocusedKey
    }

internal fun routeReturnAnchorTurnId(
    timelineItems: List<TimelineRenderItem>,
    visibleItemIndex: Int,
    anchorNodeKey: String?,
    currentTurnId: String?,
): String? {
    if (timelineItems.isEmpty()) return currentTurnId?.takeIf { it.isNotBlank() }
    val anchoredItem = anchorNodeKey
        ?.let { key -> timelineItems.firstOrNull { item -> item.containsNodeKey(key) } }
    val visibleItem = timelineItems.getOrNull(visibleItemIndex.coerceIn(0, timelineItems.lastIndex))
    return (anchoredItem ?: visibleItem)
        ?.assistantTurnIdOrNull()
        ?: currentTurnId?.takeIf { it.isNotBlank() }
}

private fun TimelineRenderItem.assistantTurnIdOrNull(): String? =
    when (this) {
        is AssistantTurnTimelineItem -> turnId
        is StandaloneTimelineItem -> node.assistantTurnIdOrNull()
        is UserTimelineItem -> null
    }?.takeIf { it.isNotBlank() }

private fun ChatUiNode.assistantTurnIdOrNull(): String? =
    when (this) {
        is ThinkingNode -> turnId
        is AiStreamNode -> turnId
        is ClarificationNode -> turnId
        is CriteriaNode -> turnId
        is ProductDeckNode -> turnId
        is FinalDecisionNode -> turnId
        is CompareCardNode -> turnId
        is CartActionNode -> turnId
        is ErrorNode -> turnId
        is UserMessageNode -> null
    }?.takeIf { it.isNotBlank() }
