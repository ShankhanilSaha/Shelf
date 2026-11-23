package com.example.shelf.feature.auth

/**
 * The OAuth web client id Google issues the ID token for (Firebase verifies it). Provided by the
 * app from its google-services.json; empty when the build has none.
 */
data class GoogleSignInConfig(val webClientId: String)
