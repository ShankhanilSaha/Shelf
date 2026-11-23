package com.example.shelf.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.example.shelf.feature.auth.AuthNavKey
import com.example.shelf.feature.auth.AuthRoute
import com.example.shelf.feature.bookdetail.BookDetailNavKey
import com.example.shelf.feature.bookdetail.BookDetailRoute
import com.example.shelf.feature.library.LibraryNavKey
import com.example.shelf.feature.library.LibraryRoute
import com.example.shelf.feature.scanner.ScannerNavKey
import com.example.shelf.feature.scanner.ScannerRoute
import com.example.shelf.feature.search.SearchNavKey
import com.example.shelf.feature.search.SearchRoute
import com.example.shelf.feature.settings.SettingsNavKey
import com.example.shelf.feature.settings.SettingsRoute
import com.example.shelf.feature.stats.StatsNavKey
import com.example.shelf.feature.stats.StatsRoute

/**
 * Wires every feature's entry point to its key. Features only receive callbacks; they never
 * know about each other or about the navigator.
 */
fun shelfEntryProvider(navigator: Navigator): (NavKey) -> NavEntry<NavKey> = entryProvider {
    entry<LibraryNavKey.Shelves> { key -> Library(key, navigator) }
    entry<LibraryNavKey.Book> { key -> Library(key, navigator) }
    entry<SearchNavKey> {
        SearchRoute(
            onBookClick = { workKey -> navigator.navigate(BookDetailNavKey(workKey)) },
            onScanClick = { navigator.navigate(ScannerNavKey) },
        )
    }
    entry<BookDetailNavKey> { key ->
        BookDetailRoute(navKey = key, onBack = navigator::goBack)
    }
    entry<ScannerNavKey> {
        ScannerRoute(
            onOpenBook = { workKey -> navigator.replaceCurrent(BookDetailNavKey(workKey)) },
            onBack = navigator::goBack,
        )
    }
    entry<StatsNavKey.Overview> { key -> Stats(key, navigator) }
    entry<StatsNavKey.LogReading>(metadata = BottomSheetSceneStrategy.bottomSheet()) { key -> Stats(key, navigator) }
    entry<SettingsNavKey> {
        SettingsRoute(onSignInClick = { navigator.navigate(AuthNavKey) })
    }
    entry<AuthNavKey> {
        AuthRoute(onBack = navigator::goBack)
    }
}

@Composable
private fun Library(key: LibraryNavKey, navigator: Navigator) {
    LibraryRoute(
        navKey = key,
        onBookClick = { workKey -> navigator.navigate(LibraryNavKey.Book(workKey)) },
        onOpenDetails = { workKey -> navigator.navigate(BookDetailNavKey(workKey)) },
        onSearchClick = { navigator.select(TopLevelDestination.SEARCH) },
        onScanClick = { navigator.navigate(ScannerNavKey) },
        onBack = navigator::goBack,
    )
}

@Composable
private fun Stats(key: StatsNavKey, navigator: Navigator) {
    StatsRoute(
        navKey = key,
        onLogReadingClick = { navigator.navigate(StatsNavKey.LogReading) },
        onDismissLogReading = navigator::goBack,
    )
}
