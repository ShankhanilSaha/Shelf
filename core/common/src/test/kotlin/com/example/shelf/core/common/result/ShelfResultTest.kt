package com.example.shelf.core.common.result

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShelfResultTest {
    @Test
    fun `map transforms success and keeps failure`() {
        assertEquals(ShelfResult.Success(4), ShelfResult.Success(2).map { it * 2 })
        val failure: ShelfResult<Int> = ShelfResult.Failure(ShelfError.Network)
        assertEquals(failure, failure.map { it * 2 })
    }

    @Test
    fun `callbacks run only for their branch`() {
        var success = 0
        var error: ShelfError? = null
        ShelfResult.Success(1).onSuccess { success = it }.onFailure { error = it }
        assertEquals(1, success)
        assertNull(error)

        ShelfResult.Failure(ShelfError.NotFound).onFailure { error = it }
        assertEquals(ShelfError.NotFound, error)
    }

    @Test
    fun `getOrNull returns data only on success`() {
        assertEquals("a", ShelfResult.Success("a").getOrNull())
        assertNull(ShelfResult.Failure(ShelfError.Unknown).getOrNull())
    }
}
