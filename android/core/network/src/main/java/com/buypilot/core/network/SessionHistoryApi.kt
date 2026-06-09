package com.buypilot.core.network

import com.buypilot.core.model.responses.SessionHistoryResponse
import javax.inject.Inject

interface SessionHistoryApi {
    suspend fun getSessionHistory(sessionId: String): SessionHistoryResponse
}

class OkHttpSessionHistoryApi @Inject constructor(
    private val restClient: RestClient,
) : SessionHistoryApi {
    override suspend fun getSessionHistory(sessionId: String): SessionHistoryResponse =
        restClient.getJson(
            path = "/chat/history/$sessionId",
            responseDeserializer = SessionHistoryResponse.serializer(),
        )
}
