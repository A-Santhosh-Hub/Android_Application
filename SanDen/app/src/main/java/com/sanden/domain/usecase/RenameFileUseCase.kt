package com.sanden.domain.usecase

import com.sanden.domain.model.VaultFile
import com.sanden.domain.repository.VaultRepository
import javax.inject.Inject

/**
 * Renames a vault file or folder.
 */
class RenameFileUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    suspend operator fun invoke(file: VaultFile, newName: String): Result<VaultFile> {
        return repository.renameFile(file, newName)
    }
}
