package com.buypilot.feature.chat

import com.buypilot.core.data.PersistedChatMessage
import com.buypilot.feature.chat.model.UserMessageNode
import com.buypilot.feature.chat.state.ChatUiState

internal fun restoredSessionState(
    sessionId: String,
    messages: List<PersistedChatMessage>,
    backendBaseUrl: String,
    useMockChat: Boolean,
    ttsEnabled: Boolean,
): ChatUiState {
    val restoredNodes = messages
        .filter { it.role.equals("user", ignoreCase = true) }
        .map { message ->
            UserMessageNode(
                key = message.messageId,
                content = message.content,
            )
        }
    val lastUserNode = restoredNodes.lastOrNull()
    return ChatUiState(
        sessionId = sessionId,
        nodes = restoredNodes,
        lastUserMessage = lastUserNode?.content,
        lastUserMessageKey = lastUserNode?.key,
        backendBaseUrl = backendBaseUrl,
        useMockChat = useMockChat,
        ttsEnabled = ttsEnabled,
    )
}
