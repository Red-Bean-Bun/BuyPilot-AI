package com.buypilot.feature.chat.state

import androidx.compose.runtime.Immutable
import com.buypilot.core.model.CartItemPayload
import com.buypilot.feature.chat.model.ChatUiNode
import com.buypilot.feature.chat.model.ProductSwipeState
import com.buypilot.feature.chat.model.PendingDecision

@Immutable
data class ChatRetryRequest(
    val message: String,
    val imageUrl: String? = null,
    val fromEditResubmit: Boolean = false,
)

@Immutable
data class ChatImageAttachmentState(
    val localUri: String = "",
    val imageUrl: String? = null,
    val fileName: String = "",
    val mimeType: String = "",
    val width: Int? = null,
    val height: Int? = null,
    val isUploading: Boolean = false,
    val error: String? = null,
) {
    val hasImage: Boolean
        get() = localUri.isNotBlank() || !imageUrl.isNullOrBlank()

    val canSend: Boolean
        get() = hasImage && !isUploading && error == null && !imageUrl.isNullOrBlank()
}

@Immutable
data class ChatCartUiState(
    val items: List<CartItemPayload> = emptyList(),
    val totalItems: Int = 0,
    val totalPrice: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val updatingProductIds: Set<String> = emptySet(),
    val pendingAddProductIds: Set<String> = emptySet(),
) {
    val isEmpty: Boolean
        get() = !isLoading && items.isEmpty()
}

@Immutable
data class ChatUiState(
    val sessionId: String? = null,
    val currentTurnId: String? = null,
    val nodes: List<ChatUiNode> = emptyList(),
    val inputState: ChatInputState = ChatInputState.Idle,
    val isStreaming: Boolean = false,
    val lastError: String? = null,
    val lastUserMessage: String? = null,
    val lastUserMessageKey: String? = null,
    val lastRetryableRequest: ChatRetryRequest? = null,
    val lastAssistantMessageKey: String? = null,
    val streamingTextKey: String? = null,
    val streamingTextLength: Int = 0,
    val productSwipeStates: Map<String, ProductSwipeState> = emptyMap(),
    val awaitingConvergenceDeckIds: Set<String> = emptySet(),
    val latestConvergeableDeckId: String? = null,
    val activeConvergenceDeckId: String? = null,
    val awaitingCriteriaAdjustment: Boolean = false,
    val pendingDecisions: Map<String, PendingDecision> = emptyMap(),
    val imageAttachment: ChatImageAttachmentState = ChatImageAttachmentState(),
    val cartState: ChatCartUiState = ChatCartUiState(),
    val cartSheetRequestId: Long = 0L,
    val backendBaseUrl: String = "",
    val useMockChat: Boolean = false,
)
