package com.example.shelf.core.testing.repository

import com.example.shelf.core.common.result.ShelfResult
import com.example.shelf.core.data.repository.AuthRepository
import com.example.shelf.core.model.AuthState
import com.example.shelf.core.model.ShelfUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeAuthRepository(initial: AuthState = AuthState.SignedOut) : AuthRepository {
    private val state = MutableStateFlow(initial)
    override val authState: StateFlow<AuthState> = state

    /** What the next sign-in returns; a success also signs the user in. */
    var signInResult: ShelfResult<ShelfUser> = ShelfResult.Success(USER)
    val idTokens = mutableListOf<String>()

    /** The `clearLocalData` argument of every sign-out. */
    val signOuts = mutableListOf<Boolean>()

    fun setState(authState: AuthState) {
        state.value = authState
    }

    override suspend fun signInWithGoogle(idToken: String): ShelfResult<ShelfUser> {
        idTokens += idToken
        (signInResult as? ShelfResult.Success)?.let { state.value = AuthState.SignedIn(it.data) }
        return signInResult
    }

    override suspend fun signOut(clearLocalData: Boolean) {
        signOuts += clearLocalData
        if (state.value is AuthState.SignedIn) state.value = AuthState.SignedOut
    }

    companion object {
        val USER = ShelfUser(uid = "user-1", displayName = "Ada Reader", email = "ada@example.com")
    }
}
