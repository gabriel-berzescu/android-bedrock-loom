package com.loom.bedrock.data.local

import androidx.room.*
import com.loom.bedrock.data.api.ChatRole

/**
 * A conversation tree (root container for all branches).
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val activeNodeId: String? = null,  // Currently selected node in the tree
    val deletedAt: Long? = null  // Timestamp when conversation was deleted (null if not deleted)
)

/**
 * A node in the conversation tree.
 * Each node is a message that can have multiple children (branches).
 */
@Entity(
    tableName = "nodes",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = NodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversationId"), Index("parentId")]
)
data class NodeEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val parentId: String?,  // null for root nodes
    val role: ChatRole,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val branchIndex: Int = 0  // Which sibling branch this is (0, 1, 2...)
)

// Keep backward compatibility alias
typealias MessageEntity = NodeEntity

/**
 * Conversation with all its nodes.
 */
data class ConversationWithNodes(
    @Embedded
    val conversation: ConversationEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "conversationId"
    )
    val nodes: List<NodeEntity>
)
