package com.buypilot.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import com.buypilot.core.model.serialization.FlexibleStringListSerializer

sealed interface AgentPayload

@Serializable
data class ThinkingPayload(
    val stage: String = "",
    val message: String = "",
    val fallback: Boolean = false,
    @SerialName("is_fallback") val isFallback: Boolean = false,
) : AgentPayload

@Serializable
data class ClarificationPayload(
    val question: String = "",
    @SerialName("required_slots") val requiredSlots: List<String> = emptyList(),
    @SerialName("suggested_options") val suggestedOptions: List<String> = emptyList(),
    @SerialName("partial_criteria") val partialCriteria: JsonObject? = null,
) : AgentPayload

@Serializable
data class CriteriaPayload(
    @SerialName("criteria_id") val criteriaId: String = "",
    val category: String = "",
    val summary: String = "",
    val chips: List<String> = emptyList(),
    val constraints: Constraints? = null,
    @SerialName("product_type") val productType: String? = null,
    @SerialName("skin_type") val skinType: String? = null,
    @SerialName("budget_min") val budgetMin: Double? = null,
    @SerialName("budget_max") val budgetMax: Double? = null,
    @SerialName("brand_avoid") @Serializable(with = FlexibleStringListSerializer::class)
    val brandAvoid: List<String> = emptyList(),
    @SerialName("origin_avoid") @Serializable(with = FlexibleStringListSerializer::class)
    val originAvoid: List<String> = emptyList(),
    @SerialName("ingredient_avoid") @Serializable(with = FlexibleStringListSerializer::class)
    val ingredientAvoid: List<String> = emptyList(),
    @SerialName("ingredient_prefer") @Serializable(with = FlexibleStringListSerializer::class)
    val ingredientPrefer: List<String> = emptyList(),
    @SerialName("use_scenario") @Serializable(with = FlexibleStringListSerializer::class)
    val useScenario: List<String> = emptyList(),
    val storage: String? = null,
    @SerialName("screen_size") val screenSize: String? = null,
    @SerialName("sport_type") val sportType: String? = null,
    val season: String? = null,
    val dietary: List<String> = emptyList(),
) : AgentPayload

@Serializable
data class CriteriaCardPayload(
    val editable: Boolean = true,
    val criteria: CriteriaPayload = CriteriaPayload(),
    @SerialName("quick_actions") val quickActions: List<QuickActionPayload> = emptyList(),
) : AgentPayload

@Serializable
data class TextDeltaPayload(
    @SerialName("message_id") val messageId: String = "",
    val delta: String = "",
    val done: Boolean = false,
) : AgentPayload

@Serializable
data class ProductPayload(
    @SerialName("product_id") val productId: String = "",
    val name: String = "",
    val price: Double? = null,
    val currency: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val category: String = "",
    @SerialName("sub_category") val subCategory: String? = null,
    val brand: String? = null,
    @SerialName("skin_type_match") val skinTypeMatch: List<String> = emptyList(),
    @SerialName("ingredient_tags") val ingredientTags: List<String> = emptyList(),
    @SerialName("ingredient_avoid") val ingredientAvoid: List<String> = emptyList(),
    @Serializable(with = FlexibleStringListSerializer::class)
    @SerialName("use_scenario")
    val useScenario: List<String> = emptyList(),
    @SerialName("sku_options") val skuOptions: List<JsonObject>? = null,
)

@Serializable
data class ReasonAtomPayload(
    val dimension: String = "",
    val value: String = "",
    val text: String = "",
    @SerialName("evidence_id") val evidenceId: String? = null,
)

@Serializable
data class ProductCardPayload(
    val rank: Int = 0,
    val product: ProductPayload = ProductPayload(),
    val reason: String = "",
    @SerialName("reason_atoms") val reasonAtoms: List<ReasonAtomPayload> = emptyList(),
    @SerialName("risk_notes") val riskNotes: List<String> = emptyList(),
    val evidence: List<EvidencePayload> = emptyList(),
    val actions: List<QuickActionPayload> = emptyList(),
) : AgentPayload

@Serializable
data class CartItemPayload(
    @SerialName("product_id") val productId: String = "",
    val name: String = "",
    val price: Double? = null,
    val quantity: Int = 1,
    @SerialName("added_at") val addedAt: String? = null,
    val product: ProductPayload? = null,
)

@Serializable
data class CartSummaryPayload(
    val items: List<CartItemPayload> = emptyList(),
    @SerialName("total_items") val totalItems: Int = 0,
    @SerialName("total_price") val totalPrice: Double = 0.0,
)

@Serializable
data class FinalDecisionPayload(
    @SerialName("winner_product_id") val winnerProductId: String? = null,
    val summary: String = "",
    val why: List<String> = emptyList(),
    @SerialName("not_for") val notFor: List<String> = emptyList(),
    val alternatives: List<AlternativePayload> = emptyList(),
    @SerialName("next_actions") val nextActions: List<QuickActionPayload> = emptyList(),
    @SerialName("decision_status") val decisionStatus: String? = null,
    val confidence: String? = null,
    @SerialName("next_step") val nextStep: String? = null,
) : AgentPayload

@Serializable
data class CartActionPayload(
    val action: String = "",
    @SerialName("product_id") val productId: String = "",
    @SerialName("cart_id") val cartId: String? = null,
    val quantity: Int = 1,
    val status: String = "",
    val cart: CartSummaryPayload? = null,
) : AgentPayload

@Serializable
data class DonePayload(
    @SerialName("criteria_id") val criteriaId: String? = null,
    @SerialName("deck_id") val deckId: String? = null,
    @SerialName("total_products") val totalProducts: Int? = null,
    @SerialName("client_turn_id") val clientTurnId: String? = null,
    @SerialName("finish_reason") val finishReason: String = "completed",
) : AgentPayload

@Serializable
data class ErrorPayload(
    val code: String = "",
    val message: String = "",
    val retryable: Boolean = true,
    @SerialName("recover_action") val recoverAction: String? = null,
) : AgentPayload

@Serializable
data class UnknownPayload(
    val raw: JsonObject,
) : AgentPayload
