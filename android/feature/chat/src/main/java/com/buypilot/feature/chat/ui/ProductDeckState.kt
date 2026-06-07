package com.buypilot.feature.chat.ui

import com.buypilot.core.model.ProductCardPayload
import com.buypilot.feature.chat.model.FinalDecisionNode
import com.buypilot.feature.chat.model.ProductDeckNode
import com.buypilot.feature.chat.model.ProductSwipeState
import com.buypilot.feature.chat.state.ChatUiState

internal fun ChatUiState.findProductDeck(
    deckId: String,
    deckNodeKey: String? = null,
): ProductDeckNode? {
    val cleanDeckNodeKey = deckNodeKey?.takeIf { it.isNotBlank() }
    val decks = nodes.filterIsInstance<ProductDeckNode>()
    if (cleanDeckNodeKey != null) {
        decks.firstOrNull { it.key == cleanDeckNodeKey && it.deckId == deckId }?.let { return it }
    }
    return decks.lastOrNull { it.deckId == deckId }
}

internal fun ProductDeckNode.productsByProductId(): Map<String, ProductCardPayload> =
    products
        .mapNotNull { payload ->
            payload.product.productId
                .takeIf { it.isNotBlank() }
                ?.let { productId -> productId to payload }
        }
        .toMap()

internal fun ProductDeckNode.deckIdsByProductId(): Map<String, String> =
    products
        .mapNotNull { payload ->
            payload.product.productId
                .takeIf { it.isNotBlank() }
                ?.let { productId -> productId to deckId }
        }
        .toMap()

internal fun ChatUiState.findProduct(
    deckId: String,
    productId: String,
    deckNodeKey: String? = null,
): ProductCardPayload? {
    val cleanDeckNodeKey = deckNodeKey?.takeIf { it.isNotBlank() }
    val sourceDeck = findProductDeck(deckId, cleanDeckNodeKey)
    val sourceProduct = sourceDeck?.products?.firstOrNull { it.product.productId == productId }
    if (cleanDeckNodeKey != null && sourceDeck != null) {
        return sourceProduct
    }
    return sourceProduct
        ?: nodes
            .filterIsInstance<ProductDeckNode>()
            .asReversed()
            .firstNotNullOfOrNull { deck ->
                deck.takeIf { it.deckId == deckId }
                    ?.products
                    ?.firstOrNull { it.product.productId == productId }
            }
}

internal fun ChatUiState.isProductInDeck(
    deckId: String,
    productId: String,
    deckNodeKey: String? = null,
): Boolean =
    findProductDeck(deckId, deckNodeKey)
        ?.products
        .orEmpty()
        .any { it.product.productId == productId }

internal fun preferredProductCarouselPage(
    products: List<ProductCardPayload>,
    swipeState: ProductSwipeState?,
): Int {
    if (products.isEmpty()) return 0
    val handledProductIds = swipeState?.swipedProductIds.orEmpty()
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

internal fun isSingleCandidateDeck(productCount: Int): Boolean =
    productCount == 1

@Suppress("UNUSED_PARAMETER")
internal fun shouldOpenProductCardAsDetail(
    productId: String?,
    singleCandidate: Boolean,
    handledProductIds: Set<String>,
    deckConverged: Boolean,
): Boolean =
    !productId.isNullOrBlank()

fun ChatUiState.canOpenDeckForConvergence(
    deckId: String,
    deckNodeKey: String? = null,
): Boolean {
    val targetDeck = findProductDeck(deckId, deckNodeKey) ?: return false
    val latestDeck = findProductDeck(deckId)
    val exactDeckIsLatest = deckNodeKey.isNullOrBlank() || targetDeck.key == latestDeck?.key
    return latestConvergeableDeckId == deckId &&
        deckId in awaitingConvergenceDeckIds &&
        exactDeckIsLatest &&
        !hasConvergedDecisionForDeck(deckId, targetDeck.key) &&
        targetDeck.products.size >= 2
}

internal fun ChatUiState.hasConvergedDecisionForDeck(
    deckId: String,
    deckNodeKey: String? = null,
): Boolean {
    val cleanDeckNodeKey = deckNodeKey?.takeIf { it.isNotBlank() }
    val deckIndex = if (cleanDeckNodeKey != null) {
        nodes.indexOfFirst { it is ProductDeckNode && it.deckId == deckId && it.key == cleanDeckNodeKey }
    } else {
        nodes.indexOfLast { it is ProductDeckNode && it.deckId == deckId }
    }
    if (deckIndex < 0) {
        return nodes.any { it is FinalDecisionNode && it.deckId == deckId }
    }
    val nextSameDeckIndex = nodes
        .drop(deckIndex + 1)
        .indexOfFirst { it is ProductDeckNode && it.deckId == deckId }
        .takeIf { it >= 0 }
        ?.let { deckIndex + 1 + it }
    val nodesAfterDeck = if (nextSameDeckIndex != null) {
        nodes.subList(deckIndex + 1, nextSameDeckIndex)
    } else {
        nodes.drop(deckIndex + 1)
    }
    return nodesAfterDeck.any { it is FinalDecisionNode && it.deckId == deckId }
}
