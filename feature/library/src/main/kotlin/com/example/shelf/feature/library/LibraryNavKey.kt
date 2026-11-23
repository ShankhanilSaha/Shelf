package com.example.shelf.feature.library

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Destinations of the library feature. */
@Serializable
sealed interface LibraryNavKey : NavKey {
    /** The three shelves. This is the app's start destination. */
    @Serializable
    data object Shelves : LibraryNavKey

    /** Edit page, rating, shelf and notes of a book in the library. */
    @Serializable
    data class Book(val workKey: String) : LibraryNavKey
}
