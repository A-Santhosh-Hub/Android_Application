package com.sanden.domain.usecase

import com.sanden.domain.model.ScanResult
import com.sanden.domain.repository.VaultRepository
import javax.inject.Inject

/**
 * Triggers a full vault scan, reconciling the database with the file system.
 */
class ScanVaultUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    suspend operator fun invoke(): ScanResult {
        return repository.scanVault()
    }
}
