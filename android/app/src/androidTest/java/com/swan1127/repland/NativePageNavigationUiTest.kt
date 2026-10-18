package com.swan1127.repland

import android.database.sqlite.SQLiteDatabase
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.swan1127.repland.domain.model.*
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class NativePageNavigationUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @org.junit.Before
    fun use_manual_planning_mode_for_legacy_workflow() {
        pauseAutomaticPlanningForLegacyWorkflow(rule.activity)
    }
    private fun waitTag(tag: String) = rule.waitUntil(10_000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    @Test fun native_back_returns_to_settings_after_time_page_recreation() {
        rule.openMineSettings()
        waitTag("mine-time-settings"); rule.onNodeWithTag("mine-time-settings").performScrollTo().performClick()
        waitTag("time-add-weekly"); rule.activityRule.scenario.recreate(); waitTag("time-add-weekly")
        NativeSheetTestInput.back(rule, "时间设置")
        waitTag("mine-settings-page"); rule.onNodeWithTag("mine-time-settings").assertExists()
        rule.onNodeWithTag("time-add-weekly").assertDoesNotExist()
    }

    @Test fun native_back_from_time_edit_sheet_does_not_pop_the_time_page() {
        rule.openMineSettings()
        waitTag("mine-time-settings"); rule.onNodeWithTag("mine-time-settings").performScrollTo().performClick()
        waitTag("time-add-weekly"); rule.onNodeWithTag("time-add-weekly").performScrollTo().performClick()
        waitTag("weekly-block-title"); rule.onNodeWithTag("weekly-block-title").performTextReplacement("未提交导航样例")
        rule.activityRule.scenario.recreate(); waitTag("weekly-block-title")
        NativeSheetTestInput.back(rule, "添加固定时间")
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("weekly-block-title").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithTag("time-add-weekly").assertExists()
        NativeSheetTestInput.back(rule, "时间设置")
        waitTag("mine-settings-page"); rule.onNodeWithTag("mine-time-settings").assertExists()
        rule.onNodeWithTag("time-add-weekly").assertDoesNotExist()
    }

    @Test fun native_back_first_hides_assistant_keyboard_then_closes_sheet_to_schedule() {
        val previous = NativeSheetTestInput.shell("settings get secure show_ime_with_hard_keyboard")
        val draft = "返回后保留的助手草稿 ${UUID.randomUUID()}"
        try {
            NativeSheetTestInput.shell("settings put secure show_ime_with_hard_keyboard 1")
            rule.openScheduleAssistant()
            NativeSheetTestInput.awaitFocusedDialog(rule, "一句话处理")
            rule.onNodeWithTag("autonomous-input").performScrollTo().performTextReplacement(draft)
            rule.onNodeWithTag("autonomous-input").performClick().assertIsFocused()
            rule.waitUntil(10_000) {
                NativeSheetTestInput.shell("dumpsys input_method").contains("mWindowVisible=true")
            }

            NativeSheetTestInput.shell("input keyevent 4")
            rule.waitUntil(10_000) {
                !NativeSheetTestInput.shell("dumpsys input_method").contains("mWindowVisible=true")
            }
            rule.onNodeWithTag("assistant-sheet").assertExists()
            rule.onNodeWithTag("autonomous-input").assertTextContains(draft, substring = false)

            NativeSheetTestInput.back(rule, "一句话处理")
            rule.waitUntil(10_000) {
                rule.onAllNodesWithTag("assistant-sheet").fetchSemanticsNodes().isEmpty()
            }
            waitTag("today-page-scroll")
            rule.onNodeWithTag("schedule-assistant-fab").assertIsDisplayed()
            rule.onNodeWithTag("navigation-agent").assertDoesNotExist()

            rule.openScheduleAssistant()
            rule.onNodeWithTag("autonomous-input").performScrollTo().assertTextContains(draft, substring = false)
        } finally {
            NativeSheetTestInput.shell(if (previous == "null") "settings delete secure show_ime_with_hard_keyboard"
                else "settings put secure show_ime_with_hard_keyboard $previous")
        }
    }

    @Test fun native_back_from_recreated_task_detail_returns_to_task_list() {
        check(rule.activity.packageName == "com.swan1127.repland.qa")
        val repo = (rule.activity.application as ReplandApplication).appContainer.taskRepository
        val id = UUID.randomUUID().toString(); val name = "QA-native22-$id"
        try {
            runBlocking { repo.save(TaskDraft(id = id, displayName = name, description = "导航样例", category = TaskCategory.COURSE,
                userPriority = TaskPriority.MEDIUM, estimatedDays = 1, totalDurationMinutes = 15, dueDate = null, scheduledForDate = LocalDate.now())) }
            waitTag("navigation-tasks"); rule.onNodeWithTag("navigation-tasks").performClick(); waitTag("task-groups-scroll")
            expandTodayTaskGroup(rule)
            rule.onNodeWithTag("task-groups-scroll").performScrollToNode(hasTestTag("task-card-$name"))
            rule.onNodeWithTag("task-card-$name").performClick(); waitTag("task-detail-scroll")
            rule.activityRule.scenario.recreate(); waitTag("task-detail-scroll")
            NativeSheetTestInput.back(rule, name)
            waitTag("task-groups-scroll"); rule.onNodeWithTag("task-detail-scroll").assertDoesNotExist()
        } finally {
            SQLiteDatabase.openDatabase(rule.activity.getDatabasePath("repland.db").absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use {
                it.execSQL("DELETE FROM tasks WHERE id = ?", arrayOf(id))
            }
        }
    }
}
