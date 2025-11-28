package com.loom.bedrock.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Unique identifier for a node in the Loom tree.
 */
@JvmInline
@Serializable
value class NodeId(val value: String) {
    companion object {
        fun generate(): NodeId = NodeId(UUID.randomUUID().toString())
    }
}

/**
 * A single node in the Loom tree, representing one piece of generated or user-entered content.
 * 
 * The tree is navigated by following parent/child relationships, with the full "prompt"
 * being the concatenation of all ancestor content from root to the current node.
 */
@Serializable
data class LoomNode(
    val id: NodeId,
    val content: String,
    val parentId: NodeId?,
    val childIds: List<NodeId> = emptyList(),
    val metadata: NodeMetadata,
    val annotations: NodeAnnotations = NodeAnnotations(),
    val createdAt: Long = System.currentTimeMillis(),
) {
    /**
     * Whether this node is the root of a tree (has no parent).
     */
    val isRoot: Boolean get() = parentId == null
    
    /**
     * Whether this node is a leaf (has no children).
     */
    val isLeaf: Boolean get() = childIds.isEmpty()
    
    /**
     * Whether this node has been starred by the user.
     */
    val isStarred: Boolean get() = annotations.starred
    
    /**
     * Whether this node is hidden from view.
     */
    val isHidden: Boolean get() = annotations.hidden
}

/**
 * Metadata about how a node was generated.
 */
@Serializable
data class NodeMetadata(
    val modelId: String? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val maxTokens: Int? = null,
    val actualTokenCount: Int? = null,
    val inputTokens: Int? = null,
    val outputTokens: Int? = null,
    val latencyMs: Long? = null,
    val isUserContent: Boolean = false,
) {
    companion object {
        /**
         * Create metadata for user-entered content (not generated).
         */
        fun userContent() = NodeMetadata(isUserContent = true)
    }
}

/**
 * User annotations on a node.
 */
@Serializable
data class NodeAnnotations(
    val starred: Boolean = false,
    val hidden: Boolean = false,
    val tags: Set<String> = emptySet(),
    val note: String? = null,
)

/**
 * Extension function to add a child ID to a node.
 */
fun LoomNode.withChild(childId: NodeId): LoomNode =
    copy(childIds = childIds + childId)

/**
 * Extension function to remove a child ID from a node.
 */
fun LoomNode.withoutChild(childId: NodeId): LoomNode =
    copy(childIds = childIds - childId)

/**
 * Extension function to toggle the starred state.
 */
fun LoomNode.toggleStarred(): LoomNode =
    copy(annotations = annotations.copy(starred = !annotations.starred))

/**
 * Extension function to toggle the hidden state.
 */
fun LoomNode.toggleHidden(): LoomNode =
    copy(annotations = annotations.copy(hidden = !annotations.hidden))

/**
 * Extension function to add a tag.
 */
fun LoomNode.withTag(tag: String): LoomNode =
    copy(annotations = annotations.copy(tags = annotations.tags + tag))

/**
 * Extension function to remove a tag.
 */
fun LoomNode.withoutTag(tag: String): LoomNode =
    copy(annotations = annotations.copy(tags = annotations.tags - tag))
