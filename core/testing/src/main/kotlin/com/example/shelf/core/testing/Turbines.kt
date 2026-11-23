package com.example.shelf.core.testing

import app.cash.turbine.ReceiveTurbine

/**
 * Awaits items until one matches [predicate] and returns it. ViewModels built with `stateIn`
 * may or may not show an intermediate state (such as Loading) depending on dispatcher timing;
 * this keeps tests about the states that matter without depending on that timing.
 */
suspend fun <T> ReceiveTurbine<T>.awaitUntil(predicate: (T) -> Boolean): T {
    while (true) {
        val item = awaitItem()
        if (predicate(item)) return item
    }
}
