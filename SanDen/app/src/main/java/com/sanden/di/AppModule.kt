package com.sanden.di

import android.content.Context
import com.sanden.data.scanner.CategoryDetector
import com.sanden.data.storage.DropFolderStorage
import com.sanden.data.storage.PrivateImportStorage
import com.sanden.data.storage.StorageModeManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCategoryDetector(): CategoryDetector {
        return CategoryDetector
    }

    @Provides
    @Singleton
    fun provideDropFolderStorage(
        @ApplicationContext context: Context
    ): DropFolderStorage {
        return DropFolderStorage(context)
    }

    @Provides
    @Singleton
    fun providePrivateImportStorage(
        @ApplicationContext context: Context
    ): PrivateImportStorage {
        return PrivateImportStorage(context)
    }

    @Provides
    @Singleton
    fun provideStorageModeManager(
        @ApplicationContext context: Context,
        dropFolderStorage: DropFolderStorage,
        privateImportStorage: PrivateImportStorage
    ): StorageModeManager {
        return StorageModeManager(context, dropFolderStorage, privateImportStorage)
    }
}
