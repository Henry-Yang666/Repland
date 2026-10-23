package com.swan1127.repland.data.room

import androidx.room.withTransaction
import com.swan1127.repland.domain.model.*
import com.swan1127.repland.domain.ports.AiProviderConfigRepository
import com.swan1127.repland.domain.ports.UsageSummaryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import org.json.JSONObject
import java.util.UUID

/** User-boundary hooks only: automatic planning must never call this recorder. */
object UsageSummaryRecorder {
    const val EVENT_PREFIX = "usage-summary:event:"
    const val EVENTS_REVISION_KEY = "usage-summary:events-revision"

    suspend fun record(
        database: ReplandDatabase,
        kind: UsageSummaryActionKind,
        taskId: String? = null,
        sourceId: String? = null,
        now: Long = System.currentTimeMillis(),
    ) = database.withTransaction {
        val action = UsageSummaryAction(UUID.randomUUID().toString(), kind, taskId, sourceId, now)
        database.planningWorkspaceDao().put(PlanningWorkspaceEntity(EVENT_PREFIX + action.id, UsageSummaryCodec.encode(action)))
        database.planningWorkspaceDao().put(PlanningWorkspaceEntity(EVENTS_REVISION_KEY, action.id))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RoomUsageSummaryRepository(
    private val database: ReplandDatabase,
    private val configuration: AiProviderConfigRepository? = null,
    private val clock: () -> Long = System::currentTimeMillis,
) : UsageSummaryRepository {
    companion object {
        const val SNAPSHOT_KEY = "usage-summary:snapshot"
        const val DOCUMENT_KEY = "usage-summary:document"
        const val ATTEMPT_KEY = "usage-summary:attempt"
    }
    private val workspace = database.planningWorkspaceDao()
    private val refreshed = MutableStateFlow<UsageSummarySnapshot?>(null)

    private fun observeSource(): Flow<UsageSummarySnapshot> = combine(
        database.taskDao().observeAll().distinctUntilChanged(),
        database.executionLogDao().observeAll().distinctUntilChanged(),
        workspace.observeEndedSessions().distinctUntilChanged(),
        workspace.observe(UsageSummaryRecorder.EVENTS_REVISION_KEY).distinctUntilChanged(),
    ) { _, _, _, _ -> Unit }.mapLatest { refresh() }.distinctUntilChanged()

    override fun observe(): Flow<UsageSummarySnapshot> = merge(observeSource(), refreshed.filterNotNull()).distinctUntilChanged()

    override fun observeSummary(): Flow<UsageSummaryDocument?> = workspace.observe(DOCUMENT_KEY)
        .distinctUntilChanged().map { row -> row?.let { UsageSummaryCodec.document(it.payload) } }

    override fun observeAttempt(): Flow<UsageSummaryAttempt?> = workspace.observe(ATTEMPT_KEY)
        .distinctUntilChanged().map { row -> row?.let { UsageSummaryCodec.attempt(it.payload) } }

    private fun actions(): List<UsageSummaryAction> = database.openHelper.writableDatabase.query(
        "SELECT payload FROM planning_workspace WHERE `key` LIKE 'usage-summary:event:%' ORDER BY `key`",
    ).use { cursor -> buildList { while (cursor.moveToNext()) add(UsageSummaryCodec.action(cursor.getString(0))) } }

    private suspend fun calculate(): UsageSummarySnapshot {
        val now = clock()
        val logs = database.executionLogDao().getAll().map(ExecutionLogEntity::toDomain)
        val byId = logs.associateBy(TaskExecutionLog::id)
        val replacedDurationLogs = mutableSetOf<String>()
        logs.filter { it.eventType == ExecutionLogEventType.CORRECTION &&
            it.feedback.actualDurationMinutes != null && it.createdAtEpochMillis <= now }.forEach { correction ->
            var ancestor = correction.correctedLogId
            val visited = mutableSetOf<String>()
            while (ancestor != null && visited.add(ancestor)) {
                replacedDurationLogs += ancestor
                ancestor = byId[ancestor]?.correctedLogId
            }
        }
        val sessions = workspace.getExecutionSessions().map { ExecutionSessionCodec.decode(it.payload) }.filter { session ->
            // Session completion already stores this provenance; no numeric-profile consent or input is needed.
            val linkage = workspace.get("numeric-input:session:${session.id}")?.let { row ->
                JSONObject(row.payload).optJSONArray("logs")?.let { array -> (0 until array.length()).map(array::getString) }
            }
            val durationWasCorrected = if (linkage == null) logs.any { it.taskId == session.taskId && it.id in replacedDurationLogs }
                else linkage.any { it in replacedDurationLogs }
            !durationWasCorrected
        }
        return UsageSummaryCalculator.calculate(
            tasks = database.taskDao().getAll().map(TaskEntity::toDomain),
            logs = logs,
            sessions = sessions,
            actions = actions(),
            now = now,
        )
    }

    override suspend fun refresh(): UsageSummarySnapshot {
        val result = database.withTransaction {
            val next = calculate()
            val previous = workspace.get(SNAPSHOT_KEY)?.let { UsageSummaryCodec.snapshot(it.payload) }
            // The source streams are distinct before refresh, so writing this cache cannot observe itself.
            if (previous != null && previous.revision == next.revision && previous.windowStart == next.windowStart && previous.windowEnd == next.windowEnd) {
                previous
            } else {
                workspace.put(PlanningWorkspaceEntity(SNAPSHOT_KEY, UsageSummaryCodec.encode(next)))
                next
            }
        }
        // Explicit foreground refreshes can expire evidence without any source-table mutation.
        refreshed.value = result
        return result
    }

    private suspend fun requireRemoteAccess(configurationRevision: String?, accessRevision: String?) {
        val access = database.aiSettingsDao().get()?.toDomain() ?: AiPreferences()
        require(access.isEnabled && access.hasExplicitConsent && UsageSummaryHash.of(access.toString()) == accessRevision) {
            "AI 授权已变化，本次总结未保存。"
        }
        val provider = requireNotNull(configuration) { "当前构建没有模型配置。" }.observe().first()
        require(provider.hasApiKey && UsageSummaryHash.of(provider.toString()) == configurationRevision) {
            "模型配置已变化，本次总结未保存。"
        }
    }

    override suspend fun saveSummary(document: UsageSummaryDocument) = database.withTransaction {
        val snapshot = calculate()
        require(document.contentVersion == UsageSummaryDocument.CURRENT_CONTENT_VERSION &&
            document.revision == snapshot.revision && document.facts == snapshot.facts &&
            document.windowStart == snapshot.windowStart && document.windowEnd == snapshot.windowEnd &&
            document.operationCount == snapshot.operationCount) { "使用记录已变化，旧总结未覆盖当前内容。" }
        require(document.generatedAt in 0..clock() && document.origin.isNotBlank()) { "总结时间或来源无效。" }
        if (document.origin != "LOCAL") requireRemoteAccess(document.configurationRevision, document.accessRevision)
        if (snapshot.facts.isEmpty() && document.origin == "LOCAL") {
            require(document.text == snapshot.localSummary && document.highlights.isEmpty() && document.sourceFactIds.isEmpty())
        } else {
            val request = UsageSummaryRequest("stored", snapshot.revision, snapshot.windowStart, snapshot.windowEnd,
                snapshot.facts, snapshot.operationCount, document.configurationRevision, document.accessRevision)
            require(UsageSummaryValidator.valid(request, UsageSummaryAdvice("stored", document.revision, document.text,
                document.highlights, document.sourceFactIds, document.configurationRevision, document.accessRevision))) {
                "总结缺少有效的事实依据，原内容保留。"
            }
        }
        workspace.put(PlanningWorkspaceEntity(DOCUMENT_KEY, UsageSummaryCodec.encode(document)))
    }

    override suspend fun saveAttempt(attempt: UsageSummaryAttempt) = database.withTransaction {
        require(attempt.contentVersion == UsageSummaryDocument.CURRENT_CONTENT_VERSION &&
            attempt.revision == calculate().revision && attempt.attemptedAt in 0..clock()) {
            "使用记录已变化，本次请求未发出。"
        }
        requireRemoteAccess(attempt.configurationRevision, attempt.accessRevision)
        workspace.put(PlanningWorkspaceEntity(ATTEMPT_KEY, UsageSummaryCodec.encode(attempt)))
    }
}
