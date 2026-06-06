package com.buypilot.feature.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buypilot.feature.chat.ui.BuyPilotChatScreen

@Composable
fun ChatRoute(
    viewModel: ChatViewModel = hiltViewModel(),
    onOpenProductDeck: (String, String?, String?) -> Unit = { _, _, _ -> },
    onOpenProductDetail: (String, String, String?) -> Unit = { _, _, _ -> },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val timelinePresentation by viewModel.timelinePresentationState.collectAsStateWithLifecycle()

    BuyPilotChatScreen(
        state = uiState,
        timelinePresentation = timelinePresentation,
        onInputChanged = viewModel::onInputChanged,
        onSendMessage = viewModel::sendMessage,
        onImageSelected = viewModel::selectImage,
        onImageCaptured = viewModel::captureImage,
        onClearImageAttachment = viewModel::clearImageAttachment,
        onCriteriaPatch = viewModel::sendCriteriaPatch,
        onCancel = viewModel::cancel,
        onQuickAction = viewModel::handleQuickAction,
        onCompareProducts = viewModel::compareProductsByRank,
        onCartOpen = viewModel::refreshCart,
        onCartSheetRequestHandled = viewModel::consumeCartSheetRequest,
        onCartQuantityChange = viewModel::updateCartQuantity,
        onOpenProductDeck = onOpenProductDeck,
        onOpenProductDetail = onOpenProductDetail,
        onRetryLastMessage = viewModel::retryLastMessage,
        onEditLastMessage = viewModel::editLastMessage,
        onClearConversation = viewModel::clearConversation,
        onConvergeProductDeck = { deckId ->
            viewModel.convergeProductDeck(deckId, allowFullyHandled = true)
        },
        ttsEnabled = uiState.ttsEnabled,
        onTtsToggle = viewModel::toggleTts,
    )
}
