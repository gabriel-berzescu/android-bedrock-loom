package com.loom.bedrock.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Settings
import android.content.Context
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loom.bedrock.data.local.ConversationDao
import com.loom.bedrock.data.local.ConversationEntity
import com.loom.bedrock.data.local.ConversationWithNodes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@Serializable
data class ExportedConversation(
    val conversation: ExportedConversationEntity,
    val nodes: List<ExportedNodeEntity>
)

@Serializable
data class ExportedConversationEntity(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val activeNodeId: String?
)

@Serializable
data class ExportedNodeEntity(
    val id: String,
    val conversationId: String,
    val parentId: String?,
    val role: String,
    val content: String,
    val createdAt: Long,
    val branchIndex: Int
)

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val conversationDao: ConversationDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val conversations = conversationDao.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val json = Json { prettyPrint = true }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            conversationDao.deleteConversation(id)
        }
    }

    fun renameConversation(id: String, newTitle: String) {
        viewModelScope.launch {
            val conversation = conversationDao.getConversation(id)
            if (conversation != null) {
                conversationDao.updateConversation(id, newTitle)
            }
        }
    }

    suspend fun exportConversation(id: String): String? {
        val conversationWithNodes = conversationDao.getConversationWithNodes(id) ?: return null

        val exported = ExportedConversation(
            conversation = ExportedConversationEntity(
                id = conversationWithNodes.conversation.id,
                title = conversationWithNodes.conversation.title,
                createdAt = conversationWithNodes.conversation.createdAt,
                updatedAt = conversationWithNodes.conversation.updatedAt,
                activeNodeId = conversationWithNodes.conversation.activeNodeId
            ),
            nodes = conversationWithNodes.nodes.map { node ->
                ExportedNodeEntity(
                    id = node.id,
                    conversationId = node.conversationId,
                    parentId = node.parentId,
                    role = node.role.name,
                    content = node.content,
                    createdAt = node.createdAt,
                    branchIndex = node.branchIndex
                )
            }
        )

        return json.encodeToString(exported)
    }

    suspend fun importConversation(jsonContent: String): Result<String> {
        return try {
            val exported = json.decodeFromString<ExportedConversation>(jsonContent)

            val conversation = ConversationEntity(
                id = exported.conversation.id,
                title = exported.conversation.title,
                createdAt = exported.conversation.createdAt,
                updatedAt = exported.conversation.updatedAt,
                activeNodeId = exported.conversation.activeNodeId
            )

            conversationDao.insertConversation(conversation)

            exported.nodes.forEach { node ->
                conversationDao.insertNode(
                    com.loom.bedrock.data.local.NodeEntity(
                        id = node.id,
                        conversationId = node.conversationId,
                        parentId = node.parentId,
                        role = com.loom.bedrock.data.api.ChatRole.valueOf(node.role),
                        content = node.content,
                        createdAt = node.createdAt,
                        branchIndex = node.branchIndex
                    )
                )
            }

            Result.success(exported.conversation.title)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToChat: (conversationId: String?) -> Unit,
    viewModel: ConversationListViewModel = hiltViewModel()
) {
    val conversations by viewModel.conversations.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var conversationToDelete by remember { mutableStateOf<ConversationEntity?>(null) }
    var conversationToRename by remember { mutableStateOf<ConversationEntity?>(null) }
    var conversationToExport by remember { mutableStateOf<ConversationEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var importErrorMessage by remember { mutableStateOf<String?>(null) }
    var importSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Import launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val jsonContent = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (jsonContent != null) {
                        val result = viewModel.importConversation(jsonContent)
                        result.onSuccess { title ->
                            importSuccessMessage = "Imported \"$title\""
                        }.onFailure { error ->
                            importErrorMessage = "Import failed: ${error.message}"
                        }
                    }
                } catch (e: Exception) {
                    importErrorMessage = "Import failed: ${e.message}"
                }
            }
        }
    }

    // Export launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            conversationToExport?.let { conversation ->
                coroutineScope.launch {
                    val jsonContent = viewModel.exportConversation(conversation.id)
                    if (jsonContent != null) {
                        try {
                            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                                it.write(jsonContent)
                            }
                            importSuccessMessage = "Exported \"${conversation.title}\""
                        } catch (e: Exception) {
                            importErrorMessage = "Export failed: ${e.message}"
                        }
                    }
                    conversationToExport = null
                }
            }
        }
    }
    
    // Show snackbar for import/export messages
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(importSuccessMessage) {
        importSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            importSuccessMessage = null
        }
    }
    LaunchedEffect(importErrorMessage) {
        importErrorMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
            importErrorMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bedrock Loom") },
                actions = {
                    IconButton(onClick = { importLauncher.launch("application/json") }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Import Conversation")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToChat(null) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Chat")
            }
        }
    ) { padding ->
        if (conversations.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "No conversations yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap + to start a new chat",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(conversations, key = { it.id }) { conversation ->
                    ConversationCard(
                        conversation = conversation,
                        onClick = { onNavigateToChat(conversation.id) },
                        onLongClick = {
                            conversationToRename = conversation
                            renameText = conversation.title
                        },
                        onDelete = { conversationToDelete = conversation },
                        onExport = {
                            conversationToExport = conversation
                            val fileName = "${conversation.title.replace(Regex("[^a-zA-Z0-9.-]"), "_")}.json"
                            exportLauncher.launch(fileName)
                        }
                    )
                }
            }
        }
    }
    
    // Delete confirmation dialog
    conversationToDelete?.let { conversation ->
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = { Text("Delete Conversation") },
            text = { Text("Are you sure you want to delete \"${conversation.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteConversation(conversation.id)
                        conversationToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename dialog
    conversationToRename?.let { conversation ->
        AlertDialog(
            onDismissRequest = {
                conversationToRename = null
                renameText = ""
            },
            title = { Text("Rename Conversation") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Title") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.renameConversation(conversation.id, renameText.trim())
                        }
                        conversationToRename = null
                        renameText = ""
                    },
                    enabled = renameText.isNotBlank()
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        conversationToRename = null
                        renameText = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationCard(
    conversation: ConversationEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = conversation.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(Date(conversation.updatedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                IconButton(onClick = onExport) {
                    Icon(
                        Icons.Default.FileDownload,
                        contentDescription = "Export",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
