package com.buypilot.core.network

import com.buypilot.core.common.sse.SseFrame
import com.buypilot.core.model.AgentPayload
import com.buypilot.core.model.AgentUiEnvelope
import com.buypilot.core.model.requests.ChatStreamRequest
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

interface SseClient {
    fun streamChat(request: ChatStreamRequest): Flow<AgentUiEnvelope<AgentPayload>>
}

class OkHttpSseClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val parser: SseEventParser,
    private val baseUrlProvider: BaseUrlProvider,
) : SseClient {
    override fun streamChat(request: ChatStreamRequest): Flow<AgentUiEnvelope<AgentPayload>> = callbackFlow {
        var seq = 0
        val body = json.encodeToString(request)
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val httpRequest = Request.Builder()
            .url(baseUrlProvider.baseUrl.trimEnd('/') + "/chat/stream")
            .post(body)
            .header("Accept", "text/event-stream")
            .header("Content-Type", "application/json")
            .build()

        val eventSource = EventSources.createFactory(okHttpClient)
            .newEventSource(
                httpRequest,
                object : EventSourceListener() {
                    override fun onEvent(
                        eventSource: EventSource,
                        id: String?,
                        type: String?,
                        data: String,
                    ) {
                        seq += 1
                        val parsed = runCatching {
                            parser.parse(SseFrame(event = type, data = data, id = id), fallbackSeq = seq)
                        }
                        parsed.onSuccess { trySend(it) }
                        parsed.onFailure { close(it) }
                    }

                    override fun onFailure(
                        eventSource: EventSource,
                        t: Throwable?,
                        response: Response?,
                    ) {
                        close(t ?: IllegalStateException("SSE failed with ${response?.code}"))
                    }

                    override fun onClosed(eventSource: EventSource) {
                        close()
                    }
                },
            )

        awaitClose {
            eventSource.cancel()
        }
    }
}
