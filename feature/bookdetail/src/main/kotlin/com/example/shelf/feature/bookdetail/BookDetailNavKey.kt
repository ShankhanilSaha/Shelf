package com.example.shelf.feature.bookdetail

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Details of one Open Library work, for example `BookDetailNavKey("OL27448W")`. */
@Serializable
data class BookDetailNavKey(val workKey: String) : NavKey
