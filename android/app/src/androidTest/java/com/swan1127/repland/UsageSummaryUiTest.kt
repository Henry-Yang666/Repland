package com.swan1127.repland

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swan1127.repland.domain.model.UsageSummaryDimension
import com.swan1127.repland.domain.model.UsageSummaryFact
import com.swan1127.repland.ui.profile.UsageSummaryCard
import com.swan1127.repland.ui.profile.UsageSummaryUiState
import com.swan1127.repland.ui.theme.LocalReducedMotion
import com.swan1127.repland.ui.theme.ReplandTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The profile is an automatically maintained readout, with optional read-only evidence. */
@RunWith(AndroidJUnit4::class)
class UsageSummaryUiTest {
    @get:Rule val rule = createComposeRule()

    private fun show(state: () -> UsageSummaryUiState) {
        rule.setContent { ReplandTheme {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                UsageSummaryCard(state())
            }
        } }
    }

    private fun assertNoManualProfileActions() {
        for (tag in listOf("numeric-profile-open", "numeric-generate", "numeric-total-input",
            "numeric-confirm-total", "numeric-confirm-input", "numeric-enabled", "numeric-ai-consent")) {
            rule.onNodeWithTag(tag).assertDoesNotExist()
        }
        for (text in listOf("关于你的记录", "生成本地画像证据", "主动生成 / 刷新描述",
            "确认输入来源 / 补录累计投入", "确认补录累计投入", "我明确确认以上当前输入")) {
            rule.onNodeWithText(text).assertDoesNotExist()
        }
        rule.onAllNodes(hasSetTextAction()).assertCountEquals(0)
    }

    @Test fun automatic_summary_and_update_time_appear_without_generation_or_confirmation_actions() {
        show { UsageSummaryUiState(
            text = "最近你通常把任务拆成多次专注完成，傍晚的使用记录较多。",
            updatedAt = 1_792_252_800_000L,
            isAiSummary = true,
            loading = false,
        ) }
        rule.onNodeWithTag("usage-summary-card").assertExists()
        rule.onNodeWithTag("usage-summary-text").performScrollTo()
            .assertTextEquals("最近你通常把任务拆成多次专注完成，傍晚的使用记录较多。")
        rule.onNodeWithTag("usage-summary-updated-at").performScrollTo().assertIsDisplayed()
        assertNoManualProfileActions()
    }

    @Test fun evidence_is_optional_and_open_sheet_survives_saved_state_restoration() {
        val fact = UsageSummaryFact(
            id = "focus:rounds", dimension = UsageSummaryDimension.FOCUS,
            text = "最近完成 3 次专注，累计投入 75 分钟。", count = 3,
            sourceIds = listOf("session:1", "session:2", "session:3"),
        )
        val state = UsageSummaryUiState(
            text = "最近有三次专注记录。", updatedAt = 1_792_252_800_000L,
            basis = listOf(fact), windowStart = "2026-09-19", windowEnd = "2026-10-18",
            loading = false,
        )
        val restoration = StateRestorationTester(rule)
        restoration.setContent { ReplandTheme {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { UsageSummaryCard(state) }
        } }
        rule.onNodeWithTag("usage-summary-basis").assertDoesNotExist()
        rule.onNodeWithTag("usage-summary-sources").performScrollTo().performClick()
        rule.onNodeWithTag("usage-summary-basis").assertExists()
        rule.onNodeWithText(fact.text).performScrollTo().assertIsDisplayed()
        assertNoManualProfileActions()

        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("usage-summary-basis").assertExists()
        rule.onNodeWithText(fact.text).performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("usage-summary-close").performClick()
        rule.onNodeWithTag("usage-summary-basis").assertDoesNotExist()
        rule.onNodeWithTag("usage-summary-text").performScrollTo().assertTextEquals(state.text)
    }

    @Test fun no_usage_records_show_an_automatic_plain_language_fallback_without_forms() {
        show { UsageSummaryUiState(
            text = "开始使用日程和专注后，我会逐渐整理你的使用习惯。",
            updatedAt = 1_792_252_800_000L, basis = emptyList(), loading = false,
        ) }
        rule.onNodeWithTag("usage-summary-text").performScrollTo()
            .assertTextEquals("开始使用日程和专注后，我会逐渐整理你的使用习惯。")
        rule.onNodeWithTag("usage-summary-basis").assertDoesNotExist()
        rule.onNodeWithTag("usage-summary-sources").performScrollTo().performClick()
        rule.onNodeWithText("目前记录还不多。照常使用 App，有记录后会自动更新。")
            .performScrollTo().assertIsDisplayed()
        assertNoManualProfileActions()
    }

    @Test fun refreshing_or_failed_reads_keep_the_last_successful_summary_and_timestamp() {
        val state = mutableStateOf(UsageSummaryUiState(
            text = "上次总结：你最近有规律地记录任务进展。",
            updatedAt = 1_792_252_800_000L, loading = false,
        ))
        show { state.value }
        val expectedTime = rule.onNodeWithTag("usage-summary-updated-at").fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.Text]
        rule.runOnIdle { state.value = state.value.copy(updating = true, pendingChanges = true) }
        rule.onNodeWithTag("usage-summary-text").performScrollTo().assertTextEquals(state.value.text)
        rule.runOnIdle {
            state.value = state.value.copy(updating = false, readError = "暂时无法读取新记录，已保留上次总结。")
        }
        rule.onNodeWithTag("usage-summary-text").performScrollTo().assertTextEquals(state.value.text)
        rule.onNodeWithTag("usage-summary-updated-at").assertTextEquals(*expectedTime.map { it.text }.toTypedArray())
        rule.onNodeWithTag("usage-summary-status").assertTextEquals("暂时无法读取新记录，已保留上次总结。")
        assertNoManualProfileActions()
    }

    @Test fun large_dark_reduced_motion_keeps_summary_and_evidence_actions_reachable() {
        val fact = UsageSummaryFact("progress:1", UsageSummaryDimension.PROGRESS,
            "近期有 2 项任务完成记录。", 2, listOf("log:1", "log:2"))
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                ReplandTheme(darkTheme = true) {
                    CompositionLocalProvider(LocalReducedMotion provides true) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            UsageSummaryCard(UsageSummaryUiState(
                                text = "近期有任务完成记录，系统会继续随使用更新。", basis = listOf(fact),
                                updatedAt = 1_792_252_800_000L, loading = false,
                            ))
                        }
                    }
                }
            }
        }
        rule.onNodeWithTag("usage-summary-text").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("usage-summary-sources").performScrollTo().assertIsDisplayed().performClick()
        rule.onNodeWithText(fact.text).performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("usage-summary-close").assertIsDisplayed().performClick()
        rule.onNodeWithTag("usage-summary-basis").assertDoesNotExist()
    }
}
