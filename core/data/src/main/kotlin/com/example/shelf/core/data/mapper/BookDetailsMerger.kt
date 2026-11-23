package com.example.shelf.core.data.mapper

import com.example.shelf.core.database.model.BookEntity
import com.example.shelf.core.model.Isbn
import com.example.shelf.core.network.model.EditionDto
import com.example.shelf.core.network.model.WorkDto

private const val MAX_SUBJECTS = 12

// Four digits not touching other digits, so "c1999" and "1999-05" match but "123456" does not.
private val yearPattern = Regex("""(?<!\d)(1[0-9]{3}|20[0-9]{2})(?!\d)""")

/** Extracts a four-digit year from Open Library's free-form dates ("January 1938", "c1999"). */
internal fun parseYear(text: String?): Int? = text?.let { yearPattern.find(it)?.value?.toInt() }

private val inlineLink = Regex("""\[([^\]]+)]\([^)]*\)""")
private val referenceLink = Regex("""\[([^\]]+)]\[[^\]]*]""")
private val footnoteDefinition = Regex("""^\[[^\]]+]:\s*\S.*$""")
private val horizontalRule = Regex("""^([-*_])(\s*\1){2,}$""")
private val underscoreEmphasis = Regex("""(?<!\w)_+(?=\S)|(?<=\S)_+(?!\w)""")
private val extraBlankLines = Regex("""\n{3,}""")

/**
 * Open Library descriptions are often Markdown: `**bold**`, `*Title*`, `[text](url)`, footnotes
 * such as `([source][1])` with a `[1]: https://...` line, and `----` rules. The app shows plain
 * text, so the markup is removed and the words are kept.
 */
internal fun String.toPlainDescription(): String = replace("\r\n", "\n")
    .lineSequence()
    .filterNot { line -> line.trim().let { footnoteDefinition.matches(it) || horizontalRule.matches(it) } }
    .joinToString("\n")
    .replace(inlineLink, "$1")
    .replace(referenceLink, "$1")
    .replace("*", "")
    .replace(underscoreEmphasis, "")
    .replace(extraBlankLines, "\n\n")
    .trim()

private val machineTag = Regex("""^[a-z_]+:\S""")

/** Catalogue tags such as `genre:fantasy` or `nyt:combined-print-and-e-book-fiction=2020-10-04`. */
private fun String.isMachineTag(): Boolean = machineTag.containsMatchIn(this) || '=' in this

/**
 * Combines a work, its first edition and its authors' names with what is already cached. A
 * value missing from the new responses never erases a value we already had, so a partial fetch
 * (for example an author request that failed) cannot make the book worse.
 *
 * @param indexedFirstPublishYear the year Open Library's search index computes from all editions
 */
internal fun mergeBookDetails(
    workKey: String,
    existing: BookEntity?,
    work: WorkDto,
    edition: EditionDto?,
    authorNames: List<String>,
    fetchedAtMillis: Long,
    indexedFirstPublishYear: Int? = null,
): BookEntity = BookEntity(
    workKey = workKey,
    title = work.title?.trim()?.takeIf { it.isNotEmpty() }
        ?: existing?.title
        ?: edition?.title?.trim()?.takeIf { it.isNotEmpty() }
        ?: workKey,
    authors = authorNames.ifEmpty { existing?.authors.orEmpty() },
    // Every source is a claim about the first publication and later reprints are the usual
    // error (the work record of The Fellowship of the Ring says 1972), so the earliest wins.
    // An edition's own date is when that printing appeared, so it is not a candidate.
    firstPublishYear = listOfNotNull(
        parseYear(work.firstPublishDate),
        existing?.firstPublishYear,
        indexedFirstPublishYear?.takeIf { it > 0 },
    ).minOrNull(),
    coverId = work.firstCoverId
        ?: existing?.coverId
        ?: edition?.covers?.firstOrNull { it > 0 },
    description = work.descriptionText?.toPlainDescription()?.takeIf { it.isNotEmpty() } ?: existing?.description,
    subjects = work.subjects
        .map(String::trim)
        .filter { it.isNotEmpty() && !it.isMachineTag() }
        .distinctBy { it.lowercase() }
        .take(MAX_SUBJECTS)
        .ifEmpty { existing?.subjects.orEmpty() },
    pageCount = edition?.numberOfPages?.takeIf { it > 0 } ?: existing?.pageCount,
    isbn = existing?.isbn
        ?: edition?.isbn13?.firstOrNull(Isbn::isValidIsbn13)
        ?: edition?.isbn10?.firstOrNull(Isbn::isValidIsbn10),
    detailsFetchedAt = fetchedAtMillis,
)

/**
 * A minimal cache row for a book found by ISBN, before its work details are fetched. The
 * edition's publish date is left out on purpose: it is not the work's first publication.
 */
internal fun EditionDto.toSeedEntity(workKey: String, isbn: String): BookEntity = BookEntity(
    workKey = workKey,
    title = title?.trim()?.takeIf { it.isNotEmpty() } ?: workKey,
    authors = emptyList(),
    firstPublishYear = null,
    coverId = covers.firstOrNull { it > 0 },
    description = null,
    subjects = emptyList(),
    pageCount = numberOfPages?.takeIf { it > 0 },
    isbn = isbn,
    detailsFetchedAt = null,
)
