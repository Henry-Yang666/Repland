package com.swan1127.repland

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.swan1127.repland.ui.agent.AutonomousAssistantSheet
import com.swan1127.repland.ui.theme.ReplandTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AutonomousAssistantSheetUiTest {
    @get:Rule val rule = createComposeRule()

    private fun awaitFocusedDialog() {
        rule.waitForIdle()
        // Espresso waits for the native dialog root to gain window focus. API 26's
        // accessibility active-window cache can still point at the launching Activity.
        onView(isRoot()).inRoot(isDialog()).check { view, error ->
            if (error != null) throw error
            assertTrue("The assistant dialog must receive native input", view.hasWindowFocus())
        }
    }

    @Test fun execution_blocks_close_and_native_back_until_the_operation_finishes() {
        val busy = mutableStateOf(false)
        val open = mutableStateOf(true)
        var dismissals = 0
        rule.setContent {
            ReplandTheme {
                if (open.value) AutonomousAssistantSheet(busy = busy.value,
                    onDismiss = { dismissals++; open.value = false }) {
                    Button(onClick = { busy.value = true }, enabled = !busy.value) {
                        Text(if (busy.value) "正在处理事项" else "开始处理")
                    }
                }
            }
        }
        // Execution begins after the native sheet has opened, as it does when
        // a user submits a request. Native Back must target that focused dialog.
        awaitFocusedDialog()
        rule.onNodeWithText("开始处理").assertIsDisplayed().performClick()
        rule.onNodeWithText("正在处理事项").assertIsDisplayed()
        rule.onNodeWithTag("assistant-close").assertIsNotEnabled().performClick()
        awaitFocusedDialog()
        Espresso.pressBack()
        rule.waitForIdle()
        rule.onNodeWithTag("assistant-sheet").assertExists()
        assertEquals(0, dismissals)
        rule.runOnIdle { busy.value = false }
        rule.onNodeWithTag("assistant-close").assertIsEnabled().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("assistant-sheet").fetchSemanticsNodes().isEmpty() }
        assertEquals(1, dismissals)
    }
}
