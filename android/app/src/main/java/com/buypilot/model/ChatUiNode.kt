package com.buypilot.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * ChatUiNode - 所有聊天时间线元素的统一抽象
 * 使用 sealed interface 保证编译期穷尽检查
 * key 字段映射 SSE envelope 的 node_id，用于 LazyColumn upsert diff
 */
sealed interface ChatUiNode {
    val key: String  // 来自 SSE node_id，稳定 key
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
    override val key: String = "clarification_default",
    val question: String,
    val suggestedOptions: List<String> = emptyList()
) : ChatUiNode

@Serializable
data class CriteriaNode(
    override val key: String = "criteria_default",
    val criteria: CriteriaPayload,
    val quickActions: List<QuickActionPayload> = emptyList()
) : ChatUiNode

@Serializable
data class ProductNode(
    override val key: String = "product_default",
    val deckId: String = "",        // 来自 SSE deck_id，SwipeDeck 聚合用
    val rank: Int = 0,
    val product: ProductPayload,
    val reason: String,
    val riskNotes: List<String> = emptyList(),
    val evidence: List<EvidencePayload> = emptyList(),
    val actions: List<QuickActionPayload> = emptyList()
) : ChatUiNode

@Serializable
data class FinalDecisionNode(
    override val key: String = "decision_default",
    val winnerProductId: String = "",
    val summary: String,
    val why: List<String> = emptyList(),
    val notFor: List<String> = emptyList(),
    val alternatives: List<AlternativePayload> = emptyList(),
    val nextActions: List<QuickActionPayload> = emptyList()
) : ChatUiNode

@Serializable
data class CartActionNode(
    override val key: String = "cart_default",
    val action: String,
    val productId: String,
    val quantity: Int = 1,
    val status: String
) : ChatUiNode

@Serializable
data class ErrorNode(
    override val key: String = "error_default",
    val code: String,
    val message: String,
    val retryable: Boolean = true
) : ChatUiNode

// ===== SSE Envelope =====

@Serializable
data class SSEEnvelope(
    @SerialName("schema_version") val schemaVersion: String = "2026-05-20",
    val event: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("turn_id") val turnId: String,
    val seq: Int,
    @SerialName("event_id") val eventId: String,
    @SerialName("node_id") val nodeId: String,
    @SerialName("deck_id") val deckId: String? = null,
    @SerialName("display_mode") val displayMode: String? = null,
    @SerialName("created_at_ms") val createdAtMs: Long? = null
)

// ===== 数据契约 =====

@Serializable
data class Constraints(
    // 通用字段
    @SerialName("budget_min") val budgetMin: Double? = null,
    @SerialName("budget_max") val budgetMax: Double? = null,
    @SerialName("use_scenario") val useScenario: String? = null,
    // 美妆护肤专属
    @SerialName("skin_type") val skinType: String? = null,
    @SerialName("ingredient_avoid") val ingredientAvoid: List<String> = emptyList(),
    @SerialName("ingredient_prefer") val ingredientPrefer: List<String> = emptyList(),
    // 数码电子专属
    val storage: String? = null,
    @SerialName("screen_size") val screenSize: String? = null,
    // 服饰运动专属
    @SerialName("sport_type") val sportType: String? = null,
    val season: String? = null,
    // 食品生活专属
    val dietary: List<String> = emptyList()
)

@Serializable
data class CriteriaPayload(
    @SerialName("criteria_id") val criteriaId: String = "",
    val category: String = "",
    val summary: String = "",
    val chips: List<String> = emptyList(),
    val constraints: Constraints = Constraints()
)

@Serializable
data class ProductPayload(
    @SerialName("product_id") val productId: String,
    val name: String,
    val price: Double? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val category: String = "",
    @SerialName("sub_category") val subCategory: String? = null,
    val brand: String? = null,
    @SerialName("skin_type_match") val skinTypeMatch: List<String> = emptyList(),
    @SerialName("ingredient_tags") val ingredientTags: List<String> = emptyList(),
    @SerialName("ingredient_avoid") val ingredientAvoid: List<String> = emptyList(),
    @SerialName("use_scenario") val useScenario: String? = null
)

@Serializable
data class EvidencePayload(
    @SerialName("source_type") val sourceType: String,
    val snippet: String,
    @SerialName("source_id") val sourceId: String? = null
)

@Serializable
data class AlternativePayload(
    @SerialName("product_id") val productId: String,
    val name: String
)

@Serializable
data class QuickActionPayload(
    @SerialName("action_id") val actionId: String,
    val label: String,
    val action: String,
    @SerialName("feedback_type") val feedbackType: String? = null,
    @SerialName("criteria_patch") val criteriaPatch: JsonObject? = null
)