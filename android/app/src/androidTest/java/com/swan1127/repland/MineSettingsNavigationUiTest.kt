package com.swan1127.repland

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Profile is the My landing page; all configuration lives behind its Settings entry. */
@RunWith(AndroidJUnit4::class)
class MineSettingsNavigationUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Before fun pause_automatic_maintenance() {
        check(rule.activity.packageName == "com.swan1127.repland.qa")
        pauseAutomaticPlanningForLegacyWorkflow(rule.activity)
    }

    private fun waitTag(tag: String) = rule.waitUntil(10_000) {
        rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }

    private fun openProfile() {
        waitTag("navigation-mine")
        rule.onNodeWithTag("navigation-mine").performClick()
        waitTag("mine-profile-page")
    }

    @Test fun my_landing_page_keeps_personal_profile_and_portrait_without_settings_content() {
        openProfile()
        for (tag in listOf("personal-profile-card", "personal-profile-avatar", "personal-profile-name")) {
            rule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        }
        waitTag("usage-summary-card")
        rule.onNodeWithTag("usage-summary-card").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("usage-summary-text").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("mine-settings").performScrollTo().assertIsDisplayed()

        for (tag in listOf("mine-settings-page", "mine-time-settings", "mine-plan-history", "app-version",
            "autonomy-enabled-switch", "local-reminders-switch", "ai-advisor-switch", "ai-provider-settings",
            "numeric-profile-open", "numeric-generate")) {
            rule.onNodeWithTag(tag).assertDoesNotExist()
        }
        for (text in listOf("类别偏好", "本地数据", "关于你的记录", "生成本地画像证据")) {
            rule.onNodeWithText(text).assertDoesNotExist()
        }
    }

    @Test fun settings_recreation_native_back_and_my_tab_return_to_profile() {
        rule.openMineSettings()
        rule.onNodeWithTag("mine-profile-page").assertDoesNotExist()
        rule.onNodeWithTag("personal-profile-card").assertDoesNotExist()
        for (tag in listOf("mine-time-settings", "mine-plan-history", "local-reminders-switch", "ai-advisor-switch")) {
            rule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        }
        rule.onNodeWithText("类别偏好").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("本地数据").performScrollTo().assertIsDisplayed()

        rule.activityRule.scenario.recreate()
        waitTag("mine-settings-page")
        rule.onNodeWithTag("settings-back").assertIsDisplayed()
        NativeSheetTestInput.back(rule, "设置")
        waitTag("mine-profile-page")
        rule.onNodeWithTag("mine-settings-page").assertDoesNotExist()

        rule.openMineSettings()
        rule.onNodeWithTag("navigation-mine").performClick()
        waitTag("mine-profile-page")
        rule.onNodeWithTag("mine-settings-page").assertDoesNotExist()
        rule.onNodeWithTag("personal-profile-card").assertExists()
    }

    @Test fun time_settings_nested_navigation_returns_to_settings_before_profile() {
        rule.openMineSettings()
        rule.onNodeWithTag("mine-time-settings").performScrollTo().performClick()
        waitTag("time-add-weekly")
        rule.activityRule.scenario.recreate()
        waitTag("time-add-weekly")
        NativeSheetTestInput.back(rule, "时间设置")
        waitTag("mine-settings-page")
        rule.onNodeWithTag("mine-profile-page").assertDoesNotExist()
        rule.onNodeWithTag("mine-time-settings").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("settings-back").performClick()
        waitTag("mine-profile-page")
        rule.onNodeWithTag("mine-settings-page").assertDoesNotExist()
        rule.onNodeWithTag("personal-profile-card").assertExists()
    }

    @Test fun usage_summary_evidence_recreation_and_settings_return_do_not_change_tasks_or_plan() {
        val app = (rule.activity.application as ReplandApplication).appContainer
        val tasksBefore = runBlocking { app.taskRepository.observeTasks().first() }
        val planBefore = runBlocking { app.planRepository.observeCurrentPlan().first() }
        val logsBefore = runBlocking { tasksBefore.associate { it.id to app.taskRepository.observeExecutionLogs(it.id).first() } }
        openProfile()
        waitTag("usage-summary-text")
        rule.onNodeWithTag("usage-summary-text").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("usage-summary-sources").performScrollTo().performClick()
        waitTag("usage-summary-basis")
        rule.activityRule.scenario.recreate()
        waitTag("usage-summary-basis")
        rule.onNodeWithTag("usage-summary-close").performClick()
        rule.onNodeWithTag("usage-summary-basis").assertDoesNotExist()

        rule.openMineSettings()
        rule.onNodeWithTag("settings-back").performClick()
        waitTag("mine-profile-page")
        rule.onNodeWithTag("usage-summary-text").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("usage-summary-updated-at").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("numeric-profile-open").assertDoesNotExist()
        rule.onNodeWithTag("numeric-generate").assertDoesNotExist()
        rule.onNodeWithText("关于你的记录").assertDoesNotExist()

        assertEquals(tasksBefore, runBlocking { app.taskRepository.observeTasks().first() })
        assertEquals(planBefore, runBlocking { app.planRepository.observeCurrentPlan().first() })
        assertEquals(logsBefore, runBlocking { tasksBefore.associate { it.id to app.taskRepository.observeExecutionLogs(it.id).first() } })
    }
}
