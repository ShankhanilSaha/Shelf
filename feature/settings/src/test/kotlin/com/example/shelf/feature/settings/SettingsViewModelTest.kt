package com.example.shelf.feature.settings

import android.net.Uri
import app.cash.turbine.test
import com.example.shelf.core.common.result.ShelfError
import com.example.shelf.core.common.result.ShelfResult
import com.example.shelf.core.data.export.ExportSummary
import com.example.shelf.core.data.export.LibraryExportRepository
import com.example.shelf.core.model.AuthState
import com.example.shelf.core.model.DailyGoal
import com.example.shelf.core.model.GoalType
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.model.ThemeMode
import com.example.shelf.core.testing.MainDispatcherRule
import com.example.shelf.core.testing.awaitUntil
import com.example.shelf.core.testing.repository.FakeAuthRepository
import com.example.shelf.core.testing.repository.FakeUserDataRepository
import com.example.shelf.core.ui.text.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class) // android.net.Uri
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userData = FakeUserDataRepository()
    private val exporter = FakeExporter()
    private val auth = FakeAuthRepository(AuthState.SignedIn(FakeAuthRepository.USER))

    private fun TestScope.viewModel(): SettingsViewModel {
        val viewModel = SettingsViewModel(userData, exporter, auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private val SettingsViewModel.success get() = assertIs<SettingsUiState.Success>(uiState.value)

    @Test
    fun `signing out keeps the library unless asked, and says which it did`() = runTest {
        val viewModel = viewModel()
        assertEquals(AuthState.SignedIn(FakeAuthRepository.USER), viewModel.success.account)

        viewModel.onSignOut(clearLocalData = false)

        assertEquals(AuthState.SignedOut, viewModel.success.account)
        assertEquals(
            UiText.Resource(R.string.settings_signed_out),
            viewModel.success.message?.message?.text,
        )

        viewModel.onMessageShown()
        viewModel.onSignOut(clearLocalData = true)

        assertEquals(listOf(false, true), auth.signOuts)
        assertEquals(
            UiText.Resource(R.string.settings_signed_out_cleared),
            viewModel.success.message?.message?.text,
        )
    }

    @Test
    fun `every setting is written to the repository and reflected in the state`() = runTest {
        val viewModel = viewModel()

        viewModel.onThemeModeChange(ThemeMode.DARK)
        viewModel.onDynamicColorChange(false)
        viewModel.onDefaultShelfChange(ShelfType.READING)
        viewModel.onDailyGoalChange(DailyGoal(GoalType.MINUTES, 40))
        viewModel.onReminderEnabledChange(true)
        viewModel.onReminderTimeChange(LocalTime.of(21, 30))

        val preferences = viewModel.success.preferences
        assertEquals(ThemeMode.DARK, preferences.themeMode)
        assertEquals(false, preferences.useDynamicColor)
        assertEquals(ShelfType.READING, preferences.defaultShelf)
        assertEquals(DailyGoal(GoalType.MINUTES, 40), preferences.dailyGoal)
        assertEquals(true, preferences.reminderEnabled)
        assertEquals(LocalTime.of(21, 30), preferences.reminderTime)
    }

    @Test
    fun `a refused notification permission explains how to allow it`() = runTest {
        val viewModel = viewModel()

        viewModel.onNotificationPermissionDenied()

        val message = assertIs<SettingsMessage.NotificationsBlocked>(viewModel.success.message)
        assertEquals(UiText.Resource(R.string.settings_open_system_settings), message.message.actionLabel)
        assertEquals(false, viewModel.success.preferences.reminderEnabled)
        viewModel.onMessageShown()
        assertNull(viewModel.success.message)
    }

    @Test
    fun `export reports how much was written, or that it failed`() = runTest {
        val viewModel = viewModel()
        val uri = Uri.parse("content://docs/1")

        exporter.result = ShelfResult.Success(ExportSummary(books = 3, readingLogs = 7))
        viewModel.onExport(uri)
        val done = assertIs<SettingsMessage.Info>(viewModel.success.message).message.text
        assertEquals(UiText.Plural(R.plurals.settings_export_done, 3, listOf(3, 7)), done)
        assertEquals(false, viewModel.success.isExporting)
        assertEquals(listOf(uri), exporter.destinations)

        exporter.result = ShelfResult.Failure(ShelfError.Unknown)
        viewModel.onExport(uri)
        val failed = assertIs<SettingsMessage.Info>(viewModel.success.message).message.text
        assertEquals(UiText.Resource(R.string.settings_export_failed), failed)
    }

    private class FakeExporter : LibraryExportRepository {
        var result: ShelfResult<ExportSummary> = ShelfResult.Success(ExportSummary(0, 0))
        val destinations = mutableListOf<Uri>()

        override suspend fun exportTo(destination: Uri): ShelfResult<ExportSummary> {
            destinations += destination
            return result
        }
    }

    @Test
    fun `turbine - a changed setting comes back through the state`() = runTest {
        val viewModel = SettingsViewModel(userData, exporter, auth)

        viewModel.uiState.test {
            awaitUntil { it is SettingsUiState.Success }

            viewModel.onThemeModeChange(ThemeMode.DARK)

            awaitUntil { (it as? SettingsUiState.Success)?.preferences?.themeMode == ThemeMode.DARK }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
