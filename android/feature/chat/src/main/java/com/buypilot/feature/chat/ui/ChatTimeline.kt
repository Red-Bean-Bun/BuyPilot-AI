package com.buypilot.feature.chat.ui

import androidx.annotation.DrawableRes
import android.graphics.Bitmap
import android.net.Uri
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import coil.load
import coil.compose.AsyncImage
import com.buypilot.core.model.CartActionPayload
import com.buypilot.core.model.ClarificationPayload
import com.buypilot.core.model.CriteriaCardPayload
import com.buypilot.core.model.EvidencePayload
import com.buypilot.core.model.FinalDecisionPayload
import com.buypilot.core.model.ProductCardPayload
import com.buypilot.core.model.ProductPayload
import com.buypilot.core.model.QuickActionPayload
import com.buypilot.core.model.ThinkingPayload
import com.buypilot.feature.chat.R
import com.buypilot.feature.chat.model.AiStreamNode
import com.buypilot.feature.chat.model.CartActionNode
import com.buypilot.feature.chat.model.ChatUiNode
import com.buypilot.feature.chat.model.ClarificationNode
import com.buypilot.feature.chat.model.CriteriaNode
import com.buypilot.feature.chat.model.ErrorNode
import com.buypilot.feature.chat.model.FinalDecisionNode
import com.buypilot.feature.chat.model.ProductDeckNode
import com.buypilot.feature.chat.model.ProductSwipeState
import com.buypilot.feature.chat.model.ThinkingNode
import com.buypilot.feature.chat.model.UserMessageNode
import com.buypilot.feature.chat.presentation.AssistantTurnTimelineItem
import com.buypilot.feature.chat.presentation.StandaloneTimelineItem
import com.buypilot.feature.chat.presentation.TimelinePresentationState
import com.buypilot.feature.chat.presentation.TimelineRenderContext
import com.buypilot.feature.chat.presentation.UserTimelineItem
import com.buypilot.feature.chat.presentation.clarificationQuestionRevealKey
import com.buypilot.feature.chat.presentation.containsNodeKey
import com.buypilot.feature.chat.presentation.lastContentIndex
import com.buypilot.feature.chat.presentation.revealTextKey
import com.buypilot.feature.chat.presentation.timelineContentType
import com.buypilot.feature.chat.state.ChatInputState
import com.buypilot.feature.chat.state.ChatCartUiState
import com.buypilot.feature.chat.state.ChatImageAttachmentState
import com.buypilot.feature.chat.state.ChatUiState
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.Direction
import com.yuyakaido.android.cardstackview.RewindAnimationSetting
import com.yuyakaido.android.cardstackview.StackFrom
import com.yuyakaido.android.cardstackview.SwipeAnimationSetting
import com.yuyakaido.android.cardstackview.SwipeableMethod
import kotlinx.serialization.json.JsonObject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt


// Timeline rendering, scroll anchoring, and turn-level reveal choreography live here so the main screen stays a screen shell.
private const val ClarificationSelectionHoldMs = 0
private const val ClarificationExitMs = 110
internal const val ClarificationFlightMs = 360
internal const val ClarificationKeyboardBirthMs = 150
internal const val ClarificationTargetSettleMs = 24L
internal const val ClarificationTargetFallbackMs = 760L
private const val ClarificationQuestionToCardDelayMs = 170L
private const val ClarificationCardEnterMs = 640

private val TimelineNearEndThreshold = 24.dp
private val TimelineFollowCorrectionTolerance = 8.dp
private val TimelineSafeGap = 12.dp
private val TimelineBottomReadingBuffer = 72.dp
private val TimelineKeyboardBottomReadingBuffer = 56.dp
private val TimelineAnchorTopGap = 28.dp
private val TimelineAnchorBottomReserve = 460.dp
private const val TimelineViewportSettleMs = 48L
private val FloatingPanelComposerGap = 8.dp
private val TimelineJumpButtonComposerGap = 14.dp
private val ChipEdgeFadeWidth = 46.dp
private const val TurnRevealIntroChars = 18
private const val TurnRevealIntroRatio = 0.42f
private const val TurnStructuredEnterMs = 320
private const val TurnStructuredEnterDelayMs = 90
private const val ThinkingToTextHandoffMs = 0L
private const val TimelineRevealScrollThrottleMs = 40L
private const val TextRevealProgressReportStep = 24

@Immutable
internal data class TextRevealProgress(
    val visibleLength: Int = 0,
    val totalLength: Int = 0,
) {
    val hasStarted: Boolean
        get() = visibleLength > 0

    val hasReachedIntroGate: Boolean
        get() {
            if (totalLength <= 0) return false
            val ratio = visibleLength.toFloat() / totalLength.toFloat()
            return visibleLength >= TurnRevealIntroChars || ratio >= TurnRevealIntroRatio
        }
}

internal data class TurnNodeVisibilityState(
    val visibleNodeKeys: Set<String>,
    val textHandoffKeys: Set<String>,
)

@Stable
private class TurnStreamingCoordinator(
    val visibleNodeKeys: Set<String>,
    val textHandoffKeys: Set<String>,
    val visualActive: Boolean,
    val markTextCompleted: (String) -> Unit,
    val updateTextProgress: (String, Int, Int) -> Unit,
    val markStructuredEntered: (String) -> Unit,
)

@Stable
internal class TimelineRevealStore {
    private val enteredTimelineItemKeys = mutableMapOf<String, Boolean>()
    private val startedStructuredNodeKeys = mutableMapOf<String, Boolean>()
    val enteredStructuredNodeKeys = mutableStateMapOf<String, Boolean>()
    val completedTextKeys = mutableStateMapOf<String, Boolean>()
    val textRevealProgressByKey = mutableStateMapOf<String, TextRevealProgress>()
    private val liveRevealedTextKeys = mutableStateMapOf<String, Boolean>()
    private val latestTextRevealProgressByKey = mutableMapOf<String, TextRevealProgress>()
    private val lastSnapshotTextRevealProgressByKey = mutableMapOf<String, TextRevealProgress>()

    fun hasEnteredTimelineItem(key: String): Boolean = enteredTimelineItemKeys[key] == true
    fun markTimelineItemEntered(key: String) {
        enteredTimelineItemKeys[key] = true
    }

    fun hasStartedStructuredNode(key: String): Boolean = startedStructuredNodeKeys[key] == true
    fun markStructuredNodeStarted(key: String) {
        startedStructuredNodeKeys[key] = true
    }

    fun hasEnteredStructuredNode(key: String): Boolean = enteredStructuredNodeKeys[key] == true
    fun markStructuredNodeEntered(key: String) {
        enteredStructuredNodeKeys[key] = true
    }

    fun hasCompletedText(key: String): Boolean = completedTextKeys[key] == true
    fun markTextCompleted(key: String) {
        completedTextKeys[key] = true
    }

    fun hasLiveRevealedText(key: String): Boolean = liveRevealedTextKeys[key] == true

    fun updateTextProgress(key: String, visible: Int, total: Int) {
        val previous = lastSnapshotTextRevealProgressByKey[key]
        val next = TextRevealProgress(visibleLength = visible, totalLength = total)
        latestTextRevealProgressByKey[key] = next
        if (total > 0 && visible > 0 && visible < total && liveRevealedTextKeys[key] != true) {
            liveRevealedTextKeys[key] = true
        }
        val crossedIntroGate = previous?.hasReachedIntroGate != true && next.hasReachedIntroGate
        val completed = total > 0 && visible >= total
        val shouldSnapshot = previous == null ||
            !previous.hasStarted ||
            crossedIntroGate ||
            completed ||
            visible - previous.visibleLength >= TextRevealProgressReportStep
        if (shouldSnapshot && textRevealProgressByKey[key] != next) {
            lastSnapshotTextRevealProgressByKey[key] = next
            textRevealProgressByKey[key] = next
        }
    }

    fun textRevealProgress(key: String): TextRevealProgress? =
        latestTextRevealProgressByKey[key] ?: textRevealProgressByKey[key]

    fun pruneToKeys(timelineItemKeys: Set<String>, nodeKeys: Set<String>) {
        enteredTimelineItemKeys.keys.retainAll(timelineItemKeys)
        startedStructuredNodeKeys.keys.retainAll(nodeKeys)
        enteredStructuredNodeKeys.keys.retainAll(nodeKeys)
        completedTextKeys.keys.retainAll(nodeKeys)
        textRevealProgressByKey.keys.retainAll(nodeKeys)
        liveRevealedTextKeys.keys.retainAll(nodeKeys)
        latestTextRevealProgressByKey.keys.retainAll(nodeKeys)
        lastSnapshotTextRevealProgressByKey.keys.retainAll(nodeKeys)
    }
}

@Composable
internal fun ChatTimeline(
    state: ChatUiState,
    timelinePresentation: TimelinePresentationState,
    onClarificationOption: (String, ClarificationChipSnapshot?) -> Unit,
    onClarificationManualInput: () -> Unit,
    onClarificationManualSource: (String, ClarificationChipSnapshot) -> Unit,
    onCriteriaEdit: (CriteriaCardPayload) -> Unit,
    onProductOpen: (String, String?) -> Unit,
    onProductDetailOpen: (String, String) -> Unit,
    onConvergeRecommendation: (String) -> Unit,
    onDecisionEvidence: (FinalDecisionPayload) -> Unit,
    onRetryLastMessage: () -> Unit,
    onEditLastMessage: (String) -> Unit,
    onQuickAction: (QuickActionPayload) -> Unit,
    onUserImagePreview: (String) -> Unit,
    onTimelineDrag: () -> Unit,
    composerHeightPx: Int,
    stableImeBottomPx: Int,
    isComposerFocused: Boolean,
    isAssistantVisualActive: Boolean,
    isClarificationFlightActive: Boolean,
    dismissingClarificationKey: String?,
    dismissedClarificationKeys: Set<String>,
    selectedClarificationOption: String?,
    hiddenUserMessageKeys: Set<String>,
    activeFlightMessageKey: String?,
    revealedMessageKeys: Set<String>,
    onMessageRevealComplete: (String) -> Unit,
    onMessageRevealActiveChange: (String, Boolean) -> Unit,
    onAssistantTurnVisualActiveChange: (String, Boolean) -> Unit,
    onUserBubblePositioned: (String, ClarificationChipSnapshot) -> Unit,
    onClarificationCardDismissed: (String) -> Unit,
) {
    val listState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isUserDragging by listState.interactionSource.collectIsDraggedAsState()
    val timelineMotionEnabled by remember {
        derivedStateOf { !listState.isScrollInProgress }
    }
    var followStreamingText by remember { mutableStateOf(false) }
    var lastHandledUserMessageKey by remember { mutableStateOf<String?>(null) }
    var lastAutoSettledUserMessageKey by remember { mutableStateOf<String?>(null) }
    var activeTurnAnchored by remember { mutableStateOf(false) }
    var lastAnchoredAssistantTurnId by remember { mutableStateOf<String?>(null) }
    var retainedAnchorUserMessageKey by remember { mutableStateOf<String?>(null) }
    var retainedAnchorAssistantTurnId by remember { mutableStateOf<String?>(null) }
    var forceAssistantReadableTurnId by remember { mutableStateOf<String?>(null) }
    var revealScrollTick by remember { mutableIntStateOf(0) }
    var lastRevealScrollAtMs by remember { mutableStateOf(0L) }
    var suppressReturnAutoFocus by rememberSaveable { mutableStateOf(false) }
    var routeReturnAnchorCaptured by rememberSaveable { mutableStateOf(false) }
    var routeReturnRestorePending by rememberSaveable { mutableStateOf(false) }
    var routeReturnWasCovered by rememberSaveable { mutableStateOf(false) }
    var routeReturnItemKey by rememberSaveable { mutableStateOf<String?>(null) }
    var routeReturnItemIndex by rememberSaveable { mutableStateOf(0) }
    var routeReturnScrollOffset by rememberSaveable { mutableStateOf(0) }
    var routeReturnFinalDecisionKey by rememberSaveable { mutableStateOf<String?>(null) }
    var routeReturnSettledTurnId by rememberSaveable { mutableStateOf<String?>(null) }
    var routeReturnSettledFinalDecisionKey by rememberSaveable { mutableStateOf<String?>(null) }
    var clarificationFreezeCaptured by remember { mutableStateOf(false) }
    var clarificationFreezeItemKey by remember { mutableStateOf<String?>(null) }
    var clarificationFreezeItemIndex by remember { mutableStateOf(0) }
    var clarificationFreezeScrollOffset by remember { mutableStateOf(0) }
    val suppressTimelineAutoFocus = suppressReturnAutoFocus || isClarificationFlightActive
    val revealStore = remember {
        TimelineRevealStore().also { store ->
            revealedMessageKeys.forEach { key -> store.markTextCompleted(key) }
            timelinePresentation.items.forEach { item ->
                if (item is AssistantTurnTimelineItem) {
                    item.nodes.forEach { node ->
                        if (node !is AiStreamNode && node !is ThinkingNode) {
                            store.markStructuredNodeStarted(node.key)
                            store.markStructuredNodeEntered(node.key)
                        }
                    }
                }
                store.markTimelineItemEntered(item.key)
            }
        }
    }
    LaunchedEffect(revealedMessageKeys) {
        revealedMessageKeys.forEach { key ->
            if (!revealStore.hasCompletedText(key)) {
                revealStore.markTextCompleted(key)
            }
        }
    }
    val hasTimelineError = timelinePresentation.hasTimelineError
    val timelineItems = timelinePresentation.items
    val latestFinalDecisionKey = timelinePresentation.latestFinalDecisionKey
    val latestFinalDecisionTurnId = timelinePresentation.latestFinalDecisionTurnId
    val latestFinalDecisionDeckId = timelinePresentation.latestFinalDecisionDeckId
    val finalDecisionKeys = timelinePresentation.finalDecisionKeys
    var lastFocusedFinalDecisionKey by rememberSaveable { mutableStateOf(latestFinalDecisionKey) }
    fun captureRouteReturnAnchor() {
        if (timelineItems.isEmpty()) return
        val index = listState.firstVisibleItemIndex.coerceIn(0, timelineItems.lastIndex)
        routeReturnItemIndex = index
        routeReturnScrollOffset = listState.firstVisibleItemScrollOffset
        routeReturnItemKey = timelineItems.getOrNull(index)?.key
        routeReturnFinalDecisionKey = latestFinalDecisionKey
        routeReturnAnchorCaptured = true
        routeReturnRestorePending = false
    }
    fun prepareForSecondaryPageOpen() {
        captureRouteReturnAnchor()
        suppressReturnAutoFocus = true
    }
    val openProductFromTimeline: (String, String?) -> Unit = { deckId, productId ->
        prepareForSecondaryPageOpen()
        onProductOpen(deckId, productId)
    }
    val openProductDetailFromTimeline: (String, String) -> Unit = { deckId, productId ->
        prepareForSecondaryPageOpen()
        onProductDetailOpen(deckId, productId)
    }
    LaunchedEffect(timelineItems, timelinePresentation.revealKeys) {
        revealStore.pruneToKeys(
            timelineItemKeys = timelineItems.mapTo(mutableSetOf()) { it.key },
            nodeKeys = timelinePresentation.revealKeys,
        )
    }
    val renderContext = timelinePresentation.renderContext
    fun lastContentIndex(): Int = timelineItems.lastContentIndex(state, hasTimelineError)
    fun requestRevealFollowScroll() {
        if (suppressTimelineAutoFocus) return
        if (
            !followStreamingText &&
            !activeTurnAnchored &&
            retainedAnchorUserMessageKey == null &&
            retainedAnchorAssistantTurnId == null
        ) return
        val now = System.currentTimeMillis()
        if (now - lastRevealScrollAtMs >= TimelineRevealScrollThrottleMs) {
            lastRevealScrollAtMs = now
            revealScrollTick += 1
        }
    }
    fun markStructuredEnteredAndFollow(key: String) {
        revealStore.markStructuredNodeEntered(key)
    }
    val timelineViewportBottomInset = calculateTimelineViewportBottomInset(
        density = density,
        composerHeightPx = composerHeightPx,
        imeBottomPx = stableImeBottomPx,
    )
    val timelineBottomPadding = TimelineSafeGap
    val keyboardVisible = stableImeBottomPx > 0
    val timelineReadingBuffer = if (keyboardVisible) {
        TimelineKeyboardBottomReadingBuffer
    } else {
        TimelineBottomReadingBuffer
    }
    val jumpButtonBottomPadding = timelineViewportBottomInset + TimelineJumpButtonComposerGap
    val activeTurnBottomReserve = with(density) {
        maxOf(
            TimelineAnchorBottomReserve.toPx(),
            72.dp.toPx(),
        ).toDp()
    }
    val shouldRetainAnchorSpacer = retainedAnchorUserMessageKey != null &&
        retainedAnchorUserMessageKey == state.lastUserMessageKey
    val shouldRetainAssistantAnchorSpacer = retainedAnchorAssistantTurnId != null &&
        retainedAnchorAssistantTurnId == state.currentTurnId
    val targetEndReadingBuffer = timelineReadingBuffer
    val endReadingBuffer = targetEndReadingBuffer
    val nearEndThresholdPx = with(density) { TimelineNearEndThreshold.toPx() }
    val followCorrectionTolerancePx = with(density) { TimelineFollowCorrectionTolerance.toPx() }
    val latestContentBottomPaddingPx = with(density) {
        timelineBottomPadding.toPx() + timelineReadingBuffer.toPx()
    }
    val anchorTopOffsetPx = with(density) { TimelineAnchorTopGap.toPx().roundToInt() }
    suspend fun keepLatestUserMessageAnchored(settleFrames: Int = 1): Boolean {
        val key = state.lastUserMessageKey ?: return false
        if (key == activeFlightMessageKey) return false
        val index = timelineItems.indexOfFirst { it is UserTimelineItem && it.node.key == key }
        if (index < 0) return false
        scrollTimelineItemToAnchorIfNeeded(
            listState = listState,
            itemIndex = index,
            anchorTopPx = anchorTopOffsetPx,
            tolerancePx = followCorrectionTolerancePx,
            settleFrames = settleFrames,
        )
        return true
    }
    suspend fun keepAssistantTurnAnchored(turnId: String?, settleFrames: Int = 1): Boolean {
        val targetTurnId = turnId?.takeIf { it.isNotBlank() } ?: return false
        val index = timelineItems.indexOfFirst {
            it is AssistantTurnTimelineItem && it.turnId == targetTurnId
        }
        if (index < 0) return false
        scrollTimelineItemToAnchorIfNeeded(
            listState = listState,
            itemIndex = index,
            anchorTopPx = anchorTopOffsetPx,
            tolerancePx = followCorrectionTolerancePx,
            settleFrames = settleFrames,
        )
        return true
    }
    suspend fun animateAssistantTurnToReadableArea(turnId: String?, settleFrames: Int = 2): Boolean {
        val targetTurnId = turnId?.takeIf { it.isNotBlank() } ?: return false
        val index = timelineItems.indexOfFirst {
            it is AssistantTurnTimelineItem && it.turnId == targetTurnId
        }
        if (index < 0) return false
        animateTimelineItemToAnchor(
            listState = listState,
            itemIndex = index,
            anchorTopPx = anchorTopOffsetPx,
            tolerancePx = followCorrectionTolerancePx,
        )
        repeat((settleFrames - 1).coerceAtLeast(0)) {
            scrollTimelineItemToAnchorIfNeeded(
                listState = listState,
                itemIndex = index,
                anchorTopPx = anchorTopOffsetPx,
                tolerancePx = followCorrectionTolerancePx,
            )
        }
        return true
    }
    val isNearTimelineEnd by remember(
        timelineItems.size,
        state.lastError,
        hasTimelineError,
        timelineBottomPadding,
        latestContentBottomPaddingPx,
        nearEndThresholdPx,
    ) {
        derivedStateOf {
            isTimelineNearEnd(
                listState = listState,
                lastContentIndex = lastContentIndex(),
                bottomPaddingPx = latestContentBottomPaddingPx,
                thresholdPx = nearEndThresholdPx,
            )
        }
    }
    val followLatestActive = followStreamingText && (state.isStreaming || isAssistantVisualActive)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    if (routeReturnAnchorCaptured) {
                        routeReturnWasCovered = true
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (routeReturnAnchorCaptured && routeReturnWasCovered) {
                        suppressReturnAutoFocus = true
                        routeReturnRestorePending = true
                        routeReturnWasCovered = false
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (routeReturnAnchorCaptured) {
                routeReturnWasCovered = true
                routeReturnRestorePending = true
            }
            suppressReturnAutoFocus = true
        }
    }

    LaunchedEffect(suppressReturnAutoFocus, routeReturnRestorePending) {
        if (!suppressReturnAutoFocus) return@LaunchedEffect
        activeTurnAnchored = false
        followStreamingText = false
        retainedAnchorUserMessageKey = null
        retainedAnchorAssistantTurnId = null
        forceAssistantReadableTurnId = null
        if (routeReturnRestorePending) return@LaunchedEffect
        kotlinx.coroutines.delay(420L)
        suppressReturnAutoFocus = false
    }

    LaunchedEffect(routeReturnRestorePending, timelineItems.size) {
        if (!routeReturnRestorePending || timelineItems.isEmpty()) return@LaunchedEffect
        suppressReturnAutoFocus = true
        activeTurnAnchored = false
        followStreamingText = false
        retainedAnchorUserMessageKey = null
        retainedAnchorAssistantTurnId = null
        forceAssistantReadableTurnId = null
        withFrameNanos { }
        val keyedIndex = routeReturnItemKey
            ?.let { key -> timelineItems.indexOfFirst { it.key == key } }
            ?: -1
        val targetIndex = keyedIndex
            .takeIf { it >= 0 }
            ?: routeReturnItemIndex.coerceIn(0, timelineItems.lastIndex)
        listState.scrollToItem(
            index = targetIndex,
            scrollOffset = routeReturnScrollOffset.coerceAtLeast(0),
        )
        routeReturnSettledTurnId = state.currentTurnId
        routeReturnSettledFinalDecisionKey = routeReturnFinalDecisionKey
        lastAnchoredAssistantTurnId = state.currentTurnId
        lastFocusedFinalDecisionKey = latestFinalDecisionKey
        lastAutoSettledUserMessageKey = state.lastUserMessageKey
        routeReturnRestorePending = false
        routeReturnAnchorCaptured = false
        routeReturnWasCovered = false
        routeReturnItemKey = null
        routeReturnFinalDecisionKey = null
        kotlinx.coroutines.delay(420L)
        suppressReturnAutoFocus = false
    }

    LaunchedEffect(isClarificationFlightActive) {
        if (isClarificationFlightActive && timelineItems.isNotEmpty()) {
            val index = listState.firstVisibleItemIndex.coerceIn(0, timelineItems.lastIndex)
            clarificationFreezeItemIndex = index
            clarificationFreezeScrollOffset = listState.firstVisibleItemScrollOffset
            clarificationFreezeItemKey = timelineItems.getOrNull(index)?.key
            clarificationFreezeCaptured = true
        } else {
            kotlinx.coroutines.delay(80L)
            clarificationFreezeCaptured = false
            clarificationFreezeItemKey = null
        }
    }

    LaunchedEffect(
        isClarificationFlightActive,
        timelineItems.size,
        composerHeightPx,
    ) {
        if (!isClarificationFlightActive || !clarificationFreezeCaptured || timelineItems.isEmpty()) {
            return@LaunchedEffect
        }
        val keyedIndex = clarificationFreezeItemKey
            ?.let { key -> timelineItems.indexOfFirst { it.key == key } }
            ?: -1
        val targetIndex = keyedIndex
            .takeIf { it >= 0 }
            ?: clarificationFreezeItemIndex.coerceIn(0, timelineItems.lastIndex)
        listState.scrollToItem(
            index = targetIndex,
            scrollOffset = clarificationFreezeScrollOffset.coerceAtLeast(0),
        )
    }

    LaunchedEffect(state.lastUserMessageKey, activeFlightMessageKey) {
        if (suppressTimelineAutoFocus) return@LaunchedEffect
        val key = state.lastUserMessageKey ?: return@LaunchedEffect
        if (key == activeFlightMessageKey) return@LaunchedEffect
        val index = timelineItems.indexOfFirst { it is UserTimelineItem && it.node.key == key }
        if (index >= 0 && key != lastHandledUserMessageKey) {
            routeReturnSettledTurnId = null
            routeReturnSettledFinalDecisionKey = null
            lastHandledUserMessageKey = key
            lastAutoSettledUserMessageKey = null
            activeTurnAnchored = false
            retainedAnchorUserMessageKey = null
            retainedAnchorAssistantTurnId = null
            forceAssistantReadableTurnId = null
            followStreamingText = false
        }
    }

    LaunchedEffect(
        activeTurnAnchored,
        state.lastUserMessageKey,
        activeFlightMessageKey,
        composerHeightPx,
    ) {
        if (suppressTimelineAutoFocus) return@LaunchedEffect
        if (state.isStreaming || isAssistantVisualActive) return@LaunchedEffect
        if (!activeTurnAnchored || isUserDragging) return@LaunchedEffect
        val key = state.lastUserMessageKey ?: return@LaunchedEffect
        if (key == activeFlightMessageKey) return@LaunchedEffect
        val index = timelineItems.indexOfFirst { it is UserTimelineItem && it.node.key == key }
        if (index < 0) return@LaunchedEffect
        kotlinx.coroutines.delay(TimelineViewportSettleMs)
        keepLatestUserMessageAnchored(settleFrames = 2)
    }

    LaunchedEffect(
        state.currentTurnId,
        activeTurnAnchored,
        activeFlightMessageKey,
        suppressTimelineAutoFocus,
        isUserDragging,
    ) {
        if (suppressTimelineAutoFocus) return@LaunchedEffect
        if (state.isStreaming || isAssistantVisualActive) return@LaunchedEffect
        val turnId = state.currentTurnId ?: return@LaunchedEffect
        if (
            turnId == lastAnchoredAssistantTurnId ||
            turnId == routeReturnSettledTurnId ||
            activeFlightMessageKey != null ||
            isUserDragging
        ) {
            return@LaunchedEffect
        }
        val assistantTurnIndex = timelineItems.indexOfFirst {
            it is AssistantTurnTimelineItem && it.turnId == turnId
        }
        if (assistantTurnIndex < 0) return@LaunchedEffect
        val latestUserIndex = state.lastUserMessageKey?.let { key ->
            timelineItems.indexOfFirst { it is UserTimelineItem && it.node.key == key }
        } ?: -1
        val userAnchorBelongsToThisTurn = activeTurnAnchored &&
            latestUserIndex >= 0 &&
            latestUserIndex == assistantTurnIndex - 1
        if (userAnchorBelongsToThisTurn) return@LaunchedEffect

        lastAnchoredAssistantTurnId = turnId
        activeTurnAnchored = false
        followStreamingText = false
        retainedAnchorUserMessageKey = null
        retainedAnchorAssistantTurnId = turnId
        forceAssistantReadableTurnId = turnId
        kotlinx.coroutines.delay(TimelineViewportSettleMs)
        animateAssistantTurnToReadableArea(turnId)
    }

    var previousImeBottom by remember { mutableIntStateOf(0) }
    var wasNearEndBeforeKeyboard by remember { mutableStateOf(true) }
    LaunchedEffect(stableImeBottomPx) {
        val delta = stableImeBottomPx - previousImeBottom
        if (delta > 0 && isComposerFocused && !isUserDragging && wasNearEndBeforeKeyboard) {
            listState.scrollBy(delta.toFloat())
        }
        if (stableImeBottomPx == 0) {
            val lastIdx = lastContentIndex()
            val lastVisibleIdx = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            wasNearEndBeforeKeyboard = lastIdx < 0 || lastVisibleIdx >= lastIdx - 1
        }
        previousImeBottom = stableImeBottomPx
    }

    LaunchedEffect(isNearTimelineEnd, isUserDragging) {
        if (isUserDragging && !isNearTimelineEnd) {
            followStreamingText = false
            activeTurnAnchored = false
            retainedAnchorUserMessageKey = null
            retainedAnchorAssistantTurnId = null
            forceAssistantReadableTurnId = null
            lastAutoSettledUserMessageKey = state.lastUserMessageKey
        }
    }

    LaunchedEffect(isUserDragging) {
        if (isUserDragging) {
            retainedAnchorUserMessageKey = null
            retainedAnchorAssistantTurnId = null
            forceAssistantReadableTurnId = null
            onTimelineDrag()
        }
    }

    LaunchedEffect(
        state.isStreaming,
        isAssistantVisualActive,
    ) {
        if (!state.isStreaming && !isAssistantVisualActive) {
            lastAutoSettledUserMessageKey = state.lastUserMessageKey
            activeTurnAnchored = false
            followStreamingText = false
            retainedAnchorUserMessageKey = null
            retainedAnchorAssistantTurnId = null
            forceAssistantReadableTurnId = null
        }
    }

    LaunchedEffect(
        state.activeConvergenceDeckId,
        state.currentTurnId,
        suppressTimelineAutoFocus,
        isUserDragging,
    ) {
        if (suppressTimelineAutoFocus || isUserDragging) return@LaunchedEffect
        if (state.isStreaming || isAssistantVisualActive) return@LaunchedEffect
        val turnId = state.currentTurnId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        if (state.activeConvergenceDeckId.isNullOrBlank()) return@LaunchedEffect
        if (turnId == routeReturnSettledTurnId) return@LaunchedEffect
        if (forceAssistantReadableTurnId == turnId) return@LaunchedEffect
        val hasAssistantTurn = timelineItems.any {
            it is AssistantTurnTimelineItem && it.turnId == turnId
        }
        if (!hasAssistantTurn) return@LaunchedEffect
        activeTurnAnchored = false
        followStreamingText = true
        retainedAnchorUserMessageKey = null
        retainedAnchorAssistantTurnId = turnId
        forceAssistantReadableTurnId = turnId
        kotlinx.coroutines.delay(TimelineViewportSettleMs)
        animateAssistantTurnToReadableArea(turnId)
    }

    LaunchedEffect(
        latestFinalDecisionKey,
        timelineItems.size,
        composerHeightPx,
        suppressTimelineAutoFocus,
        isUserDragging,
    ) {
        if (suppressTimelineAutoFocus) return@LaunchedEffect
        val decisionKey = latestFinalDecisionKey ?: return@LaunchedEffect
        if (decisionKey == routeReturnSettledFinalDecisionKey) return@LaunchedEffect
        if (decisionKey == lastFocusedFinalDecisionKey || isUserDragging) return@LaunchedEffect
        val decisionItemIndex = timelineItems.indexOfFirst { it.containsNodeKey(decisionKey) }
        if (decisionItemIndex < 0) return@LaunchedEffect
        lastFocusedFinalDecisionKey = decisionKey
        activeTurnAnchored = false
        followStreamingText = false
        retainedAnchorUserMessageKey = null
        retainedAnchorAssistantTurnId = latestFinalDecisionTurnId
        forceAssistantReadableTurnId = latestFinalDecisionTurnId
        kotlinx.coroutines.delay(TimelineViewportSettleMs)
        if (!animateAssistantTurnToReadableArea(latestFinalDecisionTurnId, settleFrames = 4)) {
            animateTimelineItemToAnchor(
                listState = listState,
                itemIndex = decisionItemIndex,
                anchorTopPx = anchorTopOffsetPx,
                tolerancePx = followCorrectionTolerancePx,
            )
        }
    }

    LaunchedEffect(
        latestFinalDecisionKey,
        latestFinalDecisionDeckId,
        timelineItems.size,
        isAssistantVisualActive,
        forceAssistantReadableTurnId,
        composerHeightPx,
    ) {
        if (suppressTimelineAutoFocus || isUserDragging) return@LaunchedEffect
        val decisionKey = latestFinalDecisionKey ?: return@LaunchedEffect
        if (latestFinalDecisionDeckId.isNullOrBlank()) return@LaunchedEffect
        if (latestFinalDecisionTurnId.isNullOrBlank()) return@LaunchedEffect
        if (forceAssistantReadableTurnId != latestFinalDecisionTurnId) return@LaunchedEffect
        kotlinx.coroutines.delay(120L)
        if (!animateAssistantTurnToReadableArea(latestFinalDecisionTurnId, settleFrames = 2)) {
            val decisionItemIndex = timelineItems.indexOfFirst { it.containsNodeKey(decisionKey) }
            if (decisionItemIndex >= 0) {
                scrollTimelineItemToAnchorIfNeeded(
                    listState = listState,
                    itemIndex = decisionItemIndex,
                    anchorTopPx = anchorTopOffsetPx,
                    tolerancePx = followCorrectionTolerancePx,
                    settleFrames = 2,
                )
            }
        }
    }

    LaunchedEffect(
        state.isStreaming,
        latestContentBottomPaddingPx,
        revealScrollTick,
        activeTurnAnchored,
        retainedAnchorUserMessageKey,
        retainedAnchorAssistantTurnId,
        forceAssistantReadableTurnId,
    ) {
        // Disabled: no auto-scroll during or after streaming
    }

    LaunchedEffect(
        followLatestActive,
        timelineItems.size,
        state.streamingTextKey,
        latestContentBottomPaddingPx,
        suppressTimelineAutoFocus,
        isUserDragging,
    ) {
        // Disabled: don't auto-scroll to follow streaming text
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = timelineViewportBottomInset),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = timelineBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(
                items = timelineItems,
                key = { it.key },
                contentType = { it.timelineContentType },
            ) { item ->
                when (item) {
                    is UserTimelineItem -> {
                        val hiddenUserMessage = item.node.key in hiddenUserMessageKeys
                        TimelineItemMotion(
                            animateEnter = timelineMotionEnabled && !hiddenUserMessage,
                            hasEntered = revealStore.hasEnteredTimelineItem(item.key),
                            onEntered = { revealStore.markTimelineItemEntered(item.key) },
                        ) {
                            TimelineNodeContent(
                                node = item.node,
                                renderContext = renderContext,
                                timelineMotionEnabled = timelineMotionEnabled,
                                hiddenUserMessage = hiddenUserMessage,
                                activeFlightMessageKey = activeFlightMessageKey,
                                revealedMessageKeys = revealedMessageKeys,
                                textRevealProgress = null,
                                textCompleted = false,
                                dismissingClarificationKey = dismissingClarificationKey,
                                dismissedClarificationKeys = dismissedClarificationKeys,
                                selectedClarificationOption = selectedClarificationOption,
                                onClarificationOption = onClarificationOption,
                                onClarificationManualInput = onClarificationManualInput,
                                onClarificationManualSource = onClarificationManualSource,
                                onCriteriaEdit = onCriteriaEdit,
                                onProductOpen = openProductFromTimeline,
                                onProductDetailOpen = openProductDetailFromTimeline,
                                onConvergeRecommendation = onConvergeRecommendation,
                                onDecisionEvidence = onDecisionEvidence,
                                onRetryLastMessage = onRetryLastMessage,
                                onEditLastMessage = onEditLastMessage,
                                onQuickAction = onQuickAction,
                                onUserImagePreview = onUserImagePreview,
                                onMessageRevealComplete = onMessageRevealComplete,
                                onMessageRevealActiveChange = onMessageRevealActiveChange,
                                onStreamingTextProgress = {},
                                onTextRevealProgress = { _, _, _ -> },
                                onStructuredEntered = ::markStructuredEnteredAndFollow,
                                onUserBubblePositioned = onUserBubblePositioned,
                                onClarificationCardDismissed = onClarificationCardDismissed,
                            )
                        }
                    }
                    is AssistantTurnTimelineItem -> TimelineItemMotion(
                        animateEnter = false,
                        hasEntered = revealStore.hasEnteredTimelineItem(item.key),
                        onEntered = { revealStore.markTimelineItemEntered(item.key) },
                    ) {
                        AssistantTurnBlock(
                            item = item,
                            renderContext = renderContext,
                            timelineMotionEnabled = timelineMotionEnabled,
                            revealStore = revealStore,
                            activeFlightMessageKey = activeFlightMessageKey,
                            revealedMessageKeys = revealedMessageKeys,
                            dismissingClarificationKey = dismissingClarificationKey,
                            dismissedClarificationKeys = dismissedClarificationKeys,
                            selectedClarificationOption = selectedClarificationOption,
                            onClarificationOption = onClarificationOption,
                            onClarificationManualInput = onClarificationManualInput,
                            onClarificationManualSource = onClarificationManualSource,
                            onCriteriaEdit = onCriteriaEdit,
                            onProductOpen = openProductFromTimeline,
                            onProductDetailOpen = openProductDetailFromTimeline,
                            onConvergeRecommendation = onConvergeRecommendation,
                            onDecisionEvidence = onDecisionEvidence,
                            onRetryLastMessage = onRetryLastMessage,
                            onEditLastMessage = onEditLastMessage,
                            onQuickAction = onQuickAction,
                            onUserImagePreview = onUserImagePreview,
                            onMessageRevealComplete = onMessageRevealComplete,
                            onMessageRevealActiveChange = onMessageRevealActiveChange,
                            onAssistantTurnVisualActiveChange = onAssistantTurnVisualActiveChange,
                            onStreamingTextProgress = { requestRevealFollowScroll() },
                            onTextCompleted = revealStore::markTextCompleted,
                            onTextRevealProgress = revealStore::updateTextProgress,
                            onStructuredStarted = revealStore::markStructuredNodeStarted,
                            onStructuredEntered = ::markStructuredEnteredAndFollow,
                            onUserBubblePositioned = onUserBubblePositioned,
                            onClarificationCardDismissed = onClarificationCardDismissed,
                        )
                    }
                    is StandaloneTimelineItem -> TimelineItemMotion(
                        animateEnter = timelineMotionEnabled,
                        hasEntered = revealStore.hasEnteredTimelineItem(item.key),
                        onEntered = { revealStore.markTimelineItemEntered(item.key) },
                    ) {
                        TimelineNodeContent(
                            node = item.node,
                            renderContext = renderContext,
                            timelineMotionEnabled = timelineMotionEnabled,
                            structuredMotionEnabled = timelineMotionEnabled &&
                                !revealStore.hasStartedStructuredNode(item.node.key),
                            structuredAlreadyEntered = revealStore.hasEnteredStructuredNode(item.node.key),
                            hiddenUserMessage = false,
                            activeFlightMessageKey = activeFlightMessageKey,
                            revealedMessageKeys = revealedMessageKeys,
                            textRevealProgress = item.node.revealTextKey()?.let { revealStore.textRevealProgress(it) },
                            textCompleted = item.node.revealTextKey()?.let {
                                revealStore.hasCompletedText(it) || it in revealedMessageKeys
                            } == true,
                            textLiveRevealed = item.node.revealTextKey()?.let {
                                revealStore.hasLiveRevealedText(it) || it in revealedMessageKeys
                            } == true,
                            dismissingClarificationKey = dismissingClarificationKey,
                            dismissedClarificationKeys = dismissedClarificationKeys,
                            selectedClarificationOption = selectedClarificationOption,
                            onClarificationOption = onClarificationOption,
                            onClarificationManualInput = onClarificationManualInput,
                            onClarificationManualSource = onClarificationManualSource,
                            onCriteriaEdit = onCriteriaEdit,
                            onProductOpen = openProductFromTimeline,
                            onProductDetailOpen = openProductDetailFromTimeline,
                            onConvergeRecommendation = onConvergeRecommendation,
                            onDecisionEvidence = onDecisionEvidence,
                            onRetryLastMessage = onRetryLastMessage,
                            onEditLastMessage = onEditLastMessage,
                            onQuickAction = onQuickAction,
                            onUserImagePreview = onUserImagePreview,
                            onMessageRevealComplete = { key ->
                                revealStore.markTextCompleted(key)
                                onMessageRevealComplete(key)
                            },
                            onMessageRevealActiveChange = onMessageRevealActiveChange,
                            onStreamingTextProgress = { requestRevealFollowScroll() },
                            onTextRevealProgress = { key, visible, total ->
                                revealStore.updateTextProgress(key, visible, total)
                            },
                            onStructuredEntered = ::markStructuredEnteredAndFollow,
                            onUserBubblePositioned = onUserBubblePositioned,
                            onClarificationCardDismissed = onClarificationCardDismissed,
                        )
                    }
                }
            }

            if (state.lastError != null && !hasTimelineError) {
                item("last_error") {
                    Box {
                        InlineSystemNotice(
                            state.lastError.userFacingErrorReason("这轮回复中断了，可以重试或编辑最近问题。"),
                        )
                    }
                }
            }

            if (timelineItems.isNotEmpty()) {
                item("timeline_bottom_reading_buffer") {
                    Spacer(Modifier.height(endReadingBuffer))
                }
            }
        }

        AnimatedVisibility(
            visible = !isComposerFocused && stableImeBottomPx <= 0 && !isNearTimelineEnd && !followLatestActive,
            enter = fadeIn(animationSpec = tween(durationMillis = 160, easing = MenuEaseOut)) +
                slideInVertically(
                    animationSpec = tween(durationMillis = 190, easing = MenuEaseOut),
                    initialOffsetY = { it / 3 },
                ),
            exit = fadeOut(animationSpec = tween(durationMillis = 120, easing = MenuEaseIn)) +
                slideOutVertically(
                    animationSpec = tween(durationMillis = 140, easing = MenuEaseIn),
                    targetOffsetY = { it / 3 },
                ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = jumpButtonBottomPadding)
                .zIndex(2f),
        ) {
            FollowLatestBubble(
                onClick = {
                    followStreamingText = state.isStreaming || isAssistantVisualActive
                    activeTurnAnchored = false
                    retainedAnchorUserMessageKey = null
                    retainedAnchorAssistantTurnId = null
                    lastRevealScrollAtMs = 0L
                    revealScrollTick += 1
                    coroutineScope.launch {
                        val lastContentIndex = lastContentIndex().coerceAtLeast(0)
                        animateLatestContentIntoView(
                            listState = listState,
                            lastContentIndex = lastContentIndex,
                            bottomPaddingPx = latestContentBottomPaddingPx,
                            tolerancePx = followCorrectionTolerancePx,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun AssistantTurnBlock(
    item: AssistantTurnTimelineItem,
    renderContext: TimelineRenderContext,
    timelineMotionEnabled: Boolean,
    revealStore: TimelineRevealStore,
    activeFlightMessageKey: String?,
    revealedMessageKeys: Set<String>,
    dismissingClarificationKey: String?,
    dismissedClarificationKeys: Set<String>,
    selectedClarificationOption: String?,
    onClarificationOption: (String, ClarificationChipSnapshot?) -> Unit,
    onClarificationManualInput: () -> Unit,
    onClarificationManualSource: (String, ClarificationChipSnapshot) -> Unit,
    onCriteriaEdit: (CriteriaCardPayload) -> Unit,
    onProductOpen: (String, String?) -> Unit,
    onProductDetailOpen: (String, String) -> Unit,
    onConvergeRecommendation: (String) -> Unit,
    onDecisionEvidence: (FinalDecisionPayload) -> Unit,
    onRetryLastMessage: () -> Unit,
    onEditLastMessage: (String) -> Unit,
    onQuickAction: (QuickActionPayload) -> Unit,
    onUserImagePreview: (String) -> Unit,
    onMessageRevealComplete: (String) -> Unit,
    onMessageRevealActiveChange: (String, Boolean) -> Unit,
    onAssistantTurnVisualActiveChange: (String, Boolean) -> Unit,
    onStreamingTextProgress: () -> Unit,
    onTextCompleted: (String) -> Unit,
    onTextRevealProgress: (String, Int, Int) -> Unit,
    onStructuredStarted: (String) -> Unit,
    onStructuredEntered: (String) -> Unit,
    onUserBubblePositioned: (String, ClarificationChipSnapshot) -> Unit,
    onClarificationCardDismissed: (String) -> Unit,
) {
    val turnSettled = renderContext.currentTurnId == null || item.turnId != renderContext.currentTurnId
    val coordinator = rememberTurnStreamingCoordinator(
        item = item,
        revealStore = revealStore,
        turnSettled = turnSettled,
        revealedMessageKeys = revealedMessageKeys,
        onTextCompleted = onTextCompleted,
        onTextRevealProgress = onTextRevealProgress,
        onStructuredEntered = onStructuredEntered,
    )

    LaunchedEffect(item.key, coordinator.visualActive) {
        onAssistantTurnVisualActiveChange(item.key, coordinator.visualActive)
    }
    DisposableEffect(item.key) {
        onDispose { onAssistantTurnVisualActiveChange(item.key, false) }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        item.nodes.forEachIndexed { index, node ->
            val nodeVisible = node.key in coordinator.visibleNodeKeys
            if (!nodeVisible && node !is ThinkingNode) return@forEachIndexed
            if (node is ThinkingNode) {
                val nextNode = item.nodes.getOrNull(index + 1)
                if (nextNode != null && nextNode.isTextOrThinkingNode()) {
                    return@forEachIndexed
                }
                key(node.key) {
                    TurnNodeMotion(
                        node = node,
                        orderIndex = index,
                        motionEnabled = timelineMotionEnabled,
                        hasStarted = turnSettled || revealStore.hasStartedStructuredNode(node.key),
                        hasEntered = turnSettled || revealStore.hasEnteredStructuredNode(node.key),
                        onStarted = {},
                        onEntered = {},
                    ) {
                        ThinkingNodeVisibility(visible = nodeVisible) {
                            AssistantInlineStatus(
                                message = (node as ThinkingNode).payload.userFacingThinkingMessage(),
                                motionEnabled = renderContext.currentTurnId == node.turnId,
                            )
                        }
                    }
                }
                return@forEachIndexed
            }
            val precedingThinking = item.nodes.getOrNull(index - 1)
                ?.takeIf { it is ThinkingNode } as? ThinkingNode
            val thinkingVisible = precedingThinking != null &&
                precedingThinking.key in coordinator.visibleNodeKeys
            key(node.key) {
                TurnNodeMotion(
                    node = node,
                    orderIndex = index,
                    motionEnabled = timelineMotionEnabled,
                    hasStarted = turnSettled || revealStore.hasStartedStructuredNode(node.key),
                    hasEntered = turnSettled || revealStore.hasEnteredStructuredNode(node.key),
                    onStarted = {
                        if (!node.isTextOrThinkingNode()) {
                            onStructuredStarted(node.key)
                        }
                    },
                    onEntered = {
                        if (!node.isTextOrThinkingNode()) {
                            coordinator.markStructuredEntered(node.key)
                        }
                    },
                ) {
                    if (node is ThinkingNode) {
                        ThinkingNodeVisibility(visible = nodeVisible) {
                            AssistantInlineStatus(
                                message = node.payload.userFacingThinkingMessage(),
                                motionEnabled = renderContext.currentTurnId == node.turnId,
                            )
                        }
                    } else if (nodeVisible) {
                        if (thinkingVisible && precedingThinking != null) {
                            AssistantInlineStatus(
                                message = precedingThinking.payload.userFacingThinkingMessage(),
                                motionEnabled = renderContext.currentTurnId == precedingThinking.turnId,
                            )
                        } else {
                            val showThinkingFade = !turnSettled &&
                                precedingThinking != null && !thinkingVisible
                            val thinkingAlpha = remember { Animatable(if (showThinkingFade) 1f else 0f) }
                            var thinkingFadeDone by remember { mutableStateOf(!showThinkingFade) }
                            if (showThinkingFade) {
                                LaunchedEffect(Unit) {
                                    thinkingAlpha.animateTo(
                                        0f,
                                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                                    )
                                    thinkingFadeDone = true
                                }
                            }
                            if (!thinkingFadeDone) {
                                Box(Modifier.graphicsLayer { alpha = thinkingAlpha.value }) {
                                    AssistantInlineStatus(
                                        message = precedingThinking!!.payload.userFacingThinkingMessage(),
                                        motionEnabled = false,
                                    )
                                }
                            } else {
                                val productConvergeActionReady = turnSettled || (
                                    (!renderContext.isStreaming || renderContext.currentTurnId != item.turnId) &&
                                        item.nodes.allTurnTextRevealed(revealStore, revealedMessageKeys)
                                    )
                                TimelineNodeContent(
                                    node = node,
                                    renderContext = renderContext,
                                    timelineMotionEnabled = timelineMotionEnabled,
                                    productConvergeActionReady = productConvergeActionReady,
                                    structuredMotionEnabled = !turnSettled &&
                                        timelineMotionEnabled &&
                                        !revealStore.hasStartedStructuredNode(node.key),
                                    structuredAlreadyEntered = turnSettled ||
                                        revealStore.hasEnteredStructuredNode(node.key),
                                    hiddenUserMessage = false,
                                    activeFlightMessageKey = activeFlightMessageKey,
                                    revealedMessageKeys = revealedMessageKeys,
                                    textRevealProgress = node.revealTextKey()?.let { revealStore.textRevealProgress(it) },
                                    textCompleted = node.revealTextKey()?.let {
                                        turnSettled || revealStore.hasCompletedText(it) || it in revealedMessageKeys
                                    } == true,
                                    textLiveRevealed = node.revealTextKey()?.let {
                                        revealStore.hasLiveRevealedText(it) || it in revealedMessageKeys
                                    } == true,
                                    delayTextReveal = !turnSettled &&
                                        node.revealTextKey()?.let { it in coordinator.textHandoffKeys } == true,
                                    dismissingClarificationKey = dismissingClarificationKey,
                                    dismissedClarificationKeys = dismissedClarificationKeys,
                                    selectedClarificationOption = selectedClarificationOption,
                                    onClarificationOption = onClarificationOption,
                                    onClarificationManualInput = onClarificationManualInput,
                                    onClarificationManualSource = onClarificationManualSource,
                                    onCriteriaEdit = onCriteriaEdit,
                                    onProductOpen = onProductOpen,
                                    onProductDetailOpen = onProductDetailOpen,
                                    onConvergeRecommendation = onConvergeRecommendation,
                                    onDecisionEvidence = onDecisionEvidence,
                                    onRetryLastMessage = onRetryLastMessage,
                                    onEditLastMessage = onEditLastMessage,
                                    onQuickAction = onQuickAction,
                                    onUserImagePreview = onUserImagePreview,
                                    onMessageRevealComplete = { key ->
                                        coordinator.markTextCompleted(key)
                                        onMessageRevealComplete(key)
                                    },
                                    onMessageRevealActiveChange = onMessageRevealActiveChange,
                                    onStreamingTextProgress = onStreamingTextProgress,
                                    onTextRevealProgress = coordinator.updateTextProgress,
                                    onStructuredEntered = coordinator.markStructuredEntered,
                                    onUserBubblePositioned = onUserBubblePositioned,
                                    onClarificationCardDismissed = onClarificationCardDismissed,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThinkingNodeVisibility(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    var delayedVisible by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible && !delayedVisible) {
            kotlinx.coroutines.delay(400L)
            delayedVisible = true
        } else if (!visible) {
            delayedVisible = false
        }
    }
    AnimatedVisibility(
        visible = visible && delayedVisible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 250, easing = MenuEaseOut),
        ),
        exit = fadeOut(
            animationSpec = tween(durationMillis = 220, easing = MenuEaseIn),
        ) + shrinkVertically(
            animationSpec = tween(durationMillis = 160, delayMillis = 220, easing = MenuEaseIn),
            shrinkTowards = Alignment.Top,
        ),
    ) {
        content()
    }
}

@Composable
private fun rememberTurnStreamingCoordinator(
    item: AssistantTurnTimelineItem,
    revealStore: TimelineRevealStore,
    turnSettled: Boolean,
    revealedMessageKeys: Set<String>,
    onTextCompleted: (String) -> Unit,
    onTextRevealProgress: (String, Int, Int) -> Unit,
    onStructuredEntered: (String) -> Unit,
): TurnStreamingCoordinator {
    val completedTextKeys = if (turnSettled) {
        item.nodes.settledTextRevealKeys()
    } else {
        item.nodes
            .mapNotNull { node ->
                node.revealTextKey()?.takeIf {
                    revealStore.hasCompletedText(it) || it in revealedMessageKeys
                }
            }
            .toSet()
    }
    val textRevealProgressByKey = item.nodes
        .mapNotNull { node ->
            node.revealTextKey()?.let { key ->
                revealStore.textRevealProgressByKey[key]?.let { key to it }
            }
        }
        .toMap()
    val enteredStructuredNodeKeys = if (turnSettled) {
        item.nodes.settledStructuredNodeKeys()
    } else {
        item.nodes
            .filterNot { it is AiStreamNode || it is ThinkingNode }
            .mapNotNull { node -> node.key.takeIf { revealStore.hasEnteredStructuredNode(it) } }
            .toSet()
    }
    val visibilityState = remember(item.nodes, completedTextKeys, textRevealProgressByKey, enteredStructuredNodeKeys) {
        item.nodes.visibleTurnNodeKeys(
            completedTextKeys = completedTextKeys,
            textRevealProgress = textRevealProgressByKey,
            enteredStructuredKeys = enteredStructuredNodeKeys,
        )
    }
    val visualActive = remember(item.nodes, completedTextKeys, enteredStructuredNodeKeys, turnSettled) {
        !turnSettled &&
            item.nodes.hasUnsettledTurnVisual(
                completedTextKeys = completedTextKeys,
                enteredStructuredKeys = enteredStructuredNodeKeys,
            )
    }

    return TurnStreamingCoordinator(
        visibleNodeKeys = visibilityState.visibleNodeKeys,
        textHandoffKeys = visibilityState.textHandoffKeys,
        visualActive = visualActive,
        markTextCompleted = { key ->
            onTextCompleted(key)
        },
        updateTextProgress = { key, visible, total ->
            onTextRevealProgress(key, visible, total)
        },
        markStructuredEntered = { key ->
            onStructuredEntered(key)
        },
    )
}

internal fun List<ChatUiNode>.visibleTurnNodeKeys(
    completedTextKeys: Set<String>,
    textRevealProgress: Map<String, TextRevealProgress>,
    enteredStructuredKeys: Set<String>,
): TurnNodeVisibilityState {
    val visible = mutableSetOf<String>()
    val handoffTexts = mutableSetOf<String>()

    forEachIndexed { index, node ->
        when (node) {
            is ThinkingNode -> {
                if (canRevealThinkingNode(index, completedTextKeys, textRevealProgress, enteredStructuredKeys)) {
                    visible += node.key
                }
            }
            else -> {
                if (canRevealTurnNode(index, node, completedTextKeys, textRevealProgress, enteredStructuredKeys)) {
                    visible += node.key
                    val revealTextKey = node.revealTextKey()
                    if (revealTextKey != null &&
                        textRevealProgress[revealTextKey]?.hasStarted != true &&
                        shouldDelayTextForThinkingExit(index)
                    ) {
                        handoffTexts += revealTextKey
                    }
                }
            }
        }
    }
    return TurnNodeVisibilityState(
        visibleNodeKeys = visible,
        textHandoffKeys = handoffTexts,
    )
}

internal fun List<ChatUiNode>.canRevealThinkingNode(
    index: Int,
    completedTextKeys: Set<String>,
    textRevealProgress: Map<String, TextRevealProgress>,
    enteredStructuredKeys: Set<String>,
): Boolean {
    for (previousIndex in 0 until index) {
        val previous = this[previousIndex]
        when (previous) {
            is ThinkingNode -> Unit
            is AiStreamNode -> {
                if (previous.key !in completedTextKeys) return false
            }
            is ClarificationNode -> {
                if (previous.revealTextKey() !in completedTextKeys || previous.key !in enteredStructuredKeys) {
                    return false
                }
            }
            else -> {
                if (previous.key !in enteredStructuredKeys) return false
            }
        }
    }
    val nextNode = (index + 1 until size)
        .map { this[it] }
        .firstOrNull { it !is ThinkingNode }
    val visualOnlyThinking = (this[index] as ThinkingNode)
        .payload
        .userFacingThinkingMessage()
        .isBlank()
    return when (nextNode) {
        null -> !visualOnlyThinking
        is AiStreamNode -> {
            val nextTextKey = nextNode.revealTextKey()
            nextTextKey != null &&
                nextTextKey !in completedTextKeys &&
                textRevealProgress[nextTextKey]?.hasStarted != true
        }
        is ClarificationNode -> {
            val questionKey = nextNode.revealTextKey()
            questionKey != null &&
                questionKey !in completedTextKeys &&
                textRevealProgress[questionKey]?.hasStarted != true
        }
        else -> false
    }
}

internal fun List<ChatUiNode>.canRevealTurnNode(
    index: Int,
    node: ChatUiNode,
    completedTextKeys: Set<String>,
    textRevealProgress: Map<String, TextRevealProgress>,
    enteredStructuredKeys: Set<String>,
): Boolean {
    for (previousIndex in 0 until index) {
        val previous = this[previousIndex]
        when (previous) {
            is ThinkingNode -> Unit
            is AiStreamNode -> {
                val previousRevealKey = previous.revealTextKey()
                if (previousRevealKey !in completedTextKeys) {
                    val allowDeckAfterIntro = node is ProductDeckNode &&
                        previousIndex == latestTextIndexBefore(index) &&
                        textRevealProgress[previousRevealKey]?.hasReachedIntroGate == true
                    if (!allowDeckAfterIntro) return false
                }
            }
            is ClarificationNode -> {
                if (previous.revealTextKey() !in completedTextKeys || previous.key !in enteredStructuredKeys) {
                    return false
                }
            }
            else -> {
                if (previous.key !in enteredStructuredKeys) return false
            }
        }
    }
    return true
}

internal fun List<ChatUiNode>.shouldDelayTextForThinkingExit(index: Int): Boolean =
    index > 0 && this[index].revealTextKey() != null && this[index - 1] is ThinkingNode

internal fun List<ChatUiNode>.settledTextRevealKeys(): Set<String> =
    mapNotNullTo(mutableSetOf()) { node -> node.revealTextKey() }

internal fun List<ChatUiNode>.settledStructuredNodeKeys(): Set<String> =
    filterNot { it is AiStreamNode || it is ThinkingNode }
        .mapTo(mutableSetOf()) { node -> node.key }

private fun List<ChatUiNode>.latestTextIndexBefore(index: Int): Int =
    (index - 1 downTo 0).firstOrNull { this[it] is AiStreamNode } ?: -1

internal fun List<ChatUiNode>.hasUnsettledTurnVisual(
    completedTextKeys: Set<String>,
    enteredStructuredKeys: Set<String>,
): Boolean =
    any { node ->
        when (node) {
            is AiStreamNode -> node.revealTextKey() !in completedTextKeys
            is ClarificationNode -> node.revealTextKey() !in completedTextKeys || node.key !in enteredStructuredKeys
            is ThinkingNode -> false
            else -> node.key !in enteredStructuredKeys
        }
    }

private fun List<ChatUiNode>.allTurnTextRevealed(
    revealStore: TimelineRevealStore,
    revealedMessageKeys: Set<String>,
): Boolean =
    all { node ->
        val revealKey = node.revealTextKey()
        revealKey == null || revealStore.hasCompletedText(revealKey) || revealKey in revealedMessageKeys
    }

private fun ChatUiNode.isTextOrThinkingNode(): Boolean =
    this is AiStreamNode || this is ThinkingNode

internal fun shouldCompactProductDeckHistory(
    node: ProductDeckNode,
    deckConverged: Boolean,
    isStreaming: Boolean,
    currentTurnId: String?,
    latestProductDeckKey: String?,
): Boolean =
    if (deckConverged) {
        true
    } else if (isStreaming && currentTurnId != null) {
        node.turnId != currentTurnId
    } else {
        latestProductDeckKey != null && latestProductDeckKey != node.key
    }

@Composable
private fun TurnNodeMotion(
    node: ChatUiNode,
    orderIndex: Int,
    motionEnabled: Boolean,
    hasStarted: Boolean,
    hasEntered: Boolean,
    onStarted: () -> Unit,
    onEntered: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (
        node is AiStreamNode ||
        node is ThinkingNode ||
        node is ClarificationNode ||
        node is CriteriaNode ||
        node is ProductDeckNode ||
        node is FinalDecisionNode
    ) {
        content()
        return
    }
    val shouldAnimate = remember(node.key) { motionEnabled && !hasStarted }
    val latestOnStarted by rememberUpdatedState(onStarted)
    val latestOnEntered by rememberUpdatedState(onEntered)

    if (!shouldAnimate) {
        LaunchedEffect(node.key) {
            if (!hasEntered) latestOnEntered()
        }
        content()
        return
    }

    var visible by remember(node.key) { mutableStateOf(false) }
    val enterDelay = TurnStructuredEnterDelayMs + (orderIndex.coerceAtMost(4) * 28)

    LaunchedEffect(node.key) {
        latestOnStarted()
        visible = false
        kotlinx.coroutines.delay(enterDelay.toLong())
        visible = true
        kotlinx.coroutines.delay(TurnStructuredEnterMs.toLong())
        latestOnEntered()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = TurnStructuredEnterMs,
                easing = MenuEaseOut,
            ),
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = TurnStructuredEnterMs,
                easing = MenuEaseOut,
            ),
            initialOffsetY = { it / 10 },
        ) + scaleIn(
            animationSpec = tween(
                durationMillis = TurnStructuredEnterMs,
                easing = MenuEaseOut,
            ),
            initialScale = 0.985f,
        ),
    ) {
        content()
    }
}

@Composable
private fun TimelineNodeContent(
    node: ChatUiNode,
    renderContext: TimelineRenderContext,
    timelineMotionEnabled: Boolean,
    productConvergeActionReady: Boolean = true,
    structuredMotionEnabled: Boolean = timelineMotionEnabled,
    structuredAlreadyEntered: Boolean = false,
    hiddenUserMessage: Boolean,
    activeFlightMessageKey: String?,
    revealedMessageKeys: Set<String>,
    textRevealProgress: TextRevealProgress?,
    textCompleted: Boolean,
    textLiveRevealed: Boolean = false,
    delayTextReveal: Boolean = false,
    dismissingClarificationKey: String?,
    dismissedClarificationKeys: Set<String>,
    selectedClarificationOption: String?,
    onClarificationOption: (String, ClarificationChipSnapshot?) -> Unit,
    onClarificationManualInput: () -> Unit,
    onClarificationManualSource: (String, ClarificationChipSnapshot) -> Unit,
    onCriteriaEdit: (CriteriaCardPayload) -> Unit,
    onProductOpen: (String, String?) -> Unit,
    onProductDetailOpen: (String, String) -> Unit,
    onConvergeRecommendation: (String) -> Unit,
    onDecisionEvidence: (FinalDecisionPayload) -> Unit,
    onRetryLastMessage: () -> Unit,
    onEditLastMessage: (String) -> Unit,
    onQuickAction: (QuickActionPayload) -> Unit,
    onUserImagePreview: (String) -> Unit,
    onMessageRevealComplete: (String) -> Unit,
    onMessageRevealActiveChange: (String, Boolean) -> Unit,
    onStreamingTextProgress: () -> Unit,
    onTextRevealProgress: (String, Int, Int) -> Unit,
    onStructuredEntered: (String) -> Unit,
    onUserBubblePositioned: (String, ClarificationChipSnapshot) -> Unit,
    onClarificationCardDismissed: (String) -> Unit,
) {
    when (node) {
        is UserMessageNode -> UserBubble(
            node = node,
            backendBaseUrl = renderContext.backendBaseUrl,
            hidden = hiddenUserMessage,
            onImagePreview = onUserImagePreview,
            onPositioned = if (node.key == activeFlightMessageKey) {
                { onUserBubblePositioned(node.key, it) }
            } else {
                null
            },
        )
        is ThinkingNode -> {
            val statusMessage = node.payload.userFacingThinkingMessage()
            AssistantInlineStatus(
                message = statusMessage,
                motionEnabled = renderContext.currentTurnId == node.turnId,
            )
        }
        is AiStreamNode -> StreamingAssistantText(
            nodeKey = node.key,
            content = node.content,
            done = node.done,
            revealState = textRevealProgress,
            alreadyCompleted = textCompleted,
            stablePlainAfterLiveReveal = textLiveRevealed,
            animateInitialCompleted = node.turnId == renderContext.currentTurnId &&
                node.key !in revealedMessageKeys,
            initialRevealDelayMs = if (delayTextReveal) ThinkingToTextHandoffMs else 0L,
            onRevealComplete = { onMessageRevealComplete(node.key) },
            onRevealActiveChange = { active -> onMessageRevealActiveChange(node.key, active) },
            onRevealProgress = { visible, total ->
                onTextRevealProgress(node.key, visible, total)
                onStreamingTextProgress()
            },
        )
        is ClarificationNode -> ClarificationBlock(
            nodeKey = node.key,
            payload = node.payload,
            questionRevealKey = node.clarificationQuestionRevealKey(),
            questionRevealProgress = textRevealProgress,
            questionCompleted = textCompleted,
            animateQuestion = node.turnId == renderContext.currentTurnId &&
                node.clarificationQuestionRevealKey() !in revealedMessageKeys,
            questionRevealDelayMs = if (delayTextReveal) ThinkingToTextHandoffMs else 0L,
            anchorRevealed = node.anchorMessageKey.isBlank() || node.anchorMessageKey in revealedMessageKeys,
            dismissed = node.key in dismissedClarificationKeys,
            dismissing = node.key == dismissingClarificationKey,
            selectedOption = selectedClarificationOption.takeIf { node.key == dismissingClarificationKey },
            onOption = onClarificationOption,
            onManualInput = onClarificationManualInput,
            onManualSource = { snapshot -> onClarificationManualSource(node.key, snapshot) },
            onQuestionRevealComplete = { onMessageRevealComplete(node.clarificationQuestionRevealKey()) },
            onQuestionRevealActiveChange = { active ->
                onMessageRevealActiveChange(node.clarificationQuestionRevealKey(), active)
            },
            onQuestionRevealProgress = { visible, total ->
                val revealKey = node.clarificationQuestionRevealKey()
                onTextRevealProgress(revealKey, visible, total)
                onStreamingTextProgress()
            },
            onCardEntered = { onStructuredEntered(node.key) },
            onCardDismissed = onClarificationCardDismissed,
        )
        is CriteriaNode -> CriteriaSummaryCard(
            motionKey = node.key,
            payload = node.payload,
            motionEnabled = structuredMotionEnabled,
            alreadyEntered = structuredAlreadyEntered,
            onEntered = { onStructuredEntered(node.key) },
            onEdit = { onCriteriaEdit(node.payload) },
        )
        is ProductDeckNode -> ProductRecommendationStrip(
            node = node,
            backendBaseUrl = renderContext.backendBaseUrl,
            swipeState = renderContext.productSwipeStates[node.deckId],
            awaitingConvergence = node.deckId == renderContext.latestConvergeableDeckId &&
                node.deckId in renderContext.awaitingConvergenceDeckIds,
            hasPendingDecision = node.deckId in renderContext.pendingDecisions,
            deckConverged = node.deckId in renderContext.convergedDeckIds,
            deckStillStreaming = renderContext.isStreaming && renderContext.currentTurnId == node.turnId,
            convergeActionReady = productConvergeActionReady,
            compactHistory = shouldCompactProductDeckHistory(
                node = node,
                deckConverged = node.deckId in renderContext.convergedDeckIds,
                isStreaming = renderContext.isStreaming,
                currentTurnId = renderContext.currentTurnId,
                latestProductDeckKey = renderContext.latestProductDeckKey,
            ),
            motionEnabled = structuredMotionEnabled,
            alreadyEntered = structuredAlreadyEntered,
            onEntered = { onStructuredEntered(node.key) },
            onOpen = onProductOpen,
            onOpenDetail = onProductDetailOpen,
            onConverge = { onConvergeRecommendation(node.deckId) },
        )
        is FinalDecisionNode -> DecisionSummaryCard(
            motionKey = node.key,
            payload = node.payload,
            productsById = renderContext.productsById,
            productDeckIdByProductId = renderContext.productDeckIdByProductId,
            cartState = renderContext.cartState,
            backendBaseUrl = renderContext.backendBaseUrl,
            motionEnabled = structuredMotionEnabled,
            alreadyEntered = structuredAlreadyEntered,
            onEntered = { onStructuredEntered(node.key) },
            onEvidence = { onDecisionEvidence(node.payload) },
            onProductDetailOpen = onProductDetailOpen,
            onQuickAction = onQuickAction,
        )
        is CartActionNode -> CartActionCard(
            payload = node.payload,
            productsById = renderContext.productsById,
            onRetryAddToCart = { productId ->
                onQuickAction(
                    QuickActionPayload(
                        actionId = "retry_add_to_cart_$productId",
                        label = "重试",
                        action = "add_to_cart",
                        productId = productId,
                    ),
                )
            },
        )
        is ErrorNode -> ErrorCard(
            node = node,
            latestUserMessage = renderContext.lastUserMessage,
            onRetry = onRetryLastMessage,
            onEditMessage = onEditLastMessage,
        )
    }
}

@Composable
private fun FollowLatestBubble(
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.1f),
            )
            .clip(CircleShape)
            .background(BuyPilotColors.SurfaceCard.copy(alpha = 0.92f))
            .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.62f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_upward_24),
            contentDescription = "回到底部",
            tint = BuyPilotColors.PrimaryDark,
            modifier = Modifier
                .size(18.dp)
                .rotate(180f),
        )
    }
}

@Composable
private fun TimelineItemMotion(
    modifier: Modifier = Modifier,
    animateEnter: Boolean = true,
    hasEntered: Boolean = false,
    onEntered: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val shouldAnimate = remember { animateEnter && !hasEntered }
    val latestOnEntered by rememberUpdatedState(onEntered)

    if (!shouldAnimate) {
        LaunchedEffect(Unit) {
            if (!hasEntered) onEntered()
        }
        Box(modifier = modifier) {
            content()
        }
        return
    }

    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        latestOnEntered()
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 210, easing = FastOutSlowInEasing),
        ) + slideInVertically(
            animationSpec = tween(durationMillis = 230, easing = FastOutSlowInEasing),
            initialOffsetY = { it / 14 },
        ),
        modifier = modifier,
    ) {
        content()
    }
}

@Composable
private fun UserBubble(
    node: UserMessageNode,
    backendBaseUrl: String,
    hidden: Boolean = false,
    onImagePreview: (String) -> Unit,
    onPositioned: ((ClarificationChipSnapshot) -> Unit)? = null,
) {
    val bubbleShape = RoundedCornerShape(18.dp)
    val positionModifier = if (onPositioned != null) {
        Modifier.onGloballyPositioned { coordinates ->
            onPositioned(coordinates.toClarificationSnapshot())
        }
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (hidden) 0f else 1f),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val resolvedImageUrl = node.imageUrl.resolveProductImageUrl(backendBaseUrl)
        if (resolvedImageUrl != null) {
            Surface(
                modifier = Modifier
                    .width(176.dp)
                    .height(132.dp)
                    .then(if (node.content.isBlank()) positionModifier else Modifier)
                    .clickable(role = Role.Button) { onImagePreview(resolvedImageUrl) },
                color = BuyPilotColors.SurfaceCard,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    BuyPilotColors.Border.copy(alpha = 0.78f),
                ),
                shadowElevation = 2.dp,
            ) {
                AsyncImage(
                    model = resolvedImageUrl,
                    contentDescription = "发送的图片，点按放大",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(BuyPilotColors.SurfaceMuted),
                )
            }
        }
        if (node.content.isNotBlank()) {
            Box(
                modifier = Modifier
                    .widthIn(max = 304.dp)
                    .background(BuyPilotColors.Primary, bubbleShape)
                    .then(if (resolvedImageUrl == null) positionModifier else Modifier)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = node.content,
                    color = Color.White,
                    fontSize = BuyPilotType.Body,
                    lineHeight = 21.sp,
                )
            }
        }
    }
}

@Composable
private fun AssistantInlineStatus(
    message: String,
    motionEnabled: Boolean,
) {
    val displayMessage = message.withoutTrailingDots().takeIf { it.isNotBlank() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThinkingMascotAnimation(
            modifier = Modifier.size(28.dp),
            motionEnabled = motionEnabled,
        )
        if (displayMessage != null) {
            Spacer(Modifier.width(8.dp))
            ThinkingShimmerText(
                text = displayMessage,
                motionEnabled = motionEnabled,
            )
        }
    }
}

@Composable
private fun ThinkingBubble(
    message: String,
    motionEnabled: Boolean = true,
) {
    val displayMessage = message.withoutTrailingDots().takeIf { it.isNotBlank() } ?: return

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThinkingMascotAnimation(motionEnabled = motionEnabled)
        Spacer(Modifier.width(10.dp))
        ThinkingShimmerText(
            text = displayMessage,
            motionEnabled = motionEnabled,
        )
    }
}

@Composable
private fun ThinkingMascotAnimation(
    modifier: Modifier = Modifier,
    motionEnabled: Boolean = true,
) {
    if (!motionEnabled) {
        Image(
            painter = painterResource(R.drawable.redbean_bun_mascot_white),
            contentDescription = "BuyPilot mascot",
            modifier = modifier.size(32.dp),
            contentScale = ContentScale.Fit,
        )
        return
    }

    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.mascot_thinking))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        restartOnPlay = false,
    )

    if (composition != null) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = modifier.size(32.dp),
        )
    } else {
        Image(
            painter = painterResource(R.drawable.redbean_bun_mascot_white),
            contentDescription = "BuyPilot mascot",
            modifier = modifier.size(32.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun ThinkingShimmerText(
    text: String,
    modifier: Modifier = Modifier,
    motionEnabled: Boolean = true,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val textStyle = TextStyle(
        fontSize = BuyPilotType.Body,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    )
    val shimmerColor = if (isDarkTheme) {
        BuyPilotColors.SurfaceCard
    } else {
        BuyPilotColors.ThinkingShimmer
    }
    if (!motionEnabled) {
        Text(
            text = text,
            style = textStyle,
            color = shimmerColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "thinking_shimmer")
    val animationProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Restart,
        ),
        label = "thinking_shimmer_progress",
    )
    val textBrush = remember(animationProgress, shimmerColor) {
        object : ShaderBrush() {
            override fun createShader(size: Size): Shader {
                val width = size.width
                val gradientWidth = width * 2f
                val gradientProgress = gradientWidth * animationProgress
                return LinearGradientShader(
                    from = Offset(-width + gradientProgress, 0f),
                    to = Offset(gradientProgress, 0f),
                    colors = listOf(
                        shimmerColor.copy(alpha = 0.64f),
                        shimmerColor,
                        shimmerColor.copy(alpha = 0.64f),
                    ),
                )
            }
        }
    }

    Text(
        text = text,
        style = textStyle.copy(brush = textBrush),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

private fun String.withoutTrailingDots(): String =
    trim().replace(Regex("""[.。…]+$"""), "")

internal fun String.toClarificationUserMessage(defaultSkinTypeOptions: Set<String>): String {
    val clean = withoutMarkdownMarkup().trim()
    if (clean.isBlank()) return this
    val skinLabel = clean.withoutSkinSuffix()
    return when {
        clean == "不确定" -> "我还不确定肤质"
        clean in defaultSkinTypeOptions || clean.contains("肌") || clean.contains("肤") -> "我是${skinLabel}肌肤"
        else -> clean
    }
}

private fun calculateTimelineViewportBottomInset(
    density: Density,
    composerHeightPx: Int,
    imeBottomPx: Int,
): Dp = with(density) {
    val fallbackComposerPx = 120.dp.toPx()
    ((composerHeightPx.takeIf { it > 0 }?.toFloat() ?: fallbackComposerPx) + imeBottomPx).toDp()
}

internal fun calculateFloatingPanelBottomPadding(
    density: Density,
    composerHeightPx: Int,
    imeBottomPx: Int,
): Dp = with(density) {
    val fallbackComposerPx = 96.dp.toPx()
    ((composerHeightPx.takeIf { it > 0 }?.toFloat() ?: fallbackComposerPx) + imeBottomPx).toDp() +
        FloatingPanelComposerGap
}

private fun isTimelineNearEnd(
    listState: LazyListState,
    lastContentIndex: Int,
    bottomPaddingPx: Float,
    thresholdPx: Float,
): Boolean {
    if (lastContentIndex < 0) return true

    val layoutInfo = listState.layoutInfo
    val viewportBottom = layoutInfo.viewportEndOffset - bottomPaddingPx.roundToInt()
    val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull { it.index == lastContentIndex }
        ?: return false
    val distanceToBottom = viewportBottom - (lastVisibleItem.offset + lastVisibleItem.size)
    return distanceToBottom >= -thresholdPx
}

private suspend fun scrollTimelineItemToAnchorIfNeeded(
    listState: LazyListState,
    itemIndex: Int,
    anchorTopPx: Int,
    tolerancePx: Float,
    settleFrames: Int = 1,
) {
    if (itemIndex < 0) return

    repeat(settleFrames.coerceAtLeast(1)) {
        val layoutInfo = listState.layoutInfo
        val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == itemIndex }
        val desiredTop = layoutInfo.viewportStartOffset + anchorTopPx
        val needsCorrection = item == null || abs(item.offset - desiredTop) > tolerancePx
        if (needsCorrection) {
            listState.scrollToItem(index = itemIndex, scrollOffset = -anchorTopPx)
        }
        kotlinx.coroutines.delay(16L)
    }
}

private suspend fun animateTimelineItemToAnchor(
    listState: LazyListState,
    itemIndex: Int,
    anchorTopPx: Int,
    tolerancePx: Float,
) {
    if (itemIndex < 0) return
    listState.animateScrollToItem(index = itemIndex, scrollOffset = -anchorTopPx)
    repeat(2) {
        val layoutInfo = listState.layoutInfo
        val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == itemIndex }
        val desiredTop = layoutInfo.viewportStartOffset + anchorTopPx
        if (item != null) {
            val correction = (item.offset - desiredTop).toFloat()
            if (abs(correction) > tolerancePx) {
                listState.animateScrollBy(
                    value = correction,
                    animationSpec = tween(durationMillis = 180, easing = MenuEaseOut),
                )
            }
        }
        kotlinx.coroutines.delay(16L)
    }
}

private suspend fun animateLatestContentIntoView(
    listState: LazyListState,
    lastContentIndex: Int,
    bottomPaddingPx: Float,
    tolerancePx: Float,
) {
    if (lastContentIndex < 0) return
    repeat(6) { step ->
        if (
            isTimelineNearEnd(
                listState = listState,
                lastContentIndex = lastContentIndex,
                bottomPaddingPx = bottomPaddingPx,
                thresholdPx = tolerancePx,
            )
        ) {
            return
        }
        val lastItemVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == lastContentIndex }
        if (!lastItemVisible && step == 5) {
            listState.animateScrollToItem(index = lastContentIndex)
        }
        scrollActiveTurnIfNeeded(
            listState = listState,
            lastContentIndex = lastContentIndex,
            bottomPaddingPx = bottomPaddingPx,
            tolerancePx = tolerancePx,
        )
    }
    scrollActiveTurnIfNeeded(
        listState = listState,
        lastContentIndex = lastContentIndex,
        bottomPaddingPx = bottomPaddingPx,
        tolerancePx = tolerancePx,
        settleFrames = 2,
    )
}

private suspend fun scrollActiveTurnIfNeeded(
    listState: LazyListState,
    lastContentIndex: Int,
    bottomPaddingPx: Float,
    tolerancePx: Float,
    settleFrames: Int = 1,
) {
    if (lastContentIndex < 0) return

    repeat(settleFrames.coerceAtLeast(1)) {
        val layoutInfo = listState.layoutInfo
        val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull { it.index == lastContentIndex }
        if (lastVisibleItem == null) {
            val visibleHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset - bottomPaddingPx)
                .coerceAtLeast(tolerancePx)
            val delta = visibleHeight * 0.72f
            if (delta > 96f) {
                listState.animateScrollBy(
                    value = delta,
                    animationSpec = tween(durationMillis = 170, easing = MenuEaseOut),
                )
            } else {
                listState.scrollBy(delta)
            }
        } else {
            val viewportBottom = layoutInfo.viewportEndOffset - bottomPaddingPx.roundToInt()
            val overflow = (lastVisibleItem.offset + lastVisibleItem.size) - viewportBottom
            if (overflow > tolerancePx) {
                val delta = overflow.toFloat()
                if (delta > 96f) {
                    listState.animateScrollBy(
                        value = delta,
                        animationSpec = tween(durationMillis = 170, easing = MenuEaseOut),
                    )
                } else {
                    listState.scrollBy(delta)
                }
            }
        }
        kotlinx.coroutines.delay(16L)
    }
}

@Composable
private fun ClarificationBlock(
    nodeKey: String,
    payload: ClarificationPayload,
    questionRevealKey: String,
    questionRevealProgress: TextRevealProgress?,
    questionCompleted: Boolean,
    animateQuestion: Boolean,
    questionRevealDelayMs: Long,
    anchorRevealed: Boolean,
    dismissed: Boolean,
    dismissing: Boolean,
    selectedOption: String?,
    onOption: (String, ClarificationChipSnapshot?) -> Unit,
    onManualInput: () -> Unit,
    onManualSource: (ClarificationChipSnapshot) -> Unit,
    onQuestionRevealComplete: () -> Unit,
    onQuestionRevealActiveChange: (Boolean) -> Unit,
    onQuestionRevealProgress: (Int, Int) -> Unit,
    onCardEntered: () -> Unit,
    onCardDismissed: (String) -> Unit,
) {
    val question = payload.question
    val options = payload.suggestedOptions
    val manualPrompt = remember(payload.requiredSlots, question) {
        clarificationManualPromptFor(payload)
    }
    var dismissNotified by remember(nodeKey) { mutableStateOf(false) }
    var exitReady by remember(nodeKey) { mutableStateOf(false) }
    var questionSettled by remember(nodeKey) { mutableStateOf(questionCompleted) }
    var cardReady by remember(nodeKey) { mutableStateOf(questionCompleted) }
    var cardEntered by remember(nodeKey) { mutableStateOf(false) }
    val hasSelection = selectedOption != null
    val cardVisible = anchorRevealed && questionSettled && cardReady && !dismissed && (!dismissing || !exitReady)

    LaunchedEffect(questionCompleted, nodeKey) {
        if (questionCompleted) {
            questionSettled = true
            if (!cardReady) {
                kotlinx.coroutines.delay(ClarificationQuestionToCardDelayMs)
                cardReady = true
            }
        }
    }

    LaunchedEffect(cardVisible, nodeKey) {
        if (cardVisible && !cardEntered) {
            kotlinx.coroutines.delay(ClarificationCardEnterMs.toLong())
            cardEntered = true
            onCardEntered()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (anchorRevealed && !dismissed) {
            StreamingAssistantText(
                nodeKey = questionRevealKey,
                content = question,
                done = true,
                revealState = questionRevealProgress,
                alreadyCompleted = questionCompleted,
                animateInitialCompleted = animateQuestion,
                initialRevealDelayMs = questionRevealDelayMs,
                onRevealComplete = {
                    questionSettled = true
                    onQuestionRevealComplete()
                },
                onRevealActiveChange = onQuestionRevealActiveChange,
                onRevealProgress = onQuestionRevealProgress,
            )
        }
        AnimatedVisibility(
            visible = cardVisible,
            enter = fadeIn(
                animationSpec = tween(durationMillis = ClarificationCardEnterMs, easing = PremiumRevealEase),
            ) + slideInVertically(
                animationSpec = tween(durationMillis = ClarificationCardEnterMs, easing = PremiumRevealEase),
                initialOffsetY = { it / 4 },
            ) + scaleIn(
                animationSpec = tween(durationMillis = ClarificationCardEnterMs, easing = PremiumRevealEase),
                initialScale = 0.955f,
                transformOrigin = TransformOrigin(0.08f, 0f),
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = ClarificationExitMs, easing = MenuEaseIn),
            ) + slideOutVertically(
                animationSpec = tween(durationMillis = ClarificationExitMs, easing = MenuEaseIn),
                targetOffsetY = { -it / 12 },
            ) + scaleOut(
                animationSpec = tween(durationMillis = ClarificationExitMs, easing = MenuEaseIn),
                targetScale = 0.975f,
            ),
        ) {
            ClarificationCardContent(
                options = options,
                selectedOption = selectedOption,
                dismissing = dismissing,
                hasSelection = hasSelection,
                manualPrompt = manualPrompt,
                onOption = onOption,
                onManualInput = onManualInput,
                onManualSource = onManualSource,
            )
        }
    }

    LaunchedEffect(dismissing, nodeKey) {
        exitReady = false
        if (dismissing) {
            kotlinx.coroutines.delay(ClarificationSelectionHoldMs.toLong())
            exitReady = true
        }
    }

    LaunchedEffect(dismissing, nodeKey) {
        if (dismissing && !dismissNotified) {
            kotlinx.coroutines.delay((ClarificationSelectionHoldMs + ClarificationExitMs).toLong())
            dismissNotified = true
            onCardDismissed(nodeKey)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClarificationCardContent(
    options: List<String>,
    selectedOption: String?,
    dismissing: Boolean,
    hasSelection: Boolean,
    manualPrompt: String,
    onOption: (String, ClarificationChipSnapshot?) -> Unit,
    onManualInput: () -> Unit,
    onManualSource: (ClarificationChipSnapshot) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(
                    if (hasSelection) BuyPilotColors.Primary.copy(alpha = 0.6f)
                    else BuyPilotColors.ProductSelectionLine,
                    RoundedCornerShape(2.dp),
                ),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (options.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    options.forEach { label ->
                        AnimatedVisibility(
                            visible = selectedOption == null || selectedOption == label,
                            enter = fadeIn(tween(190, easing = MenuEaseOut)),
                            exit = fadeOut(tween(100, easing = MenuEaseIn)) + scaleOut(
                                tween(120, easing = MenuEaseIn),
                                targetScale = 0.92f,
                            ),
                        ) {
                            ClarificationOptionChip(
                                label = label,
                                selected = selectedOption == label,
                                enabled = !dismissing && selectedOption == null,
                            ) { snapshot ->
                                onOption(label, snapshot)
                            }
                        }
                    }
                }
            }
            if (!hasSelection) {
                ClarificationManualInputRow(
                    enabled = !dismissing,
                    prompt = manualPrompt,
                    onClick = onManualInput,
                    onPositioned = onManualSource,
                )
            }
        }
    }
}

internal fun clarificationManualPromptFor(payload: ClarificationPayload): String {
    val slots = payload.requiredSlots.mapTo(mutableSetOf()) { it.trim().lowercase() }
    val question = payload.question
    val fallbackSlotLabel = slots.firstNotNullOfOrNull { it.clarificationSlotLabelOrNull() }
    return when {
        "category" in slots || "哪一类" in question || "品类" in question -> "直接输入想买的品类"
        "budget" in slots || "预算" in question || "价位" in question -> "直接输入预算范围"
        "product_type" in slots || "具体哪一类" in question -> "直接输入商品类型"
        "skin_type" in slots || "肤质" in question -> "直接输入肤质"
        "target_product" in slots || "哪个商品" in question -> "直接输入商品名称"
        fallbackSlotLabel != null -> "直接输入$fallbackSlotLabel"
        else -> "直接输入你的答案"
    }
}

private fun String.clarificationSlotLabelOrNull(): String? =
    when (this) {
        "budget_min", "budget_max", "price", "price_range" -> "预算"
        "brand", "brand_prefer" -> "品牌偏好"
        "brand_avoid" -> "排除品牌"
        "origin", "origin_prefer" -> "产地偏好"
        "origin_avoid" -> "排除产地"
        "ingredient", "ingredient_prefer" -> "成分偏好"
        "ingredient_avoid" -> "排除成分"
        "use_scenario", "scenario" -> "使用场景"
        "storage" -> "存储容量"
        "screen_size" -> "屏幕尺寸"
        "sport_type" -> "运动类型"
        "season" -> "适用季节"
        "dietary" -> "饮食偏好"
        else -> null
    }

@Composable
internal fun ClarificationOptionScroller(
    labels: List<String>,
    modifier: Modifier = Modifier,
    selectedLabel: String? = null,
    enabled: Boolean = true,
    onClick: ((String, ClarificationChipSnapshot?) -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    val canScrollBackward by remember(labels.size) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    val canScrollForward by remember(labels.size) {
        derivedStateOf { listState.canScrollForward }
    }
    val leadingEdgeAlpha by animateFloatAsState(
        targetValue = if (canScrollBackward) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = MenuEaseOut),
        label = "clarification_options_leading_edge",
    )
    val trailingEdgeAlpha by animateFloatAsState(
        targetValue = if (canScrollForward) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = MenuEaseOut),
        label = "clarification_options_trailing_edge",
    )

    EdgeFadedLazyRow(
        modifier = modifier,
        leadingAlpha = leadingEdgeAlpha,
        trailingAlpha = trailingEdgeAlpha,
        height = 38.dp,
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 8.dp, end = 40.dp),
        ) {
            itemsIndexed(labels, key = { index, label -> "$index:$label" }) { index, label ->
                AnimatedVisibility(
                    visible = selectedLabel == null || selectedLabel == label,
                    enter = fadeIn(
                        animationSpec = tween(
                            durationMillis = 190,
                            delayMillis = (index * 40).coerceAtMost(160),
                            easing = MenuEaseOut,
                        ),
                    ) + slideInHorizontally(
                        animationSpec = tween(
                            durationMillis = 220,
                            delayMillis = (index * 40).coerceAtMost(160),
                            easing = MenuEaseOut,
                        ),
                        initialOffsetX = { it / 4 },
                    ),
                    exit = fadeOut(
                        animationSpec = tween(durationMillis = 100, easing = MenuEaseIn),
                    ) + scaleOut(
                        animationSpec = tween(durationMillis = 120, easing = MenuEaseIn),
                        targetScale = 0.92f,
                    ),
                ) {
                    ClarificationOptionChip(
                        label = label,
                        selected = selectedLabel == label,
                        enabled = enabled && selectedLabel == null,
                    ) { snapshot ->
                        onClick?.invoke(label, snapshot)
                    }
                }
            }
        }
    }
}

@Composable
internal fun EdgeFadedLazyRow(
    modifier: Modifier = Modifier,
    leadingAlpha: Float,
    trailingAlpha: Float,
    height: Dp,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        content()
        ChipEdgeFade(
            leading = false,
            alpha = trailingAlpha,
            height = height,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
        ChipEdgeFade(
            leading = true,
            alpha = leadingAlpha,
            height = height,
            modifier = Modifier.align(Alignment.CenterStart),
        )
    }
}

@Composable
private fun ChipEdgeFade(
    leading: Boolean,
    alpha: Float,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    if (alpha <= 0.01f) return
    val colors = if (leading) {
        listOf(
            BuyPilotColors.SurfaceCard.copy(alpha = 0.98f * alpha),
            BuyPilotColors.SurfaceCard.copy(alpha = 0.7f * alpha),
            BuyPilotColors.SurfaceCard.copy(alpha = 0f),
        )
    } else {
        listOf(
            BuyPilotColors.SurfaceCard.copy(alpha = 0f),
            BuyPilotColors.SurfaceCard.copy(alpha = 0.7f * alpha),
            BuyPilotColors.SurfaceCard.copy(alpha = 0.98f * alpha),
        )
    }
    Box(
        modifier = modifier
            .width(ChipEdgeFadeWidth)
            .height(height)
            .background(Brush.horizontalGradient(colors)),
    )
}

@Composable
private fun ClarificationStatusPill(label: String) {
    Text(
        text = label,
        color = BuyPilotColors.Info.copy(alpha = 0.92f),
        fontSize = BuyPilotType.Tiny,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .background(BuyPilotColors.Info.copy(alpha = 0.08f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}
