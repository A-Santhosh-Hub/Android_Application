package com.sanden.di

import android.content.Context
import androidx.room.Room
import com.sanden.data.db.SanDenDatabase
import com.sanden.data.db.VaultFileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "sanden_vault.db"

    @Provides
    @Singleton
    fun provideSanDenDatabase(
        @ApplicationContext context: Context
    ): SanDenDatabase {
        return Room.databaseBuilder(
            context,
            SanDenDatabase::class.java,
            DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideVaultFileDao(
        database: SanDenDatabase
    ): VaultFileDao {
        return database.vaultFileDao()
    }
}
