package com.loom.bedrock.ui

import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.loom.bedrock.ui.screens.ChatScreen
import com.loom.bedrock.ui.screens.ConversationListScreen
import com.loom.bedrock.ui.screens.SettingsScreen
import com.loom.bedrock.ui.screens.TreeScreen

/**
 * Navigation routes for the app.
 */
object Routes {
    const val CONVERSATIONS = "conversations"
    const val CHAT = "chat?conversationId={conversationId}"
    const val TREE = "tree/{conversationId}"
    const val SETTINGS = "settings"
}

/**
 * Main app entry point composable with navigation.
 */
@Composable
fun LoomApp() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Routes.CONVERSATIONS
    ) {
        composable(Routes.CONVERSATIONS) {
            ConversationListScreen(
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToChat = { conversationId ->
                    if (conversationId != null) {
                        navController.navigate("chat?conversationId=$conversationId")
                    } else {
                        navController.navigate("chat")
                    }
                }
            )
        }

        // Chat screen (handles both new and existing conversations)
        composable(
            route = Routes.CHAT,
            arguments = listOf(
                navArgument("conversationId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            ChatScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToTree = { conversationId ->
                    navController.navigate("tree/$conversationId")
                }
            )
        }
        
        // Tree visualization
        composable(
            route = Routes.TREE,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            TreeScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNode = { nodeId ->
                    // Navigate back to chat and force reload
                    navController.navigate("chat?conversationId=$conversationId") {
                        popUpTo("chat?conversationId=$conversationId") { inclusive = true }
                    }
                }
            )
        }
        
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
