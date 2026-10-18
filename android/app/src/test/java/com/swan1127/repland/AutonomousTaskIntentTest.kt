package com.swan1127.repland

import com.swan1127.repland.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class AutonomousTaskIntentTest {
    @Test fun explicit_completed_reports_preserve_the_named_task_and_user_evidence() {
        listOf("报告写完了", "我完成了报告", "报告已经完成了", "请帮我把报告标记为完成", "标记报告为已完成").forEach { text ->
            val command = commands(text).single()
            assertEquals(text, AutonomousTaskTarget.Named("报告"), command.target)
            assertEquals(text, AutonomousTaskChange.Complete, command.change)
            assertEquals(AutonomousFactSource.USER_EXPLICIT, command.factSource)
            assertEquals(text, command.sourceText)
        }
    }

    @Test fun explicit_cancel_requests_do_not_require_a_repeated_confirmation() {
        listOf("取消跑步", "请取消跑步", "把跑步取消", "跑步不做了", "我取消了跑步").forEach { text ->
            val command = commands(text).single()
            assertEquals(text, AutonomousTaskTarget.Named("跑步"), command.target)
            assertEquals(text, AutonomousTaskChange.Cancel, command.change)
        }
    }

    @Test fun progress_is_absolute_and_remaining_work_is_inverted_without_guessing_minutes() {
        mapOf("听力做了一半" to 50, "听力完成了50%" to 50, "听力还剩一半" to 50,
            "听力还剩一半没完成" to 50, "听力剩余20%" to 80, "听力进度为0%" to 0,
            "把听力进度改为75％" to 75, "听力完成100%" to 100).forEach { (text, percent) ->
            val command = commands(text).single()
            assertEquals(text, AutonomousTaskTarget.Named("听力"), command.target)
            assertEquals(text, AutonomousTaskChange.Progress(percent), command.change)
            assertEquals(text, command.sourceText)
        }
        assertTrue(AutonomousTaskIntentParser.parse("听力预计需要50分钟") is AutonomousTaskIntent.Capture)
    }

    @Test fun task_creation_and_prospective_actions_are_never_completed_reports() {
        listOf("完成报告", "明天完成报告", "我要完成报告", "准备英语听力", "新增任务：报告写完了",
            "新建一个任务：取消订阅", "记下报告写完了", "今晚安排复习英语").forEach { text ->
            assertEquals(text, AutonomousTaskIntent.Capture(text), AutonomousTaskIntentParser.parse(text))
        }
    }

    @Test fun negation_never_completes_or_cancels_the_task() {
        listOf("报告还没完成", "报告没写完", "报告未完成", "不要取消跑步", "别取消跑步",
            "听力还没完成50%", "听力不是还剩一半").forEach { text ->
            assertAmbiguity(text, AutonomousIntentAmbiguity.NEGATED_STATUS)
        }
    }

    @Test fun quotes_examples_hypotheticals_and_questions_never_become_facts() {
        listOf("“报告写完了”", "例如报告写完了", "如果报告写完了", "他说报告写完了", "报告可能完成了").forEach {
            assertAmbiguity(it, AutonomousIntentAmbiguity.QUOTED_OR_HYPOTHETICAL)
        }
        listOf("报告写完了吗", "报告是否完成了", "取消跑步吗？").forEach {
            assertAmbiguity(it, AutonomousIntentAmbiguity.QUESTION)
        }
        val named = commands("把“英语听力”标记为完成").single()
        assertEquals(AutonomousTaskTarget.Named("英语听力"), named.target)
    }

    @Test fun one_utterance_can_report_several_independent_explicit_changes() {
        val parsed = commands("报告写完了，取消跑步；听力做了一半")
        assertEquals(listOf(AutonomousTaskTarget.Named("报告"), AutonomousTaskTarget.Named("跑步"), AutonomousTaskTarget.Named("听力")), parsed.map { it.target })
        assertEquals(listOf(AutonomousTaskChange.Complete, AutonomousTaskChange.Cancel, AutonomousTaskChange.Progress(50)), parsed.map { it.change })
        assertEquals(listOf("报告写完了", "取消跑步", "听力做了一半"), parsed.map { it.sourceText })
    }

    @Test fun ambiguous_batches_are_not_partially_executed() {
        assertAmbiguity("报告写完了，取消报告", AutonomousIntentAmbiguity.CONFLICTING_COMMANDS)
        assertAmbiguity("报告写完了，明天练听力", AutonomousIntentAmbiguity.MIXED_OPERATIONS)
        assertAmbiguity("明天练听力，报告写完了", AutonomousIntentAmbiguity.MIXED_OPERATIONS)
        assertAmbiguity("报告和听力都完成了", AutonomousIntentAmbiguity.MULTIPLE_TARGETS)
        assertAmbiguity("报告、听力完成了", AutonomousIntentAmbiguity.MULTIPLE_TARGETS)
        assertEquals(1, commands("报告写完了，报告写完了").size)
    }

    @Test fun invalid_percentages_are_not_coerced_or_wrapped() {
        listOf("听力完成101%", "听力剩余101%", "听力完成-10%", "听力进度为1.5%", "听力完成9999999999999999999%").forEach {
            assertAmbiguity(it, AutonomousIntentAmbiguity.INVALID_PROGRESS)
        }
        assertAmbiguity("  ", AutonomousIntentAmbiguity.EMPTY_INPUT)
    }

    @Test fun queries_and_arrangement_commands_have_explicit_relative_day_scopes() {
        mapOf("查看今天的任务" to AutonomousDay.TODAY, "明天要做什么" to AutonomousDay.TOMORROW,
            "今晚有什么安排？" to AutonomousDay.TONIGHT).forEach { (text, day) ->
            assertEquals(text, AutonomousTaskIntent.Query(AutonomousQueryScope.SCHEDULED, day), AutonomousTaskIntentParser.parse(text))
        }
        assertEquals(AutonomousTaskIntent.Query(AutonomousQueryScope.INBOX), AutonomousTaskIntentParser.parse("查询待安排任务"))
        assertEquals(AutonomousTaskIntent.Query(AutonomousQueryScope.OVERDUE), AutonomousTaskIntentParser.parse("查看逾期任务"))
        assertEquals(AutonomousTaskIntent.Query(AutonomousQueryScope.ACTIVE), AutonomousTaskIntentParser.parse("列出所有任务"))
        listOf("查看待办", "查看待办事项", "列出待处理事项").forEach { text ->
            assertEquals(text, AutonomousTaskIntent.Query(AutonomousQueryScope.ACTIVE), AutonomousTaskIntentParser.parse(text))
        }
        listOf("查看明天的待办", "查询明天待办事项", "列出明天的待处理事项").forEach { text ->
            assertEquals(text, AutonomousTaskIntent.Query(AutonomousQueryScope.SCHEDULED, AutonomousDay.TOMORROW), AutonomousTaskIntentParser.parse(text))
        }
        mapOf("安排今天" to AutonomousDay.TODAY, "帮我安排明天的任务" to AutonomousDay.TOMORROW,
            "重排今晚" to AutonomousDay.TONIGHT, "今天的任务重新安排一下" to AutonomousDay.TODAY).forEach { (text, day) ->
            assertEquals(text, AutonomousTaskIntent.Arrange(day), AutonomousTaskIntentParser.parse(text))
        }
        assertTrue(AutonomousTaskIntentParser.parse("查看笔记30分钟") is AutonomousTaskIntent.Capture)
    }

    @Test fun target_resolution_is_exact_and_duplicates_require_disambiguation() {
        val report = task("a", "物理报告")
        val english = task("b", "英语听力")
        val result = AutonomousTaskIntentParser.resolveTarget(AutonomousTaskTarget.Named("物理报告"), listOf(report, english))
        assertEquals(AutonomousTargetResolution.Found(report), result)
        assertTrue(AutonomousTaskIntentParser.resolveTarget(AutonomousTaskTarget.Named("报告"), listOf(report, english)) is AutonomousTargetResolution.Missing)
        assertTrue(AutonomousTaskIntentParser.resolveTarget(AutonomousTaskTarget.Named("物理报告"), listOf(report, report.copy(id = "c"))) is AutonomousTargetResolution.Ambiguous)
        assertEquals(AutonomousTargetResolution.Found(english), AutonomousTaskIntentParser.resolveTarget(AutonomousTaskTarget.Named("“英语听力”"), listOf(report, english)))
        assertEquals(AutonomousTaskTarget.Named("成都"), commands("成都完成了").single().target)
    }

    @Test fun pronouns_need_a_real_context_and_never_choose_the_first_task() {
        val contextual = commands("完成这个").single().target
        assertEquals(AutonomousTaskTarget.Contextual("这个"), contextual)
        val tasks = listOf(task("a", "物理报告"), task("b", "英语听力"))
        assertEquals(AutonomousTargetResolution.ContextRequired, AutonomousTaskIntentParser.resolveTarget(contextual, tasks))
        assertEquals(AutonomousTargetResolution.Found(tasks[1]), AutonomousTaskIntentParser.resolveTarget(contextual, tasks, "b"))
        assertTrue(AutonomousTaskIntentParser.resolveTarget(contextual, tasks, "missing") is AutonomousTargetResolution.Missing)
    }

    private fun commands(text: String): List<AutonomousStatusCommand> {
        val result = AutonomousTaskIntentParser.parse(text)
        assertTrue("$text -> $result", result is AutonomousTaskIntent.StatusUpdates)
        return (result as AutonomousTaskIntent.StatusUpdates).commands
    }

    private fun assertAmbiguity(text: String, reason: AutonomousIntentAmbiguity) {
        assertEquals(text, AutonomousTaskIntent.NeedsClarification(reason, text), AutonomousTaskIntentParser.parse(text))
    }

    private fun task(id: String, name: String) = Task(id, name, name, TaskCategory.UNSPECIFIED,
        TaskPriority.UNSPECIFIED, null, 60, null, TaskStatus.NOT_STARTED, null, null, null, 0, 1L, 1L)
}
