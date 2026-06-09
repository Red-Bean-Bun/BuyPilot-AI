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
import com.buypilot.core.model.responses.CartResponse
import com.buypilot.core.model.responses.ChatCancelResponse
import com.buypilot.core.model.responses.ImageUploadResponse
import com.buypilot.core.model.responses.ProductDetailResponse
import com.buypilot.core.model.responses.SessionHistoryResponse
import com.buypilot.core.network.BaseUrlProvider
import com.buypilot.core.network.CartApi
import com.buypilot.core.network.ChatApi
import com.buypilot.core.network.ChatCancelApi
import com.buypilot.core.network.FeedbackApi
import com.buypilot.core.network.ImageUploadApi
import com.buypilot.core.network.ProductDetailApi
import java.util.LinkedHashMap
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ChatRepository @Inject constructor(
    private val chatApi: ChatApi,
    private val chatCancelApi: ChatCancelApi,
    private val feedbackApi: FeedbackApi,
    private val imageUploadApi: ImageUploadApi,
    private val cartApi: CartApi,
    private val productDetailApi: ProductDetailApi,
    private val sessionHistoryApi: com.buypilot.core.network.SessionHistoryApi,
    private val baseUrlProvider: BaseUrlProvider,
    private val sessionDao: SessionDao,
    private val messageDao: MessageDao,
) {
    private val productDetailCacheLock = Any()
    private val productDetailCache = object : LinkedHashMap<String, ProductDetailResponse>(
        PRODUCT_DETAIL_CACHE_SIZE,
        0.75f,
        true,
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ProductDetailResponse>?): Boolean =
            size > PRODUCT_DETAIL_CACHE_SIZE
    }

    val backendBaseUrl: String
        get() = baseUrlProvider.baseUrl

    fun streamChat(request: ChatStreamRequest): Flow<AgentUiEnvelope<AgentPayload>> =
        chatApi.stream(request)

    suspend fun cancel(sessionId: String, turnId: String): ChatCancelResponse =
        chatCancelApi.cancel(ChatCancelRequest(sessionId = sessionId, turnId = turnId))

    suspend fun uploadImage(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        sessionId: String? = null,
    ): ImageUploadResponse =
        imageUploadApi.uploadImage(
            bytes = bytes,
            fileName = fileName,
            mimeType = mimeType,
            sessionId = sessionId,
        )

    suspend fun getCart(sessionId: String): CartResponse =
        cartApi.getCart(sessionId)

    fun getCachedProductDetail(productId: String): ProductDetailResponse? =
        synchronized(productDetailCacheLock) {
            productDetailCache[productId.takeIf { it.isNotBlank() } ?: return@synchronized null]
        }

    suspend fun fetchProductDetail(productId: String): ProductDetailResponse {
        getCachedProductDetail(productId)?.let { return it }
        val detail = productDetailApi.getProductDetail(productId)
        synchronized(productDetailCacheLock) {
            productDetailCache[productId] = detail
        }
        return detail
    }

    suspend fun updateCartQuantity(sessionId: String, productId: String, quantity: Int): CartResponse {
        cartApi.updateQuantity(sessionId, productId, quantity)
        return cartApi.getCart(sessionId)
    }

    suspend fun removeCartItem(sessionId: String, productId: String): CartResponse {
        cartApi.removeItem(sessionId, productId)
        return cartApi.getCart(sessionId)
    }

    suspend fun submitProductFeedback(
        sessionId: String,
        deckId: String,
        productId: String,
        feedbackType: String,
        action: String,
        reason: String? = null,
    ) {
        feedbackApi.submit(
            FeedbackRequest(
                sessionId = sessionId,
                deckId = deckId,
                productId = productId,
                feedbackType = feedbackType,
                action = action,
                reason = reason,
            ),
        )
    }

    suspend fun fetchSessionHistory(sessionId: String): SessionHistoryResponse =
        sessionHistoryApi.getSessionHistory(sessionId)

    suspend fun restoreChatMessages(sessionId: String): List<PersistedChatMessage> {
        val restored = messageDao.getMessages(sessionId)
            .asSequence()
            .map { message ->
                PersistedChatMessage(
                    messageId = message.messageId,
                    sessionId = message.sessionId,
                    turnId = message.turnId,
                    role = message.role,
                    content = message.content,
                    nodeType = message.nodeType,
                    payloadJson = message.payloadJson,
                    deckId = message.deckId,
                    createdAtMs = message.createdAtMs,
                )
            }
            .toList()
        if (restored.isNotEmpty()) return restored

        val legacySession = sessionDao.getSession(sessionId) ?: return emptyList()
        val fallbackContent = legacySession.lastMessage.trim().ifBlank { legacySession.title.trim() }
        if (fallbackContent.isBlank()) return emptyList()
        return listOf(
            PersistedChatMessage(
                messageId = "legacy_${legacySession.sessionId}_${legacySession.updatedAtMs}",
                sessionId = legacySession.sessionId,
                turnId = null,
                role = "user",
                content = fallbackContent,
                nodeType = "user_message",
                createdAtMs = legacySession.updatedAtMs,
            ),
        )
    }

    suspend fun recordAssistantMessage(
        sessionId: String,
        messageId: String,
        turnId: String?,
        content: String,
        nowMs: Long,
    ) {
        val cleanContent = content.trim()
        if (sessionId.isBlank() || messageId.isBlank() || cleanContent.isBlank()) return

        recordAssistantNode(
            sessionId = sessionId,
            messageId = messageId,
            turnId = turnId,
            content = cleanContent,
            nodeType = "assistant_text",
            payloadJson = null,
            deckId = null,
            nowMs = nowMs,
            updateLastMessage = true,
        )
    }

    suspend fun recordAssistantStructuredNode(
        sessionId: String,
        messageId: String,
        turnId: String?,
        nodeType: String,
        payloadJson: String,
        deckId: String?,
        content: String,
        nowMs: Long,
    ) {
        if (sessionId.isBlank() || messageId.isBlank() || nodeType.isBlank() || payloadJson.isBlank()) return

        recordAssistantNode(
            sessionId = sessionId,
            messageId = messageId,
            turnId = turnId,
            content = content.trim(),
            nodeType = nodeType,
            payloadJson = payloadJson,
            deckId = deckId,
            nowMs = nowMs,
            updateLastMessage = false,
        )
    }

    private suspend fun recordAssistantNode(
        sessionId: String,
        messageId: String,
        turnId: String?,
        content: String,
        nodeType: String,
        payloadJson: String?,
        deckId: String?,
        nowMs: Long,
        updateLastMessage: Boolean,
    ) {
        val existingMessage = messageDao.getMessage(messageId)
        messageDao.upsert(
            MessageEntity(
                messageId = messageId,
                sessionId = sessionId,
                turnId = turnId?.takeIf { it.isNotBlank() },
                role = "assistant",
                content = content,
                nodeType = nodeType.takeIf { it.isNotBlank() },
                payloadJson = payloadJson?.takeIf { it.isNotBlank() },
                deckId = deckId?.takeIf { it.isNotBlank() },
                createdAtMs = existingMessage?.createdAtMs ?: nowMs,
            ),
        )
        val existing = sessionDao.getSession(sessionId) ?: return
        sessionDao.upsert(
            existing.copy(
                lastMessage = if (updateLastMessage && content.isNotBlank()) {
                    content.take(120)
                } else {
                    existing.lastMessage
                },
                updatedAtMs = maxOf(existing.updatedAtMs, nowMs),
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
                nodeType = "user_message",
                createdAtMs = nowMs,
            ),
        )
        val existing = sessionDao.getSession(sessionId)
        val title = existing?.title?.takeIf { it.isNotBlank() }
            ?: content.trim().take(24).ifBlank { "新会话" }
        sessionDao.upsert(
            SessionEntity(
                sessionId = sessionId,
                title = title,
                lastMessage = content,
                createdAtMs = existing?.createdAtMs ?: nowMs,
                updatedAtMs = nowMs,
            ),
        )
    }

    private companion object {
        const val PRODUCT_DETAIL_CACHE_SIZE = 20
    }
}
