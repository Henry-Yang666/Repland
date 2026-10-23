package com.swan1127.repland

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.swan1127.repland.domain.model.TaskCaptureDraft
import com.swan1127.repland.domain.model.TaskDraft
import com.swan1127.repland.ui.components.TaskCaptureSheet
import com.swan1127.repland.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class DesignMotionUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun rapid_page_switch_keeps_latest_destination_and_accepts_input_during_transition() {
        var actions = 0
        rule.mainClock.autoAdvance = false
        rule.setContent { ReplandTheme {
            var page by remember { mutableIntStateOf(0) }
            Column {
                TextButton(onClick = { page = 1 }) { Text("去任务") }
                TextButton(onClick = { page = 2 }) { Text("去助手") }
                MotionPage(page) {
                    TextButton(onClick = { actions++ }) { Text("页面 $page 操作") }
                }
            }
        } }
        rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithText("去任务").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("去助手").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("页面 2 操作").performClick()
        rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithText("页面 2 操作").assertIsDisplayed()
        rule.onNodeWithText("页面 1 操作").assertDoesNotExist()
        rule.onNodeWithText("页面 0 操作").assertDoesNotExist()
        rule.runOnIdle { assertEquals(1, actions) }
    }

    @Test fun disclosure_can_reverse_before_enter_finishes_without_duplicate_controls() {
        var actions = 0
        rule.mainClock.autoAdvance = false
        rule.setContent { ReplandTheme {
            var open by remember { mutableStateOf(false) }
            Column {
                TextButton(onClick = { open = !open }) { Text("切换展开") }
                MotionDisclosure(open) {
                    TextButton(onClick = { actions++ }) { Text("面板操作") }
                }
            }
        } }
        rule.onNodeWithText("切换展开").performClick()
        rule.mainClock.advanceTimeBy(64)
        rule.onNodeWithText("切换展开").performClick()
        rule.mainClock.advanceTimeBy(32)
        rule.onNodeWithText("切换展开").performClick()
        rule.mainClock.advanceTimeBy(400)
        rule.onAllNodesWithText("面板操作").assertCountEquals(1)
        rule.onNodeWithText("面板操作").assertIsDisplayed().performClick()
        rule.runOnIdle { assertEquals(1, actions) }
    }

    @Test fun system_animation_setting_updates_without_activity_restart() {
        val original = NativeSheetTestInput.shell("settings get global animator_duration_scale")
        try {
            NativeSheetTestInput.shell("settings put global animator_duration_scale 1")
            rule.setContent { ReplandTheme {
                Text(if (LocalReducedMotion.current) "静态反馈" else "动态反馈")
            } }
            rule.onNodeWithText("动态反馈").assertIsDisplayed()
            NativeSheetTestInput.shell("settings put global animator_duration_scale 0")
            rule.waitUntil(5000) { rule.onAllNodesWithText("静态反馈").fetchSemanticsNodes().size == 1 }
            NativeSheetTestInput.shell("settings put global animator_duration_scale 1")
            rule.waitUntil(5000) { rule.onAllNodesWithText("动态反馈").fetchSemanticsNodes().size == 1 }
        } finally {
            NativeSheetTestInput.shell(if (original == "null") "settings delete global animator_duration_scale"
                else "settings put global animator_duration_scale $original")
        }
    }

    @Test fun capture_step_round_trip_preserves_draft_and_commits_once() {
        var saved: TaskDraft? = null
        var saves = 0
        val due = LocalDate.now().plusDays(7)
        rule.setContent { ReplandTheme {
            var draft by remember { mutableStateOf(TaskCaptureDraft(text = "动效草稿", dueDate = due, duration = 30)) }
            TaskCaptureSheet("", {}, { saved = it; saves++ }, draft = draft, onDraftChange = { draft = it })
        } }
        rule.onNodeWithTag("task-capture-arrange-today").performClick()
        rule.onNodeWithTag("task-capture-choose-duration").performScrollTo().performClick()
        rule.onNodeWithTag("task-capture-back").performClick()
        rule.onNodeWithTag("task-capture-back").performClick()
        rule.onNodeWithTag("task-capture-input").assertTextContains("动效草稿")
        rule.onNodeWithTag("task-capture-save-inbox").performClick()
        rule.runOnIdle {
            assertEquals(1, saves)
            assertEquals(due, saved!!.dueDate)
            assertEquals(30, saved!!.totalDurationMinutes)
        }
    }
}
