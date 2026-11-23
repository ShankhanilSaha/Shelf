package com.example.shelf.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shelf.core.common.result.ShelfResult
import com.example.shelf.core.data.repository.AuthRepository
import com.example.shelf.core.model.AuthState
import com.example.shelf.core.model.ShelfUser
import com.example.shelf.core.ui.text.UiText
import com.example.shelf.core.ui.text.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val config: GoogleSignInConfig,
) : ViewModel() {

    private val attempt = MutableStateFlow(Attempt())

    val uiState: StateFlow<AuthUiState> = combine(authRepository.authState, attempt) { auth, attempt ->
        when {
            auth is AuthState.SignedIn -> AuthUiState.SignedIn(auth.user)

            // Firebase without a Google OAuth client cannot issue the token Firebase needs.
            auth is AuthState.Unavailable || config.webClientId.isBlank() -> AuthUiState.Unavailable

            else -> AuthUiState.SignedOut(signingIn = attempt.signingIn, error = attempt.error)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AuthUiState.Loading)

    val webClientId: String get() = config.webClientId

    /** The account picker is about to open. */
    fun onSignInStarted() {
        attempt.value = Attempt(signingIn = true)
    }

    fun onGoogleResult(result: GoogleIdTokenResult) {
        when (result) {
            is GoogleIdTokenResult.Success -> viewModelScope.launch {
                attempt.value = when (val signIn = authRepository.signInWithGoogle(result.idToken)) {
                    is ShelfResult.Success -> Attempt()
                    is ShelfResult.Failure -> Attempt(error = signIn.error.toUiText())
                }
            }

            GoogleIdTokenResult.Cancelled -> attempt.value = Attempt()

            GoogleIdTokenResult.NoAccount ->
                attempt.value =
                    Attempt(error = UiText.Resource(R.string.auth_error_no_account))

            GoogleIdTokenResult.Failed -> attempt.value = Attempt(error = UiText.Resource(R.string.auth_error_generic))
        }
    }

    private data class Attempt(val signingIn: Boolean = false, val error: UiText? = null)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

sealed interface AuthUiState {
    data object Loading : AuthUiState

    /** No Firebase configuration (or no Google sign-in in it) in this build. */
    data object Unavailable : AuthUiState

    /** [error] explains the last failed attempt until the next one starts. */
    data class SignedOut(val signingIn: Boolean, val error: UiText?) : AuthUiState

    /** Where this screen ends: the route closes it. */
    data class SignedIn(val user: ShelfUser) : AuthUiState
}
