package com.example.shelf.core.model

/** A search result: enough to render a row and to seed the book cache. */
data class BookSummary(
    val workKey: String,
    val title: String,
    val authors: List<String>,
    val firstPublishYear: Int?,
    val coverId: Long?,
    val isbn: String?,
    val pageCount: Int?,
)

/**
 * A book as cached locally. Fields that come from the work details endpoint are null or empty
 * until [hasDetails] is true.
 */
data class Book(
    val workKey: String,
    val title: String,
    val authors: List<String>,
    val firstPublishYear: Int? = null,
    val coverId: Long? = null,
    val description: String? = null,
    val subjects: List<String> = emptyList(),
    val pageCount: Int? = null,
    val isbn: String? = null,
    val hasDetails: Boolean = false,
)

fun BookSummary.toBook(): Book = Book(
    workKey = workKey,
    title = title,
    authors = authors,
    firstPublishYear = firstPublishYear,
    coverId = coverId,
    isbn = isbn,
    pageCount = pageCount,
)

/** Cover sizes served by the Open Library covers API. */
enum class CoverSize(internal val suffix: String) {
    SMALL("S"),
    MEDIUM("M"),
    LARGE("L"),
}

/** `https://covers.openlibrary.org/b/id/{coverId}-M.jpg` */
fun coverUrl(coverId: Long, size: CoverSize = CoverSize.MEDIUM): String =
    "https://covers.openlibrary.org/b/id/$coverId-${size.suffix}.jpg"
