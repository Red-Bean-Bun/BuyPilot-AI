package com.buypilot.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * ChatUiNode - 所有聊天时间线元素的统一抽象
 * 使用 sealed interface 保证编译期穷尽检查
 */
sealed interface ChatUiNode {
    val key: String  // 稳定 key，用于 LazyColumn diff
}

@Serializable
data class UserMessageNode(
    override val key: String = "user_${System.currentTimeMillis()}",
    val content: String
) : ChatUiNode

@Serializable
data class AiStreamNode(
    override val key: String = "ai_stream",
    val messageId: String = "",
    val content: String = ""
) : ChatUiNode

@Serializable
data class ClarificationNode(
    override val key: String = "clarification_${System.currentTimeMillis()}",
    val question: String,
    val suggestedOptions: List<String> = emptyList()
) : ChatUiNode

@Serializable
data class CriteriaNode(
    override val key: String = "criteria_${System.currentTimeMillis()}",
    val criteria: CriteriaPayload,
    val quickActions: List<QuickActionPayload> = emptyList()
) : ChatUiNode

@Serializable
data class ProductNode(
    override val key: String = "product_${System.currentTimeMillis()}",
    val product: ProductPayload,
    val reason: String,
    val riskNotes: List<String> = emptyList(),
    val actions: List<QuickActionPayload> = emptyList()
) : ChatUiNode

@Serializable
data class FinalDecisionNode(
    override val key: String = "decision_${System.currentTimeMillis()}",
    val summary: String,
    val why: List<String> = emptyList(),
    val notFor: List<String> = emptyList(),
    val alternatives: List<String> = emptyList()
) : ChatUiNode

@Serializable
data class ErrorNode(
    override val key: String = "error_${System.currentTimeMillis()}",
    val code: String,
    val message: String
) : ChatUiNode

// ===== 数据契约 =====

@Serializable
data class CriteriaPayload(
    val age: Int? = null,
    val scenario: String? = null,
    @SerialName("budget_min") val budgetMin: Int? = null,
    @SerialName("budget_max") val budgetMax: Int? = null,
    @SerialName("toy_type") val toyType: String? = null,
    @SerialName("education_dimensions") val educationDimensions: List<String> = emptyList(),
    @SerialName("safety_features") val safetyFeatures: List<String> = emptyList(),
    @SerialName("requires_battery") val requiresBattery: Boolean? = null
)

@Serializable
data class ProductPayload(
    @SerialName("product_id") val productId: String,
    val name: String,
    val price: Int? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("age_min") val ageMin: Int? = null,
    @SerialName("age_max") val ageMax: Int? = null,
    @SerialName("toy_type") val toyType: String? = null,
    @SerialName("education_dimensions") val educationDimensions: List<String> = emptyList(),
    @SerialName("safety_features") val safetyFeatures: List<String> = emptyList(),
    @SerialName("requires_battery") val requiresBattery: Boolean? = null
)

@Serializable
data class QuickActionPayload(
    @SerialName("action_id") val actionId: String,
    val label: String,
    val action: String,
    @SerialName("criteria_patch") val criteriaPatch: JsonObject? = null
)
