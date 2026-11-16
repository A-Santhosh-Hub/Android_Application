package com.sanden.domain.usecase

import com.sanden.domain.model.FileOperation
import com.sanden.domain.model.VaultFile
import com.sanden.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Moves one or more vault files into a different folder.
 */
class MoveFilesUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    operator fun invoke(files: List<VaultFile>, destFolder: String): Flow<FileOperation> {
        return repository.moveFiles(files, destFolder)
    }
}
