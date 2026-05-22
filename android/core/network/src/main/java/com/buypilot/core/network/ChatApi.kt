package com.buypilot.core.network

import com.buypilot.core.model.AgentPayload
import com.buypilot.core.model.AgentUiEnvelope
import com.buypilot.core.model.requests.ChatStreamRequest
import kotlinx.coroutines.flow.Flow

interface ChatApi {
    fun stream(request: ChatStreamRequest): Flow<AgentUiEnvelope<AgentPayload>>
}

class SseChatApi(
    private val sseClient: SseClient,
) : ChatApi {
    override fun stream(request: ChatStreamRequest): Flow<AgentUiEnvelope<AgentPayload>> =
        sseClient.streamChat(request)
}
