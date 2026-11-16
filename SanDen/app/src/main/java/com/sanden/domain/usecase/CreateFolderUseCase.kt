package com.sanden.domain.usecase

import com.sanden.domain.model.VaultFile
import com.sanden.domain.repository.VaultRepository
import javax.inject.Inject

/**
 * Creates a new sub-folder inside the vault.
 */
class CreateFolderUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    suspend operator fun invoke(parentPath: String, name: String): Result<VaultFile> {
        return repository.createFolder(parentPath, name)
    }
}
