package com.swan1127.repland

import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swan1127.repland.data.room.*
import com.swan1127.repland.domain.model.*
import com.swan1127.repland.domain.ports.AiProviderConfigRepository
import com.swan1127.repland.ui.profile.UsageSummaryViewModel
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real Room snapshots and persisted caching; the advisor is always a local fake. */
@RunWith(AndroidJUnit4::class)
class UsageSummaryAutoWorkflowTest {
    private class Config : AiProviderConfigRepository {
        val state = MutableStateFlow(AiProviderConfig("https://example.invalid/v1", "synthetic-model", true, 1L))
        override fun observe() = state
        override suspend fun readSecret(): AiProviderSecret? = error("Workflow tests must not request a real credential")
        override suspend fun save(baseUrl: String, model: String, apiKey: String) = error("The summary must not edit configuration")
        override suspend fun clearApiKey() = error("The summary must not clear configuration")
    }

    private class Advisor : UsageSummaryAdvisor {
        val requests = CopyOnWriteArrayList<UsageSummaryRequest>()
        @Volatile var respond: suspend (UsageSummaryRequest) -> UsageSummaryAdvice? = { advice(it) }
        override suspend fun describe(request: UsageSummaryRequest): UsageSummaryAdvice? {
            requests += request
            return respond(request)
        }
    }

    private class Fixture {
        private val context = ApplicationProvider.getApplicationContext<Context>()
        val now = AtomicLong(System.currentTimeMillis() + 60_000L)
        val db = Room.inMemoryDatabaseBuilder(context, ReplandDatabase::class.java).build()
        val config = Config()
        val ai = RoomAiSettingsRepository(db.aiSettingsDao())
        val repository = RoomUsageSummaryRepository(db, config, now::get)
        val tasks = RoomTaskRepository(db)
        val advisor = Advisor()
        private val stores = mutableListOf<ViewModelStore>()

        suspend fun authorizeAndSeed() {
            ai.grantConsentAndEnable()
            addTask()
        }

        suspend fun addTask() {
            tasks.save(TaskDraft(id = UUID.randomUUID().toString(), displayName = "普通使用事项", totalDurationMinutes = 30))
        }

        suspend fun model(): Pair<UsageSummaryViewModel, ViewModelStore> = withContext(Dispatchers.Main) {
            val store = ViewModelStore().also(stores::add)
            val vm = UsageSummaryViewModel(repository, ai, config, advisor, true, now::get)
            store.put("usage-summary", vm)
            vm to store
        }

        suspend fun close() {
            withContext(Dispatchers.Main) { stores.forEach(ViewModelStore::clear) }
            db.close()
        }
    }

    private suspend fun await(message: String, condition: suspend () -> Boolean) {
        try {
            withTimeout(12_000) { while (!condition()) delay(20) }
        } catch (timeout: TimeoutCancellationException) {
            throw AssertionError(message, timeout)
        }
    }

    private suspend fun visible(vm: UsageSummaryViewModel, value: Boolean) = withContext(Dispatchers.Main) { vm.setVisible(value) }
    private suspend fun foreground(vm: UsageSummaryViewModel) = withContext(Dispatchers.Main) { vm.onForeground() }
    private suspend fun loaded(vm: UsageSummaryViewModel) = await("Usage records should load") { !vm.uiState.value.loading }
    private suspend fun generated(f: Fixture, vm: UsageSummaryViewModel, calls: Int) = await("Expected an automatic persisted summary after $calls requests") {
        f.advisor.requests.size == calls && vm.uiState.value.isAiSummary && !vm.uiState.value.updating &&
            f.repository.observeSummary().first()?.revision == f.repository.refresh().revision
    }

    @Test fun visible_profile_generates_from_one_ordinary_task_without_old_numeric_consent() = runBlocking {
        val f = Fixture()
        try {
            f.authorizeAndSeed()
            assertFalse(RoomNumericProfileRepository(f.db, f.config).refresh().aiConsent)
            val (vm) = f.model()
            loaded(vm)
            assertTrue(f.advisor.requests.isEmpty())
            visible(vm, true)
            generated(f, vm, 1)
            val request = f.advisor.requests.single()
            assertTrue(request.facts.isNotEmpty())
            assertTrue(request.facts.all { it.sourceIds.isEmpty() })
            assertEquals(UsageSummaryHash.of(f.config.state.value.toString()), request.configurationRevision)
            assertEquals(UsageSummaryHash.of(f.ai.observe().first().toString()), request.accessRevision)
            assertTrue(vm.uiState.value.basis.any { it.sourceIds.isNotEmpty() })
            assertEquals(1, f.tasks.observeTasks().first().size)
        } finally { f.close() }
    }

    @Test fun same_revision_reopen_and_new_viewmodel_reuse_the_persisted_summary() = runBlocking {
        val f = Fixture()
        try {
            f.authorizeAndSeed()
            val (vm, store) = f.model()
            visible(vm, true)
            generated(f, vm, 1)
            val document = f.repository.observeSummary().first()
            visible(vm, false)
            f.now.addAndGet(UsageSummaryUpdatePolicy.UPDATE_INTERVAL + 1)
            visible(vm, true)
            foreground(vm)
            await("Reopening the same revision must keep the cache") { vm.uiState.value.isAiSummary && !vm.uiState.value.updating }
            assertEquals(1, f.advisor.requests.size)
            withContext(Dispatchers.Main) { store.clear() }

            val (recreated) = f.model()
            visible(recreated, true)
            loaded(recreated)
            await("A recreated model must load the persisted summary") { recreated.uiState.value.isAiSummary && !recreated.uiState.value.updating }
            assertEquals(document, recreated.uiState.value.summary)
            assertEquals(1, f.advisor.requests.size)
        } finally { f.close() }
    }

    @Test fun legacy_writing_cache_keeps_evidence_but_refreshes_once_when_profile_becomes_visible() = runBlocking {
        val f = Fixture()
        try {
            f.authorizeAndSeed()
            val snapshot = f.repository.refresh()
            val document = UsageSummaryDocument(snapshot.revision, f.now.get(), "AI · old-template",
                "根据最近30天的记录，新建了1项任务。", emptyList(), snapshot.facts.map { it.id },
                snapshot.operationCount, UsageSummaryHash.of(f.config.state.value.toString()),
                UsageSummaryHash.of(f.ai.observe().first().toString()), snapshot.windowStart, snapshot.windowEnd, snapshot.facts)
            val legacyJson = org.json.JSONObject(UsageSummaryCodec.encode(document)).apply { remove("contentVersion") }
            f.db.planningWorkspaceDao().put(PlanningWorkspaceEntity(RoomUsageSummaryRepository.DOCUMENT_KEY, legacyJson.toString()))
            assertEquals(1, f.repository.observeSummary().first()!!.contentVersion)
            val legacyAttempt = org.json.JSONObject(UsageSummaryCodec.encode(UsageSummaryAttempt(snapshot.revision,
                f.now.get(), document.configurationRevision!!, document.accessRevision!!))).apply { remove("contentVersion") }
            f.db.planningWorkspaceDao().put(PlanningWorkspaceEntity(RoomUsageSummaryRepository.ATTEMPT_KEY, legacyAttempt.toString()))
            assertEquals(1, f.repository.observeAttempt().first()!!.contentVersion)

            val (vm) = f.model()
            loaded(vm)
            assertFalse(vm.uiState.value.isAiSummary)
            assertEquals(snapshot.localSummary, vm.uiState.value.text)
            assertEquals(snapshot.facts, vm.uiState.value.basis)
            assertTrue(f.advisor.requests.isEmpty())

            visible(vm, true)
            generated(f, vm, 1)
            assertEquals(UsageSummaryDocument.CURRENT_CONTENT_VERSION, f.repository.observeSummary().first()!!.contentVersion)
            assertEquals(snapshot.facts, vm.uiState.value.basis)
            visible(vm, false)
            visible(vm, true)
            foreground(vm)
            await("The updated writing template should reuse its cache") { !vm.uiState.value.updating }
            assertEquals(1, f.advisor.requests.size)
        } finally { f.close() }
    }

    @Test fun normal_changes_are_coalesced_and_updates_use_operation_and_six_hour_thresholds() = runBlocking {
        val f = Fixture()
        try {
            f.authorizeAndSeed()
            val (vm) = f.model()
            visible(vm, true)
            generated(f, vm, 1)
            val first = requireNotNull(f.repository.observeSummary().first())

            // A small change remains visible through the previous, sourced summary.
            f.now.addAndGet(UsageSummaryUpdatePolicy.RETRY_INTERVAL + 1)
            f.addTask()
            await("New evidence should mark the cached summary pending") { vm.uiState.value.pendingChanges }
            foreground(vm)
            assertFalse(vm.uiState.value.updating)
            assertEquals(1, f.advisor.requests.size)
            assertEquals(first.text, vm.uiState.value.text)

            // Three committed operations can request one coalesced update after backoff.
            f.addTask()
            f.addTask()
            generated(f, vm, 2)
            val second = requireNotNull(f.repository.observeSummary().first())
            assertNotEquals(first.revision, second.revision)

            f.now.addAndGet(UsageSummaryUpdatePolicy.RETRY_INTERVAL + 1)
            f.addTask()
            await("The later small change should remain pending") { vm.uiState.value.pendingChanges }
            assertEquals(2, f.advisor.requests.size)
            f.now.set(second.generatedAt + UsageSummaryUpdatePolicy.UPDATE_INTERVAL + 1)
            foreground(vm)
            generated(f, vm, 3)
        } finally { f.close() }
    }

    @Test fun failed_refresh_keeps_previous_summary_and_persists_retry_backoff_across_recreation() = runBlocking {
        val f = Fixture()
        try {
            f.authorizeAndSeed()
            val (vm, store) = f.model()
            visible(vm, true)
            generated(f, vm, 1)
            val previous = requireNotNull(f.repository.observeSummary().first())
            f.advisor.respond = { null }
            f.now.addAndGet(UsageSummaryUpdatePolicy.RETRY_INTERVAL + 1)
            repeat(3) { f.addTask() }
            await("A failed update must finish with a fallback notice") {
                f.advisor.requests.size == 2 && !vm.uiState.value.updating && vm.uiState.value.notice != null
            }
            assertEquals(previous, f.repository.observeSummary().first())
            assertEquals(previous.text, vm.uiState.value.text)
            assertTrue(vm.uiState.value.isAiSummary)
            assertEquals(f.repository.refresh().revision, f.repository.observeAttempt().first()?.revision)
            withContext(Dispatchers.Main) { store.clear() }

            val (recreated) = f.model()
            visible(recreated, true)
            loaded(recreated)
            foreground(recreated)
            await("Backoff should retain the prior summary after recreation") { recreated.uiState.value.isAiSummary && !recreated.uiState.value.updating }
            assertEquals(2, f.advisor.requests.size)
            f.advisor.respond = { advice(it, "根据新的使用记录整理后的总结。") }
            f.now.addAndGet(UsageSummaryUpdatePolicy.RETRY_INTERVAL + 1)
            foreground(recreated)
            generated(f, recreated, 3)
            assertEquals("根据新的使用记录整理后的总结。", recreated.uiState.value.text)
        } finally { f.close() }
    }

    @Test fun hiding_profile_discards_a_late_non_cancellable_advisor_reply() = runBlocking {
        lateReplyIsDiscarded("hidden") { f, vm, _ -> visible(vm, false) }
    }

    @Test fun disabling_ai_discards_a_late_non_cancellable_advisor_reply() = runBlocking {
        lateReplyIsDiscarded("disabled") { f, _, _ -> f.ai.setEnabled(false) }
    }

    @Test fun changing_provider_discards_old_reply_even_if_replacement_is_unavailable() = runBlocking {
        lateReplyIsDiscarded("configuration") { f, _, _ ->
            f.config.state.value = f.config.state.value.copy(model = "changed-model", updatedAtEpochMillis = 2L)
        }
    }

    @Test fun clearing_local_data_does_not_resurrect_a_late_summary_or_attempt() = runBlocking {
        lateReplyIsDiscarded("data-cleared") { f, _, _ -> RoomDataManagementRepository(f.db).clearAllLocalData() }
    }

    @Test fun clearing_viewmodel_store_cancels_and_discards_a_late_reply() = runBlocking {
        lateReplyIsDiscarded("store-cleared") { _, _, store -> withContext(Dispatchers.Main) { store.clear() } }
    }

    /** Ignore cancellation in the fake to exercise the post-response revision/access guards too. */
    private suspend fun lateReplyIsDiscarded(
        reason: String,
        invalidate: suspend (Fixture, UsageSummaryViewModel, ViewModelStore) -> Unit,
    ) {
        val f = Fixture()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val returned = CompletableDeferred<Unit>()
        try {
            f.authorizeAndSeed()
            f.advisor.respond = { request ->
                if (f.advisor.requests.size == 1) withContext(NonCancellable) {
                    entered.complete(Unit)
                    try { release.await(); advice(request, "迟到的旧总结不应保存。") }
                    finally { returned.complete(Unit) }
                } else null
            }
            val (vm, store) = f.model()
            visible(vm, true)
            withTimeout(12_000) { entered.await() }
            invalidate(f, vm, store)
            release.complete(Unit)
            withTimeout(12_000) { returned.await() }
            if (reason == "configuration") {
                await("Changed configuration may make one replacement request") { f.advisor.requests.size == 2 && !vm.uiState.value.updating }
            } else if (reason != "store-cleared") {
                await("Invalidated request should stop updating: $reason") { !vm.uiState.value.updating }
            }
            // Flush the main queue after the fake has returned to the cancelled request.
            withContext(Dispatchers.Main) { yield() }
            assertNull("A late summary must not be saved after $reason", f.repository.observeSummary().first())
            assertFalse("A late AI summary must not be displayed after $reason", vm.uiState.value.isAiSummary)
            if (reason == "data-cleared") {
                assertTrue(f.tasks.observeTasks().first().isEmpty())
                assertNull(f.repository.observeAttempt().first())
            }
        } finally {
            release.complete(Unit)
            f.close()
        }
    }

    companion object {
        private fun advice(request: UsageSummaryRequest, text: String = "根据近期使用记录整理的总结。") = UsageSummaryAdvice(
            request.requestId, request.revision, text, listOf("总结依据已记录的事项。"),
            request.facts.map { it.id }, request.configurationRevision, request.accessRevision,
        )
    }
}
