package com.buypilot.feature.chat.ui

import androidx.compose.runtime.saveable.Saver
import com.buypilot.core.model.CriteriaCardPayload
import com.buypilot.core.model.FinalDecisionPayload
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal sealed interface ChatSheetContent {
    data class Criteria(val payload: CriteriaCardPayload) : ChatSheetContent
    data class DecisionEvidence(
        val payload: FinalDecisionPayload,
        val sourceNodeKey: String = "",
    ) : ChatSheetContent
    data class ProductCompare(
        val sourceDeckNodeKey: String = "",
    ) : ChatSheetContent
    data object Cart : ChatSheetContent
}

@Serializable
private data class SavedDecisionEvidenceSheet(
    val payload: FinalDecisionPayload,
    val sourceNodeKey: String = "",
)

private val ChatSheetJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private const val SavedSheetNone = "none"

internal val ChatSheetContentSaver = Saver<ChatSheetContent?, String>(
    save = { content ->
        when (content) {
            null -> SavedSheetNone
            ChatSheetContent.Cart -> "cart"
            is ChatSheetContent.Criteria -> "criteria:${ChatSheetJson.encodeToString(content.payload)}"
            is ChatSheetContent.ProductCompare -> "compare:${content.sourceDeckNodeKey}"
            is ChatSheetContent.DecisionEvidence -> "decision:${ChatSheetJson.encodeToString(
                SavedDecisionEvidenceSheet(
                    payload = content.payload,
                    sourceNodeKey = content.sourceNodeKey,
                ),
            )}"
        }
    },
    restore = { saved ->
        when {
            saved == SavedSheetNone -> null
            saved == "cart" -> ChatSheetContent.Cart
            saved.startsWith("criteria:") -> runCatching {
                ChatSheetContent.Criteria(ChatSheetJson.decodeFromString(saved.removePrefix("criteria:")))
            }.getOrNull()
            saved.startsWith("compare:") -> ChatSheetContent.ProductCompare(saved.removePrefix("compare:"))
            saved.startsWith("decision:") -> runCatching {
                val payload = saved.removePrefix("decision:")
                val sheet = ChatSheetJson.decodeFromString<SavedDecisionEvidenceSheet>(payload)
                ChatSheetContent.DecisionEvidence(
                    payload = sheet.payload,
                    sourceNodeKey = sheet.sourceNodeKey,
                )
            }.getOrElse {
                runCatching {
                    ChatSheetContent.DecisionEvidence(
                        ChatSheetJson.decodeFromString(saved.removePrefix("decision:")),
                    )
                }.getOrNull()
            }
            else -> null
        }
    },
)

internal val StringSetSaver = Saver<Set<String>, ArrayList<String>>(
    save = { ArrayList(it) },
    restore = { it.toSet() },
)
