package com.buypilot.core.common.sse

data class SseFrame(
    val event: String?,
    val data: String,
    val id: String? = null,
)
