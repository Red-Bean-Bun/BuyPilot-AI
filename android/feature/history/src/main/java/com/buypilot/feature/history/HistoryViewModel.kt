package com.buypilot.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buypilot.core.data.SessionRepository
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
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.observeSessions()
                .catch { throwable ->
                    _uiState.update { it.copy(error = throwable.message) }
                }
                .collect { sessions ->
                    _uiState.update { it.copy(sessions = sessions, error = null) }
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
}
