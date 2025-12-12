package com.loom.bedrock.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import dev.jeziellago.compose.markdowntext.MarkdownText
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loom.bedrock.data.api.BedrockClient
import com.loom.bedrock.data.api.ChatMessage
import com.loom.bedrock.data.api.ChatRole
import com.loom.bedrock.data.local.ConversationDao
import com.loom.bedrock.data.local.ConversationEntity
import com.loom.bedrock.data.local.NodeEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class UiMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val content: String,
    val isStreaming: Boolean = false,
    val parentId: String? = null
)

data class ChatUiState(
    val conversationId: String = UUID.randomUUID().toString(),
    val conversationTitle: String = "Chat",
    val messages: List<UiMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentInput: String = "",
    val editingMessageId: String? = null,
    val editingContent: String = ""
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val bedrockClient: BedrockClient,
    private val conversationDao: ConversationDao,
    private val appPreferences: com.loom.bedrock.data.preferences.AppPreferences,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val conversationId: String? = savedStateHandle.get<String>("conversationId")
    
    private val _uiState = MutableStateFlow(ChatUiState(
        conversationId = conversationId ?: UUID.randomUUID().toString()
    ))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    init {
        if (conversationId != null) {
            loadConversation(conversationId)
        }
    }
    
    fun reloadConversation() {
        if (conversationId != null) {
            loadConversation(conversationId)
        }
    }

    private fun loadConversation(id: String) {
        viewModelScope.launch {
            val conversationWithNodes = conversationDao.getConversationWithNodes(id)
            if (conversationWithNodes != null) {
                // Build linear path from root to active node (or latest)
                val nodes = conversationWithNodes.nodes
                val activeNodeId = conversationWithNodes.conversation.activeNodeId

                val linearPath = buildLinearPath(nodes, activeNodeId)

                val messages = linearPath.map {
                    UiMessage(id = it.id, role = it.role, content = it.content, parentId = it.parentId)
                }
                _uiState.value = _uiState.value.copy(
                    conversationId = id,
                    conversationTitle = conversationWithNodes.conversation.title,
                    messages = messages
                )
            }
        }
    }
    
    private fun buildLinearPath(nodes: List<NodeEntity>, activeNodeId: String?): List<NodeEntity> {
        if (nodes.isEmpty()) return emptyList()
        
        val nodeMap = nodes.associateBy { it.id }
        
        // If we have an active node, trace back to root
        if (activeNodeId != null && nodeMap.containsKey(activeNodeId)) {
            val path = mutableListOf<NodeEntity>()
            var current: NodeEntity? = nodeMap[activeNodeId]
            while (current != null) {
                path.add(0, current)
                current = current.parentId?.let { nodeMap[it] }
            }
            return path
        }
        
        // Otherwise, find the deepest path (follow first children)
        val roots = nodes.filter { it.parentId == null }.sortedBy { it.createdAt }
        if (roots.isEmpty()) return emptyList()
        
        val path = mutableListOf<NodeEntity>()
        var current: NodeEntity? = roots.first()
        while (current != null) {
            path.add(current)
            val children = nodes.filter { it.parentId == current!!.id }.sortedBy { it.branchIndex }
            current = children.firstOrNull()
        }
        return path
    }
    
    fun updateInput(text: String) {
        _uiState.value = _uiState.value.copy(currentInput = text)
    }
    
    fun sendMessage() {
        val input = _uiState.value.currentInput.trim()
        if (input.isBlank() || _uiState.value.isLoading) return
        
        val parentId = _uiState.value.messages.lastOrNull()?.id
        val userMessage = UiMessage(role = ChatRole.USER, content = input, parentId = parentId)
        val assistantMessage = UiMessage(role = ChatRole.ASSISTANT, content = "", isStreaming = true, parentId = userMessage.id)
        
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage + assistantMessage,
            currentInput = "",
            isLoading = true,
            error = null
        )
        
        // Save user message immediately
        viewModelScope.launch {
            saveNode(userMessage)
        }
        
        sendToClaudeAndStream()
    }
    
    fun regenerateFrom(messageIndex: Int) {
        if (_uiState.value.isLoading) return
        
        val messages = _uiState.value.messages.toMutableList()
        if (messageIndex < 0 || messageIndex >= messages.size) return
        val targetMessage = messages[messageIndex]
        if (targetMessage.role != ChatRole.ASSISTANT) return
        
        // Keep messages up to (not including) the assistant message
        val messagesToKeep = messages.subList(0, messageIndex).toList()
        val parentId = messagesToKeep.lastOrNull()?.id
        
        // Add streaming placeholder
        val newAssistantMessage = UiMessage(role = ChatRole.ASSISTANT, content = "", isStreaming = true, parentId = parentId)
        
        _uiState.value = _uiState.value.copy(
            messages = messagesToKeep + newAssistantMessage,
            isLoading = true,
            error = null
        )
        
        sendToClaudeAndStream()
    }
    
    fun startEditing(messageId: String) {
        val message = _uiState.value.messages.find { it.id == messageId }
        if (message != null && message.role == ChatRole.USER) {
            _uiState.value = _uiState.value.copy(
                editingMessageId = messageId,
                editingContent = message.content
            )
        }
    }
    
    fun updateEditingContent(content: String) {
        _uiState.value = _uiState.value.copy(editingContent = content)
    }
    
    fun cancelEditing() {
        _uiState.value = _uiState.value.copy(
            editingMessageId = null,
            editingContent = ""
        )
    }
    
    fun saveEdit() {
        val editingId = _uiState.value.editingMessageId ?: return
        val newContent = _uiState.value.editingContent.trim()
        if (newContent.isBlank() || _uiState.value.isLoading) return
        
        val messages = _uiState.value.messages.toMutableList()
        val editIndex = messages.indexOfFirst { it.id == editingId }
        if (editIndex < 0) return
        
        // Get parent of the edited message
        val parentId = if (editIndex > 0) messages[editIndex - 1].id else null
        
        // Create new message with new ID (this creates a branch)
        val updatedMessage = UiMessage(
            id = UUID.randomUUID().toString(),
            role = ChatRole.USER,
            content = newContent,
            parentId = parentId
        )
        
        // Keep messages up to (not including) the edited one, then add updated
        val messagesToKeep = messages.subList(0, editIndex).toMutableList()
        messagesToKeep.add(updatedMessage)
        
        // Clear editing state
        _uiState.value = _uiState.value.copy(
            editingMessageId = null,
            editingContent = ""
        )
        
        // Save to database (creates a new branch)
        viewModelScope.launch {
            saveNode(updatedMessage)
        }
        
        // Add streaming placeholder
        val newAssistantMessage = UiMessage(role = ChatRole.ASSISTANT, content = "", isStreaming = true, parentId = updatedMessage.id)
        
        _uiState.value = _uiState.value.copy(
            messages = messagesToKeep + newAssistantMessage,
            isLoading = true,
            error = null
        )
        
        sendToClaudeAndStream()
    }
    
    private fun sendToClaudeAndStream() {
        viewModelScope.launch {
            val conversationHistory = _uiState.value.messages
                .filter { !it.isStreaming }
                .map { ChatMessage(it.role, it.content) }

            val systemPrompt = appPreferences.systemPrompt.first()

            val result = bedrockClient.chatStream(
                conversationHistory = conversationHistory,
                systemPrompt = systemPrompt,
                onChunk = { chunk ->
                    val messages = _uiState.value.messages.toMutableList()
                    val lastIndex = messages.lastIndex
                    if (lastIndex >= 0 && messages[lastIndex].isStreaming) {
                        messages[lastIndex] = messages[lastIndex].copy(
                            content = messages[lastIndex].content + chunk
                        )
                        _uiState.value = _uiState.value.copy(messages = messages)
                    }
                }
            )
            
            result.fold(
                onSuccess = { fullResponse ->
                    val messages = _uiState.value.messages.toMutableList()
                    val lastIndex = messages.lastIndex
                    if (lastIndex >= 0) {
                        val finalAssistantMessage = messages[lastIndex].copy(
                            content = fullResponse,
                            isStreaming = false
                        )
                        messages[lastIndex] = finalAssistantMessage
                        
                        // Save assistant message
                        saveNode(finalAssistantMessage)
                        
                        // Update active node
                        conversationDao.setActiveNode(_uiState.value.conversationId, finalAssistantMessage.id)
                    }
                    _uiState.value = _uiState.value.copy(
                        messages = messages,
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    val messages = _uiState.value.messages.toMutableList()
                    if (messages.isNotEmpty() && messages.last().isStreaming) {
                        messages.removeAt(messages.lastIndex)
                    }
                    _uiState.value = _uiState.value.copy(
                        messages = messages,
                        isLoading = false,
                        error = error.message ?: "Unknown error occurred"
                    )
                }
            )
        }
    }
    
    private suspend fun saveNode(message: UiMessage) {
        val convId = _uiState.value.conversationId
        
        val existingConv = conversationDao.getConversation(convId)
        if (existingConv == null) {
            val title = if (message.role == ChatRole.USER) {
                message.content.take(50).let { if (message.content.length > 50) "$it..." else it }
            } else {
                "New Chat"
            }
            conversationDao.insertConversation(
                ConversationEntity(id = convId, title = title)
            )
        } else {
            conversationDao.updateConversation(
                id = convId,
                title = existingConv.title
            )
        }
        
        // Calculate branch index
        val branchIndex = if (message.parentId != null) {
            (conversationDao.getMaxBranchIndex(message.parentId) ?: -1) + 1
        } else {
            0
        }
        
        conversationDao.insertNode(
            NodeEntity(
                id = message.id,
                conversationId = convId,
                parentId = message.parentId,
                role = message.role,
                content = message.content,
                branchIndex = branchIndex
            )
        )
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun clearChat() {
        _uiState.value = ChatUiState()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTree: (String) -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Reload conversation when returning from tree view
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.reloadConversation()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.content) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(message = error, duration = SnackbarDuration.Long)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.conversationTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToTree(uiState.conversationId) }) {
                        Icon(Icons.Default.AccountTree, contentDescription = "View Tree")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(uiState.messages.size) { index ->
                    val message = uiState.messages[index]
                    MessageBubble(
                        message = message,
                        isEditing = uiState.editingMessageId == message.id,
                        editingContent = if (uiState.editingMessageId == message.id) uiState.editingContent else "",
                        onEditContentChange = viewModel::updateEditingContent,
                        onStartEdit = { viewModel.startEditing(message.id) },
                        onCancelEdit = viewModel::cancelEditing,
                        onSaveEdit = viewModel::saveEdit,
                        onRegenerate = { viewModel.regenerateFrom(index) },
                        isLoading = uiState.isLoading
                    )
                }
            }
            
            ChatInputBar(
                value = uiState.currentInput,
                onValueChange = viewModel::updateInput,
                onSend = viewModel::sendMessage,
                isLoading = uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: UiMessage,
    isEditing: Boolean,
    editingContent: String,
    onEditContentChange: (String) -> Unit,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onRegenerate: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == ChatRole.USER
    var showMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column {
            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    ))
                    .background(if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .combinedClickable(
                        onClick = { },
                        onLongClick = { if (!message.isStreaming && !isLoading) showMenu = true }
                    )
                    .padding(12.dp)
            ) {
                if (isEditing) {
                    Column {
                        OutlinedTextField(
                            value = editingContent,
                            onValueChange = onEditContentChange,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                unfocusedTextColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                selectionColors = TextSelectionColors(
                                    handleColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                    backgroundColor = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                )
                            )
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = onCancelEdit) {
                                Text("Cancel", color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = onSaveEdit) {
                                Text("Save & Regenerate", color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                } else if (message.content.isEmpty() && message.isStreaming) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp), strokeWidth = 2.dp,
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Thinking...", style = MaterialTheme.typography.bodyMedium, 
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    if (isUser) {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        MarkdownText(
                            markdown = message.content,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
                
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (isUser) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = { showMenu = false; onStartEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                clipboardManager.setText(AnnotatedString(message.content))
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Regenerate") },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                            onClick = { showMenu = false; onRegenerate() }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                clipboardManager.setText(AnnotatedString(message.content))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, tonalElevation = 3.dp) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Default,
                    capitalization = KeyboardCapitalization.Sentences
                ),
                enabled = !isLoading
            )
            
            IconButton(onClick = onSend, enabled = value.isNotBlank() && !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send",
                        tint = if (value.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
