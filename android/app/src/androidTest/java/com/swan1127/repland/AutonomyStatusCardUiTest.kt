package com.swan1127.repland

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swan1127.repland.ui.components.AutonomyStatusCard
import com.swan1127.repland.ui.schedule.ScheduleCreateCard
import com.swan1127.repland.ui.theme.LocalReducedMotion
import com.swan1127.repland.ui.theme.ReplandTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutonomyStatusCardUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun switch_requests_change_without_inventing_a_saved_state() {
        val changes = mutableListOf<Boolean>()
        rule.setContent { ReplandTheme { AutonomyStatusCard(
            enabled = true, isBusy = false, summary = null, error = null, canUndo = false,
            onEnabledChange = { changes += it }, onUndo = {},
        ) } }
        rule.onNodeWithTag("autonomy-enabled-switch").performClick().assertIsOn()
        assertEquals(listOf(false), changes)
        rule.onNodeWithTag("autonomy-receipt").assertDoesNotExist()
        rule.onNodeWithTag("autonomy-undo").assertDoesNotExist()
    }

    @Test fun busy_receipt_prevents_duplicate_undo_and_allows_pausing() {
        var undoCalls = 0
        val changes = mutableListOf<Boolean>()
        rule.setContent { ReplandTheme { AutonomyStatusCard(
            enabled = true, isBusy = true, summary = "已把英语复习移到晚饭后。", error = null,
            canUndo = true, onEnabledChange = { changes += it }, onUndo = { undoCalls++ },
        ) } }
        rule.onNodeWithTag("autonomy-undo").assertIsNotEnabled().performClick()
        rule.onNodeWithTag("autonomy-enabled-switch").performClick()
        assertEquals(0, undoCalls)
        assertEquals(listOf(false), changes)
    }

    @Test fun large_text_dark_mode_keeps_errors_and_receipt_reachable() {
        val error = "课程与固定安排冲突，已保留原计划。请调整可用时间后再试。"
        val receipt = "已安排物理报告，并保留了晚间休息与明天的课程。"
        var undoCalls = 0
        rule.setContent { ReplandTheme(darkTheme = true) {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, 2f),
                LocalReducedMotion provides true,
            ) {
                Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                    AutonomyStatusCard(
                        enabled = true, isBusy = false, summary = receipt, error = error, canUndo = true,
                        onEnabledChange = {}, onUndo = { undoCalls++ },
                    )
                }
            }
        } }
        rule.onNodeWithTag("autonomy-error").performScrollTo().assertIsDisplayed().assertTextEquals(error)
        rule.onNodeWithTag("autonomy-receipt").performScrollTo().assertIsDisplayed().assertTextEquals(receipt)
        rule.onNodeWithTag("autonomy-undo").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(1, undoCalls)
    }

    @Test fun compact_capture_entry_opens_once_from_its_full_touch_surface() {
        var openings = 0
        rule.setContent { ReplandTheme { ScheduleCreateCard(onAdd = { openings++ }) } }
        rule.onNodeWithTag("schedule-create-task").assertIsDisplayed().performClick()
        assertEquals(1, openings)
    }
}
