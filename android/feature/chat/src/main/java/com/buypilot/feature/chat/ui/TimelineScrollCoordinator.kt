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
    ): Boolean =
        coordinator.request(
            intent = intent,
            autoFocusSuppressed = isTimelineAutoFocusSuppressedForIntent(
                intentKind = intent.kind,
                isRouteReturnSuppressed = isRouteReturnSuppressed,
                isClarificationFlightActive = isClarificationFlightActive,
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
    ): Boolean =
        request(
            intent = TimelineScrollIntent(
                kind = TimelineScrollIntentKind.AssistantStarted,
                turnId = turnId,
                index = index,
            ),
            isRouteReturnSuppressed = isRouteReturnSuppressed,
            isClarificationFlightActive = isClarificationFlightActive,
        )

    fun requestKeyboardChanged(
        deltaPx: Int,
        isRouteReturnSuppressed: Boolean,
        isClarificationFlightActive: Boolean,
    ): Boolean =
        request(
            intent = TimelineScrollIntent(
                kind = TimelineScrollIntentKind.KeyboardChanged,
                deltaPx = deltaPx,
            ),
            isRouteReturnSuppressed = isRouteReturnSuppressed,
            isClarificationFlightActive = isClarificationFlightActive,
        )

    fun requestFinalDecision(
        key: String,
        turnId: String?,
        index: Int,
        isRouteReturnSuppressed: Boolean,
        isClarificationFlightActive: Boolean,
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
        )

    fun requestReadableTurnFollow(
        allowOffscreenAnchor: Boolean,
        isRouteReturnSuppressed: Boolean,
        isClarificationFlightActive: Boolean,
    ): Boolean =
        request(
            intent = TimelineScrollIntent(
                kind = TimelineScrollIntentKind.FollowReadableTurn,
                allowOffscreenAnchor = allowOffscreenAnchor,
            ),
            isRouteReturnSuppressed = isRouteReturnSuppressed,
            isClarificationFlightActive = isClarificationFlightActive,
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
        if (autoFocusSuppressed && !intent.kind.canRunWhileAutoFocusSuppressed) return false
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
            allowOffscreenAnchor == other.allowOffscreenAnchor
}

private val TimelineScrollIntentKind.canRunWhileAutoFocusSuppressed: Boolean
    get() = this == TimelineScrollIntentKind.RouteReturn ||
        this == TimelineScrollIntentKind.ViewportFreeze ||
        this == TimelineScrollIntentKind.UserSent

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

internal fun shouldAutoFocusTimelineFinalDecision(
    decisionKey: String?,
    decisionTurnId: String?,
    currentTurnId: String?,
    lastFocusedDecisionKey: String?,
    routeReturnSettledDecisionKey: String?,
    autoFocusSuppressed: Boolean,
    manualScrollActive: Boolean,
    userDetachedFromLatest: Boolean = false,
): Boolean {
    val key = decisionKey?.takeIf { it.isNotBlank() } ?: return false
    if (autoFocusSuppressed || manualScrollActive || userDetachedFromLatest) return false
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
    isAutoFocusSuppressed && !intentKind.canRunWhileAutoFocusSuppressed

internal fun isTimelineAutoFocusSuppressedForIntent(
    intentKind: TimelineScrollIntentKind,
    isRouteReturnSuppressed: Boolean,
    isClarificationFlightActive: Boolean,
): Boolean =
    when (intentKind) {
        TimelineScrollIntentKind.UserDrag,
        TimelineScrollIntentKind.UserSent,
        TimelineScrollIntentKind.RouteReturn,
        TimelineScrollIntentKind.ViewportFreeze -> false

        TimelineScrollIntentKind.AssistantStarted -> isRouteReturnSuppressed

        TimelineScrollIntentKind.FinalDecision,
        TimelineScrollIntentKind.KeyboardChanged,
        TimelineScrollIntentKind.FollowReadableTurn -> isRouteReturnSuppressed || isClarificationFlightActive
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
        is CartActionNode -> turnId
        is ErrorNode -> turnId
        is UserMessageNode -> null
    }?.takeIf { it.isNotBlank() }
