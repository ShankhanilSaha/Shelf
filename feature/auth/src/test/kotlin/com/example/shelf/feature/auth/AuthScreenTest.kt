package com.example.shelf.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.shelf.core.ui.text.UiText
import com.example.shelf.core.ui.theme.ShelfTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class AuthScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun string(id: Int) = RuntimeEnvironment.getApplication().getString(id)

    private fun setScreen(state: AuthUiState, onSignInClick: () -> Unit = {}, onBack: () -> Unit = {}) {
        composeRule.setContent {
            ShelfTheme(dynamicColor = false) {
                AuthScreen(uiState = state, onSignInClick = onSignInClick, onBack = onBack)
            }
        }
    }

    @Test
    fun signedOut_offersGoogleSignIn() {
        var clicks = 0
        setScreen(AuthUiState.SignedOut(signingIn = false, error = null), onSignInClick = { clicks++ })

        composeRule.onNodeWithText(string(R.string.auth_privacy)).assertIsDisplayed()
        composeRule.onNodeWithTag(AuthTestTags.SIGN_IN).performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun signingIn_disablesTheButtonAndShowsErrorsInline() {
        setScreen(AuthUiState.SignedOut(signingIn = true, error = UiText.Resource(R.string.auth_error_generic)))

        composeRule.onNodeWithTag(AuthTestTags.SIGN_IN).assertIsNotEnabled()
        composeRule.onNodeWithText(string(R.string.auth_error_generic)).assertIsDisplayed()
    }

    @Test
    fun unavailable_explainsAndGoesBack() {
        var backs = 0
        setScreen(AuthUiState.Unavailable, onBack = { backs++ })

        composeRule.onNodeWithText(string(R.string.auth_unavailable)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.auth_back)).performClick()

        assertEquals(1, backs)
    }
}
