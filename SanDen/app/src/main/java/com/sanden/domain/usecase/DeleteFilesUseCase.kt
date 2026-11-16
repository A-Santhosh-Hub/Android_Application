package com.sanden.domain.usecase

import com.sanden.domain.model.FileOperation
import com.sanden.domain.model.VaultFile
import com.sanden.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Deletes one or more files from the vault.
 */
class DeleteFilesUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    operator fun invoke(files: List<VaultFile>): Flow<FileOperation> {
        return repository.deleteFiles(files)
    }
}
