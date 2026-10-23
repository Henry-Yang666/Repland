package com.swan1127.repland

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swan1127.repland.data.room.*
import com.swan1127.repland.domain.model.*
import com.swan1127.repland.domain.ports.AiProviderConfigRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class UsageSummaryRepositoryTest {
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private fun memory() = Room.inMemoryDatabaseBuilder(context, ReplandDatabase::class.java).build()
    private suspend fun task(db: ReplandDatabase, id: String = "physics", now: Long = System.currentTimeMillis()) {
        db.taskDao().insert(TaskEntity(id, "复习物理", "复习物理", TaskCategory.COURSE.name, TaskPriority.MEDIUM.name,
            null, 45, null, TaskStatus.NOT_STARTED.name, null, null, null, 0, now - 1000, now - 1000,
            inputSources = TaskInputSources.forInput(TaskCategory.COURSE, TaskPriority.MEDIUM, null, 45, null, null).encode()))
    }
    private fun local(snapshot: UsageSummarySnapshot, at: Long) = UsageSummaryDocument(snapshot.revision, at, "LOCAL",
        snapshot.localSummary, snapshot.localHighlights, snapshot.facts.map { it.id }, snapshot.operationCount,
        windowStart = snapshot.windowStart, windowEnd = snapshot.windowEnd, facts = snapshot.facts)
    private class Config : AiProviderConfigRepository {
        val value = MutableStateFlow(AiProviderConfig(hasApiKey = true, updatedAtEpochMillis = 1))
        override fun observe() = value
        override suspend fun save(baseUrl: String, model: String, apiKey: String) {
            value.value = value.value.copy(baseUrl = baseUrl, model = model, updatedAtEpochMillis = 2)
        }
        override suspend fun clearApiKey() { value.value = value.value.copy(hasApiKey = false) }
        override suspend fun readSecret(): AiProviderSecret? = null
    }

    @Test fun cached_facts_do_not_observe_their_own_writes_and_explicit_events_refresh_once() = runBlocking {
        val db = memory()
        try {
            task(db)
            val repo = RoomUsageSummaryRepository(db)
            val first = CompletableDeferred<Unit>()
            val changed = CompletableDeferred<Unit>()
            val seen = mutableListOf<UsageSummarySnapshot>()
            val observer = launch {
                repo.observe().collect {
                    seen += it
                    if (seen.size == 1) first.complete(Unit) else changed.complete(Unit)
                }
            }
            try {
                withTimeout(5000) { first.await() }
                val stable = seen.single()
                repo.refresh()
                repo.saveSummary(local(stable, System.currentTimeMillis()))
                delay(150)
                assertEquals(1, seen.size)
                UsageSummaryRecorder.record(db, UsageSummaryActionKind.MOVE, "physics", "segment")
                withTimeout(5000) { changed.await() }
                delay(150)
                assertEquals(2, seen.size)
                assertEquals(1, seen.last().facts.single { it.id == "manual-adjustments" }.count)
            } finally { observer.cancelAndJoin() }
        } finally { db.close() }
    }

    @Test fun stale_revision_and_changed_fact_snapshot_cannot_replace_the_last_good_document() = runBlocking {
        val db = memory()
        try {
            task(db)
            val repo = RoomUsageSummaryRepository(db)
            val snapshot = repo.refresh()
            val good = local(snapshot, System.currentTimeMillis())
            repo.saveSummary(good)
            assertTrue(runCatching { repo.saveSummary(good.copy(facts = emptyList())) }.isFailure)
            UsageSummaryRecorder.record(db, UsageSummaryActionKind.TASK_UPDATED, "physics")
            assertTrue(runCatching { repo.saveSummary(good.copy(text = "旧回复")) }.isFailure)
            assertEquals(good, repo.observeSummary().first())
        } finally { db.close() }
    }

    @Test fun foreground_refresh_publishes_window_expiry_without_a_source_table_mutation() = runBlocking {
        val db = memory()
        try {
            var now = System.currentTimeMillis()
            task(db, now = now)
            val repo = RoomUsageSummaryRepository(db, clock = { now })
            val first = CompletableDeferred<UsageSummarySnapshot>()
            val expired = CompletableDeferred<UsageSummarySnapshot>()
            val observer = launch { repo.observe().collect {
                if (!first.isCompleted) first.complete(it) else if (it.facts.isEmpty()) expired.complete(it)
            } }
            try {
                val before = withTimeout(5000) { first.await() }
                assertTrue(before.facts.isNotEmpty())
                now += 31 * 86_400_000L
                val after = repo.refresh()
                assertEquals(after, withTimeout(5000) { expired.await() })
                assertNotEquals(before.revision, after.revision)
                assertEquals(0, after.operationCount)
            } finally { observer.cancelAndJoin() }
        } finally { db.close() }
    }

    @Test fun remote_document_and_attempt_are_guarded_by_current_consent_and_provider_metadata() = runBlocking {
        val db = memory()
        try {
            val at = System.currentTimeMillis()
            task(db, now = at)
            val config = Config()
            db.aiSettingsDao().save(AiSettingsEntity(isEnabled = true, consentedAtEpochMillis = at - 1000))
            val repo = RoomUsageSummaryRepository(db, config)
            val snapshot = repo.refresh()
            val access = db.aiSettingsDao().get()!!.toDomain()
            val providerHash = UsageSummaryHash.of(config.value.value.toString())
            val accessHash = UsageSummaryHash.of(access.toString())
            val remote = local(snapshot, at).copy(origin = "AI · test", configurationRevision = providerHash, accessRevision = accessHash)
            repo.saveAttempt(UsageSummaryAttempt(snapshot.revision, at, providerHash, accessHash))
            repo.saveSummary(remote)
            config.value.value = config.value.value.copy(model = "changed-model")
            assertTrue(runCatching { repo.saveSummary(remote) }.isFailure)
            assertTrue(runCatching { repo.saveAttempt(UsageSummaryAttempt(snapshot.revision, at, providerHash, accessHash)) }.isFailure)
            db.aiSettingsDao().save(AiSettingsEntity(isEnabled = false, consentedAtEpochMillis = at - 1000))
            assertTrue(runCatching { repo.saveSummary(remote) }.isFailure)
            assertEquals(remote, repo.observeSummary().first())
        } finally { db.close() }
    }

    @Test fun user_boundary_recording_rolls_back_with_the_enclosing_mutation() = runBlocking {
        val db = memory()
        try {
            task(db)
            val repo = RoomUsageSummaryRepository(db)
            val before = repo.refresh()
            assertTrue(runCatching { db.withTransaction {
                UsageSummaryRecorder.record(db, UsageSummaryActionKind.MOVE, "physics", "segment")
                error("mutation failed")
            } }.isFailure)
            assertEquals(before, repo.refresh())
            assertNull(db.planningWorkspaceDao().get(UsageSummaryRecorder.EVENTS_REVISION_KEY))
        } finally { db.close() }
    }

    @Test fun document_attempt_and_evidence_survive_database_reopen() = runBlocking {
        val name = "usage-summary-${UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(context, ReplandDatabase::class.java, name).build()
        try {
            val at = System.currentTimeMillis()
            val config = Config()
            task(db, now = at)
            db.aiSettingsDao().save(AiSettingsEntity(isEnabled = true, consentedAtEpochMillis = at - 1000))
            UsageSummaryRecorder.record(db, UsageSummaryActionKind.MOVE, "physics")
            val repo = RoomUsageSummaryRepository(db, config)
            val snapshot = repo.refresh()
            val document = local(snapshot, System.currentTimeMillis())
            val attempt = UsageSummaryAttempt(snapshot.revision, System.currentTimeMillis(),
                UsageSummaryHash.of(config.value.value.toString()), UsageSummaryHash.of(db.aiSettingsDao().get()!!.toDomain().toString()))
            repo.saveSummary(document)
            repo.saveAttempt(attempt)
            db.close()
            db = Room.databaseBuilder(context, ReplandDatabase::class.java, name).build()
            val reopened = RoomUsageSummaryRepository(db, config)
            assertEquals(snapshot, reopened.refresh())
            assertEquals(document, reopened.observeSummary().first())
            assertEquals(attempt, reopened.observeAttempt().first())
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun clearing_local_data_removes_summary_cache_attempts_and_action_evidence() = runBlocking {
        val db = memory()
        try {
            task(db)
            val repo = RoomUsageSummaryRepository(db)
            UsageSummaryRecorder.record(db, UsageSummaryActionKind.MOVE, "physics")
            val snapshot = repo.refresh()
            repo.saveSummary(local(snapshot, System.currentTimeMillis()))
            withContext(Dispatchers.IO) { db.clearAllTables() }
            assertNull(repo.observeSummary().first())
            assertNull(repo.observeAttempt().first())
            assertEquals(0, repo.refresh().operationCount)
            assertTrue(repo.refresh().facts.isEmpty())
        } finally { db.close() }
    }

    @Test fun corrected_duration_excludes_the_linked_original_session_but_preserves_other_linked_rounds() = runBlocking {
        val db = memory()
        try {
            val at = System.currentTimeMillis()
            task(db, now = at)
            fun feedback(id: String, duration: Int, corrected: String? = null) = ExecutionLogEntity(id, "physics",
                if (corrected == null) ExecutionLogEventType.FEEDBACK.name else ExecutionLogEventType.CORRECTION.name,
                TaskStatus.IN_PROGRESS.name, duration, null, null, null, null, corrected, null,
                if (corrected == null) at - 2000 else at - 1000)
            db.executionLogDao().insert(feedback("round-log", 45))
            db.executionLogDao().insert(feedback("other-log", 20))
            for ((id, logId, minutes) in listOf(Triple("round", "round-log", 45), Triple("other-round", "other-log", 20))) {
                val session = ExecutionSession(id, "physics", "复习物理", "plan", "segment", 3600,
                    at - 7_200_000, minutes * 60_000L, null, at - 2000, ExecutionOutcome.CONTINUE)
                db.planningWorkspaceDao().put(PlanningWorkspaceEntity("execution-session-history:$id", ExecutionSessionCodec.encode(session)))
                db.planningWorkspaceDao().put(PlanningWorkspaceEntity("numeric-input:session:$id",
                    org.json.JSONObject().put("version", 1).put("logs", org.json.JSONArray(listOf(logId))).toString()))
            }
            val repo = RoomUsageSummaryRepository(db)
            assertEquals(2, repo.refresh().facts.single { it.id == "execution-records" }.count)
            db.executionLogDao().insert(feedback("correction", 15, "round-log"))
            val after = repo.refresh().facts.single { it.id == "execution-records" }
            assertEquals(listOf("session:other-round"), after.sourceIds)
            assertTrue(after.text.contains("累计 20 分钟"))
        } finally { db.close() }
    }

    @Test fun legacy_session_without_a_log_link_is_conservatively_excluded_after_duration_correction() = runBlocking {
        val db = memory()
        try {
            val at = System.currentTimeMillis()
            task(db, now = at)
            val session = ExecutionSession("legacy", "physics", "复习物理", "plan", "segment", 3600,
                at - 7_200_000, 45 * 60_000L, null, at - 2000, ExecutionOutcome.CONTINUE)
            db.planningWorkspaceDao().put(PlanningWorkspaceEntity("execution-session-history:legacy", ExecutionSessionCodec.encode(session)))
            db.executionLogDao().insert(ExecutionLogEntity("original", "physics", ExecutionLogEventType.FEEDBACK.name,
                TaskStatus.IN_PROGRESS.name, 45, null, null, null, null, null, null, at - 2000))
            db.executionLogDao().insert(ExecutionLogEntity("correction", "physics", ExecutionLogEventType.CORRECTION.name,
                TaskStatus.IN_PROGRESS.name, 15, null, null, null, null, "original", null, at - 1000))
            assertFalse(RoomUsageSummaryRepository(db).refresh().facts.any { it.id == "execution-records" })
        } finally { db.close() }
    }
}
