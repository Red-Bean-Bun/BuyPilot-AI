package com.buypilot.feature.chat.ui

import androidx.annotation.DrawableRes
import android.content.Context
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
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
import coil.request.ImageRequest
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
import com.buypilot.feature.chat.model.PendingDecision
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
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.ThematicBreak
import org.commonmark.node.Text as MarkdownText
import org.commonmark.parser.Parser
import io.noties.markwon.Markwon
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import java.util.LinkedHashMap
import kotlin.math.abs
import kotlin.math.roundToInt

private val MenuEaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val MenuEaseIn = CubicBezierEasing(0.3f, 0f, 1f, 1f)
private val PremiumRevealEase = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val ClarificationEaseOut = CubicBezierEasing(0.1f, 1f, 0.1f, 1f)
private val ClarificationFlightEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
private val ProductDetailRevealDistance = 220.dp
private const val ProductDetailSnapThreshold = 0.36f
private const val ProductDetailEnterMs = 560
private const val ProductSwipeDetailEnterMs = 520
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
private const val ClarificationQuestionToCardDelayMs = 170L
private const val ClarificationCardEnterMs = 640
private const val CriteriaCardEnterMs = 560
private const val ProductDeckArrivalMs = 1480
private const val ProductDeckAutoCloseDelayMs = 280L
private const val ProductSwipeAnimationMs = 430
private const val DecisionCardEnterMs = 760
private const val DecisionReasonBaseDelayMs = 240
private const val DecisionReasonStaggerMs = 120
private val TimelineNearEndThreshold = 24.dp
private val TimelineFollowCorrectionTolerance = 8.dp
private val TimelineSafeGap = 20.dp
private val TimelineBottomReadingBuffer = 156.dp
private val TimelineKeyboardBottomReadingBuffer = 220.dp
private val TimelineAnchorTopGap = 28.dp
private val TimelineAnchorBottomReserve = 420.dp
private const val TimelineViewportSettleMs = 48L
private val FloatingPanelComposerGap = 8.dp
private val TimelineJumpButtonComposerGap = 14.dp
private val ChipEdgeFadeWidth = 46.dp
private const val TurnRevealIntroChars = 18
private const val TurnRevealIntroRatio = 0.42f
private const val TurnStructuredEnterMs = 320
private const val TurnStructuredEnterDelayMs = 90
private const val ThinkingToTextHandoffMs = 190L
private const val TimelineRevealScrollThrottleMs = 40L
private const val TextRevealProgressReportStep = 24
private const val MarkdownRenderCacheMaxEntries = 36
private val MarkdownSoftBlockColor = Color(0xFFEFF2F5).toArgb()
private val MarkdownComposeCodeBackground = Color(0xFFEFF2F5)

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

private data class CriteriaReceiptProperty(
    val label: String,
    val value: String,
    val placeholder: Boolean = false,
)

private enum class CriteriaTileSpan {
    Half,
    Full,
}

@Immutable
private data class CriteriaLabels(
    val summary: String,
    val category: String,
    val coreNeed: String,
    val targetUser: String,
    val budget: String,
    val scenario: String,
    val exclusions: String,
    val avoidPrefix: String,
)

private data class ProductDeckSignalSummary(
    val viewed: Int,
    val liked: Int,
    val dismissed: Int,
) {
    val hasSignals: Boolean
        get() = viewed > 0 || liked > 0 || dismissed > 0
}

private data class DecisionStatusBadge(
    val label: String,
    val accent: Color,
    val surface: Color,
    val showCardWhenEmpty: Boolean = false,
)

@Immutable
internal sealed interface TimelineRenderItem {
    val key: String
}

private val TimelineRenderItem.timelineContentType: String
    get() = when (this) {
        is UserTimelineItem -> "user_message"
        is AssistantTurnTimelineItem -> "assistant_turn"
        is StandaloneTimelineItem -> when (node) {
            is ThinkingNode -> "standalone_thinking"
            is AiStreamNode -> "standalone_text"
            is ClarificationNode -> "standalone_clarification"
            is CriteriaNode -> "standalone_criteria"
            is ProductDeckNode -> "standalone_products"
            is FinalDecisionNode -> "standalone_decision"
            is CartActionNode -> "standalone_cart"
            is ErrorNode -> "standalone_error"
            is UserMessageNode -> "standalone_user"
        }
    }

@Immutable
internal data class UserTimelineItem(
    val node: UserMessageNode,
) : TimelineRenderItem {
    override val key: String = node.key
}

@Immutable
internal data class AssistantTurnTimelineItem(
    val turnId: String,
    val nodes: List<ChatUiNode>,
    val segmentIndex: Int,
) : TimelineRenderItem {
    override val key: String = "assistant_${turnId.ifBlank { "unknown" }}_$segmentIndex"
}

@Immutable
internal data class StandaloneTimelineItem(
    val node: ChatUiNode,
) : TimelineRenderItem {
    override val key: String = node.key
}

@Immutable
private data class TimelineRenderContext(
    val backendBaseUrl: String,
    val isStreaming: Boolean,
    val currentTurnId: String?,
    val productsById: Map<String, ProductCardPayload>,
    val productDeckIdByProductId: Map<String, String>,
    val latestProductDeckKey: String?,
    val productSwipeStates: Map<String, ProductSwipeState>,
    val awaitingConvergenceDeckIds: Set<String>,
    val latestConvergeableDeckId: String?,
    val pendingDecisions: Map<String, PendingDecision>,
    val convergedDeckIds: Set<String>,
    val lastUserMessage: String?,
)

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

private data class MarkdownTextRenderTag(
    val contentHash: Int,
    val contentLength: Int,
    val textColorArgb: Int,
    val fontSizePx: Float,
    val lineHeightPx: Float,
    val typefaceStyle: Int,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyPilotChatScreen(
    state: ChatUiState,
    onInputChanged: (String, Boolean) -> Unit,
    onSendMessage: (String, String?) -> Unit,
    onCriteriaPatch: (JsonObject) -> Unit,
    onCancel: () -> Unit,
    onOpenProductDeck: (String, String?) -> Unit,
    onOpenProductDetail: (String, String) -> Unit,
    onRetryLastMessage: () -> Unit,
    onEditLastMessage: (String) -> Unit,
    onClearConversation: () -> Unit,
    onConvergeProductDeck: (String) -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var sheetContent by remember { mutableStateOf<ChatSheetContent?>(null) }
    var sheetExiting by remember { mutableStateOf(false) }
    var sheetTransitionId by remember { mutableStateOf(0) }
    var dismissedClarificationKeys by remember { mutableStateOf(emptySet<String>()) }
    var dismissingClarificationKey by remember { mutableStateOf<String?>(null) }
    var revealedMessageKeyList by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val revealedMessageKeys = remember(revealedMessageKeyList) { revealedMessageKeyList.toSet() }
    var typingMessageKeys by remember { mutableStateOf(emptySet<String>()) }
    var visualActiveTurnKeys by remember { mutableStateOf(emptySet<String>()) }
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
    var welcomePromptDismissed by remember { mutableStateOf(false) }
    var welcomePromptHasAppeared by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val keyboardVisible = imeBottomPx > 0
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current
    val composerFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val defaultSkinTypeOptions = stringArrayResource(R.array.default_skin_type_options).toSet()
    val showWelcome = state.nodes.isEmpty() && input.isBlank()
    val shouldDismissWelcomeContent = showWelcome &&
        (showAttachmentMenu || keyboardVisible || (composerFocused && welcomePromptHasAppeared))
    val welcomeContentVisible = showWelcome &&
        !welcomePromptDismissed &&
        !showAttachmentMenu &&
        !composerFocused &&
        !keyboardVisible
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
    val assistantVisualActive = typingMessageKeys.isNotEmpty() || visualActiveTurnKeys.isNotEmpty()
    val clarificationFlightActive = dismissingClarificationKey != null ||
        pendingClarificationAnswer?.awaitsFlight == true ||
        activeClarificationFlight != null

    LaunchedEffect(shouldDismissWelcomeContent) {
        if (shouldDismissWelcomeContent) {
            welcomePromptDismissed = true
        }
    }

    LaunchedEffect(welcomeContentVisible) {
        if (welcomeContentVisible) {
            welcomePromptHasAppeared = true
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
        dismissedClarificationKeys = emptySet()
        dismissingClarificationKey = null
        welcomePromptDismissed = false
        welcomePromptHasAppeared = false
        revealedMessageKeyList = emptyList()
        typingMessageKeys = emptySet()
        visualActiveTurnKeys = emptySet()
        pendingClarificationAnswer = null
        activeClarificationFlight = null
        hiddenFlightMessageKeys = emptySet()
        userBubbleSnapshots = emptyMap()
        clarificationManualSource = null
        keyboardFlightSnapshot = null
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

    BackHandler(enabled = sheetContent != null || composerFocused) {
        when {
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
                centered = state.nodes.isNotEmpty(),
                showBack = state.nodes.any { it is CriteriaNode || it is ProductDeckNode || it is FinalDecisionNode },
                showClear = state.nodes.isNotEmpty(),
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
                    onConvergeRecommendation = onConvergeProductDeck,
                    onWelcomePromptClick = { loadWelcomePrompt(it) },
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
                    isAssistantVisualActive = assistantVisualActive,
                    isClarificationFlightActive = clarificationFlightActive,
                    dismissingClarificationKey = dismissingClarificationKey,
                    dismissedClarificationKeys = dismissedClarificationKeys,
                    selectedClarificationOption = pendingClarificationAnswer?.selectedOption,
                    hiddenUserMessageKeys = hiddenUserMessageKeysForFlight,
                    activeFlightMessageKey = activeFlightMessageKey,
                    revealedMessageKeys = revealedMessageKeys,
                    onMessageRevealComplete = { key ->
                        if (key !in revealedMessageKeys) {
                            revealedMessageKeyList = revealedMessageKeyList + key
                        }
                    },
                    onMessageRevealActiveChange = { key, active ->
                        typingMessageKeys = if (active) {
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
                        if (key !in userBubbleSnapshots) {
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
                )
            }
        }

        BottomComposer(
            text = input,
            inputState = state.inputState,
            isStreaming = state.isStreaming,
            isTextRevealing = assistantVisualActive,
            awaitingCriteriaAdjustment = state.awaitingCriteriaAdjustment,
            isAttachmentMenuOpen = showAttachmentMenu,
            focusRequester = composerFocusRequester,
            modifier = Modifier
                .align(Alignment.BottomCenter),
            onAttachmentClick = {
                showAttachmentMenu = !showAttachmentMenu
                focusManager.clearFocus()
            },
            onTextChange = {
                input = it
                onInputChanged(it, false)
            },
            onTextFocus = {
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

    val sheetProductDeckNodes = remember(state.nodes) {
        state.nodes.filterIsInstance<ProductDeckNode>()
    }
    val sheetProductsById = remember(sheetProductDeckNodes) {
        sheetProductDeckNodes.productsByProductId()
    }
    val sheetProductDeckIdByProductId = remember(sheetProductDeckNodes) {
        sheetProductDeckNodes.productDeckIdByProductId()
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
                    is ChatSheetContent.DecisionEvidence -> DecisionEvidenceSheet(
                        payload = targetContent.payload,
                        productsById = sheetProductsById,
                        productDeckIdByProductId = sheetProductDeckIdByProductId,
                        onProductDetailOpen = { deckId, productId ->
                            dismissSheet { onOpenProductDetail(deckId, productId) }
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
    onClarificationOption: (String, ClarificationChipSnapshot?) -> Unit,
    onClarificationManualInput: () -> Unit,
    onClarificationManualSource: (String, ClarificationChipSnapshot) -> Unit,
    onCriteriaEdit: (CriteriaCardPayload) -> Unit,
    onProductOpen: (String, String?) -> Unit,
    onProductDetailOpen: (String, String) -> Unit,
    onConvergeRecommendation: (String) -> Unit,
    onWelcomePromptClick: (String) -> Unit,
    onDecisionEvidence: (FinalDecisionPayload) -> Unit,
    onRetryLastMessage: () -> Unit,
    onEditLastMessage: (String) -> Unit,
    onQuickAction: (QuickActionPayload) -> Unit,
    onTimelineDrag: () -> Unit,
    composerHeightPx: Int,
    imeBottomPx: Int,
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
                onTimelineDrag = onTimelineDrag,
                composerHeightPx = composerHeightPx,
                imeBottomPx = imeBottomPx,
                isComposerFocused = isComposerFocused,
                isAssistantVisualActive = isAssistantVisualActive,
                isClarificationFlightActive = isClarificationFlightActive,
                dismissingClarificationKey = dismissingClarificationKey,
                dismissedClarificationKeys = dismissedClarificationKeys,
                selectedClarificationOption = selectedClarificationOption,
                hiddenUserMessageKeys = hiddenUserMessageKeys,
                activeFlightMessageKey = activeFlightMessageKey,
                revealedMessageKeys = revealedMessageKeys,
                onMessageRevealComplete = onMessageRevealComplete,
                onMessageRevealActiveChange = onMessageRevealActiveChange,
                onAssistantTurnVisualActiveChange = onAssistantTurnVisualActiveChange,
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
    showClear: Boolean,
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
            onNavigationClick = {},
            onActionClick = onClearConversation,
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
        Spacer(Modifier.height(92.dp))
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(
                animationSpec = tween(durationMillis = 260, delayMillis = 40, easing = MenuEaseOut),
            ) + slideInVertically(
                animationSpec = tween(durationMillis = 300, delayMillis = 40, easing = MenuEaseOut),
                initialOffsetY = { -it / 28 },
            ) + scaleIn(
                animationSpec = tween(durationMillis = 300, delayMillis = 40, easing = MenuEaseOut),
                initialScale = 0.992f,
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 130, easing = MenuEaseIn),
            ) + slideOutVertically(
                animationSpec = tween(durationMillis = 160, easing = MenuEaseIn),
                targetOffsetY = { -it / 20 },
            ) + scaleOut(
                animationSpec = tween(durationMillis = 160, easing = MenuEaseIn),
                targetScale = 0.99f,
            ),
        ) {
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
        }
        Spacer(Modifier.weight(1f))
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

@Composable
private fun ChatTimeline(
    state: ChatUiState,
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
    onTimelineDrag: () -> Unit,
    composerHeightPx: Int,
    imeBottomPx: Int,
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
    var clarificationFreezeCaptured by remember { mutableStateOf(false) }
    var clarificationFreezeItemKey by remember { mutableStateOf<String?>(null) }
    var clarificationFreezeItemIndex by remember { mutableStateOf(0) }
    var clarificationFreezeScrollOffset by remember { mutableStateOf(0) }
    val suppressTimelineAutoFocus = suppressReturnAutoFocus || isClarificationFlightActive
    val revealStore = remember { TimelineRevealStore() }
    val hasTimelineError = state.nodes.any { it is ErrorNode }
    val timelineItems = remember(state.nodes) { state.nodes.toTimelineRenderItems() }
    val latestFinalDecisionKey = remember(state.nodes) {
        state.nodes.filterIsInstance<FinalDecisionNode>().lastOrNull()?.key
    }
    val finalDecisionKeys = remember(state.nodes) {
        state.nodes.filterIsInstance<FinalDecisionNode>().mapTo(mutableSetOf()) { it.key }
    }
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
    LaunchedEffect(timelineItems, state.nodes) {
        revealStore.pruneToKeys(
            timelineItemKeys = timelineItems.mapTo(mutableSetOf()) { it.key },
            nodeKeys = state.nodes.timelineRevealKeys(),
        )
    }
    val productDeckNodes = remember(state.nodes) {
        state.nodes.filterIsInstance<ProductDeckNode>()
    }
    val productsById = remember(productDeckNodes) {
        productDeckNodes.productsByProductId()
    }
    val productDeckIdByProductId = remember(productDeckNodes) {
        productDeckNodes.productDeckIdByProductId()
    }
    val renderContext = remember(
        state.backendBaseUrl,
        state.isStreaming,
        state.currentTurnId,
        productsById,
        productDeckIdByProductId,
        state.productSwipeStates,
        state.awaitingConvergenceDeckIds,
        state.latestConvergeableDeckId,
        state.pendingDecisions,
        state.nodes.convergedProductDeckIds(),
        state.lastUserMessage,
    ) {
        TimelineRenderContext(
            backendBaseUrl = state.backendBaseUrl,
            isStreaming = state.isStreaming,
            currentTurnId = state.currentTurnId,
            productsById = productsById,
            productDeckIdByProductId = productDeckIdByProductId,
            latestProductDeckKey = productDeckNodes.lastOrNull()?.key,
            productSwipeStates = state.productSwipeStates,
            awaitingConvergenceDeckIds = state.awaitingConvergenceDeckIds,
            latestConvergeableDeckId = state.latestConvergeableDeckId,
            pendingDecisions = state.pendingDecisions,
            convergedDeckIds = state.nodes.convergedProductDeckIds(),
            lastUserMessage = state.lastUserMessage,
        )
    }
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
        if (key !in finalDecisionKeys) {
            requestRevealFollowScroll()
        }
    }
    val timelineViewportBottomInset = calculateTimelineViewportBottomInset(
        density = density,
        composerHeightPx = composerHeightPx,
        imeBottomPx = imeBottomPx,
    )
    val timelineBottomPadding = TimelineSafeGap
    val timelineReadingBuffer = if (imeBottomPx > 0) {
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
        if (routeReturnRestorePending) return@LaunchedEffect
        kotlinx.coroutines.delay(420L)
        suppressReturnAutoFocus = false
    }

    LaunchedEffect(routeReturnRestorePending, timelineItems.size, latestFinalDecisionKey) {
        if (!routeReturnRestorePending || timelineItems.isEmpty()) return@LaunchedEffect
        suppressReturnAutoFocus = true
        activeTurnAnchored = false
        followStreamingText = false
        retainedAnchorUserMessageKey = null
        retainedAnchorAssistantTurnId = null
        kotlinx.coroutines.delay(48L)
        val newFinalDecisionKey = latestFinalDecisionKey
            ?.takeIf { it != routeReturnFinalDecisionKey }
        if (newFinalDecisionKey != null) {
            val decisionItemIndex = timelineItems.indexOfFirst { it.containsNodeKey(newFinalDecisionKey) }
            if (decisionItemIndex >= 0) {
                lastFocusedFinalDecisionKey = newFinalDecisionKey
                animateTimelineItemToAnchor(
                    listState = listState,
                    itemIndex = decisionItemIndex,
                    anchorTopPx = anchorTopOffsetPx,
                    tolerancePx = followCorrectionTolerancePx,
                )
            }
        } else {
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
        }
        kotlinx.coroutines.delay(360L)
        routeReturnRestorePending = false
        routeReturnAnchorCaptured = false
        routeReturnWasCovered = false
        routeReturnItemKey = null
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
        imeBottomPx,
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
            lastHandledUserMessageKey = key
            lastAutoSettledUserMessageKey = null
            activeTurnAnchored = true
            retainedAnchorUserMessageKey = key
            retainedAnchorAssistantTurnId = null
            followStreamingText = false
            keepLatestUserMessageAnchored()
        }
    }

    LaunchedEffect(
        activeTurnAnchored,
        state.lastUserMessageKey,
        activeFlightMessageKey,
        composerHeightPx,
        imeBottomPx,
        timelineItems.size,
    ) {
        if (suppressTimelineAutoFocus) return@LaunchedEffect
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
        timelineItems.size,
        activeTurnAnchored,
        activeFlightMessageKey,
    ) {
        if (suppressTimelineAutoFocus) return@LaunchedEffect
        val turnId = state.currentTurnId ?: return@LaunchedEffect
        if (
            turnId == lastAnchoredAssistantTurnId ||
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
        kotlinx.coroutines.delay(TimelineViewportSettleMs)
        animateTimelineItemToAnchor(
            listState = listState,
            itemIndex = assistantTurnIndex,
            anchorTopPx = anchorTopOffsetPx,
            tolerancePx = followCorrectionTolerancePx,
        )
    }

    LaunchedEffect(isComposerFocused, imeBottomPx, composerHeightPx, timelineItems.size) {
        if (suppressTimelineAutoFocus) return@LaunchedEffect
        if (!isComposerFocused || imeBottomPx <= 0 || isUserDragging || activeTurnAnchored) return@LaunchedEffect
        kotlinx.coroutines.delay(TimelineViewportSettleMs)
        if (isNearTimelineEnd) {
            scrollActiveTurnIfNeeded(
                listState = listState,
                lastContentIndex = lastContentIndex(),
                bottomPaddingPx = latestContentBottomPaddingPx,
                tolerancePx = followCorrectionTolerancePx,
                settleFrames = 2,
            )
        }
    }

    LaunchedEffect(isNearTimelineEnd, isUserDragging) {
        if (isUserDragging && !isNearTimelineEnd) {
            followStreamingText = false
            activeTurnAnchored = false
            retainedAnchorUserMessageKey = null
            retainedAnchorAssistantTurnId = null
            lastAutoSettledUserMessageKey = state.lastUserMessageKey
        }
    }

    LaunchedEffect(isUserDragging) {
        if (isUserDragging) {
            retainedAnchorUserMessageKey = null
            retainedAnchorAssistantTurnId = null
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
        }
    }

    LaunchedEffect(latestFinalDecisionKey, timelineItems.size, composerHeightPx, imeBottomPx) {
        if (suppressTimelineAutoFocus) return@LaunchedEffect
        val decisionKey = latestFinalDecisionKey ?: return@LaunchedEffect
        if (decisionKey == lastFocusedFinalDecisionKey || isUserDragging) return@LaunchedEffect
        val decisionItemIndex = timelineItems.indexOfFirst { it.containsNodeKey(decisionKey) }
        if (decisionItemIndex < 0) return@LaunchedEffect
        lastFocusedFinalDecisionKey = decisionKey
        activeTurnAnchored = false
        followStreamingText = false
        retainedAnchorUserMessageKey = null
        retainedAnchorAssistantTurnId = null
        kotlinx.coroutines.delay(220L)
        animateTimelineItemToAnchor(
            listState = listState,
            itemIndex = decisionItemIndex,
            anchorTopPx = anchorTopOffsetPx,
            tolerancePx = followCorrectionTolerancePx,
        )
    }

    LaunchedEffect(
        timelineItems.size,
        state.streamingTextKey,
        state.isStreaming,
        latestContentBottomPaddingPx,
        revealScrollTick,
        activeTurnAnchored,
        retainedAnchorUserMessageKey,
        retainedAnchorAssistantTurnId,
    ) {
        if (suppressTimelineAutoFocus) return@LaunchedEffect
        if ((activeTurnAnchored || retainedAnchorUserMessageKey != null) && timelineItems.isNotEmpty()) {
            keepLatestUserMessageAnchored()
        } else if (retainedAnchorAssistantTurnId != null && timelineItems.isNotEmpty()) {
            keepAssistantTurnAnchored(retainedAnchorAssistantTurnId)
        } else if (followStreamingText && timelineItems.isNotEmpty()) {
            scrollActiveTurnIfNeeded(
                listState = listState,
                lastContentIndex = lastContentIndex(),
                bottomPaddingPx = latestContentBottomPaddingPx,
                tolerancePx = followCorrectionTolerancePx,
                settleFrames = 2,
            )
        }
    }

    LaunchedEffect(
        followLatestActive,
        timelineItems.size,
        state.streamingTextKey,
        latestContentBottomPaddingPx,
        suppressTimelineAutoFocus,
        isUserDragging,
    ) {
        if (!followLatestActive || suppressTimelineAutoFocus || isUserDragging) return@LaunchedEffect
        while (true) {
            if (timelineItems.isNotEmpty()) {
                scrollActiveTurnIfNeeded(
                    listState = listState,
                    lastContentIndex = lastContentIndex(),
                    bottomPaddingPx = latestContentBottomPaddingPx,
                    tolerancePx = followCorrectionTolerancePx,
                )
            }
            kotlinx.coroutines.delay(72L)
        }
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
                                onMessageRevealComplete = onMessageRevealComplete,
                                onMessageRevealActiveChange = onMessageRevealActiveChange,
                                onStreamingTextProgress = { requestRevealFollowScroll() },
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

            val shouldRetainAnchorSpacer = retainedAnchorUserMessageKey != null &&
                retainedAnchorUserMessageKey == state.lastUserMessageKey
            val shouldRetainAssistantAnchorSpacer = retainedAnchorAssistantTurnId != null &&
                retainedAnchorAssistantTurnId == state.currentTurnId
            val endReadingBuffer = if (
                activeTurnAnchored ||
                shouldRetainAnchorSpacer ||
                shouldRetainAssistantAnchorSpacer
            ) {
                activeTurnBottomReserve
            } else {
                timelineReadingBuffer
            }
            if (timelineItems.isNotEmpty()) {
                item("timeline_bottom_reading_buffer") {
                    Spacer(Modifier.height(endReadingBuffer))
                }
            }
        }

        AnimatedVisibility(
            visible = !isNearTimelineEnd && !followLatestActive,
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
                            TimelineNodeContent(
                                node = node,
                                renderContext = renderContext,
                                timelineMotionEnabled = timelineMotionEnabled,
                                hiddenUserMessage = false,
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
                                onProductOpen = onProductOpen,
                                onProductDetailOpen = onProductDetailOpen,
                                onConvergeRecommendation = onConvergeRecommendation,
                                onDecisionEvidence = onDecisionEvidence,
                                onRetryLastMessage = onRetryLastMessage,
                                onEditLastMessage = onEditLastMessage,
                                onQuickAction = onQuickAction,
                                onMessageRevealComplete = onMessageRevealComplete,
                                onMessageRevealActiveChange = onMessageRevealActiveChange,
                                onStreamingTextProgress = onStreamingTextProgress,
                                onTextRevealProgress = coordinator.updateTextProgress,
                                onStructuredEntered = coordinator.markStructuredEntered,
                                onUserBubblePositioned = onUserBubblePositioned,
                                onClarificationCardDismissed = onClarificationCardDismissed,
                            )
                        }
                    } else if (nodeVisible) {
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

@Composable
private fun ThinkingNodeVisibility(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 180, easing = MenuEaseOut),
        ) + expandVertically(
            animationSpec = tween(durationMillis = 210, easing = MenuEaseOut),
            expandFrom = Alignment.Top,
        ) + slideInVertically(
            animationSpec = tween(durationMillis = 210, easing = MenuEaseOut),
            initialOffsetY = { it / 5 },
        ),
        exit = fadeOut(
            animationSpec = tween(durationMillis = 130, easing = MenuEaseIn),
        ) + shrinkVertically(
            animationSpec = tween(durationMillis = 170, easing = MenuEaseIn),
            shrinkTowards = Alignment.Top,
        ) + slideOutVertically(
            animationSpec = tween(durationMillis = 170, easing = MenuEaseIn),
            targetOffsetY = { -it / 6 },
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
    when (nextNode) {
        null -> return true
        is AiStreamNode,
        is ClarificationNode -> return false
        else -> return false
    }
    return true
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
            hidden = hiddenUserMessage,
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
            backendBaseUrl = renderContext.backendBaseUrl,
            motionEnabled = structuredMotionEnabled,
            alreadyEntered = structuredAlreadyEntered,
            onEntered = { onStructuredEntered(node.key) },
            onEvidence = { onDecisionEvidence(node.payload) },
            onProductDetailOpen = onProductDetailOpen,
            onQuickAction = onQuickAction,
        )
        is CartActionNode -> CartActionCard(node.payload)
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
    hidden: Boolean = false,
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
        Box(
            modifier = Modifier
                .widthIn(max = 304.dp)
                .background(BuyPilotColors.Primary, bubbleShape)
                .then(positionModifier)
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
        Color(0xFFA4AAB3)
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

private val PlainMarkdownParser: Parser = Parser.builder().build()
private val MarkdownBlockQuoteMarkerRegex = Regex("""^\s{0,3}>\s?""")
private val InternalDebugLabelValueRegex = Regex(
    """(?i)\b(?:product_id|evidence_id|source_id|action_id|feedback_type|criteria_patch|cart_id)\s*[:：=]\s*[\w.-]+""",
)
private val InternalDebugLabelRegex = Regex(
    """(?i)\b(?:product_id|evidence_id|source_id|action_id|feedback_type|criteria_patch|cart_id)\b""",
)
private val InternalDebugValueRegex = Regex(
    """(?i)\b(?:not_interested|view_detail|open_evidence|criteria_patch|add_to_cart|show_evidence)\b""",
)
private val InternalIdTokenRegex = Regex("""(?i)\b(?:pg|p)_[a-z0-9_-]*\b""")

private fun String.withoutMarkdownBlockQuoteMarkers(): String =
    lineSequence()
        .joinToString("\n") { line -> line.replace(MarkdownBlockQuoteMarkerRegex, "") }

private fun String.withoutStreamingMarkdownChrome(): String =
    withoutMarkdownBlockQuoteMarkers()
        .lineSequence()
        .joinToString("\n") { line ->
            if (Regex("""^\s{0,3}(?:-{3,}|\*{3,}|_{3,})\s*$""").matches(line)) {
                "────────────"
            } else {
                line
                    .replace(Regex("""^\s{0,3}#{1,6}\s+"""), "")
                    .replace("**", "")
                    .replace("`", "")
            }
        }

private fun String.withoutInternalDebugTokens(): String =
    replace(InternalDebugLabelValueRegex, "")
        .replace(InternalIdTokenRegex, "")
        .replace(InternalDebugValueRegex, "")
        .replace(InternalDebugLabelRegex, "")
        .replace(Regex("""[（(]\s*[，,、;；:\s]*[）)]"""), "")
        .replace(Regex("""\s+([，。！？；：、,.!?;:])"""), "$1")
        .replace(Regex("""([（(])\s+"""), "$1")
        .replace(Regex("""\s+([）)])"""), "$1")
        .replace(Regex("""[ \t]{2,}"""), " ")
        .replace(Regex("""(?m)^\s*[-•、,，;；:：]+\s*$"""), "")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .trim()

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

internal fun String.needsFinalMarkdownRender(): Boolean {
    val source = trim()
    if (source.isBlank()) return false
    return source.contains("```") ||
        Regex("""(?m)^\s{0,3}#{1,6}\s+""").containsMatchIn(source) ||
        Regex("""(?m)^\s{0,3}(?:-{3,}|\*{3,}|_{3,})\s*$""").containsMatchIn(source) ||
        Regex("""(?m)^\s*(?:[-*+]|\d+\.)\s+""").containsMatchIn(source) ||
        Regex("""\[[^\]]+]\([^)]+\)""").containsMatchIn(source) ||
        Regex("""(?<!\*)\*\*[^*\n]+\*\*(?!\*)""").containsMatchIn(source) ||
        Regex("""(?<!`)`[^`\n]+`(?!`)""").containsMatchIn(source) ||
        source.hasCompletedMarkdownTable()
}

internal fun String.requiresAndroidMarkdownRender(): Boolean {
    val source = trim()
    if (source.isBlank()) return false
    return source.contains("```") ||
        source.contains("<") ||
        Regex("""\[[^\]]+]\([^)]+\)""").containsMatchIn(source) ||
        source.hasCompletedMarkdownTable()
}

internal fun String.toNativeMarkdownAnnotatedString(): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var written = 0
    var lastChar: Char? = null
    var listDepth = 0
    val orderedCounters = ArrayDeque<Int>()

    fun appendText(value: String?) {
        if (value.isNullOrEmpty()) return
        builder.append(value)
        written += value.length
        lastChar = value.last()
    }

    fun appendChar(value: Char) {
        builder.append(value)
        written += 1
        lastChar = value
    }

    fun appendBlockSeparator() {
        if (written > 0 && lastChar?.isWhitespace() != true) {
            appendChar('\n')
        }
    }

    fun push(style: SpanStyle, block: () -> Unit) {
        builder.pushStyle(style)
        block()
        builder.pop()
    }

    val visitor = object : AbstractVisitor() {
        override fun visit(text: MarkdownText) {
            appendText(text.literal)
        }

        override fun visit(code: Code) {
            push(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = MarkdownComposeCodeBackground,
                ),
            ) {
                appendText(code.literal)
            }
        }

        override fun visit(strongEmphasis: StrongEmphasis) {
            push(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                visitChildren(strongEmphasis)
            }
        }

        override fun visit(emphasis: Emphasis) {
            push(SpanStyle(fontStyle = FontStyle.Italic)) {
                visitChildren(emphasis)
            }
        }

        override fun visit(link: Link) {
            visitChildren(link)
        }

        override fun visit(image: Image) {
            visitChildren(image)
        }

        override fun visit(softLineBreak: SoftLineBreak) {
            appendChar(' ')
        }

        override fun visit(hardLineBreak: HardLineBreak) {
            appendChar('\n')
        }

        override fun visit(paragraph: Paragraph) {
            appendBlockSeparator()
            visitChildren(paragraph)
        }

        override fun visit(heading: Heading) {
            appendBlockSeparator()
            push(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                visitChildren(heading)
            }
        }

        override fun visit(thematicBreak: ThematicBreak) {
            appendBlockSeparator()
            push(SpanStyle(color = BuyPilotColors.Border)) {
                appendText("────────────")
            }
            appendBlockSeparator()
        }

        override fun visit(bulletList: BulletList) {
            appendBlockSeparator()
            listDepth += 1
            visitChildren(bulletList)
            listDepth -= 1
        }

        override fun visit(orderedList: OrderedList) {
            appendBlockSeparator()
            listDepth += 1
            orderedCounters.addLast(orderedList.startNumber)
            visitChildren(orderedList)
            orderedCounters.removeLast()
            listDepth -= 1
        }

        override fun visit(listItem: ListItem) {
            appendBlockSeparator()
            appendText("  ".repeat((listDepth - 1).coerceAtLeast(0)))
            val prefix = if (orderedCounters.isEmpty()) {
                "• "
            } else {
                val next = orderedCounters.removeLast()
                orderedCounters.addLast(next + 1)
                "$next. "
            }
            appendText(prefix)
            visitChildren(listItem)
        }

        override fun visit(codeBlock: FencedCodeBlock) {
            appendBlockSeparator()
            push(SpanStyle(fontFamily = FontFamily.Monospace, background = MarkdownComposeCodeBackground)) {
                appendText(codeBlock.literal.trim())
            }
        }

        override fun visit(codeBlock: IndentedCodeBlock) {
            appendBlockSeparator()
            push(SpanStyle(fontFamily = FontFamily.Monospace, background = MarkdownComposeCodeBackground)) {
                appendText(codeBlock.literal.trim())
            }
        }
    }

    PlainMarkdownParser.parse(withoutMarkdownBlockQuoteMarkers()).accept(visitor)
    return builder.toAnnotatedString()
}

internal object NativeMarkdownRenderer {
    private val renderedCache = object : LinkedHashMap<String, AnnotatedString>(
        MarkdownRenderCacheMaxEntries,
        0.75f,
        true,
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, AnnotatedString>?): Boolean =
            size > MarkdownRenderCacheMaxEntries
    }

    fun render(content: String): AnnotatedString {
        synchronized(this) {
            renderedCache[content]?.let { return it }
        }
        val rendered = content.toNativeMarkdownAnnotatedString()
        synchronized(this) {
            renderedCache[content]?.let { return it }
            renderedCache[content] = rendered
            return rendered
        }
    }

    fun clearForTest() {
        synchronized(this) {
            renderedCache.clear()
        }
    }
}

private fun String.hasCompletedMarkdownTable(): Boolean {
    val lines = lineSequence().map { it.trim() }.toList()
    return lines.windowed(size = 2).any { (header, separator) ->
        header.startsWith("|") &&
            header.endsWith("|") &&
            separator.startsWith("|") &&
            separator.endsWith("|") &&
            separator.contains("---")
    }
}

private fun typingStep(backlog: Int, done: Boolean): Int =
    when {
        backlog <= 0 -> 0
        done && backlog > 160 -> minOf(4, backlog)
        done && backlog > 72 -> minOf(3, backlog)
        backlog > 120 -> minOf(3, backlog)
        backlog > 48 -> minOf(2, backlog)
        else -> 1
    }

private fun typingDelayMs(lastVisibleChar: Char?, backlog: Int, done: Boolean): Long =
    when {
        lastVisibleChar == '\n' && backlog <= 48 -> if (done) 80L else 150L
        lastVisibleChar != null && backlog <= 48 && lastVisibleChar.isTypingPausePunctuation() -> {
            if (done) 72L else 128L
        }
        done && backlog > 160 -> 16L
        done && backlog > 72 -> 20L
        done -> 30L
        backlog > 120 -> 18L
        backlog > 48 -> 24L
        backlog > 20 -> 30L
        else -> 36L
    }

private fun Char.isTypingPausePunctuation(): Boolean =
    this in setOf('，', '。', '、', '；', '：', '！', '？', ',', '.', ';', ':', '!', '?')

@Composable
private fun AssistantText(
    content: String,
    style: TextStyle = TextStyle(
        color = BuyPilotColors.TextPrimary,
        fontSize = BuyPilotType.LargeBody,
        lineHeight = 26.sp,
    ),
) {
    if (content.isBlank()) return
    val renderMarkdown = remember(content) { content.needsFinalMarkdownRender() }
    if (!renderMarkdown) {
        PlainStreamingTextBlock(content = content, style = style)
        return
    }
    val useAndroidMarkdown = remember(content) { content.requiresAndroidMarkdownRender() }
    if (!useAndroidMarkdown) {
        NativeMarkdownTextBlock(content = content, style = style)
        return
    }
    MarkdownTextBlock(
        content = content,
        style = style,
    )
}

@Composable
private fun StreamingAssistantText(
    nodeKey: String,
    content: String,
    done: Boolean = false,
    revealState: TextRevealProgress? = null,
    alreadyCompleted: Boolean = false,
    stablePlainAfterLiveReveal: Boolean = false,
    animateInitialCompleted: Boolean = false,
    initialRevealDelayMs: Long = 0L,
    style: TextStyle = TextStyle(
        color = BuyPilotColors.TextPrimary,
        fontSize = BuyPilotType.LargeBody,
        lineHeight = 26.sp,
    ),
    onRevealComplete: (() -> Unit)? = null,
    onRevealActiveChange: ((Boolean) -> Unit)? = null,
    onRevealProgress: ((Int, Int) -> Unit)? = null,
) {
    if (content.isBlank()) {
        LaunchedEffect(nodeKey) {
            onRevealActiveChange?.invoke(false)
        }
        return
    }
    var hasSeenLiveStream by remember(nodeKey) { mutableStateOf(!done && !alreadyCompleted) }
    LaunchedEffect(nodeKey, done, alreadyCompleted) {
        if (!done && !alreadyCompleted) {
            hasSeenLiveStream = true
        }
    }
    val shouldRenderStatic = alreadyCompleted || (done && !hasSeenLiveStream && !animateInitialCompleted)
    if (shouldRenderStatic) {
        LaunchedEffect(nodeKey) {
            if (!alreadyCompleted) {
                onRevealActiveChange?.invoke(false)
                onRevealProgress?.invoke(content.length, content.length)
                if (done) onRevealComplete?.invoke()
            }
        }
        if (
            shouldKeepPlainTextRendererAfterStreaming(
                stablePlainAfterLiveReveal = stablePlainAfterLiveReveal,
                hasSeenLiveStream = hasSeenLiveStream,
                animateInitialCompleted = animateInitialCompleted,
            )
        ) {
            PlainStreamingTextBlock(content = content, style = style)
        } else {
            AssistantText(content = content, style = style)
        }
        return
    }

    var localVisibleLength by remember(nodeKey) {
        mutableIntStateOf(
            revealState?.visibleLength?.coerceAtMost(content.length) ?: 0,
        )
    }
    val latestOnRevealComplete by rememberUpdatedState(onRevealComplete)
    val latestOnRevealActiveChange by rememberUpdatedState(onRevealActiveChange)
    val latestOnRevealProgress by rememberUpdatedState(onRevealProgress)
    var completionReported by remember(nodeKey) { mutableStateOf(false) }
    val visibleLength = maxOf(localVisibleLength, revealState?.visibleLength ?: 0).coerceAtMost(content.length)

    LaunchedEffect(nodeKey, revealState?.visibleLength) {
        val target = maxOf(localVisibleLength, revealState?.visibleLength ?: 0).coerceAtMost(content.length)
        if (localVisibleLength < target || localVisibleLength > content.length) {
            localVisibleLength = target
        }
    }

    val latestContent by rememberUpdatedState(content)
    val latestDone by rememberUpdatedState(done)

    LaunchedEffect(nodeKey) {
        if (initialRevealDelayMs > 0 && localVisibleLength <= 0) {
            kotlinx.coroutines.delay(initialRevealDelayMs)
        }
        var activeReported = false
        while (true) {
            val targetContent = latestContent
            val targetLength = targetContent.length
            if (targetLength <= 0) {
                kotlinx.coroutines.delay(24L)
                continue
            }
            if (localVisibleLength > targetLength) {
                localVisibleLength = targetLength
            }
            val backlog = targetLength - localVisibleLength
            if (backlog <= 0) {
                if (latestDone) break
                kotlinx.coroutines.delay(24L)
                continue
            }
            if (!activeReported) {
                activeReported = true
                latestOnRevealActiveChange?.invoke(true)
            }
            val step = typingStep(backlog = backlog, done = latestDone)
            localVisibleLength = (localVisibleLength + step).coerceAtMost(targetLength)
            latestOnRevealProgress?.invoke(localVisibleLength, targetLength)
            val lastVisibleChar = targetContent.getOrNull(localVisibleLength - 1)
            kotlinx.coroutines.delay(typingDelayMs(lastVisibleChar, backlog = backlog, done = latestDone))
        }
        if (activeReported) {
            latestOnRevealActiveChange?.invoke(false)
        }
        if (latestDone && !completionReported) {
            completionReported = true
            val finalLength = latestContent.length
            latestOnRevealProgress?.invoke(finalLength, finalLength)
            latestOnRevealComplete?.invoke()
        }
    }

    if (visibleLength <= 0) return

    val visibleContent = content.take(visibleLength.coerceAtMost(content.length))
    PlainStreamingTextBlock(
        content = visibleContent,
        modifier = Modifier.fillMaxWidth(),
        style = style,
    )
}

internal fun shouldKeepPlainTextRendererAfterStreaming(
    stablePlainAfterLiveReveal: Boolean,
    hasSeenLiveStream: Boolean,
    animateInitialCompleted: Boolean,
): Boolean = stablePlainAfterLiveReveal || hasSeenLiveStream || animateInitialCompleted

@Composable
private fun StreamingAssistantText(
    content: String,
) {
    StreamingAssistantText(nodeKey = content, content = content, done = true)
}

internal fun List<ChatUiNode>.toTimelineRenderItems(): List<TimelineRenderItem> {
    val items = mutableListOf<TimelineRenderItem>()
    var assistantTurnId: String? = null
    val assistantNodes = mutableListOf<ChatUiNode>()
    var assistantSegmentIndex = 0

    fun flushAssistantTurn() {
        if (assistantNodes.isNotEmpty()) {
            items += AssistantTurnTimelineItem(
                turnId = assistantTurnId.orEmpty(),
                nodes = assistantNodes.toList(),
                segmentIndex = assistantSegmentIndex,
            )
            assistantSegmentIndex += 1
            assistantNodes.clear()
            assistantTurnId = null
        }
    }

    for (node in this) {
        val turnId = node.assistantTurnId()
        if (turnId != null) {
            if (assistantTurnId != null && assistantTurnId != turnId) {
                flushAssistantTurn()
            }
            assistantTurnId = turnId
            assistantNodes += node
        } else {
            flushAssistantTurn()
            items += when (node) {
                is UserMessageNode -> UserTimelineItem(node)
                else -> StandaloneTimelineItem(node)
            }
        }
    }
    flushAssistantTurn()
    return items
}

private fun ChatUiNode.assistantTurnId(): String? =
    when (this) {
        is ThinkingNode -> turnId
        is AiStreamNode -> turnId
        is ClarificationNode -> turnId
        is CriteriaNode -> turnId
        is ProductDeckNode -> turnId
        is FinalDecisionNode -> turnId
        else -> null
    }?.takeIf { it.isNotBlank() }

private fun ChatUiNode.revealTextKey(): String? =
    when (this) {
        is AiStreamNode -> key
        is ClarificationNode -> clarificationQuestionRevealKey()
        else -> null
    }

private fun TimelineRenderItem.containsNodeKey(nodeKey: String): Boolean =
    when (this) {
        is UserTimelineItem -> node.key == nodeKey
        is StandaloneTimelineItem -> node.key == nodeKey || node.revealTextKey() == nodeKey
        is AssistantTurnTimelineItem -> nodes.any { node ->
            node.key == nodeKey || node.revealTextKey() == nodeKey
        }
    }

private fun ClarificationNode.clarificationQuestionRevealKey(): String =
    "${key}_question"

private fun List<ChatUiNode>.timelineRevealKeys(): Set<String> =
    flatMapTo(mutableSetOf()) { node ->
        listOfNotNull(node.key, node.revealTextKey())
    }

@Composable
private fun PlainStreamingTextBlock(
    content: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    style: TextStyle = TextStyle(
        color = BuyPilotColors.TextPrimary,
        fontSize = BuyPilotType.LargeBody,
        lineHeight = 26.sp,
    ),
) {
    val displayContent = remember(content) {
        content.withoutStreamingMarkdownChrome().withoutInternalDebugTokens()
    }
    if (displayContent.isBlank()) return
    Text(
        text = displayContent,
        style = style,
        modifier = modifier,
    )
}

@Composable
private fun NativeMarkdownTextBlock(
    content: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    style: TextStyle = TextStyle(
        color = BuyPilotColors.TextPrimary,
        fontSize = BuyPilotType.LargeBody,
        lineHeight = 26.sp,
    ),
) {
    val displayContent = remember(content) { content.withoutInternalDebugTokens() }
    if (displayContent.isBlank()) return
    val annotated = remember(displayContent) {
        NativeMarkdownRenderer.render(displayContent)
    }
    if (annotated.isEmpty()) return
    Text(
        text = annotated,
        modifier = modifier,
        style = style,
    )
}

private object ChatMarkdownRenderer {
    @Volatile private var instance: Markwon? = null
    private val renderedCache = object : LinkedHashMap<String, Spanned>(
        MarkdownRenderCacheMaxEntries,
        0.75f,
        true,
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Spanned>?): Boolean =
            size > MarkdownRenderCacheMaxEntries
    }

    fun get(context: Context): Markwon {
        val cached = instance
        if (cached != null) return cached
        return synchronized(this) {
            instance ?: Markwon.builder(context.applicationContext)
                .usePlugin(object : AbstractMarkwonPlugin() {
                    override fun configureTheme(builder: MarkwonTheme.Builder) {
                        builder
                            .blockQuoteWidth(0)
                            .blockQuoteColor(android.graphics.Color.TRANSPARENT)
                            .codeBackgroundColor(MarkdownSoftBlockColor)
                            .codeBlockBackgroundColor(MarkdownSoftBlockColor)
                            .codeBlockMargin(8)
                    }
                })
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TablePlugin.create(context.applicationContext))
                .usePlugin(HtmlPlugin.create())
                .build()
                .also { instance = it }
        }
    }

    fun render(context: Context, content: String): Spanned {
        synchronized(this) {
            renderedCache[content]?.let { return it }
        }
        val rendered = get(context).toMarkdown(content.withoutMarkdownBlockQuoteMarkers())
        synchronized(this) {
            renderedCache[content]?.let { return it }
            renderedCache[content] = rendered
            return rendered
        }
    }
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
    val displayContent = remember(content) { content.withoutInternalDebugTokens() }
    if (displayContent.isBlank()) return
    val requiresAndroidMarkdown = remember(displayContent) { displayContent.requiresAndroidMarkdownRender() }
    if (!requiresAndroidMarkdown) {
        NativeMarkdownTextBlock(
            content = displayContent,
            modifier = modifier,
            style = style,
        )
        return
    }
    val context = LocalContext.current
    val appContext = context.applicationContext
    val markwon = remember(appContext) {
        ChatMarkdownRenderer.get(appContext)
    }
    val textColor = style.color.takeOrElse { BuyPilotColors.TextPrimary }
    val textColorArgb = remember(textColor) { textColor.toArgb() }
    val density = LocalDensity.current
    val fontSizePx = remember(density, style.fontSize) { with(density) { style.fontSize.toPx() } }
    val lineHeightPx = remember(density, style.lineHeight) { with(density) { style.lineHeight.toPx() } }
    val typefaceStyle = when (style.fontWeight) {
        FontWeight.Bold,
        FontWeight.ExtraBold,
        FontWeight.Black,
        FontWeight.SemiBold -> android.graphics.Typeface.BOLD
        else -> android.graphics.Typeface.NORMAL
    }
    val typeface = remember(typefaceStyle) {
        android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, typefaceStyle)
    }
    val renderKey = remember(displayContent, textColorArgb, fontSizePx, lineHeightPx, typefaceStyle) {
        MarkdownTextRenderTag(
            contentHash = displayContent.hashCode(),
            contentLength = displayContent.length,
            textColorArgb = textColorArgb,
            fontSizePx = fontSizePx,
            lineHeightPx = lineHeightPx,
            typefaceStyle = typefaceStyle,
        )
    }
    val renderedMarkdown = remember(appContext, displayContent) {
        ChatMarkdownRenderer.render(appContext, displayContent)
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            TextView(viewContext).apply {
                includeFontPadding = false
                setTextColor(textColorArgb)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSizePx)
                setLineSpacing((lineHeightPx - fontSizePx).coerceAtLeast(0f), 1f)
                this.typeface = typeface
                movementMethod = LinkMovementMethod.getInstance()
                linksClickable = true
            }
        },
        update = { textView ->
            textView.setTextColor(textColorArgb)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSizePx)
            textView.setLineSpacing((lineHeightPx - fontSizePx).coerceAtLeast(0f), 1f)
            textView.typeface = typeface
            if (textView.getTag(R.id.markdown_render_key) != renderKey) {
                markwon.setParsedMarkdown(textView, renderedMarkdown)
                textView.setTag(R.id.markdown_render_key, renderKey)
            }
        },
    )
}

private fun String.toClarificationUserMessage(defaultSkinTypeOptions: Set<String>): String {
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

private fun calculateFloatingPanelBottomPadding(
    density: Density,
    composerHeightPx: Int,
    imeBottomPx: Int,
): Dp = with(density) {
    val fallbackComposerPx = 96.dp.toPx()
    ((composerHeightPx.takeIf { it > 0 }?.toFloat() ?: fallbackComposerPx) + imeBottomPx).toDp() +
        FloatingPanelComposerGap
}

private fun List<TimelineRenderItem>.lastContentIndex(
    state: ChatUiState,
    hasTimelineError: Boolean,
): Int = lastIndex + if (state.lastError != null && !hasTimelineError) 1 else 0

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
                    else Color(0xFFFFC4B0),
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
            pressed -> Color(0xFFEDE3DF)
            else -> Color(0xFFFFF8F6)
        },
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "clarification_chip_background",
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> BuyPilotColors.Primary.copy(alpha = 0.32f)
            pressed -> Color(0xFFE0C9C2)
            else -> Color(0xFFEDE0DC)
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
private fun ClarificationManualInputRow(
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
            .background(Color(0xFFF5F7FA), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFE8ECF2), RoundedCornerShape(10.dp))
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

@Composable
private fun rememberCriteriaLabels(): CriteriaLabels =
    CriteriaLabels(
        summary = stringResource(R.string.criteria_label_summary),
        category = stringResource(R.string.criteria_label_category),
        coreNeed = stringResource(R.string.criteria_label_core_need),
        targetUser = stringResource(R.string.criteria_label_target_user),
        budget = stringResource(R.string.criteria_label_budget),
        scenario = stringResource(R.string.criteria_label_scenario),
        exclusions = stringResource(R.string.criteria_label_exclusions),
        avoidPrefix = stringResource(R.string.criteria_avoid_prefix),
    )

@Composable
private fun CriteriaSummaryCard(
    motionKey: String,
    payload: CriteriaCardPayload,
    motionEnabled: Boolean,
    alreadyEntered: Boolean,
    onEntered: () -> Unit,
    onEdit: () -> Unit,
) {
    val criteria = payload.criteria
    val summary = criteria.summary.withoutMarkdownMarkup().trim()
    val labels = rememberCriteriaLabels()
    val editLabel = stringResource(R.string.criteria_edit_title)
    val properties = criteria.receiptProperties(labels)
    val headline = criteria.criteriaReceiptHeadline().ifBlank { summary }

    StructuredCardMotion(
        key = motionKey,
        motionEnabled = motionEnabled,
        alreadyEntered = alreadyEntered,
        durationMillis = CriteriaCardEnterMs,
        initialOffsetY = 8.dp,
        initialScale = 1f,
        onEntered = onEntered,
    ) { progress ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color.Black.copy(alpha = 0.04f),
                    spotColor = Color.Black.copy(alpha = 0.06f),
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFEFD),
                            Color(0xFFF9FAFB),
                        ),
                    ),
                    RoundedCornerShape(16.dp),
                )
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE8ECF0),
                            Color(0xFFDFE3E8),
                        ),
                    ),
                    RoundedCornerShape(16.dp),
                ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val headerProgress = segmentProgress(progress, 0f, 0.52f)
                            alpha = headerProgress
                            translationY = (1f - headerProgress) * 6f
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(BuyPilotColors.Primary, CircleShape),
                        )
                        Text(
                            text = "筛选条件",
                            color = BuyPilotColors.TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp,
                        )
                    }
                    Text(
                        text = editLabel,
                        color = BuyPilotColors.Primary.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button, onClick = onEdit)
                            .background(BuyPilotColors.PrimarySoft.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
                if (headline.isNotBlank()) {
                    Text(
                        text = headline,
                        color = BuyPilotColors.TextPrimary,
                        fontSize = 17.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.graphicsLayer {
                            val headlineProgress = segmentProgress(progress, 0.12f, 0.68f)
                            alpha = headlineProgress
                            translationY = (1f - headlineProgress) * 5f
                        },
                    )
                }
                if (properties.isNotEmpty()) {
                    CriteriaReceiptTags(
                        properties = properties,
                        parentProgress = progress,
                    )
                }
            }
        }
    }
}

@Composable
private fun CriteriaReceiptTags(
    properties: List<CriteriaReceiptProperty>,
    parentProgress: Float,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        contentPadding = PaddingValues(end = 4.dp),
    ) {
        itemsIndexed(
            items = properties,
            key = { _, property -> "${property.label}:${property.value}" },
        ) { index, property ->
            CriteriaReceiptTag(
                property = property,
                revealProgress = segmentProgress(
                    value = parentProgress,
                    start = 0.24f + index.coerceAtMost(3) * 0.06f,
                    end = 0.74f + index.coerceAtMost(3) * 0.06f,
                ),
            )
        }
    }
}

@Composable
private fun CriteriaReceiptTag(
    property: CriteriaReceiptProperty,
    revealProgress: Float = 1f,
) {
    Text(
        text = property.value,
        modifier = Modifier
            .graphicsLayer {
                alpha = revealProgress
                translationY = (1f - revealProgress) * 4f
            }
            .background(Color(0xFFF0F2F5), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFE4E8ED).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = BuyPilotColors.TextPrimary.copy(alpha = 0.78f),
        fontSize = 13.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
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
    prominent || value.length >= 16

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
private fun StructuredCardMotion(
    key: String,
    motionEnabled: Boolean,
    alreadyEntered: Boolean,
    durationMillis: Int,
    initialOffsetY: Dp,
    initialScale: Float,
    onEntered: () -> Unit,
    content: @Composable (Float) -> Unit,
) {
    val progress = remember(key) { Animatable(if (!motionEnabled || alreadyEntered) 1f else 0f) }
    val density = LocalDensity.current
    val latestOnEntered by rememberUpdatedState(onEntered)

    LaunchedEffect(key, motionEnabled, alreadyEntered) {
        if (!motionEnabled || alreadyEntered) {
            progress.snapTo(1f)
            if (!alreadyEntered) latestOnEntered()
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMillis, easing = PremiumRevealEase),
        )
        latestOnEntered()
    }

    val t = progress.value
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = t
            translationY = with(density) { initialOffsetY.toPx() } * (1f - t)
            scaleX = initialScale + (1f - initialScale) * t
            scaleY = initialScale + (1f - initialScale) * t
        },
    ) {
        content(t)
    }
}

@Composable
private fun rememberProductDeckArrivalProgress(
    key: String,
    motionEnabled: Boolean,
    alreadyEntered: Boolean,
    onEntered: () -> Unit,
): Float {
    val progress = remember(key) { Animatable(if (!motionEnabled || alreadyEntered) 1f else 0f) }
    val latestOnEntered by rememberUpdatedState(onEntered)

    LaunchedEffect(key, motionEnabled, alreadyEntered) {
        if (!motionEnabled || alreadyEntered) {
            progress.snapTo(1f)
            if (!alreadyEntered) latestOnEntered()
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = ProductDeckArrivalMs, easing = PremiumRevealEase),
        )
        latestOnEntered()
    }

    return progress.value
}

@Composable
private fun ProductDeckArrivalMotion(
    arrivalProgress: Float,
    content: @Composable (Float) -> Unit,
) {
    val density = LocalDensity.current
    val t = arrivalProgress
    val seedT = segmentProgress(t, 0f, 0.12f)
    val expandT = segmentProgress(t, 0.18f, 0.78f)
    val contentT = segmentProgress(t, 0.6f, 1f)
    val settleT = segmentProgress(t, 0.72f, 1f)
    val shadowT = segmentProgress(t, 0.26f, 1f)
    val widthFraction = lerp(0.26f, 1f, expandT)
    val cardHeight = lerp(54f, 218f, expandT)
    val cornerDp = lerp(30f, 18f, expandT)
    val offsetY = lerp(24f, 0f, settleT)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(218.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .height(cardHeight.dp)
                .graphicsLayer {
                    alpha = 0.78f + seedT * 0.22f
                    translationY = with(density) { offsetY.dp.toPx() }
                    scaleX = 0.975f + settleT * 0.025f
                    scaleY = 0.975f + settleT * 0.025f
                    transformOrigin = TransformOrigin(0.06f, 0.5f)
                }
                .shadow(
                    elevation = lerp(2f, 8f, shadowT).dp,
                    shape = RoundedCornerShape(cornerDp.dp),
                    ambientColor = Color(0xFF8E97A4).copy(alpha = 0.06f),
                    spotColor = Color.Black.copy(alpha = 0.05f),
                ),
            color = BuyPilotColors.SurfaceCard,
            shape = RoundedCornerShape(cornerDp.dp),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = BuyPilotColors.Border.copy(alpha = lerp(0.38f, 0.72f, expandT)),
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = contentT
                        translationY = (1f - contentT) * 16f
                    },
            ) {
                content(t)
            }
        }
        if (t < 0.82f) {
            ProductArrivalSeedBubble(
                progress = (1f - segmentProgress(t, 0.48f, 0.82f)) * seedT,
                modifier = Modifier
                    .padding(start = 12.dp, top = 84.dp)
                    .zIndex(2f),
            )
        }
    }
}

@Composable
private fun ProductArrivalSeedBubble(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val pulse = rememberInfiniteTransition(label = "product_seed_pulse")
    val shimmer by pulse.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.52f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 920, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "product_seed_shimmer",
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = progress
                scaleX = 0.88f + progress * 0.12f
                scaleY = 0.88f + progress * 0.12f
            }
            .size(width = 74.dp, height = 48.dp)
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.07f),
            )
            .background(BuyPilotColors.SurfaceCard, RoundedCornerShape(24.dp))
            .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.58f), RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 38.dp, height = 26.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BuyPilotColors.PrimarySoft.copy(alpha = 0.28f + shimmer * 0.26f)),
        )
    }
}

@Composable
private fun ProductImageLoadingSeed(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    if (progress <= 0.01f) return
    val pulse = rememberInfiniteTransition(label = "product_image_seed_pulse")
    val shimmer by pulse.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 820, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "product_image_seed_shimmer",
    )
    Box(
        modifier = modifier
            .graphicsLayer { alpha = (1f - progress * 0.78f).coerceIn(0f, 1f) }
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        BuyPilotColors.SurfaceCard.copy(alpha = 0.78f),
                        BuyPilotColors.PrimarySoft.copy(alpha = 0.2f + shimmer * 0.22f),
                        BuyPilotColors.SurfaceMuted.copy(alpha = 0.72f),
                    ),
                ),
            ),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProductRecommendationStrip(
    node: ProductDeckNode,
    backendBaseUrl: String,
    swipeState: ProductSwipeState?,
    awaitingConvergence: Boolean,
    hasPendingDecision: Boolean,
    deckConverged: Boolean,
    deckStillStreaming: Boolean,
    convergeActionReady: Boolean,
    compactHistory: Boolean,
    motionEnabled: Boolean,
    alreadyEntered: Boolean,
    onEntered: () -> Unit,
    onOpen: (String, String?) -> Unit,
    onOpenDetail: (String, String) -> Unit,
    onConverge: () -> Unit,
) {
    val products = node.products
    if (products.isEmpty()) return
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
    val singleCandidate = products.size == 1 && !deckStillStreaming
    val deckFullyHandled = isProductDeckFullyHandled(products, swipeState)
    val preferredPage = preferredProductCarouselPage(products, swipeState)

    val pagerState = rememberPagerState(
        initialPage = preferredPage,
        pageCount = { products.size },
    )
    val activeIndex = pagerState.currentPage.coerceIn(0, products.lastIndex)

    LaunchedEffect(node.deckId, preferredPage, products.size) {
        if (preferredPage != pagerState.currentPage && !pagerState.isScrollInProgress) {
            pagerState.scrollToPage(preferredPage)
        }
    }

    val arrivalProgress = rememberProductDeckArrivalProgress(
        key = node.key,
        motionEnabled = motionEnabled,
        alreadyEntered = alreadyEntered,
        onEntered = onEntered,
    )
    val chromeProgress = segmentProgress(arrivalProgress, 0.46f, 1f)
    val convergeButtonTargetProgress = if (awaitingConvergence && !deckFullyHandled && convergeActionReady) {
        segmentProgress(arrivalProgress, 0.72f, 1f)
    } else {
        0f
    }
    val convergeButtonProgress by animateFloatAsState(
        targetValue = convergeButtonTargetProgress,
        animationSpec = tween(durationMillis = 220, easing = MenuEaseOut),
        label = "product_deck_converge_button_progress",
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = chromeProgress
                    translationY = (1f - chromeProgress) * 8f
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (singleCandidate) "唯一匹配商品" else "候选商品",
                    color = BuyPilotColors.TextPrimary,
                    fontSize = BuyPilotType.Title,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(BuyPilotColors.PrimarySoft.copy(alpha = 0.7f), CircleShape)
                    .border(1.dp, BuyPilotColors.Primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = products.size.toString(),
                    color = BuyPilotColors.PrimaryDark,
                    fontSize = 13.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
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
                    val productId = payload.product.productId.takeIf { it.isNotBlank() }
                    if (productId != null) {
                        onOpenDetail(node.deckId, productId)
                    } else {
                        onOpen(node.deckId, null)
                    }
                }
                if (page == 0) {
                    ProductDeckArrivalMotion(
                        arrivalProgress = arrivalProgress,
                    ) { cardProgress ->
                        ProductRecommendationThumb(
                            payload = payload,
                            backendBaseUrl = backendBaseUrl,
                            selected = page == activeIndex,
                            arrivalProgress = cardProgress,
                            onClick = openProduct,
                            modifier = thumbModifier,
                        )
                    }
                } else {
                    ProductRecommendationThumb(
                        payload = payload,
                        backendBaseUrl = backendBaseUrl,
                        selected = page == activeIndex,
                        arrivalProgress = 1f,
                        onClick = openProduct,
                        modifier = thumbModifier,
                    )
                }
            }
        }
        ProductDeckFooterControls(
            showIndicators = !singleCandidate,
            productCount = products.size,
            activeIndex = activeIndex,
            chromeProgress = chromeProgress,
            buttonProgress = convergeButtonProgress,
            buttonLabel = "帮我选",
            onConverge = onConverge,
        )
    }
}

@Composable
private fun ProductDeckFooterControls(
    showIndicators: Boolean,
    productCount: Int,
    activeIndex: Int,
    chromeProgress: Float,
    buttonProgress: Float,
    buttonLabel: String,
    onConverge: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (showIndicators) 46.dp else 30.dp)
            .graphicsLayer {
                alpha = maxOf(chromeProgress, buttonProgress * 0.96f)
                translationY = (1f - chromeProgress.coerceIn(0f, 1f)) * 4f
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
            onClick = onConverge,
            modifier = Modifier,
        )
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
private fun ProductDeckConvergeMiniButton(
    label: String,
    visibleProgress: Float,
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
            .clickable(role = Role.Button, onClick = onClick)
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
                .background(Color(0xFFF6F8FA))
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
private fun CandidateActionButton(
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
    arrivalProgress: Float = 1f,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val product = payload.product
    val tags = rememberProductDisplayTags(payload = payload, limit = 2)
    val reason = rememberPlainProductReason(payload.reason)
    val cardShape = RoundedCornerShape(18.dp)
    val borderAlpha = if (selected) 0.72f else 0.54f
    val cardFillProgress = segmentProgress(arrivalProgress, 0.36f, 0.76f)
    val imageProgress = segmentProgress(arrivalProgress, 0.5f, 0.92f)
    val detailProgress = segmentProgress(arrivalProgress, 0.64f, 1f)

    Surface(
        modifier = modifier
            .height(218.dp)
            .clip(cardShape)
            .clickable(onClick = onClick),
        color = BuyPilotColors.SurfaceCard.copy(alpha = cardFillProgress),
        shape = cardShape,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border.copy(alpha = borderAlpha * cardFillProgress)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .graphicsLayer {
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
                    .background(Color(0xFFF6F8FA))
                    .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.36f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                ProductImageLoadingSeed(
                    progress = segmentProgress(arrivalProgress, 0f, 0.48f),
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
private fun DecisionSummaryCard(
    motionKey: String,
    payload: FinalDecisionPayload,
    productsById: Map<String, ProductCardPayload>,
    productDeckIdByProductId: Map<String, String>,
    backendBaseUrl: String,
    motionEnabled: Boolean,
    alreadyEntered: Boolean,
    onEntered: () -> Unit,
    onEvidence: () -> Unit,
    onProductDetailOpen: (String, String) -> Unit,
    onQuickAction: (QuickActionPayload) -> Unit,
) {
    val winnerProductId = payload.winnerProductId?.takeIf { it.isNotBlank() }
    val winner = winnerProductId?.let { winnerId -> productsById[winnerId] }
    val winnerDeckId = winnerProductId?.let { productDeckIdByProductId[it] }
    val openWinnerDetail = winnerDeckId?.let { deckId ->
        { onProductDetailOpen(deckId, winnerProductId.orEmpty()) }
    }
    val product = winner?.product
    val whyItems = payload.why.map { it.withoutMarkdownMarkup().withoutInternalDebugTokens().trim() }.filter { it.isNotBlank() }
    val notForItems = payload.notFor.map { it.withoutMarkdownMarkup().withoutInternalDebugTokens().trim() }.filter { it.isNotBlank() }
    val statusBadge = payload.decisionStatusBadge()
    val nextActions = payload.nextActions.filter { it.label.isNotBlank() }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DecisionSummaryIntro(
            motionKey = motionKey,
            summary = payload.decisionSummaryText(),
            motionEnabled = motionEnabled,
            alreadyEntered = alreadyEntered,
        )
        if (
            winner != null ||
            !winnerProductId.isNullOrBlank() ||
            whyItems.isNotEmpty() ||
            notForItems.isNotEmpty() ||
            statusBadge?.showCardWhenEmpty == true
        ) {
            StructuredCardMotion(
                key = motionKey,
                motionEnabled = motionEnabled,
                alreadyEntered = alreadyEntered,
                durationMillis = DecisionCardEnterMs,
                initialOffsetY = 16.dp,
                initialScale = 0.925f,
                onEntered = onEntered,
            ) { cardProgress ->
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
                                DecisionReasonList(items = whyItems, parentProgress = cardProgress)
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
        }
        if (nextActions.isNotEmpty()) {
            ScrollableQuickActionRow(
                actions = nextActions,
                onQuickAction = onQuickAction,
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

    val lineProgress = segmentProgress(progress.value, 0f, 0.72f)
    val textProgress = segmentProgress(progress.value, 0.24f, 1f)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .graphicsLayer {
                    alpha = lineProgress
                    scaleX = lineProgress
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
                .background(BuyPilotColors.Border.copy(alpha = 0.72f), CircleShape),
        )
        Box(
            modifier = Modifier.graphicsLayer {
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
fun ProductSwipeModeScreen(
    state: ChatUiState,
    deckId: String,
    initialProductId: String?,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onDeckCompleted: (String) -> Unit,
    onOpenDetail: (String, String) -> Unit,
    onSwipe: (String, String, String, String, String?) -> Unit,
    onUndo: (String) -> Unit,
) {
    val deck = state.findProductDeck(deckId)
    val products = deck?.products.orEmpty()
    val swipeState = state.productSwipeStates[deckId] ?: ProductSwipeState()
    val remainingProducts = products.filterNot { it.product.productId in swipeState.swipedProductIds }
    val currentProductId = swipeState.currentProductId
        ?.takeIf { id -> products.size == 1 || remainingProducts.any { it.product.productId == id } }
        ?: initialProductId?.takeIf { id -> remainingProducts.any { it.product.productId == id } }
        ?: remainingProducts.firstOrNull()?.product?.productId
        ?: products.firstOrNull()?.product?.productId
    val payload = if (products.size == 1) {
        products.firstOrNull { it.product.productId == currentProductId } ?: products.firstOrNull()
    } else {
        remainingProducts.firstOrNull { it.product.productId == currentProductId }
    }
    val haptics = LocalHapticFeedback.current
    val cardStackBridge = remember(deckId) { CardStackBridge() }
    val deckFullyHandled = isProductDeckFullyHandled(
        products = products,
        swipeState = swipeState,
    )
    var autoClosed by rememberSaveable(deckId) { mutableStateOf(false) }
    val latestOnBack by rememberUpdatedState(onBack)
    val latestOnDeckCompleted by rememberUpdatedState(onDeckCompleted)

    LaunchedEffect(deckId, deckFullyHandled) {
        if (!deckFullyHandled || autoClosed) return@LaunchedEffect
        autoClosed = true
        latestOnDeckCompleted(deckId)
        kotlinx.coroutines.delay(ProductDeckAutoCloseDelayMs)
        latestOnBack()
    }

    Surface(color = BuyPilotColors.SurfaceBg, modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ProductSwipeTopBar(
                title = if (products.size > 1) "商品详情" else "唯一候选",
                onBack = onBack,
                canUndo = swipeState.undoStack.isNotEmpty(),
                onUndo = {
                    cardStackBridge.rewind()
                    onUndo(deckId)
                },
            )

            if (deck == null || payload == null) {
                if (deck != null && products.isNotEmpty() && remainingProducts.isEmpty()) {
                    ProductSwipeCompletedState(
                    )
                } else {
                    ExpiredRecommendationState(onBack = onBack)
                }
                return@Column
            }

            if (products.size == 1) {
                ProductSingleCandidateModeContent(
                    payload = payload,
                    backendBaseUrl = state.backendBaseUrl,
                    onOpenDetail = { onOpenDetail(deckId, payload.product.productId) },
                    onLike = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSwipe(deckId, payload.product.productId, "like", "like", "用户标记唯一候选感兴趣")
                    },
                )
            } else {
                ProductSwipeModeContent(
                    animationKey = deckId,
                    products = remainingProducts,
                    currentProductId = payload.product.productId,
                    backendBaseUrl = state.backendBaseUrl,
                    cardStackBridge = cardStackBridge,
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
}

@Composable
private fun ProductSwipeTopBar(
    title: String,
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
            title = title,
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
private fun ProductSingleCandidateModeContent(
    payload: ProductCardPayload,
    backendBaseUrl: String,
    onOpenDetail: () -> Unit,
    onLike: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "唯一候选",
                color = BuyPilotColors.TextPrimary,
                fontSize = 29.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "这轮只有一个匹配商品，不需要左右滑。先看详情，最终建议会判断它是否真的适合你。",
                color = BuyPilotColors.TextSecondary,
                fontSize = BuyPilotType.LargeBody,
                lineHeight = 24.sp,
            )
        }
        ProductHeroCard(
            payload = payload,
            backendBaseUrl = backendBaseUrl,
            modifier = Modifier.weight(1f),
            onOpen = onOpenDetail,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CandidateActionButton(
                label = "查看详情",
                leadingIconRes = R.drawable.ic_article_24,
                primary = false,
                modifier = Modifier.weight(1f),
                onClick = onOpenDetail,
            )
            CandidateActionButton(
                label = "感兴趣",
                leadingIconRes = R.drawable.ic_favorite_24,
                primary = true,
                modifier = Modifier.weight(1f),
                onClick = onLike,
            )
        }
    }
}

@Composable
private fun ProductSwipeCompletedState() {
    val mascotProgress by rememberRouteEnterProgress(
        key = "product_swipe_completed_mascot",
        durationMillis = 260,
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 34.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.redbean_bun_mascot_eyes_variant),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .widthIn(max = 330.dp)
                .aspectRatio(1f)
                .graphicsLayer {
                    alpha = mascotProgress
                    translationY = (1f - mascotProgress) * 18f
                    scaleX = lerp(0.96f, 1f, mascotProgress)
                    scaleY = lerp(0.96f, 1f, mascotProgress)
                },
        )
    }
}

@Composable
private fun ProductSwipeModeContent(
    animationKey: String,
    products: List<ProductCardPayload>,
    currentProductId: String,
    backendBaseUrl: String,
    cardStackBridge: CardStackBridge,
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
    val routeProgressState = rememberRouteEnterProgress(
        key = "product_swipe_detail_$animationKey",
        durationMillis = ProductSwipeDetailEnterMs,
    )

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
                .fillMaxWidth()
                .graphicsLayer {
                    val cardEnter = segmentProgress(routeProgressState.value, 0f, 0.86f)
                    alpha = cardEnter
                    translationY = (1f - cardEnter) * 22f
                    scaleX = lerp(0.982f, 1f, cardEnter)
                    scaleY = lerp(0.982f, 1f, cardEnter)
                },
            contentAlignment = Alignment.Center,
        ) {
            ProductCardStackView(
                products = orderedProducts,
                backendBaseUrl = backendBaseUrl,
                bridge = cardStackBridge,
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
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val controlsEnter = segmentProgress(routeProgressState.value, 0.34f, 1f)
                    alpha = controlsEnter
                    translationY = (1f - controlsEnter) * 18f
                    scaleX = lerp(0.96f, 1f, controlsEnter)
                    scaleY = lerp(0.96f, 1f, controlsEnter)
                },
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

internal fun isProductDeckFullyHandled(
    products: List<ProductCardPayload>,
    swipeState: ProductSwipeState?,
): Boolean {
    if (products.size <= 1) return false
    val handledProductIds = swipeState?.swipedProductIds.orEmpty().toSet()
    return products.all { it.product.productId in handledProductIds }
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
                .setDuration(ProductSwipeAnimationMs)
                .setInterpolator(PathInterpolator(0.2f, 0f, 0f, 1f))
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
    modifier: Modifier = Modifier,
    onSwiped: (Direction, Int) -> Unit,
    onStackPositionChanged: (Int) -> Unit,
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
                setTranslationInterval(13f)
                setScaleInterval(0.958f)
                setSwipeThreshold(0.24f)
                setMaxDegree(7f)
                setDirections(Direction.HORIZONTAL)
                setCanScrollHorizontal(true)
                setCanScrollVertical(false)
                setSwipeableMethod(SwipeableMethod.AutomaticAndManual)
                setSwipeAnimationSetting(
                    SwipeAnimationSetting.Builder()
                        .setDirection(Direction.Right)
                        .setDuration(ProductSwipeAnimationMs)
                        .setInterpolator(PathInterpolator(0.2f, 0f, 0f, 1f))
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
            val nextProductIds = products.map { it.product.productId }
            if (adapter.productIds != nextProductIds) {
                val changed = adapter.submit(products)
                stackView.scrollToPosition(0)
                if (changed) onStackPositionChanged(0)
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
) : RecyclerView.Adapter<ProductCardStackAdapter.ProductCardViewHolder>() {
    private val items = mutableListOf<ProductCardPayload>()
    val productIds: List<String>
        get() = items.map { it.product.productId }

    fun submit(nextItems: List<ProductCardPayload>): Boolean {
        if (items == nextItems) return false
        items.clear()
        items.addAll(nextItems)
        notifyDataSetChanged()
        return true
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

        val positionBadge = TextView(context).apply {
            includeFontPadding = false
            setTextColor(textSecondaryColor)
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 999 * density
                setColor(android.graphics.Color.argb(232, 255, 254, 252))
                setStroke((1 * density).roundToInt().coerceAtLeast(1), android.graphics.Color.argb(150, 226, 231, 238))
            }
            setPadding((10 * density).roundToInt(), (5 * density).roundToInt(), (10 * density).roundToInt(), (5 * density).roundToInt())
        }
        imageStage.addView(
            positionBadge,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.TOP or android.view.Gravity.END,
            ).apply {
                topMargin = (14 * density).roundToInt()
                rightMargin = (14 * density).roundToInt()
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

        val detailRows = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
        }
        content.addView(
            detailRows,
            android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (12 * density).roundToInt()
            },
        )

        val riskText = TextView(context).apply {
            includeFontPadding = false
            setTextColor(textSecondaryColor)
            textSize = 12.5f
            maxLines = 2
            setLineSpacing((2 * density), 1f)
            ellipsize = android.text.TextUtils.TruncateAt.END
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 12 * density
                setColor(android.graphics.Color.rgb(255, 247, 248))
                setStroke((1 * density).roundToInt().coerceAtLeast(1), android.graphics.Color.rgb(255, 226, 232))
            }
            setPadding((10 * density).roundToInt(), (7 * density).roundToInt(), (10 * density).roundToInt(), (7 * density).roundToInt())
        }
        content.addView(
            riskText,
            android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (10 * density).roundToInt()
            },
        )

        val overlays = createSwipeOverlays(context, primaryColor, textPrimaryColor, density)
        root.addView(overlays.first)
        root.addView(overlays.second)

        return ProductCardViewHolder(
            root,
            image,
            positionBadge,
            reasonRow,
            reasonText,
            brand,
            name,
            price,
            evidence,
            tagRow,
            detailRows,
            riskText,
            tagBgColor,
            tagBorderColor,
            textPrimaryColor,
            textSecondaryColor,
            textMutedColor,
            primaryDarkColor,
        )
    }

    override fun onBindViewHolder(holder: ProductCardViewHolder, position: Int) {
        val payload = items[position]
        holder.bind(
            payload = payload,
            backendBaseUrl = backendBaseUrl,
            positionLabel = "${position + 1} / ${items.size}",
        )
    }

    override fun getItemCount(): Int = items.size

    private fun createSwipeOverlays(
        context: android.content.Context,
        primaryColor: Int,
        textPrimaryColor: Int,
        density: Float,
    ): Pair<View, View> {
        fun overlay(id: Int, label: String, color: Int, gravity: Int, rotationDegrees: Float): FrameLayout =
            FrameLayout(context).apply {
                this.id = id
                alpha = 0f
                addView(
                    TextView(context).apply {
                        text = label
                        includeFontPadding = false
                        setTextColor(color)
                        textSize = 20f
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        letterSpacing = 0.04f
                        rotation = rotationDegrees
                        background = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                            cornerRadius = 999 * density
                            setColor(android.graphics.Color.argb(238, 255, 254, 252))
                            setStroke((1.5f * density).roundToInt().coerceAtLeast(1), color)
                        }
                        setPadding((16 * density).roundToInt(), (8 * density).roundToInt(), (16 * density).roundToInt(), (8 * density).roundToInt())
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
        return overlay(
            id = com.yuyakaido.android.cardstackview.R.id.left_overlay,
            label = "不合适",
            color = textPrimaryColor,
            gravity = android.view.Gravity.TOP or android.view.Gravity.END,
            rotationDegrees = 8f,
        ) to overlay(
            id = com.yuyakaido.android.cardstackview.R.id.right_overlay,
            label = "喜欢",
            color = primaryColor,
            gravity = android.view.Gravity.TOP or android.view.Gravity.START,
            rotationDegrees = -8f,
        )
    }

    class ProductCardViewHolder(
        itemView: View,
        private val image: ImageView,
        private val positionBadge: TextView,
        private val reasonRow: android.widget.LinearLayout,
        private val reasonText: TextView,
        private val brand: TextView,
        private val name: TextView,
        private val price: TextView,
        private val evidence: TextView,
        private val tagRow: android.widget.LinearLayout,
        private val detailRows: android.widget.LinearLayout,
        private val riskText: TextView,
        private val tagBgColor: Int,
        private val tagBorderColor: Int,
        private val textPrimaryColor: Int,
        private val textSecondaryColor: Int,
        private val textMutedColor: Int,
        private val primaryDarkColor: Int,
    ) : RecyclerView.ViewHolder(itemView) {
        fun bind(
            payload: ProductCardPayload,
            backendBaseUrl: String,
            positionLabel: String,
        ) {
            val product = payload.product
            itemView.setOnClickListener(null)
            itemView.isClickable = false
            positionBadge.text = positionLabel
            image.load(product.imageUrl.resolveProductImageUrl(backendBaseUrl)) {
                crossfade(180)
                placeholder(R.drawable.product_image_placeholder)
                error(R.drawable.product_image_placeholder)
                fallback(R.drawable.product_image_placeholder)
            }
            val reason = payload.reason
                .withoutMarkdownMarkup()
                .withoutInternalDebugTokens()
                .trim()
                .takeIf { it.isNotBlank() }
            reasonRow.visibility = if (reason == null) View.GONE else View.VISIBLE
            reasonText.text = reason.orEmpty()
            brand.text = product.brandLabel()
            name.text = product.displayName()
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
            detailRows.removeAllViews()
            val detailItems = listOf(
                "适用对象" to product.skinTypeMatch.userFacingJoinedOrFallback(),
                "匹配标签" to product.ingredientTags.userFacingJoinedOrFallback(),
                "使用场景" to product.useScenario.userFacingJoinedOrFallback(),
            ).filter { (_, value) -> value.isNotBlank() }
            detailRows.visibility = if (detailItems.isEmpty()) View.GONE else View.VISIBLE
            detailItems.take(3).forEach { (label, value) ->
                detailRows.addView(createDetailRow(detailRows.context, label, value))
            }
            val risk = payload.riskNotes
                .map { it.withoutMarkdownMarkup().withoutInternalDebugTokens().trim() }
                .firstOrNull { it.isNotBlank() }
            riskText.visibility = if (risk == null) View.GONE else View.VISIBLE
            riskText.text = risk.orEmpty()
        }

        private fun createDetailRow(
            context: android.content.Context,
            label: String,
            value: String,
        ): View {
            val density = context.resources.displayMetrics.density
            return android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, (3 * density).roundToInt(), 0, (3 * density).roundToInt())
                addView(
                    TextView(context).apply {
                        text = label
                        includeFontPadding = false
                        setTextColor(textMutedColor)
                        textSize = 11.5f
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    },
                    android.widget.LinearLayout.LayoutParams((64 * density).roundToInt(), ViewGroup.LayoutParams.WRAP_CONTENT),
                )
                addView(
                    TextView(context).apply {
                        text = value
                        includeFontPadding = false
                        setTextColor(textPrimaryColor)
                        textSize = 13f
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    },
                    android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
                )
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
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onOpenEvidence: (String, String) -> Unit,
    onSwipe: (String, String, String, String, String?) -> Unit,
    onDeckCompleted: (String) -> Unit,
) {
    var activeProductId by rememberSaveable(deckId, productId) { mutableStateOf(productId) }
    val payload = state.findProduct(deckId, activeProductId)
    val haptics = LocalHapticFeedback.current
    val latestOnBack by rememberUpdatedState(onBack)
    var submittedFeedback by rememberSaveable(deckId, activeProductId) { mutableStateOf<String?>(null) }
    var pendingAdvanceProductId by rememberSaveable(deckId, activeProductId) { mutableStateOf<String?>(null) }
    val handledProductIds = state.productSwipeStates[deckId]?.swipedProductIds.orEmpty().toSet()
    val deckCanConverge = state.canOpenDeckForConvergence(deckId)
    val readOnlyByState = !deckCanConverge ||
        state.hasConvergedDecisionForDeck(deckId) ||
        activeProductId in handledProductIds
    var choiceActionsAvailable by rememberSaveable(deckId, activeProductId) {
        mutableStateOf(!readOnlyByState)
    }
    val nextUnhandledProductId = state.findProductDeck(deckId)
        ?.products
        .orEmpty()
        .map { it.product.productId }
        .firstOrNull { id ->
            id.isNotBlank() && id != activeProductId && id !in handledProductIds
        }
    val deckWillBeFullyHandledAfterCurrentChoice = state.findProductDeck(deckId)
        ?.products
        .orEmpty()
        .mapNotNull { it.product.productId.takeIf(String::isNotBlank) }
        .let { productIds ->
            productIds.size >= 2 &&
                activeProductId.isNotBlank() &&
                productIds.all { id -> id == activeProductId || id in handledProductIds }
        }
    var completionRequested by rememberSaveable(deckId) { mutableStateOf(false) }
    val latestOnDeckCompleted by rememberUpdatedState(onDeckCompleted)

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
    val choiceActionsActive = choiceActionsAvailable && submittedFeedback == null
    val actionAlpha by animateFloatAsState(
        targetValue = if (choiceActionsActive && scrollProgress < 0.38f) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = PremiumRevealEase),
        label = "product_detail_action_alpha",
    )
    val actionBottomPadding by animateDpAsState(
        targetValue = if (choiceActionsActive) 118.dp else 36.dp,
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
        key = "detail_${deckId}_${activeProductId}",
        durationMillis = ProductDetailEnterMs,
    )
    val backdropEnter = segmentProgress(routeProgressState.value, 0f, 0.82f)
    val contentEnter = segmentProgress(routeProgressState.value, 0.18f, 1f)
    val chromeEnter = segmentProgress(routeProgressState.value, 0.28f, 1f)

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
                latestOnDeckCompleted(deckId)
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
            enterProgress = backdropEnter,
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
            onClick = { onOpenEvidence(deckId, activeProductId) },
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
                    progress = scrollProgress,
                )
            }
        }
        if (choiceActionsAvailable && (submittedFeedback == null || actionAlpha > 0.01f)) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .alpha(actionAlpha)
                    .graphicsLayer {
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
                    .padding(horizontal = 52.dp, vertical = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
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
                text = product.displayName(),
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
        payload.reason
            .withoutInternalDebugTokens()
            .trim()
            .takeIf { it.isNotBlank() }
            ?.let { reason ->
                ImmersiveSectionTitle("BuyPilot AI 核心推荐")
                MarkdownTextBlock(
                    content = reason,
                    style = TextStyle(
                        color = BuyPilotColors.TextSecondary,
                        fontSize = BuyPilotType.Body,
                        lineHeight = 23.sp,
                    ),
                )
            }
        Spacer(Modifier.height(20.dp))
        CinematicDetailDivider(progress)
        ImmersiveSectionTitle("详细信息")
        if (payload.riskNotes.isNotEmpty()) {
            ImmersiveWarningBox(payload.riskNotes.joinToString("；"))
        }
        ProductAttributeRowsDark(product = product)
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
private fun ProductAttributeRowsDark(product: ProductPayload) {
    val rows = listOf(
        "适用对象" to product.skinTypeMatch.userFacingJoinedOrFallback(),
        "成分标签" to product.ingredientTags.userFacingJoinedOrFallback(),
        "使用场景" to product.useScenario.userFacingJoinedOrFallback(),
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
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    val payload = state.findProduct(deckId, productId)

    if (payload == null) {
        Surface(color = Color(0xFF1A1D23), modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                ProductPageTopBar(title = "推荐证据", onBack = onBack)
                ExpiredRecommendationState(onBack = onBack)
            }
        }
        return
    }

    val product = payload.product
    val evidenceItems = payload.evidence
    val highlightTags = payload.displayTags()
    val routeProgressState = rememberRouteEnterProgress(
        key = "evidence_${deckId}_${productId}",
        durationMillis = ProductEvidenceEnterMs,
    )
    val chromeEnter = segmentProgress(routeProgressState.value, 0.12f, 0.72f)
    val contentEnter = segmentProgress(routeProgressState.value, 0.18f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1D23),
                        Color(0xFF13151A),
                        Color(0xFF0F1115),
                    ),
                ),
            ),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = contentEnter
                    translationY = (1f - contentEnter) * 36f
                }
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 132.dp, bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            item("header") {
                MagazineProductHeader(product = product)
            }
            payload.reason.withoutInternalDebugTokens().trim().takeIf { it.isNotBlank() }?.let { reason ->
                item("reason") {
                    Text(
                        text = reason,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 22.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Normal,
                    )
                }
            }
            if (highlightTags.isNotEmpty()) {
                item("highlights") {
                    MagazineHighlightChips(tags = highlightTags)
                }
            }
            if (payload.riskNotes.isNotEmpty()) {
                item("risks") {
                    MagazineRiskCard(notes = payload.riskNotes)
                }
            }
            if (evidenceItems.isNotEmpty()) {
                item("evidence_header") {
                    Text(
                        text = "推荐依据",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                    )
                }
                itemsIndexed(evidenceItems, key = { index, item -> item.evidenceId ?: item.sourceId ?: "${item.sourceType}_$index" }) { _, evidence ->
                    MagazineEvidenceQuote(evidence = evidence)
                }
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 12.dp, top = 8.dp)
                .graphicsLayer {
                    alpha = chromeEnter
                    translationY = (1f - chromeEnter) * -12f
                }
                .zIndex(2f)
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = Color.White.copy(alpha = 0.9f),
            ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back_24),
                contentDescription = "返回",
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun MagazineProductHeader(product: ProductPayload) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = product.brandLabel(),
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
        )
        Text(
            text = product.displayName(),
            color = Color.White,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = product.priceLabel(),
            color = BuyPilotColors.Primary,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MagazineHighlightChips(tags: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            Text(
                text = tag,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                lineHeight = 16.sp,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun MagazineRiskCard(notes: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2A1A1A), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFFF6B6B).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "注意事项",
            color = Color(0xFFFF6B6B),
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        notes.forEach { note ->
            val displayNote = note.withoutInternalDebugTokens().trim()
            if (displayNote.isNotBlank()) {
                Text(
                    text = displayNote,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                )
            }
        }
    }
}

@Composable
private fun MagazineEvidenceQuote(evidence: EvidencePayload) {
    val snippet = evidence.snippet.withoutInternalDebugTokens().trim()
    if (snippet.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = evidence.sourceType.userFacingEvidenceSourceLabel("商品资料"),
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "“$snippet”",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 18.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Normal,
        )
        HorizontalDivider(
            thickness = 1.dp,
            color = Color.White.copy(alpha = 0.08f),
        )
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
    val displayLabel = label.withoutInternalDebugTokens().ifBlank { "信息" }
    val displayValue = value.withoutInternalDebugTokens().ifBlank { "未返回" }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = displayLabel,
            color = BuyPilotColors.TextMuted,
            fontSize = BuyPilotType.Label,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = displayValue,
            color = BuyPilotColors.TextPrimary,
            fontSize = BuyPilotType.Body,
            lineHeight = 22.sp,
        )
    }
}

@Composable
private fun EvidenceBulletText(index: Int, text: String) {
    val displayText = text.withoutInternalDebugTokens().ifBlank { return }
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
            text = displayText,
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
    val snippet = evidence.snippet.withoutInternalDebugTokens().trim()
    if (snippet.isBlank()) return
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
                text = evidence.sourceType.userFacingEvidenceSourceLabel("商品资料"),
                color = BuyPilotColors.TextMuted,
                fontSize = BuyPilotType.Label,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            evidence.trustLabel?.withoutInternalDebugTokens()?.takeIf { it.isNotBlank() }?.let { label ->
                Text(
                    text = label,
                    color = BuyPilotColors.TextMuted,
                    fontSize = BuyPilotType.Label,
                    lineHeight = 16.sp,
                )
            }
        }
        Text(
            text = "“$snippet”",
            color = BuyPilotColors.TextPrimary,
            fontSize = 28.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Medium,
        )
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
                    text = payload.product.displayName(),
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
private fun ProductAttributeRows(product: ProductPayload) {
    val rows = listOf(
        "适用对象" to product.skinTypeMatch.userFacingJoinedOrFallback(),
        "成分标签" to product.ingredientTags.userFacingJoinedOrFallback(),
        "使用场景" to product.useScenario.userFacingJoinedOrFallback(),
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
    payload.cart?.let { cart ->
        val actionLabel = if (payload.status == "failed") {
            when (payload.action) {
                "add" -> "加购失败"
                "remove" -> "移出失败"
                "update_quantity" -> "更新失败"
                else -> "购物车操作失败"
            }
        } else {
            when (payload.action) {
                "add" -> "已加入购物车"
                "remove" -> "已移出购物车"
                "update_quantity" -> "已更新数量"
                "view" -> "购物车"
                else -> "购物车已更新"
            }
        }
        val itemSummary = if (cart.items.isEmpty()) {
            "购物车为空"
        } else {
            cart.items.take(2).joinToString(" · ") { item ->
                val itemName = item.name
                    .withoutInternalDebugTokens()
                    .takeIf { it.isNotBlank() }
                    ?: "商品"
                "$itemName x${item.quantity}"
            }
        }
        val moreSummary = if (cart.items.size > 2) "等${cart.items.size}种商品" else null
        val totalSummary = if (cart.totalItems > 0) "共${cart.totalItems}件 · ¥${cart.totalPrice.clean()}" else null
        InlineSystemNotice(
            listOfNotNull(actionLabel, itemSummary, moreSummary, totalSummary)
                .filter { it.isNotBlank() }
                .joinToString(" · "),
        )
        return
    }
    val fallback = when (payload.status) {
        "failed" -> "购物车操作失败"
        else -> "购物车状态已更新"
    }
    InlineSystemNotice(fallback)
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
            reason = message.userFacingErrorReason("当前连接不稳定，商品证据或模型回复没有完整返回。"),
            action = "可以直接重试，或先编辑问题让条件更短。",
        )
        "MODEL" in normalized || "LLM" in normalized || "GENERATION" in normalized -> ErrorPresentation(
            title = "模型生成失败",
            reason = message.userFacingErrorReason("模型这轮没有生成出可用答案。"),
            action = "保留了最近的问题，可以重新生成。",
        )
        "EVIDENCE" in normalized || "INSUFFICIENT" in normalized -> ErrorPresentation(
            title = "证据不足",
            reason = message.userFacingErrorReason("当前商品资料不足以支撑可靠推荐。"),
            action = "建议编辑问题，补充预算、肤质或排除项。",
        )
        "IMAGE" in normalized || "VISION" in normalized -> ErrorPresentation(
            title = "图片识别失败",
            reason = message.userFacingErrorReason("图片主体可能不够清晰，暂时无法稳定识别商品。"),
            action = "可以换一张更清晰的图，或改用文字描述。",
        )
        else -> ErrorPresentation(
            title = "这轮回复中断了",
            reason = message.userFacingErrorReason("系统没有拿到完整结果。"),
            action = "你可以重试，或编辑最近问题后重新发送。",
        )
    }
}

private val TechnicalErrorMarkerRegex = Regex(
    """(?i)\b(?:exception|traceback|timeout|failed|failure|network|http|ssl|socket|connect|connection|json|cancelled|canceled|unable|unknown host|econnreset)\b""",
)
private val TechnicalErrorCodeRegex = Regex("""\b[A-Z][A-Z0-9_]{2,}\b""")

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
    isTextRevealing: Boolean,
    awaitingCriteriaAdjustment: Boolean,
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
    val placeholder = when {
        inputState == ChatInputState.Clarifying -> "请回答上面的问题"
        inputState == ChatInputState.Streaming -> "正在生成，可随时停止"
        inputState == ChatInputState.Error -> "输入后重试"
        isTextRevealing -> "正在显示回复，可继续查看"
        awaitingCriteriaAdjustment -> "放宽预算、换品类或继续描述需求..."
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
                modifier = Modifier.size(if (streaming) 16.dp else 21.dp),
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
private fun DecisionEvidenceSheet(
    payload: FinalDecisionPayload,
    productsById: Map<String, ProductCardPayload>,
    productDeckIdByProductId: Map<String, String>,
    onProductDetailOpen: (String, String) -> Unit,
) {
    val whyItems = payload.why.map { it.withoutMarkdownMarkup().withoutInternalDebugTokens().trim() }.filter { it.isNotBlank() }
    val notForItems = payload.notFor.map { it.withoutMarkdownMarkup().withoutInternalDebugTokens().trim() }.filter { it.isNotBlank() }
    val alternatives = payload.alternatives.filter {
        it.name.withoutInternalDebugTokens().isNotBlank()
    }
    val nextActions = payload.nextActions.filter { it.label.isNotBlank() }

    SheetContentColumn(expandToMaxHeight = false) {
        SheetTitle("决策依据")

        if (payload.summary.isNotBlank()) {
            DecisionEvidenceSummary(
                summary = payload.summary,
                winnerProductId = payload.winnerProductId,
                productsById = productsById,
                productDeckIdByProductId = productDeckIdByProductId,
                onProductDetailOpen = onProductDetailOpen,
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
            DecisionAlternativesSection(
                alternatives = alternatives,
                productDeckIdByProductId = productDeckIdByProductId,
                onProductDetailOpen = onProductDetailOpen,
            )
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
    productsById: Map<String, ProductCardPayload>,
    productDeckIdByProductId: Map<String, String>,
    onProductDetailOpen: (String, String) -> Unit,
) {
    val cleanWinnerProductId = winnerProductId?.takeIf { it.isNotBlank() }
    val winnerDeckId = cleanWinnerProductId?.let { productDeckIdByProductId[it] }
    val winnerName = cleanWinnerProductId
        ?.let { productsById[it]?.product?.displayName("") }
        ?.takeIf { it.isNotBlank() }
    val openWinnerDetail = winnerDeckId?.let { deckId ->
        { onProductDetailOpen(deckId, cleanWinnerProductId.orEmpty()) }
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
    onProductDetailOpen: (String, String) -> Unit,
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
    onProductDetailOpen: (String, String) -> Unit,
) {
    val cleanProductId = productId.takeIf { it.isNotBlank() }
    val deckId = cleanProductId?.let { productDeckIdByProductId[it] }
    val openDetail = deckId?.let { resolvedDeckId ->
        { onProductDetailOpen(resolvedDeckId, cleanProductId.orEmpty()) }
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
    modifier: Modifier = Modifier,
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
        modifier = modifier,
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
            }) { _, action ->
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
    parentProgress: Float = 1f,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.take(4).forEachIndexed { index, item ->
            DecisionReasonItem(
                text = item,
                colorIndex = index,
                revealProgress = segmentProgress(
                    value = parentProgress,
                    start = (DecisionReasonBaseDelayMs + index * DecisionReasonStaggerMs).toFloat() / DecisionCardEnterMs,
                    end = (DecisionReasonBaseDelayMs + index * DecisionReasonStaggerMs + 280).toFloat() / DecisionCardEnterMs,
                ),
            )
        }
    }
}

@Composable
private fun DecisionReasonItem(
    text: String,
    colorIndex: Int,
    revealProgress: Float = 1f,
) {
    val colors = DecisionReasonChipColors[colorIndex % DecisionReasonChipColors.size]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = revealProgress
                translationY = (1f - revealProgress) * 16f
            }
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
private fun ProductMockImage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.product_image_placeholder),
            contentDescription = "Product image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun ProductImage(
    product: ProductPayload,
    backendBaseUrl: String,
    modifier: Modifier = Modifier,
    decodeSizePx: Int? = null,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val context = LocalContext.current
    val imageUrl = product.imageUrl.resolveProductImageUrl(backendBaseUrl)
    if (imageUrl == null) {
        ProductMockImage(modifier)
        return
    }
    val model = remember(imageUrl, decodeSizePx) {
        if (decodeSizePx == null) {
            imageUrl
        } else {
            ImageRequest.Builder(context)
                .data(imageUrl)
                .size(decodeSizePx)
                .crossfade(false)
                .build()
        }
    }
    AsyncImage(
        model = model,
        contentDescription = product.displayName("商品图片"),
        modifier = modifier,
        contentScale = contentScale,
        error = painterResource(R.drawable.product_image_placeholder),
        fallback = painterResource(R.drawable.product_image_placeholder),
        placeholder = painterResource(R.drawable.product_image_placeholder),
    )
}

private fun ChatUiState.findProductDeck(deckId: String): ProductDeckNode? =
    nodes.filterIsInstance<ProductDeckNode>().firstOrNull { it.deckId == deckId }

private fun ChatUiState.findProduct(deckId: String, productId: String): ProductCardPayload? =
    findProductDeck(deckId)?.products?.firstOrNull { it.product.productId == productId }

private fun List<ProductDeckNode>.productsByProductId(): Map<String, ProductCardPayload> =
    flatMap { it.products }
        .filter { it.product.productId.isNotBlank() }
        .associateBy { it.product.productId }

private fun List<ProductDeckNode>.productDeckIdByProductId(): Map<String, String> =
    flatMap { deck ->
        deck.products.mapNotNull { payload ->
            payload.product.productId
                .takeIf { it.isNotBlank() }
                ?.let { productId -> productId to deck.deckId }
        }
    }.toMap()

private fun ProductSwipeState?.signalSummary(): ProductDeckSignalSummary {
    val state = this ?: return ProductDeckSignalSummary(viewed = 0, liked = 0, dismissed = 0)
    val likedIds = state.undoStack
        .filter { it.feedbackType == "like" || it.action == "like" }
        .map { it.productId }
        .distinct()
    val dismissedIds = state.undoStack
        .filter { it.feedbackType == "not_interested" || it.action == "not_interested" }
        .map { it.productId }
        .distinct()
    return ProductDeckSignalSummary(
        viewed = state.viewedProductIds.distinct().size,
        liked = likedIds.size,
        dismissed = dismissedIds.size,
    )
}

internal fun preferredProductCarouselPage(
    products: List<ProductCardPayload>,
    swipeState: ProductSwipeState?,
): Int {
    if (products.isEmpty()) return 0
    val handledProductIds = swipeState?.swipedProductIds.orEmpty().toSet()
    val preferredProductId = swipeState?.currentProductId
        ?.takeIf { id ->
            id !in handledProductIds &&
                products.any { it.product.productId == id }
        }
        ?: products.firstOrNull { it.product.productId !in handledProductIds }?.product?.productId
        ?: products.firstOrNull()?.product?.productId
    return products.indexOfFirst { it.product.productId == preferredProductId }
        .takeIf { it >= 0 }
        ?: 0
}

@Suppress("UNUSED_PARAMETER")
internal fun shouldOpenProductCardAsDetail(
    productId: String?,
    singleCandidate: Boolean,
    handledProductIds: Set<String>,
    deckConverged: Boolean,
): Boolean =
    !productId.isNullOrBlank()

fun ChatUiState.canOpenDeckForConvergence(deckId: String): Boolean =
    latestConvergeableDeckId == deckId &&
        deckId in awaitingConvergenceDeckIds &&
        !hasConvergedDecisionForDeck(deckId) &&
        findProductDeck(deckId)?.products.orEmpty().size >= 2

internal fun ChatUiState.hasConvergedDecisionForDeck(deckId: String): Boolean {
    return nodes.any { it is FinalDecisionNode && it.deckId == deckId }
}

internal fun List<ChatUiNode>.convergedProductDeckIds(): Set<String> =
    filterIsInstance<FinalDecisionNode>()
        .mapNotNullTo(mutableSetOf()) { it.deckId?.takeIf(String::isNotBlank) }
        .toSet()

private fun ProductDeckSignalSummary.displayLabel(): String? {
    if (!hasSignals) return null
    return buildList {
        if (viewed > 0) add("已看 $viewed 个")
        if (liked > 0) add("喜欢 $liked 个")
        if (dismissed > 0) add("排除 $dismissed 个")
    }.joinToString("｜")
}

@Composable
private fun FinalDecisionPayload.decisionStatusBadge(): DecisionStatusBadge? =
    when (decisionStatus) {
        "no_match" -> DecisionStatusBadge(
            label = stringResource(R.string.decision_badge_no_match),
            accent = BuyPilotColors.Warning,
            surface = Color(0xFFFFF7E8),
            showCardWhenEmpty = true,
        )
        "no_suitable_winner" -> DecisionStatusBadge(
            label = stringResource(R.string.decision_badge_no_suitable_winner),
            accent = BuyPilotColors.Warning,
            surface = Color(0xFFFFF7E8),
            showCardWhenEmpty = true,
        )
        "needs_more_signal" -> DecisionStatusBadge(
            label = stringResource(R.string.decision_badge_needs_more_signal),
            accent = BuyPilotColors.Info,
            surface = Color(0xFFEDF5FF),
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

private fun com.buypilot.core.model.CriteriaPayload.criteriaReceiptHeadline(): String {
    val categoryLabel = category.withoutMarkdownMarkup().trim()
    val productType = productTypeLabel().withoutMarkdownMarkup().trim()
    return listOf(categoryLabel, productType)
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString("  /  ")
}

private fun com.buypilot.core.model.CriteriaPayload.receiptProperties(
    labels: CriteriaLabels,
): List<CriteriaReceiptProperty> {
    val headline = criteriaReceiptHeadline()
    val skinType = skinTypeLabel().withoutMarkdownMarkup().trim()
    val budget = budgetLabel().withoutMarkdownMarkup().trim()
    val scenario = useScenarioLabel().withoutMarkdownMarkup().trim()
    val properties = buildList {
        chips.forEach { chip ->
            add(CriteriaReceiptProperty(label = labels.summary, value = chip))
        }
        if (skinType.isNotBlank()) {
            add(CriteriaReceiptProperty(label = labels.targetUser, value = skinType))
        }
        if (budget.isNotBlank()) {
            add(CriteriaReceiptProperty(label = labels.budget, value = budget))
        }
        if (scenario.isNotBlank()) {
            add(CriteriaReceiptProperty(label = labels.scenario, value = scenario))
        }
        productSpecificReceiptValues().forEach { value ->
            add(CriteriaReceiptProperty(label = labels.coreNeed, value = value))
        }
        exclusionLabels().forEach { value ->
            add(CriteriaReceiptProperty(label = labels.exclusions, value = "${labels.avoidPrefix}$value"))
        }
    }
    return properties
        .mapNotNull { it.compactReceiptPropertyOrNull(headline) }
        .distinctBy { it.value }
        .take(4)
}

private fun CriteriaReceiptProperty.compactReceiptPropertyOrNull(
    headline: String,
): CriteriaReceiptProperty? {
    val compactValue = value
        .withoutMarkdownMarkup()
        .replace(Regex("\\s+"), " ")
        .trim()
    return copy(value = compactValue).takeIf {
        compactValue.isNotBlank() &&
            compactValue.length <= 14 &&
            !headline.contains(compactValue)
    }
}

private fun com.buypilot.core.model.CriteriaPayload.productSpecificReceiptValues(): List<String> =
    buildList {
        addAll(ingredientPrefer)
        addAll(constraints?.ingredientPrefer.orEmpty())
        storage.orEmpty().ifBlank { constraints?.storage.orEmpty() }.takeIf { it.isNotBlank() }?.let { add(it) }
        screenSize.orEmpty().ifBlank { constraints?.screenSize.orEmpty() }.takeIf { it.isNotBlank() }?.let { add(it) }
        sportType.orEmpty().ifBlank { constraints?.sportType.orEmpty() }.takeIf { it.isNotBlank() }?.let { add(it) }
        season.orEmpty().ifBlank { constraints?.season.orEmpty() }.takeIf { it.isNotBlank() }?.let { add(it) }
        addAll(dietary)
        addAll(constraints?.dietary.orEmpty())
    }

private fun com.buypilot.core.model.CriteriaPayload.summaryTiles(labels: CriteriaLabels): List<CriteriaTileSpec> =
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
            label = labels.summary,
            value = summary,
            iconRes = R.drawable.ic_search_24,
            accent = Color(0xFFE0643B),
            glow = Color(0xFFFFEFE8),
            background = Color(0xFFFFFAF6),
            prominent = true,
            span = CriteriaTileSpan.Full,
        )
        addTile(
            label = labels.category,
            value = category,
            iconRes = R.drawable.ic_search_24,
            accent = Color(0xFFE0643B),
            glow = Color(0xFFFFEFE8),
            background = Color(0xFFFFFAF6),
            prominent = isEmpty(),
        )
        addTile(
            label = labels.coreNeed,
            value = productTypeLabel(),
            iconRes = R.drawable.ic_search_24,
            accent = Color(0xFFE0643B),
            glow = Color(0xFFFFEFE8),
            background = Color(0xFFFFFAF6),
            prominent = isEmpty(),
        )
        addTile(
            label = labels.targetUser,
            value = skinTypeLabel(),
            iconRes = R.drawable.ic_shield_24,
            accent = Color(0xFF4F86D8),
            glow = Color(0xFFEEF5FF),
            background = Color(0xFFF8FBFF),
            prominent = isEmpty(),
        )
        addTile(
            label = labels.budget,
            value = budgetLabel(),
            iconRes = R.drawable.ic_payments_24,
            accent = Color(0xFFB87920),
            glow = Color(0xFFFFF4DE),
            background = Color(0xFFFFFBF2),
            prominent = isEmpty(),
        )
        addTile(
            label = labels.scenario,
            value = useScenarioLabel(),
            iconRes = R.drawable.ic_history_24,
            accent = Color(0xFF8B96A5),
            glow = Color(0xFFF2F4F7),
            background = Color(0xFFFAFBFC),
            prominent = isEmpty(),
        )
        addTile(
            label = labels.exclusions,
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

private fun ProductPayload.displayName(fallback: String = "推荐商品"): String =
    name.withoutInternalDebugTokens().ifBlank { fallback }

private fun String?.priceSymbol(): String =
    when (this?.trim()?.uppercase()) {
        null, "", "CNY", "RMB", "CN¥", "￥", "¥" -> "¥"
        "USD", "$" -> "$"
        else -> this.trim()
    }

private fun ProductPayload.brandLabel(): String =
    brand?.withoutInternalDebugTokens()?.takeIf { it.isNotBlank() }
        ?: subCategory?.withoutInternalDebugTokens()?.takeIf { it.isNotBlank() }
        ?: category.withoutInternalDebugTokens().takeIf { it.isNotBlank() }
        ?: "BuyPilot 推荐"

private fun List<String>.userFacingJoinedOrFallback(fallback: String = ""): String =
    map { it.withoutMarkdownMarkup().withoutInternalDebugTokens().trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString("、")
        .ifBlank { fallback }

private fun String.userFacingEvidenceSourceLabel(fallback: String): String {
    val clean = withoutMarkdownMarkup().withoutInternalDebugTokens().trim()
    return when (clean.lowercase().replace("-", "_")) {
        "official_faq", "faq" -> "官方问答"
        "user_review", "review", "reviews" -> "用户评价"
        "marketing_description", "description", "product_description", "product_chunk", "chunk" -> "商品资料"
        "recommendation_reason", "reason" -> "推荐理由"
        else -> clean.ifBlank { fallback }
    }
}

private fun ProductCardPayload.displayTags(): List<String> =
    (product.ingredientTags + product.skinTypeMatch + product.useScenario)
        .map { it.withoutMarkdownMarkup().withoutInternalDebugTokens().trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .take(6)

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
