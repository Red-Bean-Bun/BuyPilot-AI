package com.buypilot.core.data

import com.buypilot.core.common.id.Ids
import com.buypilot.core.database.dao.MessageDao
import com.buypilot.core.database.dao.SessionDao
import com.buypilot.core.database.entity.MessageEntity
import com.buypilot.core.database.entity.SessionEntity
import com.buypilot.core.model.AgentPayload
import com.buypilot.core.model.AgentUiEnvelope
import com.buypilot.core.model.requests.ChatCancelRequest
import com.buypilot.core.model.requests.ChatStreamRequest
import com.buypilot.core.network.ChatApi
import com.buypilot.core.network.ChatCancelApi
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ChatRepository @Inject constructor(
    private val chatApi: ChatApi,
    private val chatCancelApi: ChatCancelApi,
    private val sessionDao: SessionDao,
    private val messageDao: MessageDao,
) {
    fun streamChat(request: ChatStreamRequest): Flow<AgentUiEnvelope<AgentPayload>> =
        chatApi.stream(request)

    suspend fun cancel(sessionId: String, turnId: String) {
        chatCancelApi.cancel(ChatCancelRequest(sessionId = sessionId, turnId = turnId))
    }

    suspend fun recordUserMessage(
        sessionId: String,
        content: String,
        nowMs: Long,
    ) {
        messageDao.upsert(
            MessageEntity(
                messageId = Ids.userMessageId(nowMs),
                sessionId = sessionId,
                role = "user",
                content = content,
                createdAtMs = nowMs,
            ),
        )
        sessionDao.upsert(
            SessionEntity(
                sessionId = sessionId,
                title = content.take(24).ifBlank { "新会话" },
                lastMessage = content,
                createdAtMs = nowMs,
                updatedAtMs = nowMs,
            ),
        )
    }
}
