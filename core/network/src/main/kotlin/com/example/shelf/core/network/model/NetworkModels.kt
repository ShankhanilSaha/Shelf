package com.example.shelf.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** `GET /search.json` */
@Serializable
data class SearchResponseDto(
    @SerialName("numFound") val numFound: Int = 0,
    @SerialName("start") val start: Int = 0,
    @SerialName("docs") val docs: List<SearchDocDto> = emptyList(),
)

@Serializable
data class SearchDocDto(
    @SerialName("key") val key: String,
    @SerialName("title") val title: String? = null,
    @SerialName("author_name") val authorNames: List<String> = emptyList(),
    @SerialName("first_publish_year") val firstPublishYear: Int? = null,
    @SerialName("cover_i") val coverId: Long? = null,
    @SerialName("isbn") val isbns: List<String> = emptyList(),
    @SerialName("number_of_pages_median") val numberOfPagesMedian: Int? = null,
)

/** `GET /works/{id}.json` */
@Serializable
data class WorkDto(
    @SerialName("key") val key: String,
    @SerialName("title") val title: String? = null,
    /** Either a plain string or `{"type": "/type/text", "value": "..."}`; see [descriptionText]. */
    @SerialName("description") val description: JsonElement? = null,
    @SerialName("subjects") val subjects: List<String> = emptyList(),
    @SerialName("covers") val covers: List<Long> = emptyList(),
    @SerialName("authors") val authors: List<WorkAuthorDto> = emptyList(),
    @SerialName("first_publish_date") val firstPublishDate: String? = null,
) {
    val descriptionText: String?
        get() = when (val value = description) {
            is JsonPrimitive -> value.contentOrNull
            is JsonObject -> value["value"]?.jsonPrimitive?.contentOrNull
            else -> null
        }?.trim()?.takeIf { it.isNotEmpty() }

    /** Open Library uses -1 for "no cover" inside the covers array. */
    val firstCoverId: Long?
        get() = covers.firstOrNull { it > 0 }

    val authorKeys: List<String>
        get() = authors.mapNotNull { it.authorKey }
}

/**
 * An author reference inside a work. Usually `{"author": {"key": "/authors/OL1A"}}`, but some
 * old records use a bare string, so the element is parsed leniently.
 */
@Serializable
data class WorkAuthorDto(@SerialName("author") val author: JsonElement? = null) {
    val authorKey: String?
        get() = when (val value = author) {
            is JsonObject -> value["key"]?.jsonPrimitive?.contentOrNull
            is JsonPrimitive -> value.contentOrNull
            else -> null
        }
}

@Serializable
data class KeyRefDto(@SerialName("key") val key: String)

/** `GET /works/{id}/editions.json` */
@Serializable
data class EditionsResponseDto(
    @SerialName("size") val size: Int = 0,
    @SerialName("entries") val entries: List<EditionDto> = emptyList(),
)

/** An edition, from `editions.json` or from `GET /isbn/{isbn}.json` (which redirects to `/books/{id}.json`). */
@Serializable
data class EditionDto(
    @SerialName("key") val key: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("number_of_pages") val numberOfPages: Int? = null,
    @SerialName("works") val works: List<KeyRefDto> = emptyList(),
    @SerialName("covers") val covers: List<Long> = emptyList(),
    @SerialName("isbn_13") val isbn13: List<String> = emptyList(),
    @SerialName("isbn_10") val isbn10: List<String> = emptyList(),
    @SerialName("publish_date") val publishDate: String? = null,
)

/** `GET /authors/{id}.json` */
@Serializable
data class AuthorDto(
    @SerialName("key") val key: String,
    @SerialName("name") val name: String? = null,
    @SerialName("personal_name") val personalName: String? = null,
) {
    val displayName: String?
        get() = (name ?: personalName)?.trim()?.takeIf { it.isNotEmpty() }
}
