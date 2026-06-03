package com.buypilot.core.data

import com.buypilot.core.database.dao.MessageDao
import com.buypilot.core.database.dao.SessionDao
import com.buypilot.core.database.entity.MessageEntity
import com.buypilot.core.database.entity.SessionEntity
import com.buypilot.core.model.AgentPayload
import com.buypilot.core.model.AgentUiEnvelope
import com.buypilot.core.model.CartItemPayload
import com.buypilot.core.model.ProductPayload
import com.buypilot.core.model.requests.ChatCancelRequest
import com.buypilot.core.model.requests.ChatStreamRequest
import com.buypilot.core.model.requests.FeedbackRequest
import com.buypilot.core.model.responses.CartResponse
import com.buypilot.core.model.responses.ChatCancelResponse
import com.buypilot.core.model.responses.FeedbackResponse
import com.buypilot.core.model.responses.ImageUploadResponse
import com.buypilot.core.model.responses.ProductDetailResponse
import com.buypilot.core.network.BaseUrlProvider
import com.buypilot.core.network.CartApi
import com.buypilot.core.network.ChatApi
import com.buypilot.core.network.ChatCancelApi
import com.buypilot.core.network.FeedbackApi
import com.buypilot.core.network.ImageUploadApi
import com.buypilot.core.network.ProductDetailApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatRepositoryProductDetailTest {
    @Test
    fun fetchProductDetailCachesAndReusesResponse() = runTest {
        val productDetailApi = RecordingProductDetailApi()
        val repository = testRepository(productDetailApi = productDetailApi)

        val first = repository.fetchProductDetail("p_detail_001")
        val second = repository.fetchProductDetail("p_detail_001")

        assertEquals(first, second)
        assertEquals(1, productDetailApi.callCount("p_detail_001"))
        assertEquals(first, repository.getCachedProductDetail("p_detail_001"))
    }

    @Test
    fun productDetailCacheKeepsMostRecentTwentyItems() = runTest {
        val productDetailApi = RecordingProductDetailApi()
        val repository = testRepository(productDetailApi = productDetailApi)

        repository.fetchProductDetail("p_detail_000")
        (1..20).forEach { index ->
            repository.fetchProductDetail("p_detail_${index.toString().padStart(3, '0')}")
        }

        assertNull(repository.getCachedProductDetail("p_detail_000"))
        assertEquals("p_detail_020", repository.getCachedProductDetail("p_detail_020")?.product?.productId)
    }

    private fun testRepository(productDetailApi: ProductDetailApi): ChatRepository =
        ChatRepository(
            chatApi = EmptyChatApi,
            chatCancelApi = NoopChatCancelApi,
            feedbackApi = NoopFeedbackApi,
            imageUploadApi = NoopImageUploadApi,
            cartApi = NoopCartApi,
            productDetailApi = productDetailApi,
            baseUrlProvider = StaticBaseUrlProvider,
            sessionDao = InMemorySessionDao,
            messageDao = InMemoryMessageDao,
        )
}

private class RecordingProductDetailApi : ProductDetailApi {
    private val callsByProductId = mutableMapOf<String, Int>()

    override suspend fun getProductDetail(productId: String): ProductDetailResponse {
        callsByProductId[productId] = callsByProductId.getOrDefault(productId, 0) + 1
        return ProductDetailResponse(
            product = ProductPayload(
                productId = productId,
                name = "商品 $productId",
                category = "数码电子",
            ),
            marketingDescription = "详情 $productId",
            highlights = listOf("亮点 $productId"),
        )
    }

    fun callCount(productId: String): Int = callsByProductId.getOrDefault(productId, 0)
}

private object EmptyChatApi : ChatApi {
    override fun stream(request: ChatStreamRequest): Flow<AgentUiEnvelope<AgentPayload>> = emptyFlow()
}

private object NoopChatCancelApi : ChatCancelApi {
    override suspend fun cancel(request: ChatCancelRequest): ChatCancelResponse = ChatCancelResponse(canceled = true)
}

private object NoopFeedbackApi : FeedbackApi {
    override suspend fun submit(request: FeedbackRequest): FeedbackResponse =
        FeedbackResponse(sessionId = request.sessionId, feedbackType = request.feedbackType, action = request.action)
}

private object NoopImageUploadApi : ImageUploadApi {
    override suspend fun uploadImage(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        sessionId: String?,
    ): ImageUploadResponse = ImageUploadResponse(imageUrl = "/uploads/$fileName", mimeType = mimeType)
}

private object NoopCartApi : CartApi {
    override suspend fun getCart(sessionId: String): CartResponse = CartResponse()

    override suspend fun updateQuantity(sessionId: String, productId: String, quantity: Int): CartItemPayload =
        CartItemPayload(productId = productId, quantity = quantity)

    override suspend fun removeItem(sessionId: String, productId: String) = Unit
}

private object StaticBaseUrlProvider : BaseUrlProvider {
    override val baseUrl: String = "http://127.0.0.1:8000"
}

private object InMemorySessionDao : SessionDao {
    private val sessions = mutableMapOf<String, SessionEntity>()

    override suspend fun upsert(session: SessionEntity) {
        sessions[session.sessionId] = session
    }

    override fun observeSessions(): Flow<List<SessionEntity>> = flowOf(sessions.values.toList())

    override suspend fun getSession(sessionId: String): SessionEntity? = sessions[sessionId]

    override suspend fun delete(sessionId: String) {
        sessions.remove(sessionId)
    }
}

private object InMemoryMessageDao : MessageDao {
    private val messagesBySessionId = mutableMapOf<String, MutableList<MessageEntity>>()

    override suspend fun upsert(message: MessageEntity) {
        val messages = messagesBySessionId.getOrPut(message.sessionId) { mutableListOf() }
        messages.removeAll { it.messageId == message.messageId }
        messages += message
    }

    override fun observeMessages(sessionId: String): Flow<List<MessageEntity>> =
        flowOf(messagesBySessionId[sessionId].orEmpty().sortedBy { it.createdAtMs })

    override suspend fun getMessages(sessionId: String): List<MessageEntity> =
        messagesBySessionId[sessionId].orEmpty().sortedBy { it.createdAtMs }

    override suspend fun deleteForSession(sessionId: String) {
        messagesBySessionId.remove(sessionId)
    }
}
