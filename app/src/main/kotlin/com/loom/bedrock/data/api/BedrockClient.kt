package com.loom.bedrock.data.api

import aws.sdk.kotlin.services.bedrockruntime.BedrockRuntimeClient
import aws.sdk.kotlin.services.bedrockruntime.model.ContentBlock
import aws.sdk.kotlin.services.bedrockruntime.model.ConversationRole
import aws.sdk.kotlin.services.bedrockruntime.model.ConverseRequest
import aws.sdk.kotlin.services.bedrockruntime.model.Message
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.collections.Attributes
import com.loom.bedrock.data.preferences.AppPreferences
import com.loom.bedrock.data.preferences.ParsedCredentials
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client for interacting with AWS Bedrock Runtime API.
 * Uses the Converse API for Claude models.
 */
@Singleton
class BedrockClient @Inject constructor(
    private val appPreferences: AppPreferences
) {
    
    /**
     * Send a message to Claude and get a response.
     * 
     * @param conversationHistory List of previous messages in the conversation
     * @param systemPrompt Optional system prompt
     * @return The assistant's response text
     */
    suspend fun chat(
        conversationHistory: List<ChatMessage>,
        systemPrompt: String? = null
    ): Result<String> {
        return try {
            val credentialsText = appPreferences.awsCredentials.first()
            val parsedCreds = appPreferences.parseCredentials(credentialsText)
                ?: return Result.failure(Exception("Invalid or missing AWS credentials. Please configure in Settings."))

            val region = appPreferences.awsRegion.first()
            val modelId = appPreferences.modelId.first()
            val maxTokens = appPreferences.maxTokens.first()

            val client = createClient(parsedCreds, region)

            // Convert conversation history to Bedrock message format
            val messages = conversationHistory.map { msg ->
                Message {
                    role = when (msg.role) {
                        ChatRole.USER -> ConversationRole.User
                        ChatRole.ASSISTANT -> ConversationRole.Assistant
                    }
                    content = listOf(ContentBlock.Text(msg.content))
                }
            }

            val request = ConverseRequest {
                this.modelId = modelId
                this.messages = messages
                this.inferenceConfig {
                    this.maxTokens = maxTokens
                }
                if (systemPrompt != null) {
                    this.system = listOf(
                        aws.sdk.kotlin.services.bedrockruntime.model.SystemContentBlock.Text(systemPrompt)
                    )
                }
            }
            
            val response = client.converse(request)
            client.close()
            
            val outputMessage = response.output
            if (outputMessage is aws.sdk.kotlin.services.bedrockruntime.model.ConverseOutput.Message) {
                val textContent = outputMessage.value.content?.filterIsInstance<ContentBlock.Text>()
                    ?.joinToString("") { it.value }
                    ?: ""
                Result.success(textContent)
            } else {
                Result.failure(Exception("Unexpected response type from Bedrock"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Stream a response from Claude (returns chunks as they arrive).
     */
    suspend fun chatStream(
        conversationHistory: List<ChatMessage>,
        systemPrompt: String? = null,
        onChunk: (String) -> Unit
    ): Result<String> {
        val credentialsText: String
        val parsedCreds: ParsedCredentials
        val region: String
        val modelId: String
        val maxTokens: Int

        try {
            credentialsText = appPreferences.awsCredentials.first()
            parsedCreds = appPreferences.parseCredentials(credentialsText)
                ?: return Result.failure(Exception("Invalid or missing AWS credentials. Please configure in Settings."))
            region = appPreferences.awsRegion.first()
            modelId = appPreferences.modelId.first()
            maxTokens = appPreferences.maxTokens.first()
        } catch (e: Exception) {
            return Result.failure(Exception("Failed to load settings: ${e.message}"))
        }
        
        val client: BedrockRuntimeClient
        try {
            client = createClient(parsedCreds, region)
        } catch (e: Exception) {
            return Result.failure(Exception("Failed to create Bedrock client: ${e.message}"))
        }
        
        val fullResponse = StringBuilder()

        try {
            // Convert conversation history to Bedrock message format
            val messages = conversationHistory.map { msg ->
                Message {
                    role = when (msg.role) {
                        ChatRole.USER -> ConversationRole.User
                        ChatRole.ASSISTANT -> ConversationRole.Assistant
                    }
                    content = listOf(ContentBlock.Text(msg.content))
                }
            }

            val request = aws.sdk.kotlin.services.bedrockruntime.model.ConverseStreamRequest {
                this.modelId = modelId
                this.messages = messages
                this.inferenceConfig {
                    this.maxTokens = maxTokens
                }
                if (systemPrompt != null) {
                    this.system = listOf(
                        aws.sdk.kotlin.services.bedrockruntime.model.SystemContentBlock.Text(systemPrompt)
                    )
                }
            }
            
            client.converseStream(request) { response ->
                response.stream?.collect { event ->
                    when (event) {
                        is aws.sdk.kotlin.services.bedrockruntime.model.ConverseStreamOutput.ContentBlockDelta -> {
                            val delta = event.value.delta
                            if (delta is aws.sdk.kotlin.services.bedrockruntime.model.ContentBlockDelta.Text) {
                                val text = delta.value
                                fullResponse.append(text)
                                onChunk(text)
                            }
                        }
                        else -> { /* ignore other events */ }
                    }
                }
            }
        } catch (e: Exception) {
            // If we already got some response, return it as success
            if (fullResponse.isNotEmpty()) {
                try { client.close() } catch (_: Exception) {}
                return Result.success(fullResponse.toString())
            }
            try { client.close() } catch (_: Exception) {}
            return Result.failure(Exception("Bedrock API error: ${e.message}"))
        }
        
        try {
            client.close()
        } catch (_: Exception) {
            // Ignore close errors
        }
        
        return Result.success(fullResponse.toString())
    }
    
    private fun createClient(credentials: ParsedCredentials, region: String): BedrockRuntimeClient {
        return BedrockRuntimeClient {
            this.region = region
            this.credentialsProvider = StaticCredentialsProvider(credentials)
        }
    }
}

/**
 * Simple static credentials provider for AWS SDK.
 */
private class StaticCredentialsProvider(
    private val creds: ParsedCredentials
) : CredentialsProvider {
    override suspend fun resolve(attributes: Attributes): Credentials {
        return Credentials(
            accessKeyId = creds.accessKeyId,
            secretAccessKey = creds.secretAccessKey,
            sessionToken = creds.sessionToken
        )
    }
}

/**
 * A message in the chat conversation.
 */
data class ChatMessage(
    val role: ChatRole,
    val content: String
)

/**
 * Role of a chat participant.
 */
enum class ChatRole {
    USER,
    ASSISTANT
}
