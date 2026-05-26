package com.buypilot.feature.chat.model

import com.buypilot.core.model.AlternativePayload
import com.buypilot.core.model.CartActionPayload
import com.buypilot.core.model.ClarificationPayload
import com.buypilot.core.model.CriteriaCardPayload
import com.buypilot.core.model.EvidencePayload
import com.buypilot.core.model.FinalDecisionPayload
import com.buypilot.core.model.ProductCardPayload
import com.buypilot.core.model.QuickActionPayload
import com.buypilot.core.model.TextDeltaPayload
import com.buypilot.core.model.ThinkingPayload

sealed interface ChatUiNode {
    val key: String
}

data class UserMessageNode(
    override val key: String,
    val content: String,
    val imageUrl: String? = null,
) : ChatUiNode

data class ThinkingNode(
    override val key: String,
    val payload: ThinkingPayload,
    val turnId: String = "",
) : ChatUiNode

data class AiStreamNode(
    override val key: String,
    val messageId: String,
    val content: String,
    val done: Boolean,
) : ChatUiNode

data class ClarificationNode(
    override val key: String,
    val payload: ClarificationPayload,
    val turnId: String = "",
    val anchorMessageKey: String = "",
) : ChatUiNode

data class CriteriaNode(
    override val key: String,
    val payload: CriteriaCardPayload,
) : ChatUiNode

data class ProductDeckNode(
    override val key: String,
    val deckId: String,
    val products: List<ProductCardPayload>,
) : ChatUiNode

data class FinalDecisionNode(
    override val key: String,
    val payload: FinalDecisionPayload,
) : ChatUiNode {
    val alternatives: List<AlternativePayload> = payload.alternatives
    val nextActions: List<QuickActionPayload> = payload.nextActions
}

data class CartActionNode(
    override val key: String,
    val payload: CartActionPayload,
) : ChatUiNode

data class ErrorNode(
    override val key: String,
    val code: String,
    val message: String,
    val retryable: Boolean,
) : ChatUiNode

data class EvidenceBundle(
    val refs: List<EvidencePayload>,
)
