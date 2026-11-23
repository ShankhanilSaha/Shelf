package com.example.shelf.feature.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.shelf.core.model.AuthState
import com.example.shelf.core.model.ShelfUser
import com.example.shelf.core.model.UserPreferences
import com.example.shelf.core.ui.theme.ShelfTheme
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the Accessibility Test Framework over the whole screen: touch target size, text and image
 * contrast on the real rendering, missing labels, duplicate clickable areas. A finding of type
 * ERROR fails the test. Needs a device or emulator on API 34+.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SettingsAccessibilityTest {
    private fun check(account: AuthState, dark: Boolean) = runComposeUiTest {
        enableAccessibilityChecks()
        setContent {
            ShelfTheme(darkTheme = dark, dynamicColor = false) {
                SettingsScreen(
                    uiState = SettingsUiState.Success(UserPreferences(reminderEnabled = true), account, false, null),
                    onThemeModeChange = {},
                    onDynamicColorChange = {},
                    onDefaultShelfChange = {},
                    onDailyGoalChange = {},
                    onReminderToggle = {},
                    onReminderTimeChange = {},
                    onExportClick = {},
                    onSignInClick = {},
                    onSignOut = {},
                    onOpenNotificationSettings = {},
                    onMessageShown = {},
                    dynamicColorAvailable = true,
                )
            }
        }
        onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun signedOut_light() = check(AuthState.SignedOut, dark = false)

    @Test
    fun signedIn_dark() = check(AuthState.SignedIn(ShelfUser("u", "Sam Reader", "sam@example.com")), dark = true)
}
