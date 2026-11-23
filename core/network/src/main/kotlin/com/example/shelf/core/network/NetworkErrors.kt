package com.example.shelf.core.network

import com.example.shelf.core.common.result.ShelfError
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

private const val HTTP_NOT_FOUND = 404
private const val HTTP_SERVER_ERRORS = 500

/** Classifies an exception thrown by [OpenLibraryDataSource] into a UI-friendly [ShelfError]. */
fun Throwable.toShelfError(): ShelfError = when (this) {
    is HttpException -> when {
        code() == HTTP_NOT_FOUND -> ShelfError.NotFound
        code() >= HTTP_SERVER_ERRORS -> ShelfError.Server
        else -> ShelfError.Unknown
    }

    is IOException -> ShelfError.Network

    // The body did not match what we expected; treat it as a server-side problem.
    is SerializationException, is IllegalArgumentException -> ShelfError.Server

    else -> ShelfError.Unknown
}
