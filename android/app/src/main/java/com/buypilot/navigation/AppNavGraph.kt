package com.buypilot.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.buypilot.feature.chat.ChatRoute
import com.buypilot.feature.chat.ChatViewModel
import com.buypilot.feature.chat.ui.ProductEvidenceOverlayScreen
import com.buypilot.feature.chat.ui.ProductHeroDetailScreen
import com.buypilot.feature.chat.ui.ProductSwipeModeScreen
import androidx.compose.runtime.collectAsState
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
                ChatRoute(
                    viewModel = viewModel,
                    onOpenProductDeck = { deckId, productId ->
                        navController.navigate(Routes.productDeck(deckId, productId))
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
                ),
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Routes.ChatGraph)
                }
                val viewModel: ChatViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsState()
                val deckId = backStackEntry.decodedArg("deckId")
                val productId = backStackEntry.decodedArg("productId")

                androidx.compose.runtime.LaunchedEffect(deckId, productId) {
                    viewModel.selectProduct(deckId, productId)
                }

                ProductSwipeModeScreen(
                    state = uiState,
                    deckId = deckId,
                    initialProductId = productId,
                    onBack = { navController.popBackStack() },
                    onOpenDetail = { targetDeckId, targetProductId ->
                        navController.navigate(Routes.productDetail(targetDeckId, targetProductId))
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
                ),
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Routes.ChatGraph)
                }
                val viewModel: ChatViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsState()
                val deckId = backStackEntry.decodedArg("deckId")
                val productId = backStackEntry.decodedArg("productId")

                ProductHeroDetailScreen(
                    state = uiState,
                    deckId = deckId,
                    productId = productId,
                    onBack = { navController.popBackStack() },
                    onOpenEvidence = { targetDeckId, targetProductId ->
                        viewModel.openProductEvidence(targetDeckId, targetProductId)
                        navController.navigate(Routes.productEvidence(targetDeckId, targetProductId))
                    },
                    onSwipe = viewModel::swipeProduct,
                )
            }

            composable(
                route = Routes.ChatProductEvidence,
                arguments = listOf(
                    navArgument("deckId") { type = NavType.StringType },
                    navArgument("productId") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Routes.ChatGraph)
                }
                val viewModel: ChatViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsState()

                ProductEvidenceOverlayScreen(
                    state = uiState,
                    deckId = backStackEntry.decodedArg("deckId"),
                    productId = backStackEntry.decodedArg("productId"),
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

object Routes {
    const val ChatGraph = "chat_graph"
    const val ChatHome = "chat/home"
    const val ChatDeck = "chat/decks/{deckId}?productId={productId}"
    const val ChatProductDetail = "chat/decks/{deckId}/products/{productId}"
    const val ChatProductEvidence = "chat/decks/{deckId}/products/{productId}/evidence"

    fun productDeck(deckId: String, productId: String?): String =
        "chat/decks/${deckId.routeEncode()}?productId=${productId.orEmpty().routeEncode()}"

    fun productDetail(deckId: String, productId: String): String =
        "chat/decks/${deckId.routeEncode()}/products/${productId.routeEncode()}"

    fun productEvidence(deckId: String, productId: String): String =
        "chat/decks/${deckId.routeEncode()}/products/${productId.routeEncode()}/evidence"
}

private fun String.routeEncode(): String =
    URLEncoder.encode(this, StandardCharsets.UTF_8.toString())

private fun String.routeDecode(): String =
    URLDecoder.decode(this, StandardCharsets.UTF_8.toString())

private fun androidx.navigation.NavBackStackEntry.decodedArg(name: String): String =
    arguments?.getString(name).orEmpty().routeDecode()
