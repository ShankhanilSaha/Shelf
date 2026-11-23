package com.example.shelf.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shelf.core.common.result.ShelfResult
import com.example.shelf.core.data.export.LibraryExportRepository
import com.example.shelf.core.data.repository.AuthRepository
import com.example.shelf.core.data.repository.UserDataRepository
import com.example.shelf.core.model.AuthState
import com.example.shelf.core.model.DailyGoal
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.model.ThemeMode
import com.example.shelf.core.model.UserPreferences
import com.example.shelf.core.ui.component.UserMessage
import com.example.shelf.core.ui.text.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val exportRepository: LibraryExportRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val message = MutableStateFlow<SettingsMessage?>(null)
    private val exporting = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        userDataRepository.userPreferences,
        authRepository.authState,
        exporting,
        message,
    ) { preferences, account, exporting, message ->
        SettingsUiState.Success(
            preferences = preferences,
            account = account,
            isExporting = exporting,
            message = message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SettingsUiState.Loading)

    fun onThemeModeChange(mode: ThemeMode) = launch { userDataRepository.setThemeMode(mode) }

    fun onDynamicColorChange(enabled: Boolean) = launch { userDataRepository.setUseDynamicColor(enabled) }

    fun onDefaultShelfChange(shelf: ShelfType) = launch { userDataRepository.setDefaultShelf(shelf) }

    fun onDailyGoalChange(goal: DailyGoal) = launch { userDataRepository.setDailyGoal(goal) }

    /** Called once notification permission is known to be granted (or not needed). */
    fun onReminderEnabledChange(enabled: Boolean) = launch { userDataRepository.setReminderEnabled(enabled) }

    fun onReminderTimeChange(time: LocalTime) = launch { userDataRepository.setReminderTime(time) }

    /** The user declined notifications: explain, and offer the system settings page. */
    fun onNotificationPermissionDenied() {
        message.value = SettingsMessage.NotificationsBlocked(
            UserMessage(
                text = UiText.Resource(R.string.settings_notifications_blocked),
                actionLabel = UiText.Resource(R.string.settings_open_system_settings),
            ),
        )
    }

    fun onExport(destination: Uri) {
        exporting.value = true
        viewModelScope.launch {
            val result = exportRepository.exportTo(destination)
            exporting.value = false
            message.value = SettingsMessage.Info(
                UserMessage(
                    when (result) {
                        is ShelfResult.Success -> UiText.Plural(
                            id = R.plurals.settings_export_done,
                            quantity = result.data.books,
                            args = listOf(result.data.books, result.data.readingLogs),
                        )

                        is ShelfResult.Failure -> UiText.Resource(R.string.settings_export_failed)
                    },
                ),
            )
        }
    }

    /** Signs out; the library stays on this device unless [clearLocalData]. */
    fun onSignOut(clearLocalData: Boolean) {
        viewModelScope.launch {
            authRepository.signOut(clearLocalData)
            message.value = SettingsMessage.Info(
                UserMessage(
                    UiText.Resource(
                        if (clearLocalData) R.string.settings_signed_out_cleared else R.string.settings_signed_out,
                    ),
                ),
            )
        }
    }

    fun onMessageShown() {
        message.value = null
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Success(
        val preferences: UserPreferences,
        val account: AuthState,
        val isExporting: Boolean,
        val message: SettingsMessage?,
    ) : SettingsUiState
}

/** Messages differ in what their action does, so the UI can route the action correctly. */
sealed interface SettingsMessage {
    val message: UserMessage

    data class Info(override val message: UserMessage) : SettingsMessage

    /** Action opens the system notification settings. */
    data class NotificationsBlocked(override val message: UserMessage) : SettingsMessage
}
