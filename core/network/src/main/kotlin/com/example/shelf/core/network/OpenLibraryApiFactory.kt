package com.example.shelf.core.network

import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Builds the Retrofit service. Public so tests can point it at a MockWebServer. */
object OpenLibraryApiFactory {
    /** Lenient JSON: Open Library documents carry many fields we do not use. */
    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun create(baseUrl: String, callFactory: Call.Factory, json: Json = this.json): OpenLibraryApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .callFactory(callFactory)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(OpenLibraryApi::class.java)

    /** The production [OpenLibraryDataSource] over [create]; used by tests in other modules. */
    fun createDataSource(baseUrl: String, callFactory: Call.Factory): OpenLibraryDataSource =
        RetrofitOpenLibraryDataSource(create(baseUrl, callFactory))
}
