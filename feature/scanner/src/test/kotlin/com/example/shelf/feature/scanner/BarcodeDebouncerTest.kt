package com.example.shelf.feature.scanner

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BarcodeDebouncerTest {
    private val debouncer = BarcodeDebouncer(windowMillis = 2_000)

    @Test
    fun `the same code is accepted once while it stays in view`() {
        assertTrue(debouncer.accept("9780261103573", nowMillis = 0))
        assertFalse(debouncer.accept("9780261103573", nowMillis = 100))
        assertFalse(debouncer.accept("9780261103573", nowMillis = 1_900))
        // Seen continuously, so the window keeps sliding.
        assertFalse(debouncer.accept("9780261103573", nowMillis = 3_000))
    }

    @Test
    fun `the code is accepted again after it has been away for the window`() {
        debouncer.accept("9780261103573", nowMillis = 0)

        assertTrue(debouncer.accept("9780261103573", nowMillis = 2_500))
    }

    @Test
    fun `a different code is accepted immediately`() {
        debouncer.accept("9780261103573", nowMillis = 0)

        assertTrue(debouncer.accept("9780306406157", nowMillis = 10))
        assertTrue(debouncer.accept("9780261103573", nowMillis = 20))
    }
}
