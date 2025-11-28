package com.loom.bedrock.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.loom.bedrock.data.api.ChatRole

@Database(
    entities = [ConversationEntity::class, NodeEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LoomDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
}

class Converters {
    @TypeConverter
    fun fromChatRole(role: ChatRole): String = role.name
    
    @TypeConverter
    fun toChatRole(value: String): ChatRole = ChatRole.valueOf(value)
}
