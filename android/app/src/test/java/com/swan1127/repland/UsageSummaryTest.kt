package com.swan1127.repland

import com.swan1127.repland.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class UsageSummaryTest {
    private val now = Instant.parse("2026-10-18T04:00:00Z").toEpochMilli()
    private val zone = ZoneId.of("Asia/Shanghai")
    private fun task(id: String = "physics", created: Long = now - 86_400_000) = Task(
        id, "复习物理", "复习物理", TaskCategory.COURSE, TaskPriority.MEDIUM, null, 45,
        null, TaskStatus.NOT_STARTED, null, null, null, 0, created, created,
    )
    private fun log(id: String, status: TaskStatus, feedback: TaskFeedback = TaskFeedback(), corrected: String? = null,
                    at: Long = now - 1000, kind: ExecutionLogEventType = ExecutionLogEventType.STATUS_CHANGE) =
        TaskExecutionLog(id, "physics", kind, status, feedback, corrected, null, at)
    private fun snapshot(tasks: List<Task> = emptyList(), logs: List<TaskExecutionLog> = emptyList(),
                         sessions: List<ExecutionSession> = emptyList(), actions: List<UsageSummaryAction> = emptyList(), time: Long = now) =
        UsageSummaryCalculator.calculate(tasks, logs, sessions, actions, time, zone)

    @Test fun no_records_has_friendly_fallback_and_does_not_manufacture_dimensions() {
        val result = snapshot()
        assertEquals(0, result.operationCount)
        assertTrue(result.facts.isEmpty())
        assertTrue(result.localHighlights.isEmpty())
        assertTrue(result.localSummary.contains("近期回顾"))
        assertEquals(result.revision, snapshot(time = now + 86_400_000).revision)
    }

    @Test fun system_timestamp_update_does_not_become_a_user_edit_or_change_content_revision() {
        val old = task(created = now - 40 * 86_400_000L)
        val before = snapshot(tasks = listOf(old))
        val after = snapshot(tasks = listOf(old.copy(updatedAtEpochMillis = now, scheduledForDate = java.time.LocalDate.of(2026, 10, 19))))
        assertEquals(before.revision, after.revision)
        assertEquals(0, after.operationCount)
        assertFalse(after.facts.any { it.id == "tasks-edited" })
    }

    @Test fun narrative_keeps_statistics_in_evidence_and_does_not_imply_unrecorded_progress() {
        val createdOnly = snapshot(tasks = listOf(task("one"), task("two"), task("three")))
        assertEquals(3, createdOnly.facts.single { it.id == "tasks-created" }.count)
        assertTrue(createdOnly.facts.single { it.id == "tasks-created" }.text.contains("3"))
        assertFalse(createdOnly.localSummary.any(Char::isDigit))
        assertFalse(createdOnly.localSummary.contains("进展") || createdOnly.localSummary.contains("完成"))

        val completed = snapshot(tasks = listOf(task()), logs = listOf(log("done", TaskStatus.COMPLETED)),
            actions = listOf(UsageSummaryAction("moved", UsageSummaryActionKind.MOVE, "physics", "segment", now - 1000)))
        assertTrue(completed.localSummary.contains("已经完成") && completed.localSummary.contains("安排有过调整"))
        assertFalse(completed.localSummary.any(Char::isDigit))
        assertFalse(completed.localSummary.contains("根据记录") || completed.localSummary.contains("分钟"))
        assertEquals(listOf("action:moved"), completed.facts.single { it.id == "manual-adjustments" }.sourceIds)
    }

    @Test fun only_real_ended_sessions_supply_execution_time_and_pause_is_not_added() {
        val ended = ExecutionSession("round", "physics", "复习物理", "plan", "segment", 3600,
            now - 7_200_000, accumulatedMillis = 1_800_000, runningSinceEpochMillis = null,
            endedAtEpochMillis = now - 1000, outcome = ExecutionOutcome.PARTIAL)
        val running = ended.copy(id = "running", runningSinceEpochMillis = now - 1000, endedAtEpochMillis = null)
        val result = snapshot(tasks = listOf(task()), sessions = listOf(ended, running))
        val execution = result.facts.single { it.id == "execution-records" }
        assertEquals(1, execution.count)
        assertTrue(execution.text.contains("累计 30 分钟"))
        assertFalse(execution.sourceIds.any { it.contains("running") })
        assertFalse(result.facts.any { it.id.startsWith("estimate") })
    }

    @Test fun correction_chain_replaces_completion_and_partial_progress_instead_of_counting_both() {
        val records = listOf(
            log("done", TaskStatus.COMPLETED, at = now - 3000),
            log("half", TaskStatus.IN_PROGRESS, TaskFeedback(progressPercent = 50), "done", now - 2000, ExecutionLogEventType.CORRECTION),
            log("restore", TaskStatus.NOT_STARTED, corrected = "half", kind = ExecutionLogEventType.CORRECTION),
        )
        val result = snapshot(tasks = listOf(task()), logs = records)
        assertFalse(result.facts.any { it.id == "tasks-completed" || it.id == "progress-reports" })
        assertTrue(result.facts.flatMap { it.sourceIds }.none { it == "log:done" || it == "log:half" })
        assertTrue(result.facts.flatMap { it.sourceIds }.contains("log:restore"))
    }

    @Test fun feedback_on_an_old_completed_task_is_not_a_new_completion() {
        val old = task(created = now - 40 * 86_400_000L)
        val result = snapshot(tasks = listOf(old), logs = listOf(
            log("old-completion", TaskStatus.COMPLETED, at = now - 35 * 86_400_000L),
            log("note", TaskStatus.COMPLETED, TaskFeedback(completionResult = "补充备注"), kind = ExecutionLogEventType.FEEDBACK),
        ))
        assertEquals(1, result.operationCount)
        assertFalse(result.facts.any { it.id == "tasks-completed" })
        assertTrue(result.facts.flatMap { it.sourceIds }.contains("log:note"))
    }

    @Test fun lifecycle_corrections_retain_the_original_event_type_and_occurrence_window() {
        val old = task(created = now - 40 * 86_400_000L)
        for ((status, factId) in listOf(TaskStatus.COMPLETED to "tasks-completed", TaskStatus.POSTPONED to "postponements", TaskStatus.CANCELLED to "cancellations")) {
            val correction = log("correction", status, corrected = "original", kind = ExecutionLogEventType.CORRECTION)
            val oldOriginal = log("original", status, at = now - 35 * 86_400_000L)
            assertFalse(snapshot(tasks = listOf(old), logs = listOf(oldOriginal, correction)).facts.any { it.id == factId })
            val recentOriginal = oldOriginal.copy(createdAtEpochMillis = now - 15 * 86_400_000L)
            val recent = snapshot(tasks = listOf(old), logs = listOf(recentOriginal, correction))
            assertEquals(listOf("log:correction"), recent.facts.single { it.id == factId }.sourceIds)
            val feedback = recentOriginal.copy(eventType = ExecutionLogEventType.FEEDBACK)
            assertFalse(snapshot(tasks = listOf(old), logs = listOf(feedback, correction)).facts.any { it.id == factId })
        }
    }

    @Test fun only_explicit_boundary_events_count_as_manual_adjustments_and_creation_is_deduplicated() {
        val actions = listOf(
            UsageSummaryAction("created", UsageSummaryActionKind.TASK_CREATED, "physics", null, now - 86_400_000),
            UsageSummaryAction("moved", UsageSummaryActionKind.MOVE, "physics", "old-segment", now - 1000),
        )
        val result = snapshot(tasks = listOf(task()), actions = actions)
        assertEquals(2, result.operationCount)
        assertEquals(1, result.facts.single { it.id == "manual-adjustments" }.count)
        assertEquals(listOf("action:moved"), result.facts.single { it.id == "manual-adjustments" }.sourceIds)
        val adjustmentText = result.facts.single { it.id == "manual-adjustments" }.text
        assertTrue(adjustmentText.contains("移动 1 次"))
        assertFalse(adjustmentText.contains("放置") || adjustmentText.contains("锁定"))
    }

    @Test fun clock_and_input_order_do_not_invalidate_content_but_expiring_evidence_does() {
        val first = task("one")
        val second = task("two", now - 2 * 86_400_000)
        val a = snapshot(tasks = listOf(first, second))
        val b = snapshot(tasks = listOf(second, first), time = now + 86_400_000)
        assertEquals(a.revision, b.revision)
        assertNotEquals(a.windowEnd, b.windowEnd)
        assertNotEquals(a.revision, snapshot(tasks = listOf(first, second), time = now + 40 * 86_400_000L).revision)
    }

    @Test fun future_and_deleted_task_evidence_is_not_included() {
        val future = task(created = now + 1000)
        val result = snapshot(tasks = listOf(future), logs = listOf(log("future", TaskStatus.COMPLETED, at = now + 1000)),
            actions = listOf(UsageSummaryAction("removed", UsageSummaryActionKind.TASK_UPDATED, "deleted", null, now - 1000)))
        assertEquals(0, result.operationCount)
        assertTrue(result.facts.isEmpty())
    }

    @Test fun operation_time_is_separate_from_execution_time_when_no_timer_was_used() {
        val result = snapshot(tasks = listOf(task()))
        assertTrue(result.facts.single { it.id == "operation-time" }.text.contains("操作 App 的时段"))
        assertFalse(result.facts.any { it.id == "execution-time" || it.id == "execution-records" })
    }

    @Test fun validator_rejects_unsupported_fact_reference_and_changed_access_metadata() {
        val data = snapshot(tasks = listOf(task()))
        val request = UsageSummaryRequest("request", data.revision, data.windowStart, data.windowEnd, data.facts,
            data.operationCount, "provider", "access")
        val valid = UsageSummaryAdvice("request", data.revision, "最近新增了课程学习事项。", emptyList(),
            listOf("tasks-created"), "provider", "access")
        assertTrue(UsageSummaryValidator.valid(request, valid))
        assertFalse(UsageSummaryValidator.valid(request, valid.copy(sourceFactIds = listOf("invented"))))
        assertFalse(UsageSummaryValidator.valid(request, valid.copy(accessRevision = "other")))
        assertFalse(UsageSummaryValidator.valid(request, valid.copy(text = "x".repeat(1001))))
        assertFalse(UsageSummaryValidator.valid(request, valid.copy(sourceFactIds = listOf("tasks-created", "tasks-created"))))
    }
}
