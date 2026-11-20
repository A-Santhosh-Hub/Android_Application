package com.sanden.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [VaultFileEntity::class, SyncMessageEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(FileCategoryConverter::class)
abstract class SanDenDatabase : RoomDatabase() {

    abstract fun vaultFileDao(): VaultFileDao
    abstract fun syncMessageDao(): SyncMessageDao

    companion object {
        const val DATABASE_NAME = "sanden_vault.db"
    }
}
