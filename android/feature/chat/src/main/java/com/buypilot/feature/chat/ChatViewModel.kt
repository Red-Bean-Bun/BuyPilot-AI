package com.buypilot.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buypilot.core.common.id.Ids
import com.buypilot.core.data.ChatRepository
import com.buypilot.core.model.requests.ChatStreamRequest
import com.buypilot.feature.chat.state.ChatReducer
import com.buypilot.feature.chat.state.ChatUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null

    fun sendMessage(message: String, imageUrl: String? = null) {
        if (message.isBlank() && imageUrl == null) return

        val nowMs = System.currentTimeMillis()
        val clientTurnId = Ids.clientTurnId()
        val currentSessionId = _uiState.value.sessionId
        var userMessageRecorded = currentSessionId != null
        _uiState.update {
            ChatReducer.addUserMessage(
                state = it,
                key = Ids.userMessageId(nowMs),
                content = message,
                imageUrl = imageUrl,
            )
        }

        viewModelScope.launch {
            if (currentSessionId != null) {
                chatRepository.recordUserMessage(
                    sessionId = currentSessionId,
                    content = message,
                    nowMs = nowMs,
                )
            }
        }

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            val request = ChatStreamRequest(
                message = message,
                sessionId = currentSessionId,
                imageUrl = imageUrl,
                clientTurnId = clientTurnId,
                clientTraceId = Ids.clientTraceId(),
            )

            chatRepository.streamChat(request)
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isStreaming = false,
                            lastError = throwable.message ?: "流式连接失败",
                        )
                    }
                }
                .collect { envelope ->
                    val sessionId = envelope.sessionId
                    if (!userMessageRecorded && sessionId != null) {
                        userMessageRecorded = true
                        chatRepository.recordUserMessage(
                            sessionId = sessionId,
                            content = message,
                            nowMs = nowMs,
                        )
                    }
                    _uiState.update { ChatReducer.reduce(it, envelope) }
                }
        }
    }

    fun cancel() {
        val state = _uiState.value
        streamJob?.cancel()
        _uiState.update(ChatReducer::cancel)

        val sessionId = state.sessionId
        val turnId = state.currentTurnId
        if (sessionId != null && turnId != null) {
            viewModelScope.launch {
                runCatching { chatRepository.cancel(sessionId, turnId) }
            }
        }
    }

    fun onInputChanged(text: String, hasImage: Boolean = false) {
        _uiState.update { ChatReducer.markComposing(it, text.isNotBlank(), hasImage) }
    }
}
