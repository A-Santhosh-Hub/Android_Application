package com.sanden.domain.usecase

import com.sanden.domain.model.VaultFile
import com.sanden.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Retrieves the immediate children (files and sub-folders) of a given folder.
 */
class GetFolderContentsUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    operator fun invoke(parentPath: String): Flow<List<VaultFile>> {
        return repository.getFilesInFolder(parentPath)
    }
}
