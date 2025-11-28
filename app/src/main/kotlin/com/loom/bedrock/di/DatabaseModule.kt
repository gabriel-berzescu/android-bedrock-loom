package com.loom.bedrock.di

import android.content.Context
import androidx.room.Room
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
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LoomDatabase {
        return Room.databaseBuilder(
            context,
            LoomDatabase::class.java,
            "loom_database"
        ).fallbackToDestructiveMigration().build()
    }
    
    @Provides
    @Singleton
    fun provideConversationDao(database: LoomDatabase): ConversationDao {
        return database.conversationDao()
    }
}
