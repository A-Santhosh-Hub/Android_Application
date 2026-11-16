package com.sanden.domain.model

/**
 * Defines how the vault ingests files.
 *
 * [DROP_FOLDER] – The vault watches a designated folder and indexes files in-place.
 * [PRIVATE_IMPORT] – Files are copied into the app's private/internal storage on import.
 */
enum class StorageMode {
    /** Vault indexes files from a user-visible drop folder. */
    DROP_FOLDER,

    /** Vault copies files into private app storage on import. */
    PRIVATE_IMPORT
}
