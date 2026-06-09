package com.buypilot.feature.chat

import com.buypilot.core.common.json.AppJson
import com.buypilot.core.data.PersistedChatMessage
import com.buypilot.core.model.CriteriaCardPayload
import com.buypilot.core.model.FinalDecisionPayload
import com.buypilot.feature.chat.model.AiStreamNode
import com.buypilot.feature.chat.model.ChatUiNode
import com.buypilot.feature.chat.model.CriteriaNode
import com.buypilot.feature.chat.model.FinalDecisionNode
import com.buypilot.feature.chat.model.ProductDeckNode
import com.buypilot.feature.chat.model.UserMessageNode
import com.buypilot.feature.chat.state.ChatUiState
import kotlinx.serialization.decodeFromString

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
    if (messageId.isBlank()) return null
    return when {
        role.equals("user", ignoreCase = true) && cleanContent.isNotBlank() ->
            UserMessageNode(
                key = messageId,
                content = cleanContent,
            )
        nodeType == RestorableNodeTypeCriteriaCard -> restoreCriteriaNode()
        nodeType == RestorableNodeTypeProductDeck -> restoreProductDeckNode()
        nodeType == RestorableNodeTypeFinalDecision -> restoreFinalDecisionNode()
        role.equals("assistant", ignoreCase = true) -> restoreAssistantTextNode()
        else -> null
    }
}

private fun PersistedChatMessage.restoreCriteriaNode(): ChatUiNode? {
    val payload = decodePayload<CriteriaCardPayload>() ?: return restoreAssistantTextNode()
    return CriteriaNode(
        key = messageId,
        payload = payload,
        turnId = turnId.orEmpty(),
    )
}

private fun PersistedChatMessage.restoreProductDeckNode(): ChatUiNode? {
    val payload = decodePayload<PersistedProductDeckPayload>() ?: return restoreAssistantTextNode()
    val restoredDeckId = deckId
        ?.takeIf { it.isNotBlank() }
        ?: payload.deckId.takeIf { it.isNotBlank() }
        ?: messageId
    if (payload.products.isEmpty()) return restoreAssistantTextNode()
    return ProductDeckNode(
        key = messageId,
        deckId = restoredDeckId,
        products = payload.products,
        turnId = turnId.orEmpty(),
    )
}

private fun PersistedChatMessage.restoreFinalDecisionNode(): ChatUiNode? {
    val payload = decodePayload<FinalDecisionPayload>() ?: return restoreAssistantTextNode()
    return FinalDecisionNode(
        key = messageId,
        payload = payload,
        turnId = turnId.orEmpty(),
        deckId = deckId?.takeIf { it.isNotBlank() },
    )
}

private inline fun <reified T> PersistedChatMessage.decodePayload(): T? {
    val json = payloadJson?.takeIf { it.isNotBlank() } ?: return null
    return runCatching { AppJson.instance.decodeFromString<T>(json) }.getOrNull()
}

private fun PersistedChatMessage.restoreAssistantTextNode(): AiStreamNode? {
    val cleanContent = content.trim()
    if (cleanContent.isBlank()) return null
    return AiStreamNode(
        key = messageId,
        messageId = messageId,
        content = cleanContent,
        done = true,
        turnId = turnId.orEmpty(),
    )
}
