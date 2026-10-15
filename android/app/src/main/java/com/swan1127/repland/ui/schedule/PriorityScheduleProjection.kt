package com.swan1127.repland.ui.schedule

import com.swan1127.repland.domain.model.TaskPriority
import com.swan1127.repland.domain.model.TimelineEntry
import com.swan1127.repland.domain.model.TimelineKind

data class PriorityTimeSlice(val startMinute: Int, val endMinute: Int, val entries: List<TimelineEntry>)

/** A view projection only. It never updates task order, priority, times, or track membership. */
object PriorityScheduleProjection {
    fun rank(entry: TimelineEntry, priorities: Map<String, TaskPriority>): Int = when {
        entry.kind != TimelineKind.TASK -> 110 // Protected fixed times, without inventing task priority.
        priorities[entry.taskId] == TaskPriority.UNSPECIFIED -> -1
        else -> priorities[entry.taskId]?.score ?: -1
    }

    fun label(entry: TimelineEntry, priorities: Map<String, TaskPriority>): String = when {
        entry.kind == TimelineKind.COURSE -> "课程"
        entry.kind == TimelineKind.REST -> "休息"
        entry.kind == TimelineKind.COMMITMENT -> "固定事项"
        else -> when (priorities[entry.taskId]) {
            TaskPriority.REQUIRED -> "必须"
            TaskPriority.HIGH -> "高"
            TaskPriority.MEDIUM -> "中"
            TaskPriority.LOW -> "低"
            else -> "未设置"
        }
    }

    fun ordered(entries: List<TimelineEntry>, priorities: Map<String, TaskPriority>): List<TimelineEntry> =
        entries.sortedWith(compareByDescending<TimelineEntry> { rank(it, priorities) }
            .thenBy(TimelineEntry::startMinute).thenBy(TimelineEntry::endMinute).thenBy(TimelineEntry::id))

    fun slices(entries: List<TimelineEntry>, priorities: Map<String, TaskPriority>): List<PriorityTimeSlice> {
        val unique = entries.distinctBy(TimelineEntry::id)
        val boundaries = unique.flatMap { listOf(it.startMinute, it.endMinute) }.distinct().sorted()
        return boundaries.zipWithNext().mapNotNull { (start, end) ->
            val active = unique.filter { it.startMinute < end && it.endMinute > start }
            if (active.isEmpty()) null else PriorityTimeSlice(start, end, ordered(active, priorities))
        }
    }
}
