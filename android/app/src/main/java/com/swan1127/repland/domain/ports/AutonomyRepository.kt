package com.swan1127.repland.domain.ports

import com.swan1127.repland.domain.model.ArrangementCandidate
import com.swan1127.repland.domain.model.AutonomousStatusCommand
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class AutonomyState(
    val enabled: Boolean = true,
    val summary: String? = null,
    val canUndo: Boolean = false,
)

/** One explicit utterance is one atomic operation, including its planning consequences. */
interface AutonomyRepository {
    fun observe(): Flow<AutonomyState>
    /** Emits on planning inputs, not on receipts, so an automatic repair cannot trigger itself forever. */
    fun observeInputRevision(): Flow<String>
    suspend fun setEnabled(enabled: Boolean)
    suspend fun capture(candidates: List<ArrangementCandidate>, date: LocalDate, dateExplicit: Boolean,
                        requestId: String, expectedRevision: String? = null, originalText: String? = null): String
    suspend fun updateStatus(commands: List<AutonomousStatusCommand>, requestId: String,
                             contextTaskId: String? = null): String
    suspend fun arrange(date: LocalDate, requestId: String, startMinute: Int = 0, endMinute: Int = 1440): String
    suspend fun reconcile(): String?
    suspend fun undo(): String
}
