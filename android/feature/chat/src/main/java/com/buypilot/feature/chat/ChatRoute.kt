package com.buypilot.feature.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buypilot.feature.chat.ui.BuyPilotChatScreen

@Composable
fun ChatRoute(
    viewModel: ChatViewModel = hiltViewModel(),
    onOpenProductDeck: (String, String?) -> Unit = { _, _ -> },
    onOpenProductDetail: (String, String) -> Unit = { _, _ -> },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BuyPilotChatScreen(
        state = uiState,
        onInputChanged = viewModel::onInputChanged,
        onSendMessage = viewModel::sendMessage,
        onCriteriaPatch = viewModel::sendCriteriaPatch,
        onCancel = viewModel::cancel,
        onOpenProductDeck = onOpenProductDeck,
        onOpenProductDetail = onOpenProductDetail,
        onRetryLastMessage = viewModel::retryLastMessage,
        onEditLastMessage = viewModel::editLastMessage,
        onClearConversation = viewModel::clearConversation,
        onConvergeProductDeck = { deckId ->
            viewModel.convergeProductDeck(deckId, allowFullyHandled = true)
        },
    )
}
