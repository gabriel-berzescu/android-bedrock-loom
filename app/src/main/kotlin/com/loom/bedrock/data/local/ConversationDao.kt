package com.loom.bedrock.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>
    
    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversation(id: String): ConversationEntity?
    
    @Transaction
    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationWithNodes(id: String): ConversationWithNodes?
    
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
    
    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)
    
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
