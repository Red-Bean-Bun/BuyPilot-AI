package com.buypilot.core.data

data class PersistedChatMessage(
    val messageId: String,
    val sessionId: String,
    val turnId: String?,
    val role: String,
    val content: String,
    val createdAtMs: Long,
)
