package com.buypilot.feature.chat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val showWelcome = state.nodes.isEmpty() && !welcomeDismissed

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
            modifier = Modifier.align(Alignment.BottomCenter),
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

@Composable
private fun ConversationStage(
    showWelcome: Boolean,
    state: ChatUiState,
    products: List<ProductCardPayload>,
    onClarificationOption: (String) -> Unit,
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
    Box(
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
            .padding(horizontal = 24.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = (-112).dp)
                .size(220.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            BuyPilotColors.Primary.copy(alpha = 0.2f),
                            Color(0xFFFFDBD1).copy(alpha = 0.1f),
                            Color.Transparent,
                        ),
                    ),
                )
                .blur(50.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 96.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = "开启",
                color = BuyPilotColors.TextPrimary.copy(alpha = 0.86f),
                fontSize = BuyPilotType.Display,
                lineHeight = 36.sp,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(start = 56.dp),
            )
            Text(
                text = "你的购物",
                color = BuyPilotColors.Primary,
                fontSize = BuyPilotType.Hero,
                lineHeight = 48.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "新体验🌟",
                color = BuyPilotColors.TextPrimary,
                fontSize = BuyPilotType.Display,
                lineHeight = 36.sp,
                modifier = Modifier.padding(start = 104.dp, top = 12.dp),
            )
        }

        PromptSuggestions(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 2.dp, bottom = 146.dp),
        )

        Mascot(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 142.dp),
            size = 118,
            imageSize = 100,
            borderWidth = 0,
            shadowElevation = 6,
            shadowAlpha = 0.035f,
        )
    }
}

@Composable
private fun PromptSuggestions(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(204.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .border(3.dp, BuyPilotColors.Primary.copy(alpha = 0.3f), CircleShape),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "你可以这样问",
                color = BuyPilotColors.TextPrimary,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Box(
            modifier = Modifier
                .padding(start = 21.dp)
                .width(132.dp)
                .height(1.dp)
                .background(BuyPilotColors.Primary.copy(alpha = 0.2f)),
        )
        PromptSuggestionCard("油皮洁面怎么选？")
        PromptSuggestionCard("敏感肌面霜，帮我避开酒精香精")
        PromptSuggestionCard("帮我对比两款商品，选更稳的")
    }
}

@Composable
private fun PromptSuggestionCard(text: String) {
    Text(
        text = text,
        color = BuyPilotColors.TextSecondary,
        fontSize = BuyPilotType.Body,
        lineHeight = 20.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 21.dp),
    )
}

@Composable
private fun ChatTimeline(
    state: ChatUiState,
    products: List<ProductCardPayload>,
    onClarificationOption: (String) -> Unit,
    onCriteriaEdit: (CriteriaCardPayload) -> Unit,
    onProductOpen: (ProductCardPayload) -> Unit,
    onProductEvidence: (ProductCardPayload) -> Unit,
    onDecisionEvidence: (FinalDecisionPayload) -> Unit,
    onQuickAction: (QuickActionPayload) -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.nodes.size, state.isStreaming, state.lastError) {
        if (state.nodes.isNotEmpty()) {
            listState.animateScrollToItem(state.nodes.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
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
                    is AiStreamNode -> AssistantText(node.content)
                    is ClarificationNode -> ClarificationBlock(node.payload, onClarificationOption)
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Text(
            text = node.content.ifBlank { "已发送图片" },
            color = Color.White,
            fontSize = BuyPilotType.Body,
            lineHeight = 21.sp,
            modifier = Modifier
                .widthIn(max = 304.dp)
                .shadow(4.dp, RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp), ambientColor = Color.Black.copy(alpha = 0.04f))
                .background(BuyPilotColors.Primary, RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun ThinkingBubble(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BuyPilotColors.PrimarySoft.copy(alpha = 0.8f)),
            )
            Mascot(size = 24)
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = message,
                color = BuyPilotColors.TextSecondary,
                fontSize = BuyPilotType.Label,
                lineHeight = 16.sp,
            )
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .width(86.dp)
                    .height(3.dp)
                    .clip(CircleShape),
                color = BuyPilotColors.Primary,
                trackColor = BuyPilotColors.PrimarySoft,
            )
        }
    }
}

@Composable
private fun AssistantText(content: String) {
    if (content.isBlank()) return
    Text(
        text = content,
        color = BuyPilotColors.TextPrimary,
        fontSize = BuyPilotType.LargeBody,
        lineHeight = 26.sp,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ClarificationBlock(
    payload: ClarificationPayload,
    onOption: (String) -> Unit,
) {
    val question = payload.question.ifBlank { "请补充一个关键信息" }
    val options = payload.suggestedOptions.ifEmpty { payload.requiredSlots }

    Column(verticalArrangement = Arrangement.spacedBy(26.dp)) {
        Text(
            text = "为了能为您推荐最合适的产品，我还需要了解一下${question.trimEnd('？', '?')}。",
            color = BuyPilotColors.TextPrimary,
            fontSize = BuyPilotType.LargeBody,
            lineHeight = 26.sp,
        )
        Surface(
            modifier = Modifier.widthIn(max = 322.dp),
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
                Text(
                    text = question,
                    color = BuyPilotColors.PrimaryDark,
                    fontSize = BuyPilotType.Body,
                    lineHeight = 21.sp,
                )
                ChipRows(
                    labels = options.ifEmpty { listOf("油性", "干性", "混合性", "敏感性") },
                    onClick = onOption,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = "也可以直接输入补充",
                    color = BuyPilotColors.TextMuted,
                    fontSize = BuyPilotType.Label,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

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
        AssistantText("已理解你的需求")
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
                    Text(exclusions.joinToString("、"), color = BuyPilotColors.TextPrimary, fontSize = BuyPilotType.LargeBody)
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
        AssistantText(
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
                            Text(
                                text = winner?.reason ?: "匹配当前购买标准，综合风险和预算后更值得优先考虑。",
                                color = BuyPilotColors.TextSecondary,
                                fontSize = BuyPilotType.Body,
                                lineHeight = 21.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
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
    modifier: Modifier = Modifier,
    onAttachmentClick: () -> Unit,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val placeholder = when (inputState) {
        ChatInputState.Clarifying -> "请回答上面的问题"
        ChatInputState.Streaming -> "正在生成，可随时停止"
        ChatInputState.Error -> "输入后重试"
        else -> "继续追问或描述需求..."
    }
    val canSubmit = isStreaming || text.isNotBlank()
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
                .heightIn(min = 50.dp)
                .background(BuyPilotColors.SurfaceMuted, CircleShape)
                .border(1.dp, BuyPilotColors.Border, CircleShape)
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onAttachmentClick,
                modifier = Modifier.size(40.dp),
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
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
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
                enabled = true,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isStreaming) BuyPilotColors.TextPrimary else BuyPilotColors.Primary,
                    contentColor = BuyPilotColors.OnPrimary,
                    disabledContainerColor = BuyPilotColors.Primary,
                    disabledContentColor = BuyPilotColors.OnPrimary,
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
        Text(
            text = payload.reason.ifBlank { "这款商品与当前需求高度匹配，适合作为优先比较对象。" },
            color = BuyPilotColors.TextSecondary,
            fontSize = BuyPilotType.Body,
            lineHeight = 22.sp,
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
            Text(
                text = payload.summary.ifBlank { "决策详情会根据本轮商品、证据和约束生成。" },
                color = BuyPilotColors.TextSecondary,
                fontSize = BuyPilotType.Body,
                lineHeight = 22.sp,
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
                        Text(item, color = BuyPilotColors.TextPrimary, fontSize = BuyPilotType.Body, lineHeight = 22.sp)
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
            Text(
                text = "“${evidence.snippet.ifBlank { "暂无证据片段" }}”",
                color = BuyPilotColors.TextPrimary,
                fontSize = BuyPilotType.LargeBody,
                lineHeight = 26.sp,
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
        Text(
            text = "注意：$text",
            color = BuyPilotColors.TextSecondary,
            fontSize = BuyPilotType.Body,
            lineHeight = 21.sp,
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
        text = label,
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
        text = label,
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
            modifier = Modifier.size(imageSize.dp),
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
