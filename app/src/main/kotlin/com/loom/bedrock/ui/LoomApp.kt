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
    const val CHAT = "chat"
    const val CHAT_WITH_ID = "chat/{conversationId}"
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
                        navController.navigate("chat/$conversationId")
                    } else {
                        navController.navigate(Routes.CHAT)
                    }
                }
            )
        }
        
        // New chat (no conversation ID)
        composable(Routes.CHAT) {
            ChatScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToTree = { /* TODO: Tree view */ }
            )
        }
        
        // Existing chat (with conversation ID)
        composable(
            route = Routes.CHAT_WITH_ID,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            ChatScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToTree = { navController.navigate("tree/$conversationId") }
            )
        }
        
        // Tree visualization
        composable(
            route = Routes.TREE,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) {
            TreeScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNode = { nodeId ->
                    // Navigate back to chat with selected node
                    navController.popBackStack()
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
