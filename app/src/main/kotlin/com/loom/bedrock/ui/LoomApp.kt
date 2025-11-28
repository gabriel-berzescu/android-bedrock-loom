package com.loom.bedrock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loom.bedrock.ui.theme.LoomTheme

/**
 * Main app entry point composable.
 * 
 * This will eventually contain:
 * - Navigation host
 * - Tree list screen
 * - Loom screen (tree visualization)
 * - Settings screen
 */
@Composable
fun LoomApp() {
    Scaffold { paddingValues ->
        // Placeholder until navigation is set up
        WelcomeScreen(
            modifier = Modifier.padding(paddingValues),
            onGetStarted = { /* TODO: Navigate to tree creation */ }
        )
    }
}

/**
 * Welcome/empty state screen shown when no trees exist.
 */
@Composable
fun WelcomeScreen(
    modifier: Modifier = Modifier,
    onGetStarted: () -> Unit,
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
                text = "Weave realities through probabilistic space",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(onClick = onGetStarted) {
                Text("Start Weaving")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Configure AWS credentials in Settings to begin",
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
        WelcomeScreen(onGetStarted = {})
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun WelcomeScreenDarkPreview() {
    LoomTheme(darkTheme = true) {
        WelcomeScreen(onGetStarted = {})
    }
}
