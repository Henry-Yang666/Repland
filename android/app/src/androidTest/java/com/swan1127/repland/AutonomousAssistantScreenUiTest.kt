package com.swan1127.repland

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swan1127.repland.ui.agent.AutonomousAssistantScreen
import com.swan1127.repland.ui.agent.AutonomyUiState
import com.swan1127.repland.ui.theme.ReplandTheme
import com.swan1127.repland.domain.model.Task
import com.swan1127.repland.domain.model.TaskCategory
import com.swan1127.repland.domain.model.TaskPriority
import com.swan1127.repland.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutonomousAssistantScreenUiTest {
    @get:Rule val rule = createComposeRule()

    private fun show(state: () -> AutonomyUiState, tasks: () -> List<Task> = { emptyList() },
                     onOpenTask: (String) -> Unit = {}, onSubmit: (String) -> Unit) {
        rule.setContent { ReplandTheme { AutonomousAssistantScreen(
            state = state(), tasks = tasks(), onSubmit = onSubmit,
            onUndo = {}, onOpenTask = onOpenTask,
        ) } }
    }

    private fun reach(tag: String) {
        rule.onNodeWithTag(tag).performScrollTo()
    }

    private fun assertInput(value: String) {
        rule.onNodeWithTag("autonomous-input").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(value)),
        )
    }

    @Test fun submit_latches_against_duplicate_taps_without_extra_shortcut_controls() {
        val sent = mutableListOf<String>()
        show({ AutonomyUiState(ready = true) }) { sent += it }
        reach("autonomous-input")
        rule.onNodeWithTag("autonomous-input").performTextInput("安排今天")
        assertEquals(emptyList<String>(), sent)
        assertInput("安排今天")
        rule.onNodeWithTag("autonomous-prompt-arrange").assertDoesNotExist()
        rule.onNodeWithTag("autonomy-enabled").assertDoesNotExist()
        reach("autonomous-submit")
        rule.onNodeWithTag("autonomous-submit").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("autonomous-submit").assertIsNotEnabled().performClick()
        assertEquals(listOf("安排今天"), sent)
    }

    @Test fun busy_state_blocks_writes_and_does_not_replace_the_user_input() {
        val state = mutableStateOf(AutonomyUiState(ready = true))
        val sent = mutableListOf<String>()
        show({ state.value }) { sent += it }
        reach("autonomous-input")
        rule.onNodeWithTag("autonomous-input").performTextInput("复习物理 45 分钟")
        rule.runOnIdle { state.value = state.value.copy(busy = true) }
        rule.onNodeWithTag("autonomous-input").assertIsNotEnabled()
        assertInput("复习物理 45 分钟")
        reach("autonomous-submit")
        rule.onNodeWithTag("autonomous-submit").assertIsNotEnabled().performClick()
        assertEquals(emptyList<String>(), sent)
    }

    @Test fun failed_submission_preserves_text_for_correction_and_retry() {
        val state = mutableStateOf(AutonomyUiState(ready = true))
        show({ state.value }) {}
        reach("autonomous-input")
        rule.onNodeWithTag("autonomous-input").performTextInput("物理复习完成了一半")
        reach("autonomous-submit")
        rule.onNodeWithTag("autonomous-submit").performClick()
        rule.runOnIdle { state.value = state.value.copy(completedAttempts = 1, error = "有两项同名任务，请说明是哪一项。") }
        rule.waitForIdle()
        rule.onNodeWithTag("autonomous-input").assertIsEnabled()
        assertInput("物理复习完成了一半")
        reach("autonomous-submit")
        rule.onNodeWithTag("autonomous-submit").assertIsEnabled().performClick()
        // An identical failure message still acknowledges the retry through its attempt number.
        rule.runOnIdle { state.value = state.value.copy(completedAttempts = 2) }
        rule.waitForIdle()
        rule.onNodeWithTag("autonomous-submit").assertIsEnabled()
        assertInput("物理复习完成了一半")
    }

    @Test fun success_acknowledgement_clears_only_the_completed_submission() {
        val state = mutableStateOf(AutonomyUiState(ready = true))
        show({ state.value }) {}
        reach("autonomous-input")
        rule.onNodeWithTag("autonomous-input").performTextInput("明天复习物理 45 分钟")
        reach("autonomous-submit")
        rule.onNodeWithTag("autonomous-submit").performClick()
        rule.runOnIdle { state.value = state.value.copy(submission = 1, completedAttempts = 1, response = "已安排到明天下午。") }
        rule.waitForIdle()
        reach("autonomous-input")
        rule.onNodeWithTag("autonomous-input").assertIsEnabled()
        assertInput("")
        reach("autonomous-response")
        rule.onNodeWithTag("autonomous-response").assertTextEquals("已安排到明天下午。")
    }

    @Test fun returning_to_query_results_uses_live_progress_and_removes_closed_or_deleted_tasks() {
        val original = Task(
            id = "physics", description = "复习物理", displayName = "复习物理",
            category = TaskCategory.COURSE, userPriority = TaskPriority.MEDIUM,
            estimatedDays = null, totalDurationMinutes = 30, dueDate = null,
            status = TaskStatus.NOT_STARTED, completionSummary = null, actualDurationMinutes = null,
            progressPercent = null, postponeCount = 0, createdAtEpochMillis = 1, updatedAtEpochMillis = 1,
        )
        val liveTasks = mutableStateOf(listOf(original))
        val state = mutableStateOf(AutonomyUiState(ready = true, queryResults = listOf(original)))
        val opened = mutableListOf<String>()
        // The query snapshot deliberately stays unchanged while details mutate the live task flow.
        show({ state.value }, tasks = { liveTasks.value }, onOpenTask = { opened += it }) {}
        reach("autonomous-task-physics")
        rule.onNodeWithTag("autonomous-task-physics").assertTextContains("待开始 · 预计 30 分钟")
        rule.runOnIdle { state.value = state.value.copy(busy = true) }
        rule.onNodeWithTag("autonomous-task-physics").assertIsNotEnabled().performClick()
        assertEquals(emptyList<String>(), opened)
        rule.runOnIdle { state.value = state.value.copy(busy = false) }
        rule.onNodeWithTag("autonomous-task-physics").assertIsEnabled().performClick()
        assertEquals(listOf("physics"), opened)
        rule.runOnIdle {
            liveTasks.value = listOf(original.copy(status = TaskStatus.IN_PROGRESS, progressPercent = 50, updatedAtEpochMillis = 2))
        }
        rule.onNodeWithTag("autonomous-task-physics").assertTextContains("进行中 · 预计 30 分钟 · 进度 50%")
        rule.runOnIdle { liveTasks.value = listOf(original.copy(status = TaskStatus.COMPLETED, progressPercent = 100)) }
        rule.onNodeWithTag("autonomous-task-physics").assertDoesNotExist()
        rule.runOnIdle { liveTasks.value = listOf(original) }
        reach("autonomous-task-physics")
        rule.onNodeWithTag("autonomous-task-physics").assertExists()
        rule.runOnIdle { liveTasks.value = emptyList() }
        rule.onNodeWithTag("autonomous-task-physics").assertDoesNotExist()
    }
}
