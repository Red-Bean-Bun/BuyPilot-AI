package com.buypilot.feature.chat.ui

import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.buypilot.core.model.ProductCardPayload
import com.buypilot.feature.chat.R
import com.buypilot.feature.chat.model.ProductSwipeState
import com.buypilot.feature.chat.state.ChatUiState
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.Direction
import com.yuyakaido.android.cardstackview.RewindAnimationSetting
import com.yuyakaido.android.cardstackview.StackFrom
import com.yuyakaido.android.cardstackview.SwipeAnimationSetting
import com.yuyakaido.android.cardstackview.SwipeableMethod
import kotlin.math.roundToInt

private const val ProductSwipeDetailEnterMs = 520
private const val ProductDeckAutoCloseDelayMs = 280L
private const val ProductSwipeAnimationMs = 430

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductSwipeModeScreen(
    state: ChatUiState,
    deckId: String,
    deckNodeKey: String? = null,
    initialProductId: String?,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onOpenDetail: (String, String) -> Unit,
    onSwipe: (String, String, String, String, String?) -> Unit,
    onUndo: (String) -> Unit,
) {
    val deck = state.findProductDeck(deckId, deckNodeKey)
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

    LaunchedEffect(deckId, deckFullyHandled) {
        if (!deckFullyHandled || autoClosed) return@LaunchedEffect
        autoClosed = true
        kotlinx.coroutines.delay(ProductDeckAutoCloseDelayMs)
        latestOnBack()
    }

    Surface(color = BuyPilotColors.SurfaceBg, modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ProductSwipeTopBar(
                title = if (products.size > 1) {
                    stringResource(R.string.product_detail_title)
                } else {
                    stringResource(R.string.product_detail_single_title)
                },
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
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BuyPilotColors.SurfaceCard)
            .statusBarsPadding(),
    ) {
        M3TopAppBarRow(
            title = title,
            titleCentered = true,
            navigationIcon = R.drawable.ic_arrow_back_24,
            navigationDescription = stringResource(R.string.common_back),
            navigationTint = BuyPilotColors.PrimaryDark,
            actionIcon = R.drawable.ic_history_24,
            actionDescription = stringResource(R.string.swipe_undo_desc),
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
                text = stringResource(R.string.swipe_single_candidate_body),
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
                label = stringResource(R.string.product_view_detail),
                leadingIconRes = R.drawable.ic_article_24,
                primary = false,
                modifier = Modifier.weight(1f),
                onClick = onOpenDetail,
            )
            CandidateActionButton(
                label = stringResource(R.string.feedback_like_desc),
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
    val mascotProgressState = rememberRouteEnterProgress(
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
                    val mascotProgress = mascotProgressState.value
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
                contentDescription = stringResource(R.string.feedback_dislike_desc),
                active = false,
                enabled = hasActiveCard,
                onClick = { cardStackBridge.swipe(Direction.Left) },
            )
            Spacer(Modifier.width(22.dp))
            SwipeRoundButton(
                iconRes = R.drawable.ic_favorite_24,
                contentDescription = stringResource(R.string.feedback_like_desc),
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
    val handledProductIds = swipeState?.swipedProductIds.orEmpty()
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
    val leftOverlayLabel = stringResource(R.string.swipe_overlay_dislike)
    val rightOverlayLabel = stringResource(R.string.swipe_overlay_like)
    val reasonLabel = stringResource(R.string.product_recommend_reason)
    val targetUserLabel = stringResource(R.string.detail_label_target_user)
    val matchTagsLabel = stringResource(R.string.detail_label_match_tags)
    val scenarioLabel = stringResource(R.string.detail_label_scenario)
    val latestOnSwiped by rememberUpdatedState(onSwiped)
    val latestOnStackPositionChanged by rememberUpdatedState(onStackPositionChanged)

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
                leftOverlayLabel = leftOverlayLabel,
                rightOverlayLabel = rightOverlayLabel,
                reasonLabel = reasonLabel,
                targetUserLabel = targetUserLabel,
                matchTagsLabel = matchTagsLabel,
                scenarioLabel = scenarioLabel,
            )
            val listener = object : CardStackListener {
                override fun onCardDragging(direction: Direction, ratio: Float) = Unit
                override fun onCardSwiped(direction: Direction) {
                    val topPosition = bridge.manager?.getTopPosition() ?: return
                    val position = topPosition - 1
                    latestOnStackPositionChanged(topPosition)
                    latestOnSwiped(direction, position)
                }
                override fun onCardRewound() {
                    latestOnStackPositionChanged(bridge.manager?.getTopPosition() ?: 0)
                }
                override fun onCardCanceled() = Unit
                override fun onCardAppeared(view: View, position: Int) {
                    latestOnStackPositionChanged(position)
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
                if (changed) latestOnStackPositionChanged(0)
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
    private val leftOverlayLabel: String,
    private val rightOverlayLabel: String,
    private val reasonLabel: String,
    private val targetUserLabel: String,
    private val matchTagsLabel: String,
    private val scenarioLabel: String,
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
            text = this@ProductCardStackAdapter.reasonLabel
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
            setTextColor(android.graphics.Color.rgb(174, 49, 4))  // 深红色文字
            textSize = 12.5f
            maxLines = 2
            setLineSpacing((2 * density), 1f)
            ellipsize = android.text.TextUtils.TruncateAt.END
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 12 * density
                setColor(android.graphics.Color.rgb(255, 239, 232))  // 浅橙色背景
                setStroke((1 * density).roundToInt().coerceAtLeast(1), android.graphics.Color.rgb(255, 106, 61))  // 橙色边框
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
            targetUserLabel = targetUserLabel,
            matchTagsLabel = matchTagsLabel,
            scenarioLabel = scenarioLabel,
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
            label = leftOverlayLabel,
            color = textPrimaryColor,
            gravity = android.view.Gravity.TOP or android.view.Gravity.END,
            rotationDegrees = 8f,
        ) to overlay(
            id = com.yuyakaido.android.cardstackview.R.id.right_overlay,
            label = rightOverlayLabel,
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
            targetUserLabel: String,
            matchTagsLabel: String,
            scenarioLabel: String,
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
                targetUserLabel to product.skinTypeMatch.userFacingJoinedOrFallback(),
                matchTagsLabel to product.ingredientTags.userFacingJoinedOrFallback(),
                scenarioLabel to product.useScenario.userFacingJoinedOrFallback(),
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
