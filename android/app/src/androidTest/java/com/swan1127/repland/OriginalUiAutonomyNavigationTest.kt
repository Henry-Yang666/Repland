package com.swan1127.repland

import android.database.sqlite.SQLiteDatabase
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swan1127.repland.domain.model.TaskDraft
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Schedule owns the single assistant entry; the original capture workflow stays available. */
@RunWith(AndroidJUnit4::class)
class OriginalUiAutonomyNavigationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Before fun use_isolated_qa_app_with_automatic_maintenance_paused() {
        check(rule.activity.packageName == "com.swan1127.repland.qa")
        pauseAutomaticPlanningForLegacyWorkflow(rule.activity)
    }

    private fun waitTag(tag: String) = rule.waitUntil(10_000) {
        rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }

    private fun closeAssistant() {
        rule.onNodeWithTag("assistant-close").performClick()
        rule.waitUntil(10_000) {
            rule.onAllNodesWithTag("assistant-sheet").fetchSemanticsNodes().isEmpty()
        }
        waitTag("today-page-scroll")
    }

    @Test fun assistant_has_one_schedule_floating_entry_and_no_bottom_tab() {
        waitTag("navigation-today")
        rule.onNodeWithTag("navigation-agent").assertDoesNotExist()
        rule.onNodeWithTag("navigation-today").performClick()
        waitTag("schedule-assistant-fab")
        rule.onNodeWithTag("schedule-assistant-fab").assertIsDisplayed()
        rule.onNodeWithTag("add-task").assertDoesNotExist()

        rule.openScheduleAssistant()
        rule.onNodeWithTag("assistant-sheet").assertIsDisplayed()
        rule.onNodeWithTag("autonomous-input").assertIsDisplayed()
        rule.onNodeWithTag("agent-prompt").assertDoesNotExist()
        rule.onNodeWithTag("autonomy-status-card").assertDoesNotExist()
        rule.onNodeWithTag("autonomous-legacy").assertDoesNotExist()
        rule.onNodeWithTag("autonomous-time-settings").assertDoesNotExist()
        closeAssistant()

        rule.onNodeWithTag("navigation-tasks").performClick()
        waitTag("add-task")
        rule.onNodeWithTag("add-task").assertIsDisplayed()
        rule.onNodeWithTag("schedule-assistant-fab").assertDoesNotExist()
        rule.onNodeWithTag("navigation-mine").performClick()
        waitTag("mine-profile-page")
        waitTag("usage-summary-card")
        rule.onNodeWithTag("usage-summary-text").assertExists()
        rule.onNodeWithTag("numeric-profile-open").assertDoesNotExist()
        rule.onNodeWithTag("schedule-assistant-fab").assertDoesNotExist()
        rule.onNodeWithTag("add-task").assertDoesNotExist()
        rule.onNodeWithTag("navigation-agent").assertDoesNotExist()
    }

    @Test fun original_home_capture_still_opens_without_changing_tasks() {
        val tasks = (rule.activity.application as ReplandApplication).appContainer.taskRepository
        val before = runBlocking { tasks.observeTasks().first() }
        waitTag("navigation-today")
        rule.onNodeWithTag("navigation-today").performClick()
        for (entry in listOf("schedule-create-task", "schedule-create-plus")) {
            rule.onNodeWithTag("today-page-scroll").performScrollToNode(hasTestTag(entry))
            rule.onNodeWithTag(entry).performClick()
            waitTag("task-capture-input")
            rule.onNodeWithTag("task-capture-input").assertExists()
            rule.onNodeWithTag("task-capture-close").performClick()
            waitTag("today-page-scroll")
        }
        assertEquals(before, runBlocking { tasks.observeTasks().first() })
    }

    @Test fun unsent_assistant_input_survives_close_reopen_and_activity_recreation() {
        val draft = "还未发送的一句话事项 ${UUID.randomUUID()}"
        val app = (rule.activity.application as ReplandApplication).appContainer
        val before = runBlocking { app.taskRepository.observeTasks().first() }
        rule.openScheduleAssistant()
        rule.onNodeWithTag("autonomous-input").performScrollTo().performTextReplacement(draft)
        closeAssistant()

        rule.openScheduleAssistant()
        rule.onNodeWithTag("autonomous-input").performScrollTo().assertTextContains(draft, substring = false)
        rule.activityRule.scenario.recreate()
        waitTag("assistant-sheet")
        rule.onNodeWithTag("autonomous-input").performScrollTo().assertTextContains(draft, substring = false)
        closeAssistant()

        rule.activityRule.scenario.recreate()
        rule.openScheduleAssistant()
        rule.onNodeWithTag("autonomous-input").performScrollTo().assertTextContains(draft, substring = false)
        assertEquals(before, runBlocking { app.taskRepository.observeTasks().first() })
    }

    @Test fun schedule_assistant_query_preserves_tasks_plans_and_execution_evidence() {
        val app = (rule.activity.application as ReplandApplication).appContainer
        val id = "qa-floating-query-${UUID.randomUUID()}"
        runBlocking { app.taskRepository.save(TaskDraft(id = id, displayName = "悬浮助手查询样例 $id", totalDurationMinutes = 30)) }
        try {
            rule.openScheduleAssistant()
            val tasksBefore = runBlocking { app.taskRepository.observeTasks().first() }
            val plansBefore = runBlocking { app.planRepository.observePlanHistory().first() }
            val currentBefore = runBlocking { app.planRepository.observeCurrentPlan().first() }
            val logsBefore = runBlocking { tasksBefore.associate { it.id to app.taskRepository.observeExecutionLogs(it.id).first() } }
            val delegationBefore = runBlocking { app.autonomyRepository.observe().first() }

            rule.onNodeWithTag("autonomous-input").performScrollTo().performTextReplacement("查看待办")
            rule.onNodeWithTag("autonomous-submit").performScrollTo().performClick()
            waitTag("autonomous-response")
            rule.onNodeWithTag("autonomous-response").performScrollTo().assertTextContains("找到", substring = true)

            assertEquals(tasksBefore, runBlocking { app.taskRepository.observeTasks().first() })
            assertEquals(plansBefore, runBlocking { app.planRepository.observePlanHistory().first() })
            assertEquals(currentBefore, runBlocking { app.planRepository.observeCurrentPlan().first() })
            assertEquals(logsBefore, runBlocking { tasksBefore.associate { it.id to app.taskRepository.observeExecutionLogs(it.id).first() } })
            assertEquals(delegationBefore, runBlocking { app.autonomyRepository.observe().first() })
        } finally {
            SQLiteDatabase.openDatabase(rule.activity.getDatabasePath("repland.db").absolutePath, null,
                SQLiteDatabase.OPEN_READWRITE).use { it.execSQL("DELETE FROM tasks WHERE id = ?", arrayOf(id)) }
        }
    }
}
