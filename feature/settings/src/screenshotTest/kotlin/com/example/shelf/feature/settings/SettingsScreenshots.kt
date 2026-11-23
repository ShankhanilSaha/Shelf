package com.example.shelf.feature.settings

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import com.example.shelf.core.model.AuthState
import com.example.shelf.core.model.ShelfUser
import com.example.shelf.core.model.UserPreferences
import com.example.shelf.core.ui.preview.ScreenPreviews
import com.example.shelf.core.ui.theme.ShelfTheme

@Composable
private fun Settings(account: AuthState) {
    ShelfTheme(dynamicColor = false) {
        SettingsScreen(
            uiState = SettingsUiState.Success(
                preferences = UserPreferences(reminderEnabled = true),
                account = account,
                isExporting = false,
                message = null,
            ),
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

@PreviewTest
@ScreenPreviews
@Composable
fun SettingsSignedOut() = Settings(AuthState.SignedOut)

@PreviewTest
@ScreenPreviews
@Composable
fun SettingsSignedIn() =
    Settings(AuthState.SignedIn(ShelfUser(uid = "u", displayName = "Sam Reader", email = "sam@example.com")))
