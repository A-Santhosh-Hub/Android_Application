package com.sanden.domain.usecase

import com.sanden.domain.model.FileCategory
import com.sanden.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Retrieves per-category statistics: file count and total size in bytes.
 */
class GetCategoryStatsUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    /**
     * @return a flow emitting a map of [FileCategory] to **(count, totalSizeBytes)**.
     */
    operator fun invoke(): Flow<Map<FileCategory, Pair<Int, Long>>> {
        return repository.getCategoryCounts()
    }
}
