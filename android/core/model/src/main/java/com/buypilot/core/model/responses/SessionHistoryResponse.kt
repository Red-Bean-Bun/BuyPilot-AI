package com.buypilot.core.model.responses

import com.buypilot.core.model.CartItemPayload
import com.buypilot.core.model.CriteriaPayload
import com.buypilot.core.model.EvidencePayload
import com.buypilot.core.model.ProductPayload
import com.buypilot.core.model.QuickActionPayload
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class HistoryProductCard(
    val rank: Int = 0,
    val product: ProductPayload = ProductPayload(),
    val reason: String = "",
    @SerialName("risk_notes") val riskNotes: List<String> = emptyList(),
    val evidence: List<EvidencePayload> = emptyList(),
    val actions: List<QuickActionPayload> = emptyList(),
)

@Serializable
data class HistoryDecision(
    @SerialName("winner_product_id") val winnerProductId: String? = null,
    val summary: String = "",
    val why: List<String> = emptyList(),
    @SerialName("not_for") val notFor: List<String> = emptyList(),
    val alternatives: List<JsonObject>? = null,
    @SerialName("next_actions") val nextActions: List<QuickActionPayload>? = null,
    @SerialName("decision_status") val decisionStatus: String? = null,
    val confidence: Double? = null,
    @SerialName("next_step") val nextStep: String? = null,
)

@Serializable
data class HistoryTurn(
    @SerialName("user_message") val userMessage: String = "",
    val criteria: CriteriaPayload? = null,
    val products: List<HistoryProductCard>? = null,
    @SerialName("turn_text") val turnText: String? = null,
    val decision: HistoryDecision? = null,
    @SerialName("deck_id") val deckId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class HistoryCart(
    val items: List<CartItemPayload> = emptyList(),
    @SerialName("total_items") val totalItems: Int = 0,
    @SerialName("total_price") val totalPrice: Double = 0.0,
)

@Serializable
data class SessionHistoryResponse(
    @SerialName("session_id") val sessionId: String = "",
    val turns: List<HistoryTurn> = emptyList(),
    val cart: HistoryCart? = null,
)
