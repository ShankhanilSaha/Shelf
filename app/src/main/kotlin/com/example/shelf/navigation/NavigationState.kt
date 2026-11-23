package com.example.shelf.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

/**
 * Navigation state: one serializable back stack per tab plus the selected tab. Both survive
 * configuration changes and process death. Mutated only through [Navigator].
 */
@Stable
class NavigationState(
    currentTopLevel: MutableState<TopLevelDestination>,
    val backStacks: Map<TopLevelDestination, NavBackStack<NavKey>>,
) {
    var currentTopLevel: TopLevelDestination by currentTopLevel

    val currentBackStack: NavBackStack<NavKey>
        get() = backStacks.getValue(currentTopLevel)

    /** The key on top of the selected tab's stack, i.e. what is on screen. */
    val currentKey: NavKey
        get() = currentBackStack.last()

    /**
     * Entries for [androidx.navigation3.ui.NavDisplay]. Every stack is decorated (so the state and
     * ViewModels of hidden tabs are kept), but only the start tab and the selected tab are shown:
     * Back from another tab's root returns to the start tab ("exit through home").
     */
    @Composable
    fun toDecoratedEntries(entryProvider: (NavKey) -> NavEntry<NavKey>): List<NavEntry<NavKey>> {
        val decorated = backStacks.mapValues { (_, stack) ->
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider,
            )
        }
        val visibleTabs = if (currentTopLevel == TopLevelDestination.START) {
            listOf(TopLevelDestination.START)
        } else {
            listOf(TopLevelDestination.START, currentTopLevel)
        }
        return visibleTabs.flatMap { decorated.getValue(it) }
    }
}

@Composable
fun rememberNavigationState(): NavigationState {
    val currentTopLevel = rememberSaveable { mutableStateOf(TopLevelDestination.START) }
    val backStacks = TopLevelDestination.entries.associateWith { rememberNavBackStack(it.key) }
    return remember { NavigationState(currentTopLevel, backStacks) }
}
