package com.buypilot.feature.chat.presentation

import androidx.compose.runtime.Immutable
import com.buypilot.core.model.FinalDecisionPayload
import com.buypilot.core.model.ProductCardPayload
import com.buypilot.feature.chat.model.AiStreamNode
import com.buypilot.feature.chat.model.CartActionNode
import com.buypilot.feature.chat.model.ChatUiNode
import com.buypilot.feature.chat.model.ClarificationNode
import com.buypilot.feature.chat.model.CriteriaNode
import com.buypilot.feature.chat.model.ErrorNode
import com.buypilot.feature.chat.model.FinalDecisionNode
import com.buypilot.feature.chat.model.PendingDecision
import com.buypilot.feature.chat.model.ProductDeckNode
import com.buypilot.feature.chat.model.ProductSwipeState
import com.buypilot.feature.chat.model.ThinkingNode
import com.buypilot.feature.chat.model.UserMessageNode
import com.buypilot.feature.chat.state.ChatCartUiState
import com.buypilot.feature.chat.state.ChatUiState

@Immutable
internal data class TimelinePresentationState(
    val items: List<TimelineRenderItem> = emptyList(),
    val renderContext: TimelineRenderContext = TimelineRenderContext(),
    val productDeckNodes: List<ProductDeckNode> = emptyList(),
    val productsById: Map<String, ProductCardPayload> = emptyMap(),
    val productDeckIdByProductId: Map<String, String> = emptyMap(),
    val latestProductDeckKey: String? = null,
    val latestFinalDecisionKey: String? = null,
    val latestFinalDecisionTurnId: String? = null,
    val latestFinalDecisionDeckId: String? = null,
    val finalDecisionKeys: Set<String> = emptySet(),
    val clarificationKeys: List<String> = emptyList(),
    val convergedDeckIds: Set<String> = emptySet(),
    val revealKeys: Set<String> = emptySet(),
    val hasTimelineError: Boolean = false,
    val hasStructuredContent: Boolean = false,
    val hasNodes: Boolean = false,
    val latestFinalDecisionPayload: FinalDecisionPayload? = null,
)

@Immutable
internal data class TimelineRenderContext(
    val backendBaseUrl: String = "",
    val isStreaming: Boolean = false,
    val currentTurnId: String? = null,
    val cartState: ChatCartUiState = ChatCartUiState(),
    val productsById: Map<String, ProductCardPayload> = emptyMap(),
    val productDeckIdByProductId: Map<String, String> = emptyMap(),
    val latestProductDeckKey: String? = null,
    val productSwipeStates: Map<String, ProductSwipeState> = emptyMap(),
    val awaitingConvergenceDeckIds: Set<String> = emptySet(),
    val latestConvergeableDeckId: String? = null,
    val pendingDecisions: Map<String, PendingDecision> = emptyMap(),
    val convergedDeckIds: Set<String> = emptySet(),
    val lastUserMessage: String? = null,
)

@Immutable
internal sealed interface TimelineRenderItem {
    val key: String
}

internal val TimelineRenderItem.timelineContentType: String
    get() = when (this) {
        is UserTimelineItem -> "user_message"
        is AssistantTurnTimelineItem -> "assistant_turn"
        is StandaloneTimelineItem -> when (node) {
            is ThinkingNode -> "standalone_thinking"
            is AiStreamNode -> "standalone_text"
            is ClarificationNode -> "standalone_clarification"
            is CriteriaNode -> "standalone_criteria"
            is ProductDeckNode -> "standalone_products"
            is FinalDecisionNode -> "standalone_decision"
            is CartActionNode -> "standalone_cart"
            is ErrorNode -> "standalone_error"
            is UserMessageNode -> "standalone_user"
        }
    }

@Immutable
internal data class UserTimelineItem(
    val node: UserMessageNode,
) : TimelineRenderItem {
    override val key: String = node.key
}

@Immutable
internal data class AssistantTurnTimelineItem(
    val turnId: String,
    val nodes: List<ChatUiNode>,
    val segmentIndex: Int,
) : TimelineRenderItem {
    override val key: String = "assistant_${turnId.ifBlank { "unknown" }}_$segmentIndex"
}

@Immutable
internal data class StandaloneTimelineItem(
    val node: ChatUiNode,
) : TimelineRenderItem {
    override val key: String = node.key
}

internal fun List<ChatUiNode>.toTimelineRenderItems(): List<TimelineRenderItem> {
    return toTimelinePresentationState().items
}

internal fun List<ChatUiNode>.toTimelinePresentationState(): TimelinePresentationState {
    return ChatUiState(nodes = this).toTimelinePresentationState()
}

internal fun ChatUiState.toTimelinePresentationState(): TimelinePresentationState {
    val items = mutableListOf<TimelineRenderItem>()
    val productDeckNodes = mutableListOf<ProductDeckNode>()
    val productsById = linkedMapOf<String, ProductCardPayload>()
    val productDeckIdByProductId = linkedMapOf<String, String>()
    val finalDecisionKeys = linkedSetOf<String>()
    val clarificationKeys = mutableListOf<String>()
    val convergedDeckIds = linkedSetOf<String>()
    val revealKeys = linkedSetOf<String>()
    var assistantTurnId: String? = null
    val assistantNodes = mutableListOf<ChatUiNode>()
    var assistantSegmentIndex = 0
    var latestProductDeckKey: String? = null
    var latestFinalDecisionKey: String? = null
    var latestFinalDecisionTurnId: String? = null
    var latestFinalDecisionDeckId: String? = null
    var latestFinalDecisionPayload: FinalDecisionPayload? = null
    var hasTimelineError = false
    var hasStructuredContent = false

    fun flushAssistantTurn() {
        if (assistantNodes.isNotEmpty()) {
            items += AssistantTurnTimelineItem(
                turnId = assistantTurnId.orEmpty(),
                nodes = assistantNodes.toList(),
                segmentIndex = assistantSegmentIndex,
            )
            assistantSegmentIndex += 1
            assistantNodes.clear()
            assistantTurnId = null
        }
    }

    for (node in nodes) {
        revealKeys += node.key
        node.revealTextKey()?.let { revealKeys += it }
        when (node) {
            is ProductDeckNode -> {
                hasStructuredContent = true
                productDeckNodes += node
                latestProductDeckKey = node.key
                node.products.forEach { payload ->
                    val productId = payload.product.productId.takeIf { it.isNotBlank() }
                    if (productId != null) {
                        productsById[productId] = payload
                        productDeckIdByProductId[productId] = node.deckId
                    }
                }
            }
            is FinalDecisionNode -> {
                hasStructuredContent = true
                latestFinalDecisionKey = node.key
                latestFinalDecisionTurnId = node.turnId
                latestFinalDecisionDeckId = node.deckId
                latestFinalDecisionPayload = node.payload
                finalDecisionKeys += node.key
                node.deckId?.takeIf { it.isNotBlank() }?.let { convergedDeckIds += it }
            }
            is CriteriaNode -> {
                hasStructuredContent = true
            }
            is ClarificationNode -> {
                hasStructuredContent = true
                clarificationKeys += node.key
            }
            is ErrorNode -> {
                hasTimelineError = true
            }
            else -> Unit
        }

        val turnId = node.assistantTurnId()
        if (turnId != null) {
            if (assistantTurnId != null && assistantTurnId != turnId) {
                flushAssistantTurn()
            }
            assistantTurnId = turnId
            assistantNodes += node
        } else {
            flushAssistantTurn()
            items += when (node) {
                is UserMessageNode -> UserTimelineItem(node)
                else -> StandaloneTimelineItem(node)
            }
        }
    }
    flushAssistantTurn()
    return TimelinePresentationState(
        items = items,
        renderContext = TimelineRenderContext(
            backendBaseUrl = backendBaseUrl,
            isStreaming = isStreaming,
            currentTurnId = currentTurnId,
            cartState = cartState,
            productsById = productsById,
            productDeckIdByProductId = productDeckIdByProductId,
            latestProductDeckKey = latestProductDeckKey,
            productSwipeStates = productSwipeStates,
            awaitingConvergenceDeckIds = awaitingConvergenceDeckIds,
            latestConvergeableDeckId = latestConvergeableDeckId,
            pendingDecisions = pendingDecisions,
            convergedDeckIds = convergedDeckIds,
            lastUserMessage = lastUserMessage,
        ),
        productDeckNodes = productDeckNodes,
        productsById = productsById,
        productDeckIdByProductId = productDeckIdByProductId,
        latestProductDeckKey = latestProductDeckKey,
        latestFinalDecisionKey = latestFinalDecisionKey,
        latestFinalDecisionTurnId = latestFinalDecisionTurnId,
        latestFinalDecisionDeckId = latestFinalDecisionDeckId,
        finalDecisionKeys = finalDecisionKeys,
        clarificationKeys = clarificationKeys,
        convergedDeckIds = convergedDeckIds,
        revealKeys = revealKeys,
        hasTimelineError = hasTimelineError,
        hasStructuredContent = hasStructuredContent,
        hasNodes = nodes.isNotEmpty(),
        latestFinalDecisionPayload = latestFinalDecisionPayload,
    )
}

private fun ChatUiNode.assistantTurnId(): String? =
    when (this) {
        is ThinkingNode -> turnId
        is AiStreamNode -> turnId
        is ClarificationNode -> turnId
        is CriteriaNode -> turnId
        is ProductDeckNode -> turnId
        is FinalDecisionNode -> turnId
        else -> null
    }?.takeIf { it.isNotBlank() }

internal fun ChatUiNode.revealTextKey(): String? =
    when (this) {
        is AiStreamNode -> key
        is ClarificationNode -> clarificationQuestionRevealKey()
        else -> null
    }

internal fun TimelineRenderItem.containsNodeKey(nodeKey: String): Boolean =
    when (this) {
        is UserTimelineItem -> node.key == nodeKey
        is StandaloneTimelineItem -> node.key == nodeKey || node.revealTextKey() == nodeKey
        is AssistantTurnTimelineItem -> nodes.any { node ->
            node.key == nodeKey || node.revealTextKey() == nodeKey
        }
    }

internal fun ClarificationNode.clarificationQuestionRevealKey(): String =
    "${key}_question"

internal fun List<TimelineRenderItem>.lastContentIndex(
    state: ChatUiState,
    hasTimelineError: Boolean,
): Int = lastIndex + if (state.lastError != null && !hasTimelineError) 1 else 0
