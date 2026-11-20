package com.sanden.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_messages")
data class SyncMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val sender: String,       // "PHONE" or "LAPTOP"
    val type: String,         // "TEXT", "CLIPBOARD", "FILE"
    val textContent: String = "",
    val fileName: String = "",
    val filePath: String = "",
    val fileSize: Long = 0L,
    val mimeType: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
