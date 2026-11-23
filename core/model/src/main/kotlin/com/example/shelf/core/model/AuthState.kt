package com.example.shelf.core.model

data class ShelfUser(val uid: String, val displayName: String?, val email: String?)

sealed interface AuthState {
    /** Firebase is not configured in this build (no google-services.json). */
    data object Unavailable : AuthState

    data object SignedOut : AuthState

    data class SignedIn(val user: ShelfUser) : AuthState
}
