package com.buypilot.core.network

import com.buypilot.core.common.json.AppJson
import com.buypilot.core.model.requests.FeedbackRequest
import com.buypilot.core.model.responses.FeedbackResponse
import kotlinx.serialization.encodeToString

interface FeedbackApi {
    suspend fun submit(request: FeedbackRequest): FeedbackResponse
}

class OkHttpFeedbackApi(
    private val restClient: RestClient,
) : FeedbackApi {
    override suspend fun submit(request: FeedbackRequest): FeedbackResponse =
        restClient.postJson(
            path = "/feedback",
            bodyJson = AppJson.instance.encodeToString(request),
            responseDeserializer = FeedbackResponse.serializer(),
        )
}
