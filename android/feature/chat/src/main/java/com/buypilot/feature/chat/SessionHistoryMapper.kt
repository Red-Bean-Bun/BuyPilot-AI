package com.buypilot.feature.chat

import com.buypilot.core.model.AlternativePayload
import com.buypilot.core.model.CriteriaCardPayload
import com.buypilot.core.model.FinalDecisionPayload
import com.buypilot.core.model.ProductCardPayload
import com.buypilot.core.model.responses.HistoryDecision
import com.buypilot.core.model.responses.HistoryProductCard
import com.buypilot.core.model.responses.SessionHistoryResponse
import kotlinx.serialization.json.JsonPrimitive
import com.buypilot.feature.chat.model.AiStreamNode
import com.buypilot.feature.chat.model.CriteriaNode
import com.buypilot.feature.chat.model.FinalDecisionNode
import com.buypilot.feature.chat.model.ProductDeckNode
import com.buypilot.feature.chat.model.UserMessageNode
import com.buypilot.feature.chat.state.ChatUiState

internal fun mapToChatUiState(
    history: SessionHistoryResponse,
    backendBaseUrl: String,
    ttsEnabled: Boolean,
): ChatUiState {
    val nodes = mutableListOf<com.buypilot.feature.chat.model.ChatUiNode>()
    var lastDeckId: String? = null
    var turnCounter = 0

    for (turn in history.turns) {
        turnCounter++
        val turnStr = turnCounter.toString()

        // 1) User message
        nodes.add(
            UserMessageNode(
                key = "restored_user_$turnStr",
                content = turn.userMessage.ifBlank { "..." },
            ),
        )

        // 2) AI turn text (if any)
        val turnText = turn.turnText
        if (!turnText.isNullOrBlank()) {
            nodes.add(
                AiStreamNode(
                    key = "restored_text_$turnStr",
                    messageId = "restored_text_$turnStr",
                    content = turnText,
                    done = true,
                ),
            )
        }

        // 3) Product cards (if any)
        val products = turn.products.orEmpty()
        if (products.isNotEmpty()) {
            val productCards = products.map { it.toProductCardPayload() }
            val deckId = turn.deckId ?: "restored_deck_$turnStr"
            lastDeckId = deckId
            nodes.add(
                ProductDeckNode(
                    key = "restored_deck_$turnStr",
                    deckId = deckId,
                    products = productCards,
                ),
            )
        }

        // 4) Criteria (if any)
        val criteria = turn.criteria
        if (criteria != null) {
            nodes.add(
                CriteriaNode(
                    key = "restored_criteria_$turnStr",
                    payload = CriteriaCardPayload(
                        editable = false,
                        criteria = criteria,
                    ),
                ),
            )
        }

        // 5) Decision (if any)
        val decision = turn.decision
        if (decision != null) {
            nodes.add(
                FinalDecisionNode(
                    key = "restored_decision_$turnStr",
                    payload = decision.toFinalDecisionPayload(),
                    deckId = turn.deckId,
                ),
            )
        }
    }

    return ChatUiState(
        sessionId = history.sessionId,
        nodes = nodes,
        awaitingConvergenceDeckIds = setOfNotNull(lastDeckId),
        latestConvergeableDeckId = lastDeckId,
        backendBaseUrl = backendBaseUrl,
        ttsEnabled = ttsEnabled,
    )
}

private fun HistoryProductCard.toProductCardPayload(): ProductCardPayload =
    ProductCardPayload(
        rank = rank,
        product = product,
        reason = reason,
        reasonAtoms = emptyList(),
        riskNotes = riskNotes,
        evidence = evidence,
        actions = actions,
    )

private fun HistoryDecision.toFinalDecisionPayload(): FinalDecisionPayload =
    FinalDecisionPayload(
        winnerProductId = winnerProductId,
        summary = summary,
        why = why,
        notFor = notFor,
        alternatives = alternatives?.mapNotNull {
            val id = (it["product_id"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            val name = (it["name"] as? JsonPrimitive)?.contentOrNull ?: ""
            AlternativePayload(productId = id, name = name)
        } ?: emptyList(),
        nextActions = nextActions ?: emptyList(),
        decisionStatus = decisionStatus,
        confidence = confidence?.toString(),
        nextStep = nextStep,
    )
