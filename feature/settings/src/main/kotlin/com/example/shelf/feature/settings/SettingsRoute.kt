package com.example.shelf.feature.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate

/** Entry point of the settings feature. */
@Composable
fun SettingsRoute(
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // POST_NOTIFICATIONS is a runtime permission from API 33. The reminder is only switched on
    // once it is granted; a refusal (including "don't ask again") explains how to allow it.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.onReminderEnabledChange(true) else viewModel.onNotificationPermissionDenied()
    }
    // Storage Access Framework: the user picks where the file goes; no storage permission needed.
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(JSON_MIME_TYPE)) { uri ->
            if (uri != null) viewModel.onExport(uri)
        }

    SettingsScreen(
        uiState = uiState,
        onThemeModeChange = viewModel::onThemeModeChange,
        onDynamicColorChange = viewModel::onDynamicColorChange,
        onDefaultShelfChange = viewModel::onDefaultShelfChange,
        onDailyGoalChange = viewModel::onDailyGoalChange,
        onReminderToggle = { enabled ->
            if (enabled && needsNotificationPermission(context)) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.onReminderEnabledChange(enabled)
            }
        },
        onReminderTimeChange = viewModel::onReminderTimeChange,
        onExportClick = { exportLauncher.launch("shelf-library-${LocalDate.now()}.json") },
        onSignInClick = onSignInClick,
        onSignOut = viewModel::onSignOut,
        onOpenNotificationSettings = { context.startActivity(notificationSettingsIntent(context)) },
        onMessageShown = viewModel::onMessageShown,
        modifier = modifier,
    )
}

private const val JSON_MIME_TYPE = "application/json"

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

private fun notificationSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
