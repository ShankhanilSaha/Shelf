package com.example.shelf.feature.scanner

/**
 * The analyzer reports the same barcode many times per second while it stays in view. This lets
 * a value through once, and again only after it has been out of the stream for [windowMillis].
 */
class BarcodeDebouncer(private val windowMillis: Long = DEFAULT_WINDOW_MILLIS) {
    private var lastValue: String? = null
    private var lastSeenAt = Long.MIN_VALUE

    /** True when [value], seen at [nowMillis], should be acted on. */
    @Synchronized
    fun accept(value: String, nowMillis: Long): Boolean {
        val repeat = value == lastValue && nowMillis - lastSeenAt < windowMillis
        lastValue = value
        lastSeenAt = nowMillis
        return !repeat
    }

    companion object {
        const val DEFAULT_WINDOW_MILLIS = 2_000L
    }
}
