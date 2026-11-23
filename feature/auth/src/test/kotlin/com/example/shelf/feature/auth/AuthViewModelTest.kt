package com.example.shelf.feature.auth

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.example.shelf.core.common.result.ShelfError
import com.example.shelf.core.common.result.ShelfResult
import com.example.shelf.core.model.AuthState
import com.example.shelf.core.testing.MainDispatcherRule
import com.example.shelf.core.testing.repository.FakeAuthRepository
import com.example.shelf.core.ui.text.UiText
import com.example.shelf.core.ui.text.toUiText
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()

    private fun viewModel(webClientId: String = "client-id") = AuthViewModel(auth, GoogleSignInConfig(webClientId))

    /** The first state after Loading (which may or may not be observed, depending on timing). */
    private suspend fun ReceiveTurbine<AuthUiState>.awaitSettled(): AuthUiState {
        var item = awaitItem()
        while (item == AuthUiState.Loading) item = awaitItem()
        return item
    }

    @Test
    fun `a google token signs in and the screen reaches its end state`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(AuthUiState.SignedOut(signingIn = false, error = null), awaitSettled())

            viewModel.onSignInStarted()
            assertEquals(AuthUiState.SignedOut(signingIn = true, error = null), awaitItem())

            viewModel.onGoogleResult(GoogleIdTokenResult.Success("token"))
            assertEquals(AuthUiState.SignedIn(FakeAuthRepository.USER), awaitItem())
        }
        assertEquals(listOf("token"), auth.idTokens)
    }

    @Test
    fun `closing the account picker just stops the attempt`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            awaitSettled()
            viewModel.onSignInStarted()
            awaitItem()

            viewModel.onGoogleResult(GoogleIdTokenResult.Cancelled)

            assertEquals(AuthUiState.SignedOut(signingIn = false, error = null), awaitItem())
        }
    }

    @Test
    fun `failures explain themselves until the next attempt`() = runTest {
        val viewModel = viewModel()
        auth.signInResult = ShelfResult.Failure(ShelfError.Network)
        viewModel.uiState.test {
            awaitSettled()

            viewModel.onGoogleResult(GoogleIdTokenResult.NoAccount)
            assertEquals(
                AuthUiState.SignedOut(signingIn = false, error = UiText.Resource(R.string.auth_error_no_account)),
                awaitItem(),
            )

            viewModel.onGoogleResult(GoogleIdTokenResult.Success("token"))
            assertEquals(AuthUiState.SignedOut(signingIn = false, error = ShelfError.Network.toUiText()), awaitItem())

            viewModel.onSignInStarted()
            assertEquals(AuthUiState.SignedOut(signingIn = true, error = null), awaitItem())
        }
    }

    @Test
    fun `without firebase or a web client id there is nothing to sign in to`() = runTest {
        auth.setState(AuthState.Unavailable)
        viewModel().uiState.test { assertEquals(AuthUiState.Unavailable, awaitSettled()) }

        auth.setState(AuthState.SignedOut)
        viewModel(webClientId = "").uiState.test { assertEquals(AuthUiState.Unavailable, awaitSettled()) }
    }
}
