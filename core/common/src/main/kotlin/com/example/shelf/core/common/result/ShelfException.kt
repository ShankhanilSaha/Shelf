package com.example.shelf.core.common.result

/**
 * A failure that has already been classified. The network layer throws it, Paging carries it in
 * `LoadState.Error`, and the UI turns [error] into a message.
 */
class ShelfException(val error: ShelfError, cause: Throwable? = null) : Exception(error.name, cause)

/** The classified error behind any throwable; unclassified exceptions are [ShelfError.Unknown]. */
val Throwable.shelfError: ShelfError
    get() = (this as? ShelfException)?.error ?: ShelfError.Unknown
