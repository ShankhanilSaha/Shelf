package com.example.shelf.feature.settings

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.shelf.core.model.AuthState
import com.example.shelf.core.model.DailyGoal
import com.example.shelf.core.model.GoalType
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.model.ThemeMode
import com.example.shelf.core.ui.component.LoadingState
import com.example.shelf.core.ui.component.UserMessageEffect
import com.example.shelf.core.ui.icon.ShelfIcons
import com.example.shelf.core.ui.text.labelRes
import com.example.shelf.core.ui.theme.supportsDynamicColor
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onDefaultShelfChange: (ShelfType) -> Unit,
    onDailyGoalChange: (DailyGoal) -> Unit,
    onReminderToggle: (Boolean) -> Unit,
    onReminderTimeChange: (LocalTime) -> Unit,
    onExportClick: () -> Unit,
    onSignInClick: () -> Unit,
    onSignOut: (clearLocalData: Boolean) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
    dynamicColorAvailable: Boolean = supportsDynamicColor,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = (uiState as? SettingsUiState.Success)?.message
    UserMessageEffect(
        message = message?.message,
        hostState = snackbarHostState,
        onShown = onMessageShown,
        onAction = {
            onMessageShown()
            if (message is SettingsMessage.NotificationsBlocked) onOpenNotificationSettings()
        },
    )
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (uiState) {
            SettingsUiState.Loading -> LoadingState(Modifier.padding(padding))

            is SettingsUiState.Success -> Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                SettingsList(
                    state = uiState,
                    dynamicColorAvailable = dynamicColorAvailable,
                    onThemeModeChange = onThemeModeChange,
                    onDynamicColorChange = onDynamicColorChange,
                    onDefaultShelfChange = onDefaultShelfChange,
                    onDailyGoalChange = onDailyGoalChange,
                    onReminderToggle = onReminderToggle,
                    onReminderTimeChange = onReminderTimeChange,
                    onExportClick = onExportClick,
                    onSignInClick = onSignInClick,
                    onSignOut = onSignOut,
                )
            }
        }
    }
}

private enum class SettingsDialog { THEME, GOAL, DEFAULT_SHELF, REMINDER_TIME, SIGN_OUT }

@Composable
private fun SettingsList(
    state: SettingsUiState.Success,
    dynamicColorAvailable: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onDefaultShelfChange: (ShelfType) -> Unit,
    onDailyGoalChange: (DailyGoal) -> Unit,
    onReminderToggle: (Boolean) -> Unit,
    onReminderTimeChange: (LocalTime) -> Unit,
    onExportClick: () -> Unit,
    onSignInClick: () -> Unit,
    onSignOut: (clearLocalData: Boolean) -> Unit,
) {
    val preferences = state.preferences
    var openDialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    val timeFormatter = remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }

    LazyColumn(
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .testTag(SettingsTestTags.LIST),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { SectionHeader(stringResource(R.string.settings_section_appearance)) }
        item {
            SettingsRow(
                icon = ShelfIcons.DarkMode,
                title = stringResource(R.string.settings_theme),
                summary = stringResource(preferences.themeMode.labelRes),
                onClick = { openDialog = SettingsDialog.THEME },
            )
        }
        if (dynamicColorAvailable) {
            item {
                SwitchRow(
                    icon = ShelfIcons.Palette,
                    title = stringResource(R.string.settings_dynamic_color),
                    summary = stringResource(R.string.settings_dynamic_color_summary),
                    checked = preferences.useDynamicColor,
                    onCheckedChange = onDynamicColorChange,
                )
            }
        }

        item { SectionHeader(stringResource(R.string.settings_section_reading)) }
        item {
            SettingsRow(
                icon = ShelfIcons.Goal,
                title = stringResource(R.string.settings_daily_goal),
                summary = goalSummary(preferences.dailyGoal),
                onClick = { openDialog = SettingsDialog.GOAL },
            )
        }
        item {
            SettingsRow(
                icon = ShelfIcons.LibraryOutlined,
                title = stringResource(R.string.settings_default_shelf),
                summary = stringResource(preferences.defaultShelf.labelRes),
                onClick = { openDialog = SettingsDialog.DEFAULT_SHELF },
            )
        }

        item { SectionHeader(stringResource(R.string.settings_section_reminders)) }
        item {
            SwitchRow(
                icon = ShelfIcons.Notifications,
                title = stringResource(R.string.settings_reminder),
                summary = stringResource(R.string.settings_reminder_summary),
                checked = preferences.reminderEnabled,
                onCheckedChange = onReminderToggle,
            )
        }
        item {
            SettingsRow(
                icon = ShelfIcons.Timer,
                title = stringResource(R.string.settings_reminder_time),
                summary = preferences.reminderTime.format(timeFormatter),
                enabled = preferences.reminderEnabled,
                onClick = { openDialog = SettingsDialog.REMINDER_TIME },
            )
        }

        item { SectionHeader(stringResource(R.string.settings_section_sync)) }
        when (val account = state.account) {
            // Without a Firebase configuration the entry still reads "Sign in to sync"; the sign-in
            // screen then explains that sync is not set up in this build.
            AuthState.Unavailable, AuthState.SignedOut -> item {
                SettingsRow(
                    icon = ShelfIcons.Sync,
                    title = stringResource(R.string.settings_sign_in),
                    summary = stringResource(R.string.settings_sign_in_summary),
                    onClick = onSignInClick,
                )
            }

            is AuthState.SignedIn -> {
                item {
                    SettingsRow(
                        icon = ShelfIcons.Account,
                        title = account.user.displayName ?: account.user.email.orEmpty(),
                        summary = stringResource(R.string.settings_syncing_as, account.user.email.orEmpty()),
                    )
                }
                item {
                    SettingsRow(
                        icon = ShelfIcons.SignOut,
                        title = stringResource(R.string.settings_sign_out),
                        summary = stringResource(R.string.settings_sign_out_summary),
                        onClick = { openDialog = SettingsDialog.SIGN_OUT },
                    )
                }
            }
        }

        item { SectionHeader(stringResource(R.string.settings_section_data)) }
        item {
            SettingsRow(
                icon = ShelfIcons.Export,
                title = stringResource(R.string.settings_export),
                summary = stringResource(R.string.settings_export_summary),
                enabled = !state.isExporting,
                trailing = if (state.isExporting) {
                    { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                } else {
                    null
                },
                onClick = onExportClick,
            )
        }

        item { SectionHeader(stringResource(R.string.settings_section_about)) }
        item {
            val version = remember {
                runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
            }
            SettingsRow(
                icon = ShelfIcons.Book,
                title = stringResource(R.string.settings_version),
                summary = version.orEmpty(),
            )
        }
    }

    when (openDialog) {
        SettingsDialog.THEME -> ChoiceDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries,
            selected = preferences.themeMode,
            label = { stringResource(it.labelRes) },
            onSelect = onThemeModeChange,
            onDismiss = { openDialog = null },
        )

        SettingsDialog.DEFAULT_SHELF -> ChoiceDialog(
            title = stringResource(R.string.settings_default_shelf),
            options = ShelfType.entries,
            selected = preferences.defaultShelf,
            label = { stringResource(it.labelRes) },
            onSelect = onDefaultShelfChange,
            onDismiss = { openDialog = null },
        )

        SettingsDialog.GOAL -> GoalDialog(
            goal = preferences.dailyGoal,
            onSave = onDailyGoalChange,
            onDismiss = { openDialog = null },
        )

        SettingsDialog.REMINDER_TIME -> ReminderTimeDialog(
            time = preferences.reminderTime,
            is24Hour = DateFormat.is24HourFormat(context),
            onSave = onReminderTimeChange,
            onDismiss = { openDialog = null },
        )

        SettingsDialog.SIGN_OUT -> SignOutDialog(
            onConfirm = onSignOut,
            onDismiss = { openDialog = null },
        )

        null -> Unit
    }
}

@Composable
internal fun goalSummary(goal: DailyGoal): String = when (goal.type) {
    GoalType.PAGES -> pluralStringResource(R.plurals.settings_goal_pages, goal.target, goal.target)
    GoalType.MINUTES -> pluralStringResource(R.plurals.settings_goal_minutes, goal.target, goal.target)
}

private val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    }

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    summary: String,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowContent(icon = icon, title = title, summary = summary, enabled = enabled)
        trailing?.invoke()
    }
}

/** The whole row is the switch (one target, announced as "switch, on/off"). */
@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowContent(icon = icon, title = title, summary = summary, enabled = true)
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun RowScope.RowContent(icon: ImageVector, title: String, summary: String, enabled: Boolean) {
    val alpha = if (enabled) 1f else DISABLED_ALPHA
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
    )
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
        )
        if (summary.isNotEmpty()) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            )
        }
    }
}

private const val DISABLED_ALPHA = 0.38f
