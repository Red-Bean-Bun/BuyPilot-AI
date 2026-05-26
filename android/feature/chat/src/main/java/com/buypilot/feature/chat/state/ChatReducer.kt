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
            lastUserMessageKey = key,
            streamingTextKey = null,
            streamingTextLength = 0,
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

            AgentEventType.CriteriaCard -> contentBase.upsertNode(
                CriteriaNode(envelope.nodeId, envelope.payload as CriteriaCardPayload),
            )

            AgentEventType.TextDelta -> reduceTextDelta(base, envelope)

            AgentEventType.ProductCard -> reduceProductCard(contentBase, envelope)

            AgentEventType.CartAction -> contentBase.upsertNode(
                CartActionNode(envelope.nodeId, envelope.payload as CartActionPayload),
            )

            AgentEventType.FinalDecision -> contentBase.upsertNode(
                FinalDecisionNode(envelope.nodeId, envelope.payload as FinalDecisionPayload),
            )

            AgentEventType.Done -> {
                val payload = envelope.payload as DonePayload
                contentBase.copy(
                    inputState = if (payload.finishReason == "canceled") {
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

    private fun reduceTextDelta(
        state: ChatUiState,
        envelope: AgentUiEnvelope<AgentPayload>,
    ): ChatUiState {
        val payload = envelope.payload as TextDeltaPayload
        val messageKey = payload.messageId.ifBlank { envelope.nodeId }
        val existing = state.nodes.filterIsInstance<AiStreamNode>()
            .firstOrNull { it.messageId == payload.messageId || it.key == messageKey }
        val replacedThinkingKey = existing?.key ?: state.nodes
            .filterIsInstance<ThinkingNode>()
            .lastOrNull { it.turnId == envelope.turnId }
            ?.key
        val node = AiStreamNode(
            key = replacedThinkingKey ?: messageKey,
            messageId = payload.messageId,
            content = (existing?.content.orEmpty()) + payload.delta,
            done = payload.done,
        )
        return state.upsertNode(node).copy(
            streamingTextKey = node.key,
            streamingTextLength = node.content.length,
        )
    }

    private fun reduceClarification(
        state: ChatUiState,
        envelope: AgentUiEnvelope<AgentPayload>,
    ): ChatUiState {
        val payload = envelope.payload as ClarificationPayload
        val intro = clarificationIntroText(payload)
        val thinkingKey = state.nodes
            .filterIsInstance<ThinkingNode>()
            .lastOrNull { it.turnId == envelope.turnId }
            ?.key
        val introNode = AiStreamNode(
            key = thinkingKey ?: "clarification_intro_${envelope.turnId}",
            messageId = "clarification_intro_${envelope.turnId}",
            content = intro,
            done = true,
        )
        return state
            .upsertNode(introNode)
            .upsertNode(
                ClarificationNode(
                    key = envelope.nodeId,
                    payload = payload,
                    turnId = envelope.turnId,
                    anchorMessageKey = introNode.key,
                ),
            )
            .copy(
                inputState = ChatInputState.Clarifying,
                isStreaming = true,
                streamingTextKey = introNode.key,
                streamingTextLength = introNode.content.length,
            )
    }

    private fun clarificationIntroText(payload: ClarificationPayload): String {
        val question = payload.question.ifBlank { "请补充一个关键信息" }
        return "为了能为您推荐最合适的产品，我还需要了解一下**${question.trimEnd('？', '?')}**。"
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

    private fun AgentEventType.shouldClearTransientThinking(state: ChatUiState, turnId: String): Boolean =
        when (this) {
            AgentEventType.Thinking,
            AgentEventType.Clarification,
            AgentEventType.TextDelta,
            AgentEventType.Unknown -> false
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
