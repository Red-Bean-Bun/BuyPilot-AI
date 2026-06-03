package com.buypilot.feature.chat.ui

import androidx.annotation.DrawableRes
import android.graphics.Bitmap
import android.net.Uri
import android.content.Context
import android.os.SystemClock
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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
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
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
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
import com.buypilot.feature.chat.presentation.TimelineRenderItem
import com.buypilot.feature.chat.presentation.TimelineRenderContext
import com.buypilot.feature.chat.presentation.UserTimelineItem
import com.buypilot.feature.chat.presentation.clarificationQuestionRevealKey
import com.buypilot.feature.chat.presentation.containsNodeKey
import com.buypilot.feature.chat.presentation.lastContentIndex as timelineLastContentIndex
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
private const val ClarificationCardEnterMs = 460
private const val ClarificationCardSequenceReleaseMs = 120L

private val TimelineNearEndThreshold = 24.dp
private val TimelineFollowCorrectionTolerance = 8.dp
private val TimelineSafeGap = 12.dp
private val TimelineBottomReadingBuffer = 72.dp
private val TimelineKeyboardBottomReadingBuffer = 56.dp
private val TimelineAnchorTopGap = 28.dp
private val FloatingPanelComposerGap = 8.dp
private val TimelineJumpButtonComposerGap = 14.dp
private val ChipEdgeFadeWidth = 46.dp
private const val RouteReturnAnchorSettleFrames = 14
private const val RouteReturnAnchorStableFrames = 3
private const val RouteReturnAnchorFrameDelayMs = 24L
private const val ThinkingVisibilityDelayMs = 400L
private const val ThinkingTextHandoffEnterMs = 220
private const val ThinkingTextHandoffHoldMs = 240L
private const val ThinkingTextHandoffExitMs = 180L
private const val ThinkingMascotLoopDurationMs = 1_800L
private const val ThinkingShimmerDurationMs = 2_600
private const val TurnStructuredEnterMs = 320
private const val TurnStructuredEnterDelayMs = 90
private const val ThinkingToTextHandoffMs = 0L
private const val FinalDecisionAnchorWaitFrames = 12
private const val UserBubbleAnchorWaitFrames = 2
private const val UserBubbleAnchorSettleFrames = 8
private const val TimelineRevealScrollThrottleMs = 40L
private const val ManualScrollSettleMs = 520L

private enum class ThinkingTextHandoffPhase {
    Thinking,
    Exiting,
    Content,
}

private val LocalThinkingMascotComposition = staticCompositionLocalOf<LottieComposition?> { null }

@Stable
private class TurnStreamingCoordinator(
    val visibleNodeKeys: Set<String>,
    val textHandoffKeys: Set<String>,
    val structuredHandoffKeys: Set<String>,
    val visualActive: Boolean,
    val markTextCompleted: (String) -> Unit,
    val updateTextProgress: (String, Int, Int) -> Unit,
    val markStructuredEntered: (String) -> Unit,
)

@Composable
internal fun ChatTimeline(
    state: ChatUiState,
    timelinePresentation: TimelinePresentationState,
    externalRouteFreezeRequestId: Int = 0,
    externalRouteFreezeAnchorNodeKey: String? = null,
    onExternalRouteFreezeReady: (Int) -> Unit = {},
    onClarificationOption: (String, ClarificationChipSnapshot?) -> Unit,
    onClarificationManualInput: () -> Unit,
    onClarificationManualSource: (String, ClarificationChipSnapshot) -> Unit,
    onCriteriaEdit: (CriteriaCardPayload) -> Unit,
    onProductOpen: (String, String?, String?) -> Unit,
    onProductDetailOpen: (String, String, String?) -> Unit,
    onConvergeRecommendation: (String) -> Unit,
    onDecisionEvidence: (FinalDecisionPayload, String?) -> Unit,
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
    liveRevealedMessageKeys: Set<String>,
    onMessageRevealComplete: (String) -> Unit,
    onMessageRevealActiveChange: (String, Boolean) -> Unit,
    onAssistantTurnVisualActiveChange: (String, Boolean) -> Unit,
    onUserBubblePositioned: (String, ClarificationChipSnapshot) -> Unit,
    onClarificationCardDismissed: (String) -> Unit,
) {
    val listState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }
    val thinkingMascotComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.mascot_thinking),
    )
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isUserDragging by listState.interactionSource.collectIsDraggedAsState()
    var manualScrollSettling by remember { mutableStateOf(false) }
    val manualScrollActive = isUserDragging || manualScrollSettling
    val timelineMotionEnabled = !manualScrollActive
    var followCurrentTurnDuringReveal by remember { mutableStateOf(false) }
    var userDetachedFromLatest by rememberSaveable { mutableStateOf(false) }
    var lastHandledUserMessageKey by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingUserBubbleAnchorKey by rememberSaveable { mutableStateOf<String?>(null) }
    var activeUserBubbleFollowAnchorKey by rememberSaveable { mutableStateOf<String?>(null) }
    var lastAnchoredAssistantTurnId by rememberSaveable { mutableStateOf<String?>(null) }
    var lastRevealScrollAtMs by remember { mutableStateOf(0L) }
    val viewportController = remember { TimelineViewportController() }
    val measuredNodeTopByKey = remember { mutableMapOf<String, Int>() }
    var timelineRootTopPx by remember { mutableIntStateOf(0) }
    var suppressReturnAutoFocus by rememberSaveable { mutableStateOf(false) }
    var routeReturnAnchorCaptured by rememberSaveable { mutableStateOf(false) }
    var routeReturnRestorePending by rememberSaveable { mutableStateOf(false) }
    var routeReturnWasCovered by rememberSaveable { mutableStateOf(false) }
    var routeReturnItemKey by rememberSaveable { mutableStateOf<String?>(null) }
    var routeReturnItemIndex by rememberSaveable { mutableStateOf(0) }
    var routeReturnScrollOffset by rememberSaveable { mutableStateOf(0) }
    var routeReturnNodeKey by rememberSaveable { mutableStateOf<String?>(null) }
    var routeReturnNodeTopPx by rememberSaveable { mutableStateOf<Int?>(null) }
    var routeReturnTurnId by rememberSaveable { mutableStateOf<String?>(null) }
    var routeReturnFinalDecisionKey by rememberSaveable { mutableStateOf<String?>(null) }
    var routeReturnSettledTurnId by rememberSaveable { mutableStateOf<String?>(null) }
    var routeReturnSettledFinalDecisionKey by rememberSaveable { mutableStateOf<String?>(null) }
    var clarificationFreezeCaptured by remember { mutableStateOf(false) }
    var clarificationFreezeItemKey by remember { mutableStateOf<String?>(null) }
    var clarificationFreezeItemIndex by remember { mutableStateOf(0) }
    var clarificationFreezeScrollOffset by remember { mutableStateOf(0) }
    val suppressTimelineAutoFocus = suppressReturnAutoFocus || isClarificationFlightActive
    val revealSnapshotHolder = rememberSaveable(saver = TimelineRevealSnapshotHolderSaver) {
        TimelineRevealSnapshotHolder()
    }
    val revealStore = remember {
        TimelineRevealStore(
            initialSnapshot = revealSnapshotHolder.snapshot,
            onSnapshotChanged = { snapshot ->
                revealSnapshotHolder.snapshot = snapshot
            },
        ).also { store ->
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
    LaunchedEffect(revealedMessageKeys, liveRevealedMessageKeys) {
        revealedMessageKeys.forEach { key ->
            if (!revealStore.hasCompletedText(key)) {
                revealStore.markTextCompleted(key)
            }
        }
        liveRevealedMessageKeys.forEach { key ->
            if (!revealStore.hasLiveRevealedText(key)) {
                revealStore.markTextLiveRevealed(key)
            }
        }
    }
    val timelineRevealedMessageKeys = revealStore.completedTextKeySet()
    val timelineLiveRevealedMessageKeys = revealStore.liveRevealedTextKeySet()
    val hasTimelineError = timelinePresentation.hasTimelineError
    val timelineItems = timelinePresentation.items
    val timelineScrollAnchorSignature = remember(timelineItems) {
        timelineItems.scrollAnchorSignature()
    }
    val timelineItemKeys = remember(timelineScrollAnchorSignature) {
        timelineItems.mapTo(mutableSetOf()) { it.key }
    }
    val latestFinalDecisionKey = timelinePresentation.latestFinalDecisionKey
    val latestFinalDecisionTurnId = timelinePresentation.latestFinalDecisionTurnId
    val finalDecisionKeys = timelinePresentation.finalDecisionKeys
    val measuredTimelineNodeKeys = remember(timelinePresentation.productDeckNodes, finalDecisionKeys) {
        buildSet {
            addAll(finalDecisionKeys)
            timelinePresentation.productDeckNodes.forEach { deck -> add(deck.key) }
        }
    }
    var lastFocusedFinalDecisionKey by rememberSaveable {
        mutableStateOf(
            routeAwareFocusedFinalDecisionKey(
                currentFocusedKey = latestFinalDecisionKey,
                routeReturnAnchorCaptured = routeReturnAnchorCaptured,
                capturedFinalDecisionKey = routeReturnFinalDecisionKey,
                latestFinalDecisionKey = latestFinalDecisionKey,
            ),
        )
    }
    LaunchedEffect(routeReturnAnchorCaptured, routeReturnFinalDecisionKey, latestFinalDecisionKey) {
        lastFocusedFinalDecisionKey = routeAwareFocusedFinalDecisionKey(
            currentFocusedKey = lastFocusedFinalDecisionKey,
            routeReturnAnchorCaptured = routeReturnAnchorCaptured,
            capturedFinalDecisionKey = routeReturnFinalDecisionKey,
            latestFinalDecisionKey = latestFinalDecisionKey,
        )
    }
    fun recordMeasuredNodeTop(key: String, topPx: Int) {
        if (key in measuredTimelineNodeKeys && measuredNodeTopByKey[key] != topPx) {
            measuredNodeTopByKey[key] = topPx
        }
    }
    fun captureRouteReturnAnchor(anchorNodeKey: String? = null) {
        if (timelineItems.isEmpty()) return
        val index = listState.firstVisibleItemIndex.coerceIn(0, timelineItems.lastIndex)
        routeReturnItemIndex = index
        routeReturnScrollOffset = listState.firstVisibleItemScrollOffset
        routeReturnItemKey = timelineItems.getOrNull(index)?.key
        routeReturnNodeKey = anchorNodeKey
        routeReturnNodeTopPx = anchorNodeKey?.let { measuredNodeTopByKey[it] }
        routeReturnTurnId = routeReturnAnchorTurnId(
            timelineItems = timelineItems,
            visibleItemIndex = index,
            anchorNodeKey = anchorNodeKey,
            currentTurnId = state.currentTurnId,
        )
        routeReturnFinalDecisionKey = latestFinalDecisionKey
        routeReturnAnchorCaptured = true
        routeReturnRestorePending = false
    }
    fun prepareForSecondaryPageOpen(anchorNodeKey: String? = null) {
        captureRouteReturnAnchor(anchorNodeKey = anchorNodeKey)
        suppressReturnAutoFocus = true
    }
    LaunchedEffect(externalRouteFreezeRequestId) {
        if (externalRouteFreezeRequestId <= 0) return@LaunchedEffect
        prepareForSecondaryPageOpen(anchorNodeKey = externalRouteFreezeAnchorNodeKey)
        repeat(RouteReturnAnchorStableFrames) {
            withFrameNanos { }
        }
        onExternalRouteFreezeReady(externalRouteFreezeRequestId)
    }
    val openProductFromTimeline: (String, String?, String?) -> Unit = { deckId, productId, sourceNodeKey ->
        prepareForSecondaryPageOpen(anchorNodeKey = sourceNodeKey)
        onProductOpen(deckId, productId, sourceNodeKey)
    }
    val openProductDetailFromTimeline:
        (String, String, String?, String?) -> Unit = { deckId, productId, anchorNodeKey, deckNodeKey ->
        prepareForSecondaryPageOpen(anchorNodeKey = anchorNodeKey)
        onProductDetailOpen(deckId, productId, deckNodeKey)
    }
    LaunchedEffect(timelineScrollAnchorSignature, timelinePresentation.revealKeys) {
        revealStore.pruneToKeys(
            timelineItemKeys = timelineItemKeys,
            nodeKeys = timelinePresentation.revealKeys,
        )
    }
    LaunchedEffect(measuredTimelineNodeKeys) {
        measuredNodeTopByKey.keys
            .filterNot { it in measuredTimelineNodeKeys }
            .toList()
            .forEach { measuredNodeTopByKey.remove(it) }
    }
    val renderContext = timelinePresentation.renderContext
    fun nearEndReferenceIndex(): Int = timelineItems.timelineLastContentIndex(state, hasTimelineError)
    fun requestCurrentTurnRevealFollowScroll() {
        if (suppressTimelineAutoFocus) return
        if (!followCurrentTurnDuringReveal) return
        val now = System.currentTimeMillis()
        if (now - lastRevealScrollAtMs >= TimelineRevealScrollThrottleMs) {
            lastRevealScrollAtMs = now
            viewportController.requestReadableTurnFollow(
                allowOffscreenAnchor = false,
                isRouteReturnSuppressed = suppressReturnAutoFocus,
                isClarificationFlightActive = isClarificationFlightActive,
            )
        }
    }
    fun markStructuredEntered(key: String) {
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
    val targetEndReadingBuffer = timelineReadingBuffer
    val endReadingBuffer = targetEndReadingBuffer
    val nearEndThresholdPx = with(density) { TimelineNearEndThreshold.toPx() }
    val followCorrectionTolerancePx = with(density) { TimelineFollowCorrectionTolerance.toPx() }
    val latestContentBottomPaddingPx = with(density) {
        timelineBottomPadding.toPx() + timelineReadingBuffer.toPx()
    }
    val anchorTopOffsetPx = with(density) { TimelineAnchorTopGap.toPx().roundToInt() }
    val latestTimelineItems by rememberUpdatedState(timelineItems)
    val latestState by rememberUpdatedState(state)
    val latestLatestFinalDecisionKey by rememberUpdatedState(latestFinalDecisionKey)
    val latestCurrentTurnId by rememberUpdatedState(state.currentTurnId)
    val latestActiveUserBubbleFollowAnchorKey by rememberUpdatedState(activeUserBubbleFollowAnchorKey)
    suspend fun restoreRouteReturnNodeAnchorIfNeeded() {
        val nodeKey = routeReturnNodeKey ?: return
        val originalTopPx = routeReturnNodeTopPx ?: return
        var stableFrames = 0
        repeat(RouteReturnAnchorSettleFrames) {
            withFrameNanos { }
            val currentTopPx = measuredNodeTopByKey[nodeKey] ?: return
            val delta = currentTopPx - originalTopPx
            if (abs(delta) > followCorrectionTolerancePx) {
                listState.scrollBy(delta.toFloat())
                stableFrames = 0
            } else {
                stableFrames += 1
                if (stableFrames >= RouteReturnAnchorStableFrames) return
            }
            kotlinx.coroutines.delay(RouteReturnAnchorFrameDelayMs)
        }
    }
    suspend fun scrollMeasuredNodeToAnchorIfNeeded(
        nodeKey: String,
        waitFrames: Int = 1,
        settleFrames: Int = 5,
    ): Boolean {
        var measuredTop: Int? = null
        for (frame in 0 until waitFrames.coerceAtLeast(1)) {
            withFrameNanos { }
            measuredTop = measuredNodeTopByKey[nodeKey]
            if (measuredTop != null) break
        }
        if (measuredTop == null) return false
        repeat(settleFrames.coerceAtLeast(1)) {
            withFrameNanos { }
            val currentTop = measuredNodeTopByKey[nodeKey] ?: return true
            val desiredTop = timelineRootTopPx + anchorTopOffsetPx
            val delta = currentTop - desiredTop
            if (abs(delta) > followCorrectionTolerancePx) {
                if (it == 0 && abs(delta) > 96) {
                    listState.animateScrollBy(
                        value = delta.toFloat(),
                        animationSpec = tween(durationMillis = 150, easing = MenuEaseOut),
                    )
                } else {
                    listState.scrollBy(delta.toFloat())
                }
            }
            kotlinx.coroutines.delay(16L)
        }
        return true
    }
    val isNearTimelineEnd by remember(
        timelineScrollAnchorSignature,
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
                nearEndReferenceIndex = nearEndReferenceIndex(),
                bottomPaddingPx = latestContentBottomPaddingPx,
                thresholdPx = nearEndThresholdPx,
            )
        }
    }
    val currentTurnFollowActive = followCurrentTurnDuringReveal && (state.isStreaming || isAssistantVisualActive)

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
        followCurrentTurnDuringReveal = false
        if (routeReturnRestorePending) return@LaunchedEffect
        kotlinx.coroutines.delay(420L)
        suppressReturnAutoFocus = false
    }

    LaunchedEffect(
        routeReturnRestorePending,
        timelineScrollAnchorSignature,
        timelineItems.size,
        routeReturnItemKey,
        routeReturnScrollOffset,
    ) {
        if (!routeReturnRestorePending || timelineItems.isEmpty()) return@LaunchedEffect
        suppressReturnAutoFocus = true
        followCurrentTurnDuringReveal = false
        viewportController.requestRouteReturn(
            key = routeReturnItemKey,
            index = routeReturnItemIndex,
            scrollOffset = routeReturnScrollOffset,
            isRouteReturnSuppressed = suppressReturnAutoFocus,
            isClarificationFlightActive = isClarificationFlightActive,
        )
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
        timelineScrollAnchorSignature,
        timelineItems.size,
        composerHeightPx,
        clarificationFreezeItemKey,
        clarificationFreezeScrollOffset,
    ) {
        if (!isClarificationFlightActive || !clarificationFreezeCaptured || timelineItems.isEmpty()) {
            return@LaunchedEffect
        }
        if (
            !shouldFreezeTimelineViewport(
                currentIndex = listState.firstVisibleItemIndex,
                currentOffset = listState.firstVisibleItemScrollOffset,
                targetIndex = clarificationFreezeItemIndex,
                targetOffset = clarificationFreezeScrollOffset,
                tolerancePx = followCorrectionTolerancePx,
            )
        ) {
            return@LaunchedEffect
        }
        viewportController.requestViewportFreeze(
            key = clarificationFreezeItemKey,
            index = clarificationFreezeItemIndex,
            scrollOffset = clarificationFreezeScrollOffset,
            isRouteReturnSuppressed = suppressReturnAutoFocus,
            isClarificationFlightActive = isClarificationFlightActive,
        )
    }

    LaunchedEffect(state.lastUserMessageKey, activeFlightMessageKey, timelineScrollAnchorSignature, timelineItems.size) {
        val key = state.lastUserMessageKey ?: return@LaunchedEffect
        if (shouldConsumeFlightUserAnchor(key, activeFlightMessageKey)) {
            lastHandledUserMessageKey = key
            return@LaunchedEffect
        }
        val index = timelineItems.indexOfFirst { it is UserTimelineItem && it.node.key == key }
        if (index >= 0 && key != lastHandledUserMessageKey) {
            routeReturnSettledTurnId = null
            routeReturnSettledFinalDecisionKey = null
            lastAnchoredAssistantTurnId = null
            followCurrentTurnDuringReveal = false
            activeUserBubbleFollowAnchorKey = null
            userDetachedFromLatest = false
            viewportController.requestUserSent(
                key = key,
                index = index,
                isRouteReturnSuppressed = suppressReturnAutoFocus,
                isClarificationFlightActive = isClarificationFlightActive,
            )
        }
    }

    LaunchedEffect(
        state.currentTurnId,
        state.activeConvergenceDeckId,
        suppressReturnAutoFocus,
        manualScrollActive,
        timelineScrollAnchorSignature,
        timelineItems.size,
    ) {
        if (suppressReturnAutoFocus || manualScrollActive) return@LaunchedEffect
        val turnId = state.currentTurnId ?: return@LaunchedEffect
        if (
            turnId == lastAnchoredAssistantTurnId ||
            turnId == routeReturnSettledTurnId
        ) {
            return@LaunchedEffect
        }
        val assistantTurnIndex = timelineItems.indexOfFirst {
            it is AssistantTurnTimelineItem && it.turnId == turnId
        }
        if (assistantTurnIndex < 0) return@LaunchedEffect

        followCurrentTurnDuringReveal = false
        userDetachedFromLatest = false
        viewportController.requestAssistantStarted(
            turnId = turnId,
            index = assistantTurnIndex,
            isRouteReturnSuppressed = suppressReturnAutoFocus,
            isClarificationFlightActive = isClarificationFlightActive,
        )
    }

    var previousImeBottom by remember { mutableIntStateOf(0) }
    var wasNearEndBeforeKeyboard by remember { mutableStateOf(true) }
    LaunchedEffect(stableImeBottomPx) {
        val delta = stableImeBottomPx - previousImeBottom
        if (
            shouldScrollTimelineForKeyboardChange(
                deltaPx = delta,
                isComposerFocused = isComposerFocused,
                isUserDragging = manualScrollActive,
                wasNearEndBeforeKeyboard = wasNearEndBeforeKeyboard,
            )
        ) {
            viewportController.requestKeyboardChanged(
                deltaPx = delta,
                isRouteReturnSuppressed = suppressReturnAutoFocus,
                isClarificationFlightActive = isClarificationFlightActive,
            )
        }
        if (
            shouldReanchorReadableTurnAfterKeyboardCollapse(
                deltaPx = delta,
                currentTurnFollowActive = currentTurnFollowActive,
                isUserDragging = manualScrollActive,
            )
        ) {
            viewportController.requestReadableTurnFollow(
                allowOffscreenAnchor = true,
                isRouteReturnSuppressed = suppressReturnAutoFocus,
                isClarificationFlightActive = isClarificationFlightActive,
            )
        }
        if (stableImeBottomPx == 0) {
            wasNearEndBeforeKeyboard = isNearTimelineEnd
        }
        previousImeBottom = stableImeBottomPx
    }

    LaunchedEffect(isNearTimelineEnd, stableImeBottomPx) {
        if (stableImeBottomPx == 0) {
            wasNearEndBeforeKeyboard = isNearTimelineEnd
        }
    }

    LaunchedEffect(isNearTimelineEnd, isUserDragging) {
        if (isUserDragging) {
            userDetachedFromLatest = !isNearTimelineEnd
        }
        if (shouldPauseTimelineFollowForUserDrag(isUserDragging, isNearTimelineEnd)) {
            followCurrentTurnDuringReveal = false
        }
    }

    LaunchedEffect(isNearTimelineEnd) {
        if (isNearTimelineEnd) {
            userDetachedFromLatest = false
        }
    }

    LaunchedEffect(isUserDragging) {
        if (isUserDragging) {
            manualScrollSettling = true
            pendingUserBubbleAnchorKey = null
            activeUserBubbleFollowAnchorKey = null
            viewportController.interruptWithUserDrag()
            onTimelineDrag()
        } else if (manualScrollSettling) {
            kotlinx.coroutines.delay(ManualScrollSettleMs)
            manualScrollSettling = false
        }
    }

    LaunchedEffect(
        state.isStreaming,
        isAssistantVisualActive,
    ) {
        if (!state.isStreaming && !isAssistantVisualActive) {
            followCurrentTurnDuringReveal = false
            activeUserBubbleFollowAnchorKey = null
        }
    }

    LaunchedEffect(
        latestFinalDecisionKey,
        latestFinalDecisionTurnId,
        state.currentTurnId,
        timelineScrollAnchorSignature,
        timelineItems.size,
        composerHeightPx,
        suppressTimelineAutoFocus,
        manualScrollActive,
        userDetachedFromLatest,
    ) {
        val decisionKey = latestFinalDecisionKey ?: return@LaunchedEffect
        if (
            !shouldAutoFocusTimelineFinalDecision(
                decisionKey = decisionKey,
                decisionTurnId = latestFinalDecisionTurnId,
                currentTurnId = state.currentTurnId,
                lastFocusedDecisionKey = lastFocusedFinalDecisionKey,
                routeReturnSettledDecisionKey = routeReturnSettledFinalDecisionKey,
                autoFocusSuppressed = suppressTimelineAutoFocus,
                manualScrollActive = manualScrollActive,
                userDetachedFromLatest = userDetachedFromLatest,
            )
        ) {
            return@LaunchedEffect
        }
        val decisionItemIndex = timelineItems.indexOfFirst { it.containsNodeKey(decisionKey) }
        if (decisionItemIndex < 0) return@LaunchedEffect
        followCurrentTurnDuringReveal = false
        activeUserBubbleFollowAnchorKey = null
        userDetachedFromLatest = false
        viewportController.requestFinalDecision(
            key = decisionKey,
            turnId = latestFinalDecisionTurnId,
            index = decisionItemIndex,
            isRouteReturnSuppressed = suppressReturnAutoFocus,
            isClarificationFlightActive = isClarificationFlightActive,
        )
    }

    LaunchedEffect(
        currentTurnFollowActive,
        timelineItems.size,
        state.streamingTextKey,
        suppressTimelineAutoFocus,
        manualScrollActive,
    ) {
        if (!currentTurnFollowActive || suppressTimelineAutoFocus || manualScrollActive) return@LaunchedEffect
        viewportController.requestReadableTurnFollow(
            allowOffscreenAnchor = false,
            isRouteReturnSuppressed = suppressReturnAutoFocus,
            isClarificationFlightActive = isClarificationFlightActive,
        )
    }

    LaunchedEffect(
        viewportController.pendingIntent?.serial,
        suppressTimelineAutoFocus,
        manualScrollActive,
    ) {
        val pending = viewportController.pendingIntent ?: return@LaunchedEffect
        val intent = pending.intent
        fun clearPending() {
            viewportController.clear(pending.serial)
        }
        fun currentItems(): List<TimelineRenderItem> = latestTimelineItems

        if (intent.kind == TimelineScrollIntentKind.UserDrag) {
            clearPending()
            return@LaunchedEffect
        }
        if (shouldBlockTimelineAutoFocusForManualScroll(intent.kind, manualScrollActive)) {
            clearPending()
            return@LaunchedEffect
        }
        val intentAutoFocusSuppressed = isTimelineAutoFocusSuppressedForIntent(
            intentKind = intent.kind,
            isRouteReturnSuppressed = suppressReturnAutoFocus,
            isClarificationFlightActive = isClarificationFlightActive,
        )
        if (shouldBlockTimelineAutoFocusForSuppression(intent.kind, intentAutoFocusSuppressed)) {
            clearPending()
            return@LaunchedEffect
        }

        when (intent.kind) {
            TimelineScrollIntentKind.UserDrag -> Unit
            TimelineScrollIntentKind.RouteReturn -> {
                val items = currentItems()
                if (items.isEmpty()) {
                    clearPending()
                    return@LaunchedEffect
                }
                withFrameNanos { }
                val keyedIndex = intent.key
                    ?.let { key -> items.indexOfFirst { it.key == key } }
                    ?: -1
                val targetIndex = keyedIndex
                    .takeIf { it >= 0 }
                    ?: intent.index.coerceIn(0, items.lastIndex)
                listState.scrollToItem(
                    index = targetIndex,
                    scrollOffset = intent.scrollOffset.coerceAtLeast(0),
                )
                restoreRouteReturnNodeAnchorIfNeeded()
                val capturedTurnId = routeReturnTurnId
                lastHandledUserMessageKey = latestState.lastUserMessageKey
                if (capturedTurnId == latestState.currentTurnId) {
                    routeReturnSettledTurnId = capturedTurnId
                    lastAnchoredAssistantTurnId = capturedTurnId
                } else {
                    routeReturnSettledTurnId = null
                    lastAnchoredAssistantTurnId = null
                }
                val capturedFinalDecisionKey = routeReturnFinalDecisionKey
                if (capturedFinalDecisionKey == latestLatestFinalDecisionKey) {
                    routeReturnSettledFinalDecisionKey = capturedFinalDecisionKey
                    lastFocusedFinalDecisionKey = capturedFinalDecisionKey
                } else {
                    routeReturnSettledFinalDecisionKey = capturedFinalDecisionKey
                }
                routeReturnRestorePending = false
                routeReturnAnchorCaptured = false
                routeReturnWasCovered = false
                routeReturnItemKey = null
                routeReturnNodeKey = null
                routeReturnNodeTopPx = null
                routeReturnTurnId = null
                routeReturnFinalDecisionKey = null
                clearPending()
                kotlinx.coroutines.delay(420L)
                suppressReturnAutoFocus = false
            }
            TimelineScrollIntentKind.ViewportFreeze -> {
                val items = currentItems()
                if (items.isEmpty()) {
                    clearPending()
                    return@LaunchedEffect
                }
                val keyedIndex = intent.key
                    ?.let { key -> items.indexOfFirst { it.key == key } }
                    ?: -1
                val targetIndex = keyedIndex
                    .takeIf { it >= 0 }
                    ?: intent.index.coerceIn(0, items.lastIndex)
                listState.scrollToItem(
                    index = targetIndex,
                    scrollOffset = intent.scrollOffset.coerceAtLeast(0),
                )
                clearPending()
            }
            TimelineScrollIntentKind.UserSent -> {
                val items = currentItems()
                val targetIndex = intent.key
                    ?.let { key -> items.indexOfFirst { it is UserTimelineItem && it.node.key == key } }
                    ?.takeIf { it >= 0 }
                    ?: intent.index.takeIf { it >= 0 }
                    ?: run {
                        clearPending()
                        return@LaunchedEffect
                    }
                repeat(UserBubbleAnchorWaitFrames) {
                    withFrameNanos { }
                }
                animateTimelineItemToAnchor(
                    listState = listState,
                    itemIndex = targetIndex,
                    anchorTopPx = anchorTopOffsetPx,
                    tolerancePx = followCorrectionTolerancePx,
                )
                scrollTimelineItemToAnchorIfNeeded(
                    listState = listState,
                    itemIndex = targetIndex,
                    anchorTopPx = anchorTopOffsetPx,
                    tolerancePx = followCorrectionTolerancePx,
                    settleFrames = UserBubbleAnchorSettleFrames,
                )
                intent.key?.let { key ->
                    lastHandledUserMessageKey = key
                    pendingUserBubbleAnchorKey = key
                    activeUserBubbleFollowAnchorKey = key
                }
                latestCurrentTurnId
                    ?.takeIf { it.isNotBlank() && it != lastAnchoredAssistantTurnId }
                    ?.let { currentTurnId ->
                        val assistantIndex = items.indexOfFirst {
                            it is AssistantTurnTimelineItem && it.turnId == currentTurnId
                        }
                        if (assistantIndex >= 0) {
                            viewportController.requestAssistantStarted(
                                turnId = currentTurnId,
                                index = assistantIndex,
                                isRouteReturnSuppressed = suppressReturnAutoFocus,
                                isClarificationFlightActive = isClarificationFlightActive,
                            )
                        }
                    }
                clearPending()
            }
            TimelineScrollIntentKind.AssistantStarted -> {
                val turnId = intent.turnId?.takeIf { it.isNotBlank() } ?: run {
                    clearPending()
                    return@LaunchedEffect
                }
                val items = currentItems()
                val targetIndex = items.indexOfFirst {
                    it is AssistantTurnTimelineItem && it.turnId == turnId
                }.takeIf { it >= 0 } ?: run {
                    clearPending()
                    return@LaunchedEffect
                }
                val latestUserKey = latestState.lastUserMessageKey
                val latestUserIndex = latestUserKey
                    ?.let { key -> items.indexOfFirst { it is UserTimelineItem && it.node.key == key } }
                    ?: -1
                val shouldPreserveUserBubbleAnchor = shouldPreserveUserBubbleAnchorForAssistantStart(
                    pendingUserBubbleAnchorKey = pendingUserBubbleAnchorKey,
                    latestUserMessageKey = latestUserKey,
                    userItemIndex = latestUserIndex,
                    assistantItemIndex = targetIndex,
                )
                if (shouldPreserveUserBubbleAnchor) {
                    animateTimelineItemToAnchor(
                        listState = listState,
                        itemIndex = latestUserIndex,
                        anchorTopPx = anchorTopOffsetPx,
                        tolerancePx = followCorrectionTolerancePx,
                    )
                    scrollTimelineItemToAnchorIfNeeded(
                        listState = listState,
                        itemIndex = latestUserIndex,
                        anchorTopPx = anchorTopOffsetPx,
                        tolerancePx = followCorrectionTolerancePx,
                        settleFrames = UserBubbleAnchorSettleFrames,
                    )
                    pendingUserBubbleAnchorKey = null
                    activeUserBubbleFollowAnchorKey = latestUserKey
                    followCurrentTurnDuringReveal = true
                    userDetachedFromLatest = false
                    lastRevealScrollAtMs = 0L
                    lastAnchoredAssistantTurnId = turnId
                    clearPending()
                    return@LaunchedEffect
                }
                pendingUserBubbleAnchorKey = null
                activeUserBubbleFollowAnchorKey = null
                animateTimelineItemToAnchor(
                    listState = listState,
                    itemIndex = targetIndex,
                    anchorTopPx = anchorTopOffsetPx,
                    tolerancePx = followCorrectionTolerancePx,
                )
                repeat(1) {
                    scrollTimelineItemToAnchorIfNeeded(
                        listState = listState,
                        itemIndex = targetIndex,
                        anchorTopPx = anchorTopOffsetPx,
                        tolerancePx = followCorrectionTolerancePx,
                        settleFrames = 4,
                    )
                }
                followCurrentTurnDuringReveal = true
                userDetachedFromLatest = false
                lastRevealScrollAtMs = 0L
                lastAnchoredAssistantTurnId = turnId
                clearPending()
            }
            TimelineScrollIntentKind.FinalDecision -> {
                val measuredNodeKey = intent.key
                if (
                    measuredNodeKey != null &&
                    scrollMeasuredNodeToAnchorIfNeeded(
                        nodeKey = measuredNodeKey,
                        waitFrames = FinalDecisionAnchorWaitFrames,
                        settleFrames = 4,
                    )
                ) {
                    lastFocusedFinalDecisionKey = measuredNodeKey
                    clearPending()
                    return@LaunchedEffect
                }
                val items = currentItems()
                val targetIndex = intent.key
                    ?.let { key -> items.indexOfFirst { it.containsNodeKey(key) } }
                    ?.takeIf { it >= 0 }
                    ?: intent.turnId
                        ?.takeIf { it.isNotBlank() }
                        ?.let { turnId ->
                            items.indexOfFirst { it is AssistantTurnTimelineItem && it.turnId == turnId }
                        }
                        ?.takeIf { it >= 0 }
                    ?: intent.index.takeIf { it >= 0 }
                    ?: run {
                        clearPending()
                        return@LaunchedEffect
                    }
                animateTimelineItemToAnchor(
                    listState = listState,
                    itemIndex = targetIndex,
                    anchorTopPx = anchorTopOffsetPx,
                    tolerancePx = followCorrectionTolerancePx,
                )
                scrollTimelineItemToAnchorIfNeeded(
                    listState = listState,
                    itemIndex = targetIndex,
                    anchorTopPx = anchorTopOffsetPx,
                    tolerancePx = followCorrectionTolerancePx,
                    settleFrames = 5,
                )
                if (intent.key != null) {
                    if (
                        scrollMeasuredNodeToAnchorIfNeeded(
                            nodeKey = intent.key,
                            waitFrames = FinalDecisionAnchorWaitFrames,
                            settleFrames = 4,
                        )
                    ) {
                        lastFocusedFinalDecisionKey = intent.key
                    }
                    clearPending()
                    return@LaunchedEffect
                }
                lastFocusedFinalDecisionKey = intent.key ?: latestLatestFinalDecisionKey
                clearPending()
            }
            TimelineScrollIntentKind.KeyboardChanged -> {
                if (intent.deltaPx > 0) {
                    withFrameNanos { }
                    scrollReadableTurnAnchorIfNeeded(
                        listState = listState,
                        timelineItems = currentItems(),
                        currentTurnId = latestCurrentTurnId,
                        activeUserBubbleAnchorKey = latestActiveUserBubbleFollowAnchorKey,
                        anchorTopPx = anchorTopOffsetPx,
                        tolerancePx = followCorrectionTolerancePx,
                    )
                }
                clearPending()
            }
            TimelineScrollIntentKind.FollowReadableTurn -> {
                val anchored = scrollReadableTurnAnchorIfNeeded(
                    listState = listState,
                    timelineItems = currentItems(),
                    currentTurnId = latestCurrentTurnId,
                    activeUserBubbleAnchorKey = latestActiveUserBubbleFollowAnchorKey,
                    anchorTopPx = anchorTopOffsetPx,
                    tolerancePx = followCorrectionTolerancePx,
                    allowOffscreenAnchor = intent.allowOffscreenAnchor,
                )
                if (!anchored) {
                    clearPending()
                    return@LaunchedEffect
                }
                clearPending()
            }
        }
    }

    CompositionLocalProvider(LocalThinkingMascotComposition provides thinkingMascotComposition) {
        Box(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    timelineRootTopPx = coordinates.positionInRoot().y.roundToInt()
                },
        ) {
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
                                revealedMessageKeys = timelineRevealedMessageKeys,
                                liveRevealedMessageKeys = timelineLiveRevealedMessageKeys,
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
                                onStructuredEntered = ::markStructuredEntered,
                                onNodePositioned = ::recordMeasuredNodeTop,
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
                            revealedMessageKeys = timelineRevealedMessageKeys,
                            liveRevealedMessageKeys = timelineLiveRevealedMessageKeys,
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
                            onStreamingTextProgress = { requestCurrentTurnRevealFollowScroll() },
                            onTextCompleted = revealStore::markTextCompleted,
                            onTextRevealProgress = revealStore::updateTextProgress,
                            onStructuredStarted = revealStore::markStructuredNodeStarted,
                            onStructuredEntered = ::markStructuredEntered,
                            onNodePositioned = ::recordMeasuredNodeTop,
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
                            revealedMessageKeys = timelineRevealedMessageKeys,
                            liveRevealedMessageKeys = timelineLiveRevealedMessageKeys,
                            textRevealProgress = item.node.revealTextKey()?.let { revealStore.textRevealProgress(it) },
                            textCompleted = item.node.revealTextKey()?.let {
                                revealStore.hasCompletedText(it) || it in timelineRevealedMessageKeys
                            } == true,
                            textLiveRevealed = item.node.revealTextKey()?.let {
                                revealStore.hasLiveRevealedText(it) ||
                                    it in timelineLiveRevealedMessageKeys ||
                                    it in timelineRevealedMessageKeys
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
                            onStreamingTextProgress = { requestCurrentTurnRevealFollowScroll() },
                            onTextRevealProgress = { key, visible, total ->
                                revealStore.updateTextProgress(key, visible, total)
                            },
                            onStructuredEntered = ::markStructuredEntered,
                            onNodePositioned = ::recordMeasuredNodeTop,
                            onUserBubblePositioned = onUserBubblePositioned,
                            onClarificationCardDismissed = onClarificationCardDismissed,
                        )
                    }
                }
            }

            if (state.lastError != null && !hasTimelineError) {
                item("last_error") {
                    Box {
                        InlineSystemNotice(MinimalErrorText)
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
            visible = shouldShowTimelineReadableTurnBubble(
                isComposerFocused = isComposerFocused,
                keyboardVisible = stableImeBottomPx > 0,
                isNearTimelineEnd = isNearTimelineEnd,
                userDetachedFromLatest = userDetachedFromLatest,
                currentTurnFollowActive = currentTurnFollowActive,
            ),
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
            FollowReadableTurnBubble(
                onClick = {
                    manualScrollSettling = false
                    userDetachedFromLatest = false
                    followCurrentTurnDuringReveal = state.isStreaming || isAssistantVisualActive
                    lastRevealScrollAtMs = 0L
                    viewportController.requestReadableTurnFollow(
                        allowOffscreenAnchor = true,
                        isRouteReturnSuppressed = suppressReturnAutoFocus,
                        isClarificationFlightActive = isClarificationFlightActive,
                    )
                },
            )
        }
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
    liveRevealedMessageKeys: Set<String>,
    dismissingClarificationKey: String?,
    dismissedClarificationKeys: Set<String>,
    selectedClarificationOption: String?,
    onClarificationOption: (String, ClarificationChipSnapshot?) -> Unit,
    onClarificationManualInput: () -> Unit,
    onClarificationManualSource: (String, ClarificationChipSnapshot) -> Unit,
    onCriteriaEdit: (CriteriaCardPayload) -> Unit,
    onProductOpen: (String, String?, String?) -> Unit,
    onProductDetailOpen: (String, String, String?, String?) -> Unit,
    onConvergeRecommendation: (String) -> Unit,
    onDecisionEvidence: (FinalDecisionPayload, String?) -> Unit,
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
    onNodePositioned: (String, Int) -> Unit,
    onUserBubblePositioned: (String, ClarificationChipSnapshot) -> Unit,
    onClarificationCardDismissed: (String) -> Unit,
) {
    val turnSettled = renderContext.currentTurnId == null || item.turnId != renderContext.currentTurnId
    val coordinator = rememberTurnStreamingCoordinator(
        item = item,
        revealStore = revealStore,
        turnSettled = turnSettled,
        revealedMessageKeys = revealedMessageKeys,
        liveRevealedMessageKeys = liveRevealedMessageKeys,
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
    val thinkingFirstSeenAtMs = remember(item.key) { mutableMapOf<String, Long>() }
    val currentThinkingKeys = remember(item.nodes) {
        item.nodes
            .filterIsInstance<ThinkingNode>()
            .mapTo(mutableSetOf()) { it.key }
    }
    val nowMs = SystemClock.uptimeMillis()
    currentThinkingKeys.forEach { key ->
        thinkingFirstSeenAtMs.putIfAbsent(key, nowMs)
    }
    thinkingFirstSeenAtMs.keys.retainAll(currentThinkingKeys)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        item.nodes.forEachIndexed { index, node ->
            val nodeVisible = node.key in coordinator.visibleNodeKeys
            if (!nodeVisible && node !is ThinkingNode) return@forEachIndexed
            if (node is ThinkingNode) {
                if (!item.nodes.shouldRenderThinkingNodeInOwnSlot(index)) {
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
                                animationStartedAtMs = thinkingFirstSeenAtMs[node.key],
                            )
                        }
                    }
                }
                return@forEachIndexed
            }
            val precedingThinking = item.nodes.getOrNull(index - 1)
                ?.takeIf { it is ThinkingNode } as? ThinkingNode
            val precedingThinkingMessage = precedingThinking
                ?.payload
                ?.userFacingThinkingMessage()
                .orEmpty()
            val revealTextKey = node.revealTextKey()
            val rawShouldRunThinkingHandoff = !turnSettled &&
                precedingThinking != null &&
                precedingThinkingMessage.isNotBlank() &&
                (
                    revealTextKey?.let { it in coordinator.textHandoffKeys } == true ||
                        node.key in coordinator.structuredHandoffKeys
                    )
            val handoffInitialDelayMs = remember(rawShouldRunThinkingHandoff, node.key, precedingThinking?.key) {
                if (!rawShouldRunThinkingHandoff) return@remember 0L
                val firstSeenAt = precedingThinking
                    ?.key
                    ?.let { thinkingFirstSeenAtMs[it] }
                    ?: SystemClock.uptimeMillis()
                remainingThinkingVisibilityDelayMs(
                    firstSeenAtMs = firstSeenAt,
                    nowMs = SystemClock.uptimeMillis(),
                )
            }
            val shouldRunThinkingHandoff = shouldRunThinkingTextHandoff(
                requested = rawShouldRunThinkingHandoff,
                remainingVisibilityDelayMs = handoffInitialDelayMs,
            )
            var handoffPhase by remember(node.key, revealTextKey, precedingThinking?.key) {
                mutableStateOf(
                    if (shouldRunThinkingHandoff) {
                        ThinkingTextHandoffPhase.Thinking
                    } else {
                        ThinkingTextHandoffPhase.Content
                    },
                )
            }
            LaunchedEffect(shouldRunThinkingHandoff, node.key, revealTextKey, precedingThinking?.key) {
                if (shouldRunThinkingHandoff) {
                    handoffPhase = ThinkingTextHandoffPhase.Thinking
                    kotlinx.coroutines.delay(ThinkingTextHandoffHoldMs)
                    handoffPhase = ThinkingTextHandoffPhase.Exiting
                    kotlinx.coroutines.delay(ThinkingTextHandoffExitMs)
                    handoffPhase = ThinkingTextHandoffPhase.Content
                } else {
                    handoffPhase = ThinkingTextHandoffPhase.Content
                }
            }
            val nodeSlotKey = precedingThinking
                ?.key
                ?.takeIf { precedingThinkingMessage.isNotBlank() }
                ?: node.key
            key(nodeSlotKey) {
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
                        if (shouldRunThinkingHandoff && handoffPhase != ThinkingTextHandoffPhase.Content) {
                            AnimatedVisibility(
                                visible = handoffPhase == ThinkingTextHandoffPhase.Thinking,
                                enter = fadeIn(
                                    animationSpec = tween(
                                        durationMillis = 0,
                                        easing = MenuEaseOut,
                                    ),
                                ),
                                exit = fadeOut(
                                    animationSpec = tween(
                                        durationMillis = ThinkingTextHandoffExitMs.toInt(),
                                        easing = MenuEaseIn,
                                    ),
                                ),
                            ) {
                                AssistantInlineStatus(
                                    message = precedingThinkingMessage,
                                    motionEnabled = renderContext.currentTurnId == precedingThinking?.turnId,
                                    animationStartedAtMs = precedingThinking
                                        ?.key
                                        ?.let { thinkingFirstSeenAtMs[it] },
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
                                liveRevealedMessageKeys = liveRevealedMessageKeys,
                                textRevealProgress = node.revealTextKey()?.let { revealStore.textRevealProgress(it) },
                                textCompleted = node.revealTextKey()?.let {
                                    turnSettled || revealStore.hasCompletedText(it) || it in revealedMessageKeys
                                } == true,
                                textLiveRevealed = node.revealTextKey()?.let {
                                    revealStore.hasLiveRevealedText(it) ||
                                        it in liveRevealedMessageKeys ||
                                        it in revealedMessageKeys
                                } == true,
                                delayTextReveal = false,
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
                                onNodePositioned = onNodePositioned,
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

@Composable
private fun ThinkingNodeVisibility(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    var delayedVisible by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible && !delayedVisible) {
            kotlinx.coroutines.delay(ThinkingVisibilityDelayMs)
            delayedVisible = true
        } else if (!visible) {
            delayedVisible = false
        }
    }
    AnimatedVisibility(
        visible = visible && delayedVisible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = ThinkingTextHandoffEnterMs, easing = MenuEaseOut),
        ),
        exit = fadeOut(
            animationSpec = tween(durationMillis = ThinkingTextHandoffExitMs.toInt(), easing = MenuEaseIn),
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
    liveRevealedMessageKeys: Set<String>,
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
    val textRevealProgressByKey = item.nodes.textRevealProgressForVisibility(
        revealStore = revealStore,
        liveRevealedMessageKeys = liveRevealedMessageKeys,
    )
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
        structuredHandoffKeys = visibilityState.structuredHandoffKeys,
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

internal fun shouldCompactProductDeckHistory(
    node: ProductDeckNode,
    deckConverged: Boolean,
    isStreaming: Boolean,
    currentTurnId: String?,
    latestProductDeckKey: String?,
): Boolean {
    // Keep deck height stable while a new reply is still laying out. Once the
    // turn settles, old or converged decks can return to the compact history
    // thumbnail design without reintroducing timeline drift.
    if (isStreaming && currentTurnId != null) return false
    return deckConverged || (latestProductDeckKey != null && latestProductDeckKey != node.key)
}

internal fun TimelineRenderItem.structureSignature(): String =
    when (this) {
        is UserTimelineItem -> key
        is StandaloneTimelineItem -> "${key}:${node.structureSignaturePart()}"
        is AssistantTurnTimelineItem -> buildString {
            append(key)
            append(':')
            nodes.joinTo(this, separator = ",") { node ->
                node.structureSignaturePart()
            }
        }
    }

internal fun List<TimelineRenderItem>.scrollAnchorSignature(): String =
    joinToString(separator = "|") { item ->
        when (item) {
            is UserTimelineItem -> item.key
            is StandaloneTimelineItem -> item.node.scrollAnchorSignaturePart()
            is AssistantTurnTimelineItem -> buildString {
                append(item.key)
                append(':')
                item.nodes.joinTo(this, separator = ",") { node ->
                    node.scrollAnchorSignaturePart()
                }
            }
        }
    }

private fun ChatUiNode.scrollAnchorSignaturePart(): String =
    listOfNotNull(
        key,
        revealTextKey(),
    ).joinToString(separator = "/")

private fun ChatUiNode.structureSignaturePart(): String =
    when (this) {
        is AiStreamNode -> "${key}/${revealTextKey().orEmpty()}"
        is ThinkingNode -> key
        is ClarificationNode -> {
            val optionSignature = payload.suggestedOptions.joinToString(separator = "+")
            "$key/${revealTextKey().orEmpty()}/$optionSignature"
        }
        is CriteriaNode -> {
            val criteria = payload.criteria
            "$key/${criteria.criteriaId}/${criteria.category}/${criteria.budgetMin.orEmptySignature()}-${criteria.budgetMax.orEmptySignature()}/${criteria.chips.size}"
        }
        is ProductDeckNode -> {
            val productSignature = products.joinToString(separator = "+") { payload ->
                payload.product.productId.ifBlank { payload.product.name }
            }
            "$key/$deckId/$productSignature"
        }
        is FinalDecisionNode -> {
            val winner = payload.winnerProductId.orEmpty()
            val actionSignature = payload.nextActions.joinToString(separator = "+") { action ->
                action.action.ifBlank { action.actionId }
            }
            val contentSignature = listOf(
                payload.summary.length,
                payload.why.sumOf { it.length },
                payload.notFor.sumOf { it.length },
                payload.alternatives.size,
                payload.confidence.orEmpty().length,
                payload.nextStep.orEmpty().length,
            ).joinToString(separator = "/")
            "$key/${deckId.orEmpty()}/$winner/$actionSignature/$contentSignature"
        }
        is CartActionNode -> {
            val cartItems = payload.cart?.totalItems ?: 0
            "$key/${payload.action}/${payload.status}/${payload.productId.orEmpty()}/${payload.quantity}/$cartItems"
        }
        is ErrorNode -> "$key/$code/$retryable/${message.length}"
        is UserMessageNode -> key
    }

private fun Double?.orEmptySignature(): String =
    this?.toString().orEmpty()

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
    val shouldAnimate = shouldAnimateTurnNode(
        motionEnabled = motionEnabled,
        hasStarted = hasStarted,
    )
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
    liveRevealedMessageKeys: Set<String>,
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
    onProductOpen: (String, String?, String?) -> Unit,
    onProductDetailOpen: (String, String, String?, String?) -> Unit,
    onConvergeRecommendation: (String) -> Unit,
    onDecisionEvidence: (FinalDecisionPayload, String?) -> Unit,
    onRetryLastMessage: () -> Unit,
    onEditLastMessage: (String) -> Unit,
    onQuickAction: (QuickActionPayload) -> Unit,
    onUserImagePreview: (String) -> Unit,
    onMessageRevealComplete: (String) -> Unit,
    onMessageRevealActiveChange: (String, Boolean) -> Unit,
    onStreamingTextProgress: () -> Unit,
    onTextRevealProgress: (String, Int, Int) -> Unit,
    onStructuredEntered: (String) -> Unit,
    onNodePositioned: (String, Int) -> Unit,
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
            animateInitialCompleted = shouldAnimateInitialCompletedText(
                turnId = node.turnId,
                currentTurnId = renderContext.currentTurnId,
                revealKey = node.key,
                revealedMessageKeys = revealedMessageKeys,
                liveRevealedMessageKeys = liveRevealedMessageKeys,
            ),
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
            animateQuestion = shouldAnimateInitialCompletedText(
                turnId = node.turnId,
                currentTurnId = renderContext.currentTurnId,
                revealKey = node.clarificationQuestionRevealKey(),
                revealedMessageKeys = revealedMessageKeys,
                liveRevealedMessageKeys = liveRevealedMessageKeys,
            ),
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
        is ProductDeckNode -> Box(
            modifier = Modifier.onGloballyPositioned { coordinates ->
                onNodePositioned(node.key, coordinates.positionInRoot().y.roundToInt())
            },
        ) {
            val deckIsLatest = node.key == renderContext.latestProductDeckKey
            val deckConverged = node.key in renderContext.convergedProductDeckKeys
            ProductRecommendationStrip(
                node = node,
                backendBaseUrl = renderContext.backendBaseUrl,
                swipeState = renderContext.productSwipeStates[node.deckId].takeIf { deckIsLatest },
                awaitingConvergence = deckIsLatest &&
                    node.deckId == renderContext.latestConvergeableDeckId &&
                    node.deckId in renderContext.awaitingConvergenceDeckIds,
                hasPendingDecision = deckIsLatest && node.deckId in renderContext.pendingDecisions,
                deckConverged = deckConverged,
                deckStillStreaming = renderContext.isStreaming && renderContext.currentTurnId == node.turnId,
                convergeActionReady = productConvergeActionReady,
                compactHistory = shouldCompactProductDeckHistory(
                    node = node,
                    deckConverged = deckConverged,
                    isStreaming = renderContext.isStreaming,
                    currentTurnId = renderContext.currentTurnId,
                    latestProductDeckKey = renderContext.latestProductDeckKey,
                ),
                motionEnabled = structuredMotionEnabled,
                alreadyEntered = structuredAlreadyEntered,
                onEntered = { onStructuredEntered(node.key) },
                onOpen = { deckId, productId -> onProductOpen(deckId, productId, node.key) },
                onOpenDetail = { deckId, productId -> onProductDetailOpen(deckId, productId, node.key, node.key) },
                onConverge = { onConvergeRecommendation(node.deckId) },
            )
        }
        is FinalDecisionNode -> Box(
            modifier = Modifier.onGloballyPositioned { coordinates ->
                onNodePositioned(node.key, coordinates.positionInRoot().y.roundToInt())
            },
        ) {
            val sourceDeckNodeKey = renderContext.finalDecisionSourceDeckKeyByDecisionKey[node.key]
            val sourceDeck = sourceDeckNodeKey?.let { renderContext.productDeckNodeByKey[it] }
            val decisionProductsById = sourceDeck?.productsByProductId() ?: renderContext.productsById
            val decisionProductDeckIdByProductId = sourceDeck?.deckIdsByProductId()
                ?: renderContext.productDeckIdByProductId
            DecisionSummaryCard(
                motionKey = node.key,
                payload = node.payload,
                productsById = decisionProductsById,
                productDeckIdByProductId = decisionProductDeckIdByProductId,
                sourceDeckNodeKey = sourceDeckNodeKey,
                cartState = renderContext.cartState,
                backendBaseUrl = renderContext.backendBaseUrl,
                motionEnabled = structuredMotionEnabled,
                alreadyEntered = structuredAlreadyEntered,
                onEntered = { onStructuredEntered(node.key) },
                onEvidence = { onDecisionEvidence(node.payload, node.key) },
                onProductDetailOpen = { deckId, productId, deckNodeKey ->
                    onProductDetailOpen(deckId, productId, node.key, deckNodeKey)
                },
                onQuickAction = onQuickAction,
            )
        }
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
        )
    }
}

@Composable
private fun FollowReadableTurnBubble(
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
    val shouldAnimate = shouldAnimateTimelineItem(
        animateEnter = animateEnter,
        hasEntered = hasEntered,
    )
    val latestOnEntered by rememberUpdatedState(onEntered)

    if (!shouldAnimate) {
        LaunchedEffect(Unit) {
            if (!hasEntered) latestOnEntered()
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
    animationStartedAtMs: Long? = null,
) {
    val displayMessage = message.withoutTrailingDots().takeIf { it.isNotBlank() } ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThinkingMascotAnimation(
            modifier = Modifier.size(28.dp),
            motionEnabled = motionEnabled,
            animationStartedAtMs = animationStartedAtMs,
        )
        Spacer(Modifier.width(8.dp))
        ThinkingShimmerText(
            text = displayMessage,
            motionEnabled = motionEnabled,
        )
    }
}

@Composable
private fun ThinkingBubble(
    message: String,
    motionEnabled: Boolean = true,
    animationStartedAtMs: Long? = null,
) {
    val displayMessage = message.withoutTrailingDots().takeIf { it.isNotBlank() } ?: return

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThinkingMascotAnimation(
            motionEnabled = motionEnabled,
            animationStartedAtMs = animationStartedAtMs,
        )
        Spacer(Modifier.width(10.dp))
        ThinkingShimmerText(
            text = displayMessage,
            motionEnabled = motionEnabled,
        )
    }
}

@Composable
private fun ThinkingMascotAnimation(
    modifier: Modifier = Modifier.size(32.dp),
    motionEnabled: Boolean = true,
    animationStartedAtMs: Long? = null,
) {
    val mascotModifier = modifier

    if (!motionEnabled) {
        Image(
            painter = painterResource(R.drawable.redbean_bun_mascot_white),
            contentDescription = "BuyPilot mascot",
            modifier = mascotModifier,
            contentScale = ContentScale.Fit,
        )
        return
    }

    val composition = LocalThinkingMascotComposition.current
    val progress = rememberRestartingLoopProgress(
        durationMs = ThinkingMascotLoopDurationMs,
        enabled = true,
        startedAtMs = animationStartedAtMs,
    )

    if (composition == null) {
        Image(
            painter = painterResource(R.drawable.redbean_bun_mascot_white),
            contentDescription = "BuyPilot mascot",
            modifier = mascotModifier,
            contentScale = ContentScale.Fit,
        )
        return
    }

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = mascotModifier,
    )
}

@Composable
private fun rememberRestartingLoopProgress(
    durationMs: Long,
    enabled: Boolean,
    startedAtMs: Long? = null,
): Float {
    var progress by remember(startedAtMs) { mutableFloatStateOf(0f) }
    var localStartedAtMs by remember(startedAtMs) { mutableStateOf(startedAtMs) }
    LaunchedEffect(durationMs, enabled, startedAtMs) {
        if (!enabled || durationMs <= 0L) {
            progress = 0f
            return@LaunchedEffect
        }
        localStartedAtMs = startedAtMs
        while (true) {
            withFrameNanos {
                val nowMs = SystemClock.uptimeMillis()
                val startedAt = localStartedAtMs ?: nowMs.also { localStartedAtMs = it }
                val elapsedMs = (nowMs - startedAt).coerceAtLeast(0L)
                progress = (elapsedMs % durationMs).toFloat() / durationMs.toFloat()
            }
        }
    }
    return progress
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
            animation = tween(durationMillis = ThinkingShimmerDurationMs),
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
                        shimmerColor.copy(alpha = 0.52f),
                        shimmerColor.copy(alpha = 0.94f),
                        shimmerColor.copy(alpha = 0.52f),
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
    nearEndReferenceIndex: Int,
    bottomPaddingPx: Float,
    thresholdPx: Float,
): Boolean {
    if (nearEndReferenceIndex < 0) return true

    val layoutInfo = listState.layoutInfo
    val viewportBottom = layoutInfo.viewportEndOffset - bottomPaddingPx.roundToInt()
    val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull { it.index == nearEndReferenceIndex }
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

private suspend fun scrollReadableTurnAnchorIfNeeded(
    listState: LazyListState,
    timelineItems: List<TimelineRenderItem>,
    currentTurnId: String?,
    activeUserBubbleAnchorKey: String?,
    anchorTopPx: Int,
    tolerancePx: Float,
    allowOffscreenAnchor: Boolean = false,
): Boolean {
    val targetIndex = readableTurnAnchorIndexForFollow(
        timelineItems = timelineItems,
        currentTurnId = currentTurnId,
        activeUserBubbleAnchorKey = activeUserBubbleAnchorKey,
    ).takeIf { it >= 0 } ?: return false

    var visible = false
    repeat(2) {
        val layoutInfo = listState.layoutInfo
        val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
        if (item == null) {
            if (allowOffscreenAnchor) {
                animateTimelineItemToAnchor(
                    listState = listState,
                    itemIndex = targetIndex,
                    anchorTopPx = anchorTopPx,
                    tolerancePx = tolerancePx,
                )
                return true
            }
            return false
        }
        visible = true
        val desiredTop = layoutInfo.viewportStartOffset + anchorTopPx
        val correction = (item.offset - desiredTop).toFloat()
        if (abs(correction) > tolerancePx) {
            listState.scrollBy(correction)
        }
        kotlinx.coroutines.delay(16L)
    }
    return visible
}

internal fun currentAssistantTurnIndexForReadableFollow(
    timelineItems: List<TimelineRenderItem>,
    currentTurnId: String?,
): Int {
    val turnId = currentTurnId?.takeIf { it.isNotBlank() } ?: return -1
    return timelineItems.indexOfFirst { item ->
        item is AssistantTurnTimelineItem && item.turnId == turnId
    }
}

internal fun readableTurnAnchorIndexForFollow(
    timelineItems: List<TimelineRenderItem>,
    currentTurnId: String?,
    activeUserBubbleAnchorKey: String?,
): Int {
    val assistantIndex = currentAssistantTurnIndexForReadableFollow(
        timelineItems = timelineItems,
        currentTurnId = currentTurnId,
    ).takeIf { it >= 0 } ?: return -1
    val userAnchorIndex = activeUserBubbleAnchorKey
        ?.takeIf { it.isNotBlank() }
        ?.let { key ->
            timelineItems.indexOfFirst { item ->
                item is UserTimelineItem && item.node.key == key
            }
        }
        ?: -1
    return if (userAnchorIndex >= 0 && assistantIndex > userAnchorIndex) {
        userAnchorIndex
    } else {
        assistantIndex
    }
}

internal fun remainingThinkingVisibilityDelayMs(
    firstSeenAtMs: Long,
    nowMs: Long,
    visibilityDelayMs: Long = ThinkingVisibilityDelayMs,
): Long {
    val elapsedMs = (nowMs - firstSeenAtMs).coerceAtLeast(0L)
    return (visibilityDelayMs - elapsedMs).coerceAtLeast(0L)
}

internal fun shouldRunThinkingTextHandoff(
    requested: Boolean,
    remainingVisibilityDelayMs: Long,
): Boolean = requested && remainingVisibilityDelayMs <= 0L

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
            kotlinx.coroutines.delay(ClarificationCardSequenceReleaseMs)
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
    val lineColor = if (hasSelection) {
        BuyPilotColors.Primary.copy(alpha = 0.6f)
    } else {
        BuyPilotColors.ProductSelectionLine
    }
    val lineWidth = with(LocalDensity.current) { 3.dp.toPx() }
    val lineRadius = with(LocalDensity.current) { 2.dp.toPx() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .drawBehind {
                drawRoundRect(
                    color = lineColor,
                    topLeft = Offset.Zero,
                    size = Size(width = lineWidth, height = size.height),
                    cornerRadius = CornerRadius(lineRadius, lineRadius),
                )
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 17.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (options.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    options.forEach { label ->
                        key(label) {
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
