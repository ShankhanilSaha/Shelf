package com.example.shelf.core.common.dispatcher

import javax.inject.Qualifier

/** Qualifies an injected [kotlinx.coroutines.CoroutineDispatcher] so tests can replace it. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: ShelfDispatchers)

enum class ShelfDispatchers {
    Default,
    IO,
}

/** A [kotlinx.coroutines.CoroutineScope] that lives as long as the application process. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope
