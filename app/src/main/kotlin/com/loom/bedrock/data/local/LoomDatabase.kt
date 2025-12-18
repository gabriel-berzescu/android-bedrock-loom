package com.loom.bedrock.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.loom.bedrock.data.api.ChatRole

@Database(
    entities = [ConversationEntity::class, NodeEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LoomDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE nodes ADD COLUMN thinkingContent TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromChatRole(role: ChatRole): String = role.name
    
    @TypeConverter
    fun toChatRole(value: String): ChatRole = ChatRole.valueOf(value)
}
