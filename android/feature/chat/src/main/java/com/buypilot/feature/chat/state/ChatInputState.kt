package com.buypilot.feature.chat.state

sealed interface ChatInputState {
    data object Idle : ChatInputState
    data object Composing : ChatInputState
    data object ImageAttached : ChatInputState
    data object Streaming : ChatInputState
    data object Canceled : ChatInputState
    data object Error : ChatInputState
    data object Clarifying : ChatInputState
}
