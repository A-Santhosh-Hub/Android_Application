package com.sanden.domain.usecase

import com.sanden.domain.model.VaultFile
import com.sanden.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Searches vault files by name or metadata query.
 */
class SearchFilesUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    operator fun invoke(query: String): Flow<List<VaultFile>> {
        return repository.searchFiles(query)
    }
}
