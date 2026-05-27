package com.buypilot.feature.chat.model

data class ProductSwipeState(
    val currentProductId: String? = null,
    val viewedProductIds: List<String> = emptyList(),
    val swipedProductIds: List<String> = emptyList(),
    val undoStack: List<ProductSwipeAction> = emptyList(),
)

data class ProductSwipeAction(
    val productId: String,
    val feedbackType: String,
    val action: String,
)
