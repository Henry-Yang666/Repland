package com.swan1127.repland

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.swan1127.repland.data.room.NumericProfileCodec
import com.swan1127.repland.domain.model.TaskDraft
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Uses the application's database configuration, which forbids main-thread queries. */
class DataManagementWorkflowUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @org.junit.Before
    fun use_manual_planning_mode_for_legacy_workflow() {
        pauseAutomaticPlanningForLegacyWorkflow(rule.activity)
    }

    @Test fun confirmed_clear_from_mine_removes_data_and_shows_success() {
        check(rule.activity.packageName == "com.swan1127.repland.qa")
        val container = (rule.activity.application as ReplandApplication).appContainer
        runBlocking { container.taskRepository.save(TaskDraft(displayName = "QA-clear-main-thread")) }
        rule.openMineSettings()
        rule.onNodeWithText("本地数据").performScrollTo().performClick()
        rule.onNodeWithText("清除全部本地数据").performScrollTo().performClick()
        // Opening and cancelling the confirmation must preserve the seeded task.
        rule.onNodeWithText("取消").performClick()
        assertTrue(runBlocking { container.dataManagementRepository.snapshot().tasks.isNotEmpty() })
        rule.onNodeWithText("清除全部本地数据").performClick()
        rule.onNodeWithText("确认清除").performClick()
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText("全部本地数据已清除；已导出的文件不受影响。").fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithText("清除失败。原有数据已保留，请重试。").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("全部本地数据已清除；已导出的文件不受影响。").assertExists()
        val snapshot = runBlocking { container.dataManagementRepository.snapshot() }
        assertTrue(snapshot.tasks.isEmpty())
        assertTrue(snapshot.executionLogs.isEmpty())
        assertTrue(snapshot.planHistory.isEmpty())
        assertTrue(snapshot.weeklyTimeBlocks.isEmpty())
        assertTrue(snapshot.dateOverrides.isEmpty())
        assertTrue(snapshot.profileEvidence.isEmpty())
        assertTrue(snapshot.executionSessions.isEmpty())
        // The live profile observer can recreate an empty derived cache after clearing.
        // User inputs, descriptions, sources and calculated values must still be gone.
        assertTrue(snapshot.numericProfileRecords.keys.all { it == "numeric-snapshot" })
        snapshot.numericProfileRecords["numeric-snapshot"]?.let { payload ->
            val profile = NumericProfileCodec.snapshot(payload)
            assertTrue(profile.sources.isEmpty())
            assertTrue(profile.parameters.all { it.value == null && it.count == 0 && it.sources.isEmpty() })
        }
    }
}
