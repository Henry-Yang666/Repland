package com.swan1127.repland

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import kotlinx.coroutines.runBlocking

/** Legacy manual-workflow assertions explicitly exercise the app with daily delegation paused. */
internal fun pauseAutomaticPlanningForLegacyWorkflow(activity: MainActivity) = runBlocking {
    (activity.application as ReplandApplication).appContainer.autonomyRepository.setEnabled(false)
}

/** Open the only assistant through its public floating entry on Schedule. */
internal fun ComposeContentTestRule.openScheduleAssistant() {
    waitUntil(10_000) { onAllNodesWithTag("navigation-today").fetchSemanticsNodes().isNotEmpty() }
    onNodeWithTag("navigation-today").performClick()
    waitUntil(10_000) { onAllNodesWithTag("schedule-assistant-fab").fetchSemanticsNodes().isNotEmpty() }
    onNodeWithTag("schedule-assistant-fab").performClick()
    waitUntil(10_000) { onAllNodesWithTag("autonomous-input").fetchSemanticsNodes().isNotEmpty() }
}

/** Settings are reached through the profile page, never through hidden UI shortcuts. */
internal fun ComposeContentTestRule.openMineSettings() {
    waitUntil(10_000) { onAllNodesWithTag("navigation-mine").fetchSemanticsNodes().isNotEmpty() }
    onNodeWithTag("navigation-mine").performClick()
    waitUntil(10_000) { onAllNodesWithTag("mine-settings").fetchSemanticsNodes().isNotEmpty() }
    onNodeWithTag("mine-settings").performScrollTo().performClick()
    waitUntil(10_000) { onAllNodesWithTag("mine-settings-page").fetchSemanticsNodes().isNotEmpty() }
}
