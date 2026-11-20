package com.sanden.di

import com.sanden.data.repository.VaultRepositoryImpl
import com.sanden.data.storage.DropFolderStorage
import com.sanden.data.storage.PrivateImportStorage
import com.sanden.data.storage.VaultStorage
import com.sanden.data.transfer.LanFileTransferProvider
import com.sanden.domain.repository.FileTransferProvider
import com.sanden.domain.repository.VaultRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {

    @Binds
    @Singleton
    abstract fun bindVaultRepository(
        impl: VaultRepositoryImpl
    ): VaultRepository

    @Binds
    @Singleton
    abstract fun bindFileTransferProvider(
        impl: LanFileTransferProvider
    ): FileTransferProvider

    companion object {

        @Provides
        @Singleton
        @Named("dropFolder")
        fun provideNamedDropFolderStorage(
            storage: DropFolderStorage
        ): VaultStorage = storage

        @Provides
        @Singleton
        @Named("privateImport")
        fun provideNamedPrivateImportStorage(
            storage: PrivateImportStorage
        ): VaultStorage = storage
    }
}
