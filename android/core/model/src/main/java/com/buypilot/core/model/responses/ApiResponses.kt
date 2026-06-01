package com.buypilot.core.model.responses

import com.buypilot.core.model.CartItemPayload
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiStatusResponse(
    val ok: Boolean = true,
    val status: String? = null,
)

@Serializable
data class ChatCancelResponse(
    val ok: Boolean = true,
    val status: String? = null,
    val canceled: Boolean = false,
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("turn_id") val turnId: String? = null,
)

@Serializable
data class ImageUploadResponse(
    @SerialName("image_url") val imageUrl: String,
    val width: Int? = null,
    val height: Int? = null,
    @SerialName("mime_type") val mimeType: String? = null,
    @SerialName("ocr_text") val ocrText: String? = null,
)

@Serializable
data class FeedbackResponse(
    val status: String = "received",
    @SerialName("session_id") val sessionId: String,
    @SerialName("feedback_type") val feedbackType: String? = null,
    val action: String? = null,
)

@Serializable
data class CartResponse(
    val items: List<CartItemPayload> = emptyList(),
    @SerialName("total_items") val totalItems: Int = 0,
    @SerialName("total_price") val totalPrice: Double = 0.0,
)
