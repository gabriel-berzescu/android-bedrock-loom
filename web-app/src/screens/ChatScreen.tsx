import { useState, useEffect, useRef, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Box,
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  TextField,
  CircularProgress,
  Paper,
  Menu,
  MenuItem,
  Divider,
} from '@mui/material'
import {
  ArrowBack as BackIcon,
  Send as SendIcon,
  AccountTree as TreeIcon,
  ContentCopy as CopyIcon,
  Edit as EditIcon,
  Refresh as RegenerateIcon,
} from '@mui/icons-material'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter'
import { oneDark } from 'react-syntax-highlighter/dist/esm/styles/prism'
import {
  type Conversation,
  db,
  createNode,
  buildLinearPath,
  updateConversationActiveNode,
  renameConversation,
  updateNodeContent,
} from '../data/db'
import { chatStream } from '../data/bedrockClient'

interface UiMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  thinkingContent: string
  isStreaming?: boolean
}

export default function ChatScreen() {
  const { conversationId } = useParams<{ conversationId: string }>()
  const navigate = useNavigate()
  const [conversation, setConversation] = useState<Conversation | null>(null)
  const [messages, setMessages] = useState<UiMessage[]>([])
  const [input, setInput] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [editingMessageId, setEditingMessageId] = useState<string | null>(null)
  const [editContent, setEditContent] = useState('')
  const [menuAnchor, setMenuAnchor] = useState<null | HTMLElement>(null)
  const [selectedMessage, setSelectedMessage] = useState<UiMessage | null>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const abortControllerRef = useRef<AbortController | null>(null)

  const loadConversation = useCallback(async () => {
    if (!conversationId) return

    const conv = await db.conversations.get(conversationId)
    if (!conv) {
      navigate('/')
      return
    }
    setConversation(conv)

    const path = await buildLinearPath(conversationId, conv.activeNodeId)
    setMessages(
      path.map((node) => ({
        id: node.id,
        role: node.role,
        content: node.content,
        thinkingContent: node.thinkingContent,
      }))
    )
  }, [conversationId, navigate])

  useEffect(() => {
    loadConversation()
  }, [loadConversation])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const generateTitle = async (firstMessage: string) => {
    if (!conversationId || !conversation) return
    // Generate title from first message (first 50 chars)
    const title = firstMessage.slice(0, 50) + (firstMessage.length > 50 ? '...' : '')
    await renameConversation(conversationId, title)
    setConversation({ ...conversation, title })
  }

  const sendMessage = async () => {
    if (!input.trim() || isLoading || !conversationId) return

    const userContent = input.trim()
    setInput('')
    setIsLoading(true)

    // Determine parent node
    const parentId = messages.length > 0 ? messages[messages.length - 1].id : null

    // Create user message node
    const userNode = await createNode(conversationId, parentId, 'user', userContent)

    // Generate title if first message
    if (messages.length === 0) {
      await generateTitle(userContent)
    }

    // Add user message to UI
    const userMessage: UiMessage = {
      id: userNode.id,
      role: 'user',
      content: userContent,
      thinkingContent: '',
    }
    setMessages((prev) => [...prev, userMessage])

    // Create streaming placeholder for assistant
    const placeholderId = crypto.randomUUID()
    const assistantMessage: UiMessage = {
      id: placeholderId,
      role: 'assistant',
      content: '',
      thinkingContent: '',
      isStreaming: true,
    }
    setMessages((prev) => [...prev, assistantMessage])

    try {
      abortControllerRef.current = new AbortController()

      // Build conversation history
      const history = [...messages, userMessage].map((m) => ({
        role: m.role,
        content: m.content,
      }))

      let thinkingContent = ''

      const finalContent = await chatStream(
        history,
        (text) => {
          setMessages((prev) =>
            prev.map((m) =>
              m.id === placeholderId ? { ...m, content: text } : m
            )
          )
        },
        (thinking) => {
          thinkingContent = thinking
          setMessages((prev) =>
            prev.map((m) =>
              m.id === placeholderId ? { ...m, thinkingContent: thinking } : m
            )
          )
        },
        abortControllerRef.current.signal
      )

      // Save assistant response to database
      const assistantNode = await createNode(
        conversationId,
        userNode.id,
        'assistant',
        finalContent,
        thinkingContent
      )

      // Update active node
      await updateConversationActiveNode(conversationId, assistantNode.id)

      // Update UI with final node ID
      setMessages((prev) =>
        prev.map((m) =>
          m.id === placeholderId
            ? { ...m, id: assistantNode.id, isStreaming: false }
            : m
        )
      )
    } catch (error) {
      console.error('Chat error:', error)
      // Remove streaming placeholder on error
      setMessages((prev) => prev.filter((m) => m.id !== placeholderId))
      // Show error in a simple way
      const errorNode = await createNode(
        conversationId,
        userNode.id,
        'assistant',
        `Error: ${error instanceof Error ? error.message : 'Unknown error occurred'}`
      )
      setMessages((prev) => [
        ...prev,
        {
          id: errorNode.id,
          role: 'assistant',
          content: errorNode.content,
          thinkingContent: '',
        },
      ])
    } finally {
      setIsLoading(false)
      abortControllerRef.current = null
    }
  }

  const handleMessageMenu = (event: React.MouseEvent<HTMLElement>, message: UiMessage) => {
    event.preventDefault()
    setSelectedMessage(message)
    setMenuAnchor(event.currentTarget)
  }

  const handleCloseMenu = () => {
    setMenuAnchor(null)
    setSelectedMessage(null)
  }

  const handleCopy = () => {
    if (selectedMessage) {
      navigator.clipboard.writeText(selectedMessage.content)
    }
    handleCloseMenu()
  }

  const handleEdit = () => {
    if (selectedMessage && selectedMessage.role === 'user') {
      setEditingMessageId(selectedMessage.id)
      setEditContent(selectedMessage.content)
    }
    handleCloseMenu()
  }

  const handleSaveEdit = async () => {
    if (!editingMessageId || !conversationId) return

    // Find the index of the edited message
    const editIndex = messages.findIndex((m) => m.id === editingMessageId)
    if (editIndex === -1) return

    // Update the message content
    await updateNodeContent(editingMessageId, editContent)

    // Keep messages up to and including the edited one
    const keptMessages = messages.slice(0, editIndex + 1).map((m) =>
      m.id === editingMessageId ? { ...m, content: editContent } : m
    )

    setMessages(keptMessages)
    setEditingMessageId(null)
    setEditContent('')

    // Regenerate response
    await regenerateFromMessage(keptMessages[keptMessages.length - 1])
  }

  const handleCancelEdit = () => {
    setEditingMessageId(null)
    setEditContent('')
  }

  const handleRegenerate = async () => {
    handleCloseMenu()
    if (!selectedMessage || selectedMessage.role !== 'assistant') return

    // Find the user message before this assistant message
    const assistantIndex = messages.findIndex((m) => m.id === selectedMessage.id)
    if (assistantIndex <= 0) return

    const userMessage = messages[assistantIndex - 1]
    const keptMessages = messages.slice(0, assistantIndex)
    setMessages(keptMessages)

    await regenerateFromMessage(userMessage)
  }

  const regenerateFromMessage = async (userMessage: UiMessage) => {
    if (!conversationId) return

    setIsLoading(true)

    // Create streaming placeholder
    const placeholderId = crypto.randomUUID()
    const assistantMessage: UiMessage = {
      id: placeholderId,
      role: 'assistant',
      content: '',
      thinkingContent: '',
      isStreaming: true,
    }
    setMessages((prev) => [...prev, assistantMessage])

    try {
      abortControllerRef.current = new AbortController()

      // Build conversation history up to user message
      const userIndex = messages.findIndex((m) => m.id === userMessage.id)
      const history = messages.slice(0, userIndex + 1).map((m) => ({
        role: m.role,
        content: m.content,
      }))

      let thinkingContent = ''

      const finalContent = await chatStream(
        history,
        (text) => {
          setMessages((prev) =>
            prev.map((m) =>
              m.id === placeholderId ? { ...m, content: text } : m
            )
          )
        },
        (thinking) => {
          thinkingContent = thinking
          setMessages((prev) =>
            prev.map((m) =>
              m.id === placeholderId ? { ...m, thinkingContent: thinking } : m
            )
          )
        },
        abortControllerRef.current.signal
      )

      // Save assistant response (creates a new branch)
      const assistantNode = await createNode(
        conversationId,
        userMessage.id,
        'assistant',
        finalContent,
        thinkingContent
      )

      await updateConversationActiveNode(conversationId, assistantNode.id)

      setMessages((prev) =>
        prev.map((m) =>
          m.id === placeholderId
            ? { ...m, id: assistantNode.id, isStreaming: false }
            : m
        )
      )
    } catch (error) {
      console.error('Regenerate error:', error)
      setMessages((prev) => prev.filter((m) => m.id !== placeholderId))
    } finally {
      setIsLoading(false)
      abortControllerRef.current = null
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      sendMessage()
    }
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
      <AppBar position="static">
        <Toolbar>
          <IconButton edge="start" color="inherit" onClick={() => navigate('/')}>
            <BackIcon />
          </IconButton>
          <Typography variant="h6" sx={{ flexGrow: 1, ml: 1 }} noWrap>
            {conversation?.title || 'Chat'}
          </Typography>
          <IconButton color="inherit" onClick={() => navigate(`/tree/${conversationId}`)}>
            <TreeIcon />
          </IconButton>
        </Toolbar>
      </AppBar>

      <Box
        sx={{
          flexGrow: 1,
          overflow: 'auto',
          p: 2,
          display: 'flex',
          flexDirection: 'column',
          gap: 2,
        }}
      >
        {messages.map((message) => (
          <Box
            key={message.id}
            sx={{
              display: 'flex',
              justifyContent: message.role === 'user' ? 'flex-end' : 'flex-start',
            }}
          >
            <Paper
              sx={{
                p: 2,
                maxWidth: '80%',
                backgroundColor:
                  message.role === 'user' ? 'primary.dark' : 'background.paper',
                cursor: 'context-menu',
              }}
              onContextMenu={(e) => handleMessageMenu(e, message)}
              onClick={(e) => handleMessageMenu(e, message)}
            >
              {editingMessageId === message.id ? (
                <Box>
                  <TextField
                    fullWidth
                    multiline
                    value={editContent}
                    onChange={(e) => setEditContent(e.target.value)}
                    variant="outlined"
                    size="small"
                    autoFocus
                  />
                  <Box sx={{ mt: 1, display: 'flex', gap: 1 }}>
                    <IconButton size="small" onClick={handleSaveEdit} color="primary">
                      <SendIcon fontSize="small" />
                    </IconButton>
                    <IconButton size="small" onClick={handleCancelEdit}>
                      <BackIcon fontSize="small" />
                    </IconButton>
                  </Box>
                </Box>
              ) : (
                <>
                  {message.thinkingContent && (
                    <Box
                      sx={{
                        mb: 2,
                        p: 1.5,
                        backgroundColor: 'rgba(255,255,255,0.05)',
                        borderRadius: 1,
                        borderLeft: '3px solid',
                        borderColor: 'secondary.main',
                        fontSize: '0.9em',
                        color: 'text.secondary',
                      }}
                    >
                      <Typography variant="caption" sx={{ fontWeight: 'bold', mb: 1, display: 'block' }}>
                        Thinking...
                      </Typography>
                      <Box sx={{ whiteSpace: 'pre-wrap' }}>{message.thinkingContent}</Box>
                    </Box>
                  )}
                  <Box
                    sx={{
                      '& pre': {
                        overflowX: 'auto',
                        borderRadius: 1,
                      },
                      '& code': {
                        fontSize: '0.9em',
                      },
                      '& p': { m: 0 },
                      '& p + p': { mt: 1 },
                    }}
                  >
                    <ReactMarkdown
                      remarkPlugins={[remarkGfm]}
                      components={{
                        code({ className, children, ...props }) {
                          const match = /language-(\w+)/.exec(className || '')
                          const inline = !match
                          return !inline ? (
                            <SyntaxHighlighter
                              style={oneDark}
                              language={match[1]}
                              PreTag="div"
                            >
                              {String(children).replace(/\n$/, '')}
                            </SyntaxHighlighter>
                          ) : (
                            <code className={className} {...props}>
                              {children}
                            </code>
                          )
                        },
                      }}
                    >
                      {message.content}
                    </ReactMarkdown>
                  </Box>
                  {message.isStreaming && (
                    <CircularProgress size={16} sx={{ ml: 1, mt: 1 }} />
                  )}
                </>
              )}
            </Paper>
          </Box>
        ))}
        <div ref={messagesEndRef} />
      </Box>

      <Box sx={{ p: 2, borderTop: 1, borderColor: 'divider' }}>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <TextField
            fullWidth
            multiline
            maxRows={4}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Type a message..."
            disabled={isLoading}
            variant="outlined"
            size="small"
            sx={{
              '& .MuiInputBase-input': {
                textTransform: 'none',
              },
            }}
            slotProps={{
              input: {
                sx: { textTransform: 'capitalize' },
              },
            }}
          />
          <IconButton
            color="primary"
            onClick={sendMessage}
            disabled={!input.trim() || isLoading}
          >
            {isLoading ? <CircularProgress size={24} /> : <SendIcon />}
          </IconButton>
        </Box>
      </Box>

      <Menu anchorEl={menuAnchor} open={Boolean(menuAnchor)} onClose={handleCloseMenu}>
        <MenuItem onClick={handleCopy}>
          <CopyIcon sx={{ mr: 1 }} /> Copy
        </MenuItem>
        {selectedMessage?.role === 'user' && (
          <MenuItem onClick={handleEdit}>
            <EditIcon sx={{ mr: 1 }} /> Edit
          </MenuItem>
        )}
        {selectedMessage?.role === 'assistant' && (
          <>
            <Divider />
            <MenuItem onClick={handleRegenerate}>
              <RegenerateIcon sx={{ mr: 1 }} /> Regenerate
            </MenuItem>
          </>
        )}
      </Menu>
    </Box>
  )
}
