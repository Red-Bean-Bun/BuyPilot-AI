package com.buypilot.feature.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.buypilot.feature.chat.ui.BuyPilotChatScreen

@Composable
fun ChatRoute(
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    BuyPilotChatScreen(
        state = uiState,
        onInputChanged = viewModel::onInputChanged,
        onSendMessage = viewModel::sendMessage,
        onCancel = viewModel::cancel,
    )
}
