package com.buypilot.feature.chat.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

private const val ProductDeckArrivalMs = 320
private const val ProductImageSeedVisibleUntilProgress = 0.78f
private const val ProductArrivalSeedMinVisibleProgress = 0.01f
private const val ProductArrivalSeedGoneProgress = 0.82f

/** 产品级统一动画时长：页面入场 / 卡片入场 / 卡片划走（150-320ms 区间，对齐产品规范） */
internal const val RouteEnterDurationMs = 300
internal const val CardEnterDurationMs = 260
internal const val SwipeAnimationDurationMs = 300

internal fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

internal fun segmentProgress(value: Float, start: Float, end: Float): Float =
    ((value - start) / (end - start)).coerceIn(0f, 1f)

@Composable
internal fun StructuredCardMotion(
    key: String,
    modifier: Modifier = Modifier,
    motionEnabled: Boolean,
    alreadyEntered: Boolean,
    durationMillis: Int,
    initialOffsetY: Dp,
    initialScale: Float,
    animationSpec: AnimationSpec<Float> = tween(durationMillis = durationMillis, easing = PremiumRevealEase),
    onEntered: () -> Unit,
    content: @Composable () -> Unit,
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
            animationSpec = animationSpec,
        )
        latestOnEntered()
    }

    Box(
        modifier = modifier.graphicsLayer {
            val t = progress.value
            alpha = t
            translationY = with(density) { initialOffsetY.toPx() } * (1f - t)
            scaleX = initialScale + (1f - initialScale) * t
            scaleY = initialScale + (1f - initialScale) * t
        },
    ) {
        content()
    }
}

@Composable
internal fun StaggeredRevealMotion(
    key: String,
    modifier: Modifier = Modifier,
    motionEnabled: Boolean,
    alreadyEntered: Boolean,
    delayMillis: Int,
    durationMillis: Int,
    initialOffsetY: Dp,
    animationSpec: AnimationSpec<Float> = tween(
        durationMillis = durationMillis,
        delayMillis = delayMillis,
        easing = PremiumRevealEase,
    ),
    content: @Composable () -> Unit,
) {
    val progress = remember(key) { Animatable(if (!motionEnabled || alreadyEntered) 1f else 0f) }
    val density = LocalDensity.current

    LaunchedEffect(key, motionEnabled, alreadyEntered) {
        if (!motionEnabled || alreadyEntered) {
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = animationSpec,
        )
    }

    Box(
        modifier = modifier.graphicsLayer {
            val t = progress.value
            alpha = t
            translationY = with(density) { initialOffsetY.toPx() } * (1f - t)
        },
    ) {
        content()
    }
}

@Composable
internal fun rememberProductDeckArrivalProgressProvider(
    key: String,
    motionEnabled: Boolean,
    alreadyEntered: Boolean,
    onEntered: () -> Unit,
): () -> Float {
    val progress = remember(key) { Animatable(if (!motionEnabled || alreadyEntered) 1f else 0f) }
    val latestOnEntered by rememberUpdatedState(onEntered)
    val progressProvider = remember(progress) { { progress.value } }

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

    return progressProvider
}

@Composable
internal fun ProductDeckArrivalMotion(
    arrivalProgress: () -> Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(218.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(218.dp)
                .graphicsLayer {
                    val t = arrivalProgress()
                    val enterT = segmentProgress(t, 0f, 1f)
                    alpha = 0.08f + enterT * 0.92f
                    translationY = with(density) { lerp(14f, 0f, enterT).dp.toPx() }
                    scaleX = 1f
                    scaleY = 1f
                }
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = BuyPilotColors.ShadowNeutral.copy(alpha = 0.06f),
                    spotColor = Color.Black.copy(alpha = 0.05f),
                ),
            color = BuyPilotColors.SurfaceCard,
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(
                width = 1.dp,
                color = BuyPilotColors.Border.copy(alpha = 0.72f),
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val contentT = segmentProgress(arrivalProgress(), 0.18f, 1f)
                        alpha = contentT
                        translationY = (1f - contentT) * 8f
                    },
            ) {
                content()
            }
        }
        ProductArrivalSeedBubble(
            progress = {
                val t = arrivalProgress()
                val seedT = segmentProgress(t, 0f, 0.12f)
                (1f - segmentProgress(t, 0.48f, 0.82f)) * seedT
            },
            modifier = Modifier
                .padding(start = 12.dp, top = 84.dp)
                .zIndex(2f),
        )
    }
}

@Composable
private fun ProductArrivalSeedBubble(
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    // 一次性可见窗口：动画走完后翻转 Boolean 销毁 shimmer transition；
    // 窗口内进度只在 graphicsLayer/draw 阶段读取，零重组
    var seedActive by remember { mutableStateOf(true) }
    if (!seedActive) return
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { }
            if (progress().coerceIn(0f, 1f) >= ProductArrivalSeedGoneProgress) {
                seedActive = false
                break
            }
        }
    }

    val pulse = rememberInfiniteTransition(label = "product_seed_pulse")
    val shimmer = pulse.animateFloat(
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
                val t = progress()
                val seedT = segmentProgress(t, 0f, 0.12f)
                val visibility = ((1f - segmentProgress(t, 0.48f, ProductArrivalSeedGoneProgress)) * seedT)
                    .coerceIn(0f, 1f)
                alpha = if (visibility > ProductArrivalSeedMinVisibleProgress) visibility else 0f
                scaleX = 0.88f + visibility * 0.12f
                scaleY = 0.88f + visibility * 0.12f
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
                .drawBehind {
                    drawRect(BuyPilotColors.PrimarySoft.copy(alpha = 0.28f + shimmer.value * 0.26f))
                },
        )
    }
}

@Composable
internal fun ProductImageLoadingSeed(
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    var seedActive by remember { mutableStateOf(true) }
    if (!seedActive) return
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { }
            if (!shouldRenderProductImageLoadingSeed(progress().coerceIn(0f, 1f))) {
                seedActive = false
                break
            }
        }
    }

    val pulse = rememberInfiniteTransition(label = "product_image_seed_pulse")
    val shimmer = pulse.animateFloat(
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
            .graphicsLayer {
                alpha = (1f - progress().coerceIn(0f, 1f) * 0.78f).coerceIn(0f, 1f)
            }
            .clip(RoundedCornerShape(12.dp))
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(
                        listOf(
                            BuyPilotColors.SurfaceCard.copy(alpha = 0.78f),
                            BuyPilotColors.PrimarySoft.copy(alpha = 0.2f + shimmer.value * 0.22f),
                            BuyPilotColors.SurfaceMuted.copy(alpha = 0.72f),
                        ),
                    ),
                )
            },
    )
}

internal fun shouldRenderProductImageLoadingSeed(progress: Float): Boolean =
    progress < ProductImageSeedVisibleUntilProgress

internal fun shouldRenderProductArrivalSeedBubble(progress: Float): Boolean =
    progress > ProductArrivalSeedMinVisibleProgress
