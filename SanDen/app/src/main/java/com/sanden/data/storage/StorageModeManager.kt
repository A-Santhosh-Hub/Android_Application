package com.sanden.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.sanden.domain.model.StorageMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the current [StorageMode] and provides the correct [VaultStorage]
 * implementation based on the persisted user preference.
 *
 * The mode is stored in [SharedPreferences] and survives app restarts.
 */
@Singleton
class StorageModeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dropFolderStorage: DropFolderStorage,
    private val privateImportStorage: PrivateImportStorage
) {

    companion object {
        private const val PREFS_NAME = "sanden_storage_prefs"
        private const val KEY_STORAGE_MODE = "storage_mode"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Returns the currently configured [StorageMode].
     */
    fun getMode(): StorageMode {
        val stored = prefs.getString(KEY_STORAGE_MODE, StorageMode.DROP_FOLDER.name)
        return try {
            StorageMode.valueOf(stored ?: StorageMode.DROP_FOLDER.name)
        } catch (e: IllegalArgumentException) {
            StorageMode.DROP_FOLDER
        }
    }

    /**
     * Persists the given [StorageMode].
     */
    fun setMode(mode: StorageMode) {
        prefs.edit()
            .putString(KEY_STORAGE_MODE, mode.name)
            .apply()
    }

    /**
     * Returns the [VaultStorage] implementation that matches the current mode.
     */
    fun getStorage(): VaultStorage {
        return when (getMode()) {
            StorageMode.DROP_FOLDER -> dropFolderStorage
            StorageMode.PRIVATE_IMPORT -> privateImportStorage
        }
    }
}
