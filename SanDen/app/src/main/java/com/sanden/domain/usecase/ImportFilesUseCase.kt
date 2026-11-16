package com.sanden.domain.usecase

import android.net.Uri
import com.sanden.domain.model.FileOperation
import com.sanden.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Imports one or more files (identified by content URIs) into the vault.
 */
class ImportFilesUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    operator fun invoke(uris: List<Uri>, destFolder: String): Flow<FileOperation> {
        return repository.importFiles(uris, destFolder)
    }
}
