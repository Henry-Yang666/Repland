package com.swan1127.repland

import android.database.sqlite.SQLiteDatabase
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.text.AnnotatedString
import com.swan1127.repland.domain.model.*
import com.swan1127.repland.ui.plan.PlanViewModel
import com.swan1127.repland.ui.preferences.CategoryPreferenceViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.io.File

class ContextReadWorkflowUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @org.junit.Before
    fun use_manual_planning_mode_for_legacy_workflow() {
        pauseAutomaticPlanningForLegacyWorkflow(rule.activity)
    }
    private val container get() = (rule.activity.application as ReplandApplication).appContainer.also {
        check(rule.activity.packageName == "com.swan1127.repland.qa")
    }
    private fun waitTag(tag: String) = rule.waitUntil(10_000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun gone(tag: String) = rule.waitUntil(10_000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty() }
    private fun fill(tag: String, value: String) = rule.onNodeWithTag(tag).performScrollTo().performTextReplacement(value)
    private fun planFault(): Pair<ReadFaultPlans, PlanViewModel> {
        val fault = ReadFaultPlans(container.planRepository); lateinit var vm: PlanViewModel
        rule.runOnUiThread {
            vm = PlanViewModel(fault, container.planDraftGenerator, container.timeRepository, container.taskRepository, container.planningOperationService)
            rule.activity.viewModelStore.put("androidx.lifecycle.ViewModelProvider.DefaultKey:${PlanViewModel::class.java.canonicalName}", vm)
        }
        rule.activityRule.scenario.recreate(); return fault to vm
    }
    private fun preferenceFault(): Pair<ReadFaultPreferences, CategoryPreferenceViewModel> {
        val fault = ReadFaultPreferences(container.categoryPreferenceRepository); lateinit var vm: CategoryPreferenceViewModel
        rule.runOnUiThread {
            vm = CategoryPreferenceViewModel(fault)
            rule.activity.viewModelStore.put("androidx.lifecycle.ViewModelProvider.DefaultKey:${CategoryPreferenceViewModel::class.java.canonicalName}", vm)
        }
        rule.activityRule.scenario.recreate(); return fault to vm
    }
    private fun screenshot(name: String) {
        rule.mainClock.advanceTimeByFrame(); rule.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        try { File(rule.activity.getExternalFilesDir(null), name).outputStream().use { check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) } }
        finally { bitmap.recycle() }
    }
    private fun seedTask(id: String) = runBlocking {
        container.taskRepository.save(TaskDraft(id, "QA-context24-$id", "", TaskCategory.COURSE, TaskPriority.MEDIUM, 1, 60, null))
    }
    private fun cleanupTask(id: String) {
        check(rule.activity.packageName == "com.swan1127.repland.qa")
        SQLiteDatabase.openDatabase(rule.activity.getDatabasePath("repland.db").absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use {
            it.execSQL("DELETE FROM tasks WHERE id = ?", arrayOf(id))
        }
    }

    @Test fun unreadable_persisted_preview_can_be_kept_closed_recreated_and_resumed_without_writing() {
        val id = UUID.randomUUID().toString()
        try {
            seedTask(id)
            val originalPlan = runBlocking { container.planRepository.observeCurrentPlan().first() }
            val originalOrder = runBlocking { container.planRepository.observeTaskOrder().first() }
            val draft = runBlocking { container.planningOperationService.preview(PlanningPreviewKind.SORT_ONLY) }
            val (fault, vm) = planFault(); waitTag("accept-plan-draft")
            fault.failedPart.value = "draft"; rule.waitUntil(10_000) { vm.uiState.value.readError != null }
            rule.onNodeWithTag("accept-plan-draft").assertIsNotEnabled()
            rule.onNodeWithText("保留草案并关闭").assertIsEnabled().performClick(); gone("accept-plan-draft")
            assertEquals(draft, runBlocking { container.planRepository.observeDraft().first() })
            waitTag("plan-resume-preview"); rule.activityRule.scenario.recreate(); waitTag("plan-resume-preview")
            rule.onNodeWithTag("plan-resume-preview").performClick(); waitTag("accept-plan-draft")
            rule.onNodeWithTag("accept-plan-draft").assertIsNotEnabled()
            fault.failedPart.value = null
            rule.onNode(hasTestTag("retry-data-read") and hasAnyAncestor(hasTestTag("editor-sheet"))).performScrollTo().performClick()
            rule.waitUntil(10_000) { vm.uiState.value.isTrusted && vm.workspaceUiState.value.isTrusted }
            rule.onNodeWithTag("accept-plan-draft").assertIsEnabled()
            assertEquals(originalPlan, runBlocking { container.planRepository.observeCurrentPlan().first() })
            assertEquals(originalOrder, runBlocking { container.planRepository.observeTaskOrder().first() })
            rule.onNodeWithText(rule.activity.getString(R.string.discard_plan_draft)).performClick(); gone("accept-plan-draft")
            assertNull(runBlocking { container.planRepository.observeDraft().first() })
            assertEquals(originalPlan, runBlocking { container.planRepository.observeCurrentPlan().first() })
        } finally {
            runBlocking { if (container.planRepository.observeDraft().first()?.orderedTaskIds?.contains(id) == true) container.planRepository.saveDraft(null) }
            cleanupTask(id)
        }
    }

    @Test fun failed_legacy_workspace_read_keeps_quick_input_without_overwriting_persisted_context() {
        val original = runBlocking { container.planRepository.observeAssistantWorkspace().first() }
        val originalPlan = runBlocking { container.planRepository.observeCurrentPlan().first() }
        val originalTasks = runBlocking { container.taskRepository.observeTasks().first() }
        try {
            val legacy = AssistantWorkspace(LocalDate.now(), prompt = "原助手文字")
            runBlocking { container.planRepository.saveAssistantWorkspace(legacy) }
            val (fault, vm) = planFault()
            rule.openScheduleAssistant(); waitTag("autonomous-input")
            // The one-action composer is independent of the removed multi-tool workspace.
            rule.onNodeWithTag("autonomous-input").assert(
                SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")),
            )
            fill("autonomous-input", "成功读取后输入")
            assertEquals(legacy, runBlocking { container.planRepository.observeAssistantWorkspace().first() })
            fault.failedPart.value = "assistant"; rule.waitUntil(10_000) { vm.workspaceUiState.value.readError != null }
            fill("autonomous-input", "读取失败时保留的新输入")
            rule.onNodeWithTag("autonomous-submit").performScrollTo().assertIsNotEnabled()
            rule.onNodeWithTag("autonomous-error").performScrollTo().assertExists()
            assertEquals(legacy, runBlocking { container.planRepository.observeAssistantWorkspace().first() })
            rule.activityRule.scenario.recreate(); waitTag("autonomous-input")
            rule.onNodeWithTag("autonomous-input").performScrollTo().assertTextContains("读取失败时保留的新输入")
            rule.onNodeWithTag("autonomous-submit").performScrollTo().assertIsNotEnabled()
            rule.onNodeWithTag("assistant-close").performClick(); gone("assistant-sheet")
            fault.failedPart.value = null; rule.onNodeWithTag("retry-data-read").performClick()
            rule.waitUntil(10_000) { vm.uiState.value.isTrusted && vm.workspaceUiState.value.isTrusted }
            rule.openScheduleAssistant()
            rule.onNodeWithTag("autonomous-input").performScrollTo().assertTextContains("读取失败时保留的新输入")
            rule.onNodeWithTag("autonomous-submit").performScrollTo().assertIsEnabled()
            rule.onNodeWithTag("autonomous-error").assertDoesNotExist()
            assertEquals(legacy, runBlocking { container.planRepository.observeAssistantWorkspace().first() })
            assertEquals(originalPlan, runBlocking { container.planRepository.observeCurrentPlan().first() })
            assertEquals(originalTasks, runBlocking { container.taskRepository.observeTasks().first() })
            rule.onNodeWithTag("assistant-close").performClick()
        } finally { runBlocking { container.planRepository.saveAssistantWorkspace(original) } }
    }

    @Test fun preference_input_read_and_write_failures_recreation_and_real_receipt_keep_current_plan() {
        val original = runBlocking { container.categoryPreferenceRepository.observe().first() }
        val originalPlan = runBlocking { container.planRepository.observeCurrentPlan().first() }
        try {
            val (fault, vm) = preferenceFault()
            rule.openMineSettings()
            rule.onNodeWithText("类别偏好").performScrollTo().performClick(); waitTag("category-weight-COURSE")
            fill("category-weight-COURSE", "-30")
            rule.onNodeWithTag("category-preferences-save").performScrollTo().performClick()
            rule.onNodeWithTag("category-weight-COURSE").performScrollTo().assertTextContains("-30")
            assertEquals(original, runBlocking { container.categoryPreferenceRepository.observe().first() }); assertEquals(0, fault.commits.get())
            fill("category-weight-COURSE", "67")
            fault.failed.value = true; rule.waitUntil(10_000) { vm.uiState.value.readError != null }
            rule.onNodeWithTag("category-preferences-save").performScrollTo().assertIsNotEnabled()
            rule.activityRule.scenario.recreate(); waitTag("category-weight-COURSE")
            rule.onNodeWithTag("category-weight-COURSE").performScrollTo().assertTextContains("67")
            fault.failed.value = false; rule.onNodeWithTag("retry-data-read").performClick()
            rule.waitUntil(10_000) { vm.uiState.value.isTrusted }
            fault.failWrite = true
            rule.onNodeWithTag("category-preferences-save").performScrollTo().performClick()
            rule.waitUntil(10_000) { vm.uiState.value.saveError != null && !vm.uiState.value.isSaving }
            rule.onNodeWithTag("category-preferences-error").performScrollTo().assertIsDisplayed(); screenshot("qa-context24-preference-save-failure.png")
            assertEquals(original, runBlocking { container.categoryPreferenceRepository.observe().first() })
            rule.activityRule.scenario.recreate(); waitTag("category-weight-COURSE")
            rule.onNodeWithTag("category-weight-COURSE").performScrollTo().assertTextContains("67")
            fault.failWrite = false; fault.gate = CompletableDeferred()
            rule.onNodeWithTag("category-preferences-save").performScrollTo().performClick()
            rule.waitUntil(10_000) { vm.uiState.value.isSaving }
            rule.onNodeWithTag("category-preferences-save").assertIsNotEnabled().performClick()
            assertEquals(0, fault.commits.get()); rule.activityRule.scenario.recreate(); waitTag("category-preferences-save")
            rule.onNodeWithTag("category-preferences-save").assertIsNotEnabled()
            fault.gate!!.complete(Unit)
            rule.waitUntil(10_000) { vm.uiState.value.saveReceipt != null && !vm.uiState.value.isSaving }
            rule.onNodeWithTag("category-preferences-receipt").performScrollTo().assertIsDisplayed()
            assertEquals(1, fault.commits.get())
            assertEquals(original + (TaskCategory.COURSE to 67), runBlocking { container.categoryPreferenceRepository.observe().first() })
            assertEquals(originalPlan, runBlocking { container.planRepository.observeCurrentPlan().first() })
            rule.onNodeWithTag("navigation-tasks").performClick()
        } finally { runBlocking { container.categoryPreferenceRepository.save(original) } }
    }

    @Test fun plan_segment_clock_keeps_unsaved_input_through_failed_read_and_recreation() {
        val id = UUID.randomUUID().toString(); val segmentId = UUID.randomUUID().toString()
        try {
            seedTask(id)
            val originalPlan = runBlocking { container.planRepository.observeCurrentPlan().first() }
            val segment = PlannedSegment(segmentId, id, LocalDate.now().plusDays(1), 600, 660)
            val draft = PlanDraft(LocalDateTime.now(), listOf(segment), emptyList(), emptyList(), listOf(id))
            runBlocking { container.planRepository.saveDraft(draft) }
            val (fault, vm) = planFault(); waitTag("draft-segment-summary-$segmentId")
            rule.onNodeWithTag("draft-segment-summary-$segmentId").performScrollTo().performClick(); waitTag("plan-segment-start")
            fill("plan-segment-start", "11:00"); fill("plan-segment-end", "12:00")
            fault.failedPart.value = "current"; rule.waitUntil(10_000) { vm.uiState.value.readError != null }
            rule.onNodeWithTag("plan-segment-save").assertIsNotEnabled()
            rule.activityRule.scenario.recreate(); waitTag("plan-segment-start")
            rule.onNodeWithTag("plan-segment-start").performScrollTo().assertTextContains("11:00")
            rule.onNodeWithTag("plan-segment-end").performScrollTo().assertTextContains("12:00")
            assertEquals(draft, runBlocking { container.planRepository.observeDraft().first() })
            fault.failedPart.value = null
            rule.onNode(hasTestTag("retry-data-read") and hasAnyAncestor(hasTestTag("plan-segment-editor"))).performScrollTo().performClick()
            rule.waitUntil(10_000) { vm.uiState.value.isTrusted && vm.workspaceUiState.value.isTrusted }
            rule.onNodeWithTag("plan-segment-save").assertIsEnabled().performClick(); gone("plan-segment-start")
            rule.waitUntil(10_000) { runBlocking { container.planRepository.observeDraft().first()?.segments?.single()?.startMinute == 660 } }
            assertEquals(originalPlan, runBlocking { container.planRepository.observeCurrentPlan().first() })
            rule.onNodeWithText(rule.activity.getString(R.string.discard_plan_draft)).performClick(); gone("accept-plan-draft")
        } finally {
            runBlocking { if (container.planRepository.observeDraft().first()?.orderedTaskIds?.contains(id) == true) container.planRepository.saveDraft(null) }
            cleanupTask(id)
        }
    }
}
