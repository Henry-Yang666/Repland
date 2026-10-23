package com.swan1127.repland

import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.swan1127.repland.data.profile.LocalPersonalProfileStore
import com.swan1127.repland.data.profile.PersonalProfile
import com.swan1127.repland.ui.profile.*
import com.swan1127.repland.ui.theme.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class PersonalProfileUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun cancel_keeps_the_saved_username() {
        var saves = 0
        rule.setContent { ReplandTheme { PersonalProfileHeader(PersonalProfile("小林"), PersonalProfileOperation(false), {}, {}, { _, _ -> saves++ }) } }
        rule.onNodeWithTag("personal-profile-edit-name").performClick()
        rule.onNodeWithTag("personal-profile-name-input").performTextReplacement("未保存的名字")
        rule.onNodeWithTag("personal-profile-name-cancel").performClick()
        rule.onNodeWithTag("personal-profile-name").assertTextEquals("小林")
        assertEquals(0, saves)
    }

    @Test fun invalid_names_keep_the_dialog_open_and_valid_save_commits_once() {
        var saved = ""; var saves = 0
        rule.setContent { ReplandTheme { PersonalProfileHeader(PersonalProfile("小林"), PersonalProfileOperation(false), {}, {}, { value, done -> saved = value; saves++; done() }) } }
        rule.onNodeWithTag("personal-profile-edit-name").performClick()
        rule.onNodeWithTag("personal-profile-name-input").performTextReplacement(" ")
        rule.onNodeWithTag("personal-profile-name-save").performClick()
        rule.onNodeWithText("请输入用户名").assertExists()
        rule.onNodeWithTag("personal-profile-name-input").performTextReplacement("林".repeat(25))
        rule.onNodeWithTag("personal-profile-name-save").performClick()
        rule.onNodeWithText("用户名最多 24 个字").assertExists()
        assertEquals(0, saves)
        rule.onNodeWithTag("personal-profile-name-input").performTextReplacement("小林的新日程")
        rule.onNodeWithTag("personal-profile-name-save").performClick()
        rule.onNodeWithTag("personal-profile-name-input").assertDoesNotExist()
        assertEquals("小林的新日程", saved); assertEquals(1, saves)
    }

    @Test fun save_failure_retains_input_and_busy_state_disables_duplicate_actions() {
        var operation by mutableStateOf(PersonalProfileOperation(false))
        var saves = 0
        rule.setContent { ReplandTheme { PersonalProfileHeader(PersonalProfile("小林"), operation, {}, {}, { _, _ -> saves++; operation = PersonalProfileOperation(false, true) }) } }
        rule.onNodeWithTag("personal-profile-edit-name").performClick()
        rule.onNodeWithTag("personal-profile-name-input").performTextReplacement("重试时保留的名字")
        rule.onNodeWithTag("personal-profile-name-save").performClick()
        rule.onNodeWithTag("personal-profile-name-save").assertIsNotEnabled()
        rule.onNodeWithTag("personal-profile-name-cancel").assertIsNotEnabled()
        rule.runOnIdle { operation = PersonalProfileOperation(false, false, "用户名未保存，请重试") }
        rule.onNodeWithTag("personal-profile-name-input").assertTextContains("重试时保留的名字")
        rule.onNodeWithText("用户名未保存，请重试").assertExists()
        rule.onNodeWithTag("personal-profile-name-save").assertIsEnabled().performClick()
        assertEquals(2, saves)
    }

    @Test fun large_dark_reduced_motion_keeps_avatar_and_editing_reachable() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) { ReplandTheme(darkTheme = true) {
                CompositionLocalProvider(LocalReducedMotion provides true) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        PersonalProfileHeader(PersonalProfile("日程里的长用户名".repeat(3)), PersonalProfileOperation(false), {}, {}, { _, done -> done() })
                    }
                }
            } }
        }
        rule.onNodeWithTag("personal-profile-avatar").assertIsDisplayed()
        rule.onNodeWithTag("personal-profile-edit-name").performScrollTo().assertIsDisplayed().performClick()
        rule.onNodeWithTag("personal-profile-name-input").performTextReplacement("小林")
        rule.onNodeWithTag("personal-profile-name-save").assertIsDisplayed().performClick()
        rule.onNodeWithTag("personal-profile-name-input").assertDoesNotExist()
    }

    @Test fun photo_picker_cancel_preserves_the_profile_and_requests_images_only() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = LocalPersonalProfileStore(context, "profile-ui-test-${UUID.randomUUID()}")
        runBlocking { store.load(); store.saveUsername("保留的名字") }
        val vm = PersonalProfileViewModel(store)
        var launches = 0
        val registry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {
                assertTrue(contract is ActivityResultContracts.PickVisualMedia)
                assertEquals(ActivityResultContracts.PickVisualMedia.ImageOnly, (input as PickVisualMediaRequest).mediaType)
                launches++; dispatchResult<Uri?>(requestCode, null)
            }
        }
        try {
            rule.setContent { ReplandTheme {
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides object : ActivityResultRegistryOwner {
                    override val activityResultRegistry = registry
                }) { PersonalProfileSection(vm) }
            } }
            rule.waitUntil(5000) { !vm.operation.value.loading }
            rule.onNodeWithTag("personal-profile-change-avatar").performClick()
            rule.onNodeWithTag("personal-profile-name").assertTextEquals("保留的名字")
            assertEquals(1, launches); assertNull(store.profile.value!!.avatar)
        } finally { runBlocking { store.clear() } }
    }
}
