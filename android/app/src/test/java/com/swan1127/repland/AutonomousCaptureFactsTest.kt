package com.swan1127.repland

import com.swan1127.repland.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class AutonomousCaptureFactsTest {
    @Test fun paraphrasing_cannot_replace_explicit_time_or_duration() {
        val local = ArrangementAssistantInterpreter.interpret("明天下午16点复习物理45分钟").candidates
        val remote = listOf(local.single().copy(title = "物理复习", durationMinutes = 60, timeHint = ArrangementTimeHint(720)))
        val result = AutonomousCaptureFacts.reconcile(local, remote).single()
        assertEquals(local.single().title, result.title)
        assertEquals(45, result.durationMinutes)
        assertEquals(960, result.timeHint.explicitStartMinute)
        assertTrue(AutonomousTaskIntentParser.parse("查看一周内的事项") is AutonomousTaskIntent.NeedsClarification)
        assertTrue(AutonomousTaskIntentParser.parse("把报告改到17点") is AutonomousTaskIntent.NeedsClarification)
    }
    @Test fun multi_item_ambiguity_keeps_original_facts_and_single_unknown_duration_can_be_estimated() {
        assertTrue(AutonomousCaptureFacts.recordOnly("只记着：复习物理"))
        assertTrue(AutonomousTaskIntentParser.parse("只记录：尚未完成的报告") is AutonomousTaskIntent.Capture)
        val local = ArrangementAssistantInterpreter.interpret("复习物理45分钟，整理笔记30分钟").candidates
        assertEquals(local, AutonomousCaptureFacts.reconcile(local, local.map { it.copy(title = "另一个标题") }))
        val unknown = ArrangementAssistantInterpreter.interpret("整理笔记").candidates
        assertEquals(60, AutonomousCaptureFacts.reconcile(unknown, listOf(unknown.single().copy(durationMinutes = 60))).single().durationMinutes)
    }
}
