package com.buypilot.feature.chat.state

import com.buypilot.feature.chat.model.ChatUiNode
import com.buypilot.feature.chat.model.ProductSwipeState
import com.buypilot.feature.chat.model.PendingDecision

data class ChatRetryRequest(
    val message: String,
    val imageUrl: String? = null,
    val fromEditResubmit: Boolean = false,
)

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
    val pendingDecisions: Map<String, PendingDecision> = emptyMap(),
    val backendBaseUrl: String = "",
    val useMockChat: Boolean = false,
)
