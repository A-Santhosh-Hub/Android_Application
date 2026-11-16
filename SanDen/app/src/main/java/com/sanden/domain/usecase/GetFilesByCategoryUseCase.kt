package com.sanden.domain.usecase

import com.sanden.domain.model.FileCategory
import com.sanden.domain.model.VaultFile
import com.sanden.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Retrieves all vault files belonging to a specific [FileCategory].
 */
class GetFilesByCategoryUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    operator fun invoke(category: FileCategory): Flow<List<VaultFile>> {
        return repository.getFilesByCategory(category)
    }
}
