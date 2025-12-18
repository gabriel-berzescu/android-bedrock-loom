package com.loom.bedrock.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loom.bedrock.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {
    
    val awsCredentials = appPreferences.awsCredentials
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    
    val awsRegion = appPreferences.awsRegion
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences.DEFAULT_REGION)
    
    val modelId = appPreferences.modelId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences.DEFAULT_MODEL_ID)

    val maxTokens = appPreferences.maxTokens
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences.DEFAULT_MAX_TOKENS)

    val temperature = appPreferences.temperature
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences.DEFAULT_TEMPERATURE)

    val systemPrompt = appPreferences.systemPrompt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences.DEFAULT_SYSTEM_PROMPT)

    val extendedThinkingEnabled = appPreferences.extendedThinkingEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences.DEFAULT_EXTENDED_THINKING_ENABLED)

    val thinkingBudgetTokens = appPreferences.thinkingBudgetTokens
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences.DEFAULT_THINKING_BUDGET_TOKENS)

    fun saveCredentials(credentials: String) {
        viewModelScope.launch {
            appPreferences.setAwsCredentials(credentials)
        }
    }
    
    fun saveRegion(region: String) {
        viewModelScope.launch {
            appPreferences.setAwsRegion(region)
        }
    }
    
    fun saveModelId(modelId: String) {
        viewModelScope.launch {
            appPreferences.setModelId(modelId)
        }
    }

    fun saveMaxTokens(maxTokens: Int) {
        viewModelScope.launch {
            appPreferences.setMaxTokens(maxTokens)
        }
    }

    fun saveTemperature(temperature: Float) {
        viewModelScope.launch {
            appPreferences.setTemperature(temperature)
        }
    }

    fun saveSystemPrompt(systemPrompt: String) {
        viewModelScope.launch {
            appPreferences.setSystemPrompt(systemPrompt)
        }
    }

    fun saveExtendedThinkingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setExtendedThinkingEnabled(enabled)
        }
    }

    fun saveThinkingBudgetTokens(budgetTokens: Int) {
        viewModelScope.launch {
            appPreferences.setThinkingBudgetTokens(budgetTokens)
        }
    }

    fun validateCredentials(credentials: String): Boolean {
        return appPreferences.parseCredentials(credentials) != null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val credentials by viewModel.awsCredentials.collectAsState()
    val region by viewModel.awsRegion.collectAsState()
    val modelId by viewModel.modelId.collectAsState()
    val maxTokens by viewModel.maxTokens.collectAsState()
    val temperature by viewModel.temperature.collectAsState()
    val systemPrompt by viewModel.systemPrompt.collectAsState()
    val extendedThinkingEnabled by viewModel.extendedThinkingEnabled.collectAsState()
    val thinkingBudgetTokens by viewModel.thinkingBudgetTokens.collectAsState()

    var editedCredentials by remember(credentials) { mutableStateOf(credentials) }
    var editedRegion by remember(region) { mutableStateOf(region) }
    var editedModelId by remember(modelId) { mutableStateOf(modelId) }
    var editedMaxTokens by remember(maxTokens) { mutableStateOf(maxTokens.toString()) }
    var editedTemperature by remember(temperature) { mutableStateOf(temperature.toString()) }
    var editedSystemPrompt by remember(systemPrompt) { mutableStateOf(systemPrompt) }
    var editedExtendedThinkingEnabled by remember(extendedThinkingEnabled) { mutableStateOf(extendedThinkingEnabled) }
    var editedThinkingBudgetTokens by remember(thinkingBudgetTokens) { mutableStateOf(thinkingBudgetTokens.toString()) }
    var showSaved by remember { mutableStateOf(false) }
    
    val credentialsValid = editedCredentials.isBlank() || viewModel.validateCredentials(editedCredentials)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // AWS Credentials Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "AWS Credentials",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Text(
                        text = "Paste your AWS credentials in INI format:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = editedCredentials,
                        onValueChange = { editedCredentials = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        placeholder = {
                            Text(
                                text = "[profile_name]\naws_access_key_id=YOUR_KEY_ID\naws_secret_access_key=YOUR_SECRET\naws_session_token=...",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        },
                        isError = !credentialsValid,
                        supportingText = if (!credentialsValid) {
                            { Text("Invalid format. Needs aws_access_key_id and aws_secret_access_key") }
                        } else null,
                        maxLines = 10
                    )
                }
            }
            
            // Model Settings Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Model Settings",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    OutlinedTextField(
                        value = editedModelId,
                        onValueChange = { editedModelId = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Model ID") },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                    )
                    
                    Text(
                        text = "Default: ${AppPreferences.DEFAULT_MODEL_ID}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = editedMaxTokens,
                        onValueChange = { editedMaxTokens = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Max Output Tokens") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Text(
                        text = "Default: ${AppPreferences.DEFAULT_MAX_TOKENS}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = editedTemperature,
                        onValueChange = { editedTemperature = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Temperature (0.0 - 1.0)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    Text(
                        text = "Default: ${AppPreferences.DEFAULT_TEMPERATURE}. Lower = more focused, Higher = more creative",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Extended Thinking Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Extended Thinking",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Extended Thinking",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Shows Claude's reasoning process",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = editedExtendedThinkingEnabled,
                            onCheckedChange = { editedExtendedThinkingEnabled = it }
                        )
                    }

                    if (editedExtendedThinkingEnabled) {
                        OutlinedTextField(
                            value = editedThinkingBudgetTokens,
                            onValueChange = { editedThinkingBudgetTokens = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Thinking Budget (tokens)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Text(
                            text = "Min: 1024. Higher = more thorough reasoning. Must be less than Max Output Tokens.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Note: Temperature is ignored when thinking is enabled (defaults to 1.0)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            // System Prompt Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "System Prompt",
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = editedSystemPrompt,
                        onValueChange = { editedSystemPrompt = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        placeholder = { Text(AppPreferences.DEFAULT_SYSTEM_PROMPT) },
                        maxLines = 5
                    )

                    Text(
                        text = "Default: ${AppPreferences.DEFAULT_SYSTEM_PROMPT}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Region Settings Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "AWS Region",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    OutlinedTextField(
                        value = editedRegion,
                        onValueChange = { editedRegion = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Region") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                    )
                    
                    Text(
                        text = "Default: ${AppPreferences.DEFAULT_REGION}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Save Button
            Button(
                onClick = {
                    viewModel.saveCredentials(editedCredentials)
                    viewModel.saveRegion(editedRegion)
                    viewModel.saveModelId(editedModelId)
                    val maxTokensInt = editedMaxTokens.toIntOrNull() ?: AppPreferences.DEFAULT_MAX_TOKENS
                    viewModel.saveMaxTokens(maxTokensInt)
                    val temperatureFloat = editedTemperature.toFloatOrNull()?.coerceIn(0f, 1f) ?: AppPreferences.DEFAULT_TEMPERATURE
                    viewModel.saveTemperature(temperatureFloat)
                    viewModel.saveSystemPrompt(editedSystemPrompt)
                    viewModel.saveExtendedThinkingEnabled(editedExtendedThinkingEnabled)
                    val budgetTokensInt = editedThinkingBudgetTokens.toIntOrNull()?.coerceAtLeast(1024) ?: AppPreferences.DEFAULT_THINKING_BUDGET_TOKENS
                    viewModel.saveThinkingBudgetTokens(budgetTokensInt)
                    showSaved = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = credentialsValid
            ) {
                Text("Save Settings")
            }
            
            if (showSaved) {
                LaunchedEffect(showSaved) {
                    kotlinx.coroutines.delay(2000)
                    showSaved = false
                }
                Text(
                    text = "✓ Settings saved",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Reset to Defaults
            TextButton(
                onClick = {
                    editedRegion = AppPreferences.DEFAULT_REGION
                    editedModelId = AppPreferences.DEFAULT_MODEL_ID
                    editedMaxTokens = AppPreferences.DEFAULT_MAX_TOKENS.toString()
                    editedTemperature = AppPreferences.DEFAULT_TEMPERATURE.toString()
                    editedSystemPrompt = AppPreferences.DEFAULT_SYSTEM_PROMPT
                    editedExtendedThinkingEnabled = AppPreferences.DEFAULT_EXTENDED_THINKING_ENABLED
                    editedThinkingBudgetTokens = AppPreferences.DEFAULT_THINKING_BUDGET_TOKENS.toString()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset to Defaults")
            }
        }
    }
}
