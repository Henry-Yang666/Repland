package com.swan1127.repland.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.swan1127.repland.domain.model.*
import com.swan1127.repland.domain.ports.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

data class UsageSummaryUiState(
    val snapshot: UsageSummarySnapshot? = null,
    val summary: UsageSummaryDocument? = null,
    val text: String = "正在整理你的使用记录…",
    val updatedAt: Long? = null,
    val isAiSummary: Boolean = false,
    val basis: List<UsageSummaryFact> = emptyList(),
    val windowStart: String? = null,
    val windowEnd: String? = null,
    val loading: Boolean = true,
    val updating: Boolean = false,
    val pendingChanges: Boolean = false,
    val notice: String? = null,
    val readError: String? = null,
)

class UsageSummaryViewModel(
    private val repository: UsageSummaryRepository,
    private val aiSettings: AiSettingsRepository,
    private val config: AiProviderConfigRepository,
    private val advisor: UsageSummaryAdvisor,
    private val remoteSupported: Boolean,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private data class Records(val snapshot: UsageSummarySnapshot, val document: UsageSummaryDocument?, val attempt: UsageSummaryAttempt?)
    private data class Inputs(val records: Records, val access: AiPreferences, val provider: AiProviderConfig, val visible: Boolean) {
        val configurationRevision get() = UsageSummaryHash.of(provider.toString())
        val accessRevision get() = UsageSummaryHash.of(access.toString())
        val requestKey get() = "${records.snapshot.revision}:$configurationRevision:$accessRevision"
    }

    private val state = MutableStateFlow(UsageSummaryUiState())
    val uiState = state.asStateFlow()
    private val visible = MutableStateFlow(false)
    private var readJob: Job? = null
    private var requestJob: Job? = null
    private var refreshJob: Job? = null
    private var current: Inputs? = null
    private var requestKey: String? = null
    private var epoch = 0L

    init {
        observeAccess()
        retryRead()
    }

    /** Permission changes remain observable even if a usage-data stream is unavailable. */
    private fun observeAccess() = viewModelScope.launch {
        combine(aiSettings.observe(), config.observe()) { access, provider -> access to provider }
            .retryWhen { cause, _ ->
                if (cause is CancellationException) throw cause
                cancelRequest()
                delay(5_000)
                true
            }.collect { (access, provider) ->
                val accessRevision = UsageSummaryHash.of(access.toString())
                val configurationRevision = UsageSummaryHash.of(provider.toString())
                val allowed = remoteSupported && access.isEnabled && access.hasExplicitConsent && provider.hasApiKey
                val last = current
                if (!allowed || (last != null && (last.accessRevision != accessRevision || last.configurationRevision != configurationRevision))) {
                    current = null
                    cancelRequest()
                }
                val document = state.value.summary
                if (document != null && (!allowed || document.accessRevision != accessRevision || document.configurationRevision != configurationRevision)) {
                    state.update { previous ->
                        val snapshot = previous.snapshot
                        previous.copy(summary = null, text = snapshot?.localSummary ?: previous.text,
                            updatedAt = snapshot?.updatedAt, isAiSummary = false,
                            basis = snapshot?.facts.orEmpty(), windowStart = snapshot?.windowStart,
                            windowEnd = snapshot?.windowEnd, pendingChanges = false, notice = null)
                    }
                }
            }
    }

    fun setVisible(value: Boolean) {
        visible.value = value
        if (!value) cancelRequest()
    }

    /** Called only while the profile is in the foreground; no background polling or manual form. */
    fun onForeground() {
        if (readJob?.isActive != true) retryRead()
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            try {
                repository.refresh()
                current?.let(::maybeGenerate)
            } catch (c: CancellationException) { throw c }
            catch (_: Exception) {
                cancelRequest()
                state.update { it.copy(readError = "暂时无法读取新记录，已保留上次内容，稍后会自动重试。") }
            }
        }
    }

    private fun retryRead() {
        readJob?.cancel()
        readJob = viewModelScope.launch {
            try {
                val records = combine(repository.observe(), repository.observeSummary(), repository.observeAttempt(), ::Records)
                combine(records, aiSettings.observe(), config.observe(), visible, ::Inputs).collect { input ->
                    current = input
                    if (requestKey != null && (requestKey != input.requestKey || !canRequest(input))) cancelRequest()
                    val snapshot = input.records.snapshot
                    val availableSources = snapshot.facts.flatMap { it.sourceIds }.toSet()
                    val document = input.records.document?.takeIf { doc ->
                        doc.contentVersion == UsageSummaryDocument.CURRENT_CONTENT_VERSION &&
                            remoteSupported && input.access.isEnabled && input.access.hasExplicitConsent && input.provider.hasApiKey &&
                            doc.configurationRevision == input.configurationRevision && doc.accessRevision == input.accessRevision &&
                            doc.facts.isNotEmpty() && doc.facts.flatMap { it.sourceIds }.all { it in availableSources }
                    }
                    state.update { previous -> previous.copy(
                        snapshot = snapshot, summary = document,
                        text = document?.text ?: snapshot.localSummary,
                        updatedAt = document?.generatedAt ?: snapshot.updatedAt.takeIf { snapshot.facts.isNotEmpty() },
                        isAiSummary = document != null,
                        basis = document?.facts ?: snapshot.facts,
                        windowStart = document?.windowStart ?: snapshot.windowStart,
                        windowEnd = document?.windowEnd ?: snapshot.windowEnd,
                        loading = false, readError = null,
                        pendingChanges = document != null && document.revision != snapshot.revision,
                        notice = if (previous.snapshot?.revision != snapshot.revision) null else previous.notice,
                    ) }
                    maybeGenerate(input)
                }
            } catch (c: CancellationException) { throw c }
            catch (_: Exception) {
                current = null
                cancelRequest()
                state.update { it.copy(loading = false, readError = "暂时无法读取使用记录，已保留上次内容，稍后会自动重试。") }
            }
        }
    }

    private fun canRequest(input: Inputs) = input.visible && visible.value && remoteSupported &&
        input.access.isEnabled && input.access.hasExplicitConsent && input.provider.hasApiKey

    private fun maybeGenerate(input: Inputs) {
        if (!canRequest(input) || requestJob?.isActive == true || !UsageSummaryUpdatePolicy.shouldRequest(
                input.records.snapshot, input.records.document, input.records.attempt, input.configurationRevision, input.accessRevision, clock())) return
        val generation = ++epoch
        requestKey = input.requestKey
        state.update { it.copy(updating = true, notice = null) }
        requestJob = viewModelScope.launch {
            try {
                // Coalesce a burst of normal operations before creating one request.
                delay(750)
                val snapshot = repository.refresh()
                ensureCurrent(input, generation, snapshot)
                repository.saveAttempt(UsageSummaryAttempt(snapshot.revision, clock(), input.configurationRevision, input.accessRevision))
                val request = UsageSummaryRequest(UUID.randomUUID().toString(), snapshot.revision,
                    snapshot.windowStart, snapshot.windowEnd, snapshot.facts.map { it.copy(sourceIds = emptyList()) },
                    snapshot.operationCount, input.configurationRevision, input.accessRevision)
                val response = withTimeoutOrNull(35_000) { advisor.describe(request) }
                ensureActive()
                ensureCurrent(input, generation, repository.refresh())
                if (response != null && UsageSummaryValidator.valid(request, response)) {
                    repository.saveSummary(UsageSummaryDocument(snapshot.revision, clock(), "AI · ${input.provider.model}",
                        response.text.trim(), response.highlights, response.sourceFactIds, snapshot.operationCount,
                        input.configurationRevision, input.accessRevision, snapshot.windowStart, snapshot.windowEnd, snapshot.facts))
                    state.update { it.copy(notice = null) }
                } else state.update { it.copy(notice = fallbackNotice()) }
            } catch (c: CancellationException) { throw c }
            catch (_: Exception) {
                if (generation == epoch) state.update { it.copy(notice = fallbackNotice()) }
            } finally {
                if (generation == epoch) {
                    requestKey = null
                    state.update { it.copy(updating = false) }
                }
            }
        }
    }

    private fun fallbackNotice() = if (state.value.isAiSummary) "AI 暂不可用，保留上次总结，稍后自动更新。"
        else "先显示记录摘要，AI 可用后会自动更新。"

    private suspend fun ensureCurrent(input: Inputs, generation: Long, snapshot: UsageSummarySnapshot) {
        check(generation == epoch && visible.value && snapshot.revision == input.records.snapshot.revision &&
            aiSettings.observe().first() == input.access && config.observe().first() == input.provider) { "使用记录或 AI 设置已变化。" }
    }

    private fun cancelRequest() {
        epoch++
        requestJob?.cancel()
        requestJob = null
        requestKey = null
        state.update { it.copy(updating = false) }
    }

    class Factory(private val repository: UsageSummaryRepository, private val ai: AiSettingsRepository,
        private val config: AiProviderConfigRepository, private val advisor: UsageSummaryAdvisor, private val remoteSupported: Boolean) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T =
            UsageSummaryViewModel(repository, ai, config, advisor, remoteSupported) as T
    }
}
