package com.example.shelf.core.data.auth

import com.example.shelf.core.common.result.ShelfError
import com.example.shelf.core.common.result.ShelfResult
import com.example.shelf.core.data.repository.AuthRepository
import com.example.shelf.core.database.DatabaseCleaner
import com.example.shelf.core.model.AuthState
import com.example.shelf.core.model.ShelfUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/** Used when this build has no Firebase configuration: there is no account to sign in to. */
internal class NoOpAuthRepository @Inject constructor(private val databaseCleaner: DatabaseCleaner) : AuthRepository {
    override val authState: Flow<AuthState> = flowOf(AuthState.Unavailable)

    override suspend fun signInWithGoogle(idToken: String): ShelfResult<ShelfUser> =
        ShelfResult.Failure(ShelfError.Unavailable)

    override suspend fun signOut(clearLocalData: Boolean) {
        if (clearLocalData) databaseCleaner.clearAll()
    }
}
