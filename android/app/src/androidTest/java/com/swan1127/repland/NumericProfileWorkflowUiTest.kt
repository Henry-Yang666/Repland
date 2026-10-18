package com.swan1127.repland

import androidx.compose.ui.test.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.swan1127.repland.data.room.*
import com.swan1127.repland.domain.model.*
import com.swan1127.repland.domain.ports.AiProviderConfigRepository
import com.swan1127.repland.ui.profile.NumericProfileSection
import com.swan1127.repland.ui.profile.NumericProfileViewModel
import com.swan1127.repland.ui.theme.ReplandTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

/** The retained numeric editor is tested in isolation; My exposes the automatic usage summary. */
class NumericProfileWorkflowUiTest {
    @get:Rule val rule=createComposeRule()
    private val store = ViewModelStore()
    private lateinit var db: ReplandDatabase
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private class LegacyFixture(db: ReplandDatabase) {
        val taskRepository = RoomTaskRepository(db)
        val planRepository = RoomPlanRepository(db)
        val numericProfileRepository = RoomNumericProfileRepository(db)
    }

    @org.junit.After fun close_fixture() {
        rule.runOnIdle { store.clear() }
        if (::db.isInitialized) db.close()
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        val bitmap=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        try { java.io.File(context.getExternalFilesDir(null),name).outputStream().use {
            check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it))
        } } finally { bitmap.recycle() }
    }
    private fun waitTag(tag: String)=rule.waitUntil(20000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun visible(tag: String): SemanticsNodeInteraction {
        waitTag(tag); return rule.onNodeWithTag(tag).also { runCatching { it.performScrollTo() } }.assertIsDisplayed()
    }
    @Test fun actual_source_exclusion_invalid_total_confirmation_advice_cancel_adoption_and_recreation_preserve_plan() {
        check(context.packageName=="com.swan1127.repland.qa")
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),ReplandDatabase::class.java).build()
        val app=LegacyFixture(db)
        val config=object : AiProviderConfigRepository {
            private val value=MutableStateFlow(AiProviderConfig())
            override fun observe()=value
            override suspend fun save(baseUrl:String,model:String,apiKey:String) { error("No remote configuration in component fixture") }
            override suspend fun clearApiKey() { error("No remote configuration in component fixture") }
            override suspend fun readSecret(): AiProviderSecret? = null
        }
        lateinit var vm: NumericProfileViewModel
        rule.runOnIdle {
            vm=NumericProfileViewModel(app.numericProfileRepository,app.taskRepository,
                RoomAiSettingsRepository(db.aiSettingsDao()),config,NumericProfileAdvisor { null },false)
            store.put("legacy-numeric-component",vm)
        }
        val restoration=StateRestorationTester(rule)
        restoration.setContent { ReplandTheme {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { NumericProfileSection(vm) }
        } }
        val prefix="000-000-numeric-${Long.MAX_VALUE-System.currentTimeMillis()}-${UUID.randomUUID()}"; val ids=(1..3).map { "$prefix-$it" }; val target="$prefix-target"
        runBlocking {
            app.numericProfileRepository.setEnabled(true); app.numericProfileRepository.setParameterEnabled("estimate:LEISURE",true)
            for(id in ids) {
                app.taskRepository.save(TaskDraft(id=id,displayName="画像样本${id.takeLast(1)}",category=TaskCategory.LEISURE,totalDurationMinutes=20))
                app.taskRepository.confirmStatus(id,TaskStatus.IN_PROGRESS); app.taskRepository.confirmStatus(id,TaskStatus.COMPLETED)
                val task=app.taskRepository.observeTasks().first().single { it.id==id }
                app.numericProfileRepository.confirmActualTotal(id,30,RoomNumericProfileRepository.taskRevision(task,app.taskRepository.observeExecutionLogs(id).first()))
            }
            app.taskRepository.save(TaskDraft(id=target,displayName="画像估时目标",category=TaskCategory.LEISURE,totalDurationMinutes=20))
        }
        val before=runBlocking { app.planRepository.observeCurrentPlan().first() }
        try {
            rule.waitUntil(20_000) { vm.uiState.value.trusted }
            visible("numeric-profile-open").performClick()
            visible("numeric-value-estimate:LEISURE").assertTextContains("1.5",substring=true)
            capture("numeric-profile-values.png")
            visible("numeric-exclude-task:${ids.first()}").performClick()
            rule.waitUntil(20000) { runBlocking { app.numericProfileRepository.refresh().parameters.single { it.id=="estimate:LEISURE" }.count==2 } }
            // Repository commit and the lifecycle-collected Compose snapshot are
            // separate emissions. Assert the actual refreshed page, not its old frame.
            rule.waitUntil(20000) { rule.onAllNodes(hasTestTag("numeric-value-estimate:LEISURE") and
                hasText("2 个有效样本 · 仅供参考",substring=true)).fetchSemanticsNodes().isNotEmpty() }
            visible("numeric-value-estimate:LEISURE").assertTextContains("仅供参考",substring=true)
            visible("numeric-exclude-task:${ids.first()}").performClick()
            rule.waitUntil(20000) { runBlocking { app.numericProfileRepository.refresh().parameters.single { it.id=="estimate:LEISURE" }.count==3 } }
            rule.waitUntil(20000) { rule.onAllNodes(hasTestTag("numeric-value-estimate:LEISURE") and
                hasText("3 个有效样本 · 可用于建议",substring=true)).fetchSemanticsNodes().isNotEmpty() }
            visible("numeric-edit-${ids.first()}").performClick()
            visible("numeric-total-input").performTextReplacement("-1")
            visible("numeric-confirm-total").performClick(); waitTag("numeric-total-error")
            capture("numeric-profile-total-error.png")
            assertEquals(1.5,runBlocking { app.numericProfileRepository.refresh().parameters.single { it.id=="estimate:LEISURE" }.value!! },0.001)
            visible("numeric-total-input").performTextReplacement("30"); visible("numeric-confirm-total").performClick()
            rule.waitUntil(20000) { rule.onAllNodesWithTag("numeric-total-input").fetchSemanticsNodes().isEmpty() }
            visible("numeric-advice-$target").performClick(); capture("numeric-profile-advice.png"); visible("numeric-cancel-advice").performClick()
            assertEquals(20,runBlocking { app.taskRepository.observeTasks().first().single { it.id==target }.totalDurationMinutes })
            visible("numeric-advice-$target").performClick(); visible("numeric-accept-advice").performClick()
            rule.waitUntil(20000) { runBlocking { app.taskRepository.observeTasks().first().single { it.id==target }.totalDurationMinutes==30 } }
            restoration.emulateSavedInstanceStateRestore(); waitTag("numeric-close")
            visible("numeric-value-estimate:LEISURE").assertTextContains("1.5",substring=true)
            assertEquals(before,runBlocking { app.planRepository.observeCurrentPlan().first() })
            assertTrue(runBlocking { app.taskRepository.observeExecutionLogs(target).first() }.isEmpty())
            visible("numeric-close").performClick()
        } catch(failure: Throwable) {
            runCatching { capture("numeric-profile-failure.png") }
            throw failure
        } finally { runBlocking { for(id in ids) app.numericProfileRepository.clearActualTotal(id) } }
    }
}
