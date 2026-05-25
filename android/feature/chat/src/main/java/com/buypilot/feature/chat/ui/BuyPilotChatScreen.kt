package com.buypilot.feature.chat.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imeNestedScroll
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private val MenuEaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyPilotChatScreen(
    state: ChatUiState,
    onInputChanged: (String, Boolean) -> Unit,
    onSendMessage: (String, String?) -> Unit,
    onCancel: () -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var welcomeDismissed by rememberSaveable { mutableStateOf(false) }
    var sheetContent by remember { mutableStateOf<ChatSheetContent?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val products = state.nodes.filterIsInstance<ProductDeckNode>().flatMap { it.products }
    val focusManager = LocalFocusManager.current
    val composerFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val showWelcome = state.nodes.isEmpty() && !welcomeDismissed

    fun focusComposer() {
        welcomeDismissed = true
        showAttachmentMenu = false
        composerFocusRequester.requestFocus()
        keyboardController?.show()
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

    Box(
        modifier = Modifier
            .fillMaxSize()
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
                    .fillMaxWidth(),
            ) {
                ConversationStage(
                    showWelcome = showWelcome,
                    state = state,
                    products = products,
                    onClarificationOption = { sendAndClear(it) },
                    onCriteriaEdit = { sheetContent = ChatSheetContent.Criteria(it) },
                    onProductOpen = { sheetContent = ChatSheetContent.Product(it) },
                    onProductEvidence = { sheetContent = ChatSheetContent.ProductEvidence(it) },
                    onDecisionEvidence = { sheetContent = ChatSheetContent.DecisionEvidence(it) },
                    onQuickAction = { action -> sendAndClear(action.label) },
                    onClarificationManualInput = { focusComposer() },
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
            onSubmit = {
                if (state.isStreaming) {
                    onCancel()
                    return@BottomComposer
                }
                val next = input.trim()
                if (next.isNotEmpty()) {
                    sendAndClear(next)
                }
            },
        )
    }

    val content = sheetContent
    if (content != null) {
        ModalBottomSheet(
            onDismissRequest = { sheetContent = null },
            sheetState = sheetState,
            containerColor = BuyPilotColors.SurfaceCard,
            dragHandle = { SheetHandle() },
        ) {
            when (content) {
                is ChatSheetContent.Criteria -> CriteriaEditSheet(
                    payload = content.payload,
                    onQuickAction = {
                        sheetContent = null
                        sendAndClear(it.label)
                    },
                )
                is ChatSheetContent.Product -> ProductDetailSheet(
                    payload = content.payload,
                    onEvidence = { sheetContent = ChatSheetContent.ProductEvidence(content.payload) },
                    onAction = {
                        sheetContent = null
                        sendAndClear(it.label)
                    },
                )
                is ChatSheetContent.ProductEvidence -> ProductEvidenceSheet(content.payload)
                is ChatSheetContent.DecisionEvidence -> DecisionEvidenceSheet(content.payload)
            }
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
    onClarificationOption: (String) -> Unit,
    onClarificationManualInput: () -> Unit,
    onCriteriaEdit: (CriteriaCardPayload) -> Unit,
    onProductOpen: (ProductCardPayload) -> Unit,
    onProductEvidence: (ProductCardPayload) -> Unit,
    onDecisionEvidence: (FinalDecisionPayload) -> Unit,
    onQuickAction: (QuickActionPayload) -> Unit,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatTimeline(
    state: ChatUiState,
    products: List<ProductCardPayload>,
    onClarificationOption: (String) -> Unit,
    onClarificationManualInput: () -> Unit,
    onCriteriaEdit: (CriteriaCardPayload) -> Unit,
    onProductOpen: (ProductCardPayload) -> Unit,
    onProductEvidence: (ProductCardPayload) -> Unit,
    onDecisionEvidence: (FinalDecisionPayload) -> Unit,
    onQuickAction: (QuickActionPayload) -> Unit,
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

    LaunchedEffect(state.lastUserMessageKey) {
        val key = state.lastUserMessageKey ?: return@LaunchedEffect
        val index = state.nodes.indexOfFirst { it.key == key }
        if (index >= 0 && key != lastHandledUserMessageKey) {
            lastHandledUserMessageKey = key
            followStreamingText = true
            listState.animateScrollToItem(index = index, scrollOffset = 0)
        }
    }

    LaunchedEffect(isNearTimelineEnd, isUserDragging) {
        if (isUserDragging && !isNearTimelineEnd) {
            followStreamingText = false
        } else if (isNearTimelineEnd) {
            followStreamingText = true
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
            .fillMaxSize()
            .imeNestedScroll(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 136.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(state.nodes, key = { it.key }) { node ->
            TimelineItemMotion {
                when (node) {
                    is UserMessageNode -> UserBubble(node)
                    is ThinkingNode -> ThinkingBubble(node.payload.message.ifBlank { "正在思考中..." })
                    is AiStreamNode -> StreamingAssistantText(node.content, node.done)
                    is ClarificationNode -> ClarificationBlock(node.payload, onClarificationOption, onClarificationManualInput)
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
                InlineSystemNotice(it)
            }
        }
    }
}

@Composable
private fun TimelineItemMotion(content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
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
    ) {
        content()
    }
}

@Composable
private fun UserBubble(node: UserMessageNode) {
    val bubbleShape = RoundedCornerShape(18.dp)

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            text = node.content.ifBlank { "已发送图片" },
            color = Color.White,
            fontSize = BuyPilotType.Body,
            lineHeight = 21.sp,
            modifier = Modifier
                .widthIn(max = 304.dp)
                .shadow(4.dp, bubbleShape, ambientColor = Color.Black.copy(alpha = 0.04f))
                .background(BuyPilotColors.Primary, bubbleShape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun ThinkingBubble(message: String) {
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
    @Suppress("UNUSED_PARAMETER") done: Boolean = false,
) {
    if (content.isBlank()) return
    val plainContent = remember(content) { content.withoutMarkdownMarkup() }
    if (plainContent.isBlank()) return
    var visibleLength by remember { mutableStateOf(0) }

    LaunchedEffect(plainContent) {
        val targetLength = plainContent.length
        if (targetLength <= visibleLength) {
            visibleLength = targetLength
            return@LaunchedEffect
        }
        while (visibleLength < targetLength) {
            visibleLength = (visibleLength + StreamRevealCharsPerFrame).coerceAtMost(targetLength)
            kotlinx.coroutines.delay(StreamRevealFrameDelayMs)
        }
    }

    MarkdownTextBlock(content.takeMarkdownPlainChars(visibleLength))
}

@Composable
private fun StreamingAssistantText(
    content: String,
) {
    StreamingAssistantText(content = content, done = false)
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

@Composable
private fun ClarificationBlock(
    payload: ClarificationPayload,
    onOption: (String) -> Unit,
    onManualInput: () -> Unit,
) {
    val question = payload.question.ifBlank { "请补充一个关键信息" }
    val options = payload.suggestedOptions.ifEmpty { payload.requiredSlots }

    Column(verticalArrangement = Arrangement.spacedBy(26.dp)) {
        StreamingAssistantText(
            content = "为了能为您推荐最合适的产品，我还需要了解一下**${question.trimEnd('？', '?')}**。",
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = BuyPilotColors.SurfaceCard,
            shape = RoundedCornerShape(BuyPilotDimens.RadiusMd),
            shadowElevation = 4.dp,
            tonalElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, BuyPilotColors.Border),
        ) {
            Column(
                modifier = Modifier.padding(21.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PillLabel("需要确认")
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
                    onClick = onOption,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .heightIn(min = 40.dp)
                        .clip(CircleShape)
                        .clickable(
                            onClickLabel = "聚焦输入框",
                            role = Role.Button,
                            onClick = onManualInput,
                        )
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit_24),
                        contentDescription = null,
                        tint = BuyPilotColors.TextMuted,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "也可以直接输入补充",
                        color = BuyPilotColors.TextMuted,
                        fontSize = BuyPilotType.Label,
                        lineHeight = 16.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ClarificationOptionScroller(
    labels: List<String>,
    modifier: Modifier = Modifier,
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

    LaunchedEffect(labels) {
        if (labels.size > 4) {
            kotlinx.coroutines.delay(260L)
            listState.animateScrollBy(
                value = 34f,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            )
            listState.animateScrollBy(
                value = -34f,
                animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
            )
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 2.dp, end = 36.dp),
        ) {
            items(labels, key = { it }) { label ->
                SmallActionChip(label = label) { onClick?.invoke(label) }
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
    val tiles = listOf(
        "核心诉求" to criteria.category.ifBlank { criteria.summary.ifBlank { "待确认" } },
        "状态" to (criteria.skinType ?: criteria.constraints?.skinType ?: "按需求匹配"),
        "约束" to criteria.budgetLabel(),
        "频次" to criteria.useScenario.firstOrNull().orEmpty().ifBlank { criteria.constraints?.useScenario ?: "日常使用" },
    )
    val exclusions = criteria.ingredientAvoid.ifEmpty { criteria.constraints?.ingredientAvoid.orEmpty() }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StreamingAssistantText("已理解你的需求")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CriteriaTile(
                label = tiles[0].first,
                value = tiles[0].second,
                accent = BuyPilotColors.PrimaryDark,
                glow = BuyPilotColors.PrimaryDark.copy(alpha = 0.06f),
                modifier = Modifier.weight(1f),
            )
            CriteriaTile(
                label = tiles[1].first,
                value = tiles[1].second,
                accent = BuyPilotColors.TextSecondary,
                glow = BuyPilotColors.Info.copy(alpha = 0.06f),
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CriteriaTile(
                label = tiles[2].first,
                value = tiles[2].second,
                accent = BuyPilotColors.TextSecondary,
                glow = BuyPilotColors.Warning.copy(alpha = 0.08f),
                modifier = Modifier.weight(1f),
            )
            CriteriaTile(
                label = tiles[3].first,
                value = tiles[3].second,
                accent = BuyPilotColors.TextSecondary,
                glow = BuyPilotColors.TextSecondary.copy(alpha = 0.06f),
                modifier = Modifier.weight(1f),
            )
        }
        if (exclusions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BuyPilotColors.Danger.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .border(1.dp, BuyPilotColors.Danger.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                    .padding(13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("⊘", color = BuyPilotColors.Danger, fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("排除项", color = BuyPilotColors.Danger.copy(alpha = 0.8f), fontSize = BuyPilotType.Tiny)
                    MarkdownTextBlock(
                        content = exclusions.joinToString("、"),
                        style = TextStyle(
                            color = BuyPilotColors.TextPrimary,
                            fontSize = BuyPilotType.LargeBody,
                            lineHeight = 22.sp,
                        ),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            GhostButton(label = "修改标准", leading = "☷", onClick = onEdit)
        }
    }
}

@Composable
private fun CriteriaTile(
    label: String,
    value: String,
    accent: Color,
    glow: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1.55f)
            .shadow(3.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.04f))
            .background(BuyPilotColors.SurfaceCard, RoundedCornerShape(16.dp))
            .border(1.dp, BuyPilotColors.Border.copy(alpha = 0.38f), RoundedCornerShape(16.dp))
            .padding(17.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 16.dp, y = (-16).dp)
                .size(64.dp)
                .clip(CircleShape)
                .background(glow)
                .blur(8.dp),
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                color = accent,
                fontSize = BuyPilotType.Tiny,
                lineHeight = 15.sp,
                letterSpacing = 0.5.sp,
            )
            Text(
                text = value,
                color = BuyPilotColors.TextPrimary,
                fontSize = BuyPilotType.Body,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
                    .onFocusChanged { isFocused = it.isFocused }
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
) {
    val criteria = payload.criteria
    SheetContentColumn {
        SheetTitle("编辑购买标准")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TabPill("基础信息", active = true)
            TabPill("适用人群")
            TabPill("筛选条件")
        }
        FieldBlock("品类", criteria.category.ifBlank { "洗面奶" })
        FieldBlock("预算", criteria.budgetLabel())
        FieldBlock("适用人群", criteria.skinType ?: criteria.constraints?.skinType ?: "按需求匹配")
        FieldBlock("排除项", criteria.ingredientAvoid.ifEmpty { criteria.constraints?.ingredientAvoid.orEmpty() }.joinToString("、").ifBlank { "暂无" })
        Text("快速微调建议", color = BuyPilotColors.TextSecondary, fontSize = BuyPilotType.Label)
        ChipRows(
            labels = payload.quickActions.map { it.label }.ifEmpty { listOf("再便宜一点", "温和亲肤", "大容量", "注重品牌") },
            onClick = { label ->
                payload.quickActions.firstOrNull { it.label == label }?.let(onQuickAction)
            },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {},
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = BuyPilotColors.SurfaceMuted, contentColor = BuyPilotColors.TextPrimary),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("重置")
            }
            Button(
                onClick = {},
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = BuyPilotColors.Primary, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("保存并重新推荐")
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
private fun SheetContentColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier
            .padding(top = 10.dp, bottom = 10.dp)
            .size(width = 40.dp, height = 4.dp)
            .clip(CircleShape)
            .background(BuyPilotColors.Border),
    )
}

@Composable
private fun SheetTitle(title: String) {
    Text(
        text = title,
        color = BuyPilotColors.TextPrimary,
        fontSize = BuyPilotType.Title,
        lineHeight = 23.sp,
        fontWeight = FontWeight.Bold,
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
    leading: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .shadow(2.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.04f))
            .background(BuyPilotColors.SurfaceCard, CircleShape)
            .border(1.dp, BuyPilotColors.Border, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 17.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.let {
            Text(it, color = BuyPilotColors.TextSecondary, fontSize = BuyPilotType.Label)
            Spacer(Modifier.width(8.dp))
        }
        Text(label, color = Color(0xFF59413A), fontSize = BuyPilotType.Label)
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
private fun TabPill(label: String, active: Boolean = false) {
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
