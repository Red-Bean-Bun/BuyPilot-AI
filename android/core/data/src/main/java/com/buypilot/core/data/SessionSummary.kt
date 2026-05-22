package com.buypilot.core.data

data class SessionSummary(
    val sessionId: String,
    val title: String,
    val lastMessage: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)
