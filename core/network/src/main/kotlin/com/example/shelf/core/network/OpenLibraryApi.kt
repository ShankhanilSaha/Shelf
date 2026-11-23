package com.example.shelf.core.network

import com.example.shelf.core.network.model.AuthorDto
import com.example.shelf.core.network.model.EditionDto
import com.example.shelf.core.network.model.EditionsResponseDto
import com.example.shelf.core.network.model.SearchResponseDto
import com.example.shelf.core.network.model.WorkDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Open Library endpoints (https://openlibrary.org/developers/api). No API key is required. */
interface OpenLibraryApi {
    @GET("search.json")
    suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("fields") fields: String = SEARCH_FIELDS,
    ): SearchResponseDto

    @GET("works/{id}.json")
    suspend fun work(@Path("id") workId: String): WorkDto

    @GET("works/{id}/editions.json")
    suspend fun editions(@Path("id") workId: String, @Query("limit") limit: Int): EditionsResponseDto

    @GET("authors/{id}.json")
    suspend fun author(@Path("id") authorId: String): AuthorDto

    /** Redirects (HTTP 302) to the edition's `/books/{id}.json`; OkHttp follows it. */
    @GET("isbn/{isbn}.json")
    suspend fun editionByIsbn(@Path("isbn") isbn: String): EditionDto

    companion object {
        const val BASE_URL = "https://openlibrary.org/"
        const val SEARCH_FIELDS =
            "key,title,author_name,first_publish_year,cover_i,isbn,number_of_pages_median"
    }
}
