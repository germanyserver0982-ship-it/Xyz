package com.zoya.assistant.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zoya.assistant.ui.screens.ChatScreen
import com.zoya.assistant.ui.screens.HomeScreen
import com.zoya.assistant.ui.screens.SettingsScreen
import com.zoya.assistant.ui.screens.VoiceScreen
import com.zoya.assistant.viewmodel.AssistantViewModel

private object Routes {
    const val HOME = "home"
    const val VOICE = "voice"
    const val CHAT = "chat"
    const val SETTINGS = "settings"
}

@Composable
fun ZoyaNavHost(viewModel: AssistantViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onOpenChat = { navController.navigate(Routes.CHAT) },
                onOpenVoice = { navController.navigate(Routes.VOICE) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.VOICE) {
            VoiceScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenChat = { navController.navigate(Routes.CHAT) }
            )
        }
        composable(Routes.CHAT) {
            ChatScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenVoice = { navController.navigate(Routes.VOICE) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
