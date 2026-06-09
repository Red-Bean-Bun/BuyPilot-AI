package com.buypilot.feature.chat

import com.buypilot.core.common.json.AppJson
import com.buypilot.core.model.CriteriaCardPayload
import com.buypilot.core.model.CriteriaPayload
import com.buypilot.core.model.FinalDecisionPayload
import com.buypilot.core.model.ProductCardPayload
import com.buypilot.core.model.ProductPayload
import com.buypilot.feature.chat.model.AiStreamNode
import com.buypilot.feature.chat.model.CriteriaNode
import com.buypilot.feature.chat.model.FinalDecisionNode
import com.buypilot.feature.chat.model.ProductDeckNode
import com.buypilot.feature.chat.state.ChatUiState
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Test

class AssistantMessagePersistenceTest {
    @Test
    fun completedAssistantMessagesForPersistenceKeepsAllDoneTextSegmentsInTurn() {
        val state = ChatUiState(
            currentTurnId = "turn_1",
            nodes = listOf(
                assistantNode(key = "intro_turn_1", content = "开场", turnId = "turn_1", done = true),
                assistantNode(key = "followup_turn_1", content = "追问引导", turnId = "turn_1", done = true),
                assistantNode(key = "draft_turn_1", content = "未完成", turnId = "turn_1", done = false),
                assistantNode(key = "intro_turn_0", content = "旧内容", turnId = "turn_0", done = true),
            ),
        )

        val nodes = completedAssistantMessagesForPersistence(
            state = state,
            turnId = "turn_1",
            persistedMessageKeys = setOf("intro_turn_1"),
        )

        assertEquals(listOf("followup_turn_1"), nodes.map { it.key })
    }

    @Test
    fun completedAssistantMessagesForPersistenceFallsBackToCurrentTurnWhenDoneTurnIsBlank() {
        val state = ChatUiState(
            currentTurnId = "turn_current",
            nodes = listOf(
                assistantNode(key = "intro_turn_current", content = "开场", turnId = "turn_current", done = true),
                assistantNode(key = "followup_turn_current", content = "追问引导", turnId = "turn_current", done = true),
                assistantNode(key = "old_turn", content = "旧回复", turnId = "turn_old", done = true),
            ),
        )

        val nodes = completedAssistantMessagesForPersistence(
            state = state,
            turnId = "",
            persistedMessageKeys = emptySet(),
        )

        assertEquals(listOf("intro_turn_current", "followup_turn_current"), nodes.map { it.key })
    }

    @Test
    fun restorableChatNodesForPersistenceIncludesStructuredNodesForTurn() {
        val state = ChatUiState(
            currentTurnId = "turn_1",
            nodes = listOf(
                assistantNode(key = "intro_turn_1", content = "开场", turnId = "turn_1", done = true),
                CriteriaNode(
                    key = "criteria_1",
                    turnId = "turn_1",
                    payload = CriteriaCardPayload(
                        criteria = CriteriaPayload(
                            criteriaId = "criteria_1",
                            category = "美妆护肤",
                            summary = "油皮洁面",
                        ),
                    ),
                ),
                ProductDeckNode(
                    key = "deck_1",
                    deckId = "deck_1",
                    turnId = "turn_1",
                    products = listOf(
                        ProductCardPayload(
                            rank = 1,
                            product = ProductPayload(
                                productId = "p1",
                                name = "温和洁面",
                                category = "美妆护肤",
                            ),
                        ),
                    ),
                ),
                FinalDecisionNode(
                    key = "decision_1",
                    turnId = "turn_1",
                    deckId = "deck_1",
                    payload = FinalDecisionPayload(
                        winnerProductId = "p1",
                        summary = "选温和洁面。",
                    ),
                ),
                assistantNode(key = "old_turn", content = "旧回复", turnId = "turn_old", done = true),
            ),
        )

        val snapshots = restorableChatNodesForPersistence(
            state = state,
            turnId = "turn_1",
            persistedMessageKeys = setOf("intro_turn_1"),
        )

        assertEquals(listOf("criteria_1", "deck_1", "decision_1"), snapshots.map { it.messageId })
        assertEquals(
            listOf(
                RestorableNodeTypeCriteriaCard,
                RestorableNodeTypeProductDeck,
                RestorableNodeTypeFinalDecision,
            ),
            snapshots.map { it.nodeType },
        )
        val deckPayload = AppJson.instance.decodeFromString<PersistedProductDeckPayload>(
            snapshots.single { it.nodeType == RestorableNodeTypeProductDeck }.payloadJson!!,
        )
        assertEquals("deck_1", deckPayload.deckId)
        assertEquals(listOf("p1"), deckPayload.products.map { it.product.productId })
        assertEquals("deck_1", snapshots.single { it.nodeType == RestorableNodeTypeFinalDecision }.deckId)
    }
}

private fun assistantNode(
    key: String,
    content: String,
    turnId: String,
    done: Boolean,
) = AiStreamNode(
    key = key,
    messageId = key,
    content = content,
    done = done,
    turnId = turnId,
)
