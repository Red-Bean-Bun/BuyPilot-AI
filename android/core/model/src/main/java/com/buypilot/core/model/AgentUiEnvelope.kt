package com.buypilot.core.model

data class AgentUiEnvelope<out T : AgentPayload>(
    val schemaVersion: String = "2026-05-20",
    val event: AgentEventType,
    val sessionId: String?,
    val turnId: String,
    val seq: Int,
    val eventId: String,
    val nodeId: String,
    val deckId: String? = null,
    val displayMode: String? = null,
    val createdAtMs: Long? = null,
    val payload: T,
)
