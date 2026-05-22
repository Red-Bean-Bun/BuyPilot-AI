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
import com.buypilot.feature.chat.model.ProductDeckNode
import com.buypilot.feature.chat.model.ThinkingNode
import com.buypilot.feature.chat.model.UserMessageNode

object ChatReducer {
    fun addUserMessage(
        state: ChatUiState,
        key: String,
        content: String,
        imageUrl: String? = null,
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
        )

    fun reduce(
        state: ChatUiState,
        envelope: AgentUiEnvelope<AgentPayload>,
    ): ChatUiState {
        val base = state.copy(
            sessionId = state.sessionId ?: envelope.sessionId,
            currentTurnId = envelope.turnId,
        )

        return when (envelope.event) {
            AgentEventType.Thinking -> base.upsertNode(
                ThinkingNode(envelope.nodeId, envelope.payload as ThinkingPayload),
            ).copy(inputState = ChatInputState.Streaming, isStreaming = true)

            AgentEventType.Clarification -> base.upsertNode(
                ClarificationNode(envelope.nodeId, envelope.payload as ClarificationPayload),
            ).copy(inputState = ChatInputState.Clarifying, isStreaming = true)

            AgentEventType.CriteriaCard -> base.upsertNode(
                CriteriaNode(envelope.nodeId, envelope.payload as CriteriaCardPayload),
            )

            AgentEventType.TextDelta -> reduceTextDelta(base, envelope)

            AgentEventType.ProductCard -> reduceProductCard(base, envelope)

            AgentEventType.CartAction -> base.upsertNode(
                CartActionNode(envelope.nodeId, envelope.payload as CartActionPayload),
            )

            AgentEventType.FinalDecision -> base.upsertNode(
                FinalDecisionNode(envelope.nodeId, envelope.payload as FinalDecisionPayload),
            )

            AgentEventType.Done -> {
                val payload = envelope.payload as DonePayload
                base.copy(
                    inputState = if (payload.finishReason == "canceled") {
                        ChatInputState.Canceled
                    } else if (base.inputState == ChatInputState.Clarifying ||
                        base.inputState == ChatInputState.Error ||
                        base.inputState == ChatInputState.Canceled
                    ) {
                        base.inputState
                    } else {
                        ChatInputState.Idle
                    },
                    isStreaming = false,
                )
            }

            AgentEventType.Error -> {
                val payload = envelope.payload as ErrorPayload
                base.upsertNode(
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

    fun cancel(state: ChatUiState): ChatUiState =
        state.copy(inputState = ChatInputState.Canceled, isStreaming = false)

    fun markComposing(state: ChatUiState, hasText: Boolean, hasImage: Boolean): ChatUiState =
        state.copy(
            inputState = when {
                hasImage -> ChatInputState.ImageAttached
                hasText -> ChatInputState.Composing
                else -> ChatInputState.Idle
            },
        )

    private fun reduceTextDelta(
        state: ChatUiState,
        envelope: AgentUiEnvelope<AgentPayload>,
    ): ChatUiState {
        val payload = envelope.payload as TextDeltaPayload
        val messageKey = payload.messageId.ifBlank { envelope.nodeId }
        val existing = state.nodes.filterIsInstance<AiStreamNode>()
            .firstOrNull { it.messageId == payload.messageId || it.key == messageKey }
        val node = AiStreamNode(
            key = messageKey,
            messageId = payload.messageId,
            content = (existing?.content.orEmpty()) + payload.delta,
            done = payload.done,
        )
        return state.upsertNode(node)
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
        return state.upsertNode(
            ProductDeckNode(
                key = deckId,
                deckId = deckId,
                products = products,
            ),
        )
    }

    private fun ChatUiState.upsertNode(node: ChatUiNode): ChatUiState {
        val index = nodes.indexOfFirst { it.key == node.key }
        val nextNodes = if (index >= 0) {
            nodes.toMutableList().also { it[index] = node }
        } else {
            nodes + node
        }
        return copy(nodes = nextNodes)
    }
}
