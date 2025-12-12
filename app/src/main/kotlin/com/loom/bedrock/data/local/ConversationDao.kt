package com.loom.bedrock.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    
    @Query("SELECT * FROM conversations WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id AND deletedAt IS NULL")
    suspend fun getConversation(id: String): ConversationEntity?

    @Transaction
    @Query("SELECT * FROM conversations WHERE id = :id AND deletedAt IS NULL")
    suspend fun getConversationWithNodes(id: String): ConversationWithNodes?

    @Query("SELECT * FROM conversations WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getDeletedConversations(): Flow<List<ConversationEntity>>

    @Query("UPDATE conversations SET deletedAt = NULL WHERE id = :id")
    suspend fun restoreConversation(id: String)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: NodeEntity)
    
    @Query("SELECT * FROM nodes WHERE conversationId = :conversationId")
    suspend fun getNodesForConversation(conversationId: String): List<NodeEntity>
    
    @Query("SELECT * FROM nodes WHERE parentId = :parentId ORDER BY branchIndex")
    suspend fun getChildNodes(parentId: String): List<NodeEntity>
    
    @Query("SELECT * FROM nodes WHERE conversationId = :conversationId AND parentId IS NULL ORDER BY createdAt LIMIT 1")
    suspend fun getRootNode(conversationId: String): NodeEntity?
    
    @Query("SELECT MAX(branchIndex) FROM nodes WHERE parentId = :parentId")
    suspend fun getMaxBranchIndex(parentId: String): Int?
    
    @Query("UPDATE conversations SET updatedAt = :timestamp, title = :title WHERE id = :id")
    suspend fun updateConversation(id: String, title: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE conversations SET activeNodeId = :nodeId, updatedAt = :timestamp WHERE id = :id")
    suspend fun setActiveNode(id: String, nodeId: String?, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET deletedAt = :timestamp WHERE id = :id")
    suspend fun deleteConversation(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun permanentlyDeleteConversation(id: String)

    @Query("DELETE FROM conversations WHERE deletedAt IS NOT NULL AND deletedAt < :cutoffTime")
    suspend fun deleteOldDeletedConversations(cutoffTime: Long)

    @Query("DELETE FROM nodes WHERE conversationId = :conversationId")
    suspend fun deleteNodesForConversation(conversationId: String)
    
    @Query("SELECT COUNT(*) FROM nodes WHERE conversationId = :conversationId")
    suspend fun getNodeCount(conversationId: String): Int
    
    // Legacy compatibility
    suspend fun getConversationWithMessages(id: String) = getConversationWithNodes(id)
    suspend fun insertMessage(message: NodeEntity) = insertNode(message)
    suspend fun deleteMessagesForConversation(conversationId: String) = deleteNodesForConversation(conversationId)
    suspend fun getMessageCount(conversationId: String) = getNodeCount(conversationId)
}
