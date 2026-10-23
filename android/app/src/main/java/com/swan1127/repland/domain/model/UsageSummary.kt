package com.swan1127.repland.domain.model

import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

enum class UsageSummaryDimension { FOCUS, RHYTHM, PROGRESS, ADJUSTMENT }

/** Source references remain local; a provider receives the bounded aggregate text only. */
data class UsageSummaryFact(
    val id: String,
    val dimension: UsageSummaryDimension,
    val text: String,
    val count: Int,
    val sourceIds: List<String>,
)

enum class UsageSummaryActionKind { TASK_CREATED, TASK_UPDATED, PLACE, MOVE, REMOVE, CLEAR, LOCK, UNLOCK, SORT, RESTORE }

data class UsageSummaryAction(
    val id: String,
    val kind: UsageSummaryActionKind,
    val taskId: String?,
    val sourceId: String?,
    val occurredAt: Long,
)

data class UsageSummarySnapshot(
    val revision: String,
    val windowStart: String,
    val windowEnd: String,
    val updatedAt: Long,
    val facts: List<UsageSummaryFact>,
    val localSummary: String,
    val localHighlights: List<String>,
    val operationCount: Int,
    val activeDays: Int,
)

data class UsageSummaryRequest(
    val requestId: String,
    val revision: String,
    val windowStart: String,
    val windowEnd: String,
    val facts: List<UsageSummaryFact>,
    val operationCount: Int = 0,
    val configurationRevision: String? = null,
    val accessRevision: String? = null,
)

data class UsageSummaryAdvice(
    val requestId: String,
    val revision: String,
    val text: String,
    val highlights: List<String>,
    val sourceFactIds: List<String>,
    val configurationRevision: String? = null,
    val accessRevision: String? = null,
)

data class UsageSummaryDocument(
    val revision: String,
    val generatedAt: Long,
    val origin: String,
    val text: String,
    val highlights: List<String>,
    val sourceFactIds: List<String>,
    val operationCount: Int = 0,
    val configurationRevision: String? = null,
    val accessRevision: String? = null,
    val windowStart: String = "",
    val windowEnd: String = "",
    val facts: List<UsageSummaryFact> = emptyList(),
    val contentVersion: Int = CURRENT_CONTENT_VERSION,
) {
    companion object {
        const val CURRENT_CONTENT_VERSION = 4
    }
}

data class UsageSummaryAttempt(
    val revision: String,
    val attemptedAt: Long,
    val configurationRevision: String,
    val accessRevision: String,
    val contentVersion: Int = UsageSummaryDocument.CURRENT_CONTENT_VERSION,
)

fun interface UsageSummaryAdvisor {
    suspend fun describe(request: UsageSummaryRequest): UsageSummaryAdvice?
}

object NoOpUsageSummaryAdvisor : UsageSummaryAdvisor {
    override suspend fun describe(request: UsageSummaryRequest): UsageSummaryAdvice? = null
}

object UsageSummaryHash {
    fun of(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

object UsageSummaryValidator {
    const val MAX_FACTS = 24
    const val MAX_FACT_TEXT = 600
    const val MAX_SUMMARY_TEXT = 1000
    const val MAX_HIGHLIGHTS = 4
    const val MAX_HIGHLIGHT_TEXT = 160

    fun valid(request: UsageSummaryRequest, response: UsageSummaryAdvice): Boolean {
        val start = runCatching { LocalDate.parse(request.windowStart) }.getOrNull() ?: return false
        val end = runCatching { LocalDate.parse(request.windowEnd) }.getOrNull() ?: return false
        if (end < start || end.toEpochDay() - start.toEpochDay() >= 30) return false
        if (request.facts.isEmpty() || request.facts.size > MAX_FACTS ||
            request.facts.map { it.id }.distinct().size != request.facts.size ||
            request.facts.any { it.id.isBlank() || it.id.length > 128 || it.text.length !in 1..MAX_FACT_TEXT || it.count < 0 }) return false
        if (response.requestId != request.requestId || response.revision != request.revision ||
            response.configurationRevision != request.configurationRevision || response.accessRevision != request.accessRevision ||
            response.text.trim().length !in 1..MAX_SUMMARY_TEXT || response.highlights.size > MAX_HIGHLIGHTS ||
            response.highlights.any { it.trim().length !in 1..MAX_HIGHLIGHT_TEXT } ||
            response.sourceFactIds.isEmpty() || response.sourceFactIds.distinct().size != response.sourceFactIds.size) return false
        val factIds = request.facts.map { it.id }.toSet()
        return response.sourceFactIds.all { it in factIds }
    }
}

/** Describes recorded use, never personality, efficiency, unrecorded work or inferred preferences. */
object UsageSummaryCalculator {
    const val RULE = "usage-summary-v2"
    const val WINDOW_DAYS = 30L

    fun calculate(
        tasks: List<Task>,
        logs: List<TaskExecutionLog>,
        sessions: List<ExecutionSession>,
        actions: List<UsageSummaryAction>,
        now: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): UsageSummarySnapshot {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val start = today.minusDays(WINDOW_DAYS - 1)
        val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
        fun inside(at: Long) = at in startMillis..now
        val taskMap = tasks.associateBy(Task::id)
        val facts = mutableListOf<UsageSummaryFact>()
        val activity = linkedMapOf<String, Long>()
        fun fact(id: String, dimension: UsageSummaryDimension, text: String, sources: List<Pair<String, Long>>) {
            if (sources.isEmpty()) return
            val unique = sources.distinctBy { it.first }.sortedBy { it.first }
            facts += UsageSummaryFact(id, dimension, text, unique.size, unique.map { it.first })
            unique.forEach { (source, at) -> activity[source] = at }
        }
        val visibleActions = actions.distinctBy { it.id }.filter {
            inside(it.occurredAt) && (it.taskId == null || it.taskId in taskMap)
        }.sortedWith(compareBy<UsageSummaryAction> { it.occurredAt }.thenBy { it.id })
        val created = tasks.filter { inside(it.createdAtEpochMillis) }.sortedBy(Task::id)
        val creationActions = visibleActions.filter { it.kind == UsageSummaryActionKind.TASK_CREATED }.associateBy { it.taskId }
        val createdRefs = created.map { task -> creationActions[task.id]?.let { "action:${it.id}" to it.occurredAt }
            ?: ("task:${task.id}:created" to task.createdAtEpochMillis) }
        fact("tasks-created", UsageSummaryDimension.FOCUS, "最近30天新建了 ${created.size} 项任务。", createdRefs)
        val edits = visibleActions.filter { it.kind == UsageSummaryActionKind.TASK_UPDATED }
        fact("tasks-edited", UsageSummaryDimension.FOCUS,
            "主动编辑了 ${edits.mapNotNull { it.taskId }.distinct().size} 项任务，共 ${edits.size} 条编辑记录。",
            edits.map { "action:${it.id}" to it.occurredAt })

        // A correction replaces the source it corrects, including every ancestor in a correction chain.
        val pastLogs = logs.filter { it.createdAtEpochMillis <= now }
            .sortedWith(compareBy<TaskExecutionLog> { it.createdAtEpochMillis }.thenBy { it.id })
        val byId = pastLogs.associateBy(TaskExecutionLog::id)
        val corrected = mutableSetOf<String>()
        pastLogs.forEach { log ->
            var ancestor = log.correctedLogId
            val visited = mutableSetOf<String>()
            while (ancestor != null && visited.add(ancestor)) {
                corrected += ancestor
                ancestor = byId[ancestor]?.correctedLogId
            }
        }
        val effectiveLogs = pastLogs.filter { it.id !in corrected && inside(it.createdAtEpochMillis) && it.taskId in taskMap }
        data class StatusEvidence(val record: TaskExecutionLog, val original: TaskExecutionLog)
        fun originalStatusEvent(record: TaskExecutionLog): TaskExecutionLog? {
            var current = record
            val visited = mutableSetOf<String>()
            while (current.eventType == ExecutionLogEventType.CORRECTION) {
                if (!visited.add(current.id)) return null
                val target = current.correctedLogId ?: return null
                current = byId[target] ?: return null
            }
            return current.takeIf { it.eventType == ExecutionLogEventType.STATUS_CHANGE }
        }
        // A correction changes the old event's value, never its event type or occurrence date.
        // Feedback written after a task completed does not complete it again in this window.
        val statusEvidence = pastLogs.filter { it.id !in corrected && it.taskId in taskMap }.mapNotNull { record ->
            originalStatusEvent(record)?.takeIf { inside(it.createdAtEpochMillis) && record.createdAtEpochMillis >= it.createdAtEpochMillis }
                ?.let { StatusEvidence(record, it) }
        }.groupBy { it.original.id }.values.map { chain ->
            chain.maxWith(compareBy<StatusEvidence> { it.record.createdAtEpochMillis }.thenBy { it.record.id })
        }.sortedWith(compareBy<StatusEvidence> { it.original.createdAtEpochMillis }.thenBy { it.original.id })
        val completed = statusEvidence.map { it.record }.filter { it.confirmedStatus == TaskStatus.COMPLETED }.distinctBy { it.taskId }
        fact("tasks-completed", UsageSummaryDimension.PROGRESS,
            "明确记录完成了 ${completed.size} 项任务。", completed.map { "log:${it.id}" to it.createdAtEpochMillis })
        val progress = effectiveLogs.filter { it.feedback.progressPercent in 1..99 || it.eventType == ExecutionLogEventType.PARTIAL_COMPLETION }
        fact("progress-reports", UsageSummaryDimension.PROGRESS,
            "对 ${progress.map { it.taskId }.distinct().size} 项任务记录了 ${progress.size} 次阶段进度。",
            progress.map { "log:${it.id}" to it.createdAtEpochMillis })
        val postpone = statusEvidence.map { it.record }.filter { it.confirmedStatus == TaskStatus.POSTPONED }
        fact("postponements", UsageSummaryDimension.ADJUSTMENT,
            "明确顺延了 ${postpone.size} 次任务；顺延原因只以用户已写下的记录为准。",
            postpone.map { "log:${it.id}" to it.createdAtEpochMillis })
        val cancelled = statusEvidence.map { it.record }.filter { it.confirmedStatus == TaskStatus.CANCELLED }
        fact("cancellations", UsageSummaryDimension.ADJUSTMENT,
            "明确取消了 ${cancelled.size} 项次任务。", cancelled.map { "log:${it.id}" to it.createdAtEpochMillis })

        val ended = sessions.distinctBy { it.id }.filter { session ->
            session.taskId in taskMap && session.endedAtEpochMillis?.let { inside(it) && session.startedAtEpochMillis <= it } == true &&
                session.runningSinceEpochMillis == null &&
                session.outcome != null && session.accumulatedMillis in 1..86_400_000
        }.sortedBy { it.id }
        val sessionRefs = ended.map { "session:${it.id}" to it.endedAtEpochMillis!! }
        fun minutes(value: Double) = if (value < .1) "不足0.1" else "%.1f".format(Locale.ROOT, value).trimEnd('0').trimEnd('.')
        if (ended.isNotEmpty()) {
            val durations = ended.map { it.accumulatedMillis / 60_000.0 }
            val median = NumericProfileCalculator.quantile(durations, .5)!!
            fact("execution-records", UsageSummaryDimension.PROGRESS,
                "记录了 ${ended.size} 段主动开始并结束的执行，净运行累计 ${minutes(durations.sum())} 分钟；暂停时间不计入，这不是任务全部实际投入的估计。", sessionRefs)
            fact("execution-length", UsageSummaryDimension.RHYTHM,
                "已记录执行段的中位时长为 ${minutes(median)} 分钟，常见范围约 ${minutes(NumericProfileCalculator.quantile(durations, .25)!!)}–${minutes(NumericProfileCalculator.quantile(durations, .75)!!)} 分钟。", sessionRefs)
            val bands = ended.groupBy { session ->
                when (Instant.ofEpochMilli(session.startedAtEpochMillis).atZone(zone).hour) {
                    in 5..11 -> "上午"
                    in 12..17 -> "下午"
                    in 18..23 -> "晚间"
                    else -> "凌晨"
                }
            }.entries.sortedWith(compareByDescending<Map.Entry<String, List<ExecutionSession>>> { it.value.size }.thenBy { it.key })
            val band = bands.first()
            fact("execution-time", UsageSummaryDimension.RHYTHM,
                "执行开始记录中，${band.key}有 ${band.value.size} 次；这表示已记录时段，不代表效率或固定偏好。",
                band.value.map { "session:${it.id}" to it.endedAtEpochMillis!! })
        }
        val changes = visibleActions.filter { it.kind !in setOf(UsageSummaryActionKind.TASK_CREATED, UsageSummaryActionKind.TASK_UPDATED) }
        fun actionName(kind: UsageSummaryActionKind) = when (kind) {
            UsageSummaryActionKind.TASK_CREATED -> "新建任务"
            UsageSummaryActionKind.TASK_UPDATED -> "编辑任务"
            UsageSummaryActionKind.PLACE -> "放置"
            UsageSummaryActionKind.MOVE -> "移动"
            UsageSummaryActionKind.REMOVE -> "移除"
            UsageSummaryActionKind.CLEAR -> "清空安排"
            UsageSummaryActionKind.LOCK -> "锁定"
            UsageSummaryActionKind.UNLOCK -> "解锁"
            UsageSummaryActionKind.SORT -> "排序"
            UsageSummaryActionKind.RESTORE -> "恢复计划"
        }
        val adjustmentKinds = changes.groupingBy { it.kind }.eachCount().entries.sortedBy { it.key.ordinal }
            .joinToString("、") { "${actionName(it.key)} ${it.value} 次" }
        fact("manual-adjustments", UsageSummaryDimension.ADJUSTMENT,
            "主动调整日程 ${changes.size} 次：$adjustmentKinds。系统自动修复不计入。",
            changes.map { "action:${it.id}" to it.occurredAt })

        // Lifecycle records without numeric feedback are still genuine use, not missing execution.
        effectiveLogs.forEach { activity["log:${it.id}"] = it.createdAtEpochMillis }
        val touchedIds = (created.map { it.id } + edits.mapNotNull { it.taskId } + effectiveLogs.map { it.taskId } + ended.map { it.taskId } + changes.mapNotNull { it.taskId }).toSet()
        val touched = tasks.filter { it.id in touchedIds }.sortedBy(Task::id)
        val trustedCategories = touched.filter { it.inputSources.category in setOf(TaskInputSource.USER_INPUT, TaskInputSource.ACCEPTED_SUGGESTION) && it.category != TaskCategory.UNSPECIFIED }
        fun categoryName(category: TaskCategory) = when (category) {
            TaskCategory.COURSE -> "课程学习"
            TaskCategory.EXTRACURRICULAR -> "课外事项"
            TaskCategory.OFFICE -> "工作事项"
            TaskCategory.LEISURE -> "休闲事项"
            TaskCategory.UNSPECIFIED -> "未分类事项"
        }
        if (trustedCategories.isNotEmpty()) {
            val counts = trustedCategories.groupingBy { it.category }.eachCount().entries.sortedBy { it.key.name }
            val text = counts.joinToString("、") { "${categoryName(it.key)} ${it.value} 项" }
            facts += UsageSummaryFact("task-focus", UsageSummaryDimension.FOCUS,
                "近期有记录且类别已明确的任务涉及$text；类别不代表个人兴趣或能力。", trustedCategories.size,
                trustedCategories.map { "task:${it.id}" })
        }
        if (touched.isNotEmpty()) {
            fun lastRecordedAt(task: Task) = (listOf(task.createdAtEpochMillis) +
                effectiveLogs.filter { it.taskId == task.id }.map { it.createdAtEpochMillis } +
                visibleActions.filter { it.taskId == task.id }.map { it.occurredAt } +
                ended.filter { it.taskId == task.id }.map { it.endedAtEpochMillis!! }).max()
            val titles = touched.sortedWith(compareByDescending<Task> { lastRecordedAt(it) }.thenBy { it.id }).take(3).map {
                it.displayName.replace(Regex("[\\p{Cntrl}\\s]+"), " ").trim().take(40)
            }.filter(String::isNotBlank)
            if (titles.isNotEmpty()) facts += UsageSummaryFact("recent-items", UsageSummaryDimension.FOCUS,
                "最近有使用记录的事项包括：${titles.joinToString("、")}。", touched.size,
                touched.map { "task:${it.id}" })
        }
        val days = activity.values.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }.toSet().size
        if (activity.isNotEmpty()) facts += UsageSummaryFact("recorded-days", UsageSummaryDimension.RHYTHM,
            "最近30天的有效使用记录分布在 $days 天，共 ${activity.size} 条；没有记录的日期不代表没有做事。", activity.size, activity.keys.sorted())
        val operationBands = activity.filterKeys { !it.startsWith("session:") }.entries.groupBy { entry ->
            when (Instant.ofEpochMilli(entry.value).atZone(zone).hour) {
                in 5..11 -> "上午"
                in 12..17 -> "下午"
                in 18..23 -> "晚间"
                else -> "凌晨"
            }
        }.entries.sortedWith(compareByDescending<Map.Entry<String, List<Map.Entry<String, Long>>>> { it.value.size }.thenBy { it.key })
        operationBands.firstOrNull()?.let { band ->
            facts += UsageSummaryFact("operation-time", UsageSummaryDimension.RHYTHM,
                "新增、编辑和进度等操作记录中，${band.key}有 ${band.value.size} 条；这是操作 App 的时段，不等同于实际执行时段。",
                band.value.size, band.value.map { it.key }.sorted())
        }
        val sortedFacts = facts.sortedWith(compareBy<UsageSummaryFact> { it.dimension.ordinal }.thenBy { it.id })
        val highlights = listOf("task-focus", "tasks-completed", "progress-reports", "execution-length", "manual-adjustments", "tasks-created")
            .mapNotNull { id -> sortedFacts.find { it.id == id }?.text }.take(3)
        val text = if (activity.isEmpty()) {
            "你的近期回顾会在这里慢慢展开，随日程、任务和进展的积累自动更新。"
        } else buildString {
            if (trustedCategories.isNotEmpty()) {
                val subjects = trustedCategories.map { categoryName(it.category) }.distinct().joinToString("、")
                append("最近，你在整理${subjects}方面的事情。")
            } else if (created.isNotEmpty()) {
                append("最近，你把新的待办记了下来。")
            } else if (edits.isNotEmpty()) {
                append("最近，你回头整理了手头的待办。")
            } else {
                append("最近，你为手头的事情留下了新的记录。")
            }
            when {
                completed.isNotEmpty() && progress.isNotEmpty() -> append("任务留下了阶段进展，也有完成的事项收了尾。")
                completed.isNotEmpty() -> append("已经完成的事情，也在待办中有了清楚的标记。")
                progress.isNotEmpty() -> append("这段时间里，手头的事情也留下了阶段进展。")
            }
            if (ended.isNotEmpty()) append("你也为手头的事情留出了专注时间。")
            when {
                changes.isNotEmpty() -> append("安排有过调整，日程也随着你的选择更新。")
                postpone.isNotEmpty() -> append("有些任务被放到了之后。")
                cancelled.isNotEmpty() -> append("你也将不再继续的任务标记为取消。")
            }
        }
        // Calendar movement changes a revision only when evidence enters/leaves the window.
        // Generated descriptions, exact refresh time and system-produced plans never enter it.
        val revision = UsageSummaryHash.of(RULE + "|" + zone.id + "|" + sortedFacts.toString() + "|" + activity.toSortedMap().toString())
        return UsageSummarySnapshot(revision, start.toString(), today.toString(), now, sortedFacts, text, highlights, activity.size, days)
    }
}
