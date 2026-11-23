package com.example.shelf.feature.settings

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import com.example.shelf.core.common.result.ShelfResult
import com.example.shelf.core.data.export.ExportSummary
import com.example.shelf.core.data.export.LibraryExportRepository
import com.example.shelf.core.model.AuthState
import com.example.shelf.core.model.DailyGoal
import com.example.shelf.core.model.GoalType
import com.example.shelf.core.model.ThemeMode
import com.example.shelf.core.model.UserPreferences
import com.example.shelf.core.testing.MainDispatcherRule
import com.example.shelf.core.testing.repository.FakeAuthRepository
import com.example.shelf.core.testing.repository.FakeUserDataRepository
import com.example.shelf.core.ui.theme.ShelfTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val composeRule = createComposeRule()

    private fun string(id: Int) = RuntimeEnvironment.getApplication().getString(id)

    private val userData = FakeUserDataRepository()
    private val auth = FakeAuthRepository(AuthState.Unavailable)
    private val exporter = object : LibraryExportRepository {
        override suspend fun exportTo(destination: Uri) = ShelfResult.Success(ExportSummary(0, 0))
    }

    private fun setRoute() {
        val viewModel = SettingsViewModel(userData, exporter, auth)
        composeRule.setContent {
            ShelfTheme(dynamicColor = false) {
                SettingsRoute(onSignInClick = {}, viewModel = viewModel)
            }
        }
    }

    @Test
    fun withoutFirebase_theAccountEntryReadsSignInToSync() {
        setRoute()

        composeRule.onNodeWithTag(SettingsTestTags.LIST)
            .performScrollToNode(hasText(string(R.string.settings_sign_in)))
        composeRule.onNodeWithText(string(R.string.settings_sign_in)).assertIsDisplayed()
    }

    @Test
    fun signOut_asksWhetherToRemoveTheLocalLibrary() {
        auth.setState(AuthState.SignedIn(FakeAuthRepository.USER))
        setRoute()

        composeRule.onNodeWithTag(SettingsTestTags.LIST)
            .performScrollToNode(hasText(string(R.string.settings_sign_out)))
        composeRule.onNodeWithText(string(R.string.settings_sign_out)).performClick()
        composeRule.onNodeWithText(string(R.string.settings_sign_out_message)).assertIsDisplayed()
        composeRule.onNodeWithTag(SettingsTestTags.CLEAR_LOCAL_DATA).performClick()
        composeRule.onAllNodesWithText(string(R.string.settings_sign_out)).onLast().performClick()

        assertEquals(listOf(true), auth.signOuts)
        composeRule.onNodeWithTag(SettingsTestTags.LIST)
            .performScrollToNode(hasText(string(R.string.settings_sign_in)))
    }

    @Test
    fun dynamicColorSwitch_togglesThePreference() {
        setRoute()
        val row = composeRule.onNodeWithText(string(R.string.settings_dynamic_color))

        row.assertIsOn()
        row.performClick()

        row.assertIsOff()
        assertEquals(false, userData.current.useDynamicColor)
    }

    @Test
    fun themeDialog_appliesTheChosenTheme() {
        setRoute()

        composeRule.onNodeWithText(string(R.string.settings_theme)).performClick()
        composeRule.onNodeWithText(string(R.string.settings_theme_dark)).performClick()

        assertEquals(ThemeMode.DARK, userData.current.themeMode)
        composeRule.onNodeWithText(string(R.string.settings_theme_dark)).assertExists()
    }

    @Test
    fun goalDialog_savesTypeAndTarget() {
        setRoute()

        composeRule.onNodeWithText(string(R.string.settings_daily_goal)).performClick()
        composeRule.onNodeWithText(string(R.string.settings_goal_type_minutes)).performClick()
        composeRule.onNodeWithTag(SettingsTestTags.GOAL_TARGET).performTextReplacement("45")
        composeRule.onNodeWithText(string(com.example.shelf.core.ui.R.string.ui_save)).performClick()

        assertEquals(DailyGoal(GoalType.MINUTES, 45), userData.current.dailyGoal)
    }

    @Test
    fun reminderSwitch_reportsTheToggleAndGatesTheTimeRow() {
        var toggled: Boolean? = null
        composeRule.setContent {
            ShelfTheme(dynamicColor = false) {
                SettingsScreen(
                    uiState = SettingsUiState.Success(
                        preferences = UserPreferences(reminderEnabled = false),
                        account = AuthState.SignedOut,
                        isExporting = false,
                        message = null,
                    ),
                    onThemeModeChange = {},
                    onDynamicColorChange = {},
                    onDefaultShelfChange = {},
                    onDailyGoalChange = {},
                    onReminderToggle = { toggled = it },
                    onReminderTimeChange = {},
                    onExportClick = {},
                    onSignInClick = {},
                    onSignOut = {},
                    onOpenNotificationSettings = {},
                    onMessageShown = {},
                )
            }
        }
        composeRule.onNodeWithTag(SettingsTestTags.LIST)
            .performScrollToNode(hasText(string(R.string.settings_reminder_time)))
        composeRule.onNodeWithText(string(R.string.settings_reminder_time)).assertIsNotEnabled()

        composeRule.onNodeWithText(string(R.string.settings_reminder)).performClick()

        assertEquals(true, toggled)
    }
}
