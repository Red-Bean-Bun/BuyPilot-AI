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
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.buypilot.core.model.CompareCardPayload
import com.buypilot.core.model.CriteriaCardPayload
import com.buypilot.core.model.EvidencePayload
import com.buypilot.core.model.FinalDecisionPayload
import com.buypilot.core.model.ProductCardPayload
import com.buypilot.core.model.ProductPayload
import com.buypilot.core.model.QuickActionPayload
import com.buypilot.core.model.ThinkingPayload
import com.buypilot.core.model.responses.FaqItem
import com.buypilot.core.model.responses.ProductDetailResponse
import com.buypilot.core.model.responses.ReviewItem
import com.buypilot.feature.chat.R
import com.buypilot.feature.chat.model.AiStreamNode
import com.buypilot.feature.chat.model.CartActionNode
import com.buypilot.feature.chat.model.ChatUiNode
import com.buypilot.feature.chat.model.ClarificationNode
import com.buypilot.feature.chat.model.CompareCardNode
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
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

internal val MenuEaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
internal val MenuEaseIn = CubicBezierEasing(0.3f, 0f, 1f, 1f)
internal val PremiumRevealEase = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val ClarificationEaseOut = CubicBezierEasing(0.1f, 1f, 0.1f, 1f)
private val ClarificationFlightEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
private val ProductDetailRevealDistance = 220.dp
private const val ProductDetailSnapThreshold = 0.36f
private const val ProductDetailEnterMs = 560
private const val CriteriaCardEnterMs = 560
private const val DecisionCardEnterMs = 760
private const val DecisionReasonBaseDelayMs = 240
private const val DecisionReasonStaggerMs = 120
private const val MaxCompareSelectionCount = 4
private val ProductDeckFooterHeight = 46.dp

private data class PendingClarificationAnswer(
    val nodeKey: String,
    val message: String,
    val selectedOption: String? = null,
    val awaitsFlight: Boolean = false,
    val previousUserMessageKey: String? = null,
    val flightId: Int? = null,
)

internal data class ClarificationChipSnapshot(
    val position: Offset,
    val size: Size,
)

private data class ClarificationManualSource(
    val nodeKey: String,
    val snapshot: ClarificationChipSnapshot,
)

internal fun LayoutCoordinates.toClarificationSnapshot(): ClarificationChipSnapshot {
    val bounds = boundsInRoot()
    return ClarificationChipSnapshot(
        position = Offset(bounds.left, bounds.top),
        size = Size(bounds.width, bounds.height),
    )
}

private data class ClarificationFlight(
    val id: Int,
    val nodeKey: String,
    val option: String,
    val message: String,
    val startPosition: Offset,
    val startSize: Size,
    val previousUserMessageKey: String?,
    val fromKeyboard: Boolean = false,
    val targetMessageKey: String? = null,
    val messageSent: Boolean = false,
)

private data class DecisionStatusBadge(
    val label: String,
    val accent: Color,
    val surface: Color,
    val showCardWhenEmpty: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BuyPilotChatScreen(
    state: ChatUiState,
    timelinePresentation: TimelinePresentationState,
    onInputChanged: (String, Boolean) -> Unit,
    onSendMessage: (String, String?) -> Unit,
    onImageSelected: (Uri) -> Unit,
    onImageCaptured: (Bitmap) -> Unit,
    onClearImageAttachment: () -> Unit,
    onCriteriaPatch: (JsonObject) -> Unit,
    onCancel: () -> Unit,
    onQuickAction: (QuickActionPayload) -> Unit,
    onCompareProducts: (List<Int>, List<String>?) -> Unit,
    onCartOpen: () -> Unit,
    onCartSheetRequestHandled: (Long) -> Unit,
    onCartQuantityChange: (String, Int) -> Unit,
    onOpenProductDeck: (String, String?, String?) -> Unit,
    onOpenProductDetail: (String, String, String?) -> Unit,
    onRetryLastMessage: () -> Unit,
    onEditLastMessage: (String) -> Unit,
    onClearConversation: () -> Unit,
    onConvergeProductDeck: (String) -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    var showAttachmentMenu by rememberSaveable { mutableStateOf(false) }
    var imagePreviewAttachment by remember { mutableStateOf<ChatImageAttachmentState?>(null) }
    var sheetContent by rememberSaveable(stateSaver = ChatSheetContentSaver) {
        mutableStateOf<ChatSheetContent?>(null)
    }
    var timelineExternalRouteFreezeRequestId by remember { mutableIntStateOf(0) }
    var pendingProductDetailAfterTimelineFreeze by remember {
        mutableStateOf<PendingProductDetailRoute?>(null)
    }
    var sheetExiting by remember { mutableStateOf(false) }
    var sheetTransitionId by remember { mutableStateOf(0) }
    var dismissedClarificationKeys by rememberSaveable(stateSaver = StringSetSaver) {
        mutableStateOf(emptySet<String>())
    }
    var dismissingClarificationKey by remember { mutableStateOf<String?>(null) }
    var revealedMessageKeyList by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val revealedMessageKeys = remember(revealedMessageKeyList) { revealedMessageKeyList.toSet() }
    var liveRevealedMessageKeys by rememberSaveable(stateSaver = StringSetSaver) {
        mutableStateOf(emptySet<String>())
    }
    var typingMessageKeys by rememberSaveable(stateSaver = StringSetSaver) {
        mutableStateOf(emptySet<String>())
    }
    var timelineInstanceId by rememberSaveable { mutableIntStateOf(0) }
    var visualActiveTurnKeys by remember { mutableStateOf(emptySet<String>()) }
    var pendingClarificationAnswer by remember { mutableStateOf<PendingClarificationAnswer?>(null) }
    var activeClarificationFlight by remember { mutableStateOf<ClarificationFlight?>(null) }
    var hiddenFlightMessageKeys by remember { mutableStateOf(emptySet<String>()) }
    var userBubbleSnapshots by remember { mutableStateOf(emptyMap<String, ClarificationChipSnapshot>()) }
    var clarificationManualSource by remember { mutableStateOf<ClarificationManualSource?>(null) }
    var keyboardFlightSnapshot by remember { mutableStateOf<ClarificationChipSnapshot?>(null) }
    var compareDetailPayload by remember { mutableStateOf<CompareCardPayload?>(null) }
    var pendingInlineCompareDeckId by rememberSaveable { mutableStateOf<String?>(null) }
    var inlineCompareTurnIds by rememberSaveable(stateSaver = StringSetSaver) {
        mutableStateOf(emptySet<String>())
    }
    var inlineComparePayloadByDeckId by remember { mutableStateOf(emptyMap<String, CompareCardPayload>()) }
    var flightSequence by remember { mutableStateOf(0) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var timelineTopPx by remember { mutableStateOf(0f) }
    var composerHeightPx by remember { mutableIntStateOf(0) }
    var composerFocused by remember { mutableStateOf(false) }
    var welcomePromptDismissed by rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val keyboardVisible = imeBottomPx > 0
    val stableTimelineImeBottomPx = imeBottomPx
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current
    val composerFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            onImageSelected(uri)
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
    ) { bitmap ->
        if (bitmap != null) {
            onImageCaptured(bitmap)
        }
    }
    val defaultSkinTypeOptions = stringArrayResource(R.array.default_skin_type_options).toSet()
    val showWelcome = !timelinePresentation.hasNodes && input.isBlank()
    val shouldDismissWelcomeContent = showWelcome &&
        (showAttachmentMenu || keyboardVisible)
    val welcomeContentVisible = showWelcome &&
        !welcomePromptDismissed &&
        !showAttachmentMenu &&
        !keyboardVisible
    val activeClarificationKey = timelinePresentation.clarificationKeys
        .lastOrNull { it !in dismissedClarificationKeys }
    val timelineItemKeySignature = remember(timelinePresentation.items) {
        timelinePresentation.items.joinToString(separator = "|") { it.key }
    }
    val timelineItemKeys = remember(timelineItemKeySignature) {
        timelinePresentation.items.mapTo(mutableSetOf()) { it.key }
    }
    val pendingFlightMessageKey = pendingClarificationAnswer?.let { pending ->
        state.lastUserMessageKey?.takeIf { key ->
            pending.awaitsFlight && key != pending.previousUserMessageKey
        }
    }
    val activeFlightMessageKey = activeClarificationFlight?.let { flight ->
        state.lastUserMessageKey?.takeIf { key ->
            flight.messageSent && key != flight.previousUserMessageKey
        }
    }
    val hiddenUserMessageKeysForFlight = hiddenFlightMessageKeys +
        listOfNotNull(pendingFlightMessageKey, activeFlightMessageKey)
    val assistantVisualActive = typingMessageKeys.isNotEmpty() || visualActiveTurnKeys.isNotEmpty()
    val clarificationFlightActive = dismissingClarificationKey != null ||
        pendingClarificationAnswer?.awaitsFlight == true ||
        activeClarificationFlight != null

    LaunchedEffect(shouldDismissWelcomeContent) {
        if (shouldDismissWelcomeContent) {
            welcomePromptDismissed = true
        }
    }
    LaunchedEffect(timelinePresentation.revealKeys, timelineItemKeySignature) {
        val currentRevealKeys = timelinePresentation.revealKeys
        if (revealedMessageKeyList.any { it !in currentRevealKeys }) {
            revealedMessageKeyList = revealedMessageKeyList.filter { it in currentRevealKeys }
        }
        if (liveRevealedMessageKeys.any { it !in currentRevealKeys }) {
            liveRevealedMessageKeys = liveRevealedMessageKeys.filterTo(mutableSetOf()) { it in currentRevealKeys }
        }
        if (typingMessageKeys.any { it !in currentRevealKeys }) {
            typingMessageKeys = typingMessageKeys.filterTo(mutableSetOf()) { it in currentRevealKeys }
        }
        if (visualActiveTurnKeys.any { it !in timelineItemKeys }) {
            visualActiveTurnKeys = visualActiveTurnKeys.filterTo(mutableSetOf()) { it in timelineItemKeys }
        }
    }

    fun focusComposer() {
        showAttachmentMenu = false
        composerFocusRequester.requestFocus()
        keyboardController?.show()
    }

    fun closeTransientComposerUi() {
        showAttachmentMenu = false
    }

    fun openSheet(content: ChatSheetContent) {
        sheetTransitionId += 1
        sheetExiting = false
        sheetContent = content
    }

    fun dismissSheet(onDismissStarted: () -> Unit = {}) {
        if (sheetContent == null || sheetExiting) return

        sheetExiting = true
        sheetTransitionId += 1
        val transitionId = sheetTransitionId
        onDismissStarted()
        coroutineScope.launch {
            sheetState.hide()
            if (sheetTransitionId == transitionId) {
                sheetContent = null
                sheetExiting = false
            }
        }
    }

    fun openProductDetailAfterTimelineFreeze(
        deckId: String,
        productId: String,
        anchorNodeKey: String? = null,
        deckNodeKey: String? = null,
    ) {
        pendingProductDetailAfterTimelineFreeze = PendingProductDetailRoute(
            requestId = timelineExternalRouteFreezeRequestId + 1,
            deckId = deckId,
            productId = productId,
            anchorNodeKey = anchorNodeKey.orEmpty(),
            deckNodeKey = deckNodeKey.orEmpty(),
        )
        timelineExternalRouteFreezeRequestId += 1
    }

    fun finishProductDetailFreeze(requestId: Int) {
        val route = pendingProductDetailAfterTimelineFreeze
            ?.takeIf { it.requestId == requestId }
            ?: return
        pendingProductDetailAfterTimelineFreeze = null
        onOpenProductDetail(route.deckId, route.productId, route.deckNodeKey.takeIf { it.isNotBlank() })
    }

    LaunchedEffect(state.cartSheetRequestId) {
        val requestId = state.cartSheetRequestId
        if (requestId > 0L) {
            openSheet(ChatSheetContent.Cart)
            onCartSheetRequestHandled(requestId)
        }
    }

    LaunchedEffect(state.isStreaming) {
        if (state.isStreaming) {
            showAttachmentMenu = false
        }
    }

    LaunchedEffect(state.nodes, pendingInlineCompareDeckId, inlineCompareTurnIds) {
        val deckId = pendingInlineCompareDeckId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        val compareNode = state.nodes
            .filterIsInstance<CompareCardNode>()
            .lastOrNull { node ->
                node.turnId.isNotBlank() &&
                    node.turnId !in inlineCompareTurnIds &&
                    node.payload.sourceDeckId == deckId
            }
        if (compareNode != null) {
            inlineComparePayloadByDeckId = inlineComparePayloadByDeckId + (deckId to compareNode.payload)
            inlineCompareTurnIds = inlineCompareTurnIds + compareNode.turnId
            pendingInlineCompareDeckId = null
        }
    }

    fun sendAndClear(message: String, imageUrl: String? = null) {
        val next = message.trim()
        if (next.isEmpty() && imageUrl == null) return

        onSendMessage(next, imageUrl)
        input = ""
        onInputChanged("", false)
        showAttachmentMenu = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    fun dispatchQuickAction(action: QuickActionPayload) {
        action.criteriaPatch?.let {
            onCriteriaPatch(it)
            return
        }
        val latestDecision = timelinePresentation.latestFinalDecisionPayload
        when (action.action) {
            "open_evidence" -> {
                if (latestDecision != null) {
                    openSheet(
                        ChatSheetContent.DecisionEvidence(
                            payload = latestDecision,
                            sourceNodeKey = timelinePresentation.latestFinalDecisionKey.orEmpty(),
                        ),
                    )
                } else {
                    onQuickAction(action)
                }
            }
            else -> onQuickAction(action)
        }
    }

    fun editAndFocus(message: String) {
        val next = message.trim()
        if (next.isEmpty()) return

        showAttachmentMenu = false
        input = next
        onInputChanged(next, false)
        onEditLastMessage(next)
        composerFocusRequester.requestFocus()
        keyboardController?.show()
    }

    fun loadWelcomePrompt(text: String) {
        input = text
        onInputChanged(text, false)
        composerFocusRequester.requestFocus()
        keyboardController?.show()
    }

    fun clearConversationForDebug() {
        input = ""
        showAttachmentMenu = false
        sheetContent = null
        sheetExiting = false
        sheetTransitionId += 1
        pendingProductDetailAfterTimelineFreeze = null
        dismissedClarificationKeys = emptySet()
        dismissingClarificationKey = null
        welcomePromptDismissed = false
        revealedMessageKeyList = emptyList()
        liveRevealedMessageKeys = emptySet()
        typingMessageKeys = emptySet()
        timelineInstanceId += 1
        visualActiveTurnKeys = emptySet()
        pendingClarificationAnswer = null
        activeClarificationFlight = null
        hiddenFlightMessageKeys = emptySet()
        userBubbleSnapshots = emptyMap()
        clarificationManualSource = null
        keyboardFlightSnapshot = null
        compareDetailPayload = null
        pendingInlineCompareDeckId = null
        inlineCompareTurnIds = emptySet()
        inlineComparePayloadByDeckId = emptyMap()
        focusManager.clearFocus()
        keyboardController?.hide()
        onInputChanged("", false)
        onClearConversation()
    }

    fun answerClarification(
        message: String,
        selectedOption: String? = null,
        chipSnapshot: ClarificationChipSnapshot? = null,
        manualSnapshot: ClarificationChipSnapshot? = null,
    ) {
        val next = message.trim()
        if (next.isEmpty()) return

        val nodeKey = activeClarificationKey
        if (
            nodeKey != null &&
            (nodeKey == dismissingClarificationKey || pendingClarificationAnswer?.nodeKey == nodeKey)
        ) {
            return
        }
        if (nodeKey == null || nodeKey in dismissedClarificationKeys) {
            sendAndClear(next)
            return
        }

        showAttachmentMenu = false
        input = ""
        onInputChanged("", false)
        focusManager.clearFocus()
        keyboardController?.hide()
        val sourceSnapshot = chipSnapshot ?: manualSnapshot
        val shouldFly = sourceSnapshot != null
        val fromKeyboard = selectedOption == null && manualSnapshot != null
        val previousUserMessageKey = state.lastUserMessageKey
        val flightId = if (shouldFly) ++flightSequence else null
        pendingClarificationAnswer = PendingClarificationAnswer(
            nodeKey = nodeKey,
            message = next,
            selectedOption = selectedOption?.trim()?.takeIf { it.isNotEmpty() },
            awaitsFlight = shouldFly,
            previousUserMessageKey = previousUserMessageKey,
            flightId = flightId,
        )
        val snapshot = sourceSnapshot
        if (snapshot != null && flightId != null) {
            activeClarificationFlight = ClarificationFlight(
                id = flightId,
                nodeKey = nodeKey,
                option = selectedOption ?: next,
                message = next,
                startPosition = snapshot.position,
                startSize = snapshot.size,
                previousUserMessageKey = previousUserMessageKey,
                fromKeyboard = fromKeyboard,
            )
        }
        dismissingClarificationKey = nodeKey
    }

    fun finishClarificationDismissal(nodeKey: String) {
        dismissedClarificationKeys = dismissedClarificationKeys + nodeKey
        if (clarificationManualSource?.nodeKey == nodeKey) {
            clarificationManualSource = null
        }
        if (dismissingClarificationKey == nodeKey) {
            dismissingClarificationKey = null
        }
        val pending = pendingClarificationAnswer
        if (pending?.nodeKey == nodeKey) {
            if (pending.awaitsFlight) {
                val flight = activeClarificationFlight
                if (flight != null && flight.id == pending.flightId) {
                    if (!flight.messageSent) {
                        activeClarificationFlight = flight.copy(messageSent = true)
                        sendAndClear(pending.message)
                    }
                } else {
                    if (dismissingClarificationKey == nodeKey) {
                        dismissingClarificationKey = null
                    }
                    pendingClarificationAnswer = null
                    sendAndClear(pending.message)
                }
            } else {
                if (dismissingClarificationKey == nodeKey) {
                    dismissingClarificationKey = null
                }
                pendingClarificationAnswer = null
                sendAndClear(pending.message)
            }
        }
    }

    BackHandler(enabled = compareDetailPayload != null || sheetContent != null || composerFocused) {
        when {
            compareDetailPayload != null -> compareDetailPayload = null
            sheetContent != null -> dismissSheet()
            composerFocused -> focusManager.clearFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { rootSize = it }
            .background(BuyPilotColors.SurfaceBg),
    ) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                centered = timelinePresentation.hasNodes,
                showBack = timelinePresentation.hasStructuredContent,
                showClear = timelinePresentation.hasNodes,
                cartCount = state.cartState.totalItems,
                onCartOpen = {
                    onCartOpen()
                    openSheet(ChatSheetContent.Cart)
                },
                onClearConversation = ::clearConversationForDebug,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned { timelineTopPx = it.positionInRoot().y }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        focusManager.clearFocus()
                    },
            ) {
                ConversationStage(
                    showWelcome = showWelcome,
                    welcomeContentVisible = welcomeContentVisible,
                    state = state,
                    timelinePresentation = timelinePresentation,
                    timelineInstanceId = timelineInstanceId,
                    externalRouteFreezeRequestId = timelineExternalRouteFreezeRequestId,
                    externalRouteFreezeAnchorNodeKey = pendingProductDetailAfterTimelineFreeze?.anchorNodeKey,
                    inlineComparePayloadByDeckId = inlineComparePayloadByDeckId,
                    inlineCompareTurnIds = inlineCompareTurnIds,
                    onExternalRouteFreezeReady = ::finishProductDetailFreeze,
                    onClarificationOption = { label, snapshot ->
                        answerClarification(
                            label.toClarificationUserMessage(defaultSkinTypeOptions),
                            selectedOption = label,
                            chipSnapshot = snapshot,
                        )
                    },
                    onCriteriaEdit = { openSheet(ChatSheetContent.Criteria(it)) },
                    onProductOpen = onOpenProductDeck,
                    onProductDetailOpen = onOpenProductDetail,
                    onCompareProducts = onCompareProducts,
                    onInlineCompareProducts = { deckId, ranks, productIds ->
                        pendingInlineCompareDeckId = deckId
                        onCompareProducts(ranks, productIds)
                    },
                    onCompareDetailOpen = { payload -> compareDetailPayload = payload },
                    onConvergeRecommendation = onConvergeProductDeck,
                    onWelcomePromptClick = { loadWelcomePrompt(it) },
                    onDecisionEvidence = { payload, sourceNodeKey ->
                        openSheet(
                            ChatSheetContent.DecisionEvidence(
                                payload = payload,
                                sourceNodeKey = sourceNodeKey.orEmpty(),
                            ),
                        )
                    },
                    onRetryLastMessage = onRetryLastMessage,
                    onEditLastMessage = { editAndFocus(it) },
                    onQuickAction = ::dispatchQuickAction,
                    onUserImagePreview = { imageUrl ->
                        imagePreviewAttachment = ChatImageAttachmentState(
                            imageUrl = imageUrl,
                            fileName = "图片预览",
                        )
                    },
                    onClarificationManualInput = { focusComposer() },
                    onClarificationManualSource = { nodeKey, snapshot ->
                        clarificationManualSource = ClarificationManualSource(nodeKey, snapshot)
                    },
                    onTimelineDrag = { closeTransientComposerUi() },
                    composerHeightPx = composerHeightPx,
                    imeBottomPx = imeBottomPx,
                    stableImeBottomPx = stableTimelineImeBottomPx,
                    isComposerFocused = composerFocused,
                    isAssistantVisualActive = assistantVisualActive,
                    isClarificationFlightActive = clarificationFlightActive,
                    dismissingClarificationKey = dismissingClarificationKey,
                    dismissedClarificationKeys = dismissedClarificationKeys,
                    selectedClarificationOption = pendingClarificationAnswer?.selectedOption,
                    hiddenUserMessageKeys = hiddenUserMessageKeysForFlight,
                    activeFlightMessageKey = activeFlightMessageKey,
                    revealedMessageKeys = revealedMessageKeys,
                    liveRevealedMessageKeys = liveRevealedMessageKeys,
                    onMessageRevealComplete = { key ->
                        if (key !in revealedMessageKeyList) {
                            revealedMessageKeyList = revealedMessageKeyList + key
                        }
                    },
                    onMessageRevealActiveChange = { key, active ->
                        typingMessageKeys = if (active) {
                            liveRevealedMessageKeys = liveRevealedMessageKeys + key
                            typingMessageKeys + key
                        } else {
                            typingMessageKeys - key
                        }
                    },
                    onAssistantTurnVisualActiveChange = { key, active ->
                        visualActiveTurnKeys = if (active) {
                            visualActiveTurnKeys + key
                        } else {
                            visualActiveTurnKeys - key
                        }
                    },
                    onUserBubblePositioned = { key, snapshot ->
                        val shouldTrackLiveFlightTarget =
                            key == pendingFlightMessageKey || key == activeFlightMessageKey
                        if (
                            key !in userBubbleSnapshots ||
                            (shouldTrackLiveFlightTarget && userBubbleSnapshots[key] != snapshot)
                        ) {
                            userBubbleSnapshots = userBubbleSnapshots + (key to snapshot)
                        }
                    },
                    onClarificationCardDismissed = { finishClarificationDismissal(it) },
                )

                AttachmentMenuMotion(
                    visible = showAttachmentMenu,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp)
                        .padding(
                            bottom = calculateFloatingPanelBottomPadding(
                                density = density,
                                composerHeightPx = composerHeightPx,
                                imeBottomPx = imeBottomPx,
                            ),
                        ),
                ) {
                    AttachmentMenu(
                        onImageInput = {
                            showAttachmentMenu = false
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        onCameraInput = {
                            showAttachmentMenu = false
                            cameraLauncher.launch(null)
                        },
                    )
                }
            }
        }

        val composerStreaming = state.isStreaming &&
            state.currentTurnId != state.suppressComposerStreamingTurnId

        BottomComposer(
            text = input,
            inputState = state.inputState,
            isStreaming = composerStreaming,
            isTextRevealing = assistantVisualActive,
            awaitingCriteriaAdjustment = state.awaitingCriteriaAdjustment,
            isAttachmentMenuOpen = showAttachmentMenu,
            imageAttachment = state.imageAttachment,
            focusRequester = composerFocusRequester,
            modifier = Modifier
                .align(Alignment.BottomCenter),
            onAttachmentClick = {
                if (state.isStreaming) return@BottomComposer
                showAttachmentMenu = !showAttachmentMenu
                focusManager.clearFocus()
            },
            onTextChange = {
                input = it
                onInputChanged(it, state.imageAttachment.hasImage)
            },
            onTextFocus = {
                showAttachmentMenu = false
            },
            onFocusChanged = { composerFocused = it },
            onHeightChanged = { composerHeightPx = it },
            onKeyboardFlightSourceChanged = {
                keyboardFlightSnapshot = it
            },
            onRemoveImage = onClearImageAttachment,
            onPreviewImage = { imagePreviewAttachment = state.imageAttachment },
            onSubmit = {
                if (state.isStreaming) {
                    onCancel()
                    return@BottomComposer
                }
                val next = input.trim()
                val imageUrl = state.imageAttachment.imageUrl?.takeIf {
                    state.imageAttachment.canSend
                }
                if (next.isNotEmpty() || imageUrl != null) {
                    if (activeClarificationKey != null) {
                        val keyboardSnapshot = if (
                            imeBottomPx > 0 &&
                            rootSize.width > 0 &&
                            rootSize.height > imeBottomPx
                        ) {
                            ClarificationChipSnapshot(
                                position = Offset(0f, rootSize.height - imeBottomPx.toFloat()),
                                size = Size(rootSize.width.toFloat(), imeBottomPx.toFloat()),
                            )
                        } else {
                            null
                        }
                        val manualSnapshot = clarificationManualSource
                            ?.takeIf { it.nodeKey == activeClarificationKey }
                            ?.let { keyboardSnapshot ?: keyboardFlightSnapshot ?: it.snapshot }
                            ?: keyboardSnapshot
                            ?: keyboardFlightSnapshot
                        answerClarification(next, manualSnapshot = manualSnapshot)
                    } else {
                        sendAndClear(next, imageUrl)
                    }
                }
            },
        )

        imagePreviewAttachment?.let { attachment ->
            ImageAttachmentPreviewDialog(
                attachment = attachment,
                backendBaseUrl = state.backendBaseUrl,
                onDismiss = { imagePreviewAttachment = null },
            )
        }

        ClarificationFlightOverlay(
            flight = activeClarificationFlight,
            targetSnapshot = activeClarificationFlight
                ?.targetMessageKey
                ?.let { userBubbleSnapshots[it] },
            rootSize = rootSize,
            timelineTopPx = timelineTopPx,
            onFinished = { finished ->
                if (activeClarificationFlight?.id == finished.id) {
                    activeClarificationFlight = null
                    finished.targetMessageKey?.let {
                        hiddenFlightMessageKeys = hiddenFlightMessageKeys - it
                    }
                    pendingClarificationAnswer = null
                    if (dismissingClarificationKey == finished.nodeKey) {
                        dismissingClarificationKey = null
                    }
                }
            },
        )

        compareDetailPayload?.let { payload ->
            CompareModeScreen(
                payload = payload,
                backendBaseUrl = state.backendBaseUrl,
                onDismiss = { compareDetailPayload = null },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(6f),
            )
        }
    }

    LaunchedEffect(state.lastUserMessageKey, activeClarificationFlight?.id) {
        val flight = activeClarificationFlight ?: return@LaunchedEffect
        val key = state.lastUserMessageKey ?: return@LaunchedEffect
        if (
            flight.messageSent &&
            flight.targetMessageKey == null &&
            key != flight.previousUserMessageKey
        ) {
            hiddenFlightMessageKeys = hiddenFlightMessageKeys + key
            activeClarificationFlight = flight.copy(targetMessageKey = key)
        }
    }

    val sheetProductsById = timelinePresentation.productsById
    val sheetProductDeckIdByProductId = timelinePresentation.productDeckIdByProductId
    val sheetProductDeckNodeByKey = timelinePresentation.productDeckNodeByKey
    val sheetFinalDecisionSourceDeckKeyByDecisionKey = timelinePresentation.finalDecisionSourceDeckKeyByDecisionKey
    val content = sheetContent
    if (content != null) {
        ModalBottomSheet(
            onDismissRequest = { dismissSheet() },
            sheetState = sheetState,
            containerColor = BuyPilotColors.SurfaceCard,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { SheetHandle() },
        ) {
            SmoothSheetBody(
                sheetContent = content,
                exiting = sheetExiting,
            ) { targetContent ->
                when (targetContent) {
                    is ChatSheetContent.Criteria -> CriteriaEditSheet(
                        payload = targetContent.payload,
                        onQuickAction = { action ->
                            dismissSheet {
                                dispatchQuickAction(action)
                            }
                        },
                        onSave = { patch ->
                            dismissSheet { onCriteriaPatch(patch) }
                        },
                    )
                    is ChatSheetContent.DecisionEvidence -> {
                        val sourceDeckNodeKey = sheetFinalDecisionSourceDeckKeyByDecisionKey[targetContent.sourceNodeKey]
                        val sourceDeck = sourceDeckNodeKey?.let { sheetProductDeckNodeByKey[it] }
                        DecisionEvidenceSheet(
                            payload = targetContent.payload,
                            productsById = sourceDeck?.productsByProductId() ?: sheetProductsById,
                            productDeckIdByProductId = sourceDeck?.deckIdsByProductId() ?: sheetProductDeckIdByProductId,
                            swipeState = sourceDeck?.deckId?.let { state.productSwipeStates[it] },
                            sourceDeckNodeKey = sourceDeckNodeKey,
                            cartState = state.cartState,
                            onProductDetailOpen = { deckId, productId, deckNodeKey ->
                                dismissSheet {
                                    openProductDetailAfterTimelineFreeze(
                                        deckId = deckId,
                                        productId = productId,
                                        anchorNodeKey = targetContent.sourceNodeKey,
                                        deckNodeKey = deckNodeKey,
                                    )
                                }
                            },
                            onQuickAction = { action -> dispatchQuickAction(action) },
                        )
                    }
                    ChatSheetContent.Cart -> CartSheet(
                        state = state.cartState,
                        backendBaseUrl = state.backendBaseUrl,
                        onQuantityChange = onCartQuantityChange,
                        onProductDetailOpen = { productId ->
                            val deckId = sheetProductDeckIdByProductId[productId]
                            if (deckId != null) {
                                dismissSheet { openProductDetailAfterTimelineFreeze(deckId, productId) }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SmoothSheetBody(
    sheetContent: ChatSheetContent,
    exiting: Boolean,
    content: @Composable (ChatSheetContent) -> Unit,
) {
    val bodyAlpha by animateFloatAsState(
        targetValue = if (exiting) 0f else 1f,
        animationSpec = tween(
            durationMillis = if (exiting) 160 else 280,
            easing = if (exiting) MenuEaseIn else MenuEaseOut,
        ),
        label = "sheet_body_alpha",
    )
    val bodyOffsetY by animateDpAsState(
        targetValue = if (exiting) 10.dp else 0.dp,
        animationSpec = tween(
            durationMillis = if (exiting) 170 else 320,
            easing = if (exiting) MenuEaseIn else MenuEaseOut,
        ),
        label = "sheet_body_offset",
    )

    AnimatedContent(
        targetState = sheetContent,
        transitionSpec = {
            fadeIn(
                animationSpec = tween(durationMillis = 180, easing = MenuEaseOut),
            ) + slideInVertically(
                animationSpec = tween(durationMillis = 220, easing = MenuEaseOut),
                initialOffsetY = { it / 10 },
            ) togetherWith fadeOut(
                animationSpec = tween(durationMillis = 110, easing = MenuEaseIn),
            )
        },
        label = "sheet_content_transition",
    ) { targetContent ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = bodyOffsetY)
                .alpha(bodyAlpha),
        ) {
            content(targetContent)
        }
    }
}

private data class WelcomePrompt(
    @DrawableRes val iconRes: Int,
    val text: String,
    val tint: Color,
)

private data class PendingProductDetailRoute(
    val requestId: Int,
    val deckId: String,
    val productId: String,
    val anchorNodeKey: String = "",
    val deckNodeKey: String = "",
)

private val WelcomePrompts = listOf(
    WelcomePrompt(R.drawable.ic_search_24, "油皮洁面怎么选？", BuyPilotColors.Info),
    WelcomePrompt(R.drawable.ic_shield_24, "敏感肌面霜，帮我避开酒精香精", BuyPilotColors.Success),
    WelcomePrompt(R.drawable.ic_compare_arrows_24, "帮我对比两款商品，选更稳的", BuyPilotColors.PrimaryDark),
    WelcomePrompt(R.drawable.ic_payments_24, "200 元以内，推荐一支通勤防晒", BuyPilotColors.Warning),
)

@Composable
private fun ConversationStage(
    showWelcome: Boolean,
    welcomeContentVisible: Boolean,
    state: ChatUiState,
    timelinePresentation: TimelinePresentationState,
    timelineInstanceId: Int,
    externalRouteFreezeRequestId: Int,
    externalRouteFreezeAnchorNodeKey: String?,
    inlineComparePayloadByDeckId: Map<String, CompareCardPayload>,
    inlineCompareTurnIds: Set<String>,
    onExternalRouteFreezeReady: (Int) -> Unit,
    onClarificationOption: (String, ClarificationChipSnapshot?) -> Unit,
    onClarificationManualInput: () -> Unit,
    onClarificationManualSource: (String, ClarificationChipSnapshot) -> Unit,
    onCriteriaEdit: (CriteriaCardPayload) -> Unit,
    onProductOpen: (String, String?, String?) -> Unit,
    onProductDetailOpen: (String, String, String?) -> Unit,
    onCompareProducts: (List<Int>, List<String>?) -> Unit,
    onInlineCompareProducts: (String, List<Int>, List<String>?) -> Unit,
    onCompareDetailOpen: (CompareCardPayload) -> Unit,
    onConvergeRecommendation: (String) -> Unit,
    onWelcomePromptClick: (String) -> Unit,
    onDecisionEvidence: (FinalDecisionPayload, String?) -> Unit,
    onRetryLastMessage: () -> Unit,
    onEditLastMessage: (String) -> Unit,
    onQuickAction: (QuickActionPayload) -> Unit,
    onUserImagePreview: (String) -> Unit,
    onTimelineDrag: () -> Unit,
    composerHeightPx: Int,
    imeBottomPx: Int,
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
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = showWelcome,
            enter = fadeIn(
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            ) + slideInVertically(
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 24 },
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
            ) + slideOutVertically(
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                targetOffsetY = { -it / 18 },
            ) + scaleOut(
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                targetScale = 0.985f,
            ),
        ) {
            WelcomeHome(
                contentVisible = welcomeContentVisible,
                composerHeightPx = composerHeightPx,
                imeBottomPx = imeBottomPx,
                onWelcomePromptClick = onWelcomePromptClick,
            )
        }

        AnimatedVisibility(
            visible = timelinePresentation.hasNodes,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 140,
                    easing = FastOutSlowInEasing,
                ),
            ),
            exit = fadeOut(animationSpec = tween(durationMillis = 120)),
        ) {
            key(timelineInstanceId) {
                ChatTimeline(
                    state = state,
                    timelinePresentation = timelinePresentation,
                    externalRouteFreezeRequestId = externalRouteFreezeRequestId,
                    externalRouteFreezeAnchorNodeKey = externalRouteFreezeAnchorNodeKey,
                    inlineComparePayloadByDeckId = inlineComparePayloadByDeckId,
                    inlineCompareTurnIds = inlineCompareTurnIds,
                    onExternalRouteFreezeReady = onExternalRouteFreezeReady,
                    onClarificationOption = onClarificationOption,
                    onClarificationManualInput = onClarificationManualInput,
                    onClarificationManualSource = onClarificationManualSource,
                    onCriteriaEdit = onCriteriaEdit,
                    onProductOpen = onProductOpen,
                    onProductDetailOpen = onProductDetailOpen,
                    onCompareProducts = onCompareProducts,
                    onInlineCompareProducts = onInlineCompareProducts,
                    onCompareDetailOpen = onCompareDetailOpen,
                    onConvergeRecommendation = onConvergeRecommendation,
                    onDecisionEvidence = onDecisionEvidence,
                    onRetryLastMessage = onRetryLastMessage,
                    onEditLastMessage = onEditLastMessage,
                    onQuickAction = onQuickAction,
                    onUserImagePreview = onUserImagePreview,
                    onTimelineDrag = onTimelineDrag,
                    composerHeightPx = composerHeightPx,
                    stableImeBottomPx = stableImeBottomPx,
                    isComposerFocused = isComposerFocused,
                    isAssistantVisualActive = isAssistantVisualActive,
                    isClarificationFlightActive = isClarificationFlightActive,
                    dismissingClarificationKey = dismissingClarificationKey,
                    dismissedClarificationKeys = dismissedClarificationKeys,
                    selectedClarificationOption = selectedClarificationOption,
                    hiddenUserMessageKeys = hiddenUserMessageKeys,
                    activeFlightMessageKey = activeFlightMessageKey,
                    revealedMessageKeys = revealedMessageKeys,
                    liveRevealedMessageKeys = liveRevealedMessageKeys,
                    onMessageRevealComplete = onMessageRevealComplete,
                    onMessageRevealActiveChange = onMessageRevealActiveChange,
                    onAssistantTurnVisualActiveChange = onAssistantTurnVisualActiveChange,
                    onUserBubblePositioned = onUserBubblePositioned,
                    onClarificationCardDismissed = onClarificationCardDismissed,
                )
            }
        }
    }
}

@Composable
private fun AttachmentMenuMotion(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 150, easing = MenuEaseOut),
        ) + slideInVertically(
            animationSpec = tween(durationMillis = 230, easing = MenuEaseOut),
            initialOffsetY = { it / 16 },
        ) + scaleIn(
            animationSpec = tween(durationMillis = 230, easing = MenuEaseOut),
            initialScale = 0.985f,
            transformOrigin = TransformOrigin(0.18f, 1f),
        ),
        exit = fadeOut(
            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        ) + slideOutVertically(
            animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
            targetOffsetY = { it / 10 },
        ) + scaleOut(
            animationSpec = tween(durationMillis = 190, easing = FastOutSlowInEasing),
            targetScale = 0.965f,
            transformOrigin = TransformOrigin(0.18f, 1f),
        ),
        modifier = modifier,
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    centered: Boolean,
    showBack: Boolean,
    showClear: Boolean,
    cartCount: Int,
    onCartOpen: () -> Unit,
    onClearConversation: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BuyPilotColors.SurfaceCard)
            .statusBarsPadding(),
    ) {
        M3TopAppBarRow(
            title = if (centered) "BuyPilot AI" else "BuyPilot",
            titleCentered = centered,
            navigationIcon = if (showBack) R.drawable.ic_arrow_back_24 else R.drawable.ic_menu_24,
            navigationDescription = if (showBack) "返回" else "打开菜单",
            navigationTint = if (showBack) BuyPilotColors.PrimaryDark else BuyPilotColors.TextSecondary,
            actionIcon = R.drawable.ic_restart_24.takeIf { showClear },
            actionDescription = "清空当前对话",
            actionTint = BuyPilotColors.TextSecondary.copy(alpha = 0.72f),
            secondaryActionIcon = R.drawable.ic_shopping_bag_24.takeIf { showClear },
            secondaryActionDescription = "查看购物车",
            secondaryActionTint = BuyPilotColors.TextSecondary.copy(alpha = 0.82f),
            secondaryBadge = cartCount.takeIf { it > 0 }?.coerceAtMost(99)?.toString(),
            onNavigationClick = {},
            onSecondaryActionClick = onCartOpen,
            onActionClick = onClearConversation,
        )
        HorizontalDivider(thickness = 1.dp, color = BuyPilotColors.Border.copy(alpha = 0.46f))
    }
}

@Composable
internal fun M3TopAppBarRow(
    title: String,
    modifier: Modifier = Modifier,
    titleCentered: Boolean = true,
    navigationIcon: Int? = null,
    navigationDescription: String? = null,
    navigationTint: Color = BuyPilotColors.TextPrimary,
    actionIcon: Int? = null,
    actionDescription: String? = null,
    actionTint: Color = BuyPilotColors.TextSecondary,
    actionEnabled: Boolean = true,
    secondaryActionIcon: Int? = null,
    secondaryActionDescription: String? = null,
    secondaryActionTint: Color = BuyPilotColors.TextSecondary,
    secondaryActionEnabled: Boolean = true,
    secondaryBadge: String? = null,
    containerColor: Color = Color.Transparent,
    contentColor: Color = BuyPilotColors.TextPrimary,
    onNavigationClick: () -> Unit = {},
    onSecondaryActionClick: () -> Unit = {},
    onActionClick: () -> Unit = {},
) {
    val actionPadding = if (secondaryActionIcon != null && actionIcon != null) 120.dp else 72.dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(containerColor)
            .padding(horizontal = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(56.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (navigationIcon != null) {
                M3IconButton(
                    iconRes = navigationIcon,
                    contentDescription = navigationDescription.orEmpty(),
                    tint = navigationTint,
                    onClick = onNavigationClick,
                )
            }
        }
        Box(
            modifier = Modifier
                .align(if (titleCentered) Alignment.Center else Alignment.CenterStart)
                .then(
                    if (titleCentered) {
                        Modifier.padding(start = actionPadding, end = actionPadding)
                    } else {
                        Modifier.padding(start = 64.dp, end = actionPadding)
                    },
                ),
            contentAlignment = if (titleCentered) Alignment.Center else Alignment.CenterStart,
        ) {
            Text(
                text = title,
                color = contentColor,
                fontSize = 21.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (titleCentered) TextAlign.Center else TextAlign.Start,
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (secondaryActionIcon != null) {
                BadgedTopBarIconButton(
                    iconRes = secondaryActionIcon,
                    contentDescription = secondaryActionDescription.orEmpty(),
                    tint = secondaryActionTint,
                    badge = secondaryBadge,
                    enabled = secondaryActionEnabled,
                    onClick = onSecondaryActionClick,
                )
            }
            if (actionIcon != null) {
                M3IconButton(
                    iconRes = actionIcon,
                    contentDescription = actionDescription.orEmpty(),
                    tint = actionTint,
                    onClick = onActionClick,
                    enabled = actionEnabled,
                )
            }
        }
    }
}

@Composable
private fun BadgedTopBarIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    tint: Color,
    badge: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(contentAlignment = Alignment.Center) {
        M3IconButton(
            iconRes = iconRes,
            contentDescription = contentDescription,
            tint = tint,
            onClick = onClick,
            enabled = enabled,
        )
        if (!badge.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 8.dp)
                    .heightIn(min = 16.dp)
                    .background(BuyPilotColors.Primary, CircleShape)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = badge,
                    color = BuyPilotColors.OnPrimary,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun M3IconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(48.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = containerColor,
            contentColor = tint,
            disabledContentColor = tint.copy(alpha = 0.38f),
            disabledContainerColor = Color.Transparent,
        ),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun WelcomeHome(
    contentVisible: Boolean,
    composerHeightPx: Int,
    imeBottomPx: Int,
    onWelcomePromptClick: (String) -> Unit,
) {
    val density = LocalDensity.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        BuyPilotColors.SurfaceBg,
                        BuyPilotColors.SurfaceCard,
                        BuyPilotColors.SurfaceBg,
                    ),
                ),
            )
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(
                animationSpec = tween(durationMillis = 250, delayMillis = 90, easing = MenuEaseOut),
            ) + slideInVertically(
                animationSpec = tween(durationMillis = 300, delayMillis = 90, easing = MenuEaseOut),
                initialOffsetY = { it / 10 },
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 120, easing = MenuEaseIn),
            ) + slideOutVertically(
                animationSpec = tween(durationMillis = 150, easing = MenuEaseIn),
                targetOffsetY = { it / 10 },
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(1f))
                PromptSuggestions(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = calculateFloatingPanelBottomPadding(
                                density = density,
                                composerHeightPx = composerHeightPx,
                                imeBottomPx = imeBottomPx,
                            ),
                        ),
                    onPromptClick = onWelcomePromptClick,
                )
            }
        }
    }
}

@Preview(
    name = "BuyPilot Welcome",
    showBackground = true,
    backgroundColor = 0xFFF7F8FA,
    widthDp = 393,
    heightDp = 852,
)
@Composable
private fun WelcomeHomePreview() {
    WelcomeHome(
        contentVisible = true,
        composerHeightPx = 108,
        imeBottomPx = 0,
        onWelcomePromptClick = {},
    )
}

@Composable
private fun PromptSuggestions(
    modifier: Modifier = Modifier,
    onPromptClick: (String) -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        WelcomePrompts.forEach { prompt ->
            PromptSuggestionCard(prompt = prompt, onClick = { onPromptClick(prompt.text) })
        }
    }
}

@Composable
private fun PromptSuggestionCard(
    prompt: WelcomePrompt,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 28.dp)
            .padding(horizontal = 8.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(prompt.iconRes),
            contentDescription = null,
            tint = prompt.tint.copy(alpha = 0.9f),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = prompt.text,
            color = BuyPilotColors.TextPrimary.copy(alpha = 0.86f),
            fontSize = BuyPilotType.Body,
            lineHeight = 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}


private fun quadraticBezier(start: Float, control: Float, end: Float, fraction: Float): Float {
    val inverse = 1f - fraction
    return inverse * inverse * start + 2f * inverse * fraction * control + fraction * fraction * end
}

@Composable
internal fun rememberRouteEnterProgress(
    key: Any?,
    durationMillis: Int,
    delayMillis: Int = 0,
): State<Float> {
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMillis,
                delayMillis = delayMillis,
                easing = MenuEaseOut,
            ),
        )
    }
    return progress.asState()
}


@Composable
private fun ClarificationFlightOverlay(
    flight: ClarificationFlight?,
    targetSnapshot: ClarificationChipSnapshot?,
    rootSize: IntSize,
    timelineTopPx: Float,
    onFinished: (ClarificationFlight) -> Unit,
) {
    if (flight == null || rootSize.width == 0 || rootSize.height == 0) return

    val progress = remember(flight.id) { Animatable(0f) }
    val birthProgress = remember(flight.id) { Animatable(if (flight.fromKeyboard) 0f else 1f) }
    val density = LocalDensity.current
    val latestFlight by rememberUpdatedState(flight)
    val latestOnFinished by rememberUpdatedState(onFinished)
    var settledTargetSnapshot by remember(flight.id) { mutableStateOf<ClarificationChipSnapshot?>(null) }
    var flightStarted by remember(flight.id) { mutableStateOf(false) }
    val lockedTarget = settledTargetSnapshot
    val maxBubbleWidthPx = minOf(
        with(density) { 304.dp.toPx() },
        rootSize.width - with(density) { 32.dp.toPx() },
    )
    val estimatedTextWidthPx = flight.message
        .withoutMarkdownMarkup()
        .length
        .coerceAtLeast(4) *
        with(density) { 14.sp.toPx() } *
        0.92f
    val minTargetWidthPx = minOf(
        if (flight.fromKeyboard) {
            with(density) { 148.dp.toPx() }
        } else {
            maxOf(flight.startSize.width, with(density) { 132.dp.toPx() })
        },
        maxBubbleWidthPx,
    )
    val targetWidthPx = lockedTarget?.size?.width
        ?.coerceIn(0f, maxBubbleWidthPx)
        ?: (estimatedTextWidthPx + with(density) { 32.dp.toPx() })
            .coerceIn(
                minimumValue = minTargetWidthPx,
                maximumValue = maxBubbleWidthPx,
            )
    val targetHeightPx = lockedTarget?.size?.height ?: with(density) { 45.dp.toPx() }
    val endX = lockedTarget?.position?.x
        ?.coerceIn(0f, rootSize.width - targetWidthPx)
        ?: flight.startPosition.x
    val endY = lockedTarget?.position?.y
        ?.coerceIn(timelineTopPx, rootSize.height - targetHeightPx)
        ?: flight.startPosition.y

    LaunchedEffect(flight.id, targetSnapshot) {
        if (targetSnapshot == null || settledTargetSnapshot != null) return@LaunchedEffect
        kotlinx.coroutines.delay(ClarificationTargetSettleMs)
        settledTargetSnapshot = targetSnapshot
    }

    LaunchedEffect(flight.id) {
        if (flight.fromKeyboard) {
            birthProgress.snapTo(0f)
            birthProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = ClarificationKeyboardBirthMs,
                    easing = MenuEaseOut,
                ),
            )
        } else {
            birthProgress.snapTo(1f)
        }
    }

    LaunchedEffect(flight.id, settledTargetSnapshot) {
        if (settledTargetSnapshot == null) return@LaunchedEffect
        flightStarted = true
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = ClarificationFlightMs,
                easing = ClarificationFlightEase,
            ),
        )
        latestOnFinished(latestFlight)
    }

    LaunchedEffect(flight.id) {
        kotlinx.coroutines.delay(ClarificationTargetFallbackMs)
        if (settledTargetSnapshot == null) {
            latestOnFinished(latestFlight)
        }
    }

    val startWidthPx = if (flight.fromKeyboard) {
        (estimatedTextWidthPx + with(density) { 36.dp.toPx() })
            .coerceIn(
                minimumValue = with(density) { 148.dp.toPx() },
                maximumValue = maxBubbleWidthPx,
            )
    } else {
        flight.startSize.width
    }
    val startHeightPx = if (flight.fromKeyboard) {
        with(density) { 46.dp.toPx() }
    } else {
        flight.startSize.height
    }
    val startCenterX = flight.startPosition.x + flight.startSize.width / 2f
    val startCenterY = flight.startPosition.y + flight.startSize.height / 2f
    val endCenterX = endX + targetWidthPx / 2f
    val endCenterY = endY + targetHeightPx / 2f
    val verticalTravel = endCenterY - startCenterY
    val horizontalTravel = endCenterX - startCenterX
    val controlX = lerp(startCenterX, endCenterX, 0.56f)
    val controlY = lerp(startCenterY, endCenterY, 0.5f) -
        minOf(
            with(density) { 14.dp.toPx() },
            kotlin.math.abs(verticalTravel) * 0.18f + kotlin.math.abs(horizontalTravel) * 0.04f,
        )
    val bubbleShape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val t = progress.value
                    val morphT = segmentProgress(t, 0.04f, 0.88f)
                    val width = lerp(startWidthPx, targetWidthPx, morphT)
                    val height = lerp(startHeightPx, targetHeightPx, morphT)
                    val centerX = if (flightStarted) {
                        quadraticBezier(startCenterX, controlX, endCenterX, t)
                    } else {
                        startCenterX
                    }
                    val centerY = if (flightStarted) {
                        quadraticBezier(startCenterY, controlY, endCenterY, t)
                    } else {
                        startCenterY
                    }
                    val x = centerX - width / 2f
                    val y = centerY - height / 2f
                    val placeable = measurable.measure(
                        Constraints.fixed(
                            width = width.roundToInt().coerceAtLeast(1),
                            height = height.roundToInt().coerceAtLeast(1),
                        ),
                    )
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.placeRelative(
                            x = x.roundToInt(),
                            y = y.roundToInt(),
                        )
                    }
                }
                .graphicsLayer {
                    val t = progress.value
                    val birthT = birthProgress.value
                    val settleT = segmentProgress(t, 0.78f, 1f)
                    val settlePulse = (1f - settleT) * settleT * 4f
                    val birthScale = if (flight.fromKeyboard) lerp(0.78f, 1f, birthT) else 1f
                    scaleX = birthScale * (1f + 0.014f * settlePulse)
                    scaleY = birthScale * (1f + 0.008f * settlePulse)
                    alpha = if (flight.fromKeyboard) segmentProgress(t, 0f, 0.1f) else 1f
                }
                .shadow(
                    elevation = 10.dp,
                    shape = bubbleShape,
                    ambientColor = Color.Black.copy(alpha = 0.07f),
                    spotColor = Color.Black.copy(alpha = 0.09f),
                )
                .background(BuyPilotColors.Primary, bubbleShape)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = flight.option.withoutMarkdownMarkup(),
                color = BuyPilotColors.OnPrimary,
                fontSize = BuyPilotType.Label,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer {
                    val t = progress.value
                    alpha = if (flight.fromKeyboard) {
                        0f
                    } else {
                        (1f - segmentProgress(t, 0.18f, 0.34f)).coerceIn(0f, 1f)
                    }
                },
            )
            Text(
                text = flight.message.withoutMarkdownMarkup(),
                color = BuyPilotColors.OnPrimary,
                fontSize = BuyPilotType.Body,
                lineHeight = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer {
                    val t = progress.value
                    alpha = if (flight.fromKeyboard) {
                        birthProgress.value
                    } else {
                        segmentProgress(t, 0.26f, 0.48f)
                    }
                },
            )
        }
    }
}

@Composable
internal fun ClarificationOptionChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: (ClarificationChipSnapshot?) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    var snapshot by remember { mutableStateOf<ClarificationChipSnapshot?>(null) }
    val positionModifier = if (enabled) {
        Modifier.onGloballyPositioned { coordinates ->
            snapshot = coordinates.toClarificationSnapshot()
        }
    } else {
        Modifier
    }
    val backgroundColor by animateColorAsState(
        targetValue = when {
            selected -> BuyPilotColors.PrimarySoft.copy(alpha = 0.95f)
            pressed -> BuyPilotColors.ClarificationChipPressed
            else -> BuyPilotColors.ClarificationChipBackground
        },
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "clarification_chip_background",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> BuyPilotColors.Primary.copy(alpha = 0.32f)
            pressed -> BuyPilotColors.ClarificationChipPressedBorder
            else -> BuyPilotColors.ClarificationChipBorder
        },
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "clarification_chip_border",
    )
    val chipScale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.96f
            selected -> 0.98f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 130, easing = FastOutSlowInEasing),
        label = "clarification_chip_scale",
    )

    Text(
        text = label.withoutMarkdownMarkup(),
        color = if (selected) BuyPilotColors.PrimaryDark else BuyPilotColors.TextPrimary,
        fontSize = BuyPilotType.Label,
        lineHeight = 16.sp,
        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        modifier = Modifier
            .scale(chipScale)
            .clip(CircleShape)
            .background(backgroundColor, CircleShape)
            .border(1.dp, borderColor, CircleShape)
            .then(positionModifier)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick(snapshot)
                },
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
internal fun ClarificationManualInputRow(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    prompt: String,
    onClick: () -> Unit,
    onPositioned: (ClarificationChipSnapshot) -> Unit,
) {
    val positionModifier = if (enabled) {
        Modifier.onGloballyPositioned { coordinates ->
            onPositioned(coordinates.toClarificationSnapshot())
        }
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(BuyPilotColors.ClarificationManualBackground, RoundedCornerShape(10.dp))
            .border(1.dp, BuyPilotColors.ClarificationManualBorder, RoundedCornerShape(10.dp))
            .then(positionModifier)
            .clickable(
                enabled = enabled,
                onClickLabel = "聚焦输入框",
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_edit_24),
            contentDescription = null,
            tint = BuyPilotColors.TextMuted.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = prompt,
            color = BuyPilotColors.TextMuted,
            fontSize = BuyPilotType.Label,
            lineHeight = 16.sp,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ProductRecommendationStrip(
    node: ProductDeckNode,
    backendBaseUrl: String,
    swipeState: ProductSwipeState?,
    awaitingConvergence: Boolean,
    hasPendingDecision: Boolean,
    deckConverged: Boolean,
    deckStillStreaming: Boolean,
    comparePayload: CompareCardPayload?,
    compareNarrationContent: String = "",
    compareNarrationDone: Boolean = false,
    compareConclusionContent: String = "",
    compareConclusionDone: Boolean = false,
    convergeActionReady: Boolean,
    compactHistory: Boolean,
    motionEnabled: Boolean,
    alreadyEntered: Boolean,
    onEntered: () -> Unit,
    onOpen: (String, String?) -> Unit,
    onOpenDetail: (String, String) -> Unit,
    onCompare: (List<Int>) -> Unit,
    onConverge: () -> Unit,
) {
    val products = node.products
    if (products.isEmpty()) {
        LaunchedEffect(node.key, alreadyEntered) {
            if (!alreadyEntered) onEntered()
        }
        return
    }
    if (compactHistory) {
        LaunchedEffect(node.key, alreadyEntered) {
            if (!alreadyEntered) onEntered()
        }
        ProductRecommendationHistorySummary(
            node = node,
            backendBaseUrl = backendBaseUrl,
            onOpen = onOpen,
            onOpenDetail = onOpenDetail,
        )
        return
    }
    val singleCandidate = isSingleCandidateDeck(products.size)
    val deckFullyHandled = isProductDeckFullyHandled(products, swipeState)
    val preferredPage = preferredProductCarouselPage(products, swipeState)
    val productIdSignature = remember(products) {
        products.joinToString(separator = "|") { it.product.productId }
    }
    var comparePicking by rememberSaveable(node.key, productIdSignature) { mutableStateOf(false) }
    var selectedCompareRanks by rememberSaveable(node.key, productIdSignature) {
        mutableStateOf(products.take(2).mapIndexed { index, _ -> index + 1 })
    }
    LaunchedEffect(comparePicking, products.size) {
        if (comparePicking && selectedCompareRanks.size < 2 && products.size >= 2) {
            selectedCompareRanks = listOf(1, 2)
        }
    }

    val pagerState = rememberPagerState(
        initialPage = preferredPage,
        pageCount = { products.size },
    )
    val activeIndex = pagerState.currentPage.coerceIn(0, products.lastIndex)

    LaunchedEffect(node.deckId, productIdSignature, preferredPage, products.size) {
        if (preferredPage != pagerState.currentPage && !pagerState.isScrollInProgress) {
            pagerState.scrollToPage(preferredPage)
        }
    }

    val arrivalProgress = rememberProductDeckArrivalProgressProvider(
        key = node.key,
        motionEnabled = motionEnabled,
        alreadyEntered = alreadyEntered,
        onEntered = onEntered,
    )
    val chromeProgress = remember(arrivalProgress) { { segmentProgress(arrivalProgress(), 0.46f, 1f) } }
    val convergeButtonTargetProgress = if (awaitingConvergence && !deckFullyHandled && convergeActionReady) 1f else 0f
    val convergeButtonProgress by animateFloatAsState(
        targetValue = if (comparePicking) 1f else convergeButtonTargetProgress,
        animationSpec = tween(durationMillis = 220, easing = MenuEaseOut),
        label = "product_deck_converge_button_progress",
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val chrome = chromeProgress()
                    alpha = chrome
                    translationY = (1f - chrome) * 8f
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (comparePicking) {
                        "货比三家"
                    } else if (singleCandidate) {
                        "唯一匹配商品"
                    } else {
                        "候选商品"
                    },
                    color = BuyPilotColors.TextPrimary,
                    fontSize = BuyPilotType.Title,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            ProductDeckModeIconButton(
                picking = comparePicking,
                enabled = products.size >= 2 && !deckStillStreaming,
                onClick = {
                    comparePicking = !comparePicking
                    if (!comparePicking) {
                        selectedCompareRanks = products.take(2).mapIndexed { index, _ -> index + 1 }
                    }
                },
            )
        }
        AnimatedContent(
            targetState = comparePicking,
            transitionSpec = {
                if (targetState) {
                    fadeIn(
                        animationSpec = tween(durationMillis = 230, delayMillis = 45, easing = MenuEaseOut),
                    ) + slideInVertically(
                        animationSpec = tween(durationMillis = 280, delayMillis = 35, easing = MenuEaseOut),
                        initialOffsetY = { it / 14 },
                    ) + scaleIn(
                        animationSpec = tween(durationMillis = 280, delayMillis = 35, easing = MenuEaseOut),
                        initialScale = 0.972f,
                        transformOrigin = TransformOrigin(0.5f, 0.16f),
                    ) togetherWith fadeOut(
                        animationSpec = tween(durationMillis = 130, easing = MenuEaseIn),
                    ) + slideOutVertically(
                        animationSpec = tween(durationMillis = 160, easing = MenuEaseIn),
                        targetOffsetY = { -it / 20 },
                    ) + scaleOut(
                        animationSpec = tween(durationMillis = 160, easing = MenuEaseIn),
                        targetScale = 0.966f,
                        transformOrigin = TransformOrigin(0.5f, 0.12f),
                    )
                } else {
                    fadeIn(
                        animationSpec = tween(durationMillis = 190, delayMillis = 25, easing = MenuEaseOut),
                    ) + slideInVertically(
                        animationSpec = tween(durationMillis = 220, delayMillis = 20, easing = MenuEaseOut),
                        initialOffsetY = { -it / 26 },
                    ) + scaleIn(
                        animationSpec = tween(durationMillis = 220, delayMillis = 20, easing = MenuEaseOut),
                        initialScale = 0.982f,
                        transformOrigin = TransformOrigin(0.5f, 0.18f),
                    ) togetherWith fadeOut(
                        animationSpec = tween(durationMillis = 120, easing = MenuEaseIn),
                    ) + slideOutVertically(
                        animationSpec = tween(durationMillis = 140, easing = MenuEaseIn),
                        targetOffsetY = { it / 24 },
                    ) + scaleOut(
                        animationSpec = tween(durationMillis = 140, easing = MenuEaseIn),
                        targetScale = 0.986f,
                        transformOrigin = TransformOrigin(0.5f, 0.1f),
                    )
                }
            },
            label = "product_deck_compare_mode",
        ) { picking ->
            if (picking) {
                ProductDeckComparePicker(
                    products = products,
                    backendBaseUrl = backendBaseUrl,
                    selectedRanks = selectedCompareRanks,
                    chromeProgress = chromeProgress,
                    confirmEnabled = selectedCompareRanks.size >= 2,
                    motionEnabled = motionEnabled,
                    onSelectionChange = { selectedCompareRanks = it },
                    onConfirm = {
                        if (selectedCompareRanks.size >= 2) {
                            comparePicking = false
                            onCompare(selectedCompareRanks)
                        }
                    },
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(238.dp),
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = if (singleCandidate) 12.dp else 44.dp),
                        pageSpacing = 14.dp,
                        beyondViewportPageCount = 0,
                    ) { page ->
                        val payload = products[page]
                        val pageOffset = (
                            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            ).coerceIn(-1f, 1f)
                        val distance = abs(pageOffset)
                        val thumbModifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = 1f - distance * 0.055f
                                scaleY = 1f - distance * 0.075f
                                alpha = 1f - distance * 0.18f
                            }
                        val openProduct = {
                            val productId = payload.product.productId.takeIf { id -> id.isNotBlank() }
                            if (productId != null) {
                                onOpenDetail(node.deckId, productId)
                            } else {
                                onOpen(node.deckId, null)
                            }
                        }
                        if (page == 0) {
                            ProductDeckArrivalMotion(
                                arrivalProgress = arrivalProgress,
                            ) {
                                ProductRecommendationThumb(
                                    payload = payload,
                                    backendBaseUrl = backendBaseUrl,
                                    selected = page == activeIndex,
                                    arrivalProgress = arrivalProgress,
                                    onClick = openProduct,
                                    modifier = thumbModifier,
                                )
                            }
                        } else {
                            ProductRecommendationThumb(
                                payload = payload,
                                backendBaseUrl = backendBaseUrl,
                                selected = page == activeIndex,
                                onClick = openProduct,
                                modifier = thumbModifier,
                            )
                        }
                    }
                }
            }
        }
        if (!comparePicking) {
            ProductDeckFooterControls(
                showIndicators = !singleCandidate,
                productCount = products.size,
                activeIndex = activeIndex,
                chromeProgress = chromeProgress,
                buttonProgress = convergeButtonProgress,
                buttonLabel = "帮我选",
                enabled = true,
                onConverge = onConverge,
            )
        }
        AnimatedVisibility(
            visible = !comparePicking && comparePayload != null,
            enter = fadeIn(
                animationSpec = tween(durationMillis = 180, delayMillis = 40, easing = MenuEaseOut),
            ) + slideInVertically(
                animationSpec = tween(durationMillis = 220, delayMillis = 35, easing = MenuEaseOut),
                initialOffsetY = { it / 12 },
            ) + expandVertically(
                animationSpec = tween(durationMillis = 220, delayMillis = 35, easing = MenuEaseOut),
                expandFrom = Alignment.Top,
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 120, easing = MenuEaseIn),
            ) + shrinkVertically(
                animationSpec = tween(durationMillis = 140, easing = MenuEaseIn),
                shrinkTowards = Alignment.Top,
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                comparePayload?.let { payload ->
                    ProductDeckMarkdownCompareTable(
                        payload = payload,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (compareNarrationContent.isNotBlank()) {
                        StreamingAssistantText(
                            nodeKey = "inline_compare_narration_${payload.compareId}",
                            content = compareNarrationContent,
                            done = compareNarrationDone,
                        )
                    }
                    if (compareConclusionContent.isNotBlank()) {
                        StreamingAssistantText(
                            nodeKey = "inline_compare_conclusion_${payload.compareId}",
                            content = compareConclusionContent,
                            done = compareConclusionDone,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductDeckFooterControls(
    showIndicators: Boolean,
    productCount: Int,
    activeIndex: Int,
    chromeProgress: () -> Float,
    buttonProgress: Float,
    buttonLabel: String,
    enabled: Boolean,
    onConverge: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(ProductDeckFooterHeight)
            .graphicsLayer {
                val chrome = chromeProgress()
                alpha = maxOf(chrome, buttonProgress * 0.96f)
                translationY = (1f - chrome.coerceIn(0f, 1f)) * 4f
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (showIndicators) Arrangement.spacedBy(6.dp) else Arrangement.Center,
    ) {
        if (showIndicators) {
            ProductDeckPageIndicators(
                productCount = productCount,
                activeIndex = activeIndex,
            )
        }
        ProductDeckConvergeMiniButton(
            label = buttonLabel,
            visibleProgress = buttonProgress,
            enabled = enabled,
            onClick = onConverge,
            modifier = Modifier,
        )
    }
}

@Composable
private fun ProductDeckModeIconButton(
    picking: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (picking) {
            BuyPilotColors.PrimarySoft.copy(alpha = 0.76f)
        } else {
            BuyPilotColors.SurfaceCard.copy(alpha = 0.88f)
        },
        animationSpec = tween(durationMillis = 180, easing = MenuEaseOut),
        label = "product_deck_compare_icon_background",
    )
    val borderColor by animateColorAsState(
        targetValue = if (picking) {
            BuyPilotColors.Primary.copy(alpha = 0.22f)
        } else {
            BuyPilotColors.Border.copy(alpha = 0.7f)
        },
        animationSpec = tween(durationMillis = 180, easing = MenuEaseOut),
        label = "product_deck_compare_icon_border",
    )
    val tint by animateColorAsState(
        targetValue = if (picking) BuyPilotColors.PrimaryDark else BuyPilotColors.TextSecondary,
        animationSpec = tween(durationMillis = 180, easing = MenuEaseOut),
        label = "product_deck_compare_icon_tint",
    )
    val containerScale by animateFloatAsState(
        targetValue = if (picking) 1.04f else 1f,
        animationSpec = tween(durationMillis = 190, easing = MenuEaseOut),
        label = "product_deck_compare_icon_scale",
    )
    val iconTilt by animateFloatAsState(
        targetValue = if (picking) -45f else 0f,
        animationSpec = tween(durationMillis = 180, easing = MenuEaseOut),
        label = "product_deck_compare_icon_tilt",
    )
    Box(
        modifier = Modifier
            .size(32.dp)
            .graphicsLayer {
                scaleX = containerScale
                scaleY = containerScale
            }
            .clip(CircleShape)
            .background(backgroundColor, CircleShape)
            .border(1.dp, borderColor, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = picking,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 150, easing = MenuEaseOut)) +
                    scaleIn(initialScale = 0.86f, animationSpec = tween(durationMillis = 180, easing = MenuEaseOut)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 100, easing = MenuEaseIn)) +
                    scaleOut(targetScale = 0.88f, animationSpec = tween(durationMillis = 100, easing = MenuEaseIn))
            },
            label = "product_deck_compare_icon_swap",
        ) { active ->
            Icon(
                painter = painterResource(if (active) R.drawable.ic_close_24 else R.drawable.ic_compare_arrows_24),
                contentDescription = if (active) "取消对比选择" else "选择商品对比",
                tint = tint.copy(alpha = if (enabled) 1f else 0.38f),
                modifier = Modifier
                    .size(if (active) 16.dp else 17.dp)
                    .graphicsLayer {
                        rotationZ = if (active) iconTilt * 0.55f else iconTilt
                    },
            )
        }
    }
}

@Composable
private fun ProductDeckComparePicker(
    products: List<ProductCardPayload>,
    backendBaseUrl: String,
    selectedRanks: List<Int>,
    chromeProgress: () -> Float,
    confirmEnabled: Boolean,
    motionEnabled: Boolean,
    onSelectionChange: (List<Int>) -> Unit,
    onConfirm: () -> Unit,
) {
    val visibleProducts = products.take(6)
    val productIdSignature = remember(visibleProducts) {
        visibleProducts.joinToString(separator = "|") { it.product.productId }
    }
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
    ) {
        val compactGrid = visibleProducts.size >= 4
        val horizontalGap = if (compactGrid) 8.dp else 10.dp
        val itemWidth = if (compactGrid) {
            (maxWidth - horizontalGap) / 2
        } else {
            (maxWidth - horizontalGap * (visibleProducts.size - 1).coerceAtLeast(0)) /
                visibleProducts.size.coerceAtLeast(1)
        }
        val itemHeight = if (compactGrid) 94.dp else 178.dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val chrome = chromeProgress()
                    alpha = chrome
                    translationY = (1f - chrome.coerceIn(0f, 1f)) * 6f
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (compactGrid) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    visibleProducts.chunked(2).forEachIndexed { rowIndex, rowProducts ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(horizontalGap),
                        ) {
                            rowProducts.forEachIndexed { colIndex, payload ->
                                val index = rowIndex * 2 + colIndex
                                ProductDeckCompareCandidateCard(
                                    rank = index + 1,
                                    payload = payload,
                                    backendBaseUrl = backendBaseUrl,
                                    selected = index + 1 in selectedRanks,
                                    compact = true,
                                    entryIndex = index,
                                    motionEnabled = motionEnabled,
                                    modifier = Modifier
                                        .width(itemWidth)
                                        .height(itemHeight),
                                    onClick = {
                                        val rank = index + 1
                                        val selected = rank in selectedRanks
                                        val nextRanks = if (selected) {
                                            if (selectedRanks.size <= 2) selectedRanks else selectedRanks - rank
                                        } else {
                                            if (selectedRanks.size >= MaxCompareSelectionCount) selectedRanks else selectedRanks + rank
                                        }.distinct().sorted()
                                        onSelectionChange(nextRanks)
                                    },
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(horizontalGap, Alignment.CenterHorizontally),
                ) {
                    visibleProducts.forEachIndexed { index, payload ->
                        val rank = index + 1
                        val selected = rank in selectedRanks
                        ProductDeckCompareCandidateCard(
                            rank = rank,
                            payload = payload,
                            backendBaseUrl = backendBaseUrl,
                            selected = selected,
                            compact = false,
                            entryIndex = index,
                            motionEnabled = motionEnabled,
                            modifier = Modifier
                                .width(itemWidth)
                                .height(itemHeight),
                            onClick = {
                                val nextRanks = if (selected) {
                                    if (selectedRanks.size <= 2) selectedRanks else selectedRanks - rank
                                } else {
                                    if (selectedRanks.size >= MaxCompareSelectionCount) selectedRanks else selectedRanks + rank
                                }.distinct().sorted()
                                onSelectionChange(nextRanks)
                            },
                        )
                    }
                }
            }
            val confirmProgressState = if (motionEnabled) {
                rememberRouteEnterProgress(
                    key = "compare-confirm-$productIdSignature-$compactGrid",
                    durationMillis = 220,
                    delayMillis = if (compactGrid) 135 else 95,
                )
            } else {
                rememberUpdatedState(1f)
            }
            val confirmProgress = confirmProgressState.value
            ProductDeckConvergeMiniButton(
                label = "确认",
                visibleProgress = confirmProgress,
                enabled = confirmEnabled,
                onClick = onConfirm,
                modifier = Modifier
                    .height(30.dp),
            )
        }
    }
}

@Composable
private fun ProductDeckConvergeMiniButton(
    label: String,
    visibleProgress: Float,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (visibleProgress <= 0.01f) return
    val textProgress = segmentProgress(visibleProgress, 0.5f, 1f)
    Row(
        modifier = modifier
            .graphicsLayer {
                alpha = visibleProgress
                translationY = (1f - visibleProgress) * 5f
                scaleX = lerp(0.98f, 1f, textProgress)
                scaleY = lerp(0.98f, 1f, textProgress)
                transformOrigin = TransformOrigin(1f, 0.5f)
            }
            .alpha(if (enabled) 1f else 0.42f)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .height(28.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = BuyPilotColors.TextMuted.copy(alpha = lerp(0f, 0.82f, textProgress)),
            fontSize = BuyPilotType.Label,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProductDeckCompareCandidateCard(
    rank: Int,
    payload: ProductCardPayload,
    backendBaseUrl: String,
    selected: Boolean,
    compact: Boolean,
    entryIndex: Int,
    motionEnabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val product = payload.product
    val density = LocalDensity.current
    val entryDelayMs = if (compact) {
        (entryIndex / 2) * 40 + (entryIndex % 2) * 14
    } else {
        entryIndex * 38
    }
    val entryProgressState = if (motionEnabled) {
        rememberRouteEnterProgress(
            key = "compare-card-${payload.product.productId}-$compact",
            durationMillis = 260,
            delayMillis = entryDelayMs,
        )
    } else {
        rememberUpdatedState(1f)
    }
    val entryProgress = entryProgressState.value
    val stagedModifier = modifier.graphicsLayer {
        alpha = entryProgress
        translationY = (1f - entryProgress) * with(density) {
            if (compact) 8.dp.toPx() else 12.dp.toPx()
        }
        scaleX = lerp(0.974f, 1f, entryProgress)
        scaleY = lerp(0.974f, 1f, entryProgress)
        transformOrigin = TransformOrigin(0.5f, 0.12f)
    }
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            BuyPilotColors.Primary.copy(alpha = 0.48f)
        } else {
            BuyPilotColors.Border.copy(alpha = 0.56f)
        },
        animationSpec = tween(durationMillis = 170, easing = MenuEaseOut),
        label = "product_deck_compare_card_border",
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            BuyPilotColors.PrimarySoft.copy(alpha = 0.2f)
        } else {
            BuyPilotColors.SurfaceCard.copy(alpha = 0.96f)
        },
        animationSpec = tween(durationMillis = 170, easing = MenuEaseOut),
        label = "product_deck_compare_card_background",
    )
    val checkProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = if (selected) 150 else 90, easing = if (selected) MenuEaseOut else MenuEaseIn),
        label = "product_deck_compare_check_progress",
    )
    if (compact) {
        CompactCompareCandidateRow(
            rank = rank,
            payload = payload,
            backendBaseUrl = backendBaseUrl,
            borderColor = borderColor,
            backgroundColor = backgroundColor,
            checkProgress = checkProgress,
            modifier = stagedModifier,
            onClick = onClick,
        )
        return
    }
    Column(
        modifier = stagedModifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor, RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(if (compact) 7.dp else 9.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
                .heightIn(min = if (compact) 44.dp else 86.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(BuyPilotColors.SurfaceImageAlt),
            contentAlignment = Alignment.Center,
        ) {
            ProductImage(
                product = product,
                backendBaseUrl = backendBaseUrl,
                decodeSizePx = if (compact) 150 else 210,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (compact) 5.dp else 7.dp),
                contentScale = ContentScale.Fit,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .height(18.dp)
                    .background(BuyPilotColors.SurfaceCard.copy(alpha = 0.9f), CircleShape)
                    .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.5f), CircleShape)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "#$rank",
                    color = BuyPilotColors.TextSecondary,
                    fontSize = BuyPilotType.Tiny,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .graphicsLayer {
                        alpha = checkProgress
                        scaleX = lerp(0.82f, 1f, checkProgress)
                        scaleY = lerp(0.82f, 1f, checkProgress)
                    }
                    .size(18.dp)
                    .background(BuyPilotColors.Primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✓",
                    color = BuyPilotColors.OnPrimary,
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Text(
            text = product.displayName(),
            color = BuyPilotColors.TextPrimary,
            fontSize = if (compact) 12.sp else 13.sp,
            lineHeight = if (compact) 15.sp else 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        ProductPriceLabel(
            price = product.priceLabel(),
            modifier = Modifier.fillMaxWidth(),
            compact = compact,
        )
    }
}

@Composable
private fun CompactCompareCandidateRow(
    rank: Int,
    payload: ProductCardPayload,
    backendBaseUrl: String,
    borderColor: Color,
    backgroundColor: Color,
    checkProgress: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val product = payload.product
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(backgroundColor, RoundedCornerShape(15.dp))
            .border(1.dp, borderColor, RoundedCornerShape(15.dp))
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BuyPilotColors.SurfaceImageAlt),
            contentAlignment = Alignment.Center,
        ) {
            ProductImage(
                product = product,
                backendBaseUrl = backendBaseUrl,
                decodeSizePx = 140,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp),
                contentScale = ContentScale.Fit,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-2).dp, y = (-2).dp)
                    .height(18.dp)
                    .background(BuyPilotColors.SurfaceCard.copy(alpha = 0.94f), CircleShape)
                    .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.48f), CircleShape)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "#$rank",
                    color = BuyPilotColors.TextSecondary,
                    fontSize = BuyPilotType.Tiny,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = product.displayName(),
                color = BuyPilotColors.TextPrimary,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            ProductPriceLabel(
                price = product.priceLabel(),
                compact = true,
            )
        }
        Box(
            modifier = Modifier
                .graphicsLayer {
                    alpha = checkProgress
                    scaleX = lerp(0.82f, 1f, checkProgress)
                    scaleY = lerp(0.82f, 1f, checkProgress)
                }
                .size(20.dp)
                .background(BuyPilotColors.Primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "✓",
                color = BuyPilotColors.OnPrimary,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ProductDeckPageIndicators(
    productCount: Int,
    activeIndex: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(productCount.coerceAtMost(6)) { index ->
            val active = index == activeIndex.coerceAtMost(5)
            val indicatorWidth by animateDpAsState(
                targetValue = if (active) 18.dp else 5.dp,
                animationSpec = tween(durationMillis = 180, easing = MenuEaseOut),
                label = "product_carousel_indicator_width",
            )
            val indicatorColor by animateColorAsState(
                targetValue = if (active) BuyPilotColors.Primary.copy(alpha = 0.82f) else BuyPilotColors.Border.copy(alpha = 0.9f),
                animationSpec = tween(durationMillis = 180),
                label = "product_carousel_indicator_color",
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(width = indicatorWidth, height = 5.dp)
                    .background(indicatorColor, CircleShape),
            )
        }
    }
}

@Composable
private fun ProductRecommendationHistorySummary(
    node: ProductDeckNode,
    backendBaseUrl: String,
    onOpen: (String, String?) -> Unit,
    onOpenDetail: (String, String) -> Unit,
) {
    val products = node.products
    if (products.isEmpty()) return
    val visibleProducts = products.take(5)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BuyPilotColors.SurfaceCard.copy(alpha = 0.92f),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border.copy(alpha = 0.56f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "上一轮候选",
                color = BuyPilotColors.TextMuted,
                fontSize = BuyPilotType.Label,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
            )
            visibleProducts.forEachIndexed { index, payload ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 68.dp)
                            .height(1.dp)
                            .background(BuyPilotColors.Border.copy(alpha = 0.42f)),
                    )
                }
                ProductHistoryListRow(
                    payload = payload,
                    backendBaseUrl = backendBaseUrl,
                    onClick = {
                        payload.product.productId
                            .takeIf { it.isNotBlank() }
                            ?.let { onOpenDetail(node.deckId, it) }
                            ?: onOpen(node.deckId, null)
                    },
                )
            }
            if (products.size > visibleProducts.size) {
                Text(
                    text = "查看全部 ${products.size} 个候选",
                    color = BuyPilotColors.TextMuted,
                    fontSize = BuyPilotType.Label,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            visibleProducts.firstOrNull()?.product?.productId
                                ?.takeIf { it.isNotBlank() }
                                ?.let { onOpenDetail(node.deckId, it) }
                                ?: onOpen(node.deckId, null)
                        }
                        .padding(vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ProductHistoryListRow(
    payload: ProductCardPayload,
    backendBaseUrl: String,
    onClick: () -> Unit,
) {
    val product = payload.product
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BuyPilotColors.SurfaceImage)
                .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.42f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            ProductImage(
                product = product,
                backendBaseUrl = backendBaseUrl,
                decodeSizePx = 128,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = product.displayName(),
                color = BuyPilotColors.TextPrimary,
                fontSize = BuyPilotType.Body,
                lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = listOf(
                    product.brandLabel().takeIf { it.isNotBlank() },
                    product.priceLabel().takeIf { it.isNotBlank() },
                ).filterNotNull().joinToString(" · "),
                color = BuyPilotColors.TextMuted,
                fontSize = BuyPilotType.Label,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ProductDetailChevron()
    }
}

@Composable
private fun ProductDetailChevron(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .background(BuyPilotColors.SurfaceCard.copy(alpha = 0.72f), CircleShape)
            .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.36f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right_20),
            contentDescription = null,
            tint = BuyPilotColors.TextMuted.copy(alpha = 0.76f),
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
internal fun CandidateActionButton(
    label: String,
    @DrawableRes leadingIconRes: Int,
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val background = if (primary) BuyPilotColors.Primary else BuyPilotColors.SurfaceCard.copy(alpha = 0.78f)
    val content = if (primary) BuyPilotColors.OnPrimary else BuyPilotColors.TextSecondary
    val border = if (primary) BuyPilotColors.Primary.copy(alpha = 0.0f) else BuyPilotColors.Border.copy(alpha = 0.76f)
    Row(
        modifier = modifier
            .height(36.dp)
            .background(background, CircleShape)
            .border(1.dp, border, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(leadingIconRes),
            contentDescription = null,
            tint = content.copy(alpha = 0.92f),
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = content,
            fontSize = BuyPilotType.Label,
            lineHeight = 16.sp,
            fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun rememberProductDisplayTags(
    payload: ProductCardPayload,
    limit: Int,
): List<String> {
    val product = payload.product
    return remember(
        product.ingredientTags,
        product.skinTypeMatch,
        product.useScenario,
        payload.reason,
        payload.riskNotes,
        limit,
    ) {
        payload.displayTags().take(limit)
    }
}

@Composable
private fun rememberPlainProductReason(reason: String): String? =
    remember(reason) {
        reason.withoutMarkdownMarkup()
            .withoutInternalDebugTokens()
            .trim()
            .takeIf { it.isNotBlank() }
    }

@Composable
private fun ProductRecommendationThumb(
    payload: ProductCardPayload,
    backendBaseUrl: String,
    selected: Boolean,
    arrivalProgress: () -> Float = { 1f },
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val product = payload.product
    val tags = rememberProductDisplayTags(payload = payload, limit = 2)
    val reason = rememberPlainProductReason(payload.reason)
    val cardShape = RoundedCornerShape(18.dp)
    val borderAlpha = if (selected) 0.72f else 0.54f

    Surface(
        modifier = modifier
            .height(218.dp)
            .clip(cardShape)
            .clickable(onClick = onClick),
        color = BuyPilotColors.SurfaceCard,
        shape = cardShape,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border.copy(alpha = borderAlpha)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .graphicsLayer {
                    val cardFillProgress = segmentProgress(arrivalProgress(), 0.36f, 0.76f)
                    alpha = 0.08f + cardFillProgress * 0.92f
                    translationY = (1f - cardFillProgress) * 16f
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(106.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BuyPilotColors.SurfaceImage)
                    .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.36f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                ProductImageLoadingSeed(
                    progress = { segmentProgress(arrivalProgress(), 0f, 0.48f) },
                    modifier = Modifier
                        .width(92.dp)
                        .height(132.dp)
                        .padding(horizontal = 3.dp, vertical = 6.dp),
                )
                ProductImage(
                    product = product,
                    backendBaseUrl = backendBaseUrl,
                    decodeSizePx = 240,
                    modifier = Modifier
                        .graphicsLayer {
                            val imageProgress = segmentProgress(arrivalProgress(), 0.5f, 0.92f)
                            alpha = imageProgress
                            scaleX = 0.985f + imageProgress * 0.015f
                            scaleY = 0.985f + imageProgress * 0.015f
                        }
                        .width(92.dp)
                        .height(132.dp)
                        .padding(horizontal = 3.dp, vertical = 6.dp),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .graphicsLayer {
                        val detailProgress = segmentProgress(arrivalProgress(), 0.64f, 1f)
                        alpha = detailProgress
                        translationY = (1f - detailProgress) * 10f
                    }
                    .padding(start = 13.dp, top = 4.dp, end = 3.dp, bottom = 3.dp),
            ) {
                Text(
                    text = product.brandLabel(),
                    color = BuyPilotColors.TextMuted.copy(alpha = 0.86f),
                    fontSize = BuyPilotType.Tiny,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = product.displayName(),
                    color = BuyPilotColors.TextPrimary,
                    fontSize = 14.5f.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (reason != null) {
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = reason,
                        color = BuyPilotColors.TextSecondary.copy(alpha = 0.76f),
                        fontSize = 11.5f.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.weight(1f))
                ProductPriceLabel(price = product.priceLabel())
                Spacer(Modifier.height(9.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    tags.forEach { tag ->
                        ProductMiniTag(
                            label = tag,
                            compact = true,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductPriceLabel(
    price: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val match = remember(price) { Regex("""^([¥￥$]?)(\d+(?:\.\d+)?)(.*)$""").find(price) }
    if (match == null) {
        Text(
            text = price,
            color = BuyPilotColors.PrimaryDark,
            fontSize = if (compact) 13.sp else BuyPilotType.Body,
            lineHeight = if (compact) 16.sp else 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
        return
    }

    val (currency, amount, suffix) = match.destructured
    Text(
        text = buildAnnotatedString {
            if (currency.isNotBlank()) {
                withStyle(
                    SpanStyle(
                        fontSize = if (compact) 9.sp else 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                ) {
                    append(currency)
                }
            }
            withStyle(
                    SpanStyle(
                        fontSize = if (compact) 14.sp else 16.sp,
                        fontWeight = FontWeight.SemiBold,
                ),
            ) {
                append(amount)
            }
            if (suffix.isNotBlank()) {
                withStyle(
                    SpanStyle(
                        fontSize = if (compact) 9.sp else 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = BuyPilotColors.TextMuted,
                    ),
                ) {
                    append(suffix)
                }
            }
        },
        color = BuyPilotColors.PrimaryDark,
        lineHeight = if (compact) 17.sp else 20.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
internal fun ProductHeroCard(
    payload: ProductCardPayload,
    backendBaseUrl: String,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
) {
    val product = payload.product
    val tags = rememberProductDisplayTags(payload = payload, limit = 3)

    Surface(
        modifier = modifier.clickable(onClick = onOpen),
        color = BuyPilotColors.SurfaceCard,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 5.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border.copy(alpha = 0.76f)),
    ) {
        Column(Modifier.padding(bottom = 20.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(266.dp)
                    .background(BuyPilotColors.SurfaceMuted.copy(alpha = 0.72f), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                contentAlignment = Alignment.Center,
            ) {
                ProductImage(
                    product = product,
                    backendBaseUrl = backendBaseUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(244.dp),
                )
                PillLabel(
                    text = "#${payload.rank.takeIf { it > 0 } ?: 1}",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = product.brandLabel(),
                    color = BuyPilotColors.TextMuted,
                    fontSize = BuyPilotType.Label,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = product.displayName(),
                    color = BuyPilotColors.TextPrimary,
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.priceLabel(),
                        color = BuyPilotColors.Primary,
                        fontSize = 24.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { ProductMiniTag(it, active = it == tags.firstOrNull()) }
                }
            }
        }
    }
}

@Composable
internal fun DecisionSummaryCard(
    motionKey: String,
    payload: FinalDecisionPayload,
    productsById: Map<String, ProductCardPayload>,
    productDeckIdByProductId: Map<String, String>,
    sourceDeckNodeKey: String? = null,
    cartState: ChatCartUiState,
    backendBaseUrl: String,
    motionEnabled: Boolean,
    alreadyEntered: Boolean,
    onEntered: () -> Unit,
    onEvidence: () -> Unit,
    onProductDetailOpen: (String, String, String?) -> Unit,
    compareProducts: List<ProductCardPayload>,
    onCompareProducts: (List<Int>, List<String>?) -> Unit,
    onQuickAction: (QuickActionPayload) -> Unit,
) {
    val winnerProductId = payload.winnerProductId?.takeIf { it.isNotBlank() }
    val winner = winnerProductId?.let { winnerId -> productsById[winnerId] }
    val winnerDeckId = winnerProductId?.let { productDeckIdByProductId[it] }
    val openWinnerDetail = winnerDeckId?.let { deckId ->
        { onProductDetailOpen(deckId, winnerProductId.orEmpty(), sourceDeckNodeKey) }
    }
    val product = winner?.product
    val whyItems = payload.why
        .map { it.withoutMarkdownMarkup().withoutInternalDebugTokens().trim() }
        .filter { it.isNotBlank() }
        .take(3)
    val statusBadge = payload.decisionStatusBadge()
    val canAddWinnerToCart = winner != null && !winnerProductId.isNullOrBlank()
    val nextActions = payload.nextActions.filter { action ->
        action.label.isNotBlank() &&
            action.action != "view_cart" &&
            !(canAddWinnerToCart && action.action == "add_to_cart")
    }
    val winnerAlreadyInCart = winnerProductId?.let { id -> cartState.items.any { it.productId == id } } == true
    val winnerAddPending = winnerProductId?.let { it in cartState.pendingAddProductIds } == true
    val hasDecisionBody = winner != null ||
        !winnerProductId.isNullOrBlank() ||
        whyItems.isNotEmpty() ||
        statusBadge?.showCardWhenEmpty == true

    LaunchedEffect(motionKey, hasDecisionBody, alreadyEntered, motionEnabled) {
        if (!hasDecisionBody && !alreadyEntered) {
            if (motionEnabled) {
                kotlinx.coroutines.delay(420L)
            }
            onEntered()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DecisionSummaryIntro(
            motionKey = motionKey,
            summary = payload.decisionSummaryText(),
            motionEnabled = motionEnabled,
            alreadyEntered = alreadyEntered,
        )
        if (hasDecisionBody) {
            StructuredCardMotion(
                key = motionKey,
                motionEnabled = motionEnabled,
                alreadyEntered = alreadyEntered,
                durationMillis = DecisionCardEnterMs,
                initialOffsetY = 16.dp,
                initialScale = 0.925f,
                onEntered = onEntered,
            ) {
                Surface(
                    modifier = Modifier.shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = BuyPilotColors.ShadowNeutral.copy(alpha = 0.07f),
                        spotColor = Color.Black.copy(alpha = 0.04f),
                    ),
                    color = BuyPilotColors.SurfaceCard,
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 0.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border.copy(alpha = 0.72f)),
                ) {
                    Column {
                        DecisionRecommendationHeader(
                            label = if (winner != null) "首选推荐" else statusBadge?.label ?: "推荐结论",
                            accent = statusBadge?.accent ?: BuyPilotColors.PrimaryDark,
                            surface = statusBadge?.surface ?: BuyPilotColors.PrimarySoft.copy(alpha = 0.62f),
                        )
                        Column(
                            Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            if (winner == null && winnerProductId.isNullOrBlank()) {
                                DecisionStateMetaRow(payload = payload, accent = statusBadge?.accent ?: BuyPilotColors.TextSecondary)
                            }
                            if (winner != null || !winnerProductId.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .then(
                                            if (openWinnerDetail != null) {
                                                Modifier.clickable(role = Role.Button, onClick = openWinnerDetail)
                                            } else {
                                                Modifier
                                            },
                                        )
                                        .padding(2.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    if (product != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(88.dp)
                                                .background(BuyPilotColors.SurfaceImageAlt, RoundedCornerShape(20.dp))
                                                .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.48f), RoundedCornerShape(20.dp))
                                                .padding(6.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            ProductImage(
                                                product = product,
                                                backendBaseUrl = backendBaseUrl,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(16.dp)),
                                            )
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(5.dp),
                                    ) {
                                        product?.displayName("")?.takeIf { it.isNotBlank() }?.let { name ->
                                            Text(
                                                text = name,
                                                color = BuyPilotColors.TextPrimary,
                                                fontSize = 19.sp,
                                                lineHeight = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        winner?.reason?.takeIf { it.isNotBlank() }?.let { reason ->
                                            MarkdownTextBlock(
                                                content = reason,
                                                style = TextStyle(
                                                    color = BuyPilotColors.TextSecondary,
                                                    fontSize = BuyPilotType.Body,
                                                    lineHeight = 21.sp,
                                                ),
                                            )
                                        }
                                        product?.priceLabelOrNull()?.let { priceLabel ->
                                            Text(
                                                text = priceLabel,
                                                color = BuyPilotColors.Primary,
                                                fontSize = 21.sp,
                                                lineHeight = 25.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                    if (openWinnerDetail != null) {
                                        ProductDetailChevron(
                                            modifier = Modifier.padding(top = 8.dp),
                                        )
                                    }
                                }
                            }
                            if (whyItems.isNotEmpty()) {
                                SectionTitle("推荐理由", leading = "✓")
                                DecisionReasonList(
                                    items = whyItems,
                                    motionKey = motionKey,
                                    motionEnabled = motionEnabled,
                                    alreadyEntered = alreadyEntered,
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = onEvidence, contentPadding = PaddingValues(0.dp)) {
                                    Text("查看决策依据 ›", color = BuyPilotColors.Info, fontSize = BuyPilotType.Label)
                                }
                            }
                            if (canAddWinnerToCart) {
                                HorizontalDivider(color = BuyPilotColors.Border.copy(alpha = 0.62f))
                                DecisionCartActionBar(
                                    productId = winnerProductId.orEmpty(),
                                    pending = winnerAddPending,
                                    inCart = winnerAlreadyInCart,
                                    onAddToCart = {
                                        onQuickAction(
                                            QuickActionPayload(
                                                actionId = "add_to_cart_${winnerProductId.orEmpty()}",
                                                label = "加入购物车",
                                                action = "add_to_cart",
                                                productId = winnerProductId,
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        if (nextActions.isNotEmpty()) {
            ScrollableQuickActionRow(
                actions = nextActions,
                compareProducts = compareProducts,
                onCompareProducts = onCompareProducts,
                onQuickAction = onQuickAction,
            )
        }
    }
}

@Composable
private fun DecisionRecommendationHeader(
    label: String,
    accent: Color,
    surface: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        surface.copy(alpha = 0.78f),
                        BuyPilotColors.SurfaceCard,
                    ),
                ),
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(accent.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_favorite_24),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(15.dp),
                )
            }
            Text(
                text = label,
                color = accent,
                fontSize = BuyPilotType.LargeBody,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DecisionCartActionBar(
    productId: String,
    pending: Boolean,
    inCart: Boolean,
    onAddToCart: () -> Unit,
) {
    if (productId.isBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QuietCartActionButton(
            label = when {
                pending -> "加入中..."
                inCart -> "已加入"
                else -> "加入购物车"
            },
            iconRes = R.drawable.ic_shopping_bag_24,
            enabled = !pending && !inCart,
            emphasized = !inCart,
            modifier = Modifier.fillMaxWidth(),
            onClick = onAddToCart,
        )
    }
}

@Composable
private fun QuietCartActionButton(
    label: String,
    iconRes: Int,
    enabled: Boolean,
    emphasized: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val background = when {
        !enabled -> BuyPilotColors.SurfaceMuted.copy(alpha = 0.62f)
        emphasized -> BuyPilotColors.PrimarySoft.copy(alpha = 0.42f)
        else -> BuyPilotColors.SurfaceMuted.copy(alpha = 0.7f)
    }
    val contentColor = when {
        !enabled -> BuyPilotColors.TextMuted
        emphasized -> BuyPilotColors.PrimaryDark
        else -> BuyPilotColors.TextSecondary
    }
    Surface(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        color = background,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (emphasized) {
                BuyPilotColors.Primary.copy(alpha = 0.16f)
            } else {
                BuyPilotColors.Border.copy(alpha = 0.68f)
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = BuyPilotType.Label,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DecisionSummaryIntro(
    motionKey: String,
    summary: String,
    motionEnabled: Boolean,
    alreadyEntered: Boolean,
) {
    if (summary.isBlank()) return
    val shouldAnimate = motionEnabled && !alreadyEntered
    val progress = remember(motionKey) {
        Animatable(if (shouldAnimate) 0f else 1f)
    }
    LaunchedEffect(motionKey, shouldAnimate) {
        if (shouldAnimate) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 420,
                    easing = PremiumRevealEase,
                ),
            )
        } else {
            progress.snapTo(1f)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .graphicsLayer {
                    val lineProgress = segmentProgress(progress.value, 0f, 0.72f)
                    alpha = lineProgress
                    scaleX = lineProgress
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
                .background(BuyPilotColors.Border.copy(alpha = 0.72f), CircleShape),
        )
        Box(
            modifier = Modifier.graphicsLayer {
                val textProgress = segmentProgress(progress.value, 0.24f, 1f)
                alpha = textProgress
                translationY = (1f - textProgress) * 6f
            },
        ) {
            AssistantText(summary)
        }
    }
}

private fun FinalDecisionPayload.decisionSummaryText(): String {
    val summary = this.summary.withoutInternalDebugTokens().trim()
    return summary.withoutLeadingMarkdownDivider()
}

private fun String.withoutLeadingMarkdownDivider(): String =
    replaceFirst(Regex("""^\s*(?:-{3,}|\*{3,}|_{3,})\s*"""), "").trimStart()

private fun List<String>.toDecisionNotForItems(productsById: Map<String, ProductCardPayload>): List<String> =
    mapNotNull { raw ->
        val clean = raw.withoutMarkdownMarkup().withoutInternalDebugTokens().trim()
        if (clean.isBlank()) return@mapNotNull null
        productsById[clean]?.product?.displayName("")?.takeIf(String::isNotBlank) ?: clean
    }
        .filterNot { it.looksLikeQuestionAnswerEvidenceForDecision() }
        .distinct()
        .take(4)

private fun ProductSwipeState?.toDecisionFeedbackTrace(productsById: Map<String, ProductCardPayload>): DecisionFeedbackTraceUi? {
    val actions = this?.undoStack.orEmpty()
        .filter { action ->
            action.feedbackType == "like" ||
                action.feedbackType == "not_interested" ||
                action.action == "like" ||
                action.action == "not_interested"
        }
        .distinctBy { "${it.productId}_${it.feedbackType}_${it.action}" }
    if (actions.isEmpty()) return null
    fun nameOf(productId: String): String =
        productsById[productId]?.product?.displayName("")?.takeIf(String::isNotBlank)
            ?: productId.takeUnless { it.startsWith("p_", ignoreCase = true) || it.startsWith("pg_", ignoreCase = true) }
            ?: "候选商品"
    val preferred = actions
        .filter { it.feedbackType == "like" || it.action == "like" }
        .map { nameOf(it.productId) }
        .distinct()
        .take(3)
    val excluded = actions
        .filter { it.feedbackType == "not_interested" || it.action == "not_interested" }
        .map { nameOf(it.productId) }
        .distinct()
        .take(3)
    if (preferred.isEmpty() && excluded.isEmpty()) return null
    return DecisionFeedbackTraceUi(preferredNames = preferred, excludedNames = excluded)
}

private fun Map<String, Double>.toDecisionScoreItems(): List<DecisionScoreItemUi> =
    entries
        .mapNotNull { (key, value) ->
            val label = key.userFacingScoreLabel().takeIf(String::isNotBlank) ?: return@mapNotNull null
            if (value.isNaN() || value.isInfinite()) return@mapNotNull null
            DecisionScoreItemUi(label = label, value = value)
        }
        .sortedBy { item ->
            DecisionScoreOrder.indexOf(item.label).let { if (it == -1) Int.MAX_VALUE else it }
        }
        .take(6)

private val DecisionScoreOrder = listOf("检索相关", "标准匹配", "用户偏好", "证据质量", "风险扣分", "综合得分")

private fun String.userFacingScoreLabel(): String =
    when (withoutMarkdownMarkup().withoutInternalDebugTokens().trim().lowercase()) {
        "retrieval", "retrieval_score", "relevance", "retrieval_relevance" -> "检索相关"
        "criteria", "criteria_match", "constraint_match", "match_score" -> "标准匹配"
        "preference", "user_preference", "feedback", "feedback_score" -> "用户偏好"
        "evidence", "evidence_quality", "evidence_score" -> "证据质量"
        "risk", "risk_penalty", "risk_score" -> "风险扣分"
        "overall", "total", "final", "final_score", "score" -> "综合得分"
        else -> withoutMarkdownMarkup()
            .withoutInternalDebugTokens()
            .trim()
            .replace('_', ' ')
            .takeIf { it.any(Char::isCjk) }
            ?: ""
    }

private fun Double.scoreFraction(): Float {
    return normalizedScore().coerceIn(0.04, 1.0).toFloat()
}

private fun Double.scoreLabel(): String =
    "${(normalizedScore() * 100).roundToInt()}%"

private fun Double.normalizedScore(): Double =
    when {
        this <= 1.0 -> this
        this <= 10.0 -> this / 10.0
        else -> this / 100.0
    }
        .coerceIn(0.0, 1.0)

private val DecisionQaEvidenceRegex = Regex("""(?:^|\s)(?:Q|问|问题)\s*[:：].*(?:A|答|回答)\s*[:：]""", RegexOption.IGNORE_CASE)
private val DecisionQuestionOnlyEvidenceRegex = Regex("""^\s*(?:Q|问|问题)\s*[:：]""", RegexOption.IGNORE_CASE)

private fun String.looksLikeQuestionAnswerEvidenceForDecision(): Boolean =
    DecisionQaEvidenceRegex.containsMatchIn(this) || DecisionQuestionOnlyEvidenceRegex.containsMatchIn(this)

@Composable
private fun DecisionStateMetaRow(
    payload: FinalDecisionPayload,
    accent: Color,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        payload.confidence?.takeIf { it.isNotBlank() }?.let {
            DecisionMetaPill("置信度：${it.confidenceLabel()}", accent)
        }
        payload.nextStep?.takeIf { it.isNotBlank() }?.let {
            DecisionMetaPill(it.nextStepLabel(), BuyPilotColors.TextSecondary)
        }
    }
}

@Composable
private fun DecisionMetaPill(
    label: String,
    color: Color,
) {
    Text(
        text = label,
        color = color,
        fontSize = BuyPilotType.Tiny,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .background(color.copy(alpha = 0.08f), CircleShape)
            .border(1.dp, color.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 9.dp, vertical = 5.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductHeroDetailScreen(
    state: ChatUiState,
    deckId: String,
    deckNodeKey: String? = null,
    productId: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onOpenEvidence: (String, String, String?) -> Unit,
    onSwipe: (String, String, String, String, String?) -> Unit,
    onAddToCart: (String) -> Unit = {},
    onRequestProductDetail: (String) -> Unit = {},
) {
    var activeProductId by rememberSaveable(deckId, deckNodeKey, productId) { mutableStateOf(productId) }
    val payload = state.findProduct(deckId, activeProductId, deckNodeKey)
    val haptics = LocalHapticFeedback.current
    val latestOnBack by rememberUpdatedState(onBack)
    var submittedFeedback by rememberSaveable(deckId, deckNodeKey, activeProductId) { mutableStateOf<String?>(null) }
    var pendingAdvanceProductId by rememberSaveable(deckId, deckNodeKey, activeProductId) { mutableStateOf<String?>(null) }
    val productInSourceDeck = state.isProductInDeck(deckId, activeProductId, deckNodeKey)
    val handledProductIds = state.productSwipeStates[deckId]?.swipedProductIds.orEmpty().toSet()
    val deckCanConverge = state.canOpenDeckForConvergence(deckId, deckNodeKey)
    val readOnlyByState = !productInSourceDeck ||
        !deckCanConverge ||
        state.hasConvergedDecisionForDeck(deckId, deckNodeKey) ||
        activeProductId in handledProductIds
    var choiceActionsAvailable by rememberSaveable(deckId, deckNodeKey, activeProductId) {
        mutableStateOf(!readOnlyByState)
    }
    val latestDeckProducts = state.findProductDeck(deckId, deckNodeKey)?.products.orEmpty()
    val nextUnhandledProductId = if (productInSourceDeck) {
        latestDeckProducts
            .map { it.product.productId }
            .firstOrNull { id ->
                id.isNotBlank() && id != activeProductId && id !in handledProductIds
            }
    } else {
        null
    }
    val deckWillBeFullyHandledAfterCurrentChoice = if (productInSourceDeck) {
        latestDeckProducts
            .mapNotNull { it.product.productId.takeIf(String::isNotBlank) }
            .let { productIds ->
                productIds.size >= 2 &&
                    activeProductId.isNotBlank() &&
                    productIds.all { id -> id == activeProductId || id in handledProductIds }
            }
    } else {
        false
    }
    var completionRequested by rememberSaveable(deckId, deckNodeKey) { mutableStateOf(false) }

    LaunchedEffect(readOnlyByState, submittedFeedback) {
        if (readOnlyByState && submittedFeedback == null) {
            choiceActionsAvailable = false
        }
    }

    if (payload == null) {
        Surface(color = BuyPilotColors.SurfaceBg, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                ProductPageTopBar(title = "商品详情", onBack = onBack)
                ExpiredRecommendationState(onBack = onBack)
            }
        }
        return
    }

    val productDetail = state.productDetails[activeProductId]
    val product = productDetail.productOrFallback(payload.product)
    val productAlreadyInCart = state.cartState.items.any { it.productId == activeProductId }
    val productAddPending = activeProductId in state.cartState.pendingAddProductIds
    val detailListState = rememberLazyListState()
    val density = LocalDensity.current
    val revealDistancePx = with(density) { ProductDetailRevealDistance.toPx() }
    val scrollProgress by remember {
        derivedStateOf {
            val offset = detailListState.firstVisibleItemScrollOffset.toFloat()
            (offset / revealDistancePx).coerceIn(0f, 1f)
        }
    }
    val choiceActionsActive = choiceActionsAvailable && submittedFeedback == null
    val showCommerceControls = !choiceActionsAvailable
    val showInlineCartAction = activeProductId.isNotBlank() &&
        showCommerceControls &&
        readOnlyByState
    val actionAlpha by animateFloatAsState(
        targetValue = if (choiceActionsActive && scrollProgress < 0.38f) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = PremiumRevealEase),
        label = "product_detail_action_alpha",
    )
    val actionBottomPadding by animateDpAsState(
        targetValue = when {
            choiceActionsActive -> 126.dp
            else -> 48.dp
        },
        animationSpec = tween(durationMillis = 260, easing = PremiumRevealEase),
        label = "product_detail_action_bottom_padding",
    )
    val actionsVisible = choiceActionsAvailable && actionAlpha > 0.05f
    val actionsEnabled = actionsVisible && submittedFeedback == null
    val swipeMaxDistancePx = with(density) { 132.dp.toPx() }
    val swipeThresholdPx = with(density) { 74.dp.toPx() }
    val swipeThrowDistancePx = with(density) { 430.dp.toPx() }
    var gestureOffsetPx by remember(activeProductId) { mutableFloatStateOf(0f) }
    val throwing = submittedFeedback != null && abs(gestureOffsetPx) > swipeMaxDistancePx
    val animatedGestureOffsetPx by animateFloatAsState(
        targetValue = gestureOffsetPx,
        animationSpec = tween(
            durationMillis = if (throwing) 360 else 190,
            easing = if (throwing) MenuEaseIn else PremiumRevealEase,
        ),
        label = "product_detail_swipe_offset",
    )
    val gestureProgress = (abs(animatedGestureOffsetPx) / swipeThrowDistancePx).coerceIn(0f, 1f)
    val submitChoice: (String) -> Unit = { feedback ->
        if (submittedFeedback == null && choiceActionsAvailable) {
            pendingAdvanceProductId = nextUnhandledProductId
            gestureOffsetPx = if (feedback == "like") swipeThrowDistancePx else -swipeThrowDistancePx
            submittedFeedback = feedback
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onSwipe(
                deckId,
                activeProductId,
                feedback,
                feedback,
                if (feedback == "like") "用户在商品详情页标记感兴趣" else "用户在商品详情页标记不感兴趣",
            )
        }
    }
    val routeProgressState = rememberRouteEnterProgress(
        key = "detail_${deckId}_${deckNodeKey.orEmpty()}_${activeProductId}",
        durationMillis = ProductDetailEnterMs,
    )

    LaunchedEffect(product.productId) {
        onRequestProductDetail(product.productId)
    }

    LaunchedEffect(product.productId) {
        snapshotFlow { detailListState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { isScrolling ->
                if (isScrolling) return@collect

                val currentOffset = detailListState.firstVisibleItemScrollOffset.toFloat()
                val isTransitionRange = currentOffset > 8f && currentOffset < revealDistancePx - 8f
                if (!isTransitionRange) return@collect

                val targetOffset = if (scrollProgress >= ProductDetailSnapThreshold) revealDistancePx else 0f
                detailListState.animateScrollBy(
                    value = targetOffset - currentOffset,
                    animationSpec = tween(durationMillis = 280, easing = MenuEaseOut),
                )
        }
    }

    LaunchedEffect(submittedFeedback) {
        if (submittedFeedback != null) {
            kotlinx.coroutines.delay(360L)
            val nextProductId = pendingAdvanceProductId
            if (!nextProductId.isNullOrBlank()) {
                activeProductId = nextProductId
                pendingAdvanceProductId = null
                submittedFeedback = null
                detailListState.scrollToItem(0)
            } else if (deckWillBeFullyHandledAfterCurrentChoice && deckCanConverge && !completionRequested) {
                completionRequested = true
                latestOnBack()
            } else {
                latestOnBack()
            }
            gestureOffsetPx = 0f
        }
    }

    val swipeGestureModifier = if (choiceActionsAvailable) {
        Modifier.pointerInput(activeProductId, actionsEnabled) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    when {
                        gestureOffsetPx > swipeThresholdPx -> submitChoice("like")
                        gestureOffsetPx < -swipeThresholdPx -> submitChoice("not_interested")
                        else -> gestureOffsetPx = 0f
                    }
                },
                onDragCancel = {
                    gestureOffsetPx = 0f
                },
                onHorizontalDrag = { _, dragAmount ->
                    if (actionsEnabled) {
                        gestureOffsetPx = (gestureOffsetPx + dragAmount)
                            .coerceIn(-swipeMaxDistancePx, swipeMaxDistancePx)
                    }
                },
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(swipeGestureModifier),
    ) {
        ProductCinematicBackdrop(
            product = product,
            backendBaseUrl = state.backendBaseUrl,
            progress = scrollProgress,
            enterProgress = { segmentProgress(routeProgressState.value, 0f, 0.82f) },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = animatedGestureOffsetPx * 0.2f
                    alpha = 1f - gestureProgress * 0.18f
                },
        )
        ImmersiveCircleButton(
            iconRes = R.drawable.ic_arrow_back_24,
            contentDescription = "返回",
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 8.dp)
                .graphicsLayer {
                    val chromeEnter = segmentProgress(routeProgressState.value, 0.28f, 1f)
                    alpha = chromeEnter
                    translationY = (1f - chromeEnter) * -18f
                    scaleX = lerp(0.92f, 1f, chromeEnter)
                    scaleY = lerp(0.92f, 1f, chromeEnter)
                }
                .zIndex(2f),
        )
        ImmersiveCircleButton(
            iconRes = R.drawable.ic_article_24,
            contentDescription = "推荐证据",
            onClick = { onOpenEvidence(deckId, activeProductId, deckNodeKey) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 16.dp, top = 8.dp)
                .graphicsLayer {
                    val chromeEnter = segmentProgress(routeProgressState.value, 0.28f, 1f)
                    alpha = chromeEnter
                    translationY = (1f - chromeEnter) * -18f
                    scaleX = lerp(0.92f, 1f, chromeEnter)
                    scaleY = lerp(0.92f, 1f, chromeEnter)
                }
                .zIndex(2f),
        )
        LazyColumn(
            state = detailListState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val contentEnter = segmentProgress(routeProgressState.value, 0.18f, 1f)
                    alpha = contentEnter * (1f - gestureProgress * 0.42f)
                    translationX = animatedGestureOffsetPx
                    translationY = (1f - contentEnter) * 42f
                    rotationZ = (animatedGestureOffsetPx / swipeThrowDistancePx) * 11f
                    scaleX = lerp(0.975f, 1f, contentEnter)
                    scaleY = lerp(0.975f, 1f, contentEnter)
                },
            contentPadding = PaddingValues(
                start = 22.dp,
                end = 22.dp,
                top = 418.dp,
                bottom = actionBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item("detail_panel") {
                CinematicProductDetailPanel(
                    payload = payload,
                    detail = productDetail,
                    progress = scrollProgress,
                    showCommerceControls = showCommerceControls,
                    cartAction = if (showInlineCartAction) {
                        {
                            ProductDetailInlineCartAction(
                                pending = productAddPending,
                                inCart = productAlreadyInCart,
                                onAddToCart = { onAddToCart(activeProductId) },
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
        if (choiceActionsAvailable && (submittedFeedback == null || actionAlpha > 0.01f)) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .alpha(actionAlpha)
                    .graphicsLayer {
                        val chromeEnter = segmentProgress(routeProgressState.value, 0.28f, 1f)
                        val actionProgress = minOf(chromeEnter, actionAlpha)
                        translationY = (1f - actionProgress) * 26f
                        scaleX = lerp(0.96f, 1f, actionProgress)
                        scaleY = lerp(0.96f, 1f, actionProgress)
                    }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                BuyPilotColors.SurfaceBg.copy(alpha = 0.88f),
                                BuyPilotColors.SurfaceBg,
                            ),
                        ),
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 34.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SwipeRoundButton(
                        iconRes = R.drawable.ic_close_24,
                        contentDescription = "不感兴趣",
                        active = false,
                        enabled = actionsEnabled,
                        onClick = {
                            submitChoice("not_interested")
                        },
                    )
                    Spacer(Modifier.width(58.dp))
                    SwipeRoundButton(
                        iconRes = R.drawable.ic_favorite_24,
                        contentDescription = "感兴趣",
                        active = true,
                        enabled = actionsEnabled,
                        onClick = {
                            submitChoice("like")
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductCinematicBackdrop(
    product: ProductPayload,
    backendBaseUrl: String,
    progress: Float,
    enterProgress: () -> Float,
    modifier: Modifier = Modifier,
) {
    val imageAlpha = lerp(0.94f, 0.56f, progress)
    Box(modifier = modifier.background(BuyPilotColors.SurfaceBg)) {
        ProductImage(
            product = product,
            backendBaseUrl = backendBaseUrl,
            modifier = Modifier
                .fillMaxWidth()
                .height(560.dp)
                .align(Alignment.TopCenter)
                .padding(top = 86.dp, start = 20.dp, end = 20.dp)
                .graphicsLayer {
                    val enter = enterProgress()
                    val scale = lerp(0.92f, lerp(1.02f, 1.08f, progress), enter)
                    val offsetY = lerp(42f, lerp(0f, -54f, progress), enter)
                    scaleX = scale
                    scaleY = scale
                    translationY = offsetY
                    alpha = imageAlpha * lerp(0.42f, 1f, enter)
                }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BuyPilotColors.SurfaceBg.copy(alpha = 0.02f),
                            BuyPilotColors.SurfaceBg.copy(alpha = lerp(0.04f, 0.28f, progress)),
                            BuyPilotColors.SurfaceBg.copy(alpha = lerp(0.9f, 1f, progress)),
                        ),
                        startY = 0f,
                        endY = 1180f,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            BuyPilotColors.SurfaceBg.copy(alpha = 0.9f),
                            BuyPilotColors.SurfaceBg,
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun ProductImmersiveBackdrop(
    product: ProductPayload,
    backendBaseUrl: String,
    dimAlpha: Float,
    enterProgress: Float,
) {
    Box(Modifier.fillMaxSize()) {
        ProductImage(
            product = product,
            backendBaseUrl = backendBaseUrl,
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
                .align(Alignment.TopCenter)
                .padding(top = 72.dp, start = 20.dp, end = 20.dp)
                .graphicsLayer {
                    scaleX = lerp(0.96f, 1.04f, enterProgress)
                    scaleY = lerp(0.96f, 1.04f, enterProgress)
                    translationY = (1f - enterProgress) * 24f
                    alpha = lerp(0.42f, 0.82f, enterProgress)
                },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BuyPilotColors.SurfaceBg.copy(alpha = lerp(0.16f, 0.2f, enterProgress)),
                            BuyPilotColors.SurfaceBg.copy(alpha = lerp(0.72f, dimAlpha, enterProgress)),
                            BuyPilotColors.SurfaceBg,
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun ImmersiveCircleButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .shadow(8.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.12f))
            .background(BuyPilotColors.SurfaceCard.copy(alpha = 0.92f), CircleShape)
            .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.72f), CircleShape),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = BuyPilotColors.TextPrimary,
        ),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun CinematicProductDetailPanel(
    payload: ProductCardPayload,
    detail: ProductDetailResponse?,
    progress: Float,
    showCommerceControls: Boolean = true,
    cartAction: (@Composable () -> Unit)? = null,
) {
    val product = detail.productOrFallback(payload.product)
    val tags = product.detailTags().take(4)
    val highlights = detail
        ?.highlights
        ?.map(String::trim)
        ?.filter(String::isNotBlank)
        .orEmpty()
    val attributeRows = product.detailAttributeRows(includeSkuSummary = showCommerceControls)
    val description = detail?.marketingDescription?.trim()?.takeIf(String::isNotBlank)
    val faqs = detail
        ?.faqs
        ?.filter { it.question.isNotBlank() && it.answer.isNotBlank() }
        ?.takeIf { it.isNotEmpty() }
    val reviews = detail
        ?.reviews
        ?.filter { it.nickname.isNotBlank() || it.content.isNotBlank() }
        ?.takeIf { it.isNotEmpty() }
    val hasBelowDividerContent = cartAction != null ||
        attributeRows.isNotEmpty() ||
        description != null ||
        faqs != null ||
        reviews != null
    val skuOptions = remember(product.skuOptions) { product.skuOptions.toSkuUiOptions() }
    var selectedSkuIndex by rememberSaveable(product.productId, skuOptions.size) { mutableIntStateOf(0) }
    val selectedSku = skuOptions.getOrNull(selectedSkuIndex)
    val priceLabel = selectedSku?.price?.let { "¥${it.clean()}" } ?: product.priceLabel()
    val shape = RoundedCornerShape(
        topStart = lerp(30f, 22f, progress).dp,
        topEnd = lerp(30f, 22f, progress).dp,
        bottomStart = lerp(28f, 20f, progress).dp,
        bottomEnd = lerp(28f, 20f, progress).dp,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = lerp(8f, 3f, progress).dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.1f),
            )
            .clip(shape)
            .background(BuyPilotColors.SurfaceCard.copy(alpha = lerp(0.96f, 1f, progress)), shape)
            .border(1.dp, BuyPilotColors.Border.copy(alpha = lerp(0.72f, 1f, progress)), shape)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        DetailEyebrowRow(
            label = product.subCategory?.takeIf(String::isNotBlank)
                ?: product.category.takeIf(String::isNotBlank)
                ?: "商品详情",
            rank = payload.rank,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = product.displayName(),
                color = BuyPilotColors.TextPrimary,
                fontSize = 27.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = priceLabel,
                    color = BuyPilotColors.Primary,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.animateContentSize(tween(durationMillis = 200, easing = PremiumRevealEase)),
                )
                Text(
                    text = product.subCategory ?: product.category,
                    color = BuyPilotColors.TextMuted,
                    fontSize = BuyPilotType.Label,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = product.brandLabel(),
                color = BuyPilotColors.TextSecondary,
                fontSize = BuyPilotType.Body,
                lineHeight = 21.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (tags.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tags, key = { it }) { tag ->
                    ImmersiveTag(label = tag)
                }
            }
        }
        if (showCommerceControls && skuOptions.isNotEmpty()) {
            ProductSkuSelector(
                options = skuOptions,
                selectedIndex = selectedSkuIndex.coerceIn(0, skuOptions.lastIndex),
                onSelected = { selectedSkuIndex = it },
            )
        }
        if (highlights.isNotEmpty()) {
            ProductHighlightsSection(highlights = highlights)
        }
        if (hasBelowDividerContent) {
            Spacer(Modifier.height(20.dp))
            CinematicDetailDivider(progress)
            cartAction?.invoke()
        }
        if (attributeRows.isNotEmpty()) {
            ImmersiveSectionTitle("商品信息")
            ProductAttributeRowsDark(rows = attributeRows)
        }
        description?.let { ProductDescriptionSection(description = it) }
        faqs?.let { ProductFaqSection(faqs = it) }
        reviews?.let { ProductReviewsSection(reviews = it) }
        Spacer(Modifier.height(14.dp))
    }
}

@Immutable
internal data class ProductSkuUiOption(
    val label: String,
    val price: Double?,
)

@Composable
private fun DetailEyebrowRow(label: String, rank: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.withoutMarkdownMarkup(),
            color = BuyPilotColors.Primary,
            fontSize = BuyPilotType.Label,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (rank > 0) {
            Text(
                text = "#$rank",
                color = BuyPilotColors.TextMuted,
                fontSize = BuyPilotType.Tiny,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(BuyPilotColors.SurfaceMuted.copy(alpha = 0.7f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun ProductSkuSelector(
    options: List<ProductSkuUiOption>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ImmersiveSectionTitle("规格")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(options, key = { index, option -> "${option.label}_$index" }) { index, option ->
                val selected = index == selectedIndex
                Surface(
                    onClick = { onSelected(index) },
                    color = if (selected) BuyPilotColors.Primary.copy(alpha = 0.08f) else BuyPilotColors.SurfaceCard,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        if (selected) 1.5.dp else 1.dp,
                        if (selected) BuyPilotColors.Primary.copy(alpha = 0.72f) else BuyPilotColors.Border,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = option.label,
                            color = if (selected) BuyPilotColors.TextPrimary else BuyPilotColors.TextSecondary,
                            fontSize = BuyPilotType.Label,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        option.price?.let { price ->
                            Text(
                                text = "¥${price.clean()}",
                                color = if (selected) BuyPilotColors.Primary else BuyPilotColors.TextSecondary,
                                fontSize = BuyPilotType.Label,
                                lineHeight = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductHighlightsSection(highlights: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ImmersiveSectionTitle("商品亮点")
        highlights.take(4).forEach { highlight ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(6.dp)
                        .background(BuyPilotColors.Primary.copy(alpha = 0.74f), CircleShape),
                )
                Text(
                    text = highlight,
                    color = BuyPilotColors.TextSecondary,
                    fontSize = BuyPilotType.Body,
                    lineHeight = 21.sp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ProductDescriptionSection(description: String) {
    var expanded by rememberSaveable(description) { mutableStateOf(false) }
    var hasOverflow by remember(description) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ImmersiveSectionTitle("商品描述")
        Column(Modifier.animateContentSize(tween(durationMillis = 260, easing = PremiumRevealEase))) {
            Text(
                text = description,
                color = BuyPilotColors.TextSecondary,
                fontSize = BuyPilotType.Body,
                lineHeight = 23.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { result ->
                    if (!expanded) {
                        hasOverflow = result.hasVisualOverflow
                    }
                },
            )
            if (hasOverflow) {
                TextButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = if (expanded) "收起" else "展开全文",
                        color = BuyPilotColors.PrimaryDark,
                        fontSize = BuyPilotType.Label,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductFaqSection(faqs: List<FaqItem>) {
    var selectedIndex by rememberSaveable(faqs.size) { mutableIntStateOf(-1) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ImmersiveSectionTitle("官方问答")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
        ) {
            itemsIndexed(faqs.take(8), key = { index, faq -> "${faq.question}_$index" }) { index, faq ->
                ProductFaqPreviewCard(
                    faq = faq,
                    onClick = { selectedIndex = index },
                )
            }
        }
    }
    selectedIndex
        .takeIf { it in faqs.indices }
        ?.let { index ->
            val faq = faqs[index]
            ProductTextPreviewDialog(
                title = faq.question,
                eyebrow = "官方问答",
                body = faq.answer,
                onDismiss = { selectedIndex = -1 },
            )
        }
}

@Composable
private fun ProductFaqPreviewCard(
    faq: FaqItem,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(282.dp)
            .heightIn(min = 132.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick),
        color = BuyPilotColors.SurfaceCard,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border.copy(alpha = 0.74f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(
                text = faq.question,
                color = BuyPilotColors.TextPrimary,
                fontSize = BuyPilotType.Body,
                lineHeight = 21.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            faq.answer.takeIf { it.isNotBlank() }?.let { answer ->
                Text(
                    text = answer,
                    color = BuyPilotColors.TextMuted,
                    fontSize = BuyPilotType.Label,
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "查看完整",
                color = BuyPilotColors.Info,
                fontSize = BuyPilotType.Label,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ProductReviewsSection(reviews: List<ReviewItem>) {
    var selectedIndex by rememberSaveable(reviews.size) { mutableIntStateOf(-1) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ImmersiveSectionTitle("用户评价 (${reviews.size})")
        RatingDistributionBar(reviews)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
        ) {
            itemsIndexed(reviews.take(8), key = { index, review -> "${review.nickname}_${review.rating}_$index" }) { index, review ->
                ProductReviewPreviewCard(
                    review = review,
                    onClick = { selectedIndex = index },
                )
            }
        }
    }
    selectedIndex
        .takeIf { it in reviews.indices }
        ?.let { index ->
            val review = reviews[index]
            ProductTextPreviewDialog(
                title = review.nickname.ifBlank { "用户评价" },
                eyebrow = "用户评价",
                body = review.content,
                rating = review.rating,
                onDismiss = { selectedIndex = -1 },
            )
        }
}

@Composable
private fun ProductReviewPreviewCard(
    review: ReviewItem,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(260.dp)
            .heightIn(min = 132.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick),
        color = BuyPilotColors.SurfaceCard,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border.copy(alpha = 0.74f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = review.nickname.ifBlank { "用户评价" },
                    color = BuyPilotColors.TextMuted,
                    fontSize = BuyPilotType.Label,
                    lineHeight = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                RatingStars(rating = review.rating)
            }
            Text(
                text = review.content,
                color = BuyPilotColors.TextSecondary,
                fontSize = BuyPilotType.Body,
                lineHeight = 22.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "查看完整",
                color = BuyPilotColors.Info,
                fontSize = BuyPilotType.Label,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ProductTextPreviewDialog(
    title: String,
    eyebrow: String,
    body: String,
    rating: Int? = null,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            color = BuyPilotColors.SurfaceCard,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 12.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border.copy(alpha = 0.64f)),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = eyebrow,
                        color = BuyPilotColors.PrimaryDark,
                        fontSize = BuyPilotType.Label,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(BuyPilotColors.PrimarySoft.copy(alpha = 0.72f), CircleShape)
                            .padding(horizontal = 9.dp, vertical = 5.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    rating?.let { RatingStars(rating = it) }
                }
                Text(
                    text = title,
                    color = BuyPilotColors.TextPrimary,
                    fontSize = BuyPilotType.Title,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = body,
                    color = BuyPilotColors.TextSecondary,
                    fontSize = BuyPilotType.Body,
                    lineHeight = 23.sp,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}

@Composable
private fun RatingDistributionBar(reviews: List<ReviewItem>) {
    val total = reviews.size.coerceAtLeast(1)
    val counts = remember(reviews) {
        (5 downTo 1).associateWith { rating ->
            reviews.count { it.rating.coerceIn(1, 5) == rating }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        counts.forEach { (rating, count) ->
            val fraction = count.toFloat() / total.toFloat()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${rating}星",
                    color = BuyPilotColors.TextMuted,
                    fontSize = BuyPilotType.Tiny,
                    lineHeight = 14.sp,
                    modifier = Modifier.width(32.dp),
                )
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(BuyPilotColors.SurfaceMuted),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(maxWidth * fraction)
                            .background(BuyPilotColors.Primary.copy(alpha = 0.68f), CircleShape),
                    )
                }
                Text(
                    text = count.toString(),
                    color = BuyPilotColors.TextMuted,
                    fontSize = BuyPilotType.Tiny,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(24.dp),
                )
            }
        }
    }
}

@Composable
private fun RatingStars(rating: Int) {
    val safeRating = rating.coerceIn(0, 5)
    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(5) { index ->
            Text(
                text = if (index < safeRating) "★" else "☆",
                color = if (index < safeRating) BuyPilotColors.Warning else BuyPilotColors.TextMuted.copy(alpha = 0.52f),
                fontSize = 13.sp,
                lineHeight = 14.sp,
            )
        }
    }
}

@Composable
private fun ProductDetailInlineCartAction(
    pending: Boolean,
    inCart: Boolean,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = !pending && !inCart
    val label = when {
        pending -> "加入中..."
        inCart -> "已加入"
        else -> "加入购物车"
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.985f else 1f,
        animationSpec = tween(durationMillis = 150, easing = PremiumRevealEase),
        label = "product_detail_cart_press",
    )
    val enterProgressState = rememberRouteEnterProgress(
        key = "product_detail_inline_cart_${label}_${pending}_${inCart}",
        durationMillis = 360,
        delayMillis = 70,
    )
    val contentColor = when {
        pending -> BuyPilotColors.TextMuted
        inCart -> BuyPilotColors.Success
        else -> BuyPilotColors.PrimaryDark
    }
    val background = when {
        pending -> BuyPilotColors.SurfaceMuted.copy(alpha = 0.58f)
        inCart -> BuyPilotColors.SuccessSoft
        else -> BuyPilotColors.PrimarySoft.copy(alpha = 0.34f)
    }
    val borderColor = when {
        pending -> BuyPilotColors.Border.copy(alpha = 0.72f)
        inCart -> BuyPilotColors.Success.copy(alpha = 0.22f)
        else -> BuyPilotColors.Primary.copy(alpha = 0.18f)
    }
    val shape = RoundedCornerShape(18.dp)

    Surface(
        onClick = onAddToCart,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer {
                val enterProgress = enterProgressState.value
                alpha = enterProgress
                translationY = (1f - enterProgress) * 8.dp.toPx()
                scaleX = pressScale * lerp(0.992f, 1f, enterProgress)
                scaleY = pressScale * lerp(0.992f, 1f, enterProgress)
            },
        color = background,
        shape = shape,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(BuyPilotColors.SurfaceCard.copy(alpha = 0.72f), CircleShape)
                    .border(1.dp, borderColor.copy(alpha = 0.82f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_shopping_bag_24),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(17.dp),
                )
            }
            Spacer(Modifier.width(11.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = BuyPilotType.Body,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .width(30.dp)
                    .height(20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (!inCart && !pending) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right_20),
                        contentDescription = null,
                        tint = BuyPilotColors.Primary.copy(alpha = 0.58f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CinematicDetailDivider(progress: Float) {
    val widthFraction = lerp(0.22f, 1f, progress)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .height(1.dp)
                .background(BuyPilotColors.Border.copy(alpha = 0.92f), CircleShape),
        )
    }
}

@Composable
private fun ImmersiveSectionTitle(title: String) {
    Text(
        text = title,
        color = BuyPilotColors.TextPrimary,
        fontSize = BuyPilotType.Title,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun ImmersiveTag(label: String) {
    Text(
        text = label.withoutMarkdownMarkup(),
        color = BuyPilotColors.PrimaryDark,
        fontSize = BuyPilotType.Label,
        lineHeight = 16.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .background(BuyPilotColors.PrimarySoft.copy(alpha = 0.72f), CircleShape)
            .border(1.dp, BuyPilotColors.PrimarySoft, CircleShape)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun ImmersiveWarningBox(text: String) {
    val warningMarkdown = text.trim()
        .removePrefix("注意：")
        .removePrefix("注意:")
        .trim()
    Surface(
        color = BuyPilotColors.Attention,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.PrimarySoft),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = "注意：",
                color = BuyPilotColors.PrimaryDark,
                fontSize = BuyPilotType.Body,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
            MarkdownTextBlock(
                content = warningMarkdown,
                modifier = Modifier.weight(1f),
                style = TextStyle(
                    color = BuyPilotColors.TextSecondary,
                    fontSize = BuyPilotType.Body,
                    lineHeight = 22.sp,
                ),
            )
        }
    }
}

@Composable
private fun ProductAttributeRowsDark(rows: List<Pair<String, String>>) {
    if (rows.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { (label, value) ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = label,
                    color = BuyPilotColors.TextMuted,
                    fontSize = BuyPilotType.Label,
                    lineHeight = 18.sp,
                    modifier = Modifier.width(74.dp),
                )
                Text(
                    text = value,
                    color = BuyPilotColors.TextPrimary,
                    fontSize = BuyPilotType.Body,
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun ProductPayload.detailTags(): List<String> =
    listOfNotNull(
        subCategory?.withoutMarkdownMarkup()?.withoutInternalDebugTokens()?.trim()?.takeIf(String::isNotBlank),
        category.withoutMarkdownMarkup().withoutInternalDebugTokens().trim().takeIf(String::isNotBlank),
    )
        .plus(useScenario)
        .plus(skinTypeMatch)
        .plus(ingredientTags)
        .map { it.withoutMarkdownMarkup().withoutInternalDebugTokens().trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .take(6)

private fun ProductPayload.detailAttributeRows(includeSkuSummary: Boolean = true): List<Pair<String, String>> {
    val rows = mutableListOf<Pair<String, String>>()
    brand
        ?.withoutMarkdownMarkup()
        ?.withoutInternalDebugTokens()
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?.let { rows += "品牌" to it }

    listOfNotNull(
        category.withoutMarkdownMarkup().withoutInternalDebugTokens().trim().takeIf(String::isNotBlank),
        subCategory?.withoutMarkdownMarkup()?.withoutInternalDebugTokens()?.trim()?.takeIf(String::isNotBlank),
    )
        .distinct()
        .joinToString(" / ")
        .takeIf(String::isNotBlank)
        ?.let { rows += "品类" to it }

    useScenario.userFacingJoinedOrFallback().takeIf(String::isNotBlank)?.let { rows += "使用场景" to it }
    skinTypeMatch.userFacingJoinedOrFallback().takeIf(String::isNotBlank)?.let { rows += "适用肤质" to it }
    ingredientTags.userFacingJoinedOrFallback().takeIf(String::isNotBlank)?.let { rows += "成分标签" to it }
    ingredientAvoid.userFacingJoinedOrFallback().takeIf(String::isNotBlank)?.let { rows += "避开成分" to it }
    if (includeSkuSummary) {
        skuOptions.skuPropertySummary().takeIf(String::isNotBlank)?.let { rows += "可选规格" to it }
    }

    return rows.distinctBy { (label, value) -> label to value }.take(7)
}

private fun List<JsonObject>?.skuPropertySummary(): String =
    this
        ?.asSequence()
        ?.mapNotNull { option ->
            option["properties"]
                ?.let { element -> runCatching { element.jsonObject }.getOrNull() }
                ?.values
                ?.mapNotNull { element ->
                    runCatching { element.jsonPrimitive.contentOrNull }.getOrNull()?.trim()?.takeIf(String::isNotBlank)
                }
                ?.joinToString(" / ")
                ?.takeIf(String::isNotBlank)
        }
        ?.distinct()
        ?.take(3)
        ?.joinToString("、")
        .orEmpty()

internal fun List<JsonObject>?.toSkuUiOptions(): List<ProductSkuUiOption> =
    this
        ?.mapNotNull { option ->
            val label = option.skuLabel()
            if (label.isBlank()) return@mapNotNull null
            ProductSkuUiOption(
                label = label,
                price = option["price"]?.let { element ->
                    runCatching { element.jsonPrimitive.doubleOrNull }.getOrNull()
                },
            )
        }
        .orEmpty()

private fun ProductDetailResponse?.productOrFallback(fallback: ProductPayload): ProductPayload =
    this
        ?.product
        ?.takeIf { it.productId.isNotBlank() }
        ?: fallback

internal fun JsonObject.skuLabel(): String {
    val properties = this["properties"]?.let { element ->
        runCatching { element.jsonObject }.getOrNull()
    } ?: return "默认规格"
    val values = properties.values
        .mapNotNull { element ->
            runCatching { element.jsonPrimitive.contentOrNull }.getOrNull()?.trim()?.takeIf(String::isNotBlank)
        }
    return values.joinToString(" · ").ifBlank { "默认规格" }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProductPageTopBar(
    title: String,
    onBack: () -> Unit,
    action: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BuyPilotColors.SurfaceCard)
            .statusBarsPadding(),
    ) {
        M3TopAppBarRow(
            title = title,
            titleCentered = true,
            navigationIcon = R.drawable.ic_arrow_back_24,
            navigationDescription = "返回",
            navigationTint = BuyPilotColors.TextPrimary.copy(alpha = 0.92f),
            onNavigationClick = onBack,
        )
        action()
        HorizontalDivider(
            thickness = 1.dp,
            color = BuyPilotColors.Border.copy(alpha = 0.46f),
        )
    }
}

@Composable
internal fun ExpiredRecommendationState(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "推荐已过期，请返回聊天重新生成",
            color = BuyPilotColors.TextPrimary,
            fontSize = BuyPilotType.Title,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onBack,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BuyPilotColors.Primary,
                contentColor = BuyPilotColors.OnPrimary,
            ),
        ) {
            Text("返回聊天")
        }
    }
}

@Composable
internal fun CartActionCard(
    payload: CartActionPayload,
    productsById: Map<String, ProductCardPayload>,
    onRetryAddToCart: (String) -> Unit,
) {
    val cart = payload.cart
    val productId = payload.productId.takeIf { it.isNotBlank() }
    val failed = payload.status == "failed"
    val cartItem = productId
        ?.let { id -> cart?.items?.firstOrNull { it.productId == id } }
        ?: if (failed) null else cart?.items?.firstOrNull()
    val product = cartItem?.product ?: productId?.let { productsById[it]?.product }
    val productName = cartItem?.name
        ?.withoutInternalDebugTokens()
        ?.takeIf { it.isNotBlank() }
        ?: product?.displayName("商品")
        ?: if (failed) "这件商品" else "商品"
    val title = when {
        failed -> "没有加成功"
        payload.action == "add" -> "已加入购物车"
        payload.action == "remove" -> "已移出购物车"
        payload.action == "update_quantity" -> "已更新数量"
        payload.action == "view" -> "购物车已同步"
        else -> "购物车已更新"
    }
    val itemLine = when {
        failed -> "${productName}还没有加入购物车"
        cartItem != null -> "$productName x${cartItem.quantity}"
        cart?.items.isNullOrEmpty() -> "购物车为空"
        else -> productName
    }
    val totalLine = cart
        ?.takeIf { it.totalItems > 0 }
        ?.let { "共${it.totalItems}件 · ¥${it.totalPrice.clean()}" }
    val retryableAdd = failed && payload.action == "add" && !productId.isNullOrBlank()
    val subtitle = listOfNotNull(itemLine, totalLine)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
    val progressState = rememberRouteEnterProgress(
        key = "cart_receipt_${payload.action}_${payload.status}_${payload.productId}",
        durationMillis = 260,
    )
    val iconProgressState = rememberRouteEnterProgress(
        key = "cart_receipt_icon_${payload.action}_${payload.status}_${payload.productId}",
        durationMillis = 180,
        delayMillis = 70,
    )
    val accent = if (failed) BuyPilotColors.Warning else BuyPilotColors.Success
    val tintSurface = if (failed) BuyPilotColors.WarningSoft else BuyPilotColors.SuccessSoft

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val progress = progressState.value
                alpha = progress
                translationY = (1f - progress) * 8.dp.toPx()
                scaleX = lerp(0.985f, 1f, progress)
                scaleY = lerp(0.985f, 1f, progress)
            },
        color = BuyPilotColors.SurfaceCard,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border.copy(alpha = 0.68f)),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .graphicsLayer {
                        val iconProgress = iconProgressState.value
                        scaleX = lerp(0.94f, 1f, iconProgress)
                        scaleY = lerp(0.94f, 1f, iconProgress)
                        alpha = iconProgress
                    }
                    .background(tintSurface, CircleShape)
                    .border(1.dp, accent.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_shopping_bag_24),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    color = BuyPilotColors.TextPrimary,
                    fontSize = BuyPilotType.Body,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    color = BuyPilotColors.TextSecondary,
                    fontSize = BuyPilotType.Label,
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (retryableAdd) {
                TextButton(
                    onClick = { onRetryAddToCart(productId.orEmpty()) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "重试",
                        color = BuyPilotColors.PrimaryDark,
                        fontSize = BuyPilotType.Label,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

internal const val MinimalErrorText = "服务暂时无响应"

private val TechnicalErrorMarkerRegex = Regex(
    """(?i)\b(?:exception|traceback|timeout|failed|failure|network|http|ssl|socket|connect|connection|json|cancelled|canceled|unable|unknown host|econnreset)\b""",
)
private val TechnicalErrorCodeRegex = Regex("""\b[A-Z][A-Z0-9_]{2,}\b""")
private val GenericBackendFailureRegex = Regex("""(?:本轮)?(?:导购)?处理失败|请稍后重试|服务异常|系统异常""")

internal fun String.userFacingErrorReason(fallback: String): String {
    val clean = withoutInternalDebugTokens().trim()
    if (clean.isBlank() || clean.looksLikeTechnicalErrorText()) return fallback
    return clean
}

private fun String.looksLikeTechnicalErrorText(): Boolean {
    val text = trim()
    if (text.isBlank()) return true
    if (TechnicalErrorMarkerRegex.containsMatchIn(text)) return true
    if (TechnicalErrorCodeRegex.containsMatchIn(text)) return true
    if (GenericBackendFailureRegex.containsMatchIn(text)) return true
    if ('{' in text || '}' in text || '[' in text || ']' in text) return true
    if ("://" in text || "/" in text && Regex("""\b(?:api|v\d+|chat|stream)\b""").containsMatchIn(text)) return true
    val hasCjk = text.any { it.isCjk() }
    val asciiLetters = text.count { it in 'A'..'Z' || it in 'a'..'z' }
    return !hasCjk && asciiLetters >= 8
}

private fun Char.isCjk(): Boolean =
    this in '\u3400'..'\u4DBF' ||
        this in '\u4E00'..'\u9FFF' ||
        this in '\uF900'..'\uFAFF'

internal fun ThinkingPayload.userFacingThinkingMessage(): String {
    if (fallback || isFallback) return ""
    val clean = message.withoutInternalDebugTokens().trim()
    if (clean.isNotBlank() && !clean.looksLikeTechnicalStatusText()) return clean
    return ""
}

private fun String.looksLikeTechnicalStatusText(): Boolean {
    val text = trim()
    if (text.isBlank()) return true
    if (TechnicalErrorMarkerRegex.containsMatchIn(text)) return true
    if (TechnicalErrorCodeRegex.containsMatchIn(text)) return true
    if ('_' in text || ':' in text || '{' in text || '}' in text) return true
    val hasCjk = text.any { it.isCjk() }
    val asciiLetters = text.count { it in 'A'..'Z' || it in 'a'..'z' }
    return !hasCjk && asciiLetters >= 4
}

@Composable
internal fun ErrorCard(
    node: ErrorNode,
) {
    if (node.message.isBlank() && node.code.isBlank()) return
    Text(
        text = MinimalErrorText,
        color = BuyPilotColors.TextSecondary,
        fontSize = BuyPilotType.Body,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

@Composable
internal fun InlineSystemNotice(message: String) {
    val isMinimalError = message == MinimalErrorText
    Text(
        text = message,
        color = if (isMinimalError) BuyPilotColors.TextSecondary else BuyPilotColors.TextMuted,
        fontSize = if (isMinimalError) BuyPilotType.Body else BuyPilotType.Label,
        lineHeight = if (isMinimalError) 20.sp else 18.sp,
        fontWeight = if (isMinimalError) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

@Composable
private fun ImageAttachmentPreview(
    attachment: ChatImageAttachmentState,
    onRemove: () -> Unit,
    onPreview: () -> Unit,
) {
    AnimatedVisibility(
        visible = attachment.hasImage,
        enter = fadeIn(animationSpec = tween(180, easing = MenuEaseOut)) +
            expandVertically(animationSpec = tween(220, easing = MenuEaseOut)),
        exit = fadeOut(animationSpec = tween(120, easing = MenuEaseIn)) +
            shrinkVertically(animationSpec = tween(160, easing = MenuEaseIn)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 2.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(15.dp),
                        ambientColor = Color.Black.copy(alpha = 0.07f),
                        spotColor = Color.Black.copy(alpha = 0.10f),
                    )
                    .background(BuyPilotColors.SurfaceCard, RoundedCornerShape(15.dp))
                    .border(
                        1.dp,
                        if (attachment.error != null) {
                            BuyPilotColors.Danger.copy(alpha = 0.52f)
                        } else {
                            BuyPilotColors.Border.copy(alpha = 0.78f)
                        },
                        RoundedCornerShape(15.dp),
                    )
                    .clickable(
                        enabled = attachment.hasImage,
                        role = Role.Button,
                        onClick = onPreview,
                    ),
            ) {
                AsyncImage(
                    model = attachment.localUri.takeIf { it.isNotBlank() } ?: attachment.imageUrl,
                    contentDescription = "已选择的图片，点按预览",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BuyPilotColors.SurfaceSubtle),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 7.dp, y = (-7).dp)
                        .size(24.dp)
                        .background(BuyPilotColors.SurfaceCard, CircleShape)
                        .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.88f), CircleShape)
                        .clickable(role = Role.Button, onClick = onRemove),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close_24),
                        contentDescription = "移除图片",
                        tint = BuyPilotColors.TextSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                }
                when {
                    attachment.isUploading -> Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(7.dp)
                            .size(18.dp)
                            .background(BuyPilotColors.SurfaceCard.copy(alpha = 0.9f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        UploadingDot()
                    }
                    attachment.error != null -> Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(7.dp)
                            .size(18.dp)
                            .background(BuyPilotColors.Attention, CircleShape)
                            .border(1.dp, BuyPilotColors.PrimarySoft, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "!",
                            color = BuyPilotColors.Danger,
                            fontSize = 11.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadingDot() {
    val transition = rememberInfiniteTransition(label = "image_uploading_dot")
    val alpha by transition.animateFloat(
        initialValue = 0.34f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(640, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "image_uploading_dot_alpha",
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(BuyPilotColors.Primary.copy(alpha = alpha), CircleShape),
    )
}

@Composable
private fun ImageAttachmentPreviewDialog(
    attachment: ChatImageAttachmentState,
    backendBaseUrl: String,
    onDismiss: () -> Unit,
) {
    val previewModel = attachment.localUri
        .takeIf { it.isNotBlank() }
        ?: attachment.imageUrl.resolveProductImageUrl(backendBaseUrl)
        ?: return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            visible = true
        }
        val scrimInteraction = remember { MutableInteractionSource() }
        val imageInteraction = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.74f))
                .clickable(
                    interactionSource = scrimInteraction,
                    indication = null,
                    onClick = onDismiss,
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 16.dp),
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(durationMillis = 180, easing = MenuEaseOut)) +
                    scaleIn(
                        initialScale = 0.985f,
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                    ),
                exit = fadeOut(animationSpec = tween(durationMillis = 120, easing = MenuEaseIn)) +
                    scaleOut(
                        targetScale = 0.985f,
                        animationSpec = tween(durationMillis = 140, easing = MenuEaseIn),
                    ),
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .fillMaxHeight(0.84f),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = imageInteraction,
                            indication = null,
                            onClick = {},
                        ),
                    color = Color.Black.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 12.dp,
                ) {
                    AsyncImage(
                        model = previewModel,
                        contentDescription = "图片预览",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(22.dp)),
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(42.dp)
                    .background(Color.Black.copy(alpha = 0.36f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close_24),
                    contentDescription = "关闭图片预览",
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(18.dp),
                )
            }

            attachment.error?.let { message ->
                Text(
                    text = message,
                    color = Color.White.copy(alpha = 0.86f),
                    fontSize = BuyPilotType.Label,
                    lineHeight = 18.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.42f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun BottomComposer(
    text: String,
    inputState: ChatInputState,
    isStreaming: Boolean,
    isTextRevealing: Boolean,
    awaitingCriteriaAdjustment: Boolean,
    isAttachmentMenuOpen: Boolean,
    imageAttachment: ChatImageAttachmentState,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onAttachmentClick: () -> Unit,
    onTextChange: (String) -> Unit,
    onTextFocus: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onHeightChanged: (Int) -> Unit,
    onKeyboardFlightSourceChanged: (ClarificationChipSnapshot) -> Unit,
    onRemoveImage: () -> Unit,
    onPreviewImage: () -> Unit,
    onSubmit: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val placeholder = when {
        inputState == ChatInputState.Clarifying -> "请回答上面的问题"
        inputState == ChatInputState.Streaming -> "正在生成，可随时停止"
        imageAttachment.isUploading -> "图片上传中..."
        imageAttachment.error != null -> "移除图片后重试，或重新选择"
        isTextRevealing -> "正在显示回复，可继续查看"
        awaitingCriteriaAdjustment -> "放宽预算、换品类或继续描述需求..."
        else -> "继续追问或描述需求..."
    }
    val attachmentBlocksSubmit = imageAttachment.hasImage && !imageAttachment.canSend
    val canSubmit = isStreaming || (!attachmentBlocksSubmit && (text.isNotBlank() || imageAttachment.canSend))
    val containerColor by animateColorAsState(
        targetValue = when {
            isFocused -> BuyPilotColors.SurfaceCard
            else -> BuyPilotColors.SurfaceMuted
        },
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "composer_container_color",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> BuyPilotColors.Primary.copy(alpha = 0.68f)
            else -> BuyPilotColors.Border
        },
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "composer_border_color",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 1.5.dp else 1.dp,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "composer_border_width",
    )
    val attachmentIconRotation by animateFloatAsState(
        targetValue = if (isAttachmentMenuOpen) 45f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "attachment_icon_rotation",
    )
    val attachmentEnabled = !isStreaming

    Box(
        modifier = modifier
            .fillMaxWidth()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { onHeightChanged(it.height) }
                .onGloballyPositioned { coordinates ->
                    onKeyboardFlightSourceChanged(coordinates.toClarificationSnapshot())
                }
                .background(BuyPilotColors.SurfaceCard)
                .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.88f))
                .navigationBarsPadding()
                .padding(start = 12.dp, top = 13.dp, end = 12.dp, bottom = 28.dp),
        ) {
            ImageAttachmentPreview(
                attachment = imageAttachment,
                onRemove = onRemoveImage,
                onPreview = onPreviewImage,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .background(containerColor, CircleShape)
                    .border(borderWidth, borderColor, CircleShape)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onAttachmentClick,
                    enabled = attachmentEnabled,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_24),
                        contentDescription = if (isAttachmentMenuOpen) "收起更多输入方式" else "打开更多输入方式",
                        tint = if (attachmentEnabled) BuyPilotColors.TextSecondary else BuyPilotColors.TextMuted,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(attachmentIconRotation),
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    enabled = !isStreaming,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            isFocused = it.isFocused
                            onFocusChanged(it.isFocused)
                            if (it.isFocused) onTextFocus()
                        }
                        .padding(horizontal = 8.dp),
                    textStyle = TextStyle(
                        color = BuyPilotColors.TextPrimary,
                        fontSize = BuyPilotType.Body,
                        lineHeight = 20.sp,
                    ),
                    cursorBrush = SolidColor(BuyPilotColors.Primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (canSubmit) onSubmit() }),
                    maxLines = 3,
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 28.dp)
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (text.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    color = BuyPilotColors.TextMuted,
                                    fontSize = BuyPilotType.Body,
                                    lineHeight = 20.sp,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                ComposerActionButton(
                    isStreaming = isStreaming,
                    enabled = canSubmit,
                    onClick = onSubmit,
                )
            }
        }
    }
}

@Composable
private fun ComposerActionButton(
    isStreaming: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled -> BuyPilotColors.Border.copy(alpha = 0.64f)
            isStreaming -> BuyPilotColors.PrimarySoft.copy(alpha = 0.94f)
            else -> BuyPilotColors.Primary
        },
        animationSpec = tween(durationMillis = 240, easing = MenuEaseOut),
        label = "composer_action_container_color",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> BuyPilotColors.TextMuted
            isStreaming -> BuyPilotColors.PrimaryDark
            else -> BuyPilotColors.OnPrimary
        },
        animationSpec = tween(durationMillis = 220, easing = MenuEaseOut),
        label = "composer_action_content_color",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> BuyPilotColors.Border.copy(alpha = 0.2f)
            isStreaming -> BuyPilotColors.Primary.copy(alpha = 0.32f)
            else -> BuyPilotColors.Primary.copy(alpha = 0.0f)
        },
        animationSpec = tween(durationMillis = 240, easing = MenuEaseOut),
        label = "composer_action_border_color",
    )
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.94f
            isStreaming -> 1.02f
            else -> 1f
        },
        animationSpec = tween(durationMillis = if (pressed) 90 else 240, easing = MenuEaseOut),
        label = "composer_action_scale",
    )
    val shadowAlpha by animateFloatAsState(
        targetValue = when {
            !enabled -> 0f
            isStreaming -> 0.06f
            else -> 0.14f
        },
        animationSpec = tween(durationMillis = 240, easing = MenuEaseOut),
        label = "composer_action_shadow",
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (enabled) 8.dp else 0.dp,
                shape = CircleShape,
                ambientColor = BuyPilotColors.Primary.copy(alpha = shadowAlpha),
                spotColor = BuyPilotColors.Primary.copy(alpha = shadowAlpha),
            )
            .clip(CircleShape)
            .background(containerColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = isStreaming,
            transitionSpec = {
                (fadeIn(animationSpec = tween(durationMillis = 170, easing = MenuEaseOut)) +
                    scaleIn(
                        initialScale = 0.78f,
                        animationSpec = tween(durationMillis = 220, easing = MenuEaseOut),
                    ))
                    .togetherWith(
                        fadeOut(animationSpec = tween(durationMillis = 120, easing = MenuEaseIn)) +
                            scaleOut(
                                targetScale = 0.82f,
                                animationSpec = tween(durationMillis = 120, easing = MenuEaseIn),
                            ),
                    )
            },
            label = "composer_action_icon",
        ) { streaming ->
            Icon(
                painter = painterResource(
                    if (streaming) R.drawable.ic_stop_24 else R.drawable.ic_arrow_upward_24,
                ),
                contentDescription = if (streaming) "停止生成" else "发送",
                tint = contentColor,
                modifier = Modifier.size(if (streaming) 16.dp else 21.dp),
            )
        }
    }
}

@Composable
private fun AttachmentMenu(
    modifier: Modifier = Modifier,
    onImageInput: () -> Unit,
    onCameraInput: () -> Unit,
) {
    Column(
        modifier = modifier
            .width(164.dp)
            .shadow(
                4.dp,
                RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.05f),
            )
            .background(BuyPilotColors.SurfaceCard, RoundedCornerShape(16.dp))
            .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.82f), RoundedCornerShape(16.dp))
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        AttachmentAction(R.drawable.ic_image_24, "商品图识别", enabled = true, onClick = onImageInput)
        AttachmentAction(R.drawable.ic_add_photo_24, "拍照/截图识别", enabled = true, onClick = onCameraInput)
        AttachmentAction(R.drawable.ic_mic_24, "语音输入", enabled = false, onClick = {})
    }
}

@Composable
private fun AttachmentAction(
    iconRes: Int,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (enabled) BuyPilotColors.Primary else BuyPilotColors.TextMuted,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            color = if (enabled) BuyPilotColors.TextPrimary else BuyPilotColors.TextMuted,
            fontSize = BuyPilotType.Body,
            fontWeight = FontWeight.Medium,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun CriteriaEditSheet(
    payload: CriteriaCardPayload,
    onQuickAction: (QuickActionPayload) -> Unit,
    onSave: (JsonObject) -> Unit,
) {
    val criteria = payload.criteria
    val labels = rememberCriteriaLabels()
    val title = stringResource(R.string.criteria_edit_title)
    val productTypePlaceholder = stringResource(R.string.criteria_product_type_placeholder)
    val targetUserPlaceholder = stringResource(R.string.criteria_target_user_placeholder)
    val scenarioPlaceholder = stringResource(R.string.criteria_scenario_placeholder)
    val exclusionsPlaceholder = stringResource(R.string.criteria_exclusions_placeholder)
    val resetLabel = stringResource(R.string.criteria_reset)
    val applyLabel = stringResource(R.string.criteria_apply)
    val criteriaContentKey = remember(criteria) {
        criteria.productTypeLabel() + criteria.budgetMaxLabel() + criteria.skinTypeLabel() +
            criteria.useScenarioLabel() + criteria.exclusionLabels().joinToString()
    }
    var productType by remember(payload.criteria.criteriaId, criteriaContentKey) { mutableStateOf(criteria.productTypeLabel()) }
    var budgetMax by remember(payload.criteria.criteriaId, criteriaContentKey) { mutableStateOf(criteria.budgetMaxLabel()) }
    var skinType by remember(payload.criteria.criteriaId, criteriaContentKey) { mutableStateOf(criteria.skinTypeLabel()) }
    var useScenario by remember(payload.criteria.criteriaId, criteriaContentKey) { mutableStateOf(criteria.useScenarioLabel()) }
    var exclusions by remember(payload.criteria.criteriaId, criteriaContentKey) { mutableStateOf(criteria.exclusionLabels().joinToString("、")) }

    fun resetFields() {
        productType = criteria.productTypeLabel()
        budgetMax = criteria.budgetMaxLabel()
        skinType = criteria.skinTypeLabel()
        useScenario = criteria.useScenarioLabel()
        exclusions = criteria.exclusionLabels().joinToString("、")
    }

    SheetContentColumn(expandToMaxHeight = false) {
        SheetTitle(title)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EditableFieldBlock(
                label = labels.coreNeed,
                value = productType,
                placeholder = productTypePlaceholder,
                onValueChange = { productType = it },
                imeAction = ImeAction.Next,
            )
            BudgetSliderBlock(
                value = budgetMax,
                productContext = "${criteria.category} $productType",
                onValueChange = { budgetMax = it },
            )
            EditableFieldBlock(
                label = labels.targetUser,
                value = skinType,
                placeholder = targetUserPlaceholder,
                onValueChange = { skinType = it },
                imeAction = ImeAction.Next,
            )
            EditableFieldBlock(
                label = labels.scenario,
                value = useScenario,
                placeholder = scenarioPlaceholder,
                onValueChange = { useScenario = it },
                imeAction = ImeAction.Next,
            )
            EditableFieldBlock(
                label = labels.exclusions,
                value = exclusions,
                placeholder = exclusionsPlaceholder,
                onValueChange = { exclusions = it },
                imeAction = ImeAction.Done,
            )
        }
        val quickActionLabels = payload.quickActions.map { it.label.trim() }.filter { it.isNotBlank() }
        if (quickActionLabels.isNotEmpty()) {
            CriteriaSuggestionSection(
                labels = quickActionLabels,
                onClick = { label ->
                    payload.quickActions.firstOrNull { it.label == label }?.let(onQuickAction)
                },
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalButton(
                onClick = { resetFields() },
                modifier = Modifier
                    .weight(0.82f)
                    .height(52.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = BuyPilotColors.SurfaceMuted.copy(alpha = 0.9f),
                    contentColor = BuyPilotColors.TextPrimary,
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
            ) {
                Text(resetLabel, fontSize = BuyPilotType.Body, fontWeight = FontWeight.Medium)
            }
            Button(
                onClick = {
                    onSave(
                        buildCriteriaPatch(
                            productType = productType,
                            budgetMax = budgetMax,
                            skinType = skinType,
                            useScenario = useScenario,
                            exclusions = exclusions,
                        ),
                    )
                },
                modifier = Modifier
                    .weight(1.28f)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BuyPilotColors.Primary, contentColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                Text(
                    text = applyLabel,
                    fontSize = BuyPilotType.Body,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CartSheet(
    state: ChatCartUiState,
    backendBaseUrl: String,
    onQuantityChange: (String, Int) -> Unit,
    onProductDetailOpen: (String) -> Unit,
) {
    SheetContentColumn(expandToMaxHeight = false) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "购物车",
                color = BuyPilotColors.TextPrimary,
                fontSize = BuyPilotType.Title,
                lineHeight = 23.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        when {
            state.isLoading && state.items.isEmpty() -> CartLoadingState()
            state.items.isEmpty() -> CartEmptyState()
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.items.forEach { item ->
                        CartItemRow(
                            item = item,
                            backendBaseUrl = backendBaseUrl,
                            updating = item.productId in state.updatingProductIds,
                            onQuantityChange = onQuantityChange,
                            onProductDetailOpen = onProductDetailOpen,
                        )
                    }
                }
                CartSummaryFooter(
                    totalItems = state.totalItems,
                    totalPrice = state.totalPrice,
                    error = state.error,
                )
            }
        }
    }
}

@Composable
private fun CartLoadingState() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        UploadingDot()
        Text("正在同步购物车", color = BuyPilotColors.TextMuted, fontSize = BuyPilotType.Label)
    }
}

@Composable
private fun CartEmptyState() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.redbean_bun_character_03),
            contentDescription = null,
            modifier = Modifier
                .size(132.dp)
                .clip(RoundedCornerShape(32.dp)),
            contentScale = ContentScale.Fit,
        )
        Text(
            "购物车还是空的",
            color = BuyPilotColors.TextPrimary,
            fontSize = BuyPilotType.Body,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CartItemRow(
    item: com.buypilot.core.model.CartItemPayload,
    backendBaseUrl: String,
    updating: Boolean,
    onQuantityChange: (String, Int) -> Unit,
    onProductDetailOpen: (String) -> Unit,
) {
    val product = item.product
    val title = product?.displayName() ?: item.name.ifBlank { item.productId }
    val price = item.price?.let { "¥${it.clean()}" } ?: product?.priceLabel().orEmpty()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BuyPilotColors.SurfaceMuted.copy(alpha = 0.52f))
            .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.56f), RoundedCornerShape(18.dp))
            .clickable(enabled = product != null) { onProductDetailOpen(item.productId) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(BuyPilotColors.SurfaceImage)
                .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.4f), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (product != null) {
                ProductImage(
                    product = product,
                    backendBaseUrl = backendBaseUrl,
                    decodeSizePx = 144,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.product_image_placeholder),
                    contentDescription = null,
                    tint = BuyPilotColors.TextMuted,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                title,
                color = BuyPilotColors.TextPrimary,
                fontSize = BuyPilotType.Body,
                lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                price.ifBlank { "价格待确认" },
                color = BuyPilotColors.TextMuted,
                fontSize = BuyPilotType.Label,
                lineHeight = 16.sp,
                maxLines = 1,
            )
        }
        CartQuantityStepper(
            quantity = item.quantity,
            updating = updating,
            onMinus = {
                if (item.quantity <= 1) {
                    onQuantityChange(item.productId, 0)
                } else {
                    onQuantityChange(item.productId, item.quantity - 1)
                }
            },
            onPlus = { onQuantityChange(item.productId, item.quantity + 1) },
            onRemove = { onQuantityChange(item.productId, 0) },
        )
    }
}

@Composable
private fun CartQuantityStepper(
    quantity: Int,
    updating: Boolean,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier
                .height(34.dp)
                .clip(CircleShape)
                .background(BuyPilotColors.SurfaceCard)
                .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.7f), CircleShape),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CartMiniIconButton(R.drawable.ic_remove_24, "减少数量", enabled = !updating, onClick = onMinus)
            Text(
                quantity.toString(),
                color = BuyPilotColors.TextPrimary,
                fontSize = BuyPilotType.Label,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 24.dp),
            )
            CartMiniIconButton(R.drawable.ic_add_24, "增加数量", enabled = !updating, onClick = onPlus)
        }
        TextButton(
            onClick = onRemove,
            enabled = !updating,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            modifier = Modifier.height(28.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_delete_24),
                contentDescription = null,
                tint = BuyPilotColors.TextMuted,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text("移除", color = BuyPilotColors.TextMuted, fontSize = BuyPilotType.Tiny)
        }
    }
}

@Composable
private fun CartMiniIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(32.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = if (enabled) BuyPilotColors.TextSecondary else BuyPilotColors.TextMuted.copy(alpha = 0.45f),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun CartSummaryFooter(totalItems: Int, totalPrice: Double, error: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (error != null) {
            InlineSystemNotice(error.userFacingErrorReason("购物车刚才没有同步成功，请稍后再试。"))
        }
        HorizontalDivider(color = BuyPilotColors.Border.copy(alpha = 0.5f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "共 $totalItems 件",
                color = BuyPilotColors.TextSecondary,
                fontSize = BuyPilotType.Label,
                lineHeight = 17.sp,
            )
            Text(
                "¥${totalPrice.clean()}",
                color = BuyPilotColors.Primary,
                fontSize = 20.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun DecisionEvidenceSheet(
    payload: FinalDecisionPayload,
    productsById: Map<String, ProductCardPayload>,
    productDeckIdByProductId: Map<String, String>,
    swipeState: ProductSwipeState?,
    sourceDeckNodeKey: String? = null,
    cartState: ChatCartUiState,
    onProductDetailOpen: (String, String, String?) -> Unit,
    onQuickAction: (QuickActionPayload) -> Unit,
) {
    val whyItems = payload.why.map { it.withoutMarkdownMarkup().withoutInternalDebugTokens().trim() }.filter { it.isNotBlank() }
    val notForItems = payload.notFor.toDecisionNotForItems(productsById)
    val alternatives = payload.alternatives.filter {
        it.name.withoutInternalDebugTokens().isNotBlank()
    }
    val winnerProductId = payload.winnerProductId?.takeIf { it.isNotBlank() }
    val winner = winnerProductId?.let(productsById::get)
    val feedbackTrace = remember(swipeState, productsById) {
        swipeState.toDecisionFeedbackTrace(productsById)
    }
    val scoreItems = remember(payload.scoreBreakdown) { payload.scoreBreakdown.toDecisionScoreItems() }
    val winnerRiskNote = winner
        ?.riskNotes
        ?.map { it.withoutMarkdownMarkup().withoutInternalDebugTokens().trim() }
        ?.firstOrNull { it.isNotBlank() && !it.looksLikeQuestionAnswerEvidenceForDecision() }
    val nextActions = payload.nextActions
        .filter { it.label.isNotBlank() && it.action != "view_cart" }
        .map { action ->
            if (action.action == "add_to_cart" && action.productId.isNullOrBlank() && !winnerProductId.isNullOrBlank()) {
                action.copy(productId = winnerProductId)
            } else {
                action
            }
        }

    SheetContentColumn(expandToMaxHeight = false) {
        SheetTitle("决策依据")

        if (whyItems.isNotEmpty()) {
            DecisionEvidenceListSection(
                title = "为什么选它",
                items = whyItems,
                numbered = true,
                accentColor = BuyPilotColors.Primary,
                surfaceColor = BuyPilotColors.PrimarySoft.copy(alpha = 0.28f),
            )
        }

        feedbackTrace?.let {
            DecisionFeedbackTraceSection(trace = it)
        }

        if (scoreItems.isNotEmpty()) {
            DecisionScoreBreakdownSection(
                items = scoreItems,
                motionKey = "decision_evidence_${winnerProductId.orEmpty()}_${scoreItems.size}",
                motionEnabled = false,
                alreadyEntered = true,
            )
        }

        winnerRiskNote?.let {
            DecisionLightRiskNote(text = it)
        }

        if (notForItems.isNotEmpty()) {
            DecisionEvidenceListSection(
                title = "不适合这些情况",
                items = notForItems,
                numbered = false,
                accentColor = BuyPilotColors.Warning,
                surfaceColor = BuyPilotColors.WarningSoft,
            )
        }

        if (alternatives.isNotEmpty()) {
            DecisionAlternativesSection(
                alternatives = alternatives,
                productDeckIdByProductId = productDeckIdByProductId,
                sourceDeckNodeKey = sourceDeckNodeKey,
                onProductDetailOpen = onProductDetailOpen,
            )
        }
    }
}

@Composable
private fun DecisionEvidenceSummary(
    summary: String,
    winnerProductId: String?,
    productsById: Map<String, ProductCardPayload>,
    productDeckIdByProductId: Map<String, String>,
    sourceDeckNodeKey: String? = null,
    onProductDetailOpen: (String, String, String?) -> Unit,
) {
    val cleanWinnerProductId = winnerProductId?.takeIf { it.isNotBlank() }
    val winnerDeckId = cleanWinnerProductId?.let { productDeckIdByProductId[it] }
    val winnerName = cleanWinnerProductId
        ?.let { productsById[it]?.product?.displayName("") }
        ?.takeIf { it.isNotBlank() }
    val openWinnerDetail = winnerDeckId?.let { deckId ->
        { onProductDetailOpen(deckId, cleanWinnerProductId.orEmpty(), sourceDeckNodeKey) }
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (openWinnerDetail != null) {
                    Modifier.clickable(role = Role.Button, onClick = openWinnerDetail)
                } else {
                    Modifier
                },
            ),
        color = BuyPilotColors.SurfaceCard,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border.copy(alpha = 0.72f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .decisionEvidenceSurfaceGlow()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DecisionEvidenceEyebrow("最终判断")
                if (openWinnerDetail != null) {
                    ProductDetailChevron(modifier = Modifier.size(28.dp))
                }
            }
            winnerName?.let { name ->
                Text(
                    text = name,
                    color = BuyPilotColors.TextSecondary,
                    fontSize = BuyPilotType.Label,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (summary.isNotBlank()) {
                MarkdownTextBlock(
                    content = summary,
                    style = TextStyle(
                        color = BuyPilotColors.TextPrimary,
                        fontSize = BuyPilotType.LargeBody,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }
    }
}

@Composable
private fun DecisionEvidenceListSection(
    title: String,
    items: List<String>,
    numbered: Boolean,
    accentColor: Color,
    surfaceColor: Color,
) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DecisionEvidenceSectionHeader(title, items.size)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = BuyPilotColors.SurfaceCard,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border.copy(alpha = 0.72f)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items.forEachIndexed { index, item ->
                    DecisionEvidenceListRow(
                        marker = if (numbered) "${index + 1}" else "!",
                        text = item,
                        accentColor = accentColor,
                        markerSurface = surfaceColor,
                    )
                    if (index != items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 42.dp),
                            color = BuyPilotColors.Border.copy(alpha = 0.54f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DecisionEvidenceListRow(
    marker: String,
    text: String,
    accentColor: Color,
    markerSurface: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(markerSurface, CircleShape)
                .border(1.dp, accentColor.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = marker,
                color = accentColor,
                fontSize = BuyPilotType.Label,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
        MarkdownTextBlock(
            content = text,
            style = TextStyle(
                color = BuyPilotColors.TextPrimary,
                fontSize = BuyPilotType.Body,
                lineHeight = 22.sp,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DecisionAlternativesSection(
    alternatives: List<com.buypilot.core.model.AlternativePayload>,
    productDeckIdByProductId: Map<String, String>,
    sourceDeckNodeKey: String? = null,
    onProductDetailOpen: (String, String, String?) -> Unit,
) {
    if (alternatives.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DecisionEvidenceSectionHeader("备选方案", alternatives.size)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = BuyPilotColors.SurfaceCard,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border.copy(alpha = 0.72f)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                alternatives.forEachIndexed { index, alternative ->
                    DecisionAlternativeRow(
                        index = index,
                        name = alternative.name.withoutInternalDebugTokens(),
                        productId = alternative.productId,
                        productDeckIdByProductId = productDeckIdByProductId,
                        sourceDeckNodeKey = sourceDeckNodeKey,
                        onProductDetailOpen = onProductDetailOpen,
                    )
                    if (index != alternatives.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 42.dp),
                            color = BuyPilotColors.Border.copy(alpha = 0.54f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DecisionAlternativeRow(
    index: Int,
    name: String,
    productId: String,
    productDeckIdByProductId: Map<String, String>,
    sourceDeckNodeKey: String? = null,
    onProductDetailOpen: (String, String, String?) -> Unit,
) {
    val cleanProductId = productId.takeIf { it.isNotBlank() }
    val deckId = cleanProductId?.let { productDeckIdByProductId[it] }
    val openDetail = deckId?.let { resolvedDeckId ->
        { onProductDetailOpen(resolvedDeckId, cleanProductId.orEmpty(), sourceDeckNodeKey) }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (openDetail != null) {
                    Modifier.clickable(role = Role.Button, onClick = openDetail)
                } else {
                    Modifier
                },
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(BuyPilotColors.Info.copy(alpha = 0.09f), RoundedCornerShape(10.dp))
                .border(1.dp, BuyPilotColors.Info.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "A${index + 1}",
                color = BuyPilotColors.Info,
                fontSize = BuyPilotType.Tiny,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            name.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    color = BuyPilotColors.TextPrimary,
                    fontSize = BuyPilotType.Body,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (openDetail != null) {
            ProductDetailChevron()
        }
    }
}

@Composable
private fun DecisionNextActionsSection(
    actions: List<QuickActionPayload>,
    cartState: ChatCartUiState,
    onQuickAction: (QuickActionPayload) -> Unit,
) {
    val visibleActions = actions.mapNotNull { action ->
        val label = action.label.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val productId = action.productId?.takeIf { it.isNotBlank() }
        val isAddToCart = action.action == "add_to_cart"
        val isPending = isAddToCart && productId in cartState.pendingAddProductIds
        val isInCart = isAddToCart && cartState.items.any { it.productId == productId }
        val enabled = when {
            isAddToCart -> productId != null && !isPending && !isInCart
            else -> true
        }
        DecisionNextActionUi(
            action = action.copy(
                label = when {
                    isPending -> "加入中..."
                    isInCart -> "已加入"
                    else -> label
                },
            ),
            helper = when {
                isPending -> "等待真实购物车回执"
                isInCart -> "这件商品已在购物车中"
                isAddToCart && productId == null -> "缺少商品信息，暂不可操作"
                isAddToCart -> "绑定当前结论商品"
                action.action == "open_evidence" -> "查看更多判断依据"
                action.action == "compare" -> "查看当前候选差异"
                else -> "继续调整推荐"
            },
            iconRes = when (action.action) {
                "add_to_cart" -> R.drawable.ic_shopping_bag_24
                "compare" -> R.drawable.ic_compare_arrows_24
                "open_evidence" -> R.drawable.ic_article_24
                else -> R.drawable.ic_arrow_upward_24
            },
            enabled = enabled,
        )
    }
    if (visibleActions.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DecisionEvidenceSectionHeader("下一步动作", visibleActions.size)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = BuyPilotColors.SurfaceCard,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border.copy(alpha = 0.72f)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                visibleActions.forEachIndexed { index, action ->
                    DecisionNextActionRow(
                        item = action,
                        onClick = { onQuickAction(action.action) },
                    )
                    if (index != visibleActions.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 44.dp),
                            color = BuyPilotColors.Border.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }
    }
}

private data class DecisionNextActionUi(
    val action: QuickActionPayload,
    val helper: String,
    val iconRes: Int,
    val enabled: Boolean,
)

@Composable
private fun DecisionNextActionRow(
    item: DecisionNextActionUi,
    onClick: () -> Unit,
) {
    val contentAlpha = if (item.enabled) 1f else 0.56f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = item.enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .graphicsLayer { alpha = contentAlpha },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    if (item.action.action == "add_to_cart") {
                        BuyPilotColors.PrimarySoft.copy(alpha = 0.45f)
                    } else {
                        BuyPilotColors.SurfaceMuted.copy(alpha = 0.72f)
                    },
                    CircleShape,
                )
                .border(
                    1.dp,
                    if (item.action.action == "add_to_cart") {
                        BuyPilotColors.Primary.copy(alpha = 0.12f)
                    } else {
                        BuyPilotColors.Border.copy(alpha = 0.58f)
                    },
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(item.iconRes),
                contentDescription = null,
                tint = if (item.action.action == "add_to_cart") BuyPilotColors.PrimaryDark else BuyPilotColors.TextSecondary,
                modifier = Modifier.size(17.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.action.label,
                color = BuyPilotColors.TextPrimary,
                fontSize = BuyPilotType.Body,
                lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.helper,
                color = BuyPilotColors.TextMuted,
                fontSize = BuyPilotType.Tiny,
                lineHeight = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (item.enabled) {
            ProductDetailChevron(modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun DecisionEvidenceSectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = BuyPilotColors.TextPrimary,
            fontSize = BuyPilotType.Body,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "$count 项",
            color = BuyPilotColors.TextMuted,
            fontSize = BuyPilotType.Tiny,
            lineHeight = 14.sp,
        )
    }
}

@Composable
private fun DecisionEvidenceEyebrow(text: String) {
    Text(
        text = text,
        color = BuyPilotColors.PrimaryDark,
        fontSize = BuyPilotType.Label,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(BuyPilotColors.PrimarySoft.copy(alpha = 0.72f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

private fun Modifier.decisionEvidenceSurfaceGlow(): Modifier =
    drawBehind {
        val corner = 20.dp.toPx()
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    BuyPilotColors.Primary.copy(alpha = 0.075f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.92f, size.height * 0.05f),
                radius = size.minDimension * 0.92f,
            ),
            cornerRadius = CornerRadius(corner, corner),
        )
    }

@Composable
private fun SheetContentColumn(
    expandToMaxHeight: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (expandToMaxHeight) {
                    Modifier.fillMaxHeight(0.92f)
                } else {
                    Modifier
                },
            )
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 2.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        content = content,
    )
}

@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier
            .padding(top = 12.dp, bottom = 8.dp)
            .size(width = 32.dp, height = 4.dp)
            .clip(CircleShape)
            .background(BuyPilotColors.TextMuted.copy(alpha = 0.38f)),
    )
}

@Composable
private fun SheetTitle(title: String) {
    Text(
        text = title,
        color = BuyPilotColors.TextPrimary,
        fontSize = BuyPilotType.Title,
        lineHeight = 23.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun FieldBlock(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BuyPilotColors.SurfaceMuted.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text(label, color = BuyPilotColors.TextMuted, fontSize = BuyPilotType.Label)
        Text(value, color = BuyPilotColors.TextPrimary, fontSize = BuyPilotType.LargeBody, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EditableFieldBlock(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    suffix: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp),
        label = {
            Text(
                text = label,
                fontSize = BuyPilotType.Label,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        },
        placeholder = {
            Text(
                text = placeholder,
                color = BuyPilotColors.TextMuted,
                fontSize = BuyPilotType.Body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        suffix = suffix?.let {
            {
                Text(
                    text = it,
                    color = BuyPilotColors.TextMuted,
                    fontSize = BuyPilotType.Label,
                    maxLines = 1,
                )
            }
        },
        singleLine = true,
        textStyle = TextStyle(
            color = BuyPilotColors.TextPrimary,
            fontSize = BuyPilotType.LargeBody,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = BuyPilotColors.TextPrimary,
            unfocusedTextColor = BuyPilotColors.TextPrimary,
            focusedContainerColor = BuyPilotColors.SurfaceCard,
            unfocusedContainerColor = BuyPilotColors.SurfaceCard,
            disabledContainerColor = BuyPilotColors.SurfaceMuted,
            cursorColor = BuyPilotColors.Primary,
            focusedBorderColor = BuyPilotColors.Primary.copy(alpha = 0.82f),
            unfocusedBorderColor = BuyPilotColors.Border,
            focusedLabelColor = BuyPilotColors.PrimaryDark,
            unfocusedLabelColor = BuyPilotColors.TextSecondary,
            focusedPlaceholderColor = BuyPilotColors.TextMuted,
            unfocusedPlaceholderColor = BuyPilotColors.TextMuted,
            focusedSuffixColor = BuyPilotColors.TextMuted,
            unfocusedSuffixColor = BuyPilotColors.TextMuted,
        ),
        visualTransformation = VisualTransformation.None,
    )
}

@Composable
private fun BudgetSliderBlock(
    value: String,
    productContext: String,
    onValueChange: (String) -> Unit,
) {
    val parsedBudget = value.extractFirstNumber()?.roundToInt()
    val budgetOptions = remember(parsedBudget, productContext) {
        budgetSliderOptions(parsedBudget, productContext)
    }
    val selectedBudget = parsedBudget?.nearestBudgetOption(budgetOptions)
        ?: defaultBudgetPreset(productContext)
    val selectedIndex = budgetOptions.indexOf(selectedBudget).takeIf { it >= 0 } ?: 0
    val sliderInteractionSource = remember { MutableInteractionSource() }
    val isDragging by sliderInteractionSource.collectIsDraggedAsState()
    val displayIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(durationMillis = 260, easing = MenuEaseOut),
        label = "budget_slider_display_index",
    )
    LaunchedEffect(parsedBudget, selectedBudget, budgetOptions) {
        if (parsedBudget != null && parsedBudget !in budgetOptions) {
            onValueChange(selectedBudget.toString())
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BuyPilotColors.SurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border),
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "预算",
                    color = BuyPilotColors.TextSecondary,
                    fontSize = BuyPilotType.Label,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${selectedBudget}元以内",
                    color = BuyPilotColors.PrimaryDark,
                    fontSize = BuyPilotType.LargeBody,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            ElegantBudgetSlider(
                selectedIndex = selectedIndex,
                displayIndex = displayIndex,
                options = budgetOptions,
                selectedBudget = selectedBudget,
                isDragging = isDragging,
                interactionSource = sliderInteractionSource,
                onIndexChange = { nextIndex ->
                    val nextBudget = budgetOptions[nextIndex.coerceIn(budgetOptions.indices)]
                    onValueChange(nextBudget.toString())
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "¥${budgetOptions.first()}",
                    color = BuyPilotColors.TextMuted,
                    fontSize = BuyPilotType.Tiny,
                    lineHeight = 14.sp,
                )
                Text(
                    text = budgetOptions.midBudgetLabel(),
                    color = BuyPilotColors.TextMuted,
                    fontSize = BuyPilotType.Tiny,
                    lineHeight = 14.sp,
                )
                Text(
                    text = "¥${budgetOptions.last()}",
                    color = BuyPilotColors.TextMuted,
                    fontSize = BuyPilotType.Tiny,
                    lineHeight = 14.sp,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ElegantBudgetSlider(
    selectedIndex: Int,
    displayIndex: Float,
    options: List<Int>,
    selectedBudget: Int,
    isDragging: Boolean,
    interactionSource: MutableInteractionSource,
    onIndexChange: (Int) -> Unit,
) {
    val valueRange = 0f..(options.lastIndex).toFloat()
    val activeFraction = (displayIndex / options.lastIndex.coerceAtLeast(1)).coerceIn(0f, 1f)
    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.08f else 1f,
        animationSpec = tween(durationMillis = 180, easing = MenuEaseOut),
        label = "budget_thumb_scale",
    )
    val thumbHaloAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0.16f else 0.07f,
        animationSpec = tween(durationMillis = 180, easing = MenuEaseOut),
        label = "budget_thumb_halo_alpha",
    )
    val valueBubbleAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0.82f,
        animationSpec = tween(durationMillis = 190, easing = MenuEaseOut),
        label = "budget_value_bubble_alpha",
    )
    val valueBubbleOffset by animateDpAsState(
        targetValue = if (isDragging) 0.dp else 3.dp,
        animationSpec = tween(durationMillis = 210, easing = MenuEaseOut),
        label = "budget_value_bubble_offset",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp),
    ) {
        BudgetSliderValueBubble(
            budget = selectedBudget,
            fraction = activeFraction,
            alpha = valueBubbleAlpha,
            offsetY = valueBubbleOffset,
            modifier = Modifier
                .align(Alignment.TopStart)
                .zIndex(2f),
        )
        Slider(
            value = selectedIndex.toFloat(),
            onValueChange = { rawIndex ->
                onIndexChange(rawIndex.roundToInt())
            },
            valueRange = valueRange,
            steps = (options.size - 2).coerceAtLeast(0),
            interactionSource = interactionSource,
            colors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            thumb = {
                BudgetSliderThumb(
                    scale = thumbScale,
                    haloAlpha = thumbHaloAlpha,
                )
            },
            track = {
                BudgetSliderTrack(
                    activeFraction = activeFraction,
                    options = options,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun BudgetSliderValueBubble(
    budget: Int,
    fraction: Float,
    alpha: Float,
    offsetY: Dp,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            .graphicsLayer { this.alpha = alpha },
    ) {
        val bubbleWidth = 74.dp
        val bubbleX = (maxWidth - bubbleWidth) * fraction
        Surface(
            color = BuyPilotColors.PrimarySoft.copy(alpha = 0.78f),
            contentColor = BuyPilotColors.PrimaryDark,
            shape = CircleShape,
            shadowElevation = 0.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = bubbleX, y = offsetY)
                .width(bubbleWidth)
                .height(28.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "¥$budget",
                    color = BuyPilotColors.PrimaryDark,
                    fontSize = BuyPilotType.Label,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun BudgetSliderThumb(
    scale: Float,
    haloAlpha: Float,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .scale(scale),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(31.dp)
                .background(BuyPilotColors.Primary.copy(alpha = haloAlpha), CircleShape),
        )
        Box(
            modifier = Modifier
                .size(22.dp)
                .shadow(
                    elevation = 5.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.12f),
                )
                .background(BuyPilotColors.SurfaceCard, CircleShape)
                .border(5.dp, BuyPilotColors.Primary, CircleShape),
        )
    }
}

@Composable
private fun BudgetSliderTrack(
    activeFraction: Float,
    options: List<Int>,
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
    ) {
        val centerY = size.height / 2f
        val horizontalInset = 2.dp.toPx()
        val startX = horizontalInset
        val endX = size.width - horizontalInset
        val activeX = lerp(startX, endX, activeFraction)
        val inactiveStroke = 7.dp.toPx()
        val activeStroke = 7.dp.toPx()
        val tickRadius = 2.dp.toPx()

        drawLine(
            color = BuyPilotColors.SurfaceMuted,
            start = Offset(startX, centerY),
            end = Offset(endX, centerY),
            strokeWidth = inactiveStroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    BuyPilotColors.Primary.copy(alpha = 0.82f),
                    BuyPilotColors.Primary,
                ),
                startX = startX,
                endX = activeX.coerceAtLeast(startX + 1f),
            ),
            start = Offset(startX, centerY),
            end = Offset(activeX, centerY),
            strokeWidth = activeStroke,
            cap = StrokeCap.Round,
        )

        options.forEachIndexed { index, _ ->
            val fraction = index / options.lastIndex.coerceAtLeast(1).toFloat()
            val tickX = lerp(startX, endX, fraction)
            val tickColor = if (fraction <= activeFraction) {
                BuyPilotColors.OnPrimary.copy(alpha = 0.72f)
            } else {
                BuyPilotColors.Border.copy(alpha = 0.86f)
            }
            drawCircle(
                color = tickColor,
                radius = tickRadius,
                center = Offset(tickX, centerY),
            )
        }
    }
}

@Composable
private fun CriteriaSuggestionSection(
    labels: List<String>,
    onClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "快速微调建议",
            color = BuyPilotColors.TextSecondary,
            fontSize = BuyPilotType.Label,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
        )
        ClarificationOptionScroller(
            labels = labels,
            onClick = { label, _ -> onClick(label) },
        )
    }
}

@Composable
private fun EvidenceSection(title: String, items: List<String>, numbered: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(title)
        Surface(
            color = BuyPilotColors.SurfaceCard,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items.forEachIndexed { index, item ->
                    Row {
                        Text(
                            text = if (numbered) "${index + 1}." else "•",
                            color = BuyPilotColors.PrimaryDark,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(28.dp),
                        )
                        MarkdownTextBlock(
                            content = item,
                            style = TextStyle(
                                color = BuyPilotColors.TextPrimary,
                                fontSize = BuyPilotType.Body,
                                lineHeight = 22.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EvidenceBlock(evidence: EvidencePayload) {
    val snippet = evidence.snippet.withoutInternalDebugTokens().trim()
    if (snippet.isBlank()) return
    Surface(
        color = BuyPilotColors.SurfaceBg,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillLabel(evidence.sourceType.userFacingEvidenceSourceLabel("证据"))
                evidence.trustLabel?.withoutInternalDebugTokens()?.takeIf { it.isNotBlank() }?.let { PillLabel(it) }
            }
            MarkdownTextBlock(
                content = snippet,
                style = TextStyle(
                    color = BuyPilotColors.TextPrimary,
                    fontSize = BuyPilotType.LargeBody,
                    lineHeight = 26.sp,
                ),
            )
        }
    }
}

@Composable
private fun WarningBox(text: String) {
    val warningMarkdown = text.trim()
        .removePrefix("注意：")
        .removePrefix("注意:")
        .trim()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BuyPilotColors.Attention, RoundedCornerShape(14.dp))
            .border(1.dp, BuyPilotColors.PrimarySoft, RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Text("!", color = BuyPilotColors.PrimaryDark, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = "注意：",
                color = BuyPilotColors.PrimaryDark,
                fontSize = BuyPilotType.Body,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            MarkdownTextBlock(
                content = warningMarkdown,
                style = TextStyle(
                    color = BuyPilotColors.TextSecondary,
                    fontSize = BuyPilotType.Body,
                    lineHeight = 21.sp,
                ),
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, leading: String? = null, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (leading != null) {
            Text(leading, color = BuyPilotColors.Success, fontSize = BuyPilotType.Body)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = title,
            color = BuyPilotColors.TextPrimary,
            fontSize = BuyPilotType.Body,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ChipRows(
    labels: List<String>,
    modifier: Modifier = Modifier,
    onClick: ((String) -> Unit)? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { label ->
                    SmallActionChip(label = label) { onClick?.invoke(label) }
                }
            }
        }
    }
}

@Composable
private fun ScrollableChipRow(
    labels: List<String>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    colorful: Boolean = false,
    onClick: ((String) -> Unit)? = null,
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
    EdgeFadedLazyRow(
        modifier = modifier,
        leadingAlpha = if (canScrollBackward) 1f else 0f,
        trailingAlpha = if (canScrollForward) 1f else 0f,
        height = 36.dp,
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = contentPadding,
        ) {
            itemsIndexed(labels, key = { index, label -> "$index:$label" }) { index, label ->
                if (colorful) {
                    DecisionReasonChip(label = label, colorIndex = index) { onClick?.invoke(label) }
                } else {
                    SmallActionChip(label = label) { onClick?.invoke(label) }
                }
            }
        }
    }
}

@Composable
private fun ScrollableQuickActionRow(
    actions: List<QuickActionPayload>,
    compareProducts: List<ProductCardPayload> = emptyList(),
    modifier: Modifier = Modifier,
    onCompareProducts: (List<Int>, List<String>?) -> Unit = { _, _ -> },
    onQuickAction: (QuickActionPayload) -> Unit,
) {
    val listState = rememberLazyListState()
    val productSignature = remember(compareProducts) {
        compareProducts.joinToString(separator = "|") { it.product.productId }
    }
    var expandedCompareActionId by rememberSaveable(productSignature, actions.size) { mutableStateOf<String?>(null) }
    var selectedCompareRanks by rememberSaveable(productSignature, actions.size) {
        mutableStateOf(compareProducts.take(2).mapIndexed { index, _ -> index + 1 })
    }
    val compareProductsForPicker = remember(compareProducts) { compareProducts.take(MaxCompareSelectionCount) }
    LaunchedEffect(productSignature, compareProductsForPicker.size) {
        selectedCompareRanks = compareProductsForPicker.take(2).mapIndexed { index, _ -> index + 1 }
        if (compareProductsForPicker.size < 2) {
            expandedCompareActionId = null
        }
    }
    val canScrollBackward by remember(actions.size) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    val canScrollForward by remember(actions.size) {
        derivedStateOf { listState.canScrollForward }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp),
    ) {
        EdgeFadedLazyRow(
            modifier = Modifier.align(Alignment.BottomCenter),
            leadingAlpha = if (canScrollBackward) 1f else 0f,
            trailingAlpha = if (canScrollForward) 1f else 0f,
            height = 36.dp,
        ) {
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 28.dp),
            ) {
                itemsIndexed(actions, key = { index, action ->
                    action.actionId.ifEmpty { "$index:${action.label}" }
                }) { index, action ->
                    val actionKey = action.actionId.ifBlank { "$index:${action.label}" }
                    SmallActionChip(action.label) {
                        if (action.action == "compare" && compareProductsForPicker.size >= 2) {
                            expandedCompareActionId =
                                if (expandedCompareActionId == actionKey) null else actionKey
                            selectedCompareRanks = selectedCompareRanks
                                .ifEmpty { compareProductsForPicker.take(2).mapIndexed { productIndex, _ -> productIndex + 1 } }
                        } else {
                            expandedCompareActionId = null
                            onQuickAction(action)
                        }
                    }
                }
            }
        }
    }
    if (expandedCompareActionId != null && compareProductsForPicker.size >= 2) {
        CompareActionPickerDialog(
            products = compareProductsForPicker,
            selectedRanks = selectedCompareRanks,
            onSelectionChange = { selectedCompareRanks = it },
            onDismiss = { expandedCompareActionId = null },
            onConfirm = {
                if (selectedCompareRanks.size >= 2) {
                    val ranks = selectedCompareRanks.sorted()
                    val productIds = ranks.mapNotNull { rank ->
                        compareProductsForPicker.getOrNull(rank - 1)?.product?.productId
                    }.takeIf { it.size >= 2 }
                    expandedCompareActionId = null
                    onCompareProducts(ranks, productIds)
                }
            },
        )
    }
}

@Composable
private fun CompareActionPickerDialog(
    products: List<ProductCardPayload>,
    selectedRanks: List<Int>,
    onSelectionChange: (List<Int>) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    var dismissRequested by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    LaunchedEffect(dismissRequested) {
        if (dismissRequested) {
            visible = false
            kotlinx.coroutines.delay(170L)
            onDismiss()
        }
    }
    fun requestDismiss() {
        if (!dismissRequested) {
            dismissRequested = true
        }
    }
    Dialog(
        onDismissRequest = ::requestDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = ::requestDismiss,
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 30.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = visible,
                modifier = Modifier.matchParentSize(),
                enter = fadeIn(tween(durationMillis = 180, easing = MenuEaseOut)),
                exit = fadeOut(tween(durationMillis = 150, easing = MenuEaseIn)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BuyPilotColors.TextPrimary.copy(alpha = 0.2f)),
                )
            }
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(durationMillis = 190, easing = MenuEaseOut)) +
                    slideInVertically(tween(durationMillis = 260, easing = MenuEaseOut)) { it / 14 } +
                    scaleIn(
                        initialScale = 0.965f,
                        animationSpec = tween(durationMillis = 260, easing = MenuEaseOut),
                    ),
                exit = fadeOut(tween(durationMillis = 120, easing = MenuEaseIn)) +
                    slideOutVertically(tween(durationMillis = 150, easing = MenuEaseIn)) { it / 18 } +
                    scaleOut(
                        targetScale = 0.975f,
                        animationSpec = tween(durationMillis = 150, easing = MenuEaseIn),
                    ),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                    color = BuyPilotColors.SurfaceCard,
                    shape = RoundedCornerShape(26.dp),
                    shadowElevation = 22.dp,
                    tonalElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border.copy(alpha = 0.68f)),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        products.forEachIndexed { index, payload ->
                            val rank = index + 1
                            CompareActionProductRow(
                                rank = rank,
                                name = payload.product.shortDecisionCompareName(),
                                selected = rank in selectedRanks,
                                onClick = {
                                    val selected = rank in selectedRanks
                                    val next = if (selected) {
                                        if (selectedRanks.size <= 2) selectedRanks else selectedRanks - rank
                                    } else {
                                        if (selectedRanks.size >= MaxCompareSelectionCount) selectedRanks else selectedRanks + rank
                                    }.distinct().sorted()
                                    onSelectionChange(next)
                                },
                            )
                        }
                        HorizontalDivider(color = BuyPilotColors.Border.copy(alpha = 0.42f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = ::requestDismiss, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp)) {
                                Text(
                                    text = "取消",
                                    color = BuyPilotColors.TextMuted,
                                    fontSize = BuyPilotType.Label,
                                    lineHeight = 16.sp,
                                )
                            }
                            TextButton(
                                onClick = onConfirm,
                                enabled = selectedRanks.size >= 2,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
                            ) {
                                Text(
                                    text = "开始对比",
                                    color = if (selectedRanks.size >= 2) BuyPilotColors.PrimaryDark else BuyPilotColors.TextMuted,
                                    fontSize = BuyPilotType.Label,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
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
private fun CompareActionProductRow(
    rank: Int,
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (selected) BuyPilotColors.PrimarySoft.copy(alpha = 0.35f) else Color.Transparent,
        animationSpec = tween(durationMillis = 150, easing = MenuEaseOut),
        label = "compare_action_product_background",
    )
    val border by animateColorAsState(
        targetValue = if (selected) BuyPilotColors.Primary.copy(alpha = 0.18f) else BuyPilotColors.Border.copy(alpha = 0.0f),
        animationSpec = tween(durationMillis = 150, easing = MenuEaseOut),
        label = "compare_action_product_border",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background, RoundedCornerShape(14.dp))
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(
                    if (selected) BuyPilotColors.PrimaryDark else BuyPilotColors.SurfaceSubtle,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = rank.toString(),
                color = if (selected) Color.White else BuyPilotColors.TextSecondary,
                fontSize = BuyPilotType.Tiny,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = name,
            color = BuyPilotColors.TextPrimary,
            fontSize = BuyPilotType.Label,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun ProductPayload.shortDecisionCompareName(): String {
    val clean = displayName("").withoutMarkdownMarkup().withoutInternalDebugTokens().trim()
    if (clean.isBlank()) return "商品"
    val brandName = brand.orEmpty().withoutMarkdownMarkup().withoutInternalDebugTokens().trim()
    val withoutBrand = if (brandName.isNotBlank() && clean.startsWith(brandName, ignoreCase = true)) {
        clean.removePrefix(brandName).trim()
    } else {
        clean
    }
    return withoutBrand
        .replace(Regex("\\s+"), " ")
        .split(Regex("[，,（(]"))
        .firstOrNull()
        ?.trim()
        ?.take(16)
        ?.ifBlank { clean.take(16) }
        ?: clean.take(16)
}

private data class ChipColorSet(
    val background: Color,
    val border: Color,
    val content: Color,
)

private val DecisionReasonChipColors = listOf(
    ChipColorSet(
        background = BuyPilotColors.DecisionReasonWarmBackground,
        border = BuyPilotColors.DecisionReasonWarmBorder,
        content = BuyPilotColors.DecisionReasonWarmText,
    ),
    ChipColorSet(
        background = BuyPilotColors.InfoSoft,
        border = BuyPilotColors.DecisionReasonBlueBorder,
        content = BuyPilotColors.DecisionReasonBlueText,
    ),
    ChipColorSet(
        background = BuyPilotColors.DecisionReasonGreenBackground,
        border = BuyPilotColors.DecisionReasonGreenBorder,
        content = BuyPilotColors.DecisionReasonGreenText,
    ),
    ChipColorSet(
        background = BuyPilotColors.DecisionReasonPinkBackground,
        border = BuyPilotColors.DecisionReasonPinkBorder,
        content = BuyPilotColors.DecisionReasonPinkText,
    ),
)

@Composable
private fun DecisionReasonList(
    items: List<String>,
    motionKey: String,
    motionEnabled: Boolean,
    alreadyEntered: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.take(4).forEachIndexed { index, item ->
            StaggeredRevealMotion(
                key = "${motionKey}_decision_reason_$index",
                motionEnabled = motionEnabled,
                alreadyEntered = alreadyEntered,
                delayMillis = DecisionReasonBaseDelayMs + index * DecisionReasonStaggerMs,
                durationMillis = 280,
                initialOffsetY = 16.dp,
            ) {
                DecisionReasonItem(
                    text = item,
                    colorIndex = index,
                )
            }
        }
    }
}

@Composable
private fun DecisionReasonItem(
    text: String,
    colorIndex: Int,
) {
    val colors = DecisionReasonChipColors[colorIndex % DecisionReasonChipColors.size]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background.copy(alpha = 0.62f), RoundedCornerShape(13.dp))
            .border(1.dp, colors.border.copy(alpha = 0.72f), RoundedCornerShape(13.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
                .background(colors.content.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${colorIndex + 1}",
                color = colors.content,
                fontSize = BuyPilotType.Tiny,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(9.dp))
        MarkdownTextBlock(
            content = text,
            modifier = Modifier.weight(1f),
            style = TextStyle(
                color = colors.content,
                fontSize = BuyPilotType.Label,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Immutable
private data class DecisionFeedbackTraceUi(
    val preferredNames: List<String>,
    val excludedNames: List<String>,
)

@Immutable
private data class DecisionScoreItemUi(
    val label: String,
    val value: Double,
)

@Composable
private fun DecisionFeedbackTraceSection(trace: DecisionFeedbackTraceUi) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("你的反馈参与了这次判断")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = BuyPilotColors.SurfaceMuted.copy(alpha = 0.52f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border.copy(alpha = 0.68f)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DecisionFeedbackTraceRow("已偏好", trace.preferredNames, BuyPilotColors.Success)
                DecisionFeedbackTraceRow("已排除", trace.excludedNames, BuyPilotColors.TextMuted)
            }
        }
    }
}

@Composable
private fun DecisionFeedbackTraceRow(
    label: String,
    names: List<String>,
    accent: Color,
) {
    if (names.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            color = accent,
            fontSize = BuyPilotType.Label,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(48.dp),
        )
        Text(
            text = names.joinToString("、"),
            color = BuyPilotColors.TextPrimary,
            fontSize = BuyPilotType.Body,
            lineHeight = 21.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DecisionScoreBreakdownSection(
    items: List<DecisionScoreItemUi>,
    motionKey: String,
    motionEnabled: Boolean,
    alreadyEntered: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("决策评分")
        // Gauge grid: up to 3 per row
        val rows = items.chunked(3)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowItems.forEach { item ->
                        Box(modifier = Modifier.weight(1f)) {
                        StaggeredRevealMotion(
                            key = "${motionKey}_score_${item.label}",
                            motionEnabled = motionEnabled,
                            alreadyEntered = alreadyEntered,
                            delayMillis = 60,
                            durationMillis = 260,
                            initialOffsetY = 8.dp,
                        ) {
                            DecisionScoreGauge(item = item)
                        }
                        }
                    }
                    // fill empty slots in last row
                    repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun DecisionScoreGauge(item: DecisionScoreItemUi) {
    val fraction = item.value.scoreFraction()
    val isRisk = item.label.contains("风险")
    val trackColor = BuyPilotColors.SurfaceMuted
    val fillColor = if (isRisk) BuyPilotColors.Warning else BuyPilotColors.Primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BuyPilotColors.SurfaceCard, RoundedCornerShape(14.dp))
            .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.68f), RoundedCornerShape(14.dp))
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
            Canvas(modifier = Modifier.size(52.dp)) {
                val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                val sweep = 270f
                val startAngle = 135f
                // track
                drawArc(
                    color = trackColor,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = stroke,
                )
                // fill
                if (fraction > 0f) {
                    drawArc(
                        color = fillColor,
                        startAngle = startAngle,
                        sweepAngle = sweep * fraction,
                        useCenter = false,
                        style = stroke,
                    )
                }
            }
            Text(
                text = item.value.scoreLabel(),
                color = BuyPilotColors.TextPrimary,
                fontSize = BuyPilotType.Label,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = item.label,
            color = BuyPilotColors.TextSecondary,
            fontSize = BuyPilotType.Tiny,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DecisionLightRiskNote(text: String) {
    val preview = remember(text) { text.toDecisionRiskPreview() }
    if (preview.body.isBlank()) return
    var showFull by rememberSaveable(preview.body) { androidx.compose.runtime.mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(BuyPilotColors.SurfaceMuted.copy(alpha = 0.34f))
            .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.58f), RoundedCornerShape(13.dp))
            .clickable(role = Role.Button) { showFull = true }
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(BuyPilotColors.Warning.copy(alpha = 0.72f), CircleShape),
            )
            Text(
                text = preview.badge,
                color = BuyPilotColors.TextPrimary,
                fontSize = BuyPilotType.Label,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            preview.sourceLabel?.let {
                Text(
                    text = it,
                    color = BuyPilotColors.TextMuted,
                    fontSize = BuyPilotType.Tiny,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } ?: Spacer(Modifier.weight(1f))
        }
        Text(
            text = preview.body,
            color = BuyPilotColors.TextSecondary,
            fontSize = BuyPilotType.Body,
            lineHeight = 21.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (preview.body.length > 42) {
            Text(
                text = "全文",
                color = BuyPilotColors.PrimaryDark.copy(alpha = 0.82f),
                fontSize = BuyPilotType.Label,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
    if (showFull) {
        ProductTextPreviewDialog(
            title = preview.sourceLabel ?: preview.badge,
            eyebrow = preview.badge,
            body = preview.body,
            rating = preview.rating,
            onDismiss = { showFull = false },
        )
    }
}

private data class DecisionRiskPreview(
    val badge: String,
    val sourceLabel: String?,
    val body: String,
    val rating: Int?,
)

private fun String.toDecisionRiskPreview(): DecisionRiskPreview {
    var clean = withoutMarkdownMarkup()
        .withoutInternalDebugTokens()
        .trim()
    val marker = DecisionRiskRatingMarkerRegex.find(clean)
    var reviewer = marker?.groupValues?.getOrNull(1)?.trim()?.takeIf(String::isNotBlank)
    var rating = marker?.groupValues?.getOrNull(2)?.toIntOrNull()?.coerceIn(1, 5)
    clean = clean.replace(DecisionRiskRatingMarkerRegex, "").trim()

    val leading = DecisionRiskLeadingRatingRegex.find(clean)
    if (leading != null) {
        reviewer = reviewer ?: leading.groupValues.getOrNull(1)?.trim()?.takeIf(String::isNotBlank)
        rating = rating ?: leading.groupValues.getOrNull(2)?.toIntOrNull()?.coerceIn(1, 5)
        clean = clean.replaceFirst(DecisionRiskLeadingRatingRegex, "").trim()
    }

    clean = clean
        .replace(Regex("""^[：:，,、\s]+"""), "")
        .trim()

    val badge = if (rating != null) "低分反馈" else "留意点"
    val source = when {
        !reviewer.isNullOrBlank() && rating != null -> "$reviewer · $rating 分"
        rating != null -> "$rating 分评价"
        else -> null
    }
    return DecisionRiskPreview(
        badge = badge,
        sourceLabel = source,
        body = clean,
        rating = rating,
    )
}

private val DecisionRiskRatingMarkerRegex = Regex("""\[([^\]\[:：]{1,16})\s+评分[:：]\s*(\d{1,2})\]""")
private val DecisionRiskLeadingRatingRegex = Regex("""^\s*(?:用户\s*)?([^\s:：，,\[]{1,18})?\s*评分\s*(\d{1,2})\s*[:：]\s*""")

@Composable
private fun DecisionNotForSection(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        SectionTitle("不适合这些情况")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = BuyPilotColors.SurfaceMuted.copy(alpha = 0.46f),
            shape = RoundedCornerShape(15.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border.copy(alpha = 0.62f)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                items.take(3).forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp)
                                .size(5.dp)
                                .background(BuyPilotColors.TextMuted.copy(alpha = 0.72f), CircleShape),
                        )
                        Text(
                            text = item,
                            color = BuyPilotColors.TextSecondary,
                            fontSize = BuyPilotType.Body,
                            lineHeight = 21.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DecisionReasonChip(
    label: String,
    colorIndex: Int,
    onClick: () -> Unit = {},
) {
    val colors = DecisionReasonChipColors[colorIndex % DecisionReasonChipColors.size]
    Text(
        text = label.withoutMarkdownMarkup(),
        color = colors.content,
        fontSize = BuyPilotType.Label,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(CircleShape)
            .background(colors.background, CircleShape)
            .border(1.dp, colors.border, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
    )
}

@Composable
private fun SmallActionChip(
    label: String,
    onClick: () -> Unit = {},
) {
    Text(
        text = label.withoutMarkdownMarkup(),
        color = BuyPilotColors.TextPrimary,
        fontSize = BuyPilotType.Label,
        lineHeight = 16.sp,
        modifier = Modifier
            .clip(CircleShape)
            .background(BuyPilotColors.SurfaceMuted, CircleShape)
            .border(1.dp, BuyPilotColors.Border, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
    )
}

@Composable
private fun ProductMiniTag(
    label: String,
    active: Boolean = false,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val tagShape = RoundedCornerShape(if (compact) 7.dp else 999.dp)
    Text(
        text = label.withoutMarkdownMarkup(),
        color = if (active) BuyPilotColors.PrimaryDark else BuyPilotColors.TextSecondary.copy(alpha = 0.92f),
        fontSize = BuyPilotType.Tiny,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(
                if (active) {
                    BuyPilotColors.PrimarySoft.copy(alpha = 0.7f)
                } else {
                    BuyPilotColors.ProductMiniTagBackground
                },
                tagShape,
            )
            .border(
                1.dp,
                if (active) BuyPilotColors.PrimarySoft.copy(alpha = 0.82f) else BuyPilotColors.ProductMiniTagBorder,
                tagShape,
            )
            .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = if (compact) 3.dp else 4.dp),
    )
}

@Composable
private fun SwipeAction(
    label: String,
    active: Boolean = false,
) {
    Text(
        text = label,
        color = if (active) BuyPilotColors.PrimaryDark else BuyPilotColors.TextSecondary,
        fontSize = BuyPilotType.Label,
        modifier = Modifier
            .background(if (active) BuyPilotColors.PrimarySoft else BuyPilotColors.SurfaceMuted, CircleShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
internal fun SwipeRoundButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    active: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = tween(durationMillis = 140, easing = MenuEaseOut),
        label = "swipe_round_button_press",
    )
    val containerColor = when {
        !enabled -> BuyPilotColors.SurfaceMuted.copy(alpha = 0.42f)
        active -> BuyPilotColors.PrimarySoft.copy(alpha = 0.82f)
        else -> BuyPilotColors.SurfaceCard
    }
    val contentColor = when {
        !enabled -> BuyPilotColors.TextMuted.copy(alpha = 0.54f)
        active -> BuyPilotColors.PrimaryDark
        else -> BuyPilotColors.TextSecondary
    }
    val borderColor = when {
        !enabled -> BuyPilotColors.Border.copy(alpha = 0.36f)
        active -> BuyPilotColors.Primary.copy(alpha = 0.22f)
        else -> BuyPilotColors.Border.copy(alpha = 0.74f)
    }
    Box(
        modifier = Modifier
            .size(58.dp)
            .scale(scale)
            .shadow(3.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Black.copy(alpha = 0.04f))
            .background(containerColor, CircleShape)
            .border(1.dp, borderColor, CircleShape)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun ProductTag(
    label: String,
    active: Boolean = false,
) {
    Text(
        text = label.withoutMarkdownMarkup(),
        color = if (active) BuyPilotColors.PrimaryDark else BuyPilotColors.TextSecondary,
        fontSize = BuyPilotType.LargeBody,
        lineHeight = 22.sp,
        modifier = Modifier
            .background(if (active) BuyPilotColors.PrimarySoft else BuyPilotColors.SurfaceMuted, RoundedCornerShape(3.dp))
            .border(1.dp, if (active) BuyPilotColors.PrimarySoft else BuyPilotColors.Border, RoundedCornerShape(3.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun GhostButton(
    label: String,
    @DrawableRes leadingIconRes: Int? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .background(BuyPilotColors.SurfaceCard.copy(alpha = 0.72f), CircleShape)
            .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.78f), CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingIconRes?.let {
            Icon(
                painter = painterResource(it),
                contentDescription = null,
                tint = BuyPilotColors.TextSecondary.copy(alpha = 0.78f),
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            label,
            color = BuyPilotColors.TextSecondary,
            fontSize = BuyPilotType.Label,
            lineHeight = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PillLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val displayText = text.withoutInternalDebugTokens().ifBlank { return }
    Text(
        text = displayText,
        color = BuyPilotColors.PrimaryDark,
        fontSize = BuyPilotType.Tiny,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .background(BuyPilotColors.PrimarySoft.copy(alpha = 0.72f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

@Composable
private fun Mascot(
    modifier: Modifier = Modifier,
    size: Int,
    imageSize: Int = size,
    borderWidth: Int = 4,
    shadowElevation: Int = 10,
    shadowAlpha: Float = 0.22f,
    imageModifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .shadow(
                shadowElevation.dp,
                CircleShape,
                ambientColor = Color.Black.copy(alpha = shadowAlpha),
                spotColor = Color.Black.copy(alpha = shadowAlpha),
            )
            .background(Color.White, CircleShape)
            .then(
                if (borderWidth > 0) {
                    Modifier.border(borderWidth.dp, Color.White, CircleShape)
                } else {
                    Modifier
                },
            )
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.redbean_bun_mascot_white),
            contentDescription = "BuyPilot mascot",
            modifier = imageModifier.size(imageSize.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun FinalDecisionPayload.decisionStatusBadge(): DecisionStatusBadge? =
    when (decisionStatus) {
        "no_match" -> DecisionStatusBadge(
            label = stringResource(R.string.decision_badge_no_match),
            accent = BuyPilotColors.Warning,
            surface = BuyPilotColors.WarningSoft,
            showCardWhenEmpty = true,
        )
        "no_suitable_winner" -> DecisionStatusBadge(
            label = stringResource(R.string.decision_badge_no_suitable_winner),
            accent = BuyPilotColors.Warning,
            surface = BuyPilotColors.WarningSoft,
            showCardWhenEmpty = true,
        )
        "needs_more_signal" -> DecisionStatusBadge(
            label = stringResource(R.string.decision_badge_needs_more_signal),
            accent = BuyPilotColors.Info,
            surface = BuyPilotColors.InfoSoft,
            showCardWhenEmpty = true,
        )
        "selected" -> if (confidence == "low") {
            DecisionStatusBadge(
                label = stringResource(R.string.decision_badge_low_confidence),
                accent = BuyPilotColors.PrimaryDark,
                surface = BuyPilotColors.PrimarySoft.copy(alpha = 0.72f),
            )
        } else {
            null
        }
        else -> null
    }

internal fun String.confidenceLabel(): String =
    when (trim().lowercase()) {
        "high" -> "高"
        "medium" -> "中"
        "low" -> "低"
        else -> "待确认"
    }

internal fun String.nextStepLabel(): String =
    when (trim().lowercase()) {
        "adjust_criteria" -> "下一步：调整筛选"
        "replace_deck" -> "下一步：换一组"
        "continue_current_deck" -> "下一步：继续看候选"
        "accept_recommendation" -> "下一步：接受建议"
        else -> "下一步：继续补充偏好"
    }

internal fun String.userFacingEvidenceSourceLabel(fallback: String): String {
    val clean = withoutMarkdownMarkup().withoutInternalDebugTokens().trim()
    return when (clean.lowercase().replace("-", "_")) {
        "official_faq", "faq" -> "官方问答"
        "user_review", "review", "reviews" -> "用户评价"
        "marketing_description", "description", "product_description", "product_chunk", "chunk" -> "商品资料"
        "recommendation_reason", "reason" -> "推荐理由"
        else -> clean.ifBlank { fallback }
    }
}

internal fun Double.clean(): String =
    if (this % 1.0 == 0.0) toInt().toString() else String.format("%.2f", this)
