package com.example.callmind.di

import android.content.Context
import androidx.room.Room
import com.example.callmind.data.local.CallMindDatabase
import com.example.callmind.data.local.daos.NoteDao
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
    fun provideDatabase(@ApplicationContext context: Context): CallMindDatabase {
        return Room.databaseBuilder(
            context,
            CallMindDatabase::class.java,
            "callmind_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideNoteDao(database: CallMindDatabase): NoteDao {
        return database.noteDao()
    }

    @Provides
    fun provideReminderDao(database: CallMindDatabase): com.example.callmind.data.local.daos.ReminderDao {
        return database.reminderDao()
    }

    @Provides
    fun provideCommunicationLauncher(@ApplicationContext context: Context): com.example.callmind.core.communication.CommunicationLauncher {
        return com.example.callmind.core.communication.CommunicationLauncher(context)
    }
}
