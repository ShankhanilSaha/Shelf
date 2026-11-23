package com.example.shelf.core.data.sync

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConflictPolicyTest {
    @Test
    fun `a newer remote edit replaces the local row, dirty or not`() {
        assertTrue(ConflictPolicy.remoteWins(localUpdatedAt = 100, localDirty = true, remoteUpdatedAt = 200))
        assertTrue(ConflictPolicy.remoteWins(localUpdatedAt = 100, localDirty = false, remoteUpdatedAt = 200))
    }

    @Test
    fun `an older remote edit never replaces a newer local one`() {
        assertFalse(ConflictPolicy.remoteWins(localUpdatedAt = 200, localDirty = true, remoteUpdatedAt = 100))
        assertFalse(ConflictPolicy.remoteWins(localUpdatedAt = 200, localDirty = false, remoteUpdatedAt = 100))
    }

    @Test
    fun `a document missing locally is always taken`() {
        assertTrue(ConflictPolicy.remoteWins(localUpdatedAt = null, localDirty = false, remoteUpdatedAt = 1))
    }

    @Test
    fun `on a tie the server copy wins over unsaved local changes so devices converge`() {
        assertTrue(ConflictPolicy.remoteWins(localUpdatedAt = 100, localDirty = true, remoteUpdatedAt = 100))
        assertFalse(ConflictPolicy.localWins(localUpdatedAt = 100, remoteUpdatedAt = 100))
    }

    @Test
    fun `on a tie a clean local row is the same version and is left alone`() {
        assertFalse(ConflictPolicy.remoteWins(localUpdatedAt = 100, localDirty = false, remoteUpdatedAt = 100))
    }

    @Test
    fun `uploads only overwrite older server copies`() {
        assertTrue(ConflictPolicy.localWins(localUpdatedAt = 200, remoteUpdatedAt = 100))
        assertTrue(ConflictPolicy.localWins(localUpdatedAt = 1, remoteUpdatedAt = null))
        assertFalse(ConflictPolicy.localWins(localUpdatedAt = 100, remoteUpdatedAt = 200))
    }

    @Test
    fun `tombstones are kept for thirty days`() {
        val now = Duration.ofDays(100).toMillis()

        assertEquals(Duration.ofDays(70).toMillis(), ConflictPolicy.tombstoneCutoff(now))
    }
}
