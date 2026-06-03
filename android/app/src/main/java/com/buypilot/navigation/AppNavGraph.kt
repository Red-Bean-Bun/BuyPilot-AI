package com.buypilot.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.buypilot.feature.chat.ChatRoute
import com.buypilot.feature.chat.ChatViewModel
import com.buypilot.feature.chat.ui.canOpenDeckForConvergence
import com.buypilot.feature.chat.ui.ProductEvidenceOverlayScreen
import com.buypilot.feature.chat.ui.ProductHeroDetailScreen
import com.buypilot.feature.chat.ui.ProductSwipeModeScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.ChatGraph,
        modifier = modifier,
    ) {
        navigation(
            route = Routes.ChatGraph,
            startDestination = Routes.ChatHome,
        ) {
            composable(Routes.ChatHome) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Routes.ChatGraph)
                }
                val viewModel: ChatViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                ChatRoute(
                    viewModel = viewModel,
                    onOpenProductDeck = { deckId, productId, deckNodeKey ->
                        val targetProductId = productId.orEmpty()
                        if (uiState.canOpenDeckForConvergence(deckId, deckNodeKey)) {
                            navController.navigate(Routes.productDeck(deckId, productId, deckNodeKey))
                        } else if (targetProductId.isNotBlank()) {
                            navController.navigate(Routes.productDetail(deckId, targetProductId, deckNodeKey))
                        }
                    },
                    onOpenProductDetail = { deckId, productId, deckNodeKey ->
                        navController.navigate(Routes.productDetail(deckId, productId, deckNodeKey))
                    },
                )
            }

            composable(
                route = Routes.ChatDeck,
                arguments = listOf(
                    navArgument("deckId") { type = NavType.StringType },
                    navArgument("productId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("deckNodeKey") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
                enterTransition = { productForwardEnter() },
                exitTransition = { productForwardExit() },
                popEnterTransition = { productUnderlayReturnEnter() },
                popExitTransition = { productPopExit() },
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Routes.ChatGraph)
                }
                val viewModel: ChatViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val deckId = backStackEntry.decodedArg("deckId")
                val productId = backStackEntry.decodedArg("productId")
                val deckNodeKey = backStackEntry.decodedArg("deckNodeKey")

                androidx.compose.runtime.LaunchedEffect(deckId, productId, deckNodeKey, uiState.latestConvergeableDeckId) {
                    if (!uiState.canOpenDeckForConvergence(deckId, deckNodeKey)) {
                        navController.popBackStack()
                    }
                }

                androidx.compose.runtime.LaunchedEffect(deckId, productId, deckNodeKey, uiState.latestConvergeableDeckId) {
                    if (uiState.canOpenDeckForConvergence(deckId, deckNodeKey)) {
                        viewModel.selectProduct(deckId, productId)
                    }
                }

                ProductSwipeModeScreen(
                    state = uiState,
                    deckId = deckId,
                    deckNodeKey = deckNodeKey,
                    initialProductId = productId,
                    onBack = { navController.popBackStack() },
                    onOpenDetail = { targetDeckId, targetProductId ->
                        navController.navigate(Routes.productDetail(targetDeckId, targetProductId, deckNodeKey))
                    },
                    onSwipe = viewModel::swipeProduct,
                    onUndo = viewModel::undoSwipe,
                )
            }

            composable(
                route = Routes.ChatProductDetail,
                arguments = listOf(
                    navArgument("deckId") { type = NavType.StringType },
                    navArgument("productId") { type = NavType.StringType },
                    navArgument("deckNodeKey") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
                enterTransition = { productForwardEnter() },
                exitTransition = {
                    if (targetState.destination.route == Routes.ChatProductEvidence) {
                        productUnderlayExit()
                    } else {
                        productForwardExit()
                    }
                },
                popEnterTransition = {
                    if (initialState.destination.route == Routes.ChatProductEvidence) {
                        productUnderlayReturnEnter()
                    } else {
                        productPopEnter()
                    }
                },
                popExitTransition = { productPopExit() },
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Routes.ChatGraph)
                }
                val viewModel: ChatViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val deckId = backStackEntry.decodedArg("deckId")
                val productId = backStackEntry.decodedArg("productId")
                val deckNodeKey = backStackEntry.decodedArg("deckNodeKey")

                ProductHeroDetailScreen(
                    state = uiState,
                    deckId = deckId,
                    deckNodeKey = deckNodeKey,
                    productId = productId,
                    onBack = { navController.popBackStack() },
                    onOpenEvidence = { targetDeckId, targetProductId, targetDeckNodeKey ->
                        viewModel.openProductEvidence(targetDeckId, targetProductId)
                        navController.navigate(Routes.productEvidence(targetDeckId, targetProductId, targetDeckNodeKey))
                    },
                    onSwipe = viewModel::swipeProduct,
                    onAddToCart = viewModel::addProductToCart,
                )
            }

            composable(
                route = Routes.ChatProductEvidence,
                arguments = listOf(
                    navArgument("deckId") { type = NavType.StringType },
                    navArgument("productId") { type = NavType.StringType },
                    navArgument("deckNodeKey") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
                enterTransition = { productEvidenceEnter() },
                exitTransition = { productEvidenceExit() },
                popEnterTransition = { productUnderlayReturnEnter() },
                popExitTransition = { productEvidencePopExit() },
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Routes.ChatGraph)
                }
                val viewModel: ChatViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                ProductEvidenceOverlayScreen(
                    state = uiState,
                    deckId = backStackEntry.decodedArg("deckId"),
                    productId = backStackEntry.decodedArg("productId"),
                    deckNodeKey = backStackEntry.decodedArg("deckNodeKey"),
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

object Routes {
    const val ChatGraph = "chat_graph"
    const val ChatHome = "chat/home"
    const val ChatDeck = "chat/decks/{deckId}?productId={productId}&deckNodeKey={deckNodeKey}"
    const val ChatProductDetail = "chat/decks/{deckId}/products/{productId}?deckNodeKey={deckNodeKey}"
    const val ChatProductEvidence = "chat/decks/{deckId}/products/{productId}/evidence?deckNodeKey={deckNodeKey}"

    fun productDeck(deckId: String, productId: String?, deckNodeKey: String? = null): String =
        "chat/decks/${deckId.routeEncode()}?productId=${productId.orEmpty().routeEncode()}" +
            "&deckNodeKey=${deckNodeKey.orEmpty().routeEncode()}"

    fun productDetail(deckId: String, productId: String, deckNodeKey: String? = null): String =
        "chat/decks/${deckId.routeEncode()}/products/${productId.routeEncode()}" +
            "?deckNodeKey=${deckNodeKey.orEmpty().routeEncode()}"

    fun productEvidence(deckId: String, productId: String, deckNodeKey: String? = null): String =
        "chat/decks/${deckId.routeEncode()}/products/${productId.routeEncode()}/evidence" +
            "?deckNodeKey=${deckNodeKey.orEmpty().routeEncode()}"
}

private fun String.routeEncode(): String =
    URLEncoder.encode(this, StandardCharsets.UTF_8.toString())

private fun String.routeDecode(): String =
    URLDecoder.decode(this, StandardCharsets.UTF_8.toString())

private fun androidx.navigation.NavBackStackEntry.decodedArg(name: String): String =
    arguments?.getString(name).orEmpty().routeDecode()

private const val ProductRouteEnterMs = 360
private const val ProductRouteExitMs = 220
private const val ProductEvidenceEnterMs = 340
private const val ProductEvidenceExitMs = 210
private val ProductRouteEaseOut = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val ProductRouteEaseIn = CubicBezierEasing(0.3f, 0f, 1f, 1f)

private fun productForwardEnter(): EnterTransition =
    fadeIn(
        animationSpec = tween(
            durationMillis = ProductRouteEnterMs,
            easing = ProductRouteEaseOut,
        ),
    ) +
        slideInVertically(
            animationSpec = tween(durationMillis = ProductRouteEnterMs, easing = ProductRouteEaseOut),
            initialOffsetY = { height -> height / 14 },
        ) +
        scaleIn(
            animationSpec = tween(durationMillis = ProductRouteEnterMs, easing = ProductRouteEaseOut),
            initialScale = 0.985f,
        )

private fun productForwardExit(): ExitTransition =
    fadeOut(
        animationSpec = tween(durationMillis = ProductRouteExitMs, easing = ProductRouteEaseIn),
    ) +
        scaleOut(
            animationSpec = tween(durationMillis = ProductRouteExitMs, easing = ProductRouteEaseIn),
            targetScale = 0.988f,
        )

private fun productPopEnter(): EnterTransition =
    fadeIn(
        animationSpec = tween(
            durationMillis = ProductRouteEnterMs,
            easing = ProductRouteEaseOut,
        ),
    ) +
        slideInVertically(
            animationSpec = tween(durationMillis = ProductRouteEnterMs, easing = ProductRouteEaseOut),
            initialOffsetY = { height -> -height / 18 },
        ) +
        scaleIn(
            animationSpec = tween(durationMillis = ProductRouteEnterMs, easing = ProductRouteEaseOut),
            initialScale = 0.992f,
        )

private fun productPopExit(): ExitTransition =
    fadeOut(
        animationSpec = tween(durationMillis = 170, easing = ProductRouteEaseIn),
    ) +
        slideOutVertically(
            animationSpec = tween(durationMillis = ProductRouteExitMs, easing = ProductRouteEaseIn),
            targetOffsetY = { height -> height / 10 },
        ) +
        scaleOut(
            animationSpec = tween(durationMillis = ProductRouteExitMs, easing = ProductRouteEaseIn),
            targetScale = 0.985f,
        )

private fun productUnderlayExit(): ExitTransition =
    fadeOut(
        animationSpec = tween(durationMillis = ProductEvidenceExitMs, easing = ProductRouteEaseIn),
    ) +
        scaleOut(
            animationSpec = tween(durationMillis = ProductEvidenceEnterMs, easing = ProductRouteEaseIn),
            targetScale = 0.99f,
        )

private fun productUnderlayReturnEnter(): EnterTransition =
    fadeIn(
        animationSpec = tween(
            durationMillis = ProductEvidenceEnterMs,
            easing = ProductRouteEaseOut,
        ),
    ) +
        scaleIn(
            animationSpec = tween(durationMillis = ProductEvidenceEnterMs, easing = ProductRouteEaseOut),
            initialScale = 0.985f,
        )

private fun productEvidenceEnter(): EnterTransition =
    fadeIn(
        animationSpec = tween(
            durationMillis = ProductEvidenceEnterMs,
            easing = ProductRouteEaseOut,
        ),
    ) +
        slideInVertically(
        animationSpec = tween(durationMillis = ProductEvidenceEnterMs, easing = ProductRouteEaseOut),
            initialOffsetY = { height -> height / 9 },
        ) +
        scaleIn(
            animationSpec = tween(durationMillis = ProductEvidenceEnterMs, easing = ProductRouteEaseOut),
            initialScale = 0.992f,
        )

private fun productEvidenceExit(): ExitTransition =
    fadeOut(
        animationSpec = tween(durationMillis = ProductEvidenceExitMs, easing = ProductRouteEaseIn),
    ) +
        scaleOut(
            animationSpec = tween(durationMillis = ProductEvidenceExitMs, easing = ProductRouteEaseIn),
            targetScale = 0.985f,
        )

private fun productEvidencePopExit(): ExitTransition =
    fadeOut(
        animationSpec = tween(durationMillis = ProductEvidenceExitMs, easing = ProductRouteEaseIn),
    ) +
        slideOutVertically(
            animationSpec = tween(durationMillis = ProductEvidenceExitMs, easing = ProductRouteEaseIn),
            targetOffsetY = { height -> height / 8 },
        ) +
        scaleOut(
            animationSpec = tween(durationMillis = ProductEvidenceExitMs, easing = ProductRouteEaseIn),
            targetScale = 0.985f,
        )
