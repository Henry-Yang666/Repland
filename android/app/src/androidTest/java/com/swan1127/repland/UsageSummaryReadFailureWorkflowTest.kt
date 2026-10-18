package com.swan1127.repland

import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swan1127.repland.data.room.ReplandDatabase
import com.swan1127.repland.data.room.RoomAiSettingsRepository
import com.swan1127.repland.data.room.RoomTaskRepository
import com.swan1127.repland.data.room.RoomUsageSummaryRepository
import com.swan1127.repland.domain.model.*
import com.swan1127.repland.domain.ports.AiProviderConfigRepository
import com.swan1127.repland.domain.ports.AiSettingsRepository
import com.swan1127.repland.domain.ports.UsageSummaryRepository
import com.swan1127.repland.ui.profile.UsageSummaryViewModel
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Access/config changes still invalidate visible AI prose while the usage source cannot be read. */
@RunWith(AndroidJUnit4::class)
class UsageSummaryReadFailureWorkflowTest {
    private class Config : AiProviderConfigRepository {
        private val value = MutableStateFlow(AiProviderConfig(model = "cached-provider", hasApiKey = true))
        val secretReads = AtomicInteger()
        override fun observe() = value
        override suspend fun save(baseUrl: String, model: String, apiKey: String) {
            value.value = value.value.copy(baseUrl = baseUrl, model = model, updatedAtEpochMillis = System.currentTimeMillis())
        }
        override suspend fun clearApiKey() { value.value = value.value.copy(hasApiKey = false) }
        override suspend fun readSecret(): AiProviderSecret? { secretReads.incrementAndGet(); error("No secret is needed for this fixture") }
    }

    private class FailingSource(private val base: UsageSummaryRepository) : UsageSummaryRepository by base {
        val failed = MutableStateFlow(false)
        val failures = AtomicInteger()
        override fun observe() = combine(base.observe(), failed) { snapshot, fail ->
            if (fail) { failures.incrementAndGet(); error("QA usage source unavailable") }
            snapshot
        }
    }

    private fun main(action: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(action)
    private fun until(message: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 15_000
        while (!condition() && System.currentTimeMillis() < deadline) Thread.sleep(20)
        assertTrue(message, condition())
    }

    private fun verifyInvalidation(change: suspend (AiSettingsRepository, Config) -> Unit) = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ReplandDatabase::class.java).build()
        val store = ViewModelStore()
        try {
            val config = Config()
            val ai = RoomAiSettingsRepository(db.aiSettingsDao())
            ai.grantConsentAndEnable()
            val tasks = RoomTaskRepository(db)
            tasks.save(TaskDraft(id = "summary-failure-task", displayName = "使用总结读取故障样例", totalDurationMinutes = 30))
            val real = RoomUsageSummaryRepository(db, config)
            val snapshot = real.refresh()
            assertTrue(snapshot.facts.isNotEmpty())
            val cached = UsageSummaryDocument(
                snapshot.revision, System.currentTimeMillis(), "AI · cached-provider", "原模型生成的使用总结。",
                emptyList(), snapshot.facts.map { it.id }, snapshot.operationCount,
                UsageSummaryHash.of(config.observe().first().toString()),
                UsageSummaryHash.of(ai.observe().first().toString()),
                snapshot.windowStart, snapshot.windowEnd, snapshot.facts,
            )
            real.saveSummary(cached)
            val source = FailingSource(real)
            val requests = AtomicInteger()
            lateinit var vm: UsageSummaryViewModel
            main {
                vm = UsageSummaryViewModel(source, ai, config, UsageSummaryAdvisor { requests.incrementAndGet(); null }, true)
                store.put("summary-read-failure", vm)
                vm.setVisible(true)
            }
            until("The compatible stored AI summary should initially be displayed") {
                vm.uiState.value.isAiSummary && vm.uiState.value.text == cached.text
            }
            source.failed.value = true
            until("A usage-source failure should retain the last summary and report the failure") {
                vm.uiState.value.readError != null && !vm.uiState.value.updating
            }
            val readError = vm.uiState.value.readError
            assertEquals(cached.text, vm.uiState.value.text)

            change(ai, config)
            until("Access/config invalidation must be observed independently of the failed usage stream") {
                !vm.uiState.value.isAiSummary && vm.uiState.value.text == snapshot.localSummary
            }
            val state = vm.uiState.value
            assertNull(state.summary)
            assertEquals(snapshot.facts, state.basis)
            assertEquals(readError, state.readError)
            assertFalse(state.updating)
            assertEquals(snapshot.updatedAt, state.updatedAt)

            // A foreground retry still fails, but cannot bring back incompatible prose or dispatch AI.
            val failuresBefore = source.failures.get()
            main { vm.onForeground() }
            until("The decorated source should actually be retried") { source.failures.get() > failuresBefore }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertFalse(vm.uiState.value.isAiSummary)
            assertEquals(snapshot.localSummary, vm.uiState.value.text)
            assertNotNull(vm.uiState.value.readError)
            assertEquals(0, requests.get())
            assertEquals(0, config.secretReads.get())
            assertEquals(cached, real.observeSummary().first())
            assertEquals(listOf("summary-failure-task"), tasks.observeTasks().first().map { it.id })
        } finally {
            main { store.clear() }
            db.close()
        }
    }

    @Test fun disabling_ai_after_source_failure_replaces_cached_ai_text_with_the_last_local_summary() {
        verifyInvalidation { ai, _ -> ai.setEnabled(false) }
    }

    @Test fun changing_provider_after_source_failure_replaces_cached_ai_text_with_the_last_local_summary() {
        verifyInvalidation { _, config -> config.save("https://example.invalid/v1", "different-provider", "") }
    }
}
