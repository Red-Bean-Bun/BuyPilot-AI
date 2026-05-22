package com.buypilot.feature.chat

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ChatRoute(
    viewModel: ChatViewModel = hiltViewModel(),
) {
    Text("BuyPilot chat skeleton")
}
