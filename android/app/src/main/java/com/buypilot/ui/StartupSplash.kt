package com.buypilot.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.buypilot.R
import com.buypilot.navigation.AppNavGraph
import kotlinx.coroutines.delay

private val SurfaceBg = Color(0xFFF7F8FA)
private val SplashBg = Color(0xFFF84202)
private val AppEnterEase = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val SplashExitEase = CubicBezierEasing(0.3f, 0f, 1f, 1f)
private const val SplashHoldMillis = 1580L

@Composable
fun BuyPilotStartupHost(showStartupSplash: Boolean = true) {
    var showSplash by rememberSaveable { mutableStateOf(showStartupSplash) }
    var loadHome by rememberSaveable { mutableStateOf(!showStartupSplash) }
    val density = LocalDensity.current
    val appAlpha by animateFloatAsState(
        targetValue = if (showSplash) 0.92f else 1f,
        animationSpec = tween(durationMillis = 460, easing = AppEnterEase),
        label = "startup_home_alpha",
    )
    val appScale by animateFloatAsState(
        targetValue = if (showSplash) 0.988f else 1f,
        animationSpec = tween(durationMillis = 520, easing = AppEnterEase),
        label = "startup_home_scale",
    )
    val appOffset by animateDpAsState(
        targetValue = if (showSplash) 12.dp else 0.dp,
        animationSpec = tween(durationMillis = 520, easing = AppEnterEase),
        label = "startup_home_offset",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg),
    ) {
        if (loadHome) {
            AppNavGraph(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = appAlpha
                        scaleX = appScale
                        scaleY = appScale
                        translationY = with(density) { appOffset.toPx() }
                    },
            )
        }

        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut(
                animationSpec = tween(durationMillis = 260, easing = SplashExitEase),
            ) + scaleOut(
                animationSpec = tween(durationMillis = 280, easing = SplashExitEase),
                targetScale = 0.992f,
            ),
        ) {
            StartupSplash(
                onFinished = { showSplash = false },
            )
        }
    }

    LaunchedEffect(Unit) {
        delay(if (showStartupSplash) 180 else 0)
        loadHome = true
    }
}

@Composable
private fun StartupSplash(
    onFinished: () -> Unit,
) {
    BuyPilotLayeredSplash(onFinished = onFinished)
}

@Composable
fun BuyPilotLayeredSplash(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var entered by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        entered = true
        delay(SplashHoldMillis)
        onFinished()
    }

    val density = LocalDensity.current
    val transition = updateTransition(targetState = entered, label = "startup_layered_splash")
    val mascotOffsetX by transition.animateFloat(
        transitionSpec = {
            keyframes {
                durationMillis = 1160
                (-42f) at 0
                26f at 260
                (-24f) at 520
                14f at 760
                (-6f) at 960
                0f at 1160
            }
        },
        label = "startup_mascot_sway_x",
    ) { isEntered -> if (isEntered) 0f else -42f }
    val mascotOffsetY by transition.animateFloat(
        transitionSpec = {
            keyframes {
                durationMillis = 1160
                186f at 0
                126f at 260
                70f at 520
                26f at 760
                (-5f) at 960
                0f at 1160
            }
        },
        label = "startup_mascot_rise_y",
    ) { isEntered -> if (isEntered) 0f else 186f }
    val mascotRotation by transition.animateFloat(
        transitionSpec = {
            keyframes {
                durationMillis = 1180
                (-7f) at 0
                5f at 260
                (-4f) at 520
                2.4f at 760
                (-1f) at 960
                0f at 1180
            }
        },
        label = "startup_mascot_sway_rotation",
    ) { isEntered -> if (isEntered) 0f else -7f }
    val mascotScale by transition.animateFloat(
        transitionSpec = {
            keyframes {
                durationMillis = 1080
                0.985f at 0
                1.01f at 760
                1f at 1080
            }
        },
        label = "startup_mascot_scale",
    ) { isEntered -> if (isEntered) 1f else 0.985f }
    val textAlpha by transition.animateFloat(
        transitionSpec = {
            tween(durationMillis = 480, delayMillis = 120, easing = AppEnterEase)
        },
        label = "startup_text_alpha",
    ) { isEntered -> if (isEntered) 1f else 0f }
    val textOffsetX by transition.animateFloat(
        transitionSpec = {
            keyframes {
                durationMillis = 900
                (-18f) at 0
                8f at 360
                (-4f) at 610
                0f at 900
            }
        },
        label = "startup_text_sway_x",
    ) { isEntered -> if (isEntered) 0f else -18f }
    val textOffsetY by transition.animateFloat(
        transitionSpec = {
            keyframes {
                durationMillis = 900
                44f at 0
                18f at 360
                (-3f) at 680
                0f at 900
            }
        },
        label = "startup_text_rise_y",
    ) { isEntered -> if (isEntered) 0f else 44f }
    val textScale by transition.animateFloat(
        transitionSpec = {
            keyframes {
                durationMillis = 860
                0.965f at 0
                1.012f at 620
                1f at 860
            }
        },
        label = "startup_text_scale",
    ) { isEntered -> if (isEntered) 1f else 0.965f }
    val splashContentDescription = stringResource(R.string.buy_pilot_splash_content_description)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashBg)
            .semantics {
                contentDescription = splashContentDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.startup_mascot),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = with(density) { mascotOffsetX.dp.toPx() }
                    translationY = with(density) { mascotOffsetY.dp.toPx() }
                    rotationZ = mascotRotation
                    scaleX = mascotScale
                    scaleY = mascotScale
                    transformOrigin = TransformOrigin(0.84f, 0.72f)
                },
        )
        Image(
            painter = painterResource(R.drawable.startup_text),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = textAlpha
                    translationX = with(density) { textOffsetX.dp.toPx() }
                    translationY = with(density) { textOffsetY.dp.toPx() }
                    scaleX = textScale
                    scaleY = textScale
                },
        )
    }
}
