package com.loom.bedrock.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loom.bedrock.data.api.ChatRole
import com.loom.bedrock.data.local.ConversationDao
import com.loom.bedrock.data.local.NodeEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TreeNode(
    val id: String,
    val role: ChatRole,
    val content: String,
    val children: MutableList<TreeNode> = mutableListOf(),
    var x: Float = 0f,
    var y: Float = 0f
)

data class TreeUiState(
    val rootNode: TreeNode? = null,
    val activeNodeId: String? = null,
    val selectedNodeId: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class TreeViewModel @Inject constructor(
    private val conversationDao: ConversationDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val conversationId: String = savedStateHandle.get<String>("conversationId") ?: ""
    
    private val _uiState = MutableStateFlow(TreeUiState())
    val uiState: StateFlow<TreeUiState> = _uiState.asStateFlow()
    
    init {
        loadTree()
    }
    
    private fun loadTree() {
        viewModelScope.launch {
            val conversationWithNodes = conversationDao.getConversationWithNodes(conversationId)
            if (conversationWithNodes != null) {
                val nodes = conversationWithNodes.nodes
                val nodeMap = nodes.associateBy { it.id }
                
                // Build tree structure
                val rootNodes = nodes.filter { it.parentId == null }.sortedBy { it.createdAt }
                val root = if (rootNodes.isNotEmpty()) {
                    buildTree(rootNodes.first(), nodeMap)
                } else {
                    null
                }
                
                // Layout the tree
                root?.let { layoutTree(it) }
                
                _uiState.value = _uiState.value.copy(
                    rootNode = root,
                    activeNodeId = conversationWithNodes.conversation.activeNodeId,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    private fun buildTree(nodeEntity: NodeEntity, nodeMap: Map<String, NodeEntity>): TreeNode {
        val treeNode = TreeNode(
            id = nodeEntity.id,
            role = nodeEntity.role,
            content = nodeEntity.content
        )
        
        // Find all children
        val children = nodeMap.values
            .filter { it.parentId == nodeEntity.id }
            .sortedBy { it.branchIndex }
        
        for (child in children) {
            treeNode.children.add(buildTree(child, nodeMap))
        }
        
        return treeNode
    }
    
    private fun layoutTree(root: TreeNode, startX: Float = 0f, startY: Float = 0f, levelHeight: Float = 150f) {
        // Simple layout: place nodes vertically with horizontal spreading for branches
        var currentY = startY
        val nodeWidth = 180f
        
        fun layout(node: TreeNode, x: Float, y: Float, depth: Int = 0) {
            node.x = x
            node.y = y
            
            if (node.children.isNotEmpty()) {
                val totalWidth = node.children.size * nodeWidth
                var childX = x - totalWidth / 2 + nodeWidth / 2
                
                for (child in node.children) {
                    layout(child, childX, y + levelHeight, depth + 1)
                    childX += nodeWidth
                }
            }
        }
        
        layout(root, startX, startY)
    }
    
    fun selectNode(nodeId: String) {
        _uiState.value = _uiState.value.copy(selectedNodeId = nodeId)
    }
    
    fun setActiveNode(nodeId: String) {
        viewModelScope.launch {
            conversationDao.setActiveNode(conversationId, nodeId)
            _uiState.value = _uiState.value.copy(activeNodeId = nodeId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNode: (nodeId: String) -> Unit,
    viewModel: TreeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var scale by remember { mutableFloatStateOf(0.8f) }
    var offsetX by remember { mutableFloatStateOf(450f) }
    var offsetY by remember { mutableFloatStateOf(100f) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conversation Tree") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.3f, 2f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.rootNode == null) {
                Text(
                    text = "No conversation data",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Draw tree
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                ) {
                    // Draw lines first
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        fun drawConnections(node: TreeNode) {
                            for (child in node.children) {
                                drawLine(
                                    color = Color.Gray,
                                    start = Offset(node.x + 75, node.y + 60),
                                    end = Offset(child.x + 75, child.y),
                                    strokeWidth = 2f
                                )
                                drawConnections(child)
                            }
                        }
                        uiState.rootNode?.let { drawConnections(it) }
                    }
                    
                    uiState.rootNode?.let {
                        TreeNodeComposable(
                            node = it,
                            activeNodeId = uiState.activeNodeId,
                            selectedNodeId = uiState.selectedNodeId,
                            onSelectNode = viewModel::selectNode,
                            onNavigateToNode = onNavigateToNode
                        )
                    }
                }
            }
            
            // Selected node details
            uiState.selectedNodeId?.let { selectedId ->
                uiState.rootNode?.let { root ->
                    findNode(root, selectedId)?.let { selectedNode ->
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = if (selectedNode.role == ChatRole.USER) "You" else "Claude",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = selectedNode.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 5,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { onNavigateToNode(selectedId) },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Continue from here")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TreeNodeComposable(
    node: TreeNode,
    activeNodeId: String?,
    selectedNodeId: String?,
    onSelectNode: (String) -> Unit,
    onNavigateToNode: (String) -> Unit
) {
    TreeNodeCard(
        node = node,
        isActive = node.id == activeNodeId,
        isSelected = node.id == selectedNodeId,
        onClick = { onSelectNode(node.id) },
        onDoubleClick = { onNavigateToNode(node.id) },
        modifier = Modifier.offset(x = node.x.dp, y = node.y.dp)
    )
    
    node.children.forEach { child ->
        TreeNodeComposable(
            node = child,
            activeNodeId = activeNodeId,
            selectedNodeId = selectedNodeId,
            onSelectNode = onSelectNode,
            onNavigateToNode = onNavigateToNode
        )
    }
}

@Composable
fun TreeNodeCard(
    node: TreeNode,
    isActive: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = node.role == ChatRole.USER
    
    Card(
        modifier = modifier
            .width(150.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                isActive -> MaterialTheme.colorScheme.tertiaryContainer
                isUser -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isUser) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.primary
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isUser) "You" else "Claude",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = node.content,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun findNode(root: TreeNode, id: String): TreeNode? {
    if (root.id == id) return root
    for (child in root.children) {
        findNode(child, id)?.let { return it }
    }
    return null
}
