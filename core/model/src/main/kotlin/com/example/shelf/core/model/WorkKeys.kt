package com.example.shelf.core.model

/**
 * Open Library identifies a work (every edition of a book) with a key such as `/works/OL27448W`.
 * Shelf stores and passes around only the id part, for example `OL27448W`.
 */
object WorkKeys {
    private const val PREFIX = "/works/"
    private val idPattern = Regex("^OL[1-9][0-9]*W$")

    /** True for a bare work id such as `OL27448W`. */
    fun isValid(id: String): Boolean = idPattern.matches(id)

    /** Converts `/works/OL27448W` (or a bare id) to `OL27448W`, or returns null when malformed. */
    fun fromOpenLibraryKey(key: String): String? = key.trim().removePrefix(PREFIX).takeIf(::isValid)

    /** Builds the Open Library key (`/works/OL27448W`) for a bare id. */
    fun toOpenLibraryKey(id: String): String = PREFIX + id
}
