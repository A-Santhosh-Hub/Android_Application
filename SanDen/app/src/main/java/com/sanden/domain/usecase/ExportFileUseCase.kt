package com.sanden.domain.usecase

import com.sanden.domain.model.FileOperation
import com.sanden.domain.model.VaultFile
import com.sanden.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Exports a vault file to the device's public/shared storage.
 */
class ExportFileUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    operator fun invoke(vaultFile: VaultFile): Flow<FileOperation> {
        return repository.exportFile(vaultFile)
    }
}
