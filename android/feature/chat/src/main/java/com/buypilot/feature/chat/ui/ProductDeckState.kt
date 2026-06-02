package com.buypilot.feature.chat.ui

import com.buypilot.core.model.ProductCardPayload
import com.buypilot.feature.chat.model.FinalDecisionNode
import com.buypilot.feature.chat.model.ProductDeckNode
import com.buypilot.feature.chat.model.ProductSwipeState
import com.buypilot.feature.chat.state.ChatUiState

internal fun ChatUiState.findProductDeck(deckId: String): ProductDeckNode? =
    nodes.filterIsInstance<ProductDeckNode>().firstOrNull { it.deckId == deckId }

internal fun ChatUiState.findProduct(deckId: String, productId: String): ProductCardPayload? =
    findProductDeck(deckId)?.products?.firstOrNull { it.product.productId == productId }

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
