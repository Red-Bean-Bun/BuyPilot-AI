package com.buypilot.core.data

data class PersistedChatMessage(
    val messageId: String,
    val sessionId: String,
    val turnId: String?,
    val role: String,
    val content: String,
    val nodeType: String? = null,
    val payloadJson: String? = null,
    val deckId: String? = null,
    val createdAtMs: Long,
)
