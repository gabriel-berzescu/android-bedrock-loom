package com.loom.bedrock.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "loom_settings")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val AWS_CREDENTIALS = stringPreferencesKey("aws_credentials")
        private val AWS_REGION = stringPreferencesKey("aws_region")
        private val MODEL_ID = stringPreferencesKey("model_id")
        private val MAX_TOKENS = intPreferencesKey("max_tokens")
        private val TEMPERATURE = floatPreferencesKey("temperature")
        private val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        private val EXTENDED_THINKING_ENABLED = booleanPreferencesKey("extended_thinking_enabled")
        private val THINKING_BUDGET_TOKENS = intPreferencesKey("thinking_budget_tokens")

        const val DEFAULT_REGION = "eu-west-1"
        const val DEFAULT_MODEL_ID = "global.anthropic.claude-opus-4-5-20251101-v1:0"
        const val DEFAULT_MAX_TOKENS = 256
        const val DEFAULT_TEMPERATURE = 1.0f
        const val DEFAULT_SYSTEM_PROMPT = "You are Claude Opus 4.5"
        const val DEFAULT_EXTENDED_THINKING_ENABLED = false
        const val DEFAULT_THINKING_BUDGET_TOKENS = 4096
    }
    
    /**
     * AWS credentials in INI format:
     * [profile_name]
     * aws_access_key_id=xxx
     * aws_secret_access_key=xxx
     * aws_session_token=xxx (optional)
     */
    val awsCredentials: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[AWS_CREDENTIALS] ?: "" }
    
    val awsRegion: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[AWS_REGION] ?: DEFAULT_REGION }
    
    val modelId: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[MODEL_ID] ?: DEFAULT_MODEL_ID }

    val maxTokens: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[MAX_TOKENS] ?: DEFAULT_MAX_TOKENS }

    val temperature: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[TEMPERATURE] ?: DEFAULT_TEMPERATURE }

    val systemPrompt: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[SYSTEM_PROMPT] ?: DEFAULT_SYSTEM_PROMPT }

    val extendedThinkingEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[EXTENDED_THINKING_ENABLED] ?: DEFAULT_EXTENDED_THINKING_ENABLED }

    val thinkingBudgetTokens: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[THINKING_BUDGET_TOKENS] ?: DEFAULT_THINKING_BUDGET_TOKENS }

    suspend fun setAwsCredentials(credentials: String) {
        context.dataStore.edit { preferences ->
            preferences[AWS_CREDENTIALS] = credentials
        }
    }
    
    suspend fun setAwsRegion(region: String) {
        context.dataStore.edit { preferences ->
            preferences[AWS_REGION] = region
        }
    }
    
    suspend fun setModelId(modelId: String) {
        context.dataStore.edit { preferences ->
            preferences[MODEL_ID] = modelId
        }
    }

    suspend fun setMaxTokens(maxTokens: Int) {
        context.dataStore.edit { preferences ->
            preferences[MAX_TOKENS] = maxTokens
        }
    }

    suspend fun setTemperature(temperature: Float) {
        context.dataStore.edit { preferences ->
            preferences[TEMPERATURE] = temperature
        }
    }

    suspend fun setSystemPrompt(systemPrompt: String) {
        context.dataStore.edit { preferences ->
            preferences[SYSTEM_PROMPT] = systemPrompt
        }
    }

    suspend fun setExtendedThinkingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[EXTENDED_THINKING_ENABLED] = enabled
        }
    }

    suspend fun setThinkingBudgetTokens(budgetTokens: Int) {
        context.dataStore.edit { preferences ->
            preferences[THINKING_BUDGET_TOKENS] = budgetTokens
        }
    }

    /**
     * Parse the INI-format credentials and extract the values.
     * Returns null if parsing fails.
     */
    fun parseCredentials(credentialsText: String): ParsedCredentials? {
        if (credentialsText.isBlank()) return null
        
        val lines = credentialsText.lines()
        var accessKeyId: String? = null
        var secretAccessKey: String? = null
        var sessionToken: String? = null
        
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("aws_access_key_id=") -> {
                    accessKeyId = trimmed.substringAfter("=").trim()
                }
                trimmed.startsWith("aws_secret_access_key=") -> {
                    secretAccessKey = trimmed.substringAfter("=").trim()
                }
                trimmed.startsWith("aws_session_token=") -> {
                    sessionToken = trimmed.substringAfter("=").trim()
                }
            }
        }
        
        return if (accessKeyId != null && secretAccessKey != null) {
            ParsedCredentials(
                accessKeyId = accessKeyId,
                secretAccessKey = secretAccessKey,
                sessionToken = sessionToken
            )
        } else {
            null
        }
    }
}

data class ParsedCredentials(
    val accessKeyId: String,
    val secretAccessKey: String,
    val sessionToken: String? = null
)
