package com.loom.bedrock.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loom.bedrock.data.local.ConversationDao
import com.loom.bedrock.data.local.ConversationEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class RecycleBinViewModel @Inject constructor(
    private val conversationDao: ConversationDao
) : ViewModel() {

    val deletedConversations = conversationDao.getDeletedConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restoreConversation(id: String) {
        viewModelScope.launch {
            conversationDao.restoreConversation(id)
        }
    }

    fun permanentlyDeleteConversation(id: String) {
        viewModelScope.launch {
            conversationDao.permanentlyDeleteConversation(id)
        }
    }

    fun cleanupOldDeletedConversations() {
        viewModelScope.launch {
            val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
            conversationDao.deleteOldDeletedConversations(sevenDaysAgo)
        }
    }

    init {
        // Automatically clean up conversations older than 7 days when opening recycle bin
        cleanupOldDeletedConversations()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecycleBinViewModel = hiltViewModel()
) {
    val deletedConversations by viewModel.deletedConversations.collectAsState()
    var conversationToRestore by remember { mutableStateOf<ConversationEntity?>(null) }
    var conversationToPermanentlyDelete by remember { mutableStateOf<ConversationEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recycle Bin") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (deletedConversations.isEmpty()) {
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
                        text = "Recycle bin is empty",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Deleted conversations appear here for 7 days",
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
                items(deletedConversations, key = { it.id }) { conversation ->
                    DeletedConversationCard(
                        conversation = conversation,
                        onRestore = { conversationToRestore = conversation },
                        onPermanentlyDelete = { conversationToPermanentlyDelete = conversation }
                    )
                }
            }
        }
    }

    // Restore confirmation dialog
    conversationToRestore?.let { conversation ->
        AlertDialog(
            onDismissRequest = { conversationToRestore = null },
            title = { Text("Restore Conversation") },
            text = { Text("Restore \"${conversation.title}\" to your conversations?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.restoreConversation(conversation.id)
                        conversationToRestore = null
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToRestore = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Permanently delete confirmation dialog
    conversationToPermanentlyDelete?.let { conversation ->
        AlertDialog(
            onDismissRequest = { conversationToPermanentlyDelete = null },
            title = { Text("Permanently Delete") },
            text = { Text("Permanently delete \"${conversation.title}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.permanentlyDeleteConversation(conversation.id)
                        conversationToPermanentlyDelete = null
                    }
                ) {
                    Text("Delete Forever", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToPermanentlyDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeletedConversationCard(
    conversation: ConversationEntity,
    onRestore: () -> Unit,
    onPermanentlyDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()) }
    val deletedDate = remember(conversation.deletedAt) {
        conversation.deletedAt?.let { dateFormat.format(Date(it)) } ?: ""
    }

    val daysRemaining = remember(conversation.deletedAt) {
        conversation.deletedAt?.let {
            val daysLeft = 7 - TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - it)
            maxOf(0, daysLeft)
        } ?: 0
    }

    Card(
        modifier = Modifier.fillMaxWidth()
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
                    text = "Deleted $deletedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$daysRemaining day${if (daysRemaining != 1L) "s" else ""} remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (daysRemaining <= 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onRestore) {
                    Icon(
                        Icons.Default.Restore,
                        contentDescription = "Restore",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onPermanentlyDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Forever",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
