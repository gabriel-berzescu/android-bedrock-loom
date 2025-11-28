package com.loom.bedrock.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Unique identifier for a Loom tree.
 */
@JvmInline
@Serializable
value class TreeId(val value: String) {
    companion object {
        fun generate(): TreeId = TreeId(UUID.randomUUID().toString())
    }
}

/**
 * A complete Loom tree representing a multiverse of generated content.
 * 
 * The tree is stored as a map of nodes indexed by their IDs, with a reference
 * to the root node. The playhead indicates the current navigation position.
 */
@Serializable
data class LoomTree(
    val id: TreeId,
    val name: String,
    val rootId: NodeId,
    val nodes: Map<NodeId, LoomNode>,
    val playheadId: NodeId,
    val systemPrompt: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    /**
     * The root node of the tree.
     */
    val root: LoomNode get() = nodes[rootId] ?: error("Root node not found")
    
    /**
     * The current playhead node.
     */
    val playhead: LoomNode get() = nodes[playheadId] ?: error("Playhead node not found")
    
    /**
     * Total number of nodes in the tree.
     */
    val nodeCount: Int get() = nodes.size
    
    /**
     * Get a node by ID, or null if not found.
     */
    operator fun get(nodeId: NodeId): LoomNode? = nodes[nodeId]
    
    /**
     * Check if a node exists in the tree.
     */
    operator fun contains(nodeId: NodeId): Boolean = nodeId in nodes
    
    /**
     * Get the ancestry (path from root) to a given node.
     */
    fun getAncestry(nodeId: NodeId): List<LoomNode> {
        val ancestry = mutableListOf<LoomNode>()
        var current = nodes[nodeId]
        
        while (current != null) {
            ancestry.add(0, current)
            current = current.parentId?.let { nodes[it] }
        }
        
        return ancestry
    }
    
    /**
     * Get the full text content from root to a given node.
     */
    fun getFullText(nodeId: NodeId, separator: String = ""): String {
        return getAncestry(nodeId).joinToString(separator) { it.content }
    }
    
    /**
     * Get the children of a node.
     */
    fun getChildren(nodeId: NodeId): List<LoomNode> {
        val node = nodes[nodeId] ?: return emptyList()
        return node.childIds.mapNotNull { nodes[it] }
    }
    
    /**
     * Get the parent of a node.
     */
    fun getParent(nodeId: NodeId): LoomNode? {
        val node = nodes[nodeId] ?: return null
        return node.parentId?.let { nodes[it] }
    }
    
    /**
     * Get all descendants of a node (including the node itself).
     */
    fun getDescendants(nodeId: NodeId): List<LoomNode> {
        val descendants = mutableListOf<LoomNode>()
        val queue = ArrayDeque<NodeId>()
        queue.add(nodeId)
        
        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            val current = nodes[currentId] ?: continue
            descendants.add(current)
            queue.addAll(current.childIds)
        }
        
        return descendants
    }
    
    /**
     * Get all starred nodes in the tree.
     */
    fun getStarredNodes(): List<LoomNode> {
        return nodes.values.filter { it.isStarred }
    }
    
    /**
     * Get all leaf nodes in the tree.
     */
    fun getLeafNodes(): List<LoomNode> {
        return nodes.values.filter { it.isLeaf }
    }
    
    /**
     * Search for nodes containing the given text.
     */
    fun search(query: String, ignoreCase: Boolean = true): List<LoomNode> {
        return nodes.values.filter { 
            it.content.contains(query, ignoreCase) 
        }
    }
    
    /**
     * Get nodes with a specific tag.
     */
    fun getNodesByTag(tag: String): List<LoomNode> {
        return nodes.values.filter { tag in it.annotations.tags }
    }
    
    /**
     * Get all unique tags used in the tree.
     */
    fun getAllTags(): Set<String> {
        return nodes.values.flatMap { it.annotations.tags }.toSet()
    }
    
    /**
     * Calculate the depth (distance from root) of a node.
     */
    fun getDepth(nodeId: NodeId): Int {
        return getAncestry(nodeId).size - 1
    }
    
    /**
     * Get the maximum depth of the tree.
     */
    fun getMaxDepth(): Int {
        return nodes.keys.maxOfOrNull { getDepth(it) } ?: 0
    }
    
    companion object {
        /**
         * Create a new tree with a root node.
         */
        fun create(
            name: String,
            rootContent: String,
            systemPrompt: String? = null,
        ): LoomTree {
            val treeId = TreeId.generate()
            val rootId = NodeId.generate()
            val rootNode = LoomNode(
                id = rootId,
                content = rootContent,
                parentId = null,
                metadata = NodeMetadata.userContent(),
            )
            
            return LoomTree(
                id = treeId,
                name = name,
                rootId = rootId,
                nodes = mapOf(rootId to rootNode),
                playheadId = rootId,
                systemPrompt = systemPrompt,
            )
        }
    }
}

/**
 * Extension function to add a node to the tree.
 */
fun LoomTree.withNode(node: LoomNode): LoomTree {
    val updatedNodes = nodes.toMutableMap()
    updatedNodes[node.id] = node
    
    // Update parent's children list if this is not the root
    node.parentId?.let { parentId ->
        updatedNodes[parentId]?.let { parent ->
            if (node.id !in parent.childIds) {
                updatedNodes[parentId] = parent.withChild(node.id)
            }
        }
    }
    
    return copy(
        nodes = updatedNodes,
        updatedAt = System.currentTimeMillis(),
    )
}

/**
 * Extension function to update a node in the tree.
 */
fun LoomTree.withUpdatedNode(nodeId: NodeId, transform: (LoomNode) -> LoomNode): LoomTree {
    val node = nodes[nodeId] ?: return this
    val updatedNode = transform(node)
    return copy(
        nodes = nodes + (nodeId to updatedNode),
        updatedAt = System.currentTimeMillis(),
    )
}

/**
 * Extension function to remove a node and all its descendants from the tree.
 */
fun LoomTree.withoutSubtree(nodeId: NodeId): LoomTree {
    if (nodeId == rootId) {
        error("Cannot remove root node")
    }
    
    val toRemove = getDescendants(nodeId).map { it.id }.toSet()
    val updatedNodes = nodes.toMutableMap()
    
    // Remove all descendants
    toRemove.forEach { updatedNodes.remove(it) }
    
    // Update parent's children list
    val node = nodes[nodeId]
    node?.parentId?.let { parentId ->
        updatedNodes[parentId]?.let { parent ->
            updatedNodes[parentId] = parent.withoutChild(nodeId)
        }
    }
    
    // Update playhead if it was removed
    val newPlayheadId = if (playheadId in toRemove) {
        node?.parentId ?: rootId
    } else {
        playheadId
    }
    
    return copy(
        nodes = updatedNodes,
        playheadId = newPlayheadId,
        updatedAt = System.currentTimeMillis(),
    )
}

/**
 * Extension function to move the playhead to a different node.
 */
fun LoomTree.withPlayhead(nodeId: NodeId): LoomTree {
    require(nodeId in nodes) { "Node not found in tree" }
    return copy(playheadId = nodeId)
}
