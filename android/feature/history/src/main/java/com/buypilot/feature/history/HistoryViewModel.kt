package com.buypilot.feature.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buypilot.core.data.SessionSummary
import com.buypilot.core.data.SessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    @ApplicationContext appContext: Context,
) : ViewModel() {
    private val pinPreferences = appContext.getSharedPreferences(PIN_PREFS_NAME, Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(HistoryUiState(pinnedSessionIds = readPinnedSessionIds()))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.observeSessions()
                .catch { throwable ->
                    _uiState.update { it.copy(error = throwable.message) }
                }
                .collect { sessions ->
                    _uiState.update {
                        it.copy(
                            sessions = sessions.sortedByPinned(it.pinnedSessionIds),
                            error = null,
                        )
                    }
                }
        }
    }

    fun restore(sessionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(restoringSessionId = sessionId) }
            val count = runCatching { sessionRepository.restoreMessages(sessionId).size }
                .getOrElse { throwable ->
                    _uiState.update { it.copy(error = throwable.message, restoringSessionId = null) }
                    return@launch
                }
            _uiState.update {
                it.copy(
                    restoringSessionId = null,
                    restoredMessageCount = count,
                    error = null,
                )
            }
        }
    }

    fun deleteSession(sessionId: String) {
        if (sessionId.isBlank()) return
        viewModelScope.launch {
            runCatching { sessionRepository.deleteSession(sessionId) }
                .onSuccess {
                    updatePinnedSessionIds(_uiState.value.pinnedSessionIds - sessionId)
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(error = throwable.message) }
                }
        }
    }

    fun togglePinnedSession(sessionId: String) {
        if (sessionId.isBlank()) return
        val currentPinnedIds = _uiState.value.pinnedSessionIds
        val nextPinnedIds = if (sessionId in currentPinnedIds) {
            currentPinnedIds - sessionId
        } else {
            currentPinnedIds + sessionId
        }
        updatePinnedSessionIds(nextPinnedIds)
    }

    private fun updatePinnedSessionIds(pinnedSessionIds: Set<String>) {
        pinPreferences.edit()
            .putStringSet(PIN_PREFS_KEY, pinnedSessionIds)
            .apply()
        _uiState.update {
            it.copy(
                pinnedSessionIds = pinnedSessionIds,
                sessions = it.sessions.sortedByPinned(pinnedSessionIds),
            )
        }
    }

    private fun readPinnedSessionIds(): Set<String> =
        pinPreferences.getStringSet(PIN_PREFS_KEY, emptySet()).orEmpty()
            .filter { it.isNotBlank() }
            .toSet()

    private companion object {
        const val PIN_PREFS_NAME = "history_pins"
        const val PIN_PREFS_KEY = "pinned_session_ids"
    }
}

private fun List<SessionSummary>.sortedByPinned(pinnedSessionIds: Set<String>): List<SessionSummary> {
    if (pinnedSessionIds.isEmpty()) return this
    val (pinned, unpinned) = partition { it.sessionId in pinnedSessionIds }
    return pinned + unpinned
}
