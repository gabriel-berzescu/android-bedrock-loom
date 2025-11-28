package com.loom.bedrock.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.loom.bedrock.ui.screens.ChatScreen
import com.loom.bedrock.ui.screens.SettingsScreen
import com.loom.bedrock.ui.theme.LoomTheme

/**
 * Navigation routes for the app.
 */
object Routes {
    const val HOME = "home"
    const val CHAT = "chat"
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
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onStartChat = { navController.navigate(Routes.CHAT) }
            )
        }
        
        composable(Routes.CHAT) {
            ChatScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToTree = { /* TODO: Tree view */ }
            )
        }
        
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * Home screen with welcome message and settings access.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onStartChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bedrock Loom") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        WelcomeScreen(
            modifier = Modifier.padding(paddingValues),
            onStartChat = onStartChat,
            onOpenSettings = onNavigateToSettings
        )
    }
}

/**
 * Welcome/empty state screen shown when no trees exist.
 */
@Composable
fun WelcomeScreen(
    modifier: Modifier = Modifier,
    onStartChat: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.AccountTree,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Bedrock Loom",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Weave conversations through probabilistic space",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(onClick = onStartChat) {
                Text("Start New Chat")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Configure AWS Credentials")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Set up your AWS credentials in Settings to connect to Claude",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    LoomTheme {
        WelcomeScreen(onStartChat = {})
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun WelcomeScreenDarkPreview() {
    LoomTheme(darkTheme = true) {
        WelcomeScreen(onStartChat = {})
    }
}
