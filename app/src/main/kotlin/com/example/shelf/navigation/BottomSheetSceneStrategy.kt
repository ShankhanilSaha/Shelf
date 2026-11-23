package com.example.shelf.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

/** Shows an entry marked with [bottomSheet] metadata in a [ModalBottomSheet] above the previous entries. */
@OptIn(ExperimentalMaterial3Api::class)
private class BottomSheetScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val entry: NavEntry<T>,
    private val onBack: () -> Unit,
) : OverlayScene<T> {

    override val entries: List<NavEntry<T>> = listOf(entry)

    private var sheetState: SheetState? = null

    override val content: @Composable () -> Unit = {
        // Starts hidden so the sheet animates in; no half-expanded stop for a short form.
        val state = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )
        sheetState = state
        ModalBottomSheet(onDismissRequest = onBack, sheetState = state) {
            entry.Content()
        }
    }

    /** Plays the hide animation when the entry is popped from the back stack. */
    override suspend fun onRemove() {
        sheetState?.hide()
    }
}

/** Must be listed before strategies that render non-overlay scenes. */
class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val last = entries.lastOrNull() ?: return null
        if (last.metadata[BottomSheetKey] != true) return null
        return BottomSheetScene(
            key = last.contentKey,
            previousEntries = entries.dropLast(1),
            overlaidEntries = entries.dropLast(1),
            entry = last,
            onBack = onBack,
        )
    }

    companion object {
        private object BottomSheetKey : NavMetadataKey<Boolean>

        /** Metadata for `entry<Key>(metadata = bottomSheet())`. */
        fun bottomSheet(): Map<String, Any> = metadata { put(BottomSheetKey, true) }
    }
}
