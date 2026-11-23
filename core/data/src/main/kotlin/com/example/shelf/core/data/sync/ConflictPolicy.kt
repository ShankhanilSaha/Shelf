package com.example.shelf.core.data.sync

import java.time.Duration

/**
 * Conflict policy for sync: last write wins per document, by the time of the edit (`updatedAt`,
 * epoch millis on the device that made it). A deletion is a write too: the document becomes a
 * tombstone (`deleted = true`), so an edit made after a deletion brings the book back and a
 * deletion made after an edit removes it. Tombstones are dropped after [TOMBSTONE_TTL].
 */
object ConflictPolicy {
    /** How long tombstones are kept so that every device gets to see the deletion. */
    val TOMBSTONE_TTL: Duration = Duration.ofDays(30)

    /**
     * Whether a pulled document replaces the local row.
     *
     * The newer edit wins. On a tie the server copy wins when the local row has unsaved changes,
     * so that every device converges on what the server has; a clean local row with the same
     * time is the same version (usually our own upload coming back) and is left alone.
     *
     * @param localUpdatedAt null when the row does not exist locally
     */
    fun remoteWins(localUpdatedAt: Long?, localDirty: Boolean, remoteUpdatedAt: Long): Boolean = when {
        localUpdatedAt == null -> true
        remoteUpdatedAt > localUpdatedAt -> true
        remoteUpdatedAt < localUpdatedAt -> false
        else -> localDirty
    }

    /**
     * Whether an upload may overwrite the server copy: only when it is strictly newer, so a tie
     * keeps the server copy (see [remoteWins]).
     *
     * @param remoteUpdatedAt null when the server has no such document
     */
    fun localWins(localUpdatedAt: Long, remoteUpdatedAt: Long?): Boolean =
        remoteUpdatedAt == null || localUpdatedAt > remoteUpdatedAt

    /** The cut-off for purging: tombstones last written before this time are dropped. */
    fun tombstoneCutoff(nowMillis: Long): Long = nowMillis - TOMBSTONE_TTL.toMillis()
}
