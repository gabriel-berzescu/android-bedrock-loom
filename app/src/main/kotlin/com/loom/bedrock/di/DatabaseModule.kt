package com.loom.bedrock.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.loom.bedrock.data.local.ConversationDao
import com.loom.bedrock.data.local.LoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add deletedAt column to conversations table
            db.execSQL("ALTER TABLE conversations ADD COLUMN deletedAt INTEGER DEFAULT NULL")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LoomDatabase {
        return Room.databaseBuilder(
            context,
            LoomDatabase::class.java,
            "loom_database"
        )
        .addMigrations(MIGRATION_2_3)
        .fallbackToDestructiveMigration()
        .build()
    }
    
    @Provides
    @Singleton
    fun provideConversationDao(database: LoomDatabase): ConversationDao {
        return database.conversationDao()
    }
}
