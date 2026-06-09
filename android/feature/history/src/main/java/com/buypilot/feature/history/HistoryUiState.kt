package com.buypilot.feature.history

import com.buypilot.core.data.SessionSummary

data class HistoryUiState(
    val sessions: List<SessionSummary> = emptyList(),
    val pinnedSessionIds: Set<String> = emptySet(),
    val restoringSessionId: String? = null,
    val restoredMessageCount: Int = 0,
    val error: String? = null,
)
