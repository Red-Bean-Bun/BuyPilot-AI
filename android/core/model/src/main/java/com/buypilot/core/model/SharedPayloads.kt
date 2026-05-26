package com.buypilot.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Constraints(
    @SerialName("budget_min") val budgetMin: Double? = null,
    @SerialName("budget_max") val budgetMax: Double? = null,
    @SerialName("use_scenario") val useScenario: String? = null,
    @SerialName("brand_avoid") val brandAvoid: List<String> = emptyList(),
    @SerialName("origin_avoid") val originAvoid: List<String> = emptyList(),
    @SerialName("product_type") val productType: String? = null,
    @SerialName("skin_type") val skinType: String? = null,
    @SerialName("ingredient_avoid") val ingredientAvoid: List<String> = emptyList(),
    @SerialName("ingredient_prefer") val ingredientPrefer: List<String> = emptyList(),
    val storage: String? = null,
    @SerialName("screen_size") val screenSize: String? = null,
    @SerialName("sport_type") val sportType: String? = null,
    val season: String? = null,
    val dietary: List<String> = emptyList(),
)

@Serializable
data class EvidencePayload(
    @SerialName("evidence_id") val evidenceId: String? = null,
    @SerialName("source_type") val sourceType: String = "",
    @SerialName("trust_label") val trustLabel: String? = null,
    val snippet: String = "",
    @SerialName("source_id") val sourceId: String? = null,
)

@Serializable
data class AlternativePayload(
    @SerialName("product_id") val productId: String,
    val name: String,
)

@Serializable
data class QuickActionPayload(
    @SerialName("action_id") val actionId: String = "",
    val label: String = "",
    val action: String = "",
    @SerialName("feedback_type") val feedbackType: String? = null,
    @SerialName("criteria_patch") val criteriaPatch: JsonObject? = null,
)
