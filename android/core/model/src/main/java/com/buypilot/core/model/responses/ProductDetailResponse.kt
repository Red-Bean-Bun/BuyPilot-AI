package com.buypilot.core.model.responses

import com.buypilot.core.model.ProductPayload
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductDetailResponse(
    val product: ProductPayload = ProductPayload(),
    @SerialName("marketing_description") val marketingDescription: String? = null,
    val highlights: List<String> = emptyList(),
    val faqs: List<FaqItem> = emptyList(),
    val reviews: List<ReviewItem> = emptyList(),
)

@Serializable
data class FaqItem(
    val question: String = "",
    val answer: String = "",
)

@Serializable
data class ReviewItem(
    val nickname: String = "",
    val rating: Int = 0,
    val content: String = "",
)
