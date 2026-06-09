package com.buypilot.feature.chat

import com.buypilot.feature.chat.model.AiStreamNode
import com.buypilot.feature.chat.state.ChatUiState

internal fun completedAssistantMessagesForPersistence(
    state: ChatUiState,
    turnId: String,
    persistedMessageKeys: Set<String>,
): List<AiStreamNode> {
    val targetTurnId = turnId
        .takeIf { it.isNotBlank() }
        ?: state.currentTurnId?.takeIf { it.isNotBlank() }

    return state.nodes
        .filterIsInstance<AiStreamNode>()
        .filter { node ->
            node.done &&
                node.content.isNotBlank() &&
                node.key !in persistedMessageKeys &&
                when {
                    targetTurnId == null -> true
                    node.turnId == targetTurnId -> true
                    turnId.isBlank() && node.turnId.isBlank() -> true
                    else -> false
                }
        }
}
