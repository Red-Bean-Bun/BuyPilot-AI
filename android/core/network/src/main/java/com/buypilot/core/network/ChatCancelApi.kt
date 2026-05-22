package com.buypilot.core.network

import com.buypilot.core.common.json.AppJson
import com.buypilot.core.model.requests.ChatCancelRequest
import com.buypilot.core.model.responses.ApiStatusResponse
import kotlinx.serialization.encodeToString

interface ChatCancelApi {
    suspend fun cancel(request: ChatCancelRequest): ApiStatusResponse
}

class OkHttpChatCancelApi(
    private val restClient: RestClient,
) : ChatCancelApi {
    override suspend fun cancel(request: ChatCancelRequest): ApiStatusResponse =
        restClient.postJson(
            path = "/chat/cancel",
            bodyJson = AppJson.instance.encodeToString(request),
            responseDeserializer = ApiStatusResponse.serializer(),
        )
}
