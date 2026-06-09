package com.buypilot.feature.chat

import com.buypilot.core.data.PersistedChatMessage
import com.buypilot.feature.chat.model.AiStreamNode
import com.buypilot.feature.chat.model.ChatUiNode
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
        .mapNotNull { message ->
            message.toRestoredNode()
        }
    val lastUserNode = restoredNodes.filterIsInstance<UserMessageNode>().lastOrNull()
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

private fun PersistedChatMessage.toRestoredNode(): ChatUiNode? {
    val cleanContent = content.trim()
    if (messageId.isBlank() || cleanContent.isBlank()) return null
    return when {
        role.equals("user", ignoreCase = true) -> UserMessageNode(
            key = messageId,
            content = cleanContent,
        )
        role.equals("assistant", ignoreCase = true) -> AiStreamNode(
            key = messageId,
            messageId = messageId,
            content = cleanContent,
            done = true,
            turnId = turnId.orEmpty(),
        )
        else -> null
    }
}
