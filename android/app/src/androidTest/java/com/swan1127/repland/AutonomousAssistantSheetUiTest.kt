package com.swan1127.repland

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.swan1127.repland.ui.agent.AutonomousAssistantSheet
import com.swan1127.repland.ui.theme.ReplandTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AutonomousAssistantSheetUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun execution_blocks_close_and_native_back_until_the_operation_finishes() {
        val busy = mutableStateOf(true)
        val open = mutableStateOf(true)
        var dismissals = 0
        rule.setContent {
            ReplandTheme {
                if (open.value) AutonomousAssistantSheet(busy = busy.value,
                    onDismiss = { dismissals++; open.value = false }) { Text("正在处理事项") }
            }
        }
        rule.onNodeWithTag("assistant-close").assertIsNotEnabled().performClick()
        NativeSheetTestInput.back(rule, "一句话处理")
        rule.waitForIdle()
        rule.onNodeWithTag("assistant-sheet").assertExists()
        assertEquals(0, dismissals)
        rule.runOnIdle { busy.value = false }
        rule.onNodeWithTag("assistant-close").assertIsEnabled().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("assistant-sheet").fetchSemanticsNodes().isEmpty() }
        assertEquals(1, dismissals)
    }
}
