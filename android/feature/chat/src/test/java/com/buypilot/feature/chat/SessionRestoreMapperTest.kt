package com.buypilot.feature.chat

import com.buypilot.core.common.json.AppJson
import com.buypilot.core.data.PersistedChatMessage
import com.buypilot.core.model.CriteriaCardPayload
import com.buypilot.core.model.CriteriaPayload
import com.buypilot.core.model.FinalDecisionPayload
import com.buypilot.core.model.ProductCardPayload
import com.buypilot.core.model.ProductPayload
import com.buypilot.feature.chat.model.AiStreamNode
import com.buypilot.feature.chat.model.CriteriaNode
import com.buypilot.feature.chat.model.FinalDecisionNode
import com.buypilot.feature.chat.model.ProductDeckNode
import com.buypilot.feature.chat.model.UserMessageNode
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRestoreMapperTest {
    @Test
    fun restoredSessionStateKeepsUserAndAssistantTextMessages() {
        val state = restoredSessionState(
            sessionId = "sess_restore",
            messages = listOf(
                persistedMessage(
                    messageId = "msg_user_1",
                    role = "user",
                    content = "先发的需求",
                    createdAtMs = 100,
                ),
                persistedMessage(
                    messageId = "msg_assistant",
                    role = "assistant",
                    content = "助手回复内容",
                    createdAtMs = 200,
                    turnId = "turn_1",
                ),
                persistedMessage(
                    messageId = "msg_user_2",
                    role = "user",
                    content = "后发的预算",
                    createdAtMs = 300,
                ),
            ),
            backendBaseUrl = "http://127.0.0.1:8000",
            useMockChat = false,
            ttsEnabled = true,
        )

        val userNodes = state.nodes.filterIsInstance<UserMessageNode>()
        val assistantNodes = state.nodes.filterIsInstance<AiStreamNode>()
        assertEquals("sess_restore", state.sessionId)
        assertEquals(listOf("msg_user_1", "msg_assistant", "msg_user_2"), state.nodes.map { it.key })
        assertEquals(listOf("msg_user_1", "msg_user_2"), userNodes.map { it.key })
        assertEquals(listOf("先发的需求", "后发的预算"), userNodes.map { it.content })
        assertEquals(listOf("msg_assistant"), assistantNodes.map { it.key })
        assertEquals(listOf("助手回复内容"), assistantNodes.map { it.content })
        assertEquals(listOf(true), assistantNodes.map { it.done })
        assertEquals(listOf("turn_1"), assistantNodes.map { it.turnId })
        assertEquals("后发的预算", state.lastUserMessage)
        assertEquals("msg_user_2", state.lastUserMessageKey)
        assertEquals("http://127.0.0.1:8000", state.backendBaseUrl)
        assertEquals(false, state.useMockChat)
        assertEquals(true, state.ttsEnabled)
    }

    @Test
    fun restoredSessionStateRestoresStructuredTimelineNodes() {
        val criteriaPayload = CriteriaCardPayload(
            criteria = CriteriaPayload(
                criteriaId = "criteria_1",
                category = "美妆护肤",
                summary = "油皮洁面，200 元以内",
            ),
        )
        val productPayload = ProductCardPayload(
            rank = 1,
            product = ProductPayload(
                productId = "p_beauty_001",
                name = "温和洁面",
                category = "美妆护肤",
            ),
            reason = "预算和肤质都匹配",
        )
        val decisionPayload = FinalDecisionPayload(
            winnerProductId = "p_beauty_001",
            summary = "优先选温和洁面。",
        )

        val state = restoredSessionState(
            sessionId = "sess_restore",
            messages = listOf(
                persistedMessage(
                    messageId = "msg_user",
                    role = "user",
                    content = "推荐适合油皮的洗面奶",
                    createdAtMs = 100,
                ),
                persistedMessage(
                    messageId = "criteria_1",
                    role = "assistant",
                    content = criteriaPayload.criteria.summary,
                    createdAtMs = 110,
                    turnId = "turn_1",
                    nodeType = RestorableNodeTypeCriteriaCard,
                    payloadJson = AppJson.instance.encodeToString(criteriaPayload),
                ),
                persistedMessage(
                    messageId = "deck_1",
                    role = "assistant",
                    content = "已推荐 1 个商品",
                    createdAtMs = 120,
                    turnId = "turn_1",
                    nodeType = RestorableNodeTypeProductDeck,
                    payloadJson = AppJson.instance.encodeToString(
                        PersistedProductDeckPayload(
                            deckId = "deck_1",
                            products = listOf(productPayload),
                        ),
                    ),
                    deckId = "deck_1",
                ),
                persistedMessage(
                    messageId = "decision_1",
                    role = "assistant",
                    content = decisionPayload.summary,
                    createdAtMs = 130,
                    turnId = "turn_1",
                    nodeType = RestorableNodeTypeFinalDecision,
                    payloadJson = AppJson.instance.encodeToString(decisionPayload),
                    deckId = "deck_1",
                ),
            ),
            backendBaseUrl = "http://127.0.0.1:8000",
            useMockChat = false,
            ttsEnabled = false,
        )

        val criteriaNode = state.nodes[1] as CriteriaNode
        val deckNode = state.nodes[2] as ProductDeckNode
        val decisionNode = state.nodes[3] as FinalDecisionNode
        assertEquals(listOf("msg_user", "criteria_1", "deck_1", "decision_1"), state.nodes.map { it.key })
        assertEquals("油皮洁面，200 元以内", criteriaNode.payload.criteria.summary)
        assertEquals("turn_1", criteriaNode.turnId)
        assertEquals("deck_1", deckNode.deckId)
        assertEquals(listOf("p_beauty_001"), deckNode.products.map { it.product.productId })
        assertEquals("turn_1", deckNode.turnId)
        assertEquals("p_beauty_001", decisionNode.payload.winnerProductId)
        assertEquals("deck_1", decisionNode.deckId)
    }

    @Test
    fun restoredSessionStateClearsRuntimeOnlyState() {
        val state = restoredSessionState(
            sessionId = "sess_empty",
            messages = emptyList(),
            backendBaseUrl = "http://127.0.0.1:8000",
            useMockChat = false,
            ttsEnabled = false,
        )

        assertEquals("sess_empty", state.sessionId)
        assertTrue(state.nodes.isEmpty())
        assertEquals(null, state.lastUserMessage)
        assertEquals(null, state.lastUserMessageKey)
        assertEquals(false, state.isStreaming)
        assertEquals(null, state.currentTurnId)
        assertEquals(null, state.lastError)
        assertEquals(0, state.cartState.totalItems)
    }
}

private fun persistedMessage(
    messageId: String,
    role: String,
    content: String,
    createdAtMs: Long,
    turnId: String? = null,
    nodeType: String? = null,
    payloadJson: String? = null,
    deckId: String? = null,
): PersistedChatMessage =
    PersistedChatMessage(
        messageId = messageId,
        sessionId = "sess_restore",
        turnId = turnId,
        role = role,
        content = content,
        nodeType = nodeType,
        payloadJson = payloadJson,
        deckId = deckId,
        createdAtMs = createdAtMs,
    )
