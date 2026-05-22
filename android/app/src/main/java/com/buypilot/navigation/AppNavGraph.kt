package com.buypilot.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.buypilot.feature.chat.ChatRoute

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.Chat,
    ) {
        composable(Routes.Chat) {
            ChatRoute()
        }
    }
}

object Routes {
    const val Chat = "chat"
}
