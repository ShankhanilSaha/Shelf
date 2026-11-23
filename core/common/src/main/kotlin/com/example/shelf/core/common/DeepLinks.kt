package com.example.shelf.core.common

/** URIs understood by the app's single activity. Shared so that notifications can link into it. */
object DeepLinks {
    const val SCHEME = "shelf"

    /** Opens the "Log reading" sheet above stats. */
    const val LOG_READING = "shelf://log"

    /** Opens a book's details; `{workKey}` is an Open Library work id such as `OL27448W`. */
    const val BOOK_PATTERN = "shelf://book/{workKey}"

    fun book(workKey: String): String = BOOK_PATTERN.replace("{workKey}", workKey)
}
