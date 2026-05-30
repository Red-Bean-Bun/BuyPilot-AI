package com.buypilot.feature.chat.model

import androidx.compose.runtime.Immutable

@Immutable
data class ProductSwipeState(
    val currentProductId: String? = null,
    val viewedProductIds: List<String> = emptyList(),
    val swipedProductIds: List<String> = emptyList(),
    val undoStack: List<ProductSwipeAction> = emptyList(),
)

@Immutable
data class ProductSwipeAction(
    val productId: String,
    val feedbackType: String,
    val action: String,
)
