package com.example.shelf.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.example.shelf.feature.bookdetail.BookDetailNavKey
import com.example.shelf.feature.library.LibraryNavKey
import com.example.shelf.feature.scanner.ScannerNavKey
import com.example.shelf.feature.search.SearchNavKey
import com.example.shelf.feature.stats.StatsNavKey
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigatorTest {
    private val state = NavigationState(
        currentTopLevel = mutableStateOf(TopLevelDestination.START),
        backStacks = TopLevelDestination.entries.associateWith { NavBackStack<NavKey>(it.key) },
    )
    private val navigator = Navigator(state)

    @Test
    fun `navigating to a tab root selects the tab without pushing`() {
        navigator.navigate(SearchNavKey)

        assertEquals(TopLevelDestination.SEARCH, state.currentTopLevel)
        assertEquals(listOf<NavKey>(SearchNavKey), state.currentBackStack.toList())
    }

    @Test
    fun `other keys are pushed on the selected tab`() {
        navigator.navigate(SearchNavKey)
        navigator.navigate(BookDetailNavKey("OL1W"))

        assertEquals(BookDetailNavKey("OL1W"), state.currentKey)
        assertEquals(
            listOf<NavKey>(LibraryNavKey.Shelves),
            state.backStacks.getValue(TopLevelDestination.LIBRARY).toList(),
        )
    }

    @Test
    fun `each tab keeps its own stack when switching`() {
        navigator.navigate(SearchNavKey)
        navigator.navigate(BookDetailNavKey("OL1W"))
        navigator.select(TopLevelDestination.STATS)
        navigator.select(TopLevelDestination.SEARCH)

        assertEquals(BookDetailNavKey("OL1W"), state.currentKey)
    }

    @Test
    fun `reselecting the current tab pops it to its root`() {
        navigator.navigate(LibraryNavKey.Book("OL1W"))
        navigator.navigate(BookDetailNavKey("OL1W"))
        navigator.select(TopLevelDestination.LIBRARY)

        assertEquals(listOf<NavKey>(LibraryNavKey.Shelves), state.currentBackStack.toList())
    }

    @Test
    fun `back pops, then returns to the start tab from another tab's root`() {
        navigator.navigate(SearchNavKey)
        navigator.navigate(BookDetailNavKey("OL1W"))

        navigator.goBack()
        assertEquals(SearchNavKey, state.currentKey)

        navigator.goBack()
        assertEquals(TopLevelDestination.LIBRARY, state.currentTopLevel)
    }

    @Test
    fun `back at the start tab root keeps the root`() {
        navigator.goBack()

        assertEquals(LibraryNavKey.Shelves, state.currentKey)
    }

    @Test
    fun `replaceCurrent swaps the scanner for the book detail`() {
        navigator.navigate(SearchNavKey)
        navigator.navigate(ScannerNavKey)
        navigator.replaceCurrent(BookDetailNavKey("OL1W"))

        assertEquals(listOf(SearchNavKey, BookDetailNavKey("OL1W")), state.currentBackStack.toList())
    }

    @Test
    fun `opening a deep link selects the tab and replaces its stack`() {
        navigator.navigate(LibraryNavKey.Book("OL9W"))
        navigator.open(
            DeepLinkTarget(TopLevelDestination.STATS, listOf(StatsNavKey.Overview, StatsNavKey.LogReading)),
        )

        assertEquals(TopLevelDestination.STATS, state.currentTopLevel)
        assertEquals(listOf<NavKey>(StatsNavKey.Overview, StatsNavKey.LogReading), state.currentBackStack.toList())
    }
}
