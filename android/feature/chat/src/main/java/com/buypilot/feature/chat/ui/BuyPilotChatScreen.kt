package com.buypilot.feature.chat.ui

import androidx.annotation.DrawableRes
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.takeOrElse
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.toArgb
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
import com.buypilot.feature.chat.state.ChatInputState
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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.Code
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.Text as MarkdownText
import org.commonmark.parser.Parser
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import kotlin.math.abs
import kotlin.math.roundToInt

private val MenuEaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val MenuEaseIn = CubicBezierEasing(0.3f, 0f, 1f, 1f)
private val ClarificationEaseOut = CubicBezierEasing(0.1f, 1f, 0.1f, 1f)
private val ClarificationFlightEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
private val ProductDetailRevealDistance = 220.dp
private const val ProductDetailSnapThreshold = 0.36f
private const val ProductDetailEnterMs = 560
private const val ProductEvidenceEnterMs = 520
private val CriteriaLeadingNumberRegex = Regex("""^(\d+(?:\.\d+)?)(.*)$""")
private val BudgetBasePresets = listOf(50, 100, 150, 200, 300, 500, 800, 1000)
private val BudgetHighPresets = listOf(1500, 2000, 3000, 5000, 8000, 10000)
private const val DefaultBudgetPreset = 200
private const val ClarificationSelectionHoldMs = 0
private const val ClarificationExitMs = 110
private const val ClarificationFlightMs = 360
private const val ClarificationKeyboardBirthMs = 150
private const val ClarificationTargetSettleMs = 24L
private const val ClarificationTargetFallbackMs = 760L
private val TimelineNearEndThreshold = 96.dp
private val TimelineFollowCorrectionTolerance = 8.dp
private val TimelineSafeGap = 20.dp
private val TimelineAnchorTopGap = 8.dp
private val TimelineAnchorBottomReserve = 420.dp
private val TimelineKeyboardScrimHeight = 64.dp
private val TimelineJumpButtonBottomGap = 18.dp
private val ChipEdgeFadeWidth = 46.dp

private data class PendingClarificationAnswer(
    val nodeKey: String,
    val message: String,
    val selectedOption: String? = null,
    val awaitsFlight: Boolean = false,
    val previousUserMessageKey: String? = null,
    val flightId: Int? = null,
)

private data class ClarificationChipSnapshot(
    val position: Offset,
    val size: Size,
)

private data class ClarificationManualSource(
    val nodeKey: String,
    val snapshot: ClarificationChipSnapshot,
)

private fun LayoutCoordinates.toClarificationSnapshot(): ClarificationChipSnapshot {
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

private data class CriteriaTileSpec(
    val label: String,
    val value: String,
    @DrawableRes val iconRes: Int,
    val accent: Color,
    val glow: Color,
    val background: Color,
    val prominent: Boolean = false,
    val span: CriteriaTileSpan = CriteriaTileSpan.Half,
)

private enum class CriteriaTileSpan {
    Half,
    Full,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyPilotChatScreen(
    state: ChatUiState,
    onInputChanged: (String, Boolean) -> Unit,
    onSendMessage: (String, String?) -> Unit,
    onCriteriaPatch: (JsonObject) -> Unit,
    onCancel: () -> Unit,
    onOpenProductDeck: (String, String?) -> Unit,
    onRetryLastMessage: () -> Unit,
    onEditLastMessage: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var welcomeDismissed by rememberSaveable { mutableStateOf(false) }
    var sheetContent by remember { mutableStateOf<ChatSheetContent?>(null) }
    var sheetExiting by remember { mutableStateOf(false) }
    var sheetTransitionId by remember { mutableStateOf(0) }
    var dismissedClarificationKeys by remember { mutableStateOf(emptySet<String>()) }
    var dismissingClarificationKey by remember { mutableStateOf<String?>(null) }
    var revealedMessageKeys by remember { mutableStateOf(emptySet<String>()) }
    var pendingClarificationAnswer by remember { mutableStateOf<PendingClarificationAnswer?>(null) }
    var activeClarificationFlight by remember { mutableStateOf<ClarificationFlight?>(null) }
    var hiddenFlightMessageKeys by remember { mutableStateOf(emptySet<String>()) }
    var userBubbleSnapshots by remember { mutableStateOf(emptyMap<String, ClarificationChipSnapshot>()) }
    var clarificationManualSource by remember { mutableStateOf<ClarificationManualSource?>(null) }
    var keyboardFlightSnapshot by remember { mutableStateOf<ClarificationChipSnapshot?>(null) }
    var flightSequence by remember { mutableStateOf(0) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var timelineTopPx by remember { mutableStateOf(0f) }
    var composerHeightPx by remember { mutableIntStateOf(0) }
    var composerFocused by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val products = state.nodes.filterIsInstance<ProductDeckNode>().flatMap { it.products }
    val focusManager = LocalFocusManager.current
    val composerFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val showWelcome = state.nodes.isEmpty() && !welcomeDismissed
    val activeClarificationKey = state.nodes
        .filterIsInstance<ClarificationNode>()
        .lastOrNull { it.key !in dismissedClarificationKeys }
        ?.key
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

    fun focusComposer() {
        welcomeDismissed = true
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

    fun sendAndClear(message: String, imageUrl: String? = null) {
        val next = message.trim()
        if (next.isEmpty() && imageUrl == null) return

        welcomeDismissed = true
        onSendMessage(next, imageUrl)
        input = ""
        onInputChanged("", false)
        showAttachmentMenu = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    fun editAndFocus(message: String) {
        val next = message.trim()
        if (next.isEmpty()) return

        welcomeDismissed = true
        showAttachmentMenu = false
        input = next
        onInputChanged(next, false)
        onEditLastMessage(next)
        composerFocusRequester.requestFocus()
        keyboardController?.show()
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

        welcomeDismissed = true
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { rootSize = it }
            .background(BuyPilotColors.SurfaceBg),
    ) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                centered = state.nodes.isNotEmpty(),
                showBack = state.nodes.any { it is CriteriaNode || it is ProductDeckNode || it is FinalDecisionNode },
                showHistory = state.nodes.any { it is ProductDeckNode },
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned { timelineTopPx = it.positionInRoot().y },
            ) {
                ConversationStage(
                    showWelcome = showWelcome,
                    state = state,
                    products = products,
                    onClarificationOption = { label, snapshot ->
                        answerClarification(
                            label.toClarificationUserMessage(),
                            selectedOption = label,
                            chipSnapshot = snapshot,
                        )
                    },
                    onCriteriaEdit = { openSheet(ChatSheetContent.Criteria(it)) },
                    onProductOpen = onOpenProductDeck,
                    onDecisionEvidence = { openSheet(ChatSheetContent.DecisionEvidence(it)) },
                    onRetryLastMessage = onRetryLastMessage,
                    onEditLastMessage = { editAndFocus(it) },
                    onQuickAction = { action ->
                        val patch = action.criteriaPatch
                        if (patch != null) {
                            onCriteriaPatch(patch)
                        } else {
                            sendAndClear(action.label)
                        }
                    },
                    onClarificationManualInput = { focusComposer() },
                    onClarificationManualSource = { nodeKey, snapshot ->
                        clarificationManualSource = ClarificationManualSource(nodeKey, snapshot)
                    },
                    onTimelineDrag = { closeTransientComposerUi() },
                    composerHeightPx = composerHeightPx,
                    imeBottomPx = imeBottomPx,
                    isComposerFocused = composerFocused,
                    dismissingClarificationKey = dismissingClarificationKey,
                    dismissedClarificationKeys = dismissedClarificationKeys,
                    selectedClarificationOption = pendingClarificationAnswer?.selectedOption,
                    hiddenUserMessageKeys = hiddenUserMessageKeysForFlight,
                    activeFlightMessageKey = activeFlightMessageKey,
                    revealedMessageKeys = revealedMessageKeys,
                    onMessageRevealComplete = { revealedMessageKeys = revealedMessageKeys + it },
                    onUserBubblePositioned = { key, snapshot ->
                        if (userBubbleSnapshots[key] != snapshot) {
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
                        .padding(bottom = 120.dp),
                )
            }
        }

        BottomComposer(
            text = input,
            inputState = state.inputState,
            isStreaming = state.isStreaming,
            isAttachmentMenuOpen = showAttachmentMenu,
            focusRequester = composerFocusRequester,
            modifier = Modifier
                .align(Alignment.BottomCenter),
            onAttachmentClick = {
                welcomeDismissed = true
                showAttachmentMenu = !showAttachmentMenu
                focusManager.clearFocus()
            },
            onTextChange = {
                input = it
                onInputChanged(it, false)
            },
            onTextFocus = {
                welcomeDismissed = true
                showAttachmentMenu = false
            },
            onFocusChanged = { composerFocused = it },
            onHeightChanged = { composerHeightPx = it },
            onKeyboardFlightSourceChanged = {
                keyboardFlightSnapshot = it
            },
            onSubmit = {
                if (state.isStreaming) {
                    onCancel()
                    return@BottomComposer
                }
                val next = input.trim()
                if (next.isNotEmpty()) {
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
                        sendAndClear(next)
                    }
                }
            },
        )

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
                                val patch = action.criteriaPatch
                                if (patch != null) {
                                    onCriteriaPatch(patch)
                                } else {
                                    sendAndClear(action.label)
                                }
                            }
                        },
                        onSave = { patch ->
                            dismissSheet { onCriteriaPatch(patch) }
                        },
                    )
                    is ChatSheetContent.DecisionEvidence -> DecisionEvidenceSheet(targetContent.payload)
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

private val WelcomePrompts = listOf(
    WelcomePrompt(R.drawable.ic_search_24, "油皮洁面怎么选？", BuyPilotColors.Info),
    WelcomePrompt(R.drawable.ic_shield_24, "敏感肌面霜，帮我避开酒精香精", BuyPilotColors.Success),
    WelcomePrompt(R.drawable.ic_compare_arrows_24, "帮我对比两款商品，选更稳的", BuyPilotColors.PrimaryDark),
    WelcomePrompt(R.drawable.ic_payments_24, "200 元以内，推荐一支通勤防晒", BuyPilotColors.Warning),
)

@Composable
private fun ConversationStage(
    showWelcome: Boolean,
    state: ChatUiState,
    products: List<ProductCardPayload>,
    onClarificationOption: (String, ClarificationChipSnapshot?) -> Unit,
    onClarificationManualInput: () -> Unit,
    onClarificationManualSource: (String, ClarificationChipSnapshot) -> Unit,
    onCriteriaEdit: (CriteriaCardPayload) -> Unit,
    onProductOpen: (String, String?) -> Unit,
    onDecisionEvidence: (FinalDecisionPayload) -> Unit,
    onRetryLastMessage: () -> Unit,
    onEditLastMessage: (String) -> Unit,
    onQuickAction: (QuickActionPayload) -> Unit,
    onTimelineDrag: () -> Unit,
    composerHeightPx: Int,
    imeBottomPx: Int,
    isComposerFocused: Boolean,
    dismissingClarificationKey: String?,
    dismissedClarificationKeys: Set<String>,
    selectedClarificationOption: String?,
    hiddenUserMessageKeys: Set<String>,
    activeFlightMessageKey: String?,
    revealedMessageKeys: Set<String>,
    onMessageRevealComplete: (String) -> Unit,
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
            WelcomeHome()
        }

        AnimatedVisibility(
            visible = state.nodes.isNotEmpty(),
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 220,
                    delayMillis = 90,
                    easing = FastOutSlowInEasing,
                ),
            ) + slideInVertically(
                animationSpec = tween(
                    durationMillis = 240,
                    delayMillis = 90,
                    easing = FastOutSlowInEasing,
                ),
                initialOffsetY = { it / 32 },
            ),
            exit = fadeOut(animationSpec = tween(durationMillis = 120)),
        ) {
            ChatTimeline(
                state = state,
                products = products,
                onClarificationOption = onClarificationOption,
                onClarificationManualInput = onClarificationManualInput,
                onClarificationManualSource = onClarificationManualSource,
                onCriteriaEdit = onCriteriaEdit,
                onProductOpen = onProductOpen,
                onDecisionEvidence = onDecisionEvidence,
                onRetryLastMessage = onRetryLastMessage,
                onEditLastMessage = onEditLastMessage,
                onQuickAction = onQuickAction,
                onTimelineDrag = onTimelineDrag,
                composerHeightPx = composerHeightPx,
                imeBottomPx = imeBottomPx,
                isComposerFocused = isComposerFocused,
                dismissingClarificationKey = dismissingClarificationKey,
                dismissedClarificationKeys = dismissedClarificationKeys,
                selectedClarificationOption = selectedClarificationOption,
                hiddenUserMessageKeys = hiddenUserMessageKeys,
                activeFlightMessageKey = activeFlightMessageKey,
                revealedMessageKeys = revealedMessageKeys,
                onMessageRevealComplete = onMessageRevealComplete,
                onUserBubblePositioned = onUserBubblePositioned,
                onClarificationCardDismissed = onClarificationCardDismissed,
            )
        }
    }
}

@Composable
private fun AttachmentMenuMotion(
    visible: Boolean,
    modifier: Modifier = Modifier,
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
        AttachmentMenu()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    centered: Boolean,
    showBack: Boolean,
    showHistory: Boolean,
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
            actionIcon = R.drawable.ic_history_24.takeIf { showHistory },
            actionDescription = "查看推荐历史",
            actionTint = BuyPilotColors.TextSecondary.copy(alpha = 0.72f),
            onNavigationClick = {},
            onActionClick = {},
        )
        HorizontalDivider(thickness = 1.dp, color = BuyPilotColors.Border.copy(alpha = 0.46f))
    }
}

@Composable
private fun M3TopAppBarRow(
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
    containerColor: Color = Color.Transparent,
    contentColor: Color = BuyPilotColors.TextPrimary,
    onNavigationClick: () -> Unit = {},
    onActionClick: () -> Unit = {},
) {
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
                        Modifier.padding(horizontal = 72.dp)
                    } else {
                        Modifier.padding(start = 64.dp, end = 72.dp)
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
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(56.dp),
            contentAlignment = Alignment.Center,
        ) {
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
private fun WelcomeHome() {
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
        Spacer(Modifier.height(92.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "开启",
                color = BuyPilotColors.TextPrimary.copy(alpha = 0.86f),
                fontSize = 32.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "你的购物",
                color = BuyPilotColors.Primary,
                fontSize = 52.sp,
                lineHeight = 58.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "新体验🌟",
                color = BuyPilotColors.TextPrimary.copy(alpha = 0.92f),
                fontSize = 30.sp,
                lineHeight = 36.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Spacer(Modifier.weight(1f))
        PromptSuggestions(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 134.dp),
        )
    }
}

@Composable
private fun PromptSuggestions(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WelcomePrompts.forEach { prompt ->
            PromptSuggestionCard(prompt = prompt)
        }
    }
}

@Composable
private fun PromptSuggestionCard(
    prompt: WelcomePrompt,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 38.dp)
            .padding(horizontal = 8.dp),
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

@Composable
private fun ChatTimeline(
    state: ChatUiState,
    products: List<ProductCardPayload>,
    onClarificationOption: (String, ClarificationChipSnapshot?) -> Unit,
    onClarificationManualInput: () -> Unit,
    onClarificationManualSource: (String, ClarificationChipSnapshot) -> Unit,
    onCriteriaEdit: (CriteriaCardPayload) -> Unit,
    onProductOpen: (String, String?) -> Unit,
    onDecisionEvidence: (FinalDecisionPayload) -> Unit,
    onRetryLastMessage: () -> Unit,
    onEditLastMessage: (String) -> Unit,
    onQuickAction: (QuickActionPayload) -> Unit,
    onTimelineDrag: () -> Unit,
    composerHeightPx: Int,
    imeBottomPx: Int,
    isComposerFocused: Boolean,
    dismissingClarificationKey: String?,
    dismissedClarificationKeys: Set<String>,
    selectedClarificationOption: String?,
    hiddenUserMessageKeys: Set<String>,
    activeFlightMessageKey: String?,
    revealedMessageKeys: Set<String>,
    onMessageRevealComplete: (String) -> Unit,
    onUserBubblePositioned: (String, ClarificationChipSnapshot) -> Unit,
    onClarificationCardDismissed: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val isUserDragging by listState.interactionSource.collectIsDraggedAsState()
    var followStreamingText by remember { mutableStateOf(true) }
    var lastHandledUserMessageKey by remember { mutableStateOf<String?>(null) }
    var activeTurnAnchored by remember { mutableStateOf(false) }
    val hasTimelineError = state.nodes.any { it is ErrorNode }
    val timelineBottomPadding = calculateTimelineBottomPadding(
        density = density,
        composerHeightPx = composerHeightPx,
        imeBottomPx = imeBottomPx,
    )
    val jumpButtonBottomPadding = with(density) {
        (composerHeightPx + imeBottomPx + TimelineJumpButtonBottomGap.toPx()).toDp()
    }
    val activeTurnBottomReserve = with(density) {
        maxOf(
            TimelineAnchorBottomReserve.toPx(),
            composerHeightPx + imeBottomPx + 72.dp.toPx(),
        ).toDp()
    }
    val nearEndThresholdPx = with(density) { TimelineNearEndThreshold.toPx() }
    val followCorrectionTolerancePx = with(density) { TimelineFollowCorrectionTolerance.toPx() }
    val timelineBottomPaddingPx = with(density) { timelineBottomPadding.toPx() }
    val anchorTopOffsetPx = with(density) { TimelineAnchorTopGap.toPx().roundToInt() }
    val shouldShowKeyboardScrim = isComposerFocused && imeBottomPx > 0 && !followStreamingText
    val isNearTimelineEnd by remember(
        state.nodes.size,
        state.lastError,
        hasTimelineError,
        timelineBottomPadding,
        nearEndThresholdPx,
    ) {
        derivedStateOf {
            isTimelineNearEnd(
                listState = listState,
                lastContentIndex = state.lastContentIndex(hasTimelineError),
                bottomPaddingPx = timelineBottomPaddingPx,
                thresholdPx = nearEndThresholdPx,
            )
        }
    }

    LaunchedEffect(state.lastUserMessageKey, activeFlightMessageKey) {
        val key = state.lastUserMessageKey ?: return@LaunchedEffect
        if (key == activeFlightMessageKey) return@LaunchedEffect
        val index = state.nodes.indexOfFirst { it.key == key }
        if (index >= 0 && key != lastHandledUserMessageKey) {
            lastHandledUserMessageKey = key
            activeTurnAnchored = true
            followStreamingText = true
            listState.scrollToItem(index = index, scrollOffset = -anchorTopOffsetPx)
            scrollActiveTurnIfNeeded(
                listState = listState,
                lastContentIndex = state.lastContentIndex(hasTimelineError),
                bottomPaddingPx = timelineBottomPaddingPx,
                tolerancePx = followCorrectionTolerancePx,
            )
        }
    }

    LaunchedEffect(isNearTimelineEnd, isUserDragging) {
        if (isUserDragging && !isNearTimelineEnd) {
            followStreamingText = false
            activeTurnAnchored = false
        } else if (isNearTimelineEnd) {
            followStreamingText = true
        }
    }

    LaunchedEffect(isUserDragging) {
        if (isUserDragging) {
            onTimelineDrag()
        }
    }

    LaunchedEffect(state.isStreaming) {
        if (!state.isStreaming) {
            activeTurnAnchored = false
        }
    }

    LaunchedEffect(state.streamingTextKey, state.streamingTextLength) {
        if (followStreamingText && state.streamingTextKey != null && state.nodes.isNotEmpty()) {
            scrollActiveTurnIfNeeded(
                listState = listState,
                lastContentIndex = state.lastContentIndex(hasTimelineError),
                bottomPaddingPx = timelineBottomPaddingPx,
                tolerancePx = followCorrectionTolerancePx,
            )
        }
    }

    LaunchedEffect(state.nodes.size, state.streamingTextKey, state.isStreaming, timelineBottomPadding) {
        if (followStreamingText && state.nodes.isNotEmpty()) {
            scrollActiveTurnIfNeeded(
                listState = listState,
                lastContentIndex = state.lastContentIndex(hasTimelineError),
                bottomPaddingPx = timelineBottomPaddingPx,
                tolerancePx = followCorrectionTolerancePx,
                settleFrames = 2,
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = timelineBottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(state.nodes, key = { it.key }) { node ->
            val hiddenUserMessage = node is UserMessageNode && node.key in hiddenUserMessageKeys
            TimelineItemMotion(animateEnter = !hiddenUserMessage) {
                when (node) {
                    is UserMessageNode -> UserBubble(
                        node = node,
                        hidden = hiddenUserMessage,
                        onPositioned = if (node.key == activeFlightMessageKey) {
                            { onUserBubblePositioned(node.key, it) }
                        } else {
                            null
                        },
                    )
                    is ThinkingNode -> ThinkingBubble(message = node.payload.message)
                    is AiStreamNode -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StreamingAssistantText(
                            content = node.content,
                            done = node.done,
                            onRevealComplete = { onMessageRevealComplete(node.key) },
                        )
                    }
                    is ClarificationNode -> ClarificationBlock(
                        nodeKey = node.key,
                        payload = node.payload,
                        anchorRevealed = node.anchorMessageKey.isBlank() ||
                            node.anchorMessageKey in revealedMessageKeys,
                        dismissed = node.key in dismissedClarificationKeys,
                        dismissing = node.key == dismissingClarificationKey,
                        selectedOption = selectedClarificationOption.takeIf {
                            node.key == dismissingClarificationKey
                        },
                        onOption = onClarificationOption,
                        onManualInput = onClarificationManualInput,
                        onManualSource = { snapshot -> onClarificationManualSource(node.key, snapshot) },
                        onCardDismissed = onClarificationCardDismissed,
                    )
                    is CriteriaNode -> CriteriaSummaryCard(node.payload, onEdit = { onCriteriaEdit(node.payload) })
                    is ProductDeckNode -> ProductRecommendationStrip(
                        node = node,
                        backendBaseUrl = state.backendBaseUrl,
                        swipeState = state.productSwipeStates[node.deckId],
                        awaitingConvergence = node.deckId in state.awaitingConvergenceDeckIds,
                        hasPendingDecision = node.deckId in state.pendingDecisions,
                        onOpen = onProductOpen,
                    )
                    is FinalDecisionNode -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DecisionSummaryCard(
                            payload = node.payload,
                            products = products,
                            backendBaseUrl = state.backendBaseUrl,
                            onEvidence = { onDecisionEvidence(node.payload) },
                            onQuickAction = onQuickAction,
                        )
                    }
                    is CartActionNode -> CartActionCard(node.payload)
                    is ErrorNode -> ErrorCard(
                        node = node,
                        latestUserMessage = state.lastUserMessage,
                        onRetry = onRetryLastMessage,
                        onEditMessage = onEditLastMessage,
                    )
                }
            }
        }

        if (state.lastError != null && !hasTimelineError) {
            item("last_error") {
                Box {
                    InlineSystemNotice(state.lastError)
                }
            }
        }

        if (activeTurnAnchored) {
            item("active_turn_anchor_spacer") {
                Spacer(Modifier.height(activeTurnBottomReserve))
            }
        }
    }

    AnimatedVisibility(
        visible = shouldShowKeyboardScrim,
        enter = fadeIn(animationSpec = tween(durationMillis = 150, easing = MenuEaseOut)),
        exit = fadeOut(animationSpec = tween(durationMillis = 120, easing = MenuEaseIn)),
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = with(density) { (composerHeightPx + imeBottomPx).toDp() })
            .zIndex(1f),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TimelineKeyboardScrimHeight)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                BuyPilotColors.SurfaceBg.copy(alpha = 0f),
                                BuyPilotColors.SurfaceBg.copy(alpha = 0.9f),
                            ),
                        ),
                    ),
            )
        }
    }

    AnimatedVisibility(
        visible = !isNearTimelineEnd,
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
            .fillMaxSize()
            .padding(bottom = jumpButtonBottomPadding)
            .zIndex(2f),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            JumpToLatestButton(
                onClick = {
                    followStreamingText = true
                    activeTurnAnchored = false
                    coroutineScope.launch {
                        val lastContentIndex = state.lastContentIndex(hasTimelineError).coerceAtLeast(0)
                        listState.animateScrollToItem(index = lastContentIndex)
                        scrollActiveTurnIfNeeded(
                            listState = listState,
                            lastContentIndex = lastContentIndex,
                            bottomPaddingPx = timelineBottomPaddingPx,
                            tolerancePx = followCorrectionTolerancePx,
                            settleFrames = 2,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun JumpToLatestButton(
    onClick: () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(46.dp),
        containerColor = BuyPilotColors.SurfaceCard,
        contentColor = BuyPilotColors.TextPrimary,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 4.dp,
            pressedElevation = 2.dp,
        ),
        shape = CircleShape,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_upward_24),
            contentDescription = "回到底部",
            tint = BuyPilotColors.PrimaryDark,
            modifier = Modifier
                .size(20.dp)
                .rotate(180f),
        )
    }
}

@Composable
private fun TimelineItemMotion(
    modifier: Modifier = Modifier,
    animateEnter: Boolean = true,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(!animateEnter) }

    LaunchedEffect(animateEnter) {
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
    hidden: Boolean = false,
    onPositioned: ((ClarificationChipSnapshot) -> Unit)? = null,
) {
    val bubbleShape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (hidden) 0f else 1f),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 304.dp)
                .shadow(4.dp, bubbleShape, ambientColor = Color.Black.copy(alpha = 0.04f))
                .background(BuyPilotColors.Primary, bubbleShape)
                .onGloballyPositioned { coordinates -> onPositioned?.invoke(coordinates.toClarificationSnapshot()) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = node.content.ifBlank { "已发送图片" },
                color = Color.White,
                fontSize = BuyPilotType.Body,
                lineHeight = 21.sp,
            )
        }
    }
}

@Composable
private fun ThinkingBubble(
    message: String,
) {
    val displayMessage = message.withoutTrailingDots().takeIf { it.isNotBlank() } ?: return

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThinkingMascotAnimation()
        Spacer(Modifier.width(10.dp))
        AnimatedContent(
            targetState = displayMessage,
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                ) + slideInVertically(
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                    initialOffsetY = { it / 4 },
                ) togetherWith fadeOut(
                    animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
                ) + slideOutVertically(
                    animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
                    targetOffsetY = { -it / 4 },
                )
            },
            label = "thinking_message",
        ) { targetMessage ->
            ThinkingShimmerText(targetMessage)
        }
    }
}

@Composable
private fun ThinkingMascotAnimation(modifier: Modifier = Modifier) {
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
) {
    val isDarkTheme = isSystemInDarkTheme()
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
    val textStyle = TextStyle(
        fontSize = BuyPilotType.Body,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    )
    val shimmerColor = if (isDarkTheme) {
        BuyPilotColors.SurfaceCard
    } else {
        Color(0xFFA4AAB3)
    }
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

private val PlainMarkdownParser: Parser = Parser.builder().build()

private fun String.withoutMarkdownMarkup(): String {
    val source = trim()
    if (source.isBlank()) return ""

    val builder = StringBuilder()
    val visitor = object : AbstractVisitor() {
        override fun visit(text: MarkdownText) {
            builder.append(text.literal)
        }

        override fun visit(code: Code) {
            builder.append(code.literal)
        }

        override fun visit(codeBlock: FencedCodeBlock) {
            appendSeparated(codeBlock.literal)
        }

        override fun visit(codeBlock: IndentedCodeBlock) {
            appendSeparated(codeBlock.literal)
        }

        override fun visit(link: Link) {
            visitChildren(link)
        }

        override fun visit(image: Image) {
            visitChildren(image)
        }

        override fun visit(softLineBreak: SoftLineBreak) {
            builder.append(' ')
        }

        override fun visit(hardLineBreak: HardLineBreak) {
            builder.append('\n')
        }

        override fun visit(paragraph: Paragraph) {
            appendBlockSeparator()
            visitChildren(paragraph)
        }

        private fun appendSeparated(value: String?) {
            val clean = value?.trim().orEmpty()
            if (clean.isBlank()) return
            appendBlockSeparator()
            builder.append(clean)
        }

        private fun appendBlockSeparator() {
            if (builder.isNotEmpty() && !builder.endsWithWhitespace()) {
                builder.append('\n')
            }
        }
    }
    PlainMarkdownParser.parse(source).accept(visitor)
    return builder.toString()
        .replace(Regex("""[ \t]+\n"""), "\n")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .trim()
}

private fun StringBuilder.endsWithWhitespace(): Boolean =
    isNotEmpty() && last().isWhitespace()

private fun String.needsPlainStreamingRender(): Boolean =
    hasUnclosedFence() ||
        hasPartialTable() ||
        hasUnclosedInlineCode() ||
        hasUnclosedStrongEmphasis()

private fun String.hasUnclosedFence(): Boolean =
    Regex("""(?m)^```""").findAll(this).count() % 2 == 1

private fun String.hasUnclosedInlineCode(): Boolean {
    val withoutFences = replace(Regex("""(?s)```.*?(?:```|$)"""), "")
    return Regex("""(?<!`)`(?!`)""").findAll(withoutFences).count() % 2 == 1
}

private fun String.hasUnclosedStrongEmphasis(): Boolean {
    val withoutCode = replace(Regex("""(?s)```.*?(?:```|$)"""), "")
        .replace(Regex("""`[^`]*(?:`|$)"""), "")
    return Regex("""(?<!\*)\*\*(?!\*)""").findAll(withoutCode).count() % 2 == 1
}

private fun String.hasPartialTable(): Boolean {
    val lines = lineSequence().toList()
    val lastTableStart = lines.indexOfLast { it.trim().startsWith("|") && it.trim().endsWith("|") }
    if (lastTableStart < 0) return false
    val trailing = lines.drop(lastTableStart)
    val lastNonBlank = lines.lastOrNull { it.isNotBlank() }?.trim()
    if (lastNonBlank?.startsWith("|") == true) return true
    if (trailing.size <= 1) return true
    return trailing.any { line ->
        val trimmed = line.trim()
        trimmed.startsWith("|") && !trimmed.endsWith("|")
    }
}

@Composable
private fun AssistantText(content: String) {
    if (content.isBlank()) return
    MarkdownTextBlock(
        content = content,
        style = TextStyle(
            color = BuyPilotColors.TextPrimary,
            fontSize = BuyPilotType.LargeBody,
            lineHeight = 26.sp,
        ),
    )
}

@Composable
private fun StreamingAssistantText(
    content: String,
    done: Boolean = false,
    onRevealComplete: (() -> Unit)? = null,
) {
    LaunchedEffect(done) {
        if (done) {
            onRevealComplete?.invoke()
        }
    }
    if (content.isBlank()) return

    val showPlainStreaming = !done && content.needsPlainStreamingRender()
    if (showPlainStreaming) {
        PlainStreamingTextBlock(content)
    } else {
        MarkdownTextBlock(content)
    }
}

@Composable
private fun StreamingAssistantText(
    content: String,
) {
    StreamingAssistantText(content = content, done = true)
}

@Composable
private fun PlainStreamingTextBlock(
    content: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    if (content.isBlank()) return
    Text(
        text = content,
        color = BuyPilotColors.TextPrimary,
        fontSize = BuyPilotType.LargeBody,
        lineHeight = 26.sp,
        modifier = modifier,
    )
}

@Composable
private fun MarkdownTextBlock(
    content: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    style: TextStyle = TextStyle(
        color = BuyPilotColors.TextPrimary,
        fontSize = BuyPilotType.LargeBody,
        lineHeight = 26.sp,
    ),
) {
    if (content.isBlank()) return
    val context = LocalContext.current
    val markwon = remember(context) {
        Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(HtmlPlugin.create())
            .build()
    }
    val textColor = style.color.takeOrElse { BuyPilotColors.TextPrimary }
    val fontSizePx = with(LocalDensity.current) { style.fontSize.toPx() }
    val lineHeightPx = with(LocalDensity.current) { style.lineHeight.toPx() }
    val typefaceStyle = when (style.fontWeight) {
        FontWeight.Bold,
        FontWeight.ExtraBold,
        FontWeight.Black,
        FontWeight.SemiBold -> android.graphics.Typeface.BOLD
        else -> android.graphics.Typeface.NORMAL
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            TextView(viewContext).apply {
                includeFontPadding = false
                setTextColor(textColor.toArgb())
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSizePx)
                setLineSpacing((lineHeightPx - fontSizePx).coerceAtLeast(0f), 1f)
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, typefaceStyle)
                movementMethod = LinkMovementMethod.getInstance()
                linksClickable = true
            }
        },
        update = { textView ->
            textView.setTextColor(textColor.toArgb())
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSizePx)
            textView.setLineSpacing((lineHeightPx - fontSizePx).coerceAtLeast(0f), 1f)
            textView.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, typefaceStyle)
            markwon.setMarkdown(textView, content)
        },
    )
}

private fun String.toClarificationUserMessage(): String {
    val clean = withoutMarkdownMarkup().trim()
    if (clean.isBlank()) return this
    val skinLabel = clean.withoutSkinSuffix()
    return when {
        clean == "不确定" -> "我还不确定肤质"
        clean in DefaultSkinTypeOptions || clean.contains("肌") || clean.contains("肤") -> "我是${skinLabel}肌肤"
        else -> clean
    }
}

private fun calculateTimelineBottomPadding(
    density: Density,
    composerHeightPx: Int,
    imeBottomPx: Int,
): Dp = with(density) {
    val baselineComposerPx = 136.dp.toPx()
    val safeGapPx = TimelineSafeGap.toPx()
    (maxOf(composerHeightPx.toFloat(), baselineComposerPx) + imeBottomPx + safeGapPx).toDp()
}

private fun ChatUiState.lastContentIndex(hasTimelineError: Boolean): Int =
    nodes.lastIndex + if (lastError != null && !hasTimelineError) 1 else 0

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
            listState.scrollBy(visibleHeight * 0.72f)
        } else {
            val viewportBottom = layoutInfo.viewportEndOffset - bottomPaddingPx.roundToInt()
            val overflow = (lastVisibleItem.offset + lastVisibleItem.size) - viewportBottom
            if (overflow > tolerancePx) {
                listState.scrollBy(overflow.toFloat())
            }
        }
        kotlinx.coroutines.delay(16L)
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

private fun quadraticBezier(start: Float, control: Float, end: Float, fraction: Float): Float {
    val inverse = 1f - fraction
    return inverse * inverse * start + 2f * inverse * fraction * control + fraction * fraction * end
}

private fun segmentProgress(value: Float, start: Float, end: Float): Float =
    ((value - start) / (end - start)).coerceIn(0f, 1f)

@Composable
private fun rememberRouteEnterProgress(
    key: Any?,
    durationMillis: Int,
    delayMillis: Int = 0,
): Float {
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
    return progress.value
}

@Composable
private fun ClarificationBlock(
    nodeKey: String,
    payload: ClarificationPayload,
    anchorRevealed: Boolean,
    dismissed: Boolean,
    dismissing: Boolean,
    selectedOption: String?,
    onOption: (String, ClarificationChipSnapshot?) -> Unit,
    onManualInput: () -> Unit,
    onManualSource: (ClarificationChipSnapshot) -> Unit,
    onCardDismissed: (String) -> Unit,
) {
    val question = payload.question
    val options = payload.suggestedOptions
    var dismissNotified by remember(nodeKey) { mutableStateOf(false) }
    var exitReady by remember(nodeKey) { mutableStateOf(false) }
    val hasSelection = selectedOption != null
    val cardVisible = anchorRevealed && !dismissed && (!dismissing || !exitReady)

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        AnimatedVisibility(
            visible = cardVisible,
            enter = fadeIn(
                animationSpec = tween(durationMillis = 280, easing = ClarificationEaseOut),
            ) + slideInVertically(
                animationSpec = tween(durationMillis = 340, easing = ClarificationEaseOut),
                initialOffsetY = { it / 8 },
            ) + scaleIn(
                animationSpec = tween(durationMillis = 340, easing = ClarificationEaseOut),
                initialScale = 0.96f,
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
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = Color.Black.copy(alpha = 0.035f),
                        spotColor = Color.Black.copy(alpha = 0.055f),
                    ),
                color = BuyPilotColors.SurfaceCard.copy(alpha = 0.96f),
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (hasSelection) {
                        BuyPilotColors.PrimarySoft.copy(alpha = 0.92f)
                    } else {
                        Color(0xFFE8ECF3)
                    },
                ),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ClarificationStatusPill()
                    Text(
                        text = "还差一个关键信息",
                        color = BuyPilotColors.TextPrimary,
                        fontSize = BuyPilotType.Title,
                        lineHeight = 22.5.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    MarkdownTextBlock(
                        content = question,
                        style = TextStyle(
                            color = BuyPilotColors.PrimaryDark,
                            fontSize = BuyPilotType.Body,
                            lineHeight = 21.sp,
                        ),
                    )
                    if (options.isNotEmpty()) {
                        ClarificationOptionScroller(
                            labels = options,
                            selectedLabel = selectedOption,
                            enabled = !dismissing,
                            onClick = onOption,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    ClarificationManualInputRow(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .alpha(if (hasSelection) 0f else 1f),
                        enabled = !dismissing,
                        onClick = onManualInput,
                        onPositioned = onManualSource,
                    )
                }
            }
        }
    }

    LaunchedEffect(dismissing, nodeKey) {
        exitReady = false
        if (dismissing) {
            kotlinx.coroutines.delay(ClarificationSelectionHoldMs.toLong())
            exitReady = true
        }
    }

    LaunchedEffect(dismissing) {
        if (dismissing && !dismissNotified) {
            kotlinx.coroutines.delay((ClarificationSelectionHoldMs + ClarificationExitMs).toLong())
            dismissNotified = true
            onCardDismissed(nodeKey)
        }
    }
}

@Composable
private fun ClarificationOptionScroller(
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
            itemsIndexed(labels, key = { _, label -> label }) { index, label ->
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
private fun EdgeFadedLazyRow(
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
private fun ClarificationStatusPill() {
    Text(
        text = "需要确认",
        color = BuyPilotColors.Info.copy(alpha = 0.92f),
        fontSize = BuyPilotType.Tiny,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .background(BuyPilotColors.Info.copy(alpha = 0.08f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
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

    val t = progress.value
    val birthT = birthProgress.value
    val flyT = t
    val settleT = segmentProgress(t, 0.78f, 1f)
    val morphT = segmentProgress(t, 0.04f, 0.88f)
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
    val width = lerp(startWidthPx, targetWidthPx, morphT)
    val height = lerp(startHeightPx, targetHeightPx, morphT)
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
    val centerX = if (flightStarted) {
        quadraticBezier(startCenterX, controlX, endCenterX, flyT)
    } else {
        startCenterX
    }
    val centerY = if (flightStarted) {
        quadraticBezier(startCenterY, controlY, endCenterY, flyT)
    } else {
        startCenterY
    }
    val x = centerX - width / 2f
    val y = centerY - height / 2f
    val oldTextAlpha = if (flight.fromKeyboard) 0f else (1f - segmentProgress(t, 0.18f, 0.34f)).coerceIn(0f, 1f)
    val newTextAlpha = if (flight.fromKeyboard) {
        birthT
    } else {
        segmentProgress(t, 0.26f, 0.48f)
    }
    val settlePulse = (1f - settleT) * settleT * 4f
    val birthScale = if (flight.fromKeyboard) lerp(0.78f, 1f, birthT) else 1f
    val scaleX = birthScale * (1f + 0.014f * settlePulse)
    val scaleY = birthScale * (1f + 0.008f * settlePulse)
    val cornerRadius = with(density) { lerp(22.dp.toPx(), 18.dp.toPx(), segmentProgress(t, 0.48f, 1f)).toDp() }
    val bubbleShape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = x
                translationY = y
            },
    ) {
        Box(
            modifier = Modifier
                .width(with(density) { width.toDp() })
                .height(with(density) { height.toDp() })
                .scale(scaleX = scaleX, scaleY = scaleY)
                .alpha(if (flight.fromKeyboard) segmentProgress(t, 0f, 0.1f) else 1f)
                .shadow(
                    elevation = if (t < 0.86f) 12.dp else 8.dp,
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
                color = BuyPilotColors.OnPrimary.copy(alpha = oldTextAlpha),
                fontSize = BuyPilotType.Label,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = flight.message.withoutMarkdownMarkup(),
                color = BuyPilotColors.OnPrimary.copy(alpha = newTextAlpha),
                fontSize = BuyPilotType.Body,
                lineHeight = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ClarificationOptionChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: (ClarificationChipSnapshot?) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    var snapshot by remember { mutableStateOf<ClarificationChipSnapshot?>(null) }
    val backgroundColor by animateColorAsState(
        targetValue = when {
            selected -> BuyPilotColors.PrimarySoft.copy(alpha = 0.95f)
            pressed -> Color(0xFFE8EDF6)
            else -> Color(0xFFF3F6FA)
        },
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "clarification_chip_background",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> BuyPilotColors.Primary.copy(alpha = 0.26f)
            pressed -> Color(0xFFD9E0EA)
            else -> Color(0xFFE5EAF1)
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
            .onGloballyPositioned { coordinates ->
                snapshot = coordinates.toClarificationSnapshot()
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick(snapshot)
                },
            )
            .padding(horizontal = 15.dp, vertical = 9.dp),
    )
}

@Composable
private fun ClarificationManualInputRow(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
    onPositioned: (ClarificationChipSnapshot) -> Unit,
) {
    Row(
        modifier = modifier
            .heightIn(min = 38.dp)
            .clip(CircleShape)
            .onGloballyPositioned { coordinates ->
                onPositioned(coordinates.toClarificationSnapshot())
            }
            .clickable(
                enabled = enabled,
                onClickLabel = "聚焦输入框",
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_edit_24),
            contentDescription = null,
            tint = BuyPilotColors.TextMuted.copy(alpha = 0.76f),
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
            Text(
                text = "也可以直接输入补充",
                color = BuyPilotColors.TextSecondary.copy(alpha = 0.8f),
                fontSize = BuyPilotType.Label,
                lineHeight = 16.sp,
            )
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(BuyPilotColors.TextMuted.copy(alpha = 0.32f)),
            )
        }
    }
}

private val DefaultSkinTypeOptions = listOf(
    "油性",
    "干性",
    "混合性",
    "敏感性",
    "中性",
    "痘痘肌",
    "干敏肌",
    "不确定",
)

@Composable
private fun CriteriaSummaryCard(
    payload: CriteriaCardPayload,
    onEdit: () -> Unit,
) {
    val criteria = payload.criteria
    val tiles = criteria.summaryTiles()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (tiles.isNotEmpty()) {
            CriteriaBentoGrid(tiles = tiles)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            GhostButton(label = "修改标准", leadingIconRes = R.drawable.ic_edit_24, onClick = onEdit)
        }
    }
}

@Composable
private fun CriteriaBentoGrid(
    tiles: List<CriteriaTileSpec>,
) {
    val rows = remember(tiles) { planCriteriaTileRows(tiles) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        rows.forEach { rowTiles ->
            val fullWidth = rowTiles.size == 1
            val prominent = rowTiles.any { it.prominent }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        when {
                            prominent -> 112.dp
                            fullWidth -> 88.dp
                            else -> 82.dp
                        },
                    ),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                rowTiles.forEach { tile ->
                    val compact = !tile.prominent && !fullWidth
                    CriteriaTile(
                        label = tile.label,
                        value = tile.value,
                        iconRes = tile.iconRes,
                        accent = tile.accent,
                        glow = tile.glow,
                        background = tile.background,
                        prominent = tile.prominent,
                        compact = compact,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

private fun planCriteriaTileRows(tiles: List<CriteriaTileSpec>): List<List<CriteriaTileSpec>> {
    val rows = mutableListOf<List<CriteriaTileSpec>>()
    val pendingHalf = mutableListOf<CriteriaTileSpec>()

    fun flushHalfRows() {
        var index = 0
        while (index < pendingHalf.size) {
            val remaining = pendingHalf.size - index
            val rowSize = if (remaining == 3) 1 else minOf(2, remaining)
            rows += pendingHalf.subList(index, index + rowSize).toList()
            index += rowSize
        }
        pendingHalf.clear()
    }

    tiles.forEach { tile ->
        if (tile.span == CriteriaTileSpan.Full || tile.shouldUseFullCriteriaRow()) {
            flushHalfRows()
            rows += listOf(tile)
        } else {
            pendingHalf += tile
        }
    }
    flushHalfRows()
    return rows
}

private fun CriteriaTileSpec.shouldUseFullCriteriaRow(): Boolean =
    prominent || label == "摘要" || label == "排除项" || value.length >= 16

@Composable
private fun CriteriaTile(
    label: String,
    value: String,
    @DrawableRes iconRes: Int,
    accent: Color,
    glow: Color,
    background: Color,
    prominent: Boolean = false,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    val horizontalPadding = if (compact) 12.dp else 14.dp
    val verticalPadding = if (compact) 11.dp else 14.dp
    val valueSize = when {
        prominent -> 17.sp
        compact -> 14.sp
        else -> BuyPilotType.Body
    }
    val valueLineHeight = when {
        prominent -> 22.sp
        compact -> 18.sp
        else -> 18.sp
    }
    val iconSize = if (prominent) 16.dp else 15.dp
    val iconContainerSize = if (prominent) 29.dp else 27.dp
    val cardBackground = background.copy(alpha = if (prominent) 0.62f else 0.5f)
    val borderColor = accent.copy(alpha = if (prominent) 0.13f else 0.09f)

    Box(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = shape,
                ambientColor = Color(0xFF8E97A4).copy(alpha = 0.025f),
                spotColor = Color(0xFF8E97A4).copy(alpha = 0.035f),
            )
            .clip(shape)
            .background(cardBackground)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape,
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(iconContainerSize)
                        .clip(RoundedCornerShape(10.dp))
                        .background(glow.copy(alpha = if (prominent) 0.82f else 0.68f))
                        .border(
                            1.dp,
                            accent.copy(alpha = if (prominent) 0.12f else 0.1f),
                            RoundedCornerShape(10.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = accent.copy(alpha = if (prominent) 0.82f else 0.72f),
                        modifier = Modifier.size(iconSize),
                    )
                }
                Text(
                    text = label,
                    color = BuyPilotColors.TextSecondary.copy(alpha = 0.82f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            CriteriaTileValueText(
                value = value,
                prominent = prominent,
                fontSize = valueSize,
                lineHeight = valueLineHeight,
            )
        }
    }
}

@Composable
private fun CriteriaTileValueText(
    value: String,
    prominent: Boolean,
    fontSize: TextUnit,
    lineHeight: TextUnit,
) {
    val numberMatch = CriteriaLeadingNumberRegex.matchEntire(value)
    if (prominent && numberMatch != null) {
        val number = numberMatch.groupValues[1]
        val suffix = numberMatch.groupValues[2]
        val numberSize = 20.sp
        val suffixSize = 16.sp
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = numberSize,
                        fontWeight = FontWeight.Bold,
                    ),
                ) {
                    append(number)
                }
                withStyle(
                    SpanStyle(
                        fontSize = suffixSize,
                        fontWeight = FontWeight.Medium,
                    ),
                ) {
                    append(suffix)
                }
            },
            color = BuyPilotColors.TextPrimary,
            lineHeight = lineHeight,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    } else {
        Text(
            text = value,
            color = BuyPilotColors.TextPrimary,
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = if (prominent) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProductRecommendationStrip(
    node: ProductDeckNode,
    backendBaseUrl: String,
    swipeState: ProductSwipeState?,
    awaitingConvergence: Boolean,
    hasPendingDecision: Boolean,
    onOpen: (String, String?) -> Unit,
) {
    val products = node.products
    if (products.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { products.size })
    val activeIndex = pagerState.currentPage.coerceIn(0, products.lastIndex)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "候选商品",
                    color = BuyPilotColors.TextPrimary,
                    fontSize = BuyPilotType.Title,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = "${products.size} 个",
                color = BuyPilotColors.PrimaryDark,
                fontSize = BuyPilotType.Label,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .background(BuyPilotColors.PrimarySoft.copy(alpha = 0.7f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(238.dp),
            contentPadding = PaddingValues(horizontal = 44.dp),
            pageSpacing = 14.dp,
            beyondViewportPageCount = 1,
        ) { page ->
            val payload = products[page]
            val pageOffset = (
                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                ).coerceIn(-1f, 1f)
            val distance = kotlin.math.abs(pageOffset)
            ProductRecommendationThumb(
                payload = payload,
                backendBaseUrl = backendBaseUrl,
                selected = page == activeIndex,
                onClick = { onOpen(node.deckId, payload.product.productId.takeIf { it.isNotBlank() }) },
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = 1f - distance * 0.055f
                        scaleY = 1f - distance * 0.075f
                        alpha = 1f - distance * 0.18f
                    },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            products.take(6).forEachIndexed { index, _ ->
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
        AnimatedVisibility(
            visible = awaitingConvergence,
            enter = fadeIn(animationSpec = tween(durationMillis = 180, easing = MenuEaseOut)) +
                slideInVertically(
                    animationSpec = tween(durationMillis = 220, easing = MenuEaseOut),
                    initialOffsetY = { it / 4 },
                ),
            exit = fadeOut(animationSpec = tween(durationMillis = 120, easing = MenuEaseIn)),
        ) {
            ProductConvergenceHint(
                readyFromBackend = hasPendingDecision,
            )
        }
    }
}

@Composable
private fun ProductConvergenceHint(
    readyFromBackend: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(
                    if (readyFromBackend) BuyPilotColors.Primary else BuyPilotColors.TextMuted.copy(alpha = 0.45f),
                    CircleShape,
                ),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "回复「继续」我再收敛建议",
            color = BuyPilotColors.TextMuted,
            fontSize = BuyPilotType.Label,
            lineHeight = 17.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProductRecommendationThumb(
    payload: ProductCardPayload,
    backendBaseUrl: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val product = payload.product
    val tags = payload.displayTags().take(2)
    val reason = payload.reason
        .withoutMarkdownMarkup()
        .trim()
        .takeIf { it.isNotBlank() }
    val cardShape = RoundedCornerShape(18.dp)
    val lift by animateDpAsState(
        targetValue = if (selected) 2.dp else 0.dp,
        animationSpec = tween(durationMillis = 180, easing = MenuEaseOut),
        label = "product_thumb_lift",
    )

    Surface(
        modifier = modifier
            .height(218.dp)
            .clip(cardShape)
            .clickable(onClick = onClick),
        color = BuyPilotColors.SurfaceCard,
        shape = cardShape,
        shadowElevation = lift,
        tonalElevation = if (selected) 1.dp else 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border.copy(alpha = 0.62f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .premiumProductCardBackground(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(106.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFAFBFC),
                                    Color(0xFFF1F5F8),
                                    Color(0xFFF9FAFB),
                                ),
                            ),
                        )
                        .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.36f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    ProductImage(
                        product = product,
                        backendBaseUrl = backendBaseUrl,
                        modifier = Modifier
                            .width(92.dp)
                            .height(132.dp)
                            .padding(horizontal = 3.dp, vertical = 6.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
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
                        text = product.name.ifBlank { "推荐商品" },
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
}

private fun Modifier.premiumProductCardBackground(): Modifier =
    drawBehind {
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFCFDFE),
                    BuyPilotColors.SurfaceCard,
                    Color(0xFFF8FAFC),
                ),
            ),
            cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
        )
    }

@Composable
private fun ProductPriceLabel(
    price: String,
    modifier: Modifier = Modifier,
) {
    val match = remember(price) { Regex("""^([¥￥$]?)(\d+(?:\.\d+)?)(.*)$""").find(price) }
    if (match == null) {
        Text(
            text = price,
            color = BuyPilotColors.PrimaryDark,
            fontSize = BuyPilotType.Body,
            lineHeight = 18.sp,
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
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                ) {
                    append(currency)
                }
            }
            withStyle(
                SpanStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            ) {
                append(amount)
            }
            if (suffix.isNotBlank()) {
                withStyle(
                    SpanStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = BuyPilotColors.TextMuted,
                    ),
                ) {
                    append(suffix)
                }
            }
        },
        color = BuyPilotColors.PrimaryDark,
        lineHeight = 20.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun ProductHeroCard(
    payload: ProductCardPayload,
    backendBaseUrl: String,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
) {
    val product = payload.product
    val tags = payload.displayTags().take(3)

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
                    text = product.name.ifBlank { "推荐商品" },
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
private fun DecisionSummaryCard(
    payload: FinalDecisionPayload,
    products: List<ProductCardPayload>,
    backendBaseUrl: String,
    onEvidence: () -> Unit,
    onQuickAction: (QuickActionPayload) -> Unit,
) {
    val winner = payload.winnerProductId
        ?.takeIf { it.isNotBlank() }
        ?.let { winnerId -> products.firstOrNull { it.product.productId == winnerId } }
    val product = winner?.product
    val whyItems = payload.why.map { it.withoutMarkdownMarkup().trim() }.filter { it.isNotBlank() }
    val notForItems = payload.notFor.map { it.withoutMarkdownMarkup().trim() }.filter { it.isNotBlank() }
    val nextActions = payload.nextActions.filter { it.label.isNotBlank() }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AssistantText(payload.summary)
        if (winner != null || !payload.winnerProductId.isNullOrBlank() || whyItems.isNotEmpty() || notForItems.isNotEmpty()) {
            Surface(
                modifier = Modifier.shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = Color(0xFF8E97A4).copy(alpha = 0.07f),
                    spotColor = Color.Black.copy(alpha = 0.04f),
                ),
                color = BuyPilotColors.SurfaceCard,
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 0.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border.copy(alpha = 0.72f)),
            ) {
                Column {
                    if (winner != null || !payload.winnerProductId.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            BuyPilotColors.PrimarySoft.copy(alpha = 0.78f),
                                            Color(0xFFFFF7F0),
                                        ),
                                    ),
                                )
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(BuyPilotColors.Primary.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("✪", color = BuyPilotColors.PrimaryDark, fontSize = BuyPilotType.Label)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "首选推荐",
                                color = BuyPilotColors.PrimaryDark,
                                fontSize = BuyPilotType.Body,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Column(
                        Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (winner != null || !payload.winnerProductId.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                if (product != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(88.dp)
                                            .background(Color(0xFFF6F8FB), RoundedCornerShape(20.dp))
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
                                    product?.name?.takeIf { it.isNotBlank() }?.let { name ->
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
                                    payload.winnerProductId?.takeIf { it.isNotBlank() }?.let { winnerId ->
                                        Text(
                                            text = winnerId,
                                            color = BuyPilotColors.TextMuted,
                                            fontSize = BuyPilotType.Tiny,
                                            lineHeight = 14.sp,
                                            maxLines = 1,
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
                            }
                        }
                        if (whyItems.isNotEmpty()) {
                            SectionTitle("推荐理由", leading = "✓")
                            DecisionReasonList(items = whyItems)
                        }
                        if (notForItems.isNotEmpty()) {
                            WarningBox(notForItems.joinToString("；"))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = onEvidence, contentPadding = PaddingValues(0.dp)) {
                                Text("查看决策依据 ›", color = BuyPilotColors.Info, fontSize = BuyPilotType.Label)
                            }
                        }
                    }
                }
            }
        }
        if (nextActions.isNotEmpty()) {
            ScrollableQuickActionRow(
                actions = nextActions,
                onQuickAction = onQuickAction,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductSwipeModeScreen(
    state: ChatUiState,
    deckId: String,
    initialProductId: String?,
    onBack: () -> Unit,
    onOpenDetail: (String, String) -> Unit,
    onSwipe: (String, String, String, String, String?) -> Unit,
    onUndo: (String) -> Unit,
) {
    val deck = state.findProductDeck(deckId)
    val products = deck?.products.orEmpty()
    val swipeState = state.productSwipeStates[deckId] ?: ProductSwipeState()
    val currentProductId = swipeState.currentProductId
        ?: initialProductId?.takeIf { id -> products.any { it.product.productId == id } }
        ?: products.firstOrNull { it.product.productId !in swipeState.swipedProductIds }?.product?.productId
        ?: products.firstOrNull()?.product?.productId
    val payload = products.firstOrNull { it.product.productId == currentProductId }
    val haptics = LocalHapticFeedback.current
    val cardStackBridge = remember(deckId) { CardStackBridge() }

    Surface(color = BuyPilotColors.SurfaceBg, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ProductSwipeTopBar(
                onBack = onBack,
                canUndo = swipeState.undoStack.isNotEmpty(),
                onUndo = {
                    cardStackBridge.rewind()
                    onUndo(deckId)
                },
            )

            if (deck == null || payload == null) {
                ExpiredRecommendationState(onBack = onBack)
                return@Column
            }

            ProductSwipeHeroCopy()
            ProductSwipeModeContent(
                products = products,
                currentProductId = payload.product.productId,
                backendBaseUrl = state.backendBaseUrl,
                cardStackBridge = cardStackBridge,
                onOpenDetail = { productId -> onOpenDetail(deckId, productId) },
                onDislike = { productId ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSwipe(deckId, productId, "not_interested", "not_interested", null)
                },
                onLike = { productId ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSwipe(deckId, productId, "like", "like", null)
                },
            )
        }
    }
}

@Composable
private fun ProductSwipeTopBar(
    onBack: () -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BuyPilotColors.SurfaceCard)
            .statusBarsPadding(),
    ) {
        M3TopAppBarRow(
            title = "BuyPilot AI",
            titleCentered = true,
            navigationIcon = R.drawable.ic_arrow_back_24,
            navigationDescription = "返回",
            navigationTint = BuyPilotColors.PrimaryDark,
            actionIcon = R.drawable.ic_history_24,
            actionDescription = "撤销上一次选择",
            actionTint = if (canUndo) BuyPilotColors.TextSecondary.copy(alpha = 0.72f) else BuyPilotColors.TextMuted,
            actionEnabled = canUndo,
            onNavigationClick = onBack,
            onActionClick = onUndo,
            modifier = Modifier.fillMaxWidth(),
        )
        HorizontalDivider(thickness = 1.dp, color = BuyPilotColors.Border.copy(alpha = 0.46f))
    }
}

@Composable
private fun ProductSwipeHeroCopy() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 22.dp),
    ) {
        Text(
            text = "找到最佳匹配",
            color = BuyPilotColors.TextPrimary,
            fontSize = 29.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "右滑心动，左滑无感，我正在努力懂你。",
            color = BuyPilotColors.TextSecondary,
            fontSize = 18.sp,
            lineHeight = 25.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProductSwipeModeContent(
    products: List<ProductCardPayload>,
    currentProductId: String,
    backendBaseUrl: String,
    cardStackBridge: CardStackBridge,
    onOpenDetail: (String) -> Unit,
    onDislike: (String) -> Unit,
    onLike: (String) -> Unit,
) {
    val productIds = remember(products) { products.map { it.product.productId } }
    val initialProductId = remember(productIds) {
        currentProductId.takeIf { id -> productIds.any { it == id } }
    }
    val orderedProducts = remember(productIds, initialProductId) {
        val currentIndex = products.indexOfFirst { it.product.productId == initialProductId }.coerceAtLeast(0)
        products.drop(currentIndex) + products.take(currentIndex)
    }
    var activeStackIndex by remember(productIds, initialProductId) { mutableIntStateOf(0) }
    val topPayload = orderedProducts.getOrNull(activeStackIndex)
    val hasActiveCard = topPayload != null
    val latestProducts by rememberUpdatedState(orderedProducts)
    val latestOnLike by rememberUpdatedState(onLike)
    val latestOnDislike by rememberUpdatedState(onDislike)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            ProductCardStackView(
                products = orderedProducts,
                backendBaseUrl = backendBaseUrl,
                bridge = cardStackBridge,
                onOpenDetail = onOpenDetail,
                onSwiped = { direction, position ->
                    val swiped = latestProducts.getOrNull(position) ?: return@ProductCardStackView
                    when (direction) {
                        Direction.Left -> latestOnDislike(swiped.product.productId)
                        Direction.Right -> latestOnLike(swiped.product.productId)
                        else -> Unit
                    }
                },
                onStackPositionChanged = { position ->
                    activeStackIndex = position.coerceIn(0, orderedProducts.size)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SwipeRoundButton(
                iconRes = R.drawable.ic_close_24,
                contentDescription = "不感兴趣",
                active = false,
                enabled = hasActiveCard,
                onClick = { cardStackBridge.swipe(Direction.Left) },
            )
            Spacer(Modifier.width(22.dp))
            SwipeRoundButton(
                iconRes = R.drawable.ic_favorite_24,
                contentDescription = "感兴趣",
                active = true,
                enabled = hasActiveCard,
                onClick = { cardStackBridge.swipe(Direction.Right) },
            )
        }
    }
}

private class CardStackBridge {
    var view: CardStackView? = null
    var manager: CardStackLayoutManager? = null

    fun swipe(direction: Direction) {
        val stackView = view ?: return
        val stackManager = manager ?: return
        stackManager.setSwipeAnimationSetting(
            SwipeAnimationSetting.Builder()
                .setDirection(direction)
                .setDuration(300)
                .setInterpolator(PathInterpolator(0.3f, 0f, 1f, 1f))
                .build(),
        )
        stackView.swipe()
    }

    fun rewind() {
        val stackView = view ?: return
        val stackManager = manager ?: return
        stackManager.setRewindAnimationSetting(
            RewindAnimationSetting.Builder()
                .setDirection(Direction.Bottom)
                .setDuration(320)
                .setInterpolator(PathInterpolator(0.05f, 0.7f, 0.1f, 1f))
                .build(),
        )
        stackView.rewind()
    }
}

@Composable
private fun ProductCardStackView(
    products: List<ProductCardPayload>,
    backendBaseUrl: String,
    bridge: CardStackBridge,
    onOpenDetail: (String) -> Unit,
    onSwiped: (Direction, Int) -> Unit,
    onStackPositionChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val density = LocalDensity.current
    val cardCornerPx = with(density) { 28.dp.toPx() }
    val borderColor = android.graphics.Color.argb(178, 226, 231, 238)
    val surfaceColor = android.graphics.Color.rgb(255, 254, 252)
    val mutedSurfaceColor = android.graphics.Color.rgb(247, 249, 251)
    val primaryColor = android.graphics.Color.rgb(255, 106, 61)
    val primaryDarkColor = android.graphics.Color.rgb(174, 49, 4)
    val textPrimaryColor = android.graphics.Color.rgb(24, 28, 34)
    val textSecondaryColor = android.graphics.Color.rgb(100, 106, 115)
    val textMutedColor = android.graphics.Color.rgb(138, 145, 159)
    val tagBgColor = android.graphics.Color.rgb(242, 247, 250)
    val tagBorderColor = android.graphics.Color.rgb(220, 232, 240)

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            val adapter = ProductCardStackAdapter(
                backendBaseUrl = backendBaseUrl,
                cardCornerPx = cardCornerPx,
                borderColor = borderColor,
                surfaceColor = surfaceColor,
                mutedSurfaceColor = mutedSurfaceColor,
                primaryColor = primaryColor,
                primaryDarkColor = primaryDarkColor,
                textPrimaryColor = textPrimaryColor,
                textSecondaryColor = textSecondaryColor,
                textMutedColor = textMutedColor,
                tagBgColor = tagBgColor,
                tagBorderColor = tagBorderColor,
                onOpenDetail = onOpenDetail,
            )
            val listener = object : CardStackListener {
                override fun onCardDragging(direction: Direction, ratio: Float) = Unit
                override fun onCardSwiped(direction: Direction) {
                    val topPosition = bridge.manager?.getTopPosition() ?: return
                    val position = topPosition - 1
                    onStackPositionChanged(topPosition)
                    onSwiped(direction, position)
                }
                override fun onCardRewound() {
                    onStackPositionChanged(bridge.manager?.getTopPosition() ?: 0)
                }
                override fun onCardCanceled() = Unit
                override fun onCardAppeared(view: View, position: Int) {
                    onStackPositionChanged(position)
                }
                override fun onCardDisappeared(view: View, position: Int) = Unit
            }
            val manager = CardStackLayoutManager(viewContext, listener).apply {
                setStackFrom(StackFrom.None)
                setVisibleCount(3)
                setTranslationInterval(8f)
                setScaleInterval(0.97f)
                setSwipeThreshold(0.26f)
                setMaxDegree(11f)
                setDirections(Direction.HORIZONTAL)
                setCanScrollHorizontal(true)
                setCanScrollVertical(false)
                setSwipeableMethod(SwipeableMethod.AutomaticAndManual)
                setSwipeAnimationSetting(
                    SwipeAnimationSetting.Builder()
                        .setDirection(Direction.Right)
                        .setDuration(300)
                        .setInterpolator(PathInterpolator(0.3f, 0f, 1f, 1f))
                        .build(),
                )
                setRewindAnimationSetting(
                    RewindAnimationSetting.Builder()
                        .setDirection(Direction.Bottom)
                        .setDuration(320)
                        .setInterpolator(PathInterpolator(0.05f, 0.7f, 0.1f, 1f))
                        .build(),
                )
            }
            CardStackView(viewContext).apply {
                clipToPadding = false
                clipChildren = false
                overScrollMode = View.OVER_SCROLL_NEVER
                layoutManager = manager
                this.adapter = adapter
                bridge.view = this
                bridge.manager = manager
                adapter.submit(products)
            }
        },
        update = { stackView ->
            val adapter = stackView.adapter as? ProductCardStackAdapter ?: return@AndroidView
            adapter.backendBaseUrl = backendBaseUrl
            adapter.onOpenDetail = onOpenDetail
            if (adapter.productIds != products.map { it.product.productId }) {
                adapter.submit(products)
                stackView.scrollToPosition(0)
                onStackPositionChanged(0)
            } else {
                adapter.submit(products)
            }
            bridge.view = stackView
            bridge.manager = stackView.layoutManager as? CardStackLayoutManager
        },
    )
}

private class ProductCardStackAdapter(
    var backendBaseUrl: String,
    private val cardCornerPx: Float,
    private val borderColor: Int,
    private val surfaceColor: Int,
    private val mutedSurfaceColor: Int,
    private val primaryColor: Int,
    private val primaryDarkColor: Int,
    private val textPrimaryColor: Int,
    private val textSecondaryColor: Int,
    private val textMutedColor: Int,
    private val tagBgColor: Int,
    private val tagBorderColor: Int,
    var onOpenDetail: (String) -> Unit,
) : RecyclerView.Adapter<ProductCardStackAdapter.ProductCardViewHolder>() {
    private val items = mutableListOf<ProductCardPayload>()
    val productIds: List<String>
        get() = items.map { it.product.productId }

    fun submit(nextItems: List<ProductCardPayload>) {
        items.clear()
        items.addAll(nextItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductCardViewHolder {
        val context = parent.context
        val density = context.resources.displayMetrics.density
        val root = FrameLayout(context).apply {
            clipToPadding = false
            clipChildren = false
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ).apply {
                val horizontal = (7 * density).roundToInt()
                val vertical = (6 * density).roundToInt()
                setMargins(horizontal, vertical, horizontal, vertical)
            }
        }
        val card = FrameLayout(context).apply {
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, cardCornerPx)
                }
            }
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = cardCornerPx
                setColor(surfaceColor)
                setStroke((1 * density).roundToInt().coerceAtLeast(1), borderColor)
            }
            elevation = 2.5f * density
        }
        root.addView(
            card,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        val shell = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            clipChildren = false
        }
        card.addView(
            shell,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        val imageStage = FrameLayout(context).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                setColor(mutedSurfaceColor)
                cornerRadii = floatArrayOf(cardCornerPx, cardCornerPx, cardCornerPx, cardCornerPx, 0f, 0f, 0f, 0f)
            }
        }
        shell.addView(
            imageStage,
            android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )

        val image = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        imageStage.addView(
            image,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                android.view.Gravity.CENTER,
            ).apply {
                leftMargin = (16 * density).roundToInt()
                rightMargin = (16 * density).roundToInt()
                topMargin = (18 * density).roundToInt()
                bottomMargin = (18 * density).roundToInt()
            },
        )

        val content = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding((18 * density).roundToInt(), (15 * density).roundToInt(), (18 * density).roundToInt(), (17 * density).roundToInt())
        }
        shell.addView(
            content,
            android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        val brand = TextView(context).apply {
            includeFontPadding = false
            setTextColor(textMutedColor)
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        content.addView(brand, android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val name = TextView(context).apply {
            includeFontPadding = false
            setTextColor(textPrimaryColor)
            textSize = 21f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            maxLines = 2
            this.setLineSpacing(0.0f, 1.06f)
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        content.addView(
            name,
            android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (10 * density).roundToInt()
            },
        )

        val metaRow = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        content.addView(
            metaRow,
            android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (12 * density).roundToInt()
            },
        )

        val price = TextView(context).apply {
            includeFontPadding = false
            setTextColor(primaryColor)
            textSize = 21f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            maxLines = 1
        }
        metaRow.addView(price, android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val evidence = TextView(context).apply {
            includeFontPadding = false
            setTextColor(textSecondaryColor)
            textSize = 12f
            maxLines = 1
        }
        metaRow.addView(evidence, android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val tagRow = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        content.addView(
            tagRow,
            android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (12 * density).roundToInt()
            },
        )

        val reasonRow = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 14 * density
                setColor(android.graphics.Color.rgb(248, 250, 252))
                setStroke((1 * density).roundToInt().coerceAtLeast(1), android.graphics.Color.argb(150, 226, 231, 238))
            }
            setPadding((10 * density).roundToInt(), (8 * density).roundToInt(), (10 * density).roundToInt(), (8 * density).roundToInt())
        }
        content.addView(
            reasonRow,
            android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (12 * density).roundToInt()
            },
        )

        val reasonLabel = TextView(context).apply {
            text = "推荐理由"
            includeFontPadding = false
            setTextColor(primaryDarkColor)
            textSize = 11f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 999 * density
                setColor(android.graphics.Color.rgb(255, 239, 232))
            }
            setPadding((8 * density).roundToInt(), (5 * density).roundToInt(), (8 * density).roundToInt(), (5 * density).roundToInt())
        }
        reasonRow.addView(
            reasonLabel,
            android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        val reasonText = TextView(context).apply {
            includeFontPadding = false
            setTextColor(textSecondaryColor)
            textSize = 14f
            maxLines = 2
            setLineSpacing((2 * density), 1f)
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        reasonRow.addView(
            reasonText,
            android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = (10 * density).roundToInt()
            },
        )

        val overlays = createSwipeOverlays(context, primaryColor, textPrimaryColor, density)
        root.addView(overlays.first)
        root.addView(overlays.second)

        return ProductCardViewHolder(root, image, reasonRow, reasonText, brand, name, price, evidence, tagRow, tagBgColor, tagBorderColor, textSecondaryColor, primaryDarkColor)
    }

    override fun onBindViewHolder(holder: ProductCardViewHolder, position: Int) {
        val payload = items[position]
        holder.bind(payload, backendBaseUrl, onOpenDetail)
    }

    override fun getItemCount(): Int = items.size

    private fun createSwipeOverlays(
        context: android.content.Context,
        primaryColor: Int,
        textPrimaryColor: Int,
        density: Float,
    ): Pair<View, View> {
        fun overlay(id: Int, label: String, color: Int, gravity: Int): FrameLayout =
            FrameLayout(context).apply {
                this.id = id
                alpha = 0f
                addView(
                    TextView(context).apply {
                        text = label
                        includeFontPadding = false
                        setTextColor(color)
                        textSize = 24f
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        rotation = if (label == "LIKE") -10f else 10f
                        background = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                            cornerRadius = 999 * density
                            setColor(android.graphics.Color.argb(238, 255, 254, 252))
                            setStroke((2 * density).roundToInt(), color)
                        }
                        setPadding((18 * density).roundToInt(), (8 * density).roundToInt(), (18 * density).roundToInt(), (8 * density).roundToInt())
                    },
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        gravity,
                    ).apply {
                        topMargin = (34 * density).roundToInt()
                        leftMargin = (28 * density).roundToInt()
                        rightMargin = (28 * density).roundToInt()
                    },
                )
            }
        return overlay(com.yuyakaido.android.cardstackview.R.id.left_overlay, "PASS", textPrimaryColor, android.view.Gravity.TOP or android.view.Gravity.END) to
            overlay(com.yuyakaido.android.cardstackview.R.id.right_overlay, "LIKE", primaryColor, android.view.Gravity.TOP or android.view.Gravity.START)
    }

    class ProductCardViewHolder(
        itemView: View,
        private val image: ImageView,
        private val reasonRow: android.widget.LinearLayout,
        private val reasonText: TextView,
        private val brand: TextView,
        private val name: TextView,
        private val price: TextView,
        private val evidence: TextView,
        private val tagRow: android.widget.LinearLayout,
        private val tagBgColor: Int,
        private val tagBorderColor: Int,
        private val textSecondaryColor: Int,
        private val primaryDarkColor: Int,
    ) : RecyclerView.ViewHolder(itemView) {
        fun bind(
            payload: ProductCardPayload,
            backendBaseUrl: String,
            onOpenDetail: (String) -> Unit,
        ) {
            val product = payload.product
            itemView.setOnClickListener {
                product.productId.takeIf { it.isNotBlank() }?.let(onOpenDetail)
            }
            image.load(product.imageUrl.resolveProductImageUrl(backendBaseUrl)) {
                placeholder(R.drawable.product_cleanser_sample)
                error(R.drawable.product_cleanser_sample)
                fallback(R.drawable.product_cleanser_sample)
            }
            val reason = payload.reason
                .withoutMarkdownMarkup()
                .trim()
                .takeIf { it.isNotBlank() }
            reasonRow.visibility = if (reason == null) View.GONE else View.VISIBLE
            reasonText.text = reason.orEmpty()
            brand.text = product.brandLabel()
            name.text = product.name.ifBlank { "推荐商品" }
            price.text = product.priceLabel()
            evidence.visibility = View.GONE
            evidence.text = ""
            tagRow.removeAllViews()
            val tags = (product.ingredientTags + product.skinTypeMatch + product.useScenario)
                .map { it.withoutMarkdownMarkup().trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .take(3)
            tagRow.visibility = if (tags.isEmpty()) View.GONE else View.VISIBLE
            tags.forEachIndexed { index, tag ->
                tagRow.addView(createTagView(tagRow.context, tag, active = index == 0))
            }
        }

        private fun createTagView(
            context: android.content.Context,
            label: String,
            active: Boolean,
        ): TextView {
            val density = context.resources.displayMetrics.density
            return TextView(context).apply {
                text = label.withoutMarkdownMarkup()
                includeFontPadding = false
                setTextColor(if (active) primaryDarkColor else textSecondaryColor)
                textSize = 12f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = 999 * density
                    setColor(if (active) android.graphics.Color.rgb(255, 229, 218) else tagBgColor)
                    setStroke((1 * density).roundToInt().coerceAtLeast(1), if (active) android.graphics.Color.rgb(255, 229, 218) else tagBorderColor)
                }
                setPadding((10 * density).roundToInt(), (5 * density).roundToInt(), (10 * density).roundToInt(), (5 * density).roundToInt())
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    rightMargin = (6 * density).roundToInt()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductHeroDetailScreen(
    state: ChatUiState,
    deckId: String,
    productId: String,
    onBack: () -> Unit,
    onOpenEvidence: (String, String) -> Unit,
    onSwipe: (String, String, String, String, String?) -> Unit,
) {
    val payload = state.findProduct(deckId, productId)
    val haptics = LocalHapticFeedback.current

    if (payload == null) {
        Surface(color = BuyPilotColors.SurfaceBg, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                ProductPageTopBar(title = "商品详情", onBack = onBack)
                ExpiredRecommendationState(onBack = onBack)
            }
        }
        return
    }

    val product = payload.product
    val detailListState = rememberLazyListState()
    val density = LocalDensity.current
    val revealDistancePx = with(density) { ProductDetailRevealDistance.toPx() }
    val scrollProgress by remember {
        derivedStateOf {
            val offset = detailListState.firstVisibleItemScrollOffset.toFloat()
            (offset / revealDistancePx).coerceIn(0f, 1f)
        }
    }
    val actionAlpha by animateFloatAsState(
        targetValue = if (scrollProgress < 0.38f) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = MenuEaseOut),
        label = "product_detail_action_alpha",
    )
    val actionsVisible = actionAlpha > 0.05f
    val routeProgress = rememberRouteEnterProgress(
        key = "detail_${deckId}_${productId}",
        durationMillis = ProductDetailEnterMs,
    )
    val backdropEnter = segmentProgress(routeProgress, 0f, 0.82f)
    val contentEnter = segmentProgress(routeProgress, 0.18f, 1f)
    val chromeEnter = segmentProgress(routeProgress, 0.28f, 1f)

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

    Box(modifier = Modifier.fillMaxSize()) {
        ProductCinematicBackdrop(
            product = product,
            backendBaseUrl = state.backendBaseUrl,
            progress = scrollProgress,
            enterProgress = backdropEnter,
            modifier = Modifier
                .fillMaxSize(),
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
            onClick = { onOpenEvidence(deckId, productId) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 16.dp, top = 8.dp)
                .graphicsLayer {
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
                    alpha = contentEnter
                    translationY = (1f - contentEnter) * 42f
                    scaleX = lerp(0.975f, 1f, contentEnter)
                    scaleY = lerp(0.975f, 1f, contentEnter)
                },
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 418.dp, bottom = 118.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item("detail_panel") {
                CinematicProductDetailPanel(
                    payload = payload,
                    progress = scrollProgress,
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .alpha(actionAlpha)
                .graphicsLayer {
                    translationY = (1f - chromeEnter) * 26f
                    scaleX = lerp(0.96f, 1f, chromeEnter)
                    scaleY = lerp(0.96f, 1f, chromeEnter)
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
                .padding(horizontal = 52.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SwipeRoundButton(
                iconRes = R.drawable.ic_close_24,
                contentDescription = "不感兴趣",
                active = false,
                enabled = actionsVisible,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSwipe(deckId, productId, "not_interested", "not_interested", null)
                },
            )
            SwipeRoundButton(
                iconRes = R.drawable.ic_favorite_24,
                contentDescription = "感兴趣",
                active = true,
                enabled = actionsVisible,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSwipe(deckId, productId, "like", "like", null)
                },
            )
        }
    }
}

@Composable
private fun ProductCinematicBackdrop(
    product: ProductPayload,
    backendBaseUrl: String,
    progress: Float,
    enterProgress: Float,
    modifier: Modifier = Modifier,
) {
    val scale = lerp(0.92f, lerp(1.02f, 1.08f, progress), enterProgress)
    val offsetY = lerp(42f, lerp(0f, -54f, progress), enterProgress)
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
                    scaleX = scale
                    scaleY = scale
                    translationY = offsetY
                    alpha = imageAlpha * lerp(0.42f, 1f, enterProgress)
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
    progress: Float,
) {
    val product = payload.product
    val tags = payload.displayTags().take(4)
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if ((payload.rank.takeIf { it > 0 } ?: 1) == 1) "核心匹配" else "#${payload.rank} 匹配",
                color = BuyPilotColors.Primary,
                fontSize = BuyPilotType.Label,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = product.name.ifBlank { "推荐商品" },
                color = BuyPilotColors.TextPrimary,
                fontSize = 28.sp,
                lineHeight = 35.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = product.priceLabel(),
                    color = BuyPilotColors.Primary,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
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
                text = "⌖",
                color = BuyPilotColors.TextMuted,
                fontSize = 20.sp,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.width(10.dp))
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
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(tags, key = { it }) { tag ->
                    ImmersiveTag(label = tag)
                }
            }
        }
        ImmersiveSectionTitle("BuyPilot AI 核心推荐")
        MarkdownTextBlock(
            content = payload.reason.ifBlank { "后端已返回该商品为本轮候选，推荐解释待补充。" },
            style = TextStyle(
                color = BuyPilotColors.TextSecondary,
                fontSize = BuyPilotType.Body,
                lineHeight = 23.sp,
            ),
        )
        Spacer(Modifier.height(20.dp))
        CinematicDetailDivider(progress)
        ImmersiveSectionTitle("详细信息")
        if (payload.riskNotes.isNotEmpty()) {
            ImmersiveWarningBox(payload.riskNotes.joinToString("；"))
        }
        ProductAttributeRowsDark(product = product, payload = payload)
        Spacer(Modifier.height(14.dp))
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
private fun ProductAttributeRowsDark(product: ProductPayload, payload: ProductCardPayload) {
    val rows = listOf(
        "适用对象" to product.skinTypeMatch.ifEmpty { listOf("后端暂未返回") }.joinToString("、"),
        "成分标签" to product.ingredientTags.ifEmpty { payload.tagsFromText() }.joinToString("、"),
        "使用场景" to product.useScenario.ifEmpty { listOf("按当前需求匹配") }.joinToString("、"),
    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEvidenceOverlayScreen(
    state: ChatUiState,
    deckId: String,
    productId: String,
    onBack: () -> Unit,
) {
    val payload = state.findProduct(deckId, productId)

    if (payload == null) {
        Surface(color = BuyPilotColors.SurfaceBg, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                ProductPageTopBar(title = "推荐证据", onBack = onBack)
                ExpiredRecommendationState(onBack = onBack)
            }
        }
        return
    }

    val evidenceItems = payload.evidence.ifEmpty {
        listOf(
            EvidencePayload(
                sourceType = "推荐理由",
                trustLabel = "fallback",
                snippet = payload.reason.ifBlank { "后端暂未返回独立证据，当前仅展示推荐解释。" },
            ),
        )
    }
    val routeProgress = rememberRouteEnterProgress(
        key = "evidence_${deckId}_${productId}",
        durationMillis = ProductEvidenceEnterMs,
    )
    val backdropEnter = segmentProgress(routeProgress, 0f, 0.82f)
    val chromeEnter = segmentProgress(routeProgress, 0.18f, 1f)
    val contentEnter = segmentProgress(routeProgress, 0.16f, 1f)
    Box(modifier = Modifier.fillMaxSize()) {
        ProductImmersiveBackdrop(
            product = payload.product,
            backendBaseUrl = state.backendBaseUrl,
            dimAlpha = 0.78f,
            enterProgress = backdropEnter,
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
                    alpha = chromeEnter
                    translationY = (1f - chromeEnter) * -16f
                    scaleX = lerp(0.92f, 1f, chromeEnter)
                    scaleY = lerp(0.92f, 1f, chromeEnter)
                }
                .zIndex(2f),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = contentEnter
                    translationY = (1f - contentEnter) * 42f
                }
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 30.dp, end = 30.dp, top = 224.dp, bottom = 52.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item("product_snapshot") {
                ProductEvidenceHeader(payload = payload, backendBaseUrl = state.backendBaseUrl)
            }
            item("recommendation_reason") {
                FullEvidenceSection(title = "推荐理由") {
                    Text(
                        text = payload.reason.ifBlank { "后端暂未返回推荐理由。" },
                        color = BuyPilotColors.TextPrimary,
                        fontSize = 25.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            item("product_fields") {
                ProductBackendSnapshot(payload)
            }
            if (payload.reasonAtoms.isNotEmpty()) {
                item("reason_atoms") {
                    FullEvidenceSection(title = "结构化匹配维度") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            payload.reasonAtoms.forEachIndexed { index, atom ->
                                EvidenceKeyValue(
                                    label = "${index + 1}. ${atom.dimension.ifBlank { "dimension" }}",
                                    value = listOf(
                                        atom.value.takeIf { it.isNotBlank() },
                                        atom.text.takeIf { it.isNotBlank() },
                                        atom.evidenceId?.takeIf { it.isNotBlank() }?.let { "证据：$it" },
                                    ).filterNotNull().joinToString("\n"),
                                )
                            }
                        }
                    }
                }
            }
            if (payload.riskNotes.isNotEmpty()) {
                item("risk_notes") {
                    FullEvidenceSection(title = "风险与不适用提醒") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            payload.riskNotes.forEachIndexed { index, note ->
                                EvidenceBulletText(index = index + 1, text = note)
                            }
                        }
                    }
                }
            }
            if (payload.evidence.isEmpty()) {
                item("evidence_fallback_notice") {
                    Text(
                        text = "后端暂未返回独立证据，当前展示推荐理由 fallback。",
                        color = BuyPilotColors.TextMuted,
                        fontSize = BuyPilotType.Label,
                        lineHeight = 18.sp,
                    )
                }
            }
            item("evidence_title") {
                EvidenceEyebrowTitle("原始证据片段", "${evidenceItems.size} 条")
            }
            itemsIndexed(evidenceItems, key = { index, item -> item.evidenceId ?: item.sourceId ?: "${item.sourceType}_$index" }) { index, evidence ->
                ImmersiveEvidenceQuote(
                    evidence = evidence,
                    index = index + 1,
                )
            }
            if (payload.actions.isNotEmpty()) {
                item("actions") {
                    FullEvidenceSection(title = "后端返回动作") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            payload.actions.forEachIndexed { index, action ->
                                EvidenceKeyValue(
                                    label = "${index + 1}. ${action.label.ifBlank { action.action.ifBlank { "action" } }}",
                                    value = listOf(
                                        action.actionId.takeIf { it.isNotBlank() }?.let { "action_id: $it" },
                                        action.action.takeIf { it.isNotBlank() }?.let { "action: $it" },
                                        action.feedbackType?.takeIf { it.isNotBlank() }?.let { "feedback_type: $it" },
                                        action.criteriaPatch?.toString()?.let { "criteria_patch: $it" },
                                    ).filterNotNull().joinToString("\n"),
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
private fun ProductBackendSnapshot(payload: ProductCardPayload) {
    val product = payload.product
    FullEvidenceSection(title = "商品字段快照") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            EvidenceKeyValue("rank", payload.rank.takeIf { it > 0 }?.toString() ?: "未返回")
            EvidenceKeyValue("product_id", product.productId.ifBlank { "未返回" })
            EvidenceKeyValue("商品名", product.name.ifBlank { "未返回" })
            EvidenceKeyValue("价格", product.priceLabel())
            EvidenceKeyValue("currency", product.currency ?: "未返回")
            EvidenceKeyValue("category", product.category.ifBlank { "未返回" })
            EvidenceKeyValue("sub_category", product.subCategory ?: "未返回")
            EvidenceKeyValue("brand", product.brand ?: "未返回")
            EvidenceKeyValue("image_url", product.imageUrl ?: "未返回")
            EvidenceKeyValue("skin_type_match", product.skinTypeMatch.ifEmpty { listOf("未返回") }.joinToString("、"))
            EvidenceKeyValue("ingredient_tags", product.ingredientTags.ifEmpty { listOf("未返回") }.joinToString("、"))
            EvidenceKeyValue("ingredient_avoid", product.ingredientAvoid.ifEmpty { listOf("未返回") }.joinToString("、"))
            EvidenceKeyValue("use_scenario", product.useScenario.ifEmpty { listOf("未返回") }.joinToString("、"))
        }
    }
}

@Composable
private fun FullEvidenceSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EvidenceEyebrowTitle(title)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun EvidenceEyebrowTitle(title: String, trailing: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            color = BuyPilotColors.TextPrimary,
            fontSize = BuyPilotType.Title,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        trailing?.let {
            Text(
                text = it,
                color = BuyPilotColors.TextMuted,
                fontSize = BuyPilotType.Label,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun EvidenceKeyValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            color = BuyPilotColors.TextMuted,
            fontSize = BuyPilotType.Label,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value.ifBlank { "未返回" },
            color = BuyPilotColors.TextPrimary,
            fontSize = BuyPilotType.Body,
            lineHeight = 22.sp,
        )
    }
}

@Composable
private fun EvidenceBulletText(index: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = index.toString(),
            color = BuyPilotColors.Primary,
            fontSize = BuyPilotType.Label,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp),
        )
        Text(
            text = text,
            color = BuyPilotColors.TextPrimary,
            fontSize = BuyPilotType.Body,
            lineHeight = 22.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ImmersiveEvidenceQuote(
    evidence: EvidencePayload,
    index: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "#${index.toString().padStart(2, '0')}",
                color = BuyPilotColors.Primary,
                fontSize = BuyPilotType.Label,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = evidence.sourceType.ifBlank { "商品资料" },
                color = BuyPilotColors.TextMuted,
                fontSize = BuyPilotType.Label,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            evidence.trustLabel?.takeIf { it.isNotBlank() }?.let { label ->
                Text(
                    text = label,
                    color = BuyPilotColors.TextMuted,
                    fontSize = BuyPilotType.Label,
                    lineHeight = 16.sp,
                )
            }
        }
        Text(
            text = "“${evidence.snippet.ifBlank { "暂无证据片段" }}”",
            color = BuyPilotColors.TextPrimary,
            fontSize = 28.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Medium,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            EvidenceKeyValue("source_id", evidence.sourceId ?: "未返回")
            EvidenceKeyValue("evidence_id", evidence.evidenceId ?: "未返回")
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = BuyPilotColors.Border.copy(alpha = 0.92f),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun ProductEvidenceHeader(payload: ProductCardPayload, backendBaseUrl: String) {
    Surface(
        color = BuyPilotColors.SurfaceCard,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(BuyPilotColors.SurfaceMuted, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                ProductImage(
                    product = payload.product,
                    backendBaseUrl = backendBaseUrl,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = payload.product.name.ifBlank { "推荐商品" },
                    color = BuyPilotColors.TextPrimary,
                    fontSize = BuyPilotType.Body,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductPageTopBar(
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
private fun ExpiredRecommendationState(onBack: () -> Unit) {
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
private fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(title)
        Surface(
            color = BuyPilotColors.SurfaceCard,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun ProductAttributeRows(product: ProductPayload, payload: ProductCardPayload) {
    val rows = listOf(
        "适用对象" to product.skinTypeMatch.ifEmpty { listOf("后端暂未返回") }.joinToString("、"),
        "成分标签" to product.ingredientTags.ifEmpty { payload.tagsFromText() }.joinToString("、"),
        "使用场景" to product.useScenario.ifEmpty { listOf("按当前需求匹配") }.joinToString("、"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { (label, value) ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = label,
                    color = BuyPilotColors.TextMuted,
                    fontSize = BuyPilotType.Label,
                    lineHeight = 18.sp,
                    modifier = Modifier.width(72.dp),
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

@Composable
private fun CartActionCard(payload: CartActionPayload) {
    val parts = listOf(payload.action, payload.status, payload.productId)
        .filter { it.isNotBlank() }
    if (parts.isEmpty()) return
    InlineSystemNotice(parts.joinToString(" · "))
}

private data class ErrorPresentation(
    val title: String,
    val reason: String,
    val action: String,
)

private fun ErrorNode.presentation(): ErrorPresentation {
    val normalized = code.uppercase()
    return when {
        "NETWORK" in normalized || "TIMEOUT" in normalized || "CONNECTION" in normalized -> ErrorPresentation(
            title = "网络连接失败",
            reason = message.ifBlank { "当前连接不稳定，商品证据或模型回复没有完整返回。" },
            action = "可以直接重试，或先编辑问题让条件更短。",
        )
        "MODEL" in normalized || "LLM" in normalized || "GENERATION" in normalized -> ErrorPresentation(
            title = "模型生成失败",
            reason = message.ifBlank { "模型这轮没有生成出可用答案。" },
            action = "保留了最近的问题，可以重新生成。",
        )
        "EVIDENCE" in normalized || "INSUFFICIENT" in normalized -> ErrorPresentation(
            title = "证据不足",
            reason = message.ifBlank { "当前商品资料不足以支撑可靠推荐。" },
            action = "建议编辑问题，补充预算、肤质或排除项。",
        )
        "IMAGE" in normalized || "VISION" in normalized -> ErrorPresentation(
            title = "图片识别失败",
            reason = message.ifBlank { "图片主体可能不够清晰，暂时无法稳定识别商品。" },
            action = "可以换一张更清晰的图，或改用文字描述。",
        )
        else -> ErrorPresentation(
            title = "这轮回复中断了",
            reason = message.ifBlank { "系统没有拿到完整结果。" },
            action = "你可以重试，或编辑最近问题后重新发送。",
        )
    }
}

@Composable
private fun ErrorCard(
    node: ErrorNode,
    latestUserMessage: String?,
    onRetry: () -> Unit,
    onEditMessage: (String) -> Unit,
) {
    if (node.message.isBlank() && node.code.isBlank()) return
    val presentation = node.presentation()
    Surface(
        color = BuyPilotColors.Attention,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.PrimarySoft),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                presentation.title,
                color = BuyPilotColors.TextPrimary,
                fontSize = BuyPilotType.Body,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            node.code.takeIf { it.isNotBlank() }?.let {
                Text(it, color = BuyPilotColors.TextMuted, fontSize = BuyPilotType.Label, lineHeight = 18.sp)
            }
            Text(
                presentation.reason,
                color = BuyPilotColors.TextSecondary,
                fontSize = BuyPilotType.Body,
                lineHeight = 20.sp,
            )
            Text(
                presentation.action,
                color = BuyPilotColors.TextMuted,
                fontSize = BuyPilotType.Label,
                lineHeight = 18.sp,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (node.retryable) {
                    GhostButton(label = "重试", onClick = onRetry)
                }
                latestUserMessage?.takeIf { it.isNotBlank() }?.let { message ->
                    GhostButton(
                        label = "编辑问题",
                        leadingIconRes = R.drawable.ic_edit_24,
                        onClick = { onEditMessage(message) },
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineSystemNotice(message: String) {
    Text(
        text = message,
        color = BuyPilotColors.TextSecondary,
        fontSize = BuyPilotType.Label,
        lineHeight = 18.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(BuyPilotColors.SurfaceMuted, RoundedCornerShape(12.dp))
            .padding(12.dp),
    )
}

@Composable
private fun BottomComposer(
    text: String,
    inputState: ChatInputState,
    isStreaming: Boolean,
    isAttachmentMenuOpen: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onAttachmentClick: () -> Unit,
    onTextChange: (String) -> Unit,
    onTextFocus: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onHeightChanged: (Int) -> Unit,
    onKeyboardFlightSourceChanged: (ClarificationChipSnapshot) -> Unit,
    onSubmit: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val placeholder = when (inputState) {
        ChatInputState.Clarifying -> "请回答上面的问题"
        ChatInputState.Streaming -> "正在生成，可随时停止"
        ChatInputState.Error -> "输入后重试"
        else -> "继续追问或描述需求..."
    }
    val canSubmit = isStreaming || text.isNotBlank()
    val hasError = inputState == ChatInputState.Error
    val containerColor by animateColorAsState(
        targetValue = when {
            hasError -> BuyPilotColors.Attention
            isFocused -> BuyPilotColors.SurfaceCard
            else -> BuyPilotColors.SurfaceMuted
        },
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "composer_container_color",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            hasError -> BuyPilotColors.Danger.copy(alpha = 0.58f)
            isFocused -> BuyPilotColors.Primary.copy(alpha = 0.68f)
            else -> BuyPilotColors.Border
        },
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "composer_border_color",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused || hasError) 1.5.dp else 1.dp,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "composer_border_width",
    )
    val attachmentIconRotation by animateFloatAsState(
        targetValue = if (isAttachmentMenuOpen) 45f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "attachment_icon_rotation",
    )

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
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_24),
                        contentDescription = if (isAttachmentMenuOpen) "收起更多输入方式" else "打开更多输入方式",
                        tint = BuyPilotColors.TextSecondary,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(attachmentIconRotation),
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
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
                modifier = Modifier
                    .size(if (streaming) 16.dp else 21.dp)
                    .graphicsLayer {
                        rotationZ = if (streaming) 0f else 0f
                    },
            )
        }
    }
}

@Composable
private fun AttachmentMenu(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(148.dp)
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
        AttachmentAction(R.drawable.ic_image_24, "图片输入", enabled = true)
        AttachmentAction(R.drawable.ic_mic_24, "语音输入", enabled = false)
        AttachmentAction(R.drawable.ic_videocam_24, "实时视频", enabled = false)
    }
}

@Composable
private fun AttachmentAction(
    iconRes: Int,
    label: String,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
    var productType by remember(payload.criteria.criteriaId) { mutableStateOf(criteria.productTypeLabel()) }
    var budgetMax by remember(payload.criteria.criteriaId) { mutableStateOf(criteria.budgetMaxLabel()) }
    var skinType by remember(payload.criteria.criteriaId) { mutableStateOf(criteria.skinTypeLabel()) }
    var useScenario by remember(payload.criteria.criteriaId) { mutableStateOf(criteria.useScenarioLabel()) }
    var exclusions by remember(payload.criteria.criteriaId) { mutableStateOf(criteria.exclusionLabels().joinToString("、")) }

    fun resetFields() {
        productType = criteria.productTypeLabel()
        budgetMax = criteria.budgetMaxLabel()
        skinType = criteria.skinTypeLabel()
        useScenario = criteria.useScenarioLabel()
        exclusions = criteria.exclusionLabels().joinToString("、")
    }

    SheetContentColumn(expandToMaxHeight = false) {
        SheetTitle("编辑购买标准")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EditableFieldBlock(
                label = "核心诉求",
                value = productType,
                placeholder = "例如 洁面乳、防晒霜、蓝牙耳机",
                onValueChange = { productType = it },
                imeAction = ImeAction.Next,
            )
            BudgetSliderBlock(
                value = budgetMax,
                onValueChange = { budgetMax = it },
            )
            EditableFieldBlock(
                label = "适用对象",
                value = skinType,
                placeholder = "例如 油性、敏感、通勤党",
                onValueChange = { skinType = it },
                imeAction = ImeAction.Next,
            )
            EditableFieldBlock(
                label = "场景/用途",
                value = useScenario,
                placeholder = "例如 日常护肤、通勤、跑步训练",
                onValueChange = { useScenario = it },
                imeAction = ImeAction.Next,
            )
            EditableFieldBlock(
                label = "排除项",
                value = exclusions,
                placeholder = "例如 酒精、日系、SK-II",
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
                Text("重置", fontSize = BuyPilotType.Body, fontWeight = FontWeight.Medium)
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
                    text = "保存并重新推荐",
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
private fun DecisionEvidenceSheet(payload: FinalDecisionPayload) {
    val whyItems = payload.why.map { it.withoutMarkdownMarkup().trim() }.filter { it.isNotBlank() }
    val notForItems = payload.notFor.map { it.withoutMarkdownMarkup().trim() }.filter { it.isNotBlank() }
    val alternatives = payload.alternatives.filter {
        it.name.isNotBlank() || it.productId.isNotBlank()
    }
    val nextActions = payload.nextActions.filter { it.label.isNotBlank() }

    SheetContentColumn(expandToMaxHeight = false) {
        SheetTitle("决策依据")

        if (payload.summary.isNotBlank() || !payload.winnerProductId.isNullOrBlank()) {
            DecisionEvidenceSummary(
                summary = payload.summary,
                winnerProductId = payload.winnerProductId,
            )
        }

        if (whyItems.isNotEmpty()) {
            DecisionEvidenceListSection(
                title = "为什么选它",
                items = whyItems,
                numbered = true,
                accentColor = BuyPilotColors.Primary,
                surfaceColor = BuyPilotColors.PrimarySoft.copy(alpha = 0.28f),
            )
        }

        if (notForItems.isNotEmpty()) {
            DecisionEvidenceListSection(
                title = "不适合这些情况",
                items = notForItems,
                numbered = false,
                accentColor = BuyPilotColors.Warning,
                surfaceColor = Color(0xFFFFF7E8),
            )
        }

        if (alternatives.isNotEmpty()) {
            DecisionAlternativesSection(alternatives)
        }

        if (nextActions.isNotEmpty()) {
            DecisionNextActionsSection(nextActions)
        }
    }
}

@Composable
private fun DecisionEvidenceSummary(
    summary: String,
    winnerProductId: String?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                winnerProductId?.takeIf { it.isNotBlank() }?.let { id ->
                    Text(
                        text = id,
                        color = BuyPilotColors.TextMuted,
                        fontSize = BuyPilotType.Tiny,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .widthIn(max = 156.dp)
                            .background(BuyPilotColors.SurfaceMuted.copy(alpha = 0.62f), CircleShape)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
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
private fun DecisionAlternativesSection(alternatives: List<com.buypilot.core.model.AlternativePayload>) {
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
                    DecisionAlternativeRow(index = index, name = alternative.name, productId = alternative.productId)
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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
            productId.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    color = BuyPilotColors.TextMuted,
                    fontSize = BuyPilotType.Tiny,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DecisionNextActionsSection(actions: List<QuickActionPayload>) {
    val labels = actions.mapNotNull { it.label.takeIf { label -> label.isNotBlank() } }
    if (labels.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DecisionEvidenceSectionHeader("下一步动作", actions.size)
        ChipRows(labels = labels)
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
    onValueChange: (String) -> Unit,
) {
    val parsedBudget = value.extractFirstNumber()?.roundToInt()
    val budgetOptions = remember(parsedBudget) { budgetSliderOptions(parsedBudget) }
    val selectedBudget = parsedBudget?.nearestBudgetOption(budgetOptions) ?: DefaultBudgetPreset
    val selectedIndex = budgetOptions.indexOf(selectedBudget).takeIf { it >= 0 } ?: 0
    val sliderInteractionSource = remember { MutableInteractionSource() }
    val isDragging by sliderInteractionSource.collectIsDraggedAsState()
    val displayIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(durationMillis = 260, easing = MenuEaseOut),
        label = "budget_slider_display_index",
    )

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
                    text = "预算上限",
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
    Surface(
        color = BuyPilotColors.SurfaceBg,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillLabel(evidence.sourceType.ifBlank { "证据" })
                evidence.trustLabel?.takeIf { it.isNotBlank() }?.let { PillLabel(it) }
            }
            evidence.evidenceId?.let {
                Text(it, color = BuyPilotColors.TextMuted, fontSize = BuyPilotType.Tiny)
            }
            MarkdownTextBlock(
                content = evidence.snippet.ifBlank { "暂无证据片段" },
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
private fun SectionTitle(title: String, leading: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
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
            itemsIndexed(labels) { index, label ->
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
    onQuickAction: (QuickActionPayload) -> Unit,
) {
    val listState = rememberLazyListState()
    val canScrollBackward by remember(actions.size) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    val canScrollForward by remember(actions.size) {
        derivedStateOf { listState.canScrollForward }
    }
    EdgeFadedLazyRow(
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
            items(actions) { action ->
                SmallActionChip(action.label) { onQuickAction(action) }
            }
        }
    }
}

private data class ChipColorSet(
    val background: Color,
    val border: Color,
    val content: Color,
)

private val DecisionReasonChipColors = listOf(
    ChipColorSet(
        background = Color(0xFFFFF2EA),
        border = Color(0xFFFFCFBA),
        content = Color(0xFFB24617),
    ),
    ChipColorSet(
        background = Color(0xFFEDF5FF),
        border = Color(0xFFCFE3FF),
        content = Color(0xFF245CBA),
    ),
    ChipColorSet(
        background = Color(0xFFEFFBF8),
        border = Color(0xFFCBEFE7),
        content = Color(0xFF16745F),
    ),
    ChipColorSet(
        background = Color(0xFFFFF1F6),
        border = Color(0xFFFAD4E1),
        content = Color(0xFF9C315F),
    ),
)

@Composable
private fun DecisionReasonList(
    items: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.take(4).forEachIndexed { index, item ->
            DecisionReasonItem(text = item, colorIndex = index)
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
                    Color(0xFFF2F7FA)
                },
                tagShape,
            )
            .border(
                1.dp,
                if (active) BuyPilotColors.PrimarySoft.copy(alpha = 0.82f) else Color(0xFFDCE8F0),
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
private fun SwipeRoundButton(
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
    Text(
        text = text,
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
private fun ProductMockImage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.product_cleanser_sample),
            contentDescription = "Product image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun ProductImage(
    product: ProductPayload,
    backendBaseUrl: String,
    modifier: Modifier = Modifier,
) {
    val imageUrl = product.imageUrl.resolveProductImageUrl(backendBaseUrl)
    if (imageUrl == null) {
        ProductMockImage(modifier)
        return
    }
    AsyncImage(
        model = imageUrl,
        contentDescription = product.name.ifBlank { "商品图片" },
        modifier = modifier,
        contentScale = ContentScale.Fit,
        error = painterResource(R.drawable.product_cleanser_sample),
        fallback = painterResource(R.drawable.product_cleanser_sample),
        placeholder = painterResource(R.drawable.product_cleanser_sample),
    )
}

private fun ChatUiState.findProductDeck(deckId: String): ProductDeckNode? =
    nodes.filterIsInstance<ProductDeckNode>().firstOrNull { it.deckId == deckId }

private fun ChatUiState.findProduct(deckId: String, productId: String): ProductCardPayload? =
    findProductDeck(deckId)?.products?.firstOrNull { it.product.productId == productId }

private fun com.buypilot.core.model.CriteriaPayload.budgetLabel(): String {
    val max = budgetMax ?: constraints?.budgetMax
    val min = budgetMin ?: constraints?.budgetMin
    return when {
        min != null && max != null -> "¥${min.clean()}-${max.clean()}"
        max != null -> "${max.clean()}元以内"
        min != null -> "¥${min.clean()}以上"
        else -> ""
    }
}

private fun com.buypilot.core.model.CriteriaPayload.summaryTiles(): List<CriteriaTileSpec> =
    buildList {
        fun addTile(
            label: String,
            value: String,
            @DrawableRes iconRes: Int,
            accent: Color,
            glow: Color,
            background: Color,
            prominent: Boolean = false,
            span: CriteriaTileSpan = CriteriaTileSpan.Half,
        ) {
            val cleanValue = value.withoutMarkdownMarkup().trim()
            if (cleanValue.isBlank()) return
            add(
                CriteriaTileSpec(
                    label = label,
                    value = cleanValue,
                    iconRes = iconRes,
                    accent = accent,
                    glow = glow,
                    background = background,
                    prominent = prominent,
                    span = span,
                ),
            )
        }

        addTile(
            label = "摘要",
            value = summary,
            iconRes = R.drawable.ic_search_24,
            accent = Color(0xFFE0643B),
            glow = Color(0xFFFFEFE8),
            background = Color(0xFFFFFAF6),
            prominent = true,
            span = CriteriaTileSpan.Full,
        )
        addTile(
            label = "品类",
            value = category,
            iconRes = R.drawable.ic_search_24,
            accent = Color(0xFFE0643B),
            glow = Color(0xFFFFEFE8),
            background = Color(0xFFFFFAF6),
            prominent = isEmpty(),
        )
        addTile(
            label = "核心诉求",
            value = productTypeLabel(),
            iconRes = R.drawable.ic_search_24,
            accent = Color(0xFFE0643B),
            glow = Color(0xFFFFEFE8),
            background = Color(0xFFFFFAF6),
            prominent = isEmpty(),
        )
        addTile(
            label = "适用对象",
            value = skinTypeLabel(),
            iconRes = R.drawable.ic_shield_24,
            accent = Color(0xFF4F86D8),
            glow = Color(0xFFEEF5FF),
            background = Color(0xFFF8FBFF),
            prominent = isEmpty(),
        )
        addTile(
            label = "预算",
            value = budgetLabel(),
            iconRes = R.drawable.ic_payments_24,
            accent = Color(0xFFB87920),
            glow = Color(0xFFFFF4DE),
            background = Color(0xFFFFFBF2),
            prominent = isEmpty(),
        )
        addTile(
            label = "场景/用途",
            value = useScenarioLabel(),
            iconRes = R.drawable.ic_history_24,
            accent = Color(0xFF8B96A5),
            glow = Color(0xFFF2F4F7),
            background = Color(0xFFFAFBFC),
            prominent = isEmpty(),
        )
        addTile(
            label = "排除项",
            value = exclusionLabels().joinToString("、"),
            iconRes = R.drawable.ic_block_24,
            accent = Color(0xFFC75B76),
            glow = Color(0xFFFFF0F4),
            background = Color(0xFFFFFAFC),
            prominent = isEmpty(),
            span = CriteriaTileSpan.Full,
        )
    }

private fun com.buypilot.core.model.CriteriaPayload.productTypeLabel(): String =
    productType.orEmpty().ifBlank { constraints?.productType.orEmpty() }

private fun com.buypilot.core.model.CriteriaPayload.skinTypeLabel(): String =
    skinType.orEmpty().ifBlank { constraints?.skinType.orEmpty() }

private fun com.buypilot.core.model.CriteriaPayload.useScenarioLabel(): String =
    useScenario.firstOrNull().orEmpty().ifBlank { constraints?.useScenario.orEmpty() }

private fun com.buypilot.core.model.CriteriaPayload.budgetMaxLabel(): String {
    val max = budgetMax ?: constraints?.budgetMax
    return max?.clean().orEmpty()
}

private fun com.buypilot.core.model.CriteriaPayload.exclusionLabels(): List<String> =
    (
        ingredientAvoid + constraints?.ingredientAvoid.orEmpty() +
            brandAvoid + constraints?.brandAvoid.orEmpty() +
            originAvoid + constraints?.originAvoid.orEmpty()
        )
        .map { it.withoutAvoidPrefix().trim() }
        .filter { it.isNotBlank() }
        .distinct()

private fun buildCriteriaPatch(
    productType: String,
    budgetMax: String,
    skinType: String,
    useScenario: String,
    exclusions: String,
): JsonObject {
    val exclusionItems = exclusions.split('、', ',', '，', ';', '；', '\n')
        .map { it.withoutAvoidPrefix().trim() }
        .filter { it.isNotBlank() }
        .distinct()
    val originAvoid = exclusionItems.filter { it.looksLikeOriginAvoidance() }
    val ingredientAvoid = exclusionItems.filterNot { it.looksLikeOriginAvoidance() || it.looksLikeBrandAvoidance() }
    val brandAvoid = exclusionItems.filter { it.looksLikeBrandAvoidance() }
    return buildJsonObject {
        putJsonObject("constraints") {
            productType.trim().takeIf { it.isNotBlank() }?.let { put("product_type", it) }
            budgetMax.extractFirstNumber()?.let { put("budget_max", it) }
            skinType.trim().withoutSkinSuffix().takeIf { it.isNotBlank() }?.let { put("skin_type", it) }
            useScenario.trim().takeIf { it.isNotBlank() }?.let { put("use_scenario", it) }
            if (ingredientAvoid.isNotEmpty()) {
                put(
                    "ingredient_avoid",
                    buildJsonArray {
                        ingredientAvoid.forEach { add(JsonPrimitive(it)) }
                    },
                )
            }
            if (brandAvoid.isNotEmpty()) {
                put(
                    "brand_avoid",
                    buildJsonArray {
                        brandAvoid.forEach { add(JsonPrimitive(it)) }
                    },
                )
            }
            if (originAvoid.isNotEmpty()) {
                put(
                    "origin_avoid",
                    buildJsonArray {
                        originAvoid.forEach { add(JsonPrimitive(it)) }
                    },
                )
            }
        }
    }
}

private fun String.withoutAvoidPrefix(): String =
    trim()
        .removePrefix("不要含")
        .removePrefix("不要")
        .removePrefix("排除")

private fun String.withoutSkinSuffix(): String =
    trim()
        .removeSuffix("肌肤")
        .removeSuffix("肤质")
        .removeSuffix("肌")

private fun String.extractFirstNumber(): Double? =
    Regex("""\d+(?:\.\d+)?""").find(this)?.value?.toDoubleOrNull()

private fun budgetSliderOptions(currentBudget: Int?): List<Int> {
    val positiveBudget = currentBudget?.takeIf { it > 0 }
    val ceiling = when {
        positiveBudget == null -> BudgetBasePresets.last()
        positiveBudget <= BudgetBasePresets.last() -> BudgetBasePresets.last()
        else -> BudgetHighPresets.firstOrNull { it >= positiveBudget } ?: positiveBudget.roundUpBudgetCeiling()
    }
    return (BudgetBasePresets + BudgetHighPresets.filter { it <= ceiling } + listOfNotNull(positiveBudget))
        .distinct()
        .sorted()
}

private fun Int.nearestBudgetOption(options: List<Int>): Int =
    options.minByOrNull { abs(it - this) } ?: this

private fun Int.roundUpBudgetCeiling(): Int {
    val step = 5000
    return ((this + step - 1) / step) * step
}

private fun List<Int>.midBudgetLabel(): String {
    val midBudget = firstOrNull { it >= 500 } ?: get(size / 2)
    return "¥$midBudget"
}

private fun String.looksLikeOriginAvoidance(): Boolean =
    listOf("日系", "日本", "韩系", "韩国", "欧美", "国产", "进口").any { it in this }

private fun String.looksLikeBrandAvoidance(): Boolean =
    any { it in 'A'..'Z' || it in 'a'..'z' } || contains("-") || contains("·")

private fun com.buypilot.core.model.ProductPayload.priceLabel(): String =
    price?.let { "${currency.priceSymbol()}${it.clean()}" } ?: "价格待确认"

private fun com.buypilot.core.model.ProductPayload.priceLabelOrNull(): String? =
    price?.let { "${currency.priceSymbol()}${it.clean()}" }

private fun String?.priceSymbol(): String =
    when (this?.trim()?.uppercase()) {
        null, "", "CNY", "RMB", "CN¥", "￥", "¥" -> "¥"
        "USD", "$" -> "$"
        else -> this.trim()
    }

private fun ProductPayload.brandLabel(): String =
    brand?.takeIf { it.isNotBlank() }
        ?: subCategory?.takeIf { it.isNotBlank() }
        ?: category.takeIf { it.isNotBlank() }
        ?: "BuyPilot 推荐"

private fun ProductCardPayload.displayTags(): List<String> =
    (product.ingredientTags + product.skinTypeMatch + product.useScenario)
        .map { it.withoutMarkdownMarkup().trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .ifEmpty { tagsFromText() }
        .take(6)

private fun ProductCardPayload.tagsFromText(): List<String> {
    val text = listOf(reason, riskNotes.joinToString(" ")).joinToString(" ")
    return listOf(
        "预算友好" to listOf("预算", "价格", "便宜", "性价比"),
        "温和" to listOf("温和", "敏感", "不刺激"),
        "控油" to listOf("控油", "油皮", "清爽"),
        "证据充分" to listOf("证据", "评价", "资料"),
        "风险可控" to listOf("风险", "注意", "谨慎"),
    ).filter { (_, needles) -> needles.any { it in text } }
        .map { it.first }
        .ifEmpty { listOf("需求匹配", "可进一步确认") }
        .take(2)
}

private fun String?.resolveProductImageUrl(backendBaseUrl: String): String? {
    val raw = this?.trim()?.takeIf { it.isNotBlank() } ?: return null
    if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
    if (raw.startsWith("/assets/products/") || raw.startsWith("/uploads/")) {
        return backendBaseUrl.trimEnd('/') + raw
    }
    return raw
}

private fun Double.clean(): String =
    if (this % 1.0 == 0.0) toInt().toString() else String.format("%.2f", this)

private sealed interface ChatSheetContent {
    data class Criteria(val payload: CriteriaCardPayload) : ChatSheetContent
    data class DecisionEvidence(val payload: FinalDecisionPayload) : ChatSheetContent
}
