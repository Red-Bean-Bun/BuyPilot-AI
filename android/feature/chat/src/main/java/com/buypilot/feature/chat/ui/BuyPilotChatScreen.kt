package com.buypilot.feature.chat.ui

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.buypilot.core.model.CartActionPayload
import com.buypilot.core.model.ClarificationPayload
import com.buypilot.core.model.CriteriaCardPayload
import com.buypilot.core.model.EvidencePayload
import com.buypilot.core.model.FinalDecisionPayload
import com.buypilot.core.model.ProductCardPayload
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
import com.buypilot.feature.chat.model.ThinkingNode
import com.buypilot.feature.chat.model.UserMessageNode
import com.buypilot.feature.chat.state.ChatInputState
import com.buypilot.feature.chat.state.ChatUiState
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val MenuEaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val MenuEaseIn = CubicBezierEasing(0.3f, 0f, 1f, 1f)
private val ClarificationEaseOut = CubicBezierEasing(0.1f, 1f, 0.1f, 1f)
private val ClarificationFlightEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
private val CriteriaLeadingNumberRegex = Regex("""^(\d+(?:\.\d+)?)(.*)$""")
private val BudgetBasePresets = listOf(50, 100, 150, 200, 300, 500, 800, 1000)
private val BudgetHighPresets = listOf(1500, 2000, 3000, 5000, 8000, 10000)
private const val DefaultBudgetPreset = 200
private const val ClarificationSelectionHoldMs = 0
private const val ClarificationExitMs = 110
private const val ClarificationFlightMs = 360
private const val ClarificationTargetSettleMs = 24L
private const val ClarificationTargetFallbackMs = 760L
private const val StreamRevealCharsPerFrame = 1
private const val StreamRevealFrameDelayMs = 24L

private enum class MarkdownInlineStyle {
    Normal,
    Bold,
    Italic,
    Code,
}

private data class MarkdownInlineSegment(
    val text: String,
    val style: MarkdownInlineStyle,
)

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
    val targetMessageKey: String? = null,
    val messageSent: Boolean = false,
)

private data class CriteriaTileSpec(
    val label: String,
    val value: String,
    @DrawableRes val iconRes: Int,
    val accent: Color,
    val glow: Color,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyPilotChatScreen(
    state: ChatUiState,
    onInputChanged: (String, Boolean) -> Unit,
    onSendMessage: (String, String?) -> Unit,
    onCriteriaPatch: (JsonObject) -> Unit,
    onCancel: () -> Unit,
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
    var flightSequence by remember { mutableStateOf(0) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var timelineTopPx by remember { mutableStateOf(0f) }
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

    fun dismissComposerFocus() {
        showAttachmentMenu = false
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
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
    }

    fun answerClarification(
        message: String,
        selectedOption: String? = null,
        chipSnapshot: ClarificationChipSnapshot? = null,
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
        val shouldFly = selectedOption != null && chipSnapshot != null
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
        val snapshot = chipSnapshot
        if (snapshot != null && flightId != null) {
            activeClarificationFlight = ClarificationFlight(
                id = flightId,
                nodeKey = nodeKey,
                option = selectedOption ?: next,
                message = next,
                startPosition = snapshot.position,
                startSize = snapshot.size,
                previousUserMessageKey = previousUserMessageKey,
            )
        }
        dismissingClarificationKey = nodeKey
    }

    fun finishClarificationDismissal(nodeKey: String) {
        dismissedClarificationKeys = dismissedClarificationKeys + nodeKey
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
                    onProductOpen = { openSheet(ChatSheetContent.Product(it)) },
                    onProductEvidence = { openSheet(ChatSheetContent.ProductEvidence(it)) },
                    onDecisionEvidence = { openSheet(ChatSheetContent.DecisionEvidence(it)) },
                    onQuickAction = { action ->
                        val patch = action.criteriaPatch
                        if (patch != null) {
                            onCriteriaPatch(patch)
                        } else {
                            sendAndClear(action.label)
                        }
                    },
                    onClarificationManualInput = { focusComposer() },
                    onTimelineDrag = { dismissComposerFocus() },
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
                .align(Alignment.BottomCenter)
                .imePadding(),
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
            onSubmit = {
                if (state.isStreaming) {
                    onCancel()
                    return@BottomComposer
                }
                val next = input.trim()
                if (next.isNotEmpty()) {
                    if (activeClarificationKey != null) {
                        answerClarification(next)
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
                    is ChatSheetContent.Product -> ProductDetailSheet(
                        payload = targetContent.payload,
                        onEvidence = { openSheet(ChatSheetContent.ProductEvidence(targetContent.payload)) },
                        onAction = { action ->
                            dismissSheet { sendAndClear(action.label) }
                        },
                    )
                    is ChatSheetContent.ProductEvidence -> ProductEvidenceSheet(targetContent.payload)
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
    onCriteriaEdit: (CriteriaCardPayload) -> Unit,
    onProductOpen: (ProductCardPayload) -> Unit,
    onProductEvidence: (ProductCardPayload) -> Unit,
    onDecisionEvidence: (FinalDecisionPayload) -> Unit,
    onQuickAction: (QuickActionPayload) -> Unit,
    onTimelineDrag: () -> Unit,
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
                onCriteriaEdit = onCriteriaEdit,
                onProductOpen = onProductOpen,
                onProductEvidence = onProductEvidence,
                onDecisionEvidence = onDecisionEvidence,
                onQuickAction = onQuickAction,
                onTimelineDrag = onTimelineDrag,
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
    CenterAlignedTopAppBar(
        modifier = Modifier.fillMaxWidth(),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BuyPilotColors.SurfaceCard,
            scrolledContainerColor = BuyPilotColors.SurfaceCard,
            navigationIconContentColor = BuyPilotColors.TextPrimary,
            titleContentColor = BuyPilotColors.TextPrimary,
            actionIconContentColor = BuyPilotColors.PrimaryDark,
        ),
        title = {
            Text(
                text = if (centered) "BuyPilot AI" else "BuyPilot",
                color = BuyPilotColors.TextPrimary,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = {}) {
                Icon(
                    painter = painterResource(
                        if (showBack) R.drawable.ic_arrow_back_24 else R.drawable.ic_menu_24,
                    ),
                    contentDescription = if (showBack) "返回" else "打开菜单",
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        actions = {
            if (showHistory) {
                IconButton(onClick = {}) {
                    Icon(
                        painter = painterResource(R.drawable.ic_history_24),
                        contentDescription = "查看推荐历史",
                        modifier = Modifier.size(24.dp),
                    )
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
        },
    )
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
    onCriteriaEdit: (CriteriaCardPayload) -> Unit,
    onProductOpen: (ProductCardPayload) -> Unit,
    onProductEvidence: (ProductCardPayload) -> Unit,
    onDecisionEvidence: (FinalDecisionPayload) -> Unit,
    onQuickAction: (QuickActionPayload) -> Unit,
    onTimelineDrag: () -> Unit,
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
    val isUserDragging by listState.interactionSource.collectIsDraggedAsState()
    var followStreamingText by remember { mutableStateOf(true) }
    var lastHandledUserMessageKey by remember { mutableStateOf<String?>(null) }
    val isNearTimelineEnd by remember(state.nodes.size, state.lastError) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf true
            val lastContentIndex = state.nodes.lastIndex + if (state.lastError != null) 1 else 0
            lastVisibleIndex >= lastContentIndex - 1
        }
    }

    LaunchedEffect(state.lastUserMessageKey, activeFlightMessageKey) {
        val key = state.lastUserMessageKey ?: return@LaunchedEffect
        if (key == activeFlightMessageKey) return@LaunchedEffect
        val index = state.nodes.indexOfFirst { it.key == key }
        if (index >= 0 && key != lastHandledUserMessageKey) {
            lastHandledUserMessageKey = key
            followStreamingText = true
            listState.scrollToItem(index = index, scrollOffset = 0)
        }
    }

    LaunchedEffect(isNearTimelineEnd, isUserDragging) {
        if (isUserDragging && !isNearTimelineEnd) {
            followStreamingText = false
        } else if (isNearTimelineEnd) {
            followStreamingText = true
        }
    }

    LaunchedEffect(isUserDragging) {
        if (isUserDragging) {
            onTimelineDrag()
        }
    }

    LaunchedEffect(state.streamingTextKey, state.streamingTextLength) {
        if (followStreamingText && state.streamingTextKey != null && state.nodes.isNotEmpty()) {
            listState.animateScrollToItem(state.nodes.lastIndex)
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
            bottom = 136.dp,
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
                        onPositioned = { onUserBubblePositioned(node.key, it) },
                    )
                    is ThinkingNode -> ThinkingBubble(
                        message = node.payload.message.ifBlank { "正在思考中..." },
                    )
                    is AiStreamNode -> StreamingAssistantText(
                        content = node.content,
                        done = node.done,
                        revealed = node.key in revealedMessageKeys,
                        onRevealComplete = { onMessageRevealComplete(node.key) },
                    )
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
                        onCardDismissed = onClarificationCardDismissed,
                    )
                    is CriteriaNode -> CriteriaSummaryCard(node.payload, onEdit = { onCriteriaEdit(node.payload) })
                    is ProductDeckNode -> ProductSwipeDeck(node, onOpen = onProductOpen, onEvidence = onProductEvidence)
                    is FinalDecisionNode -> DecisionSummaryCard(
                        payload = node.payload,
                        products = products,
                        onEvidence = { onDecisionEvidence(node.payload) },
                        onQuickAction = onQuickAction,
                    )
                    is CartActionNode -> CartActionCard(node.payload)
                    is ErrorNode -> ErrorCard(node)
                }
            }
        }

        state.lastError?.let {
            item("last_error") {
                Box {
                    InlineSystemNotice(it)
                }
            }
        }
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (hidden) 0f else 1f),
        horizontalArrangement = Arrangement.End,
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
    val displayMessage = message.ifBlank { "正在思考中..." }.withoutTrailingDots()

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

private fun String.withoutMarkdownMarkup(): String =
    parseMarkdownInlineSegments().joinToString("") { it.text }

private fun String.parseMarkdownInlineSegments(): List<MarkdownInlineSegment> {
    val source = replace(Regex("""^\s*[-*>]\s*""", RegexOption.MULTILINE), "")
    val segments = mutableListOf<MarkdownInlineSegment>()
    val plain = StringBuilder()
    var index = 0

    fun flushPlain() {
        if (plain.isNotEmpty()) {
            segments += MarkdownInlineSegment(plain.toString(), MarkdownInlineStyle.Normal)
            plain.clear()
        }
    }

    while (index < source.length) {
        val marker = when {
            source.startsWith("**", index) -> "**"
            source.startsWith("__", index) -> "__"
            source[index] == '`' -> "`"
            source[index] == '*' -> "*"
            source[index] == '_' -> "_"
            else -> null
        }
        if (marker == null) {
            plain.append(source[index])
            index += 1
            continue
        }

        val end = source.indexOf(marker, startIndex = index + marker.length)
        if (end < 0) {
            plain.append(marker)
            index += marker.length
            continue
        }

        val inner = source.substring(index + marker.length, end)
        if (inner.isBlank()) {
            plain.append(source.substring(index, end + marker.length))
            index = end + marker.length
            continue
        }

        flushPlain()
        val style = when (marker) {
            "**", "__" -> MarkdownInlineStyle.Bold
            "`" -> MarkdownInlineStyle.Code
            else -> MarkdownInlineStyle.Italic
        }
        segments += MarkdownInlineSegment(inner, style)
        index = end + marker.length
    }
    flushPlain()
    return segments
}

private fun String.toMarkdownAnnotatedString(
    baseColor: Color,
): AnnotatedString {
    val segments = parseMarkdownInlineSegments()
    return buildAnnotatedString {
        segments.forEach { segment ->
            val start = length
            append(segment.text)
            val style = when (segment.style) {
                MarkdownInlineStyle.Normal -> null
                MarkdownInlineStyle.Bold -> SpanStyle(fontWeight = FontWeight.Bold)
                MarkdownInlineStyle.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
                MarkdownInlineStyle.Code -> SpanStyle(
                    color = baseColor.copy(alpha = 0.86f),
                    fontWeight = FontWeight.Medium,
                    background = BuyPilotColors.SurfaceMuted,
                )
            }
            if (style != null) {
                addStyle(style, start, length)
            }
        }
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
    revealed: Boolean = false,
    onRevealComplete: (() -> Unit)? = null,
) {
    if (content.isBlank()) {
        LaunchedEffect(done, revealed, onRevealComplete) {
            if (done || revealed) {
                onRevealComplete?.invoke()
            }
        }
        return
    }
    val plainContent = remember(content) { content.withoutMarkdownMarkup() }
    if (plainContent.isBlank()) {
        LaunchedEffect(done, revealed, onRevealComplete) {
            if (done || revealed) {
                onRevealComplete?.invoke()
            }
        }
        return
    }
    var visibleLength by rememberSaveable {
        mutableStateOf(if (revealed) plainContent.length else 0)
    }

    LaunchedEffect(plainContent, done, revealed) {
        val targetLength = plainContent.length
        if (revealed) {
            visibleLength = targetLength
            onRevealComplete?.invoke()
            return@LaunchedEffect
        }
        if (targetLength <= visibleLength) {
            visibleLength = targetLength
            if (done) {
                onRevealComplete?.invoke()
            }
            return@LaunchedEffect
        }
        while (visibleLength < targetLength) {
            visibleLength = (visibleLength + StreamRevealCharsPerFrame).coerceAtMost(targetLength)
            kotlinx.coroutines.delay(StreamRevealFrameDelayMs)
        }
        if (done) {
            onRevealComplete?.invoke()
        }
    }

    MarkdownTextBlock(content.takeMarkdownPlainChars(visibleLength))
}

@Composable
private fun StreamingAssistantText(
    content: String,
) {
    StreamingAssistantText(content = content, done = false, revealed = true)
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
    Text(
        text = remember(content, style.color) { content.toMarkdownAnnotatedString(style.color) },
        style = style,
        modifier = modifier,
    )
}

private fun String.takeMarkdownPlainChars(count: Int): String {
    if (count <= 0) return ""
    val result = StringBuilder()
    var remaining = count
    parseMarkdownInlineSegments().forEach { segment ->
        if (remaining <= 0) return@forEach
        val takeCount = segment.text.length.coerceAtMost(remaining)
        val text = segment.text.take(takeCount)
        result.append(
            when (segment.style) {
                MarkdownInlineStyle.Bold -> "**$text**"
                MarkdownInlineStyle.Italic -> "*$text*"
                MarkdownInlineStyle.Code -> "`$text`"
                MarkdownInlineStyle.Normal -> text
            },
        )
        remaining -= takeCount
    }
    return result.toString()
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

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

private fun quadraticBezier(start: Float, control: Float, end: Float, fraction: Float): Float {
    val inverse = 1f - fraction
    return inverse * inverse * start + 2f * inverse * fraction * control + fraction * fraction * end
}

private fun segmentProgress(value: Float, start: Float, end: Float): Float =
    ((value - start) / (end - start)).coerceIn(0f, 1f)

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
    onCardDismissed: (String) -> Unit,
) {
    val question = payload.question.ifBlank { "请补充一个关键信息" }
    val options = payload.suggestedOptions.ifEmpty { payload.requiredSlots }
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
                    ClarificationOptionScroller(
                        labels = options.ifEmpty { DefaultSkinTypeOptions },
                        selectedLabel = selectedOption,
                        enabled = !dismissing,
                        onClick = onOption,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    ClarificationManualInputRow(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .alpha(if (hasSelection) 0f else 1f),
                        enabled = !dismissing,
                        onClick = onManualInput,
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

    Box(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 2.dp, end = 36.dp),
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
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(42.dp)
                .height(38.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            BuyPilotColors.SurfaceCard.copy(alpha = 0f),
                            BuyPilotColors.SurfaceCard.copy(alpha = 0.62f * trailingEdgeAlpha),
                            BuyPilotColors.SurfaceCard.copy(alpha = 0.96f * trailingEdgeAlpha),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(30.dp)
                .height(38.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            BuyPilotColors.SurfaceCard.copy(alpha = 0.96f * leadingEdgeAlpha),
                            BuyPilotColors.SurfaceCard.copy(alpha = 0.56f * leadingEdgeAlpha),
                            BuyPilotColors.SurfaceCard.copy(alpha = 0f),
                        ),
                    ),
                ),
        )
    }
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
    val targetWidthPx = lockedTarget?.size?.width ?: (estimatedTextWidthPx + with(density) { 32.dp.toPx() })
        .coerceIn(
            minimumValue = maxOf(flight.startSize.width, with(density) { 132.dp.toPx() }),
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
    val flyT = t
    val settleT = segmentProgress(t, 0.78f, 1f)
    val morphT = segmentProgress(t, 0.04f, 0.88f)
    val width = lerp(flight.startSize.width, targetWidthPx, morphT)
    val height = lerp(flight.startSize.height, targetHeightPx, morphT)
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
    val oldTextAlpha = (1f - segmentProgress(t, 0.18f, 0.34f)).coerceIn(0f, 1f)
    val newTextAlpha = segmentProgress(t, 0.26f, 0.48f)
    val settlePulse = (1f - settleT) * settleT * 4f
    val scaleX = 1f + 0.014f * settlePulse
    val scaleY = 1f + 0.008f * settlePulse
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
) {
    Row(
        modifier = modifier
            .heightIn(min = 38.dp)
            .clip(CircleShape)
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
    val exclusions = criteria.exclusionLabels()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StreamingAssistantText("已理解你的需求")
        CriteriaBentoGrid(tiles = tiles, exclusions = exclusions)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            GhostButton(label = "修改标准", leadingIconRes = R.drawable.ic_edit_24, onClick = onEdit)
        }
    }
}

@Composable
private fun CriteriaBentoGrid(
    tiles: List<CriteriaTileSpec>,
    exclusions: List<String>,
) {
    val exclusionTile = CriteriaTileSpec(
        label = "排除项",
        value = exclusions.joinToString("、").ifBlank { "暂无排除" },
        iconRes = R.drawable.ic_block_24,
        accent = Color(0xFFB56B76),
        glow = Color(0xFFFFEEF1),
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            CriteriaTile(
                label = tiles[0].label,
                value = tiles[0].value,
                iconRes = tiles[0].iconRes,
                accent = tiles[0].accent,
                glow = tiles[0].glow,
                prominent = true,
                modifier = Modifier
                    .weight(1.12f)
                    .fillMaxHeight(),
            )
            CriteriaTile(
                label = tiles[1].label,
                value = tiles[1].value,
                iconRes = tiles[1].iconRes,
                accent = tiles[1].accent,
                glow = tiles[1].glow,
                prominent = true,
                modifier = Modifier
                    .weight(0.88f)
                    .fillMaxHeight(),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(142.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            CriteriaTile(
                label = tiles[2].label,
                value = tiles[2].value,
                iconRes = tiles[2].iconRes,
                accent = tiles[2].accent,
                glow = tiles[2].glow,
                prominent = true,
                modifier = Modifier
                    .weight(0.92f)
                    .fillMaxHeight(),
            )
            Column(
                modifier = Modifier
                    .weight(1.08f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                CriteriaTile(
                    label = tiles[3].label,
                    value = tiles[3].value,
                    iconRes = tiles[3].iconRes,
                    accent = tiles[3].accent,
                    glow = tiles[3].glow,
                    compact = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                CriteriaTile(
                    label = exclusionTile.label,
                    value = exclusionTile.value,
                    iconRes = exclusionTile.iconRes,
                    accent = exclusionTile.accent,
                    glow = exclusionTile.glow,
                    compact = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CriteriaTile(
    label: String,
    value: String,
    @DrawableRes iconRes: Int,
    accent: Color,
    glow: Color,
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

    Box(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = shape,
                ambientColor = Color(0xFF8E97A4).copy(alpha = 0.025f),
                spotColor = Color(0xFF8E97A4).copy(alpha = 0.035f),
            )
            .clip(shape)
            .background(BuyPilotColors.SurfaceCard)
            .border(
                width = 1.dp,
                color = BuyPilotColors.Border.copy(alpha = 0.62f),
                shape = shape,
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
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

@Composable
private fun ProductSwipeDeck(
    node: ProductDeckNode,
    onOpen: (ProductCardPayload) -> Unit,
    onEvidence: (ProductCardPayload) -> Unit,
) {
    val product = node.products.firstOrNull() ?: return
    val count = node.products.size

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "找到最佳匹配",
            color = BuyPilotColors.TextPrimary,
            fontSize = 26.sp,
            lineHeight = 31.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "右滑心动，左滑无感，我正在努力懂你。",
            color = BuyPilotColors.TextMuted,
            fontSize = BuyPilotType.Body,
            lineHeight = 20.sp,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(430.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (count > 1) {
                DeckShadowCard(Modifier.offset(x = 20.dp, y = 18.dp).rotate(5f))
            }
            if (count > 2) {
                DeckShadowCard(Modifier.offset(x = (-20).dp, y = 26.dp).rotate(-6f))
            }
            ProductHeroCard(
                payload = product,
                modifier = Modifier.fillMaxWidth(),
                onOpen = { onOpen(product) },
                onEvidence = { onEvidence(product) },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SwipeRoundButton(label = "×", active = false)
            Spacer(Modifier.width(32.dp))
            SwipeRoundButton(label = "♥", active = true)
        }
    }
}

@Composable
private fun DeckShadowCard(modifier: Modifier) {
    Box(
        modifier = modifier
            .width(268.dp)
            .height(360.dp)
            .background(Color.White.copy(alpha = 0.68f), RoundedCornerShape(28.dp))
            .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(28.dp)),
    )
}

@Composable
private fun ProductHeroCard(
    payload: ProductCardPayload,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onEvidence: () -> Unit,
) {
    val product = payload.product
    val tags = (product.ingredientTags + product.skinTypeMatch + product.useScenario).distinct().take(3)

    Column(
        modifier = modifier
            .shadow(18.dp, RoundedCornerShape(32.dp), ambientColor = Color.Black.copy(alpha = 0.12f))
            .background(BuyPilotColors.SurfaceCard, RoundedCornerShape(32.dp))
            .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
            .clickable(onClick = onOpen)
            .padding(bottom = 22.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(242.dp)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFD7D8D2), Color(0xFFF1F1EC), Color(0xFFCBCDCA)),
                    ),
                    RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                ),
        ) {
            ProductMockImage(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(176.dp)
                    .height(214.dp),
            )
            Text(
                text = "PREMIUM CHOICE",
                color = BuyPilotColors.PrimaryDark,
                fontSize = BuyPilotType.Tiny,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
                    .background(Color.White.copy(alpha = 0.74f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = (product.brand ?: "LA ROCHE-POSAY").uppercase(),
            color = BuyPilotColors.TextMuted,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = 3.5.sp,
            modifier = Modifier.padding(horizontal = 18.dp),
        )
        Text(
            text = product.name.ifBlank { "推荐商品" },
            color = BuyPilotColors.TextPrimary,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 18.dp),
        )
        Spacer(Modifier.height(32.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = product.priceLabel(),
                color = BuyPilotColors.Primary,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 40.sp,
            )
            Spacer(Modifier.weight(1f))
            ProductTag("控油王者", active = true)
            Spacer(Modifier.width(6.dp))
            ProductTag("温和")
        }
        Spacer(Modifier.height(56.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProductTag(tags.firstOrNull { it.contains("敏感") } ?: "敏感肌可用")
            Spacer(Modifier.weight(1f))
            Text(
                text = "✿ Verified ${payload.evidence.size.coerceAtLeast(2)}k+",
                color = BuyPilotColors.TextSecondary,
                fontSize = BuyPilotType.LargeBody,
                lineHeight = 22.sp,
            )
        }
    }
}

@Composable
private fun DecisionSummaryCard(
    payload: FinalDecisionPayload,
    products: List<ProductCardPayload>,
    onEvidence: () -> Unit,
    onQuickAction: (QuickActionPayload) -> Unit,
) {
    val winner = products.firstOrNull { it.product.productId == payload.winnerProductId } ?: products.firstOrNull()
    val product = winner?.product

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StreamingAssistantText(
            payload.summary.ifBlank {
                "综合考量你的需求，并对比候选商品后，我为你得出了最终购买建议。"
            },
        )
        Surface(
            color = BuyPilotColors.SurfaceCard,
            shape = RoundedCornerShape(18.dp),
            shadowElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BuyPilotColors.PrimarySoft.copy(alpha = 0.7f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("✪", color = BuyPilotColors.PrimaryDark, fontSize = BuyPilotType.Body)
                    Spacer(Modifier.width(8.dp))
                    Text("首选推荐", color = BuyPilotColors.PrimaryDark, fontSize = BuyPilotType.Label, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(BuyPilotColors.SurfaceMuted, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            ProductMockImage(Modifier.width(52.dp).height(68.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = product?.name ?: payload.winnerProductId ?: "首选商品",
                                color = BuyPilotColors.TextPrimary,
                                fontSize = BuyPilotType.Title,
                                lineHeight = 23.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            MarkdownTextBlock(
                                content = winner?.reason ?: "匹配当前购买标准，综合风险和预算后更值得优先考虑。",
                                style = TextStyle(
                                    color = BuyPilotColors.TextSecondary,
                                    fontSize = BuyPilotType.Body,
                                    lineHeight = 21.sp,
                                ),
                            )
                            Text(
                                text = product?.priceLabel() ?: "价格待确认",
                                color = BuyPilotColors.Primary,
                                fontSize = BuyPilotType.Title,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    SectionTitle("推荐理由", leading = "✓")
                    ChipRows(labels = payload.why.ifEmpty { listOf("需求匹配", "预算合适", "证据充分", "风险可控") }.take(4))
                    if (payload.notFor.isNotEmpty()) {
                        WarningBox(payload.notFor.joinToString("；"))
                    }
                    TextButton(onClick = onEvidence, contentPadding = PaddingValues(0.dp)) {
                        Text("查看决策依据 ›", color = BuyPilotColors.Info, fontSize = BuyPilotType.Label)
                    }
                }
            }
        }
        if (payload.nextActions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                payload.nextActions.take(3).forEachIndexed { index, action ->
                    if (index > 0) Spacer(Modifier.width(8.dp))
                    SmallActionChip(action.label) { onQuickAction(action) }
                }
            }
        }
    }
}

@Composable
private fun CartActionCard(payload: CartActionPayload) {
    InlineSystemNotice("购物车${payload.status.ifBlank { "已更新" }}：${payload.productId}")
}

@Composable
private fun ErrorCard(node: ErrorNode) {
    Surface(
        color = BuyPilotColors.Attention,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.PrimarySoft),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("连接遇到问题", color = BuyPilotColors.TextPrimary, fontSize = BuyPilotType.Body, fontWeight = FontWeight.Bold)
            Text(node.message, color = BuyPilotColors.TextSecondary, fontSize = BuyPilotType.Body, lineHeight = 20.sp)
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.92f))
            .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.75f))
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
            FilledIconButton(
                onClick = onSubmit,
                enabled = canSubmit,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isStreaming) BuyPilotColors.TextPrimary else BuyPilotColors.Primary,
                    contentColor = BuyPilotColors.OnPrimary,
                    disabledContainerColor = BuyPilotColors.Border.copy(alpha = 0.65f),
                    disabledContentColor = BuyPilotColors.TextMuted,
                ),
            ) {
                Icon(
                    painter = painterResource(
                        if (isStreaming) R.drawable.ic_stop_24 else R.drawable.ic_arrow_upward_24,
                    ),
                    contentDescription = if (isStreaming) "停止生成" else "发送",
                    modifier = Modifier.size(if (isStreaming) 17.dp else 21.dp),
                )
            }
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
        CriteriaSuggestionSection(
            labels = payload.quickActions.map { it.label }
                .ifEmpty { listOf("再便宜一点", "温和亲肤", "大容量", "注重品牌") },
            onClick = { label ->
                payload.quickActions.firstOrNull { it.label == label }?.let(onQuickAction)
            },
        )
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
private fun ProductDetailSheet(
    payload: ProductCardPayload,
    onEvidence: () -> Unit,
    onAction: (QuickActionPayload) -> Unit,
) {
    val product = payload.product
    SheetContentColumn {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFFFFE1D4), Color(0xFFFFFAF7))),
                    RoundedCornerShape(28.dp),
                ),
        ) {
            ProductMockImage(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(132.dp)
                    .height(176.dp),
            )
            PillLabel(
                text = "最佳匹配",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(18.dp),
            )
        }
        Text(
            product.name.ifBlank { "商品详情" },
            color = BuyPilotColors.TextPrimary,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(product.priceLabel(), color = BuyPilotColors.Primary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        ChipRows((product.ingredientTags + product.skinTypeMatch + product.useScenario).distinct().take(5).ifEmpty { listOf("控油", "温和", "日常洁面") })
        MarkdownTextBlock(
            content = payload.reason.ifBlank { "这款商品与当前需求高度匹配，适合作为优先比较对象。" },
            style = TextStyle(
                color = BuyPilotColors.TextSecondary,
                fontSize = BuyPilotType.Body,
                lineHeight = 22.sp,
            ),
        )
        if (payload.riskNotes.isNotEmpty()) {
            WarningBox(payload.riskNotes.joinToString("；"))
        }
        TextButton(onClick = onEvidence, contentPadding = PaddingValues(0.dp)) {
            Text("查看为什么推荐这个商品 ›", color = BuyPilotColors.Info)
        }
        payload.actions.take(3).forEach { action ->
            SmallActionChip(action.label) { onAction(action) }
        }
    }
}

@Composable
private fun ProductEvidenceSheet(payload: ProductCardPayload) {
    SheetContentColumn {
        SheetTitle("推荐证据")
        if (payload.evidence.isEmpty()) {
            EvidenceBlock(
                EvidencePayload(
                    sourceType = "商品资料",
                    trustLabel = "待后端补充",
                    snippet = payload.reason.ifBlank { "当前推荐理由来自商品结构化资料与用户需求匹配。" },
                ),
            )
        } else {
            payload.evidence.forEach { EvidenceBlock(it) }
        }
    }
}

@Composable
private fun DecisionEvidenceSheet(payload: FinalDecisionPayload) {
    SheetContentColumn {
        SheetTitle("决策依据")
        EvidenceSection("为什么选它", payload.why.ifEmpty { listOf("匹配当前需求", "预算与风险更均衡", "推荐证据更完整") }, numbered = true)
        EvidenceSection("不适合这些情况", payload.notFor.ifEmpty { listOf("极度敏感或完全不接受相关成分时，需要进一步确认。") })
        if (payload.alternatives.isNotEmpty()) {
            SectionTitle("备选方案")
            payload.alternatives.forEach {
                Surface(
                    color = BuyPilotColors.SurfaceMuted.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(it.name, color = BuyPilotColors.TextPrimary, modifier = Modifier.padding(16.dp))
                }
            }
        }
        Surface(color = BuyPilotColors.SurfaceMuted, shape = RoundedCornerShape(14.dp)) {
            MarkdownTextBlock(
                content = payload.summary.ifBlank { "决策详情会根据本轮商品、证据和约束生成。" },
                style = TextStyle(
                    color = BuyPilotColors.TextSecondary,
                    fontSize = BuyPilotType.Body,
                    lineHeight = 22.sp,
                ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BuyPilotColors.Attention, RoundedCornerShape(14.dp))
            .border(1.dp, BuyPilotColors.PrimarySoft, RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Text("!", color = BuyPilotColors.PrimaryDark, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        MarkdownTextBlock(
            content = "**注意：**$text",
            style = TextStyle(
                color = BuyPilotColors.TextSecondary,
                fontSize = BuyPilotType.Body,
                lineHeight = 21.sp,
            ),
        )
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
    label: String,
    active: Boolean,
) {
    Box(
        modifier = Modifier
            .size(if (active) 72.dp else 60.dp)
            .shadow(10.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.12f))
            .background(if (active) BuyPilotColors.Primary else BuyPilotColors.SurfaceMuted, CircleShape)
            .border(1.dp, if (active) BuyPilotColors.PrimaryDark.copy(alpha = 0.16f) else BuyPilotColors.Border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (active) Color.White else BuyPilotColors.TextSecondary,
            fontSize = if (active) 42.sp else 44.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
        Text(label, color = BuyPilotColors.TextSecondary, fontSize = BuyPilotType.Label)
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

private fun com.buypilot.core.model.CriteriaPayload.budgetLabel(): String {
    val max = budgetMax ?: constraints?.budgetMax
    val min = budgetMin ?: constraints?.budgetMin
    return when {
        min != null && max != null -> "¥${min.clean()}-${max.clean()}"
        max != null -> "${max.clean()}元以内"
        min != null -> "¥${min.clean()}以上"
        else -> "预算待确认"
    }
}

private fun com.buypilot.core.model.CriteriaPayload.summaryTiles(): List<CriteriaTileSpec> =
    listOf(
        CriteriaTileSpec(
            label = "核心诉求",
            value = productTypeLabel().ifBlank { category.ifBlank { summary.withoutMarkdownMarkup().ifBlank { "待确认" } } },
            iconRes = R.drawable.ic_search_24,
            accent = Color(0xFFE0643B),
            glow = Color(0xFFFFEFE8),
        ),
        CriteriaTileSpec(
            label = "适用对象",
            value = skinTypeLabel().ifBlank { "按需求匹配" },
            iconRes = R.drawable.ic_shield_24,
            accent = Color(0xFF4F86D8),
            glow = Color(0xFFEEF5FF),
        ),
        CriteriaTileSpec(
            label = "预算",
            value = budgetLabel(),
            iconRes = R.drawable.ic_payments_24,
            accent = Color(0xFFB87920),
            glow = Color(0xFFFFF4DE),
        ),
        CriteriaTileSpec(
            label = "场景/用途",
            value = useScenarioLabel().ifBlank { "日常使用" },
            iconRes = R.drawable.ic_history_24,
            accent = Color(0xFF8B96A5),
            glow = Color(0xFFF2F4F7),
        ),
    )

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
    price?.let { "${currency.orEmpty().ifBlank { "¥" }}${it.clean()}" } ?: "价格待确认"

private fun Double.clean(): String =
    if (this % 1.0 == 0.0) toInt().toString() else String.format("%.2f", this)

private sealed interface ChatSheetContent {
    data class Criteria(val payload: CriteriaCardPayload) : ChatSheetContent
    data class Product(val payload: ProductCardPayload) : ChatSheetContent
    data class ProductEvidence(val payload: ProductCardPayload) : ChatSheetContent
    data class DecisionEvidence(val payload: FinalDecisionPayload) : ChatSheetContent
}
