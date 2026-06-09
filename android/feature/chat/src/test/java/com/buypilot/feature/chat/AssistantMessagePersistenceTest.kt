package com.buypilot.feature.chat

import com.buypilot.feature.chat.model.AiStreamNode
import com.buypilot.feature.chat.state.ChatUiState
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
