package com.buypilot.feature.chat.state

import com.buypilot.core.model.AgentEventType
import com.buypilot.core.model.AgentPayload
import com.buypilot.core.model.AgentUiEnvelope
import com.buypilot.core.model.CartActionPayload
import com.buypilot.core.model.ClarificationPayload
import com.buypilot.core.model.CriteriaCardPayload
import com.buypilot.core.model.DonePayload
import com.buypilot.core.model.ErrorPayload
import com.buypilot.core.model.FinalDecisionPayload
import com.buypilot.core.model.ProductCardPayload
import com.buypilot.core.model.TextDeltaPayload
import com.buypilot.core.model.ThinkingPayload
import com.buypilot.feature.chat.model.AiStreamNode
import com.buypilot.feature.chat.model.CartActionNode
import com.buypilot.feature.chat.model.ChatUiNode
import com.buypilot.feature.chat.model.ClarificationNode
import com.buypilot.feature.chat.model.CriteriaNode
import com.buypilot.feature.chat.model.ErrorNode
import com.buypilot.feature.chat.model.FinalDecisionNode
import com.buypilot.feature.chat.model.PendingDecision
import com.buypilot.feature.chat.model.ProductDeckNode
import com.buypilot.feature.chat.model.ProductSwipeAction
import com.buypilot.feature.chat.model.ProductSwipeState
import com.buypilot.feature.chat.model.ThinkingNode
import com.buypilot.feature.chat.model.UserMessageNode

object ChatReducer {
    fun addUserMessage(
        state: ChatUiState,
        key: String,
        content: String,
        imageUrl: String? = null,
        fromEditResubmit: Boolean = false,
    ): ChatUiState =
        state.copy(
            nodes = state.nodes + UserMessageNode(
                key = key,
                content = content,
                imageUrl = imageUrl,
            ),
            inputState = ChatInputState.Streaming,
            isStreaming = true,
            lastError = null,
            lastUserMessage = content,
            lastUserMessageKey = key,
            lastRetryableRequest = ChatRetryRequest(
                message = content,
                imageUrl = imageUrl,
                fromEditResubmit = fromEditResubmit,
            ),
            streamingTextKey = null,
            streamingTextLength = 0,
            awaitingCriteriaAdjustment = false,
        )

    fun reduce(
        state: ChatUiState,
        envelope: AgentUiEnvelope<AgentPayload>,
    ): ChatUiState {
        val base = state.copy(
            sessionId = state.sessionId ?: envelope.sessionId,
            currentTurnId = envelope.turnId,
        )
        val contentBase = if (envelope.event.shouldClearTransientThinking(base, envelope.turnId)) {
            base.withoutThinking(envelope.turnId)
        } else {
            base
        }

        return when (envelope.event) {
            AgentEventType.Thinking -> base.upsertNode(
                ThinkingNode(
                    key = envelope.nodeId,
                    payload = envelope.payload as ThinkingPayload,
                    turnId = envelope.turnId,
                ),
            ).copy(
                inputState = ChatInputState.Streaming,
                isStreaming = true,
            )

            AgentEventType.Clarification -> reduceClarification(contentBase, envelope)

            AgentEventType.CriteriaCard -> reduceCriteriaCard(contentBase, envelope)

            AgentEventType.TextDelta -> reduceTextDelta(base, envelope)

            AgentEventType.ProductCard -> reduceProductCard(contentBase, envelope)

            AgentEventType.CartAction -> contentBase.upsertNode(
                CartActionNode(envelope.nodeId, envelope.payload as CartActionPayload),
            )

            AgentEventType.FinalDecision -> reduceFinalDecision(contentBase, envelope)

            AgentEventType.Done -> {
                val payload = envelope.payload as DonePayload
                val awaitingDeckId = payload.deckId?.takeIf { it.isNotBlank() }
                val doneBase = if (
                    payload.finishReason == "awaiting_product_feedback" &&
                    awaitingDeckId != null &&
                    contentBase.productCountForDeck(awaitingDeckId) >= 2
                ) {
                    contentBase.markDeckAwaitingConvergence(awaitingDeckId)
                } else {
                    contentBase
                }
                doneBase.copy(
                    inputState = if (payload.finishReason == "canceled" || payload.finishReason == "cancelled") {
                        ChatInputState.Canceled
                    } else if (contentBase.inputState == ChatInputState.Clarifying ||
                        contentBase.inputState == ChatInputState.Error ||
                        contentBase.inputState == ChatInputState.Canceled
                    ) {
                        contentBase.inputState
                    } else {
                        ChatInputState.Idle
                    },
                    isStreaming = false,
                    awaitingCriteriaAdjustment = payload.finishReason == "awaiting_criteria_adjustment",
                )
            }

            AgentEventType.Error -> {
                val payload = envelope.payload as ErrorPayload
                contentBase.upsertNode(
                    ErrorNode(
                        key = envelope.nodeId,
                        code = payload.code,
                        message = payload.message,
                        retryable = payload.retryable,
                    ),
                ).copy(
                    inputState = ChatInputState.Error,
                    isStreaming = false,
                    lastError = payload.message,
                )
            }

            AgentEventType.Unknown -> base
        }
    }

    fun cancel(state: ChatUiState): ChatUiState {
        val base = state.currentTurnId?.let { state.withoutThinking(it) } ?: state
        return base.copy(inputState = ChatInputState.Canceled, isStreaming = false)
    }

    fun markComposing(state: ChatUiState, hasText: Boolean, hasImage: Boolean): ChatUiState =
        state.copy(
            inputState = when {
                hasImage -> ChatInputState.ImageAttached
                hasText -> ChatInputState.Composing
                else -> ChatInputState.Idle
            },
        )

    fun selectProduct(
        state: ChatUiState,
        deckId: String,
        productId: String?,
    ): ChatUiState {
        val products = state.productIdsForDeck(deckId)
        val selectedProductId = productId
            ?.takeIf { it in products }
            ?: products.firstOrNull { it !in state.productSwipeStates[deckId].orEmpty().swipedProductIds }
            ?: products.firstOrNull()
        val swipeState = state.productSwipeStates[deckId].orEmpty()
        return state.copy(
            productSwipeStates = state.productSwipeStates + (
                deckId to swipeState.copy(currentProductId = selectedProductId)
                ),
        )
    }

    fun swipeProduct(
        state: ChatUiState,
        deckId: String,
        productId: String,
        feedbackType: String,
        action: String,
    ): ChatUiState {
        val products = state.productIdsForDeck(deckId)
        if (productId !in products) return state

        val swipeState = state.productSwipeStates[deckId].orEmpty()
        if (feedbackType == "view_detail" || action == "view_detail" || action == "open_evidence") {
            val nextSwipeState = swipeState.copy(
                currentProductId = productId,
                viewedProductIds = (swipeState.viewedProductIds + productId).distinct(),
            )
            return state.copy(productSwipeStates = state.productSwipeStates + (deckId to nextSwipeState))
        }
        val swiped = (swipeState.swipedProductIds + productId).distinct()
        val nextProductId = products.firstOrNull { it !in swiped }
            ?: products.firstOrNull { it != productId }
            ?: productId
        val nextSwipeState = swipeState.copy(
            currentProductId = nextProductId,
            viewedProductIds = (swipeState.viewedProductIds + productId).distinct(),
            swipedProductIds = swiped,
            undoStack = swipeState.undoStack + ProductSwipeAction(
                productId = productId,
                feedbackType = feedbackType,
                action = action,
            ),
        )
        return state.copy(productSwipeStates = state.productSwipeStates + (deckId to nextSwipeState))
    }

    fun undoSwipe(state: ChatUiState, deckId: String): ChatUiState {
        val swipeState = state.productSwipeStates[deckId] ?: return state
        val restored = swipeState.undoStack.lastOrNull() ?: return state
        val nextSwipeState = swipeState.copy(
            currentProductId = restored.productId,
            swipedProductIds = swipeState.swipedProductIds.filterNot { it == restored.productId },
            undoStack = swipeState.undoStack.dropLast(1),
        )
        return state.copy(productSwipeStates = state.productSwipeStates + (deckId to nextSwipeState))
    }

    fun convergeDeck(state: ChatUiState, deckId: String): ChatUiState {
        val pending = state.pendingDecisions[deckId]
        val withoutWaiting = state.copy(
            awaitingConvergenceDeckIds = state.awaitingConvergenceDeckIds - deckId,
            pendingDecisions = state.pendingDecisions - deckId,
            inputState = ChatInputState.Idle,
            isStreaming = false,
        )
        return if (pending == null) {
            withoutWaiting
        } else {
            withoutWaiting.upsertNode(FinalDecisionNode(pending.key, pending.payload))
        }
    }

    private fun reduceTextDelta(
        state: ChatUiState,
        envelope: AgentUiEnvelope<AgentPayload>,
    ): ChatUiState {
        val payload = envelope.payload as TextDeltaPayload
        val messageKey = payload.messageId.ifBlank { envelope.nodeId }
        val existing = state.nodes.filterIsInstance<AiStreamNode>()
            .firstOrNull {
                it.key == messageKey ||
                    (payload.messageId.isNotBlank() && it.messageId == payload.messageId)
            }
        val textBase = state.withoutThinking(envelope.turnId)
        val node = AiStreamNode(
            key = existing?.key ?: messageKey,
            messageId = payload.messageId,
            content = (existing?.content.orEmpty()) + payload.delta,
            done = payload.done,
            turnId = envelope.turnId,
        )
        return textBase.upsertNode(node).copy(
            inputState = ChatInputState.Streaming,
            isStreaming = true,
            lastError = null,
            lastAssistantMessageKey = node.key,
            streamingTextKey = node.key,
            streamingTextLength = node.content.length,
        )
    }

    private fun reduceCriteriaCard(
        state: ChatUiState,
        envelope: AgentUiEnvelope<AgentPayload>,
    ): ChatUiState {
        return state.upsertNode(
            CriteriaNode(envelope.nodeId, envelope.payload as CriteriaCardPayload),
        )
    }

    private fun reduceClarification(
        state: ChatUiState,
        envelope: AgentUiEnvelope<AgentPayload>,
    ): ChatUiState {
        val payload = envelope.payload as ClarificationPayload
        return state
            .upsertNode(
                ClarificationNode(
                    key = envelope.nodeId,
                    payload = payload,
                    turnId = envelope.turnId,
                ),
            )
            .copy(
                inputState = ChatInputState.Clarifying,
                isStreaming = true,
                streamingTextKey = null,
                streamingTextLength = 0,
            )
    }

    private fun reduceProductCard(
        state: ChatUiState,
        envelope: AgentUiEnvelope<AgentPayload>,
    ): ChatUiState {
        val payload = envelope.payload as ProductCardPayload
        val deckId = envelope.deckId ?: envelope.nodeId
        val existing = state.nodes.filterIsInstance<ProductDeckNode>()
            .firstOrNull { it.deckId == deckId }
        val products = (existing?.products.orEmpty()
            .filterNot { it.product.productId == payload.product.productId } + payload)
            .sortedBy { it.rank }
        val nextState = state.upsertNode(
            ProductDeckNode(
                key = deckId,
                deckId = deckId,
                products = products,
            ),
        )
        return if (products.size <= 1) nextState.clearDeckConvergence(deckId) else nextState
    }

    private fun reduceFinalDecision(
        state: ChatUiState,
        envelope: AgentUiEnvelope<AgentPayload>,
    ): ChatUiState {
        val payload = envelope.payload as FinalDecisionPayload
        val latestDeck = state.nodes.filterIsInstance<ProductDeckNode>().lastOrNull()
        val deckId = latestDeck?.deckId
        return if (
            deckId != null &&
            latestDeck.products.size >= 2 &&
            deckId in state.awaitingConvergenceDeckIds
        ) {
            state.copy(
                pendingDecisions = state.pendingDecisions + (
                    deckId to PendingDecision(
                        key = envelope.nodeId,
                        payload = payload,
                        turnId = envelope.turnId,
                    )
                    ),
            )
        } else {
            state.upsertNode(FinalDecisionNode(envelope.nodeId, payload))
        }
    }

    private fun ChatUiState.productIdsForDeck(deckId: String): List<String> =
        nodes.filterIsInstance<ProductDeckNode>()
            .firstOrNull { it.deckId == deckId }
            ?.products
            .orEmpty()
            .map { it.product.productId }
            .filter { it.isNotBlank() }

    private fun ChatUiState.productCountForDeck(deckId: String): Int =
        nodes.filterIsInstance<ProductDeckNode>()
            .firstOrNull { it.deckId == deckId }
            ?.products
            .orEmpty()
            .size

    private fun ProductSwipeState?.orEmpty(): ProductSwipeState = this ?: ProductSwipeState()

    private fun ChatUiState.markDeckAwaitingConvergence(deckId: String): ChatUiState =
        copy(awaitingConvergenceDeckIds = awaitingConvergenceDeckIds + deckId)

    private fun ChatUiState.clearDeckConvergence(deckId: String): ChatUiState =
        copy(
            awaitingConvergenceDeckIds = awaitingConvergenceDeckIds - deckId,
            pendingDecisions = pendingDecisions - deckId,
        )

    private fun ChatUiState.upsertNode(node: ChatUiNode): ChatUiState {
        val index = nodes.indexOfFirst { it.key == node.key }
        val nextNodes = if (index >= 0) {
            nodes.toMutableList().also { it[index] = node }
        } else {
            nodes + node
        }
        return copy(nodes = nextNodes)
    }

    private fun AgentEventType.shouldClearTransientThinking(state: ChatUiState, turnId: String): Boolean =
        when (this) {
            AgentEventType.Thinking,
            AgentEventType.TextDelta,
            AgentEventType.Unknown -> false
            AgentEventType.Clarification -> true
            AgentEventType.Done -> !state.hasClarificationForTurn(turnId)
            else -> true
        }

    private fun ChatUiState.hasClarificationForTurn(turnId: String): Boolean =
        turnId.isNotBlank() && nodes.any { it is ClarificationNode && it.turnId == turnId }

    private fun ChatUiState.withoutThinking(turnId: String): ChatUiState {
        val thinkingKey = "thinking_$turnId"
        val nextNodes = nodes.filterNot {
            it is ThinkingNode && (it.turnId == turnId || it.key == thinkingKey)
        }
        return if (nextNodes.size == nodes.size) {
            this
        } else {
            copy(nodes = nextNodes)
        }
    }
}
