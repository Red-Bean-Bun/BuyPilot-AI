package com.buypilot.feature.chat.state

import com.buypilot.feature.chat.model.ChatUiNode

data class ChatUiState(
    val sessionId: String? = null,
    val currentTurnId: String? = null,
    val nodes: List<ChatUiNode> = emptyList(),
    val inputState: ChatInputState = ChatInputState.Idle,
    val isStreaming: Boolean = false,
    val lastError: String? = null,
    val lastUserMessage: String? = null,
)
