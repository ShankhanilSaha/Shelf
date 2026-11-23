package com.example.shelf.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Entry point of the sign-in feature. Closes itself once the user is signed in. */
@Composable
fun AuthRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
    idTokenClient: GoogleIdTokenClient = remember { CredentialManagerGoogleIdTokenClient() },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Credential Manager shows the account picker over the current activity, so it is called
    // from the UI with the activity context rather than from the ViewModel.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.SignedIn) onBack()
    }

    AuthScreen(
        uiState = uiState,
        onSignInClick = {
            viewModel.onSignInStarted()
            scope.launch {
                val result = try {
                    idTokenClient.requestIdToken(context, viewModel.webClientId)
                } catch (e: CancellationException) {
                    viewModel.onGoogleResult(GoogleIdTokenResult.Cancelled) // left the screen meanwhile
                    throw e
                }
                viewModel.onGoogleResult(result)
            }
        },
        onBack = onBack,
        modifier = modifier,
    )
}
