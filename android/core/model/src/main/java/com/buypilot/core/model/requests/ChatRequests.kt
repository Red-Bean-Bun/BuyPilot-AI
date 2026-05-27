package com.buypilot.core.model.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ChatStreamRequest(
    val message: String,
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("client_turn_id") val clientTurnId: String? = null,
    val history: List<MessageLite> = emptyList(),
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("criteria_patch") val criteriaPatch: JsonObject? = null,
    @SerialName("skip_stages") val skipStages: List<String> = emptyList(),
    @SerialName("client_trace_id") val clientTraceId: String? = null,
)

@Serializable
data class MessageLite(
    val role: String,
    val content: String,
)

@Serializable
data class ChatCancelRequest(
    @SerialName("session_id") val sessionId: String,
    @SerialName("turn_id") val turnId: String,
)

@Serializable
data class FeedbackRequest(
    @SerialName("session_id") val sessionId: String,
    @SerialName("product_id") val productId: String? = null,
    @SerialName("feedback_type") val feedbackType: String? = null,
    val action: String? = null,
    val reason: String? = null,
)
