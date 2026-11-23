package com.example.shelf.core.data.auth

import com.example.shelf.core.common.result.ShelfError
import com.example.shelf.core.common.result.ShelfResult
import com.example.shelf.core.data.repository.AuthRepository
import com.example.shelf.core.data.sync.SyncScheduler
import com.example.shelf.core.database.DatabaseCleaner
import com.example.shelf.core.model.AuthState
import com.example.shelf.core.model.ShelfUser
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** Firebase Auth with Google credentials. Used only when Firebase is configured. */
internal class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val syncScheduler: SyncScheduler,
    private val databaseCleaner: DatabaseCleaner,
) : AuthRepository {

    // The listener is called right away with the current user, then on every change.
    override val authState: Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser.toAuthState()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override suspend fun signInWithGoogle(idToken: String): ShelfResult<ShelfUser> = try {
        val user = auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await().user
        if (user == null) {
            ShelfResult.Failure(ShelfError.Unknown)
        } else {
            // Pull the account's library now and keep pulling it every few hours.
            syncScheduler.schedulePeriodicSync()
            syncScheduler.requestSync(immediate = true)
            ShelfResult.Success(user.toShelfUser())
        }
    } catch (e: FirebaseNetworkException) {
        ShelfResult.Failure(ShelfError.Network, e)
    } catch (e: FirebaseException) {
        ShelfResult.Failure(ShelfError.Server, e)
    }

    override suspend fun signOut(clearLocalData: Boolean) {
        syncScheduler.cancelSync()
        auth.signOut()
        if (clearLocalData) databaseCleaner.clearAll()
    }
}

private fun FirebaseUser?.toAuthState(): AuthState = this?.let { AuthState.SignedIn(it.toShelfUser()) }
    ?: AuthState.SignedOut

private fun FirebaseUser.toShelfUser() = ShelfUser(uid = uid, displayName = displayName, email = email)
