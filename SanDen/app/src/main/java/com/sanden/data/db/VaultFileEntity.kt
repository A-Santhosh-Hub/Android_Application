package com.sanden.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.sanden.domain.model.FileCategory
import com.sanden.domain.model.VaultFile

@Entity(
    tableName = "vault_files",
    indices = [
        Index(value = ["parentPath"]),
        Index(value = ["category"]),
        Index(value = ["name"]),
        Index(value = ["path"], unique = true)
    ]
)
@TypeConverters(FileCategoryConverter::class)
data class VaultFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "path")
    val path: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "parentPath")
    val parentPath: String,

    @ColumnInfo(name = "size")
    val size: Long,

    @ColumnInfo(name = "lastModified")
    val lastModified: Long,

    @ColumnInfo(name = "mimeType")
    val mimeType: String?,

    @ColumnInfo(name = "category")
    val category: FileCategory,

    @ColumnInfo(name = "isDirectory")
    val isDirectory: Boolean
)

class FileCategoryConverter {
    @TypeConverter
    fun fromFileCategory(category: FileCategory): String {
        return category.name
    }

    @TypeConverter
    fun toFileCategory(value: String): FileCategory {
        return try {
            FileCategory.valueOf(value)
        } catch (e: IllegalArgumentException) {
            FileCategory.OTHER
        }
    }
}

/**
 * Data class used by the DAO to return category aggregation results.
 */
data class CategoryCount(
    val category: FileCategory,
    val count: Int,
    val totalSize: Long
)

// ── Extension functions: Entity ↔ Domain Model ─────────────────────────

fun VaultFileEntity.toDomainModel(): VaultFile {
    return VaultFile(
        id = id,
        path = path,
        name = name,
        parentPath = parentPath,
        size = size,
        lastModified = lastModified,
        mimeType = mimeType,
        category = category,
        isDirectory = isDirectory
    )
}

fun VaultFile.toEntity(): VaultFileEntity {
    return VaultFileEntity(
        id = id,
        path = path,
        name = name,
        parentPath = parentPath,
        size = size,
        lastModified = lastModified,
        mimeType = mimeType,
        category = category,
        isDirectory = isDirectory
    )
}

fun List<VaultFileEntity>.toDomainModels(): List<VaultFile> = map { it.toDomainModel() }

fun List<VaultFile>.toEntities(): List<VaultFileEntity> = map { it.toEntity() }
