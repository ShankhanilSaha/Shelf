package com.example.shelf.core.data.repository

import com.example.shelf.core.common.result.ShelfResult
import com.example.shelf.core.model.AuthState
import com.example.shelf.core.model.ShelfUser
import kotlinx.coroutines.flow.Flow

/**
 * The optional account used for sync. Signing in never changes what is stored locally; it
 * starts syncing it. [AuthState.Unavailable] means this build has no Firebase configuration.
 */
interface AuthRepository {
    val authState: Flow<AuthState>

    /** Signs in to Firebase with a Google ID token obtained through Credential Manager. */
    suspend fun signInWithGoogle(idToken: String): ShelfResult<ShelfUser>

    /** Stops syncing and signs out. Local data stays unless [clearLocalData]. */
    suspend fun signOut(clearLocalData: Boolean)
}
