package com.swan1127.repland

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.swan1127.repland.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

/** Task sorting and persisted planning previews stay isolated from the quick assistant composer. */
class PlanningIsolationWorkflowUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @org.junit.Before
    fun use_manual_planning_mode_for_legacy_workflow() {
        pauseAutomaticPlanningForLegacyWorkflow(rule.activity)
    }
    private val app get() = (rule.activity.application as ReplandApplication).appContainer
    private fun node(tag: String): SemanticsNodeInteraction {
        rule.waitUntil(20_000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        return rule.onNodeWithTag(tag).also { runCatching { it.performScrollTo() } }
    }

    @Test fun sort_keeps_schedule_and_unsent_quick_input_survives_independent_draft_recreation_and_discard() {
        check(rule.activity.packageName == "com.swan1127.repland.qa")
        val id = "qa-isolation-${UUID.randomUUID()}"
        val originalDraft = runBlocking { app.planRepository.observeDraft().first() }
        val originalWorkspace = runBlocking { app.planRepository.observeAssistantWorkspace().first() }
        val originalOrder = runBlocking { app.planRepository.observeTaskOrder().first() }
        val availabilityTitle = "QA availability $id"
        var availabilityId: String? = null
        var undoNeeded = false
        try {
            runBlocking {
                app.planRepository.saveDraft(null)
                app.planRepository.saveAssistantWorkspace(null)
                app.taskRepository.save(TaskDraft(id = id, displayName = "QA planning isolation", totalDurationMinutes = 30))
                app.timeRepository.saveDateOverride(DateOverrideDraft(title = availabilityTitle,
                    type = DateOverrideType.AVAILABLE, date = LocalDate.now().plusDays(2), startMinute = 600, endMinute = 720))
                availabilityId = app.timeRepository.observeDateOverrides().first().single { it.title == availabilityTitle }.id
            }
            val originalPlan = runBlocking { app.planRepository.observeCurrentPlan().first() }
            val tasks = runBlocking { app.taskRepository.observeTasks().first() }
            val logs = runBlocking { app.taskRepository.observeExecutionLogs(id).first() }
            node("navigation-tasks").performClick()
            node("auto-sort-tasks").assertIsEnabled().performClick()
            node("accept-plan-draft")
            assertTrue(runBlocking { app.planRepository.observeDraft().first() }!!.orderOnly)
            assertEquals(originalPlan, runBlocking { app.planRepository.observeCurrentPlan().first() })
            node("accept-plan-draft").performClick()
            rule.waitUntil(20_000) { runBlocking { app.planRepository.observeDraft().first() == null } }
            undoNeeded = true
            assertEquals(originalPlan, runBlocking { app.planRepository.observeCurrentPlan().first() })
            node("undo-task-sort").performClick()
            rule.waitUntil(20_000) { runBlocking { app.planRepository.observeTaskOrder().first() == originalOrder } }
            undoNeeded = false
            rule.openScheduleAssistant()
            val unsent = "QA-unsent-${id.takeLast(8)}"
            node("autonomous-input").performTextReplacement(unsent)
            node("assistant-close").performClick()
            rule.openScheduleAssistant()
            node("autonomous-input").assertTextContains(unsent)
            rule.activityRule.scenario.recreate()
            node("autonomous-input").assertTextContains(unsent)
            node("assistant-close").performClick()
            // The removed formulate button is no longer a user entry point. Seed its real
            // domain preview to verify that persisted preview UI cannot consume unsent input.
            val draft = runBlocking { app.planningOperationService.preview(PlanningPreviewKind.FORMULATE) }
            node("accept-plan-draft")
            assertFalse(draft.orderOnly)
            assertEquals(originalPlan, runBlocking { app.planRepository.observeCurrentPlan().first() })
            rule.activityRule.scenario.recreate()
            node("accept-plan-draft")
            assertEquals(draft, runBlocking { app.planRepository.observeDraft().first() })
            rule.onNodeWithText("放弃本次调整").performClick()
            rule.waitUntil(20_000) { runBlocking { app.planRepository.observeDraft().first() == null } }
            rule.openScheduleAssistant()
            node("autonomous-input").assertTextContains(unsent)
            assertNull(runBlocking { app.planRepository.observeAssistantWorkspace().first() })
            assertEquals(originalPlan, runBlocking { app.planRepository.observeCurrentPlan().first() })
            assertEquals(tasks, runBlocking { app.taskRepository.observeTasks().first() })
            assertEquals(logs, runBlocking { app.taskRepository.observeExecutionLogs(id).first() })
            node("assistant-close").performClick()
        } finally {
            runBlocking {
                if (undoNeeded) app.planRepository.undoTaskOrder()
                availabilityId?.let { app.timeRepository.deleteDateOverride(it) }
                app.planRepository.saveAssistantWorkspace(originalWorkspace)
                app.planRepository.saveDraft(originalDraft)
            }
        }
    }
}
