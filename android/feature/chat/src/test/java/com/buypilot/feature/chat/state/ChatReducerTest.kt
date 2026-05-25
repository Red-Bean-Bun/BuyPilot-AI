package com.buypilot.feature.chat.state

import com.buypilot.core.model.AgentEventType
import com.buypilot.core.model.AgentPayload
import com.buypilot.core.model.AgentUiEnvelope
import com.buypilot.core.model.ClarificationPayload
import com.buypilot.core.model.DonePayload
import com.buypilot.core.model.ErrorPayload
import com.buypilot.core.model.ProductCardPayload
import com.buypilot.core.model.ProductPayload
import com.buypilot.core.model.TextDeltaPayload
import com.buypilot.core.model.ThinkingPayload
import com.buypilot.feature.chat.model.AiStreamNode
import com.buypilot.feature.chat.model.ClarificationNode
import com.buypilot.feature.chat.model.ProductDeckNode
import com.buypilot.feature.chat.model.ThinkingNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatReducerTest {
    @Test
    fun fillsSessionIdFromFirstEventAndUpsertsNode() {
        val first = envelope(
            event = AgentEventType.Thinking,
            nodeId = "thinking_turn_1",
            payload = ThinkingPayload(stage = "understanding", message = "A"),
        )
        val second = first.copy(payload = ThinkingPayload(stage = "searching", message = "B"))

        val state = ChatReducer.reduce(ChatReducer.reduce(ChatUiState(), first), second)

        assertEquals("sess_1", state.sessionId)
        assertEquals(1, state.nodes.size)
    }

    @Test
    fun appendsTextDeltaIntoSameNode() {
        val state = listOf("hello ", "world").fold(ChatUiState()) { acc, delta ->
            ChatReducer.reduce(
                acc,
                envelope(
                    event = AgentEventType.TextDelta,
                    nodeId = "ignored_node",
                    payload = TextDeltaPayload(messageId = "msg_1", delta = delta),
                ),
            )
        }

        val node = state.nodes.single() as AiStreamNode
        assertEquals("hello world", node.content)
        assertEquals("msg_1", node.messageId)
        assertEquals("msg_1", node.key)
        assertEquals("msg_1", state.streamingTextKey)
        assertEquals("hello world".length, state.streamingTextLength)
    }

    @Test
    fun addUserMessageStoresScrollAnchorAndResetsStreamingTextAnchor() {
        val state = ChatReducer.addUserMessage(
            state = ChatUiState(streamingTextKey = "old_msg", streamingTextLength = 12),
            key = "user_1",
            content = "推荐适合油皮的洗面奶",
        )

        assertEquals("user_1", state.lastUserMessageKey)
        assertEquals("推荐适合油皮的洗面奶", state.lastUserMessage)
        assertEquals(null, state.streamingTextKey)
        assertEquals(0, state.streamingTextLength)
    }

    @Test
    fun clarificationRemovesTransientThinkingForSameTurn() {
        val thinking = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.Thinking,
                nodeId = "thinking_turn_1",
                payload = ThinkingPayload(stage = "understanding", message = "正在理解您的需求"),
            ),
        )

        val state = ChatReducer.reduce(
            thinking,
            envelope(
                event = AgentEventType.Clarification,
                nodeId = "clarify_turn_1",
                payload = ClarificationPayload(question = "请问你的肤质是？"),
            ),
        )

        assertEquals(1, state.nodes.size)
        assertTrue(state.nodes.single() is ClarificationNode)
        assertFalse(state.nodes.any { it is ThinkingNode })
    }

    @Test
    fun textDeltaRemovesTransientThinkingAndAppendsIntoAiStreamNode() {
        val thinking = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.Thinking,
                nodeId = "thinking_turn_1",
                payload = ThinkingPayload(stage = "understanding", message = "正在理解您的需求"),
            ),
        )

        val state = listOf("敏感肌面霜，", "我会先避开酒精和香精。").fold(thinking) { acc, delta ->
            ChatReducer.reduce(
                acc,
                envelope(
                    event = AgentEventType.TextDelta,
                    nodeId = "assistant_intro_turn_1",
                    payload = TextDeltaPayload(messageId = "assistant_intro_turn_1", delta = delta),
                ),
            )
        }

        val node = state.nodes.single() as AiStreamNode
        assertEquals("敏感肌面霜，我会先避开酒精和香精。", node.content)
        assertFalse(state.nodes.any { it is ThinkingNode })
    }

    @Test
    fun doneRemovesTransientThinkingAndClosesStreaming() {
        val thinking = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.Thinking,
                nodeId = "thinking_turn_1",
                payload = ThinkingPayload(stage = "understanding", message = "正在理解您的需求"),
            ),
        )

        val state = ChatReducer.reduce(
            thinking,
            envelope(
                event = AgentEventType.Done,
                nodeId = "done_turn_1",
                payload = DonePayload(),
            ),
        )

        assertTrue(state.nodes.isEmpty())
        assertFalse(state.isStreaming)
        assertEquals(ChatInputState.Idle, state.inputState)
    }

    @Test
    fun groupsProductCardsByDeckId() {
        val first = product(rank = 2, productId = "p2")
        val second = product(rank = 1, productId = "p1")

        val state = ChatReducer.reduce(ChatReducer.reduce(ChatUiState(), first), second)
        val deck = state.nodes.single() as ProductDeckNode

        assertEquals("deck_1", deck.deckId)
        assertEquals(listOf("p1", "p2"), deck.products.map { it.product.productId })
    }

    @Test
    fun doneAndErrorCloseStreaming() {
        val streaming = ChatUiState(isStreaming = true, inputState = ChatInputState.Streaming)
        val clarifying = ChatUiState(isStreaming = true, inputState = ChatInputState.Clarifying)
        val done = ChatReducer.reduce(
            streaming,
            envelope(
                event = AgentEventType.Done,
                nodeId = "done_turn_1",
                payload = DonePayload(),
            ),
        )
        val clarificationDone = ChatReducer.reduce(
            clarifying,
            envelope(
                event = AgentEventType.Done,
                nodeId = "done_turn_1",
                payload = DonePayload(),
            ),
        )
        val error = ChatReducer.reduce(
            streaming,
            envelope(
                event = AgentEventType.Error,
                nodeId = "error_turn_1",
                payload = ErrorPayload(code = "LLM_TIMEOUT", message = "timeout"),
            ),
        )

        assertFalse(done.isStreaming)
        assertEquals(ChatInputState.Idle, done.inputState)
        assertFalse(clarificationDone.isStreaming)
        assertEquals(ChatInputState.Clarifying, clarificationDone.inputState)
        assertFalse(error.isStreaming)
        assertEquals(ChatInputState.Error, error.inputState)
        assertTrue(error.lastError!!.contains("timeout"))
    }

    @Test
    fun clarificationKeepsStreamingUntilDone() {
        val state = ChatReducer.reduce(
            ChatUiState(),
            envelope(
                event = AgentEventType.Clarification,
                nodeId = "clarify_turn_1",
                payload = ClarificationPayload(
                    question = "Q",
                    requiredSlots = listOf("skin_type"),
                    suggestedOptions = listOf("油性"),
                ),
            ),
        )

        assertTrue(state.isStreaming)
        assertEquals(ChatInputState.Clarifying, state.inputState)
    }

    private fun product(rank: Int, productId: String) = envelope(
        event = AgentEventType.ProductCard,
        nodeId = "product_$productId",
        deckId = "deck_1",
        payload = ProductCardPayload(
            rank = rank,
            product = ProductPayload(productId = productId, name = productId),
            reason = "reason",
        ),
    )

    private fun envelope(
        event: AgentEventType,
        nodeId: String,
        deckId: String? = null,
        payload: AgentPayload,
    ) = AgentUiEnvelope(
        event = event,
        sessionId = "sess_1",
        turnId = "turn_1",
        seq = 1,
        eventId = "turn_1:1",
        nodeId = nodeId,
        deckId = deckId,
        displayMode = null,
        payload = payload,
    )
}
