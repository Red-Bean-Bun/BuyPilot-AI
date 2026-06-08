package com.buypilot.feature.chat

import com.buypilot.core.data.PersistedChatMessage
import com.buypilot.feature.chat.model.UserMessageNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRestoreMapperTest {
    @Test
    fun restoredSessionStateKeepsOnlyUserTextMessages() {
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
                    content = "v1 不恢复 assistant 文本",
                    createdAtMs = 200,
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
        assertEquals("sess_restore", state.sessionId)
        assertEquals(listOf("msg_user_1", "msg_user_2"), userNodes.map { it.key })
        assertEquals(listOf("先发的需求", "后发的预算"), userNodes.map { it.content })
        assertEquals("后发的预算", state.lastUserMessage)
        assertEquals("msg_user_2", state.lastUserMessageKey)
        assertEquals("http://127.0.0.1:8000", state.backendBaseUrl)
        assertEquals(false, state.useMockChat)
        assertEquals(true, state.ttsEnabled)
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
): PersistedChatMessage =
    PersistedChatMessage(
        messageId = messageId,
        sessionId = "sess_restore",
        turnId = null,
        role = role,
        content = content,
        createdAtMs = createdAtMs,
    )
