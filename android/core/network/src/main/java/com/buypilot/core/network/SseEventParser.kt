package com.buypilot.core.network

import com.buypilot.core.common.json.AppJson
import com.buypilot.core.common.sse.SseFrame
import com.buypilot.core.model.AgentEventType
import com.buypilot.core.model.AgentPayload
import com.buypilot.core.model.AgentUiEnvelope
import com.buypilot.core.model.CartActionPayload
import com.buypilot.core.model.ClarificationPayload
import com.buypilot.core.model.CriteriaCardPayload
import com.buypilot.core.model.DonePayload
import com.buypilot.core.model.ErrorPayload
import com.buypilot.core.model.FinalDecisionPayload
import com.buypilot.core.model.ProductCardPayload
import com.buypilot.core.model.TextDeltaPayload
import com.buypilot.core.model.ThinkingPayload
import com.buypilot.core.model.UnknownPayload
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class SseEventParser @Inject constructor(
    private val json: Json,
) {
    fun parse(frame: SseFrame, fallbackSeq: Int): AgentUiEnvelope<AgentPayload> {
        val root = json.parseToJsonElement(frame.data).jsonObject
        val eventValue = root.string("event") ?: frame.event ?: AgentEventType.Unknown.wireValue
        val event = AgentEventType.fromWireValue(eventValue)
        val payloadSource = root["payload"]?.jsonObject ?: root
        val sessionId = root.string("session_id")
        val turnId = root.string("turn_id") ?: "turn_${sessionId ?: "unknown"}"
        val seq = root.int("seq") ?: fallbackSeq
        val deckId = root.string("deck_id")
            ?: payloadSource.string("deck_id")
            ?: fallbackDeckId(event, turnId)
        val nodeId = root.string("node_id") ?: fallbackNodeId(event, turnId, deckId, payloadSource)
        val eventId = root.string("event_id") ?: frame.id ?: "$turnId:$seq"
        val payload = decodePayload(event, payloadSource)

        return AgentUiEnvelope(
            schemaVersion = root.string("schema_version") ?: "2026-05-20",
            event = event,
            sessionId = sessionId,
            turnId = turnId,
            seq = seq,
            eventId = eventId,
            nodeId = nodeId,
            deckId = deckId,
            displayMode = root.string("display_mode") ?: fallbackDisplayMode(event),
            createdAtMs = root.long("created_at_ms"),
            payload = payload,
        )
    }

    private fun decodePayload(event: AgentEventType, source: JsonObject): AgentPayload {
        return when (event) {
            AgentEventType.Thinking -> json.decodeFromJsonElement<ThinkingPayload>(source)
            AgentEventType.Clarification -> json.decodeFromJsonElement<ClarificationPayload>(source)
            AgentEventType.CriteriaCard -> json.decodeFromJsonElement<CriteriaCardPayload>(source)
            AgentEventType.TextDelta -> json.decodeFromJsonElement<TextDeltaPayload>(source)
            AgentEventType.ProductCard -> json.decodeFromJsonElement<ProductCardPayload>(source)
            AgentEventType.CartAction -> json.decodeFromJsonElement<CartActionPayload>(source)
            AgentEventType.FinalDecision -> json.decodeFromJsonElement<FinalDecisionPayload>(source)
            AgentEventType.Done -> json.decodeFromJsonElement<DonePayload>(source)
            AgentEventType.Error -> json.decodeFromJsonElement<ErrorPayload>(source)
            AgentEventType.Unknown -> UnknownPayload(source)
        }
    }

    private fun fallbackNodeId(
        event: AgentEventType,
        turnId: String,
        deckId: String?,
        source: JsonObject,
    ): String {
        return when (event) {
            AgentEventType.Thinking -> "thinking_$turnId"
            AgentEventType.Clarification -> "clarify_$turnId"
            AgentEventType.CriteriaCard -> {
                val criteriaId = source.jsonObject("criteria")?.string("criteria_id")
                    ?: source.string("criteria_id")
                    ?: turnId
                "criteria_$criteriaId"
            }
            AgentEventType.TextDelta -> "ai_text_$turnId"
            AgentEventType.ProductCard -> deckId ?: "deck_$turnId"
            AgentEventType.CartAction -> "cart_$turnId"
            AgentEventType.FinalDecision -> "decision_$turnId"
            AgentEventType.Done -> "done_$turnId"
            AgentEventType.Error -> "error_$turnId"
            AgentEventType.Unknown -> "unknown_$turnId"
        }
    }

    private fun fallbackDeckId(event: AgentEventType, turnId: String): String? {
        return when (event) {
            AgentEventType.ProductCard -> "deck_$turnId"
            else -> null
        }
    }

    private fun fallbackDisplayMode(event: AgentEventType): String {
        return when (event) {
            AgentEventType.Thinking -> "inline_thinking"
            AgentEventType.Clarification -> "inline_card"
            AgentEventType.CriteriaCard -> "summary_card"
            AgentEventType.TextDelta -> "inline_text"
            AgentEventType.ProductCard -> "swipe_deck_item"
            AgentEventType.CartAction -> "inline_card"
            AgentEventType.FinalDecision -> "summary_card"
            AgentEventType.Done -> "none"
            AgentEventType.Error -> "inline_card"
            AgentEventType.Unknown -> "none"
        }
    }
}

private fun JsonObject.string(name: String): String? =
    this[name]?.jsonPrimitive?.contentOrNull

private fun JsonObject.int(name: String): Int? =
    this[name]?.jsonPrimitive?.intOrNull

private fun JsonObject.long(name: String): Long? =
    this[name]?.jsonPrimitive?.longOrNull

private fun JsonObject.jsonObject(name: String): JsonObject? =
    (this[name] as? JsonObject)
