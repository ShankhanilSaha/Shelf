package com.example.shelf.core.common.result

/**
 * Outcome of a one-shot data operation (network refresh, lookup, write). Errors are classified so
 * the UI can show a meaningful message without knowing about HTTP or IO exceptions.
 */
sealed interface ShelfResult<out T> {
    data class Success<out T>(val data: T) : ShelfResult<T>

    data class Failure(val error: ShelfError, val cause: Throwable? = null) : ShelfResult<Nothing>
}

enum class ShelfError {
    /** No connection, timeout, DNS failure. Retrying later may help. */
    Network,

    /** The server answered that the resource does not exist. */
    NotFound,

    /** The server failed or returned something we could not understand. */
    Server,

    /** The operation needs a signed-in user or a configured backend. */
    Unavailable,

    Unknown,
}

inline fun <T, R> ShelfResult<T>.map(transform: (T) -> R): ShelfResult<R> = when (this) {
    is ShelfResult.Success -> ShelfResult.Success(transform(data))
    is ShelfResult.Failure -> this
}

inline fun <T> ShelfResult<T>.onSuccess(action: (T) -> Unit): ShelfResult<T> {
    if (this is ShelfResult.Success) action(data)
    return this
}

inline fun <T> ShelfResult<T>.onFailure(action: (ShelfError) -> Unit): ShelfResult<T> {
    if (this is ShelfResult.Failure) action(error)
    return this
}

fun <T> ShelfResult<T>.getOrNull(): T? = (this as? ShelfResult.Success)?.data
