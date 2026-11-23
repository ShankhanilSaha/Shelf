package com.example.shelf.feature.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

sealed interface GoogleIdTokenResult {
    data class Success(val idToken: String) : GoogleIdTokenResult

    /** The user closed the account picker. */
    data object Cancelled : GoogleIdTokenResult

    /** There is no Google account on the device. */
    data object NoAccount : GoogleIdTokenResult

    data object Failed : GoogleIdTokenResult
}

/** Gets a Google ID token from the user; needs an activity context to show the account picker. */
fun interface GoogleIdTokenClient {
    suspend fun requestIdToken(activityContext: Context, serverClientId: String): GoogleIdTokenResult
}

/** "Sign in with Google" through Credential Manager. */
class CredentialManagerGoogleIdTokenClient : GoogleIdTokenClient {
    override suspend fun requestIdToken(activityContext: Context, serverClientId: String): GoogleIdTokenResult {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId).build())
            .build()
        return try {
            val credential = CredentialManager.create(
                activityContext,
            ).getCredential(activityContext, request).credential
            if (credential is CustomCredential && credential.type in GOOGLE_ID_TOKEN_TYPES) {
                GoogleIdTokenResult.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                GoogleIdTokenResult.Failed
            }
        } catch (_: GetCredentialCancellationException) {
            GoogleIdTokenResult.Cancelled
        } catch (_: NoCredentialException) {
            GoogleIdTokenResult.NoAccount
        } catch (_: GetCredentialException) {
            GoogleIdTokenResult.Failed
        } catch (_: GoogleIdTokenParsingException) {
            GoogleIdTokenResult.Failed
        }
    }

    private companion object {
        // The "Sign in with Google" button flow may report its own credential type.
        val GOOGLE_ID_TOKEN_TYPES = setOf(
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL,
        )
    }
}
