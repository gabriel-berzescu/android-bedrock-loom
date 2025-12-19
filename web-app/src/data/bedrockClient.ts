import {
  BedrockRuntimeClient,
  ConverseStreamCommand,
  type Message,
  type ContentBlock,
  type ConversationRole,
} from '@aws-sdk/client-bedrock-runtime'
import { getSettings, type ChatRole } from './db'

interface ChatMessage {
  role: ChatRole
  content: string
}

interface Credentials {
  accessKeyId: string
  secretAccessKey: string
  sessionToken?: string
}

function parseCredentials(iniContent: string): Credentials | null {
  const lines = iniContent.split('\n')
  let accessKeyId = ''
  let secretAccessKey = ''
  let sessionToken: string | undefined

  for (const line of lines) {
    const trimmed = line.trim()
    if (trimmed.startsWith('aws_access_key_id')) {
      accessKeyId = trimmed.split('=')[1]?.trim() || ''
    } else if (trimmed.startsWith('aws_secret_access_key')) {
      secretAccessKey = trimmed.split('=')[1]?.trim() || ''
    } else if (trimmed.startsWith('aws_session_token')) {
      sessionToken = trimmed.split('=')[1]?.trim()
    }
  }

  if (!accessKeyId || !secretAccessKey) {
    return null
  }

  return { accessKeyId, secretAccessKey, sessionToken }
}

export async function chatStream(
  messages: ChatMessage[],
  onChunk: (text: string) => void,
  onThinkingChunk?: (text: string) => void,
  abortSignal?: AbortSignal
): Promise<string> {
  const settings = await getSettings()
  const creds = parseCredentials(settings.credentials)

  if (!creds) {
    throw new Error('Invalid AWS credentials. Please check your settings.')
  }

  const client = new BedrockRuntimeClient({
    region: settings.region,
    credentials: {
      accessKeyId: creds.accessKeyId,
      secretAccessKey: creds.secretAccessKey,
      sessionToken: creds.sessionToken,
    },
  })

  // Convert messages to Bedrock format
  const bedrockMessages: Message[] = messages.map((msg) => ({
    role: (msg.role === 'user' ? 'user' : 'assistant') as ConversationRole,
    content: [{ text: msg.content } as ContentBlock],
  }))

  // Build inference config
  const inferenceConfig: { maxTokens: number; temperature?: number } = {
    maxTokens: settings.maxTokens,
  }

  // Temperature must be 1.0 when extended thinking is enabled
  if (!settings.extendedThinkingEnabled) {
    inferenceConfig.temperature = settings.temperature
  }

  // Build additional model request fields for extended thinking
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let additionalModelRequestFields: Record<string, any> | undefined
  if (settings.extendedThinkingEnabled) {
    additionalModelRequestFields = {
      thinking: {
        type: 'enabled',
        budget_tokens: settings.thinkingBudget,
      },
    }
  }

  const command = new ConverseStreamCommand({
    modelId: settings.modelId,
    messages: bedrockMessages,
    system: settings.systemPrompt ? [{ text: settings.systemPrompt }] : undefined,
    inferenceConfig,
    additionalModelRequestFields,
  })

  let fullResponse = ''
  let fullThinking = ''

  try {
    const response = await client.send(command, { abortSignal })

    if (response.stream) {
      for await (const event of response.stream) {
        if (abortSignal?.aborted) {
          break
        }

        if (event.contentBlockDelta) {
          const delta = event.contentBlockDelta.delta
          if (delta) {
            // Handle text content
            if ('text' in delta && delta.text) {
              fullResponse += delta.text
              onChunk(fullResponse)
            }
            // Handle thinking content
            if ('reasoningContent' in delta) {
              // eslint-disable-next-line @typescript-eslint/no-explicit-any
              const reasoning = delta.reasoningContent as any
              if (reasoning?.text) {
                fullThinking += reasoning.text
                onThinkingChunk?.(fullThinking)
              }
            }
          }
        }
      }
    }
  } catch (error) {
    if (abortSignal?.aborted) {
      return fullResponse
    }
    throw error
  }

  return fullResponse
}

export async function validateCredentials(): Promise<boolean> {
  const settings = await getSettings()
  const creds = parseCredentials(settings.credentials)
  return creds !== null
}
