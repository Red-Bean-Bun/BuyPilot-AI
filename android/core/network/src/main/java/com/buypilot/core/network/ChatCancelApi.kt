package com.buypilot.core.network

import com.buypilot.core.common.json.AppJson
import com.buypilot.core.model.requests.ChatCancelRequest
import com.buypilot.core.model.responses.ChatCancelResponse
import kotlinx.serialization.encodeToString

interface ChatCancelApi {
    suspend fun cancel(request: ChatCancelRequest): ChatCancelResponse
}

class OkHttpChatCancelApi(
    private val restClient: RestClient,
) : ChatCancelApi {
    override suspend fun cancel(request: ChatCancelRequest): ChatCancelResponse =
        restClient.postJson(
            path = "/chat/cancel",
            bodyJson = AppJson.instance.encodeToString(request),
            responseDeserializer = ChatCancelResponse.serializer(),
        )
}
