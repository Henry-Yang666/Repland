package com.swan1127.repland.domain.ports

import com.swan1127.repland.domain.model.UsageSummaryAttempt
import com.swan1127.repland.domain.model.UsageSummaryDocument
import com.swan1127.repland.domain.model.UsageSummarySnapshot
import kotlinx.coroutines.flow.Flow

interface UsageSummaryRepository {
    fun observe(): Flow<UsageSummarySnapshot>
    suspend fun refresh(): UsageSummarySnapshot
    fun observeSummary(): Flow<UsageSummaryDocument?>
    /** A stale response cannot replace the document belonging to newer evidence or access. */
    suspend fun saveSummary(document: UsageSummaryDocument)
    fun observeAttempt(): Flow<UsageSummaryAttempt?>
    /** Recorded before dispatch so a process restart cannot bypass the request backoff. */
    suspend fun saveAttempt(attempt: UsageSummaryAttempt)
}
