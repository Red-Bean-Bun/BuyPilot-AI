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
import com.buypilot.core.model.requests.FeedbackRequest
import com.buypilot.core.network.BaseUrlProvider
import com.buypilot.core.network.ChatApi
import com.buypilot.core.network.ChatCancelApi
import com.buypilot.core.network.FeedbackApi
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ChatRepository @Inject constructor(
    private val chatApi: ChatApi,
    private val chatCancelApi: ChatCancelApi,
    private val feedbackApi: FeedbackApi,
    private val baseUrlProvider: BaseUrlProvider,
    private val sessionDao: SessionDao,
    private val messageDao: MessageDao,
) {
    val backendBaseUrl: String
        get() = baseUrlProvider.baseUrl

    fun streamChat(request: ChatStreamRequest): Flow<AgentUiEnvelope<AgentPayload>> =
        chatApi.stream(request)

    suspend fun cancel(sessionId: String, turnId: String) {
        chatCancelApi.cancel(ChatCancelRequest(sessionId = sessionId, turnId = turnId))
    }

    suspend fun submitProductFeedback(
        sessionId: String,
        productId: String,
        feedbackType: String,
        action: String,
        reason: String? = null,
    ) {
        feedbackApi.submit(
            FeedbackRequest(
                sessionId = sessionId,
                productId = productId,
                feedbackType = feedbackType,
                action = action,
                reason = reason,
            ),
        )
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
