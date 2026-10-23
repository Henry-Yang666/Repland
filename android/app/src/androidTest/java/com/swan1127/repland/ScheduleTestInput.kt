package com.swan1127.repland

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.semantics.SemanticsActions
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Bring a month cell above the floating entry before sending a real visible click. */
internal fun ComposeTestRule.clickScheduleDate(date: LocalDate) {
    val dateTag = "month-date-$date"
    val list = onNodeWithTag("today-page-scroll")
    list.performScrollToNode(hasTestTag(dateTag))
    var attempts = 0
    while (true) {
        waitForIdle()
        val cell = onNodeWithTag(dateTag).fetchSemanticsNode().boundsInRoot
        val fab = onAllNodesWithTag("schedule-assistant-fab").fetchSemanticsNodes().singleOrNull()?.boundsInRoot
        val overlapped = fab != null && cell.left < fab.right && cell.right > fab.left &&
            cell.top < fab.bottom && cell.bottom > fab.top
        if (!overlapped) break
        check(attempts++ < 4) { "Calendar date $date is still covered by the assistant: cell=$cell, fab=$fab" }
        // Positive scrolling moves the calendar upward; leave a physical touch margin.
        list.performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy ->
            scrollBy(0f, cell.bottom - requireNotNull(fab).top + 16f)
        }
    }
    onNodeWithTag(dateTag).assertIsDisplayed().performClick()
    waitForIdle()
    onNodeWithTag("schedule-selected-date").assertTextEquals(
        date.format(DateTimeFormatter.ofPattern("yyyy/MM/dd E", Locale.CHINA)))
}

/** Follow the visible mode switch before interacting with detailed timeline cards. */
internal fun ComposeTestRule.openDetailedSchedule() {
    onNodeWithTag("today-page-scroll").performScrollToNode(hasTestTag("schedule-detail-mode"))
    onNodeWithTag("schedule-detail-mode").performClick()
    waitForIdle()
}
