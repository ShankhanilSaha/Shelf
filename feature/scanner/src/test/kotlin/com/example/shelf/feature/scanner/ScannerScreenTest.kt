package com.example.shelf.feature.scanner

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.shelf.core.model.ShelfType
import com.example.shelf.core.testing.TestData
import com.example.shelf.core.ui.theme.ShelfTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ScannerScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun string(id: Int, vararg args: Any) = RuntimeEnvironment.getApplication().getString(id, *args)

    private fun setScreen(
        uiState: ScannerUiState = ScannerUiState.Scanning(),
        permission: CameraPermission,
        onOpenSettings: () -> Unit = {},
        onManualSubmit: () -> Boolean = { false },
        onOpenDetails: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            ShelfTheme(dynamicColor = false) {
                ScannerScreen(
                    uiState = uiState,
                    permission = permission,
                    cameraAvailable = true,
                    torchAvailable = false,
                    torchOn = false,
                    manualIsbn = "",
                    onRequestPermission = {},
                    onOpenSettings = onOpenSettings,
                    onBack = {},
                    onToggleTorch = {},
                    onManualIsbnChange = {},
                    onManualSubmit = onManualSubmit,
                    onOpenDetails = onOpenDetails,
                    onAddToShelf = {},
                    onScanAgain = {},
                    onRetry = {},
                    cameraPreview = {},
                )
            }
        }
    }

    @Test
    fun deniedOnce_offersToAskAgainAndManualEntry() {
        setScreen(permission = CameraPermission.DENIED)

        composeRule.onNodeWithText(string(R.string.scanner_allow_camera)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.scanner_enter_manually)).assertIsDisplayed()
    }

    @Test
    fun permanentlyDenied_pointsToSystemSettings() {
        var openedSettings = false
        setScreen(permission = CameraPermission.PERMANENTLY_DENIED, onOpenSettings = { openedSettings = true })

        composeRule.onNodeWithText(string(R.string.scanner_permission_blocked_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.scanner_open_settings)).performClick()

        assertEquals(true, openedSettings)
    }

    @Test
    fun manualEntry_showsAnErrorForAnInvalidIsbn() {
        setScreen(permission = CameraPermission.PERMANENTLY_DENIED, onManualSubmit = { false })

        composeRule.onNodeWithText(string(R.string.scanner_enter_manually)).performClick()
        composeRule.onNodeWithTag(ScannerTestTags.MANUAL_FIELD).performTextInput("123")
        composeRule.onNodeWithText(string(R.string.scanner_look_up)).performClick()

        composeRule.onNodeWithText(string(R.string.scanner_isbn_invalid)).assertIsDisplayed()
    }

    @Test
    fun foundBook_canBeOpened() {
        var opened: String? = null
        setScreen(
            uiState = ScannerUiState.Found(
                isbn = "9780261103573",
                workKey = "OL1W",
                book = TestData.book(1, title = "The Fellowship of the Ring"),
                defaultShelf = ShelfType.WANT_TO_READ,
                addedTo = null,
            ),
            permission = CameraPermission.GRANTED,
            onOpenDetails = { opened = it },
        )

        composeRule.onNodeWithText("The Fellowship of the Ring").assertIsDisplayed()
        // The scanning hint makes no sense behind a result.
        composeRule.onNodeWithText(string(R.string.scanner_hint)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.scanner_open_details)).performClick()

        assertEquals("OL1W", opened)
    }
}
