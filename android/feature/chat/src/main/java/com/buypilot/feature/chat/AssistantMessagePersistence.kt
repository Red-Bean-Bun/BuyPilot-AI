package com.buypilot.feature.chat

import com.buypilot.core.common.json.AppJson
import com.buypilot.core.model.CriteriaCardPayload
import com.buypilot.core.model.FinalDecisionPayload
import com.buypilot.core.model.ProductCardPayload
import com.buypilot.feature.chat.model.AiStreamNode
import com.buypilot.feature.chat.model.CriteriaNode
import com.buypilot.feature.chat.model.FinalDecisionNode
import com.buypilot.feature.chat.model.ProductDeckNode
import com.buypilot.feature.chat.state.ChatUiState
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

internal const val RestorableNodeTypeAssistantText = "assistant_text"
internal const val RestorableNodeTypeCriteriaCard = "criteria_card"
internal const val RestorableNodeTypeProductDeck = "product_deck"
internal const val RestorableNodeTypeFinalDecision = "final_decision"

internal data class ChatNodePersistenceSnapshot(
    val messageId: String,
    val turnId: String?,
    val nodeType: String,
    val content: String,
    val payloadJson: String? = null,
    val deckId: String? = null,
)

@Serializable
internal data class PersistedProductDeckPayload(
    val deckId: String = "",
    val products: List<ProductCardPayload> = emptyList(),
)

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

internal fun restorableChatNodesForPersistence(
    state: ChatUiState,
    turnId: String,
    persistedMessageKeys: Set<String>,
): List<ChatNodePersistenceSnapshot> {
    val targetTurnId = turnId
        .takeIf { it.isNotBlank() }
        ?: state.currentTurnId?.takeIf { it.isNotBlank() }
    val json = AppJson.instance

    return state.nodes.mapNotNull { node ->
        when (node) {
            is AiStreamNode -> node
                .takeIf { it.done && it.content.isNotBlank() && it.key !in persistedMessageKeys }
                ?.takeIf { it.matchesPersistenceTurn(targetTurnId, turnId) }
                ?.let {
                    ChatNodePersistenceSnapshot(
                        messageId = it.key,
                        turnId = it.turnId.ifBlank { targetTurnId },
                        nodeType = RestorableNodeTypeAssistantText,
                        content = it.content.trim(),
                    )
                }
            is CriteriaNode -> node
                .takeIf { it.key !in state.staleCriteriaNodeKeys }
                ?.takeIf { it.matchesPersistenceTurn(targetTurnId) }
                ?.let {
                    ChatNodePersistenceSnapshot(
                        messageId = it.key,
                        turnId = it.turnId.ifBlank { targetTurnId },
                        nodeType = RestorableNodeTypeCriteriaCard,
                        content = it.payload.criteria.summary.trim(),
                        payloadJson = json.encodeToString<CriteriaCardPayload>(it.payload),
                    )
                }
            is ProductDeckNode -> node
                .takeIf { it.products.isNotEmpty() }
                ?.takeIf { it.matchesPersistenceTurn(targetTurnId) }
                ?.let {
                    ChatNodePersistenceSnapshot(
                        messageId = it.key,
                        turnId = it.turnId.ifBlank { targetTurnId },
                        nodeType = RestorableNodeTypeProductDeck,
                        content = "已推荐 ${it.products.size} 个商品",
                        payloadJson = json.encodeToString(
                            PersistedProductDeckPayload(
                                deckId = it.deckId,
                                products = it.products,
                            ),
                        ),
                        deckId = it.deckId,
                    )
                }
            is FinalDecisionNode -> node
                .takeIf { it.matchesPersistenceTurn(targetTurnId) }
                ?.let {
                    ChatNodePersistenceSnapshot(
                        messageId = it.key,
                        turnId = it.turnId.ifBlank { targetTurnId },
                        nodeType = RestorableNodeTypeFinalDecision,
                        content = it.payload.summary.trim(),
                        payloadJson = json.encodeToString<FinalDecisionPayload>(it.payload),
                        deckId = it.deckId,
                    )
                }
            else -> null
        }
    }
}

private fun AiStreamNode.matchesPersistenceTurn(targetTurnId: String?, rawTurnId: String): Boolean =
    when {
        targetTurnId == null -> true
        turnId == targetTurnId -> true
        rawTurnId.isBlank() && turnId.isBlank() -> true
        else -> false
    }

private fun CriteriaNode.matchesPersistenceTurn(targetTurnId: String?): Boolean =
    targetTurnId == null || turnId == targetTurnId

private fun ProductDeckNode.matchesPersistenceTurn(targetTurnId: String?): Boolean =
    targetTurnId == null || turnId == targetTurnId

private fun FinalDecisionNode.matchesPersistenceTurn(targetTurnId: String?): Boolean =
    targetTurnId == null || turnId == targetTurnId
