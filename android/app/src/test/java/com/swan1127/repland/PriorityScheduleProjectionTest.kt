package com.swan1127.repland

import com.swan1127.repland.domain.model.*
import com.swan1127.repland.ui.schedule.PriorityScheduleProjection
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class PriorityScheduleProjectionTest {
    private val day = LocalDate.of(2026, 10, 15)
    private fun entry(id: String, start: Int, end: Int, kind: TimelineKind = TimelineKind.TASK) =
        TimelineEntry(id, id, kind, day, start, end, taskId = if (kind == TimelineKind.TASK) id else null)

    @Test fun partial_overlaps_split_at_real_boundaries_instead_of_merging_a_chain() {
        val a = entry("a", 540, 600)
        val b = entry("b", 570, 630)
        val c = entry("c", 615, 660)
        val slices = PriorityScheduleProjection.slices(listOf(c, b, a), mapOf("a" to TaskPriority.HIGH, "b" to TaskPriority.LOW, "c" to TaskPriority.REQUIRED))
        assertEquals(listOf(540 to 570, 570 to 600, 600 to 615, 615 to 630, 630 to 660), slices.map { it.startMinute to it.endMinute })
        assertEquals(listOf("a", "b"), slices[1].entries.map { it.id })
        assertEquals(listOf("c", "b"), slices[3].entries.map { it.id })
        assertFalse(slices.any { slice -> slice.entries.any { it.id == "a" } && slice.entries.any { it.id == "c" } })
    }

    @Test fun touching_endpoints_do_not_create_a_simultaneous_slot() {
        val slices = PriorityScheduleProjection.slices(listOf(entry("a", 540, 600), entry("b", 600, 660)), emptyMap())
        assertEquals(2, slices.size)
        assertTrue(slices.all { it.entries.size == 1 })
    }

    @Test fun unknown_priority_is_last_even_though_its_legacy_numeric_score_equals_medium() {
        val priorities = mapOf("required" to TaskPriority.REQUIRED, "high" to TaskPriority.HIGH, "medium" to TaskPriority.MEDIUM,
            "low" to TaskPriority.LOW, "unknown" to TaskPriority.UNSPECIFIED)
        val ids = listOf("unknown", "low", "medium", "high", "required", "missing")
        assertEquals(listOf("required", "high", "medium", "low", "missing", "unknown"),
            PriorityScheduleProjection.ordered(ids.map { entry(it, 540, 600) }, priorities).map { it.id })
        assertEquals("未设置", PriorityScheduleProjection.label(entry("missing", 540, 600), priorities))
    }

    @Test fun projection_preserves_source_identity_times_and_tracks() {
        val a = entry("a", 540, 630).copy(trackId = "custom", lane = 3, note = "原始备注")
        val b = entry("b", 570, 600)
        val original = listOf(a, b)
        val slices = PriorityScheduleProjection.slices(original, emptyMap())
        slices.flatMap { it.entries }.filter { it.id == "a" }.forEach { assertSame(a, it) }
        assertEquals(540, a.startMinute); assertEquals(630, a.endMinute)
        assertEquals("custom", a.trackId); assertEquals(3, a.lane); assertEquals("原始备注", a.note)
        assertEquals(listOf(a, b), original)
    }

    @Test fun fixed_entries_remain_visible_without_inventing_task_priorities() {
        val fixed = entry("course", 540, 600, TimelineKind.COURSE)
        val task = entry("task", 540, 600)
        assertEquals(listOf("course", "task"), PriorityScheduleProjection.ordered(listOf(task, fixed), mapOf("task" to TaskPriority.REQUIRED)).map { it.id })
        assertEquals("课程", PriorityScheduleProjection.label(fixed, emptyMap()))
    }

    @Test fun ties_have_stable_time_then_identity_order() {
        val entries = listOf(entry("z", 600, 660), entry("b", 540, 600), entry("a", 540, 600))
        assertEquals(listOf("a", "b", "z"), PriorityScheduleProjection.ordered(entries, emptyMap()).map { it.id })
        assertEquals(PriorityScheduleProjection.ordered(entries, emptyMap()), PriorityScheduleProjection.ordered(entries.reversed(), emptyMap()))
    }

    @Test fun empty_days_do_not_generate_placeholder_slices() {
        assertTrue(PriorityScheduleProjection.slices(emptyList(), emptyMap()).isEmpty())
    }

    @Test fun midnight_and_duplicates_preserve_the_last_real_time_boundary() {
        val entry = entry("late", 1410, 1440)
        val slices = PriorityScheduleProjection.slices(listOf(entry, entry), emptyMap())
        assertEquals(1, slices.size); assertEquals(1440, slices.single().endMinute)
        assertEquals(1, slices.single().entries.size)
    }
}
