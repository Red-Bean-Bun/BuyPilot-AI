package com.buypilot.core.model.responses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiStatusResponse(
    val ok: Boolean = true,
    val status: String? = null,
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
    val ok: Boolean = true,
    @SerialName("feedback_id") val feedbackId: String? = null,
)

@Serializable
data class CartResponse(
    val items: List<CartItemResponse> = emptyList(),
)

@Serializable
data class CartItemResponse(
    @SerialName("product_id") val productId: String,
    val quantity: Int = 1,
)
